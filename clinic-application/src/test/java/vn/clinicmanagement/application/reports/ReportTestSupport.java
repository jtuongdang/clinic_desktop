package vn.clinicmanagement.application.reports;

import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.domain.billing.CongNoBenhNhan;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.billing.PhieuThu;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.inventory.DonDatHang;
import vn.clinicmanagement.domain.inventory.Item;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.PhieuNhapKho;
import vn.clinicmanagement.domain.inventory.PhieuXuatKho;
import vn.clinicmanagement.domain.inventory.Stocktake;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;
import vn.clinicmanagement.domain.repositories.IKhoRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;
import vn.clinicmanagement.domain.repositories.ISpecification;
import vn.clinicmanagement.domain.repositories.PagedResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Các lớp Fake In-Memory hỗ trợ kiểm thử phân hệ Báo cáo Thống kê (RPT-01).
 * Tuân thủ Hợp đồng Cách ly 18847587: 100% in-memory, zero DB access.
 */
public final class ReportTestSupport {

    public static class InMemoryInvoiceRepository implements IHoaDonRepository {
        public final Map<Integer, HoaDon> invoices = new HashMap<>();
        public final List<PhieuThu> receipts = new ArrayList<>();
        private int seq = 1;

        @Override
        public Optional<HoaDon> getBySoHoaDon(String soHoaDon) {
            return invoices.values().stream().filter(h -> soHoaDon.equals(h.getSoHoaDon())).findFirst();
        }

        @Override
        public Optional<HoaDon> getByLuotKham(int maLuotKham) {
            return invoices.values().stream().filter(h -> h.getMaLuotKham() == maLuotKham).findFirst();
        }

        @Override
        public List<HoaDon> getByBenhNhan(int maBenhNhan) {
            return invoices.values().stream().filter(h -> h.getMaBenhNhan() == maBenhNhan).toList();
        }

        @Override
        public HoaDon save(HoaDon hoaDon) {
            if (hoaDon.getId() == null || hoaDon.getId() == 0) {
                hoaDon.setId(seq++);
            }
            invoices.put(hoaDon.getId(), hoaDon);
            return hoaDon;
        }

        @Override
        public void savePhieuThu(PhieuThu phieuThu) {
            receipts.add(phieuThu);
        }

        @Override
        public List<PhieuThu> getPhieuThuByHoaDon(int maHoaDon) {
            return receipts.stream().filter(p -> p.getMaHoaDon() == maHoaDon).toList();
        }

        @Override
        public void saveCongNo(CongNoBenhNhan congNo) {}

        @Override
        public Optional<CongNoBenhNhan> getCongNoByHoaDon(int maHoaDon) {
            return Optional.empty();
        }

        @Override
        public List<CongNoBenhNhan> getCongNoByBenhNhan(int maBenhNhan) {
            return List.of();
        }

        @Override
        public Optional<HoaDon> getById(int id) {
            return Optional.ofNullable(invoices.get(id));
        }

        @Override
        public List<HoaDon> getAll() {
            return new ArrayList<>(invoices.values());
        }

        @Override
        public PagedResult<HoaDon> getPaged(int pageIndex, int pageSize, ISpecification<HoaDon> specification) {
            return new PagedResult<>(new ArrayList<>(invoices.values()), invoices.size(), pageIndex, pageSize);
        }

        @Override
        public List<HoaDon> find(ISpecification<HoaDon> specification) {
            return new ArrayList<>(invoices.values());
        }

        @Override
        public boolean exists(ISpecification<HoaDon> specification) {
            return !invoices.isEmpty();
        }

        @Override
        public int count(ISpecification<HoaDon> specification) {
            return invoices.size();
        }

        @Override
        public void add(HoaDon entity) {
            save(entity);
        }

        @Override
        public void update(HoaDon entity) {
            save(entity);
        }

        @Override
        public void delete(HoaDon entity) {
            if (entity != null && entity.getId() != null) {
                invoices.remove(entity.getId());
            }
        }

        @Override
        public IUnitOfWork getUnitOfWork() {
            return null;
        }
    }

    public static class InMemoryVisitRepository implements ILuotKhamRepository {
        public final Map<Integer, LuotKham> visits = new HashMap<>();
        private int seq = 1;

        public LuotKham save(LuotKham v) {
            if (v.getId() == null || v.getId() == 0) {
                v.setId(seq++);
            }
            visits.put(v.getId(), v);
            return v;
        }

        @Override
        public List<LuotKham> getVisitsByDoctor(int maBacSi, LocalDate date) {
            return visits.values().stream()
                    .filter(v -> v.getMaBacSi() != null && v.getMaBacSi() == maBacSi)
                    .toList();
        }

        @Override
        public int getNextQueueNumber(int maCoSo, int maKhoa, LocalDate date) {
            return seq;
        }

        @Override
        public Optional<LuotKham> getById(int id) {
            return Optional.ofNullable(visits.get(id));
        }

        @Override
        public List<LuotKham> getAll() {
            return new ArrayList<>(visits.values());
        }

        @Override
        public PagedResult<LuotKham> getPaged(int pageIndex, int pageSize, ISpecification<LuotKham> specification) {
            return new PagedResult<>(new ArrayList<>(visits.values()), visits.size(), pageIndex, pageSize);
        }

        @Override
        public List<LuotKham> find(ISpecification<LuotKham> specification) {
            return new ArrayList<>(visits.values());
        }

        @Override
        public boolean exists(ISpecification<LuotKham> specification) {
            return !visits.isEmpty();
        }

        @Override
        public int count(ISpecification<LuotKham> specification) {
            return visits.size();
        }

        @Override
        public void add(LuotKham entity) {
            save(entity);
        }

        @Override
        public void update(LuotKham entity) {
            save(entity);
        }

        @Override
        public void delete(LuotKham entity) {
            if (entity != null && entity.getId() != null) {
                visits.remove(entity.getId());
            }
        }

        @Override
        public IUnitOfWork getUnitOfWork() {
            return null;
        }
    }

    public static class InMemoryKhoRepository implements IKhoRepository {
        public final Map<Integer, Item> items = new HashMap<>();
        public final Map<Integer, TonKho> batches = new HashMap<>();
        private int itemSeq = 1;
        private int batchSeq = 1;

        @Override
        public Optional<Item> getItemById(int id) {
            return Optional.ofNullable(items.get(id));
        }

        @Override
        public Optional<Item> getItemByCode(String code) {
            return items.values().stream().filter(i -> code.equals(i.getMaItem())).findFirst();
        }

        @Override
        public Item saveItem(Item item) {
            if (item.getId() == null || item.getId() == 0) {
                item.setId(itemSeq++);
            }
            items.put(item.getId(), item);
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
            return batches.values().stream()
                    .filter(b -> b.getMaKho() == maKho && b.getMaItem() == maItem)
                    .toList();
        }

        @Override
        public List<TonKho> getBatchesByKho(int maKho) {
            return batches.values().stream()
                    .filter(b -> b.getMaKho() == maKho)
                    .toList();
        }

        @Override
        public Optional<TonKho> getBatchById(int id) {
            return Optional.ofNullable(batches.get(id));
        }

        @Override
        public TonKho saveTonKho(TonKho tonKho) {
            if (tonKho.getId() == null || tonKho.getId() == 0) {
                tonKho.setId(batchSeq++);
            }
            batches.put(tonKho.getId(), tonKho);
            return tonKho;
        }

        @Override
        public PhieuNhapKho savePhieuNhapKho(PhieuNhapKho phieuNhapKho) { return phieuNhapKho; }

        @Override
        public Optional<PhieuNhapKho> getPhieuNhapKhoById(int id) { return Optional.empty(); }

        @Override
        public PhieuXuatKho savePhieuXuatKho(PhieuXuatKho phieuXuatKho) { return phieuXuatKho; }

        @Override
        public Optional<PhieuXuatKho> getPhieuXuatKhoById(int id) { return Optional.empty(); }

        @Override
        public DonDatHang savePurchaseOrder(DonDatHang donDatHang) { return donDatHang; }

        @Override
        public Optional<DonDatHang> getPurchaseOrderById(int id) { return Optional.empty(); }

        @Override
        public Stocktake saveStocktake(Stocktake stocktake) { return stocktake; }

        @Override
        public Optional<Stocktake> getStocktakeById(int id) { return Optional.empty(); }

        @Override
        public int saveChanges() { return 1; }

        @Override
        public boolean saveEntities() { return true; }
    }

    public static class RecordingAuditWritePort implements AuditWritePort {
        public final List<SystemAuditEntry> systemAudits = new ArrayList<>();
        public final List<MedicalRecordAuditEntry> medicalAudits = new ArrayList<>();

        @Override
        public void appendInCurrentTransaction(SystemAuditEntry entry) {
            systemAudits.add(entry);
        }

        @Override
        public void appendIndependently(SystemAuditEntry entry) {
            systemAudits.add(entry);
        }

        @Override
        public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {
            medicalAudits.add(entry);
        }

        @Override
        public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {
            medicalAudits.add(entry);
        }
    }
}
