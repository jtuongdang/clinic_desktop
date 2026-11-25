package vn.clinicmanagement.application.inventory.create;

import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.*;
import vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi;
import vn.clinicmanagement.domain.common.enums.LoaiMatHang;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.repositories.IKhoRepository;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.*;
import java.util.*;

/** Tạo thuốc, metadata, biểu mẫu và audit cùng một giao dịch. Không tạo tồn kho từ số lô nhập trên danh mục. */
public final class CreateThuocDayDuHandler {
    private final IKhoRepository inventory;
    private final NhaThuocDanhSachReadCongKetNoi details;
    private final GiaoDichRanhGioi transactions;
    private final KiemToanWriteCongKetNoi audit;
    public CreateThuocDayDuHandler(IKhoRepository inventory,NhaThuocDanhSachReadCongKetNoi details,GiaoDichRanhGioi transactions,KiemToanWriteCongKetNoi audit){
        this.inventory=Objects.requireNonNull(inventory);this.details=Objects.requireNonNull(details);this.transactions=Objects.requireNonNull(transactions);this.audit=Objects.requireNonNull(audit);
    }
    private static final Set<String> MONEY=Set.of("giaBHYT","giaVienPhi","giaYeuCau","giaNuocNgoai","giaMua","giaBan");
    private static final Set<String> PERCENT=Set.of("tyLeDungTuyen","tyLeTraiTuyen","vatMua","vatBan");
    private static final Set<String> WARNINGS=Set.of("canhBaoHSD","canhBaoTon","canhBaoKhaDung","canhBaoNgay");
    public KetQua<CreateMatHangBienNhan> handle(Map<String,String> input,PhanQuyenNguCanh auth){
        if(auth==null||!auth.isValid()||!Set.of("ADMIN","PHARMACIST").contains(auth.roleCode()))return KetQua.failure("AUTH_ACCESS_DENIED","Không có quyền thêm thuốc trong phiên hiện tại.");
        if(input==null)return KetQua.failure("VALIDATION_ERROR","Biểu mẫu thuốc không được để trống.");
        Map<String,String> fields=new LinkedHashMap<>();input.forEach((key,value)->fields.put(key,Objects.toString(value,"").trim()));
        if(fields.getOrDefault("maThuoc","").isBlank()||fields.getOrDefault("tenThuoc","").isBlank()||fields.getOrDefault("donViTinh","").isBlank())return KetQua.failure("VALIDATION_ERROR","Vui lòng nhập mã thuốc, tên thuốc và đơn vị tính.");
        if(fields.get("maThuoc").length()>50||fields.get("tenThuoc").length()>255||fields.get("donViTinh").length()>50)return KetQua.failure("VALIDATION_ERROR","Mã thuốc tối đa 50 ký tự, tên thuốc 255 ký tự, đơn vị tính 50 ký tự.");
        for(String key:List.of("hamLuong","nongDo","theTich","loaiDichVu","maThuocBHYT","maDuocQuocGia","nuocSanXuat"))if(fields.getOrDefault(key,"").length()>100)return KetQua.failure("VALIDATION_ERROR","Thông tin mã/phân loại/hàm lượng tối đa 100 ký tự.");
        if(fields.getOrDefault("soDangKy","").length()>50||fields.getOrDefault("tenHoatChat","").length()>255||fields.getOrDefault("quyCach","").length()>255||fields.getOrDefault("hangSanXuat","").length()>255)return KetQua.failure("VALIDATION_ERROR","Số đăng ký tối đa 50 ký tự; hoạt chất, quy cách, hãng sản xuất tối đa 255 ký tự.");
        for(var value:fields.values())if(value.length()>2000)return KetQua.failure("VALIDATION_ERROR","Nội dung một trường không được vượt quá 2000 ký tự.");
        try{
            for(String key:MONEY){var value=decimal(fields,key);if(value.signum()<0||value.precision()-value.scale()>16||value.stripTrailingZeros().scale()>2)throw new IllegalArgumentException("Giá tiền không hợp lệ: "+key);}
            for(String key:PERCENT){var value=decimal(fields,key);if(value.signum()<0||value.compareTo(new BigDecimal("100"))>0)throw new IllegalArgumentException("Tỷ lệ và VAT phải từ 0 đến 100%.");}
            for(String key:WARNINGS){var value=decimal(fields,key);if(value.signum()<0||value.stripTrailingZeros().scale()>0||value.compareTo(new BigDecimal(Integer.MAX_VALUE))>0)throw new IllegalArgumentException("Cảnh báo phải là số nguyên không âm.");}
            if(!fields.getOrDefault("hanSuDung","").isBlank())LocalDate.parse(fields.get("hanSuDung"),DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT));
        }catch(DateTimeException ex){return KetQua.failure("VALIDATION_ERROR","Hạn sử dụng phải có dạng dd/MM/yyyy và là ngày hợp lệ.");}
        catch(IllegalArgumentException ex){return KetQua.failure("VALIDATION_ERROR","Giá tiền, tỷ lệ hoặc mức cảnh báo không hợp lệ. Dùng số, dấu chấm cho phần thập phân; tỷ lệ từ 0 đến 100%.");}
        return transactions.inTransaction(()->{
            if(inventory.getItemByCode(fields.get("maThuoc")).isPresent())return KetQua.failure("DUPLICATE_CODE","Mã thuốc đã tồn tại. Vui lòng đổi mã thuốc.");
            MatHang item=new MatHang();item.setMaItem(fields.get("maThuoc"));item.setTenItem(fields.get("tenThuoc"));item.setLoaiItem(LoaiMatHang.THUOC);
            item.setHoatChat(fields.getOrDefault("tenHoatChat",""));item.setHamLuong(fields.getOrDefault("hamLuong",""));item.setDonViCoBan(fields.get("donViTinh"));
            item.setQuyCachDongGoi(fields.getOrDefault("quyCach",""));item.setGiaNhapMacDinh(decimal(fields,"giaMua"));item.setGiaBanMacDinh(decimal(fields,"giaBan"));item.setTonToiThieu(decimal(fields,"canhBaoTon"));item.setTonToiDa(new BigDecimal("1000000"));
            item.setDangBaoChe(fields.getOrDefault("loaiDichVu",""));item.setNuocSanXuat(fields.getOrDefault("nuocSanXuat",""));item.setHangSanXuat(fields.getOrDefault("hangSanXuat",""));item.setSoDangKy(fields.getOrDefault("soDangKy",""));item.setHoatDong(!Boolean.parseBoolean(fields.getOrDefault("khoa","false")));item.setCreatedBy(auth.employeeId());inventory.saveItem(item);
            String insurance=fields.getOrDefault("nhomBHYT","");Boolean insured="Trong danh mục BHYT".equals(insurance)?true:"Ngoài danh mục BHYT".equals(insurance)?false:null;
            var metadata=new NhaThuocDanhSachReadCongKetNoi.Thuoc(item.getId(),item.getVersion(),item.getMaItem(),fields.getOrDefault("maTuongDuong",""),item.getTenItem(),item.getHoatChat(),item.getHamLuong(),fields.getOrDefault("nongDo",""),fields.getOrDefault("theTich",""),item.getDonViCoBan(),fields.getOrDefault("loaiDichVu",""),insured,fields.getOrDefault("maDanhMucYHCT",""),"","",null,BigDecimal.ZERO);
            details.saveDrugForm(metadata,fields,auth.facilityId(),auth.employeeId());
            audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(auth.employeeId(),auth.roleId(),auth.roleCode(),auth.facilityId(),auth.departmentId(),KiemToanHanhDongMa.INV_ITEM_CREATED,KiemToanTaiNguyenLoai.ITEM,item.getId(),KiemToanKetQua.SUCCESS,null,Instant.now(),null,UUID.randomUUID()));
            return KetQua.success(new CreateMatHangBienNhan(item.getId(),item.getMaItem(),item.getTenItem()));
        });
    }

    /** Cập nhật toàn bộ biểu mẫu thuốc và metadata trong cùng một giao dịch. */
    public KetQua<CreateMatHangBienNhan> handleUpdate(int itemId,Map<String,String> input,PhanQuyenNguCanh auth){
        if(itemId<=0)return KetQua.failure("VALIDATION_ERROR","Mã định danh thuốc không hợp lệ.");
        if(auth==null||!auth.isValid()||!Set.of("ADMIN","PHARMACIST").contains(auth.roleCode()))return KetQua.failure("AUTH_ACCESS_DENIED","Không có quyền sửa thuốc trong phiên hiện tại.");
        if(input==null)return KetQua.failure("VALIDATION_ERROR","Biểu mẫu thuốc không được để trống.");
        Map<String,String> fields=new LinkedHashMap<>();input.forEach((key,value)->fields.put(key,Objects.toString(value,"").trim()));
        if(fields.getOrDefault("maThuoc","").isBlank()||fields.getOrDefault("tenThuoc","").isBlank()||fields.getOrDefault("donViTinh","").isBlank())return KetQua.failure("VALIDATION_ERROR","Vui lòng nhập mã thuốc, tên thuốc và đơn vị tính.");
        if(fields.get("maThuoc").length()>50||fields.get("tenThuoc").length()>255||fields.get("donViTinh").length()>50)return KetQua.failure("VALIDATION_ERROR","Mã thuốc tối đa 50 ký tự, tên thuốc 255 ký tự, đơn vị tính 50 ký tự.");
        for(String key:List.of("hamLuong","nongDo","theTich","loaiDichVu","maThuocBHYT","maDuocQuocGia","nuocSanXuat"))if(fields.getOrDefault(key,"").length()>100)return KetQua.failure("VALIDATION_ERROR","Thông tin mã/phân loại/hàm lượng tối đa 100 ký tự.");
        if(fields.getOrDefault("soDangKy","").length()>50||fields.getOrDefault("tenHoatChat","").length()>255||fields.getOrDefault("quyCach","").length()>255||fields.getOrDefault("hangSanXuat","").length()>255)return KetQua.failure("VALIDATION_ERROR","Số đăng ký tối đa 50 ký tự; hoạt chất, quy cách, hãng sản xuất tối đa 255 ký tự.");
        for(var value:fields.values())if(value.length()>2000)return KetQua.failure("VALIDATION_ERROR","Nội dung một trường không được vượt quá 2000 ký tự.");
        try{
            for(String key:MONEY){var value=decimal(fields,key);if(value.signum()<0||value.precision()-value.scale()>16||value.stripTrailingZeros().scale()>2)throw new IllegalArgumentException("Giá tiền không hợp lệ: "+key);}
            for(String key:PERCENT){var value=decimal(fields,key);if(value.signum()<0||value.compareTo(new BigDecimal("100"))>0)throw new IllegalArgumentException("Tỷ lệ và VAT phải từ 0 đến 100%.");}
            for(String key:WARNINGS){var value=decimal(fields,key);if(value.signum()<0||value.stripTrailingZeros().scale()>0||value.compareTo(new BigDecimal(Integer.MAX_VALUE))>0)throw new IllegalArgumentException("Cảnh báo phải là số nguyên không âm.");}
            if(!fields.getOrDefault("hanSuDung","").isBlank())LocalDate.parse(fields.get("hanSuDung"),DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT));
        }catch(DateTimeException ex){return KetQua.failure("VALIDATION_ERROR","Hạn sử dụng phải có dạng dd/MM/yyyy và là ngày hợp lệ.");}
        catch(IllegalArgumentException ex){return KetQua.failure("VALIDATION_ERROR","Giá tiền, tỷ lệ hoặc mức cảnh báo không hợp lệ.");}
        return transactions.inTransaction(()->{
            var existingOpt=inventory.getItemById(itemId);
            if(existingOpt.isEmpty())return KetQua.failure("NOT_FOUND","Không tìm thấy thuốc để cập nhật.");
            MatHang item=existingOpt.get();
            String newCode=fields.get("maThuoc");
            if(!newCode.equalsIgnoreCase(item.getMaItem())&&inventory.getItemByCode(newCode).isPresent())return KetQua.failure("DUPLICATE_CODE","Mã thuốc đã tồn tại. Vui lòng đổi mã thuốc.");
            item.setMaItem(newCode);item.setTenItem(fields.get("tenThuoc"));item.setLoaiItem(LoaiMatHang.THUOC);
            item.setHoatChat(fields.getOrDefault("tenHoatChat",""));item.setHamLuong(fields.getOrDefault("hamLuong",""));item.setDonViCoBan(fields.get("donViTinh"));
            item.setQuyCachDongGoi(fields.getOrDefault("quyCach",""));item.setGiaNhapMacDinh(decimal(fields,"giaMua"));item.setGiaBanMacDinh(decimal(fields,"giaBan"));item.setTonToiThieu(decimal(fields,"canhBaoTon"));
            item.setDangBaoChe(fields.getOrDefault("loaiDichVu",""));item.setNuocSanXuat(fields.getOrDefault("nuocSanXuat",""));item.setHangSanXuat(fields.getOrDefault("hangSanXuat",""));item.setSoDangKy(fields.getOrDefault("soDangKy",""));
            item.setHoatDong(!Boolean.parseBoolean(fields.getOrDefault("khoa","false")));item.setUpdatedBy(auth.employeeId());inventory.saveItem(item);
            String insurance=fields.getOrDefault("nhomBHYT","");Boolean insured="Trong danh mục BHYT".equals(insurance)?true:"Ngoài danh mục BHYT".equals(insurance)?false:null;
            var metadata=new NhaThuocDanhSachReadCongKetNoi.Thuoc(item.getId(),item.getVersion(),item.getMaItem(),fields.getOrDefault("maTuongDuong",""),item.getTenItem(),item.getHoatChat(),item.getHamLuong(),fields.getOrDefault("nongDo",""),fields.getOrDefault("theTich",""),item.getDonViCoBan(),fields.getOrDefault("loaiDichVu",""),insured,fields.getOrDefault("maDanhMucYHCT",""),"","",null,BigDecimal.ZERO);
            details.updateDrugForm(item.getId(),metadata,fields,auth.facilityId(),auth.employeeId());
            audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(auth.employeeId(),auth.roleId(),auth.roleCode(),auth.facilityId(),auth.departmentId(),KiemToanHanhDongMa.INV_ITEM_CREATED,KiemToanTaiNguyenLoai.ITEM,item.getId(),KiemToanKetQua.SUCCESS,null,Instant.now(),null,UUID.randomUUID()));
            return KetQua.success(new CreateMatHangBienNhan(item.getId(),item.getMaItem(),item.getTenItem()));
        });
    }
    private static BigDecimal decimal(Map<String,String> fields,String key){String value=fields.getOrDefault(key,"");return value.isBlank()?BigDecimal.ZERO:new BigDecimal(value);}
}
