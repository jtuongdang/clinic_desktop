package vn.clinicmanagement.desktop.modules.print.inventory;


import vn.clinicmanagement.desktop.modules.inventory.*;
import vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.Bang;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi.Kho;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi.Phieu;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi;
import vn.clinicmanagement.application.inventory.TuTrucCongKetNoi.*;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocBieuTuong;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Màn hình toàn màn hình "PHIẾU NHẬP BÙ CƠ SỐ TỦ TRỰC" mở khi bấm Lưu
 * từ màn hình Nhập Xuất Bù Cơ Số Tủ Trực.
 */
public final class TuTrucPhieuNhapBuCoSoHopThoai extends JDialog {
    private static final Color GRID = new Color(207, 210, 215);
    private static final Color SELECTED = new Color(0, 145, 255);
    private static final Color BLUE_VAL = new Color(0, 70, 175);
    private static final Color RED_TITLE = new Color(175, 0, 0);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DecimalFormat NUM_FMT = createNumFormat();

    private static final String[] COLUMNS = {
            "Trạng Thái", "STT", "Mã Vật Tư", "Tên Vật Tư", "SL Yêu Cầu", "SL Phát",
            "Đơn Giá", "VAT", "Thành Tiền", "SĐK", "Số Lô", "Đơn Vị", "Hạn Sử Dụng"
    };
    private static final int[] COL_WIDTHS = {85, 45, 95, 250, 80, 70, 80, 55, 90, 80, 75, 65, 95};

    record DongPhieuNhapBu(
            String status, int stt, String code, String name,
            BigDecimal reqQty, BigDecimal issuedQty, BigDecimal unitPrice,
            BigDecimal vat, BigDecimal totalAmount, String regNo,
            String batch, String unit, String expiry
    ) {}

    private final Kho retailCabinet;
    private final String wholesaleName;
    private final String ticketCode;
    private final String createdTimeText;
    private final boolean approvalMode;
    private final TuTrucCongKetNoi data;
    private final Phieu ticket;
    private final Runnable refreshed;
    private ChuyenKho transfer;
    private boolean busy;
    private UUID pendingOperationKey;
    private int transferLoadGeneration;
    private final List<DongPhieuNhapBu> allLines = new ArrayList<>();
    private List<DongPhieuNhapBu> displayed = List.of();

    private final JLabel statusRight = new JLabel("Đã gửi yêu cầu");
    private final JLabel approvedDateVal = valueLabel();
    private final JLabel approvedByVal = valueLabel();
    private final JLabel issuedDateVal = valueLabel();
    private final JLabel issuedByVal = valueLabel();
    private final JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
    final JTextField search = new JTextField() {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                g.setFont(getFont());
                g.setColor(new Color(102, 102, 81));
                g.drawString("Tìm kiếm (Ctrl+F)", 6,
                        (getHeight() - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent());
            }
        }
    };
    final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    final JTable table;

    public TuTrucPhieuNhapBuCoSoHopThoai(
            Window owner,TuTrucCongKetNoi data,Kho retailCabinet,Phieu ticket,Bang details,Runnable refreshed) {
        this(owner,
                data,
                retailCabinet,
                ticket == null ? "" : Objects.toString(ticket.location(), ""),
                ticket == null ? "" : Objects.toString(ticket.code(), ""),
                ticket == null || ticket.createdAt() == null ? TIME_FMT.format(LocalDateTime.now()) : TIME_FMT.format(ticket.createdAt()),
                ticket == null ? "Đã gửi yêu cầu" : TuTrucVatTuManHinh.status(ticket),
                true,ticket,refreshed);
        buildLinesFromBang(details, statusRight.getText());
        applyFilter();
    }

    private TuTrucPhieuNhapBuCoSoHopThoai(
            Window owner,TuTrucCongKetNoi data,Kho retailCabinet,String wholesaleName,String ticketCode,
            String createdTimeText,String initialStatus,boolean approvalMode,Phieu ticket,Runnable refreshed) {
        super(owner, "PHẦN MỀM QUẢN LÝ BỆNH VIỆN", ModalityType.MODELESS);
        this.retailCabinet = retailCabinet;
        this.data=data;
        this.ticket=ticket;
        this.refreshed=refreshed==null?()->{}:refreshed;
        this.wholesaleName = wholesaleName;
        this.ticketCode = (ticketCode == null || ticketCode.isBlank()) ? "—" : ticketCode;
        this.createdTimeText = createdTimeText;
        this.approvalMode = approvalMode;
        this.statusRight.setText(initialStatus);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        JPanel top = new JPanel(new BorderLayout());
        top.add(buildToolbar(), BorderLayout.NORTH);
        top.add(buildInfoPanel(), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        table = buildTable();
        JScrollPane scroll = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(BorderFactory.createLineBorder(GRID));
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);

        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("control F"), "find");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("meta F"), "find");
        getRootPane().getActionMap().put("find", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                search.requestFocusInWindow();
                search.selectAll();
            }
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "closeWindow");
        getRootPane().getActionMap().put("closeWindow", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { dispose(); }
        });
        if(data!=null&&ticket!=null)refreshTransfer();
    }

    private void buildLinesFromBang(Bang details, String lineStatus) {
        if (details == null || details.rows() == null) return;
        int stt = 1;
        for (List<Object> r : details.rows()) {
            String code = r.size() > 0 ? Objects.toString(r.get(0), "") : "";
            String name = r.size() > 1 ? Objects.toString(r.get(1), "") : "";
            String unit = r.size() > 2 ? Objects.toString(r.get(2), "") : "";
            String batch = r.size() > 3 ? Objects.toString(r.get(3), "") : "";
            String expiry = r.size() > 4 ? Objects.toString(r.get(4), "") : "";
            BigDecimal qty = toBigDecimal(r.size() > 5 ? r.get(5) : null);
            BigDecimal price = toBigDecimal(r.size() > 6 ? r.get(6) : null);
            BigDecimal amount = toBigDecimal(r.size() > 7 ? r.get(7) : null);
            allLines.add(new DongPhieuNhapBu(
                    lineStatus, stt++, code, name,
                    qty, qty, price, BigDecimal.ZERO, amount,
                    "", batch, unit, expiry));
        }
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return new BigDecimal(n.toString());
        if (o == null) return BigDecimal.ZERO;
        try { return new BigDecimal(o.toString().trim().replace(",", "")); }
        catch (NumberFormatException ex) { return BigDecimal.ZERO; }
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(248, 248, 248));
        bar.setBorder(new MatteBorder(0, 0, 1, 0, GRID));
        bar.setPreferredSize(new Dimension(100, 56));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        left.setOpaque(false);

        JButton menuBtn = new JButton(TuTrucThanhCongCu.scaled(NhaThuocBieuTuong.createMenuIcon(), 22));
        menuBtn.setPreferredSize(new Dimension(40, 48));
        menuBtn.setFocusPainted(false);
        menuBtn.setContentAreaFilled(false);
        menuBtn.setBorder(new EmptyBorder(2, 4, 2, 4));
        menuBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPopupMenu sysMenu = new JPopupMenu();
        JMenuItem miPrint = new JMenuItem("In phiếu nhập bù");
        miPrint.setFont(UngDungPhongChu.BODY);
        miPrint.addActionListener(e -> printTicket());
        JMenuItem miExport = new JMenuItem("Xuất CSV");
        miExport.setFont(UngDungPhongChu.BODY);
        miExport.addActionListener(e -> exportCsv());
        JMenuItem miClose = new JMenuItem("Đóng");
        miClose.setFont(UngDungPhongChu.BODY);
        miClose.addActionListener(e -> dispose());
        sysMenu.add(miPrint);
        sysMenu.add(miExport);
        sysMenu.addSeparator();
        sysMenu.add(miClose);
        menuBtn.addActionListener(e -> sysMenu.show(menuBtn, 0, menuBtn.getHeight()));

        JButton printBtn = toolBtn("In ▾", printerIcon(), 54);
        JPopupMenu printMenu = new JPopupMenu();
        JMenuItem miPrintDirect = new JMenuItem("In phiếu");
        miPrintDirect.setFont(UngDungPhongChu.BODY);
        miPrintDirect.addActionListener(e -> printTicket());
        JMenuItem miPrintCsv = new JMenuItem("Xuất CSV");
        miPrintCsv.setFont(UngDungPhongChu.BODY);
        miPrintCsv.addActionListener(e -> exportCsv());
        printMenu.add(miPrintDirect);
        printMenu.add(miPrintCsv);
        printBtn.addActionListener(e -> printMenu.show(printBtn, 0, printBtn.getHeight()));

        JButton refreshBtn = toolBtn("Làm Mới", refreshIcon(20), 64);
        refreshBtn.addActionListener(e -> {if(data!=null&&ticket!=null)refreshTransfer();else applyFilter();});

        JButton detailBtn = toolBtn("Xem Thông Tin Chi Tiết", detailIcon(), 138);
        detailBtn.addActionListener(e -> showSelectedInfo());

        left.add(menuBtn);
        left.add(printBtn);
        left.add(refreshBtn);
        left.add(detailBtn);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new GridLayout(2, 1));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(4, 8, 4, 16));
        String headingText = approvalMode
                ? "PHIẾU NHẬP BÙ CƠ SỐ TỦ TRỰC - " + ticketCode
                : "PHIẾU NHẬP BÙ CƠ SỐ TỦ TRỰC  " + ticketCode;
        JLabel title = new JLabel(headingText, SwingConstants.RIGHT);
        title.setFont(UngDungPhongChu.SECTION);
        title.setForeground(RED_TITLE);
        statusRight.setFont(UngDungPhongChu.BODY);
        statusRight.setForeground(RED_TITLE);
        statusRight.setHorizontalAlignment(SwingConstants.RIGHT);
        right.add(title);
        right.add(statusRight);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    private JPanel buildInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(248, 249, 251));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, GRID),
                new EmptyBorder(6, 12, 6, 12)));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(2, 4, 2, 16);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        String user = Objects.toString(NguoiDungPhien.getInstance().getFullName(), "").trim();
        if (user.isEmpty()) user = "Quản Trị Hệ Thống";
        String retailName = retailCabinet == null ? "" : retailCabinet.name();

        addField(panel, gc, 0, 0, "Kho lẻ:", valueLabel(retailName));
        addField(panel, gc, 2, 0, "Ngày lập:", valueLabel(createdTimeText));
        addField(panel, gc, 4, 0, "Người lập:", valueLabel(user));

        addField(panel, gc, 0, 1, "Ngày duyệt:", approvedDateVal);
        addField(panel, gc, 2, 1, "Người duyệt:", approvedByVal);
        addField(panel, gc, 4, 1, "Kho chẵn:", valueLabel(wholesaleName));

        addField(panel, gc, 0, 2, "Ngày xuất:", issuedDateVal);
        addField(panel, gc, 2, 2, "Người xuất:", issuedByVal);

        return panel;
    }

    private static JLabel valueLabel() {
        return valueLabel("");
    }

    private static JLabel valueLabel(String value) {
        JLabel val = new JLabel(value == null || value.isBlank() ? " " : value);
        val.setFont(UngDungPhongChu.BODY);
        val.setForeground(BLUE_VAL);
        val.setPreferredSize(new Dimension(170, 20));
        val.setBorder(new MatteBorder(0, 0, 1, 0, new Color(180, 185, 195)));
        return val;
    }

    private static void addField(JPanel panel, GridBagConstraints gc, int col, int row, String label, JLabel val) {
        gc.gridx = col;
        gc.gridy = row;
        gc.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(UngDungPhongChu.BODY);
        lbl.setPreferredSize(new Dimension(78, 20));
        panel.add(lbl, gc);

        gc.gridx = col + 1;
        gc.weightx = 1.0;
        panel.add(val, gc);
    }

    private JTable buildTable() {
        JTable t = new JTable(model) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int y = getRowCount() * getRowHeight();
                g.setColor(GRID);
                for (int line = y; line < getHeight(); line += getRowHeight()) {
                    g.drawLine(0, line, getWidth(), line);
                }
                int x = 0;
                for (int c = 0; c < getColumnCount(); c++) {
                    x += getColumnModel().getColumn(c).getWidth();
                    g.drawLine(x - 1, y, x - 1, getHeight());
                }
            }
        };
        t.setRowHeight(24);
        t.setFont(UngDungPhongChu.BODY);
        t.setGridColor(GRID);
        t.setShowGrid(true);
        t.setIntercellSpacing(new Dimension(1, 1));
        t.setFillsViewportHeight(true);
        t.setBackground(Color.WHITE);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        t.getTableHeader().setFont(UngDungPhongChu.BODY_BOLD);
        t.getTableHeader().setPreferredSize(new Dimension(100, 28));
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setBackground(new Color(250, 250, 250));

        for (int i = 0; i < COL_WIDTHS.length; i++) {
            var col = t.getColumnModel().getColumn(i);
            col.setPreferredWidth(COL_WIDTHS[i]);
            col.setWidth(COL_WIDTHS[i]);
        }

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected,
                                                           boolean focus, int row, int column) {
                super.getTableCellRendererComponent(tbl, value, selected, false, row, column);
                setFont(UngDungPhongChu.BODY);
                setBorder(new EmptyBorder(0, 5, 0, 5));
                if (column == 1 || column == 9 || column == 10 || column == 11 || column == 12) {
                    setHorizontalAlignment(CENTER);
                } else if (column >= 4 && column <= 8) {
                    setHorizontalAlignment(RIGHT);
                } else {
                    setHorizontalAlignment(LEFT);
                }
                if (selected) {
                    setBackground(SELECTED);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(Color.WHITE);
                    setForeground(Color.BLACK);
                }
                return this;
            }
        });
        return t;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(245, 246, 248));
        footer.setBorder(new MatteBorder(1, 0, 0, 0, GRID));
        footer.setPreferredSize(new Dimension(100, 36));

        JPanel leftSearch = new JPanel(new BorderLayout());
        leftSearch.setOpaque(false);
        leftSearch.setBorder(new EmptyBorder(4, 6, 4, 6));
        leftSearch.setPreferredSize(new Dimension(280, 28));
        search.setFont(UngDungPhongChu.BODY);
        search.setBackground(new Color(255, 255, 193));
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(172, 172, 151)),
                new EmptyBorder(0, 6, 0, 4)));
        JButton find = new JButton(TuTrucThanhCongCu.scaled(NhaThuocBieuTuong.createSearchIcon(), 16));
        find.setPreferredSize(new Dimension(26, 24));
        find.addActionListener(e -> applyFilter());
        leftSearch.add(search, BorderLayout.CENTER);
        leftSearch.add(find, BorderLayout.EAST);
        footer.add(leftSearch, BorderLayout.WEST);

        rightBtns.setOpaque(false);
        rebuildRightButtons();
        footer.add(rightBtns, BorderLayout.EAST);

        return footer;
    }

    private void rebuildRightButtons() {
        rightBtns.removeAll();
        if (data != null && transfer != null) {
            JButton printBtn = borderedFooterBtn("In Phiếu", printerIcon());
            printBtn.addActionListener(e -> printTicket());rightBtns.add(printBtn);
            if(Set.of("REQUESTED","PARTIALLY_APPROVED").contains(transfer.status())){
                JButton approveBtn=footerActionBtn("Duyệt",arrowCircleIcon());approveBtn.addActionListener(e->approveAll());rightBtns.add(approveBtn);
                JButton approveLine=footerActionBtn("Duyệt dòng chọn",arrowCircleIcon());approveLine.addActionListener(e->approveSelectedLine());rightBtns.add(approveLine);
                JButton cancel=footerActionBtn("Hủy Yêu Cầu",redCrossIcon());cancel.addActionListener(e->cancelRequest());rightBtns.add(cancel);
            }else if(Set.of("APPROVED","PARTIALLY_ISSUED").contains(transfer.status())){
                JButton issueBtn=footerActionBtn("Xuất Kho (FEFO)",pinkCheckIcon());issueBtn.addActionListener(e->issueStock());rightBtns.add(issueBtn);
                if("APPROVED".equals(transfer.status())){JButton cancel=footerActionBtn("Hủy Yêu Cầu",redCrossIcon());cancel.addActionListener(e->cancelRequest());rightBtns.add(cancel);}
            }
            if(Set.of("ISSUED","PARTIALLY_ISSUED","PARTIALLY_RECEIVED").contains(transfer.status())
                    &&transfer.lots().stream().anyMatch(l->l.issued().compareTo(l.received())>0)){
                JButton receive=footerActionBtn("Nhận vào tủ",pinkCheckIcon());receive.addActionListener(e->receiveOutstanding());rightBtns.add(receive);
            }
            if("RECEIVED".equals(transfer.status())){
                JButton reverse=footerActionBtn("Đảo phiếu",redCrossIcon());reverse.addActionListener(e->reverseTransfer());rightBtns.add(reverse);
            }
        } else if (!approvalMode) {
            JButton cancelReqBtn = footerActionBtn("Hủy Yêu Cầu", refreshIcon(18));
            cancelReqBtn.addActionListener(e -> cancelRequest());
            rightBtns.add(cancelReqBtn);
        }
        rightBtns.revalidate();
        rightBtns.repaint();
    }

    private void applyFilter() {
        String q = TuTrucVatTuManHinh.normalize(search.getText());
        displayed = allLines.stream()
                .filter(l -> q.isEmpty() || TuTrucVatTuManHinh.normalize(
                        l.code() + " " + l.name() + " " + l.status() + " " + l.batch()).contains(q))
                .toList();
        model.setRowCount(0);
        for (DongPhieuNhapBu l : displayed) {
            model.addRow(new Object[]{
                    l.status(),
                    l.stt(),
                    l.code(),
                    l.name(),
                    NUM_FMT.format(l.reqQty()),
                    NUM_FMT.format(l.issuedQty()),
                    NUM_FMT.format(l.unitPrice()),
                    NUM_FMT.format(l.vat()),
                    NUM_FMT.format(l.totalAmount()),
                    l.regNo(),
                    l.batch(),
                    l.unit(),
                    l.expiry()
            });
        }
    }

    private void showSelectedInfo() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= displayed.size()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một dòng vật tư để xem chi tiết.",
                    "Xem Thông Tin Chi Tiết", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DongPhieuNhapBu item = displayed.get(row);
        JOptionPane.showMessageDialog(this,
                "Mã vật tư: " + item.code() + "\nTên vật tư: " + item.name()
                        + "\nTrạng thái: " + item.status()
                        + "\nSL yêu cầu: " + NUM_FMT.format(item.reqQty()),
                "Thông Tin Chi Tiết", JOptionPane.INFORMATION_MESSAGE);
    }

    private void cancelRequest() {
        if(transfer==null)return;
        int confirm=JOptionPane.showConfirmDialog(this,"Hủy yêu cầu chưa xuất " + ticketCode + "?","Hủy Yêu Cầu",JOptionPane.YES_NO_OPTION);
        if(confirm==JOptionPane.YES_OPTION)sendTransfer(()->data.cancel(command("Hủy yêu cầu")));
    }

    private void approveAll() {
        if(transfer==null)return;
        var lines=transfer.lines().stream().map(l->new CapDuyet(l.id(),l.requested())).toList();
        sendTransfer(()->data.approve(new LenhDuyet(command("Duyệt phiếu"),lines)));
    }
    private void approveSelectedLine(){
        int row=table.getSelectedRow();if(transfer==null||row<0||row>=displayed.size()){
            JOptionPane.showMessageDialog(this,"Chọn dòng vật tư cần duyệt.","Duyệt dòng",JOptionPane.INFORMATION_MESSAGE);return;}
        DongPhieuNhapBu shown=displayed.get(row);int index=shown.stt()-1;if(index<0||index>=transfer.lines().size())return;
        var line=transfer.lines().get(index);String value=JOptionPane.showInputDialog(this,"Số lượng duyệt (tối đa "+NUM_FMT.format(line.requested())+"):",NUM_FMT.format(line.requested().subtract(line.approved())));
        if(value==null)return;
        try{BigDecimal quantity=new BigDecimal(value.trim().replace(",",""));if(quantity.signum()<0||quantity.compareTo(line.requested())>0)throw new NumberFormatException();
            sendTransfer(()->data.approve(new LenhDuyet(command("Duyệt một phần"),List.of(new CapDuyet(line.id(),quantity)))));
        }catch(NumberFormatException ex){JOptionPane.showMessageDialog(this,"Số lượng duyệt không hợp lệ.","Duyệt dòng",JOptionPane.ERROR_MESSAGE);}
    }

    private void issueStock() {
        sendTransfer(()->data.issue(new LenhXuat(command("Xuất kho FEFO"),List.of())));
    }

    private ThaoTac command(String reason){
        var context=KhamBenhPhienNguCanh.current();
        if(pendingOperationKey==null)pendingOperationKey=UUID.randomUUID();
        return new ThaoTac(ticket.id(),transfer.version(),pendingOperationKey,reason,context);
    }
    private void refreshTransfer(){
        var context=KhamBenhPhienNguCanh.current();
        if(context==null||ticket==null||!"Nhập bù".equalsIgnoreCase(ticket.type()))return;
        int generation=++transferLoadGeneration;
        TuTrucVatTuManHinh.async(()->data.getTransfer(context,retailCabinet.id(),ticket.id()),value->{
            if(generation!=transferLoadGeneration||!TuTrucVatTuManHinh.current(context.sessionId().toString()))return;
            transfer=value;allLines.clear();int stt=1;
            for(var line:value.lines()){
                String batch=value.lots().stream().filter(a->a.lineId()==line.id()).map(LoChuyenKho::batch).distinct().reduce((a,b)->a+", "+b).orElse("");
                allLines.add(new DongPhieuNhapBu(statusText(value.status()),stt++,line.code(),line.name(),line.requested(),line.issued(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,"",batch,line.unit(),""));
            }
            statusRight.setText(statusText(value.status()));applyFilter();rebuildRightButtons();refreshed.run();
        },error->{if(generation!=transferLoadGeneration||!TuTrucVatTuManHinh.current(context.sessionId().toString()))return;statusRight.setText("Không tải được trạng thái chứng từ");rightBtns.removeAll();rightBtns.revalidate();rightBtns.repaint();});
    }
    private static String statusText(String value){return switch(value){case "REQUESTED"->"Chờ duyệt";case "PARTIALLY_APPROVED"->"Duyệt một phần";case "APPROVED"->"Đã duyệt";case "PARTIALLY_ISSUED"->"Xuất một phần";case "ISSUED"->"Đã xuất kho";case "PARTIALLY_RECEIVED"->"Nhận một phần";case "RECEIVED"->"Đã nhập tủ";case "CANCELLED"->"Đã hủy";case "REVERSED"->"Đã đảo";default->value;};}
    private void receiveOutstanding(){
        if(transfer==null)return;
        var allocations=new ArrayList<PhanBoNhan>();
        for(var lot:transfer.lots()){
            BigDecimal outstanding=lot.issued().subtract(lot.received());if(outstanding.signum()<=0)continue;
            String value=JOptionPane.showInputDialog(this,"Lô "+lot.batch()+" — số lượng thực nhận (tối đa "+NUM_FMT.format(outstanding)+"):",NUM_FMT.format(outstanding));
            if(value==null)return;
            try{BigDecimal quantity=new BigDecimal(value.trim().replace(",",""));if(quantity.signum()<0||quantity.compareTo(outstanding)>0)throw new NumberFormatException();
                String reason=null;if(quantity.compareTo(outstanding)<0){reason=JOptionPane.showInputDialog(this,"Lý do nhận thiếu/hỏng (bắt buộc):");if(reason==null)return;}
                allocations.add(new PhanBoNhan(lot.id(),quantity,reason));
            }catch(NumberFormatException ex){JOptionPane.showMessageDialog(this,"Số lượng nhận không hợp lệ.","Nhận vật tư",JOptionPane.ERROR_MESSAGE);return;}
        }
        if(allocations.isEmpty())return;
        sendTransfer(()->data.receive(new LenhNhan(command("Xác nhận số lượng nhận thực tế"),allocations)));
    }
    private void reverseTransfer(){
        if(transfer==null)return;
        String reason=JOptionPane.showInputDialog(this,"Lý do đảo phiếu (bắt buộc):","Đảo phiếu",JOptionPane.WARNING_MESSAGE);
        if(reason==null||reason.isBlank())return;
        sendTransfer(()->data.reverse(command(reason.trim())));
    }
    private void sendTransfer(java.util.function.Supplier<vn.clinicmanagement.application.common.KetQua<ChuyenKho>> operation){
        var context=KhamBenhPhienNguCanh.current();if(busy||data==null||context==null)return;
        String session=context.sessionId().toString();busy=true;setTransferButtonsEnabled(false);
        TuTrucVatTuManHinh.async(operation,result->{busy=false;if(!TuTrucVatTuManHinh.current(session))return;setTransferButtonsEnabled(true);
            if(result!=null&&result.isSuccess()){pendingOperationKey=null;refreshTransfer();}
            else {String message=result==null?"Không nhận được kết quả từ máy chủ.":result.getError().map(e->e.message()).orElse("Thao tác chưa hoàn tất.");statusRight.setText(message);rebuildRightButtons();}
        },error->{busy=false;if(!TuTrucVatTuManHinh.current(session))return;setTransferButtonsEnabled(true);statusRight.setText("Chưa xác nhận được kết quả. Tải lại chứng từ trước khi thử lại.");rebuildRightButtons();});
    }
    private void setTransferButtonsEnabled(boolean enabled){for(Component c:rightBtns.getComponents())c.setEnabled(enabled);}

    private static JButton footerActionBtn(String text, Icon icon) {
        JButton b = new JButton(text, icon);
        b.setFont(UngDungPhongChu.BODY);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorder(new EmptyBorder(2, 6, 2, 6));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JButton borderedFooterBtn(String text, Icon icon) {
        JButton b = new JButton(text, icon);
        b.setFont(UngDungPhongChu.BODY);
        b.setFocusPainted(false);
        b.setBackground(new Color(245, 246, 248));
        b.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(new Color(185, 188, 194)),
                new EmptyBorder(3, 10, 3, 10)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static Icon docIcon() {
        return new Icon() {
            @Override public int getIconWidth() { return 16; }
            @Override public int getIconHeight() { return 16; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.translate(x, y);
                g.setColor(Color.WHITE);
                g.fillRect(2, 1, 11, 14);
                g.setColor(new Color(70, 120, 185));
                g.drawRect(2, 1, 11, 14);
                g.drawLine(4, 5, 10, 5);
                g.drawLine(4, 8, 10, 8);
                g.drawLine(4, 11, 8, 11);
                g.dispose();
            }
        };
    }

    private static Icon pinkCheckIcon() {
        return new Icon() {
            @Override public int getIconWidth() { return 16; }
            @Override public int getIconHeight() { return 16; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.translate(x, y);
                g.setColor(new Color(220, 45, 120));
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawPolyline(new int[]{2, 6, 14}, new int[]{9, 13, 3}, 3);
                g.dispose();
            }
        };
    }

    private static Icon redCrossIcon() {
        return new Icon() {
            @Override public int getIconWidth() { return 16; }
            @Override public int getIconHeight() { return 16; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.translate(x, y);
                g.setColor(new Color(205, 45, 35));
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(3, 3, 13, 13);
                g.drawLine(13, 3, 3, 13);
                g.dispose();
            }
        };
    }

    private static Icon arrowCircleIcon() {
        return new Icon() {
            @Override public int getIconWidth() { return 18; }
            @Override public int getIconHeight() { return 18; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.translate(x, y);
                g.setColor(new Color(0, 70, 140));
                g.setStroke(new BasicStroke(1.6f));
                g.drawOval(1, 1, 15, 15);
                g.drawLine(5, 9, 12, 9);
                g.drawPolyline(new int[]{9, 13, 9}, new int[]{6, 9, 12}, 3);
                g.dispose();
            }
        };
    }

    private void printTicket() {
        try {
            table.print(JTable.PrintMode.FIT_WIDTH);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không in được phiếu.", "In", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void exportCsv() {
        JFileChooser picker = new JFileChooser();
        picker.setSelectedFile(new File("phieu-nhap-bu-" + ticketCode + ".csv"));
        if (picker.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = picker.getSelectedFile();
        try (var writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            for (int r = -1; r < model.getRowCount(); r++) {
                for (int c = 0; c < model.getColumnCount(); c++) {
                    if (c > 0) writer.write(',');
                    String cell = String.valueOf(r < 0 ? model.getColumnName(c) : model.getValueAt(r, c));
                    if (cell.matches("^[=+@\\-].*")) cell = "'" + cell;
                    writer.write('"' + cell.replace("\"", "\"\"") + '"');
                }
                writer.write("\r\n");
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không ghi được tệp CSV.", "Xuất CSV", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JButton toolBtn(String text, Icon icon, int width) {
        JButton b = new JButton(text, icon);
        b.setFont(UngDungPhongChu.SMALL);
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setVerticalTextPosition(SwingConstants.BOTTOM);
        b.setIconTextGap(2);
        b.setPreferredSize(new Dimension(width, 48));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorder(new EmptyBorder(2, 4, 2, 4));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static DecimalFormat createNumFormat() {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN"));
        return new DecimalFormat("#,##0.###", sym);
    }

    private static Icon refreshIcon(int size) {
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.translate(x, y);
                g.setColor(new Color(0, 114, 198));
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int m = 2;
                int d = size - m * 2;
                g.drawArc(m, m, d, d, 30, 290);
                int ax = size - m - 2;
                int ay = m + 3;
                g.fillPolygon(new int[]{ax - 4, ax + 2, ax + 1}, new int[]{ay, ay - 3, ay + 4}, 3);
                g.dispose();
            }
        };
    }

    private static Icon printerIcon() {
        return new Icon() {
            @Override public int getIconWidth() { return 20; }
            @Override public int getIconHeight() { return 20; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.translate(x, y);
                g.setColor(new Color(90, 95, 105));
                g.drawRect(5, 2, 10, 5);
                g.setColor(new Color(70, 100, 140));
                g.fillRect(2, 7, 16, 7);
                g.setColor(Color.WHITE);
                g.fillRect(5, 11, 10, 6);
                g.setColor(new Color(90, 95, 105));
                g.drawRect(5, 11, 10, 6);
                g.dispose();
            }
        };
    }

    private static Icon detailIcon() {
        return new Icon() {
            @Override public int getIconWidth() { return 20; }
            @Override public int getIconHeight() { return 20; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.translate(x, y);
                g.setColor(new Color(45, 110, 175));
                g.fillOval(6, 2, 8, 8);
                g.fillRoundRect(3, 11, 14, 7, 6, 6);
                g.dispose();
            }
        };
    }
}
