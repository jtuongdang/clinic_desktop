package vn.clinicmanagement.desktop.modules.paraclinical;

import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.cls.cancel.CancelDichVuYeuCauHandler;
import vn.clinicmanagement.application.cls.get.GetDichVuTheoLuotKhamHandler;
import vn.clinicmanagement.application.cls.request.YeuCauDichVuHandler;
import vn.clinicmanagement.application.cls.record.RecordDichVuKetQuaHandler;
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
        @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
    };

    public record Services(
            RecordDichVuKetQuaHandler recordResult,
            GetDichVuTheoLuotKhamHandler getServices,
            YeuCauDichVuHandler orderService,
            CancelDichVuYeuCauHandler cancelOrder,
            IChiDinhDichVuRepository serviceRepository) {
        public Services {
            Objects.requireNonNull(recordResult, "recordResult");
            Objects.requireNonNull(getServices, "getServices");
            Objects.requireNonNull(orderService, "orderService");
            Objects.requireNonNull(cancelOrder, "cancelOrder");
            Objects.requireNonNull(serviceRepository, "serviceRepository");
        }

        public RecordDichVuKetQuaHandler getRecordResult() { return recordResult; }
        public GetDichVuTheoLuotKhamHandler getGetServices() { return getServices; }
        public YeuCauDichVuHandler getOrderService() { return orderService; }
        public CancelDichVuYeuCauHandler getCancelOrder() { return cancelOrder; }
        public IChiDinhDichVuRepository getServiceRepository() { return serviceRepository; }
    }

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

    public static Services create(
            RecordDichVuKetQuaHandler recordResult,
            GetDichVuTheoLuotKhamHandler getServices,
            YeuCauDichVuHandler orderService,
            CancelDichVuYeuCauHandler cancelOrder,
            IChiDinhDichVuRepository serviceRepository) {
        return new Services(recordResult, getServices, orderService, cancelOrder, serviceRepository);
    }

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

    public static Services create(
            IChiDinhDichVuRepository serviceRepository,
            ILuotKhamRepository visitRepository,
            KiemToanWriteCongKetNoi auditWritePort) {
        Objects.requireNonNull(serviceRepository, "serviceRepository");
        Objects.requireNonNull(visitRepository, "visitRepository");
        Objects.requireNonNull(auditWritePort, "auditWritePort");

        RecordDichVuKetQuaHandler recordResult = new RecordDichVuKetQuaHandler(
                serviceRepository, visitRepository, null, auditWritePort);
        GetDichVuTheoLuotKhamHandler getServices = new GetDichVuTheoLuotKhamHandler(serviceRepository, auditWritePort);
        YeuCauDichVuHandler orderService = new YeuCauDichVuHandler(serviceRepository, visitRepository, auditWritePort);
        CancelDichVuYeuCauHandler cancelOrder = new CancelDichVuYeuCauHandler(
                serviceRepository, visitRepository, null, auditWritePort);

        return new Services(recordResult, getServices, orderService, cancelOrder, serviceRepository);
    }

    public static Services create(PhienVongDoiDichVu sessionLifecycle) {
        return createDefault();
    }

    public static Services createProduction() {
        var lifecycle=new PhienVongDoiDichVu(vn.clinicmanagement.desktop.core.session.NguoiDungPhien.getInstance(),
                new vn.clinicmanagement.application.session.PhienHetGioChinhSach(java.time.Duration.ofMinutes(15),java.time.Duration.ofHours(12)),
                java.time.Clock.systemUTC(),FALLBACK_AUDIT);
        return createProduction(lifecycle);
    }

    public static Services createProduction(PhienVongDoiDichVu lifecycle) {
        var uow = new vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork();
        var transactions = new vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy(uow);
        var connections = new vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap(transactions);
        var audit = new vn.clinicmanagement.infrastructure.persistence.system.JdbcKiemToanRepository(
                connections, uow, vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi::getConnection);
        var orders = new vn.clinicmanagement.infrastructure.persistence.service.ChiDinhDichVuRepository(connections, uow);
        var visits = new vn.clinicmanagement.infrastructure.persistence.examination.LuotKhamRepository(connections, uow);
        var authorization=new vn.clinicmanagement.application.authorization.PhanQuyenDichVu(lifecycle,
                new vn.clinicmanagement.infrastructure.persistence.identity.NhanVienRepository(connections,uow),
                new vn.clinicmanagement.infrastructure.persistence.system.HeThongRepository(connections,uow));
        var consumption=new vn.clinicmanagement.infrastructure.persistence.inventory.TieuHaoDichVuRepository(connections,transactions,authorization,audit);
        var billing = new vn.clinicmanagement.application.billing.ClsVienPhiDongBo(
                new vn.clinicmanagement.infrastructure.persistence.billing.HoaDonRepository(connections, uow));
        return new Services(new RecordDichVuKetQuaHandler(orders, visits, transactions, audit,consumption),
                new GetDichVuTheoLuotKhamHandler(orders, audit),
                new YeuCauDichVuHandler(orders, visits, transactions, audit, billing),
                new CancelDichVuYeuCauHandler(orders, visits, transactions, audit, billing), orders);
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

    public RecordDichVuKetQuaHandler getRecordResult() {
        return services.recordResult();
    }

    public GetDichVuTheoLuotKhamHandler getGetServices() {
        return services.getServices();
    }

    public YeuCauDichVuHandler getOrderService() {
        return services.orderService();
    }

    public CancelDichVuYeuCauHandler getCancelOrder() {
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
                results.removeIf(r -> r.getMaChiDinhDichVu() == ketQua.getMaChiDinhDichVu()
                        && Objects.equals(r.getTenChiSo(), ketQua.getTenChiSo()));
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
        public PhanTrangKetQua<ChiDinhDichVu> getPaged(int pageIndex, int pageSize, ITieuChuan<ChiDinhDichVu> specification) {
            List<ChiDinhDichVu> all = getAll();
            return new PhanTrangKetQua<>(all, all.size(), pageIndex, pageSize);
        }

        @Override
        public List<ChiDinhDichVu> find(ITieuChuan<ChiDinhDichVu> specification) {
            return getAll();
        }

        @Override
        public boolean exists(ITieuChuan<ChiDinhDichVu> specification) {
            return !orders.isEmpty();
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
