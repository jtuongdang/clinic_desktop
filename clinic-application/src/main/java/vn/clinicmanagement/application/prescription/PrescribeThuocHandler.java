package vn.clinicmanagement.application.prescription;

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
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.prescription.ChiTietDonThuoc;
import vn.clinicmanagement.domain.prescription.LamSangThuocAnToanDichVu;
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

public final class PrescribeThuocHandler
        implements CommandHandler<PrescribeThuocCommand, PrescribeThuocBienNhan> {
    private static final AtomicInteger SYNTHETIC_PRESCRIPTION_ID = new AtomicInteger(Integer.MAX_VALUE);

    /** Trường `prescriptionRepository`: đơn thuốc kho dữ liệu. */
    private final IDonThuocRepository prescriptionRepository;
    /** Trường `visitRepository`: luotkham kho dữ liệu. */
    private final ILuotKhamRepository visitRepository;
    /** Trường `patientRepository`: bệnh nhân kho dữ liệu. */
    private final IBenhNhanRepository patientRepository;
    /** Trường `inventoryRepository`: kho kho dữ liệu. */
    private final IKhoRepository inventoryRepository;
    /** Trường `drugSafetyService`: thuốc antoan dịch vụ. */
    private final LamSangThuocAnToanDichVu drugSafetyService;
    private final GiaoDichRanhGioi transactions;
    /** Trường `audit`: kiểm toán. */
    private final KiemToanWriteCongKetNoi audit;

    public PrescribeThuocHandler(
            IDonThuocRepository prescriptionRepository,
            ILuotKhamRepository visitRepository,
            IBenhNhanRepository patientRepository,
            IKhoRepository inventoryRepository,
            LamSangThuocAnToanDichVu drugSafetyService,
            GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi audit) {
        this.prescriptionRepository = Objects.requireNonNull(prescriptionRepository, "prescriptionRepository");
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.patientRepository = Objects.requireNonNull(patientRepository, "patientRepository");
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        this.drugSafetyService = Objects.requireNonNull(drugSafetyService, "drugSafetyService");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public KetQua<PrescribeThuocBienNhan> handle(PrescribeThuocCommand command) {
        if (command == null || command.visitId() <= 0 || command.doctorId() <= 0 || command.facilityId() <= 0) {
            return KetQua.failure("VALIDATION_ERROR", "Thông tin kê đơn không hợp lệ.");
        }
        if (command.items() == null || command.items().isEmpty()) {
            return KetQua.failure("VALIDATION_ERROR", "Đơn thuốc phải có ít nhất một loại thuốc.");
        }
        for (DonThuocMatHangDauVao item : command.items()) {
            if (item == null || item.itemId() <= 0 || item.soLuongKe() == null
                    || item.soLuongKe().compareTo(BigDecimal.ZERO) <= 0) {
                return KetQua.failure("VALIDATION_ERROR", "Chi tiết thuốc kê đơn không hợp lệ.");
            }
        }

        PhanQuyenNguCanh context = command.authorizationContext();
        if (context == null || !context.isValid() || !"DOCTOR".equals(context.roleCode())) {
            return deny(context, command.auditActorContext());
        }

        Optional<LuotKham> visitResult = visitRepository.getById(command.visitId());
        if (visitResult.isEmpty()) {
            return KetQua.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
        }

        LuotKham visit = visitResult.get();
        if (visit.getMaBacSi() != null && visit.getMaBacSi() != command.doctorId()) {
            return KetQua.failure("DOCTOR_MISMATCH", "Chỉ bác sĩ phụ trách mới có quyền kê đơn thuốc.");
        }
        if (visit.isEmrLocked()) {
            return KetQua.failure("EMR_LOCKED", "Hồ sơ bệnh án đã bị khóa không thể kê đơn thuốc.");
        }
        if (visit.getTrangThai() != TrangThaiLuotKham.DANG_KHAM) {
            return KetQua.failure("INVALID_STATE", "Chỉ có thể kê đơn khi lượt khám đang diễn ra.");
        }

        Optional<BenhNhan> patientResult = patientRepository.getById(visit.getMaBenhNhan());
        if (patientResult.isEmpty()) {
            return KetQua.failure("NOT_FOUND", "Không tìm thấy bệnh nhân.");
        }
        BenhNhan patient = patientResult.get();

        List<MatHang> resolvedItems = new ArrayList<>(command.items().size());
        for (DonThuocMatHangDauVao itemInput : command.items()) {
            Optional<MatHang> itemResult = inventoryRepository.getItemById(itemInput.itemId());
            if (itemResult.isEmpty()) {
                return KetQua.failure("NOT_FOUND", "Không tìm thấy thuốc.");
            }
            resolvedItems.add(itemResult.get());
        }

        List<LamSangThuocAnToanDichVu.SafetyWarning> warnings =
                drugSafetyService.evaluatePrescriptionSafety(patient, resolvedItems);
        if (!warnings.isEmpty() && !command.overrideAllergyWarning()) {
            return KetQua.failure("ALLERGY_WARNING", warnings.get(0).warningMessage());
        }

        return transactions.inTransaction(() -> createPrescription(command, context, visit, resolvedItems));
    }

    /** Hàm `createPrescription`: tạo đơn thuốc. */
    private KetQua<PrescribeThuocBienNhan> createPrescription(
            PrescribeThuocCommand command,
            PhanQuyenNguCanh context,
            LuotKham visit,
            List<MatHang> resolvedItems) {
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
            DonThuocMatHangDauVao itemInput = command.items().get(index);
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

        audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(
                context.employeeId(),
                context.roleId(),
                context.roleCode(),
                context.facilityId(),
                context.departmentId(),
                KiemToanHanhDongMa.RX_CREATED,
                KiemToanTaiNguyenLoai.PRESCRIPTION,
                prescription.getId(),
                KiemToanKetQua.SUCCESS,
                null,
                Instant.now(),
                command.auditActorContext() == null ? null : command.auditActorContext().workstationId(),
                command.auditActorContext() == null
                        ? context.sessionId()
                        : command.auditActorContext().correlationId()));

        return KetQua.success(new PrescribeThuocBienNhan(
                prescription.getId(),
                visit.getId(),
                details.size(),
                totalAmount,
                prescription.getTrangThai(),
                prescription.getNgayKe()));
    }

    private KetQua<PrescribeThuocBienNhan> deny(PhanQuyenNguCanh context, KiemToanTacNhanNguCanh actor) {
        boolean validContext = context != null && context.isValid();
        audit.appendIndependently(new HeThongKiemToanNhapLieu(
                validContext ? context.employeeId() : actor == null ? null : actor.employeeId(),
                validContext ? context.roleId() : actor == null ? null : actor.roleId(),
                validContext ? context.roleCode() : actor == null ? null : actor.roleCode(),
                validContext ? context.facilityId() : actor == null ? null : actor.facilityId(),
                validContext ? context.departmentId() : actor == null ? null : actor.departmentId(),
                KiemToanHanhDongMa.AUTH_ACCESS_DENIED,
                null,
                null,
                KiemToanKetQua.DENIED,
                KiemToanLyDoMa.AUTH_FORBIDDEN,
                Instant.now(),
                actor == null ? null : actor.workstationId(),
                actor == null ? UUID.randomUUID() : actor.correlationId()));
        return KetQua.failure("AUTH_ACCESS_DENIED", "Không được phép kê đơn thuốc trong phiên hiện tại.");
    }
}
