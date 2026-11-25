package vn.clinicmanagement.infrastructure.persistence.inventory;
import vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import java.sql.*;
import java.time.*;
import java.util.*;
public final class NhaThuocDanhSachRepository implements NhaThuocDanhSachReadCongKetNoi {
 private final JdbcKetNoiNhaCungCap connections;
 public NhaThuocDanhSachRepository(JdbcKetNoiNhaCungCap connections){this.connections=Objects.requireNonNull(connections);}
 public List<Phieu> getTickets(int facilityId,LocalDateTime from,LocalDateTime to){
  return getTickets(facilityId,from,to,NgayLoc.NGAY_NHAP_XUAT);
 }
 @Override
 public List<Phieu> getTickets(int facilityId,LocalDateTime from,LocalDateTime to,NgayLoc dateField){
  List<Phieu> rows=new ArrayList<>();
  NgayLoc mode=dateField==null?NgayLoc.TONG_HOP:dateField;
  String inFilter=switch(mode){
   case NGAY_TAO -> "p.createdAt>=? AND p.createdAt<?";
   case NGAY_NHAP_XUAT -> "p.ngayNhap>=? AND p.ngayNhap<?";
   case TONG_HOP -> "((p.ngayNhap>=? AND p.ngayNhap<?) OR (p.createdAt>=? AND p.createdAt<?))";
  };
  String outFilter=switch(mode){
   case NGAY_TAO -> "p.createdAt>=? AND p.createdAt<?";
   case NGAY_NHAP_XUAT -> "p.ngayXuat>=? AND p.ngayXuat<?";
   case TONG_HOP -> "((p.ngayXuat>=? AND p.ngayXuat<?) OR (p.createdAt>=? AND p.createdAt<?))";
  };
  String sql="SELECT p.id,'NHAP_KHO' type,p.soPhieuNhap code,CASE WHEN p.isDeleted=1 THEN 'DA_HUY' ELSE p.trangThai END status,p.createdAt,p.ngayNhap date,p.deletedAt,ISNULL(n.ho+N' '+n.ten,N'') cancelledBy,k.id warehouseId,k.tenKho warehouse,N'' destination,N'Nhập nhà cung cấp' detailType,p.tongTien FROM PhieuNhapKho p JOIN Kho k ON k.id=p.maKho LEFT JOIN NhanVien n ON n.id=p.deletedBy WHERE k.maCoSo=? AND k.isDeleted=0 AND "+inFilter+" "
   +"UNION ALL SELECT p.id,'XUAT_KHO',p.soPhieuXuat,CASE WHEN p.isDeleted=1 THEN 'DA_HUY' ELSE p.trangThaiNoiBo END,p.createdAt,p.ngayXuat,p.deletedAt,ISNULL(n.ho+N' '+n.ten,N''),k.id,k.tenKho,ISNULL(d.tenKho,N''),CASE WHEN p.maDonThuoc IS NOT NULL AND p.loaiXuat='BAN_THUOC' THEN 'XUAT_DON' ELSE p.loaiXuat END,p.tongTien FROM PhieuXuatKho p JOIN Kho k ON k.id=p.maKhoXuat LEFT JOIN Kho d ON d.id=p.maKhoNhan LEFT JOIN NhanVien n ON n.id=p.deletedBy WHERE k.maCoSo=? AND k.isDeleted=0 AND "+outFilter+" ORDER BY date DESC,id DESC";
  try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){
   int idx=1;
   Timestamp f=Timestamp.valueOf(from),t=Timestamp.valueOf(to);
   ps.setInt(idx++,facilityId);ps.setTimestamp(idx++,f);ps.setTimestamp(idx++,t);
   if(mode==NgayLoc.TONG_HOP){ps.setTimestamp(idx++,f);ps.setTimestamp(idx++,t);}
   ps.setInt(idx++,facilityId);ps.setTimestamp(idx++,f);ps.setTimestamp(idx++,t);
   if(mode==NgayLoc.TONG_HOP){ps.setTimestamp(idx++,f);ps.setTimestamp(idx++,t);}
   try(var rs=ps.executeQuery()){while(rs.next())rows.add(new Phieu(rs.getInt("id"),rs.getString("type"),rs.getString("code"),rs.getString("status"),time(rs,"createdAt"),time(rs,"date"),time(rs,"deletedAt"),rs.getString("cancelledBy"),rs.getInt("warehouseId"),rs.getString("warehouse"),rs.getString("destination"),rs.getString("detailType"),rs.getBigDecimal("tongTien")));}
   return List.copyOf(rows);
  }catch(SQLException ex){throw new IllegalStateException("Không thể tải phiếu kho từ CSDL.",ex);}
 }
 public Bang getList(DanhSach type,int facilityId,Integer warehouseId,LocalDateTime from,LocalDateTime to){
  if(type==DanhSach.PHIEU_XUAT_LE){
   var rows=getTickets(facilityId,from,to).stream().filter(p->"XUAT_KHO".equals(p.type())&&("BAN_THUOC".equals(p.detailType())||"XUAT_LE".equals(p.detailType())))
    .filter(p->warehouseId==null||p.warehouseId()==warehouseId).map(p->List.<Object>of(p.code(),p.date(),p.warehouse(),p.cancelledAt()!=null?"Đã hủy":p.status(),p.amount())).toList();
   return new Bang(List.of("Mã phiếu","Ngày xuất","Kho thuốc","Trạng thái","Tổng tiền"),rows);
  }
  boolean reserve=type==DanhSach.DU_TRU_THUOC;
  String sql=reserve
   ?"SELECT k.tenKho,i.maItem,i.tenItem,i.donViCoBan,ISNULL(SUM(CASE WHEN t.hanSuDung>=CAST(GETDATE() AS DATE) THEN t.soLuongTon ELSE 0 END),0) available,i.tonToiThieu FROM Kho k CROSS JOIN Item i LEFT JOIN TonKho t ON t.maKho=k.id AND t.maItem=i.id AND t.isDeleted=0 WHERE k.maCoSo=? AND k.isDeleted=0 AND k.trangThai='HOAT_DONG' AND i.isDeleted=0 AND i.hoatDong=1 AND i.loaiItem='THUOC' AND (? IS NULL OR k.id=?) GROUP BY k.id,k.tenKho,i.maItem,i.tenItem,i.donViCoBan,i.tonToiThieu HAVING ISNULL(SUM(CASE WHEN t.hanSuDung>=CAST(GETDATE() AS DATE) THEN t.soLuongTon ELSE 0 END),0)<i.tonToiThieu ORDER BY k.tenKho,i.tenItem"
   :"SELECT k.tenKho,i.maItem,i.tenItem,i.donViCoBan,t.soLo,t.hanSuDung,t.soLuongTon,t.giaNhap,t.giaBan FROM TonKho t JOIN Kho k ON k.id=t.maKho JOIN Item i ON i.id=t.maItem WHERE k.maCoSo=? AND k.isDeleted=0 AND t.isDeleted=0 AND i.isDeleted=0 AND i.loaiItem='THUOC' AND (? IS NULL OR k.id=?) ORDER BY k.tenKho,i.tenItem,t.hanSuDung";
  try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){
   ps.setInt(1,facilityId);ps.setObject(2,warehouseId,Types.INTEGER);ps.setObject(3,warehouseId,Types.INTEGER);
   List<List<Object>> rows=new ArrayList<>();try(var rs=ps.executeQuery()){while(rs.next()){
    if(reserve){var available=rs.getBigDecimal("available");var minimum=rs.getBigDecimal("tonToiThieu");rows.add(List.of(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),available,minimum,minimum.subtract(available).max(java.math.BigDecimal.ZERO)));}
    else rows.add(List.of(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getDate(6),rs.getBigDecimal(7),rs.getBigDecimal(8),rs.getBigDecimal(9)));
   }}return new Bang(reserve?List.of("Kho thuốc","Mã thuốc","Tên thuốc","Đơn vị","Tồn khả dụng","Tồn tối thiểu","Đề nghị bổ sung"):List.of("Kho thuốc","Mã thuốc","Tên thuốc","Đơn vị","Số lô","Hạn dùng","Tồn kho","Giá nhập","Giá bán"),rows);
  }catch(SQLException ex){throw new IllegalStateException("Không thể tải danh sách thuốc từ CSDL.",ex);}
 }
 public List<Thuoc> getDrugDirectory(int facilityId,Integer warehouseId){
  String sql="SELECT i.id,i.version,i.maItem,i.tenItem,i.hoatChat,i.hamLuong,i.donViCoBan,i.maTuongDuong,i.nhomThuoc,i.nongDo,i.theTich,i.thuocBHYT,i.maDanhMucYHCT,s.tenKho,s.soLo,s.hanSuDung,ISNULL(s.soLuongTon,0) stock FROM Item i "
   +"LEFT JOIN (SELECT t.maItem,k.tenKho,t.soLo,t.hanSuDung,t.soLuongTon FROM TonKho t JOIN Kho k ON k.id=t.maKho WHERE k.maCoSo=? AND k.isDeleted=0 AND t.isDeleted=0 AND (? IS NULL OR k.id=?)) s ON s.maItem=i.id "
   +"WHERE i.isDeleted=0 AND i.loaiItem='THUOC' AND EXISTS(SELECT 1 FROM Kho k WHERE k.maCoSo=? AND k.isDeleted=0 AND (? IS NULL OR k.id=?)) ORDER BY i.tenItem,i.maItem,s.hanSuDung";
  try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){
   ps.setInt(1,facilityId);ps.setObject(2,warehouseId,Types.INTEGER);ps.setObject(3,warehouseId,Types.INTEGER);
   ps.setInt(4,facilityId);ps.setObject(5,warehouseId,Types.INTEGER);ps.setObject(6,warehouseId,Types.INTEGER);
   List<Thuoc> result=new ArrayList<>();try(var rs=ps.executeQuery()){while(rs.next()){
    var expiry=rs.getDate("hanSuDung");Boolean insured=(Boolean)rs.getObject("thuocBHYT");
    result.add(new Thuoc(rs.getInt("id"),rs.getLong("version"),text(rs,"maItem"),text(rs,"maTuongDuong"),text(rs,"tenItem"),text(rs,"hoatChat"),text(rs,"hamLuong"),text(rs,"nongDo"),text(rs,"theTich"),text(rs,"donViCoBan"),text(rs,"nhomThuoc"),insured,text(rs,"maDanhMucYHCT"),text(rs,"tenKho"),text(rs,"soLo"),expiry==null?null:expiry.toLocalDate(),rs.getBigDecimal("stock")));
   }}return List.copyOf(result);
  }catch(SQLException ex){throw new IllegalStateException("Không thể tải danh mục thuốc từ CSDL.",ex);}
 }
 public void saveDrugMetadata(Thuoc drug,int facilityId,Integer warehouseId,int employeeId){
  String sql="UPDATE Item SET maTuongDuong=?,nhomThuoc=?,nongDo=?,theTich=?,thuocBHYT=?,maDanhMucYHCT=?,updatedBy=?,updatedAt=SYSUTCDATETIME(),version=version+1 WHERE id=? AND version=? AND isDeleted=0 AND loaiItem='THUOC' AND EXISTS(SELECT 1 FROM Kho k WHERE k.maCoSo=? AND k.isDeleted=0 AND (? IS NULL OR k.id=?))";
  try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){
   ps.setString(1,drug.equivalentCode());ps.setString(2,drug.group());ps.setString(3,drug.concentration());ps.setString(4,drug.volume());ps.setObject(5,drug.insured(),Types.BIT);ps.setString(6,drug.traditionalCode());ps.setInt(7,employeeId);ps.setInt(8,drug.id());ps.setLong(9,drug.version());ps.setInt(10,facilityId);ps.setObject(11,warehouseId,Types.INTEGER);ps.setObject(12,warehouseId,Types.INTEGER);
   if(ps.executeUpdate()!=1)throw new IllegalStateException("Thuốc đã thay đổi hoặc không thuộc phạm vi truy cập. Vui lòng làm mới.");
  }catch(SQLException ex){throw new IllegalStateException("Không thể lưu thông tin danh mục thuốc.",ex);}
 }
 public void saveDrugForm(Thuoc drug,Map<String,String> fields,int facilityId,int employeeId){
  java.util.Properties values=new java.util.Properties();values.putAll(fields);String xml;
  try(var bytes=new java.io.ByteArrayOutputStream()){values.storeToXML(bytes,"Thông tin bổ sung danh mục thuốc",java.nio.charset.StandardCharsets.UTF_8);xml=bytes.toString(java.nio.charset.StandardCharsets.UTF_8);}catch(java.io.IOException ex){throw new IllegalStateException("Không thể chuẩn bị dữ liệu thuốc.",ex);}
  String sql="UPDATE Item SET maTuongDuong=?,nhomThuoc=?,nongDo=?,theTich=?,thuocBHYT=?,maDanhMucYHCT=?,thongTinThuoc=?,updatedBy=?,updatedAt=SYSUTCDATETIME(),version=version+1 WHERE id=? AND version=? AND isDeleted=0 AND loaiItem='THUOC' AND EXISTS(SELECT 1 FROM Kho WHERE maCoSo=? AND isDeleted=0)";
  try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){
   ps.setString(1,drug.equivalentCode());ps.setString(2,drug.group());ps.setString(3,drug.concentration());ps.setString(4,drug.volume());ps.setObject(5,drug.insured(),Types.BIT);ps.setString(6,drug.traditionalCode());ps.setString(7,xml);ps.setInt(8,employeeId);ps.setInt(9,drug.id());ps.setLong(10,drug.version());ps.setInt(11,facilityId);
   if(ps.executeUpdate()!=1)throw new IllegalStateException("Thuốc đã thay đổi hoặc cơ sở không có kho hợp lệ.");
  }catch(SQLException ex){throw new IllegalStateException("Không thể lưu biểu mẫu thuốc.",ex);}
 }

 @Override
 public void updateDrugForm(int itemId,Thuoc drug,Map<String,String> fields,int facilityId,int employeeId){
  java.util.Properties values=new java.util.Properties();values.putAll(fields);String xml;
  try(var bytes=new java.io.ByteArrayOutputStream()){values.storeToXML(bytes,"Thông tin bổ sung danh mục thuốc",java.nio.charset.StandardCharsets.UTF_8);xml=bytes.toString(java.nio.charset.StandardCharsets.UTF_8);}catch(java.io.IOException ex){throw new IllegalStateException("Không thể chuẩn bị dữ liệu thuốc.",ex);}
  String sql="UPDATE Item SET maTuongDuong=?,nhomThuoc=?,nongDo=?,theTich=?,thuocBHYT=?,maDanhMucYHCT=?,thongTinThuoc=?,updatedBy=?,updatedAt=SYSUTCDATETIME(),version=version+1 WHERE id=? AND isDeleted=0 AND loaiItem='THUOC' AND EXISTS(SELECT 1 FROM Kho WHERE maCoSo=? AND isDeleted=0)";
  try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){
   ps.setString(1,drug.equivalentCode());ps.setString(2,drug.group());ps.setString(3,drug.concentration());ps.setString(4,drug.volume());ps.setObject(5,drug.insured(),Types.BIT);ps.setString(6,drug.traditionalCode());ps.setString(7,xml);ps.setInt(8,employeeId);ps.setInt(9,itemId);ps.setInt(10,facilityId);
   if(ps.executeUpdate()!=1)throw new IllegalStateException("Không thể cập nhật biểu mẫu thuốc.");
  }catch(SQLException ex){throw new IllegalStateException("Không thể lưu biểu mẫu thuốc.",ex);}
 }

 @Override
 public Map<String,String> getDrugFullValues(int itemId,int facilityId){
  String sql="SELECT maItem,tenItem,hoatChat,hamLuong,donViCoBan,quyCachDongGoi,giaNhapMacDinh,giaBanMacDinh,"
   +"tonToiThieu,nuocSanXuat,hangSanXuat,soDangKy,hoatDong,maTuongDuong,nhomThuoc,nongDo,theTich,thuocBHYT,maDanhMucYHCT,thongTinThuoc "
   +"FROM Item WHERE id=? AND isDeleted=0 AND EXISTS(SELECT 1 FROM Kho WHERE maCoSo=? AND isDeleted=0)";
  try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){
   ps.setInt(1,itemId);ps.setInt(2,facilityId);
   try(var rs=ps.executeQuery()){
    if(!rs.next())return Map.of();
    Map<String,String> result=new LinkedHashMap<>(decodeDrugForm(rs.getString("thongTinThuoc")));
    result.put("maThuoc",text(rs,"maItem"));
    result.put("tenThuoc",text(rs,"tenItem"));
    result.put("tenHoatChat",text(rs,"hoatChat"));
    result.put("hamLuong",text(rs,"hamLuong"));
    result.put("donViTinh",text(rs,"donViCoBan"));
    result.put("quyCach",text(rs,"quyCachDongGoi"));
    result.put("giaMua",money(rs,"giaNhapMacDinh").stripTrailingZeros().toPlainString());
    result.put("giaBan",money(rs,"giaBanMacDinh").stripTrailingZeros().toPlainString());
    result.put("canhBaoTon",money(rs,"tonToiThieu").stripTrailingZeros().toPlainString());
    result.put("nuocSanXuat",text(rs,"nuocSanXuat"));
    result.put("hangSanXuat",text(rs,"hangSanXuat"));
    result.put("soDangKy",text(rs,"soDangKy"));
    result.put("khoa",Boolean.toString(!rs.getBoolean("hoatDong")));
    result.put("maTuongDuong",text(rs,"maTuongDuong"));
    result.put("loaiDichVu",text(rs,"nhomThuoc"));
    result.put("nongDo",text(rs,"nongDo"));
    result.put("theTich",text(rs,"theTich"));
    Boolean ins=(Boolean)rs.getObject("thuocBHYT");
    result.put("nhomBHYT",ins==null?"Chưa xác định":ins?"Trong danh mục BHYT":"Ngoài danh mục BHYT");
    result.put("maDanhMucYHCT",text(rs,"maDanhMucYHCT"));
    return Map.copyOf(result);
   }
  }catch(SQLException|java.io.IOException ex){throw new IllegalStateException("Không thể tải thông tin thuốc.",ex);}
 }

 @Override
 public void setDrugActive(int itemId,boolean active,int facilityId,int employeeId){
  String sql="UPDATE Item SET hoatDong=?,updatedBy=?,updatedAt=SYSUTCDATETIME(),version=version+1 "
   +"WHERE id=? AND isDeleted=0 AND loaiItem='THUOC' AND EXISTS(SELECT 1 FROM Kho WHERE maCoSo=? AND isDeleted=0)";
  try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){
   ps.setBoolean(1,active);ps.setInt(2,employeeId);ps.setInt(3,itemId);ps.setInt(4,facilityId);
   if(ps.executeUpdate()!=1)throw new IllegalStateException("Không thể cập nhật trạng thái hoạt động của thuốc.");
  }catch(SQLException ex){throw new IllegalStateException("Không thể cập nhật trạng thái thuốc.",ex);}
 }

 @Override
 public List<TheKhoDong> getStockCard(int itemId,int facilityId,Integer warehouseId){
  String sql="SELECT TOP 200 tx.thoiGian,k.tenKho,ISNULL(t.soLo,N'') soLo,tx.loaiGiaoDich,"
   +"tx.tonTruocKhiThayDoi,tx.soLuongThayDoi,tx.tonSauKhiThayDoi,ISNULL(tx.maChungTu,N'') maChungTu,"
   +"LTRIM(RTRIM(CONCAT(ISNULL(nv.ho,N''),N' ',ISNULL(nv.ten,N'')))) actor "
   +"FROM InventoryTransaction tx JOIN Kho k ON k.id=tx.maKho "
   +"LEFT JOIN TonKho t ON t.id=tx.maTonKho LEFT JOIN NhanVien nv ON nv.id=tx.createdBy "
   +"WHERE tx.maItem=? AND k.maCoSo=? AND tx.isDeleted=0 AND (? IS NULL OR k.id=?) "
   +"ORDER BY tx.thoiGian DESC,tx.id DESC";
  try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){
   ps.setInt(1,itemId);ps.setInt(2,facilityId);ps.setObject(3,warehouseId,Types.INTEGER);ps.setObject(4,warehouseId,Types.INTEGER);
   List<TheKhoDong> rows=new ArrayList<>();
   try(var rs=ps.executeQuery()){while(rs.next()){
    rows.add(new TheKhoDong(time(rs,"thoiGian"),text(rs,"tenKho"),text(rs,"soLo"),text(rs,"loaiGiaoDich"),
     money(rs,"tonTruocKhiThayDoi"),money(rs,"soLuongThayDoi"),money(rs,"tonSauKhiThayDoi"),
     text(rs,"maChungTu"),text(rs,"actor")));
   }}
   return List.copyOf(rows);
  }catch(SQLException ex){throw new IllegalStateException("Không thể tải thẻ kho từ CSDL.",ex);}
 }
 public Map<String,String> getDrugForm(int itemId,int facilityId){
  String sql="SELECT thongTinThuoc FROM Item WHERE id=? AND isDeleted=0 AND EXISTS(SELECT 1 FROM Kho WHERE maCoSo=? AND isDeleted=0)";
  try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){
   ps.setInt(1,itemId);ps.setInt(2,facilityId);try(var rs=ps.executeQuery()){
    return rs.next()?decodeDrugForm(rs.getString(1)):Map.of();
   }
  }catch(SQLException|java.io.IOException ex){throw new IllegalStateException("Không thể tải biểu mẫu thuốc.",ex);}
 }
 @Override
 public List<DonThuocChoCap> getPrescriptions(int facilityId,LocalDateTime from,LocalDateTime to){
  return getPrescriptions(facilityId,from,to,NgayLoc.NGAY_NHAP_XUAT);
 }
 @Override
 public List<DonThuocChoCap> getPrescriptions(int facilityId,LocalDateTime from,LocalDateTime to,NgayLoc dateField){
  NgayLoc mode=dateField==null?NgayLoc.TONG_HOP:dateField;
  String dateFilter=switch(mode){
   case NGAY_TAO -> "d.createdAt>=? AND d.createdAt<?";
   case NGAY_NHAP_XUAT -> "d.ngayKe>=? AND d.ngayKe<?";
   case TONG_HOP -> "((d.ngayKe>=? AND d.ngayKe<?) OR (d.createdAt>=? AND d.createdAt<?))";
  };
  String sql="SELECT d.id,CONCAT('DT-',d.id) code,d.trangThai,d.createdAt,d.ngayKe,d.maKhoXuat,"
   +"ISNULL(k.tenKho,N'Kho Thuốc Ngoại Trú') warehouse,"
   +"LTRIM(RTRIM(CONCAT(ISNULL(bn.ho,N''),N' ',ISNULL(bn.ten,N'')))) patientName,"
   +"LTRIM(RTRIM(CONCAT(ISNULL(bs.ho,N''),N' ',ISNULL(bs.ten,N'')))) doctorName,"
   +"CONCAT('LK-',d.maLuotKham) visitCode,"
   +"ISNULL((SELECT SUM(c.soLuongKe*c.donGiaKe) FROM ChiTietDonThuoc c WHERE c.maDonThuoc=d.id AND c.isDeleted=0),0) totalAmount,"
   +"CASE WHEN EXISTS(SELECT 1 FROM HoaDon h WHERE h.maLuotKham=d.maLuotKham AND h.isDeleted=0 AND h.trangThai<>'DA_HUY' AND h.daThanhToan>=h.soTienBenhNhanChiTra) THEN 1 ELSE 0 END isPaid "
   +"FROM DonThuoc d JOIN LuotKham lk ON lk.id=d.maLuotKham JOIN BenhNhan bn ON bn.id=lk.maBenhNhan "
   +"LEFT JOIN NhanVien bs ON bs.id=d.maBacSi LEFT JOIN Kho k ON k.id=d.maKhoXuat "
   +"WHERE d.maCoSo=? AND d.isDeleted=0 AND "+dateFilter+" "
   +"AND NOT EXISTS(SELECT 1 FROM PhieuXuatKho px WHERE px.maDonThuoc=d.id AND px.isDeleted=0) "
   +"ORDER BY d.ngayKe DESC,d.id DESC";
  try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){
   Timestamp f=Timestamp.valueOf(from),t=Timestamp.valueOf(to);
   ps.setInt(1,facilityId);ps.setTimestamp(2,f);ps.setTimestamp(3,t);
   if(mode==NgayLoc.TONG_HOP){ps.setTimestamp(4,f);ps.setTimestamp(5,t);}
   List<DonThuocChoCap> rows=new ArrayList<>();
   try(var rs=ps.executeQuery()){while(rs.next()){
    rows.add(new DonThuocChoCap(rs.getInt("id"),rs.getString("code"),rs.getString("trangThai"),
     time(rs,"createdAt"),time(rs,"ngayKe"),(Integer)rs.getObject("maKhoXuat"),
     text(rs,"warehouse"),text(rs,"patientName"),text(rs,"doctorName"),
     text(rs,"visitCode"),rs.getBigDecimal("totalAmount"),rs.getInt("isPaid")==1));
   }}
   return List.copyOf(rows);
  }catch(SQLException ex){throw new IllegalStateException("Không thể tải đơn thuốc chờ cấp phát từ CSDL.",ex);}
 }

 @Override
 public List<PhieuChiTietDong> getTicketDetails(String ticketType,int ticketId,int facilityId){
  if(ticketId<=0 || ticketType==null)return List.of();
  String sql=switch(ticketType){
   case "NHAP_KHO" ->
    "SELECT i.maItem,i.tenItem,i.donViCoBan,c.soLo,c.hanSuDung,c.soLuong,c.giaNhap price,(c.soLuong*c.giaNhap) amount "
    +"FROM ChiTietNhapKho c JOIN PhieuNhapKho p ON p.id=c.maPhieuNhap JOIN Kho k ON k.id=p.maKho JOIN Item i ON i.id=c.maItem "
    +"WHERE p.id=? AND k.maCoSo=? AND c.isDeleted=0 ORDER BY c.id";
   case "XUAT_KHO" ->
    "SELECT i.maItem,i.tenItem,i.donViCoBan,ISNULL(t.soLo,N'') soLo,t.hanSuDung,c.soLuong,c.donGia price,"
    +"ISNULL(c.thanhTienBanLe,c.soLuong*c.donGia) amount "
    +"FROM ChiTietXuatKho c JOIN PhieuXuatKho p ON p.id=c.maPhieuXuat JOIN Kho k ON k.id=p.maKhoXuat "
    +"JOIN Item i ON i.id=c.maItem LEFT JOIN TonKho t ON t.id=c.maTonKho "
    +"WHERE p.id=? AND k.maCoSo=? AND c.isDeleted=0 ORDER BY c.id";
   case "DON_THUOC" ->
    "SELECT i.maItem,i.tenItem,i.donViCoBan,N'' soLo,CAST(NULL AS DATE) hanSuDung,c.soLuongKe soLuong,c.donGiaKe price,"
    +"(c.soLuongKe*c.donGiaKe) amount "
    +"FROM ChiTietDonThuoc c JOIN DonThuoc d ON d.id=c.maDonThuoc JOIN Item i ON i.id=c.maItem "
    +"WHERE d.id=? AND d.maCoSo=? AND c.isDeleted=0 ORDER BY c.id";
   default -> null;
  };
  if(sql==null)return List.of();
  try(var lease=connections.acquire();var ps=lease.connection().prepareStatement(sql)){
   ps.setInt(1,ticketId);ps.setInt(2,facilityId);
   List<PhieuChiTietDong> rows=new ArrayList<>();
   try(var rs=ps.executeQuery()){while(rs.next()){
    var exp=rs.getDate("hanSuDung");
    rows.add(new PhieuChiTietDong(text(rs,"maItem"),text(rs,"tenItem"),text(rs,"donViCoBan"),
     text(rs,"soLo"),exp==null?null:exp.toLocalDate(),rs.getBigDecimal("soLuong"),
     rs.getBigDecimal("price"),rs.getBigDecimal("amount")));
   }}
   return List.copyOf(rows);
  }catch(SQLException ex){throw new IllegalStateException("Không thể tải chi tiết phiếu từ CSDL.",ex);}
 }

 @Override
 public Optional<PhieuChiTietDayDu> getTicketFullDetail(String ticketType,int ticketId,int facilityId){
  if(ticketId<=0 || ticketType==null)return Optional.empty();
  try(var lease=connections.acquire()){
   Connection c=lease.connection();
   return switch(ticketType){
    case "DON_THUOC" -> loadPrescriptionFullDetail(c,ticketId,facilityId);
    case "XUAT_KHO" -> loadIssueFullDetail(c,ticketId,facilityId);
    case "NHAP_KHO" -> loadReceiptFullDetail(c,ticketId,facilityId);
    default -> Optional.empty();
   };
  }catch(SQLException ex){throw new IllegalStateException("Không thể tải đầy đủ chi tiết phiếu từ CSDL.",ex);}
 }

 private Optional<PhieuChiTietDayDu> loadPrescriptionFullDetail(Connection c,int id,int facilityId)throws SQLException{
  String hSql="SELECT d.id,CONCAT('DT-',d.id) code,d.trangThai,d.createdAt,d.ngayKe,d.ngayCapThuoc,"
   +"ISNULL(d.maKhoXuat,1) whId,ISNULL(k.tenKho,N'Kho Thuốc Ngoại Trú') warehouse,"
   +"LTRIM(RTRIM(CONCAT(ISNULL(bn.ho,N''),N' ',ISNULL(bn.ten,N'')))) patientName,"
   +"LTRIM(RTRIM(CONCAT(ISNULL(bs.ho,N''),N' ',ISNULL(bs.ten,N'')))) doctorName,"
   +"LTRIM(RTRIM(CONCAT(ISNULL(nc.ho,N''),N' ',ISNULL(nc.ten,N'')))) dispenserName,"
   +"(SELECT TOP 1 h.daThanhToan FROM HoaDon h WHERE h.maLuotKham=d.maLuotKham AND h.isDeleted=0 ORDER BY h.id DESC) paidAmt,"
   +"(SELECT TOP 1 h.soTienBenhNhanChiTra FROM HoaDon h WHERE h.maLuotKham=d.maLuotKham AND h.isDeleted=0 ORDER BY h.id DESC) copayAmt,"
   +"(SELECT TOP 1 pt.ngayThu FROM HoaDon h JOIN PhieuThu pt ON pt.maHoaDon=h.id WHERE h.maLuotKham=d.maLuotKham AND h.isDeleted=0 AND pt.isDeleted=0 ORDER BY pt.id DESC) paidAt,"
   +"(SELECT TOP 1 LTRIM(RTRIM(CONCAT(ISNULL(nv.ho,N''),N' ',ISNULL(nv.ten,N'')))) FROM HoaDon h JOIN PhieuThu pt ON pt.maHoaDon=h.id JOIN NhanVien nv ON nv.id=pt.nguoiThu WHERE h.maLuotKham=d.maLuotKham AND h.isDeleted=0 AND pt.isDeleted=0 ORDER BY pt.id DESC) paidBy "
   +"FROM DonThuoc d JOIN LuotKham lk ON lk.id=d.maLuotKham JOIN BenhNhan bn ON bn.id=lk.maBenhNhan "
   +"LEFT JOIN NhanVien bs ON bs.id=d.maBacSi LEFT JOIN NhanVien nc ON nc.id=d.maNguoiCapThuoc "
   +"LEFT JOIN Kho k ON k.id=d.maKhoXuat WHERE d.id=? AND d.maCoSo=? AND d.isDeleted=0";
  try(var ps=c.prepareStatement(hSql)){
   ps.setInt(1,id);ps.setInt(2,facilityId);
   try(var rs=ps.executeQuery()){
    if(!rs.next())return Optional.empty();
    int whId=rs.getInt("whId");
    String lSql="SELECT i.maItem,i.tenItem,ISNULL(i.hoatChat,N'') hoatChat,i.donViCoBan,"
     +"ISNULL((SELECT SUM(t.soLuongTon) FROM TonKho t WHERE t.maKho=? AND t.maItem=c.maItem AND t.isDeleted=0 AND t.hanSuDung>=CAST(GETDATE() AS DATE)),0) stock,"
     +"c.soLuongKe,c.donGiaKe,(c.soLuongKe*c.donGiaKe) amt,ISNULL(i.soDangKy,N'') soDangKy "
     +"FROM ChiTietDonThuoc c JOIN Item i ON i.id=c.maItem WHERE c.maDonThuoc=? AND c.isDeleted=0 ORDER BY c.id";
    List<PhieuChiTietDongDayDu> lines=new ArrayList<>();
    java.math.BigDecimal total=java.math.BigDecimal.ZERO;
    try(var lps=c.prepareStatement(lSql)){
     lps.setInt(1,whId);lps.setInt(2,id);
     try(var lrs=lps.executeQuery()){while(lrs.next()){
      var amt=money(lrs,"amt");total=total.add(amt);
      lines.add(new PhieuChiTietDongDayDu(text(lrs,"maItem"),text(lrs,"tenItem"),text(lrs,"hoatChat"),
       text(lrs,"donViCoBan"),money(lrs,"stock"),money(lrs,"soLuongKe"),money(lrs,"soLuongKe"),
       money(lrs,"donGiaKe"),amt,text(lrs,"soDangKy"),"",null));
     }}
    }
    var paid=money(rs,"paidAmt");
    var copay=rs.getBigDecimal("copayAmt");
    var debt=copay!=null?copay.subtract(paid).max(java.math.BigDecimal.ZERO):total.subtract(paid).max(java.math.BigDecimal.ZERO);
    String st=text(rs,"trangThai");
    LocalDateTime created=time(rs,"ngayKe")!=null?time(rs,"ngayKe"):time(rs,"createdAt");
    LocalDateTime approved="DA_DUYET".equals(st)||"DA_XUAT_HET".equals(st)?created:null;
    String approvedBy=approved!=null?text(rs,"doctorName"):"";
    return Optional.of(new PhieuChiTietDayDu(text(rs,"code"),text(rs,"patientName"),st,created,
     text(rs,"doctorName"),approved,approvedBy,time(rs,"paidAt"),text(rs,"paidBy"),
     time(rs,"ngayCapThuoc"),text(rs,"dispenserName"),text(rs,"warehouse"),total,paid,debt,lines));
   }
  }
 }

 private Optional<PhieuChiTietDayDu> loadIssueFullDetail(Connection c,int id,int facilityId)throws SQLException{
  String hSql="SELECT p.id,p.soPhieuXuat code,CASE WHEN p.isDeleted=1 THEN 'DA_HUY' ELSE p.trangThaiNoiBo END status,"
   +"p.createdAt,p.ngayXuat,p.tongTien,k.id whId,k.tenKho warehouse,p.maDonThuoc,ISNULL(p.tenKhachLe,N'') guestName,"
   +"LTRIM(RTRIM(CONCAT(ISNULL(nv.ho,N''),N' ',ISNULL(nv.ten,N'')))) actorName "
   +"FROM PhieuXuatKho p JOIN Kho k ON k.id=p.maKhoXuat LEFT JOIN NhanVien nv ON nv.id=p.createdBy "
   +"WHERE p.id=? AND k.maCoSo=?";
  try(var ps=c.prepareStatement(hSql)){
   ps.setInt(1,id);ps.setInt(2,facilityId);
   try(var rs=ps.executeQuery()){
    if(!rs.next())return Optional.empty();
    int whId=rs.getInt("whId");
    String lSql="SELECT i.maItem,i.tenItem,ISNULL(i.hoatChat,N'') hoatChat,i.donViCoBan,"
     +"ISNULL((SELECT SUM(tk.soLuongTon) FROM TonKho tk WHERE tk.maKho=? AND tk.maItem=c.maItem AND tk.isDeleted=0 AND tk.hanSuDung>=CAST(GETDATE() AS DATE)),0) stock,"
     +"c.soLuong,c.donGia,ISNULL(c.thanhTienBanLe,c.soLuong*c.donGia) amt,ISNULL(i.soDangKy,N'') soDangKy,ISNULL(t.soLo,N'') soLo,t.hanSuDung "
     +"FROM ChiTietXuatKho c JOIN Item i ON i.id=c.maItem LEFT JOIN TonKho t ON t.id=c.maTonKho WHERE c.maPhieuXuat=? AND c.isDeleted=0 ORDER BY c.id";
    List<PhieuChiTietDongDayDu> lines=new ArrayList<>();
    try(var lps=c.prepareStatement(lSql)){
     lps.setInt(1,whId);lps.setInt(2,id);
     try(var lrs=lps.executeQuery()){while(lrs.next()){
      var exp=lrs.getDate("hanSuDung");
      lines.add(new PhieuChiTietDongDayDu(text(lrs,"maItem"),text(lrs,"tenItem"),text(lrs,"hoatChat"),
       text(lrs,"donViCoBan"),money(lrs,"stock"),money(lrs,"soLuong"),money(lrs,"soLuong"),
       money(lrs,"donGia"),money(lrs,"amt"),text(lrs,"soDangKy"),text(lrs,"soLo"),exp==null?null:exp.toLocalDate()));
     }}
    }
    var total=money(rs,"tongTien");
    String target=text(rs,"guestName");
    if(target.isBlank() && rs.getObject("maDonThuoc")!=null)target="Đơn DT-"+rs.getInt("maDonThuoc");
    if(target.isBlank())target="Xuất kho";
    var created=time(rs,"createdAt");var issued=time(rs,"ngayXuat");
    return Optional.of(new PhieuChiTietDayDu(text(rs,"code"),target,text(rs,"status"),created,text(rs,"actorName"),
     created,text(rs,"actorName"),issued,text(rs,"actorName"),issued,text(rs,"actorName"),
     text(rs,"warehouse"),total,total,java.math.BigDecimal.ZERO,lines));
   }
  }
 }

 private Optional<PhieuChiTietDayDu> loadReceiptFullDetail(Connection c,int id,int facilityId)throws SQLException{
  String hSql="SELECT p.id,p.soPhieuNhap code,CASE WHEN p.isDeleted=1 THEN 'DA_HUY' ELSE p.trangThai END status,"
   +"p.createdAt,p.ngayNhap,p.tongTien,k.id whId,k.tenKho warehouse,ISNULL(ncc.tenNhaCungCap,N'') nccName,"
   +"LTRIM(RTRIM(CONCAT(ISNULL(nv.ho,N''),N' ',ISNULL(nv.ten,N'')))) actorName "
   +"FROM PhieuNhapKho p JOIN Kho k ON k.id=p.maKho LEFT JOIN NhaCungCap ncc ON ncc.id=p.maNhaCungCap "
   +"LEFT JOIN NhanVien nv ON nv.id=p.createdBy WHERE p.id=? AND k.maCoSo=?";
  try(var ps=c.prepareStatement(hSql)){
   ps.setInt(1,id);ps.setInt(2,facilityId);
   try(var rs=ps.executeQuery()){
    if(!rs.next())return Optional.empty();
    int whId=rs.getInt("whId");
    String lSql="SELECT i.maItem,i.tenItem,ISNULL(i.hoatChat,N'') hoatChat,i.donViCoBan,"
     +"ISNULL((SELECT SUM(tk.soLuongTon) FROM TonKho tk WHERE tk.maKho=? AND tk.maItem=c.maItem AND tk.isDeleted=0 AND tk.hanSuDung>=CAST(GETDATE() AS DATE)),0) stock,"
     +"c.soLuong,c.giaNhap,(c.soLuong*c.giaNhap) amt,ISNULL(i.soDangKy,N'') soDangKy,c.soLo,c.hanSuDung "
     +"FROM ChiTietNhapKho c JOIN Item i ON i.id=c.maItem WHERE c.maPhieuNhap=? AND c.isDeleted=0 ORDER BY c.id";
    List<PhieuChiTietDongDayDu> lines=new ArrayList<>();
    try(var lps=c.prepareStatement(lSql)){
     lps.setInt(1,whId);lps.setInt(2,id);
     try(var lrs=lps.executeQuery()){while(lrs.next()){
      var exp=lrs.getDate("hanSuDung");
      lines.add(new PhieuChiTietDongDayDu(text(lrs,"maItem"),text(lrs,"tenItem"),text(lrs,"hoatChat"),
       text(lrs,"donViCoBan"),money(lrs,"stock"),money(lrs,"soLuong"),money(lrs,"soLuong"),
       money(lrs,"giaNhap"),money(lrs,"amt"),text(lrs,"soDangKy"),text(lrs,"soLo"),exp==null?null:exp.toLocalDate()));
     }}
    }
    var total=money(rs,"tongTien");
    var created=time(rs,"createdAt");var imp=time(rs,"ngayNhap");
    return Optional.of(new PhieuChiTietDayDu(text(rs,"code"),text(rs,"nccName"),text(rs,"status"),created,
     text(rs,"actorName"),imp,text(rs,"actorName"),null,"",imp,text(rs,"actorName"),
     text(rs,"warehouse"),total,java.math.BigDecimal.ZERO,total,lines));
   }
  }
 }

 private static java.math.BigDecimal money(ResultSet rs,String col)throws SQLException{
  var v=rs.getBigDecimal(col);return v!=null?v:java.math.BigDecimal.ZERO;
 }

 static Map<String,String> decodeDrugForm(String xml) throws java.io.IOException {
  if(xml==null || xml.isBlank())return Map.of();
  Properties values=new Properties();
  try(var input=new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8))){values.loadFromXML(input);}
  Map<String,String> result=new HashMap<>();for(String key:values.stringPropertyNames())result.put(key,values.getProperty(key));
  return Map.copyOf(result);
 }
 private static String text(ResultSet rs,String field)throws SQLException{return Objects.toString(rs.getString(field),"");}
 private static LocalDateTime time(ResultSet rs,String col)throws SQLException{var value=rs.getTimestamp(col);return value==null?null:value.toLocalDateTime();}
}
