package vn.clinicmanagement.domain.service;
import vn.clinicmanagement.domain.common.CoSoThucThe;

public class ChiTietKetQuaXetNghiem extends CoSoThucThe<Integer> {
    private int maChiDinhDichVu;
    private String tenChiSo = "";
    private String giaTri = "";
    private String donViDo;
    private String khoangThamChieu;
    private String danhGia;

    public ChiTietKetQuaXetNghiem() {}
    /** Hàm `getMaChiDinhDichVu`: lấy mã chi dinh dich vu. */
    public int getMaChiDinhDichVu() { return maChiDinhDichVu; }
    /** Hàm `setMaChiDinhDichVu`: gán mã chi dinh dich vu. */
    public void setMaChiDinhDichVu(int maChiDinhDichVu) { this.maChiDinhDichVu = maChiDinhDichVu; }
    /** Hàm `getTenChiSo`: lấy `Ten` chi so. */
    public String getTenChiSo() { return tenChiSo; }
    /** Hàm `setTenChiSo`: gán `Ten` chi so. */
    public void setTenChiSo(String tenChiSo) { this.tenChiSo = tenChiSo; }
    /** Hàm `getGiaTri`: lấy `Gia` tri. */
    public String getGiaTri() { return giaTri; }
    /** Hàm `setGiaTri`: gán `Gia` tri. */
    public void setGiaTri(String giaTri) { this.giaTri = giaTri; }
    /** Hàm `getDonViDo`: lấy don vi `Do`. */
    public String getDonViDo() { return donViDo; }
    /** Hàm `setDonViDo`: gán don vi `Do`. */
    public void setDonViDo(String donViDo) { this.donViDo = donViDo; }
    /** Hàm `getKhoangThamChieu`: lấy `Khoang` tham `Chieu`. */
    public String getKhoangThamChieu() { return khoangThamChieu; }
    /** Hàm `setKhoangThamChieu`: gán `Khoang` tham `Chieu`. */
    public void setKhoangThamChieu(String khoangThamChieu) { this.khoangThamChieu = khoangThamChieu; }
    /** Hàm `getDanhGia`: lấy danh `Gia`. */
    public String getDanhGia() { return danhGia; }
    /** Hàm `setDanhGia`: gán danh `Gia`. */
    public void setDanhGia(String danhGia) { this.danhGia = danhGia; }
}
