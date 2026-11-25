package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.inventory.dispense.DispenseDonThuocCommand;
import vn.clinicmanagement.application.inventory.dispense.DispenseDonThuocHandler;
import vn.clinicmanagement.application.inventory.receive.NhapNhaCungCap.*;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.inventory.FEFOPhanBoDichVu;
import vn.clinicmanagement.domain.prescription.ChiTietDonThuoc;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.infrastructure.persistence.inventory.KhoRepository;
import vn.clinicmanagement.infrastructure.persistence.inventory.NhaThuocDanhSachRepository;
import vn.clinicmanagement.infrastructure.persistence.inventory.NhapNhaCungCapRepository;
import vn.clinicmanagement.infrastructure.persistence.prescription.DonThuocRepository;
import vn.clinicmanagement.infrastructure.persistence.system.JdbcKiemToanRepository;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Kiểm thử SQL Server thật (rollback hoàn toàn sau khi chạy) cho Nhập NCC và Cấp phát FEFO. */
class NhapNhaCungCapSQLTest {
    @Test
    void supplierReceiptDraftImportAndPrescriptionDispenseWorkEndToEndWithRollback() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("clinic.pharmacy.sql"));
        try (Connection real = CoSoDuLieuKetNoi.getConnection()) {
            real.setAutoCommit(false);
            try {
                int wh = scalar(real, "SELECT TOP 1 id FROM Kho WHERE isDeleted=0 AND trangThai='HOAT_DONG'");
                int facility = scalar(real, "SELECT maCoSo FROM Kho WHERE id=" + wh);
                int emp = scalar(real, "SELECT TOP 1 id FROM NhanVien WHERE isDeleted=0");
                int role = scalar(real, "SELECT TOP 1 id FROM VaiTro WHERE isDeleted=0");
                int visitId = scalar(real, "SELECT TOP 1 id FROM LuotKham WHERE maCoSo=" + facility + " AND isDeleted=0");

                String itemCode = "NCC-TST-" + UUID.randomUUID().toString().substring(0, 8);
                int itemId = scalar(real,
                        "INSERT INTO Item(maItem,tenItem,loaiItem,donViCoBan,hoatDong) OUTPUT inserted.id "
                                + "VALUES('" + itemCode + "',N'Thuốc kiểm thử NCC','THUOC',N'Viên',1)");

                Savepoint[] point = {null};
                Connection proxy = (Connection) Proxy.newProxyInstance(
                        Connection.class.getClassLoader(),
                        new Class<?>[]{Connection.class},
                        (o, m, a) -> {
                            if (Set.of("close", "commit").contains(m.getName())) return null;
                            if (m.getName().equals("rollback") && (a == null || a.length == 0)) {
                                real.rollback(point[0]);
                                return null;
                            }
                            try {
                                return m.invoke(real, a);
                            } catch (InvocationTargetException ex) {
                                throw ex.getCause();
                            }
                        });
                var uow = new JdbcUnitOfWork();
                var tx = new JdbcGiaoDichQuanLy(uow, () -> {
                    point[0] = real.setSavepoint();
                    return proxy;
                });
                var provider = new JdbcKetNoiNhaCungCap(tx);
                var audit = new JdbcKiemToanRepository(provider, uow, () -> proxy);
                var auth = new PhanQuyenNguCanh(emp, role, "PHARMACIST", facility, 1, UUID.randomUUID());

                var nccRepo = new NhapNhaCungCapRepository(provider, tx, audit);
                var supRes = nccRepo.saveSupplier(
                        new Supplier(0, "NCC Kiểm Thử " + itemCode, "0101010101", "0909000111", "ncc@test.vn", "Hà Nội"),
                        auth);
                assertTrue(supRes.isSuccess(), () -> supRes.getError().toString());
                Supplier sup = supRes.valueOrThrow();
                var loadedSuppliers = tx.inTransaction(() -> KetQua.success(nccRepo.suppliers(auth))).valueOrThrow();
                assertTrue(loadedSuppliers.stream().anyMatch(s -> s.id() == sup.id()));

                // 1. Lưu tạm -> không tạo TonKho
                var lineDraft = new ReceiptLine(itemId, itemCode, "Thuốc kiểm thử NCC", "Viên", "LO-DRAFT",
                        LocalDate.now().plusMonths(12), BigDecimal.valueOf(10), BigDecimal.valueOf(5000),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(7000));
                String draftCode = "PN-D-" + UUID.randomUUID().toString().substring(0, 8);
                var draftRes = nccRepo.save(new DraftReceipt(draftCode, wh, sup.id(), LocalDate.now(),
                        LocalDate.now(), "HD-01", "Shipper", "Nháp", List.of(lineDraft), false), auth);
                assertTrue(draftRes.isSuccess(), () -> draftRes.getError().toString());
                assertEquals(0, scalar(real, "SELECT COUNT(*) FROM TonKho WHERE maItem=" + itemId));

                // 2. Nhập kho -> tạo PhieuNhapKho DA_NHAP + ChiTietNhapKho + TonKho + InventoryTransaction
                var linePost = new ReceiptLine(itemId, itemCode, "Thuốc kiểm thử NCC", "Viên", "LO-POST",
                        LocalDate.now().plusMonths(6), BigDecimal.valueOf(10), BigDecimal.valueOf(5000),
                        BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.valueOf(8000));
                String postCode = "PN-P-" + UUID.randomUUID().toString().substring(0, 8);
                var postRes = nccRepo.save(new DraftReceipt(postCode, wh, sup.id(), LocalDate.now(),
                        LocalDate.now(), "HD-02", "Shipper", "Nhập thật", List.of(linePost), true), auth);
                assertTrue(postRes.isSuccess(), () -> postRes.getError().toString());
                assertEquals(1, scalar(real, "SELECT COUNT(*) FROM TonKho WHERE maItem=" + itemId + " AND soLuongTon=10"));
                assertEquals(1, scalar(real, "SELECT COUNT(*) FROM InventoryTransaction WHERE maItem=" + itemId + " AND loaiGiaoDich='NHAP_KHO'"));

                var listRepo = new NhaThuocDanhSachRepository(provider);
                var from = LocalDate.now().minusDays(1).atStartOfDay();
                var to = LocalDate.now().plusDays(1).atStartOfDay();
                var nhapTicket = tx.inTransaction(() -> KetQua.success(
                        listRepo.getTickets(facility, from, to).stream()
                                .filter(p -> postCode.equals(p.code())).findFirst().orElseThrow())).valueOrThrow();
                assertEquals("DA_NHAP", nhapTicket.status());
                var nhapDetails = tx.inTransaction(() -> KetQua.success(
                        listRepo.getTicketDetails("NHAP_KHO", nhapTicket.id(), facility))).valueOrThrow();
                assertEquals(1, nhapDetails.size());
                assertEquals("LO-POST", nhapDetails.get(0).batch());

                // 3. Cấp phát DonThuoc FEFO -> lưu PhieuXuatKho + ChiTietXuatKho + trừ TonKho + cập nhật ChiTietDonThuoc
                var rxRepo = new DonThuocRepository(provider, uow);
                var khoRepo = new KhoRepository(provider, uow);
                DonThuoc rx = new DonThuoc();
                rx.setMaLuotKham(visitId);
                rx.setMaBacSi(emp);
                rx.setMaCoSo(facility);
                rx.setMaKhoXuat(wh);
                rx.setTrangThai(TrangThaiDonThuoc.DA_DUYET);
                rx.setNgayKe(LocalDateTime.now());
                ChiTietDonThuoc detail = new ChiTietDonThuoc();
                detail.setMaItem(itemId);
                detail.setSoLuongKe(BigDecimal.valueOf(4));
                detail.setDonGiaKe(BigDecimal.valueOf(8000));
                rx.setChiTietDonThuocs(List.of(detail));
                tx.inTransaction(() -> { rxRepo.add(rx); return KetQua.success(rx.getId()); });

                var pendingBefore = tx.inTransaction(() -> KetQua.success(listRepo.getPrescriptions(facility, from, to))).valueOrThrow();
                assertTrue(pendingBefore.stream().anyMatch(d -> d.id() == rx.getId()));

                vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi noopAudit =
                        new vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi() {
                            public void appendInCurrentTransaction(vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu e) {}
                            public void appendIndependently(vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu e) {}
                            public void appendMedicalRecordInCurrentTransaction(vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu e) {}
                            public void appendMedicalRecordIndependently(vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu e) {}
                        };
                var dispenseHandler = new DispenseDonThuocHandler(rxRepo, khoRepo, new FEFOPhanBoDichVu(), tx, noopAudit);
                var dispenseRes = tx.inTransaction(() -> dispenseHandler.handle(
                        new DispenseDonThuocCommand(rx.getId(), emp, wh, auth)));
                assertTrue(dispenseRes.isSuccess(), () -> dispenseRes.getError().toString());
                assertEquals(6, scalar(real, "SELECT soLuongTon FROM TonKho WHERE maItem=" + itemId));
                assertEquals(1, scalar(real, "SELECT COUNT(*) FROM PhieuXuatKho WHERE maDonThuoc=" + rx.getId() + " AND trangThaiNoiBo='DA_XUAT'"));
                assertEquals(4, scalar(real, "SELECT soLuongDaXuat FROM ChiTietDonThuoc WHERE maDonThuoc=" + rx.getId()));
                assertEquals(1, scalar(real, "SELECT COUNT(*) FROM InventoryTransaction WHERE maItem=" + itemId + " AND loaiGiaoDich='XUAT_DON_THUOC'"));

                var pendingAfter = tx.inTransaction(() -> KetQua.success(listRepo.getPrescriptions(facility, from, to))).valueOrThrow();
                assertFalse(pendingAfter.stream().anyMatch(d -> d.id() == rx.getId()));
                var xuatTicket = tx.inTransaction(() -> KetQua.success(
                        listRepo.getTickets(facility, from, to).stream()
                                .filter(p -> ("PXK-" + rx.getId()).equals(p.code())).findFirst().orElseThrow())).valueOrThrow();
                assertEquals("XUAT_DON", xuatTicket.detailType());
            } finally {
                real.rollback();
            }
        }
    }

    private int scalar(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }
}
