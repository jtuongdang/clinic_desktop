package vn.clinicmanagement.desktop.modules.reports.components;

import vn.clinicmanagement.application.reports.PhieuBaoCaoReadCongKetNoi.*;
import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.text.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;

/** Presentation only: translates stored state and keeps identity independent of displayed row numbers. */
public final class PhieuBaoCaoMoHinh extends AbstractTableModel {
    public enum NhomTrangThai {PENDING("Đang xử lý"),DONE("Đã hoàn thành"),CANCELLED("Đã hủy");private final String title;NhomTrangThai(String title){this.title=title;}public String title(){return title;}}
    private static final String[] COLUMNS={"Trạng thái","Ngày phiếu","Mã phiếu","Loại phiếu","Mã bệnh nhân","Họ tên bệnh nhân","Khoa / Phòng","Người lập / Bác sĩ","Thành tiền"};
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private List<Phieu> all=List.of(),visible=List.of();
    private String keyword="",code="";private NhomTrangThai state;private Predicate<Phieu> branch=row->true;
    public void replace(List<Phieu> rows){all=rows==null?List.of():List.copyOf(rows);apply();}
    public void filter(String keyword,String code,NhomTrangThai state,Predicate<Phieu> branch){this.keyword=normalize(keyword);this.code=normalize(code);this.state=state;this.branch=branch;apply();}
    private void apply(){visible=all.stream().filter(branch).filter(row->state==null||state(row.status())==state).filter(row->code.isEmpty()||normalize(row.code()).contains(code)).filter(row->keyword.isEmpty()||normalize(safe(row.patientName())+" "+safe(row.patientCode())+" "+safe(row.code())+" "+row.type().title()+" "+safe(row.department())+" "+safe(row.author())+" "+status(row.status())).contains(keyword)).toList();fireTableDataChanged();}
    public List<Phieu> all(){return all;}public List<Phieu> visible(){return visible;}public Phieu row(int row){return visible.get(row);}
    @Override public int getRowCount(){return visible.size();}@Override public int getColumnCount(){return COLUMNS.length;}@Override public String getColumnName(int col){return COLUMNS[col];}
    @Override public Object getValueAt(int row,int col){Phieu p=visible.get(row);return switch(col){case 0->status(p.status());case 1->p.date()==null?"":p.date().format(DATE);case 2->safe(p.code());case 3->p.type().title();case 4->safe(p.patientCode());case 5->safe(p.patientName());case 6->safe(p.department());case 7->safe(p.author());case 8->money(p.amount());default->"";};}
    public static String safe(String s){return s==null?"":s;}
    public static String normalize(String s){return Normalizer.normalize(safe(s),Normalizer.Form.NFD).replaceAll("\\p{M}","").replace('đ','d').replace('Đ','D').toLowerCase(Locale.ROOT).strip();}
    public static String money(BigDecimal n){return n==null?"—":new DecimalFormat("#,##0.##",DecimalFormatSymbols.getInstance(Locale.US)).format(n)+"đ";}
    public static NhomTrangThai state(String code){String s=safe(code).toUpperCase(Locale.ROOT);if(Set.of("DA_HUY","HUY","CANCELLED").contains(s))return NhomTrangThai.CANCELLED;if(Set.of("HOAN_THANH","HOAN_TAT","DA_THANH_TOAN","DA_CAP_PHAT","DA_PHAT_HET","DA_XUAT_HET","DA_CO_KET_QUA","DA_DUYET_KET_QUA","DA_KHAM","DA_TRA_KET_QUA").contains(s))return NhomTrangThai.DONE;return NhomTrangThai.PENDING;}
    public static String status(String code){return switch(safe(code).toUpperCase(Locale.ROOT)){
        case "HOAN_THANH","HOAN_TAT"->"Hoàn thành";case "DA_HUY","HUY"->"Đã hủy";case "CHO_KHAM"->"Chờ khám";case "DANG_KHAM"->"Đang khám";case "CHO_CLS"->"Chờ cận lâm sàng";case "KET_LUAN"->"Kết luận";
        case "CHO_THANH_TOAN"->"Chờ thanh toán";case "DA_THANH_TOAN"->"Đã thanh toán";case "THANH_TOAN_MOT_PHAN"->"Thanh toán một phần";
        case "TAM","NHAP"->"Bản nháp";case "DA_KE"->"Đã kê đơn";case "DA_DUYET"->"Đã duyệt";case "DA_XUAT_HET"->"Đã xuất hết";case "CHO_CAP_PHAT"->"Chờ cấp phát";case "DA_CAP_PHAT","DA_PHAT_HET"->"Đã cấp phát";
        case "DA_CHI_DINH"->"Đã chỉ định";case "DA_TIEP_NHAN"->"Đã tiếp nhận";case "CHO_KET_QUA"->"Chờ kết quả";case "DA_CO_KET_QUA"->"Đã có kết quả";case "DA_DUYET_KET_QUA"->"Đã duyệt kết quả";case "DA_TRA_KET_QUA"->"Đã trả kết quả";
        case "CHO_XAC_NHAN"->"Chờ xác nhận";case "DA_XAC_NHAN"->"Đã xác nhận";case "DA_DEN"->"Đã đến";case "DA_KHAM"->"Đã khám";case "KHONG_DEN"->"Không đến";
        case "CHO_VE_TOA"->"Cho về, kê toa";case "NHAP_VIEN"->"Nhập viện";case "CHUYEN_VIEN"->"Chuyển viện";case "HEN_TAI_KHAM"->"Hẹn tái khám";
        default->{String text=safe(code).replace('_',' ').toLowerCase(Locale.ROOT);yield text.isBlank()?"Chưa xác định":Character.toUpperCase(text.charAt(0))+text.substring(1);}};}
}
