package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.CoSoThucThe;

public class LoaiCa extends CoSoThucThe<Integer> {
    private String tenLoaiCa = "";
    private String moTa;
    public LoaiCa() {}
    /** Hàm `getTenLoaiCa`: lấy `Ten` loai ca. */
    public String getTenLoaiCa() { return tenLoaiCa; }
    /** Hàm `setTenLoaiCa`: gán `Ten` loai ca. */
    public void setTenLoaiCa(String tenLoaiCa) { this.tenLoaiCa = tenLoaiCa; }
    /** Hàm `getMoTa`: lấy `Mo` `Ta`. */
    public String getMoTa() { return moTa; }
    /** Hàm `setMoTa`: gán `Mo` `Ta`. */
    public void setMoTa(String moTa) { this.moTa = moTa; }
}
