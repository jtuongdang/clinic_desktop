package vn.clinicmanagement.desktop.modules.system.services;

import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.infrastructure.persistence.*;
import java.util.List;
import vn.clinicmanagement.application.catalog.DichVuCauHinh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.infrastructure.persistence.service.DichVuCauHinhRepository;
import vn.clinicmanagement.infrastructure.persistence.system.JdbcKiemToanRepository;

@FunctionalInterface
public interface DichVuDanhMucNguon {
    List<DichVuHang> load();
    default List<vn.clinicmanagement.application.catalog.VatTuHaoPhi> loadMaterials(){return List.of();}
    default DichVuCauHinh loadDetail(DichVuHang item){
        var fields=new java.util.LinkedHashMap<String,String>();
        fields.put("code",item.code());fields.put("name",item.name());fields.put("category",item.category());fields.put("group",item.group());
        fields.put("unit",item.unit());fields.put("insuranceCode",item.equivalent());
        if(item.insurancePrice()!=null)fields.put("insurancePrice",item.insurancePrice().toPlainString());
        if(item.hospitalPrice()!=null)fields.put("hospitalPrice",item.hospitalPrice().toPlainString());
        if(item.servicePrice()!=null)fields.put("servicePrice",item.servicePrice().toPlainString());
        return new DichVuCauHinh(item.id(),0,fields);
    }
    default boolean canSaveDetail(){return false;}
    default KetQua<DichVuCauHinh> saveDetail(DichVuCauHinh value,PhanQuyenNguCanh actor){
        return KetQua.failure("UNSUPPORTED","Nguồn danh mục này chưa hỗ trợ lưu cấu hình.");
    }
    static DichVuDanhMucNguon production(){
        var uow=new JdbcUnitOfWork();var tx=new JdbcGiaoDichQuanLy(uow);var provider=new JdbcKetNoiNhaCungCap(tx);
        var repository=new DichVuCauHinhRepository(provider,tx,new JdbcKiemToanRepository(provider,uow,CoSoDuLieuKetNoi::getConnection));
        return new DichVuDanhMucNguon(){
            public List<DichVuHang> load(){var actor=KhamBenhPhienNguCanh.current();var result=repository.list(actor).stream().map(DichVuHang::from).toList();check(actor);return result;}
            public DichVuCauHinh loadDetail(DichVuHang item){var actor=KhamBenhPhienNguCanh.current();var value=repository.load(item.id(),actor);check(actor);return value;}
            public boolean canSaveDetail(){return true;}
            public List<vn.clinicmanagement.application.catalog.VatTuHaoPhi> loadMaterials(){var actor=KhamBenhPhienNguCanh.current();var result=repository.materials(actor);check(actor);return result;}
            public KetQua<DichVuCauHinh> saveDetail(DichVuCauHinh value,PhanQuyenNguCanh actor){check(actor);return repository.save(value,actor);}
            private void check(PhanQuyenNguCanh actor){if(actor==null||!actor.equals(KhamBenhPhienNguCanh.current()))throw new IllegalStateException("Phiên làm việc đã thay đổi.");}
        };
    }
}
