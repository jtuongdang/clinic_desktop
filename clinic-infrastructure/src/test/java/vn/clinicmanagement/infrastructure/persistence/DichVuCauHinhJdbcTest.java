package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.catalog.DichVuCauHinh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.infrastructure.persistence.service.DichVuCauHinhRepository;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real repository SQL binding/mapper and transaction boundary, backed by synthetic JDBC rows. */
class DichVuCauHinhJdbcTest {
    static PhanQuyenNguCanh actor(String role){return new PhanQuyenNguCanh(8,2,role,1,2,UUID.randomUUID());}
    static DichVuCauHinh update(){return new DichVuCauHinh(1,7,Map.of("code","XN1","name","Urê mới","category","Xét nghiệm","group","Hóa sinh",
            "insurancePrice","21,500","servicePrice","35,000","hospitalPrice","21,200","consumedMaterials","{\"Tên\":\"Ống & bơm <5ml>\"}","skipTimeOverlapCheck","true","unit","Lần"));}
    @Test void coreAndExtendedFieldsRoundTripInOneCommitWithUpdatedVersion(){
        Db db=new Db();var response=db.repository.save(update(),actor("ADMIN"));assertTrue(response.isSuccess());var saved=response.valueOrThrow();
        assertEquals(8,saved.version());assertEquals("Urê mới",saved.value("name"));assertEquals("Lần",saved.value("unit"));
        assertEquals("",saved.value("consumedMaterials"));assertEquals("true",saved.value("skipTimeOverlapCheck"));
        assertEquals("21200",saved.value("hospitalPrice"));assertEquals(new BigDecimal("21200"),db.row.get("giaVienPhi"));assertEquals(new BigDecimal("35000"),db.row.get("donGia"));assertEquals(1,db.commits);assertEquals(0,db.rollbacks);assertEquals(1,db.audits);
    }
    @Test void staleCatalogVersionLeavesCoreAndSettingsUntouched(){
        Db db=new Db();db.row.put("version",8L);var response=db.repository.save(update(),actor("ADMIN"));
        assertTrue(response.isFailure());assertEquals("CONFLICT",response.getError().orElseThrow().code());assertEquals("Urê cũ",db.row.get("tenDichVu"));assertNull(db.row.get("giaTriXml"));assertEquals(0,db.audits);assertEquals(1,db.rollbacks);
    }
    @Test void auditFailureRollsBackBothCatalogPriceAndExtendedSettings(){
        Db db=new Db();db.failAudit=true;assertTrue(db.repository.save(update(),actor("ADMIN")).isFailure());
        assertEquals(new BigDecimal("25000"),db.row.get("donGia"));assertEquals(7L,db.row.get("version"));assertNull(db.row.get("giaTriXml"));assertEquals(0,db.commits);assertEquals(1,db.rollbacks);
    }
    @Test void invalidActorOrPriceIsRejectedBeforeOpeningTransaction(){
        Db db=new Db();assertTrue(db.repository.save(update(),actor("DOCTOR")).isFailure());assertEquals(0,db.begins);
        var values=new HashMap<>(update().values());values.put("servicePrice","21.500");assertTrue(db.repository.save(new DichVuCauHinh(1,7,values),actor("ADMIN")).isFailure());assertEquals(0,db.begins);
        assertThrows(IllegalArgumentException.class,()->DichVuCauHinh.number("21,50"));assertThrows(IllegalArgumentException.class,()->DichVuCauHinh.number("abc"));
    }
    @Test void failedSettingsWriteCannotLeaveNewPriceCommitted(){Db db=new Db();db.failSettings=true;assertTrue(db.repository.save(update(),actor("ADMIN")).isFailure());assertEquals("Urê cũ",db.row.get("tenDichVu"));assertEquals(new BigDecimal("25000"),db.row.get("donGia"));assertEquals(0,db.commits);}
    @Test void repeatedSaveUsesNewCatalogVersionAndReplacesSettings(){
        Db db=new Db();var first=db.repository.save(update(),actor("ADMIN")).valueOrThrow();var values=new HashMap<>(first.values());values.put("unit","Mẫu");
        var second=db.repository.save(new DichVuCauHinh(1,first.version(),values),actor("ADMIN")).valueOrThrow();assertEquals(9,second.version());assertEquals("Mẫu",second.value("unit"));assertEquals(2,db.commits);
    }
    @Test void unavailableConsumptionWarehouseRejectsSaveBeforePriceChange(){
        Db db=new Db();var values=new HashMap<>(update().values());values.put("consumedMaterialsDefinition",materialDefinition());db.materialValid=false;
        var response=db.repository.save(new DichVuCauHinh(1,7,values),actor("ADMIN"));assertTrue(response.isFailure());assertEquals("MATERIAL_UNAVAILABLE",response.getError().orElseThrow().code());
        assertEquals("Urê cũ",db.row.get("tenDichVu"));assertEquals(0,db.commits);assertEquals(1,db.rollbacks);
    }
    @Test void consumptionDefinitionSurvivesCatalogSaveWithoutIssuingStock(){
        Db db=new Db();var values=new HashMap<>(update().values());values.put("consumedMaterialsDefinition",materialDefinition());
        var response=db.repository.save(new DichVuCauHinh(1,7,values),actor("ADMIN"));assertTrue(response.isSuccess());
        assertTrue(db.sql.stream().anyMatch(s->s.startsWith("INSERT INTO dbo.DichVuVatTuHaoPhi(")));
        assertTrue(db.sql.stream().anyMatch(s->s.startsWith("INSERT INTO dbo.DichVuVatTuHaoPhiDong(")));
        assertEquals(1,db.commits);assertTrue(db.sql.stream().noneMatch(s->s.contains("InventoryTransaction")||s.startsWith("UPDATE TonKho")));
    }
    static String materialDefinition(){return new vn.clinicmanagement.application.catalog.VatTuHaoPhiCauHinh(vn.clinicmanagement.application.catalog.VatTuHaoPhiCauHinh.Stage.RESULT,true,
            List.of(new vn.clinicmanagement.application.catalog.VatTuHaoPhiCauHinh.Line(8,3,"VT8","Ống & bơm","ml","Hộp","Kho thử",new BigDecimal("0.125")))).encode();}
    static final class Db {
        Map<String,Object> row=new HashMap<>(),before;int begins,commits,rollbacks,audits;boolean failAudit,failSettings;boolean materialValid=true;final List<String> sql=new ArrayList<>();
        final Connection connection=proxy(Connection.class,(p,m,a)->switch(m.getName()){
            case "setAutoCommit"->{before=new HashMap<>(row);begins++;yield null;}
            case "prepareStatement"->statement((String)a[0]);case "commit"->{commits++;yield null;}
            case "rollback"->{row=new HashMap<>(before);rollbacks++;yield null;}case "close"->null;default->throw new UnsupportedOperationException(m.getName());
        });
        final JdbcGiaoDichQuanLy tx=new JdbcGiaoDichQuanLy(new JdbcUnitOfWork(),()->connection);
        final DichVuCauHinhRepository repository=new DichVuCauHinhRepository(new JdbcKetNoiNhaCungCap(tx),tx,new KiemToanWriteCongKetNoi(){
            public void appendInCurrentTransaction(HeThongKiemToanNhapLieu event){assertEquals(KiemToanHanhDongMa.SERVICE_CATALOG_UPDATED,event.actionCode());if(failAudit)throw new IllegalStateException("audit failure");audits++;}
            public void appendIndependently(HeThongKiemToanNhapLieu e){fail();}public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu e){fail();}public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu e){fail();}
        });
        Db(){row.put("id",1);row.put("version",7L);row.put("maDichVu","XN1");row.put("tenDichVu","Urê cũ");row.put("loaiDichVu","XET_NGHIEM");row.put("nhomDichVu","Hóa sinh");row.put("giaBHYT",new BigDecimal("21500"));row.put("donGia",new BigDecimal("25000"));row.put("hoatDong",true);}
        PreparedStatement statement(String sql){this.sql.add(sql);Map<Integer,Object> args=new HashMap<>();return proxy(PreparedStatement.class,(p,m,a)->{
            if(m.getName().startsWith("set")){args.put((Integer)a[0],a[1]);return null;}
            return switch(m.getName()){
                case "addBatch","clearBatch"->null;
                case "executeBatch"->new int[]{1};
                case "close"->null;
                case "executeUpdate"->{
                    if(sql.startsWith("UPDATE DanhMucDichVu")){
                        assertTrue(sql.contains("id=? AND version=? AND isDeleted=0"));if(!Objects.equals(row.get("id"),args.get(14))||!Objects.equals(row.get("version"),args.get(15)))yield 0;
                        String[] keys={"maDichVu","tenDichVu","loaiDichVu","nhomDichVu","giaBHYT","donGia","giaVienPhi","giaNuocNgoai","hoatDong","moTa","updatedBy"};
                        for(int i=0;i<keys.length;i++)row.put(keys[i],args.get(i+1));
                        if(args.get(12) instanceof String specimen)row.put("loaiBenhPham",specimen.isEmpty()?null:specimen);
                        row.put("version",((Number)row.get("version")).longValue()+1);yield 1;
                    }
                    if(failSettings)throw new SQLException("settings write failed");
                    if(sql.startsWith("UPDATE DichVuCauHinh")){if(row.get("giaTriXml")==null)yield 0;row.put("giaTriXml",args.get(1));yield 1;}
                    if(sql.startsWith("DELETE FROM dbo.DichVuVatTuHaoPhiDong"))yield 1;
                    assertTrue(sql.startsWith("INSERT INTO DichVuCauHinh"));row.put("giaTriXml",args.get(2));yield 1;
                }
                case "executeQuery"->{
                    if(sql.startsWith("INSERT INTO dbo.DichVuVatTuHaoPhi(")){int[] cursor={0};yield proxy(ResultSet.class,(r,method,params)->switch(method.getName()){
                        case "next"->cursor[0]++==0;case "getInt"->51;case "close"->null;default->throw new UnsupportedOperationException(method.getName());});}
                    if(sql.startsWith("SELECT id,maKho FROM dbo.DichVuVatTuHaoPhi")
                            ||sql.startsWith("SELECT h.stage,h.rounding,h.version")){
                        int[] cursor={0};yield proxy(ResultSet.class,(r,method,params)->switch(method.getName()){
                            case "next"->cursor[0]++<0;
                            case "close"->null;
                            default->throw new UnsupportedOperationException(method.getName());
                        });
                    }
                    if(sql.startsWith("SELECT TOP 1 i.id")){
                        assertEquals(8,args.get(1));assertEquals(3,args.get(2));assertEquals(1,args.get(3));assertTrue(sql.contains("k.maCoSo=?"));assertTrue(sql.contains("'VAT_TU','HOA_CHAT'"));
                        int[] cursor={0};yield proxy(ResultSet.class,(r,method,params)->switch(method.getName()){case "next"->materialValid&&cursor[0]++==0;case "close"->null;default->throw new UnsupportedOperationException(method.getName());});
                    }
                    assertTrue(sql.startsWith("SELECT d.*"));int[] cursor={0};yield proxy(ResultSet.class,(r,method,params)->{
                    if(method.getName().equals("next"))return cursor[0]++==0;if(method.getName().equals("close"))return null;
                    Object value=row.get(params[0]);return switch(method.getName()){
                        case "getInt"->value==null?0:((Number)value).intValue();case "getLong"->value==null?0L:((Number)value).longValue();case "getString"->value==null?null:value.toString();case "getBoolean"->Boolean.TRUE.equals(value);case "getBigDecimal"->value;default->throw new UnsupportedOperationException(method.getName());};
                });}
                default->throw new UnsupportedOperationException(m.getName());
            };
        });}
    }
    @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,InvocationHandler handler){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},handler);}
}
