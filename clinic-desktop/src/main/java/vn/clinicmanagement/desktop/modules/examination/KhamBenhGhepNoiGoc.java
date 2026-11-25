package vn.clinicmanagement.desktop.modules.examination;

import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.cls.cancel.CancelDichVuYeuCauHandler;
import vn.clinicmanagement.application.cls.get.GetDichVuTheoLuotKhamHandler;
import vn.clinicmanagement.application.cls.request.YeuCauDichVuHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.application.examination.add.AddChanDoanCommand;
import vn.clinicmanagement.application.examination.add.AddChanDoanHandler;
import vn.clinicmanagement.application.examination.complete.CompleteKhamCommand;
import vn.clinicmanagement.application.examination.complete.CompleteKhamHandler;
import vn.clinicmanagement.application.examination.lock.KhoaEMRCommand;
import vn.clinicmanagement.application.examination.lock.KhoaEMRHandler;
import vn.clinicmanagement.application.examination.record.RecordSinhHieuDauHieuHandler;
import vn.clinicmanagement.application.examination.KhamBenhHangDoiCongKetNoi;
import vn.clinicmanagement.application.examination.KhamBenhHangDoiMatHang;
import vn.clinicmanagement.application.examination.KhamBenhLichSuMatHang;
import vn.clinicmanagement.application.examination.start.StartKhamCommand;
import vn.clinicmanagement.application.examination.start.StartKhamHandler;
import vn.clinicmanagement.application.examination.transfer.TransferLuotKhamCommand;
import vn.clinicmanagement.application.examination.cancel.CancelLuotKhamHandler;
import vn.clinicmanagement.application.examination.transfer.TransferLuotKhamHandler;
import vn.clinicmanagement.application.examination.update.UpdateEMRLamSangRecordCommand;
import vn.clinicmanagement.application.examination.update.UpdateEMRLamSangRecordHandler;
import vn.clinicmanagement.application.prescription.cancel.CancelDonThuocHandler;
import vn.clinicmanagement.application.prescription.get.GetDonThuocTheoLuotKhamHandler;
import vn.clinicmanagement.application.prescription.prescribe.PrescribeThuocBienNhan;
import vn.clinicmanagement.application.prescription.prescribe.PrescribeThuocCommand;
import vn.clinicmanagement.application.prescription.prescribe.PrescribeThuocHandler;
import vn.clinicmanagement.desktop.modules.billing.VienPhiGhepNoiGoc;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.common.enums.HuongXuTri;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import vn.clinicmanagement.domain.examination.ICD;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.inventory.DonDatHang;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.PhieuNhapKho;
import vn.clinicmanagement.domain.inventory.PhieuXuatKho;
import vn.clinicmanagement.domain.inventory.KiemKe;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.prescription.LamSangThuocAnToanDichVu;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;
import vn.clinicmanagement.domain.repositories.IDonThuocRepository;
import vn.clinicmanagement.domain.repositories.IKhoRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;
import vn.clinicmanagement.domain.repositories.ITieuChuan;
import vn.clinicmanagement.domain.repositories.PhanTrangKetQua;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiTietKetQuaXetNghiem;
import vn.clinicmanagement.domain.service.DanhMucDichVu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Composition root cho phân hệ Khám Chữa Bệnh Ngoại Trú & Bác Sĩ (Examination).
 * Khởi tạo và lắp ráp 11 Clean Architecture Handlers:
 * - StartExamHandler, CompleteExamHandler, AddDiagnosisHandler, UpdateEmrClinicalRecordHandler, TransferVisitHandler
 * - PrescribeMedicationHandler, GetPrescriptionByVisitHandler, CancelPrescriptionHandler
 * - OrderServiceHandler, GetServicesByVisitHandler, CancelServiceOrderHandler
 */
public final class KhamBenhGhepNoiGoc {

    private static final KiemToanWriteCongKetNoi FALLBACK_AUDIT = new KiemToanWriteCongKetNoi() {
        @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
    };

    private static final GiaoDichRanhGioi DIRECT_TRANSACTIONS = new GiaoDichRanhGioi() {
        @Override
        public <T> KetQua<T> inTransaction(Supplier<KetQua<T>> operation) {
            try {
                return operation.get();
            } catch (Exception ex) {
                return KetQua.failure("TX_ERROR", ex.getMessage());
            }
        }
    };

    private static final IUnitOfWork NOOP_UOW = new IUnitOfWork() {
        @Override public int saveChanges() { return 1; }
        @Override public boolean saveEntities() { return true; }
    };

    public record Services(
            StartKhamHandler startExam,
            CompleteKhamHandler completeExam,
            AddChanDoanHandler addDiagnosis,
            PrescribeThuocHandler prescribeMedication,
            GetDonThuocTheoLuotKhamHandler getPrescriptionByVisit,
            CancelDonThuocHandler cancelPrescription,
            YeuCauDichVuHandler orderService,
            GetDichVuTheoLuotKhamHandler getServicesByVisit,
            CancelDichVuYeuCauHandler cancelServiceOrder,
            UpdateEMRLamSangRecordHandler updateEmrClinicalRecord,
            TransferLuotKhamHandler transferVisit,
            KhamBenhHangDoiCongKetNoi examQueue,
            IKhoRepository inventoryRepository,
            KhoaEMRHandler lockEmr,
            RecordSinhHieuDauHieuHandler recordVitalSigns,
            CancelLuotKhamHandler cancelVisit) {

        public Services(
                StartKhamHandler startExam,
                CompleteKhamHandler completeExam,
                AddChanDoanHandler addDiagnosis,
                PrescribeThuocHandler prescribeMedication,
                GetDonThuocTheoLuotKhamHandler getPrescriptionByVisit,
                CancelDonThuocHandler cancelPrescription,
                YeuCauDichVuHandler orderService,
                GetDichVuTheoLuotKhamHandler getServicesByVisit,
                CancelDichVuYeuCauHandler cancelServiceOrder,
                UpdateEMRLamSangRecordHandler updateEmrClinicalRecord,
                TransferLuotKhamHandler transferVisit) {
            this(startExam, completeExam, addDiagnosis, prescribeMedication, getPrescriptionByVisit,
                    cancelPrescription, orderService, getServicesByVisit, cancelServiceOrder,
                    updateEmrClinicalRecord, transferVisit, new InMemoryExamQueuePort(), new InMemoryInventoryRepository(), null, null, null);
        }

        public Services(
                StartKhamHandler startExam,
                CompleteKhamHandler completeExam,
                AddChanDoanHandler addDiagnosis,
                PrescribeThuocHandler prescribeMedication,
                GetDonThuocTheoLuotKhamHandler getPrescriptionByVisit,
                CancelDonThuocHandler cancelPrescription,
                YeuCauDichVuHandler orderService,
                GetDichVuTheoLuotKhamHandler getServicesByVisit,
                CancelDichVuYeuCauHandler cancelServiceOrder,
                UpdateEMRLamSangRecordHandler updateEmrClinicalRecord,
                TransferLuotKhamHandler transferVisit,
                KhamBenhHangDoiCongKetNoi examQueue) {
        this(startExam, completeExam, addDiagnosis, prescribeMedication, getPrescriptionByVisit,
                    cancelPrescription, orderService, getServicesByVisit, cancelServiceOrder,
                    updateEmrClinicalRecord, transferVisit, examQueue, new InMemoryInventoryRepository(), null, null, null);
        }

        public Services(
                StartKhamHandler startExam,
                CompleteKhamHandler completeExam,
                AddChanDoanHandler addDiagnosis,
                PrescribeThuocHandler prescribeMedication,
                GetDonThuocTheoLuotKhamHandler getPrescriptionByVisit,
                CancelDonThuocHandler cancelPrescription,
                YeuCauDichVuHandler orderService,
                GetDichVuTheoLuotKhamHandler getServicesByVisit,
                CancelDichVuYeuCauHandler cancelServiceOrder,
                UpdateEMRLamSangRecordHandler updateEmrClinicalRecord,
                TransferLuotKhamHandler transferVisit,
                KhamBenhHangDoiCongKetNoi examQueue,
                IKhoRepository inventoryRepository) {
            this(startExam, completeExam, addDiagnosis, prescribeMedication, getPrescriptionByVisit,
                    cancelPrescription, orderService, getServicesByVisit, cancelServiceOrder,
                    updateEmrClinicalRecord, transferVisit, examQueue, inventoryRepository, null, null, null);
        }

        public Services(
                StartKhamHandler startExam,
                CompleteKhamHandler completeExam,
                AddChanDoanHandler addDiagnosis,
                PrescribeThuocHandler prescribeMedication,
                GetDonThuocTheoLuotKhamHandler getPrescriptionByVisit,
                CancelDonThuocHandler cancelPrescription,
                YeuCauDichVuHandler orderService,
                GetDichVuTheoLuotKhamHandler getServicesByVisit,
                CancelDichVuYeuCauHandler cancelServiceOrder,
                UpdateEMRLamSangRecordHandler updateEmrClinicalRecord,
                TransferLuotKhamHandler transferVisit,
                KhamBenhHangDoiCongKetNoi examQueue,
                IKhoRepository inventoryRepository,
                KhoaEMRHandler lockEmr) {
            this(startExam, completeExam, addDiagnosis, prescribeMedication, getPrescriptionByVisit,
                    cancelPrescription, orderService, getServicesByVisit, cancelServiceOrder,
                    updateEmrClinicalRecord, transferVisit, examQueue, inventoryRepository, lockEmr, null, null);
        }

        public Services(
                StartKhamHandler startExam,
                CompleteKhamHandler completeExam,
                AddChanDoanHandler addDiagnosis,
                PrescribeThuocHandler prescribeMedication,
                GetDonThuocTheoLuotKhamHandler getPrescriptionByVisit,
                CancelDonThuocHandler cancelPrescription,
                YeuCauDichVuHandler orderService,
                GetDichVuTheoLuotKhamHandler getServicesByVisit,
                CancelDichVuYeuCauHandler cancelServiceOrder,
                UpdateEMRLamSangRecordHandler updateEmrClinicalRecord,
                TransferLuotKhamHandler transferVisit,
                KhamBenhHangDoiCongKetNoi examQueue,
                IKhoRepository inventoryRepository,
                KhoaEMRHandler lockEmr,
                RecordSinhHieuDauHieuHandler recordVitalSigns) {
            this(startExam, completeExam, addDiagnosis, prescribeMedication, getPrescriptionByVisit,
                    cancelPrescription, orderService, getServicesByVisit, cancelServiceOrder,
                    updateEmrClinicalRecord, transferVisit, examQueue, inventoryRepository, lockEmr, recordVitalSigns, null);
        }

        public Services {
            Objects.requireNonNull(startExam, "startExam");
            Objects.requireNonNull(completeExam, "completeExam");
            Objects.requireNonNull(addDiagnosis, "addDiagnosis");
            Objects.requireNonNull(prescribeMedication, "prescribeMedication");
            Objects.requireNonNull(getPrescriptionByVisit, "getPrescriptionByVisit");
            Objects.requireNonNull(cancelPrescription, "cancelPrescription");
            Objects.requireNonNull(orderService, "orderService");
            Objects.requireNonNull(getServicesByVisit, "getServicesByVisit");
            Objects.requireNonNull(cancelServiceOrder, "cancelServiceOrder");
            Objects.requireNonNull(updateEmrClinicalRecord, "updateEmrClinicalRecord");
            Objects.requireNonNull(transferVisit, "transferVisit");
            Objects.requireNonNull(examQueue, "examQueue");
            inventoryRepository = inventoryRepository != null ? inventoryRepository : new InMemoryInventoryRepository();
        }
    }

    /**
     * Kê đơn thuốc / phiếu vật tư từ màn hình khám bệnh.
     * Nếu lượt khám đang ở trạng thái `CHO_KHAM` (chưa bấm Bắt đầu khám), tự động kích hoạt `startExam`
     * rồi thực hiện kê đơn trong một luồng thống nhất.
     */
    public static KetQua<PrescribeThuocBienNhan> prescribeWithAutoStart(
            Services services,
            PrescribeThuocCommand cmd) {
        return prescribeWithAutoStart(services, cmd, null, null, null, null);
    }

    /**
     * Kê đơn thuốc / phiếu vật tư từ màn hình khám bệnh; tự kích hoạt lượt khám `CHO_KHAM`
     * và lưu chẩn đoán ICD mới vào `ChanDoan` (nếu bác sĩ vừa chọn/cập nhật chẩn đoán).
     */
    public static KetQua<PrescribeThuocBienNhan> prescribeWithAutoStart(
            Services services,
            PrescribeThuocCommand cmd,
            Integer icdId,
            String diagnosisText,
            String diagnosisDisplay,
            String previousDiagnosis) {
        KetQua<PrescribeThuocBienNhan> res = services.prescribeMedication().handle(cmd);
        if (res != null && !res.isSuccess()
                && "INVALID_STATE".equals(res.getErrorCode().orElse(null))
                && services.startExam() != null
                && cmd.authorizationContext() != null) {
            var started = services.startExam().handle(
                    new StartKhamCommand(cmd.visitId(), cmd.doctorId(), cmd.authorizationContext()));
            if (started != null && started.isSuccess()) {
                res = services.prescribeMedication().handle(cmd);
            }
        }
        if (res != null && res.isSuccess()
                && services.addDiagnosis() != null
                && cmd.authorizationContext() != null
                && diagnosisText != null && !diagnosisText.isBlank()) {
            String prev = previousDiagnosis == null ? "" : previousDiagnosis.trim();
            String curr = diagnosisDisplay == null ? diagnosisText.trim() : diagnosisDisplay.trim();
            if (!curr.equalsIgnoreCase(prev)) {
                services.addDiagnosis().handle(new AddChanDoanCommand(
                        cmd.visitId(),
                        cmd.doctorId(),
                        icdId,
                        diagnosisText.trim(),
                        true,
                        "XAC_DINH",
                        cmd.authorizationContext()));
            }
        }
        return res;
    }

    /**
     * Lưu xử trí tại tab Xử Trí: tự kích hoạt lượt khám `CHO_KHAM`, lưu chẩn đoán xác định
     * vào `ChanDoan` và cập nhật kết quả/hướng xử trí/lời dặn/ngày hẹn tái khám qua `CompleteKhamHandler`.
     */
    public static String completeDispositionWithAutoStart(
            Services services,
            AddChanDoanCommand diagCmd,
            CompleteKhamCommand completeCmd,
            boolean isCompleteExam) {
        return completeDispositionWithAutoStart(services, diagCmd, List.of(), null, completeCmd, null, isCompleteExam);
    }

    /**
     * Lưu toàn bộ dữ liệu tab Xử Trí:
     * 1) Tự kích hoạt `CHO_KHAM` nếu cần,
     * 2) Lưu chẩn đoán chính + danh sách bệnh kèm theo (`KEM_THEO`),
     * 3) Lưu triệu chứng & khám lâm sàng qua `UpdateEMRLamSangRecordHandler`,
     * 4) Chuyển phòng khám (`TransferLuotKhamCommand`) hoặc hoàn tất / hủy bỏ khám (`CompleteKhamCommand`).
     */
    public static String completeDispositionWithAutoStart(
            Services services,
            AddChanDoanCommand diagCmd,
            List<AddChanDoanCommand> secondaryDiagCmds,
            UpdateEMRLamSangRecordCommand clinicalCmd,
            CompleteKhamCommand completeCmd,
            TransferLuotKhamCommand transferCmd,
            boolean isCompleteExam) {
        if (services == null || diagCmd == null) return "Chưa kết nối dịch vụ xử trí.";
        if (services.addDiagnosis() == null) return "Chưa kết nối lưu chẩn đoán.";

        var diagnosis = services.addDiagnosis().handle(diagCmd);
        if (diagnosis != null && !diagnosis.isSuccess()
                && "INVALID_STATE".equals(diagnosis.getErrorCode().orElse(null))
                && services.startExam() != null
                && diagCmd.authorizationContext() != null) {
            var started = services.startExam().handle(
                    new StartKhamCommand(diagCmd.visitId(), diagCmd.doctorId(), diagCmd.authorizationContext()));
            if (started != null && started.isSuccess()) {
                diagnosis = services.addDiagnosis().handle(diagCmd);
            }
        }
        if (diagnosis == null || !diagnosis.isSuccess()) {
            return diagnosis == null ? "Không nhận được kết quả lưu chẩn đoán."
                    : diagnosis.getError().map(error -> error.message()).orElse("Không thể lưu chẩn đoán.");
        }
        if (secondaryDiagCmds != null) {
            for (AddChanDoanCommand secCmd : secondaryDiagCmds) {
                if (secCmd == null) continue;
                var secRes = services.addDiagnosis().handle(secCmd);
                if (secRes == null || !secRes.isSuccess()) {
                    return secRes == null ? "Không nhận được kết quả lưu bệnh kèm theo."
                            : secRes.getError().map(error -> error.message()).orElse("Không thể lưu bệnh kèm theo.");
                }
            }
        }
        if (clinicalCmd != null && services.updateEmrClinicalRecord() != null) {
            var emrRes = services.updateEmrClinicalRecord().handle(clinicalCmd);
            if (emrRes == null || !emrRes.isSuccess()) {
                return emrRes == null ? "Không nhận được kết quả lưu thông tin lâm sàng."
                        : emrRes.getError().map(error -> error.message()).orElse("Không thể lưu triệu chứng / khám lâm sàng.");
            }
        }
        if (transferCmd != null) {
            if (services.transferVisit() == null) return "Chưa kết nối chuyển phòng khám.";
            var trRes = services.transferVisit().handle(transferCmd);
            if (trRes == null || !trRes.isSuccess()) {
                return trRes == null ? "Không nhận được kết quả chuyển phòng khám."
                        : trRes.getError().map(error -> error.message()).orElse("Không thể chuyển phòng khám.");
            }
            return null;
        }
        if (isCompleteExam && completeCmd != null) {
            if (services.completeExam() == null) return "Chưa kết nối kết thúc khám.";
            var completed = services.completeExam().handle(completeCmd);
            if (completed == null || !completed.isSuccess()) {
                return completed == null ? "Không nhận được kết quả kết thúc khám."
                        : completed.getError().map(error -> error.message()).orElse("Không thể kết thúc khám.");
            }
            if (completeCmd.huongXuTri() != HuongXuTri.BO_KHAM
                    && services.lockEmr() != null
                    && completeCmd.authorizationContext() != null) {
                PhanQuyenNguCanh auth = completeCmd.authorizationContext();
                KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(
                        auth.employeeId(),
                        auth.roleId(),
                        auth.roleCode(),
                        auth.facilityId(),
                        auth.departmentId(),
                        "DESKTOP-HIS",
                        UUID.randomUUID());
                String signature = "BS#" + completeCmd.doctorId();
                var locked = services.lockEmr().handle(new KhoaEMRCommand(
                        completeCmd.visitId(),
                        completeCmd.doctorId(),
                        signature,
                        auth,
                        actor));
                if (locked == null || (!locked.isSuccess()
                        && !"EMR_ALREADY_LOCKED".equals(locked.getErrorCode().orElse(null)))) {
                    return locked == null ? "Không nhận được kết quả khóa bệnh án."
                            : locked.getError().map(error -> error.message()).orElse("Đã lưu xử trí nhưng không thể khóa bệnh án.");
                }
            }
        }
        return null;
    }

    /** Lắp ráp các repository JDBC cho màn hình khám bệnh trong phiên sử dụng thật. */
    public static Services createProduction() {
        var unitOfWork = new vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork();
        var transactions = new vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy(unitOfWork);
        var connections = new vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap(transactions);
        var audit = new vn.clinicmanagement.infrastructure.persistence.system.JdbcKiemToanRepository(
                connections, unitOfWork,
                vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi::getConnection);
        var invoices = new vn.clinicmanagement.infrastructure.persistence.billing.HoaDonRepository(connections, unitOfWork);
        return create(
                new vn.clinicmanagement.infrastructure.persistence.examination.LuotKhamRepository(connections, unitOfWork),
                new vn.clinicmanagement.infrastructure.persistence.facility.PhongKhamRepository(connections),
                new vn.clinicmanagement.infrastructure.persistence.patient.BenhNhanRepository(connections, unitOfWork),
                new vn.clinicmanagement.infrastructure.persistence.prescription.DonThuocRepository(connections, unitOfWork),
                new vn.clinicmanagement.infrastructure.persistence.service.ChiDinhDichVuRepository(connections, unitOfWork),
                new vn.clinicmanagement.infrastructure.persistence.inventory.KhoRepository(connections, unitOfWork),
                new LamSangThuocAnToanDichVu(), transactions, audit,
                new vn.clinicmanagement.infrastructure.persistence.examination.KhamBenhHangDoiRepository(connections),
                new vn.clinicmanagement.application.billing.ClsVienPhiDongBo(invoices),
                new vn.clinicmanagement.application.billing.DonThuocVienPhiDongBo(invoices));
    }

    public static Services createDefault() {
        ILuotKhamRepository visitRepo = new VienPhiGhepNoiGoc.InMemoryVisitRepository();
        IPhongKhamRepository roomRepo = new InMemoryRoomRepository();
        IBenhNhanRepository patientRepo = new InMemoryPatientRepository();
        IDonThuocRepository prescriptionRepo = new InMemoryPrescriptionRepository();
        IChiDinhDichVuRepository serviceRepo = new InMemoryServiceOrderRepository();
        IKhoRepository inventoryRepo = new InMemoryInventoryRepository();
        LamSangThuocAnToanDichVu drugSafety = new LamSangThuocAnToanDichVu();

        return create(
                visitRepo,
                roomRepo,
                patientRepo,
                prescriptionRepo,
                serviceRepo,
                inventoryRepo,
                drugSafety,
                DIRECT_TRANSACTIONS,
                FALLBACK_AUDIT,
                new InMemoryExamQueuePort(visitRepo, roomRepo)
        );
    }

    public static Services create(
            ILuotKhamRepository visitRepository,
            IPhongKhamRepository roomRepository,
            IBenhNhanRepository patientRepository,
            IDonThuocRepository prescriptionRepository,
            IChiDinhDichVuRepository serviceRepository,
            IKhoRepository inventoryRepository,
            LamSangThuocAnToanDichVu drugSafetyService,
            GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi audit) {
        return create(visitRepository, roomRepository, patientRepository, prescriptionRepository,
                serviceRepository, inventoryRepository, drugSafetyService, transactions, audit,
                new InMemoryExamQueuePort(visitRepository, roomRepository));
    }

    public static Services create(
            ILuotKhamRepository visitRepository,
            IPhongKhamRepository roomRepository,
            IBenhNhanRepository patientRepository,
            IDonThuocRepository prescriptionRepository,
            IChiDinhDichVuRepository serviceRepository,
            IKhoRepository inventoryRepository,
            LamSangThuocAnToanDichVu drugSafetyService,
            GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi audit,
            KhamBenhHangDoiCongKetNoi examQueue) {
        return create(visitRepository, roomRepository, patientRepository, prescriptionRepository,
                serviceRepository, inventoryRepository, drugSafetyService, transactions, audit, examQueue, null);
    }

    public static Services create(
            ILuotKhamRepository visitRepository, IPhongKhamRepository roomRepository,
            IBenhNhanRepository patientRepository, IDonThuocRepository prescriptionRepository,
            IChiDinhDichVuRepository serviceRepository, IKhoRepository inventoryRepository,
            LamSangThuocAnToanDichVu drugSafetyService, GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi audit, KhamBenhHangDoiCongKetNoi examQueue,
            vn.clinicmanagement.application.billing.ClsVienPhiDongBo billing) {
        return create(visitRepository, roomRepository, patientRepository, prescriptionRepository,
                serviceRepository, inventoryRepository, drugSafetyService, transactions, audit, examQueue, billing, null);
    }

    public static Services create(
            ILuotKhamRepository visitRepository, IPhongKhamRepository roomRepository,
            IBenhNhanRepository patientRepository, IDonThuocRepository prescriptionRepository,
            IChiDinhDichVuRepository serviceRepository, IKhoRepository inventoryRepository,
            LamSangThuocAnToanDichVu drugSafetyService, GiaoDichRanhGioi transactions,
            KiemToanWriteCongKetNoi audit, KhamBenhHangDoiCongKetNoi examQueue,
            vn.clinicmanagement.application.billing.ClsVienPhiDongBo billing,
            vn.clinicmanagement.application.billing.DonThuocVienPhiDongBo rxBilling) {
            Objects.requireNonNull(visitRepository, "visitRepository");
        Objects.requireNonNull(roomRepository, "roomRepository");
        Objects.requireNonNull(patientRepository, "patientRepository");
        Objects.requireNonNull(prescriptionRepository, "prescriptionRepository");
        Objects.requireNonNull(serviceRepository, "serviceRepository");
        Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        Objects.requireNonNull(drugSafetyService, "drugSafetyService");
        Objects.requireNonNull(transactions, "transactions");
        Objects.requireNonNull(audit, "audit");
        Objects.requireNonNull(examQueue, "examQueue");

        StartKhamHandler startExam = new StartKhamHandler(visitRepository, transactions, audit);
        CompleteKhamHandler completeExam = new CompleteKhamHandler(visitRepository, transactions, audit);
        AddChanDoanHandler addDiagnosis = new AddChanDoanHandler(visitRepository, transactions, audit);
        PrescribeThuocHandler prescribeMedication = new PrescribeThuocHandler(
                prescriptionRepository, visitRepository, patientRepository, inventoryRepository,
                drugSafetyService, transactions, audit, rxBilling);
        GetDonThuocTheoLuotKhamHandler getPrescriptionByVisit = new GetDonThuocTheoLuotKhamHandler(
                prescriptionRepository, visitRepository, audit);
        CancelDonThuocHandler cancelPrescription = new CancelDonThuocHandler(
                prescriptionRepository, visitRepository, transactions, audit, rxBilling);
        YeuCauDichVuHandler orderService = new YeuCauDichVuHandler(
                serviceRepository, visitRepository, transactions, audit, billing);
        GetDichVuTheoLuotKhamHandler getServicesByVisit = new GetDichVuTheoLuotKhamHandler(serviceRepository, audit);
        CancelDichVuYeuCauHandler cancelServiceOrder = new CancelDichVuYeuCauHandler(
                serviceRepository, visitRepository, transactions, audit, billing);
        UpdateEMRLamSangRecordHandler updateEmr = new UpdateEMRLamSangRecordHandler(visitRepository, transactions, audit);
        TransferLuotKhamHandler transferVisit = new TransferLuotKhamHandler(visitRepository, roomRepository, transactions, audit);
        KhoaEMRHandler lockEmr = new KhoaEMRHandler(visitRepository, transactions, audit);
        RecordSinhHieuDauHieuHandler recordVitalSigns = new RecordSinhHieuDauHieuHandler(visitRepository, transactions, audit);
        CancelLuotKhamHandler cancelVisit = new CancelLuotKhamHandler(visitRepository, transactions, audit);

        return new Services(
                startExam,
                completeExam,
                addDiagnosis,
                prescribeMedication,
                getPrescriptionByVisit,
                cancelPrescription,
                orderService,
                getServicesByVisit,
                cancelServiceOrder,
                updateEmr,
                transferVisit,
                examQueue,
                inventoryRepository,
                lockEmr,
                recordVitalSigns,
                cancelVisit
        );
    }

    public static final class InMemoryExamQueuePort implements KhamBenhHangDoiCongKetNoi {
        private final List<KhamBenhHangDoiMatHang> queueItems = new ArrayList<>();
        private final Map<Integer, List<KhamBenhLichSuMatHang>> historyByPatient = new ConcurrentHashMap<>();
        private final List<ICD> icdCatalog = new ArrayList<>();
        private final ILuotKhamRepository visitRepository;
        private final IPhongKhamRepository roomRepository;

        public InMemoryExamQueuePort() {
            this(null, null);
        }

        public InMemoryExamQueuePort(ILuotKhamRepository visitRepository, IPhongKhamRepository roomRepository) {
            this.visitRepository = visitRepository;
            this.roomRepository = roomRepository;
            seedDefaultIcds();
        }

        private void seedDefaultIcds() {
            String[][] defaults = {
                    {"1", "I10", "Bệnh tăng huyết áp vô căn (nguyên phát)"},
                    {"2", "E11", "Bệnh đái tháo đường không phụ thuộc insulin (typ 2)"},
                    {"3", "J00", "Viêm mũi họng cấp [cảm thường]"},
                    {"4", "J02", "Viêm họng cấp"},
                    {"5", "J20", "Viêm phế quản cấp"},
                    {"6", "K29", "Viêm dạ dày và tá tràng"},
                    {"7", "A09", "Tiêu chảy và viêm dạ dày - ruột do nhiễm khuẩn"}
            };
            for (String[] row : defaults) {
                ICD icd = new ICD();
                icd.setId(Integer.parseInt(row[0]));
                icd.setMaICD(row[1]);
                icd.setTenBenh(row[2]);
                icdCatalog.add(icd);
            }
        }

        @Override
        public List<KhamBenhHangDoiMatHang> getTodayQueue(int facilityId, int doctorId) {
            return List.copyOf(queueItems);
        }

        @Override
        public List<KhamBenhLichSuMatHang> getPatientVisitHistory(int patientId) {
            return historyByPatient.getOrDefault(patientId, List.of());
        }

        @Override
        public List<ICD> searchIcdCatalog(String keyword) {
            if (keyword == null || keyword.isBlank()) return List.copyOf(icdCatalog);
            String q = keyword.trim().toLowerCase(java.util.Locale.ROOT);
            return icdCatalog.stream()
                    .filter(i -> (i.getMaICD() != null && i.getMaICD().toLowerCase(java.util.Locale.ROOT).contains(q))
                            || (i.getTenBenh() != null && i.getTenBenh().toLowerCase(java.util.Locale.ROOT).contains(q)))
                    .toList();
        }

        @Override
        public Optional<LuotKham> getVisitDetail(int visitId) {
            return visitRepository != null ? visitRepository.getById(visitId) : Optional.empty();
        }

        @Override
        public List<PhongKham> getActiveClinicRooms() {
            return roomRepository != null ? roomRepository.getActiveRooms() : List.of();
        }

        public void setIcdCatalog(List<ICD> items) {
            icdCatalog.clear();
            if (items != null) icdCatalog.addAll(items);
        }

        public void setQueueItems(List<KhamBenhHangDoiMatHang> items) {
            queueItems.clear();
            if (items != null) queueItems.addAll(items);
        }

        public void setHistory(int patientId, List<KhamBenhLichSuMatHang> items) {
            historyByPatient.put(patientId, items == null ? List.of() : List.copyOf(items));
        }
    }

    public static final class InMemoryRoomRepository implements IPhongKhamRepository {
        private final Map<Integer, PhongKham> rooms = new ConcurrentHashMap<>();

        @Override
        public Optional<PhongKham> getById(int id) {
            return Optional.ofNullable(rooms.get(id));
        }

        @Override
        public void add(PhongKham phongKham) {
            if (phongKham != null && phongKham.getId() != null) {
                rooms.put(phongKham.getId(), phongKham);
            }
        }
    }

    public static final class InMemoryPatientRepository implements IBenhNhanRepository {
        private final Map<Integer, BenhNhan> patients = new ConcurrentHashMap<>();

        @Override
        public Optional<BenhNhan> getById(int id) {
            return Optional.ofNullable(patients.get(id));
        }

        @Override
        public Optional<BenhNhan> getByCCCD(String cccd) {
            return patients.values().stream().filter(p -> Objects.equals(p.getSoCCCD(), cccd)).findFirst();
        }

        @Override
        public Optional<BenhNhan> getByPhone(String phone) {
            return patients.values().stream().filter(p -> Objects.equals(p.getDienThoai(), phone)).findFirst();
        }

        @Override
        public List<BenhNhan> search(String keyword) {
            return new ArrayList<>(patients.values());
        }

        @Override
        public List<BenhNhan> searchInScope(String keyword, int facilityId, int departmentId, int limit) {
            return new ArrayList<>(patients.values());
        }

        @Override
        public List<BenhNhan> getAll() {
            return new ArrayList<>(patients.values());
        }

        @Override
        public PhanTrangKetQua<BenhNhan> getPaged(int pageIndex, int pageSize, ITieuChuan<BenhNhan> specification) {
            return new PhanTrangKetQua<>(List.of(), 0, pageIndex, pageSize);
        }

        @Override
        public List<BenhNhan> find(ITieuChuan<BenhNhan> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ITieuChuan<BenhNhan> specification) {
            return false;
        }

        @Override
        public int count(ITieuChuan<BenhNhan> specification) {
            return patients.size();
        }

        @Override
        public void add(BenhNhan entity) {
            if (entity != null && entity.getId() != null) {
                patients.put(entity.getId(), entity);
            }
        }

        @Override
        public void update(BenhNhan entity) {
            if (entity != null && entity.getId() != null) {
                patients.put(entity.getId(), entity);
            }
        }

        @Override
        public void delete(BenhNhan entity) {
            if (entity != null && entity.getId() != null) {
                patients.remove(entity.getId());
            }
        }

        @Override
        public IUnitOfWork getUnitOfWork() {
            return NOOP_UOW;
        }
    }

    public static final class InMemoryPrescriptionRepository implements IDonThuocRepository {
        private final Map<Integer, DonThuoc> prescriptions = new ConcurrentHashMap<>();
        private int idSeq = 1;

        @Override
        public synchronized List<DonThuoc> getByLuotKhamId(int maLuotKham) {
            return prescriptions.values().stream()
                    .filter(d -> d.getMaLuotKham() == maLuotKham)
                    .toList();
        }

        @Override
        public synchronized Optional<DonThuoc> getById(int id) {
            return Optional.ofNullable(prescriptions.get(id));
        }

        @Override
        public synchronized List<DonThuoc> getAll() {
            return new ArrayList<>(prescriptions.values());
        }

        @Override
        public PhanTrangKetQua<DonThuoc> getPaged(int pageIndex, int pageSize, ITieuChuan<DonThuoc> specification) {
            return new PhanTrangKetQua<>(List.of(), 0, pageIndex, pageSize);
        }

        @Override
        public List<DonThuoc> find(ITieuChuan<DonThuoc> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ITieuChuan<DonThuoc> specification) {
            return false;
        }

        @Override
        public int count(ITieuChuan<DonThuoc> specification) {
            return prescriptions.size();
        }

        @Override
        public synchronized void add(DonThuoc entity) {
            if (entity.getId() == null || entity.getId() == 0) {
                entity.setId(idSeq++);
            }
            prescriptions.put(entity.getId(), entity);
        }

        @Override
        public synchronized void update(DonThuoc entity) {
            if (entity.getId() != null) {
                prescriptions.put(entity.getId(), entity);
            }
        }

        @Override
        public synchronized void delete(DonThuoc entity) {
            if (entity != null && entity.getId() != null) {
                prescriptions.remove(entity.getId());
            }
        }

        @Override
        public IUnitOfWork getUnitOfWork() {
            return NOOP_UOW;
        }
    }

    public static final class InMemoryServiceOrderRepository implements IChiDinhDichVuRepository {
        private final Map<Integer, ChiDinhDichVu> orders = new ConcurrentHashMap<>();
        private final List<ChiTietKetQuaXetNghiem> results = Collections.synchronizedList(new ArrayList<>());
        private int idSeq = 1;

        @Override
        public synchronized void save(ChiDinhDichVu chiDinh) {
            if (chiDinh.getId() == null || chiDinh.getId() == 0) {
                chiDinh.setId(idSeq++);
            }
            orders.put(chiDinh.getId(), chiDinh);
        }

        @Override
        public synchronized List<ChiDinhDichVu> getByLuotKham(int maLuotKham) {
            return orders.values().stream()
                    .filter(c -> c.getMaLuotKham() == maLuotKham)
                    .toList();
        }

        @Override
        public synchronized List<ChiTietKetQuaXetNghiem> getKetQuaByChiDinh(int maChiDinh) {
            return results.stream().filter(r -> r.getMaChiDinhDichVu() == maChiDinh).toList();
        }

        @Override
        public synchronized void saveKetQua(ChiTietKetQuaXetNghiem ketQua) {
            results.add(ketQua);
        }

        @Override
        public Optional<DanhMucDichVu> getDichVuById(int maDichVu) {
            return Optional.empty();
        }

        @Override
        public List<DanhMucDichVu> searchDichVu(String keyword, LoaiDichVu loaiDichVu) {
            return List.of();
        }

        @Override
        public Optional<ChiDinhDichVu> getById(int id) {
            return Optional.ofNullable(orders.get(id));
        }

        @Override
        public List<ChiDinhDichVu> getAll() {
            return new ArrayList<>(orders.values());
        }

        @Override
        public PhanTrangKetQua<ChiDinhDichVu> getPaged(int pageIndex, int pageSize, ITieuChuan<ChiDinhDichVu> specification) {
            return new PhanTrangKetQua<>(List.of(), 0, pageIndex, pageSize);
        }

        @Override
        public List<ChiDinhDichVu> find(ITieuChuan<ChiDinhDichVu> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ITieuChuan<ChiDinhDichVu> specification) {
            return false;
        }

        @Override
        public int count(ITieuChuan<ChiDinhDichVu> specification) {
            return orders.size();
        }

        @Override
        public void add(ChiDinhDichVu entity) {
            save(entity);
        }

        @Override
        public void update(ChiDinhDichVu entity) {
            save(entity);
        }

        @Override
        public void delete(ChiDinhDichVu entity) {
            if (entity != null && entity.getId() != null) {
                orders.remove(entity.getId());
            }
        }

        @Override
        public IUnitOfWork getUnitOfWork() {
            return NOOP_UOW;
        }
    }

    public static final class InMemoryInventoryRepository implements IKhoRepository {
        private final Map<Integer, MatHang> items = new ConcurrentHashMap<>();
        private final Map<Integer, TonKho> batches = new ConcurrentHashMap<>();

        @Override
        public Optional<MatHang> getItemById(int id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public Optional<MatHang> getItemByCode(String code) {
            return items.values().stream().filter(i -> Objects.equals(i.getMaItem(), code)).findFirst();
        }

        @Override
        public MatHang saveItem(MatHang item) {
            if (item.getId() != null) items.put(item.getId(), item);
            return item;
        }

        @Override
        public List<MatHang> searchItems(String keyword, Boolean hoatDong) {
            return new ArrayList<>(items.values());
        }

        @Override
        public Optional<Kho> getKhoById(int id) {
            return Optional.empty();
        }

        @Override
        public List<TonKho> getAvailableBatches(int maKho, int maItem) {
            return batches.values().stream().filter(b -> b.getMaKho() == maKho && b.getMaItem() == maItem).toList();
        }

        @Override
        public List<TonKho> getBatchesByKho(int maKho) {
            return batches.values().stream().filter(b -> b.getMaKho() == maKho).toList();
        }

        @Override
        public Optional<TonKho> getBatchById(int id) {
            return Optional.ofNullable(batches.get(id));
        }

        @Override
        public TonKho saveTonKho(TonKho tonKho) {
            if (tonKho.getId() != null) batches.put(tonKho.getId(), tonKho);
            return tonKho;
        }

        @Override public PhieuNhapKho savePhieuNhapKho(PhieuNhapKho phieu) { return phieu; }
        @Override public Optional<PhieuNhapKho> getPhieuNhapKhoById(int id) { return Optional.empty(); }
        @Override public PhieuXuatKho savePhieuXuatKho(PhieuXuatKho phieu) { return phieu; }
        @Override public Optional<PhieuXuatKho> getPhieuXuatKhoById(int id) { return Optional.empty(); }
        @Override public DonDatHang savePurchaseOrder(DonDatHang don) { return don; }
        @Override public Optional<DonDatHang> getPurchaseOrderById(int id) { return Optional.empty(); }
        @Override public KiemKe saveStocktake(KiemKe stocktake) { return stocktake; }
        @Override public Optional<KiemKe> getStocktakeById(int id) { return Optional.empty(); }

        @Override public int saveChanges() { return 1; }
        @Override public boolean saveEntities() { return true; }
    }
}
