package vn.clinicmanagement.application.inventory;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CapNhatDanhMucThuocTest {
 @Test void authorizationValidationAndConflictsDoNotSilentlySucceed(){
  int[] calls={0};
  var port=new NhaThuocDanhSachReadCongKetNoi(){
   public List<Phieu> getTickets(int f,LocalDateTime a,LocalDateTime b){return List.of();}
   public Bang getList(DanhSach type,int f,Integer w,LocalDateTime a,LocalDateTime b){return new Bang(List.of(),List.of());}
   public void saveDrugMetadata(Thuoc d,int f,Integer w,int employee){calls[0]++;assertEquals(1,f);assertEquals(10,employee);throw new IllegalStateException("Thuốc đã thay đổi.");}
  };
  var service=new CapNhatDanhMucThuocDichVu(port);
  var doctor=new PhanQuyenNguCanh(10,1,"DOCTOR",1,1,UUID.randomUUID());
  var pharmacist=new PhanQuyenNguCanh(10,1,"PHARMACIST",1,1,UUID.randomUUID());
  assertEquals("AUTH_ACCESS_DENIED",service.save(drug("ABC"),null,doctor).getErrorCode().orElseThrow());assertEquals(0,calls[0]);
  assertEquals("INVALID_FIELD",service.save(drug("X".repeat(101)),null,pharmacist).getErrorCode().orElseThrow());assertEquals(0,calls[0]);
  assertEquals("SAVE_FAILED",service.save(drug("ABC"),null,pharmacist).getErrorCode().orElseThrow());assertEquals(1,calls[0]);
 }
 private NhaThuocDanhSachReadCongKetNoi.Thuoc drug(String equivalent){return new NhaThuocDanhSachReadCongKetNoi.Thuoc(1,1,"T1",equivalent,"Test","","","","","Viên","",null,"","","",null,BigDecimal.ZERO);}
}
