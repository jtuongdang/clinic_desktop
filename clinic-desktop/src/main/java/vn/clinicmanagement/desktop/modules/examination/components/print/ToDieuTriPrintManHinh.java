package vn.clinicmanagement.desktop.modules.examination.components.print;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;
import vn.clinicmanagement.domain.examination.ChanDoan;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.prescription.ChiTietDonThuoc;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import static vn.clinicmanagement.desktop.modules.examination.components.print.ToDieuTriBieuTuong.Kind.*;

/** Full-window treatment-document viewer measured from the reference image. */
public class ToDieuTriPrintManHinh extends JPanel {
    public record TreatmentSheetData(
            String sheetNumber, String visitId, String patientCode, String patientName,
            String ageText, String gender, String subjectType, String address,
            String departmentName, String roomName, String bedName, String diagnosisText,
            String examDate, String reasonText, String diseaseProgress, String personalHistory,
            String familyHistory, String treatmentMethod, List<String> medicationLines,
            List<String> supplyLines, List<String> clsLines, String doctorName,
            String birthYear, String insuranceNumber, int insuranceRate, String insuranceRemaining) {
        public TreatmentSheetData(String sheetNumber, String visitId, String patientCode, String patientName,
                String ageText, String gender, String subjectType, String address,
                String departmentName, String roomName, String bedName, String diagnosisText,
                String examDate, String reasonText, String diseaseProgress, String personalHistory,
                String familyHistory, String treatmentMethod, List<String> medicationLines,
                List<String> supplyLines, List<String> clsLines, String doctorName) {
            this(sheetNumber,visitId,patientCode,patientName,ageText,gender,subjectType,address,
                    departmentName,roomName,bedName,diagnosisText,examDate,reasonText,diseaseProgress,
                    personalHistory,familyHistory,treatmentMethod,medicationLines,supplyLines,clsLines,
                    doctorName,"","",0,"");
        }
        public TreatmentSheetData {
            sheetNumber = safe(sheetNumber);
            visitId = safe(visitId);
            patientCode = safe(patientCode);
            patientName = safe(patientName);
            ageText = safe(ageText);
            gender = safe(gender);
            subjectType = safe(subjectType);
            address = safe(address);
            departmentName = safe(departmentName);
            roomName = safe(roomName);
            bedName = safe(bedName);
            diagnosisText = safe(diagnosisText);
            examDate = safe(examDate);
            reasonText = safe(reasonText);
            diseaseProgress = safe(diseaseProgress);
            personalHistory = safe(personalHistory);
            familyHistory = safe(familyHistory);
            treatmentMethod = safe(treatmentMethod);
            doctorName = safe(doctorName);
            birthYear = safe(birthYear);
            insuranceNumber = safe(insuranceNumber);
            insuranceRemaining = safe(insuranceRemaining);

            medicationLines = medicationLines == null ? List.of() : List.copyOf(medicationLines);
            supplyLines = supplyLines == null ? List.of() : List.copyOf(supplyLines);
            clsLines = clsLines == null ? List.of() : List.copyOf(clsLines);
        }
    }
    private final TreatmentSheetData data;
    private final ToDieuTriTrangGiay paperPanel;
    private final JPanel sidebar;
    private final JPanel toolbar;
    private final JPanel sidebarActions;
    private final JPanel statusPanel;
    private final JScrollPane documentScroll;
    private final JButton btnSign, btnApprove, btnSave, btnCancel, btnPdf, btnPrint, btnCheck, btnMenu;
    private double referenceScale = 1;

    public ToDieuTriPrintManHinh(TreatmentSheetData data) {
        this.data = java.util.Objects.requireNonNull(data);
        setLayout(new BorderLayout());setBackground(Color.WHITE);setPreferredSize(new Dimension(1586,959));
        btnSign = button("Ký Tài Liệu",SIGN,true);
        btnApprove = button("Duyệt Tài Liệu",APPROVE,true);
        btnSave = button("Lưu Tài Liệu",SAVE,true);
        btnCancel = button("Hủy Tài Liệu",CANCEL,true);
        btnPdf = button("PDF",PDF,true);
        btnPrint = button("In Tài Liệu",PRINT,false);
        btnCheck = button("Kiểm Tra",CHECK,false);
        btnMenu = button("",MENU,false);
        // These operations have no signing/storage service in the existing composition.
        for(JButton button : List.of(btnSign,btnApprove,btnSave,btnCancel)) {
            button.setEnabled(false);
            button.setToolTipText("Chưa kết nối dịch vụ ký, duyệt và lưu tài liệu vào hồ sơ.");
        }
        btnPrint.addActionListener(e -> printPaper());btnPdf.addActionListener(e -> printPaper());
        btnPdf.setToolTipText("Mở hộp thoại in; chọn lưu PDF nếu hệ thống hỗ trợ.");
        btnCheck.addActionListener(e -> checkDocument());
        sidebar = new JPanel(new BorderLayout());sidebar.setBackground(new Color(250,250,250));
        sidebar.setBorder(new MatteBorder(0,0,0,1,new Color(166,166,166)));
        ReferencePanel left = new ReferencePanel(183,520);sidebarActions=left;
        left.place(label("Bệnh án điện tử",UngDungPhongChu.TO_DIEU_TRI_BANNER),10,7,165,24);
        left.place(btnSign,10,36,165,80);left.place(btnApprove,10,121,165,76);
        left.place(btnSave,10,202,165,76);left.place(btnCancel,10,282,165,75);
        left.place(label("Xuất file...",UngDungPhongChu.TO_DIEU_TRI_BANNER),10,363,165,24);
        left.place(btnPdf,10,392,165,73);left.place(btnMenu,61,474,61,43);
        sidebar.add(left,BorderLayout.NORTH);
        statusPanel=new JPanel(new BorderLayout());statusPanel.setOpaque(false);
        statusPanel.setBorder(new CompoundBorder(new EmptyBorder(0,9,0,7),new MatteBorder(1,0,0,0,new Color(175,175,175))));
        JLabel status=label("Chưa ký !",UngDungPhongChu.TO_DIEU_TRI_UI);status.setForeground(Color.RED);status.setHorizontalAlignment(SwingConstants.CENTER);
        statusPanel.add(status);sidebar.add(statusPanel,BorderLayout.SOUTH);add(sidebar,BorderLayout.WEST);
        toolbar=buildToolbar();
        paperPanel=new ToDieuTriTrangGiay(data);
        JPanel holder=new JPanel(null) {
            @Override public Dimension getPreferredSize() { Dimension paper=paperPanel.getPreferredSize();return new Dimension(paper.width+24,paper.height+20); }
            @Override public void doLayout() {
                Dimension paper=paperPanel.getPreferredSize();paperPanel.setBounds(Math.max(0,(getWidth()-paper.width)/2),0,paper.width,paper.height);
            }
        };
        holder.setBackground(new Color(83,83,83));holder.add(paperPanel);
        documentScroll=new JScrollPane(holder);documentScroll.setBorder(null);
        documentScroll.getViewport().setBackground(new Color(83,83,83));
        documentScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        documentScroll.getVerticalScrollBar().setUnitIncrement(24);
        JPanel workspace=new JPanel(new BorderLayout());workspace.add(toolbar,BorderLayout.NORTH);workspace.add(documentScroll);
        add(workspace);
        JPopupMenu menu=new JPopupMenu();JMenuItem print=new JMenuItem("In tài liệu");print.addActionListener(e -> printPaper());menu.add(print);
        JMenuItem close=new JMenuItem("Đóng");close.addActionListener(e -> {Window window=SwingUtilities.getWindowAncestor(this);if(window instanceof ToDieuTriPrintHopThoai)window.dispose();});menu.add(close);
        btnMenu.addActionListener(e -> menu.show(btnMenu,0,btnMenu.getHeight()));
        paperPanel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if(e.getClickCount()!=2)return;
                double scale=paperPanel.getWidth()/(double)ToDieuTriTrangGiay.WIDTH;
                java.awt.geom.Point2D point=new java.awt.geom.Point2D.Double(e.getX()/scale,e.getY()/scale);
                if(paperPanel.leftSignatureBounds().contains(point)||paperPanel.rightSignatureBounds().contains(point))
                    JOptionPane.showMessageDialog(ToDieuTriPrintManHinh.this,"Chưa kết nối dịch vụ ký tài liệu cho tờ điều trị.","Ký tài liệu",JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }
    public static TreatmentSheetData fromVisit(LuotKham visit, AdminPatientItem fallback) {
        BenhNhan bn = visit != null ? visit.getBenhNhan() : null;
        String code = bn != null && bn.getMaBenhNhanFormat() != null ? bn.getMaBenhNhanFormat()
                : (fallback != null ? safe(fallback.code) : "");
        String name = bn != null && bn.getHoTen() != null ? bn.getHoTen()
                : (fallback != null ? safe(fallback.name) : "");
        LocalDate dob = bn != null ? bn.getNgaySinh() : null;
        String age = dob != null ? String.valueOf(Math.max(0, LocalDate.now().getYear() - dob.getYear()))
                : (fallback != null ? safe(fallback.queueAge) : "");
        String gender = bn != null && bn.getGioiTinh() != null
                ? ("NAM".equalsIgnoreCase(bn.getGioiTinh().name()) || "M".equalsIgnoreCase(bn.getGioiTinh().name()) ? "Nam" : "Nữ")
                : (fallback != null ? safe(fallback.gender) : "");
        String address = bn != null && bn.getDiaChi() != null ? bn.getDiaChi()
                : (fallback != null ? safe(fallback.bannerAddress) : "");
        String subject = visit != null && visit.getDoiTuongKham() != null ? visit.getDoiTuongKham().getDescription()
                : (fallback != null ? safe(fallback.requestType) : "Viện phí");
        String dept = visit != null && visit.getKhoa() != null && visit.getKhoa().getTenKhoa() != null
                ? visit.getKhoa().getTenKhoa()
                : (fallback != null && fallback.departmentName != null && !fallback.departmentName.isBlank()
                ? fallback.departmentName : "Khoa khám bệnh");
        String room = visit != null && visit.getPhongKham() != null && visit.getPhongKham().getTenPhong() != null
                ? visit.getPhongKham().getTenPhong()
                : (fallback != null ? safe(fallback.clinicRoomName) : "");

        String diag = fallback != null ? safe(fallback.diagnosis) : "";
        if (visit != null && visit.getChanDoans() != null) {
            for (ChanDoan cd : visit.getChanDoans()) {
                if (cd != null && !cd.isDeleted() && cd.isLaChinh()) {
                    String icd = cd.getIcd() != null && cd.getIcd().getMaICD() != null ? cd.getIcd().getMaICD() + " - " : "";
                    diag = icd + safe(cd.getChanDoanText());
                    break;
                }
            }
        }

        List<String> meds = new ArrayList<>();
        List<String> supplies = new ArrayList<>();
        if (visit != null && visit.getDonThuocs() != null) {
            for (DonThuoc dt : visit.getDonThuocs()) {
                if (dt == null || dt.isDeleted() || dt.getTrangThai() == vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc.HUY || dt.getChiTietDonThuocs() == null) continue;
                for (ChiTietDonThuoc ct : dt.getChiTietDonThuocs()) {
                    if (ct == null || ct.isDeleted()) continue;
                    String itemName = ct.getItem() != null && ct.getItem().getTenItem() != null
                            ? ct.getItem().getTenItem() : ("Thuốc #" + ct.getMaItem());
                    String qty = ct.getSoLuongKe() != null ? ct.getSoLuongKe().stripTrailingZeros().toPlainString() : "1";
                    String route = ct.getDuongDung() != null && !ct.getDuongDung().isBlank() ? ", " + ct.getDuongDung() : "";
                    String dose = ct.getLieuDung() != null && !ct.getLieuDung().isBlank() ? ", " + ct.getLieuDung() : "";
                    String line = "- " + itemName + " x " + qty + route + dose;
                    if (ct.getItem() != null && ct.getItem().getLoaiItem() == vn.clinicmanagement.domain.common.enums.LoaiMatHang.VAT_TU) supplies.add("- " + itemName + " x " + qty);
                    else meds.add(line);
                }
            }
        }

        List<String> cls = new ArrayList<>();
        if (visit != null && visit.getChiDinhDichVus() != null) {
            for (ChiDinhDichVu cd : visit.getChiDinhDichVus()) {
                if (cd == null || cd.isDeleted() || cd.getTrangThai() == vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu.DA_HUY) continue;
                if (cd.getDanhMucDichVu() != null && cd.getDanhMucDichVu().getTenDichVu() != null) {
                    cls.add("- " + cd.getDanhMucDichVu().getTenDichVu());
                }
            }
        }

        LocalDateTime examTime = visit != null && visit.getThoiGianTiepNhan() != null
                ? visit.getThoiGianTiepNhan() : LocalDateTime.now();
        String doc = NguoiDungPhien.getInstance().getUsername();
        if (doc == null || doc.isBlank()) doc = fallback != null ? safe(fallback.doctor) : "Bác sĩ khám";

        return new TreatmentSheetData(
                "",
                visit != null ? String.valueOf(visit.getId()) : (fallback != null ? String.valueOf(fallback.visitId) : ""),
                code,
                name.toUpperCase(),
                age,
                gender,
                subject,
                address,
                dept,
                room,
                "",
                diag,
                examTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                visit != null ? safe(visit.getLyDoKham()) : "",
                visit != null ? safe(visit.getQuaTrinhBenhLy()) : "",
                visit != null ? safe(visit.getTienSuBanThan()) : "",
                visit != null ? safe(visit.getTienSuGiaDinh()) : "",
                visit != null ? safe(visit.getHuongDieuTri()) : "",
                meds,
                supplies,
                cls,
                doc,
                dob != null ? String.valueOf(dob.getYear()) : fallback != null ? safe(fallback.birthYear) : "",
                fallback != null ? safe(fallback.bhytNumber) : "",
                fallback != null ? fallback.bhytRate : 0,
                ""
        );
    }


    private JPanel buildToolbar() {
        ReferencePanel bar=new ReferencePanel(1403,66);bar.setOpaque(true);bar.setBackground(new Color(250,250,250));
        bar.setBorder(new MatteBorder(0,0,1,0,new Color(155,155,155)));
        bar.place(btnPrint,1,0,92,65);
        JPanel divider=new JPanel();divider.setBackground(new Color(160,160,160));bar.place(divider,93,0,1,66);
        bar.place(label("Loại chữ ký : ---",UngDungPhongChu.TO_DIEU_TRI_TOOL),99,8,304,23);
        bar.place(label("Hiệu lực :      ---",UngDungPhongChu.TO_DIEU_TRI_TOOL),99,34,304,23);
        bar.place(btnCheck,416,0,95,65);
        JPanel divider2=new JPanel();divider2.setBackground(new Color(160,160,160));bar.place(divider2,511,0,1,66);
        String year=data.birthYear().isBlank()?"":data.birthYear()+" ";
        JLabel patient=right(data.patientName()+" | "+data.patientCode()+" | "+year+"("+data.ageText()+" tuổi) | "+data.gender(),true);
        JLabel insurance=right("Mã VP : "+data.visitId()+" | "+(data.insuranceNumber().isBlank()?data.subjectType():data.insuranceNumber())
                +(data.insuranceRemaining().isBlank()?"":" | "+data.insuranceRemaining()),false);
        bar.place(patient,520,0,790,25);bar.place(insurance,520,25,790,22);bar.place(right(data.address(),false),520,46,790,19);
        JPanel badge=new JPanel(new GridLayout(2,1));badge.setOpaque(false);badge.setBorder(new MatteBorder(0,1,0,0,new Color(175,175,175)));
        boolean bhyt=data.subjectType().toUpperCase(java.util.Locale.ROOT).contains("BHYT")||data.insuranceRate()>0;
        JLabel type=label(bhyt?"BHYT":"Viện phí",UngDungPhongChu.TO_DIEU_TRI_BANNER);type.setForeground(Color.RED);type.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel rate=label(bhyt&&data.insuranceRate()>0?data.insuranceRate()+"%":"",UngDungPhongChu.TO_DIEU_TRI_BANNER);rate.setForeground(Color.BLUE);rate.setHorizontalAlignment(SwingConstants.CENTER);
        badge.add(type);badge.add(rate);bar.place(badge,1323,0,80,65);return bar;
    }
    @Override public void doLayout() {
        referenceScale=Math.max(.5,getWidth()/1586.0);
        sidebar.setPreferredSize(new Dimension(px(183),0));sidebarActions.setPreferredSize(new Dimension(0,px(520)));
        statusPanel.setPreferredSize(new Dimension(0,px(55)));toolbar.setPreferredSize(new Dimension(0,px(66)));
        scaleFonts(this,referenceScale);
        for(JButton b:List.of(btnSign,btnApprove,btnSave,btnCancel,btnPdf,btnMenu,btnPrint,btnCheck)) {
            ToDieuTriBieuTuong.Kind kind=(ToDieuTriBieuTuong.Kind)b.getClientProperty("treatment.kind");
            b.setIcon(new ToDieuTriBieuTuong(kind,px(b==btnPrint?32:34)));
        }
        documentScroll.getVerticalScrollBar().setPreferredSize(new Dimension(px(23),0));
        double paperScale=Math.min(referenceScale,Math.max(.35,(getWidth()-px(183)-px(23)-24)/844.0));
        paperPanel.setDisplayScale(paperScale);super.doLayout();
    }
    private int px(int value) { return Math.max(1,(int)Math.round(value*referenceScale)); }
    private void checkDocument() {
        boolean missing=data.patientCode().isBlank()||data.visitId().isBlank();
        JOptionPane.showMessageDialog(this,missing?"Tài liệu thiếu mã bệnh nhân hoặc mã lượt khám.":"Đã tải thông tin tờ điều trị. Tài liệu chưa có chữ ký điện tử.",
                "Kiểm tra tài liệu",missing?JOptionPane.WARNING_MESSAGE:JOptionPane.INFORMATION_MESSAGE);
    }
    private void printPaper() {
        if(GraphicsEnvironment.isHeadless())return;
        PrinterJob job=PrinterJob.getPrinterJob();job.setJobName("ToDieuTri_"+data.patientCode());
        job.setPrintable((graphics,format,index) -> {
            if(index>0)return Printable.NO_SUCH_PAGE;
            Graphics2D g=(Graphics2D)graphics.create();g.translate(format.getImageableX(),format.getImageableY());
            double scale=Math.min(format.getImageableWidth()/paperPanel.getWidth(),format.getImageableHeight()/paperPanel.getHeight());
            g.scale(scale,scale);paperPanel.printAll(g);g.dispose();return Printable.PAGE_EXISTS;
        });
        if(job.printDialog())try{job.print();}catch(PrinterException ex){JOptionPane.showMessageDialog(this,"Không thể in tờ điều trị.","Lỗi in",JOptionPane.ERROR_MESSAGE);}
    }
    private static JButton button(String text,ToDieuTriBieuTuong.Kind kind,boolean bordered) {
        JButton b=new JButton(text,new ToDieuTriBieuTuong(kind,34));
        b.putClientProperty("treatment.kind",kind);b.setFont(bordered?UngDungPhongChu.TO_DIEU_TRI_BUTTON:UngDungPhongChu.TO_DIEU_TRI_TOOL);
        b.setVerticalTextPosition(SwingConstants.BOTTOM);b.setHorizontalTextPosition(SwingConstants.CENTER);b.setIconTextGap(4);
        b.setFocusPainted(false);b.setContentAreaFilled(bordered);b.setBackground(new Color(247,247,247));
        b.setBorder(bordered?new LineBorder(new Color(0,0,160)):new EmptyBorder(0,0,0,0));return b;
    }
    private static JLabel label(String text,Font font) { JLabel label=new JLabel(text);label.setFont(font);return label; }
    private static JLabel right(String text,boolean bold) {
        JLabel label=label(text,bold?UngDungPhongChu.TO_DIEU_TRI_BANNER:UngDungPhongChu.TO_DIEU_TRI_TOOL);
        label.setForeground(new Color(145,0,0));label.setHorizontalAlignment(SwingConstants.RIGHT);return label;
    }
    private static void scaleFonts(Component c,double scale) {
        if(c instanceof JComponent jc) {
            Font font=(Font)jc.getClientProperty("treatment.font");
            if(font==null&&jc.getFont()!=null){font=jc.getFont();jc.putClientProperty("treatment.font",font);}
            if(font!=null)jc.setFont(font.deriveFont((float)(font.getSize2D()*scale)));
        }
        if(c instanceof Container container)for(Component child:container.getComponents())scaleFonts(child,scale);
    }
    private static String safe(String text) { return text==null?"":text.trim(); }
    private static final class ReferencePanel extends JPanel {
        private final int referenceWidth,referenceHeight;
        private final Map<Component,Rectangle> positions=new LinkedHashMap<>();
        ReferencePanel(int w,int h){super(null);referenceWidth=w;referenceHeight=h;setOpaque(false);}
        void place(Component c,int x,int y,int w,int h){positions.put(c,new Rectangle(x,y,w,h));add(c);}
        @Override public void doLayout(){double sx=getWidth()/(double)referenceWidth,sy=getHeight()/(double)referenceHeight;
            positions.forEach((c,r)->c.setBounds((int)Math.round(r.x*sx),(int)Math.round(r.y*sy),(int)Math.round(r.width*sx),(int)Math.round(r.height*sy)));}
    }
    public JPanel getPaperPanel(){return paperPanel;}
}
