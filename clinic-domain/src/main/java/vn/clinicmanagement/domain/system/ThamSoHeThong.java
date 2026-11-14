package vn.clinicmanagement.domain.system;
import vn.clinicmanagement.domain.common.CoSoThucThe;

public class ThamSoHeThong extends CoSoThucThe<Integer> {
    private String maThamSo = "";
    private String tenThamSo = "";
    private String giaTri = "";
    private String moTa;
    public ThamSoHeThong() {}
    /** Hàm `getMaThamSo`: lấy mã tham so. */
    public String getMaThamSo() { return maThamSo; }
    /** Hàm `setMaThamSo`: gán mã tham so. */
    public void setMaThamSo(String maThamSo) { this.maThamSo = maThamSo; }
    /** Hàm `getTenThamSo`: lấy `Ten` tham so. */
    public String getTenThamSo() { return tenThamSo; }
    /** Hàm `setTenThamSo`: gán `Ten` tham so. */
    public void setTenThamSo(String tenThamSo) { this.tenThamSo = tenThamSo; }
    /** Hàm `getGiaTri`: lấy `Gia` tri. */
    public String getGiaTri() { return giaTri; }
    /** Hàm `setGiaTri`: gán `Gia` tri. */
    public void setGiaTri(String giaTri) { this.giaTri = giaTri; }
    /** Hàm `getMoTa`: lấy `Mo` `Ta`. */
    public String getMoTa() { return moTa; }
    /** Hàm `setMoTa`: gán `Mo` `Ta`. */
    public void setMoTa(String moTa) { this.moTa = moTa; }
}
