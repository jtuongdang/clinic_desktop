package vn.clinicmanagement.domain.patient;

import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.IAggregateRoot;
import vn.clinicmanagement.domain.common.enums.GioiTinh;
import vn.clinicmanagement.domain.common.events.BenhNhanRegisteredEvent;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.billing.HoaDon;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

public class BenhNhan extends BaseEntity<Integer> implements IAggregateRoot {
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
        registerDomainEvent(new BenhNhanRegisteredEvent(getId() != null ? getId() : 0, soCCCD, getHoTen()));
    }

    public String getHoTen() { return (ho + " " + ten).trim(); }

    public int getTuoi() {
        if (ngaySinh == null) return 0;
        return Period.between(ngaySinh, LocalDate.now()).getYears();
    }

    public String getHo() { return ho; }
    public void setHo(String ho) { this.ho = ho; }
    public String getTen() { return ten; }
    public void setTen(String ten) { this.ten = ten; }
    public LocalDate getNgaySinh() { return ngaySinh; }
    public void setNgaySinh(LocalDate ngaySinh) { this.ngaySinh = ngaySinh; }
    public GioiTinh getGioiTinh() { return gioiTinh; }
    public void setGioiTinh(GioiTinh gioiTinh) { this.gioiTinh = gioiTinh; }
    public String getDienThoai() { return dienThoai; }
    public void setDienThoai(String dienThoai) { this.dienThoai = dienThoai; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSoCCCD() { return soCCCD; }
    public void setSoCCCD(String soCCCD) { this.soCCCD = soCCCD; }
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    public String getNguoiLienHeTen() { return nguoiLienHeTen; }
    public void setNguoiLienHeTen(String nguoiLienHeTen) { this.nguoiLienHeTen = nguoiLienHeTen; }
    public String getNguoiLienHeDienThoai() { return nguoiLienHeDienThoai; }
    public void setNguoiLienHeDienThoai(String nguoiLienHeDienThoai) { this.nguoiLienHeDienThoai = nguoiLienHeDienThoai; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getMaBenhNhanFormat() { return maBenhNhanFormat; }
    public void setMaBenhNhanFormat(String maBenhNhanFormat) { this.maBenhNhanFormat = maBenhNhanFormat; }
    public String getNgheNghiep() { return ngheNghiep; }
    public void setNgheNghiep(String ngheNghiep) { this.ngheNghiep = ngheNghiep; }
    public String getDanToc() { return danToc; }
    public void setDanToc(String danToc) { this.danToc = danToc; }
    public String getQuocTich() { return quocTich; }
    public void setQuocTich(String quocTich) { this.quocTich = quocTich; }
    public String getMaTinh() { return maTinh; }
    public void setMaTinh(String maTinh) { this.maTinh = maTinh; }
    public String getMaHuyen() { return maHuyen; }
    public void setMaHuyen(String maHuyen) { this.maHuyen = maHuyen; }
    public String getMaXa() { return maXa; }
    public void setMaXa(String maXa) { this.maXa = maXa; }

    public String getNoiSinh() { return noiSinh; }
    public void setNoiSinh(String noiSinh) { this.noiSinh = noiSinh; }
    public String getSoNhaThonPho() { return soNhaThonPho; }
    public void setSoNhaThonPho(String soNhaThonPho) { this.soNhaThonPho = soNhaThonPho; }
    public LocalDate getNgayCapCCCD() { return ngayCapCCCD; }
    public void setNgayCapCCCD(LocalDate ngayCapCCCD) { this.ngayCapCCCD = ngayCapCCCD; }
    public String getNoiCapCCCD() { return noiCapCCCD; }
    public void setNoiCapCCCD(String noiCapCCCD) { this.noiCapCCCD = noiCapCCCD; }
    public String getNoiLamViec() { return noiLamViec; }
    public void setNoiLamViec(String noiLamViec) { this.noiLamViec = noiLamViec; }

    public List<BenhNhanCoSo> getBenhNhanCoSo() { return benhNhanCoSo; }
    public void setBenhNhanCoSo(List<BenhNhanCoSo> benhNhanCoSo) { this.benhNhanCoSo = benhNhanCoSo; }
    public List<BaoHiem> getBaoHiems() { return baoHiems; }
    public void setBaoHiems(List<BaoHiem> baoHiems) { this.baoHiems = baoHiems; }
    public List<TienSuBenh> getTienSuBenhs() { return tienSuBenhs; }
    public void setTienSuBenhs(List<TienSuBenh> tienSuBenhs) { this.tienSuBenhs = tienSuBenhs; }
    public List<LuotKham> getLuotKhams() { return luotKhams; }
    public void setLuotKhams(List<LuotKham> luotKhams) { this.luotKhams = luotKhams; }
    public List<HoaDon> getHoaDons() { return hoaDons; }
    public void setHoaDons(List<HoaDon> hoaDons) { this.hoaDons = hoaDons; }
}
