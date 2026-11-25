package vn.clinicmanagement.infrastructure.persistence.billing;
import vn.clinicmanagement.application.billing.VienPhiManHinhReadCongKetNoi;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import java.sql.*;
import java.util.*;
public final class VienPhiManHinhRepository implements VienPhiManHinhReadCongKetNoi {
 private final JdbcKetNoiNhaCungCap connections;
 public VienPhiManHinhRepository(JdbcKetNoiNhaCungCap connections){this.connections=connections;}
 public Optional<ThongTin> getByVisit(int visitId,int facilityId){
  String sql="SELECT p.maBenhNhanFormat,p.ho+N' '+p.ten patientName,p.ngaySinh,p.gioiTinh,p.diaChi,p.dienThoai,k.tenKhoa,r.tenPhong,l.doiTuongKham,l.hinhThucDieuTri,l.loaiDangKyKham,l.trangThai,l.thoiGianTiepNhan,l.thoiGianKetThucKham,"
   +"ISNULL(cs.tenCoSo,N'') facilityName,"
   +"ISNULL(bh.soBaoHiem,N'') bhytNumber,bh.ngayHetHan bhytExpiry,"
   +"COALESCE(NULLIF(diag.chanDoanDisplay,N''),l.lyDoKham,N'') diagnosis "
   +"FROM LuotKham l JOIN BenhNhan p ON p.id=l.maBenhNhan AND p.isDeleted=0 JOIN Khoa k ON k.id=l.maKhoa LEFT JOIN PhongKham r ON r.id=l.maPhongKham LEFT JOIN CoSo cs ON cs.id=l.maCoSo "
   +"OUTER APPLY (SELECT TOP 1 b.soBaoHiem,b.ngayHetHan FROM BaoHiem b WHERE b.maBenhNhan=p.id AND b.isDeleted=0 ORDER BY b.ngayHetHan DESC,b.id DESC) bh "
   +"OUTER APPLY (SELECT TOP 1 CASE WHEN icd.maICD IS NOT NULL AND icd.maICD<>'' THEN icd.maICD+N'-'+cd.chanDoanText ELSE cd.chanDoanText END chanDoanDisplay FROM ChanDoan cd LEFT JOIN ICD icd ON icd.id=cd.maICD WHERE cd.maLuotKham=l.id AND cd.isDeleted=0 ORDER BY cd.laChinh DESC,cd.id DESC) diag "
   +"WHERE l.id=? AND l.maCoSo=? AND l.isDeleted=0";
  try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){
   ps.setInt(1,visitId);ps.setInt(2,facilityId);
   try(var rs=ps.executeQuery()){
    if(!rs.next())return Optional.empty();
    Map<Integer,String> collectors=new HashMap<>();
    try(var names=lease.connection().prepareStatement("SELECT DISTINCT n.id,n.ho+N' '+n.ten name FROM PhieuThu pt JOIN HoaDon h ON h.id=pt.maHoaDon JOIN NhanVien n ON n.id=pt.nguoiThu WHERE h.maLuotKham=? AND h.maCoSo=? AND h.isDeleted=0 AND pt.isDeleted=0")){
     names.setInt(1,visitId);names.setInt(2,facilityId);try(var rows=names.executeQuery()){while(rows.next())collectors.put(rows.getInt(1),rows.getString(2));}
    }
    var birth=rs.getDate("ngaySinh");var admitted=rs.getTimestamp("thoiGianTiepNhan");var discharged=rs.getTimestamp("thoiGianKetThucKham");var bhytExp=rs.getDate("bhytExpiry");
    return Optional.of(new ThongTin(rs.getString("maBenhNhanFormat"),rs.getString("patientName"),birth==null?null:birth.toLocalDate(),rs.getString("gioiTinh"),rs.getString("diaChi"),rs.getString("dienThoai"),rs.getString("tenKhoa"),rs.getString("tenPhong"),rs.getString("doiTuongKham"),rs.getString("trangThai"),admitted==null?null:admitted.toLocalDateTime(),discharged==null?null:discharged.toLocalDateTime(),Map.copyOf(collectors),rs.getString("hinhThucDieuTri"),rs.getString("loaiDangKyKham"),rs.getString("bhytNumber"),bhytExp==null?null:bhytExp.toLocalDate(),rs.getString("diagnosis"),rs.getString("facilityName")));
   }
  }catch(SQLException ex){throw new IllegalStateException("Không thể tải thông tin viện phí.",ex);}
 }
}
