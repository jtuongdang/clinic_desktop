package vn.clinicmanagement.infrastructure.persistence.inventory;

import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.*;
import vn.clinicmanagement.application.inventory.receive.NhapNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Triển khai JDBC lưu phiếu nhập nhà cung cấp, nhà cung cấp và tồn kho theo lô. */
public final class NhapNhaCungCapRepository implements NhapNhaCungCap {
    private static final DateTimeFormatter CODE_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmssSSS");
    private final JdbcKetNoiNhaCungCap connections;
    private final GiaoDichRanhGioi transactions;
    private final KiemToanWriteCongKetNoi audit;

    public NhapNhaCungCapRepository(
            JdbcKetNoiNhaCungCap connections,
            GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi audit) {
        this.connections = Objects.requireNonNull(connections, "connections");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public List<Warehouse> warehouses(PhanQuyenNguCanh auth) {
        if (!NhapNhaCungCap.permitted(auth)) return List.of();
        String sql = "SELECT id, tenKho FROM Kho WHERE maCoSo=? AND isDeleted=0 AND trangThai='HOAT_DONG' ORDER BY tenKho, id";
        try (var lease = connections.acquire(); var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, auth.facilityId());
            List<Warehouse> result = new ArrayList<>();
            try (var rs = ps.executeQuery()) {
                while (rs.next()) result.add(new Warehouse(rs.getInt(1), rs.getString(2)));
            }
            return List.copyOf(result);
        } catch (SQLException ex) {
            throw new IllegalStateException("Không tải được danh sách kho.", ex);
        }
    }

    @Override
    public List<Supplier> suppliers(PhanQuyenNguCanh auth) {
        if (!NhapNhaCungCap.permitted(auth)) return List.of();
        String sql = "SELECT id, tenNhaCungCap, maSoThue, soDienThoai, email, diaChi FROM NhaCungCap "
                + "WHERE isDeleted=0 AND trangThai='DANG_GIAO_DICH' ORDER BY tenNhaCungCap, id";
        try (var lease = connections.acquire(); var ps = lease.connection().prepareStatement(sql);
             var rs = ps.executeQuery()) {
            List<Supplier> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new Supplier(
                        rs.getInt("id"),
                        text(rs, "tenNhaCungCap"),
                        text(rs, "maSoThue"),
                        text(rs, "soDienThoai"),
                        text(rs, "email"),
                        text(rs, "diaChi")));
            }
            return List.copyOf(result);
        } catch (SQLException ex) {
            throw new IllegalStateException("Không tải được danh sách nhà cung cấp.", ex);
        }
    }

    @Override
    public KetQua<Supplier> saveSupplier(Supplier supplier, PhanQuyenNguCanh auth) {
        if (!NhapNhaCungCap.permitted(auth)) {
            return KetQua.failure("FORBIDDEN", "Tài khoản chưa có quyền quản lý nhà cung cấp.");
        }
        if (supplier == null || supplier.name() == null || supplier.name().isBlank()) {
            return KetQua.failure("VALIDATION_ERROR", "Vui lòng nhập tên nhà cung cấp.");
        }
        String name = supplier.name().trim();
        if (name.length() > 255) {
            return KetQua.failure("VALIDATION_ERROR", "Tên nhà cung cấp tối đa 255 ký tự.");
        }
        return transactions.inTransaction(() -> {
            try (var lease = connections.acquire()) {
                Connection c = lease.connection();
                if (supplier.id() <= 0) {
                    String sql = "INSERT INTO NhaCungCap (tenNhaCungCap, maSoThue, soDienThoai, email, diaChi, trangThai, createdBy) "
                            + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, 'DANG_GIAO_DICH', ?)";
                    try (var ps = c.prepareStatement(sql)) {
                        ps.setString(1, name);
                        ps.setString(2, trimTo(supplier.taxCode(), 50));
                        ps.setString(3, trimTo(supplier.phone(), 20));
                        ps.setString(4, trimTo(supplier.email(), 100));
                        ps.setString(5, trimTo(supplier.address(), 500));
                        ps.setInt(6, auth.employeeId());
                        try (var rs = ps.executeQuery()) {
                            if (rs.next()) {
                                return KetQua.success(new Supplier(rs.getInt(1), name,
                                        trimTo(supplier.taxCode(), 50), trimTo(supplier.phone(), 20),
                                        trimTo(supplier.email(), 100), trimTo(supplier.address(), 500)));
                            }
                        }
                    }
                } else {
                    String sql = "UPDATE NhaCungCap SET tenNhaCungCap=?, maSoThue=?, soDienThoai=?, email=?, diaChi=?, "
                            + "updatedAt=SYSDATETIME(), updatedBy=?, version=version+1 WHERE id=? AND isDeleted=0";
                    try (var ps = c.prepareStatement(sql)) {
                        ps.setString(1, name);
                        ps.setString(2, trimTo(supplier.taxCode(), 50));
                        ps.setString(3, trimTo(supplier.phone(), 20));
                        ps.setString(4, trimTo(supplier.email(), 100));
                        ps.setString(5, trimTo(supplier.address(), 500));
                        ps.setInt(6, auth.employeeId());
                        ps.setInt(7, supplier.id());
                        if (ps.executeUpdate() != 1) {
                            return KetQua.failure("NOT_FOUND", "Không tìm thấy nhà cung cấp để cập nhật.");
                        }
                        return KetQua.success(new Supplier(supplier.id(), name,
                                trimTo(supplier.taxCode(), 50), trimTo(supplier.phone(), 20),
                                trimTo(supplier.email(), 100), trimTo(supplier.address(), 500)));
                    }
                }
                return KetQua.failure("SAVE_FAILED", "Không lưu được nhà cung cấp.");
            } catch (SQLException ex) {
                throw new IllegalStateException("Không lưu được nhà cung cấp.", ex);
            }
        });
    }

    @Override
    public List<DrugOption> drugs(PhanQuyenNguCanh auth) {
        if (!NhapNhaCungCap.permitted(auth)) return List.of();
        String sql = "SELECT id, maItem, tenItem, donViCoBan, nongDo, hamLuong, nuocSanXuat, hangSanXuat, "
                + "soDangKy, nhomThuoc, giaNhapMacDinh, giaBanMacDinh "
                + "FROM Item WHERE isDeleted=0 AND hoatDong=1 AND loaiItem='THUOC' ORDER BY tenItem, maItem";
        try (var lease = connections.acquire(); var ps = lease.connection().prepareStatement(sql);
             var rs = ps.executeQuery()) {
            List<DrugOption> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new DrugOption(
                        rs.getInt("id"),
                        text(rs, "maItem"),
                        text(rs, "tenItem"),
                        text(rs, "donViCoBan"),
                        text(rs, "nongDo"),
                        text(rs, "hamLuong"),
                        text(rs, "nuocSanXuat"),
                        text(rs, "hangSanXuat"),
                        text(rs, "soDangKy"),
                        text(rs, "nhomThuoc"),
                        money(rs, "giaNhapMacDinh"),
                        money(rs, "giaBanMacDinh")));
            }
            return List.copyOf(result);
        } catch (SQLException ex) {
            throw new IllegalStateException("Không tải được danh mục thuốc.", ex);
        }
    }

    @Override
    public KetQua<String> save(DraftReceipt receipt, PhanQuyenNguCanh auth) {
        if (!NhapNhaCungCap.permitted(auth)) {
            return KetQua.failure("FORBIDDEN", "Tài khoản chưa có quyền nhập kho nhà cung cấp.");
        }
        if (receipt == null) {
            return KetQua.failure("VALIDATION_ERROR", "Dữ liệu phiếu nhập không hợp lệ.");
        }
        try {
            receipt.validate();
        } catch (IllegalArgumentException ex) {
            return KetQua.failure("VALIDATION_ERROR", ex.getMessage());
        }

        String receiptCode = receipt.number() != null && !receipt.number().isBlank()
                ? receipt.number().trim()
                : "PNK-" + LocalDateTime.now().format(CODE_TIME);
        String status = receipt.postStock() ? "DA_NHAP" : "TAM";
        String ghiChu = buildNotes(receipt);

        return transactions.inTransaction(() -> {
            try (var lease = connections.acquire()) {
                Connection c = lease.connection();
                int receiptId = insertHeader(c, receiptCode, receipt, status, ghiChu, auth);
                if (receiptId == 0) {
                    return KetQua.failure("INVALID_TARGET", "Kho thuốc không hoạt động tại cơ sở hoặc nhà cung cấp không hợp lệ.");
                }

                String detailSql = "INSERT INTO ChiTietNhapKho (maPhieuNhap, maItem, soLo, hanSuDung, soLuong, giaNhap, giaBan, createdBy) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                String findLotSql = "SELECT TOP 1 id, soLuongTon FROM TonKho WITH (UPDLOCK, HOLDLOCK) "
                        + "WHERE maKho=? AND maItem=? AND soLo=? AND isDeleted=0 ORDER BY id";
                String updateLotSql = "UPDATE TonKho SET soLuongTon=?, giaNhap=?, giaBan=?, hanSuDung=?, "
                        + "updatedAt=SYSDATETIME(), updatedBy=?, version=version+1 WHERE id=?";
                String insertLotSql = "INSERT INTO TonKho (maKho, maItem, soLo, hanSuDung, soLuongTon, giaNhap, giaBan, createdBy) "
                        + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                String txSql = "INSERT INTO InventoryTransaction (maKho, maItem, maTonKho, loaiGiaoDich, soLuongThayDoi, "
                        + "tonTruocKhiThayDoi, tonSauKhiThayDoi, maChungTu, createdBy) VALUES (?, ?, ?, 'NHAP_KHO', ?, ?, ?, ?, ?)";

                try (var dps = c.prepareStatement(detailSql);
                     var fps = receipt.postStock() ? c.prepareStatement(findLotSql) : null;
                     var ups = receipt.postStock() ? c.prepareStatement(updateLotSql) : null;
                     var lps = receipt.postStock() ? c.prepareStatement(insertLotSql) : null;
                     var tps = receipt.postStock() ? c.prepareStatement(txSql) : null) {
                    for (ReceiptLine line : receipt.lines()) {
                        String batchTrim = line.batch().trim();
                        BigDecimal realCost = line.realUnitCost();
                        dps.setInt(1, receiptId);
                        dps.setInt(2, line.itemId());
                        dps.setString(3, batchTrim);
                        dps.setDate(4, java.sql.Date.valueOf(line.expiry()));
                        dps.setBigDecimal(5, line.quantity());
                        dps.setBigDecimal(6, realCost);
                        dps.setBigDecimal(7, line.sellPrice());
                        dps.setInt(8, auth.employeeId());
                        dps.executeUpdate();

                        if (receipt.postStock()) {
                            int tonKhoId = 0;
                            BigDecimal beforeQty = BigDecimal.ZERO;
                            fps.setInt(1, receipt.warehouseId());
                            fps.setInt(2, line.itemId());
                            fps.setString(3, batchTrim);
                            try (var frs = fps.executeQuery()) {
                                if (frs.next()) {
                                    tonKhoId = frs.getInt(1);
                                    BigDecimal existing = frs.getBigDecimal(2);
                                    beforeQty = existing != null ? existing : BigDecimal.ZERO;
                                }
                            }
                            BigDecimal afterQty = beforeQty.add(line.quantity());
                            if (tonKhoId > 0) {
                                ups.setBigDecimal(1, afterQty);
                                ups.setBigDecimal(2, realCost);
                                ups.setBigDecimal(3, line.sellPrice());
                                ups.setDate(4, java.sql.Date.valueOf(line.expiry()));
                                ups.setInt(5, auth.employeeId());
                                ups.setInt(6, tonKhoId);
                                ups.executeUpdate();
                            } else {
                                lps.setInt(1, receipt.warehouseId());
                                lps.setInt(2, line.itemId());
                                lps.setString(3, batchTrim);
                                lps.setDate(4, java.sql.Date.valueOf(line.expiry()));
                                lps.setBigDecimal(5, afterQty);
                                lps.setBigDecimal(6, realCost);
                                lps.setBigDecimal(7, line.sellPrice());
                                lps.setInt(8, auth.employeeId());
                                try (var lrs = lps.executeQuery()) {
                                    if (lrs.next()) tonKhoId = lrs.getInt(1);
                                }
                            }
                            if (tonKhoId > 0) {
                                tps.setInt(1, receipt.warehouseId());
                                tps.setInt(2, line.itemId());
                                tps.setInt(3, tonKhoId);
                                tps.setBigDecimal(4, line.quantity());
                                tps.setBigDecimal(5, beforeQty);
                                tps.setBigDecimal(6, afterQty);
                                tps.setString(7, receiptCode);
                                tps.setInt(8, auth.employeeId());
                                tps.executeUpdate();
                            }
                        }
                    }
                }

                if (receipt.postStock()) {
                    audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(
                            auth.employeeId(),
                            auth.roleId(),
                            auth.roleCode(),
                            auth.facilityId(),
                            auth.departmentId(),
                            KiemToanHanhDongMa.INV_RECEIPT_CREATED,
                            KiemToanTaiNguyenLoai.INVENTORY_RECEIPT,
                            receiptId,
                            KiemToanKetQua.SUCCESS,
                            null,
                            Instant.now(),
                            null,
                            auth.sessionId() != null ? auth.sessionId() : UUID.randomUUID()));
                }
                return KetQua.success(receiptCode);
            } catch (SQLException ex) {
                if (ex.getErrorCode() == 2627 || ex.getErrorCode() == 2601) {
                    return KetQua.failure("DUPLICATE_CODE", "Mã phiếu nhập " + receiptCode + " đã tồn tại. Vui lòng đổi mã phiếu.");
                }
                throw new IllegalStateException("Không thể lưu phiếu nhập kho.", ex);
            }
        });
    }

    @Override
    public List<SupplierPayment> payments(String receiptCode, PhanQuyenNguCanh auth) {
        if (!NhapNhaCungCap.permitted(auth) || receiptCode == null || receiptCode.isBlank()) {
            return List.of();
        }
        String sql = "SELECT t.id, t.soPhieuChi, t.loaiPhieu, t.ngayThanhToan, t.soTien, t.ghiChu, t.trangThai "
                + "FROM ThanhToanPhieuNhap t "
                + "JOIN PhieuNhapKho p ON p.id = t.maPhieuNhap "
                + "JOIN Kho k ON k.id = p.maKho "
                + "WHERE p.soPhieuNhap = ? AND k.maCoSo = ? AND p.isDeleted = 0 AND t.isDeleted = 0 "
                + "ORDER BY t.id DESC";
        try (var lease = connections.acquire(); var ps = lease.connection().prepareStatement(sql)) {
            ps.setString(1, receiptCode.trim());
            ps.setInt(2, auth.facilityId());
            List<SupplierPayment> result = new ArrayList<>();
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("ngayThanhToan");
                    result.add(new SupplierPayment(
                            rs.getInt("id"),
                            text(rs, "soPhieuChi"),
                            text(rs, "loaiPhieu"),
                            ts != null ? ts.toLocalDateTime() : LocalDateTime.now(),
                            money(rs, "soTien"),
                            text(rs, "ghiChu"),
                            text(rs, "trangThai")));
                }
            }
            return List.copyOf(result);
        } catch (SQLException ex) {
            throw new IllegalStateException("Không tải được lịch sử thanh toán phiếu nhập.", ex);
        }
    }

    @Override
    public KetQua<SupplierPayment> addPayment(String receiptCode, String paymentType, BigDecimal amount,
                                              String note, PhanQuyenNguCanh auth) {
        if (!NhapNhaCungCap.permitted(auth)) {
            return KetQua.failure("FORBIDDEN", "Tài khoản chưa có quyền thanh toán hóa đơn nhập kho.");
        }
        if (receiptCode == null || receiptCode.isBlank()) {
            return KetQua.failure("VALIDATION_ERROR", "Vui lòng Lưu Tạm hoặc Nhập Kho trước khi thanh toán.");
        }
        if (amount == null || amount.signum() <= 0) {
            return KetQua.failure("VALIDATION_ERROR", "Số tiền thanh toán phải lớn hơn 0.");
        }
        String type = paymentType != null && !paymentType.isBlank() ? trimTo(paymentType, 50) : "Tiền mặt";
        String cleanNote = trimTo(note, 500);
        String code = "PCNCC-" + LocalDateTime.now().format(CODE_TIME);

        return transactions.inTransaction(() -> {
            try (var lease = connections.acquire()) {
                Connection c = lease.connection();
                int receiptId;
                BigDecimal totalBill;
                String findSql = "SELECT p.id, p.tongTien FROM PhieuNhapKho p WITH (UPDLOCK, HOLDLOCK) "
                        + "JOIN Kho k ON k.id = p.maKho "
                        + "WHERE p.soPhieuNhap = ? AND k.maCoSo = ? AND p.isDeleted = 0";
                try (var ps = c.prepareStatement(findSql)) {
                    ps.setString(1, receiptCode.trim());
                    ps.setInt(2, auth.facilityId());
                    try (var rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            return KetQua.failure("NOT_FOUND", "Không tìm thấy phiếu nhập " + receiptCode.trim() + ".");
                        }
                        receiptId = rs.getInt(1);
                        totalBill = rs.getBigDecimal(2) != null ? rs.getBigDecimal(2) : BigDecimal.ZERO;
                    }
                }

                BigDecimal alreadyPaid = BigDecimal.ZERO;
                String sumSql = "SELECT ISNULL(SUM(soTien), 0) FROM ThanhToanPhieuNhap "
                        + "WHERE maPhieuNhap = ? AND isDeleted = 0 AND trangThai = 'HOAT_DONG'";
                try (var ps = c.prepareStatement(sumSql)) {
                    ps.setInt(1, receiptId);
                    try (var rs = ps.executeQuery()) {
                        if (rs.next() && rs.getBigDecimal(1) != null) {
                            alreadyPaid = rs.getBigDecimal(1);
                        }
                    }
                }
                BigDecimal remaining = totalBill.subtract(alreadyPaid);
                if (amount.compareTo(remaining) > 0) {
                    return KetQua.failure("OVERPAYMENT",
                            "Số tiền thanh toán vượt quá công nợ còn lại (" + remaining.stripTrailingZeros().toPlainString() + "đ).");
                }

                String insertSql = "INSERT INTO ThanhToanPhieuNhap (maPhieuNhap, soPhieuChi, loaiPhieu, soTien, ghiChu, createdBy) "
                        + "OUTPUT INSERTED.id, INSERTED.ngayThanhToan VALUES (?, ?, ?, ?, ?, ?)";
                try (var ps = c.prepareStatement(insertSql)) {
                    ps.setInt(1, receiptId);
                    ps.setString(2, code);
                    ps.setString(3, type);
                    ps.setBigDecimal(4, amount);
                    ps.setString(5, cleanNote);
                    ps.setInt(6, auth.employeeId());
                    try (var rs = ps.executeQuery()) {
                        if (rs.next()) {
                            Timestamp ts = rs.getTimestamp(2);
                            return KetQua.success(new SupplierPayment(
                                    rs.getInt(1), code, type,
                                    ts != null ? ts.toLocalDateTime() : LocalDateTime.now(),
                                    amount, cleanNote, "HOAT_DONG"));
                        }
                    }
                }
                return KetQua.failure("SAVE_FAILED", "Không lưu được phiếu thanh toán.");
            } catch (SQLException ex) {
                throw new IllegalStateException("Không thể lưu thanh toán hóa đơn nhập kho.", ex);
            }
        });
    }

    @Override
    public KetQua<Void> cancelPayment(int paymentId, PhanQuyenNguCanh auth) {
        if (!NhapNhaCungCap.permitted(auth)) {
            return KetQua.failure("FORBIDDEN", "Tài khoản chưa có quyền hủy phiếu thanh toán.");
        }
        if (paymentId <= 0) {
            return KetQua.failure("VALIDATION_ERROR", "Vui lòng chọn phiếu thanh toán cần hủy.");
        }
        return transactions.inTransaction(() -> {
            try (var lease = connections.acquire()) {
                String sql = "UPDATE t SET t.trangThai = 'DA_HUY', t.updatedAt = SYSDATETIME(), t.updatedBy = ? "
                        + "FROM ThanhToanPhieuNhap t "
                        + "JOIN PhieuNhapKho p ON p.id = t.maPhieuNhap "
                        + "JOIN Kho k ON k.id = p.maKho "
                        + "WHERE t.id = ? AND k.maCoSo = ? AND t.isDeleted = 0 AND t.trangThai = 'HOAT_DONG'";
                try (var ps = lease.connection().prepareStatement(sql)) {
                    ps.setInt(1, auth.employeeId());
                    ps.setInt(2, paymentId);
                    ps.setInt(3, auth.facilityId());
                    if (ps.executeUpdate() != 1) {
                        return KetQua.failure("NOT_FOUND", "Không tìm thấy phiếu thanh toán đang hoạt động để hủy.");
                    }
                }
                return KetQua.success(null);
            } catch (SQLException ex) {
                throw new IllegalStateException("Không thể hủy phiếu thanh toán.", ex);
            }
        });
    }

    private static int insertHeader(Connection c, String code, DraftReceipt r, String status, String ghiChu, PhanQuyenNguCanh auth)
            throws SQLException {
        try (var chkKho = c.prepareStatement(
                "SELECT 1 FROM Kho WHERE id=? AND maCoSo=? AND isDeleted=0 AND trangThai='HOAT_DONG'")) {
            chkKho.setInt(1, r.warehouseId());
            chkKho.setInt(2, auth.facilityId());
            try (var rs = chkKho.executeQuery()) {
                if (!rs.next()) return 0;
            }
        }
        try (var chkSup = c.prepareStatement(
                "SELECT 1 FROM NhaCungCap WHERE id=? AND isDeleted=0")) {
            chkSup.setInt(1, r.supplierId());
            try (var rs = chkSup.executeQuery()) {
                if (!rs.next()) return 0;
            }
        }
        String sql = "INSERT INTO PhieuNhapKho (soPhieuNhap, maKho, maNhaCungCap, ngayNhap, tongTien, trangThai, ghiChu, createdBy) "
                + "OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (var ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setInt(2, r.warehouseId());
            ps.setInt(3, r.supplierId());
            ps.setTimestamp(4, Timestamp.valueOf(r.docDate().atTime(LocalTime.now())));
            ps.setBigDecimal(5, r.total());
            ps.setString(6, status);
            ps.setString(7, ghiChu);
            ps.setInt(8, auth.employeeId());
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static String buildNotes(DraftReceipt r) {
        List<String> parts = new ArrayList<>();
        if (r.invoiceNumber() != null && !r.invoiceNumber().isBlank()) parts.add("Số HĐ: " + r.invoiceNumber().trim());
        if (r.invoiceDate() != null) parts.add("Ngày HĐ: " + r.invoiceDate());
        if (r.deliverer() != null && !r.deliverer().isBlank()) parts.add("Người giao: " + r.deliverer().trim());
        if (r.note() != null && !r.note().isBlank()) parts.add(r.note().trim());
        String joined = String.join(" | ", parts);
        return joined.length() > 500 ? joined.substring(0, 500) : joined;
    }

    private static String text(ResultSet rs, String col) throws SQLException {
        return Objects.toString(rs.getString(col), "");
    }

    private static BigDecimal money(ResultSet rs, String col) throws SQLException {
        BigDecimal v = rs.getBigDecimal(col);
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String trimTo(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
