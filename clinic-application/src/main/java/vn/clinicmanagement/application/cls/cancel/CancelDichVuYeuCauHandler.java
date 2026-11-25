package vn.clinicmanagement.application.cls.cancel;

import vn.clinicmanagement.application.cls.ClsLuotKhamTrangThaiDongBo;
import vn.clinicmanagement.application.billing.ClsVienPhiDongBo;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CancelDichVuYeuCauHandler implements CommandHandler<CancelDichVuYeuCauCommand, CancelDichVuYeuCauBienNhan> {

    private static final Set<String> ALLOWED_ROLES = Set.of("DOCTOR", "ADMIN");

    private final IChiDinhDichVuRepository serviceRepository;
    private final ILuotKhamRepository visitRepository;
    private final GiaoDichRanhGioi transactions;
    private final ClsVienPhiDongBo billing;
    private final KiemToanWriteCongKetNoi auditWritePort;

    public CancelDichVuYeuCauHandler(
            IChiDinhDichVuRepository serviceRepository,
            KiemToanWriteCongKetNoi auditWritePort) {
        this(serviceRepository, ClsLuotKhamTrangThaiDongBo.NOOP_VISIT_REPOSITORY,
                ClsLuotKhamTrangThaiDongBo.DIRECT_TRANSACTIONS, auditWritePort);
    }

    public CancelDichVuYeuCauHandler(
            IChiDinhDichVuRepository serviceRepository,
            ILuotKhamRepository visitRepository,
            GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi auditWritePort) {
        this(serviceRepository, visitRepository, transactions, auditWritePort, null);
    }

    public CancelDichVuYeuCauHandler(IChiDinhDichVuRepository serviceRepository,
            ILuotKhamRepository visitRepository, GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi auditWritePort, ClsVienPhiDongBo billing) {
        this.billing = billing;
        this.serviceRepository = Objects.requireNonNull(serviceRepository, "serviceRepository");
        this.visitRepository = visitRepository != null ? visitRepository : ClsLuotKhamTrangThaiDongBo.NOOP_VISIT_REPOSITORY;
        this.transactions = transactions != null ? transactions : ClsLuotKhamTrangThaiDongBo.DIRECT_TRANSACTIONS;
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public KetQua<CancelDichVuYeuCauBienNhan> handle(CancelDichVuYeuCauCommand command) {
        if (command == null) {
            return KetQua.failure("VALIDATION_ERROR", "Lệnh hủy chỉ định dịch vụ không được để trống.");
        }
        if (command.orderId() <= 0) {
            return KetQua.failure("INVALID_ORDER", "Mã chỉ định dịch vụ không hợp lệ.");
        }

        PhanQuyenNguCanh context = command.sessionContext();
        if (!isAuthorized(context)) {
            emitDeniedAudit(context, command.auditActorContext(), "Quyền không được phép hủy chỉ định dịch vụ.");
            return KetQua.failure("AUTH_ACCESS_DENIED", "Không có quyền hủy chỉ định dịch vụ kỹ thuật trong phiên hiện tại.");
        }

        return transactions.inTransaction(() -> {
            Optional<ChiDinhDichVu> orderOpt = serviceRepository.getById(command.orderId());
            if (orderOpt.isEmpty()) {
                return KetQua.failure("ORDER_NOT_FOUND", "Không tìm thấy chỉ định dịch vụ.");
            }
            ChiDinhDichVu order = orderOpt.get();
            TrangThaiChiDinhDichVu st = order.getTrangThai();
            if (st == TrangThaiChiDinhDichVu.DA_CO_KET_QUA) {
                return KetQua.failure("ORDER_ALREADY_COMPLETED", "Chỉ định dịch vụ đã có kết quả, không thể hủy.");
            }
            if (st == TrangThaiChiDinhDichVu.DA_HUY) {
                return KetQua.failure("ORDER_ALREADY_CANCELLED", "Chỉ định dịch vụ đã bị hủy trước đó.");
            }
            if (st == TrangThaiChiDinhDichVu.DA_LAY_MAU
                    || st == TrangThaiChiDinhDichVu.DANG_THUC_HIEN
                    || st == TrangThaiChiDinhDichVu.DA_DUYET_KET_QUA) {
                return KetQua.failure("ORDER_IN_PROGRESS",
                        "Chỉ định dịch vụ đang ở trạng thái \"" + st.getDescription() + "\", không thể hủy.");
            }
            if (serviceRepository.getExecutionSettings(order.getId()).chanHuyChiDinh()) {
                return KetQua.failure("SERVICE_CANCELLATION_BLOCKED",
                        "Dịch vụ này đã được cấu hình không cho hủy sau khi chỉ định.");
            }

            if (billing != null) {
                var cancelled = billing.cancelCharge(order, context.employeeId(), context.facilityId());
                if (cancelled.isFailure()) {
                    var error = cancelled.getError().orElseThrow();
                    return KetQua.failure(error.code(), error.message());
                }
            }
            LocalDateTime now = LocalDateTime.now();
            order.setTrangThai(TrangThaiChiDinhDichVu.DA_HUY);
            order.markAsUpdated(context.employeeId());
            serviceRepository.save(order);
            ClsLuotKhamTrangThaiDongBo.restoreVisitToExaminingIfNoPendingCls(
                    order.getMaLuotKham(), context.employeeId(), serviceRepository, visitRepository);

            emitSuccessAudit(context, command.auditActorContext(), order.getId());

            CancelDichVuYeuCauBienNhan receipt = new CancelDichVuYeuCauBienNhan(
                    order.getId(),
                    order.getMaLuotKham(),
                    order.getTrangThai().name(),
                    context.employeeId(),
                    command.reason() != null ? command.reason() : "",
                    now);

            return KetQua.success(receipt);
        });
    }

    private boolean isAuthorized(PhanQuyenNguCanh context) {
        if (context == null || !context.isValid()) {
            return false;
        }
        String role = context.roleCode().trim().toUpperCase(Locale.ROOT);
        return ALLOWED_ROLES.contains(role);
    }

    private void emitDeniedAudit(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor, String reason) {
        boolean validContext = context != null && context.isValid();
        auditWritePort.appendIndependently(new HeThongKiemToanNhapLieu(
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
                actor != null ? actor.workstationId() : null,
                correlationId(context, actor)));
    }

    private void emitSuccessAudit(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor, int orderId) {
        auditWritePort.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(
                context.employeeId(),
                context.roleId(),
                context.roleCode().trim().toUpperCase(Locale.ROOT),
                context.facilityId(),
                context.departmentId(),
                KiemToanHanhDongMa.SERVICE_CANCELLED,
                KiemToanTaiNguyenLoai.SERVICE_ORDER,
                orderId,
                KiemToanKetQua.SUCCESS,
                null,
                Instant.now(),
                actor != null ? actor.workstationId() : null,
                correlationId(context, actor)));
    }

    private UUID correlationId(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        if (actor != null && actor.correlationId() != null) {
            return actor.correlationId();
        }
        if (context != null && context.sessionId() != null) {
            return context.sessionId();
        }
        return UUID.randomUUID();
    }
}
