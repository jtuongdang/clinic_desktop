package vn.clinicmanagement.domain.repositories;

import java.util.Collections;
import java.util.List;

public class PhanTrangKetQua<T> {
    private final List<T> items;
    /** Trường `totalCount`: `total` dem. */
    private final int totalCount;
    private final int pageIndex;
    private final int pageSize;

    public PhanTrangKetQua(List<T> items, int totalCount, int pageIndex, int pageSize) {
        this.items = items != null ? items : Collections.emptyList();
        this.totalCount = totalCount;
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
    }

    /** Hàm `getItems`: lấy `Items`. */
    public List<T> getItems() { return items; }
    /** Hàm `getTotalCount`: lấy `Total` dem. */
    public int getTotalCount() { return totalCount; }
    /** Hàm `getPageIndex`: lấy `Page` `Index`. */
    public int getPageIndex() { return pageIndex; }
    /** Hàm `getPageSize`: lấy `Page` `Size`. */
    public int getPageSize() { return pageSize; }
    /** Hàm `getTotalPages`: lấy `Total` `Pages`. */
    public int getTotalPages() { return pageSize <= 0 ? 0 : (int) Math.ceil((double) totalCount / pageSize); }
    /** Hàm `hasPreviousPage`: có `Previous` `Page`. */
    public boolean hasPreviousPage() { return pageIndex > 1; }
    /** Hàm `hasNextPage`: có `Next` `Page`. */
    public boolean hasNextPage() { return pageIndex < getTotalPages(); }
}
