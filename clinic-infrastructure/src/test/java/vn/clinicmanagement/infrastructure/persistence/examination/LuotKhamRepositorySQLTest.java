package vn.clinicmanagement.infrastructure.persistence.examination;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.infrastructure.persistence.*;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Đọc/ghi lượt khám thật và rollback toàn bộ dữ liệu kiểm thử. */
class LuotKhamRepositorySQLTest {
    @Test void readWriteRollback() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("clinic.examination.sql"),
                "Bật clinic.examination.sql để chạy với SQL Server đã chuẩn bị dữ liệu.");
        try (Connection connection = CoSoDuLieuKetNoi.getConnection()) {
            connection.setAutoCommit(false);
            int id;
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT TOP 1 id FROM LuotKham WHERE isDeleted=0 AND id>0 ORDER BY id DESC")) {
                assertTrue(result.next()); id = result.getInt(1);
            }
            var unitOfWork = new JdbcUnitOfWork();
            var transactions = new JdbcGiaoDichQuanLy(unitOfWork);
            Field field = JdbcGiaoDichQuanLy.class.getDeclaredField("currentConnection");
            field.setAccessible(true);
            @SuppressWarnings("unchecked") ThreadLocal<Connection> current = (ThreadLocal<Connection>) field.get(transactions);
            current.set(connection);
            try {
                var repo = new LuotKhamRepository(new JdbcKetNoiNhaCungCap(transactions), unitOfWork);
                var visit = repo.getById(id).orElseThrow();
                String original = visit.getQuaTrinhBenhLy();
                String marker = "ROLLBACK-" + UUID.randomUUID();
                visit.setQuaTrinhBenhLy(marker);
                repo.update(visit);
                assertEquals(marker, repo.getById(id).orElseThrow().getQuaTrinhBenhLy());
                connection.rollback();
                assertEquals(original, repo.getById(id).orElseThrow().getQuaTrinhBenhLy());
            } finally { connection.rollback(); current.remove(); }
        }
    }
}
