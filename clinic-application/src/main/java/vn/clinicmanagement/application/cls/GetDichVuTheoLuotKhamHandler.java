package vn.clinicmanagement.application.cls;

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
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiTietKetQuaXetNghiem;
import vn.clinicmanagement.domain.service.DanhMucDichVu;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class GetDichVuTheoLuotKhamHandler implements QueryHandler<GetDichVuTheoLuotKhamQuery, List<DichVuYeuCauDuLieu>> {

    private static final Set<String> ALLOWED_ROLES = Set.of("DOCTOR", "LAB_TECH", "RECEPTIONIST", "ADMIN");

    /** Trường `serviceRepository`: dịch vụ kho dữ liệu. */
    private final IChiDinhDichVuRepository serviceRepository;
    /** Trường `auditWritePort`: kiểm toán ghi congketnoi. */
    private final KiemToanWriteCongKetNoi auditWritePort;

    public GetDichVuTheoLuotKhamHandler(
            IChiDinhDichVuRepository serviceRepository,
            KiemToanWriteCongKetNoi auditWritePort) {
        this.serviceRepository = Objects.requireNonNull(serviceRepository, "serviceRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public KetQua<List<DichVuYeuCauDuLieu>> handle(GetDichVuTheoLuotKhamQuery query) {
        if (query == null) {
            return KetQua.failure("VALIDATION_ERROR", "Truy vấn chỉ định dịch vụ không được để trống.");
        }
        if (query.visitId() <= 0) {
            return KetQua.failure("INVALID_VISIT", "Mã lượt khám không hợp lệ.");
        }

        PhanQuyenNguCanh context = query.sessionContext();
        if (!isAuthorized(context)) {
            emitDeniedAudit(context, query.auditActorContext(), "Quyền không được phép xem chỉ định dịch vụ.");
            return KetQua.failure("AUTH_ACCESS_DENIED", "Không có quyền tra cứu dịch vụ kỹ thuật/cận lâm sàng trong phiên hiện tại.");
        }

        List<ChiDinhDichVu> orders = serviceRepository.getByLuotKham(query.visitId());
        List<DichVuYeuCauDuLieu> dtos = new ArrayList<>();

        for (ChiDinhDichVu order : orders) {
            if (order == null) continue;
            int orderId = order.getId() != null ? order.getId() : 0;
            Optional<DanhMucDichVu> catalogOpt = serviceRepository.getDichVuById(order.getMaDichVu());
            String serviceCode = catalogOpt.map(DanhMucDichVu::getMaDichVu).orElse("");
            String serviceName = catalogOpt.map(DanhMucDichVu::getTenDichVu).orElse("");
            String serviceType = catalogOpt.map(d -> d.getLoaiDichVu() != null ? d.getLoaiDichVu().name() : "").orElse("");

            List<ChiTietKetQuaXetNghiem> results = serviceRepository.getKetQuaByChiDinh(orderId);
            List<XetNghiemKetQuaDuLieu> resultDtos = new ArrayList<>();
            for (ChiTietKetQuaXetNghiem res : results) {
                if (res == null) continue;
                resultDtos.add(new XetNghiemKetQuaDuLieu(
                        res.getId() != null ? res.getId() : 0,
                        orderId,
                        res.getTenChiSo(),
                        res.getGiaTri(),
                        res.getDonViDo(),
                        res.getKhoangThamChieu(),
                        res.getDanhGia()));
            }

            dtos.add(new DichVuYeuCauDuLieu(
                    orderId,
                    order.getMaLuotKham(),
                    order.getMaDichVu(),
                    serviceCode,
                    serviceName,
                    serviceType,
                    order.getMaBacSiChiDinh(),
                    order.getSoLuong(),
                    order.getDonGia(),
                    order.getTrangThai() != null ? order.getTrangThai().name() : "",
                    order.getKetQua(),
                    order.getKetLuan(),
                    order.getDanhSachHinhAnhUrl(),
                    order.getMaPhongThucHien(),
                    order.getBacSiThucHien(),
                    order.getThoiGianChiDinh(),
                    order.getThoiGianThucHien(),
                    order.getThoiGianTraKetQua(),
                    resultDtos));
        }

        emitSuccessAudit(context, query.auditActorContext(), query.visitId());
        return KetQua.success(dtos);
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
    private void emitSuccessAudit(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor, int visitId) {
        auditWritePort.appendIndependently(new HeThongKiemToanNhapLieu(
                context.employeeId(),
                context.roleId(),
                context.roleCode().trim().toUpperCase(Locale.ROOT),
                context.facilityId(),
                context.departmentId(),
                KiemToanHanhDongMa.PATIENT_VIEW,
                KiemToanTaiNguyenLoai.SERVICE_ORDER,
                visitId,
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
