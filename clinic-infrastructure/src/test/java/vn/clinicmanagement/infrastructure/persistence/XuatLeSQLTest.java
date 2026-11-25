package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.*;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.inventory.retail.XuatLe.*;
import vn.clinicmanagement.infrastructure.persistence.inventory.XuatLeRepository;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class XuatLeSQLTest {
    @Test void retailTicketAndStockCommitOrRollbackTogether() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("clinic.pharmacy.sql"));
        try(var db=CoSoDuLieuKetNoi.getConnection()){
            db.setAutoCommit(false);
            try {
                if(scalar(db,"SELECT COUNT(*) FROM sys.columns WHERE object_id=OBJECT_ID('dbo.PhieuXuatKho') AND name='tenKhachLe'")==0)
                    try(var in=getClass().getResourceAsStream("/migrations/V17__Retail_Stock_Issue.sql")){
                        CoSoDuLieuDiCuChay.executeSqlScript(db,new String(in.readAllBytes(),StandardCharsets.UTF_8));
                    }
                int wh=scalar(db,"SELECT TOP 1 id FROM Kho WHERE isDeleted=0 AND trangThai='HOAT_DONG'");
                int facility=scalar(db,"SELECT maCoSo FROM Kho WHERE id="+wh);
                String code="TEST-"+UUID.randomUUID();
                int item=scalar(db,"INSERT INTO Item(maItem,tenItem,donViCoBan) OUTPUT inserted.id VALUES('"+code+"',N'Test',N'Viên')");
                int first=lot(db,wh,item,"L1"), second=lot(db,wh,item,"L2");
                Savepoint[] start={null};
                var scoped=(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(o,m,a)->{
                    if(Set.of("close","commit").contains(m.getName()))return null;
                    if(m.getName().equals("rollback") && a==null){db.rollback(start[0]);return null;}
                    try{return m.invoke(db,a);}catch(InvocationTargetException ex){throw ex.getCause();}
                });
                var tx=new JdbcGiaoDichQuanLy(new JdbcUnitOfWork(),()->{start[0]=db.setSavepoint();return scoped;});
                var repo=new XuatLeRepository(new JdbcKetNoiNhaCungCap(tx),tx);
                var auth=new PhanQuyenNguCanh(1,1,"PHARMACIST",facility,1,UUID.randomUUID());
                var rows=tx.inTransaction(()->vn.clinicmanagement.application.common.KetQua.success(repo.lots(wh,auth))).valueOrThrow();
                Lot a=rows.stream().filter(l->l.id()==first).findFirst().orElseThrow();
                Lot b=rows.stream().filter(l->l.id()==second).findFirst().orElseThrow();
                var buyer=new Customer("","Khách kiểm thử","30","","","","","Ghi chú","Bác sĩ thử");
                var line=new Line(a,BigDecimal.valueOf(2),BigDecimal.TEN,BigDecimal.TEN,"Hướng dẫn","Chẩn đoán");
                var good=new Ticket(UUID.randomUUID(),wh,LocalDate.now(),buyer,List.of(line));
                var result=repo.save(good,auth);
                assertTrue(result.isSuccess(),()->result.getError().toString());
                assertEquals(3,scalar(db,"SELECT soLuongTon FROM TonKho WHERE id="+first));
                assertEquals(198,scalar(db,"SELECT tongTien FROM PhieuXuatKho WHERE soPhieuXuat='"+good.number()+"' AND tenKhachLe=N'Khách kiểm thử' AND trangThaiNoiBo='DA_XUAT'"));
                assertEquals(198,scalar(db,"SELECT thanhTienBanLe FROM ChiTietXuatKho WHERE maTonKho="+first+" AND huongDanBanLe=N'Hướng dẫn'"));
                assertEquals(-2,scalar(db,"SELECT soLuongThayDoi FROM InventoryTransaction WHERE maTonKho="+first));
                assertTrue(repo.save(good,auth).isFailure());
                var tooMany=new Line(b,BigDecimal.TEN,BigDecimal.ZERO,BigDecimal.ZERO,"","");
                var bad=new Ticket(UUID.randomUUID(),wh,LocalDate.now(),buyer,List.of(line,tooMany));
                assertEquals("STOCK_CHANGED",repo.save(bad,auth).getErrorCode().orElseThrow());
                assertEquals(0,scalar(db,"SELECT COUNT(*) FROM PhieuXuatKho WHERE soPhieuXuat='"+bad.number()+"'"));
                assertEquals(3,scalar(db,"SELECT soLuongTon FROM TonKho WHERE id="+first));
                assertEquals(1,scalar(db,"SELECT COUNT(*) FROM InventoryTransaction WHERE maTonKho="+first));
                var outside=new PhanQuyenNguCanh(1,1,"ADMIN",Integer.MAX_VALUE,1,UUID.randomUUID());
                assertEquals("INVALID_WAREHOUSE",repo.save(bad,outside).getErrorCode().orElseThrow());
            } finally {db.rollback();}
        }
    }
    private int lot(Connection c,int warehouse,int item,String batch)throws SQLException {
        return scalar(c,"INSERT INTO TonKho(maKho,maItem,soLo,hanSuDung,soLuongTon,giaBan) OUTPUT inserted.id VALUES("
                +warehouse+","+item+",'"+batch+"',DATEADD(day,5,GETDATE()),5,100)");
    }
    private int scalar(Connection c,String sql)throws SQLException {
        try(var s=c.createStatement();var rs=s.executeQuery(sql)){assertTrue(rs.next());return rs.getInt(1);}
    }
}
