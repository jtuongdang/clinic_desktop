package vn.clinicmanagement.domain.service;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.ITongHopGoc;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import java.math.BigDecimal;

public class DanhMucDichVu extends CoSoThucThe<Integer> implements ITongHopGoc {
    private String maDichVu = "";
    private String tenDichVu = "";
    private LoaiDichVu loaiDichVu = LoaiDichVu.KHAM;
    private String nhomDichVu = "";
    private BigDecimal donGia = BigDecimal.ZERO;
    private BigDecimal giaBHYT = BigDecimal.ZERO;
    private boolean hoatDong = true;
    private String moTa;

    public DanhMucDichVu() {}
    public String getMaDichVu() { return maDichVu; }
    public void setMaDichVu(String maDichVu) { this.maDichVu = maDichVu; }
    public String getTenDichVu() { return tenDichVu; }
    public void setTenDichVu(String tenDichVu) { this.tenDichVu = tenDichVu; }
    public LoaiDichVu getLoaiDichVu() { return loaiDichVu; }
    public void setLoaiDichVu(LoaiDichVu loaiDichVu) { this.loaiDichVu = loaiDichVu; }
    public String getNhomDichVu() { return nhomDichVu; }
    public void setNhomDichVu(String nhomDichVu) { this.nhomDichVu = nhomDichVu != null ? nhomDichVu : ""; }
    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
    public BigDecimal getGiaBHYT() { return giaBHYT; }
    public void setGiaBHYT(BigDecimal giaBHYT) { this.giaBHYT = giaBHYT; }
    public boolean isHoatDong() { return hoatDong; }
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
}
