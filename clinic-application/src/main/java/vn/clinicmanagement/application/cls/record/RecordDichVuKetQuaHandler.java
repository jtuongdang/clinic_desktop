package vn.clinicmanagement.application.cls.record;

import vn.clinicmanagement.application.cls.ClsLuotKhamTrangThaiDongBo;
import vn.clinicmanagement.application.cls.XetNghiemKetQuaMatHangDauVao;

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

    private final IChiDinhDichVuRepository serviceRepository;
    private final ILuotKhamRepository visitRepository;
    private final GiaoDichRanhGioi transactions;
    private final KiemToanWriteCongKetNoi auditWritePort;
    private final vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi consumption;

    public RecordDichVuKetQuaHandler(
            IChiDinhDichVuRepository serviceRepository,
            KiemToanWriteCongKetNoi auditWritePort) {
        this(serviceRepository, ClsLuotKhamTrangThaiDongBo.NOOP_VISIT_REPOSITORY,
                ClsLuotKhamTrangThaiDongBo.DIRECT_TRANSACTIONS, auditWritePort);
    }

    public RecordDichVuKetQuaHandler(
            IChiDinhDichVuRepository serviceRepository,
            ILuotKhamRepository visitRepository,
            GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi auditWritePort) {
        this(serviceRepository,visitRepository,transactions,auditWritePort,null);
    }

    public RecordDichVuKetQuaHandler(IChiDinhDichVuRepository serviceRepository,ILuotKhamRepository visitRepository,
            GiaoDichRanhGioi transactions,KiemToanWriteCongKetNoi auditWritePort,
            vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi consumption) {
        this.serviceRepository = Objects.requireNonNull(serviceRepository, "serviceRepository");
        this.visitRepository = visitRepository != null ? visitRepository : ClsLuotKhamTrangThaiDongBo.NOOP_VISIT_REPOSITORY;
        this.transactions = transactions != null ? transactions : ClsLuotKhamTrangThaiDongBo.DIRECT_TRANSACTIONS;
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
        this.consumption=consumption;
    }

    public java.util.List<vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi.CauHinh> getConsumptionDefinition(PhanQuyenNguCanh context,int orderId){
        return consumption==null?java.util.List.of():consumption.getDefinition(context,orderId);
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

        return transactions.inTransaction(() -> {
            Optional<ChiDinhDichVu> orderOpt = serviceRepository.getById(command.orderId());
            if (orderOpt.isEmpty()) {
                return KetQua.failure("ORDER_NOT_FOUND", "Không tìm thấy chỉ định dịch vụ.");
            }
            ChiDinhDichVu order = orderOpt.get();
            if (order.getTrangThai() == TrangThaiChiDinhDichVu.DA_HUY) {
                return KetQua.failure("ORDER_CANCELLED", "Chỉ định dịch vụ đã bị hủy, không thể nhập kết quả.");
            }
            if (order.getTrangThai() == TrangThaiChiDinhDichVu.DA_CO_KET_QUA
                    || order.getTrangThai() == TrangThaiChiDinhDichVu.DA_DUYET_KET_QUA) {
                return KetQua.failure("ORDER_ALREADY_COMPLETED", "Chỉ định dịch vụ đã trả kết quả, không thể nhập lại.");
            }

            LocalDateTime now = LocalDateTime.now();
            String summary = command.resultSummary() != null ? command.resultSummary().trim() : "";
            String conclusion = command.conclusion() != null ? command.conclusion().trim() : "";
            if (summary.isEmpty() && command.resultItems() != null && !command.resultItems().isEmpty()) {
                var valid = command.resultItems().stream()
                        .filter(i -> i != null && i.value() != null && !i.value().isBlank())
                        .toList();
                if (valid.size() == 1) {
                    var only = valid.get(0);
                    String u = only.unit() != null && !only.unit().isBlank() ? " " + only.unit().trim() : "";
                    summary = only.value().trim() + u;
                } else if (!valid.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (var it : valid) {
                        if (!sb.isEmpty()) sb.append("; ");
                        String u = it.unit() != null && !it.unit().isBlank() ? " " + it.unit().trim() : "";
                        sb.append(it.indicatorName() != null ? it.indicatorName().trim() : "").append(": ")
                                .append(it.value().trim()).append(u);
                    }
                    summary = sb.toString();
                }
            }
            if (conclusion.isEmpty() && command.resultItems() != null && !command.resultItems().isEmpty()) {
                var evals = command.resultItems().stream()
                        .filter(i -> i != null && i.evaluation() != null && !i.evaluation().isBlank())
                        .map(i -> i.evaluation().trim())
                        .distinct()
                        .toList();
                if (!evals.isEmpty()) {
                    conclusion = String.join("; ", evals);
                }
            }
            if (summary.isEmpty() && conclusion.isEmpty()) {
                return KetQua.failure("RESULT_REQUIRED", "Nhập kết quả hoặc kết luận trước khi lưu.");
            }

            if (command.approveFinal()) {
                order.setTrangThai(TrangThaiChiDinhDichVu.DA_CO_KET_QUA);
            }
            order.setKetQua(summary);
            order.setKetLuan(conclusion);
            if (command.imageAttachmentUrls() != null && !command.imageAttachmentUrls().isBlank()) {
                order.setDanhSachHinhAnhUrl(command.imageAttachmentUrls());
            }
            order.setBacSiThucHien(context.employeeId());
            if (order.getThoiGianThucHien() == null) {
                order.setThoiGianThucHien(now);
            }
            if (command.approveFinal()) order.setThoiGianTraKetQua(now);
            order.markAsUpdated(context.employeeId());

            String consumptionStatus=command.approveFinal() ? "NOT_CONFIGURED" : "NOT_TRIGGERED";
            if(command.approveFinal() && consumption!=null){
                var consumed=consumption.record(context,order.getId(),command.consumptionOperationKey(),command.actualConsumption(),null);
                if(consumed==null||consumed.isFailure())return KetQua.failure(consumed==null?"CONSUMPTION_FAILED":consumed.getError().map(e->e.code()).orElse("CONSUMPTION_FAILED"),
                        consumed==null?"Không ghi được tiêu hao vật tư.":consumed.getError().map(e->e.message()).orElse("Không ghi được tiêu hao vật tư."));
                consumptionStatus=consumed.getValue().map(v->v.status()).orElse("PENDING");
            }
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
            if (command.approveFinal()) ClsLuotKhamTrangThaiDongBo.restoreVisitToExaminingIfNoPendingCls(
                    order.getMaLuotKham(), context.employeeId(), serviceRepository, visitRepository);

            emitSuccessAudit(context, command.auditActorContext(), order.getId());

            RecordDichVuKetQuaBienNhan receipt = new RecordDichVuKetQuaBienNhan(
                    order.getId(),
                    order.getMaLuotKham(),
                    order.getTrangThai().name(),
                    context.employeeId(),
                    itemCount,
                    order.getThoiGianThucHien(),
                    order.getThoiGianTraKetQua(),
                    consumptionStatus);

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
