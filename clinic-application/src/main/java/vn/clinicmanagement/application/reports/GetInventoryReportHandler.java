package vn.clinicmanagement.application.reports;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.inventory.Item;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Xử lý truy vấn báo cáo xuất nhập tồn kho dược & cảnh báo hết hạn (RPT-01).
 * Thẩm quyền yêu cầu: ADMIN, DIRECTOR, PHARMACIST.
 * Thống kê số lượng tồn, giá trị tồn (nhập/bán), cảnh báo cận hạn (< 6 tháng) và hết hạn.
 */
public final class GetInventoryReportHandler implements QueryHandler<GetInventoryReportQuery, InventoryReportDto> {

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "DIRECTOR", "PHARMACIST");

    private final IInventoryReportDataSource dataSource;
    private final AuditWritePort auditWritePort;

    public GetInventoryReportHandler(IInventoryReportDataSource dataSource, AuditWritePort auditWritePort) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.auditWritePort = auditWritePort;
    }

    public GetInventoryReportHandler(IInventoryReportDataSource dataSource) {
        this(dataSource, null);
    }

    public GetInventoryReportHandler(IKhoRepository khoRepository, AuditWritePort auditWritePort) {
        this(fromRepository(khoRepository), auditWritePort);
    }

    public GetInventoryReportHandler(IKhoRepository khoRepository) {
        this(fromRepository(khoRepository), null);
    }

    public GetInventoryReportHandler(IKhoRepository khoRepository, IInventoryReportDataSource dataSource) {
        this(dataSource != null ? dataSource : fromRepository(khoRepository), null);
    }

    public GetInventoryReportHandler(IKhoRepository khoRepository, IInventoryReportDataSource dataSource, AuditWritePort auditWritePort) {
        this(dataSource != null ? dataSource : fromRepository(khoRepository), auditWritePort);
    }

    private static IInventoryReportDataSource fromRepository(IKhoRepository khoRepository) {
        Objects.requireNonNull(khoRepository, "khoRepository");
        return new IInventoryReportDataSource() {
            @Override
            public List<TonKho> getBatches(Integer warehouseId) {
                if (warehouseId != null && warehouseId > 0) {
                    List<TonKho> list = khoRepository.getBatchesByKho(warehouseId);
                    return list != null ? list : List.of();
                }
                // Nếu không chỉ định kho, gom các kho thông dụng 1..10 hoặc từ searchItems
                List<Item> allItems = khoRepository.searchItems(null, true);
                if (allItems == null) return List.of();
                List<TonKho> result = new ArrayList<>();
                for (Item item : allItems) {
                    if (item != null && item.getTonKhos() != null) {
                        result.addAll(item.getTonKhos());
                    }
                }
                if (result.isEmpty()) {
                    for (int k = 1; k <= 5; k++) {
                        List<TonKho> b = khoRepository.getBatchesByKho(k);
                        if (b != null) result.addAll(b);
                    }
                }
                return result;
            }

            @Override
            public Item getItemById(int itemId) {
                return khoRepository.getItemById(itemId).orElse(null);
            }

            @Override
            public List<Item> getAllItems() {
                List<Item> items = khoRepository.searchItems(null, true);
                return items != null ? items : List.of();
            }
        };
    }

    @Override
    public Result<InventoryReportDto> handle(GetInventoryReportQuery query) {
        if (query == null) {
            return Result.failure("QUERY_INVALID", "Truy vấn báo cáo tồn kho dược phẩm không được để trống.");
        }

        // 1. Kiểm tra thẩm quyền nghiêm ngặt (ADMIN, DIRECTOR, PHARMACIST)
        String role = query.getEffectiveRole();
        if (role == null || role.isBlank() || !ALLOWED_ROLES.contains(role)) {
            logAudit(query.getAuditActorContext(), AuditOutcome.DENIED, AuditReasonCode.AUTH_FORBIDDEN,
                    "Từ chối quyền xem báo cáo tồn kho cho vai trò: " + role);
            return Result.failure("AUTHZ_DENIED",
                    "Chỉ Quản trị viên (ADMIN), Giám đốc (DIRECTOR) hoặc Dược sĩ (PHARMACIST) mới có quyền truy cập báo cáo kho dược phẩm.");
        }

        LocalDate asOfDate = query.getToDate() != null ? query.getToDate() : LocalDate.now();
        int alertDays = query.getExpiryAlertDays();

        // 2. Tra cứu danh sách lô tồn kho
        List<TonKho> batches;
        try {
            batches = dataSource.getBatches(query.getWarehouseId());
            if (batches == null) {
                batches = List.of();
            }
        } catch (Exception ex) {
            return Result.failure("DATA_FETCH_FAILED", "Không thể lấy dữ liệu tồn kho: " + ex.getMessage());
        }

        // 3. Cache danh mục Item để tra cứu nhanh thông tin
        Map<Integer, Item> itemMap = new HashMap<>();
        try {
            List<Item> allItems = dataSource.getAllItems();
            if (allItems != null) {
                for (Item it : allItems) {
                    if (it != null && it.getId() != null) {
                        itemMap.put(it.getId(), it);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 4. Tính toán tổng lượng tồn theo từng Item để kiểm tra mức tồn tối thiểu
        Map<Integer, BigDecimal> itemTotalQuantities = new HashMap<>();
        for (TonKho tk : batches) {
            if (tk == null || tk.isDeleted()) continue;
            int itemId = tk.getMaItem();
            BigDecimal qty = tk.getSoLuongTon() != null ? tk.getSoLuongTon() : BigDecimal.ZERO;
            itemTotalQuantities.put(itemId, itemTotalQuantities.getOrDefault(itemId, BigDecimal.ZERO).add(qty));
        }

        // 5. Tổng hợp đa chiều
        BigDecimal totalStockQuantity = BigDecimal.ZERO;
        BigDecimal totalStockValue = BigDecimal.ZERO;
        BigDecimal totalRetailValue = BigDecimal.ZERO;
        Set<Integer> uniqueItemIds = new HashSet<>();

        List<InventoryReportItemDto> items = new ArrayList<>();
        List<InventoryReportItemDto> expiryAlerts = new ArrayList<>();
        List<InventoryReportItemDto> lowStockAlerts = new ArrayList<>();

        int expiredCount = 0;
        int nearExpiryCount = 0;
        Set<Integer> lowStockItemIds = new HashSet<>();

        int stt = 1;
        for (TonKho tk : batches) {
            if (tk == null || tk.isDeleted()) continue;

            int itemId = tk.getMaItem();
            uniqueItemIds.add(itemId);

            Item item = itemMap.computeIfAbsent(itemId, id -> dataSource.getItemById(id));
            String itemCode = (item != null && item.getMaItem() != null) ? item.getMaItem() : "ITM-" + itemId;
            String itemName = (item != null && item.getTenItem() != null) ? item.getTenItem() : "Dược phẩm #" + itemId;
            String unit = (item != null && item.getDonViCoBan() != null && !item.getDonViCoBan().isBlank()) ? item.getDonViCoBan() : "Đơn vị";

            BigDecimal qty = tk.getSoLuongTon() != null ? tk.getSoLuongTon() : BigDecimal.ZERO;
            BigDecimal importPrice = tk.getGiaNhap() != null && tk.getGiaNhap().compareTo(BigDecimal.ZERO) > 0
                    ? tk.getGiaNhap()
                    : (item != null && item.getGiaNhapMacDinh() != null ? item.getGiaNhapMacDinh() : BigDecimal.ZERO);
            BigDecimal retailPrice = tk.getGiaBan() != null && tk.getGiaBan().compareTo(BigDecimal.ZERO) > 0
                    ? tk.getGiaBan()
                    : (item != null && item.getGiaBanMacDinh() != null ? item.getGiaBanMacDinh() : BigDecimal.ZERO);

            BigDecimal lineValue = qty.multiply(importPrice);
            BigDecimal lineRetail = qty.multiply(retailPrice);

            totalStockQuantity = totalStockQuantity.add(qty);
            totalStockValue = totalStockValue.add(lineValue);
            totalRetailValue = totalRetailValue.add(lineRetail);

            // Kiểm tra hạn dùng
            LocalDate exp = tk.getHanSuDung();
            Long daysToExpiry = null;
            boolean isExpired = false;
            boolean isNearExpiry = false;
            String status = "BÌNH THƯỜNG";

            if (exp != null) {
                daysToExpiry = ChronoUnit.DAYS.between(asOfDate, exp);
                if (exp.isBefore(asOfDate)) {
                    isExpired = true;
                    status = "HẾT HẠN";
                    expiredCount++;
                } else if (daysToExpiry <= alertDays) {
                    isNearExpiry = true;
                    status = "CẬN HẠN";
                    nearExpiryCount++;
                }
            }

            // Kiểm tra tồn tối thiểu
            BigDecimal totalItemQty = itemTotalQuantities.getOrDefault(itemId, BigDecimal.ZERO);
            boolean isLowStock = false;
            if (item != null && item.getTonToiThieu() != null && item.getTonToiThieu().compareTo(BigDecimal.ZERO) > 0) {
                if (totalItemQty.compareTo(item.getTonToiThieu()) <= 0) {
                    isLowStock = true;
                    lowStockItemIds.add(itemId);
                }
            }

            String note = "";
            if (isExpired) {
                note = "ĐÃ HẾT HẠN - Khóa xuất kho ngay";
            } else if (isNearExpiry) {
                note = "Ưu tiên cấp phát FEFO (còn " + daysToExpiry + " ngày)";
            } else if (isLowStock) {
                note = "Dưới mức tồn tối thiểu";
            }

            String warehouseName = "Kho Dược " + (tk.getMaKho() > 0 ? tk.getMaKho() : 1);

            InventoryReportItemDto itemDto = new InventoryReportItemDto(
                    stt++,
                    itemId,
                    itemCode,
                    itemName,
                    unit,
                    tk.getMaKho(),
                    warehouseName,
                    tk.getSoLo(),
                    exp,
                    BigDecimal.ZERO, // Tồn đầu (mở rộng khi có thẻ kho chi tiết)
                    BigDecimal.ZERO, // Nhập trong kỳ
                    BigDecimal.ZERO, // Xuất trong kỳ
                    qty,
                    importPrice,
                    retailPrice,
                    lineValue,
                    daysToExpiry,
                    isExpired,
                    isNearExpiry,
                    isLowStock,
                    status,
                    note
            );

            items.add(itemDto);

            if (isExpired || isNearExpiry) {
                expiryAlerts.add(itemDto);
            }
            if (isLowStock) {
                lowStockAlerts.add(itemDto);
            }
        }

        InventoryReportDto dto = new InventoryReportDto(
                asOfDate,
                query.getWarehouseId(),
                uniqueItemIds.size(),
                totalStockQuantity,
                totalStockValue,
                totalRetailValue,
                expiredCount,
                nearExpiryCount,
                lowStockItemIds.size(),
                items,
                expiryAlerts,
                lowStockAlerts
        );

        // 6. Ghi nhật ký kiểm toán nếu có context
        logAudit(query.getAuditActorContext(), AuditOutcome.SUCCESS, null, "Xem báo cáo xuất nhập tồn kho dược");

        return Result.success(dto);
    }

    private void logAudit(AuditActorContext ctx, AuditOutcome outcome, AuditReasonCode reasonCode, String message) {
        if (auditWritePort == null || ctx == null) {
            return;
        }
        try {
            SystemAuditEntry entry = new SystemAuditEntry(
                    ctx.employeeId(),
                    ctx.roleId(),
                    ctx.roleCode(),
                    ctx.facilityId(),
                    ctx.departmentId(),
                    outcome == AuditOutcome.SUCCESS ? AuditActionCode.CATALOG_VIEW : AuditActionCode.AUTH_ACCESS_DENIED,
                    AuditResourceType.ITEM,
                    1,
                    outcome,
                    reasonCode,
                    Instant.now(),
                    ctx.workstationId(),
                    ctx.correlationId()
            );
            auditWritePort.appendIndependently(entry);
        } catch (Exception ignored) {
            // Read-only query does not fail on audit exception
        }
    }
}
