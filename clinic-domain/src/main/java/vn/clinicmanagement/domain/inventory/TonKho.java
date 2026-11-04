package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TonKho extends BaseEntity<Integer> {
    private int maKho;
    private int maItem;
    private String soLo = "";
    private LocalDate hanSuDung;
    private BigDecimal soLuongTon = BigDecimal.ZERO;
    private BigDecimal giaNhap = BigDecimal.ZERO;
    private BigDecimal giaBan = BigDecimal.ZERO;

    public TonKho() {}
    public int getMaKho() { return maKho; }
    public void setMaKho(int maKho) { this.maKho = maKho; }
    public int getMaItem() { return maItem; }
    public void setMaItem(int maItem) { this.maItem = maItem; }
    public String getSoLo() { return soLo; }
    public void setSoLo(String soLo) { this.soLo = soLo; }
    public LocalDate getHanSuDung() { return hanSuDung; }
    public void setHanSuDung(LocalDate hanSuDung) { this.hanSuDung = hanSuDung; }
    public BigDecimal getSoLuongTon() { return soLuongTon; }
    public void setSoLuongTon(BigDecimal soLuongTon) { this.soLuongTon = soLuongTon; }
    public BigDecimal getGiaNhap() { return giaNhap; }
    public void setGiaNhap(BigDecimal giaNhap) { this.giaNhap = giaNhap; }
    public BigDecimal getGiaBan() { return giaBan; }
    public void setGiaBan(BigDecimal giaBan) { this.giaBan = giaBan; }
}
