package vn.clinicmanagement.infrastructure.persistence.reports;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.reports.PhieuBaoCaoReadCongKetNoi.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class PhieuBaoCaoRepositoryTest {
    @Test void everyQueryIsReadOnlyScopedAndUsesTheRealTicketDate(){
        for(Loai type:Loai.values()){String sql=PhieuBaoCaoRepository.sql(type,true);assertTrue(sql.startsWith("SELECT "));assertTrue(sql.contains("t.isDeleted=0"));assertTrue(sql.contains("maCoSo=?"));assertTrue(sql.contains("maKhoa=?"));assertTrue(sql.contains(">=?"));assertTrue(sql.contains("<?"));assertFalse(sql.contains("DELETE "));assertFalse(sql.contains("INSERT "));assertFalse(sql.contains("UPDATE "));}
        assertTrue(PhieuBaoCaoRepository.sql(Loai.LICH_HEN,false).contains("thoiGianBatDau"));assertFalse(PhieuBaoCaoRepository.sql(Loai.LICH_HEN,false).contains("ngayHen"));assertFalse(PhieuBaoCaoRepository.sql(Loai.DON_THUOC,false).contains("ChiTietDonThuoc"));
    }
    @Test void bindsRangeFacilityDepartmentAndPreservesDistinctDocumentIdentities(){
        AtomicInteger queries=new AtomicInteger(),closed=new AtomicInteger();List<Map<Integer,Object>> parameters=new ArrayList<>();
        Connection connection=proxy(Connection.class,(p,m,args)->{
            if(m.getName().equals("close")){closed.incrementAndGet();return null;}
            if(m.getName().equals("prepareStatement")){int query=queries.incrementAndGet();Map<Integer,Object> bound=new HashMap<>();parameters.add(bound);AtomicInteger cursor=new AtomicInteger();
                ResultSet rows=proxy(ResultSet.class,(rp,rm,ra)->switch(rm.getName()){
                    case "next"->cursor.incrementAndGet()==1;case "close"->null;case "getInt"->1;
                    case "getTimestamp"->Timestamp.valueOf(LocalDateTime.of(2026,11,20,9,query));
                    case "getBigDecimal"->new BigDecimal("120000");
                    case "getString"->switch(ra[0].toString()){case "status"->"HOAN_THANH";case "extra2"->"true";case "patientName"->"Bệnh nhân kiểm thử";default->"TEST";};
                    default->throw new UnsupportedOperationException(rm.getName());});
                return proxy(PreparedStatement.class,(sp,sm,sa)->{if(sm.getName().startsWith("set")&&sa.length==2){bound.put((Integer)sa[0],sa[1]);return null;}return switch(sm.getName()){case "executeQuery"->rows;case "close","setQueryTimeout"->null;default->throw new UnsupportedOperationException(sm.getName());};});
            }throw new UnsupportedOperationException(m.getName());});
        var from=LocalDateTime.of(2026,11,20,0,0);var to=from.plusDays(1);var result=new PhieuBaoCaoRepository(()->connection).load(new TruyVan(from,to,7,11));
        int expectedCount=Loai.values().length;
        assertEquals(expectedCount,queries.get());assertEquals(1,closed.get());assertEquals(expectedCount,result.size());assertEquals(expectedCount,result.stream().map(Phieu::key).distinct().count());
        for(Map<Integer,Object> bound:parameters){assertEquals(Timestamp.valueOf(from),bound.get(1));assertEquals(Timestamp.valueOf(to),bound.get(2));assertEquals(7,bound.get(3));assertEquals(11,bound.get(4));}
        assertEquals("Đã khóa",result.stream().filter(r->r.type()==Loai.KHAM_BENH).findFirst().orElseThrow().details().get("Đã khóa bệnh án"));
        var selected=new PhieuBaoCaoRepository(()->connection).load(new TruyVan(from,to,7,11),Loai.DON_THUOC);
        assertEquals(expectedCount+1,queries.get());assertEquals(2,closed.get());assertEquals(1,selected.size());assertEquals(Loai.DON_THUOC,selected.get(0).type());
    }
    @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,java.lang.reflect.InvocationHandler handler){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},handler);}
}
