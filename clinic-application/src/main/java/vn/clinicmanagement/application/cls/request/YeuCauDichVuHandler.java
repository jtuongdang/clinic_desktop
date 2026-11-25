package vn.clinicmanagement.application.cls.request;

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
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.domain.service.DanhMucDichVu;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class YeuCauDichVuHandler implements CommandHandler<YeuCauDichVuCommand, YeuCauDichVuBienNhan> {

    private static final Set<String> ALLOWED_ROLES = Set.of("DOCTOR", "ADMIN");

    private final IChiDinhDichVuRepository serviceRepository;
    private final ILuotKhamRepository visitRepository;
    private final GiaoDichRanhGioi transactions;
    private final ClsVienPhiDongBo billing;
    private final KiemToanWriteCongKetNoi auditWritePort;

    public YeuCauDichVuHandler(
            IChiDinhDichVuRepository serviceRepository,
            KiemToanWriteCongKetNoi auditWritePort) {
        this(serviceRepository, ClsLuotKhamTrangThaiDongBo.NOOP_VISIT_REPOSITORY,
                ClsLuotKhamTrangThaiDongBo.DIRECT_TRANSACTIONS, auditWritePort);
    }

    public YeuCauDichVuHandler(
            IChiDinhDichVuRepository serviceRepository,
            ILuotKhamRepository visitRepository,
            KiemToanWriteCongKetNoi auditWritePort) {
        this(serviceRepository, visitRepository, ClsLuotKhamTrangThaiDongBo.DIRECT_TRANSACTIONS, auditWritePort);
    }

    public YeuCauDichVuHandler(
            IChiDinhDichVuRepository serviceRepository,
            ILuotKhamRepository visitRepository,
            GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi auditWritePort) {
        this(serviceRepository, visitRepository, transactions, auditWritePort, null);
    }

    public YeuCauDichVuHandler(IChiDinhDichVuRepository serviceRepository,
            ILuotKhamRepository visitRepository, GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi auditWritePort, ClsVienPhiDongBo billing) {
        this.billing = billing;
        this.serviceRepository = Objects.requireNonNull(serviceRepository, "serviceRepository");
        this.visitRepository = visitRepository != null ? visitRepository : ClsLuotKhamTrangThaiDongBo.NOOP_VISIT_REPOSITORY;
        this.transactions = transactions != null ? transactions : ClsLuotKhamTrangThaiDongBo.DIRECT_TRANSACTIONS;
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public KetQua<YeuCauDichVuBienNhan> handle(YeuCauDichVuCommand command) {
        if (command == null) {
            return KetQua.failure("VALIDATION_ERROR", "Lệnh chỉ định dịch vụ không được để trống.");
        }
        if (command.visitId() <= 0) {
            return KetQua.failure("INVALID_VISIT", "Mã lượt khám không hợp lệ.");
        }
        if (command.serviceId() <= 0) {
            return KetQua.failure("INVALID_SERVICE", "Mã dịch vụ không hợp lệ.");
        }
        if (command.quantity() == null || command.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            return KetQua.failure("INVALID_QUANTITY", "Số lượng chỉ định phải lớn hơn 0.");
        }

        if (command.quantity().stripTrailingZeros().scale() > 2) {
            return KetQua.failure("INVALID_QUANTITY", "Số lượng chỉ định chỉ được có tối đa 2 chữ số thập phân.");
        }
        PhanQuyenNguCanh context = command.sessionContext();
        if (!isAuthorized(context)) {
            emitDeniedAudit(context, command.auditActorContext(), "Quyền không được phép chỉ định dịch vụ.");
            return KetQua.failure("AUTH_ACCESS_DENIED", "Không có quyền chỉ định dịch vụ kỹ thuật trong phiên hiện tại.");
        }

        return transactions.inTransaction(() -> {
            Optional<LuotKham> visitOpt = visitRepository.getById(command.visitId());
            if (visitOpt.isEmpty()) {
                return KetQua.failure("VISIT_NOT_FOUND", "Không tìm thấy lượt khám.");
            }
            LuotKham visit = visitOpt.get();
            if (visit.getTrangThai() == TrangThaiLuotKham.DA_HUY) {
                return KetQua.failure("VISIT_CANCELLED", "Lượt khám đã bị hủy, không thể chỉ định dịch vụ.");
            }
            if (visit.isEmrLocked()) {
                return KetQua.failure("EMR_LOCKED", "Bệnh án đã khóa, không thể chỉ định thêm dịch vụ.");
            }

            Optional<DanhMucDichVu> serviceOpt = serviceRepository.getDichVuById(command.serviceId());
            if (serviceOpt.isEmpty()) {
                return KetQua.failure("SERVICE_NOT_FOUND", "Không tìm thấy dịch vụ kỹ thuật trong danh mục.");
            }
            DanhMucDichVu service = serviceOpt.get();
            if (!service.isHoatDong()) {
                return KetQua.failure("SERVICE_INACTIVE", "Dịch vụ đã ngừng hoạt động.");
            }

            ChiDinhDichVu order = new ChiDinhDichVu();
            order.setMaLuotKham(command.visitId());
            order.setMaDichVu(command.serviceId());
            order.setMaBacSiChiDinh(context.employeeId());
            order.setSoLuong(command.quantity());
            order.setDonGia(service.getDonGia() != null ? service.getDonGia() : BigDecimal.ZERO);
            order.setMaPhongThucHien(command.roomId());
            order.setTrangThai(TrangThaiChiDinhDichVu.DA_CHI_DINH);
            order.setThoiGianChiDinh(LocalDateTime.now());
            order.setCreatedBy(context.employeeId());

            serviceRepository.save(order);
            if (billing != null) {
                var charged = billing.addCharge(order, service.getTenDichVu(), context.employeeId(), context.facilityId());
                if (charged.isFailure()) {
                    var error = charged.getError().orElseThrow();
                    return KetQua.failure(error.code(), error.message());
                }
            }
            ClsLuotKhamTrangThaiDongBo.markVisitAwaitingClsIfApplicable(
                    visit, service, context.employeeId(), visitRepository);

            int orderId = order.getId() != null ? order.getId() : 0;

            emitSuccessAudit(context, command.auditActorContext(), orderId);

            YeuCauDichVuBienNhan receipt = new YeuCauDichVuBienNhan(
                    orderId,
                    command.visitId(),
                    command.serviceId(),
                    service.getTenDichVu(),
                    order.getSoLuong(),
                    order.getDonGia(),
                    order.getTrangThai().name(),
                    order.getThoiGianChiDinh());

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
                KiemToanHanhDongMa.SERVICE_ORDERED,
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
