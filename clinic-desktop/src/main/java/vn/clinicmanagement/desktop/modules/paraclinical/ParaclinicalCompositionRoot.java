package vn.clinicmanagement.desktop.modules.paraclinical;

import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.cls.CancelServiceOrderHandler;
import vn.clinicmanagement.application.cls.GetServicesByVisitHandler;
import vn.clinicmanagement.application.cls.OrderServiceHandler;
import vn.clinicmanagement.application.cls.RecordServiceResultHandler;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;
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
public final class ParaclinicalCompositionRoot {

    private static final AuditWritePort FALLBACK_AUDIT = new AuditWritePort() {
        @Override public void appendInCurrentTransaction(SystemAuditEntry entry) {}
        @Override public void appendIndependently(SystemAuditEntry entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {}
        @Override public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {}
    };

    public record Services(
            RecordServiceResultHandler recordResult,
            GetServicesByVisitHandler getServices,
            OrderServiceHandler orderService,
            CancelServiceOrderHandler cancelOrder,
            IChiDinhDichVuRepository serviceRepository) {
        public Services {
            Objects.requireNonNull(recordResult, "recordResult");
            Objects.requireNonNull(getServices, "getServices");
            Objects.requireNonNull(orderService, "orderService");
            Objects.requireNonNull(cancelOrder, "cancelOrder");
            Objects.requireNonNull(serviceRepository, "serviceRepository");
        }

        public RecordServiceResultHandler getRecordResult() { return recordResult; }
        public GetServicesByVisitHandler getGetServices() { return getServices; }
        public OrderServiceHandler getOrderService() { return orderService; }
        public CancelServiceOrderHandler getCancelOrder() { return cancelOrder; }
        public IChiDinhDichVuRepository getServiceRepository() { return serviceRepository; }
    }

    private final Services services;

    public ParaclinicalCompositionRoot(
            RecordServiceResultHandler recordResult,
            GetServicesByVisitHandler getServices,
            OrderServiceHandler orderService,
            CancelServiceOrderHandler cancelOrder,
            IChiDinhDichVuRepository serviceRepository) {
        this(new Services(recordResult, getServices, orderService, cancelOrder, serviceRepository));
    }

    public ParaclinicalCompositionRoot(Services services) {
        this.services = Objects.requireNonNull(services, "services");
    }

    public static Services create(
            RecordServiceResultHandler recordResult,
            GetServicesByVisitHandler getServices,
            OrderServiceHandler orderService,
            CancelServiceOrderHandler cancelOrder,
            IChiDinhDichVuRepository serviceRepository) {
        return new Services(recordResult, getServices, orderService, cancelOrder, serviceRepository);
    }

    public static Services create(
            IChiDinhDichVuRepository serviceRepository,
            AuditWritePort auditWritePort) {
        Objects.requireNonNull(serviceRepository, "serviceRepository");
        Objects.requireNonNull(auditWritePort, "auditWritePort");

        RecordServiceResultHandler recordResult = new RecordServiceResultHandler(serviceRepository, auditWritePort);
        GetServicesByVisitHandler getServices = new GetServicesByVisitHandler(serviceRepository, auditWritePort);
        OrderServiceHandler orderService = new OrderServiceHandler(serviceRepository, auditWritePort);
        CancelServiceOrderHandler cancelOrder = new CancelServiceOrderHandler(serviceRepository, auditWritePort);

        return new Services(recordResult, getServices, orderService, cancelOrder, serviceRepository);
    }

    public static Services create(
            IChiDinhDichVuRepository serviceRepository,
            ILuotKhamRepository visitRepository,
            AuditWritePort auditWritePort) {
        Objects.requireNonNull(serviceRepository, "serviceRepository");
        Objects.requireNonNull(visitRepository, "visitRepository");
        Objects.requireNonNull(auditWritePort, "auditWritePort");

        RecordServiceResultHandler recordResult = new RecordServiceResultHandler(serviceRepository, auditWritePort);
        GetServicesByVisitHandler getServices = new GetServicesByVisitHandler(serviceRepository, auditWritePort);
        OrderServiceHandler orderService = new OrderServiceHandler(serviceRepository, visitRepository, auditWritePort);
        CancelServiceOrderHandler cancelOrder = new CancelServiceOrderHandler(serviceRepository, auditWritePort);

        return new Services(recordResult, getServices, orderService, cancelOrder, serviceRepository);
    }

    public static Services create(SessionLifecycleService sessionLifecycle) {
        return createDefault();
    }

    public static Services createDefault() {
        InMemoryChiDinhDichVuRepository serviceRepo = new InMemoryChiDinhDichVuRepository();
        return create(serviceRepo, FALLBACK_AUDIT);
    }

    public Services getServices() {
        return services;
    }

    public Services services() {
        return services;
    }

    public RecordServiceResultHandler getRecordResult() {
        return services.recordResult();
    }

    public GetServicesByVisitHandler getGetServices() {
        return services.getServices();
    }

    public OrderServiceHandler getOrderService() {
        return services.orderService();
    }

    public CancelServiceOrderHandler getCancelOrder() {
        return services.cancelOrder();
    }

    public IChiDinhDichVuRepository getServiceRepository() {
        return services.serviceRepository();
    }

    /**
     * In-memory implementation của {@link IChiDinhDichVuRepository} phục vụ chạy độc lập,
     * demo offline hoặc kiểm thử tích hợp không chạm CSDL thật.
     */
    public static final class InMemoryChiDinhDichVuRepository implements IChiDinhDichVuRepository {

        private static final IUnitOfWork NOOP_UOW = new IUnitOfWork() {
            @Override public int saveChanges() { return 1; }
            @Override public boolean saveEntities() { return true; }
        };

        private final Map<Integer, ChiDinhDichVu> orders = new ConcurrentHashMap<>();
        private final List<ChiTietKetQuaXetNghiem> results = Collections.synchronizedList(new ArrayList<>());
        private final Map<Integer, DanhMucDichVu> catalog = new ConcurrentHashMap<>();
        private int idSeq = 1;

        @Override
        public synchronized void save(ChiDinhDichVu chiDinh) {
            if (chiDinh != null) {
                if (chiDinh.getId() == null || chiDinh.getId() == 0) {
                    chiDinh.setId(idSeq++);
                }
                orders.put(chiDinh.getId(), chiDinh);
            }
        }

        @Override
        public synchronized List<ChiDinhDichVu> getByLuotKham(int maLuotKham) {
            return orders.values().stream()
                    .filter(c -> c.getMaLuotKham() == maLuotKham)
                    .toList();
        }

        @Override
        public synchronized List<ChiTietKetQuaXetNghiem> getKetQuaByChiDinh(int maChiDinh) {
            return results.stream()
                    .filter(r -> r.getMaChiDinhDichVu() == maChiDinh)
                    .toList();
        }

        @Override
        public synchronized void saveKetQua(ChiTietKetQuaXetNghiem ketQua) {
            if (ketQua != null) {
                results.add(ketQua);
            }
        }

        @Override
        public Optional<DanhMucDichVu> getDichVuById(int maDichVu) {
            return Optional.ofNullable(catalog.get(maDichVu));
        }

        private int serviceIdSeq = 100;

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
            List<ChiDinhDichVu> all = getAll();
            return new PagedResult<>(all, all.size(), pageIndex, pageSize);
        }

        @Override
        public List<ChiDinhDichVu> find(ISpecification<ChiDinhDichVu> specification) {
            return getAll();
        }

        @Override
        public boolean exists(ISpecification<ChiDinhDichVu> specification) {
            return !orders.isEmpty();
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

        public synchronized void addDichVu(DanhMucDichVu dv) {
            if (dv != null) {
                if (dv.getId() == null || dv.getId() == 0) {
                    dv.setId(serviceIdSeq++);
                }
                catalog.put(dv.getId(), dv);
            }
        }

        public void clear() {
            orders.clear();
            results.clear();
            catalog.clear();
        }
    }
}
