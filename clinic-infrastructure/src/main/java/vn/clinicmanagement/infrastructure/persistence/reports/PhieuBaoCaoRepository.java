package vn.clinicmanagement.infrastructure.persistence.reports;

import vn.clinicmanagement.application.reports.PhieuBaoCaoReadCongKetNoi;
import vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi;
import java.sql.*;
import java.util.*;

/** One row per persisted ticket/header. No query creates or changes documents. */
public final class PhieuBaoCaoRepository implements PhieuBaoCaoReadCongKetNoi {
    @FunctionalInterface public interface Connections {Connection open() throws SQLException;}
    private final Connections connections;
    public PhieuBaoCaoRepository(){this(CoSoDuLieuKetNoi::getConnection);}
    public PhieuBaoCaoRepository(Connections connections){this.connections=Objects.requireNonNull(connections);}
    static String sql(Loai type,boolean department){
        String patient="p.maBenhNhanFormat patientCode,p.ho+N' '+p.ten patientName,p.dienThoai phone,ISNULL(k.tenKhoa,N'') department, ";
        String visit=" JOIN LuotKham v ON v.id=t.maLuotKham AND v.isDeleted=0 JOIN BenhNhan p ON p.id=v.maBenhNhan AND p.isDeleted=0 LEFT JOIN Khoa k ON k.id=v.maKhoa ";
        String fields,from,date,scope,extra;
        switch(type){
            case KHAM_BENH->{fields="t.id,t.id visitId,CAST(t.id AS NVARCHAR(50)) code,t.thoiGianTiepNhan docDate,t.trangThai status,"+patient+"ISNULL(n.ho+N' '+n.ten,N'') author,CAST(NULL AS DECIMAL(18,2)) amount,CAST(NULL AS DECIMAL(18,2)) paid,ISNULL(t.huongXuTri,N'') extra1,CAST(t.emrLocked AS NVARCHAR(10)) extra2";
                from="LuotKham t JOIN BenhNhan p ON p.id=t.maBenhNhan AND p.isDeleted=0 LEFT JOIN Khoa k ON k.id=t.maKhoa LEFT JOIN NhanVien n ON n.id=t.createdBy";date="t.thoiGianTiepNhan";scope="t.maCoSo";extra="t.maKhoa";}
            case LICH_HEN->{fields="t.id,0 visitId,CAST(t.id AS NVARCHAR(50)) code,t.thoiGianBatDau docDate,t.trangThai status,"+patient+"ISNULL(n.ho+N' '+n.ten,N'') author,CAST(NULL AS DECIMAL(18,2)) amount,CAST(NULL AS DECIMAL(18,2)) paid,ISNULL(t.lyDo,N'') extra1,ISNULL(t.ghiChuHuy,N'') extra2";
                from="LichHen t JOIN BenhNhan p ON p.id=t.maBenhNhan AND p.isDeleted=0 LEFT JOIN Khoa k ON k.id=t.maKhoa LEFT JOIN NhanVien n ON n.id=t.maBacSi";date="t.thoiGianBatDau";scope="t.maCoSo";extra="t.maKhoa";}
            case HOA_DON->{fields="t.id,t.maLuotKham visitId,t.soHoaDon code,t.ngayLap docDate,t.trangThai status,"+patient+"ISNULL(n.ho+N' '+n.ten,N'') author,t.tongTien amount,t.daThanhToan paid,ISNULL(t.ghiChu,N'') extra1,CAST(t.soTienBaoHiemChiTra AS NVARCHAR(50)) extra2";
                from="HoaDon t"+visit+" LEFT JOIN NhanVien n ON n.id=t.createdBy";date="t.ngayLap";scope="t.maCoSo";extra="v.maKhoa";}
            case DON_THUOC->{fields="t.id,t.maLuotKham visitId,CAST(t.id AS NVARCHAR(50)) code,t.ngayKe docDate,t.trangThai status,"+patient+"ISNULL(n.ho+N' '+n.ten,N'') author,CAST(NULL AS DECIMAL(18,2)) amount,CAST(NULL AS DECIMAL(18,2)) paid,ISNULL(t.ghiChu,N'') extra1,ISNULL(t.loiDanDonThuoc,N'') extra2";
                from="DonThuoc t"+visit+" LEFT JOIN NhanVien n ON n.id=t.maBacSi";date="t.ngayKe";scope="t.maCoSo";extra="v.maKhoa";}
            case CHI_DINH->{fields="t.id,t.maLuotKham visitId,CAST(t.id AS NVARCHAR(50)) code,t.thoiGianChiDinh docDate,t.trangThai status,"+patient+"ISNULL(n.ho+N' '+n.ten,N'') author,t.soLuong*t.donGia amount,CAST(NULL AS DECIMAL(18,2)) paid,ISNULL(d.tenDichVu,N'') extra1,ISNULL(t.ketLuan,N'') extra2";
                from="ChiDinhDichVu t"+visit+" LEFT JOIN DanhMucDichVu d ON d.id=t.maDichVu LEFT JOIN NhanVien n ON n.id=t.maBacSiChiDinh";date="t.thoiGianChiDinh";scope="v.maCoSo";extra="v.maKhoa";}
            case PHIEU_THU->{fields="t.id,h.maLuotKham visitId,CONCAT(N'PT-',t.id) code,t.ngayThu docDate,t.phuongThuc status,"+patient+"ISNULL(n.ho+N' '+n.ten,N'') author,h.tongTien amount,t.soTien paid,ISNULL(t.ghiChu,N'') extra1,ISNULL(h.soHoaDon,N'') extra2";
                from="PhieuThu t JOIN HoaDon h ON h.id=t.maHoaDon AND h.isDeleted=0 JOIN LuotKham v ON v.id=h.maLuotKham AND v.isDeleted=0 JOIN BenhNhan p ON p.id=v.maBenhNhan AND p.isDeleted=0 LEFT JOIN Khoa k ON k.id=v.maKhoa LEFT JOIN NhanVien n ON n.id=t.nguoiThu";date="t.ngayThu";scope="h.maCoSo";extra="v.maKhoa";}
            case PHIEU_HOAN_TRA->{fields="t.id,h.maLuotKham visitId,CONCAT(N'HT-',t.id) code,t.ngayHoan docDate,t.phuongThuc status,"+patient+"ISNULL(n.ho+N' '+n.ten,N'') author,h.tongTien amount,t.soTien paid,ISNULL(t.lyDo,N'') extra1,ISNULL(h.soHoaDon,N'') extra2";
                from="PhieuHoanTra t JOIN HoaDon h ON h.id=t.maHoaDon AND h.isDeleted=0 JOIN LuotKham v ON v.id=h.maLuotKham AND v.isDeleted=0 JOIN BenhNhan p ON p.id=v.maBenhNhan AND p.isDeleted=0 LEFT JOIN Khoa k ON k.id=v.maKhoa LEFT JOIN NhanVien n ON n.id=t.nguoiHoan";date="t.ngayHoan";scope="h.maCoSo";extra="v.maKhoa";}
            default->throw new IllegalArgumentException("Loại phiếu không hợp lệ.");
        }
        String guard=(type==Loai.HOA_DON||type==Loai.DON_THUOC)?" AND v.maCoSo=t.maCoSo":(type==Loai.PHIEU_THU||type==Loai.PHIEU_HOAN_TRA)?" AND v.maCoSo=h.maCoSo":"";
        return "SELECT "+fields+" FROM "+from+" WHERE t.isDeleted=0 AND "+date+">=? AND "+date+"<? AND "+scope+"=?"+guard+(department?" AND "+extra+"=?":"")+" ORDER BY "+date+" DESC,t.id DESC";
    }
    @Override public List<Phieu> load(TruyVan query){return loadTypes(query,List.of(Loai.values()));}
    @Override public List<Phieu> load(TruyVan query,Loai type){return loadTypes(query,List.of(type));}
    private List<Phieu> loadTypes(TruyVan query,List<Loai> types){
        List<Phieu> rows=new ArrayList<>();
        try(Connection c=connections.open()){
            for(Loai type:types)try(PreparedStatement ps=c.prepareStatement(sql(type,query.departmentId()!=null))){
                ps.setQueryTimeout(30);ps.setTimestamp(1,Timestamp.valueOf(query.from()));ps.setTimestamp(2,Timestamp.valueOf(query.toExclusive()));ps.setInt(3,query.facilityId());if(query.departmentId()!=null)ps.setInt(4,query.departmentId());
                try(ResultSet rs=ps.executeQuery()){while(rs.next()){
                    Timestamp date=rs.getTimestamp("docDate");Map<String,String> details=new LinkedHashMap<>();
                    String first=safe(rs.getString("extra1")),second=safe(rs.getString("extra2"));
                    switch(type){
                        case KHAM_BENH->{details.put("Xử trí",first);details.put("Đã khóa bệnh án",("1".equals(second)||"true".equalsIgnoreCase(second))?"Đã khóa":"Chưa khóa");}
                        case LICH_HEN->{details.put("Lý do hẹn",first);details.put("Ghi chú hủy",second);}
                        case HOA_DON->{details.put("Ghi chú",first);details.put("BHYT chi trả",second);}
                        case DON_THUOC->{details.put("Ghi chú",first);details.put("Lời dặn",second);}
                        case CHI_DINH->{details.put("Dịch vụ",first);details.put("Kết luận",second);}
                        case PHIEU_THU->{details.put("Ghi chú",first);details.put("Số hóa đơn",second);}
                        case PHIEU_HOAN_TRA->{details.put("Lý do",first);details.put("Số hóa đơn",second);}
                    }
                    rows.add(new Phieu(type,rs.getInt("id"),rs.getInt("visitId"),rs.getString("code"),date==null?null:date.toLocalDateTime(),rs.getString("status"),rs.getString("patientCode"),rs.getString("patientName"),rs.getString("phone"),rs.getString("department"),rs.getString("author"),rs.getBigDecimal("amount"),rs.getBigDecimal("paid"),details));
                }}
                if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();
            }
        }catch(SQLException ex){throw new IllegalStateException("Không tải được danh sách phiếu báo cáo. Kiểm tra kết nối và thử lại.",ex);}
        rows.sort(Comparator.comparing(Phieu::date,Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Phieu::key));return List.copyOf(rows);
    }
    private static String safe(String s){return s==null?"":s;}
}
