package vn.clinicmanagement.domain.system;
import vn.clinicmanagement.domain.common.BaseEntity;

public class CauHinhTichHop extends BaseEntity<Integer> {
    private String maDichVuTichHop = "";
    private String endpoint = "";
    private String apiKey;
    private String secretKey;
    private boolean hoatDong = true;
    public CauHinhTichHop() {}
    public String getMaDichVuTichHop() { return maDichVuTichHop; }
    public void setMaDichVuTichHop(String maDichVuTichHop) { this.maDichVuTichHop = maDichVuTichHop; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public boolean isHoatDong() { return hoatDong; }
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
}
