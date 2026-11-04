package vn.clinicmanagement.domain.service;
import vn.clinicmanagement.domain.common.BaseEntity;

public class ChiTietKetQuaXetNghiem extends BaseEntity<Integer> {
    private int maChiDinhDichVu;
    private String tenChiSo = "";
    private String giaTri = "";
    private String donViDo;
    private String khoangThamChieu;
    private String danhGia;

    public ChiTietKetQuaXetNghiem() {}
    public int getMaChiDinhDichVu() { return maChiDinhDichVu; }
    public void setMaChiDinhDichVu(int maChiDinhDichVu) { this.maChiDinhDichVu = maChiDinhDichVu; }
    public String getTenChiSo() { return tenChiSo; }
    public void setTenChiSo(String tenChiSo) { this.tenChiSo = tenChiSo; }
    public String getGiaTri() { return giaTri; }
    public void setGiaTri(String giaTri) { this.giaTri = giaTri; }
    public String getDonViDo() { return donViDo; }
    public void setDonViDo(String donViDo) { this.donViDo = donViDo; }
    public String getKhoangThamChieu() { return khoangThamChieu; }
    public void setKhoangThamChieu(String khoangThamChieu) { this.khoangThamChieu = khoangThamChieu; }
    public String getDanhGia() { return danhGia; }
    public void setDanhGia(String danhGia) { this.danhGia = danhGia; }
}
