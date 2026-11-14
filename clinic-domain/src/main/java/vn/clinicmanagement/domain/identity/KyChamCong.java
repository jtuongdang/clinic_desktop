package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.time.LocalDate;

public class KyChamCong extends CoSoThucThe<Integer> {
    private String tenKy = "";
    private LocalDate tuNgay;
    private LocalDate denNgay;
    private boolean daKhoa = false;
    public KyChamCong() {}
    /** Hàm `getTenKy`: lấy `Ten` ky. */
    public String getTenKy() { return tenKy; }
    /** Hàm `setTenKy`: gán `Ten` ky. */
    public void setTenKy(String tenKy) { this.tenKy = tenKy; }
    /** Hàm `getTuNgay`: lấy tu `Ngay`. */
    public LocalDate getTuNgay() { return tuNgay; }
    /** Hàm `setTuNgay`: gán tu `Ngay`. */
    public void setTuNgay(LocalDate tuNgay) { this.tuNgay = tuNgay; }
    /** Hàm `getDenNgay`: lấy `Den` `Ngay`. */
    public LocalDate getDenNgay() { return denNgay; }
    /** Hàm `setDenNgay`: gán `Den` `Ngay`. */
    public void setDenNgay(LocalDate denNgay) { this.denNgay = denNgay; }
    /** Hàm `isDaKhoa`: là `Da` khoa. */
    public boolean isDaKhoa() { return daKhoa; }
    /** Hàm `setDaKhoa`: gán `Da` khoa. */
    public void setDaKhoa(boolean daKhoa) { this.daKhoa = daKhoa; }
}
