package vn.clinicmanagement.application.inventory;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Dữ liệu tủ trực, tách khỏi kho bán thuốc. Phiếu nhập bù là phiếu chuyển kho đến tủ. */
public interface TuTrucCongKetNoi {
    record Kho(int id, String name) { @Override public String toString() { return name; } }
    record Phieu(int id, String sourceType, String status, LocalDateTime createdAt,
                 LocalDateTime orderDate, String code, String executionCode, String type,
                 String patientType, String department, String location) {}
    record Lo(int id, int itemId, String code, String name, String unit, String batch, LocalDate expiry,
              BigDecimal quantity) {
        public Lo(int id, String code, String name, String unit, String batch, LocalDate expiry, BigDecimal quantity) {
            this(id, 0, code, name, unit, batch, expiry, quantity);
        }
    }
    record NhuCauBu(long id,int itemId,String code,String name,String unit,BigDecimal quantityAvailable) {}
    record TonTheoLo(int itemId,String code,String name,String unit,String batch,LocalDate expiry,
                     BigDecimal opening,BigDecimal received,BigDecimal issued,BigDecimal ending) {}
    record GiaoDichTon(LocalDateTime occurredAt,int itemId,String code,String name,String unit,String batch,
                       String transactionType,String documentCode,BigDecimal delta,BigDecimal before,BigDecimal after) {}
    record BaoCaoTon(List<TonTheoLo> balances,List<GiaoDichTon> transactions) {
        public BaoCaoTon { balances=balances==null?List.of():List.copyOf(balances);transactions=transactions==null?List.of():List.copyOf(transactions); }
    }
    record ClaimNhuCau(long demandId,BigDecimal quantity) {}
    record DongYeuCau(int itemId, BigDecimal quantity,List<ClaimNhuCau> claims) {
        public DongYeuCau(int itemId,BigDecimal quantity){this(itemId,quantity,List.of());}
        public DongYeuCau { claims=claims==null?List.of():List.copyOf(claims); }
    }
    record YeuCau(int cabinetId, int sourceWarehouseId, String note, List<DongYeuCau> lines,
                  PhanQuyenNguCanh context, UUID operationKey) {
        public YeuCau(int cabinetId, int sourceWarehouseId, String note, List<DongYeuCau> lines,
                      PhanQuyenNguCanh context) {
            this(cabinetId, sourceWarehouseId, note, lines, context, UUID.randomUUID());
        }
        public YeuCau { lines = lines == null ? List.of() : List.copyOf(lines); }
    }
    record DongChuyenKho(int id, int itemId, String code, String name, String unit,
                         BigDecimal requested, BigDecimal approved, BigDecimal issued, BigDecimal received) {}
    record LoChuyenKho(int id, int lineId, int batchId, String batch, LocalDate expiry,
                       BigDecimal issued, BigDecimal received) {}
    record ChuyenKho(int id, String code, String status, long version, List<DongChuyenKho> lines,
                     List<LoChuyenKho> lots) {
        public ChuyenKho(int id,String code,String status,long version,List<DongChuyenKho> lines){
            this(id,code,status,version,lines,List.of());
        }
        public ChuyenKho { lines = lines == null ? List.of() : List.copyOf(lines); lots=lots==null?List.of():List.copyOf(lots); }
    }
    record CapDuyet(int lineId, BigDecimal quantity) {}
    record PhanBoXuat(int lineId, int batchId, BigDecimal quantity) {}
    record PhanBoNhan(int allocationId, BigDecimal quantity, String reason) {}
    record DinhMucTuTruc(int itemId,BigDecimal minimum,BigDecimal target,BigDecimal maximum,long version) {}
    record KhoaTuTruc(int id,String name,boolean assigned) { @Override public String toString(){return name;} }
    record LuuDinhMuc(PhanQuyenNguCanh context,int cabinetId,int itemId,BigDecimal minimum,
                      BigDecimal target,BigDecimal maximum,long expectedVersion) {}
    record ThaoTac(int documentId, long expectedVersion, UUID operationKey, String reason,
                   PhanQuyenNguCanh context) {}
    record LenhDuyet(ThaoTac thaoTac, List<CapDuyet> lines) {
        public LenhDuyet { lines = lines == null ? List.of() : List.copyOf(lines); }
    }
    record LenhXuat(ThaoTac thaoTac, List<PhanBoXuat> allocations) {
        public LenhXuat { allocations = allocations == null ? List.of() : List.copyOf(allocations); }
    }
    record LenhNhan(ThaoTac thaoTac, List<PhanBoNhan> allocations) {
        public LenhNhan { allocations = allocations == null ? List.of() : List.copyOf(allocations); }
    }
    List<Kho> getCabinets(int facilityId);
    default List<Kho> getCabinets(PhanQuyenNguCanh context) { return context==null?List.of():getCabinets(context.facilityId()); }
    List<Kho> getSupplyWarehouses(int facilityId);
    default List<Kho> getSupplyWarehouses(PhanQuyenNguCanh context) { return context==null?List.of():getSupplyWarehouses(context.facilityId()); }
    List<Phieu> getTickets(int facilityId, int cabinetId, LocalDateTime from, LocalDateTime until);
    default List<Phieu> getTickets(PhanQuyenNguCanh context,int cabinetId,LocalDateTime from,LocalDateTime until) { return context==null?List.of():getTickets(context.facilityId(),cabinetId,from,until); }
    List<Lo> getStock(int facilityId, int warehouseId);
    default List<Lo> getStock(PhanQuyenNguCanh context,int warehouseId) { return context==null?List.of():getStock(context.facilityId(),warehouseId); }
    /** Danh mục vật tư hoạt động, kể cả vật tư chưa có lô tồn trong kho cấp. */
    default List<Lo> getActiveItems(PhanQuyenNguCanh context) { return List.of(); }
    NhaThuocDanhSachReadCongKetNoi.Bang getDetails(int facilityId, int cabinetId, Phieu ticket);
    default NhaThuocDanhSachReadCongKetNoi.Bang getDetails(PhanQuyenNguCanh context,int cabinetId,Phieu ticket) {
        if(context==null)throw new IllegalStateException("Phiên hiện tại không hợp lệ.");
        return getDetails(context.facilityId(),cabinetId,ticket);
    }
    /** Chỉ lập yêu cầu CHO_DUYET; không trừ kho cấp hoặc cộng tồn tủ. */
    KetQua<String> requestReplenishment(YeuCau request);
    default ChuyenKho getTransfer(int facilityId, int cabinetId, int documentId) {
        throw new UnsupportedOperationException("Chưa hỗ trợ đọc chi tiết chuyển kho.");
    }
    default ChuyenKho getTransfer(PhanQuyenNguCanh context,int cabinetId,int documentId) {
        if(context==null)throw new IllegalStateException("Phiên hiện tại không hợp lệ.");
        return getTransfer(context.facilityId(),cabinetId,documentId);
    }
    default List<DinhMucTuTruc> getPar(PhanQuyenNguCanh context,int cabinetId) { return List.of(); }
    default List<NhuCauBu> getAvailableDemand(PhanQuyenNguCanh context,int cabinetId){return List.of();}
    default BaoCaoTon getStockMovement(PhanQuyenNguCanh context,int warehouseId,LocalDateTime from,LocalDateTime until){return new BaoCaoTon(List.of(),List.of());}
    default List<KhoaTuTruc> getCabinetDepartments(PhanQuyenNguCanh context,int cabinetId) { return List.of(); }
    default KetQua<Boolean> saveCabinetDepartments(PhanQuyenNguCanh context,int cabinetId,List<Integer> departmentIds) {
        return KetQua.failure("NOT_SUPPORTED","Chưa hỗ trợ gán tủ trực cho khoa/phòng.");
    }
    default List<TieuHaoDichVuCongKetNoi.ChoXuLy> getPendingConsumption(PhanQuyenNguCanh context){return List.of();}
    default KetQua<TieuHaoDichVuCongKetNoi.BienNhan> retryPendingConsumption(PhanQuyenNguCanh context,long consumptionId){return KetQua.failure("NOT_SUPPORTED","Chưa hỗ trợ xử lý tiêu hao chờ.");}
    default List<TieuHaoDichVuCongKetNoi.BaoCaoDong> getConsumptionReport(PhanQuyenNguCanh context,LocalDateTime from,LocalDateTime until){return List.of();}
    default KetQua<DinhMucTuTruc> savePar(LuuDinhMuc command) {
        return KetQua.failure("NOT_SUPPORTED","Chưa hỗ trợ cấu hình cơ số tủ trực.");
    }
    default KetQua<ChuyenKho> approve(LenhDuyet command) {
        return KetQua.failure("NOT_SUPPORTED", "Chưa hỗ trợ duyệt phiếu tủ trực.");
    }
    default KetQua<ChuyenKho> issue(LenhXuat command) {
        return KetQua.failure("NOT_SUPPORTED", "Chưa hỗ trợ xuất phiếu tủ trực.");
    }
    default KetQua<ChuyenKho> receive(LenhNhan command) {
        return KetQua.failure("NOT_SUPPORTED", "Chưa hỗ trợ nhận phiếu tủ trực.");
    }
    default KetQua<ChuyenKho> cancel(ThaoTac command) {
        return KetQua.failure("NOT_SUPPORTED", "Chưa hỗ trợ hủy phiếu tủ trực.");
    }
    default KetQua<ChuyenKho> reverse(ThaoTac command) {
        return KetQua.failure("NOT_SUPPORTED", "Chưa hỗ trợ đảo phiếu tủ trực.");
    }
}
