package vn.clinicmanagement.domain.payroll;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import java.math.BigDecimal;

public class CauHinhHoaHongLoaiDichVu extends CoSoThucThe<Integer> {
    private LoaiDichVu loaiDichVu = LoaiDichVu.KHAM;
    private BigDecimal tyLeHoaHong = BigDecimal.ZERO;
    private boolean hoatDong = true;
    public CauHinhHoaHongLoaiDichVu() {}
    public LoaiDichVu getLoaiDichVu() { return loaiDichVu; }
    public void setLoaiDichVu(LoaiDichVu loaiDichVu) { this.loaiDichVu = loaiDichVu; }
    public BigDecimal getTyLeHoaHong() { return tyLeHoaHong; }
    public void setTyLeHoaHong(BigDecimal tyLeHoaHong) { this.tyLeHoaHong = tyLeHoaHong; }
    public boolean isHoatDong() { return hoatDong; }
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
}
