package vn.clinicmanagement.application.inventory.get;

import vn.clinicmanagement.application.inventory.TonKhoKhaDungDuLieu;
import vn.clinicmanagement.application.inventory.TonKhoLoDuLieu;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class GetTonKhoKhaDungHandler
        implements QueryHandler<GetTonKhoKhaDungQuery, TonKhoKhaDungDuLieu> {
    private static final Set<String> ALLOWED_ROLES = Set.of("PHARMACIST", "DOCTOR", "ADMIN");

    private final IKhoRepository inventoryRepository;
    private final KiemToanWriteCongKetNoi audit;

    public GetTonKhoKhaDungHandler(IKhoRepository inventoryRepository, KiemToanWriteCongKetNoi audit) {
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public KetQua<TonKhoKhaDungDuLieu> handle(GetTonKhoKhaDungQuery query) {
        if (query == null || query.warehouseId() <= 0 || query.itemId() <= 0) {
            return KetQua.failure("VALIDATION_ERROR", "Thông tin tra cứu kho hoặc mặt hàng không hợp lệ.");
        }

        PhanQuyenNguCanh context = query.authorizationContext();
        if (!isAuthorized(context)) {
            emitDeniedAudit(context, query.auditActorContext());
            return KetQua.failure("AUTH_ACCESS_DENIED", "Không có quyền tra cứu tồn kho trong phiên hiện tại.");
        }

        Optional<Kho> warehouse = inventoryRepository.getKhoById(query.warehouseId());
        if (warehouse.isEmpty()) {
            return KetQua.failure("NOT_FOUND", "Không tìm thấy kho dược.");
        }

        Optional<MatHang> item = inventoryRepository.getItemById(query.itemId());
        if (item.isEmpty()) {
            return KetQua.failure("NOT_FOUND", "Không tìm thấy mặt hàng.");
        }

        LocalDate today = LocalDate.now();
        List<TonKho> batches = inventoryRepository.getAvailableBatches(query.warehouseId(), query.itemId()).stream()
                .sorted(Comparator.comparing(TonKho::getHanSuDung, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        BigDecimal availableQuantity = BigDecimal.ZERO;
        List<TonKhoLoDuLieu> batchDtos = batches.stream()
                .map(batch -> toDto(batch, today))
                .toList();
        for (TonKhoLoDuLieu batch : batchDtos) {
            if (!batch.expired() && batch.soLuongTon() != null
                    && batch.soLuongTon().compareTo(BigDecimal.ZERO) > 0) {
                availableQuantity = availableQuantity.add(batch.soLuongTon());
            }
        }

        TonKhoKhaDungDuLieu dto = new TonKhoKhaDungDuLieu(
                query.warehouseId(),
                query.itemId(),
                item.get().getMaItem(),
                item.get().getTenItem(),
                availableQuantity,
                batchDtos);
        emitSuccessAudit(context, query.auditActorContext(), query.itemId());
        return KetQua.success(dto);
    }

    private boolean isAuthorized(PhanQuyenNguCanh context) {
        return context != null && context.isValid()
                && ALLOWED_ROLES.contains(context.roleCode().trim().toUpperCase(Locale.ROOT));
    }

    private TonKhoLoDuLieu toDto(TonKho batch, LocalDate today) {
        LocalDate expiryDate = batch.getHanSuDung();
        return new TonKhoLoDuLieu(
                batch.getId(),
                batch.getSoLo(),
                expiryDate,
                batch.getSoLuongTon(),
                batch.getGiaNhap(),
                batch.getGiaBan(),
                expiryDate != null && expiryDate.isBefore(today));
    }

    private void emitDeniedAudit(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        boolean validContext = context != null && context.isValid();
        audit.appendIndependently(new HeThongKiemToanNhapLieu(
                validContext ? context.employeeId() : null,
                validContext ? context.roleId() : null,
                validContext ? context.roleCode().trim().toUpperCase(Locale.ROOT) : null,
                validContext ? context.facilityId() : null,
                validContext ? context.departmentId() : null,
                KiemToanHanhDongMa.AUTH_ACCESS_DENIED,
                null,
                null,
                KiemToanKetQua.DENIED,
                KiemToanLyDoMa.AUTH_FORBIDDEN,
                Instant.now(),
                actor == null ? null : actor.workstationId(),
                correlationId(context, actor)));
    }

    private void emitSuccessAudit(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor, int itemId) {
        audit.appendIndependently(new HeThongKiemToanNhapLieu(
                context.employeeId(),
                context.roleId(),
                context.roleCode().trim().toUpperCase(Locale.ROOT),
                context.facilityId(),
                context.departmentId(),
                KiemToanHanhDongMa.CATALOG_VIEW,
                KiemToanTaiNguyenLoai.ITEM,
                itemId,
                KiemToanKetQua.SUCCESS,
                null,
                Instant.now(),
                actor == null ? null : actor.workstationId(),
                correlationId(context, actor)));
    }

    private UUID correlationId(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        if (actor != null) {
            return actor.correlationId();
        }
        if (context != null && context.sessionId() != null) {
            return context.sessionId();
        }
        return UUID.randomUUID();
    }
}
