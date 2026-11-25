package vn.clinicmanagement.application.reports;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Read-only ticket register. Date range is [from, toExclusive). */
public interface PhieuBaoCaoReadCongKetNoi {
    enum Loai {
        KHAM_BENH("Phiếu khám"),LICH_HEN("Phiếu hẹn khám"),HOA_DON("Hóa đơn viện phí"),
        DON_THUOC("Đơn thuốc"),CHI_DINH("Phiếu chỉ định CLS"),
        PHIEU_THU("Phiếu thu tiền"),PHIEU_HOAN_TRA("Phiếu hoàn tiền");
        private final String title;Loai(String title){this.title=title;}public String title(){return title;}
    }
    record TruyVan(LocalDateTime from,LocalDateTime toExclusive,int facilityId,Integer departmentId){
        public TruyVan {Objects.requireNonNull(from);Objects.requireNonNull(toExclusive);if(!from.isBefore(toExclusive))throw new IllegalArgumentException("Từ ngày phải trước đến ngày.");if(facilityId<=0)throw new IllegalArgumentException("Chưa xác định cơ sở làm việc.");}
    }
    record Phieu(Loai type,int id,int visitId,String code,LocalDateTime date,String status,String patientCode,
            String patientName,String phone,String department,String author,BigDecimal amount,BigDecimal paid,
            Map<String,String> details){
        public Phieu{Objects.requireNonNull(type);details=details==null?Map.of():Map.copyOf(details);}
        public String key(){return type.name()+":"+id;}
    }
    List<Phieu> load(TruyVan query);
    default List<Phieu> load(TruyVan query,Loai type){return load(query).stream().filter(row->row.type()==type).toList();}
}
