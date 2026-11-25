package vn.clinicmanagement.application.inventory;

import java.time.LocalDateTime;
import java.util.List;

/** Cổng đọc dữ liệu cho 13 mẫu báo cáo phân hệ Nhà Thuốc. */
public interface NhaThuocBaoCaoReadPort {

    enum LoaiBaoCao {
        XUAT_NHAP_TON("Báo Cáo Xuất Nhập Tồn"),
        DOANH_THU_NHA_THUOC("Báo Cáo Doanh Thu Nhà Thuốc"),
        LOI_NHUAN_NHA_THUOC("Báo Cáo Lợi Nhuận Nhà Thuốc"),
        PHIEU_HOAN_TRA_THUOC("Phiếu Hoàn Trả Thuốc"),
        NHAP_TRA_LE("Báo Cáo Nhập Trả Lẻ"),
        PHIEU_NHAP_NCC("Báo Cáo Phiếu Nhập Nhà Cung Cấp"),
        THUOC_NHAP_NCC("Báo Cáo Thuốc Nhập Nhà Cung Cấp"),
        KIEM_NHAP_THUOC_VAT_TU_NCC("Báo Cáo Kiểm Nhập Thuốc Vật Tư (Ncc)"),
        BIEN_BAN_KIEM_NHAP_TU_NCC("Biên Bản Kiểm Nhập Thuốc Từ Nhà Cung Cấp"),
        XUAT_THUOC_THEO_PHIEU("Báo Cáo Xuất Thuốc Theo Phiếu"),
        MUA_HANG_NHAP_KHO("Báo Cáo Mua Hàng Nhập Kho"),
        XUAT_BAN_HANG("Báo Cáo Xuất Bán Hàng"),
        THU_HOI_KHA_DUNG("Báo Cáo Thu Hồi Khả Dụng");

        private final String title;

        LoaiBaoCao(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    record BaoCaoKetQua(
            String title,
            List<String> columns,
            List<List<Object>> rows,
            String summaryText
    ) {
        public BaoCaoKetQua {
            columns = columns != null ? List.copyOf(columns) : List.of();
            rows = rows != null ? rows.stream().map(List::copyOf).toList() : List.of();
            summaryText = summaryText != null ? summaryText : "";
        }
    }

    record XuatNhapTonOptions(
            String groupBy,
            String sortBy,
            boolean mergeSameCode,
            boolean mergeSameBatchPriceExpiry,
            boolean includeLockedItems,
            String printFilter,
            boolean onlyCurrentUser,
            Integer currentUserId,
            List<Integer> warehouseIds,
            String supplierFilter,
            String actorFilter,
            String sourceFilter
    ) {
        public XuatNhapTonOptions {
            groupBy = groupBy != null ? groupBy : "NONE";
            sortBy = sortBy != null ? sortBy : "";
            printFilter = printFilter != null ? printFilter : "ALL";
            warehouseIds = warehouseIds != null ? List.copyOf(warehouseIds) : List.of();
            supplierFilter = supplierFilter != null ? supplierFilter.trim() : "";
            actorFilter = actorFilter != null ? actorFilter.trim() : "";
            sourceFilter = sourceFilter != null ? sourceFilter.trim() : "";
        }

        public static XuatNhapTonOptions defaults() {
            return new XuatNhapTonOptions("NONE", "", false, false, false, "ALL", false, null, List.of(), "", "", "");
        }
    }

    record FilterItem(int id, String name) {}

    record XuatNhapTonPrintRow(
            String groupName,
            String itemCode,
            String itemName,
            String strength,
            String activeIngredient,
            String unit,
            String expiryDate,
            java.math.BigDecimal importPrice,
            java.math.BigDecimal exportPrice,
            java.math.BigDecimal openingQty,
            java.math.BigDecimal openingAmount,
            java.math.BigDecimal importQty,
            java.math.BigDecimal importAmount,
            java.math.BigDecimal exportQty,
            java.math.BigDecimal exportAmount,
            java.math.BigDecimal closingQty,
            java.math.BigDecimal closingAmount
    ) {}

    record XuatNhapTonPrintData(String warehouseName, List<XuatNhapTonPrintRow> rows) {
        public XuatNhapTonPrintData {
            warehouseName = warehouseName != null && !warehouseName.isBlank() ? warehouseName : "Kho Nhà Thuốc";
            rows = rows != null ? List.copyOf(rows) : List.of();
        }
    }

    record FunctionalFoodItem(
            int id,
            String code,
            String name,
            String unit,
            String packing,
            java.math.BigDecimal defaultPrice,
            java.math.BigDecimal stock
    ) {}

    record FunctionalFoodConsultLine(
            String itemCode,
            String itemName,
            String unit,
            java.math.BigDecimal qty,
            java.math.BigDecimal price,
            java.math.BigDecimal amount,
            String dosage,
            String frequency,
            String instruction
    ) {}

    record FunctionalFoodConsultData(
            int visitId,
            String visitCode,
            String patientCode,
            String patientName,
            String gender,
            String doctorName,
            LocalDateTime prescribedAt,
            String advice,
            List<FunctionalFoodConsultLine> lines
    ) {
        public FunctionalFoodConsultData {
            lines = lines != null ? List.copyOf(lines) : List.of();
        }
    }

    BaoCaoKetQua getReport(LoaiBaoCao type, int facilityId, Integer warehouseId, LocalDateTime from, LocalDateTime to);

    default BaoCaoKetQua getXuatNhapTonReport(
            int facilityId,
            Integer warehouseId,
            LocalDateTime from,
            LocalDateTime to,
            XuatNhapTonOptions options
    ) {
        return getReport(LoaiBaoCao.XUAT_NHAP_TON, facilityId, warehouseId, from, to);
    }

    default XuatNhapTonPrintData getXuatNhapTonPrintData(
            int facilityId,
            Integer warehouseId,
            LocalDateTime from,
            LocalDateTime to,
            XuatNhapTonOptions options
    ) {
        return new XuatNhapTonPrintData("Kho Nhà Thuốc", List.of());
    }

    default List<FilterItem> getWarehouses(int facilityId) {
        return List.of();
    }

    default List<FilterItem> getSuppliers(int facilityId) {
        return List.of();
    }

    default List<FilterItem> getActors(int facilityId) {
        return List.of();
    }

    default List<FunctionalFoodItem> listFunctionalFoodItems(int facilityId) {
        return List.of();
    }

    default FunctionalFoodConsultData getFunctionalFoodConsultation(int visitId, int facilityId) {
        return null;
    }

    default BaoCaoKetQua getFunctionalFoodReport(int facilityId, LocalDateTime from, LocalDateTime to) {
        return new BaoCaoKetQua("Báo Cáo Thực Phẩm Chức Năng", List.of(), List.of(), "");
    }
}
