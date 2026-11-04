package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.IAggregateRoot;
import vn.clinicmanagement.domain.common.enums.TrangThaiKiemKe;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Stocktake extends BaseEntity<Integer> implements IAggregateRoot {
    private int maKho;
    private LocalDateTime ngayKiemKe = LocalDateTime.now();
    private TrangThaiKiemKe trangThai = TrangThaiKiemKe.MOI_TAO;
    private String ghiChu;
    private List<StocktakeCount> counts = new ArrayList<>();

    public Stocktake() {}
    public int getMaKho() { return maKho; }
    public void setMaKho(int maKho) { this.maKho = maKho; }
    public LocalDateTime getNgayKiemKe() { return ngayKiemKe; }
    public void setNgayKiemKe(LocalDateTime ngayKiemKe) { this.ngayKiemKe = ngayKiemKe; }
    public TrangThaiKiemKe getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiKiemKe trangThai) { this.trangThai = trangThai; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public List<StocktakeCount> getCounts() { return counts; }
    public void setCounts(List<StocktakeCount> counts) { this.counts = counts; }
}
