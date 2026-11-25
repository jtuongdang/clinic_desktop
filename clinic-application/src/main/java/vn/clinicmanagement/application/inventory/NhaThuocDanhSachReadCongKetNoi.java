package vn.clinicmanagement.application.inventory;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
public interface NhaThuocDanhSachReadCongKetNoi {
    enum DanhSach { THUOC_TRONG_KHO, PHIEU_XUAT_LE, DU_TRU_THUOC }
    enum NgayLoc { TONG_HOP, NGAY_TAO, NGAY_NHAP_XUAT }
    record Phieu(int id,String type,String code,String status,LocalDateTime createdAt,LocalDateTime date,
                 LocalDateTime cancelledAt,String cancelledBy,int warehouseId,String warehouse,String destination,
                 String detailType,BigDecimal amount) {}
    record DonThuocChoCap(int id,String code,String status,LocalDateTime createdAt,LocalDateTime date,
                          Integer warehouseId,String warehouse,String patientName,String doctorName,
                          String visitCode,BigDecimal totalAmount,boolean paid) {
        public DonThuocChoCap(int id,String code,String status,LocalDateTime createdAt,LocalDateTime date,
                              Integer warehouseId,String warehouse,String patientName,String doctorName,
                              String visitCode,BigDecimal totalAmount) {
            this(id,code,status,createdAt,date,warehouseId,warehouse,patientName,doctorName,visitCode,totalAmount,false);
        }
    }
    record PhieuChiTietDong(String itemCode,String itemName,String unit,String batch,LocalDate expiry,
                            BigDecimal quantity,BigDecimal price,BigDecimal amount) {}
    record TheKhoDong(LocalDateTime time,String warehouse,String batch,String type,
                      BigDecimal before,BigDecimal change,BigDecimal after,String docCode,String actor) {}
    record PhieuChiTietDongDayDu(String itemCode,String itemName,String activeIngredient,String unit,
                                 BigDecimal stockAvailable,BigDecimal requestedQty,BigDecimal approvedQty,
                                 BigDecimal unitPrice,BigDecimal amount,String registrationNo,String batch,LocalDate expiry) {}
    record PhieuChiTietDayDu(String code,String titleTarget,String status,LocalDateTime createdAt,String createdByName,
                             LocalDateTime approvedAt,String approvedByName,LocalDateTime paidAt,String paidByName,
                             LocalDateTime issuedAt,String issuedByName,String warehouseName,
                             BigDecimal invoiceTotal,BigDecimal paidAmount,BigDecimal debtAmount,
                             List<PhieuChiTietDongDayDu> lines) {
        public PhieuChiTietDayDu { lines = lines != null ? List.copyOf(lines) : List.of(); }
    }
    record Bang(List<String> columns,List<List<Object>> rows){public Bang {columns=List.copyOf(columns);rows=rows.stream().map(List::copyOf).toList();}}
    record Thuoc(int id,long version,String code,String equivalentCode,String name,String ingredient,String strength,
                 String concentration,String volume,String unit,String group,Boolean insured,String traditionalCode,
                 String warehouse,String batch,LocalDate expiry,BigDecimal stock) {}
    default List<Thuoc> getDrugDirectory(int facilityId,Integer warehouseId){return List.of();}
    default void saveDrugMetadata(Thuoc drug,int facilityId,Integer warehouseId,int employeeId){throw new UnsupportedOperationException("Chưa kết nối lưu thông tin thuốc.");}
    default void saveDrugForm(Thuoc drug,Map<String,String> fields,int facilityId,int employeeId){throw new UnsupportedOperationException("Chưa kết nối lưu biểu mẫu thuốc.");}
    default void updateDrugForm(int itemId,Thuoc drug,Map<String,String> fields,int facilityId,int employeeId){saveDrugForm(drug,fields,facilityId,employeeId);}
    default Map<String,String> getDrugForm(int itemId,int facilityId){return Map.of();}
    default Map<String,String> getDrugFullValues(int itemId,int facilityId){return getDrugForm(itemId,facilityId);}
    default void setDrugActive(int itemId,boolean active,int facilityId,int employeeId){throw new UnsupportedOperationException("Chưa kết nối khóa/mở khóa thuốc.");}
    default List<TheKhoDong> getStockCard(int itemId,int facilityId,Integer warehouseId){return List.of();}
    default List<DonThuocChoCap> getPrescriptions(int facilityId,LocalDateTime from,LocalDateTime to){return List.of();}
    default List<DonThuocChoCap> getPrescriptions(int facilityId,LocalDateTime from,LocalDateTime to,NgayLoc dateField){return getPrescriptions(facilityId,from,to);}
    default List<PhieuChiTietDong> getTicketDetails(String ticketType,int ticketId,int facilityId){return List.of();}
    default Optional<PhieuChiTietDayDu> getTicketFullDetail(String ticketType,int ticketId,int facilityId){return Optional.empty();}
    List<Phieu> getTickets(int facilityId,LocalDateTime from,LocalDateTime to);
    default List<Phieu> getTickets(int facilityId,LocalDateTime from,LocalDateTime to,NgayLoc dateField){return getTickets(facilityId,from,to);}
    Bang getList(DanhSach type,int facilityId,Integer warehouseId,LocalDateTime from,LocalDateTime to);
}
