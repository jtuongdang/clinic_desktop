package vn.clinicmanagement.application.inventory.retail;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

public interface XuatLe {
    record Warehouse(int id, String name) {
        @Override public String toString() { return name; }
    }

    record Lot(int id, int itemId, String code, String name, String unit, String strength,
               String packing, String country, String maker, String registration, String batch,
               LocalDate expiry, BigDecimal stock, BigDecimal cost, BigDecimal price,
               BigDecimal vat, String barcode, String directions) {}

    record Line(Lot lot, BigDecimal quantity, BigDecimal vat, BigDecimal discount,
                String directions, String diagnosis) {
        public BigDecimal amount() {
            return quantity.multiply(lot.price()).multiply(BigDecimal.valueOf(100).add(vat))
                    .multiply(BigDecimal.valueOf(100).subtract(discount)).movePointLeft(4)
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    record Customer(String code, String name, String age, String gender, String address,
                    String phone, String email, String note, String doctor) {}

    record Ticket(UUID key, int warehouseId, LocalDate date, Customer customer, List<Line> lines) {
        public Ticket { lines = List.copyOf(lines); }
        public String number() { return "XL-" + key; }
        public BigDecimal total() { return lines.stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add); }

        public void validate() {
            if (date == null || date.isAfter(LocalDate.now()) || lines.isEmpty() || warehouseId <= 0 || key == null)
                throw new IllegalArgumentException("Kiểm tra kho, ngày lập và danh sách thuốc.");
            if (!customer.age().isBlank() && !customer.age().matches("[0-9]{1,3}"))
                throw new IllegalArgumentException("Tuổi phải là số nguyên từ 0 đến 999.");
            if (customer.code().length() > 50 || customer.phone().length() > 50 || customer.gender().length() > 20
                    || customer.address().length() > 500 || customer.note().length() > 500
                    || customer.name().length() > 255 || customer.email().length() > 255 || customer.doctor().length() > 255)
                throw new IllegalArgumentException("Thông tin khách hàng vượt độ dài cho phép.");
            var batches = new HashSet<Integer>();
            for (Line line : lines) {
                var qty = line.quantity().stripTrailingZeros();
                if (!batches.add(line.lot().id()) || qty.signum() <= 0 || qty.scale() > 4 || qty.precision()-qty.scale() > 14)
                    throw new IllegalArgumentException("Kiểm tra lô thuốc và số lượng xuất (tối đa 4 chữ số thập phân).");
                for (BigDecimal rate : List.of(line.vat(), line.discount()))
                    if (rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0 || rate.stripTrailingZeros().scale() > 2)
                        throw new IllegalArgumentException("VAT và miễn giảm phải từ 0 đến 100%, tối đa 2 chữ số thập phân.");
                if (line.lot().price().signum() < 0 || line.directions().length() > 1000 || line.diagnosis().length() > 1000)
                    throw new IllegalArgumentException("Kiểm tra giá bán, hướng dẫn sử dụng và chẩn đoán.");
            }
            if (total().precision() > 18) throw new IllegalArgumentException("Tổng tiền vượt giới hạn của phiếu xuất.");
        }
    }

    static boolean permitted(PhanQuyenNguCanh auth) {
        return auth != null && auth.isValid() && Set.of("ADMIN", "PHARMACIST").contains(auth.roleCode());
    }

    List<Warehouse> warehouses(PhanQuyenNguCanh auth);
    List<Lot> lots(int warehouseId, PhanQuyenNguCanh auth);
    KetQua<String> save(Ticket ticket, PhanQuyenNguCanh auth);
}
