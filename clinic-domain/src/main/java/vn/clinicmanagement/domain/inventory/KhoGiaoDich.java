package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.enums.LoaiGiaoDichKho;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class KhoGiaoDich extends CoSoThucThe<Integer> {
    private int maKho;
    /** Trường `maItem`: `ma` mathang. */
    private int maItem;
    private int maTonKho;
    private LoaiGiaoDichKho loaiGiaoDich = LoaiGiaoDichKho.NHAP;
    private BigDecimal soLuongThayDoi = BigDecimal.ZERO;
    private BigDecimal tonTruocKhiThayDoi = BigDecimal.ZERO;
    private BigDecimal tonSauKhiThayDoi = BigDecimal.ZERO;
    private LocalDateTime thoiGian = LocalDateTime.now();
    private String maChungTu;

    public KhoGiaoDich() {}
    /** Hàm `getMaKho`: lấy mã kho. */
    public int getMaKho() { return maKho; }
    /** Hàm `setMaKho`: gán mã kho. */
    public void setMaKho(int maKho) { this.maKho = maKho; }
    /** Hàm `getMaItem`: lấy mã mathang. */
    public int getMaItem() { return maItem; }
    /** Hàm `setMaItem`: gán mã mathang. */
    public void setMaItem(int maItem) { this.maItem = maItem; }
    /** Hàm `getMaTonKho`: lấy mã ton kho. */
    public int getMaTonKho() { return maTonKho; }
    /** Hàm `setMaTonKho`: gán mã ton kho. */
    public void setMaTonKho(int maTonKho) { this.maTonKho = maTonKho; }
    /** Hàm `getLoaiGiaoDich`: lấy loai giao dich. */
    public LoaiGiaoDichKho getLoaiGiaoDich() { return loaiGiaoDich; }
    /** Hàm `setLoaiGiaoDich`: gán loai giao dich. */
    public void setLoaiGiaoDich(LoaiGiaoDichKho loaiGiaoDich) { this.loaiGiaoDich = loaiGiaoDich; }
    /** Hàm `getSoLuongThayDoi`: lấy so luong `Thay` doi. */
    public BigDecimal getSoLuongThayDoi() { return soLuongThayDoi; }
    /** Hàm `setSoLuongThayDoi`: gán so luong `Thay` doi. */
    public void setSoLuongThayDoi(BigDecimal soLuongThayDoi) { this.soLuongThayDoi = soLuongThayDoi; }
    /** Hàm `getTonTruocKhiThayDoi`: lấy ton `Truoc` `Khi` `Thay` doi. */
    public BigDecimal getTonTruocKhiThayDoi() { return tonTruocKhiThayDoi; }
    /** Hàm `setTonTruocKhiThayDoi`: gán ton `Truoc` `Khi` `Thay` doi. */
    public void setTonTruocKhiThayDoi(BigDecimal tonTruocKhiThayDoi) { this.tonTruocKhiThayDoi = tonTruocKhiThayDoi; }
    /** Hàm `getTonSauKhiThayDoi`: lấy ton `Sau` `Khi` `Thay` doi. */
    public BigDecimal getTonSauKhiThayDoi() { return tonSauKhiThayDoi; }
    /** Hàm `setTonSauKhiThayDoi`: gán ton `Sau` `Khi` `Thay` doi. */
    public void setTonSauKhiThayDoi(BigDecimal tonSauKhiThayDoi) { this.tonSauKhiThayDoi = tonSauKhiThayDoi; }
    /** Hàm `getThoiGian`: lấy `Thoi` `Gian`. */
    public LocalDateTime getThoiGian() { return thoiGian; }
    /** Hàm `setThoiGian`: gán `Thoi` `Gian`. */
    public void setThoiGian(LocalDateTime thoiGian) { this.thoiGian = thoiGian; }
    /** Hàm `getMaChungTu`: lấy mã `Chung` tu. */
    public String getMaChungTu() { return maChungTu; }
    /** Hàm `setMaChungTu`: gán mã `Chung` tu. */
    public void setMaChungTu(String maChungTu) { this.maChungTu = maChungTu; }
}
