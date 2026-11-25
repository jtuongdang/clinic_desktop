package vn.clinicmanagement.infrastructure.persistence.service;

import vn.clinicmanagement.application.catalog.DichVuCauHinh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.infrastructure.persistence.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Core catalog, optional fields and audit are updated atomically. No existing order is repriced. */
public final class DichVuCauHinhRepository {
    private static final String SELECT="SELECT d.*,c.giaTriXml FROM DanhMucDichVu d LEFT JOIN DichVuCauHinh c ON c.maDichVu=d.id WHERE d.isDeleted=0";
    private final JdbcKetNoiNhaCungCap connections;
    private final JdbcGiaoDichQuanLy tx;
    private final KiemToanWriteCongKetNoi audit;
    public DichVuCauHinhRepository(JdbcKetNoiNhaCungCap connections,JdbcGiaoDichQuanLy tx,KiemToanWriteCongKetNoi audit){this.connections=connections;this.tx=tx;this.audit=audit;}
    private static void authorize(PhanQuyenNguCanh context){
        if(context==null||!context.isValid()||!"ADMIN".equals(context.roleCode()))throw new IllegalStateException("Không có quyền cập nhật danh mục dịch vụ.");
    }
    public List<DichVuCauHinh> list(PhanQuyenNguCanh context){
        authorize(context);
        try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(SELECT+" ORDER BY d.id");var rs=ps.executeQuery()){
            List<DichVuCauHinh> result=new ArrayList<>();while(rs.next())result.add(withRelationalMaterials(lease.connection(),map(rs),context));return List.copyOf(result);
        }catch(SQLException ex){throw new IllegalStateException("Không tải được danh mục cấu hình dịch vụ.",ex);}
    }
    public DichVuCauHinh load(int id,PhanQuyenNguCanh context){
        authorize(context);
        try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(SELECT+" AND d.id=?")){
            ps.setInt(1,id);try(var rs=ps.executeQuery()){if(rs.next())return withRelationalMaterials(lease.connection(),map(rs),context);throw new IllegalStateException("Dịch vụ không còn tồn tại.");}
        }catch(SQLException ex){throw new IllegalStateException("Không tải được cấu hình dịch vụ.",ex);}
    }
    public List<vn.clinicmanagement.application.catalog.VatTuHaoPhi> materials(PhanQuyenNguCanh context){
        authorize(context);
        String sql="SELECT DISTINCT i.id,i.maItem,i.tenItem,i.donViCoBan,ISNULL(i.quyCachDongGoi,N'') packing,"
                +"COALESCE(NULLIF(i.nhomThuoc,N''),N'Vật tư') itemGroup,k.id warehouseId,k.tenKho,i.giaBanMacDinh "
                +"FROM Item i CROSS JOIN Kho k "
                +"WHERE k.maCoSo=? AND k.isDeleted=0 AND k.trangThai='HOAT_DONG' AND i.isDeleted=0 AND i.hoatDong=1 "
                +"AND k.loaiKho LIKE 'TU[_]TRUC%' AND EXISTS(SELECT 1 FROM dbo.TuTrucKhoKhoa m WHERE m.maKho=k.id AND m.maCoSo=k.maCoSo AND m.maKhoa=?) "
                +"AND i.loaiItem IN ('VAT_TU','HOA_CHAT') ORDER BY k.tenKho,i.tenItem";
        try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){
            ps.setInt(1,context.facilityId());ps.setInt(2,context.departmentId());List<vn.clinicmanagement.application.catalog.VatTuHaoPhi> result=new ArrayList<>();
            try(var rs=ps.executeQuery()){while(rs.next())result.add(new vn.clinicmanagement.application.catalog.VatTuHaoPhi(rs.getInt("id"),rs.getString("maItem"),rs.getString("tenItem"),
                    rs.getString("donViCoBan"),rs.getString("packing"),rs.getString("itemGroup"),rs.getInt("warehouseId"),rs.getString("tenKho"),null,null,rs.getBigDecimal("giaBanMacDinh")));}
            return List.copyOf(result);
        }catch(SQLException ex){throw new IllegalStateException("Không tải được vật tư trong các kho của cơ sở.",ex);}
    }
    public KetQua<DichVuCauHinh> save(DichVuCauHinh value,PhanQuyenNguCanh context){
        if(value==null)return KetQua.failure("INVALID","Thiếu thông tin dịch vụ.");
        try{authorize(context);value.validate();}catch(IllegalArgumentException|IllegalStateException ex){return KetQua.failure("INVALID",ex.getMessage());}
        Map<String,String> nonInventoryValues=new LinkedHashMap<>(value.values());
        String materialDefinition=nonInventoryValues.remove("consumedMaterialsDefinition");
        nonInventoryValues.remove("consumedMaterials");
        String specimenType=nonInventoryValues.remove("labSpecimenType");
        String xml=encode(nonInventoryValues);
        return tx.inTransaction(()->{
            try(var lease=connections.acquire()){
                if(!value.value("consumedMaterialsDefinition").isBlank()){
                    var materials=vn.clinicmanagement.application.catalog.VatTuHaoPhiCauHinh.decode(value.value("consumedMaterialsDefinition"));
                    for(var line:materials.lines())try(var check=lease.connection().prepareStatement(
                            "SELECT TOP 1 i.id FROM Item i JOIN Kho k ON 1=1 "
                            +"WHERE i.id=? AND k.id=? AND k.maCoSo=? AND i.isDeleted=0 AND i.hoatDong=1 AND i.loaiItem IN ('VAT_TU','HOA_CHAT') "
                            +"AND k.isDeleted=0 AND k.trangThai='HOAT_DONG' AND k.loaiKho LIKE 'TU[_]TRUC%' AND EXISTS(SELECT 1 FROM dbo.TuTrucKhoKhoa m WHERE m.maKho=k.id AND m.maCoSo=k.maCoSo AND m.maKhoa=?)")){
                        check.setInt(1,line.itemId());check.setInt(2,line.warehouseId());check.setInt(3,context.facilityId());check.setInt(4,context.departmentId());
                        try(var rs=check.executeQuery()){if(!rs.next())return KetQua.failure("MATERIAL_UNAVAILABLE","Vật tư hoặc kho hao phí không còn hợp lệ trong cơ sở hiện tại.");}
                    }
                }
                saveRelationalMaterials(lease.connection(),value.id(),context,materialDefinition);
                String sql="UPDATE DanhMucDichVu SET maDichVu=?,tenDichVu=?,loaiDichVu=?,nhomDichVu=?,giaBHYT=?,donGia=?,giaVienPhi=?,giaNuocNgoai=?,hoatDong=?,moTa=?,updatedBy=?,"
                        +"loaiBenhPham=CASE WHEN ? IS NULL THEN loaiBenhPham ELSE NULLIF(?,N'') END,"
                        +"updatedAt=SYSDATETIME(),version=version+1 WHERE id=? AND version=? AND isDeleted=0";
                try(var ps=lease.connection().prepareStatement(sql)){
                    ps.setString(1,value.value("code").trim());ps.setString(2,value.value("name").trim());
                    ps.setString(3,switch(value.value("category")){case "Khám bệnh"->"KHAM";case "Xét nghiệm"->"XET_NGHIEM";case "Chẩn đoán hình ảnh"->"CDHA";default->"DVKHAC";});
                    ps.setString(4,value.value("group").trim());ps.setBigDecimal(5,DichVuCauHinh.number(value.value("insurancePrice")));
                    ps.setBigDecimal(6,DichVuCauHinh.number(value.value("servicePrice")));
                    ps.setBigDecimal(7,DichVuCauHinh.number(value.value("hospitalPrice")));
                    ps.setBigDecimal(8,DichVuCauHinh.number(value.value("foreignPrice")));
                    ps.setBoolean(9,!Boolean.parseBoolean(value.value("locked")));
                    ps.setString(10,value.value("note"));ps.setInt(11,context.employeeId());
                    ps.setString(12,specimenType);ps.setString(13,specimenType);
                    ps.setInt(14,value.id());ps.setLong(15,value.version());
                    if(ps.executeUpdate()!=1)return KetQua.failure("CONFLICT","Dịch vụ đã được người khác sửa. Đóng và mở lại để tải bản mới; nội dung đang nhập vẫn được giữ.");
                }
                try(var ps=lease.connection().prepareStatement("UPDATE DichVuCauHinh SET giaTriXml=?,updatedAt=SYSDATETIME(),updatedBy=? WHERE maDichVu=?")){
                    ps.setString(1,xml);ps.setInt(2,context.employeeId());ps.setInt(3,value.id());
                    if(ps.executeUpdate()==0)try(var insert=lease.connection().prepareStatement("INSERT INTO DichVuCauHinh(maDichVu,giaTriXml,updatedBy) VALUES(?,?,?)")){
                        insert.setInt(1,value.id());insert.setString(2,xml);insert.setInt(3,context.employeeId());insert.executeUpdate();
                    }
                }
                audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(context.employeeId(),context.roleId(),context.roleCode(),context.facilityId(),context.departmentId(),
                        KiemToanHanhDongMa.SERVICE_CATALOG_UPDATED,KiemToanTaiNguyenLoai.SERVICE_CATALOG,value.id(),KiemToanKetQua.SUCCESS,null,Instant.now(),null,context.sessionId()));
                return KetQua.success(load(value.id(),context));
            }catch(SQLException ex){
                if(ex.getErrorCode()==2601||ex.getErrorCode()==2627)return KetQua.failure("DUPLICATE","Mã dịch vụ đã tồn tại. Chọn mã khác.");
                throw new IllegalStateException("Không lưu được cấu hình dịch vụ.",ex);
            }
        });
    }
    private static DichVuCauHinh map(ResultSet rs)throws SQLException{
        Map<String,String> fields=new LinkedHashMap<>(decode(rs.getString("giaTriXml")));
        fields.put("code",rs.getString("maDichVu"));fields.put("name",rs.getString("tenDichVu"));
        fields.put("category",switch(rs.getString("loaiDichVu")){case "KHAM","KHAM_BENH"->"Khám bệnh";case "XET_NGHIEM"->"Xét nghiệm";case "CDHA","CHAN_DOAN_HINH_ANH"->"Chẩn đoán hình ảnh";default->"Chuyên khoa";});
        fields.put("group",Objects.toString(rs.getString("nhomDichVu"),""));
        fields.put("insurancePrice",rs.getBigDecimal("giaBHYT").toPlainString());fields.put("servicePrice",rs.getBigDecimal("donGia").toPlainString());
        var hospitalPrice=rs.getBigDecimal("giaVienPhi");if(hospitalPrice!=null)fields.put("hospitalPrice",hospitalPrice.toPlainString());
        var foreignPrice=rs.getBigDecimal("giaNuocNgoai");if(foreignPrice!=null)fields.put("foreignPrice",foreignPrice.toPlainString());
        fields.put("note",Objects.toString(rs.getString("moTa"),""));fields.put("locked",String.valueOf(!rs.getBoolean("hoatDong")));
        String specimenType=rs.getString("loaiBenhPham");
        if(specimenType!=null)fields.put("labSpecimenType",specimenType);
        return new DichVuCauHinh(rs.getInt("id"),rs.getLong("version"),fields);
    }
    private static DichVuCauHinh withRelationalMaterials(Connection c,DichVuCauHinh value,PhanQuyenNguCanh context)throws SQLException{
        String sql="SELECT h.stage,h.rounding,h.version,i.id,i.maItem,i.tenItem,i.donViCoBan,ISNULL(i.quyCachDongGoi,N''),k.id,k.tenKho,d.soLuongDinhMuc "
                +"FROM dbo.DichVuVatTuHaoPhi h LEFT JOIN dbo.DichVuVatTuHaoPhiDong d ON d.maCauHinh=h.id "
                +"LEFT JOIN dbo.Item i ON i.id=d.maItem LEFT JOIN dbo.Kho k ON k.id=h.maKho "
                +"WHERE h.maDichVu=? AND h.maCoSo=? AND h.maKhoa=? ORDER BY h.maKho,d.id";
        List<vn.clinicmanagement.application.catalog.VatTuHaoPhiCauHinh.Line> lines=new ArrayList<>();
        vn.clinicmanagement.application.catalog.VatTuHaoPhiCauHinh.Stage stage=null;boolean rounding=false,found=false;
        try(var ps=c.prepareStatement(sql)){ps.setInt(1,value.id());ps.setInt(2,context.facilityId());ps.setInt(3,context.departmentId());try(var rs=ps.executeQuery()){
            while(rs.next()){
                found=true;var rowStage=vn.clinicmanagement.application.catalog.VatTuHaoPhiCauHinh.Stage.valueOf(rs.getString(1));boolean rowRounding=rs.getBoolean(2);
                if(stage!=null&&(stage!=rowStage||rounding!=rowRounding))throw new SQLException("Inconsistent service consumption config scope");stage=rowStage;rounding=rowRounding;
                if(rs.getObject(4)!=null)lines.add(new vn.clinicmanagement.application.catalog.VatTuHaoPhiCauHinh.Line(rs.getInt(4),rs.getInt(9),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(10),rs.getBigDecimal(11)));
            }
        }}
        if(!found)return value;
        var definition=new vn.clinicmanagement.application.catalog.VatTuHaoPhiCauHinh(stage,rounding,lines);
        Map<String,String> fields=new LinkedHashMap<>(value.values());fields.put("consumedMaterialsDefinition",definition.encode());fields.put("consumedMaterials",definition.summary());
        return new DichVuCauHinh(value.id(),value.version(),fields);
    }
    private static void saveRelationalMaterials(Connection c,int serviceId,PhanQuyenNguCanh context,String encoded)throws SQLException{
        var definition=vn.clinicmanagement.application.catalog.VatTuHaoPhiCauHinh.decode(encoded);definition.validate();
        Map<Integer,List<vn.clinicmanagement.application.catalog.VatTuHaoPhiCauHinh.Line>> groups=new TreeMap<>();
        for(var line:definition.lines())groups.computeIfAbsent(line.warehouseId(),ignored->new ArrayList<>()).add(line);
        Map<Integer,Integer> existing=new HashMap<>();
        try(var ps=c.prepareStatement("SELECT id,maKho FROM dbo.DichVuVatTuHaoPhi WHERE maDichVu=? AND maCoSo=? AND maKhoa=?")){
            ps.setInt(1,serviceId);ps.setInt(2,context.facilityId());ps.setInt(3,context.departmentId());try(var rs=ps.executeQuery()){while(rs.next())existing.put(rs.getInt(2),rs.getInt(1));}}
        Set<Integer> keep=new HashSet<>();
        for(var entry:groups.entrySet()){
            int warehouse=entry.getKey(),headerId;
            Integer old=existing.get(warehouse);
            if(old==null){
                try(var ps=c.prepareStatement("INSERT INTO dbo.DichVuVatTuHaoPhi(maDichVu,maCoSo,maKhoa,maKho,stage,rounding,updatedBy) OUTPUT INSERTED.id VALUES(?,?,?,?,?,?,?)")){
                    ps.setInt(1,serviceId);ps.setInt(2,context.facilityId());ps.setInt(3,context.departmentId());ps.setInt(4,warehouse);ps.setString(5,definition.stage().name());ps.setBoolean(6,definition.rounding());ps.setInt(7,context.employeeId());try(var rs=ps.executeQuery()){rs.next();headerId=rs.getInt(1);}}
            }else{
                headerId=old;try(var ps=c.prepareStatement("UPDATE dbo.DichVuVatTuHaoPhi SET stage=?,rounding=?,version=version+1,updatedAt=SYSUTCDATETIME(),updatedBy=? WHERE id=?")){
                    ps.setString(1,definition.stage().name());ps.setBoolean(2,definition.rounding());ps.setInt(3,context.employeeId());ps.setInt(4,headerId);ps.executeUpdate();}
            }
            keep.add(headerId);
            try(var del=c.prepareStatement("DELETE FROM dbo.DichVuVatTuHaoPhiDong WHERE maCauHinh=?")){del.setInt(1,headerId);del.executeUpdate();}
            try(var ins=c.prepareStatement("INSERT INTO dbo.DichVuVatTuHaoPhiDong(maCauHinh,maItem,soLuongDinhMuc,donVi) VALUES(?,?,?,?)")){
                for(var line:entry.getValue()){ins.setInt(1,headerId);ins.setInt(2,line.itemId());ins.setBigDecimal(3,line.quantity());ins.setString(4,line.unit());ins.addBatch();}ins.executeBatch();
            }
        }
        List<Integer> stale=existing.entrySet().stream().filter(e->!keep.contains(e.getValue())).map(Map.Entry::getValue).toList();
        for(int id:stale){try(var del=c.prepareStatement("DELETE FROM dbo.DichVuVatTuHaoPhiDong WHERE maCauHinh=?")){del.setInt(1,id);del.executeUpdate();}try(var del=c.prepareStatement("DELETE FROM dbo.DichVuVatTuHaoPhi WHERE id=?")){del.setInt(1,id);del.executeUpdate();}}
    }
    static String encode(Map<String,String> values){
        Properties p=new Properties();p.putAll(values);
        try(var bytes=new ByteArrayOutputStream()){p.storeToXML(bytes,"Cấu hình dịch vụ",StandardCharsets.UTF_8);return bytes.toString(StandardCharsets.UTF_8);}
        catch(IOException ex){throw new IllegalStateException("Không chuẩn bị được dữ liệu cấu hình.",ex);}
    }
    static Map<String,String> decode(String xml){
        if(xml==null||xml.isBlank())return Map.of();Properties p=new Properties();
        try(var bytes=new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))){p.loadFromXML(bytes);}
        catch(IOException ex){throw new IllegalStateException("Cấu hình dịch vụ bị lỗi; không thể mở để ghi đè.",ex);}
        Map<String,String> result=new LinkedHashMap<>();p.forEach((k,v)->result.put(k.toString(),v.toString()));return result;
    }
}
