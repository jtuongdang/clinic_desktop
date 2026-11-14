package vn.clinicmanagement.domain.system;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;

public class MaTranPhanQuyen extends CoSoThucThe<Integer> {
    private int maVaiTro;
    private String phanHe = "";
    private HanhDongQuyen hanhDong = HanhDongQuyen.XEM;
    private PhamViDuLieu phamViDuLieu = PhamViDuLieu.KHOA_PHONG;
    private boolean duocPhep = false;
    public MaTranPhanQuyen() {}
    /** Hàm `getMaVaiTro`: lấy mã vai tro. */
    public int getMaVaiTro() { return maVaiTro; }
    /** Hàm `setMaVaiTro`: gán mã vai tro. */
    public void setMaVaiTro(int maVaiTro) { this.maVaiTro = maVaiTro; }
    /** Hàm `getPhanHe`: lấy phan he. */
    public String getPhanHe() { return phanHe; }
    /** Hàm `setPhanHe`: gán phan he. */
    public void setPhanHe(String phanHe) { this.phanHe = phanHe; }
    /** Hàm `getHanhDong`: lấy hanh đóng. */
    public HanhDongQuyen getHanhDong() { return hanhDong; }
    /** Hàm `setHanhDong`: gán hanh đóng. */
    public void setHanhDong(HanhDongQuyen hanhDong) { this.hanhDong = hanhDong; }
    /** Hàm `getPhamViDuLieu`: lấy pham vi du lieu. */
    public PhamViDuLieu getPhamViDuLieu() { return phamViDuLieu; }
    /** Hàm `setPhamViDuLieu`: gán pham vi du lieu. */
    public void setPhamViDuLieu(PhamViDuLieu phamViDuLieu) { this.phamViDuLieu = phamViDuLieu; }
    /** Hàm `isDuocPhep`: là `Duoc` phep. */
    public boolean isDuocPhep() { return duocPhep; }
    /** Hàm `setDuocPhep`: gán `Duoc` phep. */
    public void setDuocPhep(boolean duocPhep) { this.duocPhep = duocPhep; }
}
