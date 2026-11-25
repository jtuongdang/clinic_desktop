package vn.clinicmanagement.infrastructure.persistence.reception;

import org.junit.jupiter.api.*;
import vn.clinicmanagement.infrastructure.persistence.*;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import java.sql.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Kiểm tra bằng dữ liệu tổng hợp; mọi thay đổi thử nghiệm đều rollback. */
class PhieuDangKyKhamSQLMayChuTest {
    @Test void giaChotKhongDoiKhiSuaDanhMucVaGioiHanPhamVi() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("clinic.print.sql"));
        try(Connection connection=CoSoDuLieuKetNoi.getConnection()) {
            connection.setAutoCommit(false);
            JdbcUnitOfWork uow=new JdbcUnitOfWork();var transactions=new JdbcGiaoDichQuanLy(uow);
            Field field=JdbcGiaoDichQuanLy.class.getDeclaredField("currentConnection");field.setAccessible(true);
            @SuppressWarnings("unchecked") ThreadLocal<Connection> current=(ThreadLocal<Connection>)field.get(transactions);
            current.set(connection);
            try {
                int room,dept,facility;
                try(Statement s=connection.createStatement();ResultSet rs=s.executeQuery("SELECT TOP 1 r.id,r.maKhoa,k.maCoSo FROM PhongKham r JOIN Khoa k ON k.id=r.maKhoa WHERE r.isDeleted=0 AND r.hoatDong=1 AND k.isDeleted=0")){
                    assertTrue(rs.next());room=rs.getInt(1);dept=rs.getInt(2);facility=rs.getInt(3);
                }
                String unique=UUID.randomUUID().toString().substring(0,8);
                int patient=insert(connection,"INSERT INTO BenhNhan (maBenhNhanFormat,ho,ten,ngaySinh,gioiTinh) OUTPUT INSERTED.id VALUES ('TEST-PRINT-"+unique+"',N'Kiểm thử',N'Phiếu','1999-01-01','M')");
                int service=insert(connection,"INSERT INTO DanhMucDichVu (maDichVu,tenDichVu,loaiDichVu,donGia) OUTPUT INSERTED.id VALUES ('PRINT-"+unique+"',N'Dịch vụ thử nghiệm','KHAM',80000)");
                var provider=new JdbcKetNoiNhaCungCap(transactions);
                var visits=new TiepDonHangDoiRepository(provider,uow);
                var visit=visits.registerVisit(patient,facility,dept,room,service,"Khám bệnh","Kiểm thử",DoiTuongKham.THU_PHI,false,1);
                try(PreparedStatement ps=connection.prepareStatement("UPDATE DanhMucDichVu SET donGia=120000,tenDichVu=N'Đã đổi tên thử nghiệm' WHERE id=?")){ps.setInt(1,service);ps.executeUpdate();}
                var repository=new PhieuDangKyKhamRepository(provider);
                var slip=repository.getByVisit(visit.getId(),facility,dept).orElseThrow();
                assertEquals(0,new BigDecimal("80000").compareTo(slip.total()));
                assertEquals("Dịch vụ thử nghiệm",slip.services().get(0).name());
                assertNull(slip.collectorName()); assertNull(slip.collectedAt());
                assertTrue(repository.getByVisit(visit.getId(),facility+10000,dept).isEmpty());
                assertTrue(repository.getByVisit(visit.getId(),facility,dept+10000).isEmpty());
                var oldVisit=visits.registerVisit(patient,facility,dept,room,"Khám bệnh","",DoiTuongKham.THU_PHI,false,1);
                assertNull(repository.getByVisit(oldVisit.getId(),facility,dept).orElseThrow().total());
            } finally {connection.rollback();current.remove();}
        }
    }
    private static int insert(Connection c,String sql)throws SQLException {try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getInt(1);}}
}
