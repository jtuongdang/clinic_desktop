package vn.clinicmanagement.application.inventory.receive;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Hợp đồng nhập thuốc từ nhà cung cấp (tải danh mục và lưu phiếu tạm / nhập kho). */
public interface NhapNhaCungCap {
    record Warehouse(int id, String name) {
        @Override public String toString() { return name; }
    }

    record Supplier(int id, String name, String taxCode, String phone, String email, String address) {
        @Override public String toString() { return name; }
    }

    record DrugOption(int id, String code, String name, String unit, String concentration,
                      String strength, String country, String maker, String registration,
                      String group, BigDecimal defaultCost, BigDecimal defaultPrice) {
        @Override public String toString() { return name; }
    }

    record ReceiptLine(int itemId, String itemCode, String itemName, String unit, String batch,
                       LocalDate expiry, BigDecimal quantity, BigDecimal unitCost,
                       BigDecimal vatPercent, BigDecimal discountPercent, BigDecimal sellPrice) {
        public BigDecimal realUnitCost() {
            BigDecimal base = unitCost != null ? unitCost : BigDecimal.ZERO;
            BigDecimal vat = vatPercent != null ? vatPercent : BigDecimal.ZERO;
            BigDecimal discount = discountPercent != null ? discountPercent : BigDecimal.ZERO;
            return base.multiply(BigDecimal.valueOf(100).add(vat))
                    .multiply(BigDecimal.valueOf(100).subtract(discount))
                    .movePointLeft(4)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        public BigDecimal amount() {
            BigDecimal qty = quantity != null ? quantity : BigDecimal.ZERO;
            return qty.multiply(realUnitCost()).setScale(2, RoundingMode.HALF_UP);
        }
    }

    record DraftReceipt(String number, int warehouseId, int supplierId, LocalDate docDate,
                        LocalDate invoiceDate, String invoiceNumber, String deliverer,
                        String note, List<ReceiptLine> lines, boolean postStock) {
        public DraftReceipt {
            lines = lines != null ? List.copyOf(lines) : List.of();
        }

        public BigDecimal total() {
            return lines.stream().map(ReceiptLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public void validate() {
            if (warehouseId <= 0 || supplierId <= 0 || docDate == null || lines.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn kho thuốc, nhà cung cấp, ngày lập và ít nhất 1 dòng thuốc.");
            }
            if (number != null && number.trim().length() > 50) {
                throw new IllegalArgumentException("Mã phiếu nhập tối đa 50 ký tự.");
            }
            for (ReceiptLine line : lines) {
                if (line.itemId() <= 0 || line.batch() == null || line.batch().isBlank() || line.batch().trim().length() > 50) {
                    throw new IllegalArgumentException("Mỗi dòng thuốc cần chọn thuốc và nhập số lô (tối đa 50 ký tự).");
                }
                if (line.expiry() == null) {
                    throw new IllegalArgumentException("Vui lòng nhập đầy đủ hạn sử dụng (dd/MM/yyyy).");
                }
                if (postStock && !line.expiry().isAfter(LocalDate.now())) {
                    throw new IllegalArgumentException("Không thể nhập kho lô " + line.batch() + " đã hết hạn hoặc hết hạn hôm nay.");
                }
                if (line.quantity() == null || line.quantity().signum() <= 0) {
                    throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0.");
                }
                if (line.unitCost() == null || line.unitCost().signum() < 0
                        || line.sellPrice() == null || line.sellPrice().signum() < 0) {
                    throw new IllegalArgumentException("Giá mua và giá bán không được âm.");
                }
                for (BigDecimal rate : List.of(line.vatPercent(), line.discountPercent())) {
                    if (rate == null || rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
                        throw new IllegalArgumentException("VAT và chiết khấu phải nằm trong khoảng 0% - 100%.");
                    }
                }
            }
        }
    }

    record SupplierPayment(int id, String paymentCode, String paymentType, LocalDateTime paidAt,
                           BigDecimal amount, String note, String status) {
        public boolean isActive() { return "HOAT_DONG".equals(status); }
    }

    static boolean permitted(PhanQuyenNguCanh auth) {
        return auth != null && auth.isValid()
                && Set.of("ADMIN", "PHARMACIST", "WAREHOUSE_MANAGER").contains(auth.roleCode());
    }

    List<Warehouse> warehouses(PhanQuyenNguCanh auth);
    List<Supplier> suppliers(PhanQuyenNguCanh auth);
    KetQua<Supplier> saveSupplier(Supplier supplier, PhanQuyenNguCanh auth);
    List<DrugOption> drugs(PhanQuyenNguCanh auth);
    KetQua<String> save(DraftReceipt receipt, PhanQuyenNguCanh auth);

    default List<SupplierPayment> payments(String receiptCode, PhanQuyenNguCanh auth) {
        return List.of();
    }

    default KetQua<SupplierPayment> addPayment(String receiptCode, String paymentType, BigDecimal amount,
                                               String note, PhanQuyenNguCanh auth) {
        return KetQua.failure("UNSUPPORTED", "Chức năng thanh toán hóa đơn chưa được kích hoạt.");
    }

    default KetQua<Void> cancelPayment(int paymentId, PhanQuyenNguCanh auth) {
        return KetQua.failure("UNSUPPORTED", "Chức năng hủy thanh toán chưa được kích hoạt.");
    }
}
