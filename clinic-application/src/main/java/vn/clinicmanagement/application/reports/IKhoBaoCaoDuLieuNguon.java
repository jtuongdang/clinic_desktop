package vn.clinicmanagement.application.reports;

import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.inventory.TonKho;

import java.util.List;

/**
 * Cổng giao tiếp nguồn dữ liệu tồn kho dược phẩm (Application Port).
 */
public interface IKhoBaoCaoDuLieuNguon {
    List<TonKho> getBatches(Integer warehouseId);
    MatHang getItemById(int itemId);
    List<MatHang> getAllItems();
}
