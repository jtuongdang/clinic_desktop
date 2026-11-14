package vn.clinicmanagement.domain.prescription;

import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.inventory.MatHang;

import java.math.BigDecimal;

public class ChiTietDonThuoc extends CoSoThucThe<Integer> {
    private int maDonThuoc;
    /** Trường `maItem`: `ma` mathang. */
    private int maItem;
    private BigDecimal soLuongKe = BigDecimal.ZERO;
    private BigDecimal soLuongDaXuat = BigDecimal.ZERO;
    private String lieuDung = "";
    private String tanSuat = "";
    private Integer soNgay;
    private String huongDan = "";
    private BigDecimal donGiaKe = BigDecimal.ZERO;

    // Bổ sung cho giao diện Desktop & Đơn thuốc Bộ Y Tế
    private String duongDung = "Uống";
    private BigDecimal sang = BigDecimal.ZERO;
    private BigDecimal trua = BigDecimal.ZERO;
    private BigDecimal chieu = BigDecimal.ZERO;
    private BigDecimal toi = BigDecimal.ZERO;

    private DonThuoc donThuoc;
    /** Trường `item`: mathang. */
    private MatHang item;

    public ChiTietDonThuoc() {}

    /** Hàm `getMaDonThuoc`: lấy mã don thuốc. */
    public int getMaDonThuoc() { return maDonThuoc; }
    /** Hàm `setMaDonThuoc`: gán mã don thuốc. */
    public void setMaDonThuoc(int maDonThuoc) { this.maDonThuoc = maDonThuoc; }
    /** Hàm `getMaItem`: lấy mã mathang. */
    public int getMaItem() { return maItem; }
    /** Hàm `setMaItem`: gán mã mathang. */
    public void setMaItem(int maItem) { this.maItem = maItem; }
    /** Hàm `getSoLuongKe`: lấy so luong ke. */
    public BigDecimal getSoLuongKe() { return soLuongKe; }
    /** Hàm `setSoLuongKe`: gán so luong ke. */
    public void setSoLuongKe(BigDecimal soLuongKe) { this.soLuongKe = soLuongKe; }
    /** Hàm `getSoLuongDaXuat`: lấy so luong `Da` xuất. */
    public BigDecimal getSoLuongDaXuat() { return soLuongDaXuat; }
    /** Hàm `setSoLuongDaXuat`: gán so luong `Da` xuất. */
    public void setSoLuongDaXuat(BigDecimal soLuongDaXuat) { this.soLuongDaXuat = soLuongDaXuat; }
    /** Hàm `getLieuDung`: lấy lieu `Dung`. */
    public String getLieuDung() { return lieuDung; }
    /** Hàm `setLieuDung`: gán lieu `Dung`. */
    public void setLieuDung(String lieuDung) { this.lieuDung = lieuDung; }
    /** Hàm `getTanSuat`: lấy `Tan` `Suat`. */
    public String getTanSuat() { return tanSuat; }
    /** Hàm `setTanSuat`: gán `Tan` `Suat`. */
    public void setTanSuat(String tanSuat) { this.tanSuat = tanSuat; }
    /** Hàm `getSoNgay`: lấy so `Ngay`. */
    public Integer getSoNgay() { return soNgay; }
    /** Hàm `setSoNgay`: gán so `Ngay`. */
    public void setSoNgay(Integer soNgay) { this.soNgay = soNgay; }
    /** Hàm `getHuongDan`: lấy huong `Dan`. */
    public String getHuongDan() { return huongDan; }
    /** Hàm `setHuongDan`: gán huong `Dan`. */
    public void setHuongDan(String huongDan) { this.huongDan = huongDan; }
    /** Hàm `getDonGiaKe`: lấy don `Gia` ke. */
    public BigDecimal getDonGiaKe() { return donGiaKe; }
    /** Hàm `setDonGiaKe`: gán don `Gia` ke. */
    public void setDonGiaKe(BigDecimal donGiaKe) { this.donGiaKe = donGiaKe; }

    /** Hàm `getDuongDung`: lấy `Duong` `Dung`. */
    public String getDuongDung() { return duongDung; }
    /** Hàm `setDuongDung`: gán `Duong` `Dung`. */
    public void setDuongDung(String duongDung) { this.duongDung = duongDung; }
    /** Hàm `getSang`: lấy `Sang`. */
    public BigDecimal getSang() { return sang; }
    /** Hàm `setSang`: gán `Sang`. */
    public void setSang(BigDecimal sang) { this.sang = sang; }
    /** Hàm `getTrua`: lấy `Trua`. */
    public BigDecimal getTrua() { return trua; }
    /** Hàm `setTrua`: gán `Trua`. */
    public void setTrua(BigDecimal trua) { this.trua = trua; }
    /** Hàm `getChieu`: lấy `Chieu`. */
    public BigDecimal getChieu() { return chieu; }
    /** Hàm `setChieu`: gán `Chieu`. */
    public void setChieu(BigDecimal chieu) { this.chieu = chieu; }
    /** Hàm `getToi`: lấy `Toi`. */
    public BigDecimal getToi() { return toi; }
    /** Hàm `setToi`: gán `Toi`. */
    public void setToi(BigDecimal toi) { this.toi = toi; }

    /** Hàm `getDonThuoc`: lấy don thuốc. */
    public DonThuoc getDonThuoc() { return donThuoc; }
    /** Hàm `setDonThuoc`: gán don thuốc. */
    public void setDonThuoc(DonThuoc donThuoc) { this.donThuoc = donThuoc; }
    /** Hàm `getItem`: lấy mathang. */
    public MatHang getItem() { return item; }
    /** Hàm `setItem`: gán mathang. */
    public void setItem(MatHang item) { this.item = item; }
}
