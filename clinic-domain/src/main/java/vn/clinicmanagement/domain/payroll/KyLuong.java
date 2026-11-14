package vn.clinicmanagement.domain.payroll;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class KyLuong extends CoSoThucThe<Integer> {
    private String tenKy = "";
    private int thang;
    private int nam;
    private LocalDate tuNgay;
    private LocalDate denNgay;
    private boolean daChot = false;
    private List<BangLuong> bangLuongs = new ArrayList<>();

    public KyLuong() {}
    /** Hàm `getTenKy`: lấy `Ten` ky. */
    public String getTenKy() { return tenKy; }
    /** Hàm `setTenKy`: gán `Ten` ky. */
    public void setTenKy(String tenKy) { this.tenKy = tenKy; }
    /** Hàm `getThang`: lấy `Thang`. */
    public int getThang() { return thang; }
    /** Hàm `setThang`: gán `Thang`. */
    public void setThang(int thang) { this.thang = thang; }
    /** Hàm `getNam`: lấy nam. */
    public int getNam() { return nam; }
    /** Hàm `setNam`: gán nam. */
    public void setNam(int nam) { this.nam = nam; }
    /** Hàm `getTuNgay`: lấy tu `Ngay`. */
    public LocalDate getTuNgay() { return tuNgay; }
    /** Hàm `setTuNgay`: gán tu `Ngay`. */
    public void setTuNgay(LocalDate tuNgay) { this.tuNgay = tuNgay; }
    /** Hàm `getDenNgay`: lấy `Den` `Ngay`. */
    public LocalDate getDenNgay() { return denNgay; }
    /** Hàm `setDenNgay`: gán `Den` `Ngay`. */
    public void setDenNgay(LocalDate denNgay) { this.denNgay = denNgay; }
    /** Hàm `isDaChot`: là `Da` `Chot`. */
    public boolean isDaChot() { return daChot; }
    /** Hàm `setDaChot`: gán `Da` `Chot`. */
    public void setDaChot(boolean daChot) { this.daChot = daChot; }
    /** Hàm `getBangLuongs`: lấy bảng `Luongs`. */
    public List<BangLuong> getBangLuongs() { return bangLuongs; }
    /** Hàm `setBangLuongs`: gán bảng `Luongs`. */
    public void setBangLuongs(List<BangLuong> bangLuongs) { this.bangLuongs = bangLuongs; }
}
