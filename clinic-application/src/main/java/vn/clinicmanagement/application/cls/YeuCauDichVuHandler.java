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

    /** Trường `serviceRepository`: dịch vụ kho dữ liệu. */
    private final IChiDinhDichVuRepository serviceRepository;
    /** Trường `visitRepository`: luotkham kho dữ liệu. */
    private final ILuotKhamRepository visitRepository;
    /** Trường `auditWritePort`: kiểm toán ghi congketnoi. */
    private final KiemToanWriteCongKetNoi auditWritePort;

    public YeuCauDichVuHandler(
            IChiDinhDichVuRepository serviceRepository,
            KiemToanWriteCongKetNoi auditWritePort) {
        this(serviceRepository, new NoOpVisitRepository(), auditWritePort);
    }

    public YeuCauDichVuHandler(
            IChiDinhDichVuRepository serviceRepository,
            ILuotKhamRepository visitRepository,
            KiemToanWriteCongKetNoi auditWritePort) {
        this.serviceRepository = Objects.requireNonNull(serviceRepository, "serviceRepository");
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    private static final class NoOpVisitRepository implements ILuotKhamRepository {
        private static final vn.clinicmanagement.domain.common.IUnitOfWork NOOP_UOW = new vn.clinicmanagement.domain.common.IUnitOfWork() {
            /** Hàm `saveChanges`: lưu thaydoi. */
            @Override public int saveChanges() { return 1; }
            /** Hàm `saveEntities`: lưu thực thể. */
            @Override public boolean saveEntities() { return true; }
        };
        /** Hàm `getById`: lấy theo mã. Gốc khai báo tại `ILuotKhamRepository#getById`. */
        @Override public Optional<LuotKham> getById(int id) { return Optional.empty(); }
        /** Hàm `getAll`: lấy `All`. Gốc khai báo tại `ILuotKhamRepository#getAll`. */
        @Override public java.util.List<LuotKham> getAll() { return java.util.List.of(); }
        /** Hàm `getPaged`: lấy phân trang. Gốc khai báo tại `ILuotKhamRepository#getPaged`. */
        @Override public vn.clinicmanagement.domain.repositories.PhanTrangKetQua<LuotKham> getPaged(int p, int s, vn.clinicmanagement.domain.repositories.ITieuChuan<LuotKham> sp) { return new vn.clinicmanagement.domain.repositories.PhanTrangKetQua<>(java.util.List.of(), 0, p, s); }
        /** Hàm `find`: tìm. Gốc khai báo tại `ILuotKhamRepository#find`. */
        @Override public java.util.List<LuotKham> find(vn.clinicmanagement.domain.repositories.ITieuChuan<LuotKham> sp) { return java.util.List.of(); }
        @Override public boolean exists(vn.clinicmanagement.domain.repositories.ITieuChuan<LuotKham> sp) { return false; }
        /** Hàm `count`: dem. Gốc khai báo tại `ILuotKhamRepository#count`. */
        @Override public int count(vn.clinicmanagement.domain.repositories.ITieuChuan<LuotKham> sp) { return 0; }
        /** Hàm `add`: thêm. Gốc khai báo tại `ILuotKhamRepository#add`. */
        @Override public void add(LuotKham entity) {}
        /** Hàm `update`: cập nhật. Gốc khai báo tại `ILuotKhamRepository#update`. */
        @Override public void update(LuotKham entity) {}
        /** Hàm `delete`: xóa. Gốc khai báo tại `ILuotKhamRepository#delete`. */
        @Override public void delete(LuotKham entity) {}
        /** Hàm `getUnitOfWork`: lấy đơn vị của làm việc. Gốc khai báo tại `ILuotKhamRepository#getUnitOfWork`. */
        @Override public vn.clinicmanagement.domain.common.IUnitOfWork getUnitOfWork() { return NOOP_UOW; }
        /** Hàm `getVisitsByDoctor`: lấy `Visits` theo bacsi. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.ILuotKhamRepository#getVisitsByDoctor`. */
        @Override public java.util.List<LuotKham> getVisitsByDoctor(int b, java.time.LocalDate d) { return java.util.List.of(); }
        /** Hàm `getNextQueueNumber`: lấy `Next` hangdoi so. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.ILuotKhamRepository#getNextQueueNumber`. */
        @Override public int getNextQueueNumber(int c, int k, java.time.LocalDate d) { return 1; }
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

        PhanQuyenNguCanh context = command.sessionContext();
        if (!isAuthorized(context)) {
            emitDeniedAudit(context, command.auditActorContext(), "Quyền không được phép chỉ định dịch vụ.");
            return KetQua.failure("AUTH_ACCESS_DENIED", "Không có quyền chỉ định dịch vụ kỹ thuật trong phiên hiện tại.");
        }

        Optional<LuotKham> visitOpt = visitRepository.getById(command.visitId());
        if (visitOpt.isEmpty()) {
            return KetQua.failure("VISIT_NOT_FOUND", "Không tìm thấy lượt khám.");
        }
        LuotKham visit = visitOpt.get();
        if (visit.getTrangThai() == TrangThaiLuotKham.DA_HUY) {
            return KetQua.failure("VISIT_CANCELLED", "Lượt khám đã bị hủy, không thể chỉ định dịch vụ.");
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
                KiemToanHanhDongMa.SERVICE_ORDERED,
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
