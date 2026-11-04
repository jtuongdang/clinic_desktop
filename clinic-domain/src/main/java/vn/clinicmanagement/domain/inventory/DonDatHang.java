package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.IAggregateRoot;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonDatHang;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DonDatHang extends BaseEntity<Integer> implements IAggregateRoot {
    private String soDonDatHang = "";
    private int maNhaCungCap;
    private LocalDateTime ngayDat = LocalDateTime.now();
    private BigDecimal tongTien = BigDecimal.ZERO;
    private TrangThaiDonDatHang trangThai = TrangThaiDonDatHang.CHO_DUYET;
    private List<ChiTietDonDatHang> chiTiets = new ArrayList<>();

    public DonDatHang() {}
    public String getSoDonDatHang() { return soDonDatHang; }
    public void setSoDonDatHang(String soDonDatHang) { this.soDonDatHang = soDonDatHang; }
    public int getMaNhaCungCap() { return maNhaCungCap; }
    public void setMaNhaCungCap(int maNhaCungCap) { this.maNhaCungCap = maNhaCungCap; }
    public LocalDateTime getNgayDat() { return ngayDat; }
    public void setNgayDat(LocalDateTime ngayDat) { this.ngayDat = ngayDat; }
    public BigDecimal getTongTien() { return tongTien; }
    public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }
    public TrangThaiDonDatHang getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiDonDatHang trangThai) { this.trangThai = trangThai; }
    public List<ChiTietDonDatHang> getChiTiets() { return chiTiets; }
    public void setChiTiets(List<ChiTietDonDatHang> chiTiets) { this.chiTiets = chiTiets; }
}
