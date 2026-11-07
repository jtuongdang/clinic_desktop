package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.billing.ChiTietHoaDon;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.billing.PhieuThu;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class GetInvoiceByVisitHandler implements QueryHandler<GetInvoiceByVisitQuery, InvoiceDto> {

    private static final Set<String> ALLOWED_ROLES = Set.of("CASHIER", "ADMIN", "DOCTOR", "RECEPTIONIST");

    private final IHoaDonRepository invoiceRepository;
    private final AuditWritePort auditWritePort;

    public GetInvoiceByVisitHandler(IHoaDonRepository invoiceRepository, AuditWritePort auditWritePort) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "invoiceRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public Result<InvoiceDto> handle(GetInvoiceByVisitQuery query) {
        if (query == null) {
            return Result.failure("VALIDATION_ERROR", "Truy vấn hóa đơn không được để trống.");
        }

        // 1. Phân quyền
        AuthorizationContext auth = query.authorizationContext();
        if (auth == null || !isRoleAllowed(auth.roleCode())) {
            logAudit(query.auditActorContext(), 0, AuditOutcome.DENIED, AuditReasonCode.AUTH_FORBIDDEN);
            return Result.failure("AUTHORIZATION_DENIED", "Tài khoản không có quyền xem thông tin hóa đơn.");
        }

        if (query.visitId() <= 0) {
            return Result.failure("INVALID_VISIT", "Mã lượt khám không hợp lệ.");
        }

        // 2. Tra cứu hóa đơn
        Optional<HoaDon> invoiceOpt = invoiceRepository.getByLuotKham(query.visitId());
        if (invoiceOpt.isEmpty()) {
            return Result.failure("INVOICE_NOT_FOUND", "Không tìm thấy hóa đơn cho lượt khám: " + query.visitId());
        }

        HoaDon hoaDon = invoiceOpt.get();
        int invoiceId = hoaDon.getId() != null ? hoaDon.getId() : 0;

        // 3. Tra cứu danh sách phiếu thu
        List<PhieuThu> receipts = invoiceRepository.getPhieuThuByHoaDon(invoiceId);
        List<ReceiptDto> receiptDtos = new ArrayList<>();
        if (receipts != null) {
            for (PhieuThu pt : receipts) {
                receiptDtos.add(new ReceiptDto(
                        pt.getId() != null ? pt.getId() : 0,
                        pt.getMaHoaDon(),
                        pt.getSoTien(),
                        pt.getNgayThu(),
                        pt.getPhuongThuc(),
                        pt.getNguoiThu(),
                        pt.getGhiChu()
                ));
            }
        }

        // 4. Chi tiết các dòng hóa đơn
        List<InvoiceLineDto> lineDtos = new ArrayList<>();
        if (hoaDon.getChiTietHoaDons() != null) {
            for (ChiTietHoaDon ct : hoaDon.getChiTietHoaDons()) {
                BigDecimal total = (ct.getSoLuong() != null && ct.getDonGia() != null)
                        ? ct.getSoLuong().multiply(ct.getDonGia())
                        : BigDecimal.ZERO;
                lineDtos.add(new InvoiceLineDto(
                        ct.getId() != null ? ct.getId() : 0,
                        ct.getLoaiDoiTuong(),
                        ct.getMaDoiTuong(),
                        ct.getMoTa(),
                        ct.getSoLuong(),
                        ct.getDonGia(),
                        total
                ));
            }
        }

        BigDecimal remaining = hoaDon.getSoTienBenhNhanChiTra().subtract(hoaDon.getDaThanhToan());
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        InvoiceDto dto = new InvoiceDto(
                invoiceId,
                hoaDon.getSoHoaDon(),
                hoaDon.getMaLuotKham(),
                hoaDon.getMaBenhNhan(),
                hoaDon.getMaCoSo(),
                hoaDon.getNgayLap(),
                hoaDon.getTongTien(),
                hoaDon.getSoTienBaoHiemChiTra(),
                hoaDon.getSoTienTamUng(),
                hoaDon.getSoTienGiamGia(),
                hoaDon.getChietKhauPhanTram(),
                hoaDon.getLyDoGiamGia(),
                hoaDon.getSoTienBenhNhanChiTra(),
                hoaDon.getDaThanhToan(),
                remaining,
                hoaDon.getTrangThai(),
                hoaDon.getGhiChu(),
                lineDtos,
                receiptDtos
        );

        logAudit(query.auditActorContext(), invoiceId, AuditOutcome.SUCCESS, null);
        return Result.success(dto);
    }

    private boolean isRoleAllowed(String roleCode) {
        if (roleCode == null) return false;
        return ALLOWED_ROLES.contains(roleCode.trim().toUpperCase(Locale.ROOT));
    }

    private void logAudit(AuditActorContext actor, int invoiceId, AuditOutcome outcome, AuditReasonCode reason) {
        if (actor == null) return;
        AuditResourceType resourceType = invoiceId > 0 ? AuditResourceType.INVOICE : null;
        Integer resourceId = invoiceId > 0 ? invoiceId : null;
        auditWritePort.appendIndependently(new SystemAuditEntry(
                actor.employeeId(),
                actor.roleId(),
                actor.roleCode() != null ? actor.roleCode().trim().toUpperCase(Locale.ROOT) : null,
                actor.facilityId(),
                actor.departmentId(),
                AuditActionCode.CATALOG_VIEW,
                resourceType,
                resourceId,
                outcome,
                reason,
                Instant.now(),
                actor.workstationId(),
                actor.correlationId()
        ));
    }
}
