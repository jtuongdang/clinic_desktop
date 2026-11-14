package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.enums.TrangThaiNhaCungCap;

public class NhaCungCap extends CoSoThucThe<Integer> {
    private String tenNhaCungCap = "";
    private String maSoThue;
    private String soDienThoai;
    private String email;
    private String diaChi;
    private TrangThaiNhaCungCap trangThai = TrangThaiNhaCungCap.HOAT_DONG;

    public NhaCungCap() {}
    /** Hàm `getTenNhaCungCap`: lấy `Ten` nha cung cap. */
    public String getTenNhaCungCap() { return tenNhaCungCap; }
    /** Hàm `setTenNhaCungCap`: gán `Ten` nha cung cap. */
    public void setTenNhaCungCap(String tenNhaCungCap) { this.tenNhaCungCap = tenNhaCungCap; }
    /** Hàm `getMaSoThue`: lấy mã so `Thue`. */
    public String getMaSoThue() { return maSoThue; }
    /** Hàm `setMaSoThue`: gán mã so `Thue`. */
    public void setMaSoThue(String maSoThue) { this.maSoThue = maSoThue; }
    /** Hàm `getSoDienThoai`: lấy so `Dien` `Thoai`. */
    public String getSoDienThoai() { return soDienThoai; }
    /** Hàm `setSoDienThoai`: gán so `Dien` `Thoai`. */
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    /** Hàm `getEmail`: lấy `Email`. */
    public String getEmail() { return email; }
    /** Hàm `setEmail`: gán `Email`. */
    public void setEmail(String email) { this.email = email; }
    /** Hàm `getDiaChi`: lấy `Dia` chi. */
    public String getDiaChi() { return diaChi; }
    /** Hàm `setDiaChi`: gán `Dia` chi. */
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    /** Hàm `getTrangThai`: lấy trang thai. */
    public TrangThaiNhaCungCap getTrangThai() { return trangThai; }
    /** Hàm `setTrangThai`: gán trang thai. */
    public void setTrangThai(TrangThaiNhaCungCap trangThai) { this.trangThai = trangThai; }
}
