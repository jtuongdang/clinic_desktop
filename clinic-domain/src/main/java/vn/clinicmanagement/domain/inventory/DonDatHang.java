package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.ITongHopGoc;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonDatHang;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DonDatHang extends CoSoThucThe<Integer> implements ITongHopGoc {
    private String soDonDatHang = "";
    private int maNhaCungCap;
    private LocalDateTime ngayDat = LocalDateTime.now();
    private BigDecimal tongTien = BigDecimal.ZERO;
    private TrangThaiDonDatHang trangThai = TrangThaiDonDatHang.CHO_DUYET;
    private List<ChiTietDonDatHang> chiTiets = new ArrayList<>();

    public DonDatHang() {}
    /** Hàm `getSoDonDatHang`: lấy so don đặt hang. */
    public String getSoDonDatHang() { return soDonDatHang; }
    /** Hàm `setSoDonDatHang`: gán so don đặt hang. */
    public void setSoDonDatHang(String soDonDatHang) { this.soDonDatHang = soDonDatHang; }
    /** Hàm `getMaNhaCungCap`: lấy mã nha cung cap. */
    public int getMaNhaCungCap() { return maNhaCungCap; }
    /** Hàm `setMaNhaCungCap`: gán mã nha cung cap. */
    public void setMaNhaCungCap(int maNhaCungCap) { this.maNhaCungCap = maNhaCungCap; }
    /** Hàm `getNgayDat`: lấy `Ngay` đặt. */
    public LocalDateTime getNgayDat() { return ngayDat; }
    /** Hàm `setNgayDat`: gán `Ngay` đặt. */
    public void setNgayDat(LocalDateTime ngayDat) { this.ngayDat = ngayDat; }
    /** Hàm `getTongTien`: lấy `Tong` tien. */
    public BigDecimal getTongTien() { return tongTien; }
    /** Hàm `setTongTien`: gán `Tong` tien. */
    public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }
    /** Hàm `getTrangThai`: lấy trang thai. */
    public TrangThaiDonDatHang getTrangThai() { return trangThai; }
    /** Hàm `setTrangThai`: gán trang thai. */
    public void setTrangThai(TrangThaiDonDatHang trangThai) { this.trangThai = trangThai; }
    /** Hàm `getChiTiets`: lấy chi `Tiets`. */
    public List<ChiTietDonDatHang> getChiTiets() { return chiTiets; }
    /** Hàm `setChiTiets`: gán chi `Tiets`. */
    public void setChiTiets(List<ChiTietDonDatHang> chiTiets) { this.chiTiets = chiTiets; }
}
