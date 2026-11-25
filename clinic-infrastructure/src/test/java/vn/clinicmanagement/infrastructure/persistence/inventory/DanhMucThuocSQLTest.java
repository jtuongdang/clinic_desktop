package vn.clinicmanagement.infrastructure.persistence.inventory;
import org.junit.jupiter.api.*;
import vn.clinicmanagement.infrastructure.persistence.*;
import vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.Thuoc;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class DanhMucThuocSQLTest {
 @Test void metadataMigrationScopeAndVersionAreVerifiedWithRollback()throws Exception{
  Assumptions.assumeTrue(Boolean.getBoolean("clinic.pharmacy.sql"));
  try(var c=CoSoDuLieuKetNoi.getConnection()){
   c.setAutoCommit(false);var uow=new JdbcUnitOfWork();var tx=new JdbcGiaoDichQuanLy(uow);
   var field=JdbcGiaoDichQuanLy.class.getDeclaredField("currentConnection");field.setAccessible(true);
   @SuppressWarnings("unchecked") ThreadLocal<Connection> current=(ThreadLocal<Connection>)field.get(tx);current.set(c);
   try{
    try(var ps=c.prepareStatement("SELECT COL_LENGTH('dbo.Item','maTuongDuong')");var rs=ps.executeQuery()){
     rs.next();if(rs.getObject(1)==null)try(var input=getClass().getClassLoader().getResourceAsStream("migrations/V15__Add_Drug_Directory_Metadata.sql");var stmt=c.createStatement()){stmt.execute(new String(input.readAllBytes(),StandardCharsets.UTF_8));}
    }
    int facility,warehouse,employee;try(var stmt=c.createStatement();var rs=stmt.executeQuery("SELECT TOP 1 id,maCoSo,(SELECT TOP 1 id FROM NhanVien WHERE isDeleted=0) employee FROM Kho WHERE isDeleted=0")){assertTrue(rs.next());warehouse=rs.getInt(1);facility=rs.getInt(2);employee=rs.getInt(3);}
    String code="DIR-"+UUID.randomUUID().toString().substring(0,8);int id;
    try(var ps=c.prepareStatement("INSERT INTO Item(maItem,tenItem,loaiItem,donViCoBan,nhomThuoc,thuocBHYT) OUTPUT INSERTED.id VALUES (?,N'Thuốc kiểm thử','THUOC',N'Viên',N'Thuốc Hô Hấp',NULL)")){ps.setString(1,code);try(var rs=ps.executeQuery()){assertTrue(rs.next());id=rs.getInt(1);}}
    var repo=new NhaThuocDanhSachRepository(new JdbcKetNoiNhaCungCap(tx));
    var drug=repo.getDrugDirectory(facility,warehouse).stream().filter(d->d.id()==id).findFirst().orElseThrow();
    assertNull(drug.insured());assertEquals("Thuốc Hô Hấp",drug.group());
    assertTrue(repo.getDrugDirectory(99999999,null).isEmpty());assertTrue(repo.getDrugDirectory(facility,99999999).isEmpty());
    var changed=new Thuoc(drug.id(),drug.version(),drug.code(),"EQUIV-TEST",drug.name(),drug.ingredient(),drug.strength(),"10 mg/ml","20 ml",drug.unit(),"Thuốc Kháng Sinh",true,"YHCT-TEST",drug.warehouse(),drug.batch(),drug.expiry(),drug.stock());
    repo.saveDrugMetadata(changed,facility,warehouse,employee);
    var saved=repo.getDrugDirectory(facility,warehouse).stream().filter(d->d.id()==id).findFirst().orElseThrow();
    assertEquals("EQUIV-TEST",saved.equivalentCode());assertTrue(saved.insured());assertEquals(drug.version()+1,saved.version());
    assertThrows(IllegalStateException.class,()->repo.saveDrugMetadata(changed,facility,warehouse,employee));
    assertThrows(IllegalStateException.class,()->repo.saveDrugMetadata(saved,99999999,null,employee));
   }finally{c.rollback();current.remove();}
  }
 }
}
