package vn.clinicmanagement.domain.prescription;

import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.ITongHopGoc;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.common.events.DonThuocDaDuyetSuKien;
import vn.clinicmanagement.domain.common.events.DonThuocDaCapPhatSuKien;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.facility.CoSo;
import vn.clinicmanagement.domain.identity.NhanVien;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DonThuoc extends CoSoThucThe<Integer> implements ITongHopGoc {
    private int maLuotKham;
    private int maBacSi;
    private int maCoSo;
    private LocalDateTime ngayKe = LocalDateTime.now();
    private TrangThaiDonThuoc trangThai = TrangThaiDonThuoc.TAM;
    private String ghiChu = "";
    private LocalDateTime ngayCapThuoc;
    private Integer maNguoiCapThuoc;

    // Bổ sung kho xuất & dặn dò tổng quát
    private Integer maKhoXuat;
    private String loiDanDonThuoc = "";

    private LuotKham luotKham;
    private NhanVien bacSi;
    private CoSo coSo;
    private NhanVien nguoiCapThuoc;
    private List<ChiTietDonThuoc> chiTietDonThuocs = new ArrayList<>();

    public DonThuoc() {}

    /** Hàm `approve`: duyệt. */
    public void approve() {
        if (this.trangThai != TrangThaiDonThuoc.TAM) {
            throw new IllegalStateException("Đơn thuốc không ở trạng thái lưu tạm.");
        }
        this.trangThai = TrangThaiDonThuoc.DA_DUYET;
        registerDomainEvent(new DonThuocDaDuyetSuKien(getId() != null ? getId() : 0, maLuotKham, maBacSi));
    }

    /** Hàm `completeDispense`: hoàn tất cấp phát. */
    public void completeDispense(int maNguoiCapThuoc) {
        if (this.trangThai != TrangThaiDonThuoc.DA_DUYET) {
            throw new IllegalStateException("Đơn chưa được duyệt.");
        }
        this.trangThai = TrangThaiDonThuoc.DA_XUAT_HET;
        this.ngayCapThuoc = LocalDateTime.now();
        this.maNguoiCapThuoc = maNguoiCapThuoc;
        registerDomainEvent(new DonThuocDaCapPhatSuKien(getId() != null ? getId() : 0, maNguoiCapThuoc));
    }

    /** Hàm `cancel`: hủy. */
    public void cancel(String reason) {
        if (this.trangThai == TrangThaiDonThuoc.DA_XUAT_HET) {
            throw new IllegalStateException("Không thể hủy đơn thuốc đã xuất hết.");
        }
        if (this.trangThai == TrangThaiDonThuoc.HUY) {
            throw new IllegalStateException("Đơn thuốc đã bị hủy trước đó.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Lý do hủy đơn thuốc không được để trống.");
        }
        this.trangThai = TrangThaiDonThuoc.HUY;
        this.ghiChu = (this.ghiChu != null && !this.ghiChu.isBlank())
                ? this.ghiChu + "; Lý do hủy: " + reason.trim()
                : "Lý do hủy: " + reason.trim();
        registerDomainEvent(new vn.clinicmanagement.domain.common.events.DonThuocDaHuySuKien(
                getId() != null ? getId() : 0, reason.trim()));
    }

    /** Hàm `getMaLuotKham`: lấy mã luot kham. */
    public int getMaLuotKham() { return maLuotKham; }
    /** Hàm `setMaLuotKham`: gán mã luot kham. */
    public void setMaLuotKham(int maLuotKham) { this.maLuotKham = maLuotKham; }
    /** Hàm `getMaBacSi`: lấy mã `Bac` `Si`. */
    public int getMaBacSi() { return maBacSi; }
    /** Hàm `setMaBacSi`: gán mã `Bac` `Si`. */
    public void setMaBacSi(int maBacSi) { this.maBacSi = maBacSi; }
    /** Hàm `getMaCoSo`: lấy mã có so. */
    public int getMaCoSo() { return maCoSo; }
    /** Hàm `setMaCoSo`: gán mã có so. */
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    /** Hàm `getNgayKe`: lấy `Ngay` ke. */
    public LocalDateTime getNgayKe() { return ngayKe; }
    /** Hàm `setNgayKe`: gán `Ngay` ke. */
    public void setNgayKe(LocalDateTime ngayKe) { this.ngayKe = ngayKe; }
    /** Hàm `getTrangThai`: lấy trang thai. */
    public TrangThaiDonThuoc getTrangThai() { return trangThai; }
    /** Hàm `setTrangThai`: gán trang thai. */
    public void setTrangThai(TrangThaiDonThuoc trangThai) { this.trangThai = trangThai; }
    /** Hàm `getGhiChu`: lấy `Ghi` chu. */
    public String getGhiChu() { return ghiChu; }
    /** Hàm `setGhiChu`: gán `Ghi` chu. */
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    /** Hàm `getNgayCapThuoc`: lấy `Ngay` cap thuốc. */
    public LocalDateTime getNgayCapThuoc() { return ngayCapThuoc; }
    /** Hàm `setNgayCapThuoc`: gán `Ngay` cap thuốc. */
    public void setNgayCapThuoc(LocalDateTime ngayCapThuoc) { this.ngayCapThuoc = ngayCapThuoc; }
    /** Hàm `getMaNguoiCapThuoc`: lấy mã `Nguoi` cap thuốc. */
    public Integer getMaNguoiCapThuoc() { return maNguoiCapThuoc; }
    /** Hàm `setMaNguoiCapThuoc`: gán mã `Nguoi` cap thuốc. */
    public void setMaNguoiCapThuoc(Integer maNguoiCapThuoc) { this.maNguoiCapThuoc = maNguoiCapThuoc; }

    /** Hàm `getMaKhoXuat`: lấy mã kho xuất. */
    public Integer getMaKhoXuat() { return maKhoXuat; }
    /** Hàm `setMaKhoXuat`: gán mã kho xuất. */
    public void setMaKhoXuat(Integer maKhoXuat) { this.maKhoXuat = maKhoXuat; }
    /** Hàm `getLoiDanDonThuoc`: lấy `Loi` `Dan` don thuốc. */
    public String getLoiDanDonThuoc() { return loiDanDonThuoc; }
    /** Hàm `setLoiDanDonThuoc`: gán `Loi` `Dan` don thuốc. */
    public void setLoiDanDonThuoc(String loiDanDonThuoc) { this.loiDanDonThuoc = loiDanDonThuoc; }

    /** Hàm `getLuotKham`: lấy luot kham. */
    public LuotKham getLuotKham() { return luotKham; }
    /** Hàm `setLuotKham`: gán luot kham. */
    public void setLuotKham(LuotKham luotKham) { this.luotKham = luotKham; }
    /** Hàm `getBacSi`: lấy `Bac` `Si`. */
    public NhanVien getBacSi() { return bacSi; }
    /** Hàm `setBacSi`: gán `Bac` `Si`. */
    public void setBacSi(NhanVien bacSi) { this.bacSi = bacSi; }
    /** Hàm `getCoSo`: lấy có so. */
    public CoSo getCoSo() { return coSo; }
    /** Hàm `setCoSo`: gán có so. */
    public void setCoSo(CoSo coSo) { this.coSo = coSo; }
    /** Hàm `getNguoiCapThuoc`: lấy `Nguoi` cap thuốc. */
    public NhanVien getNguoiCapThuoc() { return nguoiCapThuoc; }
    /** Hàm `setNguoiCapThuoc`: gán `Nguoi` cap thuốc. */
    public void setNguoiCapThuoc(NhanVien nguoiCapThuoc) { this.nguoiCapThuoc = nguoiCapThuoc; }
    /** Hàm `getChiTietDonThuocs`: lấy chi tiet don `Thuocs`. */
    public List<ChiTietDonThuoc> getChiTietDonThuocs() { return chiTietDonThuocs; }
    /** Hàm `setChiTietDonThuocs`: gán chi tiet don `Thuocs`. */
    public void setChiTietDonThuocs(List<ChiTietDonThuoc> chiTietDonThuocs) { this.chiTietDonThuocs = chiTietDonThuocs; }
}
