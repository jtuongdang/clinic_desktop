package vn.clinicmanagement.domain.examination;
import vn.clinicmanagement.domain.common.CoSoThucThe;

public class ICD extends CoSoThucThe<Integer> {
    private String maICD = "";
    private String tenBenh = "";
    private String moTa;
    private boolean hoatDong = true;

    public ICD() {}
    /** Hàm `getMaICD`: lấy mã icd. */
    public String getMaICD() { return maICD; }
    /** Hàm `setMaICD`: gán mã icd. */
    public void setMaICD(String maICD) { this.maICD = maICD; }
    /** Hàm `getTenBenh`: lấy `Ten` benh. */
    public String getTenBenh() { return tenBenh; }
    /** Hàm `setTenBenh`: gán `Ten` benh. */
    public void setTenBenh(String tenBenh) { this.tenBenh = tenBenh; }
    /** Hàm `getMoTa`: lấy `Mo` `Ta`. */
    public String getMoTa() { return moTa; }
    /** Hàm `setMoTa`: gán `Mo` `Ta`. */
    public void setMoTa(String moTa) { this.moTa = moTa; }
    /** Hàm `isHoatDong`: là `Hoat` đóng. */
    public boolean isHoatDong() { return hoatDong; }
    /** Hàm `setHoatDong`: gán `Hoat` đóng. */
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
}
