package vn.clinicmanagement.domain.prescription;

import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.inventory.Item;

import java.math.BigDecimal;

public class ChiTietDonThuoc extends BaseEntity<Integer> {
    private int maDonThuoc;
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
    private Item item;

    public ChiTietDonThuoc() {}

    public int getMaDonThuoc() { return maDonThuoc; }
    public void setMaDonThuoc(int maDonThuoc) { this.maDonThuoc = maDonThuoc; }
    public int getMaItem() { return maItem; }
    public void setMaItem(int maItem) { this.maItem = maItem; }
    public BigDecimal getSoLuongKe() { return soLuongKe; }
    public void setSoLuongKe(BigDecimal soLuongKe) { this.soLuongKe = soLuongKe; }
    public BigDecimal getSoLuongDaXuat() { return soLuongDaXuat; }
    public void setSoLuongDaXuat(BigDecimal soLuongDaXuat) { this.soLuongDaXuat = soLuongDaXuat; }
    public String getLieuDung() { return lieuDung; }
    public void setLieuDung(String lieuDung) { this.lieuDung = lieuDung; }
    public String getTanSuat() { return tanSuat; }
    public void setTanSuat(String tanSuat) { this.tanSuat = tanSuat; }
    public Integer getSoNgay() { return soNgay; }
    public void setSoNgay(Integer soNgay) { this.soNgay = soNgay; }
    public String getHuongDan() { return huongDan; }
    public void setHuongDan(String huongDan) { this.huongDan = huongDan; }
    public BigDecimal getDonGiaKe() { return donGiaKe; }
    public void setDonGiaKe(BigDecimal donGiaKe) { this.donGiaKe = donGiaKe; }

    public String getDuongDung() { return duongDung; }
    public void setDuongDung(String duongDung) { this.duongDung = duongDung; }
    public BigDecimal getSang() { return sang; }
    public void setSang(BigDecimal sang) { this.sang = sang; }
    public BigDecimal getTrua() { return trua; }
    public void setTrua(BigDecimal trua) { this.trua = trua; }
    public BigDecimal getChieu() { return chieu; }
    public void setChieu(BigDecimal chieu) { this.chieu = chieu; }
    public BigDecimal getToi() { return toi; }
    public void setToi(BigDecimal toi) { this.toi = toi; }

    public DonThuoc getDonThuoc() { return donThuoc; }
    public void setDonThuoc(DonThuoc donThuoc) { this.donThuoc = donThuoc; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
}
