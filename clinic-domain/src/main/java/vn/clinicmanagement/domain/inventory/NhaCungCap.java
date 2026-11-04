package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.enums.TrangThaiNhaCungCap;

public class NhaCungCap extends BaseEntity<Integer> {
    private String tenNhaCungCap = "";
    private String maSoThue;
    private String soDienThoai;
    private String email;
    private String diaChi;
    private TrangThaiNhaCungCap trangThai = TrangThaiNhaCungCap.HOAT_DONG;

    public NhaCungCap() {}
    public String getTenNhaCungCap() { return tenNhaCungCap; }
    public void setTenNhaCungCap(String tenNhaCungCap) { this.tenNhaCungCap = tenNhaCungCap; }
    public String getMaSoThue() { return maSoThue; }
    public void setMaSoThue(String maSoThue) { this.maSoThue = maSoThue; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    public TrangThaiNhaCungCap getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiNhaCungCap trangThai) { this.trangThai = trangThai; }
}
