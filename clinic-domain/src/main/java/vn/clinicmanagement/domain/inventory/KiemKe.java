package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.ITongHopGoc;
import vn.clinicmanagement.domain.common.enums.TrangThaiKiemKe;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class KiemKe extends CoSoThucThe<Integer> implements ITongHopGoc {
    private int maKho;
    private LocalDateTime ngayKiemKe = LocalDateTime.now();
    private TrangThaiKiemKe trangThai = TrangThaiKiemKe.MOI_TAO;
    private String ghiChu;
    private List<KiemKeDem> counts = new ArrayList<>();

    public KiemKe() {}
    /** Hàm `getMaKho`: lấy mã kho. */
    public int getMaKho() { return maKho; }
    /** Hàm `setMaKho`: gán mã kho. */
    public void setMaKho(int maKho) { this.maKho = maKho; }
    /** Hàm `getNgayKiemKe`: lấy `Ngay` kiem ke. */
    public LocalDateTime getNgayKiemKe() { return ngayKiemKe; }
    /** Hàm `setNgayKiemKe`: gán `Ngay` kiem ke. */
    public void setNgayKiemKe(LocalDateTime ngayKiemKe) { this.ngayKiemKe = ngayKiemKe; }
    /** Hàm `getTrangThai`: lấy trang thai. */
    public TrangThaiKiemKe getTrangThai() { return trangThai; }
    /** Hàm `setTrangThai`: gán trang thai. */
    public void setTrangThai(TrangThaiKiemKe trangThai) { this.trangThai = trangThai; }
    /** Hàm `getGhiChu`: lấy `Ghi` chu. */
    public String getGhiChu() { return ghiChu; }
    /** Hàm `setGhiChu`: gán `Ghi` chu. */
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    /** Hàm `getCounts`: lấy `Counts`. */
    public List<KiemKeDem> getCounts() { return counts; }
    /** Hàm `setCounts`: gán `Counts`. */
    public void setCounts(List<KiemKeDem> counts) { this.counts = counts; }

    /** Hàm `approve`: duyệt. */
    public void approve() {
        if (this.trangThai != TrangThaiKiemKe.DA_NOP && this.trangThai != TrangThaiKiemKe.MOI_TAO) {
            throw new IllegalStateException("Phiếu kiểm kê không ở trạng thái hợp lệ để duyệt.");
        }
        this.trangThai = TrangThaiKiemKe.DA_DUYET;
    }
}
