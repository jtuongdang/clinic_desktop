package vn.clinicmanagement.application.reports;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.inventory.MatHang;
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
public final class GetKhoBaoCaoHandler implements QueryHandler<GetKhoBaoCaoQuery, KhoBaoCaoDuLieu> {

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "DIRECTOR", "PHARMACIST");

    /** Trường `dataSource`: dữ liệu nguồn. */
    private final IKhoBaoCaoDuLieuNguon dataSource;
    /** Trường `auditWritePort`: kiểm toán ghi congketnoi. */
    private final KiemToanWriteCongKetNoi auditWritePort;

    public GetKhoBaoCaoHandler(IKhoBaoCaoDuLieuNguon dataSource, KiemToanWriteCongKetNoi auditWritePort) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.auditWritePort = auditWritePort;
    }

    public GetKhoBaoCaoHandler(IKhoBaoCaoDuLieuNguon dataSource) {
        this(dataSource, null);
    }

    public GetKhoBaoCaoHandler(IKhoRepository khoRepository, KiemToanWriteCongKetNoi auditWritePort) {
        this(fromRepository(khoRepository), auditWritePort);
    }

    public GetKhoBaoCaoHandler(IKhoRepository khoRepository) {
        this(fromRepository(khoRepository), null);
    }

    public GetKhoBaoCaoHandler(IKhoRepository khoRepository, IKhoBaoCaoDuLieuNguon dataSource) {
        this(dataSource != null ? dataSource : fromRepository(khoRepository), null);
    }

    public GetKhoBaoCaoHandler(IKhoRepository khoRepository, IKhoBaoCaoDuLieuNguon dataSource, KiemToanWriteCongKetNoi auditWritePort) {
        this(dataSource != null ? dataSource : fromRepository(khoRepository), auditWritePort);
    }

    /** Hàm `fromRepository`: tu kho dữ liệu. */
    private static IKhoBaoCaoDuLieuNguon fromRepository(IKhoRepository khoRepository) {
        Objects.requireNonNull(khoRepository, "khoRepository");
        return new IKhoBaoCaoDuLieuNguon() {
            /** Hàm `getBatches`: lấy `Batches`. */
            @Override
            public List<TonKho> getBatches(Integer warehouseId) {
                if (warehouseId != null && warehouseId > 0) {
                    List<TonKho> list = khoRepository.getBatchesByKho(warehouseId);
                    return list != null ? list : List.of();
                }
                // Nếu không chỉ định kho, gom các kho thông dụng 1..10 hoặc từ searchItems
                List<MatHang> allItems = khoRepository.searchItems(null, true);
                if (allItems == null) return List.of();
                List<TonKho> result = new ArrayList<>();
                for (MatHang item : allItems) {
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

            /** Hàm `getItemById`: lấy mathang theo mã. */
            @Override
            public MatHang getItemById(int itemId) {
                return khoRepository.getItemById(itemId).orElse(null);
            }

            /** Hàm `getAllItems`: lấy `All` `Items`. */
            @Override
            public List<MatHang> getAllItems() {
                List<MatHang> items = khoRepository.searchItems(null, true);
                return items != null ? items : List.of();
            }
        };
    }

    @Override
    public KetQua<KhoBaoCaoDuLieu> handle(GetKhoBaoCaoQuery query) {
        if (query == null) {
            return KetQua.failure("QUERY_INVALID", "Truy vấn báo cáo tồn kho dược phẩm không được để trống.");
        }

        // 1. Kiểm tra thẩm quyền nghiêm ngặt (ADMIN, DIRECTOR, PHARMACIST)
        String role = query.getEffectiveRole();
        if (role == null || role.isBlank() || !ALLOWED_ROLES.contains(role)) {
            logAudit(query.getAuditActorContext(), KiemToanKetQua.DENIED, KiemToanLyDoMa.AUTH_FORBIDDEN,
                    "Từ chối quyền xem báo cáo tồn kho cho vai trò: " + role);
            return KetQua.failure("AUTHZ_DENIED",
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
            return KetQua.failure("DATA_FETCH_FAILED", "Không thể lấy dữ liệu tồn kho: " + ex.getMessage());
        }

        // 3. Cache danh mục Item để tra cứu nhanh thông tin
        Map<Integer, MatHang> itemMap = new HashMap<>();
        try {
            List<MatHang> allItems = dataSource.getAllItems();
            if (allItems != null) {
                for (MatHang it : allItems) {
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

        List<KhoBaoCaoMatHangDuLieu> items = new ArrayList<>();
        List<KhoBaoCaoMatHangDuLieu> expiryAlerts = new ArrayList<>();
        List<KhoBaoCaoMatHangDuLieu> lowStockAlerts = new ArrayList<>();

        int expiredCount = 0;
        int nearExpiryCount = 0;
        Set<Integer> lowStockItemIds = new HashSet<>();

        int stt = 1;
        for (TonKho tk : batches) {
            if (tk == null || tk.isDeleted()) continue;

            int itemId = tk.getMaItem();
            uniqueItemIds.add(itemId);

            MatHang item = itemMap.computeIfAbsent(itemId, id -> dataSource.getItemById(id));
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

            KhoBaoCaoMatHangDuLieu itemDto = new KhoBaoCaoMatHangDuLieu(
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

        KhoBaoCaoDuLieu dto = new KhoBaoCaoDuLieu(
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
        logAudit(query.getAuditActorContext(), KiemToanKetQua.SUCCESS, null, "Xem báo cáo xuất nhập tồn kho dược");

        return KetQua.success(dto);
    }

    /** Hàm `logAudit`: nhatky kiểm toán. */
    private void logAudit(KiemToanTacNhanNguCanh ctx, KiemToanKetQua outcome, KiemToanLyDoMa reasonCode, String message) {
        if (auditWritePort == null || ctx == null) {
            return;
        }
        try {
            HeThongKiemToanNhapLieu entry = new HeThongKiemToanNhapLieu(
                    ctx.employeeId(),
                    ctx.roleId(),
                    ctx.roleCode(),
                    ctx.facilityId(),
                    ctx.departmentId(),
                    outcome == KiemToanKetQua.SUCCESS ? KiemToanHanhDongMa.CATALOG_VIEW : KiemToanHanhDongMa.AUTH_ACCESS_DENIED,
                    KiemToanTaiNguyenLoai.ITEM,
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
