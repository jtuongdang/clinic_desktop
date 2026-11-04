package vn.clinicmanagement.domain.payroll;
import vn.clinicmanagement.domain.common.BaseEntity;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class KyLuong extends BaseEntity<Integer> {
    private String tenKy = "";
    private int thang;
    private int nam;
    private LocalDate tuNgay;
    private LocalDate denNgay;
    private boolean daChot = false;
    private List<BangLuong> bangLuongs = new ArrayList<>();

    public KyLuong() {}
    public String getTenKy() { return tenKy; }
    public void setTenKy(String tenKy) { this.tenKy = tenKy; }
    public int getThang() { return thang; }
    public void setThang(int thang) { this.thang = thang; }
    public int getNam() { return nam; }
    public void setNam(int nam) { this.nam = nam; }
    public LocalDate getTuNgay() { return tuNgay; }
    public void setTuNgay(LocalDate tuNgay) { this.tuNgay = tuNgay; }
    public LocalDate getDenNgay() { return denNgay; }
    public void setDenNgay(LocalDate denNgay) { this.denNgay = denNgay; }
    public boolean isDaChot() { return daChot; }
    public void setDaChot(boolean daChot) { this.daChot = daChot; }
    public List<BangLuong> getBangLuongs() { return bangLuongs; }
    public void setBangLuongs(List<BangLuong> bangLuongs) { this.bangLuongs = bangLuongs; }
}
