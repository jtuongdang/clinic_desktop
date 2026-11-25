package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.examination.transfer.*;
import vn.clinicmanagement.domain.examination.TransferLyDoMa;
import vn.clinicmanagement.domain.facility.*;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;
import vn.clinicmanagement.infrastructure.persistence.examination.LuotKhamRepository;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Executes the actual JDBC binder, mapper and transaction boundary without touching a live patient. */
class TransferKhoaPhongJdbcTest {
    static PhanQuyenNguCanh auth(){return new PhanQuyenNguCanh(2,2,"DOCTOR",10,20,UUID.randomUUID());}
    static PhongKham destination(){var facility=new CoSo();facility.setId(10);var department=new Khoa();department.setId(30);department.setMaCoSo(10);department.setCoSo(facility);var room=new PhongKham();room.setId(102);room.setMaKhoa(30);room.setKhoa(department);return room;}
    @Test void departmentRoomAndReasonRoundTripTogetherInOneVersionedUpdate(){
        FakeDb db=new FakeDb();run(db,false,true);
        assertEquals(30,db.row.get("maKhoa"));assertEquals(102,db.row.get("maPhongKham"));assertEquals(8L,db.row.get("version"));assertTrue(db.row.get("lyDoChuyenTuyen").toString().startsWith("Ghi chú cũ; CLINICAL_SPECIALTY_REFERRAL"));assertEquals(1,db.updates);assertEquals(1,db.commits);assertEquals(0,db.rollbacks);
    }
    @Test void auditFailureRollsBackBothIdsAndPreservesOriginalReason(){FakeDb db=new FakeDb();run(db,true,false);assertOriginal(db);assertEquals(1,db.updates);assertEquals(0,db.commits);assertEquals(1,db.rollbacks);}
    @Test void versionConflictDoesNotCommitEitherDestinationId(){FakeDb db=new FakeDb();db.conflict=true;run(db,false,false);assertOriginal(db);assertEquals(0,db.commits);assertEquals(1,db.rollbacks);}
    static void assertOriginal(FakeDb db){assertEquals(20,db.row.get("maKhoa"));assertEquals(101,db.row.get("maPhongKham"));assertEquals(7L,db.row.get("version"));assertEquals("Ghi chú cũ",db.row.get("lyDoChuyenTuyen"));}
    static void run(FakeDb db,boolean failAudit,boolean success){
        var uow=new JdbcUnitOfWork();var tx=new JdbcGiaoDichQuanLy(uow,()->db.connection);var repository=new LuotKhamRepository(new JdbcKetNoiNhaCungCap(tx),uow);
        IPhongKhamRepository rooms=proxy(IPhongKhamRepository.class,(p,m,a)->m.getName().equals("getById")?Optional.of(destination()):null);
        KiemToanWriteCongKetNoi audit=new KiemToanWriteCongKetNoi(){public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry){assertEquals(30,entry.departmentId());if(failAudit)throw new IllegalStateException("Audit failure");}public void appendIndependently(HeThongKiemToanNhapLieu entry){}public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry){}public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry){}};
        var handler=new TransferLuotKhamHandler(repository,rooms,tx,audit);
        var result=tx.inTransaction(()->{
            var response=handler.handle(new TransferLuotKhamCommand(501,102,TransferLyDoMa.CLINICAL_SPECIALTY_REFERRAL,"Chuyển khoa",auth()));
            if(response.isSuccess()){var reloaded=repository.getById(501).orElseThrow();assertEquals(30,reloaded.getMaKhoa());assertEquals(102,reloaded.getMaPhongKham());assertTrue(reloaded.getLyDoChuyenTuyen().contains("Chuyển khoa"));}
            return response;
        });assertEquals(success,result.isSuccess());
    }
    static final class FakeDb {
        final Map<String,Object> row=new HashMap<>();Map<String,Object> before;
        int updates,commits,rollbacks;boolean conflict;
        final Connection connection=proxy(Connection.class,(p,m,a)->switch(m.getName()){
            case "setAutoCommit"->{before=new HashMap<>(row);yield null;}
            case "prepareStatement"->statement((String)a[0]);
            case "commit"->{commits++;yield null;}
            case "rollback"->{rollbacks++;row.clear();row.putAll(before);yield null;}
            case "close"->null;
            default->throw new UnsupportedOperationException(m.getName());
        });
        FakeDb(){row.put("id",501);row.put("version",7L);row.put("maBenhNhan",11);row.put("maCoSo",10);row.put("maKhoa",20);row.put("maPhongKham",101);row.put("maBacSi",2);row.put("trangThai","CHO_KHAM");row.put("lyDoChuyenTuyen","Ghi chú cũ");}
        PreparedStatement statement(String sql){Map<Integer,Object> args=new HashMap<>();return proxy(PreparedStatement.class,(p,m,a)->{if(m.getName().startsWith("set")){args.put((Integer)a[0],a[1]);return null;}return switch(m.getName()){
            case "close"->null;
            case "executeQuery"->resultSet(sql.startsWith("SELECT * FROM LuotKham"));
            case "executeUpdate"->{assertTrue(sql.startsWith("UPDATE LuotKham SET"));assertTrue(sql.contains("maKhoa=?"));assertTrue(sql.contains("maPhongKham=?"));assertTrue(sql.contains("WHERE id=? AND version=? AND isDeleted=0"));updates++;if(conflict||!Objects.equals(args.get(args.size()-1),row.get("id"))||!Objects.equals(args.get(args.size()),row.get("version")))yield 0;String set=sql.substring(sql.indexOf(" SET ")+5,sql.indexOf(" WHERE "));int bind=1;for(String assignment:set.split(",")){String[] field=assignment.split("=",2);if(field[1].equals("?"))row.put(field[0],args.get(bind++));}row.put("version",((Number)row.get("version")).longValue()+1);yield 1;}
            default->throw new UnsupportedOperationException(m.getName());};});}
        ResultSet resultSet(boolean hasRow){int[] cursor={0};return proxy(ResultSet.class,(p,m,a)->{if(m.getName().equals("next"))return hasRow&&cursor[0]++==0;if(m.getName().equals("close"))return null;Object value=a==null?null:row.get(a[0].toString());return switch(m.getName()){
            case "getInt"->value==null?0:((Number)value).intValue();case "getLong"->value==null?0L:((Number)value).longValue();case "getBoolean"->Boolean.TRUE.equals(value);case "getString"->value==null?null:value.toString();case "getObject","getTimestamp","getDate"->value;default->throw new UnsupportedOperationException(m.getName());};});}
    }
    @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,InvocationHandler handler){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},handler);}
}
