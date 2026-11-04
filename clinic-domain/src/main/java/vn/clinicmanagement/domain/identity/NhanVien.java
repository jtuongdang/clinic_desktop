package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.IAggregateRoot;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class NhanVien extends BaseEntity<Integer> implements IAggregateRoot {
    private String maNhanVien = "";
    private String ho = "";
    private String ten = "";
    private String soDienThoai = "";
    private String email;
    private String diaChi;
    private GioiTinh gioiTinh = GioiTinh.M;
    private LocalDate ngaySinh;
    private Integer maChucVu;
    private String username = "";
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
    public String getHoTen() { return (ho + " " + ten).trim(); }
    public String getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(String maNhanVien) { this.maNhanVien = maNhanVien; }
    public String getHo() { return ho; }
    public void setHo(String ho) { this.ho = ho; }
    public String getTen() { return ten; }
    public void setTen(String ten) { this.ten = ten; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    public GioiTinh getGioiTinh() { return gioiTinh; }
    public void setGioiTinh(GioiTinh gioiTinh) { this.gioiTinh = gioiTinh; }
    public LocalDate getNgaySinh() { return ngaySinh; }
    public void setNgaySinh(LocalDate ngaySinh) { this.ngaySinh = ngaySinh; }
    public Integer getMaChucVu() { return maChucVu; }
    public void setMaChucVu(Integer maChucVu) { this.maChucVu = maChucVu; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isHoatDong() { return hoatDong; }
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
    public LocalDateTime getDangNhapCuoi() { return dangNhapCuoi; }
    public void setDangNhapCuoi(LocalDateTime dangNhapCuoi) { this.dangNhapCuoi = dangNhapCuoi; }
    public String getChungChiHanhNghe() { return chungChiHanhNghe; }
    public void setChungChiHanhNghe(String chungChiHanhNghe) { this.chungChiHanhNghe = chungChiHanhNghe; }
    public LocalDate getNgayVaoLam() { return ngayVaoLam; }
    public void setNgayVaoLam(LocalDate ngayVaoLam) { this.ngayVaoLam = ngayVaoLam; }
    public LocalDate getNgayNghiViec() { return ngayNghiViec; }
    public void setNgayNghiViec(LocalDate ngayNghiViec) { this.ngayNghiViec = ngayNghiViec; }
    public String getLyDoNghiViec() { return lyDoNghiViec; }
    public void setLyDoNghiViec(String lyDoNghiViec) { this.lyDoNghiViec = lyDoNghiViec; }
    public BigDecimal getLuongCoBan() { return luongCoBan; }
    public void setLuongCoBan(BigDecimal luongCoBan) { this.luongCoBan = luongCoBan; }
    public BigDecimal getHeSoLuong() { return heSoLuong; }
    public void setHeSoLuong(BigDecimal heSoLuong) { this.heSoLuong = heSoLuong; }
    public BigDecimal getPhuCap() { return phuCap; }
    public void setPhuCap(BigDecimal phuCap) { this.phuCap = phuCap; }
}
