package vn.clinicmanagement.domain.payroll;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import java.math.BigDecimal;

public class CauHinhHoaHongLoaiDichVu extends CoSoThucThe<Integer> {
    private LoaiDichVu loaiDichVu = LoaiDichVu.KHAM;
    private BigDecimal tyLeHoaHong = BigDecimal.ZERO;
    private boolean hoatDong = true;
    public CauHinhHoaHongLoaiDichVu() {}
    /** Hàm `getLoaiDichVu`: lấy loai dich vu. */
    public LoaiDichVu getLoaiDichVu() { return loaiDichVu; }
    /** Hàm `setLoaiDichVu`: gán loai dich vu. */
    public void setLoaiDichVu(LoaiDichVu loaiDichVu) { this.loaiDichVu = loaiDichVu; }
    /** Hàm `getTyLeHoaHong`: lấy `Ty` `Le` hoa hong. */
    public BigDecimal getTyLeHoaHong() { return tyLeHoaHong; }
    /** Hàm `setTyLeHoaHong`: gán `Ty` `Le` hoa hong. */
    public void setTyLeHoaHong(BigDecimal tyLeHoaHong) { this.tyLeHoaHong = tyLeHoaHong; }
    /** Hàm `isHoatDong`: là `Hoat` đóng. */
    public boolean isHoatDong() { return hoatDong; }
    /** Hàm `setHoatDong`: gán `Hoat` đóng. */
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
}
