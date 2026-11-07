package vn.clinicmanagement.application.reports;

import vn.clinicmanagement.domain.inventory.Item;
import vn.clinicmanagement.domain.inventory.TonKho;

import java.util.List;

/**
 * Cổng giao tiếp nguồn dữ liệu tồn kho dược phẩm (Application Port).
 */
public interface IInventoryReportDataSource {
    List<TonKho> getBatches(Integer warehouseId);
    Item getItemById(int itemId);
    List<Item> getAllItems();
}
