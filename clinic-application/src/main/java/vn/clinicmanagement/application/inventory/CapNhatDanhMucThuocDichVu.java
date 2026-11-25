package vn.clinicmanagement.application.inventory;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import java.util.*;
/** Kiểm tra quyền, phạm vi và giới hạn dữ liệu trước khi lưu thông tin danh mục bổ sung. */
public final class CapNhatDanhMucThuocDichVu {
 private final NhaThuocDanhSachReadCongKetNoi repository;
 public CapNhatDanhMucThuocDichVu(NhaThuocDanhSachReadCongKetNoi repository){this.repository=Objects.requireNonNull(repository);}
 public KetQua<Void> save(NhaThuocDanhSachReadCongKetNoi.Thuoc drug,Integer warehouseId,PhanQuyenNguCanh auth){
  if(auth==null||!auth.isValid()||!Set.of("ADMIN","PHARMACIST").contains(auth.roleCode()))return KetQua.failure("AUTH_ACCESS_DENIED","Không có quyền cập nhật danh mục thuốc.");
  if(drug==null||drug.id()<=0||drug.version()<0)return KetQua.failure("INVALID_DRUG","Thuốc không hợp lệ.");
  for(String field:new String[]{drug.equivalentCode(),drug.group(),drug.concentration(),drug.volume(),drug.traditionalCode()})if(field!=null&&field.length()>100)return KetQua.failure("INVALID_FIELD","Thông tin bổ sung không được vượt quá 100 ký tự.");
  try{repository.saveDrugMetadata(drug,auth.facilityId(),warehouseId,auth.employeeId());return KetQua.success(null);}
  catch(IllegalStateException ex){return KetQua.failure("SAVE_FAILED",ex.getMessage());}
 }
}
