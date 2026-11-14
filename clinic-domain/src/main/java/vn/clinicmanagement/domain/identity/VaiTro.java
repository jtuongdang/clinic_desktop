package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.CoSoThucThe;

public class VaiTro extends CoSoThucThe<Integer> {
    private String tenVaiTro = "";
    private String moTa;
    public VaiTro() {}
    /** Hàm `getTenVaiTro`: lấy `Ten` vai tro. */
    public String getTenVaiTro() { return tenVaiTro; }
    /** Hàm `setTenVaiTro`: gán `Ten` vai tro. */
    public void setTenVaiTro(String tenVaiTro) { this.tenVaiTro = tenVaiTro; }
    /** Hàm `getMoTa`: lấy `Mo` `Ta`. */
    public String getMoTa() { return moTa; }
    /** Hàm `setMoTa`: gán `Mo` `Ta`. */
    public void setMoTa(String moTa) { this.moTa = moTa; }
}
