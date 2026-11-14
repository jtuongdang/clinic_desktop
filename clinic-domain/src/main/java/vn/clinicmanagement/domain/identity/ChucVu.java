package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.CoSoThucThe;

public class ChucVu extends CoSoThucThe<Integer> {
    /** Trường `tenChucVu`: `ten` chuc vu. */
    private String tenChucVu = "";
    private String moTa;
    public ChucVu() {}
    /** Hàm `getTenChucVu`: lấy `Ten` chuc vu. */
    public String getTenChucVu() { return tenChucVu; }
    /** Hàm `setTenChucVu`: gán `Ten` chuc vu. */
    public void setTenChucVu(String tenChucVu) { this.tenChucVu = tenChucVu; }
    /** Hàm `getMoTa`: lấy `Mo` `Ta`. */
    public String getMoTa() { return moTa; }
    /** Hàm `setMoTa`: gán `Mo` `Ta`. */
    public void setMoTa(String moTa) { this.moTa = moTa; }
}
