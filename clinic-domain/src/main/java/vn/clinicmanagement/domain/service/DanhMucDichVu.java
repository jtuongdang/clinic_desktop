package vn.clinicmanagement.domain.service;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.ITongHopGoc;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import java.math.BigDecimal;

public class DanhMucDichVu extends CoSoThucThe<Integer> implements ITongHopGoc {
    private String maDichVu = "";
    private String tenDichVu = "";
    private LoaiDichVu loaiDichVu = LoaiDichVu.KHAM;
    private BigDecimal donGia = BigDecimal.ZERO;
    private BigDecimal giaBHYT = BigDecimal.ZERO;
    private boolean hoatDong = true;
    private String moTa;

    public DanhMucDichVu() {}
    /** Hàm `getMaDichVu`: lấy mã dich vu. */
    public String getMaDichVu() { return maDichVu; }
    /** Hàm `setMaDichVu`: gán mã dich vu. */
    public void setMaDichVu(String maDichVu) { this.maDichVu = maDichVu; }
    /** Hàm `getTenDichVu`: lấy `Ten` dich vu. */
    public String getTenDichVu() { return tenDichVu; }
    /** Hàm `setTenDichVu`: gán `Ten` dich vu. */
    public void setTenDichVu(String tenDichVu) { this.tenDichVu = tenDichVu; }
    /** Hàm `getLoaiDichVu`: lấy loai dich vu. */
    public LoaiDichVu getLoaiDichVu() { return loaiDichVu; }
    /** Hàm `setLoaiDichVu`: gán loai dich vu. */
    public void setLoaiDichVu(LoaiDichVu loaiDichVu) { this.loaiDichVu = loaiDichVu; }
    /** Hàm `getDonGia`: lấy don `Gia`. */
    public BigDecimal getDonGia() { return donGia; }
    /** Hàm `setDonGia`: gán don `Gia`. */
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
    /** Hàm `getGiaBHYT`: lấy `Gia` `BHYT`. */
    public BigDecimal getGiaBHYT() { return giaBHYT; }
    /** Hàm `setGiaBHYT`: gán `Gia` `BHYT`. */
    public void setGiaBHYT(BigDecimal giaBHYT) { this.giaBHYT = giaBHYT; }
    /** Hàm `isHoatDong`: là `Hoat` đóng. */
    public boolean isHoatDong() { return hoatDong; }
    /** Hàm `setHoatDong`: gán `Hoat` đóng. */
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
    /** Hàm `getMoTa`: lấy `Mo` `Ta`. */
    public String getMoTa() { return moTa; }
    /** Hàm `setMoTa`: gán `Mo` `Ta`. */
    public void setMoTa(String moTa) { this.moTa = moTa; }
}
