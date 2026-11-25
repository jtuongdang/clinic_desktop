package vn.clinicmanagement.desktop.modules.billing.examination;

import vn.clinicmanagement.application.billing.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public record ThanhToanKhamBenhDuLieu(String patientName,String patientCode,String birthYear,String age,
        String gender,String address,String insuranceNumber,int insuranceRate,String insuranceRemaining,
        String visitCode,HoaDonDuLieu invoice,List<Dong> rows,Map<Integer,String> collectors,String message,String subject) {
    public ThanhToanKhamBenhDuLieu(String patientName,String patientCode,String birthYear,String age,
            String gender,String address,String insuranceNumber,int insuranceRate,String insuranceRemaining,
            String visitCode,HoaDonDuLieu invoice,List<Dong> rows,Map<Integer,String> collectors,String message){
        this(patientName,patientCode,birthYear,age,gender,address,insuranceNumber,insuranceRate,insuranceRemaining,
                visitCode,invoice,rows,collectors,message,safe(insuranceNumber).isEmpty()?"Viện phí":"BHYT");
    }
    public ThanhToanKhamBenhDuLieu {rows=rows==null?List.of():List.copyOf(rows);collectors=collectors==null?Map.of():Map.copyOf(collectors);}
    public record Dong(int id,LocalDateTime orderedAt,String equivalentCode,String description,String serviceType,
            String subject,String paymentSource,BigDecimal insurancePrice,String unit,BigDecimal quantity,
            BigDecimal unitPrice,BigDecimal amount,String category,String department,String doctor,boolean confirmed,
            String payerCode,BigDecimal catalogServicePrice,BigDecimal insuranceAmount,BigDecimal discountAmount,BigDecimal patientAmount) {
        public Dong(int id,LocalDateTime orderedAt,String equivalentCode,String description,String serviceType,
                String subject,String paymentSource,BigDecimal insurancePrice,String unit,BigDecimal quantity,
                BigDecimal unitPrice,BigDecimal amount,String category,String department,String doctor,boolean confirmed) {
            this(id,orderedAt,equivalentCode,description,serviceType,subject,paymentSource,insurancePrice,unit,quantity,
                    unitPrice,amount,category,department,doctor,confirmed,"THU_PHI",unitPrice,BigDecimal.ZERO,BigDecimal.ZERO,amount);
        }
    }
    public static List<Dong> mapRows(HoaDonDuLieu invoice,Map<Integer,ThanhToanDichVuReadCongKetNoi.NguonDong> sources){
        return mapRows(invoice,sources,"");
    }
    public static List<Dong> mapRows(HoaDonDuLieu invoice,Map<Integer,ThanhToanDichVuReadCongKetNoi.NguonDong> sources,String visitSubject){
        if(invoice==null||invoice.lines()==null)return List.of();List<Dong> rows=new ArrayList<>();
        boolean selfPay=List.of("THU_PHI","VIEN_PHI","THU PHÍ","VIỆN PHÍ").contains(safe(visitSubject).toUpperCase(Locale.ROOT));
        boolean paid = invoice.status() == vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon.DA_THANH_TOAN;
        for(var line:invoice.lines()){
            var s=sources.get(line.id());String category=category(s!=null?s.category():line.itemType());
            String rawPayer=safe(line.payerType()).toUpperCase(Locale.ROOT);
            String subject;
            String paySource;
            if(!rawPayer.isEmpty()){
                switch(rawPayer){
                    case "BHYT" -> {
                        subject="BHYT";
                        paySource=line.coverageRate()>0?"Quỹ BHYT ("+line.coverageRate()+"%)":"Quỹ BHYT / Đồng chi trả";
                    }
                    case "BHYT_DICH_VU" -> {
                        subject="BHYT + Dịch vụ";
                        paySource=line.coverageRate()>0?"Quỹ BHYT ("+line.coverageRate()+"%) + BN tự trả":"Quỹ BHYT + BN tự trả chênh lệch";
                    }
                    case "MIEN_PHI" -> {
                        subject="Miễn phí";
                        paySource="Miễn giảm dịch vụ";
                    }
                    default -> {
                        subject="Thu phí";
                        paySource="Bệnh nhân tự trả";
                    }
                }
            }else{
                subject=selfPay?"Thu phí":"Chưa phân bổ";
                paySource=selfPay?"Bệnh nhân tự trả":"Quỹ BHYT / Đồng chi trả";
            }
            String serviceLabel="Thăm khám".equals(category)?"Khám bệnh":category;
            BigDecimal catInsPrice = s != null && s.catalogInsurancePrice() != null
                    ? s.catalogInsurancePrice()
                    : (line.insurancePrice() != null && line.insurancePrice().signum() > 0 ? line.insurancePrice() : null);
            BigDecimal catSvcPrice = s != null && s.catalogServicePrice() != null && s.catalogServicePrice().signum() > 0
                    ? s.catalogServicePrice()
                    : line.unitPrice();
            rows.add(new Dong(line.id(),s==null?null:s.orderedAt(),s==null?"":s.equivalentCode(),line.description(),
                    serviceLabel,subject,paySource,catInsPrice,
                    s==null?"":s.unit(),line.quantity(),line.unitPrice(),line.lineTotal(),category,
                    s==null?"":s.department(),s==null?"":s.doctor(),paid,
                    rawPayer.isEmpty()?"THU_PHI":rawPayer,catSvcPrice,
                    zero(line.insuranceAmount()),zero(line.discountAmount()),zero(line.patientAmount())));
        }return List.copyOf(rows);
    }
    static String category(String type){return switch(safe(type).toUpperCase(Locale.ROOT)){
        case "KHAM","KHAM_BENH","DICH_VU_KHAM"->"Thăm khám";
        case "XET_NGHIEM"->"Xét nghiệm";
        case "CDHA","CHAN_DOAN_HINH_ANH"->"Chẩn đoán hình ảnh";
        case "THUOC"->"Thuốc";
        case "DON_THUOC"->"Thuốc/Vật tư";
        case "VAT_TU"->"Vật tư";
        case "PHAU_THUAT","THU_THUAT"->"Phẫu thuật";
        default->"Dịch vụ khác";};}
    static String safe(String s){return s==null?"":s.trim();}
    static BigDecimal zero(BigDecimal n){return n==null?BigDecimal.ZERO:n;}
}
