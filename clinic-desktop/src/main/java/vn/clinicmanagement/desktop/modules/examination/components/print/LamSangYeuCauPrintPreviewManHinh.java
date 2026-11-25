package vn.clinicmanagement.desktop.modules.examination.components.print;

import vn.clinicmanagement.application.cls.DichVuYeuCauDuLieu;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;
import vn.clinicmanagement.desktop.modules.reception.print.PhieuDangKyKhamPdf;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.print.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import static vn.clinicmanagement.desktop.modules.examination.components.print.ToDieuTriBieuTuong.Kind.*;

/** CLS document viewer. Coordinates use the 1586 x 964 reference content area. */
public class LamSangYeuCauPrintPreviewManHinh extends JPanel {
    private static final DateTimeFormatter VIETNAMESE_ORDER_TIME =
            DateTimeFormatter.ofPattern("HH:mm, 'ngày' dd 'tháng' MM 'năm' yyyy");

    public record OrderItem(String group, String name, String quantity, String unit, String notes,
            String department, String subgroup, String queueNumber, String barcode, String location, String performed) {
        public OrderItem(String group, String name, String quantity, String unit, String notes) {
            this(group,name,quantity,unit,notes,group,"","","","","");
        }
    }
    public record ClinicalOrderData(String patientCode, String patientName, String patientBirthYear,
            String patientAge, String patientGender, String patientBhyt, String patientExpiry,
            String patientAddress, String patientDepartment, String patientDiagnosis, String medicalRecordCode,
            String pidCode, String invoiceCode, String daysLeft, String orderDateTime, List<OrderItem> items) {
        public ClinicalOrderData { items = items == null ? List.of() : List.copyOf(items); }
    }
    /** Tạo dữ liệu phiếu chỉ định cận lâm sàng từ thông tin bệnh nhân & danh sách chỉ định thật. */
    public static ClinicalOrderData fromVisitOrders(
            AdminPatientItem patient,
            List<DichVuYeuCauDuLieu> orders,
            Map<Integer, String> serviceGroupById) {
        String pCode = patient != null ? safeStr(patient.code) : "";
        String pName = patient != null ? safeStr(patient.name) : "";
        String pBirthYear = patient != null ? safeStr(patient.birthYear) : "";
        String pAge = patient != null ? safeStr(patient.queueAge) : "";
        String pGender = patient != null ? safeStr(patient.gender) : "";
        String requestType = patient != null && !safeStr(patient.requestType).isEmpty()
                ? patient.requestType.trim() : "Viện phí";
        boolean hasBhyt = requestType.toUpperCase(Locale.ROOT).contains("BH")
                || (patient != null && !safeStr(patient.bhytNumber).isEmpty());
        String pBhyt = "";
        if (hasBhyt) {
            pBhyt = patient != null && !safeStr(patient.bhytNumber).isEmpty()
                    ? patient.bhytNumber.trim()
                    : (patient != null && patient.bhytRate > 0 ? "BHYT " + patient.bhytRate + "%" : requestType);
        }
        String pAddress = patient != null
                ? (!safeStr(patient.bannerAddress).isEmpty() ? patient.bannerAddress : safeStr(patient.address))
                : "";
        String pDept = patient != null
                ? (!safeStr(patient.clinicRoomName).isEmpty() ? patient.clinicRoomName
                : (!safeStr(patient.departmentName).isEmpty() ? patient.departmentName : safeStr(patient.department)))
                : "";
        String pDiagnosis = patient != null ? safeStr(patient.diagnosis) : "";
        String pMedRecord = patient != null ? safeStr(patient.medicalRecordCode) : "";
        String pInvoice = patient != null ? safeStr(patient.invoiceCode) : "";
        String objectBadge = hasBhyt
                ? (patient != null && patient.bhytRate > 0 ? "BHYT " + patient.bhytRate + "%" : requestType)
                : "Viện phí";

        List<OrderItem> mappedItems = new ArrayList<>();
        LocalDateTime latestOrderedAt = null;
        if (orders != null) {
            for (DichVuYeuCauDuLieu o : orders) {
                if (o == null || "DA_HUY".equalsIgnoreCase(safeStr(o.status()))) {
                    continue;
                }
                if (isExamFeeType(o.serviceType(), o.serviceCode())) {
                    continue;
                }
                if (o.orderedAt() != null && (latestOrderedAt == null || o.orderedAt().isAfter(latestOrderedAt))) {
                    latestOrderedAt = o.orderedAt();
                }
                String subGroup = serviceGroupById != null ? safeStr(serviceGroupById.get(o.serviceId())) : "";
                String groupLabel = formatOrderGroupHeader(o.serviceType(), o.serviceCode(), subGroup);
                String qty = o.quantity() != null ? o.quantity().stripTrailingZeros().toPlainString() : "1";
                String performed = List.of("DA_CO_KET_QUA", "DA_DUYET_KET_QUA").contains(safeStr(o.status()).toUpperCase(Locale.ROOT)) ? "Đã thực hiện" : "";
                String name = !safeStr(o.serviceName()).isEmpty() ? o.serviceName() : safeStr(o.serviceCode());
                mappedItems.add(new OrderItem(groupLabel, name, qty, "Lần", "",
                        formatOrderGroupHeader(o.serviceType(), o.serviceCode(), ""), subGroup, "", "", "", performed));
            }
        }
        String orderTimeStr = latestOrderedAt == null ? "" : latestOrderedAt.format(VIETNAMESE_ORDER_TIME);

        return new ClinicalOrderData(
                pCode,
                pName,
                pBirthYear,
                pAge,
                pGender,
                pBhyt,
                "",
                pAddress,
                pDept,
                pDiagnosis,
                pMedRecord,
                pCode,
                pInvoice,
                objectBadge,
                orderTimeStr,
                mappedItems
        );
    }

    private static boolean isExamFeeType(String serviceType, String serviceCode) {
        String normType = safeStr(serviceType).toUpperCase(Locale.ROOT);
        if ("KHAM".equals(normType) || "KHAM_BENH".equals(normType)) {
            return true;
        }
        return normType.isEmpty() && safeStr(serviceCode).toUpperCase(Locale.ROOT).startsWith("DV_KHAM");
    }

    private static String formatOrderGroupHeader(String serviceType, String serviceCode, String subGroup) {
        String norm = safeStr(serviceType).toUpperCase(Locale.ROOT);
        String code = safeStr(serviceCode).toUpperCase(Locale.ROOT);
        String deptHeader;
        if ("CDHA".equals(norm) || "CHAN_DOAN_HINH_ANH".equals(norm) || code.startsWith("CDHA")) {
            deptHeader = "PHÒNG CHẨN ĐOÁN HÌNH ẢNH";
        } else if ("DVKHAC".equals(norm) || "DICH_VU_KY_THUAT".equals(norm)) {
            deptHeader = "PHÒNG THỦ THUẬT / KỸ THUẬT";
        } else {
            deptHeader = "PHÒNG XÉT NGHIỆM";
        }
        return subGroup.isEmpty() ? deptHeader : deptHeader + " - " + subGroup;
    }

    static String safeStr(String s) { return s == null ? "" : s.trim(); }
    private ClinicalOrderData boundData;
    private LamSangPhieuTrang document;
    private JPanel sidebar, toolbar, actions, status, holder, bottom;
    private JScrollPane scroll;
    private final Map<JButton,ToDieuTriBieuTuong.Kind> buttons = new LinkedHashMap<>();
    private double scale = 1;

    public LamSangYeuCauPrintPreviewManHinh() { this(null); }
    public LamSangYeuCauPrintPreviewManHinh(ClinicalOrderData data) { bindData(data); }
    public ClinicalOrderData getBoundData() { return boundData; }
    public LamSangPhieuTrang getDocument() { return document; }
    public final void bindData(ClinicalOrderData data) {
        boundData=data;removeAll();buttons.clear();setLayout(new BorderLayout());setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1586,964));
        document=new LamSangPhieuTrang(data);
        sidebar=new JPanel(new BorderLayout());sidebar.setBackground(new Color(250,250,250));
        sidebar.setBorder(new MatteBorder(0,0,0,1,new Color(160,160,160)));sidebar.setName("cls.sidebar");
        ReferencePanel left=new ReferencePanel(198,520);actions=left;
        left.place(label("Bệnh án điện tử",UngDungPhongChu.TO_DIEU_TRI_BANNER),8,7,182,24);
        JButton sign=button("Ký Tài Liệu",SIGN,true),approve=button("Duyệt Tài Liệu",APPROVE,true),
                save=button("Lưu Tài Liệu",SAVE,true),cancel=button("Hủy Tài Liệu",CANCEL,true);
        for(JButton b:List.of(sign,approve,save,cancel)) {
            b.setEnabled(false);b.setToolTipText("Chưa kết nối dịch vụ ký, duyệt và lưu tài liệu vào hồ sơ.");
        }
        left.place(sign,9,38,178,76);left.place(approve,9,120,178,75);
        left.place(save,9,201,178,75);left.place(cancel,9,281,178,75);
        left.place(label("Xuất file...",UngDungPhongChu.TO_DIEU_TRI_BANNER),8,363,180,24);
        JButton pdf=button("PDF",PDF,true);pdf.addActionListener(e->exportPdf());left.place(pdf,9,394,178,77);
        JButton menu=button("",MENU,false);left.place(menu,68,478,62,39);
        JPopupMenu popup=new JPopupMenu();JMenuItem print=new JMenuItem("In tài liệu");print.addActionListener(e->printDocument());popup.add(print);
        JMenuItem close=new JMenuItem("Đóng");close.addActionListener(e->{Window w=SwingUtilities.getWindowAncestor(this);if(w instanceof LamSangYeuCauPrintPreviewHopThoai)w.dispose();});popup.add(close);
        menu.addActionListener(e->popup.show(menu,0,menu.getHeight()));sidebar.add(left,BorderLayout.NORTH);
        status=new JPanel(new BorderLayout());status.setOpaque(false);
        status.setBorder(new CompoundBorder(new EmptyBorder(0,2,0,3),new MatteBorder(1,0,0,0,Color.GRAY)));
        JLabel unsigned=label("Chưa ký !",UngDungPhongChu.TO_DIEU_TRI_UI);unsigned.setForeground(Color.RED);unsigned.setHorizontalAlignment(SwingConstants.CENTER);
        status.add(unsigned);sidebar.add(status,BorderLayout.SOUTH);add(sidebar,BorderLayout.WEST);
        toolbar=buildToolbar();toolbar.setName("cls.toolbar");
        holder=new JPanel(null) {
            @Override public Dimension getPreferredSize() {return new Dimension(document.getPreferredSize().width+48,document.getPreferredSize().height);}
            @Override public void doLayout(){Dimension d=document.getPreferredSize();document.setBounds(Math.max(0,(getWidth()-d.width)/2-px(24)),0,d.width,d.height);}
        };
        holder.setBackground(new Color(85,85,85));holder.add(document);
        scroll=new JScrollPane(holder);scroll.setName("cls.scroll");scroll.setBorder(null);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        JPanel workspace=new JPanel(new BorderLayout());workspace.add(toolbar,BorderLayout.NORTH);workspace.add(scroll);add(workspace);
        bottom=new JPanel();bottom.setBackground(new Color(0,198,211));add(bottom,BorderLayout.SOUTH);
        boolean available=data!=null&&!data.items().isEmpty();pdf.setEnabled(available);print.setEnabled(available);
        revalidate();repaint();
    }
    private JPanel buildToolbar() {
        ReferencePanel bar=new ReferencePanel(1388,68);bar.setOpaque(true);bar.setBackground(new Color(250,250,250));
        bar.setBorder(new MatteBorder(0,0,1,0,Color.GRAY));
        JButton print=button("In Tài Liệu",PRINT,false);print.addActionListener(e->printDocument());print.setEnabled(boundData!=null&&!boundData.items().isEmpty());bar.place(print,1,0,96,67);
        bar.place(divider(),97,0,1,68);bar.place(label("Loại chữ ký :  ---",UngDungPhongChu.TO_DIEU_TRI_TOOL),105,7,310,24);
        bar.place(label("Hiệu lực :       ---",UngDungPhongChu.TO_DIEU_TRI_TOOL),105,34,310,24);
        JButton check=button("Kiểm Tra",CHECK,false);check.addActionListener(e->checkDocument());bar.place(check,415,0,90,67);bar.place(divider(),505,0,1,68);
        ClinicalOrderData d=boundData;
        String patient=d==null?"":safeStr(d.patientName())+" | "+safeStr(d.patientCode())+" | "+safeStr(d.patientBirthYear())+" ("+safeStr(d.patientAge())+" tuổi) | "+safeStr(d.patientGender());
        bar.place(right(patient,true),515,0,791,24);
        bar.place(right(d==null?"":"Hồ VP : "+safeStr(d.invoiceCode())+" | "+document.subject(),false),515,24,791,22);
        bar.place(right(d==null?"":safeStr(d.patientAddress()),false),515,46,791,20);
        bar.place(divider(),1312,0,1,68);
        JLabel badge=label("<html><center>"+(d==null?"":document.subject().startsWith("BHYT")?document.subject().replace(" ","<br>"):"Viện<br>phí")+"</center></html>",UngDungPhongChu.TO_DIEU_TRI_BANNER);
        badge.setForeground(Color.RED);badge.setHorizontalAlignment(SwingConstants.CENTER);bar.place(badge,1314,0,74,68);return bar;
    }
    @Override public void doLayout() {
        scale=Math.max(.45,getWidth()/1586.0);
        sidebar.setPreferredSize(new Dimension(px(198),0));actions.setPreferredSize(new Dimension(0,px(520)));
        status.setPreferredSize(new Dimension(0,px(56)));toolbar.setPreferredSize(new Dimension(0,px(68)));bottom.setPreferredSize(new Dimension(0,px(13)));
        scaleFonts(this);buttons.forEach((b,k)->b.setIcon(new ToDieuTriBieuTuong(k,px(k==PRINT?32:36))));
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(px(19),0));scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0,px(17)));
        document.setDisplayScale(scale);super.doLayout();
    }
    private int px(int n){return Math.max(1,(int)Math.round(n*scale));}
    private JButton button(String text,ToDieuTriBieuTuong.Kind kind,boolean border) {
        JButton b=new JButton(text);buttons.put(b,kind);b.setIcon(new ToDieuTriBieuTuong(kind,36));
        b.setFont(border?UngDungPhongChu.TO_DIEU_TRI_BUTTON:UngDungPhongChu.TO_DIEU_TRI_TOOL);
        b.setVerticalTextPosition(SwingConstants.BOTTOM);b.setHorizontalTextPosition(SwingConstants.CENTER);b.setIconTextGap(2);
        b.setFocusPainted(false);b.setContentAreaFilled(border);b.setBackground(new Color(247,247,247));
        b.setBorder(border?new LineBorder(new Color(0,0,160)):new EmptyBorder(0,0,0,0));return b;
    }
    private void scaleFonts(Component c) {
        if(c instanceof JComponent jc){Font f=(Font)jc.getClientProperty("cls.font");if(f==null&&jc.getFont()!=null){f=jc.getFont();jc.putClientProperty("cls.font",f);}if(f!=null)jc.setFont(f.deriveFont((float)(f.getSize2D()*scale)));}
        if(c instanceof Container ct)for(Component child:ct.getComponents())scaleFonts(child);
    }
    private static JPanel divider(){JPanel p=new JPanel();p.setBackground(new Color(165,165,165));return p;}
    private static JLabel label(String text,Font font){JLabel l=new JLabel(text);l.setFont(font);return l;}
    private static JLabel right(String text,boolean bold){JLabel l=label(text,bold?UngDungPhongChu.TO_DIEU_TRI_BANNER:UngDungPhongChu.TO_DIEU_TRI_TOOL);l.setForeground(new Color(135,0,0));l.setHorizontalAlignment(SwingConstants.RIGHT);return l;}
    private void checkDocument(){
        String text=boundData==null?"Chưa chọn phiếu chỉ định.":safeStr(boundData.patientCode()).isEmpty()||boundData.items().isEmpty()?"Phiếu thiếu mã bệnh nhân hoặc danh sách dịch vụ.":"Đã tải "+boundData.items().size()+" dịch vụ, "+document.pageCount()+" trang. Tài liệu chưa có chữ ký điện tử.";
        JOptionPane.showMessageDialog(this,text,"Kiểm tra tài liệu",JOptionPane.INFORMATION_MESSAGE);
    }
    private void printDocument(){
        if(boundData==null||boundData.items().isEmpty()||GraphicsEnvironment.isHeadless())return;
        PrinterJob job=PrinterJob.getPrinterJob();job.setJobName("PhieuChiDinhCLS_"+safeStr(boundData.patientCode()));job.setPrintable(document,document.pageFormat());
        if(job.printDialog())try{job.print();}catch(PrinterException ex){JOptionPane.showMessageDialog(this,"Không thể in phiếu chỉ định: "+ex.getMessage(),"Lỗi in",JOptionPane.ERROR_MESSAGE);}
    }
    private void exportPdf(){
        if(boundData==null||boundData.items().isEmpty()||GraphicsEnvironment.isHeadless())return;
        JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Xuất phiếu chỉ định CLS ra PDF");chooser.setSelectedFile(new java.io.File("PhieuChiDinhCLS_"+safeStr(boundData.patientCode()).replaceAll("[^a-zA-Z0-9_-]","_")+".pdf"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Path chosen=chooser.getSelectedFile().toPath();Path destination=chosen.toString().toLowerCase(Locale.ROOT).endsWith(".pdf")?chosen:Path.of(chosen+".pdf");
        if(Files.exists(destination)&&JOptionPane.showConfirmDialog(this,"Thay thế tệp PDF đã tồn tại?","Lưu PDF",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        LamSangPhieuTrang snapshot=document;setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Void,Void>() {
            @Override protected Void doInBackground() throws Exception {int[] pages=java.util.stream.IntStream.range(0,snapshot.pageCount()).toArray();PhieuDangKyKhamPdf.write(snapshot,snapshot.pageCount(),destination,pages,snapshot.pageFormat());return null;}
            @Override protected void done(){setCursor(Cursor.getDefaultCursor());try{get();JOptionPane.showMessageDialog(LamSangYeuCauPrintPreviewManHinh.this,"Đã lưu PDF: "+destination,"Xuất PDF",JOptionPane.INFORMATION_MESSAGE);}catch(Exception ex){JOptionPane.showMessageDialog(LamSangYeuCauPrintPreviewManHinh.this,"Không thể lưu PDF. Kiểm tra đường dẫn và thử lại.","Lỗi xuất PDF",JOptionPane.ERROR_MESSAGE);}}
        }.execute();
    }
    private static final class ReferencePanel extends JPanel {
        private final int width,height;private final Map<Component,Rectangle> positions=new LinkedHashMap<>();
        ReferencePanel(int w,int h){super(null);width=w;height=h;setOpaque(false);}
        void place(Component c,int x,int y,int w,int h){positions.put(c,new Rectangle(x,y,w,h));add(c);}
        @Override public void doLayout(){double sx=getWidth()/(double)width,sy=getHeight()/(double)height;positions.forEach((c,r)->c.setBounds((int)Math.round(r.x*sx),(int)Math.round(r.y*sy),(int)Math.round(r.width*sx),(int)Math.round(r.height*sy)));}
    }
}
