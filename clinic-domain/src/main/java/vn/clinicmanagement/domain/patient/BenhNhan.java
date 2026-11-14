package vn.clinicmanagement.domain.patient;

import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.ITongHopGoc;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.common.events.BenhNhanDaDangKySuKien;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.billing.HoaDon;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

public class BenhNhan extends CoSoThucThe<Integer> implements ITongHopGoc {
    private String ho = "";
    private String ten = "";
    private LocalDate ngaySinh;
    private GioiTinh gioiTinh = GioiTinh.M;
    private String dienThoai = "";
    private String email;
    private String soCCCD;
    private String diaChi;
    private String nguoiLienHeTen;
    private String nguoiLienHeDienThoai;
    private String username;
    /** Trường `passwordHash`: matkhau `Hash`. */
    private String passwordHash;

    // Chuẩn hồ sơ hành chính Bộ Y Tế & Barcode
    private String maBenhNhanFormat = "";
    private String ngheNghiep = "";
    private String danToc = "Kinh";
    private String quocTich = "Việt Nam";
    private String maTinh;
    private String maHuyen;
    private String maXa;

    // Bổ sung chuẩn đón tiếp chi tiết (EHC Reception Station)
    private String noiSinh = "";
    private String soNhaThonPho = "";
    private LocalDate ngayCapCCCD;
    private String noiCapCCCD = "";
    private String noiLamViec = "";

    private List<BenhNhanCoSo> benhNhanCoSo = new ArrayList<>();
    private List<BaoHiem> baoHiems = new ArrayList<>();
    private List<TienSuBenh> tienSuBenhs = new ArrayList<>();
    private List<LuotKham> luotKhams = new ArrayList<>();
    private List<HoaDon> hoaDons = new ArrayList<>();

    public BenhNhan() {}

    public BenhNhan(String ho, String ten, LocalDate ngaySinh, GioiTinh gioiTinh, String dienThoai, String email, String soCCCD, String diaChi, int createdBy) {
        this.ho = ho;
        this.ten = ten;
        this.ngaySinh = ngaySinh;
        this.gioiTinh = gioiTinh;
        this.dienThoai = dienThoai;
        this.email = email;
        this.soCCCD = soCCCD;
        this.diaChi = diaChi;
        setCreatedBy(createdBy);
        registerDomainEvent(new BenhNhanDaDangKySuKien(getId() != null ? getId() : 0, soCCCD, getHoTen()));
    }

    /** Hàm `getHoTen`: lấy ho `Ten`. */
    public String getHoTen() { return (ho + " " + ten).trim(); }

    /** Hàm `getTuoi`: lấy `Tuoi`. */
    public int getTuoi() {
        if (ngaySinh == null) return 0;
        return Period.between(ngaySinh, LocalDate.now()).getYears();
    }

    /** Hàm `getHo`: lấy ho. */
    public String getHo() { return ho; }
    /** Hàm `setHo`: gán ho. */
    public void setHo(String ho) { this.ho = ho; }
    /** Hàm `getTen`: lấy `Ten`. */
    public String getTen() { return ten; }
    /** Hàm `setTen`: gán `Ten`. */
    public void setTen(String ten) { this.ten = ten; }
    /** Hàm `getNgaySinh`: lấy `Ngay` sinh. */
    public LocalDate getNgaySinh() { return ngaySinh; }
    /** Hàm `setNgaySinh`: gán `Ngay` sinh. */
    public void setNgaySinh(LocalDate ngaySinh) { this.ngaySinh = ngaySinh; }
    /** Hàm `getGioiTinh`: lấy gioi tính. */
    public GioiTinh getGioiTinh() { return gioiTinh; }
    /** Hàm `setGioiTinh`: gán gioi tính. */
    public void setGioiTinh(GioiTinh gioiTinh) { this.gioiTinh = gioiTinh; }
    /** Hàm `getDienThoai`: lấy `Dien` `Thoai`. */
    public String getDienThoai() { return dienThoai; }
    /** Hàm `setDienThoai`: gán `Dien` `Thoai`. */
    public void setDienThoai(String dienThoai) { this.dienThoai = dienThoai; }
    /** Hàm `getEmail`: lấy `Email`. */
    public String getEmail() { return email; }
    /** Hàm `setEmail`: gán `Email`. */
    public void setEmail(String email) { this.email = email; }
    /** Hàm `getSoCCCD`: lấy so `CCCD`. */
    public String getSoCCCD() { return soCCCD; }
    /** Hàm `setSoCCCD`: gán so `CCCD`. */
    public void setSoCCCD(String soCCCD) { this.soCCCD = soCCCD; }
    /** Hàm `getDiaChi`: lấy `Dia` chi. */
    public String getDiaChi() { return diaChi; }
    /** Hàm `setDiaChi`: gán `Dia` chi. */
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    /** Hàm `getNguoiLienHeTen`: lấy `Nguoi` `Lien` he `Ten`. */
    public String getNguoiLienHeTen() { return nguoiLienHeTen; }
    /** Hàm `setNguoiLienHeTen`: gán `Nguoi` `Lien` he `Ten`. */
    public void setNguoiLienHeTen(String nguoiLienHeTen) { this.nguoiLienHeTen = nguoiLienHeTen; }
    /** Hàm `getNguoiLienHeDienThoai`: lấy `Nguoi` `Lien` he `Dien` `Thoai`. */
    public String getNguoiLienHeDienThoai() { return nguoiLienHeDienThoai; }
    /** Hàm `setNguoiLienHeDienThoai`: gán `Nguoi` `Lien` he `Dien` `Thoai`. */
    public void setNguoiLienHeDienThoai(String nguoiLienHeDienThoai) { this.nguoiLienHeDienThoai = nguoiLienHeDienThoai; }
    /** Hàm `getUsername`: lấy `Username`. */
    public String getUsername() { return username; }
    /** Hàm `setUsername`: gán `Username`. */
    public void setUsername(String username) { this.username = username; }
    /** Hàm `getPasswordHash`: lấy matkhau `Hash`. */
    public String getPasswordHash() { return passwordHash; }
    /** Hàm `setPasswordHash`: gán matkhau `Hash`. */
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    /** Hàm `getMaBenhNhanFormat`: lấy mã benh nhãn `Format`. */
    public String getMaBenhNhanFormat() { return maBenhNhanFormat; }
    /** Hàm `setMaBenhNhanFormat`: gán mã benh nhãn `Format`. */
    public void setMaBenhNhanFormat(String maBenhNhanFormat) { this.maBenhNhanFormat = maBenhNhanFormat; }
    /** Hàm `getNgheNghiep`: lấy `Nghe` `Nghiep`. */
    public String getNgheNghiep() { return ngheNghiep; }
    /** Hàm `setNgheNghiep`: gán `Nghe` `Nghiep`. */
    public void setNgheNghiep(String ngheNghiep) { this.ngheNghiep = ngheNghiep; }
    /** Hàm `getDanToc`: lấy `Dan` `Toc`. */
    public String getDanToc() { return danToc; }
    /** Hàm `setDanToc`: gán `Dan` `Toc`. */
    public void setDanToc(String danToc) { this.danToc = danToc; }
    /** Hàm `getQuocTich`: lấy `Quoc` tich. */
    public String getQuocTich() { return quocTich; }
    /** Hàm `setQuocTich`: gán `Quoc` tich. */
    public void setQuocTich(String quocTich) { this.quocTich = quocTich; }
    /** Hàm `getMaTinh`: lấy mã tính. */
    public String getMaTinh() { return maTinh; }
    /** Hàm `setMaTinh`: gán mã tính. */
    public void setMaTinh(String maTinh) { this.maTinh = maTinh; }
    /** Hàm `getMaHuyen`: lấy mã `Huyen`. */
    public String getMaHuyen() { return maHuyen; }
    /** Hàm `setMaHuyen`: gán mã `Huyen`. */
    public void setMaHuyen(String maHuyen) { this.maHuyen = maHuyen; }
    /** Hàm `getMaXa`: lấy mã `Xa`. */
    public String getMaXa() { return maXa; }
    /** Hàm `setMaXa`: gán mã `Xa`. */
    public void setMaXa(String maXa) { this.maXa = maXa; }

    /** Hàm `getNoiSinh`: lấy noi sinh. */
    public String getNoiSinh() { return noiSinh; }
    /** Hàm `setNoiSinh`: gán noi sinh. */
    public void setNoiSinh(String noiSinh) { this.noiSinh = noiSinh; }
    /** Hàm `getSoNhaThonPho`: lấy so nha `Thon` `Pho`. */
    public String getSoNhaThonPho() { return soNhaThonPho; }
    /** Hàm `setSoNhaThonPho`: gán so nha `Thon` `Pho`. */
    public void setSoNhaThonPho(String soNhaThonPho) { this.soNhaThonPho = soNhaThonPho; }
    /** Hàm `getNgayCapCCCD`: lấy `Ngay` cap `CCCD`. */
    public LocalDate getNgayCapCCCD() { return ngayCapCCCD; }
    /** Hàm `setNgayCapCCCD`: gán `Ngay` cap `CCCD`. */
    public void setNgayCapCCCD(LocalDate ngayCapCCCD) { this.ngayCapCCCD = ngayCapCCCD; }
    /** Hàm `getNoiCapCCCD`: lấy noi cap `CCCD`. */
    public String getNoiCapCCCD() { return noiCapCCCD; }
    /** Hàm `setNoiCapCCCD`: gán noi cap `CCCD`. */
    public void setNoiCapCCCD(String noiCapCCCD) { this.noiCapCCCD = noiCapCCCD; }
    /** Hàm `getNoiLamViec`: lấy noi lam viec. */
    public String getNoiLamViec() { return noiLamViec; }
    /** Hàm `setNoiLamViec`: gán noi lam viec. */
    public void setNoiLamViec(String noiLamViec) { this.noiLamViec = noiLamViec; }

    /** Hàm `getBenhNhanCoSo`: lấy benh nhãn có so. */
    public List<BenhNhanCoSo> getBenhNhanCoSo() { return benhNhanCoSo; }
    /** Hàm `setBenhNhanCoSo`: gán benh nhãn có so. */
    public void setBenhNhanCoSo(List<BenhNhanCoSo> benhNhanCoSo) { this.benhNhanCoSo = benhNhanCoSo; }
    /** Hàm `getBaoHiems`: lấy bao `Hiems`. */
    public List<BaoHiem> getBaoHiems() { return baoHiems; }
    /** Hàm `setBaoHiems`: gán bao `Hiems`. */
    public void setBaoHiems(List<BaoHiem> baoHiems) { this.baoHiems = baoHiems; }
    /** Hàm `getTienSuBenhs`: lấy tien su `Benhs`. */
    public List<TienSuBenh> getTienSuBenhs() { return tienSuBenhs; }
    /** Hàm `setTienSuBenhs`: gán tien su `Benhs`. */
    public void setTienSuBenhs(List<TienSuBenh> tienSuBenhs) { this.tienSuBenhs = tienSuBenhs; }
    /** Hàm `getLuotKhams`: lấy luot `Khams`. */
    public List<LuotKham> getLuotKhams() { return luotKhams; }
    /** Hàm `setLuotKhams`: gán luot `Khams`. */
    public void setLuotKhams(List<LuotKham> luotKhams) { this.luotKhams = luotKhams; }
    /** Hàm `getHoaDons`: lấy hoa `Dons`. */
    public List<HoaDon> getHoaDons() { return hoaDons; }
    /** Hàm `setHoaDons`: gán hoa `Dons`. */
    public void setHoaDons(List<HoaDon> hoaDons) { this.hoaDons = hoaDons; }
}
