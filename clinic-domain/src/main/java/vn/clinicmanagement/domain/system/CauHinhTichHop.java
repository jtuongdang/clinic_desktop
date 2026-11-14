package vn.clinicmanagement.domain.system;
import vn.clinicmanagement.domain.common.CoSoThucThe;

public class CauHinhTichHop extends CoSoThucThe<Integer> {
    private String maDichVuTichHop = "";
    private String endpoint = "";
    private String apiKey;
    /** Trường `secretKey`: bimat `Key`. */
    private String secretKey;
    private boolean hoatDong = true;
    public CauHinhTichHop() {}
    /** Hàm `getMaDichVuTichHop`: lấy mã dich vu tich hop. */
    public String getMaDichVuTichHop() { return maDichVuTichHop; }
    /** Hàm `setMaDichVuTichHop`: gán mã dich vu tich hop. */
    public void setMaDichVuTichHop(String maDichVuTichHop) { this.maDichVuTichHop = maDichVuTichHop; }
    /** Hàm `getEndpoint`: lấy `Endpoint`. */
    public String getEndpoint() { return endpoint; }
    /** Hàm `setEndpoint`: gán `Endpoint`. */
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    /** Hàm `getApiKey`: lấy `Api` `Key`. */
    public String getApiKey() { return apiKey; }
    /** Hàm `setApiKey`: gán `Api` `Key`. */
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    /** Hàm `getSecretKey`: lấy bimat `Key`. */
    public String getSecretKey() { return secretKey; }
    /** Hàm `setSecretKey`: gán bimat `Key`. */
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    /** Hàm `isHoatDong`: là `Hoat` đóng. */
    public boolean isHoatDong() { return hoatDong; }
    /** Hàm `setHoatDong`: gán `Hoat` đóng. */
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
}
