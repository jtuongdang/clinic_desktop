package vn.clinicmanagement.infrastructure.persistence.inventory;

import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Triển khai truy vấn CSDL cho 13 mẫu báo cáo phân hệ Nhà Thuốc. */
public final class NhaThuocBaoCaoRepository implements NhaThuocBaoCaoReadPort {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final JdbcKetNoiNhaCungCap connections;

    public NhaThuocBaoCaoRepository(JdbcKetNoiNhaCungCap connections) {
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    @Override
    public BaoCaoKetQua getReport(LoaiBaoCao type, int facilityId, Integer warehouseId, LocalDateTime from, LocalDateTime to) {
        Objects.requireNonNull(type, "type");
        return switch (type) {
            case XUAT_NHAP_TON -> loadXuatNhapTon(type, facilityId, warehouseId, from, to);
            case DOANH_THU_NHA_THUOC -> loadDoanhThuNhaThuoc(type, facilityId, warehouseId, from, to);
            case LOI_NHUAN_NHA_THUOC -> loadLoiNhuanNhaThuoc(type, facilityId, warehouseId, from, to);
            case PHIEU_HOAN_TRA_THUOC, NHAP_TRA_LE -> loadHoanTra(type, facilityId, warehouseId, from, to);
            case PHIEU_NHAP_NCC, MUA_HANG_NHAP_KHO -> loadPhieuNhapNcc(type, facilityId, warehouseId, from, to);
            case THUOC_NHAP_NCC, KIEM_NHAP_THUOC_VAT_TU_NCC, BIEN_BAN_KIEM_NHAP_TU_NCC ->
                    loadChiTietNhapNcc(type, facilityId, warehouseId, from, to);
            case XUAT_THUOC_THEO_PHIEU -> loadXuatTheoPhieu(type, facilityId, warehouseId, from, to);
            case XUAT_BAN_HANG -> loadXuatBanHang(type, facilityId, warehouseId, from, to);
            case THU_HOI_KHA_DUNG -> loadThuHoiKhaDung(type, facilityId, warehouseId);
        };
    }

    @Override
    public BaoCaoKetQua getXuatNhapTonReport(
            int facilityId,
            Integer warehouseId,
            LocalDateTime from,
            LocalDateTime to,
            XuatNhapTonOptions options
    ) {
        XuatNhapTonOptions opts = options != null ? options : XuatNhapTonOptions.defaults();
        List<Integer> whIds = !opts.warehouseIds().isEmpty()
                ? opts.warehouseIds()
                : (warehouseId != null ? List.of(warehouseId) : List.of());

        String whFilter = whIds.isEmpty()
                ? ""
                : " AND k.id IN (" + String.join(",", whIds.stream().map(id -> "?").toList()) + ") ";
        String actorFilterSql = opts.onlyCurrentUser() && opts.currentUserId() != null
                ? " AND p.createdBy = ? "
                : (!opts.actorFilter().isBlank() ? " AND LTRIM(RTRIM(CONCAT(ISNULL(nv.ho, N''), N' ', ISNULL(nv.ten, N'')))) LIKE ? " : "");
        String supplierFilterSql = !opts.supplierFilter().isBlank()
                ? " AND ISNULL(ncc.tenNhaCungCap, N'') LIKE ? "
                : "";
        String sourceFilterSql = !opts.sourceFilter().isBlank()
                ? " AND ISNULL(p.trangThai, N'') LIKE ? "
                : "";
        String itemActiveSql = opts.includeLockedItems() ? "" : " AND i.hoatDong = 1 ";

        String groupExpr = switch (opts.groupBy()) {
            case "ITEM_TYPE" -> "ISNULL(i.loaiItem, N'')";
            case "PHARMACY_GROUP" -> "ISNULL(i.nhomThuoc, N'')";
            case "WAREHOUSE" -> "ISNULL(ton.tenKho, N'')";
            case "SUPPLIER" -> "ISNULL(nhap.nccName, N'')";
            case "IMPORTER" -> "ISNULL(nhap.actorName, N'')";
            case "EXPORTER" -> "ISNULL(xuat.actorName, N'')";
            default -> "N''";
        };
        String groupColLabel = switch (opts.groupBy()) {
            case "ITEM_TYPE" -> "Loại thuốc, vật tư";
            case "PHARMACY_GROUP" -> "Nhóm dược lý";
            case "WAREHOUSE" -> "Phòng lưu / Kho";
            case "SUPPLIER" -> "Nhà cung cấp";
            case "IMPORTER" -> "Người nhập";
            case "EXPORTER" -> "Người xuất";
            default -> "Nhóm dữ liệu";
        };

        String havingSql = switch (opts.printFilter()) {
            case "WITH_MOVEMENT" -> "(ISNULL(nhap.slNhap, 0) > 0 OR ISNULL(xuat.slXuat, 0) > 0)";
            case "STOCK_GT_ZERO" -> "ISNULL(ton.slTon, 0) > 0";
            case "STOCK_ZERO" -> "ISNULL(ton.slTon, 0) <= 0";
            default -> "(ISNULL(nhap.slNhap, 0) > 0 OR ISNULL(xuat.slXuat, 0) > 0 OR ISNULL(ton.slTon, 0) > 0)";
        };

        String orderSql = switch (opts.sortBy()) {
            case "CODE" -> "ORDER BY i.maItem ASC";
            case "STOCK_VALUE_DESC" -> "ORDER BY giaTriTon DESC, i.tenItem ASC";
            case "STOCK_QTY_DESC" -> "ORDER BY slTon DESC, i.tenItem ASC";
            default -> "ORDER BY nhomDuLieu ASC, i.tenItem ASC";
        };

        String sql = """
                SELECT %s AS nhomDuLieu,
                       i.maItem, i.tenItem, i.donViCoBan,
                       ISNULL(ton.soLo, N'') soLo, ton.hanSuDung, ISNULL(ton.giaNhap, 0) donGia,
                       ISNULL(nhap.slNhap, 0) slNhap,
                       ISNULL(xuat.slXuat, 0) slXuat,
                       ISNULL(ton.slTon, 0) slTon,
                       ISNULL(ton.giaTriTon, 0) giaTriTon
                FROM Item i
                LEFT JOIN (
                    SELECT c.maItem,
                           SUM(c.soLuong) slNhap,
                           MAX(ISNULL(ncc.tenNhaCungCap, N'')) nccName,
                           MAX(LTRIM(RTRIM(CONCAT(ISNULL(nv.ho, N''), N' ', ISNULL(nv.ten, N''))))) actorName
                    FROM ChiTietNhapKho c
                    JOIN PhieuNhapKho p ON p.id = c.maPhieuNhap
                    JOIN Kho k ON k.id = p.maKho
                    LEFT JOIN NhaCungCap ncc ON ncc.id = p.maNhaCungCap
                    LEFT JOIN NhanVien nv ON nv.id = p.createdBy
                    WHERE k.maCoSo = ? AND k.isDeleted = 0 AND p.isDeleted = 0 AND c.isDeleted = 0
                      AND p.ngayNhap >= ? AND p.ngayNhap < ? %s %s %s %s
                    GROUP BY c.maItem
                ) nhap ON nhap.maItem = i.id
                LEFT JOIN (
                    SELECT c.maItem,
                           SUM(c.soLuong) slXuat,
                           MAX(LTRIM(RTRIM(CONCAT(ISNULL(nv.ho, N''), N' ', ISNULL(nv.ten, N''))))) actorName
                    FROM ChiTietXuatKho c
                    JOIN PhieuXuatKho p ON p.id = c.maPhieuXuat
                    JOIN Kho k ON k.id = p.maKhoXuat
                    LEFT JOIN NhanVien nv ON nv.id = p.createdBy
                    WHERE k.maCoSo = ? AND k.isDeleted = 0 AND p.isDeleted = 0 AND c.isDeleted = 0
                      AND p.ngayXuat >= ? AND p.ngayXuat < ? %s %s
                    GROUP BY c.maItem
                ) xuat ON xuat.maItem = i.id
                LEFT JOIN (
                    SELECT t.maItem,
                           %s,
                           SUM(t.soLuongTon) slTon,
                           SUM(t.soLuongTon * ISNULL(t.giaNhap, 0)) giaTriTon
                    FROM TonKho t
                    JOIN Kho k ON k.id = t.maKho
                    WHERE k.maCoSo = ? AND k.isDeleted = 0 AND t.isDeleted = 0 %s
                    GROUP BY t.maItem %s
                ) ton ON ton.maItem = i.id
                WHERE i.isDeleted = 0 AND i.loaiItem IN ('THUOC', 'VAT_TU') %s
                  AND %s
                %s
                """.formatted(
                groupExpr,
                whFilter, actorFilterSql, supplierFilterSql, sourceFilterSql,
                whFilter, actorFilterSql,
                opts.mergeSameBatchPriceExpiry()
                        ? "MAX(k.tenKho) tenKho, t.soLo, t.hanSuDung, ISNULL(t.giaNhap, 0) giaNhap"
                        : "MAX(k.tenKho) tenKho, MAX(t.soLo) soLo, MIN(t.hanSuDung) hanSuDung, AVG(ISNULL(t.giaNhap, 0)) giaNhap",
                whFilter,
                opts.mergeSameBatchPriceExpiry() ? ", t.soLo, t.hanSuDung, ISNULL(t.giaNhap, 0)" : "",
                itemActiveSql,
                havingSql,
                orderSql
        );

        boolean showGroup = !"NONE".equals(opts.groupBy());
        boolean showBatch = opts.mergeSameBatchPriceExpiry() || !opts.mergeSameCode();
        List<String> cols = new ArrayList<>();
        if (showGroup) cols.add(groupColLabel);
        cols.add("Mã thuốc");
        cols.add("Tên thuốc / Vật tư");
        cols.add("ĐVT");
        if (showBatch) {
            cols.add("Số lô");
            cols.add("Hạn dùng");
            cols.add("Đơn giá");
        }
        cols.add("Nhập kỳ");
        cols.add("Xuất kỳ");
        cols.add("Tồn hiện tại");
        cols.add("Giá trị tồn");
        int sumColIdx = cols.size() - 1;

        return runQuery(LoaiBaoCao.XUAT_NHAP_TON.title(), List.copyOf(cols), sql, ps -> {
            Timestamp f = Timestamp.valueOf(from);
            Timestamp t = Timestamp.valueOf(to);
            int idx = 1;
            // nhap subquery
            ps.setInt(idx++, facilityId);
            ps.setTimestamp(idx++, f);
            ps.setTimestamp(idx++, t);
            for (int whId : whIds) ps.setInt(idx++, whId);
            if (opts.onlyCurrentUser() && opts.currentUserId() != null) ps.setInt(idx++, opts.currentUserId());
            else if (!opts.actorFilter().isBlank()) ps.setString(idx++, "%" + opts.actorFilter() + "%");
            if (!opts.supplierFilter().isBlank()) ps.setString(idx++, "%" + opts.supplierFilter() + "%");
            if (!opts.sourceFilter().isBlank()) ps.setString(idx++, "%" + opts.sourceFilter() + "%");
            // xuat subquery
            ps.setInt(idx++, facilityId);
            ps.setTimestamp(idx++, f);
            ps.setTimestamp(idx++, t);
            for (int whId : whIds) ps.setInt(idx++, whId);
            if (opts.onlyCurrentUser() && opts.currentUserId() != null) ps.setInt(idx++, opts.currentUserId());
            else if (!opts.actorFilter().isBlank()) ps.setString(idx++, "%" + opts.actorFilter() + "%");
            // ton subquery
            ps.setInt(idx++, facilityId);
            for (int whId : whIds) ps.setInt(idx++, whId);
        }, rs -> {
            List<Object> row = new ArrayList<>();
            if (showGroup) row.add(text(rs, "nhomDuLieu"));
            row.add(text(rs, "maItem"));
            row.add(text(rs, "tenItem"));
            row.add(text(rs, "donViCoBan"));
            if (showBatch) {
                row.add(text(rs, "soLo"));
                row.add(formatDate(rs, "hanSuDung"));
                row.add(money(rs, "donGia"));
            }
            row.add(money(rs, "slNhap"));
            row.add(money(rs, "slXuat"));
            row.add(money(rs, "slTon"));
            row.add(money(rs, "giaTriTon"));
            return row;
        }, sumColIdx, "Tổng giá trị tồn");
    }

    @Override
    public XuatNhapTonPrintData getXuatNhapTonPrintData(
            int facilityId,
            Integer warehouseId,
            LocalDateTime from,
            LocalDateTime to,
            XuatNhapTonOptions options
    ) {
        XuatNhapTonOptions opts = options != null ? options : XuatNhapTonOptions.defaults();
        List<Integer> whIds = !opts.warehouseIds().isEmpty()
                ? opts.warehouseIds()
                : (warehouseId != null ? List.of(warehouseId) : List.of());

        String whName = resolveWarehouseName(facilityId, whIds);
        String whFilter = whIds.isEmpty()
                ? ""
                : " AND k.id IN (" + String.join(",", whIds.stream().map(id -> "?").toList()) + ") ";
        String actorFilterSql = opts.onlyCurrentUser() && opts.currentUserId() != null
                ? " AND p.createdBy = ? "
                : (!opts.actorFilter().isBlank() ? " AND LTRIM(RTRIM(CONCAT(ISNULL(nv.ho, N''), N' ', ISNULL(nv.ten, N'')))) LIKE ? " : "");
        String supplierFilterSql = !opts.supplierFilter().isBlank()
                ? " AND ISNULL(ncc.tenNhaCungCap, N'') LIKE ? "
                : "";
        String sourceFilterSql = !opts.sourceFilter().isBlank()
                ? " AND ISNULL(p.trangThai, N'') LIKE ? "
                : "";
        String itemActiveSql = opts.includeLockedItems() ? "" : " AND i.hoatDong = 1 ";

        String groupExpr = switch (opts.groupBy()) {
            case "ITEM_TYPE" -> "ISNULL(NULLIF(i.loaiItem, N''), N'(Khác)')";
            case "PHARMACY_GROUP" -> "ISNULL(NULLIF(i.nhomThuoc, N''), N'(Chưa phân nhóm)')";
            case "WAREHOUSE" -> "ISNULL(NULLIF(ton.tenKho, N''), N'(Chưa xác định kho)')";
            case "SUPPLIER" -> "ISNULL(NULLIF(nhap.nccName, N''), N'(Không có NCC)')";
            case "IMPORTER" -> "ISNULL(NULLIF(nhap.actorName, N''), N'(Chưa xác định)')";
            case "EXPORTER" -> "ISNULL(NULLIF(xuat.actorName, N''), N'(Chưa xác định)')";
            default -> "N'(Không nhóm dữ liệu)'";
        };

        String havingSql = switch (opts.printFilter()) {
            case "WITH_MOVEMENT" -> "(ISNULL(nhap.slNhap, 0) > 0 OR ISNULL(xuat.slXuat, 0) > 0)";
            case "STOCK_GT_ZERO" -> "ISNULL(ton.slTon, 0) > 0";
            case "STOCK_ZERO" -> "ISNULL(ton.slTon, 0) <= 0";
            default -> "(ISNULL(nhap.slNhap, 0) > 0 OR ISNULL(xuat.slXuat, 0) > 0 OR ISNULL(ton.slTon, 0) > 0)";
        };

        String orderSql = switch (opts.sortBy()) {
            case "CODE" -> "ORDER BY nhomDuLieu ASC, i.maItem ASC";
            case "STOCK_VALUE_DESC" -> "ORDER BY nhomDuLieu ASC, giaTriTon DESC, i.tenItem ASC";
            case "STOCK_QTY_DESC" -> "ORDER BY nhomDuLieu ASC, slTon DESC, i.tenItem ASC";
            default -> "ORDER BY nhomDuLieu ASC, i.tenItem ASC";
        };

        String sql = """
                SELECT %s AS nhomDuLieu,
                       i.maItem, i.tenItem, ISNULL(i.hamLuong, N'') hamLuong, ISNULL(i.hoatChat, N'') hoatChat,
                       i.donViCoBan, ton.hanSuDung,
                       ISNULL(ton.giaNhap, 0) giaNhap, ISNULL(ton.giaBan, 0) giaXuất,
                       ISNULL(nhap.slNhap, 0) slNhap, ISNULL(nhap.ttNhap, 0) ttNhap,
                       ISNULL(xuat.slXuat, 0) slXuat, ISNULL(xuat.ttXuat, 0) ttXuat,
                       ISNULL(ton.slTon, 0) slTon, ISNULL(ton.giaTriTon, 0) giaTriTon
                FROM Item i
                LEFT JOIN (
                    SELECT c.maItem,
                           SUM(c.soLuong) slNhap,
                           SUM(c.soLuong * ISNULL(c.giaNhap, 0)) ttNhap,
                           MAX(ISNULL(ncc.tenNhaCungCap, N'')) nccName,
                           MAX(LTRIM(RTRIM(CONCAT(ISNULL(nv.ho, N''), N' ', ISNULL(nv.ten, N''))))) actorName
                    FROM ChiTietNhapKho c
                    JOIN PhieuNhapKho p ON p.id = c.maPhieuNhap
                    JOIN Kho k ON k.id = p.maKho
                    LEFT JOIN NhaCungCap ncc ON ncc.id = p.maNhaCungCap
                    LEFT JOIN NhanVien nv ON nv.id = p.createdBy
                    WHERE k.maCoSo = ? AND k.isDeleted = 0 AND p.isDeleted = 0 AND c.isDeleted = 0
                      AND p.ngayNhap >= ? AND p.ngayNhap < ? %s %s %s %s
                    GROUP BY c.maItem
                ) nhap ON nhap.maItem = i.id
                LEFT JOIN (
                    SELECT c.maItem,
                           SUM(c.soLuong) slXuat,
                           SUM(ISNULL(c.thanhTienBanLe, c.soLuong * ISNULL(c.donGia, 0))) ttXuat,
                           MAX(LTRIM(RTRIM(CONCAT(ISNULL(nv.ho, N''), N' ', ISNULL(nv.ten, N''))))) actorName
                    FROM ChiTietXuatKho c
                    JOIN PhieuXuatKho p ON p.id = c.maPhieuXuat
                    JOIN Kho k ON k.id = p.maKhoXuat
                    LEFT JOIN NhanVien nv ON nv.id = p.createdBy
                    WHERE k.maCoSo = ? AND k.isDeleted = 0 AND p.isDeleted = 0 AND c.isDeleted = 0
                      AND p.ngayXuat >= ? AND p.ngayXuat < ? %s %s
                    GROUP BY c.maItem
                ) xuat ON xuat.maItem = i.id
                LEFT JOIN (
                    SELECT t.maItem,
                           %s,
                           SUM(t.soLuongTon) slTon,
                           SUM(t.soLuongTon * ISNULL(t.giaNhap, 0)) giaTriTon
                    FROM TonKho t
                    JOIN Kho k ON k.id = t.maKho
                    WHERE k.maCoSo = ? AND k.isDeleted = 0 AND t.isDeleted = 0 %s
                    GROUP BY t.maItem %s
                ) ton ON ton.maItem = i.id
                WHERE i.isDeleted = 0 AND i.loaiItem IN ('THUOC', 'VAT_TU') %s
                  AND %s
                %s
                """.formatted(
                groupExpr,
                whFilter, actorFilterSql, supplierFilterSql, sourceFilterSql,
                whFilter, actorFilterSql,
                opts.mergeSameBatchPriceExpiry()
                        ? "MAX(k.tenKho) tenKho, t.hanSuDung, ISNULL(t.giaNhap, 0) giaNhap, MAX(ISNULL(t.giaBan, 0)) giaBan"
                        : "MAX(k.tenKho) tenKho, MIN(t.hanSuDung) hanSuDung, AVG(ISNULL(t.giaNhap, 0)) giaNhap, AVG(ISNULL(t.giaBan, 0)) giaBan",
                whFilter,
                opts.mergeSameBatchPriceExpiry() ? ", t.soLo, t.hanSuDung, ISNULL(t.giaNhap, 0)" : "",
                itemActiveSql,
                havingSql,
                orderSql
        ).replace("giaXuất", "giaXuat");

        List<XuatNhapTonPrintRow> rows = new ArrayList<>();
        DateTimeFormatter hsdFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            Timestamp f = Timestamp.valueOf(from);
            Timestamp t = Timestamp.valueOf(to);
            int idx = 1;
            ps.setInt(idx++, facilityId);
            ps.setTimestamp(idx++, f);
            ps.setTimestamp(idx++, t);
            for (int whId : whIds) ps.setInt(idx++, whId);
            if (opts.onlyCurrentUser() && opts.currentUserId() != null) ps.setInt(idx++, opts.currentUserId());
            else if (!opts.actorFilter().isBlank()) ps.setString(idx++, "%" + opts.actorFilter() + "%");
            if (!opts.supplierFilter().isBlank()) ps.setString(idx++, "%" + opts.supplierFilter() + "%");
            if (!opts.sourceFilter().isBlank()) ps.setString(idx++, "%" + opts.sourceFilter() + "%");

            ps.setInt(idx++, facilityId);
            ps.setTimestamp(idx++, f);
            ps.setTimestamp(idx++, t);
            for (int whId : whIds) ps.setInt(idx++, whId);
            if (opts.onlyCurrentUser() && opts.currentUserId() != null) ps.setInt(idx++, opts.currentUserId());
            else if (!opts.actorFilter().isBlank()) ps.setString(idx++, "%" + opts.actorFilter() + "%");

            ps.setInt(idx++, facilityId);
            for (int whId : whIds) ps.setInt(idx++, whId);

            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal impPrice = money(rs, "giaNhap");
                    BigDecimal expPrice = money(rs, "giaXuat");
                    BigDecimal impQty = money(rs, "slNhap");
                    BigDecimal impAmt = money(rs, "ttNhap");
                    BigDecimal expQty = money(rs, "slXuat");
                    BigDecimal expAmt = money(rs, "ttXuat");
                    BigDecimal closeQty = money(rs, "slTon");
                    BigDecimal closeAmt = money(rs, "giaTriTon");
                    BigDecimal openQty = closeQty.subtract(impQty).add(expQty).max(BigDecimal.ZERO);
                    BigDecimal openAmt = openQty.multiply(impPrice);
                    var hsd = rs.getDate("hanSuDung");
                    rows.add(new XuatNhapTonPrintRow(
                            text(rs, "nhomDuLieu"),
                            text(rs, "maItem"),
                            text(rs, "tenItem"),
                            text(rs, "hamLuong"),
                            text(rs, "hoatChat"),
                            text(rs, "donViCoBan"),
                            hsd == null ? "" : hsd.toLocalDate().format(hsdFmt),
                            impPrice,
                            expPrice,
                            openQty,
                            openAmt,
                            impQty,
                            impAmt,
                            expQty,
                            expAmt,
                            closeQty,
                            closeAmt
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải dữ liệu in báo cáo xuất nhập tồn.", ex);
        }
        return new XuatNhapTonPrintData(whName, rows);
    }

    private String resolveWarehouseName(int facilityId, List<Integer> whIds) {
        if (whIds.size() == 1) {
            try (var lease = connections.acquire();
                 var ps = lease.connection().prepareStatement("SELECT tenKho FROM Kho WHERE id = ? AND maCoSo = ? AND isDeleted = 0")) {
                ps.setInt(1, whIds.get(0));
                ps.setInt(2, facilityId);
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getString("tenKho");
                }
            } catch (SQLException ignored) {}
        }
        return whIds.size() > 1 ? "Tổng hợp nhiều kho" : "Kho Nhà Thuốc";
    }

    @Override
    public List<FilterItem> getWarehouses(int facilityId) {
        return loadFilterItems("SELECT id, tenKho AS name FROM Kho WHERE maCoSo = ? AND isDeleted = 0 ORDER BY tenKho", facilityId);
    }

    @Override
    public List<FilterItem> getSuppliers(int facilityId) {
        return loadFilterItems("SELECT id, tenNhaCungCap AS name FROM NhaCungCap WHERE isDeleted = 0 AND (maCoSo = ? OR maCoSo IS NULL) ORDER BY tenNhaCungCap", facilityId);
    }

    @Override
    public List<FilterItem> getActors(int facilityId) {
        return loadFilterItems("SELECT id, LTRIM(RTRIM(CONCAT(ISNULL(ho, N''), N' ', ISNULL(ten, N'')))) AS name FROM NhanVien WHERE isDeleted = 0 AND maCoSo = ? ORDER BY ten, ho", facilityId);
    }

    private List<FilterItem> loadFilterItems(String sql, int facilityId) {
        List<FilterItem> items = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, facilityId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    if (name != null && !name.isBlank()) {
                        items.add(new FilterItem(rs.getInt("id"), name));
                    }
                }
            }
        } catch (SQLException ex) {
            return List.of();
        }
        return List.copyOf(items);
    }

    private BaoCaoKetQua loadXuatNhapTon(LoaiBaoCao type, int facilityId, Integer warehouseId, LocalDateTime from, LocalDateTime to) {
        String sql = """
                SELECT i.maItem, i.tenItem, i.donViCoBan,
                       ISNULL(nhap.slNhap, 0) slNhap,
                       ISNULL(xuat.slXuat, 0) slXuat,
                       ISNULL(ton.slTon, 0) slTon,
                       ISNULL(ton.giaTriTon, 0) giaTriTon
                FROM Item i
                LEFT JOIN (
                    SELECT c.maItem, SUM(c.soLuong) slNhap
                    FROM ChiTietNhapKho c
                    JOIN PhieuNhapKho p ON p.id = c.maPhieuNhap
                    JOIN Kho k ON k.id = p.maKho
                    WHERE k.maCoSo = ? AND k.isDeleted = 0 AND p.isDeleted = 0 AND c.isDeleted = 0
                      AND (? IS NULL OR k.id = ?) AND p.ngayNhap >= ? AND p.ngayNhap < ?
                    GROUP BY c.maItem
                ) nhap ON nhap.maItem = i.id
                LEFT JOIN (
                    SELECT c.maItem, SUM(c.soLuong) slXuat
                    FROM ChiTietXuatKho c
                    JOIN PhieuXuatKho p ON p.id = c.maPhieuXuat
                    JOIN Kho k ON k.id = p.maKhoXuat
                    WHERE k.maCoSo = ? AND k.isDeleted = 0 AND p.isDeleted = 0 AND c.isDeleted = 0
                      AND (? IS NULL OR k.id = ?) AND p.ngayXuat >= ? AND p.ngayXuat < ?
                    GROUP BY c.maItem
                ) xuat ON xuat.maItem = i.id
                LEFT JOIN (
                    SELECT t.maItem, SUM(t.soLuongTon) slTon, SUM(t.soLuongTon * ISNULL(t.giaNhap, 0)) giaTriTon
                    FROM TonKho t
                    JOIN Kho k ON k.id = t.maKho
                    WHERE k.maCoSo = ? AND k.isDeleted = 0 AND t.isDeleted = 0 AND (? IS NULL OR k.id = ?)
                    GROUP BY t.maItem
                ) ton ON ton.maItem = i.id
                WHERE i.isDeleted = 0 AND i.loaiItem = 'THUOC'
                  AND (ISNULL(nhap.slNhap, 0) > 0 OR ISNULL(xuat.slXuat, 0) > 0 OR ISNULL(ton.slTon, 0) > 0)
                ORDER BY i.tenItem
                """;
        List<String> cols = List.of("Mã thuốc", "Tên thuốc", "ĐVT", "Nhập kỳ", "Xuất kỳ", "Tồn hiện tại", "Giá trị tồn");
        return runQuery(type.title(), cols, sql, ps -> {
            Timestamp f = Timestamp.valueOf(from);
            Timestamp t = Timestamp.valueOf(to);
            int idx = 1;
            ps.setInt(idx++, facilityId);
            ps.setObject(idx++, warehouseId, Types.INTEGER);
            ps.setObject(idx++, warehouseId, Types.INTEGER);
            ps.setTimestamp(idx++, f);
            ps.setTimestamp(idx++, t);
            ps.setInt(idx++, facilityId);
            ps.setObject(idx++, warehouseId, Types.INTEGER);
            ps.setObject(idx++, warehouseId, Types.INTEGER);
            ps.setTimestamp(idx++, f);
            ps.setTimestamp(idx++, t);
            ps.setInt(idx++, facilityId);
            ps.setObject(idx++, warehouseId, Types.INTEGER);
            ps.setObject(idx, warehouseId, Types.INTEGER);
        }, rs -> List.of(
                text(rs, "maItem"), text(rs, "tenItem"), text(rs, "donViCoBan"),
                money(rs, "slNhap"), money(rs, "slXuat"), money(rs, "slTon"), money(rs, "giaTriTon")
        ), 6, "Tổng giá trị tồn");
    }

    private BaoCaoKetQua loadDoanhThuNhaThuoc(LoaiBaoCao type, int facilityId, Integer warehouseId, LocalDateTime from, LocalDateTime to) {
        String sql = """
                SELECT p.soPhieuXuat, p.ngayXuat, k.tenKho, p.loaiXuat, ISNULL(p.tongTien, 0) tongTien
                FROM PhieuXuatKho p
                JOIN Kho k ON k.id = p.maKhoXuat
                WHERE k.maCoSo = ? AND k.isDeleted = 0 AND p.isDeleted = 0
                  AND p.loaiXuat IN ('BAN_THUOC', 'XUAT_LE')
                  AND (? IS NULL OR k.id = ?) AND p.ngayXuat >= ? AND p.ngayXuat < ?
                ORDER BY p.ngayXuat DESC, p.id DESC
                """;
        List<String> cols = List.of("Số phiếu", "Ngày xuất", "Kho thuốc", "Loại xuất", "Tổng tiền");
        return runDateRangeQuery(type.title(), cols, sql, facilityId, warehouseId, from, to,
                rs -> List.of(text(rs, "soPhieuXuat"), formatTs(rs, "ngayXuat"), text(rs, "tenKho"),
                        text(rs, "loaiXuat"), money(rs, "tongTien")), 4, "Tổng doanh thu");
    }

    private BaoCaoKetQua loadLoiNhuanNhaThuoc(LoaiBaoCao type, int facilityId, Integer warehouseId, LocalDateTime from, LocalDateTime to) {
        String sql = """
                SELECT i.maItem, i.tenItem, i.donViCoBan,
                       SUM(c.soLuong) slXuat,
                       SUM(ISNULL(c.thanhTienBanLe, c.soLuong * ISNULL(c.donGia, 0))) doanhThu,
                       SUM(c.soLuong * ISNULL(t.giaNhap, 0)) giaVon,
                       SUM(ISNULL(c.thanhTienBanLe, c.soLuong * ISNULL(c.donGia, 0)) - c.soLuong * ISNULL(t.giaNhap, 0)) loiNhuan
                FROM ChiTietXuatKho c
                JOIN PhieuXuatKho p ON p.id = c.maPhieuXuat
                JOIN Kho k ON k.id = p.maKhoXuat
                JOIN Item i ON i.id = c.maItem
                LEFT JOIN TonKho t ON t.id = c.maTonKho
                WHERE k.maCoSo = ? AND k.isDeleted = 0 AND p.isDeleted = 0 AND c.isDeleted = 0
                  AND p.loaiXuat IN ('BAN_THUOC', 'XUAT_LE')
                  AND (? IS NULL OR k.id = ?) AND p.ngayXuat >= ? AND p.ngayXuat < ?
                GROUP BY i.maItem, i.tenItem, i.donViCoBan
                ORDER BY loiNhuan DESC, i.tenItem
                """;
        List<String> cols = List.of("Mã thuốc", "Tên thuốc", "ĐVT", "SL xuất", "Doanh thu", "Giá vốn", "Lợi nhuận");
        return runDateRangeQuery(type.title(), cols, sql, facilityId, warehouseId, from, to,
                rs -> List.of(text(rs, "maItem"), text(rs, "tenItem"), text(rs, "donViCoBan"),
                        money(rs, "slXuat"), money(rs, "doanhThu"), money(rs, "giaVon"), money(rs, "loiNhuan")),
                6, "Tổng lợi nhuận");
    }

    private BaoCaoKetQua loadHoanTra(LoaiBaoCao type, int facilityId, Integer warehouseId, LocalDateTime from, LocalDateTime to) {
        String sql = type == LoaiBaoCao.PHIEU_HOAN_TRA_THUOC
                ? """
                  SELECT p.soPhieuXuat code, p.ngayXuat dt, k.tenKho, p.loaiXuat loai, p.trangThaiNoiBo st, ISNULL(p.tongTien, 0) tongTien
                  FROM PhieuXuatKho p
                  JOIN Kho k ON k.id = p.maKhoXuat
                  WHERE k.maCoSo = ? AND k.isDeleted = 0 AND p.isDeleted = 0
                    AND p.loaiXuat IN ('TRA_NCC', 'HOAN_TRA', 'XUAT_TRA')
                    AND (? IS NULL OR k.id = ?) AND p.ngayXuat >= ? AND p.ngayXuat < ?
                  ORDER BY p.ngayXuat DESC, p.id DESC
                  """
                : """
                  SELECT p.soPhieuNhap code, p.ngayNhap dt, k.tenKho, N'Nhập trả lẻ' loai, p.trangThai st, ISNULL(p.tongTien, 0) tongTien
                  FROM PhieuNhapKho p
                  JOIN Kho k ON k.id = p.maKho
                  WHERE k.maCoSo = ? AND k.isDeleted = 0 AND p.isDeleted = 0
                    AND p.trangThai IN ('NHAP_TRA', 'HOAN_TRA', 'TRA_LE')
                    AND (? IS NULL OR k.id = ?) AND p.ngayNhap >= ? AND p.ngayNhap < ?
                  ORDER BY p.ngayNhap DESC, p.id DESC
                  """;
        List<String> cols = List.of("Mã phiếu", "Ngày lập", "Kho thuốc", "Loại phiếu", "Trạng thái", "Tổng tiền");
        return runDateRangeQuery(type.title(), cols, sql, facilityId, warehouseId, from, to,
                rs -> List.of(text(rs, "code"), formatTs(rs, "dt"), text(rs, "tenKho"),
                        text(rs, "loai"), text(rs, "st"), money(rs, "tongTien")), 5, "Tổng tiền");
    }

    private BaoCaoKetQua loadPhieuNhapNcc(LoaiBaoCao type, int facilityId, Integer warehouseId, LocalDateTime from, LocalDateTime to) {
        String sql = """
                SELECT p.soPhieuNhap, p.ngayNhap, k.tenKho, ISNULL(ncc.tenNhaCungCap, N'') nccName,
                       p.trangThai, ISNULL(p.tongTien, 0) tongTien
                FROM PhieuNhapKho p
                JOIN Kho k ON k.id = p.maKho
                LEFT JOIN NhaCungCap ncc ON ncc.id = p.maNhaCungCap
                WHERE k.maCoSo = ? AND k.isDeleted = 0 AND p.isDeleted = 0
                  AND (? IS NULL OR k.id = ?) AND p.ngayNhap >= ? AND p.ngayNhap < ?
                ORDER BY p.ngayNhap DESC, p.id DESC
                """;
        List<String> cols = List.of("Số phiếu nhập", "Ngày nhập", "Kho nhập", "Nhà cung cấp", "Trạng thái", "Tổng tiền");
        return runDateRangeQuery(type.title(), cols, sql, facilityId, warehouseId, from, to,
                rs -> List.of(text(rs, "soPhieuNhap"), formatTs(rs, "ngayNhap"), text(rs, "tenKho"),
                        text(rs, "nccName"), text(rs, "trangThai"), money(rs, "tongTien")), 5, "Tổng tiền nhập");
    }

    private BaoCaoKetQua loadChiTietNhapNcc(LoaiBaoCao type, int facilityId, Integer warehouseId, LocalDateTime from, LocalDateTime to) {
        String sql = """
                SELECT p.soPhieuNhap, p.ngayNhap, ISNULL(ncc.tenNhaCungCap, N'') nccName,
                       i.maItem, i.tenItem, i.donViCoBan, c.soLo, c.hanSuDung,
                       c.soLuong, c.giaNhap, (c.soLuong * c.giaNhap) thanhTien
                FROM ChiTietNhapKho c
                JOIN PhieuNhapKho p ON p.id = c.maPhieuNhap
                JOIN Kho k ON k.id = p.maKho
                JOIN Item i ON i.id = c.maItem
                LEFT JOIN NhaCungCap ncc ON ncc.id = p.maNhaCungCap
                WHERE k.maCoSo = ? AND k.isDeleted = 0 AND p.isDeleted = 0 AND c.isDeleted = 0
                  AND (? IS NULL OR k.id = ?) AND p.ngayNhap >= ? AND p.ngayNhap < ?
                ORDER BY p.ngayNhap DESC, p.id DESC, c.id
                """;
        List<String> cols = List.of("Số phiếu", "Ngày nhập", "Nhà cung cấp", "Mã hàng", "Tên thuốc / Vật tư",
                "ĐVT", "Số lô", "Hạn dùng", "Số lượng", "Đơn giá", "Thành tiền");
        return runDateRangeQuery(type.title(), cols, sql, facilityId, warehouseId, from, to,
                rs -> List.of(text(rs, "soPhieuNhap"), formatTs(rs, "ngayNhap"), text(rs, "nccName"),
                        text(rs, "maItem"), text(rs, "tenItem"), text(rs, "donViCoBan"),
                        text(rs, "soLo"), formatDate(rs, "hanSuDung"),
                        money(rs, "soLuong"), money(rs, "giaNhap"), money(rs, "thanhTien")),
                10, "Tổng thành tiền");
    }

    private BaoCaoKetQua loadXuatTheoPhieu(LoaiBaoCao type, int facilityId, Integer warehouseId, LocalDateTime from, LocalDateTime to) {
        String sql = """
                SELECT p.soPhieuXuat, p.ngayXuat, k.tenKho, p.loaiXuat,
                       LTRIM(RTRIM(CONCAT(ISNULL(nv.ho, N''), N' ', ISNULL(nv.ten, N'')))) actorName,
                       p.trangThaiNoiBo, ISNULL(p.tongTien, 0) tongTien
                FROM PhieuXuatKho p
                JOIN Kho k ON k.id = p.maKhoXuat
                LEFT JOIN NhanVien nv ON nv.id = p.createdBy
                WHERE k.maCoSo = ? AND k.isDeleted = 0 AND p.isDeleted = 0
                  AND (? IS NULL OR k.id = ?) AND p.ngayXuat >= ? AND p.ngayXuat < ?
                ORDER BY p.ngayXuat DESC, p.id DESC
                """;
        List<String> cols = List.of("Số phiếu xuất", "Ngày xuất", "Kho xuất", "Loại xuất", "Người lập", "Trạng thái", "Tổng tiền");
        return runDateRangeQuery(type.title(), cols, sql, facilityId, warehouseId, from, to,
                rs -> List.of(text(rs, "soPhieuXuat"), formatTs(rs, "ngayXuat"), text(rs, "tenKho"),
                        text(rs, "loaiXuat"), text(rs, "actorName"), text(rs, "trangThaiNoiBo"),
                        money(rs, "tongTien")), 6, "Tổng tiền xuất");
    }

    private BaoCaoKetQua loadXuatBanHang(LoaiBaoCao type, int facilityId, Integer warehouseId, LocalDateTime from, LocalDateTime to) {
        String sql = """
                SELECT p.soPhieuXuat, p.ngayXuat, k.tenKho, i.maItem, i.tenItem, i.donViCoBan,
                       c.soLuong, c.donGia, ISNULL(c.thanhTienBanLe, c.soLuong * ISNULL(c.donGia, 0)) thanhTien
                FROM ChiTietXuatKho c
                JOIN PhieuXuatKho p ON p.id = c.maPhieuXuat
                JOIN Kho k ON k.id = p.maKhoXuat
                JOIN Item i ON i.id = c.maItem
                WHERE k.maCoSo = ? AND k.isDeleted = 0 AND p.isDeleted = 0 AND c.isDeleted = 0
                  AND p.loaiXuat IN ('BAN_THUOC', 'XUAT_LE')
                  AND (? IS NULL OR k.id = ?) AND p.ngayXuat >= ? AND p.ngayXuat < ?
                ORDER BY p.ngayXuat DESC, p.id DESC, c.id
                """;
        List<String> cols = List.of("Số phiếu", "Ngày xuất", "Kho xuất", "Mã thuốc", "Tên thuốc", "ĐVT", "Số lượng", "Đơn giá", "Thành tiền");
        return runDateRangeQuery(type.title(), cols, sql, facilityId, warehouseId, from, to,
                rs -> List.of(text(rs, "soPhieuXuat"), formatTs(rs, "ngayXuat"), text(rs, "tenKho"),
                        text(rs, "maItem"), text(rs, "tenItem"), text(rs, "donViCoBan"),
                        money(rs, "soLuong"), money(rs, "donGia"), money(rs, "thanhTien")),
                8, "Tổng thành tiền bán");
    }

    private BaoCaoKetQua loadThuHoiKhaDung(LoaiBaoCao type, int facilityId, Integer warehouseId) {
        String sql = """
                SELECT k.tenKho, i.maItem, i.tenItem, i.donViCoBan, t.soLo, t.hanSuDung,
                       t.soLuongTon, ISNULL(t.giaNhap, 0) giaNhap, (t.soLuongTon * ISNULL(t.giaNhap, 0)) giaTri
                FROM TonKho t
                JOIN Kho k ON k.id = t.maKho
                JOIN Item i ON i.id = t.maItem
                WHERE k.maCoSo = ? AND k.isDeleted = 0 AND t.isDeleted = 0 AND i.isDeleted = 0
                  AND (? IS NULL OR k.id = ?)
                  AND t.soLuongTon > 0
                  AND t.hanSuDung <= DATEADD(day, 30, CAST(GETDATE() AS DATE))
                ORDER BY t.hanSuDung ASC, i.tenItem
                """;
        List<String> cols = List.of("Kho thuốc", "Mã thuốc", "Tên thuốc", "ĐVT", "Số lô", "Hạn dùng", "Tồn khả dụng", "Đơn giá nhập", "Giá trị");
        return runQuery(type.title(), cols, sql, ps -> {
            ps.setInt(1, facilityId);
            ps.setObject(2, warehouseId, Types.INTEGER);
            ps.setObject(3, warehouseId, Types.INTEGER);
        }, rs -> List.of(text(rs, "tenKho"), text(rs, "maItem"), text(rs, "tenItem"), text(rs, "donViCoBan"),
                text(rs, "soLo"), formatDate(rs, "hanSuDung"),
                money(rs, "soLuongTon"), money(rs, "giaNhap"), money(rs, "giaTri")),
                8, "Tổng giá trị cần thu hồi / cận hạn");
    }

    private BaoCaoKetQua runDateRangeQuery(
            String title,
            List<String> columns,
            String sql,
            int facilityId,
            Integer warehouseId,
            LocalDateTime from,
            LocalDateTime to,
            RowMapper mapper,
            int sumColumnIndex,
            String sumLabel
    ) {
        return runQuery(title, columns, sql, ps -> {
            ps.setInt(1, facilityId);
            ps.setObject(2, warehouseId, Types.INTEGER);
            ps.setObject(3, warehouseId, Types.INTEGER);
            ps.setTimestamp(4, Timestamp.valueOf(from));
            ps.setTimestamp(5, Timestamp.valueOf(to));
        }, mapper, sumColumnIndex, sumLabel);
    }

    private BaoCaoKetQua runQuery(
            String title,
            List<String> columns,
            String sql,
            StatementBinder binder,
            RowMapper mapper,
            int sumColumnIndex,
            String sumLabel
    ) {
        List<List<Object>> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            binder.bind(ps);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    List<Object> row = mapper.map(rs);
                    rows.add(row);
                    if (sumColumnIndex >= 0 && sumColumnIndex < row.size() && row.get(sumColumnIndex) instanceof BigDecimal amount) {
                        total = total.add(amount);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải dữ liệu báo cáo: " + title, ex);
        }
        String formattedTotal = NumberFormat.getNumberInstance(Locale.US).format(total);
        String summary = "Số dòng: " + rows.size() + "   |   " + sumLabel + ": " + formattedTotal;
        return new BaoCaoKetQua(title, columns, rows, summary);
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    private interface RowMapper {
        List<Object> map(ResultSet rs) throws SQLException;
    }

    private static String text(ResultSet rs, String col) throws SQLException {
        String value = rs.getString(col);
        return value == null ? "" : value;
    }

    private static BigDecimal money(ResultSet rs, String col) throws SQLException {
        BigDecimal value = rs.getBigDecimal(col);
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String formatTs(ResultSet rs, String col) throws SQLException {
        Timestamp ts = rs.getTimestamp(col);
        return ts == null ? "" : ts.toLocalDateTime().format(DT_FMT);
    }

    @Override
    public List<FunctionalFoodItem> listFunctionalFoodItems(int facilityId) {
        String sql = """
                SELECT i.id, i.maItem, i.tenItem, ISNULL(i.donViCoBan, N'') donViCoBan,
                       ISNULL(i.quyCachDongGoi, N'') quyCachDongGoi,
                       ISNULL(i.giaBanMacDinh, 0) giaBanMacDinh,
                       ISNULL((SELECT SUM(t.soLuongTon)
                               FROM TonKho t
                               JOIN Kho k ON k.id = t.maKho
                               WHERE t.maItem = i.id AND k.maCoSo = ? AND k.isDeleted = 0
                                 AND t.isDeleted = 0 AND t.hanSuDung >= CAST(GETDATE() AS DATE)), 0) stock
                FROM Item i
                WHERE i.isDeleted = 0 AND i.hoatDong = 1
                  AND (i.nhomThuoc = N'Thực phẩm chức năng' OR i.thongTinThuoc LIKE N'%<entry key="thucPhamChucNang">true</entry>%')
                ORDER BY i.tenItem
                """;
        List<FunctionalFoodItem> items = new ArrayList<>();
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, facilityId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new FunctionalFoodItem(
                            rs.getInt("id"),
                            text(rs, "maItem"),
                            text(rs, "tenItem"),
                            text(rs, "donViCoBan"),
                            text(rs, "quyCachDongGoi"),
                            money(rs, "giaBanMacDinh"),
                            money(rs, "stock")
                    ));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải danh mục thực phẩm chức năng.", ex);
        }
        return List.copyOf(items);
    }

    @Override
    public FunctionalFoodConsultData getFunctionalFoodConsultation(int visitId, int facilityId) {
        String headerSql = """
                SELECT TOP 1 l.id, l.maLuotKham, b.maBenhNhan, b.ho + N' ' + b.ten AS patientName,
                       ISNULL(b.gioiTinh, N'') gioiTinh,
                       LTRIM(RTRIM(CONCAT(ISNULL(nv.ho, N''), N' ', ISNULL(nv.ten, N'')))) AS doctorName,
                       d.id AS donId, d.ngayKe, ISNULL(d.loiDanDonThuoc, N'') loiDan
                FROM DonThuoc d
                JOIN LuotKham l ON l.id = d.maLuotKham
                JOIN BenhNhan b ON b.id = l.maBenhNhan
                LEFT JOIN NhanVien nv ON nv.id = d.maBacSi
                WHERE d.maLuotKham = ? AND d.maCoSo = ? AND d.isDeleted = 0
                  AND (d.ghiChu = N'Tư vấn Thực phẩm chức năng'
                       OR EXISTS (SELECT 1 FROM ChiTietDonThuoc ct JOIN Item i ON i.id = ct.maItem
                                  WHERE ct.maDonThuoc = d.id AND ct.isDeleted = 0
                                    AND (i.nhomThuoc = N'Thực phẩm chức năng' OR i.thongTinThuoc LIKE N'%<entry key="thucPhamChucNang">true</entry>%')))
                ORDER BY d.id DESC
                """;
        try (var lease = connections.acquire();
             var ps = lease.connection().prepareStatement(headerSql)) {
            ps.setInt(1, visitId);
            ps.setInt(2, facilityId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                int donId = rs.getInt("donId");
                String lineSql = """
                        SELECT i.maItem, i.tenItem, ISNULL(i.donViCoBan, N'') donViCoBan,
                               ct.soLuongKe, ISNULL(ct.donGiaKe, 0) donGiaKe,
                               (ct.soLuongKe * ISNULL(ct.donGiaKe, 0)) thanhTien,
                               ISNULL(ct.lieuDung, N'') lieuDung,
                               ISNULL(ct.tanSuat, N'') tanSuat,
                               ISNULL(ct.huongDan, N'') huongDan
                        FROM ChiTietDonThuoc ct
                        JOIN Item i ON i.id = ct.maItem
                        WHERE ct.maDonThuoc = ? AND ct.isDeleted = 0
                          AND (i.nhomThuoc = N'Thực phẩm chức năng' OR i.thongTinThuoc LIKE N'%<entry key="thucPhamChucNang">true</entry>%')
                        ORDER BY ct.id
                        """;
                List<FunctionalFoodConsultLine> lines = new ArrayList<>();
                try (var lps = lease.connection().prepareStatement(lineSql)) {
                    lps.setInt(1, donId);
                    try (var lrs = lps.executeQuery()) {
                        while (lrs.next()) {
                            lines.add(new FunctionalFoodConsultLine(
                                    text(lrs, "maItem"),
                                    text(lrs, "tenItem"),
                                    text(lrs, "donViCoBan"),
                                    money(lrs, "soLuongKe"),
                                    money(lrs, "donGiaKe"),
                                    money(lrs, "thanhTien"),
                                    text(lrs, "lieuDung"),
                                    text(lrs, "tanSuat"),
                                    text(lrs, "huongDan")
                            ));
                        }
                    }
                }
                Timestamp ngayKe = rs.getTimestamp("ngayKe");
                return new FunctionalFoodConsultData(
                        rs.getInt("id"),
                        text(rs, "maLuotKham"),
                        text(rs, "maBenhNhan"),
                        text(rs, "patientName"),
                        text(rs, "gioiTinh"),
                        text(rs, "doctorName"),
                        ngayKe == null ? LocalDateTime.now() : ngayKe.toLocalDateTime(),
                        text(rs, "loiDan"),
                        lines
                );
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Không thể tải phiếu tư vấn thực phẩm chức năng.", ex);
        }
    }

    @Override
    public BaoCaoKetQua getFunctionalFoodReport(int facilityId, LocalDateTime from, LocalDateTime to) {
        String sql = """
                SELECT d.ngayKe, l.maLuotKham, b.ho + N' ' + b.ten AS patientName,
                       LTRIM(RTRIM(CONCAT(ISNULL(nv.ho, N''), N' ', ISNULL(nv.ten, N'')))) AS doctorName,
                       i.maItem, i.tenItem, ISNULL(i.donViCoBan, N'') donViCoBan,
                       ct.soLuongKe, ISNULL(ct.donGiaKe, 0) donGiaKe,
                       (ct.soLuongKe * ISNULL(ct.donGiaKe, 0)) thanhTien
                FROM ChiTietDonThuoc ct
                JOIN DonThuoc d ON d.id = ct.maDonThuoc
                JOIN Item i ON i.id = ct.maItem
                JOIN LuotKham l ON l.id = d.maLuotKham
                JOIN BenhNhan b ON b.id = l.maBenhNhan
                LEFT JOIN NhanVien nv ON nv.id = d.maBacSi
                WHERE d.maCoSo = ? AND d.isDeleted = 0 AND ct.isDeleted = 0
                  AND d.ngayKe >= ? AND d.ngayKe < ?
                  AND (i.nhomThuoc = N'Thực phẩm chức năng' OR i.thongTinThuoc LIKE N'%<entry key="thucPhamChucNang">true</entry>%')
                ORDER BY d.ngayKe DESC, ct.id DESC
                """;
        List<String> cols = List.of("Ngày kê", "Mã lượt khám", "Bệnh nhân", "Bác sĩ tư vấn",
                "Mã TPCN", "Tên thực phẩm chức năng", "ĐVT", "Số lượng", "Đơn giá", "Thành tiền");
        return runQuery("Báo Cáo Thống Kê Thực Phẩm Chức Năng", cols, sql, ps -> {
            ps.setInt(1, facilityId);
            ps.setTimestamp(2, Timestamp.valueOf(from));
            ps.setTimestamp(3, Timestamp.valueOf(to));
        }, rs -> List.of(
                formatTs(rs, "ngayKe"),
                text(rs, "maLuotKham"),
                text(rs, "patientName"),
                text(rs, "doctorName"),
                text(rs, "maItem"),
                text(rs, "tenItem"),
                text(rs, "donViCoBan"),
                money(rs, "soLuongKe"),
                money(rs, "donGiaKe"),
                money(rs, "thanhTien")
        ), 9, "Tổng giá trị tư vấn TPCN");
    }

    private static String formatDate(ResultSet rs, String col) throws SQLException {
        var date = rs.getDate(col);
        return date == null ? "" : date.toLocalDate().format(D_FMT);
    }
}
