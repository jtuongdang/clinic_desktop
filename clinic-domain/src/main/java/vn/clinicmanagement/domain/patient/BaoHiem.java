package vn.clinicmanagement.domain.patient;

import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.enums.LoaiBaoHiem;
import java.time.LocalDate;

public class BaoHiem extends BaseEntity<Integer> {
    private int maBenhNhan;
    private LoaiBaoHiem loaiBaoHiem = LoaiBaoHiem.BHYT;
    private String soBaoHiem = "";
    private LocalDate ngayBatDau;
    private LocalDate ngayHetHan;
    private int mucHuong;
    private String noiDangKyKCB = "";

    // Bổ sung chuẩn tiếp đón & Cổng giám định BHYT QĐ 130/BYT
    private String maNoiSong = "";         // K1, K2, K3
    private String maNoiDangKyKCB = "";    // Ví dụ: 01-025
    private String diaChiThe = "";         // Địa chỉ in trên phôi thẻ BHYT
    private boolean dungTuyen = true;      // Đúng tuyến / Trái tuyến
    private boolean coGiayKhaiSinh = false;// Trẻ em có giấy khai sinh
    private boolean khongGiuThe = false;   // Không giữ thẻ BHYT bản cứng
    private boolean mienCungChiTra = false;// Giấy chứng nhận không cùng chi trả trong năm
    private LocalDate ngayMienCungChiTra;  // Ngày bắt đầu được miễn cùng chi trả
    private LocalDate ngayDu5NamLienTuc;   // Ngày tham gia đủ 5 năm liên tục

    private BenhNhan benhNhan;

    public BaoHiem() {}

    public int getMaBenhNhan() { return maBenhNhan; }
    public void setMaBenhNhan(int maBenhNhan) { this.maBenhNhan = maBenhNhan; }
    public LoaiBaoHiem getLoaiBaoHiem() { return loaiBaoHiem; }
    public void setLoaiBaoHiem(LoaiBaoHiem loaiBaoHiem) { this.loaiBaoHiem = loaiBaoHiem; }
    public String getSoBaoHiem() { return soBaoHiem; }
    public void setSoBaoHiem(String soBaoHiem) { this.soBaoHiem = soBaoHiem; }
    public LocalDate getNgayBatDau() { return ngayBatDau; }
    public void setNgayBatDau(LocalDate ngayBatDau) { this.ngayBatDau = ngayBatDau; }
    public LocalDate getNgayHetHan() { return ngayHetHan; }
    public void setNgayHetHan(LocalDate ngayHetHan) { this.ngayHetHan = ngayHetHan; }
    public int getMucHuong() { return mucHuong; }
    public void setMucHuong(int mucHuong) { this.mucHuong = mucHuong; }
    public String getNoiDangKyKCB() { return noiDangKyKCB; }
    public void setNoiDangKyKCB(String noiDangKyKCB) { this.noiDangKyKCB = noiDangKyKCB; }

    public String getMaNoiSong() { return maNoiSong; }
    public void setMaNoiSong(String maNoiSong) { this.maNoiSong = maNoiSong; }
    public String getMaNoiDangKyKCB() { return maNoiDangKyKCB; }
    public void setMaNoiDangKyKCB(String maNoiDangKyKCB) { this.maNoiDangKyKCB = maNoiDangKyKCB; }
    public String getDiaChiThe() { return diaChiThe; }
    public void setDiaChiThe(String diaChiThe) { this.diaChiThe = diaChiThe; }
    public boolean isDungTuyen() { return dungTuyen; }
    public void setDungTuyen(boolean dungTuyen) { this.dungTuyen = dungTuyen; }
    public boolean isCoGiayKhaiSinh() { return coGiayKhaiSinh; }
    public void setCoGiayKhaiSinh(boolean coGiayKhaiSinh) { this.coGiayKhaiSinh = coGiayKhaiSinh; }
    public boolean isKhongGiuThe() { return khongGiuThe; }
    public void setKhongGiuThe(boolean khongGiuThe) { this.khongGiuThe = khongGiuThe; }
    public boolean isMienCungChiTra() { return mienCungChiTra; }
    public void setMienCungChiTra(boolean mienCungChiTra) { this.mienCungChiTra = mienCungChiTra; }
    public LocalDate getNgayMienCungChiTra() { return ngayMienCungChiTra; }
    public void setNgayMienCungChiTra(LocalDate ngayMienCungChiTra) { this.ngayMienCungChiTra = ngayMienCungChiTra; }
    public LocalDate getNgayDu5NamLienTuc() { return ngayDu5NamLienTuc; }
    public void setNgayDu5NamLienTuc(LocalDate ngayDu5NamLienTuc) { this.ngayDu5NamLienTuc = ngayDu5NamLienTuc; }

    public BenhNhan getBenhNhan() { return benhNhan; }
    public void setBenhNhan(BenhNhan benhNhan) { this.benhNhan = benhNhan; }
}
