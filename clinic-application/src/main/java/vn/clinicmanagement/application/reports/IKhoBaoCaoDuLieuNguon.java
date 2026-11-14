package vn.clinicmanagement.application.reports;

import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.inventory.TonKho;

import java.util.List;

/**
 * Cổng giao tiếp nguồn dữ liệu tồn kho dược phẩm (Application Port).
 */
public interface IKhoBaoCaoDuLieuNguon {
    /** Hàm `getBatches`: lấy `Batches`. */
    List<TonKho> getBatches(Integer warehouseId);
    /** Hàm `getItemById`: lấy mathang theo mã. */
    MatHang getItemById(int itemId);
    /** Hàm `getAllItems`: lấy `All` `Items`. */
    List<MatHang> getAllItems();
}
