package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.ITongHopGoc;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class NhanVien extends CoSoThucThe<Integer> implements ITongHopGoc {
    private String maNhanVien = "";
    private String ho = "";
    private String ten = "";
    private String soDienThoai = "";
    private String email;
    private String diaChi;
    private GioiTinh gioiTinh = GioiTinh.M;
    private LocalDate ngaySinh;
    /** Trường `maChucVu`: `ma` chuc vu. */
    private Integer maChucVu;
    private String username = "";
    /** Trường `passwordHash`: matkhau `Hash`. */
    private String passwordHash = "";
    private boolean hoatDong = true;
    private LocalDateTime dangNhapCuoi;
    private String chungChiHanhNghe;
    private LocalDate ngayVaoLam;
    private LocalDate ngayNghiViec;
    private String lyDoNghiViec;
    private BigDecimal luongCoBan = BigDecimal.ZERO;
    private BigDecimal heSoLuong = BigDecimal.ONE;
    private BigDecimal phuCap = BigDecimal.ZERO;

    public NhanVien() {}
    /** Hàm `getHoTen`: lấy ho `Ten`. */
    public String getHoTen() { return (ho + " " + ten).trim(); }
    /** Hàm `getMaNhanVien`: lấy mã nhãn vien. */
    public String getMaNhanVien() { return maNhanVien; }
    /** Hàm `setMaNhanVien`: gán mã nhãn vien. */
    public void setMaNhanVien(String maNhanVien) { this.maNhanVien = maNhanVien; }
    /** Hàm `getHo`: lấy ho. */
    public String getHo() { return ho; }
    /** Hàm `setHo`: gán ho. */
    public void setHo(String ho) { this.ho = ho; }
    /** Hàm `getTen`: lấy `Ten`. */
    public String getTen() { return ten; }
    /** Hàm `setTen`: gán `Ten`. */
    public void setTen(String ten) { this.ten = ten; }
    /** Hàm `getSoDienThoai`: lấy so `Dien` `Thoai`. */
    public String getSoDienThoai() { return soDienThoai; }
    /** Hàm `setSoDienThoai`: gán so `Dien` `Thoai`. */
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    /** Hàm `getEmail`: lấy `Email`. */
    public String getEmail() { return email; }
    /** Hàm `setEmail`: gán `Email`. */
    public void setEmail(String email) { this.email = email; }
    /** Hàm `getDiaChi`: lấy `Dia` chi. */
    public String getDiaChi() { return diaChi; }
    /** Hàm `setDiaChi`: gán `Dia` chi. */
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    /** Hàm `getGioiTinh`: lấy gioi tính. */
    public GioiTinh getGioiTinh() { return gioiTinh; }
    /** Hàm `setGioiTinh`: gán gioi tính. */
    public void setGioiTinh(GioiTinh gioiTinh) { this.gioiTinh = gioiTinh; }
    /** Hàm `getNgaySinh`: lấy `Ngay` sinh. */
    public LocalDate getNgaySinh() { return ngaySinh; }
    /** Hàm `setNgaySinh`: gán `Ngay` sinh. */
    public void setNgaySinh(LocalDate ngaySinh) { this.ngaySinh = ngaySinh; }
    /** Hàm `getMaChucVu`: lấy mã chuc vu. */
    public Integer getMaChucVu() { return maChucVu; }
    /** Hàm `setMaChucVu`: gán mã chuc vu. */
    public void setMaChucVu(Integer maChucVu) { this.maChucVu = maChucVu; }
    /** Hàm `getUsername`: lấy `Username`. */
    public String getUsername() { return username; }
    /** Hàm `setUsername`: gán `Username`. */
    public void setUsername(String username) { this.username = username; }
    /** Hàm `getPasswordHash`: lấy matkhau `Hash`. */
    public String getPasswordHash() { return passwordHash; }
    /** Hàm `setPasswordHash`: gán matkhau `Hash`. */
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    /** Hàm `isHoatDong`: là `Hoat` đóng. */
    public boolean isHoatDong() { return hoatDong; }
    /** Hàm `setHoatDong`: gán `Hoat` đóng. */
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
    /** Hàm `getDangNhapCuoi`: lấy `Dang` nhap `Cuoi`. */
    public LocalDateTime getDangNhapCuoi() { return dangNhapCuoi; }
    /** Hàm `setDangNhapCuoi`: gán `Dang` nhap `Cuoi`. */
    public void setDangNhapCuoi(LocalDateTime dangNhapCuoi) { this.dangNhapCuoi = dangNhapCuoi; }
    /** Hàm `getChungChiHanhNghe`: lấy `Chung` chi hanh `Nghe`. */
    public String getChungChiHanhNghe() { return chungChiHanhNghe; }
    /** Hàm `setChungChiHanhNghe`: gán `Chung` chi hanh `Nghe`. */
    public void setChungChiHanhNghe(String chungChiHanhNghe) { this.chungChiHanhNghe = chungChiHanhNghe; }
    /** Hàm `getNgayVaoLam`: lấy `Ngay` `Vao` lam. */
    public LocalDate getNgayVaoLam() { return ngayVaoLam; }
    /** Hàm `setNgayVaoLam`: gán `Ngay` `Vao` lam. */
    public void setNgayVaoLam(LocalDate ngayVaoLam) { this.ngayVaoLam = ngayVaoLam; }
    /** Hàm `getNgayNghiViec`: lấy `Ngay` nghi viec. */
    public LocalDate getNgayNghiViec() { return ngayNghiViec; }
    /** Hàm `setNgayNghiViec`: gán `Ngay` nghi viec. */
    public void setNgayNghiViec(LocalDate ngayNghiViec) { this.ngayNghiViec = ngayNghiViec; }
    /** Hàm `getLyDoNghiViec`: lấy `Ly` `Do` nghi viec. */
    public String getLyDoNghiViec() { return lyDoNghiViec; }
    /** Hàm `setLyDoNghiViec`: gán `Ly` `Do` nghi viec. */
    public void setLyDoNghiViec(String lyDoNghiViec) { this.lyDoNghiViec = lyDoNghiViec; }
    /** Hàm `getLuongCoBan`: lấy luong có `Ban`. */
    public BigDecimal getLuongCoBan() { return luongCoBan; }
    /** Hàm `setLuongCoBan`: gán luong có `Ban`. */
    public void setLuongCoBan(BigDecimal luongCoBan) { this.luongCoBan = luongCoBan; }
    /** Hàm `getHeSoLuong`: lấy he so luong. */
    public BigDecimal getHeSoLuong() { return heSoLuong; }
    /** Hàm `setHeSoLuong`: gán he so luong. */
    public void setHeSoLuong(BigDecimal heSoLuong) { this.heSoLuong = heSoLuong; }
    /** Hàm `getPhuCap`: lấy `Phu` cap. */
    public BigDecimal getPhuCap() { return phuCap; }
    /** Hàm `setPhuCap`: gán `Phu` cap. */
    public void setPhuCap(BigDecimal phuCap) { this.phuCap = phuCap; }
}
