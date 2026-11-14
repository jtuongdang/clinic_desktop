package vn.clinicmanagement.domain.service;

import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.ITongHopGoc;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.examination.LuotKham;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ChiDinhDichVu extends CoSoThucThe<Integer> implements ITongHopGoc {
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

    /** Hàm `getMaLuotKham`: lấy mã luot kham. */
    public int getMaLuotKham() { return maLuotKham; }
    /** Hàm `setMaLuotKham`: gán mã luot kham. */
    public void setMaLuotKham(int maLuotKham) { this.maLuotKham = maLuotKham; }
    /** Hàm `getMaDichVu`: lấy mã dich vu. */
    public int getMaDichVu() { return maDichVu; }
    /** Hàm `setMaDichVu`: gán mã dich vu. */
    public void setMaDichVu(int maDichVu) { this.maDichVu = maDichVu; }
    /** Hàm `getMaBacSiChiDinh`: lấy mã `Bac` `Si` chi dinh. */
    public int getMaBacSiChiDinh() { return maBacSiChiDinh; }
    /** Hàm `setMaBacSiChiDinh`: gán mã `Bac` `Si` chi dinh. */
    public void setMaBacSiChiDinh(int maBacSiChiDinh) { this.maBacSiChiDinh = maBacSiChiDinh; }
    /** Hàm `getSoLuong`: lấy so luong. */
    public BigDecimal getSoLuong() { return soLuong; }
    /** Hàm `setSoLuong`: gán so luong. */
    public void setSoLuong(BigDecimal soLuong) { this.soLuong = soLuong; }
    /** Hàm `getDonGia`: lấy don `Gia`. */
    public BigDecimal getDonGia() { return donGia; }
    /** Hàm `setDonGia`: gán don `Gia`. */
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
    /** Hàm `getTrangThai`: lấy trang thai. */
    public TrangThaiChiDinhDichVu getTrangThai() { return trangThai; }
    /** Hàm `setTrangThai`: gán trang thai. */
    public void setTrangThai(TrangThaiChiDinhDichVu trangThai) { this.trangThai = trangThai; }
    /** Hàm `getKetQua`: lấy ket qua. */
    public String getKetQua() { return ketQua; }
    /** Hàm `setKetQua`: gán ket qua. */
    public void setKetQua(String ketQua) { this.ketQua = ketQua; }
    /** Hàm `getKetLuan`: lấy ket `Luan`. */
    public String getKetLuan() { return ketLuan; }
    /** Hàm `setKetLuan`: gán ket `Luan`. */
    public void setKetLuan(String ketLuan) { this.ketLuan = ketLuan; }
    /** Hàm `getThoiGianChiDinh`: lấy `Thoi` `Gian` chi dinh. */
    public LocalDateTime getThoiGianChiDinh() { return thoiGianChiDinh; }
    /** Hàm `setThoiGianChiDinh`: gán `Thoi` `Gian` chi dinh. */
    public void setThoiGianChiDinh(LocalDateTime thoiGianChiDinh) { this.thoiGianChiDinh = thoiGianChiDinh; }
    /** Hàm `getThoiGianThucHien`: lấy `Thoi` `Gian` thuc `Hien`. */
    public LocalDateTime getThoiGianThucHien() { return thoiGianThucHien; }
    /** Hàm `setThoiGianThucHien`: gán `Thoi` `Gian` thuc `Hien`. */
    public void setThoiGianThucHien(LocalDateTime thoiGianThucHien) { this.thoiGianThucHien = thoiGianThucHien; }

    /** Hàm `getMaPhongThucHien`: lấy mã phòng thuc `Hien`. */
    public Integer getMaPhongThucHien() { return maPhongThucHien; }
    /** Hàm `setMaPhongThucHien`: gán mã phòng thuc `Hien`. */
    public void setMaPhongThucHien(Integer maPhongThucHien) { this.maPhongThucHien = maPhongThucHien; }
    /** Hàm `getBacSiThucHien`: lấy `Bac` `Si` thuc `Hien`. */
    public Integer getBacSiThucHien() { return bacSiThucHien; }
    /** Hàm `setBacSiThucHien`: gán `Bac` `Si` thuc `Hien`. */
    public void setBacSiThucHien(Integer bacSiThucHien) { this.bacSiThucHien = bacSiThucHien; }
    /** Hàm `getDanhSachHinhAnhUrl`: lấy danh `Sach` hinh `Anh` `Url`. */
    public String getDanhSachHinhAnhUrl() { return danhSachHinhAnhUrl; }
    /** Hàm `setDanhSachHinhAnhUrl`: gán danh `Sach` hinh `Anh` `Url`. */
    public void setDanhSachHinhAnhUrl(String danhSachHinhAnhUrl) { this.danhSachHinhAnhUrl = danhSachHinhAnhUrl; }
    /** Hàm `getThoiGianTraKetQua`: lấy `Thoi` `Gian` `Tra` ket qua. */
    public LocalDateTime getThoiGianTraKetQua() { return thoiGianTraKetQua; }
    /** Hàm `setThoiGianTraKetQua`: gán `Thoi` `Gian` `Tra` ket qua. */
    public void setThoiGianTraKetQua(LocalDateTime thoiGianTraKetQua) { this.thoiGianTraKetQua = thoiGianTraKetQua; }

    /** Hàm `getLuotKham`: lấy luot kham. */
    public LuotKham getLuotKham() { return luotKham; }
    /** Hàm `setLuotKham`: gán luot kham. */
    public void setLuotKham(LuotKham luotKham) { this.luotKham = luotKham; }
    /** Hàm `getDanhMucDichVu`: lấy danh muc dich vu. */
    public DanhMucDichVu getDanhMucDichVu() { return danhMucDichVu; }
    /** Hàm `setDanhMucDichVu`: gán danh muc dich vu. */
    public void setDanhMucDichVu(DanhMucDichVu danhMucDichVu) { this.danhMucDichVu = danhMucDichVu; }
}
