package vn.clinicmanagement.domain.system;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;

public class MaTranPhanQuyen extends BaseEntity<Integer> {
    private int maVaiTro;
    private String phanHe = "";
    private HanhDongQuyen hanhDong = HanhDongQuyen.XEM;
    private PhamViDuLieu phamViDuLieu = PhamViDuLieu.KHOA_PHONG;
    private boolean duocPhep = false;
    public MaTranPhanQuyen() {}
    public int getMaVaiTro() { return maVaiTro; }
    public void setMaVaiTro(int maVaiTro) { this.maVaiTro = maVaiTro; }
    public String getPhanHe() { return phanHe; }
    public void setPhanHe(String phanHe) { this.phanHe = phanHe; }
    public HanhDongQuyen getHanhDong() { return hanhDong; }
    public void setHanhDong(HanhDongQuyen hanhDong) { this.hanhDong = hanhDong; }
    public PhamViDuLieu getPhamViDuLieu() { return phamViDuLieu; }
    public void setPhamViDuLieu(PhamViDuLieu phamViDuLieu) { this.phamViDuLieu = phamViDuLieu; }
    public boolean isDuocPhep() { return duocPhep; }
    public void setDuocPhep(boolean duocPhep) { this.duocPhep = duocPhep; }
}
