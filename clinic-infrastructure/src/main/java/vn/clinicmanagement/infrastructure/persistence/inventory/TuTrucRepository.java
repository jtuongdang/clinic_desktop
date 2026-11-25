package vn.clinicmanagement.infrastructure.persistence.inventory;

import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi;
import vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi;
import vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.Bang;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenYeuCau;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.infrastructure.persistence.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;

public final class TuTrucRepository implements TuTrucCongKetNoi {
    // Existing installations store cabinet subtypes such as TU_TRUC_CAP_CUU.
    private static final String CABINET = "k.loaiKho LIKE 'TU[_]TRUC%'";
    private final JdbcKetNoiNhaCungCap connections;
    private final JdbcGiaoDichQuanLy transactions;
    private final KiemToanWriteCongKetNoi audit;
    private final PhanQuyenDichVu authorization;
    private final TieuHaoDichVuRepository consumption;

    public TuTrucRepository(JdbcKetNoiNhaCungCap connections, JdbcGiaoDichQuanLy transactions,
                           KiemToanWriteCongKetNoi audit) {
        this(connections, transactions, audit, null);
    }

    public TuTrucRepository(JdbcKetNoiNhaCungCap connections, JdbcGiaoDichQuanLy transactions,
                           KiemToanWriteCongKetNoi audit, PhanQuyenDichVu authorization) {
        this.connections = Objects.requireNonNull(connections);
        this.transactions = Objects.requireNonNull(transactions);
        this.audit = Objects.requireNonNull(audit);
        this.authorization = authorization;
        this.consumption = authorization==null?null:new TieuHaoDichVuRepository(connections,transactions,authorization,audit);
    }

    @Override public List<TieuHaoDichVuCongKetNoi.ChoXuLy> getPendingConsumption(PhanQuyenNguCanh context){return consumption==null?List.of():consumption.getPending(context);}
    @Override public KetQua<TieuHaoDichVuCongKetNoi.BienNhan> retryPendingConsumption(PhanQuyenNguCanh context,long id){return consumption==null?KetQua.failure("NOT_SUPPORTED","Chưa hỗ trợ xử lý tiêu hao chờ."):consumption.retryPending(context,id);}
    @Override public List<TieuHaoDichVuCongKetNoi.BaoCaoDong> getConsumptionReport(PhanQuyenNguCanh context,LocalDateTime from,LocalDateTime until){return consumption==null?List.of():consumption.getReport(context,from,until);}

    @Override public List<Kho> getCabinets(int facilityId) { return warehouses(facilityId, true); }
    @Override public List<Kho> getSupplyWarehouses(int facilityId) { return warehouses(facilityId, false); }
    @Override public List<Kho> getCabinets(PhanQuyenNguCanh context){
        if(!isAuthorized(context,HanhDongQuyen.XEM))return List.of();
        String sql="SELECT k.id,k.tenKho FROM dbo.Kho k JOIN dbo.TuTrucKhoKhoa m ON m.maKho=k.id AND m.maCoSo=k.maCoSo WHERE k.maCoSo=? AND m.maKhoa=? AND k.isDeleted=0 AND k.trangThai='HOAT_DONG' AND "+CABINET+" ORDER BY k.tenKho,k.id";
        return scopedWarehouses(sql,context.facilityId(),context.departmentId());
    }
    @Override public List<Kho> getSupplyWarehouses(PhanQuyenNguCanh context){return isAuthorized(context,HanhDongQuyen.XEM)?getSupplyWarehouses(context.facilityId()):List.of();}
    @Override public List<Phieu> getTickets(PhanQuyenNguCanh context,int cabinetId,LocalDateTime from,LocalDateTime until){return isAuthorized(context,HanhDongQuyen.XEM)&&cabinetMapped(context,cabinetId)?getTicketsScoped(context.facilityId(),context.departmentId(),cabinetId,from,until):List.of();}
    @Override public List<Lo> getStock(PhanQuyenNguCanh context,int warehouseId){return isAuthorized(context,HanhDongQuyen.XEM)&&warehouseScoped(context,warehouseId)?getStock(context.facilityId(),warehouseId):List.of();}
    @Override public List<Lo> getActiveItems(PhanQuyenNguCanh context){
        if(!isAuthorized(context,HanhDongQuyen.XEM))return List.of();
        String sql="SELECT 0,i.id,i.maItem,i.tenItem,i.donViCoBan,'',CAST(NULL AS DATE),CAST(0 AS DECIMAL(18,4)) FROM dbo.Item i WHERE i.isDeleted=0 AND i.hoatDong=1 ORDER BY i.tenItem,i.id";
        try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql);var rs=ps.executeQuery()){
            List<Lo> rows=new ArrayList<>();while(rs.next()){java.sql.Date expiry=rs.getDate(7);rows.add(new Lo(rs.getInt(1),rs.getInt(2),rs.getString(3),rs.getString(4),rs.getString(5),"",expiry==null?null:expiry.toLocalDate(),rs.getBigDecimal(8)));}return List.copyOf(rows);
        }catch(SQLException ex){throw new IllegalStateException("Không tải được danh mục vật tư hoạt động.",ex);}
    }
    @Override public Bang getDetails(PhanQuyenNguCanh context,int cabinetId,Phieu ticket){
        if(!isAuthorized(context,HanhDongQuyen.XEM)||!cabinetMapped(context,cabinetId))throw new IllegalStateException("Phiên hiện tại không có quyền xem tủ trực này.");
        try(var lease=connections.acquire();var ps=lease.connection().prepareStatement("SELECT maKhoa FROM dbo.TuTrucChuyenKho WHERE maPhieuXuat=? AND maCoSo=?")){
            ps.setInt(1,ticket.id());ps.setInt(2,context.facilityId());try(var rs=ps.executeQuery()){if(rs.next()&&rs.getInt(1)!=context.departmentId())throw new IllegalStateException("Phiếu không thuộc khoa/phòng của phiên hiện tại.");}
        }catch(SQLException ex){throw new IllegalStateException("Không kiểm tra được phạm vi phiếu.",ex);}
        return getDetails(context.facilityId(),cabinetId,ticket);
    }
    @Override public ChuyenKho getTransfer(PhanQuyenNguCanh context,int cabinetId,int documentId){
        if(!isAuthorized(context,HanhDongQuyen.XEM)||!cabinetMapped(context,cabinetId))throw new IllegalStateException("Phiên hiện tại không có quyền xem tủ trực này.");
        try(var lease=connections.acquire();var ps=lease.connection().prepareStatement("SELECT maKhoa FROM dbo.TuTrucChuyenKho WHERE maPhieuXuat=? AND maCoSo=?")){
            ps.setInt(1,documentId);ps.setInt(2,context.facilityId());try(var rs=ps.executeQuery()){if(!rs.next()||rs.getInt(1)!=context.departmentId())throw new IllegalStateException("Phiếu không thuộc khoa/phòng của phiên hiện tại.");}
        }catch(SQLException ex){throw new IllegalStateException("Không kiểm tra được phạm vi phiếu tủ trực.",ex);}
        return getTransfer(context.facilityId(),cabinetId,documentId);
    }
    @Override public List<DinhMucTuTruc> getPar(PhanQuyenNguCanh context,int cabinetId){
        if(!isAuthorized(context,HanhDongQuyen.XEM)||!cabinetMapped(context,cabinetId))return List.of();
        String sql="SELECT maItem,soLuongToiThieu,soLuongMucTieu,soLuongToiDa,version FROM dbo.TuTrucDinhMuc WHERE maCoSo=? AND maKhoa=? AND maKho=? AND isDeleted=0 ORDER BY maItem";
        try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){
            ps.setInt(1,context.facilityId());ps.setInt(2,context.departmentId());ps.setInt(3,cabinetId);
            List<DinhMucTuTruc> rows=new ArrayList<>();try(var rs=ps.executeQuery()){while(rs.next())rows.add(new DinhMucTuTruc(rs.getInt(1),rs.getBigDecimal(2),rs.getBigDecimal(3),rs.getBigDecimal(4),rs.getLong(5)));}
            return List.copyOf(rows);
        }catch(SQLException ex){throw new IllegalStateException("Không tải được cơ số tủ trực.",ex);}
    }
    @Override public List<NhuCauBu> getAvailableDemand(PhanQuyenNguCanh context,int cabinetId){
        if(!isAuthorized(context,HanhDongQuyen.XEM)||!cabinetMapped(context,cabinetId))return List.of();
        String sql="SELECT n.id,n.maItem,i.maItem,i.tenItem,i.donViCoBan,n.soLuongCoTheBu-n.soLuongDaClaim FROM dbo.TuTrucNhuCauBu n JOIN dbo.Item i ON i.id=n.maItem WHERE n.maCoSo=? AND n.maKhoa=? AND n.maKho=? AND n.trangThai IN ('OPEN','PARTIAL') AND n.soLuongCoTheBu>n.soLuongDaClaim AND i.isDeleted=0 AND i.hoatDong=1 ORDER BY n.id";
        try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){ps.setInt(1,context.facilityId());ps.setInt(2,context.departmentId());ps.setInt(3,cabinetId);List<NhuCauBu> rows=new ArrayList<>();try(var rs=ps.executeQuery()){while(rs.next())rows.add(new NhuCauBu(rs.getLong(1),rs.getInt(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getBigDecimal(6)));}return List.copyOf(rows);}
        catch(SQLException ex){throw new IllegalStateException("Không tải được nhu cầu bù từ tiêu hao.",ex);}
    }
    @Override public BaoCaoTon getStockMovement(PhanQuyenNguCanh context,int warehouseId,LocalDateTime from,LocalDateTime until){
        if(!isAuthorized(context,HanhDongQuyen.XEM)||!warehouseScoped(context,warehouseId))throw new IllegalStateException("Không có quyền xem sổ tồn của kho này.");
        if(from==null||until==null||!from.isBefore(until))throw new IllegalArgumentException("Khoảng báo cáo phải theo [từ,đến) và thời điểm bắt đầu nhỏ hơn kết thúc.");
        String balanceSql="SELECT i.id,i.maItem,i.tenItem,i.donViCoBan,t.soLo,t.hanSuDung,t.soLuongTon "
                +",ISNULL(SUM(CASE WHEN x.thoiGian>=? THEN x.soLuongThayDoi ELSE 0 END),0) futureDelta "
                +",ISNULL(SUM(CASE WHEN x.thoiGian>=? AND x.thoiGian<? AND x.soLuongThayDoi>0 THEN x.soLuongThayDoi ELSE 0 END),0) received "
                +",ISNULL(SUM(CASE WHEN x.thoiGian>=? AND x.thoiGian<? AND x.soLuongThayDoi<0 THEN -x.soLuongThayDoi ELSE 0 END),0) issued "
                +"FROM dbo.TonKho t JOIN dbo.Item i ON i.id=t.maItem LEFT JOIN dbo.InventoryTransaction x ON x.maTonKho=t.id AND x.isDeleted=0 "
                +"WHERE t.maKho=? AND t.isDeleted=0 AND i.isDeleted=0 GROUP BY i.id,i.maItem,i.tenItem,i.donViCoBan,t.soLo,t.hanSuDung,t.soLuongTon ORDER BY i.tenItem,t.hanSuDung,t.soLo";
        String txSql="SELECT x.thoiGian,i.id,i.maItem,i.tenItem,i.donViCoBan,t.soLo,x.loaiGiaoDich,ISNULL(x.maChungTu,''),x.soLuongThayDoi,x.tonTruocKhiThayDoi,x.tonSauKhiThayDoi FROM dbo.InventoryTransaction x JOIN dbo.Item i ON i.id=x.maItem JOIN dbo.TonKho t ON t.id=x.maTonKho WHERE x.maKho=? AND x.thoiGian>=? AND x.thoiGian<? AND x.isDeleted=0 ORDER BY x.thoiGian,x.id";
        try(var lease=connections.acquire()){
            Connection c=lease.connection();List<TonTheoLo> balances=new ArrayList<>();
            try(var ps=c.prepareStatement(balanceSql)){
                ps.setTimestamp(1,Timestamp.valueOf(from));ps.setTimestamp(2,Timestamp.valueOf(from));ps.setTimestamp(3,Timestamp.valueOf(until));ps.setTimestamp(4,Timestamp.valueOf(from));ps.setTimestamp(5,Timestamp.valueOf(until));ps.setInt(6,warehouseId);
                try(var rs=ps.executeQuery()){while(rs.next()){
                    BigDecimal opening=rs.getBigDecimal("soLuongTon").subtract(rs.getBigDecimal("futureDelta"));BigDecimal received=rs.getBigDecimal("received"),issued=rs.getBigDecimal("issued");
                    balances.add(new TonTheoLo(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getDate(6).toLocalDate(),opening,received,issued,opening.add(received).subtract(issued)));
                }}
            }
            List<GiaoDichTon> entries=new ArrayList<>();try(var ps=c.prepareStatement(txSql)){
                ps.setInt(1,warehouseId);ps.setTimestamp(2,Timestamp.valueOf(from));ps.setTimestamp(3,Timestamp.valueOf(until));try(var rs=ps.executeQuery()){while(rs.next())entries.add(new GiaoDichTon(rs.getTimestamp(1).toLocalDateTime(),rs.getInt(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getBigDecimal(9),rs.getBigDecimal(10),rs.getBigDecimal(11)));}
            }
            return new BaoCaoTon(balances,entries);
        }catch(SQLException ex){throw new IllegalStateException("Không truy vấn được sổ tồn theo kỳ.",ex);}
    }
    @Override public KetQua<DinhMucTuTruc> savePar(LuuDinhMuc command){
        if(command==null||!isAuthorized(command.context(),HanhDongQuyen.SUA))return KetQua.failure("FORBIDDEN","Phiên hiện tại không có quyền sửa cơ số tủ trực.");
        if(command.cabinetId()<=0||command.itemId()<=0||command.minimum()==null||command.target()==null||command.maximum()==null
                ||command.minimum().signum()<0||command.minimum().scale()>4||command.target().scale()>4||command.maximum().scale()>4
                ||command.target().compareTo(command.minimum())<0||command.maximum().compareTo(command.target())<0
                ||command.expectedVersion()<0)return KetQua.failure("INVALID_PAR","Cơ số cần thỏa 0 ≤ min ≤ target ≤ max; tối đa 4 số lẻ.");
        return transactions.inTransaction(()->{
            try(var lease=connections.acquire()){
                Connection c=lease.connection();
                try(var warehouse=c.prepareStatement("SELECT 1 FROM dbo.Kho k JOIN dbo.TuTrucKhoKhoa m ON m.maKho=k.id AND m.maCoSo=k.maCoSo WHERE k.id=? AND k.maCoSo=? AND m.maKhoa=? AND k.loaiKho LIKE 'TU[_]TRUC%' AND k.isDeleted=0 AND k.trangThai='HOAT_DONG'")){
                    warehouse.setInt(1,command.cabinetId());warehouse.setInt(2,command.context().facilityId());warehouse.setInt(3,command.context().departmentId());try(var rs=warehouse.executeQuery()){if(!rs.next())return KetQua.failure("SCOPE","Tủ trực chưa được gán cho khoa/phòng của phiên hiện tại.");}}
                try(var item=c.prepareStatement("SELECT 1 FROM dbo.Item WHERE id=? AND isDeleted=0 AND hoatDong=1")){
                    item.setInt(1,command.itemId());try(var rs=item.executeQuery()){if(!rs.next())return KetQua.failure("ITEM","Vật tư không còn hoạt động.");}}
                DinhMucTuTruc saved;
                if(command.expectedVersion()==0){
                    try(var ps=c.prepareStatement("INSERT INTO dbo.TuTrucDinhMuc(maCoSo,maKhoa,maKho,maItem,soLuongToiThieu,soLuongMucTieu,soLuongToiDa,createdBy) OUTPUT INSERTED.version VALUES(?,?,?,?,?,?,?,?)")){
                        ps.setInt(1,command.context().facilityId());ps.setInt(2,command.context().departmentId());ps.setInt(3,command.cabinetId());ps.setInt(4,command.itemId());ps.setBigDecimal(5,command.minimum());ps.setBigDecimal(6,command.target());ps.setBigDecimal(7,command.maximum());ps.setInt(8,command.context().employeeId());
                        try(var rs=ps.executeQuery()){if(!rs.next())throw new SQLException("Missing par version");saved=new DinhMucTuTruc(command.itemId(),command.minimum(),command.target(),command.maximum(),rs.getLong(1));}
                    }
                }else{
                    try(var ps=c.prepareStatement("UPDATE dbo.TuTrucDinhMuc SET soLuongToiThieu=?,soLuongMucTieu=?,soLuongToiDa=?,version=version+1,updatedAt=SYSUTCDATETIME(),updatedBy=? OUTPUT INSERTED.version WHERE maCoSo=? AND maKhoa=? AND maKho=? AND maItem=? AND version=? AND isDeleted=0")){
                        ps.setBigDecimal(1,command.minimum());ps.setBigDecimal(2,command.target());ps.setBigDecimal(3,command.maximum());ps.setInt(4,command.context().employeeId());ps.setInt(5,command.context().facilityId());ps.setInt(6,command.context().departmentId());ps.setInt(7,command.cabinetId());ps.setInt(8,command.itemId());ps.setLong(9,command.expectedVersion());
                        try(var rs=ps.executeQuery()){if(!rs.next())return KetQua.failure("VERSION_CONFLICT","Cơ số đã đổi hoặc chưa được cấu hình. Tải lại trước khi lưu.");saved=new DinhMucTuTruc(command.itemId(),command.minimum(),command.target(),command.maximum(),rs.getLong(1));}
                    }
                }
                audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(command.context().employeeId(),command.context().roleId(),command.context().roleCode(),command.context().facilityId(),command.context().departmentId(),KiemToanHanhDongMa.INV_CABINET_PAR_UPDATED,KiemToanTaiNguyenLoai.CABINET_PAR,command.cabinetId(),KiemToanKetQua.SUCCESS,null,Instant.now(),null,command.context().sessionId()));
                return KetQua.success(saved);
            }catch(SQLException ex){
                if(ex.getErrorCode()==2627||ex.getErrorCode()==2601)return KetQua.failure("PAR_EXISTS","Cơ số đã được tạo ở một phiên khác. Tải lại trước khi sửa.");
                throw new IllegalStateException("Không lưu được cơ số tủ trực.",ex);
            }
        });
    }
    @Override public List<KhoaTuTruc> getCabinetDepartments(PhanQuyenNguCanh context,int cabinetId){
        if(!isAuthorized(context,HanhDongQuyen.XEM)||cabinetId<=0)return List.of();
        String sql="SELECT d.id,d.tenKhoa,CASE WHEN m.id IS NULL THEN CAST(0 AS BIT) ELSE CAST(1 AS BIT) END assigned FROM dbo.Khoa d LEFT JOIN dbo.TuTrucKhoKhoa m ON m.maKhoa=d.id AND m.maKho=? AND m.maCoSo=d.maCoSo WHERE d.maCoSo=? AND d.isDeleted=0 AND d.hoatDong=1 ORDER BY d.tenKhoa,d.id";
        try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){ps.setInt(1,cabinetId);ps.setInt(2,context.facilityId());List<KhoaTuTruc> rows=new ArrayList<>();try(var rs=ps.executeQuery()){while(rs.next())rows.add(new KhoaTuTruc(rs.getInt(1),rs.getString(2),rs.getBoolean(3)));}return List.copyOf(rows);}
        catch(SQLException ex){throw new IllegalStateException("Không tải được phạm vi khoa/phòng của tủ.",ex);}
    }
    @Override public KetQua<Boolean> saveCabinetDepartments(PhanQuyenNguCanh context,int cabinetId,List<Integer> departmentIds){
        if(context==null||!isAuthorized(context,HanhDongQuyen.SUA)||!"ADMIN".equalsIgnoreCase(context.roleCode()))return KetQua.failure("FORBIDDEN","Chỉ quản trị viên có quyền gán tủ trực cho khoa/phòng.");
        List<Integer> ids=departmentIds==null?List.of():List.copyOf(departmentIds);
        if(cabinetId<=0||ids.stream().anyMatch(id->id==null||id<=0)||ids.stream().distinct().count()!=ids.size())return KetQua.failure("INVALID","Danh sách khoa/phòng không hợp lệ.");
        return transactions.inTransaction(()->{try(var lease=connections.acquire()){Connection c=lease.connection();
            try(var check=c.prepareStatement("SELECT 1 FROM dbo.Kho WHERE id=? AND maCoSo=? AND loaiKho LIKE 'TU[_]TRUC%' AND isDeleted=0 AND trangThai='HOAT_DONG'")){check.setInt(1,cabinetId);check.setInt(2,context.facilityId());try(var rs=check.executeQuery()){if(!rs.next())return KetQua.failure("SCOPE","Tủ trực không thuộc cơ sở hiện tại.");}}
            if(!ids.isEmpty()){String marks=String.join(",",Collections.nCopies(ids.size(),"?"));try(var check=c.prepareStatement("SELECT COUNT(*) FROM dbo.Khoa WHERE maCoSo=? AND isDeleted=0 AND hoatDong=1 AND id IN ("+marks+")")){check.setInt(1,context.facilityId());for(int i=0;i<ids.size();i++)check.setInt(i+2,ids.get(i));try(var rs=check.executeQuery()){rs.next();if(rs.getInt(1)!=ids.size())return KetQua.failure("SCOPE","Có khoa/phòng ngoài cơ sở hoặc đã ngừng hoạt động.");}}}
            try(var del=c.prepareStatement("DELETE FROM dbo.TuTrucKhoKhoa WHERE maCoSo=? AND maKho=?")){del.setInt(1,context.facilityId());del.setInt(2,cabinetId);del.executeUpdate();}
            try(var ins=c.prepareStatement("INSERT INTO dbo.TuTrucKhoKhoa(maCoSo,maKhoa,maKho,createdBy) VALUES(?,?,?,?)")){for(int id:ids){ins.setInt(1,context.facilityId());ins.setInt(2,id);ins.setInt(3,cabinetId);ins.setInt(4,context.employeeId());ins.addBatch();}if(!ids.isEmpty())ins.executeBatch();}
            audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(context.employeeId(),context.roleId(),context.roleCode(),context.facilityId(),context.departmentId(),KiemToanHanhDongMa.INV_CABINET_SCOPE_UPDATED,KiemToanTaiNguyenLoai.CABINET_SCOPE,cabinetId,KiemToanKetQua.SUCCESS,null,Instant.now(),null,context.sessionId()));
            return KetQua.success(Boolean.TRUE);
        }catch(SQLException ex){throw new IllegalStateException("Không lưu được phạm vi tủ trực.",ex);}});
    }
    private List<Kho> warehouses(int facilityId, boolean cabinets) {
        if (facilityId <= 0) return List.of();
        String sql = "SELECT k.id,k.tenKho FROM Kho k WHERE k.maCoSo=? AND k.isDeleted=0 "
                + "AND k.trangThai='HOAT_DONG' AND " + (cabinets ? CABINET : "NOT (" + CABINET + ")")
                + " ORDER BY k.tenKho,k.id";
        try (var lease = connections.acquire(); var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, facilityId);
            List<Kho> result = new ArrayList<>();
            try (var rs = ps.executeQuery()) { while (rs.next()) result.add(new Kho(rs.getInt(1), rs.getString(2))); }
            return List.copyOf(result);
        } catch (SQLException ex) { throw new IllegalStateException("Không tải được danh sách tủ/kho.", ex); }
    }
    private List<Kho> scopedWarehouses(String sql,int facilityId,int departmentId){
        try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){ps.setInt(1,facilityId);ps.setInt(2,departmentId);List<Kho> rows=new ArrayList<>();try(var rs=ps.executeQuery()){while(rs.next())rows.add(new Kho(rs.getInt(1),rs.getString(2)));}return List.copyOf(rows);}
        catch(SQLException ex){throw new IllegalStateException("Không tải được tủ theo khoa/phòng.",ex);}
    }
    private boolean cabinetMapped(PhanQuyenNguCanh context,int cabinetId){
        if(context==null||cabinetId<=0)return false;
        String sql="SELECT 1 FROM dbo.Kho k JOIN dbo.TuTrucKhoKhoa m ON m.maKho=k.id AND m.maCoSo=k.maCoSo WHERE k.id=? AND k.maCoSo=? AND m.maKhoa=? AND k.isDeleted=0 AND k.trangThai='HOAT_DONG' AND "+CABINET;
        try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){ps.setInt(1,cabinetId);ps.setInt(2,context.facilityId());ps.setInt(3,context.departmentId());try(var rs=ps.executeQuery()){return rs.next();}}
        catch(SQLException ex){throw new IllegalStateException("Không kiểm tra được phạm vi tủ trực.",ex);}
    }
    private boolean warehouseScoped(PhanQuyenNguCanh context,int warehouseId){
        if(context==null||warehouseId<=0)return false;
        String sql="SELECT 1 FROM dbo.Kho k WHERE k.id=? AND k.maCoSo=? AND k.isDeleted=0 AND k.trangThai='HOAT_DONG' AND (NOT("+CABINET+") OR EXISTS(SELECT 1 FROM dbo.TuTrucKhoKhoa m WHERE m.maKho=k.id AND m.maCoSo=k.maCoSo AND m.maKhoa=?))";
        try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){ps.setInt(1,warehouseId);ps.setInt(2,context.facilityId());ps.setInt(3,context.departmentId());try(var rs=ps.executeQuery()){return rs.next();}}
        catch(SQLException ex){throw new IllegalStateException("Không kiểm tra được phạm vi kho.",ex);}
    }

    @Override public List<Phieu> getTickets(int facilityId, int cabinetId, LocalDateTime from, LocalDateTime until) {
        return getTicketsScoped(facilityId,null,cabinetId,from,until);
    }
    private List<Phieu> getTicketsScoped(int facilityId,Integer departmentId,int cabinetId,LocalDateTime from,LocalDateTime until){
        if (facilityId <= 0 || cabinetId <= 0) return List.of();
        String sql = """
                SELECT p.id,'XUAT_KHO' sourceType,CASE WHEN p.isDeleted=1 THEN 'DA_HUY' ELSE p.trangThaiNoiBo END status,
                  p.createdAt,p.ngayXuat orderDate,p.soPhieuXuat code,
                  CASE WHEN p.maKhoNhan=k.id THEN N'Nhập bù' ELSE N'Xuất sử dụng' END ticketType,
                  ISNULL(lk.doiTuongKham,'') patientType,ISNULL(kh.tenKhoa,N'') department,k.tenKho location
                FROM PhieuXuatKho p JOIN Kho k ON (k.id=p.maKhoNhan OR k.id=p.maKhoXuat)
                JOIN Kho source ON source.id=p.maKhoXuat AND source.maCoSo=k.maCoSo
                LEFT JOIN DonThuoc d ON d.id=p.maDonThuoc
                LEFT JOIN LuotKham lk ON lk.id=d.maLuotKham AND lk.maCoSo=k.maCoSo
                OUTER APPLY (SELECT TOP 1 h.maKhoa FROM NhatKyHeThong h
                  WHERE h.hanhDong='INV_ISSUE_CREATED' AND h.loaiDoiTuong='INVENTORY_ISSUE'
                    AND h.maDoiTuong=CAST(p.id AS VARCHAR(50)) AND h.maCoSo=k.maCoSo
                    AND h.ketQua='SUCCESS' AND h.isDeleted=0 ORDER BY h.id) actor
                LEFT JOIN Khoa kh ON kh.id=COALESCE(actor.maKhoa,lk.maKhoa) AND kh.maCoSo=k.maCoSo
                WHERE k.maCoSo=? AND k.id=? AND k.isDeleted=0 AND %s
                  AND p.ngayXuat>=? AND p.ngayXuat<?
                  AND (? IS NULL OR NOT EXISTS(SELECT 1 FROM dbo.TuTrucChuyenKho tc WHERE tc.maPhieuXuat=p.id)
                       OR EXISTS(SELECT 1 FROM dbo.TuTrucChuyenKho tc WHERE tc.maPhieuXuat=p.id AND tc.maKhoa=?))
                UNION ALL
                SELECT p.id,'NHAP_KHO',CASE WHEN p.isDeleted=1 THEN 'DA_HUY' ELSE p.trangThai END,
                  p.createdAt,p.ngayNhap,p.soPhieuNhap,N'Nhập kho',N'',N'',k.tenKho
                FROM PhieuNhapKho p JOIN Kho k ON k.id=p.maKho
                WHERE k.maCoSo=? AND k.id=? AND k.isDeleted=0 AND %s
                  AND p.ngayNhap>=? AND p.ngayNhap<?
                ORDER BY orderDate DESC,id DESC
                """.formatted(CABINET, CABINET);
        try (var lease = connections.acquire(); var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1,facilityId);ps.setInt(2,cabinetId);ps.setTimestamp(3,Timestamp.valueOf(from));ps.setTimestamp(4,Timestamp.valueOf(until));
            if(departmentId==null){ps.setNull(5,Types.INTEGER);ps.setNull(6,Types.INTEGER);}else{ps.setInt(5,departmentId);ps.setInt(6,departmentId);}
            ps.setInt(7,facilityId);ps.setInt(8,cabinetId);ps.setTimestamp(9,Timestamp.valueOf(from));ps.setTimestamp(10,Timestamp.valueOf(until));
            List<Phieu> result = new ArrayList<>();
            try (var rs = ps.executeQuery()) { while (rs.next()) result.add(new Phieu(
                    rs.getInt("id"), rs.getString("sourceType"), rs.getString("status"), time(rs,"createdAt"),
                    time(rs,"orderDate"), rs.getString("code"), "", rs.getString("ticketType"),
                    rs.getString("patientType"), rs.getString("department"), rs.getString("location"))); }
            return List.copyOf(result);
        } catch (SQLException ex) { throw new IllegalStateException("Không tải được phiếu tủ trực.", ex); }
    }

    @Override public List<Lo> getStock(int facilityId, int warehouseId) {
        if (facilityId <= 0 || warehouseId <= 0) return List.of();
        String sql = """
                SELECT t.id,i.id,i.maItem,i.tenItem,i.donViCoBan,t.soLo,t.hanSuDung,t.soLuongTon
                FROM TonKho t JOIN Kho k ON k.id=t.maKho JOIN Item i ON i.id=t.maItem
                WHERE k.maCoSo=? AND k.id=? AND k.isDeleted=0 AND k.trangThai='HOAT_DONG'
                  AND t.isDeleted=0 AND i.isDeleted=0 AND t.soLuongTon>0
                ORDER BY t.hanSuDung,i.tenItem,t.id
                """;
        try (var lease = connections.acquire(); var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, facilityId); ps.setInt(2, warehouseId);
            List<Lo> result = new ArrayList<>();
            try (var rs = ps.executeQuery()) { while (rs.next()) result.add(new Lo(rs.getInt(1), rs.getInt(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), rs.getString(6), rs.getDate(7).toLocalDate(), rs.getBigDecimal(8))); }
            return List.copyOf(result);
        } catch (SQLException ex) { throw new IllegalStateException("Không tải được tồn vật tư theo lô.", ex); }
    }

    @Override public Bang getDetails(int facilityId, int cabinetId, Phieu ticket) {
        // Recheck membership, rather than trusting IDs received from a stale UI row.
        String table = "NHAP_KHO".equals(ticket.sourceType()) ? "PhieuNhapKho" : "PhieuXuatKho";
        String membership = "NHAP_KHO".equals(ticket.sourceType()) ? "p.maKho=k.id" : "(p.maKhoNhan=k.id OR p.maKhoXuat=k.id)";
        String sql = "SELECT p.id FROM " + table + " p JOIN Kho k ON " + membership
                + " WHERE p.id=? AND k.id=? AND k.maCoSo=? AND k.isDeleted=0 AND " + CABINET;
        try (var lease = connections.acquire(); var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1, ticket.id()); ps.setInt(2, cabinetId); ps.setInt(3, facilityId);
            try (var rs = ps.executeQuery()) { if (!rs.next()) throw new IllegalStateException("Phiếu không thuộc tủ đang chọn."); }
        } catch (SQLException ex) { throw new IllegalStateException("Không kiểm tra được phiếu tủ trực.", ex); }
        boolean transferTicket=false;
        try(var lease=connections.acquire();var ps=lease.connection().prepareStatement("SELECT 1 FROM dbo.TuTrucChuyenKho t JOIN dbo.PhieuXuatKho p ON p.id=t.maPhieuXuat WHERE p.id=? AND t.maCoSo=? AND p.maKhoNhan=?")){
            ps.setInt(1,ticket.id());ps.setInt(2,facilityId);ps.setInt(3,cabinetId);try(var rs=ps.executeQuery()){transferTicket=rs.next();}
        }catch(SQLException ex){throw new IllegalStateException("Không tải được chi tiết phiếu tủ trực.",ex);}
        if(transferTicket){
            var transfer=getTransfer(facilityId,cabinetId,ticket.id());
            List<List<Object>> rows=transfer.lines().stream().map(l->List.<Object>of(l.code(),l.name(),l.unit(),"","",l.requested(),BigDecimal.ZERO,BigDecimal.ZERO)).toList();
            return new Bang(List.of("Mã vật tư","Tên vật tư","Đơn vị","Số lô","Hạn dùng","Số lượng yêu cầu","Đơn giá","Thành tiền"),rows);
        }
        var lines = new NhaThuocDanhSachRepository(connections).getTicketDetails(ticket.sourceType(), ticket.id(), facilityId);
        return new Bang(List.of("Mã vật tư", "Tên vật tư", "Đơn vị", "Số lô", "Hạn dùng", "Số lượng", "Đơn giá", "Thành tiền"),
                lines.stream().map(l -> List.<Object>of(l.itemCode(),l.itemName(),l.unit(),l.batch(),
                        l.expiry()==null?"":l.expiry(),l.quantity(),l.price(),l.amount())).toList());
    }

    @Override public KetQua<String> requestReplenishment(YeuCau request) {
        var context = request == null ? null : request.context();
        if (!isAuthorized(context, HanhDongQuyen.TAO))
            return KetQua.failure("FORBIDDEN", "Phiên hiện tại không có quyền lập phiếu nhập bù.");
        if (request.cabinetId() <= 0 || request.sourceWarehouseId() <= 0 || request.lines().isEmpty()
                || request.operationKey() == null
                || request.lines().stream().anyMatch(l -> l == null || l.itemId() <= 0 || l.quantity() == null
                    || l.quantity().signum() <= 0 || l.quantity().scale() > 4 || l.quantity().precision() > 18)
                || request.lines().stream().anyMatch(l->l.claims().stream().anyMatch(cl->cl==null||cl.demandId()<=0||cl.quantity()==null||cl.quantity().signum()<=0||cl.quantity().scale()>4)
                    ||l.claims().stream().map(ClaimNhuCau::demandId).distinct().count()!=l.claims().size()
                    ||l.claims().stream().map(ClaimNhuCau::quantity).reduce(BigDecimal.ZERO,BigDecimal::add).compareTo(l.quantity())>0)
                || request.lines().stream().map(DongYeuCau::itemId).distinct().count() != request.lines().size()
                || (request.note() != null && request.note().length() > 400))
            return KetQua.failure("INVALID", "Kiểm tra kho cấp, tủ nhận, số lượng và ghi chú (tối đa 400 ký tự).");
        return transactions.inTransaction(() -> {
            var cabinets = getCabinets(context.facilityId());
            var supply = getSupplyWarehouses(context.facilityId());
            if (!cabinetMapped(context,request.cabinetId())
                    || supply.stream().noneMatch(k -> k.id()==request.sourceWarehouseId()))
                return KetQua.failure("SCOPE", "Kho cấp hoặc tủ nhận không thuộc cơ sở hiện tại.");
            String code = "NB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
            try (var lease = connections.acquire()) {
                var connection = lease.connection();
                String payload=requestPayload(request);
                try (var ps=connection.prepareStatement("SELECT p.maPhieuXuat,p.payloadHash,t.maCoSo,t.maKhoa,x.maKhoXuat,x.maKhoNhan FROM TuTrucChuyenKhoThaoTac p JOIN dbo.TuTrucChuyenKho t ON t.maPhieuXuat=p.maPhieuXuat JOIN dbo.PhieuXuatKho x ON x.id=p.maPhieuXuat WHERE p.operationKey=? AND p.hanhDong='CREATE'")) {
                    ps.setObject(1,request.operationKey());
                    try (var rs=ps.executeQuery()) { if (rs.next()) {
                        if(!Objects.equals(rs.getString(2),payload)||rs.getInt(3)!=context.facilityId()||rs.getInt(4)!=context.departmentId()
                                ||rs.getInt(5)!=request.sourceWarehouseId()||rs.getInt(6)!=request.cabinetId())return KetQua.failure("IDEMPOTENCY_CONFLICT","Mã thao tác đã dùng cho nội dung khác. Tải lại danh sách phiếu.");
                        if(!sameRequestedLines(connection,rs.getInt(1),request.lines()))return KetQua.failure("IDEMPOTENCY_CONFLICT","Mã thao tác đã dùng cho nội dung khác. Tải lại danh sách phiếu.");
                        return KetQua.success(ticketCode(connection,rs.getInt(1))); }
                    }
                }
                for (var input : request.lines()) try (var ps=connection.prepareStatement(
                        "SELECT i.id FROM Item i WHERE i.id=? AND i.isDeleted=0 AND i.hoatDong=1")) {
                    ps.setInt(1,input.itemId());
                    try (var rs=ps.executeQuery()) { if (!rs.next()) return KetQua.failure("ITEM", "Vật tư không còn hoạt động."); }
                }
                int id;
                String insert = "INSERT INTO PhieuXuatKho(soPhieuXuat,maKhoXuat,maKhoNhan,loaiXuat,trangThaiNoiBo,tongTien,ghiChu,createdBy) "
                        + "OUTPUT INSERTED.id VALUES(?,?,?,'CHUYEN_KHO','CHO_DUYET',0,?,?)";
                try (var ps=connection.prepareStatement(insert)) {
                    ps.setString(1,code); ps.setInt(2,request.sourceWarehouseId()); ps.setInt(3,request.cabinetId());
                    ps.setString(4,"Nhập bù tủ trực" + (request.note()==null || request.note().isBlank()?"":"; "+request.note().trim()));
                    ps.setInt(5,context.employeeId());
                    try (var rs=ps.executeQuery()) { if (!rs.next()) throw new SQLException("Missing inserted ID"); id=rs.getInt(1); }
                }
                try (var ps=connection.prepareStatement("INSERT INTO TuTrucChuyenKho(maPhieuXuat,maCoSo,maKhoa,trangThai,lyDo,createdBy) VALUES(?,?,?,'REQUESTED',?,?)")) {
                    ps.setInt(1,id); ps.setInt(2,context.facilityId()); ps.setInt(3,context.departmentId());
                    ps.setString(4,request.note()); ps.setInt(5,context.employeeId()); ps.executeUpdate();
                }
                for (var line:request.lines()) {
                    int transferLineId;
                    try(var ps=connection.prepareStatement("INSERT INTO TuTrucChuyenKhoDong(maPhieuXuat,maItem,soLuongYeuCau,createdAt) OUTPUT INSERTED.id VALUES(?,?,?,SYSUTCDATETIME())")){
                        ps.setInt(1,id);ps.setInt(2,line.itemId());ps.setBigDecimal(3,line.quantity());try(var rs=ps.executeQuery()){if(!rs.next())throw new SQLException("Missing transfer line ID");transferLineId=rs.getInt(1);}}
                    for(var claim:line.claims()){
                        try(var up=connection.prepareStatement("UPDATE dbo.TuTrucNhuCauBu SET soLuongDaClaim=soLuongDaClaim+?,trangThai=CASE WHEN soLuongDaClaim+?=soLuongCoTheBu THEN 'CLAIMED' ELSE 'PARTIAL' END,version=version+1 WHERE id=? AND maCoSo=? AND maKhoa=? AND maKho=? AND maItem=? AND soLuongCoTheBu-soLuongDaClaim>=? AND trangThai IN ('OPEN','PARTIAL')")){
                            up.setBigDecimal(1,claim.quantity());up.setBigDecimal(2,claim.quantity());up.setLong(3,claim.demandId());up.setInt(4,context.facilityId());up.setInt(5,context.departmentId());up.setInt(6,request.cabinetId());up.setInt(7,line.itemId());up.setBigDecimal(8,claim.quantity());
                            if(up.executeUpdate()!=1)return KetQua.failure("DEMAND_CONFLICT","Nhu cầu tiêu hao đã được bù hoặc đổi ở phiên khác. Tải lại trước khi lập phiếu.");}
                        try(var link=connection.prepareStatement("INSERT INTO dbo.TuTrucNhuCauBuClaim(maNhuCauBu,maDongChuyenKho,soLuongClaim,createdBy) VALUES(?,?,?,?)")){
                            link.setLong(1,claim.demandId());link.setInt(2,transferLineId);link.setBigDecimal(3,claim.quantity());link.setInt(4,context.employeeId());link.executeUpdate();}
                    }
                }
                recordOperation(connection,id,request.operationKey(),"CREATE",context.employeeId(),payload);
                audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(context.employeeId(),context.roleId(),context.roleCode(),
                        context.facilityId(),context.departmentId(),KiemToanHanhDongMa.INV_ISSUE_CREATED,KiemToanTaiNguyenLoai.INVENTORY_ISSUE,
                        id,KiemToanKetQua.SUCCESS,null,Instant.now(),null,context.sessionId()));
                return KetQua.success(code);
            } catch (SQLException ex) { throw new IllegalStateException("Không lưu được yêu cầu nhập bù.",ex); }
        });
    }

    @Override public ChuyenKho getTransfer(int facilityId,int cabinetId,int documentId) {
        if(facilityId<=0||cabinetId<=0||documentId<=0) throw new IllegalArgumentException("Phạm vi phiếu không hợp lệ.");
        try(var lease=connections.acquire()){return loadTransfer(lease.connection(),facilityId,cabinetId,documentId);}
        catch(SQLException ex){throw new IllegalStateException("Không tải được chi tiết phiếu tủ trực.",ex);}
    }

    @Override public KetQua<ChuyenKho> approve(LenhDuyet command) {
        ThaoTac op=command==null?null:command.thaoTac();
        if(op==null||!isAuthorized(op.context(),HanhDongQuyen.DUYET)) return KetQua.failure("FORBIDDEN","Phiên hiện tại không có quyền duyệt phiếu.");
        if(command.lines().isEmpty()||command.lines().stream().anyMatch(Objects::isNull)||op.documentId()<=0||op.operationKey()==null||op.expectedVersion()<0
                ||command.lines().stream().map(CapDuyet::lineId).distinct().count()!=command.lines().size())
            return KetQua.failure("INVALID","Dữ liệu duyệt phiếu không hợp lệ.");
        return transactions.inTransaction(()->{
            try(var lease=connections.acquire()){
                Connection c=lease.connection();
                String payload=command.lines().stream().sorted(Comparator.comparingInt(CapDuyet::lineId)).map(l->l.lineId()+"="+l.quantity()).collect(java.util.stream.Collectors.joining(";"));
                ChuyenKho replay=replay(c,op,"APPROVE",payload); if(replay!=null)return KetQua.success(replay);
                Header h=lockHeader(c,op); if(h==null)return KetQua.failure("NOT_FOUND","Không tìm thấy phiếu trong phạm vi hiện tại.");
                if(h.createdBy()==op.context().employeeId())return KetQua.failure("SEPARATION_OF_DUTIES","Người lập không được tự duyệt phiếu.");
                if(!Set.of("REQUESTED","PARTIALLY_APPROVED").contains(h.status()))return KetQua.failure("STATE","Phiếu không ở trạng thái chờ duyệt.");
                for(var line:command.lines()){
                    if(line==null||line.lineId()<=0||line.quantity()==null||line.quantity().signum()<0||line.quantity().scale()>4)
                        return KetQua.failure("INVALID_LINE","Số lượng duyệt không hợp lệ.");
                    try(var ps=c.prepareStatement("UPDATE dbo.TuTrucChuyenKhoDong SET soLuongDuyet=?,version=version+1 WHERE id=? AND maPhieuXuat=? AND ?<=soLuongYeuCau AND soLuongXuat=0")){
                        ps.setBigDecimal(1,line.quantity());ps.setInt(2,line.lineId());ps.setInt(3,op.documentId());ps.setBigDecimal(4,line.quantity());
                        if(ps.executeUpdate()!=1)return KetQua.failure("LINE_CONFLICT","Dòng đã thay đổi hoặc lượng duyệt vượt yêu cầu.");
                    }
                }
                String status=approvedStatus(c,op.documentId());
                try(var ps=c.prepareStatement("UPDATE dbo.TuTrucChuyenKho SET trangThai=?,approvedAt=SYSUTCDATETIME(),approvedBy=?,version=version+1 WHERE maPhieuXuat=? AND version=?")){
                    ps.setString(1,status);ps.setInt(2,op.context().employeeId());ps.setInt(3,op.documentId());ps.setLong(4,op.expectedVersion());
                    if(ps.executeUpdate()!=1)return KetQua.failure("VERSION_CONFLICT","Phiếu đã được cập nhật bởi người khác. Hãy tải lại.");
                }
                updateTicketStatus(c,op.documentId(),status);
                recordOperation(c,op.documentId(),op.operationKey(),"APPROVE",op.context().employeeId(),payload);
                auditAction(op.context(),op.documentId(),KiemToanHanhDongMa.INV_TRANSFER_APPROVED);
                return KetQua.success(loadTransfer(c,op.context().facilityId(),cabinetFor(c,op.documentId()),op.documentId()));
            }catch(SQLException ex){throw new IllegalStateException("Không duyệt được phiếu tủ trực.",ex);}
        });
    }

    @Override public KetQua<ChuyenKho> issue(LenhXuat command) {
        ThaoTac op=command==null?null:command.thaoTac();
        if(op==null||!isAuthorized(op.context(),HanhDongQuyen.XUAT_KHO))return KetQua.failure("FORBIDDEN","Phiên hiện tại không có quyền xuất kho.");
        if(op.documentId()<=0||op.expectedVersion()<0||op.operationKey()==null||!command.allocations().isEmpty())
            return KetQua.failure("INVALID","Lệnh xuất phải dùng phân bổ FEFO và có dữ liệu hợp lệ.");
        return transactions.inTransaction(()->{
            try(var lease=connections.acquire()){
                Connection c=lease.connection();ChuyenKho replay=replay(c,op,"ISSUE","FEFO");if(replay!=null)return KetQua.success(replay);
                Header h=lockHeader(c,op);if(h==null)return KetQua.failure("NOT_FOUND","Không tìm thấy phiếu trong phạm vi hiện tại.");
                if(h.version()!=op.expectedVersion())return KetQua.failure("VERSION_CONFLICT","Phiếu đã thay đổi. Hãy tải lại.");
                if(h.version()!=op.expectedVersion())return KetQua.failure("VERSION_CONFLICT","Phiếu đã thay đổi. Hãy tải lại.");
                if(!Set.of("APPROVED","PARTIALLY_ISSUED").contains(h.status()))return KetQua.failure("STATE","Phiếu chưa duyệt hoặc không thể xuất ở trạng thái hiện tại.");
                String code=ticketCode(c,op.documentId());BigDecimal posted=BigDecimal.ZERO;
                List<RequestedLine> requestLines=new ArrayList<>();
                try(var lp=c.prepareStatement("SELECT id,maItem,soLuongDuyet,soLuongXuat FROM dbo.TuTrucChuyenKhoDong WITH (UPDLOCK,HOLDLOCK) WHERE maPhieuXuat=? ORDER BY id")){
                    lp.setInt(1,op.documentId());try(var lr=lp.executeQuery()){while(lr.next())requestLines.add(new RequestedLine(lr.getInt(1),lr.getInt(2),lr.getBigDecimal(3),lr.getBigDecimal(4)));}
                }
                for(RequestedLine line:requestLines){
                    int lineId=line.id(),itemId=line.itemId();BigDecimal remaining=line.approved().subtract(line.issued());
                    if(remaining.signum()<=0)continue;
                    List<StockBatch> batches=new ArrayList<>();
                    try(var ps=c.prepareStatement("SELECT t.id,t.soLuongTon,t.giaNhap FROM dbo.TonKho t WITH (UPDLOCK,ROWLOCK) JOIN dbo.Item i ON i.id=t.maItem WHERE t.maKho=? AND t.maItem=? AND t.isDeleted=0 AND i.isDeleted=0 AND i.hoatDong=1 AND t.hanSuDung>=CAST(GETDATE() AS DATE) AND t.soLuongTon>0 ORDER BY t.hanSuDung,t.id")){
                        ps.setInt(1,h.source());ps.setInt(2,itemId);try(var rs=ps.executeQuery()){while(rs.next())batches.add(new StockBatch(rs.getInt(1),rs.getBigDecimal(2),rs.getBigDecimal(3)));}
                    }
                    for(StockBatch batch:batches){if(remaining.signum()<=0)break;int lotId=batch.id();BigDecimal before=batch.quantity(),take=before.min(remaining),after=before.subtract(take),price=batch.price();
                        try(var up=c.prepareStatement("UPDATE dbo.TonKho SET soLuongTon=?,version=version+1,updatedAt=SYSUTCDATETIME(),updatedBy=? WHERE id=? AND soLuongTon>=? AND isDeleted=0")){
                            up.setBigDecimal(1,after);up.setInt(2,op.context().employeeId());up.setInt(3,lotId);up.setBigDecimal(4,take);if(up.executeUpdate()!=1)throw new SQLException("Stock changed during transfer issue");}
                        try(var dp=c.prepareStatement("INSERT INTO dbo.ChiTietXuatKho(maPhieuXuat,maItem,maTonKho,soLuong,donGia,createdBy) VALUES(?,?,?,?,?,?)")){
                            dp.setInt(1,op.documentId());dp.setInt(2,itemId);dp.setInt(3,lotId);dp.setBigDecimal(4,take);dp.setBigDecimal(5,price);dp.setInt(6,op.context().employeeId());dp.executeUpdate();}
                        try(var tx=c.prepareStatement("INSERT INTO dbo.InventoryTransaction(maKho,maItem,maTonKho,loaiGiaoDich,soLuongThayDoi,tonTruocKhiThayDoi,tonSauKhiThayDoi,maChungTu,createdBy) VALUES(?,?,?,'TU_TRUC_XUAT',?,?,?,?,?)")){
                            tx.setInt(1,h.source());tx.setInt(2,itemId);tx.setInt(3,lotId);tx.setBigDecimal(4,take.negate());tx.setBigDecimal(5,before);tx.setBigDecimal(6,after);tx.setString(7,code);tx.setInt(8,op.context().employeeId());tx.executeUpdate();}
                        try(var al=c.prepareStatement("INSERT INTO dbo.TuTrucChuyenKhoLo(maDong,maTonKhoXuat,soLuongXuat) VALUES(?,?,?)")){
                            al.setInt(1,lineId);al.setInt(2,lotId);al.setBigDecimal(3,take);al.executeUpdate();}
                        remaining=remaining.subtract(take);posted=posted.add(take);
                    }
                    try(var up=c.prepareStatement("UPDATE dbo.TuTrucChuyenKhoDong SET soLuongXuat=?,version=version+1 WHERE id=?")){
                        up.setBigDecimal(1,postedForLine(c,lineId));up.setInt(2,lineId);up.executeUpdate();}
                }
                if(posted.signum()==0)return KetQua.failure("STOCK","Kho cấp hiện không còn tồn khả dụng của các vật tư đã duyệt.");
                String status=issuedStatus(c,op.documentId());
                try(var up=c.prepareStatement("UPDATE dbo.TuTrucChuyenKho SET trangThai=?,issuedAt=SYSUTCDATETIME(),issuedBy=?,version=version+1 WHERE maPhieuXuat=? AND version=?")){
                    up.setString(1,status);up.setInt(2,op.context().employeeId());up.setInt(3,op.documentId());up.setLong(4,op.expectedVersion());if(up.executeUpdate()!=1)return KetQua.failure("VERSION_CONFLICT","Phiếu đã thay đổi. Hãy tải lại.");}
                updateTicketStatus(c,op.documentId(),status);recordOperation(c,op.documentId(),op.operationKey(),"ISSUE",op.context().employeeId(),"FEFO");
                auditAction(op.context(),op.documentId(),KiemToanHanhDongMa.INV_TRANSFER_ISSUED);
                return KetQua.success(loadTransfer(c,op.context().facilityId(),h.destination(),op.documentId()));
            }catch(SQLException ex){throw new IllegalStateException("Không xuất được phiếu tủ trực.",ex);}
        });
    }
    private record RequestedLine(int id,int itemId,BigDecimal approved,BigDecimal issued){}
    private record StockBatch(int id,BigDecimal quantity,BigDecimal price){}
    private static BigDecimal postedForLine(Connection c,int lineId)throws SQLException{
        try(var ps=c.prepareStatement("SELECT ISNULL(SUM(soLuongXuat),0) FROM dbo.TuTrucChuyenKhoLo WHERE maDong=?")){ps.setInt(1,lineId);try(var rs=ps.executeQuery()){rs.next();return rs.getBigDecimal(1);}}
    }
    private static String issuedStatus(Connection c,int id)throws SQLException{
        try(var ps=c.prepareStatement("SELECT CASE WHEN SUM(CASE WHEN soLuongXuat<soLuongDuyet THEN 1 ELSE 0 END)=0 THEN 'ISSUED' ELSE 'PARTIALLY_ISSUED' END FROM dbo.TuTrucChuyenKhoDong WHERE maPhieuXuat=? AND soLuongDuyet>0")){ps.setInt(1,id);try(var rs=ps.executeQuery()){rs.next();return rs.getString(1);}}
    }

    @Override public KetQua<ChuyenKho> receive(LenhNhan command){
        ThaoTac op=command==null?null:command.thaoTac();
        if(op==null||!isAuthorized(op.context(),HanhDongQuyen.NHAN_KHO))return KetQua.failure("FORBIDDEN","Phiên hiện tại không có quyền nhận vật tư vào tủ.");
        if(op.documentId()<=0||op.expectedVersion()<0||op.operationKey()==null||command.allocations().isEmpty()
                ||command.allocations().stream().anyMatch(Objects::isNull)
                ||command.allocations().stream().map(PhanBoNhan::allocationId).distinct().count()!=command.allocations().size())
            return KetQua.failure("INVALID","Dữ liệu nhận phiếu không hợp lệ.");
        return transactions.inTransaction(()->{
            try(var lease=connections.acquire()){
                String payload=command.allocations().stream().sorted(Comparator.comparingInt(PhanBoNhan::allocationId)).map(a->a.allocationId()+"="+a.quantity()+"="+Objects.toString(a.reason(),"")).collect(java.util.stream.Collectors.joining(";"));
                Connection c=lease.connection();ChuyenKho replay=replay(c,op,"RECEIVE",payload);if(replay!=null)return KetQua.success(replay);
                Header h=lockHeader(c,op);if(h==null)return KetQua.failure("NOT_FOUND","Không tìm thấy phiếu trong phạm vi hiện tại.");
                if(h.version()!=op.expectedVersion())return KetQua.failure("VERSION_CONFLICT","Phiếu đã thay đổi. Hãy tải lại.");
                if(!Set.of("ISSUED","PARTIALLY_ISSUED","PARTIALLY_RECEIVED").contains(h.status()))return KetQua.failure("STATE","Phiếu chưa xuất hoặc không thể nhận ở trạng thái hiện tại.");
                long operationId=recordOperation(c,op.documentId(),op.operationKey(),"RECEIVE",op.context().employeeId(),payload);
                for(var receipt:command.allocations()){
                    if(receipt==null||receipt.allocationId()<=0||receipt.quantity()==null||receipt.quantity().signum()<0||receipt.quantity().scale()>4
                            ||(receipt.quantity().signum()<1&&(receipt.reason()==null||receipt.reason().isBlank()))
                            ||(receipt.reason()!=null&&receipt.reason().length()>500))return KetQua.failure("INVALID_LINE","Số lượng nhận hoặc lý do chênh lệch không hợp lệ.");
                    Allocation a=loadAllocation(c,op.documentId(),receipt.allocationId());if(a==null)return KetQua.failure("LINE_CONFLICT","Dòng phân bổ không thuộc phiếu.");
                    BigDecimal remaining=a.issued().subtract(a.received());if(receipt.quantity().compareTo(remaining)>0)return KetQua.failure("OVER_RECEIVE","Số lượng nhận vượt số đã xuất còn lại.");
                    if(receipt.quantity().compareTo(remaining)<0&&(receipt.reason()==null||receipt.reason().isBlank()))return KetQua.failure("REASON_REQUIRED","Nhận thiếu phải có lý do.");
                    if(receipt.quantity().signum()==0){
                        try(var up=c.prepareStatement("UPDATE dbo.TuTrucChuyenKhoLo SET lyDoChenhLech=? WHERE id=?")){up.setString(1,receipt.reason());up.setInt(2,a.id());up.executeUpdate();}
                        try(var event=c.prepareStatement("INSERT INTO dbo.TuTrucChuyenKhoNhan(maThaoTac,maPhanBo,soLuongNhan,lyDoChenhLech,createdBy) VALUES(?,?,0,?,?)")){
                            event.setLong(1,operationId);event.setInt(2,a.id());event.setString(3,receipt.reason());event.setInt(4,op.context().employeeId());event.executeUpdate();}
                        continue;
                    }
                    LotData lot=loadLot(c,a.sourceLotId());if(lot==null||lot.expiry().isBefore(LocalDate.now()))return KetQua.failure("EXPIRED_LOT","Không thể nhập tủ lô đã hết hạn.");
                    int targetLot=addCabinetStock(c,h.destination(),lot,receipt.quantity(),op.context().employeeId(),ticketCode(c,op.documentId()));
                    BigDecimal received=a.received().add(receipt.quantity());
                    try(var up=c.prepareStatement("UPDATE dbo.TuTrucChuyenKhoLo SET soLuongNhan=?,maTonKhoNhan=?,lyDoChenhLech=? WHERE id=? AND soLuongNhan=?")){
                        up.setBigDecimal(1,received);up.setInt(2,targetLot);up.setString(3,receipt.reason());up.setInt(4,a.id());up.setBigDecimal(5,a.received());if(up.executeUpdate()!=1)return KetQua.failure("LINE_CONFLICT","Phân bổ đã được nhận bởi người khác.");}
                    try(var event=c.prepareStatement("INSERT INTO dbo.TuTrucChuyenKhoNhan(maThaoTac,maPhanBo,soLuongNhan,lyDoChenhLech,createdBy) VALUES(?,?,?,?,?)")){
                        event.setLong(1,operationId);event.setInt(2,a.id());event.setBigDecimal(3,receipt.quantity());event.setString(4,receipt.reason());event.setInt(5,op.context().employeeId());event.executeUpdate();}
                }
                refreshReceivedQuantities(c,op.documentId());String status=receivedStatus(c,op.documentId());
                try(var up=c.prepareStatement("UPDATE dbo.TuTrucChuyenKho SET trangThai=?,receivedAt=SYSUTCDATETIME(),receivedBy=?,version=version+1 WHERE maPhieuXuat=? AND version=?")){
                    up.setString(1,status);up.setInt(2,op.context().employeeId());up.setInt(3,op.documentId());up.setLong(4,op.expectedVersion());if(up.executeUpdate()!=1)return KetQua.failure("VERSION_CONFLICT","Phiếu đã thay đổi. Hãy tải lại.");}
                updateTicketStatus(c,op.documentId(),status);
                auditAction(op.context(),op.documentId(),KiemToanHanhDongMa.INV_TRANSFER_RECEIVED);
                return KetQua.success(loadTransfer(c,op.context().facilityId(),h.destination(),op.documentId()));
            }catch(SQLException ex){throw new IllegalStateException("Không ghi nhận được nhận hàng tủ trực.",ex);}
        });
    }
    private record Allocation(int id,int lineId,int sourceLotId,BigDecimal issued,BigDecimal received){}
    private record LotData(int itemId,String batch,LocalDate expiry,BigDecimal price){}
    private static Allocation loadAllocation(Connection c,int doc,int id)throws SQLException{
        try(var ps=c.prepareStatement("SELECT a.id,a.maDong,a.maTonKhoXuat,a.soLuongXuat,a.soLuongNhan FROM dbo.TuTrucChuyenKhoLo a WITH (UPDLOCK,HOLDLOCK) JOIN dbo.TuTrucChuyenKhoDong d ON d.id=a.maDong WHERE a.id=? AND d.maPhieuXuat=?")){
            ps.setInt(1,id);ps.setInt(2,doc);try(var rs=ps.executeQuery()){return rs.next()?new Allocation(rs.getInt(1),rs.getInt(2),rs.getInt(3),rs.getBigDecimal(4),rs.getBigDecimal(5)):null;}}
    }
    private static LotData loadLot(Connection c,int id)throws SQLException{
        try(var ps=c.prepareStatement("SELECT maItem,soLo,hanSuDung,giaNhap FROM dbo.TonKho WHERE id=? AND isDeleted=0")){ps.setInt(1,id);try(var rs=ps.executeQuery()){return rs.next()?new LotData(rs.getInt(1),rs.getString(2),rs.getDate(3).toLocalDate(),rs.getBigDecimal(4)):null;}}
    }
    private static int addCabinetStock(Connection c,int cabinet,LotData lot,BigDecimal quantity,int actor,String code)throws SQLException{
        int id=0;BigDecimal before=BigDecimal.ZERO;
        try(var ps=c.prepareStatement("SELECT TOP 1 id,soLuongTon FROM dbo.TonKho WITH (UPDLOCK,HOLDLOCK) WHERE maKho=? AND maItem=? AND soLo=? AND hanSuDung=? AND giaNhap=? AND isDeleted=0 ORDER BY id")){
            ps.setInt(1,cabinet);ps.setInt(2,lot.itemId());ps.setString(3,lot.batch());ps.setDate(4,java.sql.Date.valueOf(lot.expiry()));ps.setBigDecimal(5,lot.price());try(var rs=ps.executeQuery()){if(rs.next()){id=rs.getInt(1);before=rs.getBigDecimal(2);}}}
        BigDecimal after=before.add(quantity);
        if(id>0){try(var up=c.prepareStatement("UPDATE dbo.TonKho SET soLuongTon=?,version=version+1,updatedAt=SYSUTCDATETIME(),updatedBy=? WHERE id=?")){up.setBigDecimal(1,after);up.setInt(2,actor);up.setInt(3,id);up.executeUpdate();}}
        else try(var ins=c.prepareStatement("INSERT INTO dbo.TonKho(maKho,maItem,soLo,hanSuDung,soLuongTon,giaNhap,giaBan,createdBy) OUTPUT INSERTED.id VALUES(?,?,?,?,?,?,0,?)")){
            ins.setInt(1,cabinet);ins.setInt(2,lot.itemId());ins.setString(3,lot.batch());ins.setDate(4,java.sql.Date.valueOf(lot.expiry()));ins.setBigDecimal(5,quantity);ins.setBigDecimal(6,lot.price());ins.setInt(7,actor);try(var rs=ins.executeQuery()){rs.next();id=rs.getInt(1);}}
        try(var tx=c.prepareStatement("INSERT INTO dbo.InventoryTransaction(maKho,maItem,maTonKho,loaiGiaoDich,soLuongThayDoi,tonTruocKhiThayDoi,tonSauKhiThayDoi,maChungTu,createdBy) VALUES(?,?,?,'TU_TRUC_NHAN',?,?,?,?,?)")){
            tx.setInt(1,cabinet);tx.setInt(2,lot.itemId());tx.setInt(3,id);tx.setBigDecimal(4,quantity);tx.setBigDecimal(5,before);tx.setBigDecimal(6,after);tx.setString(7,code);tx.setInt(8,actor);tx.executeUpdate();}
        return id;
    }
    private static void refreshReceivedQuantities(Connection c,int doc)throws SQLException{
        try(var ps=c.prepareStatement("UPDATE d SET soLuongNhan=x.received,version=d.version+1 FROM dbo.TuTrucChuyenKhoDong d CROSS APPLY(SELECT ISNULL(SUM(a.soLuongNhan),0) received FROM dbo.TuTrucChuyenKhoLo a WHERE a.maDong=d.id) x WHERE d.maPhieuXuat=?")){ps.setInt(1,doc);ps.executeUpdate();}
    }
    private static String receivedStatus(Connection c,int doc)throws SQLException{
        try(var ps=c.prepareStatement("SELECT CASE WHEN SUM(CASE WHEN soLuongNhan<soLuongXuat THEN 1 ELSE 0 END)=0 THEN 'RECEIVED' ELSE 'PARTIALLY_RECEIVED' END FROM dbo.TuTrucChuyenKhoDong WHERE maPhieuXuat=? AND soLuongXuat>0")){ps.setInt(1,doc);try(var rs=ps.executeQuery()){rs.next();return rs.getString(1);}}
    }

    @Override public KetQua<ChuyenKho> cancel(ThaoTac op){
        if(op==null||!isAuthorized(op.context(),HanhDongQuyen.SUA))return KetQua.failure("FORBIDDEN","Phiên hiện tại không có quyền hủy yêu cầu.");
        if(op.documentId()<=0||op.expectedVersion()<0||op.operationKey()==null)return KetQua.failure("INVALID","Lệnh hủy không hợp lệ.");
        return transactions.inTransaction(()->{
            try(var lease=connections.acquire()){
                Connection c=lease.connection();ChuyenKho replay=replay(c,op,"CANCEL",Objects.toString(op.reason(),""));if(replay!=null)return KetQua.success(replay);
                Header h=lockHeader(c,op);if(h==null)return KetQua.failure("NOT_FOUND","Không tìm thấy phiếu trong phạm vi hiện tại.");
                if(h.version()!=op.expectedVersion())return KetQua.failure("VERSION_CONFLICT","Phiếu đã thay đổi. Hãy tải lại.");
                if(!Set.of("REQUESTED","PARTIALLY_APPROVED","APPROVED").contains(h.status()))return KetQua.failure("STATE","Phiếu đã xuất không thể hủy; phải lập giao dịch đảo.");
                try(var up=c.prepareStatement("UPDATE dbo.TuTrucChuyenKho SET trangThai='CANCELLED',version=version+1 WHERE maPhieuXuat=? AND version=?")){
                    up.setInt(1,op.documentId());up.setLong(2,op.expectedVersion());if(up.executeUpdate()!=1)return KetQua.failure("VERSION_CONFLICT","Phiếu đã thay đổi. Hãy tải lại.");}
                updateTicketStatus(c,op.documentId(),"CANCELLED");recordOperation(c,op.documentId(),op.operationKey(),"CANCEL",op.context().employeeId(),Objects.toString(op.reason(),""));
                auditAction(op.context(),op.documentId(),KiemToanHanhDongMa.INV_TRANSFER_CANCELLED);
                return KetQua.success(loadTransfer(c,op.context().facilityId(),h.destination(),op.documentId()));
            }catch(SQLException ex){throw new IllegalStateException("Không hủy được phiếu tủ trực.",ex);}
        });
    }

    @Override public KetQua<ChuyenKho> reverse(ThaoTac op){
        if(op==null||!isAuthorized(op.context(),HanhDongQuyen.DAO_GIAO_DICH))return KetQua.failure("FORBIDDEN","Phiên hiện tại không có quyền đảo giao dịch.");
        if(op.documentId()<=0||op.expectedVersion()<0||op.operationKey()==null||op.reason()==null||op.reason().isBlank()||op.reason().length()>500)
            return KetQua.failure("INVALID","Lệnh đảo cần mã phiếu, phiên bản và lý do.");
        return transactions.inTransaction(()->{
            try(var lease=connections.acquire()){
                Connection c=lease.connection();ChuyenKho replay=replay(c,op,"REVERSE",op.reason());if(replay!=null)return KetQua.success(replay);
                Header h=lockHeader(c,op);if(h==null)return KetQua.failure("NOT_FOUND","Không tìm thấy phiếu trong phạm vi hiện tại.");
                if(h.version()!=op.expectedVersion())return KetQua.failure("VERSION_CONFLICT","Phiếu đã thay đổi. Hãy tải lại.");
                if(!Set.of("RECEIVED").contains(h.status()))return KetQua.failure("STATE","Chỉ đảo phiếu đã nhận đủ; phiếu đang chuyển phải được xử lý nhận trước.");
                String reversalCode="REV-"+ticketCode(c,op.documentId());
                List<ReverseAllocation> posted=new ArrayList<>();
                try(var lp=c.prepareStatement("SELECT a.id,a.maTonKhoXuat,a.maTonKhoNhan,a.soLuongNhan,d.maItem FROM dbo.TuTrucChuyenKhoLo a WITH (UPDLOCK,HOLDLOCK) JOIN dbo.TuTrucChuyenKhoDong d ON d.id=a.maDong WHERE d.maPhieuXuat=? AND a.soLuongNhan>0 ORDER BY a.id")){
                    lp.setInt(1,op.documentId());try(var lr=lp.executeQuery()){while(lr.next())posted.add(new ReverseAllocation(lr.getInt(2),lr.getInt(3),lr.getBigDecimal(4),lr.getInt(5)));}
                }
                for(ReverseAllocation allocation:posted){
                    int sourceLot=allocation.sourceLot(),destLot=allocation.destinationLot(),item=allocation.itemId();BigDecimal qty=allocation.quantity();
                    BigDecimal[] beforeAfter=decrementLot(c,destLot,qty,op.context().employeeId());if(beforeAfter==null)return KetQua.failure("STOCK_CHANGED","Tồn tủ đã thay đổi; không thể đảo an toàn.");
                    try(var tx=c.prepareStatement("INSERT INTO dbo.InventoryTransaction(maKho,maItem,maTonKho,loaiGiaoDich,soLuongThayDoi,tonTruocKhiThayDoi,tonSauKhiThayDoi,maChungTu,createdBy) VALUES(?,?,?,'TU_TRUC_DAO_NHAN',?,?,?,?,?)")){
                        tx.setInt(1,h.destination());tx.setInt(2,item);tx.setInt(3,destLot);tx.setBigDecimal(4,qty.negate());tx.setBigDecimal(5,beforeAfter[0]);tx.setBigDecimal(6,beforeAfter[1]);tx.setString(7,reversalCode);tx.setInt(8,op.context().employeeId());tx.executeUpdate();}
                    LotData lot=loadLot(c,sourceLot);incrementLot(c,h.source(),sourceLot,item,lot,qty,op.context().employeeId(),reversalCode);
                }
                try(var up=c.prepareStatement("UPDATE dbo.TuTrucChuyenKho SET trangThai='REVERSED',lyDo=?,version=version+1 WHERE maPhieuXuat=? AND version=?")){
                    up.setString(1,op.reason());up.setInt(2,op.documentId());up.setLong(3,op.expectedVersion());if(up.executeUpdate()!=1)return KetQua.failure("VERSION_CONFLICT","Phiếu đã thay đổi. Hãy tải lại.");}
                updateTicketStatus(c,op.documentId(),"REVERSED");recordOperation(c,op.documentId(),op.operationKey(),"REVERSE",op.context().employeeId(),op.reason());
                auditAction(op.context(),op.documentId(),KiemToanHanhDongMa.INV_TRANSFER_REVERSED);
                return KetQua.success(loadTransfer(c,op.context().facilityId(),h.destination(),op.documentId()));
            }catch(SQLException ex){throw new IllegalStateException("Không đảo được phiếu tủ trực.",ex);}
        });
    }
    private static BigDecimal[] decrementLot(Connection c,int lot,BigDecimal qty,int actor)throws SQLException{
        try(var ps=c.prepareStatement("UPDATE dbo.TonKho SET soLuongTon=soLuongTon-?,version=version+1,updatedBy=?,updatedAt=SYSUTCDATETIME() OUTPUT deleted.soLuongTon,inserted.soLuongTon WHERE id=? AND soLuongTon>=? AND isDeleted=0")){
            ps.setBigDecimal(1,qty);ps.setInt(2,actor);ps.setInt(3,lot);ps.setBigDecimal(4,qty);try(var rs=ps.executeQuery()){return rs.next()?new BigDecimal[]{rs.getBigDecimal(1),rs.getBigDecimal(2)}:null;}}
    }
    private record ReverseAllocation(int sourceLot,int destinationLot,BigDecimal quantity,int itemId){}
    private static void incrementLot(Connection c,int warehouse,int lotId,int item,LotData lot,BigDecimal qty,int actor,String code)throws SQLException{
        BigDecimal before;
        try(var ps=c.prepareStatement("SELECT soLuongTon FROM dbo.TonKho WITH (UPDLOCK,HOLDLOCK) WHERE id=? AND maKho=? AND maItem=? AND isDeleted=0")){
            ps.setInt(1,lotId);ps.setInt(2,warehouse);ps.setInt(3,item);try(var rs=ps.executeQuery()){if(!rs.next())throw new SQLException("Source lot no longer available");before=rs.getBigDecimal(1);}}
        BigDecimal after=before.add(qty);
        try(var up=c.prepareStatement("UPDATE dbo.TonKho SET soLuongTon=?,version=version+1,updatedBy=?,updatedAt=SYSUTCDATETIME() WHERE id=?")){up.setBigDecimal(1,after);up.setInt(2,actor);up.setInt(3,lotId);up.executeUpdate();}
        try(var tx=c.prepareStatement("INSERT INTO dbo.InventoryTransaction(maKho,maItem,maTonKho,loaiGiaoDich,soLuongThayDoi,tonTruocKhiThayDoi,tonSauKhiThayDoi,maChungTu,createdBy) VALUES(?,?,?,'TU_TRUC_DAO_XUAT',?,?,?,?,?)")){
            tx.setInt(1,warehouse);tx.setInt(2,item);tx.setInt(3,lotId);tx.setBigDecimal(4,qty);tx.setBigDecimal(5,before);tx.setBigDecimal(6,after);tx.setString(7,code);tx.setInt(8,actor);tx.executeUpdate();}
    }

    private record Header(String status,long version,int createdBy,int source,int destination) {}
    private Header lockHeader(Connection c,ThaoTac op)throws SQLException{
        String sql="SELECT t.trangThai,t.version,t.createdBy,p.maKhoXuat,p.maKhoNhan FROM dbo.TuTrucChuyenKho t WITH (UPDLOCK,HOLDLOCK) JOIN dbo.PhieuXuatKho p ON p.id=t.maPhieuXuat JOIN dbo.Kho s ON s.id=p.maKhoXuat JOIN dbo.Kho d ON d.id=p.maKhoNhan WHERE t.maPhieuXuat=? AND t.maCoSo=? AND t.maKhoa=? AND p.isDeleted=0 AND s.maCoSo=t.maCoSo AND d.maCoSo=t.maCoSo";
        try(var ps=c.prepareStatement(sql)){ps.setInt(1,op.documentId());ps.setInt(2,op.context().facilityId());ps.setInt(3,op.context().departmentId());try(var rs=ps.executeQuery()){return rs.next()?new Header(rs.getString(1),rs.getLong(2),rs.getInt(3),rs.getInt(4),rs.getInt(5)):null;}}
    }
    private static String approvedStatus(Connection c,int id)throws SQLException{
        try(var ps=c.prepareStatement("SELECT CASE WHEN COUNT(*)=SUM(CASE WHEN soLuongDuyet=soLuongYeuCau THEN 1 ELSE 0 END) THEN 'APPROVED' ELSE 'PARTIALLY_APPROVED' END FROM dbo.TuTrucChuyenKhoDong WHERE maPhieuXuat=?")){
            ps.setInt(1,id);try(var rs=ps.executeQuery()){rs.next();return rs.getString(1);}}
    }
    private static void updateTicketStatus(Connection c,int id,String status)throws SQLException{
        try(var ps=c.prepareStatement("UPDATE dbo.PhieuXuatKho SET trangThaiNoiBo=?,version=version+1,updatedAt=SYSUTCDATETIME() WHERE id=? AND isDeleted=0")){ps.setString(1,status);ps.setInt(2,id);ps.executeUpdate();}
    }
    private static int cabinetFor(Connection c,int id)throws SQLException{
        try(var ps=c.prepareStatement("SELECT maKhoNhan FROM dbo.PhieuXuatKho WHERE id=?")){ps.setInt(1,id);try(var rs=ps.executeQuery()){if(!rs.next())throw new SQLException("Missing transfer");return rs.getInt(1);}}
    }
    private static ChuyenKho loadTransfer(Connection c,int facility,int cabinet,int id)throws SQLException{
        String code,status;long version;
        try(var ps=c.prepareStatement("SELECT p.soPhieuXuat,t.trangThai,t.version FROM dbo.TuTrucChuyenKho t JOIN dbo.PhieuXuatKho p ON p.id=t.maPhieuXuat WHERE t.maPhieuXuat=? AND t.maCoSo=? AND p.maKhoNhan=?")){
            ps.setInt(1,id);ps.setInt(2,facility);ps.setInt(3,cabinet);
            try(var rs=ps.executeQuery()){
                if(!rs.next())throw new IllegalStateException("Phiếu không thuộc tủ/cơ sở đang chọn.");
                code=rs.getString(1);status=rs.getString(2);version=rs.getLong(3);
            }
        }
        List<DongChuyenKho> lines=new ArrayList<>();
        try(var lp=c.prepareStatement("SELECT d.id,i.id,i.maItem,i.tenItem,i.donViCoBan,d.soLuongYeuCau,d.soLuongDuyet,d.soLuongXuat,d.soLuongNhan FROM dbo.TuTrucChuyenKhoDong d JOIN dbo.Item i ON i.id=d.maItem WHERE d.maPhieuXuat=? ORDER BY d.id")){
            lp.setInt(1,id);try(var lr=lp.executeQuery()){while(lr.next())lines.add(new DongChuyenKho(lr.getInt(1),lr.getInt(2),lr.getString(3),lr.getString(4),lr.getString(5),lr.getBigDecimal(6),lr.getBigDecimal(7),lr.getBigDecimal(8),lr.getBigDecimal(9)));}}
        List<LoChuyenKho> lots=new ArrayList<>();
        try(var lp=c.prepareStatement("SELECT a.id,a.maDong,t.id,t.soLo,t.hanSuDung,a.soLuongXuat,a.soLuongNhan FROM dbo.TuTrucChuyenKhoLo a JOIN dbo.TonKho t ON t.id=a.maTonKhoXuat JOIN dbo.TuTrucChuyenKhoDong d ON d.id=a.maDong WHERE d.maPhieuXuat=? ORDER BY a.id")){
            lp.setInt(1,id);try(var lr=lp.executeQuery()){while(lr.next())lots.add(new LoChuyenKho(lr.getInt(1),lr.getInt(2),lr.getInt(3),lr.getString(4),lr.getDate(5).toLocalDate(),lr.getBigDecimal(6),lr.getBigDecimal(7)));}}
        return new ChuyenKho(id,code,status,version,lines,lots);
    }
    private ChuyenKho replay(Connection c,ThaoTac op,String action,String payload)throws SQLException{
        int id;
        try(var ps=c.prepareStatement("SELECT maPhieuXuat,payloadHash FROM dbo.TuTrucChuyenKhoThaoTac WHERE operationKey=? AND maPhieuXuat=? AND hanhDong=?")){
            ps.setObject(1,op.operationKey());ps.setInt(2,op.documentId());ps.setString(3,action);try(var rs=ps.executeQuery()){
                if(!rs.next())return null;id=rs.getInt(1);if(!Objects.equals(rs.getString(2),fingerprint(payload)))throw new SQLException("Idempotency key reused with a different payload");}}
        return loadTransfer(c,op.context().facilityId(),cabinetFor(c,id),id);
    }
    private void auditAction(PhanQuyenNguCanh context,int id,KiemToanHanhDongMa action){
        audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(context.employeeId(),context.roleId(),context.roleCode(),context.facilityId(),context.departmentId(),action,KiemToanTaiNguyenLoai.INVENTORY_ISSUE,id,KiemToanKetQua.SUCCESS,null,Instant.now(),null,context.sessionId()));
    }

    private static String ticketCode(Connection c,int id) throws SQLException {
        try (var ps=c.prepareStatement("SELECT soPhieuXuat FROM PhieuXuatKho WHERE id=?")) {
            ps.setInt(1,id); try (var rs=ps.executeQuery()) { return rs.next()?rs.getString(1):""; }
        }
    }
    private static long recordOperation(Connection c,int id,UUID key,String action,int actor,String payload) throws SQLException {
        try (var ps=c.prepareStatement("INSERT INTO TuTrucChuyenKhoThaoTac(maPhieuXuat,operationKey,hanhDong,createdBy,payloadHash) OUTPUT INSERTED.id VALUES(?,?,?,?,?)")) {
            ps.setInt(1,id); ps.setObject(2,key); ps.setString(3,action); ps.setInt(4,actor);ps.setString(5,fingerprint(payload));
            try(var rs=ps.executeQuery()){if(!rs.next())throw new SQLException("Missing transfer operation ID");return rs.getLong(1);}
        }
    }
    private static String requestPayload(YeuCau request){
        return request.cabinetId()+"|"+request.sourceWarehouseId()+"|"+request.context().facilityId()+"|"+request.context().departmentId()+"|"
                +Objects.toString(request.note(),"").trim()+"|"+request.lines().stream().sorted(Comparator.comparingInt(DongYeuCau::itemId)
                ).map(l->l.itemId()+"="+l.quantity().stripTrailingZeros().toPlainString()+"["+l.claims().stream().sorted(Comparator.comparingLong(ClaimNhuCau::demandId)
                        ).map(cl->cl.demandId()+":"+cl.quantity().stripTrailingZeros().toPlainString()).collect(java.util.stream.Collectors.joining(","))+"]").collect(java.util.stream.Collectors.joining(";"));
    }
    private static boolean sameRequestedLines(Connection c,int documentId,List<DongYeuCau> requested)throws SQLException{
        Map<Integer,BigDecimal> existing=new TreeMap<>();try(var ps=c.prepareStatement("SELECT maItem,soLuongYeuCau FROM dbo.TuTrucChuyenKhoDong WHERE maPhieuXuat=? ORDER BY maItem")){
            ps.setInt(1,documentId);try(var rs=ps.executeQuery()){while(rs.next())existing.put(rs.getInt(1),rs.getBigDecimal(2));}}
        Map<Integer,BigDecimal> expected=new TreeMap<>();for(var line:requested)expected.put(line.itemId(),line.quantity());
        if(!existing.keySet().equals(expected.keySet()))return false;
        return existing.entrySet().stream().allMatch(e->e.getValue().compareTo(expected.get(e.getKey()))==0);
    }
    private static String fingerprint(String payload){
        try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Objects.toString(payload,"").getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException("SHA-256 unavailable",ex);}
    }

    private static LocalDateTime time(ResultSet rs,String column) throws SQLException {
        var value=rs.getTimestamp(column); return value==null?null:value.toLocalDateTime();
    }

    private boolean isAuthorized(PhanQuyenNguCanh context, HanhDongQuyen action) {
        return authorization != null && context != null && context.isValid()
                && authorization.evaluate(new PhanQuyenYeuCau(context, "INVENTORY", action,
                context.facilityId(), context.departmentId())).allowed();
    }
}
