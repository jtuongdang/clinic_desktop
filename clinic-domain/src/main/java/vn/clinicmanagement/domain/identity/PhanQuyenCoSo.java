package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.CoSoThucThe;

public class PhanQuyenCoSo extends CoSoThucThe<Integer> {
    private int maNhanVien;
    private int maCoSo;
    public PhanQuyenCoSo() {}
    public int getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(int maNhanVien) { this.maNhanVien = maNhanVien; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
}
