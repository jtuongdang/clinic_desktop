package vn.clinicmanagement.application.inventory;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.inventory.create.*;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.*;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.repositories.IKhoRepository;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CreateThuocDayDuHandlerTest {
 @Test void fullFormMapsCoreMetadataFlagsAndAudit(){
  Fixture fixture=new Fixture();var values=values();var result=fixture.handler.handle(values,fixture.auth("PHARMACIST"));
  assertTrue(result.isSuccess());assertEquals(42,result.valueOrThrow().itemId());
  assertEquals(new BigDecimal("25000.00"),fixture.item.getGiaBanMacDinh());assertEquals("Hoạt chất thử",fixture.item.getHoatChat());assertFalse(fixture.item.isHoatDong());
  assertEquals(values,fixture.fields);assertEquals("",fixture.metadata.equivalentCode());assertEquals("Thuốc viên",fixture.metadata.group());assertTrue(fixture.metadata.insured());assertEquals(1,fixture.audits);
 }
 @Test void invalidInputAndDeniedRolesNeverSave(){
  Fixture fixture=new Fixture();
  assertEquals("AUTH_ACCESS_DENIED",fixture.handler.handle(values(),fixture.auth("DOCTOR")).getErrorCode().orElseThrow());
  for(var entry:Map.of("vatMua","101","giaBan","-1","canhBaoTon","1.5","hanSuDung","31/02/2030","donViTinh","").entrySet()){
   var invalid=values();invalid.put(entry.getKey(),entry.getValue());assertEquals("VALIDATION_ERROR",fixture.handler.handle(invalid,fixture.auth("ADMIN")).getErrorCode().orElseThrow());
  }
  assertNull(fixture.item);assertNull(fixture.fields);assertEquals(0,fixture.audits);
 }
 static Map<String,String> values(){Map<String,String> values=new LinkedHashMap<>();values.put("maThuoc","TEST-THU");values.put("tenThuoc","Thuốc thử");values.put("donViTinh","Viên");values.put("tenHoatChat","Hoạt chất thử");values.put("giaMua","10000");values.put("giaBan","25000.00");values.put("canhBaoTon","10");values.put("loaiDichVu","Thuốc viên");values.put("nhomBHYT","Trong danh mục BHYT");values.put("maThuocBHYT","BHYT-TEST");values.put("nongDo","10 mg/ml");values.put("theTich","20 ml");values.put("khoa","true");values.put("keDon","true");values.put("nhomDuocLy","Nhóm thử");values.put("huongDan","Thông tin thử nghiệm");return values;}
 static final class Fixture {
  MatHang item;Map<String,String> fields;NhaThuocDanhSachReadCongKetNoi.Thuoc metadata;int audits;
  final IKhoRepository inventory=(IKhoRepository)Proxy.newProxyInstance(IKhoRepository.class.getClassLoader(),new Class[]{IKhoRepository.class},(proxy,method,args)->switch(method.getName()){
   case "getItemByCode"->Optional.empty();case "saveItem"->{item=(MatHang)args[0];item.setId(42);yield item;}default->throw new UnsupportedOperationException(method.getName());});
  final NhaThuocDanhSachReadCongKetNoi read=new NhaThuocDanhSachReadCongKetNoi(){
   public List<Phieu> getTickets(int f,LocalDateTime a,LocalDateTime b){return List.of();}public Bang getList(DanhSach d,int f,Integer w,LocalDateTime a,LocalDateTime b){return new Bang(List.of(),List.of());}
   public void saveDrugForm(Thuoc drug,Map<String,String> data,int facility,int employee){metadata=drug;fields=Map.copyOf(data);assertEquals(1,facility);assertEquals(10,employee);}
  };
  final GiaoDichRanhGioi tx=new GiaoDichRanhGioi(){public <T>KetQua<T> inTransaction(java.util.function.Supplier<KetQua<T>> operation){return operation.get();}};
  final KiemToanWriteCongKetNoi audit=new KiemToanWriteCongKetNoi(){
   public void appendInCurrentTransaction(HeThongKiemToanNhapLieu e){audits++;assertEquals(KiemToanHanhDongMa.INV_ITEM_CREATED,e.actionCode());}
   public void appendIndependently(HeThongKiemToanNhapLieu e){fail("Audit must share transaction");}
   public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu e){}public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu e){}
  };
  final CreateThuocDayDuHandler handler=new CreateThuocDayDuHandler(inventory,read,tx,audit);
  PhanQuyenNguCanh auth(String role){return new PhanQuyenNguCanh(10,1,role,1,1,UUID.randomUUID());}
 }
}
