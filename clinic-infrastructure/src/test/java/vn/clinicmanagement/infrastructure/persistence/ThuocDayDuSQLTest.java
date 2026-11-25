package vn.clinicmanagement.infrastructure.persistence;
import org.junit.jupiter.api.*;
import vn.clinicmanagement.application.inventory.*;
import vn.clinicmanagement.application.inventory.create.*;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.infrastructure.persistence.inventory.*;
import vn.clinicmanagement.infrastructure.persistence.system.JdbcKiemToanRepository;
import java.lang.reflect.*;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
/** Dữ liệu, audit và thay đổi schema thử nghiệm đều rollback; commit của transaction được giữ trong giao dịch ngoài. */
class ThuocDayDuSQLTest {
 @Test void fullDrugAndExtensionPersistTogetherAndFailuresRollback()throws Exception{
  Assumptions.assumeTrue(Boolean.getBoolean("clinic.pharmacy.sql"));
  try(Connection real=CoSoDuLieuKetNoi.getConnection()){
   real.setAutoCommit(false);
   try{
    try(var ps=real.prepareStatement("SELECT COL_LENGTH('dbo.Item','thongTinThuoc')");var rs=ps.executeQuery()){
     rs.next();if(rs.getObject(1)==null)try(var input=getClass().getClassLoader().getResourceAsStream("migrations/V16__Add_Drug_Full_Form.sql")){CoSoDuLieuDiCuChay.executeSqlScript(real,new String(input.readAllBytes(),StandardCharsets.UTF_8));}
    }
    int facility,department,employee,role;
    try(var s=real.createStatement();var rs=s.executeQuery("SELECT TOP 1 k.maCoSo,(SELECT TOP 1 id FROM Khoa WHERE maCoSo=k.maCoSo AND isDeleted=0) department,(SELECT TOP 1 id FROM NhanVien WHERE isDeleted=0) employee,(SELECT TOP 1 id FROM VaiTro WHERE isDeleted=0) role FROM Kho k WHERE k.isDeleted=0")){
     assertTrue(rs.next());facility=rs.getInt(1);department=rs.getInt(2);employee=rs.getInt(3);role=rs.getInt(4);
    }
    Savepoint[] point={null};
    Connection proxy=(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(object,method,args)->{
     if(Set.of("commit","close").contains(method.getName()))return null;
     if(method.getName().equals("rollback")&&(args==null||args.length==0)){real.rollback(point[0]);return null;}
     try{return method.invoke(real,args);}catch(InvocationTargetException ex){throw ex.getCause();}
    });
    var uow=new JdbcUnitOfWork();var tx=new JdbcGiaoDichQuanLy(uow,()->{point[0]=real.setSavepoint();return proxy;});var provider=new JdbcKetNoiNhaCungCap(tx);
    var inventory=new KhoRepository(provider,uow);var details=new NhaThuocDanhSachRepository(provider);
    var audit=new JdbcKiemToanRepository(provider,uow,()->{throw new SQLException("Independent audit must not be used.");});
    var auth=new PhanQuyenNguCanh(employee,role,"PHARMACIST",facility,department,UUID.randomUUID());
    String code="FULL-"+UUID.randomUUID().toString().substring(0,8);
    Map<String,String> values=new LinkedHashMap<>(Map.of("maThuoc",code,"tenThuoc","Thuốc kiểm thử đầy đủ","donViTinh","Viên","giaMua","10000","giaBan","25000","maThuocBHYT","BHYT-FULL","nhomBHYT","Trong danh mục BHYT","loaiDichVu","Thuốc viên","keDon","true","huongDan","Thông tin kiểm thử"));
    values.put("soLoNhap","REF-TEST");values.put("hanSuDung","01/01/2030");values.put("nhomDuocLy","Nhóm thử");
    var handler=new CreateThuocDayDuHandler(inventory,details,tx,audit);var result=handler.handle(values,auth);assertTrue(result.isSuccess(),result.getError().map(Object::toString).orElse(""));int id=result.valueOrThrow().itemId();
    var stored=tx.inTransaction(()->KetQua.success(details.getDrugForm(id,facility))).valueOrThrow();assertEquals(values,stored);
    var item=tx.inTransaction(()->KetQua.success(inventory.getItemByCode(code))).valueOrThrow().orElseThrow();assertEquals(0,new java.math.BigDecimal("25000").compareTo(item.getGiaBanMacDinh()));
    var fullVals=tx.inTransaction(()->KetQua.success(details.getDrugFullValues(id,facility))).valueOrThrow();assertEquals(code,fullVals.get("maThuoc"));
    Map<String,String> edited=new LinkedHashMap<>(fullVals);edited.put("tenThuoc","Thuốc kiểm thử đã sửa");edited.put("giaBan","30000");
    assertTrue(handler.handleUpdate(id,edited,auth).isSuccess());
    assertEquals("Thuốc kiểm thử đã sửa",tx.inTransaction(()->KetQua.success(details.getDrugFullValues(id,facility))).valueOrThrow().get("tenThuoc"));
    tx.inTransaction(()->{details.setDrugActive(id,false,facility,employee);return KetQua.success(null);});
    assertEquals("true",tx.inTransaction(()->KetQua.success(details.getDrugFullValues(id,facility))).valueOrThrow().get("khoa"));
    assertNotNull(tx.inTransaction(()->KetQua.success(details.getStockCard(id,facility,null))).valueOrThrow());
    assertTrue(tx.inTransaction(()->KetQua.success(details.getDrugForm(id,99999999))).valueOrThrow().isEmpty());
    assertEquals("DUPLICATE_CODE",handler.handle(values,auth).getErrorCode().orElseThrow());
    NhaThuocDanhSachReadCongKetNoi broken=new NhaThuocDanhSachReadCongKetNoi(){
     public List<Phieu> getTickets(int f,LocalDateTime a,LocalDateTime b){return List.of();}public Bang getList(DanhSach t,int f,Integer w,LocalDateTime a,LocalDateTime b){return new Bang(List.of(),List.of());}
     public void saveDrugForm(Thuoc drug,Map<String,String> fields,int f,int e){throw new IllegalStateException("Failure after core item insert");}
    };
    String failedCode=code+"-FAIL";values.put("maThuoc",failedCode);
    assertTrue(new CreateThuocDayDuHandler(inventory,broken,tx,audit).handle(values,auth).isFailure());
    assertTrue(tx.inTransaction(()->KetQua.success(inventory.getItemByCode(failedCode))).valueOrThrow().isEmpty());
    try(var ps=real.prepareStatement("SELECT COUNT(*) FROM TonKho WHERE maItem=?")){ps.setInt(1,id);try(var rs=ps.executeQuery()){rs.next();assertEquals(0,rs.getInt(1));}}
   }finally{real.rollback();}
  }
 }
}
