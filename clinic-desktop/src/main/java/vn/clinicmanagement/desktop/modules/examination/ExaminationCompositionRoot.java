package vn.clinicmanagement.desktop.modules.examination;

import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.cls.CancelServiceOrderHandler;
import vn.clinicmanagement.application.cls.GetServicesByVisitHandler;
import vn.clinicmanagement.application.cls.OrderServiceHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.application.examination.AddDiagnosisHandler;
import vn.clinicmanagement.application.examination.CompleteExamHandler;
import vn.clinicmanagement.application.examination.StartExamHandler;
import vn.clinicmanagement.application.examination.TransferVisitHandler;
import vn.clinicmanagement.application.examination.UpdateEmrClinicalRecordHandler;
import vn.clinicmanagement.application.prescription.CancelPrescriptionHandler;
import vn.clinicmanagement.application.prescription.GetPrescriptionByVisitHandler;
import vn.clinicmanagement.application.prescription.PrescribeMedicationHandler;
import vn.clinicmanagement.desktop.modules.billing.BillingCompositionRoot;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.inventory.DonDatHang;
import vn.clinicmanagement.domain.inventory.Item;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.PhieuNhapKho;
import vn.clinicmanagement.domain.inventory.PhieuXuatKho;
import vn.clinicmanagement.domain.inventory.Stocktake;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.prescription.ClinicalDrugSafetyService;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;
import vn.clinicmanagement.domain.repositories.IDonThuocRepository;
import vn.clinicmanagement.domain.repositories.IKhoRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;
import vn.clinicmanagement.domain.repositories.ISpecification;
import vn.clinicmanagement.domain.repositories.PagedResult;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiTietKetQuaXetNghiem;
import vn.clinicmanagement.domain.service.DanhMucDichVu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Composition root cho phân hệ Khám Chữa Bệnh Ngoại Trú & Bác Sĩ (Examination).
 * Khởi tạo và lắp ráp 11 Clean Architecture Handlers:
 * - StartExamHandler, CompleteExamHandler, AddDiagnosisHandler, UpdateEmrClinicalRecordHandler, TransferVisitHandler
 * - PrescribeMedicationHandler, GetPrescriptionByVisitHandler, CancelPrescriptionHandler
 * - OrderServiceHandler, GetServicesByVisitHandler, CancelServiceOrderHandler
 */
public final class ExaminationCompositionRoot {

    private static final AuditWritePort FALLBACK_AUDIT = new AuditWritePort() {
        @Override public void appendInCurrentTransaction(SystemAuditEntry entry) {}
        @Override public void appendIndependently(SystemAuditEntry entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {}
        @Override public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {}
    };

    private static final TransactionBoundary DIRECT_TRANSACTIONS = new TransactionBoundary() {
        @Override
        public <T> Result<T> inTransaction(Supplier<Result<T>> operation) {
            try {
                return operation.get();
            } catch (Exception ex) {
                return Result.failure("TX_ERROR", ex.getMessage());
            }
        }
    };

    private static final IUnitOfWork NOOP_UOW = new IUnitOfWork() {
        @Override public int saveChanges() { return 1; }
        @Override public boolean saveEntities() { return true; }
    };

    public record Services(
            StartExamHandler startExam,
            CompleteExamHandler completeExam,
            AddDiagnosisHandler addDiagnosis,
            PrescribeMedicationHandler prescribeMedication,
            GetPrescriptionByVisitHandler getPrescriptionByVisit,
            CancelPrescriptionHandler cancelPrescription,
            OrderServiceHandler orderService,
            GetServicesByVisitHandler getServicesByVisit,
            CancelServiceOrderHandler cancelServiceOrder,
            UpdateEmrClinicalRecordHandler updateEmrClinicalRecord,
            TransferVisitHandler transferVisit) {
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
        }
    }

    public static Services createDefault() {
        ILuotKhamRepository visitRepo = new BillingCompositionRoot.InMemoryVisitRepository();
        IPhongKhamRepository roomRepo = new InMemoryRoomRepository();
        IBenhNhanRepository patientRepo = new InMemoryPatientRepository();
        IDonThuocRepository prescriptionRepo = new InMemoryPrescriptionRepository();
        IChiDinhDichVuRepository serviceRepo = new InMemoryServiceOrderRepository();
        IKhoRepository inventoryRepo = new InMemoryInventoryRepository();
        ClinicalDrugSafetyService drugSafety = new ClinicalDrugSafetyService();

        return create(
                visitRepo,
                roomRepo,
                patientRepo,
                prescriptionRepo,
                serviceRepo,
                inventoryRepo,
                drugSafety,
                DIRECT_TRANSACTIONS,
                FALLBACK_AUDIT
        );
    }

    public static Services create(
            ILuotKhamRepository visitRepository,
            IPhongKhamRepository roomRepository,
            IBenhNhanRepository patientRepository,
            IDonThuocRepository prescriptionRepository,
            IChiDinhDichVuRepository serviceRepository,
            IKhoRepository inventoryRepository,
            ClinicalDrugSafetyService drugSafetyService,
            TransactionBoundary transactions,
            AuditWritePort audit) {
        Objects.requireNonNull(visitRepository, "visitRepository");
        Objects.requireNonNull(roomRepository, "roomRepository");
        Objects.requireNonNull(patientRepository, "patientRepository");
        Objects.requireNonNull(prescriptionRepository, "prescriptionRepository");
        Objects.requireNonNull(serviceRepository, "serviceRepository");
        Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        Objects.requireNonNull(drugSafetyService, "drugSafetyService");
        Objects.requireNonNull(transactions, "transactions");
        Objects.requireNonNull(audit, "audit");

        StartExamHandler startExam = new StartExamHandler(visitRepository, transactions, audit);
        CompleteExamHandler completeExam = new CompleteExamHandler(visitRepository, transactions, audit);
        AddDiagnosisHandler addDiagnosis = new AddDiagnosisHandler(visitRepository, transactions, audit);
        PrescribeMedicationHandler prescribeMedication = new PrescribeMedicationHandler(
                prescriptionRepository, visitRepository, patientRepository, inventoryRepository,
                drugSafetyService, transactions, audit);
        GetPrescriptionByVisitHandler getPrescriptionByVisit = new GetPrescriptionByVisitHandler(
                prescriptionRepository, visitRepository, audit);
        CancelPrescriptionHandler cancelPrescription = new CancelPrescriptionHandler(
                prescriptionRepository, visitRepository, transactions, audit);
        OrderServiceHandler orderService = new OrderServiceHandler(serviceRepository, visitRepository, audit);
        GetServicesByVisitHandler getServicesByVisit = new GetServicesByVisitHandler(serviceRepository, audit);
        CancelServiceOrderHandler cancelServiceOrder = new CancelServiceOrderHandler(serviceRepository, audit);
        UpdateEmrClinicalRecordHandler updateEmr = new UpdateEmrClinicalRecordHandler(visitRepository, transactions, audit);
        TransferVisitHandler transferVisit = new TransferVisitHandler(visitRepository, roomRepository, transactions, audit);

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
                transferVisit
        );
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
        public PagedResult<BenhNhan> getPaged(int pageIndex, int pageSize, ISpecification<BenhNhan> specification) {
            return new PagedResult<>(List.of(), 0, pageIndex, pageSize);
        }

        @Override
        public List<BenhNhan> find(ISpecification<BenhNhan> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ISpecification<BenhNhan> specification) {
            return false;
        }

        @Override
        public int count(ISpecification<BenhNhan> specification) {
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
        public PagedResult<DonThuoc> getPaged(int pageIndex, int pageSize, ISpecification<DonThuoc> specification) {
            return new PagedResult<>(List.of(), 0, pageIndex, pageSize);
        }

        @Override
        public List<DonThuoc> find(ISpecification<DonThuoc> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ISpecification<DonThuoc> specification) {
            return false;
        }

        @Override
        public int count(ISpecification<DonThuoc> specification) {
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
        public PagedResult<ChiDinhDichVu> getPaged(int pageIndex, int pageSize, ISpecification<ChiDinhDichVu> specification) {
            return new PagedResult<>(List.of(), 0, pageIndex, pageSize);
        }

        @Override
        public List<ChiDinhDichVu> find(ISpecification<ChiDinhDichVu> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ISpecification<ChiDinhDichVu> specification) {
            return false;
        }

        @Override
        public int count(ISpecification<ChiDinhDichVu> specification) {
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
        private final Map<Integer, Item> items = new ConcurrentHashMap<>();
        private final Map<Integer, TonKho> batches = new ConcurrentHashMap<>();

        @Override
        public Optional<Item> getItemById(int id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public Optional<Item> getItemByCode(String code) {
            return items.values().stream().filter(i -> Objects.equals(i.getMaItem(), code)).findFirst();
        }

        @Override
        public Item saveItem(Item item) {
            if (item.getId() != null) items.put(item.getId(), item);
            return item;
        }

        @Override
        public List<Item> searchItems(String keyword, Boolean hoatDong) {
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
        @Override public Stocktake saveStocktake(Stocktake stocktake) { return stocktake; }
        @Override public Optional<Stocktake> getStocktakeById(int id) { return Optional.empty(); }

        @Override public int saveChanges() { return 1; }
        @Override public boolean saveEntities() { return true; }
    }
}
