package vn.clinicmanagement.infrastructure.persistence.inventory;
import org.junit.jupiter.api.*;
import vn.clinicmanagement.infrastructure.persistence.*;
import vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.DanhSach;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class NhaThuocDanhSachSQLTest {
 @Test void listsUseRealSchemaAndRespectFacility()throws Exception{
  Assumptions.assumeTrue(Boolean.getBoolean("clinic.pharmacy.sql"));
  var uow=new JdbcUnitOfWork();var repo=new NhaThuocDanhSachRepository(new JdbcKetNoiNhaCungCap(new JdbcGiaoDichQuanLy(uow)));
  int facility;try(var c=CoSoDuLieuKetNoi.getConnection();var ps=c.prepareStatement("SELECT TOP 1 maCoSo FROM Kho WHERE isDeleted=0");var rs=ps.executeQuery()){assertTrue(rs.next());facility=rs.getInt(1);}
  var from=LocalDate.of(2000,1,1).atStartOfDay();var to=LocalDate.of(2100,1,1).atStartOfDay();
  assertNotNull(repo.getTickets(facility,from,to));assertTrue(repo.getTickets(99999999,from,to).isEmpty());
  for(var mode:vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.NgayLoc.values()){
   assertNotNull(repo.getTickets(facility,from,to,mode));
   var rxList=repo.getPrescriptions(facility,from,to,mode);
   assertNotNull(rxList);
   if(!rxList.isEmpty()){
    assertTrue(repo.getTicketFullDetail("DON_THUOC",rxList.get(0).id(),facility).isPresent());
   }
  }
  for(var type:DanhSach.values()){assertNotNull(repo.getList(type,facility,null,from,to));assertTrue(repo.getList(type,99999999,null,from,to).rows().isEmpty());}
 }
}
