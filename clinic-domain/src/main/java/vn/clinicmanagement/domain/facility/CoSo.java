package vn.clinicmanagement.domain.facility;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.ITongHopGoc;
import vn.clinicmanagement.domain.common.enums.LoaiCoSo;
import java.util.ArrayList;
import java.util.List;

public class CoSo extends CoSoThucThe<Integer> implements ITongHopGoc {
    private String tenCoSo = "";
    private String diaChi = "";
    private String dienThoai = "";
    private String email;
    private LoaiCoSo loaiCoSo = LoaiCoSo.PHONG_KHAM_DA_KHOA;
    private boolean hoatDong = true;
    private List<Khoa> khoas = new ArrayList<>();

    public CoSo() {}
    /** Hàm `getTenCoSo`: lấy `Ten` có so. */
    public String getTenCoSo() { return tenCoSo; }
    /** Hàm `setTenCoSo`: gán `Ten` có so. */
    public void setTenCoSo(String tenCoSo) { this.tenCoSo = tenCoSo; }
    /** Hàm `getDiaChi`: lấy `Dia` chi. */
    public String getDiaChi() { return diaChi; }
    /** Hàm `setDiaChi`: gán `Dia` chi. */
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    /** Hàm `getDienThoai`: lấy `Dien` `Thoai`. */
    public String getDienThoai() { return dienThoai; }
    /** Hàm `setDienThoai`: gán `Dien` `Thoai`. */
    public void setDienThoai(String dienThoai) { this.dienThoai = dienThoai; }
    /** Hàm `getEmail`: lấy `Email`. */
    public String getEmail() { return email; }
    /** Hàm `setEmail`: gán `Email`. */
    public void setEmail(String email) { this.email = email; }
    /** Hàm `getLoaiCoSo`: lấy loai có so. */
    public LoaiCoSo getLoaiCoSo() { return loaiCoSo; }
    /** Hàm `setLoaiCoSo`: gán loai có so. */
    public void setLoaiCoSo(LoaiCoSo loaiCoSo) { this.loaiCoSo = loaiCoSo; }
    /** Hàm `isHoatDong`: là `Hoat` đóng. */
    public boolean isHoatDong() { return hoatDong; }
    /** Hàm `setHoatDong`: gán `Hoat` đóng. */
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
    /** Hàm `getKhoas`: lấy `Khoas`. */
    public List<Khoa> getKhoas() { return khoas; }
    /** Hàm `setKhoas`: gán `Khoas`. */
    public void setKhoas(List<Khoa> khoas) { this.khoas = khoas; }
}
