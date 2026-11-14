package vn.clinicmanagement.application.cls;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiTietKetQuaXetNghiem;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RecordDichVuKetQuaHandler implements CommandHandler<RecordDichVuKetQuaCommand, RecordDichVuKetQuaBienNhan> {

    private static final Set<String> ALLOWED_ROLES = Set.of("LAB_TECH", "DOCTOR", "ADMIN");

    /** Trường `serviceRepository`: dịch vụ kho dữ liệu. */
    private final IChiDinhDichVuRepository serviceRepository;
    /** Trường `auditWritePort`: kiểm toán ghi congketnoi. */
    private final KiemToanWriteCongKetNoi auditWritePort;

    public RecordDichVuKetQuaHandler(
            IChiDinhDichVuRepository serviceRepository,
            KiemToanWriteCongKetNoi auditWritePort) {
        this.serviceRepository = Objects.requireNonNull(serviceRepository, "serviceRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public KetQua<RecordDichVuKetQuaBienNhan> handle(RecordDichVuKetQuaCommand command) {
        if (command == null) {
            return KetQua.failure("VALIDATION_ERROR", "Lệnh nhập kết quả dịch vụ không được để trống.");
        }
        if (command.orderId() <= 0) {
            return KetQua.failure("INVALID_ORDER", "Mã chỉ định dịch vụ không hợp lệ.");
        }

        PhanQuyenNguCanh context = command.sessionContext();
        if (!isAuthorized(context)) {
            emitDeniedAudit(context, command.auditActorContext(), "Quyền không được phép nhập kết quả dịch vụ.");
            return KetQua.failure("AUTH_ACCESS_DENIED", "Không có quyền nhập kết quả kỹ thuật/cận lâm sàng trong phiên hiện tại.");
        }

        Optional<ChiDinhDichVu> orderOpt = serviceRepository.getById(command.orderId());
        if (orderOpt.isEmpty()) {
            return KetQua.failure("ORDER_NOT_FOUND", "Không tìm thấy chỉ định dịch vụ.");
        }
        ChiDinhDichVu order = orderOpt.get();
        if (order.getTrangThai() == TrangThaiChiDinhDichVu.DA_HUY) {
            return KetQua.failure("ORDER_CANCELLED", "Chỉ định dịch vụ đã bị hủy, không thể nhập kết quả.");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setTrangThai(TrangThaiChiDinhDichVu.DA_CO_KET_QUA);
        order.setKetQua(command.resultSummary() != null ? command.resultSummary() : "");
        order.setKetLuan(command.conclusion() != null ? command.conclusion() : "");
        order.setDanhSachHinhAnhUrl(command.imageAttachmentUrls() != null ? command.imageAttachmentUrls() : "");
        order.setBacSiThucHien(context.employeeId());
        order.setThoiGianThucHien(now);
        order.setThoiGianTraKetQua(now);
        order.markAsUpdated(context.employeeId());

        serviceRepository.save(order);

        int itemCount = 0;
        if (command.resultItems() != null) {
            for (XetNghiemKetQuaMatHangDauVao item : command.resultItems()) {
                if (item == null) continue;
                ChiTietKetQuaXetNghiem line = new ChiTietKetQuaXetNghiem();
                line.setMaChiDinhDichVu(order.getId());
                line.setTenChiSo(item.indicatorName() != null ? item.indicatorName() : "");
                line.setGiaTri(item.value() != null ? item.value() : "");
                line.setDonViDo(item.unit());
                line.setKhoangThamChieu(item.referenceRange());
                line.setDanhGia(item.evaluation());
                line.setCreatedBy(context.employeeId());
                serviceRepository.saveKetQua(line);
                itemCount++;
            }
        }

        emitSuccessAudit(context, command.auditActorContext(), order.getId());

        RecordDichVuKetQuaBienNhan receipt = new RecordDichVuKetQuaBienNhan(
                order.getId(),
                order.getMaLuotKham(),
                order.getTrangThai().name(),
                context.employeeId(),
                itemCount,
                now,
                now);

        return KetQua.success(receipt);
    }

    /** Hàm `isAuthorized`: là `Authorized`. */
    private boolean isAuthorized(PhanQuyenNguCanh context) {
        if (context == null || !context.isValid()) {
            return false;
        }
        String role = context.roleCode().trim().toUpperCase(Locale.ROOT);
        return ALLOWED_ROLES.contains(role);
    }

    /** Hàm `emitDeniedAudit`: `emit` tuchoi kiểm toán. */
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

    /** Hàm `emitSuccessAudit`: `emit` thành công kiểm toán. */
    private void emitSuccessAudit(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor, int orderId) {
        auditWritePort.appendIndependently(new HeThongKiemToanNhapLieu(
                context.employeeId(),
                context.roleId(),
                context.roleCode().trim().toUpperCase(Locale.ROOT),
                context.facilityId(),
                context.departmentId(),
                KiemToanHanhDongMa.SERVICE_RESULT_RECORDED,
                KiemToanTaiNguyenLoai.SERVICE_ORDER,
                orderId,
                KiemToanKetQua.SUCCESS,
                null,
                Instant.now(),
                actor != null ? actor.workstationId() : null,
                correlationId(context, actor)));
    }

    /** Hàm `correlationId`: `correlation` mã. */
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
