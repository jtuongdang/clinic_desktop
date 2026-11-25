package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.*;
import vn.clinicmanagement.infrastructure.persistence.billing.HoaDonRepository;
import vn.clinicmanagement.domain.billing.*;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.application.billing.ClsVienPhiDongBo;
import vn.clinicmanagement.domain.common.enums.*;
import java.sql.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Kiểm tra SQL thật; migration và mọi chứng từ thử nghiệm đều rollback. */
class BillingLedgerSQLTest {
    @Test void migrationLedgerIdentityConcurrencyAndImmutableReceipts() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("clinic.billing.sql"));
        try (Connection connection = CoSoDuLieuKetNoi.getConnection()) {
            connection.setAutoCommit(false);
            var uow = new JdbcUnitOfWork();
            var transactions = new JdbcGiaoDichQuanLy(uow);
            var field = JdbcGiaoDichQuanLy.class.getDeclaredField("currentConnection"); field.setAccessible(true);
            @SuppressWarnings("unchecked") ThreadLocal<Connection> current = (ThreadLocal<Connection>) field.get(transactions);
            current.set(connection);
            try {
                try (var input = getClass().getClassLoader().getResourceAsStream("migrations/V14__Enforce_Visit_Billing_Ledger.sql")) {
                    try (var ps = connection.prepareStatement("SELECT COL_LENGTH('dbo.PhieuThu','requestId')"); var rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getObject(1) == null) CoSoDuLieuDiCuChay.executeSqlScript(connection, new String(input.readAllBytes(), StandardCharsets.UTF_8));
                    }
                }
                int facility, department, room, employee;
                try (var s = connection.createStatement(); var rs = s.executeQuery("SELECT TOP 1 p.id,p.maKhoa,k.maCoSo,(SELECT TOP 1 id FROM NhanVien WHERE isDeleted=0) AS employee FROM PhongKham p JOIN Khoa k ON k.id=p.maKhoa WHERE p.isDeleted=0 AND p.hoatDong=1 AND k.isDeleted=0")) {
                    assertTrue(rs.next()); room=rs.getInt(1); department=rs.getInt(2); facility=rs.getInt(3); employee=rs.getInt(4);
                }
                String unique=UUID.randomUUID().toString().substring(0,8);
                int patient=insert(connection,"INSERT INTO BenhNhan(maBenhNhanFormat,ho,ten,ngaySinh,gioiTinh) OUTPUT INSERTED.id VALUES('BILL-"+unique+"',N'Test',N'Billing','1999-01-01','M')");
                var provider=new JdbcKetNoiNhaCungCap(transactions);
                var visits=new vn.clinicmanagement.infrastructure.persistence.reception.TiepDonHangDoiRepository(provider,uow);
                var visit=visits.registerVisit(patient,facility,department,room,"Khám bệnh","",DoiTuongKham.THU_PHI,false,employee);
                var screen=new vn.clinicmanagement.infrastructure.persistence.billing.VienPhiManHinhRepository(provider);
                assertEquals("BILL-"+unique,screen.getByVisit(visit.getId(),facility).orElseThrow().patientCode());
                assertTrue(screen.getByVisit(visit.getId(),facility+10000).isEmpty());
                var repo=new HoaDonRepository(provider,uow);
                var billing=new ClsVienPhiDongBo(repo);
                var invoice=new HoaDon(); invoice.setSoHoaDon("TEST-BILL-"+unique); invoice.setMaLuotKham(visit.getId());
                invoice.setMaBenhNhan(patient);invoice.setMaCoSo(facility);
                var line=new ChiTietHoaDon();line.setLoaiDoiTuong("DICH_VU_KHAM");line.setMaDoiTuong(visit.getId());line.setMoTa("Test");line.setSoLuong(BigDecimal.ONE);line.setDonGia(new BigDecimal("120000"));
                invoice.addChiTiet(line);invoice.recalculateCharges();repo.save(invoice);
                int originalLine=line.getId();
                invoice.recordPayment(new BigDecimal("120000"),PhuongThucThanhToan.TIEN_MAT,employee,"");
                repo.save(invoice); var receipt=invoice.getPhieuThus().get(0);receipt.setRequestId(UUID.randomUUID());repo.savePhieuThu(receipt);
                assertEquals(originalLine,repo.getById(invoice.getId()).orElseThrow().getChiTietHoaDons().get(0).getId());
                var stale=repo.getById(invoice.getId()).orElseThrow();
                var order=new ChiDinhDichVu();order.setId(991234);order.setMaLuotKham(visit.getId());order.setSoLuong(BigDecimal.ONE);order.setDonGia(new BigDecimal("85000"));
                billing.addCharge(order,"Test CLS",employee,facility).valueOrThrow();
                var loaded=repo.getById(invoice.getId()).orElseThrow();
                assertEquals(0,new BigDecimal("205000").compareTo(loaded.getTongTien()));
                assertEquals(TrangThaiHoaDon.THANH_TOAN_MOT_PHAN,loaded.getTrangThai());
                assertThrows(IllegalStateException.class,()->repo.save(stale));
                billing.cancelCharge(order,employee,facility).valueOrThrow();
                assertEquals(TrangThaiHoaDon.DA_THANH_TOAN,repo.getById(invoice.getId()).orElseThrow().getTrangThai());
                assertEquals(1,repo.getPhieuThuByHoaDon(invoice.getId()).size());
                assertThrows(IllegalArgumentException.class,()->repo.savePhieuThu(receipt));
                try(var s=connection.createStatement()) {
                    assertThrows(SQLException.class,()->s.executeUpdate("UPDATE PhieuThu SET soTien=1 WHERE id="+receipt.getId()));
                }
            } finally { connection.rollback();current.remove(); }
        }
    }
    private static int insert(Connection connection,String sql) throws SQLException {
        try(var s=connection.createStatement();var rs=s.executeQuery(sql)){assertTrue(rs.next());return rs.getInt(1);}
    }
}
