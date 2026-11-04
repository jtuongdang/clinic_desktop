package vn.clinicmanagement.domain.repositories;

import java.util.Collections;
import java.util.List;

public class PagedResult<T> {
    private final List<T> items;
    private final int totalCount;
    private final int pageIndex;
    private final int pageSize;

    public PagedResult(List<T> items, int totalCount, int pageIndex, int pageSize) {
        this.items = items != null ? items : Collections.emptyList();
        this.totalCount = totalCount;
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
    }

    public List<T> getItems() { return items; }
    public int getTotalCount() { return totalCount; }
    public int getPageIndex() { return pageIndex; }
    public int getPageSize() { return pageSize; }
    public int getTotalPages() { return pageSize <= 0 ? 0 : (int) Math.ceil((double) totalCount / pageSize); }
    public boolean hasPreviousPage() { return pageIndex > 1; }
    public boolean hasNextPage() { return pageIndex < getTotalPages(); }
}
