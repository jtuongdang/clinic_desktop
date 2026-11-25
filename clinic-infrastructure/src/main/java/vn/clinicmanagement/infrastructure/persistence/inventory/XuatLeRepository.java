package vn.clinicmanagement.infrastructure.persistence.inventory;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.*;
import vn.clinicmanagement.application.inventory.retail.XuatLe;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public final class XuatLeRepository implements XuatLe {
    private final JdbcKetNoiNhaCungCap connections;
    private final GiaoDichRanhGioi transactions;

    public XuatLeRepository(JdbcKetNoiNhaCungCap connections, GiaoDichRanhGioi transactions) {
        this.connections = connections;
        this.transactions = transactions;
    }

    @Override public List<Warehouse> warehouses(PhanQuyenNguCanh auth) {
        if (!XuatLe.permitted(auth)) return List.of();
        String sql = "SELECT id,tenKho FROM Kho WHERE maCoSo=? AND isDeleted=0 AND trangThai='HOAT_DONG' ORDER BY tenKho,id";
        try (var lease = connections.acquire(); var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1,auth.facilityId());
            var result = new ArrayList<Warehouse>();
            try (var rs = ps.executeQuery()) {
                while (rs.next()) result.add(new Warehouse(rs.getInt(1),rs.getString(2)));
            }
            return result;
        } catch (SQLException ex) { throw new IllegalStateException("Không tải được danh sách kho.",ex); }
    }

    @Override public List<Lot> lots(int warehouseId, PhanQuyenNguCanh auth) {
        if (!XuatLe.permitted(auth)) return List.of();
        String sql = """
                SELECT t.id,i.id,i.maItem,i.tenItem,i.donViCoBan,i.hamLuong,i.quyCachDongGoi,i.nuocSanXuat,
                       i.hangSanXuat,i.soDangKy,t.soLo,t.hanSuDung,t.soLuongTon,t.giaNhap,t.giaBan,i.thongTinThuoc
                FROM TonKho t JOIN Item i ON t.maItem=i.id JOIN Kho k ON t.maKho=k.id
                WHERE k.maCoSo=? AND k.id=? AND k.trangThai='HOAT_DONG' AND k.isDeleted=0
                  AND i.isDeleted=0 AND i.hoatDong=1 AND i.loaiItem='THUOC' AND t.isDeleted=0
                  AND t.hanSuDung>=CAST(GETDATE() AS date) AND t.soLuongTon>0
                ORDER BY i.tenItem,t.hanSuDung,t.id
                """;
        try (var lease = connections.acquire(); var ps = lease.connection().prepareStatement(sql)) {
            ps.setInt(1,auth.facilityId()); ps.setInt(2,warehouseId);
            var result = new ArrayList<Lot>();
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    var form = NhaThuocDanhSachRepository.decodeDrugForm(rs.getString(16));
                    String vat = form.getOrDefault("vatBan","").trim();
                    result.add(new Lot(rs.getInt(1),rs.getInt(2),rs.getString(3),rs.getString(4),rs.getString(5),
                            rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(9),rs.getString(10),
                            rs.getString(11),rs.getDate(12).toLocalDate(),rs.getBigDecimal(13),rs.getBigDecimal(14),
                            rs.getBigDecimal(15),vat.isEmpty()?BigDecimal.ZERO:new BigDecimal(vat.replace(',','.')),
                            form.getOrDefault("maVach",""),form.getOrDefault("huongDan","")));
                }
            }
            return result;
        } catch (SQLException | java.io.IOException ex) { throw new IllegalStateException("Không tải được lô thuốc.",ex); }
    }

    @Override public KetQua<String> save(Ticket ticket, PhanQuyenNguCanh auth) {
        if (!XuatLe.permitted(auth)) return KetQua.failure("FORBIDDEN","Tài khoản chưa có quyền xuất thuốc.");
        try { ticket.validate(); }
        catch (IllegalArgumentException ex) { return KetQua.failure("INVALID_TICKET",ex.getMessage()); }
        return transactions.inTransaction(() -> {
            try (var lease = connections.acquire()) {
                Connection c = lease.connection();
                int id = insertHeader(c,ticket,auth);
                if (id == 0) return KetQua.failure("INVALID_WAREHOUSE","Kho không còn hoạt động tại cơ sở.");
                // Use a consistent lock order when multiple tills issue the same lots.
                for (var line : ticket.lines().stream().sorted(Comparator.comparingInt(l -> l.lot().id())).toList()) {
                    BigDecimal remaining = takeStock(c,line,ticket.warehouseId(),auth.employeeId());
                    if (remaining == null) return KetQua.failure("STOCK_CHANGED",
                            "Lô " + line.lot().batch() + " đã đổi giá, hết hạn hoặc không đủ tồn. Hãy tải lại lô thuốc.");
                    insertLine(c,id,line,auth.employeeId());
                    recordMovement(c,ticket,line,remaining,auth.employeeId());
                }
                return KetQua.success(ticket.number());
            } catch (SQLException ex) {
                if (ex.getErrorCode()==2627 || ex.getErrorCode()==2601)
                    return KetQua.failure("ALREADY_SAVED","Mã phiếu đã được lưu. Kiểm tra Danh Sách Phiếu Xuất Lẻ.");
                throw new IllegalStateException("Không lưu được phiếu xuất.",ex);
            }
        });
    }

    private static int insertHeader(Connection c, Ticket t, PhanQuyenNguCanh auth) throws SQLException {
        String sql = """
                INSERT INTO PhieuXuatKho(soPhieuXuat,maKhoXuat,loaiXuat,trangThaiNoiBo,ngayXuat,
                    tongTien,createdBy,ghiChu,maKhachLe,tenKhachLe,tuoiKhachLe,gioiTinhKhachLe,
                    diaChiKhachLe,dienThoaiKhachLe,emailKhachLe,bacSiKe)
                OUTPUT inserted.id
                SELECT ?,id,'XUAT_LE','DA_XUAT',?,?,?,?,?,?,?,?,?,?,?,?
                FROM Kho WHERE id=? AND maCoSo=? AND isDeleted=0 AND trangThai='HOAT_DONG'
                """;
        try (var ps = c.prepareStatement(sql)) {
            var buyer = t.customer();
            ps.setString(1,t.number());
            ps.setTimestamp(2,Timestamp.valueOf(t.date().atTime(java.time.LocalTime.now())));
            ps.setBigDecimal(3,t.total()); ps.setInt(4,auth.employeeId()); ps.setString(5,buyer.note());
            ps.setString(6,buyer.code()); ps.setString(7,buyer.name());
            ps.setObject(8,buyer.age().isBlank()?null:Integer.valueOf(buyer.age()),Types.INTEGER);
            ps.setString(9,buyer.gender()); ps.setString(10,buyer.address()); ps.setString(11,buyer.phone());
            ps.setString(12,buyer.email()); ps.setString(13,buyer.doctor());
            ps.setInt(14,t.warehouseId()); ps.setInt(15,auth.facilityId());
            try (var rs=ps.executeQuery()) { return rs.next()?rs.getInt(1):0; }
        }
    }

    private static BigDecimal takeStock(Connection c, Line l, int warehouse, int employee) throws SQLException {
        String sql = """
                UPDATE t SET soLuongTon=soLuongTon-?,version=t.version+1,updatedBy=?,updatedAt=GETDATE()
                OUTPUT inserted.soLuongTon
                FROM TonKho t JOIN Item i ON i.id=t.maItem
                WHERE t.id=? AND t.maKho=? AND t.maItem=? AND t.isDeleted=0 AND t.soLuongTon>=?
                  AND t.hanSuDung>=CAST(GETDATE() AS date) AND t.giaBan=? AND i.hoatDong=1
                  AND i.loaiItem='THUOC' AND i.isDeleted=0 AND t.soLo=? AND t.hanSuDung=?
                """;
        try (var ps=c.prepareStatement(sql)) {
            ps.setBigDecimal(1,l.quantity()); ps.setInt(2,employee); ps.setInt(3,l.lot().id()); ps.setInt(4,warehouse);
            ps.setInt(5,l.lot().itemId()); ps.setBigDecimal(6,l.quantity()); ps.setBigDecimal(7,l.lot().price());
            ps.setString(8,l.lot().batch()); ps.setDate(9,java.sql.Date.valueOf(l.lot().expiry()));
            try (var rs=ps.executeQuery()) { return rs.next()?rs.getBigDecimal(1):null; }
        }
    }

    private static void insertLine(Connection c,int id,Line l,int actor) throws SQLException {
        try (var ps=c.prepareStatement("""
                INSERT INTO ChiTietXuatKho(maPhieuXuat,maItem,maTonKho,soLuong,donGia,vatBanLe,mienGiamBanLe,
                    thanhTienBanLe,huongDanBanLe,chanDoanBanLe,createdBy) VALUES(?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            ps.setInt(1,id); ps.setInt(2,l.lot().itemId()); ps.setInt(3,l.lot().id()); ps.setBigDecimal(4,l.quantity());
            ps.setBigDecimal(5,l.lot().price()); ps.setBigDecimal(6,l.vat()); ps.setBigDecimal(7,l.discount());
            ps.setBigDecimal(8,l.amount()); ps.setString(9,l.directions()); ps.setString(10,l.diagnosis());
            ps.setInt(11,actor); ps.executeUpdate();
        }
    }

    private static void recordMovement(Connection c,Ticket t,Line l,BigDecimal remaining,int actor) throws SQLException {
        try (var ps=c.prepareStatement("""
                INSERT INTO InventoryTransaction(maKho,maItem,maTonKho,loaiGiaoDich,soLuongThayDoi,
                    tonTruocKhiThayDoi,tonSauKhiThayDoi,maChungTu,createdBy) VALUES(?,?,?,'XUAT_LE',?,?,?,?,?)
                """)) {
            ps.setInt(1,t.warehouseId()); ps.setInt(2,l.lot().itemId()); ps.setInt(3,l.lot().id());
            ps.setBigDecimal(4,l.quantity().negate()); ps.setBigDecimal(5,remaining.add(l.quantity()));
            ps.setBigDecimal(6,remaining); ps.setString(7,t.number()); ps.setInt(8,actor); ps.executeUpdate();
        }
    }
}
