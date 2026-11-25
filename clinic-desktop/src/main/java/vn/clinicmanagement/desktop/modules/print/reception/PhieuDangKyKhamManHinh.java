package vn.clinicmanagement.desktop.modules.print.reception;


import vn.clinicmanagement.application.reception.PhieuDangKyKhamDuLieu;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.reception.components.TiepDonBieuTuong;
import javax.swing.*;
import javax.swing.border.*;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Cửa sổ xem trước theo mauphieudangkykhamIn.png; dữ liệu chỉ đến từ truy vấn lượt khám. */
public final class PhieuDangKyKhamManHinh extends JPanel {
    private final PhieuDangKyKhamTrang document;
    private final BooleanSupplier activeSession;
    private final java.util.function.Predicate<vn.clinicmanagement.domain.common.enums.HanhDongQuyen> authorize;
    private final JComboBox<PrintService> printers = new JComboBox<>();
    private final JComboBox<MediaTray> trays = new JComboBox<>();
    private final JSpinner copies = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    private final JCheckBox autoPrint = new JCheckBox("In tự động"), keepOpen = new JCheckBox("In tiếp"), duplex = new JCheckBox("In 2 mặt"), a5onA4 = new JCheckBox("In A5 vào khay giấy A4");
    private final JComboBox<String> scope = new JComboBox<>(new String[]{"In tất cả các trang", "Trang hiện tại", "Các trang chỉ định"});
    private final JTextField pageRange = new JTextField();
    private final JComboBox<String> orientation = new JComboBox<>(new String[]{"Chiều dọc", "Chiều ngang"});
    private final JComboBox<String> paper = new JComboBox<>(new String[]{"A5 (148mm x 210mm)", "A4 (210mm x 297mm)"});
    private final JSpinner page;
    private final JSlider zoom = new JSlider(25, 200, 100);
    private final JLabel zoomLabel = new JLabel("100%");
    private final Preview preview = new Preview();
    private final JButton print = new JButton("In", new PhieuInBieuTuong(false));
    private final JButton pdf = new JButton("Pdf", new PhieuInBieuTuong(true));
    private boolean busy;
    private boolean closed;
    private PageFormat configuredFormat;
    private static final double PREVIEW_SCALE = 411.0 / (148 / 25.4 * 72);
    private static boolean automaticPreference, keepOpenPreference;

    public PhieuDangKyKhamManHinh(PhieuDangKyKhamDuLieu data, BooleanSupplier activeSession, java.util.function.Predicate<vn.clinicmanagement.domain.common.enums.HanhDongQuyen> authorize) {
        this.document = new PhieuDangKyKhamTrang(data); this.activeSession = activeSession; this.authorize = authorize;
        page = new JSpinner(new SpinnerNumberModel(1, 1, document.pageCount(), 1));
        setLayout(new BorderLayout()); setBackground(new Color(248,248,248));
        add(sidebar(), BorderLayout.WEST);
        JScrollPane scroll = new JScrollPane(preview); scroll.setBorder(null); scroll.getVerticalScrollBar().setUnitIncrement(20);
        add(scroll, BorderLayout.CENTER); add(bottom(), BorderLayout.SOUTH);
        print.addActionListener(e -> printDocument()); pdf.addActionListener(e -> exportPdf());
        page.addChangeListener(e -> preview.repaint()); zoom.addChangeListener(e -> updatePreview());
        paper.addActionListener(e -> resetPaper()); orientation.addActionListener(e -> resetPaper());
        a5onA4.addActionListener(e -> resetPaper());
        scope.addActionListener(e -> pageRange.setEnabled(scope.getSelectedIndex() == 2)); pageRange.setEnabled(false);
        printers.addActionListener(e -> updatePrinterOptions());
        autoPrint.setSelected(automaticPreference); keepOpen.setSelected(keepOpenPreference);
        autoPrint.setToolTipText("Tự mở hộp thoại in cho những phiếu mở tiếp theo");
        autoPrint.addActionListener(e -> automaticPreference = autoPrint.isSelected());
        keepOpen.addActionListener(e -> keepOpenPreference = keepOpen.isSelected());
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control P"), "print");
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("meta P"), "print");
        getActionMap().put("print", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { printDocument(); } });
        loadPrinters(); updatePreview();
    }
    private JPanel sidebar() {
        JPanel side = new JPanel(null); side.setPreferredSize(new Dimension(222,700)); side.setBackground(new Color(248,248,248));
        side.setBorder(new MatteBorder(0,0,0,1,Color.GRAY));
        print.setVerticalTextPosition(SwingConstants.BOTTOM); print.setHorizontalTextPosition(SwingConstants.CENTER); print.setFont(UngDungPhongChu.PHIEU_IN_NUT);
        print.setBorder(new LineBorder(new Color(95,85,185))); place(side,print,6,7,116,90);
        place(side,new JLabel("Bản in"),128,9,43,22); place(side,copies,172,7,42,24);
        place(side,autoPrint,128,48,91,25); place(side,keepOpen,128,77,91,25);
        label(side,"Máy In",6,110,true); place(side,printers,6,137,207,24); place(side,trays,6,163,207,23);
        JButton settings = new JButton("<html><u>Cấu hình máy in</u></html>"); settings.setForeground(new Color(55,50,180)); settings.setBorderPainted(false); settings.setContentAreaFilled(false); settings.setMargin(new Insets(0,0,0,0)); settings.setFont(UngDungPhongChu.SMALL);
        place(side,settings,100,190,114,23); settings.addActionListener(e -> printerSettings());
        label(side,"Tùy Chọn",6,228,true); place(side,scope,6,255,207,24);
        label(side,"Trang in :",6,285,false); place(side,pageRange,80,280,133,24); place(side,duplex,80,313,132,24);
        label(side,"Chiều giấy",6,340,false); place(side,orientation,6,364,207,24);
        label(side,"Khổ giấy",6,397,false); place(side,paper,6,422,207,24); place(side,a5onA4,6,452,209,24);
        label(side,"Xuất File…",6,485,true); pdf.setVerticalTextPosition(SwingConstants.BOTTOM); pdf.setHorizontalTextPosition(SwingConstants.CENTER); pdf.setFont(UngDungPhongChu.PHIEU_IN_NUT);
        pdf.setBorder(new LineBorder(new Color(95,85,185))); place(side,pdf,6,515,100,68);
        if (!document.data().hasConfirmedPrice()) {
            JLabel warning = new JLabel("<html>Chưa có giá chốt.<br>Phiếu chưa đủ thông tin phí.</html>"); warning.setForeground(new Color(160,65,0));
            place(side,warning,6,600,205,45);
        }
        for (Component component : side.getComponents()) {
            if (component != print && component != pdf) component.setFont(component instanceof JLabel && component.getFont().isBold() ? UngDungPhongChu.BODY_BOLD : UngDungPhongChu.BODY);
            if (component instanceof JComponent c) c.setOpaque(!(component instanceof JCheckBox) && c.isOpaque());
        }
        settings.setFont(UngDungPhongChu.SMALL);
        return side;
    }
    private static void place(JPanel p, Component c, int x, int y, int w, int h) { c.setBounds(x,y,w,h); p.add(c); }
    private static void label(JPanel p, String text, int x, int y, boolean bold) { JLabel label = new JLabel(text); label.setFont(bold ? UngDungPhongChu.BODY_BOLD : UngDungPhongChu.BODY); place(p,label,x,y,200,23); }
    private JPanel bottom() {
        JPanel bar = new JPanel(new BorderLayout()); bar.setPreferredSize(new Dimension(1000,42)); bar.setBorder(new MatteBorder(1,0,0,0,Color.GRAY));
        JPanel menu = new JPanel(new FlowLayout(FlowLayout.LEFT)); menu.setPreferredSize(new Dimension(222,42));
        JButton options = new JButton(TiepDonBieuTuong.createMenuIcon()); options.setToolTipText("Đóng cửa sổ xem trước"); options.addActionListener(e -> close()); menu.add(options); bar.add(menu,BorderLayout.WEST);
        JPanel pages = new JPanel(new FlowLayout(FlowLayout.LEFT,8,5)); JButton previous = new JButton("◀"), next = new JButton("▶");
        previous.addActionListener(e -> page.setValue(Math.max(1,(int)page.getValue()-1))); next.addActionListener(e -> page.setValue(Math.min(document.pageCount(),(int)page.getValue()+1)));
        previous.setEnabled(document.pageCount()>1); next.setEnabled(document.pageCount()>1); pages.add(previous); pages.add(page); pages.add(new JLabel("/"+document.pageCount())); pages.add(next); bar.add(pages,BorderLayout.CENTER);
        JPanel scale = new JPanel(new FlowLayout(FlowLayout.RIGHT)); JButton minus=new JButton("−"), plus=new JButton("+");
        minus.addActionListener(e -> zoom.setValue(zoom.getValue()-10)); plus.addActionListener(e -> zoom.setValue(zoom.getValue()+10));
        zoom.setPreferredSize(new Dimension(265,25)); scale.add(zoomLabel);scale.add(minus);scale.add(zoom);scale.add(plus);bar.add(scale,BorderLayout.EAST); return bar;
    }
    private void loadPrinters() {
        print.setEnabled(false);
        new SwingWorker<PrintService[],Void>() {
            @Override protected PrintService[] doInBackground() { return PrintServiceLookup.lookupPrintServices(null,null); }
            @Override protected void done() {
                try {
                    for (PrintService service : get()) printers.addItem(service);
                    printers.setRenderer(new DefaultListCellRenderer() { @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) { return super.getListCellRendererComponent(list,value instanceof PrintService s ? s.getName() : "Không có máy in",index,selected,focus); } });
                    PrintService preferred=PrintServiceLookup.lookupDefaultPrintService(); if(preferred!=null) printers.setSelectedItem(preferred);
                    print.setEnabled(printers.getItemCount()>0); updatePrinterOptions();
                    if (autoPrint.isSelected() && isShowing() && !closed && printers.getItemCount() > 0) printDocument();
                } catch (Exception error) { print.setEnabled(false); }
            }
        }.execute();
    }
    private void updatePrinterOptions() {
        PrintService service=(PrintService)printers.getSelectedItem(); trays.removeAllItems(); trays.addItem(null);
        if(service!=null) {
            Object values=service.getSupportedAttributeValues(Media.class,null,null);
            if(values instanceof Media[] media) for(Media value:media) if(value instanceof MediaTray tray) trays.addItem(tray);
        }
        trays.setEnabled(trays.getItemCount()>1);
        duplex.setEnabled(service!=null && service.isAttributeValueSupported(Sides.DUPLEX,null,null));
        if(!duplex.isEnabled()) duplex.setSelected(false);
    }
    private void resetPaper() { configuredFormat = null; updatePreview(); }
    private double[] dimensions() {
        if (configuredFormat != null) return new double[]{configuredFormat.getWidth(), configuredFormat.getHeight()};
        boolean a4=paper.getSelectedIndex()==1 || a5onA4.isSelected();
        double w=(a4?210:148)/25.4*72,h=(a4?297:210)/25.4*72;
        return orientation.getSelectedIndex()==0 ? new double[]{w,h} : new double[]{h,w};
    }
    private PageFormat pageFormat() {
        if (configuredFormat != null) return (PageFormat)configuredFormat.clone();
        double[] dims=dimensions(); Paper sheet=new Paper(); sheet.setSize(dims[0],dims[1]); sheet.setImageableArea(0,0,dims[0],dims[1]); PageFormat format=new PageFormat();format.setPaper(sheet); return format;
    }
    public static int[] parsePages(String value,int count) {
        TreeSet<Integer> result=new TreeSet<>();
        for(String part:value.trim().split(",")) {
            String[] pair=part.trim().split("-",-1);
            if(pair.length>2) throw new IllegalArgumentException("Trang in không hợp lệ. Ví dụ: 1,3-5.");
            try { int first=Integer.parseInt(pair[0].trim()),last=pair.length==1?first:Integer.parseInt(pair[1].trim());
                if(first<1||last<first||last>count) throw new NumberFormatException();
                for(int i=first;i<=last;i++) result.add(i-1);
            } catch(NumberFormatException error) { throw new IllegalArgumentException("Trang in phải nằm trong khoảng 1 đến "+count+"."); }
        }
        return result.stream().mapToInt(Integer::intValue).toArray();
    }
    private int[] selectedPages() {
        if(scope.getSelectedIndex()==1) return new int[]{(int)page.getValue()-1};
        if(scope.getSelectedIndex()==2) return parsePages(pageRange.getText(),document.pageCount());
        return java.util.stream.IntStream.range(0,document.pageCount()).toArray();
    }
    private boolean allowed() { if (closed) return false; if(activeSession.getAsBoolean()) return true; message("Phiên làm việc đã hết hạn. Vui lòng xác thực lại."); return false; }
    private void printerSettings() {
        if(busy || !allowed()) return;
        try { PrinterJob job=PrinterJob.getPrinterJob(); if(printers.getSelectedItem()!=null) job.setPrintService((PrintService)printers.getSelectedItem()); PageFormat selected = job.pageDialog(pageFormat());
            boolean landscape = selected.getWidth() > selected.getHeight();
            orientation.setSelectedIndex(landscape ? 1 : 0);
            paper.setSelectedIndex(Math.max(selected.getWidth(), selected.getHeight()) > 700 ? 1 : 0);
            configuredFormat = selected; updatePreview(); }
        catch(PrinterException error) { message("Không thể mở cấu hình máy in."); }
    }
    private void printDocument() { requestAction(vn.clinicmanagement.domain.common.enums.HanhDongQuyen.IN, this::preparePrint); }
    private void exportPdf() { requestAction(vn.clinicmanagement.domain.common.enums.HanhDongQuyen.XUAT_FILE, this::prepareExport); }
    private void requestAction(vn.clinicmanagement.domain.common.enums.HanhDongQuyen action, Runnable continuation) {
        if (busy || !allowed()) return;
        busy = true; print.setEnabled(false); pdf.setEnabled(false);
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() { return authorize.test(action); }
            @Override protected void done() {
                busy = false; print.setEnabled(printers.getItemCount() > 0); pdf.setEnabled(true);
                try { if (!allowed()) return; if (!get()) { message("Không được phép in/xuất phiếu trong phạm vi hiện tại. Kiểm tra quyền In hoặc Xuất file của phân hệ tiếp đón."); return; } continuation.run(); }
                catch (Exception error) { message("Không thể kiểm tra quyền thao tác phiếu."); }
            }
        }.execute();
    }
    private void preparePrint() {
        if(busy || !allowed()) return;
        if(printers.getSelectedItem()==null) { message("Chưa có máy in. Bạn có thể xuất PDF.");return; }
        try {
            int[] pages=selectedPages(); PrinterJob job=PrinterJob.getPrinterJob(); job.setPrintService((PrintService)printers.getSelectedItem());
            job.setJobName("Phiếu khám - "+document.data().patientCode());
            PrintRequestAttributeSet attributes=new HashPrintRequestAttributeSet(); attributes.add(new Copies((int)copies.getValue()));
            attributes.add(duplex.isSelected()?Sides.DUPLEX:Sides.ONE_SIDED);
            attributes.add(paper.getSelectedIndex()==1 || a5onA4.isSelected()?MediaSizeName.ISO_A4:MediaSizeName.ISO_A5);
            attributes.add(orientation.getSelectedIndex()==0?OrientationRequested.PORTRAIT:OrientationRequested.LANDSCAPE);
            // Khay giấy chỉ áp dụng nếu người dùng chọn; khổ trang vẫn được xác định bởi PageFormat.
            if(trays.getSelectedItem()!=null) attributes.add((MediaTray)trays.getSelectedItem());
            job.setPrintable((g,f,index) -> index>=pages.length?Printable.NO_SUCH_PAGE:document.print(g,f,pages[index]),job.validatePage(pageFormat()));
            if(!job.printDialog(attributes) || !allowed()) return;
            runTask(() -> { if (!closed && activeSession.getAsBoolean()) job.print(attributes); else throw new IllegalStateException("Phiên không còn hiệu lực."); return null; },true,"Đã gửi phiếu tới máy in.");
        } catch(Exception error) { message(error instanceof IllegalArgumentException?error.getMessage():"Không thể chuẩn bị bản in."); }
    }
    private void prepareExport() {
        if(busy || !allowed()) return;
        final int[] pages; try { pages=selectedPages(); } catch(IllegalArgumentException error) { message(error.getMessage()); return; }
        JFileChooser chooser=new JFileChooser(); chooser.setSelectedFile(new java.io.File("PhieuKham-"+document.data().visitId()+".pdf"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Tài liệu PDF","pdf"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;
        Path file=chooser.getSelectedFile().toPath(); if(!file.toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) file=Path.of(file+".pdf");
        if(Files.exists(file) && JOptionPane.showConfirmDialog(this,"Thay thế file PDF đã tồn tại?","Xuất PDF",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
        if(!allowed()) return;
        final Path output=file; PageFormat format=pageFormat();
        runTask(() -> { PhieuDangKyKhamPdf.write(document,output,pages,format); return null; },false,"Đã xuất PDF: "+output);
    }
    private void runTask(java.util.concurrent.Callable<Void> task,boolean printing,String success) {
        busy=true; print.setEnabled(false);pdf.setEnabled(false);
        new SwingWorker<Void,Void>() {
            @Override protected Void doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                busy=false;print.setEnabled(printers.getItemCount()>0);pdf.setEnabled(true);
                try {get(); if(printing && !keepOpen.isSelected()) close(); else message(success);}
                catch(Exception error) {message("Không thể hoàn tất "+(printing?"in phiếu. Kiểm tra máy in.":"xuất PDF. Kiểm tra đường dẫn và quyền ghi file."));}
            }
        }.execute();
    }
    private void updatePreview() { zoomLabel.setText(zoom.getValue()+"%");double[] dims=dimensions();preview.setPreferredSize(new Dimension((int)(dims[0]*zoom.getValue()/100.0*PREVIEW_SCALE)+120,(int)(dims[1]*zoom.getValue()/100.0*PREVIEW_SCALE)+118));preview.revalidate();preview.repaint(); }
    private void message(String text) { JOptionPane.showMessageDialog(this,text,"Phiếu đăng ký khám",JOptionPane.INFORMATION_MESSAGE); }
    private void close() { closed = true; Window window=SwingUtilities.getWindowAncestor(this);if(window!=null) window.dispose(); }
    private final class Preview extends JPanel {
        Preview() {setBackground(new Color(248,248,248));}
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics); Graphics2D g=(Graphics2D)graphics.create();double[] dims=dimensions();double scale=zoom.getValue()/100.0*PREVIEW_SCALE;
            int w=(int)(dims[0]*scale),h=(int)(dims[1]*scale),x=Math.max(12,(getWidth()-w)/2),y=60;
            g.setColor(Color.WHITE);g.fillRect(x,y,w,h);g.setColor(Color.BLACK);g.drawRect(x,y,w,h);
            Graphics2D content = (Graphics2D) g.create();
            content.translate(x,y); content.scale(scale,scale);
            document.print(content,pageFormat(),(int)page.getValue()-1); content.dispose();
            g.setColor(Color.BLACK); g.setStroke(new BasicStroke(1)); g.drawRect(x,y,w,h); g.dispose();
        }
    }
    public static JDialog open(Window owner,PhieuDangKyKhamDuLieu data,BooleanSupplier active,java.util.function.Predicate<vn.clinicmanagement.domain.common.enums.HanhDongQuyen> authorize) {
        JDialog dialog=new JDialog(owner,"EHC - Phiếu khám - ["+data.patientCode()+"] - PK_017_A6_PhieuKham.doc",Dialog.ModalityType.MODELESS);
        PhieuDangKyKhamManHinh panel=new PhieuDangKyKhamManHinh(data,active,authorize);dialog.setContentPane(panel);dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Rectangle screen=GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();dialog.setSize(Math.min(1440,screen.width),Math.min(770,screen.height));dialog.setLocationRelativeTo(owner);
        dialog.addWindowListener(new WindowAdapter(){@Override public void windowOpened(WindowEvent e){if(panel.autoPrint.isSelected() && panel.printers.getItemCount()>0) panel.printDocument();} @Override public void windowClosed(WindowEvent e) { panel.closed = true; }});
        dialog.setVisible(true); return dialog;
    }
}
