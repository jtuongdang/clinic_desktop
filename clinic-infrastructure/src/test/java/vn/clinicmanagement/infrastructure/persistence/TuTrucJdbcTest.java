package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.authorization.HoatDongPhienCongKetNoi;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.identity.*;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;
import vn.clinicmanagement.domain.repositories.*;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi.*;
import vn.clinicmanagement.infrastructure.persistence.inventory.TuTrucRepository;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises JDBC bindings and commit/rollback without a live hospital database. */
class TuTrucJdbcTest {
    static final Map<UUID,PhanQuyenNguCanh> CONTEXTS=new java.util.concurrent.ConcurrentHashMap<>();
    static PhanQuyenNguCanh auth(String role){int roleId=switch(role){case "ADMIN"->1;case "PHARMACIST"->2;case "DOCTOR"->3;default->4;};var context=new PhanQuyenNguCanh(8,roleId,role,10,20,UUID.randomUUID());CONTEXTS.put(context.sessionId(),context);return context;}
    static PhanQuyenDichVu authorization(){
        NhanVien employee=new NhanVien();employee.setId(8);
        List<VaiTro> roles=new ArrayList<>();for(var entry:Map.of(1,"ADMIN",2,"PHARMACIST",3,"DOCTOR",4,"RECEPTIONIST").entrySet()){VaiTro role=new VaiTro();role.setId(entry.getKey());role.setTenVaiTro(entry.getValue());roles.add(role);}
        INhanVienRepository employees=proxy(INhanVienRepository.class,(p,m,a)->switch(m.getName()){
            case "getById"->Optional.of(employee);case "getAssignedRoles"->roles;
            case "getAssignedWorkScopes"->List.of(new NhanVienWorkPhamVi(10,"Cơ sở thử",20,"Khoa thử"));
            case "getAll","getActiveDoctors","getRoleNames"->List.of();case "getByUsername"->Optional.empty();default->null;});
        IHeThongRepository system=proxy(IHeThongRepository.class,(p,m,a)->{
            if(!m.getName().equals("getMatrixByRoleId"))return Optional.empty();int roleId=(Integer)a[0];String role=switch(roleId){case 1->"ADMIN";case 2->"PHARMACIST";case 3->"DOCTOR";default->"RECEPTIONIST";};
            Set<HanhDongQuyen> actions=switch(role){case "ADMIN"->EnumSet.allOf(HanhDongQuyen.class);case "PHARMACIST"->EnumSet.of(HanhDongQuyen.XEM,HanhDongQuyen.TAO,HanhDongQuyen.SUA,HanhDongQuyen.XUAT_KHO);case "DOCTOR"->EnumSet.of(HanhDongQuyen.XEM,HanhDongQuyen.NHAN_KHO);default->EnumSet.of(HanhDongQuyen.XEM);};
            return actions.stream().map(action->{MaTranPhanQuyen grant=new MaTranPhanQuyen();grant.setId(roleId*100+action.getValue());grant.setMaVaiTro(roleId);grant.setPhanHe("INVENTORY");grant.setHanhDong(action);grant.setPhamViDuLieu(PhamViDuLieu.KHOA_PHONG);grant.setDuocPhep(true);return grant;}).toList();});
        HoatDongPhienCongKetNoi sessions=id->{var c=CONTEXTS.get(id);return c==null?Optional.empty():Optional.of(new HoatDongPhienCongKetNoi.ActiveSession(id,c.employeeId(),c.roleId(),c.roleCode(),c.facilityId(),c.departmentId(),true,true,true));};
        return new PhanQuyenDichVu(sessions,employees,system);
    }
    static YeuCau request(String role,int cabinet,int source,BigDecimal quantity){return new YeuCau(cabinet,source,"Bù vật tư",List.of(new DongYeuCau(23,quantity)),auth(role));}
    @Test void createsPendingTransferWithBatchLinesAndAuditWithoutMovingStock(){
        Db db=new Db();var response=db.repository.requestReplenishment(request("PHARMACIST",3,1,BigDecimal.valueOf(2)));
        assertTrue(response.isSuccess(),()->response.getError().map(e->e.code()+": "+e.message()).orElse("missing success value")+" SQL="+db.sql);assertTrue(response.valueOrThrow().startsWith("NB-"));assertEquals(1,db.commits);assertEquals(0,db.rollbacks);
        assertEquals(4,db.committedWrites);assertEquals(1,db.audits);assertEquals(1,db.header.get(2));
        assertEquals(3,db.header.get(3));assertEquals(101,db.line.get(1));assertEquals(23,db.line.get(2));assertEquals(new BigDecimal("2"),db.line.get(3));
        assertTrue(db.sql.stream().noneMatch(s->s.startsWith("UPDATE")||s.contains("INSERT INTO TonKho")||s.contains("INSERT INTO InventoryTransaction")));
    }
    @Test void rejectsSessionWithoutInventoryRoleBeforeAnyDatabaseCall(){
        Db db=new Db();assertTrue(db.repository.requestReplenishment(request("DOCTOR",3,1,BigDecimal.ONE)).isFailure());
        assertTrue(db.repository.requestReplenishment(null).isFailure());assertTrue(db.sql.isEmpty());assertEquals(0,db.commits);
    }
    @Test void rejectsDuplicateOrNonpositiveQuantitiesBeforeStartingTransaction(){
        Db db=new Db();var command=new YeuCau(3,1,"",List.of(new DongYeuCau(23,BigDecimal.ONE),new DongYeuCau(23,BigDecimal.ONE)),auth("ADMIN"));
        assertTrue(db.repository.requestReplenishment(command).isFailure());assertTrue(db.repository.requestReplenishment(request("ADMIN",3,1,BigDecimal.ZERO)).isFailure());assertTrue(db.sql.isEmpty());
    }
    @Test void sourceOutsideCurrentFacilityIsRejected(){Db db=new Db();assertTrue(db.repository.requestReplenishment(request("ADMIN",3,99,BigDecimal.ONE)).isFailure());assertEquals(1,db.rollbacks);assertEquals(0,db.committedWrites);}
    @Test void requestDoesNotCheckOrMoveSourceLotsBeforeIssue(){
        Db db=new Db();var response=db.repository.requestReplenishment(request("ADMIN",3,1,new BigDecimal("11")));assertTrue(response.isSuccess(),()->response.getError().map(e->e.code()+": "+e.message()).orElse("missing success value"));
        assertTrue(db.header.size()>0);assertTrue(db.sql.stream().noneMatch(s->s.contains("FROM dbo.TonKho")||s.contains("INSERT INTO dbo.InventoryTransaction")));
    }
    @Test void detailFailureRollsBackHeader(){Db db=new Db();db.failLine=true;assertTrue(db.repository.requestReplenishment(request("ADMIN",3,1,BigDecimal.ONE)).isFailure());assertEquals(0,db.committedWrites);assertEquals(1,db.rollbacks);assertEquals(0,db.audits);}
    @Test void auditFailureRollsBackHeaderAndDetails(){Db db=new Db();db.failAudit=true;assertTrue(db.repository.requestReplenishment(request("ADMIN",3,1,BigDecimal.ONE)).isFailure());assertEquals(0,db.committedWrites);assertEquals(1,db.rollbacks);}
    @Test void ticketQueryBindsCabinetFacilityAndExclusiveRangeToBothDirections(){
        Db db=new Db();var from=LocalDateTime.of(2026,11,1,0,0);var until=from.plusMonths(1);
        var result=db.tx.inTransaction(()->vn.clinicmanagement.application.common.KetQua.success(db.repository.getTickets(10,3,from,until)));
        assertTrue(result.isSuccess());assertEquals(1,result.valueOrThrow().size());assertEquals("Nhập bù",result.valueOrThrow().get(0).type());
        assertEquals("",result.valueOrThrow().get(0).executionCode());
        assertEquals(10,db.query.get(1));assertEquals(3,db.query.get(2));assertEquals(Timestamp.valueOf(from),db.query.get(3));assertEquals(Timestamp.valueOf(until),db.query.get(4));
        assertEquals(10,db.query.get(7));assertEquals(3,db.query.get(8));assertEquals(Timestamp.valueOf(from),db.query.get(9));assertEquals(Timestamp.valueOf(until),db.query.get(10));
    }
    static final class Db {
        int writes,committedWrites,commits,rollbacks,audits;boolean failLine,failAudit,expired;
        final List<String> sql=new ArrayList<>();Map<Integer,Object> header=new HashMap<>(),line=new HashMap<>(),query=new HashMap<>();
        final Connection connection=proxy(Connection.class,(p,m,a)->switch(m.getName()){
            case "setAutoCommit","close"->null;case "prepareStatement"->statement((String)a[0]);
            case "commit"->{committedWrites=writes;commits++;yield null;}case "rollback"->{writes=0;rollbacks++;yield null;}
            default->throw new UnsupportedOperationException(m.getName());
        });
        final JdbcGiaoDichQuanLy tx=new JdbcGiaoDichQuanLy(new JdbcUnitOfWork(),()->connection);
        final TuTrucRepository repository=new TuTrucRepository(new JdbcKetNoiNhaCungCap(tx),tx,new KiemToanWriteCongKetNoi(){
            public void appendInCurrentTransaction(HeThongKiemToanNhapLieu e){assertEquals(10,e.facilityId());assertEquals(20,e.departmentId());assertEquals(KiemToanTaiNguyenLoai.INVENTORY_ISSUE,e.resourceType());if(failAudit)throw new IllegalStateException("audit unavailable");audits++;}
            public void appendIndependently(HeThongKiemToanNhapLieu e){fail("Audit must share the stock request transaction");}
            public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu e){fail();}public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu e){fail();}
        },authorization());
        PreparedStatement statement(String text){sql.add(text);Map<Integer,Object> args=new HashMap<>();return proxy(PreparedStatement.class,(p,m,a)->{
            if(m.getName().startsWith("set")){args.put((Integer)a[0],a[1]);return null;}
            return switch(m.getName()){
                case "close"->null;
                case "executeQuery"->{
                    if(text.startsWith("SELECT k.id,k.tenKho")){assertEquals(10,args.get(1));assertTrue(text.contains("k.maCoSo=?"));assertTrue(text.contains("TU[_]TRUC%"));
                        yield rs(List.of(Map.of(1,text.contains("NOT (")?1:3,2,"Kho thử")));}
                    if(text.startsWith("SELECT 1 FROM dbo.Kho k JOIN dbo.TuTrucKhoKhoa"))yield rs(List.of(Map.of(1,1)));
                    if(text.startsWith("SELECT p.maPhieuXuat,p.payloadHash"))yield rs(List.of());
                    if(text.startsWith("SELECT i.id FROM Item i"))yield rs(List.of(Map.of(1,23)));
                    if(text.startsWith("INSERT INTO PhieuXuatKho")){assertTrue(text.contains("'CHUYEN_KHO','CHO_DUYET'"));header=new HashMap<>(args);writes++;yield rs(List.of(Map.of(1,101)));}
                    if(text.startsWith("INSERT INTO TuTrucChuyenKhoDong")){if(failLine)throw new SQLException("line failure");line=new HashMap<>(args);writes++;yield rs(List.of(Map.of(1,301)));}
                    if(text.startsWith("INSERT INTO TuTrucChuyenKhoThaoTac")){writes++;yield rs(List.of(Map.of(1,401)));}
                    if(text.startsWith("SELECT p.id,'XUAT_KHO'")){query=new HashMap<>(args);assertTrue(text.contains("k.id=?"));
                        yield rs(List.of(Map.of("id",587,"sourceType","XUAT_KHO","status","DA_NHAN_HANG","code","NB-587","ticketType","Nhập bù","location","Tủ trực thử")));}
                    throw new UnsupportedOperationException(text);
                }
                case "executeUpdate"->{if(text.startsWith("INSERT INTO TuTrucChuyenKho(")){writes++;yield 1;}throw new UnsupportedOperationException(text);}
                default->throw new UnsupportedOperationException(m.getName());
            };
        });}
        ResultSet rs(List<? extends Map<?,?>> rows){int[] cursor={-1};return proxy(ResultSet.class,(p,m,a)->{
            if(m.getName().equals("next"))return ++cursor[0]<rows.size();if(m.getName().equals("close"))return null;
            Object value=rows.get(cursor[0]).get(a[0]);return switch(m.getName()){
                case "getInt"->value==null?0:((Number)value).intValue();case "getLong"->value==null?0L:((Number)value).longValue();case "getString"->value==null?null:value.toString();case "getBigDecimal","getTimestamp"->value;
                default->throw new UnsupportedOperationException(m.getName());};
        });}
    }
    @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,InvocationHandler handler){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},handler);}
}
