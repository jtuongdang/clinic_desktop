package vn.clinicmanagement.infrastructure.persistence.inventory;

import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.*;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.infrastructure.persistence.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Atomic service-consumption posting to cabinet stock with FEFO allocations and a PENDING shortage state. */
public final class TieuHaoDichVuRepository implements TieuHaoDichVuCongKetNoi {
    private final JdbcKetNoiNhaCungCap connections;
    private final JdbcGiaoDichQuanLy transactions;
    private final PhanQuyenDichVu authorization;
    private final KiemToanWriteCongKetNoi audit;
    public TieuHaoDichVuRepository(JdbcKetNoiNhaCungCap connections,JdbcGiaoDichQuanLy transactions,
                                   PhanQuyenDichVu authorization,KiemToanWriteCongKetNoi audit){
        this.connections=Objects.requireNonNull(connections);this.transactions=Objects.requireNonNull(transactions);
        this.authorization=Objects.requireNonNull(authorization);this.audit=Objects.requireNonNull(audit);
    }
    @Override public List<CauHinh> getDefinition(PhanQuyenNguCanh context,int orderId){
        if(!canView(context))return List.of();
        try(var lease=connections.acquire()){
            Connection c=lease.connection();Order order=loadOrder(c,context,orderId);if(order==null)return List.of();
            return loadDefinitions(c,order,context).stream()
                    .filter(line -> "RESULT".equals(line.stage()))
                    .map(line -> new CauHinh(line.itemId(),line.cabinetId(),line.code(),line.name(),line.unit(),line.standard(),line.rounding()))
                    .toList();
        }catch(SQLException ex){throw new IllegalStateException("Không tải được định mức tiêu hao dịch vụ.",ex);}
    }
    @Override public KetQua<BienNhan> record(PhanQuyenNguCanh context,int serviceOrderId,UUID operationKey,List<SoLuongThucTe> actualQuantities,String reason){
        if(!allowed(context))return KetQua.failure("FORBIDDEN","Phiên hiện tại không có quyền ghi nhận tiêu hao dịch vụ.");
        if(serviceOrderId<=0||operationKey==null||actualQuantities==null||actualQuantities.stream().anyMatch(Objects::isNull)
                ||actualQuantities.stream().map(x->x.itemId()+":"+x.cabinetId()).distinct().count()!=actualQuantities.size()
                ||actualQuantities.stream().anyMatch(x->x.itemId()<=0||x.cabinetId()<=0||x.quantity()==null||x.quantity().signum()<0||x.quantity().scale()>4||x.quantity().precision()>18)
                ||(reason!=null&&reason.length()>500))return KetQua.failure("INVALID","Lượng tiêu hao thực tế không hợp lệ.");
        return transactions.inTransaction(()->{try(var lease=connections.acquire()){
            Connection c=lease.connection();Order order=loadOrder(c,context,serviceOrderId);if(order==null)return KetQua.failure("SCOPE","Chỉ định không thuộc cơ sở/khoa/phòng hiện tại.");
            String payload=payload(actualQuantities,reason);
            Existing existing=findExisting(c,serviceOrderId);
            if(existing!=null){
                if(!existing.operationKey().equals(operationKey)||!sameLines(c,existing.id(),actualQuantities))return KetQua.failure("ALREADY_RECORDED","Chỉ định đã có chứng từ tiêu hao. Mở lại dữ liệu thay vì ghi thêm.");
                return KetQua.success(new BienNhan(existing.id(),existing.status(),countPostedLines(c,existing.id())));
            }
            List<DefinitionLine> definitions=loadDefinitions(c,order,context);
            if (!definitions.isEmpty() && definitions.stream().noneMatch(line -> "RESULT".equals(line.stage()))) {
                return KetQua.success(new BienNhan(0,"NOT_TRIGGERED",0));
            }
            definitions=definitions.stream().filter(line -> "RESULT".equals(line.stage())).toList();
            if(definitions.isEmpty()){
                long id=insertHeader(c,serviceOrderId,order,context,operationKey,null,"RESULT","NO_DEFINITION",reason);
                audit(context,serviceOrderId,KiemToanHanhDongMa.SERVICE_CONSUMPTION_PENDING);
                return KetQua.success(new BienNhan(id,"NO_DEFINITION",0));
            }
            Map<String,SoLuongThucTe> actuals=new HashMap<>();for(var actual:actualQuantities)actuals.put(key(actual.itemId(),actual.cabinetId()),actual);
            if(actuals.size()!=definitions.size()||definitions.stream().anyMatch(d->!actuals.containsKey(key(d.itemId(),d.cabinetId()))))
                return KetQua.failure("ACTUAL_REQUIRED","Nhập lượng thực tế cho từng vật tư trong định mức trước khi lưu kết quả.");
            String stage=definitions.get(0).stage();long configVersion=definitions.stream().mapToLong(DefinitionLine::version).max().orElse(0);
            long eventId=insertHeader(c,serviceOrderId,order,context,operationKey,configVersion,stage,"PENDING",reason);
            List<UsageLine> lines=new ArrayList<>();
            for(var def:definitions){var input=actuals.get(key(def.itemId(),def.cabinetId()));
                try(var ps=c.prepareStatement("INSERT INTO dbo.TuTrucTieuHaoDong(maTieuHao,maKho,maItem,soLuongDinhMuc,soLuongThucTe,donVi,basis,trangThai) OUTPUT INSERTED.id VALUES(?,?,?,?,?,?,?,'PENDING')")){
                    ps.setLong(1,eventId);ps.setInt(2,def.cabinetId());ps.setInt(3,def.itemId());ps.setBigDecimal(4,def.standard());ps.setBigDecimal(5,input.quantity());ps.setString(6,def.unit());ps.setString(7,def.basis());try(var rs=ps.executeQuery()){rs.next();lines.add(new UsageLine(rs.getLong(1),def,input.quantity()));}}
            }
            Map<Long,List<Batch>> allocations=new LinkedHashMap<>();boolean enough=true;
            for(var line:lines){List<Batch> lots=new ArrayList<>();BigDecimal remaining=line.actual();
                if(remaining.signum()>0)try(var ps=c.prepareStatement("SELECT t.id,t.soLuongTon,t.giaNhap FROM dbo.TonKho t WITH(UPDLOCK,HOLDLOCK) JOIN dbo.Kho k ON k.id=t.maKho JOIN dbo.Item i ON i.id=t.maItem WHERE t.maKho=? AND t.maItem=? AND k.maCoSo=? AND k.isDeleted=0 AND k.trangThai='HOAT_DONG' AND k.loaiKho LIKE 'TU[_]TRUC%' AND i.isDeleted=0 AND i.hoatDong=1 AND t.isDeleted=0 AND t.hanSuDung>=CAST(GETDATE() AS DATE) AND t.soLuongTon>0 ORDER BY t.hanSuDung,t.id")){
                    ps.setInt(1,line.definition().cabinetId());ps.setInt(2,line.definition().itemId());ps.setInt(3,context.facilityId());try(var rs=ps.executeQuery()){while(rs.next()&&remaining.signum()>0){BigDecimal take=remaining.min(rs.getBigDecimal(2));lots.add(new Batch(rs.getInt(1),take,rs.getBigDecimal(2),rs.getBigDecimal(3)));remaining=remaining.subtract(take);}}}
                if(remaining.signum()>0)enough=false;allocations.put(line.id(),lots);
            }
            if(!enough){audit(context,serviceOrderId,KiemToanHanhDongMa.SERVICE_CONSUMPTION_PENDING);return KetQua.success(new BienNhan(eventId,"PENDING",0));}
            int posted=0;String code="TH-"+eventId;
            for(var line:lines){for(var batch:allocations.get(line.id())){
                BigDecimal after=batch.before().subtract(batch.quantity());
                try(var up=c.prepareStatement("UPDATE dbo.TonKho SET soLuongTon=?,version=version+1,updatedAt=SYSUTCDATETIME(),updatedBy=? WHERE id=? AND soLuongTon>=? AND isDeleted=0")){
                    up.setBigDecimal(1,after);up.setInt(2,context.employeeId());up.setInt(3,batch.id());up.setBigDecimal(4,batch.quantity());if(up.executeUpdate()!=1)throw new SQLException("Stock changed during consumption posting");}
                try(var tx=c.prepareStatement("INSERT INTO dbo.InventoryTransaction(maKho,maItem,maTonKho,loaiGiaoDich,soLuongThayDoi,tonTruocKhiThayDoi,tonSauKhiThayDoi,maChungTu,createdBy) VALUES(?,?,?,'TU_TRUC_TIEU_HAO',?,?,?,?,?)")){
                    tx.setInt(1,line.definition().cabinetId());tx.setInt(2,line.definition().itemId());tx.setInt(3,batch.id());tx.setBigDecimal(4,batch.quantity().negate());tx.setBigDecimal(5,batch.before());tx.setBigDecimal(6,after);tx.setString(7,code);tx.setInt(8,context.employeeId());tx.executeUpdate();}
                try(var al=c.prepareStatement("INSERT INTO dbo.TuTrucTieuHaoLo(maDong,maTonKho,soLuong,donGia) VALUES(?,?,?,?)")){
                    al.setLong(1,line.id());al.setInt(2,batch.id());al.setBigDecimal(3,batch.quantity());al.setBigDecimal(4,batch.price());al.executeUpdate();}
            }if(line.actual().signum()>0)posted++;try(var up=c.prepareStatement("UPDATE dbo.TuTrucTieuHaoDong SET trangThai='POSTED' WHERE id=?")){up.setLong(1,line.id());up.executeUpdate();}}
            try(var up=c.prepareStatement("UPDATE dbo.TuTrucTieuHao SET trangThai='POSTED' WHERE id=? AND trangThai='PENDING'")){up.setLong(1,eventId);up.executeUpdate();}
            try(var demand=c.prepareStatement("INSERT INTO dbo.TuTrucNhuCauBu(maTieuHaoDong,maCoSo,maKhoa,maKho,maItem,soLuongCoTheBu) SELECT d.id,h.maCoSo,h.maKhoa,d.maKho,d.maItem,d.soLuongThucTe FROM dbo.TuTrucTieuHaoDong d JOIN dbo.TuTrucTieuHao h ON h.id=d.maTieuHao WHERE h.id=? AND d.soLuongThucTe>0 AND NOT EXISTS(SELECT 1 FROM dbo.TuTrucNhuCauBu n WHERE n.maTieuHaoDong=d.id)")){demand.setLong(1,eventId);demand.executeUpdate();}
            audit(context,serviceOrderId,KiemToanHanhDongMa.SERVICE_CONSUMPTION_POSTED);
            return KetQua.success(new BienNhan(eventId,"POSTED",posted));
        }catch(SQLException ex){throw new IllegalStateException("Không ghi được tiêu hao vật tư tủ trực.",ex);}});
    }
    @Override public List<ChoXuLy> getPending(PhanQuyenNguCanh context){
        if(!allowed(context))return List.of();String sql="SELECT id,maChiDinhDichVu,maDichVu,trangThai,reason FROM dbo.TuTrucTieuHao WHERE maCoSo=? AND maKhoa=? AND trangThai='PENDING' ORDER BY executedAt,id";
        try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){ps.setInt(1,context.facilityId());ps.setInt(2,context.departmentId());List<ChoXuLy> rows=new ArrayList<>();try(var rs=ps.executeQuery()){while(rs.next())rows.add(new ChoXuLy(rs.getLong(1),rs.getInt(2),rs.getInt(3),rs.getString(4),rs.getString(5)));}return List.copyOf(rows);}
        catch(SQLException ex){throw new IllegalStateException("Không tải được tiêu hao đang chờ.",ex);}
    }
    @Override public KetQua<BienNhan> retryPending(PhanQuyenNguCanh context,long eventId){
        if(!allowed(context))return KetQua.failure("FORBIDDEN","Phiên hiện tại không có quyền xử lý tiêu hao đang chờ.");
        if(eventId<=0)return KetQua.failure("INVALID","Mã tiêu hao không hợp lệ.");
        return transactions.inTransaction(()->{try(var lease=connections.acquire()){
            Connection c=lease.connection();String status;int orderId;
            try(var lock=c.prepareStatement("SELECT trangThai,maChiDinhDichVu FROM dbo.TuTrucTieuHao WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND maCoSo=? AND maKhoa=?")){
                lock.setLong(1,eventId);lock.setInt(2,context.facilityId());lock.setInt(3,context.departmentId());try(var rs=lock.executeQuery()){if(!rs.next())return KetQua.failure("NOT_FOUND","Không tìm thấy tiêu hao trong khoa/phòng hiện tại.");status=rs.getString(1);orderId=rs.getInt(2);}}
            if("POSTED".equals(status))return KetQua.success(new BienNhan(eventId,status,countPostedLines(c,eventId)));
            if(!"PENDING".equals(status))return KetQua.failure("STATE","Tiêu hao không ở trạng thái chờ xử lý.");
            List<UsageLine> lines=new ArrayList<>();
            try(var ps=c.prepareStatement("SELECT id,maKho,maItem,soLuongDinhMuc,donVi,basis,soLuongThucTe FROM dbo.TuTrucTieuHaoDong WHERE maTieuHao=? ORDER BY maKho,maItem")){
                ps.setLong(1,eventId);try(var rs=ps.executeQuery()){while(rs.next()){DefinitionLine d=new DefinitionLine(rs.getInt(3),rs.getInt(2),"","",rs.getString(5),rs.getBigDecimal(4),rs.getString(6),"RESULT",0,false);lines.add(new UsageLine(rs.getLong(1),d,rs.getBigDecimal(7)));}}}
            Map<Long,List<Batch>> allocations=new LinkedHashMap<>();boolean enough=true;
            for(var line:lines){List<Batch> lots=new ArrayList<>();BigDecimal remaining=line.actual();
                if(remaining.signum()>0)try(var ps=c.prepareStatement("SELECT t.id,t.soLuongTon,t.giaNhap FROM dbo.TonKho t WITH(UPDLOCK,HOLDLOCK) JOIN dbo.Item i ON i.id=t.maItem JOIN dbo.TuTrucKhoKhoa m ON m.maKho=t.maKho AND m.maCoSo=? AND m.maKhoa=? WHERE t.maKho=? AND t.maItem=? AND i.isDeleted=0 AND i.hoatDong=1 AND t.isDeleted=0 AND t.hanSuDung>=CAST(GETDATE() AS DATE) AND t.soLuongTon>0 ORDER BY t.hanSuDung,t.id")){
                    ps.setInt(1,context.facilityId());ps.setInt(2,context.departmentId());ps.setInt(3,line.definition().cabinetId());ps.setInt(4,line.definition().itemId());try(var rs=ps.executeQuery()){while(rs.next()&&remaining.signum()>0){BigDecimal take=remaining.min(rs.getBigDecimal(2));lots.add(new Batch(rs.getInt(1),take,rs.getBigDecimal(2),rs.getBigDecimal(3)));remaining=remaining.subtract(take);}}}
                if(remaining.signum()>0)enough=false;allocations.put(line.id(),lots);
            }
            if(!enough)return KetQua.success(new BienNhan(eventId,"PENDING",0));
            String code="TH-"+eventId;int posted=0;
            for(var line:lines){for(var batch:allocations.get(line.id())){
                BigDecimal after=batch.before().subtract(batch.quantity());
                try(var up=c.prepareStatement("UPDATE dbo.TonKho SET soLuongTon=?,version=version+1,updatedAt=SYSUTCDATETIME(),updatedBy=? WHERE id=? AND soLuongTon>=? AND isDeleted=0")){
                    up.setBigDecimal(1,after);up.setInt(2,context.employeeId());up.setInt(3,batch.id());up.setBigDecimal(4,batch.quantity());if(up.executeUpdate()!=1)throw new SQLException("Stock changed while retrying pending consumption");}
                try(var tx=c.prepareStatement("INSERT INTO dbo.InventoryTransaction(maKho,maItem,maTonKho,loaiGiaoDich,soLuongThayDoi,tonTruocKhiThayDoi,tonSauKhiThayDoi,maChungTu,createdBy) VALUES(?,?,?,'TU_TRUC_TIEU_HAO',?,?,?,?,?)")){
                    tx.setInt(1,line.definition().cabinetId());tx.setInt(2,line.definition().itemId());tx.setInt(3,batch.id());tx.setBigDecimal(4,batch.quantity().negate());tx.setBigDecimal(5,batch.before());tx.setBigDecimal(6,after);tx.setString(7,code);tx.setInt(8,context.employeeId());tx.executeUpdate();}
                try(var al=c.prepareStatement("INSERT INTO dbo.TuTrucTieuHaoLo(maDong,maTonKho,soLuong,donGia) VALUES(?,?,?,?)")){
                    al.setLong(1,line.id());al.setInt(2,batch.id());al.setBigDecimal(3,batch.quantity());al.setBigDecimal(4,batch.price());al.executeUpdate();}
            }if(line.actual().signum()>0)posted++;try(var up=c.prepareStatement("UPDATE dbo.TuTrucTieuHaoDong SET trangThai='POSTED' WHERE id=? AND trangThai='PENDING'")){up.setLong(1,line.id());up.executeUpdate();}}
            try(var up=c.prepareStatement("UPDATE dbo.TuTrucTieuHao SET trangThai='POSTED' WHERE id=? AND trangThai='PENDING'")){up.setLong(1,eventId);if(up.executeUpdate()!=1)return KetQua.failure("CONFLICT","Tiêu hao đã được xử lý bởi người khác.");}
            insertDemandRows(c,eventId);audit(context,orderId,KiemToanHanhDongMa.SERVICE_CONSUMPTION_POSTED);
            return KetQua.success(new BienNhan(eventId,"POSTED",posted));
        }catch(SQLException ex){throw new IllegalStateException("Không thể thử ghi sổ tiêu hao đang chờ.",ex);}});
    }
    private static void insertDemandRows(Connection c,long eventId)throws SQLException{
        try(var demand=c.prepareStatement("INSERT INTO dbo.TuTrucNhuCauBu(maTieuHaoDong,maCoSo,maKhoa,maKho,maItem,soLuongCoTheBu) SELECT d.id,h.maCoSo,h.maKhoa,d.maKho,d.maItem,d.soLuongThucTe FROM dbo.TuTrucTieuHaoDong d JOIN dbo.TuTrucTieuHao h ON h.id=d.maTieuHao WHERE h.id=? AND d.soLuongThucTe>0 AND NOT EXISTS(SELECT 1 FROM dbo.TuTrucNhuCauBu n WHERE n.maTieuHaoDong=d.id)")){demand.setLong(1,eventId);demand.executeUpdate();}
    }
    @Override public List<BaoCaoDong> getReport(PhanQuyenNguCanh context,java.time.LocalDateTime from,java.time.LocalDateTime until){
        if(!canView(context))return List.of();if(from==null||until==null||!from.isBefore(until))throw new IllegalArgumentException("Khoảng báo cáo không hợp lệ.");
        String sql="SELECT h.executedAt,o.id,s.maDichVu,s.tenDichVu,d.maItem,i.maItem,i.tenItem,d.donVi,k.tenKho,d.soLuongThucTe,ISNULL(SUM(a.soLuong),0),h.trangThai "
                +"FROM dbo.TuTrucTieuHao h JOIN dbo.ChiDinhDichVu o ON o.id=h.maChiDinhDichVu JOIN dbo.DanhMucDichVu s ON s.id=h.maDichVu "
                +"JOIN dbo.TuTrucTieuHaoDong d ON d.maTieuHao=h.id JOIN dbo.Item i ON i.id=d.maItem JOIN dbo.Kho k ON k.id=d.maKho "
                +"LEFT JOIN dbo.TuTrucTieuHaoLo a ON a.maDong=d.id WHERE h.maCoSo=? AND h.maKhoa=? AND h.executedAt>=? AND h.executedAt<? "
                +"GROUP BY h.executedAt,o.id,s.maDichVu,s.tenDichVu,d.maItem,i.maItem,i.tenItem,d.donVi,k.tenKho,d.soLuongThucTe,h.trangThai ORDER BY h.executedAt,o.id,k.tenKho,i.tenItem";
        try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){ps.setInt(1,context.facilityId());ps.setInt(2,context.departmentId());ps.setTimestamp(3,Timestamp.valueOf(from));ps.setTimestamp(4,Timestamp.valueOf(until));List<BaoCaoDong> rows=new ArrayList<>();try(var rs=ps.executeQuery()){while(rs.next())rows.add(new BaoCaoDong(rs.getTimestamp(1).toLocalDateTime(),rs.getInt(2),rs.getString(3),rs.getString(4),rs.getInt(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(9),rs.getBigDecimal(10),rs.getBigDecimal(11),rs.getString(12)));}return List.copyOf(rows);}
        catch(SQLException ex){throw new IllegalStateException("Không truy vấn được báo cáo tiêu hao dịch vụ.",ex);}
    }
    private boolean allowed(PhanQuyenNguCanh context){return authorized(context,HanhDongQuyen.GHI_NHAN_TIEU_HAO);}
    private boolean canView(PhanQuyenNguCanh context){return authorized(context,HanhDongQuyen.XEM);}
    private boolean authorized(PhanQuyenNguCanh context,HanhDongQuyen action){return context!=null&&context.isValid()&&authorization.evaluate(new PhanQuyenYeuCau(context,"INVENTORY",action,context.facilityId(),context.departmentId())).allowed();}
    private record Order(int serviceId,int departmentId){}
    private record DefinitionLine(int itemId,int cabinetId,String code,String name,String unit,BigDecimal standard,String basis,String stage,long version,boolean rounding){}
    private record UsageLine(long id,DefinitionLine definition,BigDecimal actual){}
    private record Batch(int id,BigDecimal quantity,BigDecimal before,BigDecimal price){}
    private record Existing(long id,UUID operationKey,String status){}
    private static Order loadOrder(Connection c,PhanQuyenNguCanh ctx,int id)throws SQLException{
        try(var ps=c.prepareStatement("SELECT o.maDichVu,COALESCE(p.maKhoa,l.maKhoa) FROM dbo.ChiDinhDichVu o JOIN dbo.LuotKham l ON l.id=o.maLuotKham LEFT JOIN dbo.PhongKham p ON p.id=o.maPhongThucHien WHERE o.id=? AND l.maCoSo=? AND COALESCE(p.maKhoa,l.maKhoa)=? AND (p.id IS NULL OR EXISTS(SELECT 1 FROM dbo.Khoa k WHERE k.id=p.maKhoa AND k.maCoSo=?)) AND o.isDeleted=0")){
            ps.setInt(1,id);ps.setInt(2,ctx.facilityId());ps.setInt(3,ctx.departmentId());ps.setInt(4,ctx.facilityId());try(var rs=ps.executeQuery()){return rs.next()?new Order(rs.getInt(1),rs.getInt(2)):null;}}
    }
    private static List<DefinitionLine> loadDefinitions(Connection c,Order order,PhanQuyenNguCanh ctx)throws SQLException{
        String sql="SELECT d.maItem,h.maKho,i.maItem,i.tenItem,d.donVi,d.soLuongDinhMuc,d.basis,h.stage,h.version,h.rounding FROM dbo.DichVuVatTuHaoPhi h JOIN dbo.DichVuVatTuHaoPhiDong d ON d.maCauHinh=h.id JOIN dbo.Item i ON i.id=d.maItem JOIN dbo.TuTrucKhoKhoa m ON m.maKho=h.maKho AND m.maCoSo=h.maCoSo AND m.maKhoa=h.maKhoa WHERE h.maDichVu=? AND h.maCoSo=? AND h.maKhoa=? AND m.maKhoa=? ORDER BY h.maKho,d.id";
        List<DefinitionLine> result=new ArrayList<>();try(var ps=c.prepareStatement(sql)){ps.setInt(1,order.serviceId());ps.setInt(2,ctx.facilityId());ps.setInt(3,order.departmentId());ps.setInt(4,ctx.departmentId());try(var rs=ps.executeQuery()){while(rs.next())result.add(new DefinitionLine(rs.getInt(1),rs.getInt(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getBigDecimal(6),rs.getString(7),rs.getString(8),rs.getLong(9),rs.getBoolean(10)));}}return result;
    }
    private static long insertHeader(Connection c,int orderId,Order order,PhanQuyenNguCanh ctx,UUID key,Long configVersion,String stage,String status,String reason)throws SQLException{
        try(var ps=c.prepareStatement("INSERT INTO dbo.TuTrucTieuHao(maChiDinhDichVu,operationKey,maDichVu,maCoSo,maKhoa,configVersion,triggerStage,trangThai,createdBy,reason) OUTPUT INSERTED.id VALUES(?,?,?,?,?,?,?,?,?,?)")){
            ps.setInt(1,orderId);ps.setObject(2,key);ps.setInt(3,order.serviceId());ps.setInt(4,ctx.facilityId());ps.setInt(5,order.departmentId());
            if(configVersion==null)ps.setNull(6,Types.BIGINT);else ps.setLong(6,configVersion);ps.setString(7,stage);ps.setString(8,status);ps.setInt(9,ctx.employeeId());ps.setString(10,reason);
            try(var rs=ps.executeQuery()){if(!rs.next())throw new SQLException("Missing consumption event ID");return rs.getLong(1);}
        }
    }
    private static Existing findExisting(Connection c,int orderId)throws SQLException{
        try(var ps=c.prepareStatement("SELECT id,operationKey,trangThai FROM dbo.TuTrucTieuHao WHERE maChiDinhDichVu=?")){ps.setInt(1,orderId);try(var rs=ps.executeQuery()){return rs.next()?new Existing(rs.getLong(1),rs.getObject(2,UUID.class),rs.getString(3)):null;}}
    }
    private static boolean sameLines(Connection c,long event,List<SoLuongThucTe> actual)throws SQLException{
        Map<String,BigDecimal> stored=new HashMap<>();try(var ps=c.prepareStatement("SELECT maItem,maKho,soLuongThucTe FROM dbo.TuTrucTieuHaoDong WHERE maTieuHao=?")){ps.setLong(1,event);try(var rs=ps.executeQuery()){while(rs.next())stored.put(key(rs.getInt(1),rs.getInt(2)),rs.getBigDecimal(3));}}
        if(stored.size()!=actual.size())return false;return actual.stream().allMatch(a->stored.containsKey(key(a.itemId(),a.cabinetId()))&&stored.get(key(a.itemId(),a.cabinetId())).compareTo(a.quantity())==0);
    }
    private static int countPostedLines(Connection c,long event)throws SQLException{try(var ps=c.prepareStatement("SELECT COUNT(*) FROM dbo.TuTrucTieuHaoDong WHERE maTieuHao=? AND trangThai='POSTED' AND soLuongThucTe>0")){ps.setLong(1,event);try(var rs=ps.executeQuery()){rs.next();return rs.getInt(1);}}}
    private static String key(int item,int cabinet){return item+":"+cabinet;}
    private static String payload(List<SoLuongThucTe> values,String reason){return values.stream().sorted(Comparator.comparingInt(SoLuongThucTe::cabinetId).thenComparingInt(SoLuongThucTe::itemId)).map(v->v.cabinetId()+":"+v.itemId()+"="+v.quantity().stripTrailingZeros().toPlainString()).collect(java.util.stream.Collectors.joining(";"))+"|"+Objects.toString(reason,"");}
    private void audit(PhanQuyenNguCanh ctx,int orderId,KiemToanHanhDongMa action){audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(ctx.employeeId(),ctx.roleId(),ctx.roleCode(),ctx.facilityId(),ctx.departmentId(),action,KiemToanTaiNguyenLoai.SERVICE_CONSUMPTION,orderId,KiemToanKetQua.SUCCESS,null,Instant.now(),null,ctx.sessionId()));}
}
