package vn.clinicmanagement.application.prescription;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.inventory.Item;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.prescription.ChiTietDonThuoc;
import vn.clinicmanagement.domain.prescription.ClinicalDrugSafetyService;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;
import vn.clinicmanagement.domain.repositories.IDonThuocRepository;
import vn.clinicmanagement.domain.repositories.IKhoRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class PrescribeMedicationHandler
        implements CommandHandler<PrescribeMedicationCommand, PrescribeMedicationReceipt> {
    private static final AtomicInteger SYNTHETIC_PRESCRIPTION_ID = new AtomicInteger(Integer.MAX_VALUE);

    private final IDonThuocRepository prescriptionRepository;
    private final ILuotKhamRepository visitRepository;
    private final IBenhNhanRepository patientRepository;
    private final IKhoRepository inventoryRepository;
    private final ClinicalDrugSafetyService drugSafetyService;
    private final TransactionBoundary transactions;
    private final AuditWritePort audit;

    public PrescribeMedicationHandler(
            IDonThuocRepository prescriptionRepository,
            ILuotKhamRepository visitRepository,
            IBenhNhanRepository patientRepository,
            IKhoRepository inventoryRepository,
            ClinicalDrugSafetyService drugSafetyService,
            TransactionBoundary transactions,
            AuditWritePort audit) {
        this.prescriptionRepository = Objects.requireNonNull(prescriptionRepository, "prescriptionRepository");
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.patientRepository = Objects.requireNonNull(patientRepository, "patientRepository");
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        this.drugSafetyService = Objects.requireNonNull(drugSafetyService, "drugSafetyService");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public Result<PrescribeMedicationReceipt> handle(PrescribeMedicationCommand command) {
        if (command == null || command.visitId() <= 0 || command.doctorId() <= 0 || command.facilityId() <= 0) {
            return Result.failure("VALIDATION_ERROR", "Thông tin kê đơn không hợp lệ.");
        }
        if (command.items() == null || command.items().isEmpty()) {
            return Result.failure("VALIDATION_ERROR", "Đơn thuốc phải có ít nhất một loại thuốc.");
        }
        for (PrescriptionItemInput item : command.items()) {
            if (item == null || item.itemId() <= 0 || item.soLuongKe() == null
                    || item.soLuongKe().compareTo(BigDecimal.ZERO) <= 0) {
                return Result.failure("VALIDATION_ERROR", "Chi tiết thuốc kê đơn không hợp lệ.");
            }
        }

        AuthorizationContext context = command.authorizationContext();
        if (context == null || !context.isValid() || !"DOCTOR".equals(context.roleCode())) {
            return deny(context, command.auditActorContext());
        }

        Optional<LuotKham> visitResult = visitRepository.getById(command.visitId());
        if (visitResult.isEmpty()) {
            return Result.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
        }

        LuotKham visit = visitResult.get();
        if (visit.getMaBacSi() != null && visit.getMaBacSi() != command.doctorId()) {
            return Result.failure("DOCTOR_MISMATCH", "Chỉ bác sĩ phụ trách mới có quyền kê đơn thuốc.");
        }
        if (visit.isEmrLocked()) {
            return Result.failure("EMR_LOCKED", "Hồ sơ bệnh án đã bị khóa không thể kê đơn thuốc.");
        }
        if (visit.getTrangThai() != TrangThaiLuotKham.DANG_KHAM) {
            return Result.failure("INVALID_STATE", "Chỉ có thể kê đơn khi lượt khám đang diễn ra.");
        }

        Optional<BenhNhan> patientResult = patientRepository.getById(visit.getMaBenhNhan());
        if (patientResult.isEmpty()) {
            return Result.failure("NOT_FOUND", "Không tìm thấy bệnh nhân.");
        }
        BenhNhan patient = patientResult.get();

        List<Item> resolvedItems = new ArrayList<>(command.items().size());
        for (PrescriptionItemInput itemInput : command.items()) {
            Optional<Item> itemResult = inventoryRepository.getItemById(itemInput.itemId());
            if (itemResult.isEmpty()) {
                return Result.failure("NOT_FOUND", "Không tìm thấy thuốc.");
            }
            resolvedItems.add(itemResult.get());
        }

        List<ClinicalDrugSafetyService.SafetyWarning> warnings =
                drugSafetyService.evaluatePrescriptionSafety(patient, resolvedItems);
        if (!warnings.isEmpty() && !command.overrideAllergyWarning()) {
            return Result.failure("ALLERGY_WARNING", warnings.get(0).warningMessage());
        }

        return transactions.inTransaction(() -> createPrescription(command, context, visit, resolvedItems));
    }

    private Result<PrescribeMedicationReceipt> createPrescription(
            PrescribeMedicationCommand command,
            AuthorizationContext context,
            LuotKham visit,
            List<Item> resolvedItems) {
        DonThuoc prescription = new DonThuoc();
        prescription.setMaLuotKham(visit.getId());
        prescription.setMaBacSi(command.doctorId());
        prescription.setMaCoSo(command.facilityId());
        prescription.setMaKhoXuat(command.warehouseId());
        prescription.setGhiChu(command.ghiChu() != null ? command.ghiChu() : "");
        prescription.setLoiDanDonThuoc(command.loiDanDonThuoc() != null ? command.loiDanDonThuoc() : "");
        prescription.setTrangThai(TrangThaiDonThuoc.TAM);
        prescription.setNgayKe(LocalDateTime.now());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<ChiTietDonThuoc> details = new ArrayList<>(command.items().size());
        for (int index = 0; index < command.items().size(); index++) {
            PrescriptionItemInput itemInput = command.items().get(index);
            BigDecimal unitPrice = itemInput.donGiaKe() != null ? itemInput.donGiaKe() : BigDecimal.ZERO;
            ChiTietDonThuoc detail = new ChiTietDonThuoc();
            detail.setMaItem(itemInput.itemId());
            detail.setSoLuongKe(itemInput.soLuongKe());
            detail.setDonGiaKe(unitPrice);
            detail.setLieuDung(itemInput.lieuDung() != null ? itemInput.lieuDung() : "");
            detail.setDuongDung(itemInput.duongDung() != null ? itemInput.duongDung() : "Uống");
            detail.setTanSuat(itemInput.tanSuat() != null ? itemInput.tanSuat() : "");
            detail.setSoNgay(itemInput.soNgay());
            if (itemInput.sang() != null) detail.setSang(itemInput.sang());
            if (itemInput.trua() != null) detail.setTrua(itemInput.trua());
            if (itemInput.chieu() != null) detail.setChieu(itemInput.chieu());
            if (itemInput.toi() != null) detail.setToi(itemInput.toi());
            detail.setHuongDan(itemInput.huongDan() != null ? itemInput.huongDan() : "");
            detail.setDonThuoc(prescription);
            detail.setItem(resolvedItems.get(index));
            details.add(detail);
            totalAmount = totalAmount.add(itemInput.soLuongKe().multiply(unitPrice));
        }
        prescription.setChiTietDonThuocs(details);

        prescriptionRepository.add(prescription);
        if (prescription.getId() == null) {
            prescription.setId(SYNTHETIC_PRESCRIPTION_ID.getAndUpdate(
                    current -> current > 1 ? current - 1 : Integer.MAX_VALUE));
        }

        audit.appendInCurrentTransaction(new SystemAuditEntry(
                context.employeeId(),
                context.roleId(),
                context.roleCode(),
                context.facilityId(),
                context.departmentId(),
                AuditActionCode.RX_CREATED,
                AuditResourceType.PRESCRIPTION,
                prescription.getId(),
                AuditOutcome.SUCCESS,
                null,
                Instant.now(),
                command.auditActorContext() == null ? null : command.auditActorContext().workstationId(),
                command.auditActorContext() == null
                        ? context.sessionId()
                        : command.auditActorContext().correlationId()));

        return Result.success(new PrescribeMedicationReceipt(
                prescription.getId(),
                visit.getId(),
                details.size(),
                totalAmount,
                prescription.getTrangThai(),
                prescription.getNgayKe()));
    }

    private Result<PrescribeMedicationReceipt> deny(AuthorizationContext context, AuditActorContext actor) {
        boolean validContext = context != null && context.isValid();
        audit.appendIndependently(new SystemAuditEntry(
                validContext ? context.employeeId() : actor == null ? null : actor.employeeId(),
                validContext ? context.roleId() : actor == null ? null : actor.roleId(),
                validContext ? context.roleCode() : actor == null ? null : actor.roleCode(),
                validContext ? context.facilityId() : actor == null ? null : actor.facilityId(),
                validContext ? context.departmentId() : actor == null ? null : actor.departmentId(),
                AuditActionCode.AUTH_ACCESS_DENIED,
                null,
                null,
                AuditOutcome.DENIED,
                AuditReasonCode.AUTH_FORBIDDEN,
                Instant.now(),
                actor == null ? null : actor.workstationId(),
                actor == null ? UUID.randomUUID() : actor.correlationId()));
        return Result.failure("AUTH_ACCESS_DENIED", "Không được phép kê đơn thuốc trong phiên hiện tại.");
    }
}
