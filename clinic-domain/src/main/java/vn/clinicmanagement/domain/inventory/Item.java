package vn.clinicmanagement.domain.inventory;

import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.IAggregateRoot;
import vn.clinicmanagement.domain.common.enums.LoaiItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Item extends BaseEntity<Integer> implements IAggregateRoot {
    private String maItem = "";
    private String tenItem = "";
    private LoaiItem loaiItem = LoaiItem.THUOC;
    private String hoatChat;
    private String hamLuong;
    private String donViCoBan = "";
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

    private List<DonViTinhQuyDoi> donViTinhQuyDois = new ArrayList<>();
    private List<TonKho> tonKhos = new ArrayList<>();

    public Item() {}

    public String getMaItem() { return maItem; }
    public void setMaItem(String maItem) { this.maItem = maItem; }
    public String getTenItem() { return tenItem; }
    public void setTenItem(String tenItem) { this.tenItem = tenItem; }
    public LoaiItem getLoaiItem() { return loaiItem; }
    public void setLoaiItem(LoaiItem loaiItem) { this.loaiItem = loaiItem; }
    public String getHoatChat() { return hoatChat; }
    public void setHoatChat(String hoatChat) { this.hoatChat = hoatChat; }
    public String getHamLuong() { return hamLuong; }
    public void setHamLuong(String hamLuong) { this.hamLuong = hamLuong; }
    public String getDonViCoBan() { return donViCoBan; }
    public void setDonViCoBan(String donViCoBan) { this.donViCoBan = donViCoBan; }
    public String getQuyCachDongGoi() { return quyCachDongGoi; }
    public void setQuyCachDongGoi(String quyCachDongGoi) { this.quyCachDongGoi = quyCachDongGoi; }
    public BigDecimal getGiaNhapMacDinh() { return giaNhapMacDinh; }
    public void setGiaNhapMacDinh(BigDecimal giaNhapMacDinh) { this.giaNhapMacDinh = giaNhapMacDinh; }
    public BigDecimal getGiaBanMacDinh() { return giaBanMacDinh; }
    public void setGiaBanMacDinh(BigDecimal giaBanMacDinh) { this.giaBanMacDinh = giaBanMacDinh; }
    public BigDecimal getTonToiThieu() { return tonToiThieu; }
    public void setTonToiThieu(BigDecimal tonToiThieu) { this.tonToiThieu = tonToiThieu; }
    public BigDecimal getTonToiDa() { return tonToiDa; }
    public void setTonToiDa(BigDecimal tonToiDa) { this.tonToiDa = tonToiDa; }
    public boolean isHoatDong() { return hoatDong; }
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }

    public String getDangBaoChe() { return dangBaoChe; }
    public void setDangBaoChe(String dangBaoChe) { this.dangBaoChe = dangBaoChe; }
    public String getNuocSanXuat() { return nuocSanXuat; }
    public void setNuocSanXuat(String nuocSanXuat) { this.nuocSanXuat = nuocSanXuat; }
    public String getHangSanXuat() { return hangSanXuat; }
    public void setHangSanXuat(String hangSanXuat) { this.hangSanXuat = hangSanXuat; }
    public String getSoDangKy() { return soDangKy; }
    public void setSoDangKy(String soDangKy) { this.soDangKy = soDangKy; }

    public List<DonViTinhQuyDoi> getDonViTinhQuyDois() { return donViTinhQuyDois; }
    public void setDonViTinhQuyDois(List<DonViTinhQuyDoi> donViTinhQuyDois) { this.donViTinhQuyDois = donViTinhQuyDois; }
    public List<TonKho> getTonKhos() { return tonKhos; }
    public void setTonKhos(List<TonKho> tonKhos) { this.tonKhos = tonKhos; }
}
