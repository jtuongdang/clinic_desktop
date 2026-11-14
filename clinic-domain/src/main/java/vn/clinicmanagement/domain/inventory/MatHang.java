package vn.clinicmanagement.domain.inventory;

import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.ITongHopGoc;
import vn.clinicmanagement.domain.common.enums.LoaiMatHang;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MatHang extends CoSoThucThe<Integer> implements ITongHopGoc {
    /** Trường `maItem`: `ma` mathang. */
    private String maItem = "";
    /** Trường `tenItem`: `ten` mathang. */
    private String tenItem = "";
    /** Trường `loaiItem`: `loai` mathang. */
    private LoaiMatHang loaiItem = LoaiMatHang.THUOC;
    private String hoatChat;
    private String hamLuong;
    private String donViCoBan = "";
    /** Trường `quyCachDongGoi`: quy `Cach` đóng `Goi`. */
    private String quyCachDongGoi;
    private BigDecimal giaNhapMacDinh = BigDecimal.ZERO;
    private BigDecimal giaBanMacDinh = BigDecimal.ZERO;
    private BigDecimal tonToiThieu = BigDecimal.ZERO;
    private BigDecimal tonToiDa = new BigDecimal("1000000");
    private boolean hoatDong = true;

    // Bổ sung chuẩn Dược lâm sàng & BHYT
    private String dangBaoChe = "";
    private String nuocSanXuat = "";
    private String hangSanXuat = "";
    private String soDangKy = "";

    /** Trường `donViTinhQuyDois`: `don` vi tính quy `Dois`. */
    private List<DonViTinhQuyDoi> donViTinhQuyDois = new ArrayList<>();
    private List<TonKho> tonKhos = new ArrayList<>();

    public MatHang() {}

    /** Hàm `getMaItem`: lấy mã mathang. */
    public String getMaItem() { return maItem; }
    /** Hàm `setMaItem`: gán mã mathang. */
    public void setMaItem(String maItem) { this.maItem = maItem; }
    /** Hàm `getTenItem`: lấy `Ten` mathang. */
    public String getTenItem() { return tenItem; }
    /** Hàm `setTenItem`: gán `Ten` mathang. */
    public void setTenItem(String tenItem) { this.tenItem = tenItem; }
    /** Hàm `getLoaiItem`: lấy loai mathang. */
    public LoaiMatHang getLoaiItem() { return loaiItem; }
    /** Hàm `setLoaiItem`: gán loai mathang. */
    public void setLoaiItem(LoaiMatHang loaiItem) { this.loaiItem = loaiItem; }
    /** Hàm `getHoatChat`: lấy `Hoat` `Chat`. */
    public String getHoatChat() { return hoatChat; }
    /** Hàm `setHoatChat`: gán `Hoat` `Chat`. */
    public void setHoatChat(String hoatChat) { this.hoatChat = hoatChat; }
    /** Hàm `getHamLuong`: lấy `Ham` luong. */
    public String getHamLuong() { return hamLuong; }
    /** Hàm `setHamLuong`: gán `Ham` luong. */
    public void setHamLuong(String hamLuong) { this.hamLuong = hamLuong; }
    /** Hàm `getDonViCoBan`: lấy don vi có `Ban`. */
    public String getDonViCoBan() { return donViCoBan; }
    /** Hàm `setDonViCoBan`: gán don vi có `Ban`. */
    public void setDonViCoBan(String donViCoBan) { this.donViCoBan = donViCoBan; }
    /** Hàm `getQuyCachDongGoi`: lấy quy `Cach` đóng `Goi`. */
    public String getQuyCachDongGoi() { return quyCachDongGoi; }
    /** Hàm `setQuyCachDongGoi`: gán quy `Cach` đóng `Goi`. */
    public void setQuyCachDongGoi(String quyCachDongGoi) { this.quyCachDongGoi = quyCachDongGoi; }
    /** Hàm `getGiaNhapMacDinh`: lấy `Gia` nhap `Mac` dinh. */
    public BigDecimal getGiaNhapMacDinh() { return giaNhapMacDinh; }
    /** Hàm `setGiaNhapMacDinh`: gán `Gia` nhap `Mac` dinh. */
    public void setGiaNhapMacDinh(BigDecimal giaNhapMacDinh) { this.giaNhapMacDinh = giaNhapMacDinh; }
    /** Hàm `getGiaBanMacDinh`: lấy `Gia` `Ban` `Mac` dinh. */
    public BigDecimal getGiaBanMacDinh() { return giaBanMacDinh; }
    /** Hàm `setGiaBanMacDinh`: gán `Gia` `Ban` `Mac` dinh. */
    public void setGiaBanMacDinh(BigDecimal giaBanMacDinh) { this.giaBanMacDinh = giaBanMacDinh; }
    /** Hàm `getTonToiThieu`: lấy ton `Toi` `Thieu`. */
    public BigDecimal getTonToiThieu() { return tonToiThieu; }
    /** Hàm `setTonToiThieu`: gán ton `Toi` `Thieu`. */
    public void setTonToiThieu(BigDecimal tonToiThieu) { this.tonToiThieu = tonToiThieu; }
    /** Hàm `getTonToiDa`: lấy ton `Toi` `Da`. */
    public BigDecimal getTonToiDa() { return tonToiDa; }
    /** Hàm `setTonToiDa`: gán ton `Toi` `Da`. */
    public void setTonToiDa(BigDecimal tonToiDa) { this.tonToiDa = tonToiDa; }
    /** Hàm `isHoatDong`: là `Hoat` đóng. */
    public boolean isHoatDong() { return hoatDong; }
    /** Hàm `setHoatDong`: gán `Hoat` đóng. */
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }

    /** Hàm `getDangBaoChe`: lấy `Dang` bao `Che`. */
    public String getDangBaoChe() { return dangBaoChe; }
    /** Hàm `setDangBaoChe`: gán `Dang` bao `Che`. */
    public void setDangBaoChe(String dangBaoChe) { this.dangBaoChe = dangBaoChe; }
    /** Hàm `getNuocSanXuat`: lấy `Nuoc` `San` xuất. */
    public String getNuocSanXuat() { return nuocSanXuat; }
    /** Hàm `setNuocSanXuat`: gán `Nuoc` `San` xuất. */
    public void setNuocSanXuat(String nuocSanXuat) { this.nuocSanXuat = nuocSanXuat; }
    /** Hàm `getHangSanXuat`: lấy hang `San` xuất. */
    public String getHangSanXuat() { return hangSanXuat; }
    /** Hàm `setHangSanXuat`: gán hang `San` xuất. */
    public void setHangSanXuat(String hangSanXuat) { this.hangSanXuat = hangSanXuat; }
    /** Hàm `getSoDangKy`: lấy so `Dang` ky. */
    public String getSoDangKy() { return soDangKy; }
    /** Hàm `setSoDangKy`: gán so `Dang` ky. */
    public void setSoDangKy(String soDangKy) { this.soDangKy = soDangKy; }

    /** Hàm `getDonViTinhQuyDois`: lấy don vi tính quy `Dois`. */
    public List<DonViTinhQuyDoi> getDonViTinhQuyDois() { return donViTinhQuyDois; }
    /** Hàm `setDonViTinhQuyDois`: gán don vi tính quy `Dois`. */
    public void setDonViTinhQuyDois(List<DonViTinhQuyDoi> donViTinhQuyDois) { this.donViTinhQuyDois = donViTinhQuyDois; }
    /** Hàm `getTonKhos`: lấy ton `Khos`. */
    public List<TonKho> getTonKhos() { return tonKhos; }
    /** Hàm `setTonKhos`: gán ton `Khos`. */
    public void setTonKhos(List<TonKho> tonKhos) { this.tonKhos = tonKhos; }
}
