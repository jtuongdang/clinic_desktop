package vn.clinicmanagement.domain.prescription;

import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.IAggregateRoot;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.common.events.DonThuocApprovedEvent;
import vn.clinicmanagement.domain.common.events.DonThuocDispensedEvent;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.facility.CoSo;
import vn.clinicmanagement.domain.identity.NhanVien;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DonThuoc extends BaseEntity<Integer> implements IAggregateRoot {
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

    public void approve() {
        if (this.trangThai != TrangThaiDonThuoc.TAM) {
            throw new IllegalStateException("Đơn thuốc không ở trạng thái lưu tạm.");
        }
        this.trangThai = TrangThaiDonThuoc.DA_DUYET;
        registerDomainEvent(new DonThuocApprovedEvent(getId() != null ? getId() : 0, maLuotKham, maBacSi));
    }

    public void completeDispense(int maNguoiCapThuoc) {
        if (this.trangThai != TrangThaiDonThuoc.DA_DUYET) {
            throw new IllegalStateException("Đơn chưa được duyệt.");
        }
        this.trangThai = TrangThaiDonThuoc.DA_XUAT_HET;
        this.ngayCapThuoc = LocalDateTime.now();
        this.maNguoiCapThuoc = maNguoiCapThuoc;
        registerDomainEvent(new DonThuocDispensedEvent(getId() != null ? getId() : 0, maNguoiCapThuoc));
    }

    public int getMaLuotKham() { return maLuotKham; }
    public void setMaLuotKham(int maLuotKham) { this.maLuotKham = maLuotKham; }
    public int getMaBacSi() { return maBacSi; }
    public void setMaBacSi(int maBacSi) { this.maBacSi = maBacSi; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public LocalDateTime getNgayKe() { return ngayKe; }
    public void setNgayKe(LocalDateTime ngayKe) { this.ngayKe = ngayKe; }
    public TrangThaiDonThuoc getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiDonThuoc trangThai) { this.trangThai = trangThai; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public LocalDateTime getNgayCapThuoc() { return ngayCapThuoc; }
    public void setNgayCapThuoc(LocalDateTime ngayCapThuoc) { this.ngayCapThuoc = ngayCapThuoc; }
    public Integer getMaNguoiCapThuoc() { return maNguoiCapThuoc; }
    public void setMaNguoiCapThuoc(Integer maNguoiCapThuoc) { this.maNguoiCapThuoc = maNguoiCapThuoc; }

    public Integer getMaKhoXuat() { return maKhoXuat; }
    public void setMaKhoXuat(Integer maKhoXuat) { this.maKhoXuat = maKhoXuat; }
    public String getLoiDanDonThuoc() { return loiDanDonThuoc; }
    public void setLoiDanDonThuoc(String loiDanDonThuoc) { this.loiDanDonThuoc = loiDanDonThuoc; }

    public LuotKham getLuotKham() { return luotKham; }
    public void setLuotKham(LuotKham luotKham) { this.luotKham = luotKham; }
    public NhanVien getBacSi() { return bacSi; }
    public void setBacSi(NhanVien bacSi) { this.bacSi = bacSi; }
    public CoSo getCoSo() { return coSo; }
    public void setCoSo(CoSo coSo) { this.coSo = coSo; }
    public NhanVien getNguoiCapThuoc() { return nguoiCapThuoc; }
    public void setNguoiCapThuoc(NhanVien nguoiCapThuoc) { this.nguoiCapThuoc = nguoiCapThuoc; }
    public List<ChiTietDonThuoc> getChiTietDonThuocs() { return chiTietDonThuocs; }
    public void setChiTietDonThuocs(List<ChiTietDonThuoc> chiTietDonThuocs) { this.chiTietDonThuocs = chiTietDonThuocs; }
}
