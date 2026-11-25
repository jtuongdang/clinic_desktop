package vn.clinicmanagement.desktop.modules.system.services;

import vn.clinicmanagement.domain.service.DanhMucDichVu;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import java.math.BigDecimal;

/** Read model keeps unavailable tariff fields null instead of inventing prices. */
public record DichVuHang(int id,String code,String equivalent,String name,String unit,
        BigDecimal insurancePrice,BigDecimal hospitalPrice,BigDecimal servicePrice,String category,String group) {
    public DichVuHang {
        if(id<=0||code==null||code.isBlank()||name==null||name.isBlank())throw new IllegalArgumentException("Dịch vụ thiếu mã hoặc tên.");
        equivalent=equivalent==null?"":equivalent;unit=unit==null?"":unit;
        category=category==null||category.isBlank()?"Chuyên khoa":category;
        group=group==null||group.isBlank()?category:group;
    }
    public String key(){return String.valueOf(id);}
    public static DichVuHang from(vn.clinicmanagement.application.catalog.DichVuCauHinh value){
        return new DichVuHang(value.id(),value.value("code"),value.value("insuranceCode"),value.value("name"),value.value("unit"),
                vn.clinicmanagement.application.catalog.DichVuCauHinh.number(value.value("insurancePrice")),
                vn.clinicmanagement.application.catalog.DichVuCauHinh.number(value.value("hospitalPrice")),
                vn.clinicmanagement.application.catalog.DichVuCauHinh.number(value.value("servicePrice")),value.value("category"),value.value("group"));
    }
    public static DichVuHang from(DanhMucDichVu item){
        LoaiDichVu type=item.getLoaiDichVu();
        String category=type==LoaiDichVu.KHAM?"Khám bệnh":type==LoaiDichVu.XET_NGHIEM?"Xét nghiệm":type==LoaiDichVu.CHAN_DOAN_HINH_ANH?"Chẩn đoán hình ảnh":"Chuyên khoa";
        return new DichVuHang(item.getId(),item.getMaDichVu(),"",item.getTenDichVu(),"",item.getGiaBHYT(),null,item.getDonGia(),category,item.getNhomDichVu());
    }
}
