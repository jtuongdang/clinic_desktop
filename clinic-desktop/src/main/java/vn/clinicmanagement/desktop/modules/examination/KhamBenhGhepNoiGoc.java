package vn.clinicmanagement.desktop.modules.examination;

import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.cls.CancelDichVuYeuCauHandler;
import vn.clinicmanagement.application.cls.GetDichVuTheoLuotKhamHandler;
import vn.clinicmanagement.application.cls.YeuCauDichVuHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.application.examination.AddChanDoanHandler;
import vn.clinicmanagement.application.examination.CompleteKhamHandler;
import vn.clinicmanagement.application.examination.StartKhamHandler;
import vn.clinicmanagement.application.examination.TransferLuotKhamHandler;
import vn.clinicmanagement.application.examination.UpdateEMRLamSangRecordHandler;
import vn.clinicmanagement.application.prescription.CancelDonThuocHandler;
import vn.clinicmanagement.application.prescription.GetDonThuocTheoLuotKhamHandler;
import vn.clinicmanagement.application.prescription.PrescribeThuocHandler;
import vn.clinicmanagement.desktop.modules.billing.VienPhiGhepNoiGoc;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.inventory.DonDatHang;
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
        /** Hàm `appendInCurrentTransaction`: thêm vào trong hientai giao dịch. */
        @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {}
        /** Hàm `appendIndependently`: thêm vào `Independently`. */
        @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {}
        /** Hàm `appendMedicalRecordInCurrentTransaction`: thêm vào yte ghi nhận trong hientai giao dịch. */
        @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}
        /** Hàm `appendMedicalRecordIndependently`: thêm vào yte ghi nhận `Independently`. */
        @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
    };

    private static final GiaoDichRanhGioi DIRECT_TRANSACTIONS = new GiaoDichRanhGioi() {
        /** Hàm `inTransaction`: trong giao dịch. */
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
        /** Hàm `saveChanges`: lưu thaydoi. */
        @Override public int saveChanges() { return 1; }
        /** Hàm `saveEntities`: lưu thực thể. */
        @Override public boolean saveEntities() { return true; }
    };

    public record Services(
            /** Trường `startExam`: bắt đầu kham. */
            StartKhamHandler startExam,
            /** Trường `completeExam`: hoàn tất kham. */
            CompleteKhamHandler completeExam,
            /** Trường `addDiagnosis`: thêm chandoan. */
            AddChanDoanHandler addDiagnosis,
            /** Trường `prescribeMedication`: kê đơn thuốc. */
            PrescribeThuocHandler prescribeMedication,
            /** Trường `getPrescriptionByVisit`: lấy đơn thuốc theo luotkham. */
            GetDonThuocTheoLuotKhamHandler getPrescriptionByVisit,
            /** Trường `cancelPrescription`: hủy đơn thuốc. */
            CancelDonThuocHandler cancelPrescription,
            /** Trường `orderService`: yêu cầu dịch vụ. */
            YeuCauDichVuHandler orderService,
            /** Trường `getServicesByVisit`: lấy dịch vụ theo luotkham. */
            GetDichVuTheoLuotKhamHandler getServicesByVisit,
            /** Trường `cancelServiceOrder`: hủy dịch vụ yêu cầu. */
            CancelDichVuYeuCauHandler cancelServiceOrder,
            /** Trường `updateEmrClinicalRecord`: cập nhật emr lamsang ghi nhận. */
            UpdateEMRLamSangRecordHandler updateEmrClinicalRecord,
            /** Trường `transferVisit`: chuyển luotkham. */
            TransferLuotKhamHandler transferVisit) {
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

    /** Hàm `createDefault`: tạo macdinh. */
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
                FALLBACK_AUDIT
        );
    }

    /** Hàm `create`: tạo. */
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
        Objects.requireNonNull(visitRepository, "visitRepository");
        Objects.requireNonNull(roomRepository, "roomRepository");
        Objects.requireNonNull(patientRepository, "patientRepository");
        Objects.requireNonNull(prescriptionRepository, "prescriptionRepository");
        Objects.requireNonNull(serviceRepository, "serviceRepository");
        Objects.requireNonNull(inventoryRepository, "inventoryRepository");
        Objects.requireNonNull(drugSafetyService, "drugSafetyService");
        Objects.requireNonNull(transactions, "transactions");
        Objects.requireNonNull(audit, "audit");

        StartKhamHandler startExam = new StartKhamHandler(visitRepository, transactions, audit);
        CompleteKhamHandler completeExam = new CompleteKhamHandler(visitRepository, transactions, audit);
        AddChanDoanHandler addDiagnosis = new AddChanDoanHandler(visitRepository, transactions, audit);
        PrescribeThuocHandler prescribeMedication = new PrescribeThuocHandler(
                prescriptionRepository, visitRepository, patientRepository, inventoryRepository,
                drugSafetyService, transactions, audit);
        GetDonThuocTheoLuotKhamHandler getPrescriptionByVisit = new GetDonThuocTheoLuotKhamHandler(
                prescriptionRepository, visitRepository, audit);
        CancelDonThuocHandler cancelPrescription = new CancelDonThuocHandler(
                prescriptionRepository, visitRepository, transactions, audit);
        YeuCauDichVuHandler orderService = new YeuCauDichVuHandler(serviceRepository, visitRepository, audit);
        GetDichVuTheoLuotKhamHandler getServicesByVisit = new GetDichVuTheoLuotKhamHandler(serviceRepository, audit);
        CancelDichVuYeuCauHandler cancelServiceOrder = new CancelDichVuYeuCauHandler(serviceRepository, audit);
        UpdateEMRLamSangRecordHandler updateEmr = new UpdateEMRLamSangRecordHandler(visitRepository, transactions, audit);
        TransferLuotKhamHandler transferVisit = new TransferLuotKhamHandler(visitRepository, roomRepository, transactions, audit);

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
        /** Trường `rooms`: phòng. */
        private final Map<Integer, PhongKham> rooms = new ConcurrentHashMap<>();

        /** Hàm `getById`: lấy theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IPhongKhamRepository#getById`. */
        @Override
        public Optional<PhongKham> getById(int id) {
            return Optional.ofNullable(rooms.get(id));
        }

        /** Hàm `add`: thêm. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IPhongKhamRepository#add`. */
        @Override
        public void add(PhongKham phongKham) {
            if (phongKham != null && phongKham.getId() != null) {
                rooms.put(phongKham.getId(), phongKham);
            }
        }
    }

    public static final class InMemoryPatientRepository implements IBenhNhanRepository {
        /** Trường `patients`: bệnh nhân. */
        private final Map<Integer, BenhNhan> patients = new ConcurrentHashMap<>();

        /** Hàm `getById`: lấy theo mã. Gốc khai báo tại `IBenhNhanRepository#getById`. */
        @Override
        public Optional<BenhNhan> getById(int id) {
            return Optional.ofNullable(patients.get(id));
        }

        /** Hàm `getByCCCD`: lấy theo `CCCD`. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IBenhNhanRepository#getByCCCD`. */
        @Override
        public Optional<BenhNhan> getByCCCD(String cccd) {
            return patients.values().stream().filter(p -> Objects.equals(p.getSoCCCD(), cccd)).findFirst();
        }

        /** Hàm `getByPhone`: lấy theo dienthoai. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IBenhNhanRepository#getByPhone`. */
        @Override
        public Optional<BenhNhan> getByPhone(String phone) {
            return patients.values().stream().filter(p -> Objects.equals(p.getDienThoai(), phone)).findFirst();
        }

        /** Hàm `search`: tìm kiếm. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IBenhNhanRepository#search`. */
        @Override
        public List<BenhNhan> search(String keyword) {
            return new ArrayList<>(patients.values());
        }

        /** Hàm `searchInScope`: tìm kiếm trong phamvi. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IBenhNhanRepository#searchInScope`. */
        @Override
        public List<BenhNhan> searchInScope(String keyword, int facilityId, int departmentId, int limit) {
            return new ArrayList<>(patients.values());
        }

        /** Hàm `getAll`: lấy `All`. Gốc khai báo tại `IBenhNhanRepository#getAll`. */
        @Override
        public List<BenhNhan> getAll() {
            return new ArrayList<>(patients.values());
        }

        /** Hàm `getPaged`: lấy phân trang. Gốc khai báo tại `IBenhNhanRepository#getPaged`. */
        @Override
        public PhanTrangKetQua<BenhNhan> getPaged(int pageIndex, int pageSize, ITieuChuan<BenhNhan> specification) {
            return new PhanTrangKetQua<>(List.of(), 0, pageIndex, pageSize);
        }

        /** Hàm `find`: tìm. Gốc khai báo tại `IBenhNhanRepository#find`. */
        @Override
        public List<BenhNhan> find(ITieuChuan<BenhNhan> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ITieuChuan<BenhNhan> specification) {
            return false;
        }

        /** Hàm `count`: dem. Gốc khai báo tại `IBenhNhanRepository#count`. */
        @Override
        public int count(ITieuChuan<BenhNhan> specification) {
            return patients.size();
        }

        /** Hàm `add`: thêm. Gốc khai báo tại `IBenhNhanRepository#add`. */
        @Override
        public void add(BenhNhan entity) {
            if (entity != null && entity.getId() != null) {
                patients.put(entity.getId(), entity);
            }
        }

        /** Hàm `update`: cập nhật. Gốc khai báo tại `IBenhNhanRepository#update`. */
        @Override
        public void update(BenhNhan entity) {
            if (entity != null && entity.getId() != null) {
                patients.put(entity.getId(), entity);
            }
        }

        /** Hàm `delete`: xóa. Gốc khai báo tại `IBenhNhanRepository#delete`. */
        @Override
        public void delete(BenhNhan entity) {
            if (entity != null && entity.getId() != null) {
                patients.remove(entity.getId());
            }
        }

        /** Hàm `getUnitOfWork`: lấy đơn vị của làm việc. Gốc khai báo tại `IBenhNhanRepository#getUnitOfWork`. */
        @Override
        public IUnitOfWork getUnitOfWork() {
            return NOOP_UOW;
        }
    }

    public static final class InMemoryPrescriptionRepository implements IDonThuocRepository {
        private final Map<Integer, DonThuoc> prescriptions = new ConcurrentHashMap<>();
        /** Trường `idSeq`: mã `Seq`. */
        private int idSeq = 1;

        /** Hàm `getByLuotKhamId`: lấy theo luot kham mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IDonThuocRepository#getByLuotKhamId`. */
        @Override
        public synchronized List<DonThuoc> getByLuotKhamId(int maLuotKham) {
            return prescriptions.values().stream()
                    .filter(d -> d.getMaLuotKham() == maLuotKham)
                    .toList();
        }

        /** Hàm `getById`: lấy theo mã. Gốc khai báo tại `IDonThuocRepository#getById`. */
        @Override
        public synchronized Optional<DonThuoc> getById(int id) {
            return Optional.ofNullable(prescriptions.get(id));
        }

        /** Hàm `getAll`: lấy `All`. Gốc khai báo tại `IDonThuocRepository#getAll`. */
        @Override
        public synchronized List<DonThuoc> getAll() {
            return new ArrayList<>(prescriptions.values());
        }

        /** Hàm `getPaged`: lấy phân trang. Gốc khai báo tại `IDonThuocRepository#getPaged`. */
        @Override
        public PhanTrangKetQua<DonThuoc> getPaged(int pageIndex, int pageSize, ITieuChuan<DonThuoc> specification) {
            return new PhanTrangKetQua<>(List.of(), 0, pageIndex, pageSize);
        }

        /** Hàm `find`: tìm. Gốc khai báo tại `IDonThuocRepository#find`. */
        @Override
        public List<DonThuoc> find(ITieuChuan<DonThuoc> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ITieuChuan<DonThuoc> specification) {
            return false;
        }

        /** Hàm `count`: dem. Gốc khai báo tại `IDonThuocRepository#count`. */
        @Override
        public int count(ITieuChuan<DonThuoc> specification) {
            return prescriptions.size();
        }

        /** Hàm `add`: thêm. Gốc khai báo tại `IDonThuocRepository#add`. */
        @Override
        public synchronized void add(DonThuoc entity) {
            if (entity.getId() == null || entity.getId() == 0) {
                entity.setId(idSeq++);
            }
            prescriptions.put(entity.getId(), entity);
        }

        /** Hàm `update`: cập nhật. Gốc khai báo tại `IDonThuocRepository#update`. */
        @Override
        public synchronized void update(DonThuoc entity) {
            if (entity.getId() != null) {
                prescriptions.put(entity.getId(), entity);
            }
        }

        /** Hàm `delete`: xóa. Gốc khai báo tại `IDonThuocRepository#delete`. */
        @Override
        public synchronized void delete(DonThuoc entity) {
            if (entity != null && entity.getId() != null) {
                prescriptions.remove(entity.getId());
            }
        }

        /** Hàm `getUnitOfWork`: lấy đơn vị của làm việc. Gốc khai báo tại `IDonThuocRepository#getUnitOfWork`. */
        @Override
        public IUnitOfWork getUnitOfWork() {
            return NOOP_UOW;
        }
    }

    public static final class InMemoryServiceOrderRepository implements IChiDinhDichVuRepository {
        private final Map<Integer, ChiDinhDichVu> orders = new ConcurrentHashMap<>();
        private final List<ChiTietKetQuaXetNghiem> results = Collections.synchronizedList(new ArrayList<>());
        /** Trường `idSeq`: mã `Seq`. */
        private int idSeq = 1;

        /** Hàm `save`: lưu. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository#save`. */
        @Override
        public synchronized void save(ChiDinhDichVu chiDinh) {
            if (chiDinh.getId() == null || chiDinh.getId() == 0) {
                chiDinh.setId(idSeq++);
            }
            orders.put(chiDinh.getId(), chiDinh);
        }

        /** Hàm `getByLuotKham`: lấy theo luot kham. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository#getByLuotKham`. */
        @Override
        public synchronized List<ChiDinhDichVu> getByLuotKham(int maLuotKham) {
            return orders.values().stream()
                    .filter(c -> c.getMaLuotKham() == maLuotKham)
                    .toList();
        }

        /** Hàm `getKetQuaByChiDinh`: lấy ket qua theo chi dinh. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository#getKetQuaByChiDinh`. */
        @Override
        public synchronized List<ChiTietKetQuaXetNghiem> getKetQuaByChiDinh(int maChiDinh) {
            return results.stream().filter(r -> r.getMaChiDinhDichVu() == maChiDinh).toList();
        }

        /** Hàm `saveKetQua`: lưu ket qua. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository#saveKetQua`. */
        @Override
        public synchronized void saveKetQua(ChiTietKetQuaXetNghiem ketQua) {
            results.add(ketQua);
        }

        /** Hàm `getDichVuById`: lấy dich vu theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository#getDichVuById`. */
        @Override
        public Optional<DanhMucDichVu> getDichVuById(int maDichVu) {
            return Optional.empty();
        }

        /** Hàm `searchDichVu`: tìm kiếm dich vu. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository#searchDichVu`. */
        @Override
        public List<DanhMucDichVu> searchDichVu(String keyword, LoaiDichVu loaiDichVu) {
            return List.of();
        }

        /** Hàm `getById`: lấy theo mã. Gốc khai báo tại `IChiDinhDichVuRepository#getById`. */
        @Override
        public Optional<ChiDinhDichVu> getById(int id) {
            return Optional.ofNullable(orders.get(id));
        }

        /** Hàm `getAll`: lấy `All`. Gốc khai báo tại `IChiDinhDichVuRepository#getAll`. */
        @Override
        public List<ChiDinhDichVu> getAll() {
            return new ArrayList<>(orders.values());
        }

        /** Hàm `getPaged`: lấy phân trang. Gốc khai báo tại `IChiDinhDichVuRepository#getPaged`. */
        @Override
        public PhanTrangKetQua<ChiDinhDichVu> getPaged(int pageIndex, int pageSize, ITieuChuan<ChiDinhDichVu> specification) {
            return new PhanTrangKetQua<>(List.of(), 0, pageIndex, pageSize);
        }

        /** Hàm `find`: tìm. Gốc khai báo tại `IChiDinhDichVuRepository#find`. */
        @Override
        public List<ChiDinhDichVu> find(ITieuChuan<ChiDinhDichVu> specification) {
            return List.of();
        }

        @Override
        public boolean exists(ITieuChuan<ChiDinhDichVu> specification) {
            return false;
        }

        /** Hàm `count`: dem. Gốc khai báo tại `IChiDinhDichVuRepository#count`. */
        @Override
        public int count(ITieuChuan<ChiDinhDichVu> specification) {
            return orders.size();
        }

        /** Hàm `add`: thêm. Gốc khai báo tại `IChiDinhDichVuRepository#add`. */
        @Override
        public void add(ChiDinhDichVu entity) {
            save(entity);
        }

        /** Hàm `update`: cập nhật. Gốc khai báo tại `IChiDinhDichVuRepository#update`. */
        @Override
        public void update(ChiDinhDichVu entity) {
            save(entity);
        }

        /** Hàm `delete`: xóa. Gốc khai báo tại `IChiDinhDichVuRepository#delete`. */
        @Override
        public void delete(ChiDinhDichVu entity) {
            if (entity != null && entity.getId() != null) {
                orders.remove(entity.getId());
            }
        }

        /** Hàm `getUnitOfWork`: lấy đơn vị của làm việc. Gốc khai báo tại `IChiDinhDichVuRepository#getUnitOfWork`. */
        @Override
        public IUnitOfWork getUnitOfWork() {
            return NOOP_UOW;
        }
    }

    public static final class InMemoryInventoryRepository implements IKhoRepository {
        private final Map<Integer, MatHang> items = new ConcurrentHashMap<>();
        private final Map<Integer, TonKho> batches = new ConcurrentHashMap<>();

        /** Hàm `getItemById`: lấy mathang theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getItemById`. */
        @Override
        public Optional<MatHang> getItemById(int id) {
            return Optional.ofNullable(items.get(id));
        }

        /** Hàm `getItemByCode`: lấy mathang theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getItemByCode`. */
        @Override
        public Optional<MatHang> getItemByCode(String code) {
            return items.values().stream().filter(i -> Objects.equals(i.getMaItem(), code)).findFirst();
        }

        /** Hàm `saveItem`: lưu mathang. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#saveItem`. */
        @Override
        public MatHang saveItem(MatHang item) {
            if (item.getId() != null) items.put(item.getId(), item);
            return item;
        }

        /** Hàm `searchItems`: tìm kiếm `Items`. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#searchItems`. */
        @Override
        public List<MatHang> searchItems(String keyword, Boolean hoatDong) {
            return new ArrayList<>(items.values());
        }

        /** Hàm `getKhoById`: lấy kho theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getKhoById`. */
        @Override
        public Optional<Kho> getKhoById(int id) {
            return Optional.empty();
        }

        /** Hàm `getAvailableBatches`: lấy khadung `Batches`. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getAvailableBatches`. */
        @Override
        public List<TonKho> getAvailableBatches(int maKho, int maItem) {
            return batches.values().stream().filter(b -> b.getMaKho() == maKho && b.getMaItem() == maItem).toList();
        }

        /** Hàm `getBatchesByKho`: lấy `Batches` theo kho. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getBatchesByKho`. */
        @Override
        public List<TonKho> getBatchesByKho(int maKho) {
            return batches.values().stream().filter(b -> b.getMaKho() == maKho).toList();
        }

        /** Hàm `getBatchById`: lấy lo theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getBatchById`. */
        @Override
        public Optional<TonKho> getBatchById(int id) {
            return Optional.ofNullable(batches.get(id));
        }

        /** Hàm `saveTonKho`: lưu ton kho. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#saveTonKho`. */
        @Override
        public TonKho saveTonKho(TonKho tonKho) {
            if (tonKho.getId() != null) batches.put(tonKho.getId(), tonKho);
            return tonKho;
        }

        /** Hàm `savePhieuNhapKho`: lưu phiếu nhap kho. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#savePhieuNhapKho`. */
        @Override public PhieuNhapKho savePhieuNhapKho(PhieuNhapKho phieu) { return phieu; }
        /** Hàm `getPhieuNhapKhoById`: lấy phiếu nhap kho theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getPhieuNhapKhoById`. */
        @Override public Optional<PhieuNhapKho> getPhieuNhapKhoById(int id) { return Optional.empty(); }
        /** Hàm `savePhieuXuatKho`: lưu phiếu xuất kho. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#savePhieuXuatKho`. */
        @Override public PhieuXuatKho savePhieuXuatKho(PhieuXuatKho phieu) { return phieu; }
        /** Hàm `getPhieuXuatKhoById`: lấy phiếu xuất kho theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getPhieuXuatKhoById`. */
        @Override public Optional<PhieuXuatKho> getPhieuXuatKhoById(int id) { return Optional.empty(); }
        /** Hàm `savePurchaseOrder`: lưu muahang yêu cầu. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#savePurchaseOrder`. */
        @Override public DonDatHang savePurchaseOrder(DonDatHang don) { return don; }
        /** Hàm `getPurchaseOrderById`: lấy muahang yêu cầu theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getPurchaseOrderById`. */
        @Override public Optional<DonDatHang> getPurchaseOrderById(int id) { return Optional.empty(); }
        /** Hàm `saveStocktake`: lưu kiemke. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#saveStocktake`. */
        @Override public KiemKe saveStocktake(KiemKe stocktake) { return stocktake; }
        /** Hàm `getStocktakeById`: lấy kiemke theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IKhoRepository#getStocktakeById`. */
        @Override public Optional<KiemKe> getStocktakeById(int id) { return Optional.empty(); }

        /** Hàm `saveChanges`: lưu thaydoi. Gốc khai báo tại `IKhoRepository#saveChanges`. */
        @Override public int saveChanges() { return 1; }
        /** Hàm `saveEntities`: lưu thực thể. Gốc khai báo tại `IKhoRepository#saveEntities`. */
        @Override public boolean saveEntities() { return true; }
    }
}
