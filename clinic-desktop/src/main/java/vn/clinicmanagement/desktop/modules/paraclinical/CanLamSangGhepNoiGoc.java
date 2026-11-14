package vn.clinicmanagement.desktop.modules.paraclinical;

import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.cls.CancelDichVuYeuCauHandler;
import vn.clinicmanagement.application.cls.GetDichVuTheoLuotKhamHandler;
import vn.clinicmanagement.application.cls.YeuCauDichVuHandler;
import vn.clinicmanagement.application.cls.RecordDichVuKetQuaHandler;
import vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;
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

/**
 * Composition root cho phân hệ Cận Lâm Sàng & Kết Quả CLS (Paraclinical Module).
 * Khởi tạo và lắp ráp sạch sẽ 4 Clean Architecture Handlers:
 * - RecordServiceResultHandler: Nhập kết quả định tính, định lượng và kết luận CLS
 * - GetServicesByVisitHandler: Lấy danh sách chỉ định dịch vụ theo lượt khám
 * - OrderServiceHandler: Bác sĩ/KTV chỉ định dịch vụ kỹ thuật
 * - CancelServiceOrderHandler: Hủy chỉ định dịch vụ kỹ thuật
 * Cùng với Repository:
 * - IChiDinhDichVuRepository: Quản lý phiếu chỉ định, chi tiết kết quả và danh mục dịch vụ
 */
public final class CanLamSangGhepNoiGoc {

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

    public record Services(
            /** Trường `recordResult`: ghi nhận kết quả. */
            RecordDichVuKetQuaHandler recordResult,
            /** Trường `getServices`: lấy dịch vụ. */
            GetDichVuTheoLuotKhamHandler getServices,
            /** Trường `orderService`: yêu cầu dịch vụ. */
            YeuCauDichVuHandler orderService,
            /** Trường `cancelOrder`: hủy yêu cầu. */
            CancelDichVuYeuCauHandler cancelOrder,
            /** Trường `serviceRepository`: dịch vụ kho dữ liệu. */
            IChiDinhDichVuRepository serviceRepository) {
        public Services {
            Objects.requireNonNull(recordResult, "recordResult");
            Objects.requireNonNull(getServices, "getServices");
            Objects.requireNonNull(orderService, "orderService");
            Objects.requireNonNull(cancelOrder, "cancelOrder");
            Objects.requireNonNull(serviceRepository, "serviceRepository");
        }

        /** Hàm `getRecordResult`: lấy ghi nhận kết quả. */
        public RecordDichVuKetQuaHandler getRecordResult() { return recordResult; }
        /** Hàm `getGetServices`: lấy lấy dịch vụ. */
        public GetDichVuTheoLuotKhamHandler getGetServices() { return getServices; }
        /** Hàm `getOrderService`: lấy yêu cầu dịch vụ. */
        public YeuCauDichVuHandler getOrderService() { return orderService; }
        /** Hàm `getCancelOrder`: lấy hủy yêu cầu. */
        public CancelDichVuYeuCauHandler getCancelOrder() { return cancelOrder; }
        /** Hàm `getServiceRepository`: lấy dịch vụ kho dữ liệu. */
        public IChiDinhDichVuRepository getServiceRepository() { return serviceRepository; }
    }

    /** Trường `services`: dịch vụ. */
    private final Services services;

    public CanLamSangGhepNoiGoc(
            RecordDichVuKetQuaHandler recordResult,
            GetDichVuTheoLuotKhamHandler getServices,
            YeuCauDichVuHandler orderService,
            CancelDichVuYeuCauHandler cancelOrder,
            IChiDinhDichVuRepository serviceRepository) {
        this(new Services(recordResult, getServices, orderService, cancelOrder, serviceRepository));
    }

    public CanLamSangGhepNoiGoc(Services services) {
        this.services = Objects.requireNonNull(services, "services");
    }

    /** Hàm `create`: tạo. */
    public static Services create(
            RecordDichVuKetQuaHandler recordResult,
            GetDichVuTheoLuotKhamHandler getServices,
            YeuCauDichVuHandler orderService,
            CancelDichVuYeuCauHandler cancelOrder,
            IChiDinhDichVuRepository serviceRepository) {
        return new Services(recordResult, getServices, orderService, cancelOrder, serviceRepository);
    }

    /** Hàm `create`: tạo. */
    public static Services create(
            IChiDinhDichVuRepository serviceRepository,
            KiemToanWriteCongKetNoi auditWritePort) {
        Objects.requireNonNull(serviceRepository, "serviceRepository");
        Objects.requireNonNull(auditWritePort, "auditWritePort");

        RecordDichVuKetQuaHandler recordResult = new RecordDichVuKetQuaHandler(serviceRepository, auditWritePort);
        GetDichVuTheoLuotKhamHandler getServices = new GetDichVuTheoLuotKhamHandler(serviceRepository, auditWritePort);
        YeuCauDichVuHandler orderService = new YeuCauDichVuHandler(serviceRepository, auditWritePort);
        CancelDichVuYeuCauHandler cancelOrder = new CancelDichVuYeuCauHandler(serviceRepository, auditWritePort);

        return new Services(recordResult, getServices, orderService, cancelOrder, serviceRepository);
    }

    /** Hàm `create`: tạo. */
    public static Services create(
            IChiDinhDichVuRepository serviceRepository,
            ILuotKhamRepository visitRepository,
            KiemToanWriteCongKetNoi auditWritePort) {
        Objects.requireNonNull(serviceRepository, "serviceRepository");
        Objects.requireNonNull(visitRepository, "visitRepository");
        Objects.requireNonNull(auditWritePort, "auditWritePort");

        RecordDichVuKetQuaHandler recordResult = new RecordDichVuKetQuaHandler(serviceRepository, auditWritePort);
        GetDichVuTheoLuotKhamHandler getServices = new GetDichVuTheoLuotKhamHandler(serviceRepository, auditWritePort);
        YeuCauDichVuHandler orderService = new YeuCauDichVuHandler(serviceRepository, visitRepository, auditWritePort);
        CancelDichVuYeuCauHandler cancelOrder = new CancelDichVuYeuCauHandler(serviceRepository, auditWritePort);

        return new Services(recordResult, getServices, orderService, cancelOrder, serviceRepository);
    }

    /** Hàm `create`: tạo. */
    public static Services create(PhienVongDoiDichVu sessionLifecycle) {
        return createDefault();
    }

    /** Hàm `createDefault`: tạo macdinh. */
    public static Services createDefault() {
        InMemoryChiDinhDichVuRepository serviceRepo = new InMemoryChiDinhDichVuRepository();
        return create(serviceRepo, FALLBACK_AUDIT);
    }

    /** Hàm `getServices`: lấy dịch vụ. */
    public Services getServices() {
        return services;
    }

    /** Hàm `services`: dịch vụ. */
    public Services services() {
        return services;
    }

    /** Hàm `getRecordResult`: lấy ghi nhận kết quả. */
    public RecordDichVuKetQuaHandler getRecordResult() {
        return services.recordResult();
    }

    /** Hàm `getGetServices`: lấy lấy dịch vụ. */
    public GetDichVuTheoLuotKhamHandler getGetServices() {
        return services.getServices();
    }

    /** Hàm `getOrderService`: lấy yêu cầu dịch vụ. */
    public YeuCauDichVuHandler getOrderService() {
        return services.orderService();
    }

    /** Hàm `getCancelOrder`: lấy hủy yêu cầu. */
    public CancelDichVuYeuCauHandler getCancelOrder() {
        return services.cancelOrder();
    }

    /** Hàm `getServiceRepository`: lấy dịch vụ kho dữ liệu. */
    public IChiDinhDichVuRepository getServiceRepository() {
        return services.serviceRepository();
    }

    /**
     * In-memory implementation của {@link IChiDinhDichVuRepository} phục vụ chạy độc lập,
     * demo offline hoặc kiểm thử tích hợp không chạm CSDL thật.
     */
    public static final class InMemoryChiDinhDichVuRepository implements IChiDinhDichVuRepository {

        private static final IUnitOfWork NOOP_UOW = new IUnitOfWork() {
            /** Hàm `saveChanges`: lưu thaydoi. */
            @Override public int saveChanges() { return 1; }
            /** Hàm `saveEntities`: lưu thực thể. */
            @Override public boolean saveEntities() { return true; }
        };

        private final Map<Integer, ChiDinhDichVu> orders = new ConcurrentHashMap<>();
        private final List<ChiTietKetQuaXetNghiem> results = Collections.synchronizedList(new ArrayList<>());
        /** Trường `catalog`: danhmuc. */
        private final Map<Integer, DanhMucDichVu> catalog = new ConcurrentHashMap<>();
        /** Trường `idSeq`: mã `Seq`. */
        private int idSeq = 1;

        /** Hàm `save`: lưu. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository#save`. */
        @Override
        public synchronized void save(ChiDinhDichVu chiDinh) {
            if (chiDinh != null) {
                if (chiDinh.getId() == null || chiDinh.getId() == 0) {
                    chiDinh.setId(idSeq++);
                }
                orders.put(chiDinh.getId(), chiDinh);
            }
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
            return results.stream()
                    .filter(r -> r.getMaChiDinhDichVu() == maChiDinh)
                    .toList();
        }

        /** Hàm `saveKetQua`: lưu ket qua. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository#saveKetQua`. */
        @Override
        public synchronized void saveKetQua(ChiTietKetQuaXetNghiem ketQua) {
            if (ketQua != null) {
                results.add(ketQua);
            }
        }

        /** Hàm `getDichVuById`: lấy dich vu theo mã. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository#getDichVuById`. */
        @Override
        public Optional<DanhMucDichVu> getDichVuById(int maDichVu) {
            return Optional.ofNullable(catalog.get(maDichVu));
        }

        /** Trường `serviceIdSeq`: dịch vụ mã `Seq`. */
        private int serviceIdSeq = 100;

        /** Hàm `searchDichVu`: tìm kiếm dich vu. Gốc khai báo tại `vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository#searchDichVu`. */
        @Override
        public List<DanhMucDichVu> searchDichVu(String keyword, LoaiDichVu loaiDichVu) {
            return catalog.values().stream()
                    .filter(d -> loaiDichVu == null || d.getLoaiDichVu() == loaiDichVu)
                    .filter(d -> {
                        if (keyword == null || keyword.isBlank()) return true;
                        String kw = keyword.toLowerCase();
                        return (d.getTenDichVu() != null && d.getTenDichVu().toLowerCase().contains(kw))
                                || (d.getMaDichVu() != null && d.getMaDichVu().toLowerCase().contains(kw));
                    })
                    .toList();
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
            List<ChiDinhDichVu> all = getAll();
            return new PhanTrangKetQua<>(all, all.size(), pageIndex, pageSize);
        }

        /** Hàm `find`: tìm. Gốc khai báo tại `IChiDinhDichVuRepository#find`. */
        @Override
        public List<ChiDinhDichVu> find(ITieuChuan<ChiDinhDichVu> specification) {
            return getAll();
        }

        @Override
        public boolean exists(ITieuChuan<ChiDinhDichVu> specification) {
            return !orders.isEmpty();
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

        /** Hàm `addDichVu`: thêm dich vu. */
        public synchronized void addDichVu(DanhMucDichVu dv) {
            if (dv != null) {
                if (dv.getId() == null || dv.getId() == 0) {
                    dv.setId(serviceIdSeq++);
                }
                catalog.put(dv.getId(), dv);
            }
        }

        /** Hàm `clear`: xóa. */
        public void clear() {
            orders.clear();
            results.clear();
            catalog.clear();
        }
    }
}
