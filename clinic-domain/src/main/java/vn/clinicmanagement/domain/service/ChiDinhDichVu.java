package vn.clinicmanagement.domain.service;

import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.IAggregateRoot;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.examination.LuotKham;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ChiDinhDichVu extends BaseEntity<Integer> implements IAggregateRoot {
    private int maLuotKham;
    private int maDichVu;
    private int maBacSiChiDinh;
    private BigDecimal soLuong = BigDecimal.ONE;
    private BigDecimal donGia = BigDecimal.ZERO;
    private TrangThaiChiDinhDichVu trangThai = TrangThaiChiDinhDichVu.DA_CHI_DINH;
    private String ketQua;
    private String ketLuan;
    private LocalDateTime thoiGianChiDinh = LocalDateTime.now();
    private LocalDateTime thoiGianThucHien;

    // Bổ sung buồng thực hiện & trả kết quả hình ảnh PACS/DICOM
    private Integer maPhongThucHien;
    private Integer bacSiThucHien;
    private String danhSachHinhAnhUrl = "";
    private LocalDateTime thoiGianTraKetQua;

    private LuotKham luotKham;
    private DanhMucDichVu danhMucDichVu;

    public ChiDinhDichVu() {}

    public int getMaLuotKham() { return maLuotKham; }
    public void setMaLuotKham(int maLuotKham) { this.maLuotKham = maLuotKham; }
    public int getMaDichVu() { return maDichVu; }
    public void setMaDichVu(int maDichVu) { this.maDichVu = maDichVu; }
    public int getMaBacSiChiDinh() { return maBacSiChiDinh; }
    public void setMaBacSiChiDinh(int maBacSiChiDinh) { this.maBacSiChiDinh = maBacSiChiDinh; }
    public BigDecimal getSoLuong() { return soLuong; }
    public void setSoLuong(BigDecimal soLuong) { this.soLuong = soLuong; }
    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
    public TrangThaiChiDinhDichVu getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiChiDinhDichVu trangThai) { this.trangThai = trangThai; }
    public String getKetQua() { return ketQua; }
    public void setKetQua(String ketQua) { this.ketQua = ketQua; }
    public String getKetLuan() { return ketLuan; }
    public void setKetLuan(String ketLuan) { this.ketLuan = ketLuan; }
    public LocalDateTime getThoiGianChiDinh() { return thoiGianChiDinh; }
    public void setThoiGianChiDinh(LocalDateTime thoiGianChiDinh) { this.thoiGianChiDinh = thoiGianChiDinh; }
    public LocalDateTime getThoiGianThucHien() { return thoiGianThucHien; }
    public void setThoiGianThucHien(LocalDateTime thoiGianThucHien) { this.thoiGianThucHien = thoiGianThucHien; }

    public Integer getMaPhongThucHien() { return maPhongThucHien; }
    public void setMaPhongThucHien(Integer maPhongThucHien) { this.maPhongThucHien = maPhongThucHien; }
    public Integer getBacSiThucHien() { return bacSiThucHien; }
    public void setBacSiThucHien(Integer bacSiThucHien) { this.bacSiThucHien = bacSiThucHien; }
    public String getDanhSachHinhAnhUrl() { return danhSachHinhAnhUrl; }
    public void setDanhSachHinhAnhUrl(String danhSachHinhAnhUrl) { this.danhSachHinhAnhUrl = danhSachHinhAnhUrl; }
    public LocalDateTime getThoiGianTraKetQua() { return thoiGianTraKetQua; }
    public void setThoiGianTraKetQua(LocalDateTime thoiGianTraKetQua) { this.thoiGianTraKetQua = thoiGianTraKetQua; }

    public LuotKham getLuotKham() { return luotKham; }
    public void setLuotKham(LuotKham luotKham) { this.luotKham = luotKham; }
    public DanhMucDichVu getDanhMucDichVu() { return danhMucDichVu; }
    public void setDanhMucDichVu(DanhMucDichVu danhMucDichVu) { this.danhMucDichVu = danhMucDichVu; }
}
