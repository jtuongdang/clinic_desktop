package vn.clinicmanagement.application.cls;

import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;
import vn.clinicmanagement.domain.repositories.ITieuChuan;
import vn.clinicmanagement.domain.repositories.PhanTrangKetQua;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.domain.service.DanhMucDichVu;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Đồng bộ trạng thái lượt khám (`CHO_CLS` / `DANG_KHAM`) theo vòng đời chỉ định cận lâm sàng.
 */
public final class ClsLuotKhamTrangThaiDongBo {

    public static final GiaoDichRanhGioi DIRECT_TRANSACTIONS = new GiaoDichRanhGioi() {
        @Override
        public <T> KetQua<T> inTransaction(Supplier<KetQua<T>> operation) {
            return operation.get();
        }
    };

    public static final ILuotKhamRepository NOOP_VISIT_REPOSITORY = new ILuotKhamRepository() {
        private final IUnitOfWork uow = new IUnitOfWork() {
            @Override public int saveChanges() { return 1; }
            @Override public boolean saveEntities() { return true; }
        };
        @Override public Optional<LuotKham> getById(int id) { return Optional.empty(); }
        @Override public List<LuotKham> getAll() { return List.of(); }
        @Override public PhanTrangKetQua<LuotKham> getPaged(int p, int s, ITieuChuan<LuotKham> sp) { return new PhanTrangKetQua<>(List.of(), 0, p, s); }
        @Override public List<LuotKham> find(ITieuChuan<LuotKham> sp) { return List.of(); }
        @Override public boolean exists(ITieuChuan<LuotKham> sp) { return false; }
        @Override public int count(ITieuChuan<LuotKham> sp) { return 0; }
        @Override public void add(LuotKham entity) {}
        @Override public void update(LuotKham entity) {}
        @Override public void delete(LuotKham entity) {}
        @Override public IUnitOfWork getUnitOfWork() { return uow; }
        @Override public List<LuotKham> getVisitsByDoctor(int b, LocalDate d) { return List.of(); }
        @Override public int getNextQueueNumber(int c, int k, LocalDate d) { return 1; }
    };

    private ClsLuotKhamTrangThaiDongBo() {}

    public static void markVisitAwaitingClsIfApplicable(
            LuotKham visit,
            DanhMucDichVu service,
            int actorEmployeeId,
            ILuotKhamRepository visitRepository) {
        if (visit == null || service == null || service.getLoaiDichVu() == LoaiDichVu.KHAM) {
            return;
        }
        TrangThaiLuotKham status = visit.getTrangThai();
        if (status == TrangThaiLuotKham.CHO_KHAM || status == TrangThaiLuotKham.DANG_KHAM) {
            if (visit.getThoiGianBatDauKham() == null) {
                visit.setThoiGianBatDauKham(LocalDateTime.now());
            }
            visit.setTrangThai(TrangThaiLuotKham.CHO_CLS);
            visit.markAsUpdated(actorEmployeeId);
            visitRepository.update(visit);
        }
    }

    public static void restoreVisitToExaminingIfNoPendingCls(
            int visitId,
            int actorEmployeeId,
            IChiDinhDichVuRepository serviceRepository,
            ILuotKhamRepository visitRepository) {
        if (visitId <= 0 || visitRepository == null) {
            return;
        }
        Optional<LuotKham> visitOpt = visitRepository.getById(visitId);
        if (visitOpt.isEmpty()) {
            return;
        }
        LuotKham visit = visitOpt.get();
        if (visit.getTrangThai() != TrangThaiLuotKham.CHO_CLS) {
            return;
        }
        List<ChiDinhDichVu> orders = serviceRepository.getByLuotKham(visitId);
        boolean hasPendingCls = false;
        for (ChiDinhDichVu order : orders) {
            if (order == null || !isPendingOrderStatus(order.getTrangThai())) {
                continue;
            }
            Optional<DanhMucDichVu> catalog = serviceRepository.getDichVuById(order.getMaDichVu());
            if (catalog.isPresent() && catalog.get().getLoaiDichVu() == LoaiDichVu.KHAM) {
                continue;
            }
            hasPendingCls = true;
            break;
        }
        if (!hasPendingCls) {
            visit.setTrangThai(TrangThaiLuotKham.DANG_KHAM);
            visit.markAsUpdated(actorEmployeeId);
            visitRepository.update(visit);
        }
    }

    private static boolean isPendingOrderStatus(TrangThaiChiDinhDichVu status) {
        if (status == null) {
            return true;
        }
        return status != TrangThaiChiDinhDichVu.DA_HUY && status != TrangThaiChiDinhDichVu.DA_CO_KET_QUA;
    }
}
