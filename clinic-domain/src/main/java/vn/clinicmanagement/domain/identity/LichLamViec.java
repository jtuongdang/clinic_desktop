package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.enums.NguonLich;
import vn.clinicmanagement.domain.common.enums.TrangThaiLichLamViec;
import java.time.LocalDate;

public class LichLamViec extends BaseEntity<Integer> {
    private int maNhanVien;
    private int maCaTruc;
    private int maCoSo;
    private Integer maKhoa;
    private Integer maPhongKham;
    private LocalDate ngayLamViec;
    private TrangThaiLichLamViec trangThai = TrangThaiLichLamViec.LAM_VIEC;
    private NguonLich nguonLich = NguonLich.TU_DONG_THEO_LICH_MAU;
    private String ghiChu;

    public LichLamViec() {}
    public int getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(int maNhanVien) { this.maNhanVien = maNhanVien; }
    public int getMaCaTruc() { return maCaTruc; }
    public void setMaCaTruc(int maCaTruc) { this.maCaTruc = maCaTruc; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public Integer getMaKhoa() { return maKhoa; }
    public void setMaKhoa(Integer maKhoa) { this.maKhoa = maKhoa; }
    public Integer getMaPhongKham() { return maPhongKham; }
    public void setMaPhongKham(Integer maPhongKham) { this.maPhongKham = maPhongKham; }
    public LocalDate getNgayLamViec() { return ngayLamViec; }
    public void setNgayLamViec(LocalDate ngayLamViec) { this.ngayLamViec = ngayLamViec; }
    public TrangThaiLichLamViec getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiLichLamViec trangThai) { this.trangThai = trangThai; }
    public NguonLich getNguonLich() { return nguonLich; }
    public void setNguonLich(NguonLich nguonLich) { this.nguonLich = nguonLich; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
}
