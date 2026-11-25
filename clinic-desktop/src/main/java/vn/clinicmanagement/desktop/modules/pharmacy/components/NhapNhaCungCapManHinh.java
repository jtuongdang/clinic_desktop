package vn.clinicmanagement.desktop.modules.pharmacy.components;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.inventory.receive.NhapNhaCungCap;
import vn.clinicmanagement.application.inventory.receive.NhapNhaCungCap.*;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Màn hình nhập nhà cung cấp kết nối CSDL (lưu tạm / nhập kho và cập nhật danh mục nhà cung cấp). */
public final class NhapNhaCungCapManHinh extends JPanel {
    private static final Font CHU = new Font("Arial", Font.PLAIN, 13);
    private static final Color VIEN = new Color(160, 160, 160);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final NumberFormat MONEY_FMT = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));

    private final NhapNhaCungCap service;
    private final Integer initialWarehouseId;
    private final Runnable onSaved;
    private final PhanQuyenNguCanh auth = KhamBenhPhienNguCanh.current();

    private final Map<String, JComponent> fields = new LinkedHashMap<>();
    private final List<ReceiptLine> lines = new ArrayList<>();
    private final List<SupplierPayment> payments = new ArrayList<>();
    private final JTable drugTable;
    private final JTable paymentTable;
    private final JButton saveDraft;
    private final JButton importStock;
    private JButton printReceiptBtn;
    private PhieuNhapNhaCungCapInManHinh.ReceiptPrintData lastSavedPrintData;
    private JButton addLineBtn;
    private JButton removeLineBtn;
    private JLabel paymentCodeLabel;
    private JTextField paymentAmountField;
    private JButton addPaymentBtn;
    private JButton savePaymentBtn;
    private JButton cancelPaymentEditBtn;
    private JButton voidPaymentBtn;
    private String savedReceiptCode;
    private boolean editingPayment;
    private JLabel totalBillLabel;
    private JLabel paidBillLabel;
    private JLabel debtBillLabel;
    private JLabel statusLabel;
    private JTextField docDateField;
    private JTextField invoiceDateField;
    private JTextField expiryDayField;
    private JTextField expiryMonthField;
    private JTextField expiryYearField;
    private JPopupMenu supplierMenu;
    /** Nhận mục danh sách được chọn để nối màn hình danh mục. */
    private Consumer<String> supplierMenuListener;
    private final String headingTitle;
    private boolean saving;
    private boolean posted;
    private boolean reloadingCatalogs;
    private boolean suppressingComboEvents;

    public NhapNhaCungCapManHinh() {
        this(null, null, null);
    }

    public NhapNhaCungCapManHinh(NhapNhaCungCap service, Integer initialWarehouseId, Runnable onSaved) {
        this(service, initialWarehouseId, onSaved, "NHẬP TỪ NHÀ CUNG CẤP");
    }

    public NhapNhaCungCapManHinh(NhapNhaCungCap service, Integer initialWarehouseId, Runnable onSaved, String headingTitle) {
        super(new BorderLayout());
        this.service = service;
        this.initialWarehouseId = initialWarehouseId;
        this.onSaved = onSaved;
        this.headingTitle = headingTitle != null && !headingTitle.isBlank() ? headingTitle.toUpperCase(Locale.ROOT) : "NHẬP TỪ NHÀ CUNG CẤP";
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1240, 680));
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(buildHeading());
        top.add(buildDocumentFields());
        top.add(buildDrugFields());
        add(top, BorderLayout.NORTH);

        drugTable = table(new String[]{"STT", "Mã thuốc", "Tên thuốc", "Đơn Vị", "Số Lượng", "Giá Mua", "VAT(%)",
                        "Chiết Khấu(%)", "Giá Mua Thực", "Thành Tiền", "SĐK", "Số Lô", "Hạn Sử Dụng", "Giá Bán", "Giá Viện Phí", "Giá BHYT"},
                new int[]{43, 80, 186, 68, 69, 65, 57, 89, 87, 83, 46, 69, 91, 77, 89, 77});
        drugTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) loadSelectedRowToForm();
            }
        });
        paymentTable = table(new String[]{"Mã Phiếu", "Ngày Thu", "Số Tiền"}, new int[]{97, 95, 100});
        JScrollPane drugs = scroll(drugTable, true);
        JPanel payments = buildPayments();
        JSplitPane body = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, drugs, payments);
        body.setBorder(new EmptyBorder(0, 4, 0, 2));
        body.setDividerSize(4);
        body.setResizeWeight(1);
        body.setContinuousLayout(true);
        add(body, BorderLayout.CENTER);
        body.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent event) {
                body.setDividerLocation(Math.max(360, body.getWidth() - 280));
            }
        });

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setPreferredSize(new Dimension(100, 57));
        footer.setBorder(new MatteBorder(1, 0, 0, 0, VIEN));
        JPanel totals = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 14));
        totals.setOpaque(false);
        JButton footerMenu = button("", "menu");
        connectSupplierMenu(footerMenu);
        totals.add(footerMenu);
        totalBillLabel = amount(Color.RED);
        paidBillLabel = amount(Color.BLUE);
        debtBillLabel = amount(Color.RED);
        statusLabel = new JLabel(" ");
        statusLabel.setFont(CHU);
        statusLabel.setForeground(new Color(0, 95, 175));
        totals.add(label("Tổng hóa đơn:"));
        totals.add(totalBillLabel);
        totals.add(label("đã thanh toán:"));
        totals.add(paidBillLabel);
        totals.add(label("Còn nợ:"));
        totals.add(debtBillLabel);
        totals.add(statusLabel);
        footer.add(totals, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        actions.setOpaque(false);
        printReceiptBtn = button("In Phiếu", "save");
        printReceiptBtn.setEnabled(false);
        printReceiptBtn.addActionListener(e -> openPrintPreview());
        saveDraft = button("Lưu Tạm", "save");
        importStock = button("Nhập Kho", "check");
        saveDraft.addActionListener(e -> submitReceipt(false));
        importStock.addActionListener(e -> submitReceipt(true));
        actions.add(printReceiptBtn);
        actions.add(saveDraft);
        actions.add(importStock);
        footer.add(actions, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        wireLiveInteractions();
        loadCatalogs();
    }

    private JPanel buildHeading() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setPreferredSize(new Dimension(1240, 44));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        p.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, VIEN), new EmptyBorder(4, 14, 4, 12)));
        JButton menu = button("", "menu");
        supplierMenu = buildSupplierMenu();
        connectSupplierMenu(menu);
        p.add(menu, BorderLayout.WEST);
        JLabel title = label(headingTitle);
        title.setFont(CHU.deriveFont(Font.BOLD, 20));
        title.setForeground(new Color(157, 0, 0));
        p.add(title, BorderLayout.EAST);
        return p;
    }

    /** Ba mục xổ xuống theo ảnh, có nền vàng khi rê chuột hoặc chọn bằng bàn phím. */
    private JPopupMenu buildSupplierMenu() {
        JPopupMenu popup = new JPopupMenu();
        popup.setLayout(new BoxLayout(popup, BoxLayout.Y_AXIS));
        popup.setBackground(Color.WHITE);
        popup.setBorder(new LineBorder(new Color(150, 150, 150)));
        popup.getAccessibleContext().setAccessibleName("Menu nhập thuốc từ nhà cung cấp");
        for (String title : new String[]{"Danh Sách Thuốc", "Danh Sách Nhà Cung Cấp", "Đóng"}) {
            if ("Đóng".equals(title)) {
                JSeparator separator = new JSeparator();
                separator.setAlignmentX(Component.LEFT_ALIGNMENT);
                separator.setPreferredSize(new Dimension(268, 2));
                separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
                popup.add(separator);
            }
            JMenuItem item = new JMenuItem(title) {
                @Override protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    boolean selected = getModel().isArmed();
                    g.setPaint(new GradientPaint(0, 0, selected ? new Color(255, 242, 196) : Color.WHITE,
                            0, getHeight(), selected ? new Color(255, 228, 151) : Color.WHITE));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    if (selected) {
                        g.setColor(new Color(235, 184, 63));
                        g.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
                    }
                    g.dispose();
                    super.paintComponent(graphics);
                }
            };
            item.setUI(new javax.swing.plaf.basic.BasicMenuItemUI() {
                @Override protected void installDefaults() {
                    super.installDefaults();
                    selectionForeground = Color.BLACK;
                }

                @Override protected void paintBackground(Graphics g, JMenuItem menuItem, Color background) {
                    // Nền được vẽ phía trên để giữ đúng màu thiết kế trên macOS.
                }
            });
            item.setOpaque(false);
            item.setFont(CHU.deriveFont(16f));
            item.setForeground(Color.BLACK);
            item.setBorder(new EmptyBorder(1, 5, 1, 5));
            item.setIcon(new Icon() {
                public int getIconWidth() { return 28; }
                public int getIconHeight() { return 23; }
                public void paintIcon(Component c, Graphics graphics, int x, int y) {
                    if (!"Đóng".equals(title)) return;
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(new Color(212, 20, 20));
                    g.setStroke(new BasicStroke(3));
                    g.drawLine(x + 4, y + 4, x + 19, y + 19);
                    g.drawLine(x + 19, y + 4, x + 4, y + 19);
                    g.dispose();
                }
            });
            item.setIconTextGap(8);
            item.setPreferredSize(new Dimension(268, 29));
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));
            item.setAlignmentX(Component.LEFT_ALIGNMENT);
            item.addActionListener(e -> {
                if ("Đóng".equals(title)) {
                    Window window = SwingUtilities.getWindowAncestor(this);
                    if (window instanceof JDialog) window.dispose();
                } else if (supplierMenuListener != null) {
                    supplierMenuListener.accept(title);
                } else if ("Danh Sách Nhà Cung Cấp".equals(title)) {
                    openSupplierDirectoryDialog();
                }
            });
            popup.add(item);
        }
        return popup;
    }

    /** Cả hai nút ba gạch mở cùng menu; nút sáng xanh khi menu đang hiển thị. */
    private void connectSupplierMenu(JButton button) {
        button.getAccessibleContext().setAccessibleName("Menu nhập nhà cung cấp");
        button.addActionListener(e -> {
            if (supplierMenu.isVisible()) supplierMenu.setVisible(false);
            else if (button.isShowing()) supplierMenu.show(button, 0, button.getHeight());
        });
        button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DOWN"), "openSupplierMenu");
        button.getActionMap().put("openSupplierMenu", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { button.doClick(); }
        });
        supplierMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) { highlight(); }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { button.setContentAreaFilled(false); }
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) { button.setContentAreaFilled(false); }
            private void highlight() {
                if (supplierMenu.getInvoker() == button) {
                    button.setBackground(new Color(216, 235, 249));
                    button.setContentAreaFilled(true);
                }
            }
        });
    }

    /** Đăng ký màn hình sẽ mở khi chọn danh sách thuốc hoặc nhà cung cấp. */
    public void setOnSupplierMenuListener(Consumer<String> listener) {
        supplierMenuListener = listener;
    }

    public JPopupMenu getSupplierMenu() { return supplierMenu; }

    private JPanel buildDocumentFields() {
        MauPanel p = new MauPanel(88);
        p.setBorder(new MatteBorder(0, 0, 1, 0, VIEN));
        field(p, "Kho thuốc:", combo("Nhà thuốc"), 8, 6, 84, 230, 23);
        docDateField = text(today());
        field(p, "Ngày lập:", dateBox(docDateField), 332, 6, 82, 102, 23);
        invoiceDateField = text(today());
        field(p, "Ngày HĐ:", dateBox(invoiceDateField), 524, 6, 70, 97, 23);
        field(p, "Số HĐ:", text(""), 700, 6, 78, 91, 23);
        field(p, "Mã phiếu:", text(""), 876, 6, 72, 91, 23);
        field(p, "Tên NCC:", combo(""), 1045, 6, 80, 249, 23);
        field(p, "Địa chỉ:", text(""), 8, 32, 84, 230, 23);
        field(p, "Mã số thuế:", text(""), 332, 32, 82, 102, 23);
        field(p, "Nội dung:", text(""), 524, 32, 70, 97, 23);
        field(p, "Người lập:", text(NguoiDungPhien.getInstance().getFullName()), 700, 32, 78, 261, 23);
        field(p, "Người giao:", text(""), 1045, 32, 80, 249, 23);
        field(p, "Ngành khác:", combo(""), 8, 58, 84, 230, 23);
        return p;
    }

    private JPanel buildDrugFields() {
        MauPanel p = new MauPanel(191);
        field(p, "Tên thuốc:", combo(""), 8, 10, 84, 414, 23);
        field(p, "Mã thuốc:", text(""), 514, 10, 86, 94, 23);
        field(p, "Nồng độ:", text(""), 701, 10, 77, 148, 23);
        field(p, "Hàm lượng:", text(""), 933, 10, 88, 83, 23);
        field(p, "Nước SX:", text(""), 1117, 10, 82, 228, 23);
        field(p, "Hãng SX:", text(""), 8, 36, 84, 250, 23);
        field(p, "Phòng lưu:", combo(""), 357, 36, 89, 177, 23);
        field(p, "ĐV nhập:", combo(""), 8, 61, 84, 90, 23);
        field(p, "SL nhập:", number(), 194, 61, 64, 85, 23);
        field(p, "Số lượng:", number(), 357, 61, 89, 85, 23);
        field(p, "Đơn vị tính:", text(""), 544, 61, 79, 85, 23);
        field(p, "Bảo quản:", text(""), 713, 61, 80, 166, 23);
        field(p, "Giá mua:", number(), 8, 86, 84, 90, 23);
        field(p, "%VAT:", number(), 194, 86, 64, 85, 23);
        JTextField discount = number();
        discount.setLayout(new BorderLayout());
        JLabel calculator = label("▦");
        calculator.setForeground(new Color(0, 140, 220));
        calculator.setBorder(new MatteBorder(0, 1, 0, 0, VIEN));
        discount.add(calculator, BorderLayout.EAST);
        field(p, "Chiết khấu:", discount, 357, 86, 89, 85, 23);
        field(p, "Giá thực:", number(), 544, 86, 79, 85, 23);
        field(p, "Thành tiền:", number(), 713, 86, 80, 86, 23);
        field(p, "QĐ thầu:", text(""), 8, 110, 84, 90, 23);
        field(p, "Giá thầu:", text(""), 194, 110, 64, 85, 23);
        field(p, "Nhóm thuốc:", text(""), 357, 110, 89, 85, 23);
        field(p, "Năm thầu:", text(""), 544, 110, 79, 85, 23);
        field(p, "Nhà NK:", text(""), 713, 110, 80, 166, 23);
        field(p, "Số ĐK:", text(""), 8, 134, 84, 90, 23);
        field(p, "Số lô:", text(""), 194, 134, 64, 85, 23);
        JPanel expiry = new JPanel(new GridLayout(1, 3, 1, 0));
        expiry.setOpaque(false);
        expiryDayField = text("");
        expiryMonthField = text("");
        expiryYearField = text("");
        expiry.add(expiryDayField);
        expiry.add(expiryMonthField);
        expiry.add(expiryYearField);
        field(p, "Hạn SD:", expiry, 357, 134, 89, 85, 23);
        field(p, "Giá bán:", number(), 544, 134, 79, 85, 23);
        field(p, "Hết hạn thầu:", text(""), 713, 134, 80, 166, 23);
        field(p, "Giá BHYT:", number(), 8, 159, 84, 90, 23);
        field(p, "Giá VP:", number(), 194, 159, 64, 85, 23);
        field(p, "Giá DV:", number(), 357, 159, 89, 85, 23);
        field(p, "Giá NNN:", number(), 544, 159, 79, 85, 23);
        JTextField inactive = text("");
        inactive.setEnabled(false);
        inactive.setBackground(new Color(209, 209, 209));
        p.place(inactive, 713, 159, 166, 22);

        addLineBtn = new JButton("Thêm Dòng (Ctrl+A)");
        addLineBtn.setFont(CHU);
        addLineBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addLineBtn.addActionListener(e -> addOrUpdateLine());
        p.place(addLineBtn, 900, 158, 160, 25);

        removeLineBtn = new JButton("Xóa Dòng");
        removeLineBtn.setFont(CHU);
        removeLineBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeLineBtn.addActionListener(e -> removeSelectedLine());
        p.place(removeLineBtn, 1068, 158, 105, 25);

        JButton addNccBtn = new JButton("+ Nhà Cung Cấp");
        addNccBtn.setFont(CHU);
        addNccBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addNccBtn.addActionListener(e -> openSupplierDirectoryDialog());
        p.place(addNccBtn, 1181, 158, 145, 25);
        return p;
    }

    private JPanel buildPayments() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setMinimumSize(new Dimension(245, 200));
        MauPanel input = new MauPanel(213, 296);
        JLabel title = label("Thanh Toán Hóa Đơn");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setBorder(new MatteBorder(1, 0, 1, 0, VIEN));
        input.place(title, 0, 0, 296, 25);
        input.place(label("Số phiếu:"), 2, 28, 76, 21);
        paymentCodeLabel = new JLabel("(Chưa lưu phiếu)");
        paymentCodeLabel.setFont(CHU.deriveFont(Font.BOLD));
        paymentCodeLabel.setForeground(new Color(0, 95, 175));
        input.place(paymentCodeLabel, 79, 28, 213, 21);
        JComboBox<Object> payType = combo("Tiền mặt");
        payType.addItem("Chuyển khoản");
        payType.addItem("Công nợ");
        field(input, "Loại phiếu:", payType, 2, 49, 77, 213, 23);
        paymentAmountField = number();
        paymentAmountField.setEditable(false);
        field(input, "Số tiền:", paymentAmountField, 2, 73, 77, 213, 23);
        JTextArea note = new JTextArea();
        note.setFont(CHU);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        input.place(label("Ghi chú:"), 2, 112, 76, 42);
        input.place(new JScrollPane(note), 79, 98, 213, 85);
        fields.put("Ghi chú", note);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        tools.setOpaque(false);
        addPaymentBtn = paymentButton("Thêm", "plus");
        savePaymentBtn = paymentButton("Lưu", "save");
        cancelPaymentEditBtn = paymentButton("Hủy", "cancel");
        voidPaymentBtn = paymentButton("Hủy Phiếu", "cancel");
        savePaymentBtn.setEnabled(false);
        cancelPaymentEditBtn.setEnabled(false);
        voidPaymentBtn.setEnabled(false);
        addPaymentBtn.addActionListener(e -> beginAddPayment());
        savePaymentBtn.addActionListener(e -> savePayment());
        cancelPaymentEditBtn.addActionListener(e -> cancelPaymentEdit());
        voidPaymentBtn.addActionListener(e -> voidSelectedPayment());
        tools.add(addPaymentBtn);
        tools.add(savePaymentBtn);
        tools.add(cancelPaymentEditBtn);
        tools.add(voidPaymentBtn);
        input.place(tools, 4, 191, 290, 22);
        p.add(input, BorderLayout.NORTH);
        paymentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updatePaymentButtons();
        });
        p.add(scroll(paymentTable, false), BorderLayout.CENTER);
        return p;
    }

    private void wireLiveInteractions() {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ctrl A"), "addLine");
        getActionMap().put("addLine", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { addOrUpdateLine(); }
        });

        comboBox("Tên NCC:").addActionListener(e -> {
            if (suppressingComboEvents) return;
            Object sel = comboBox("Tên NCC:").getSelectedItem();
            if (sel instanceof Supplier s) {
                setText("Địa chỉ:", s.address());
                setText("Mã số thuế:", s.taxCode());
            }
        });
        comboBox("Tên thuốc:").addActionListener(e -> {
            if (suppressingComboEvents) return;
            Object sel = comboBox("Tên thuốc:").getSelectedItem();
            if (sel instanceof DrugOption d) {
                setText("Mã thuốc:", d.code());
                setText("Nồng độ:", d.concentration());
                setText("Hàm lượng:", d.strength());
                setText("Nước SX:", d.country());
                setText("Hãng SX:", d.maker());
                setText("Đơn vị tính:", d.unit());
                setText("Nhóm thuốc:", d.group());
                setText("Số ĐK:", d.registration());
                setText("Giá mua:", formatDecimal(d.defaultCost()));
                setText("Giá bán:", formatDecimal(d.defaultPrice()));
                setText("SL nhập:", "1");
                setText("Số lượng:", "1");
                recalculateLineTotals();
            }
        });
        textField("SL nhập:").getDocument().addDocumentListener(docListener(() -> {
            String v = textField("SL nhập:").getText();
            if (!Objects.equals(textField("Số lượng:").getText(), v)) {
                textField("Số lượng:").setText(v);
            }
            recalculateLineTotals();
        }));
        for (String key : List.of("Giá mua:", "%VAT:", "Chiết khấu:", "Số lượng:")) {
            textField(key).getDocument().addDocumentListener(docListener(this::recalculateLineTotals));
        }
        textField("Giá thực:").setEditable(false);
        textField("Thành tiền:").setEditable(false);
        textField("Người lập:").setEditable(false);
    }

    private void loadCatalogs() {
        boolean hasService = service != null && NhapNhaCungCap.permitted(auth);
        saveDraft.setEnabled(hasService);
        importStock.setEnabled(hasService);
        if (!hasService) {
            statusLabel.setText(service == null ? "Chưa kết nối CSDL nhập kho." : "Cần tài khoản có quyền kho.");
            return;
        }
        reloadingCatalogs = true;
        statusLabel.setText("Đang tải danh mục kho / NCC / thuốc...");
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                var wh = service.warehouses(auth);
                var sup = service.suppliers(auth);
                var dr = service.drugs(auth);
                return new Object[]{wh, sup, dr};
            }

            @Override protected void done() {
                reloadingCatalogs = false;
                if (!sameSession()) return;
                try {
                    Object[] res = get();
                    @SuppressWarnings("unchecked") List<Warehouse> wh = (List<Warehouse>) res[0];
                    @SuppressWarnings("unchecked") List<Supplier> sup = (List<Supplier>) res[1];
                    @SuppressWarnings("unchecked") List<DrugOption> dr = (List<DrugOption>) res[2];
                    comboBox("Kho thuốc:").setModel(new DefaultComboBoxModel<>(wh.toArray()));
                    if (initialWarehouseId != null) {
                        wh.stream().filter(w -> w.id() == initialWarehouseId).findFirst()
                                .ifPresent(w -> comboBox("Kho thuốc:").setSelectedItem(w));
                    }
                    reloadSupplierCombo(sup, null);
                    comboBox("Tên thuốc:").setModel(new DefaultComboBoxModel<>(dr.toArray()));
                    if (comboBox("Tên thuốc:").getItemCount() > 0) {
                        comboBox("Tên thuốc:").setSelectedIndex(0);
                    }
                    statusLabel.setText(sup.isEmpty()
                            ? "Chưa có Nhà Cung Cấp trong CSDL — bấm '+ Nhà Cung Cấp' để tạo."
                            : "Sẵn sàng nhập phiếu.");
                } catch (Exception ex) {
                    statusLabel.setText("Không tải được danh mục: " + ex.getMessage());
                }
            }
        }.execute();
    }

    /** Tải lại danh mục thuốc và nhà cung cấp mới nhất từ CSDL mà không làm mất dữ liệu đang nhập. */
    public void refreshCatalogsPreservingSelection() {
        if (service == null || !NhapNhaCungCap.permitted(auth) || reloadingCatalogs || saving || posted) {
            return;
        }
        if (auth == null || !auth.equals(KhamBenhPhienNguCanh.current())) {
            return;
        }
        reloadingCatalogs = true;
        Integer selectedDrugId = comboBox("Tên thuốc:").getSelectedItem() instanceof DrugOption d ? d.id() : null;
        Integer selectedSupplierId = comboBox("Tên NCC:").getSelectedItem() instanceof Supplier s ? s.id() : null;
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                return new Object[]{service.suppliers(auth), service.drugs(auth)};
            }

            @Override protected void done() {
                reloadingCatalogs = false;
                if (auth == null || !auth.equals(KhamBenhPhienNguCanh.current())) return;
                try {
                    Object[] res = get();
                    @SuppressWarnings("unchecked") List<Supplier> sup = (List<Supplier>) res[0];
                    @SuppressWarnings("unchecked") List<DrugOption> dr = (List<DrugOption>) res[1];
                    suppressingComboEvents = true;
                    try {
                        JComboBox<Object> supBox = comboBox("Tên NCC:");
                        supBox.setModel(new DefaultComboBoxModel<>(sup.toArray()));
                        if (selectedSupplierId != null) {
                            sup.stream().filter(s -> s.id() == selectedSupplierId).findFirst()
                                    .ifPresent(supBox::setSelectedItem);
                        } else if (!sup.isEmpty()) {
                            supBox.setSelectedIndex(0);
                        }

                        JComboBox<Object> drugBox = comboBox("Tên thuốc:");
                        drugBox.setModel(new DefaultComboBoxModel<>(dr.toArray()));
                        boolean restoredDrug = false;
                        if (selectedDrugId != null) {
                            for (DrugOption item : dr) {
                                if (item.id() == selectedDrugId) {
                                    drugBox.setSelectedItem(item);
                                    restoredDrug = true;
                                    break;
                                }
                            }
                        }
                        if (!restoredDrug && !dr.isEmpty()) {
                            suppressingComboEvents = false;
                            drugBox.setSelectedIndex(0);
                        }
                    } finally {
                        suppressingComboEvents = false;
                    }
                } catch (Exception ignored) {
                    // Giữ nguyên danh mục hiện có nếu tải nền gặp lỗi tạm thời.
                }
            }
        }.execute();
    }

    private void reloadSupplierCombo(List<Supplier> suppliers, Integer selectId) {
        JComboBox<Object> box = comboBox("Tên NCC:");
        box.setModel(new DefaultComboBoxModel<>(suppliers.toArray()));
        if (selectId != null) {
            suppliers.stream().filter(s -> s.id() == selectId).findFirst().ifPresent(box::setSelectedItem);
        } else if (!suppliers.isEmpty()) {
            box.setSelectedIndex(0);
        }
    }

    private void recalculateLineTotals() {
        try {
            BigDecimal qty = parseDecimal(textField("Số lượng:").getText());
            BigDecimal cost = parseDecimal(textField("Giá mua:").getText());
            BigDecimal vat = parseDecimal(textField("%VAT:").getText());
            BigDecimal disc = parseDecimal(textField("Chiết khấu:").getText());
            ReceiptLine preview = new ReceiptLine(1, "", "", "", "L", LocalDate.now().plusDays(1),
                    qty, cost, vat, disc, BigDecimal.ZERO);
            setText("Giá thực:", formatDecimal(preview.realUnitCost()));
            setText("Thành tiền:", formatDecimal(preview.amount()));
        } catch (NumberFormatException ignored) {
            setText("Giá thực:", "0");
            setText("Thành tiền:", "0");
        }
    }

    private void addOrUpdateLine() {
        if (saving || posted) return;
        Object sel = comboBox("Tên thuốc:").getSelectedItem();
        if (!(sel instanceof DrugOption drug)) {
            showInfo("Vui lòng chọn thuốc từ danh mục.");
            return;
        }
        String batch = textField("Số lô:").getText().trim();
        if (batch.isEmpty()) {
            showInfo("Vui lòng nhập Số lô.");
            textField("Số lô:").requestFocusInWindow();
            return;
        }
        LocalDate expiry;
        try {
            expiry = parseExpiry();
        } catch (IllegalArgumentException ex) {
            showInfo(ex.getMessage());
            return;
        }
        BigDecimal qty;
        BigDecimal cost;
        BigDecimal vat;
        BigDecimal disc;
        BigDecimal sell;
        try {
            qty = parseDecimal(textField("Số lượng:").getText());
            cost = parseDecimal(textField("Giá mua:").getText());
            vat = parseDecimal(textField("%VAT:").getText());
            disc = parseDecimal(textField("Chiết khấu:").getText());
            sell = parseDecimal(textField("Giá bán:").getText());
        } catch (NumberFormatException ex) {
            showInfo("Số lượng, giá hoặc VAT/chiết khấu không hợp lệ.");
            return;
        }
        if (qty.signum() <= 0) {
            showInfo("Số lượng phải lớn hơn 0.");
            return;
        }
        ReceiptLine line = new ReceiptLine(drug.id(), drug.code(), drug.name(), drug.unit(),
                batch, expiry, qty, cost, vat, disc, sell);
        int idx = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).itemId() == drug.id() && lines.get(i).batch().equalsIgnoreCase(batch)) {
                idx = i;
                break;
            }
        }
        if (idx >= 0) lines.set(idx, line);
        else lines.add(line);
        redrawDrugTable();
        clearDrugEntryInputs();
    }

    private void loadSelectedRowToForm() {
        int row = drugTable.getSelectedRow();
        if (row < 0 || row >= lines.size() || saving || posted) return;
        ReceiptLine l = lines.get(row);
        JComboBox<Object> box = comboBox("Tên thuốc:");
        for (int i = 0; i < box.getItemCount(); i++) {
            if (box.getItemAt(i) instanceof DrugOption d && d.id() == l.itemId()) {
                box.setSelectedIndex(i);
                break;
            }
        }
        setText("Số lô:", l.batch());
        expiryDayField.setText(String.format("%02d", l.expiry().getDayOfMonth()));
        expiryMonthField.setText(String.format("%02d", l.expiry().getMonthValue()));
        expiryYearField.setText(String.valueOf(l.expiry().getYear()));
        setText("SL nhập:", formatDecimal(l.quantity()));
        setText("Số lượng:", formatDecimal(l.quantity()));
        setText("Giá mua:", formatDecimal(l.unitCost()));
        setText("%VAT:", formatDecimal(l.vatPercent()));
        setText("Chiết khấu:", formatDecimal(l.discountPercent()));
        setText("Giá bán:", formatDecimal(l.sellPrice()));
        recalculateLineTotals();
    }

    private void removeSelectedLine() {
        int row = drugTable.getSelectedRow();
        if (row < 0 || row >= lines.size() || saving || posted) return;
        lines.remove(row);
        redrawDrugTable();
    }

    private void clearDrugEntryInputs() {
        setText("Số lô:", "");
        expiryDayField.setText("");
        expiryMonthField.setText("");
        expiryYearField.setText("");
        setText("SL nhập:", "0");
        setText("Số lượng:", "0");
        setText("%VAT:", "0");
        setText("Chiết khấu:", "0");
        recalculateLineTotals();
    }

    private void redrawDrugTable() {
        DefaultTableModel model = (DefaultTableModel) drugTable.getModel();
        model.setRowCount(0);
        for (int i = 0; i < lines.size(); i++) {
            ReceiptLine l = lines.get(i);
            BigDecimal amt = l.amount();
            model.addRow(new Object[]{
                    i + 1, l.itemCode(), l.itemName(), l.unit(),
                    formatDecimal(l.quantity()), formatDecimal(l.unitCost()),
                    formatDecimal(l.vatPercent()), formatDecimal(l.discountPercent()),
                    formatDecimal(l.realUnitCost()), MONEY_FMT.format(amt),
                    textField("Số ĐK:").getText().trim(), l.batch(),
                    DATE_FMT.format(l.expiry()), MONEY_FMT.format(l.sellPrice()),
                    MONEY_FMT.format(l.sellPrice()), "0"
            });
        }
        updateBillSummary();
    }

    private BigDecimal computeBillTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (ReceiptLine l : lines) total = total.add(l.amount());
        return total;
    }

    private BigDecimal computePaidTotal() {
        BigDecimal paid = BigDecimal.ZERO;
        for (SupplierPayment p : payments) {
            if (p.isActive() && p.amount() != null) paid = paid.add(p.amount());
        }
        return paid;
    }

    private void updateBillSummary() {
        BigDecimal total = computeBillTotal();
        BigDecimal paid = computePaidTotal();
        BigDecimal debt = total.subtract(paid).max(BigDecimal.ZERO);
        totalBillLabel.setText(MONEY_FMT.format(total) + "đ");
        paidBillLabel.setText(MONEY_FMT.format(paid) + "đ");
        debtBillLabel.setText(MONEY_FMT.format(debt) + "đ");
    }

    private void beginAddPayment() {
        if (service == null || !NhapNhaCungCap.permitted(auth)) return;
        if (savedReceiptCode == null || savedReceiptCode.isBlank()) {
            showInfo("Vui lòng bấm Lưu Tạm hoặc Nhập Kho trước khi tạo phiếu thanh toán.");
            return;
        }
        BigDecimal remaining = computeBillTotal().subtract(computePaidTotal()).max(BigDecimal.ZERO);
        if (remaining.signum() <= 0) {
            showInfo("Hóa đơn này đã thanh toán đủ.");
            return;
        }
        editingPayment = true;
        paymentAmountField.setEditable(true);
        paymentAmountField.setText(formatDecimal(remaining));
        paymentAmountField.requestFocusInWindow();
        paymentAmountField.selectAll();
        updatePaymentButtons();
    }

    private void cancelPaymentEdit() {
        editingPayment = false;
        paymentAmountField.setEditable(false);
        paymentAmountField.setText("0");
        if (fields.get("Ghi chú") instanceof JTextArea area) area.setText("");
        updatePaymentButtons();
    }

    private void savePayment() {
        if (!editingPayment || service == null || savedReceiptCode == null) return;
        if (!sameSession()) return;
        BigDecimal amount;
        try {
            amount = parseDecimal(paymentAmountField.getText());
        } catch (NumberFormatException ex) {
            showInfo("Số tiền thanh toán không hợp lệ.");
            return;
        }
        if (amount.signum() <= 0) {
            showInfo("Số tiền thanh toán phải lớn hơn 0.");
            return;
        }
        String type = Objects.toString(comboBox("Loại phiếu:").getSelectedItem(), "Tiền mặt");
        String note = fields.get("Ghi chú") instanceof JTextArea area ? area.getText().trim() : "";
        savePaymentBtn.setEnabled(false);
        statusLabel.setText("Đang lưu phiếu thanh toán...");
        new SwingWorker<KetQua<SupplierPayment>, Void>() {
            @Override protected KetQua<SupplierPayment> doInBackground() {
                return service.addPayment(savedReceiptCode, type, amount, note, auth);
            }

            @Override protected void done() {
                if (!sameSession()) return;
                try {
                    KetQua<SupplierPayment> res = get();
                    if (res.isFailure()) {
                        savePaymentBtn.setEnabled(true);
                        String msg = res.getError().orElseThrow().message();
                        statusLabel.setText(msg);
                        showInfo(msg);
                    } else {
                        SupplierPayment created = res.valueOrThrow();
                        cancelPaymentEdit();
                        statusLabel.setText("Đã thanh toán: " + created.paymentCode());
                        reloadPayments();
                    }
                } catch (Exception ex) {
                    savePaymentBtn.setEnabled(true);
                    statusLabel.setText("Lỗi lưu thanh toán.");
                    showInfo("Không thể lưu thanh toán: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void voidSelectedPayment() {
        int row = paymentTable.getSelectedRow();
        if (row < 0 || row >= payments.size() || service == null) return;
        if (!sameSession()) return;
        SupplierPayment selected = payments.get(row);
        if (!selected.isActive()) {
            showInfo("Phiếu thanh toán này đã bị hủy trước đó.");
            return;
        }
        if (!GraphicsEnvironment.isHeadless()) {
            int ans = JOptionPane.showConfirmDialog(this,
                    "Hủy phiếu thanh toán " + selected.paymentCode() + " (" + MONEY_FMT.format(selected.amount()) + "đ)?",
                    "Xác nhận hủy phiếu", JOptionPane.YES_NO_OPTION);
            if (ans != JOptionPane.YES_OPTION) return;
        }
        voidPaymentBtn.setEnabled(false);
        statusLabel.setText("Đang hủy phiếu thanh toán...");
        new SwingWorker<KetQua<Void>, Void>() {
            @Override protected KetQua<Void> doInBackground() {
                return service.cancelPayment(selected.id(), auth);
            }

            @Override protected void done() {
                if (!sameSession()) return;
                try {
                    KetQua<Void> res = get();
                    if (res.isFailure()) {
                        String msg = res.getError().orElseThrow().message();
                        statusLabel.setText(msg);
                        showInfo(msg);
                        updatePaymentButtons();
                    } else {
                        statusLabel.setText("Đã hủy phiếu thanh toán: " + selected.paymentCode());
                        reloadPayments();
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Lỗi hủy phiếu thanh toán.");
                    showInfo("Không thể hủy phiếu thanh toán: " + ex.getMessage());
                    updatePaymentButtons();
                }
            }
        }.execute();
    }

    private void reloadPayments() {
        if (service == null || savedReceiptCode == null || savedReceiptCode.isBlank()) return;
        async(() -> service.payments(savedReceiptCode, auth), list -> {
            payments.clear();
            payments.addAll(list);
            redrawPaymentTable();
            updateBillSummary();
            updatePaymentButtons();
        });
    }

    private void redrawPaymentTable() {
        DefaultTableModel model = (DefaultTableModel) paymentTable.getModel();
        model.setRowCount(0);
        DateTimeFormatter payDateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        for (SupplierPayment p : payments) {
            String code = p.isActive() ? p.paymentCode() : p.paymentCode() + " (Hủy)";
            String date = p.paidAt() != null ? payDateFmt.format(p.paidAt()) : "";
            String amt = MONEY_FMT.format(p.amount()) + "đ";
            model.addRow(new Object[]{code, date, amt});
        }
    }

    private void updatePaymentButtons() {
        boolean canUse = service != null && NhapNhaCungCap.permitted(auth) && savedReceiptCode != null;
        if (addPaymentBtn != null) addPaymentBtn.setEnabled(!editingPayment && service != null);
        if (savePaymentBtn != null) savePaymentBtn.setEnabled(editingPayment && canUse);
        if (cancelPaymentEditBtn != null) cancelPaymentEditBtn.setEnabled(editingPayment);
        if (voidPaymentBtn != null) {
            int row = paymentTable.getSelectedRow();
            boolean activeSelected = row >= 0 && row < payments.size() && payments.get(row).isActive();
            voidPaymentBtn.setEnabled(!editingPayment && canUse && activeSelected);
        }
    }

    private void submitReceipt(boolean postStock) {
        if (saving || posted || service == null) return;
        if (!sameSession()) return;
        if (!(comboBox("Kho thuốc:").getSelectedItem() instanceof Warehouse wh)) {
            showInfo("Vui lòng chọn Kho thuốc.");
            return;
        }
        if (!(comboBox("Tên NCC:").getSelectedItem() instanceof Supplier sup)) {
            showInfo("Vui lòng chọn hoặc tạo Nhà cung cấp trước.");
            openSupplierDirectoryDialog();
            return;
        }
        if (lines.isEmpty()) {
            showInfo("Vui lòng thêm ít nhất 1 dòng thuốc vào bảng trước khi lưu.");
            return;
        }
        LocalDate docDate;
        LocalDate invDate;
        try {
            docDate = LocalDate.parse(docDateField.getText().trim(), DATE_FMT);
            invDate = LocalDate.parse(invoiceDateField.getText().trim(), DATE_FMT);
        } catch (DateTimeException ex) {
            showInfo("Ngày lập / Ngày HĐ phải có dạng dd/MM/yyyy.");
            return;
        }

        String note = textField("Nội dung:").getText().trim();
        if (fields.get("Ghi chú") instanceof JTextArea area && !area.getText().trim().isEmpty()) {
            note = note.isEmpty() ? area.getText().trim() : note + " - " + area.getText().trim();
        }
        DraftReceipt draft = new DraftReceipt(
                textField("Mã phiếu:").getText().trim(),
                wh.id(),
                sup.id(),
                docDate,
                invDate,
                textField("Số HĐ:").getText().trim(),
                textField("Người giao:").getText().trim(),
                note,
                lines,
                postStock);
        try {
            draft.validate();
        } catch (IllegalArgumentException ex) {
            showInfo(ex.getMessage());
            return;
        }

        saving = true;
        setActionsEnabled(false);
        statusLabel.setText(postStock ? "Đang nhập kho..." : "Đang lưu tạm...");
        new SwingWorker<KetQua<String>, Void>() {
            @Override protected KetQua<String> doInBackground() {
                return service.save(draft, auth);
            }

            @Override protected void done() {
                saving = false;
                try {
                    KetQua<String> result = get();
                    if (result.isFailure()) {
                        statusLabel.setText(result.getError().orElseThrow().message());
                        setActionsEnabled(true);
                        showInfo(result.getError().orElseThrow().message());
                    } else {
                        String code = result.valueOrThrow();
                        savedReceiptCode = code;
                        setText("Mã phiếu:", code);
                        if (paymentCodeLabel != null) paymentCodeLabel.setText(code);
                        posted = postStock;
                        setActionsEnabled(!posted);
                        lastSavedPrintData = buildPrintData(code, wh, sup, docDate, invDate, draft);
                        printReceiptBtn.setEnabled(true);
                        statusLabel.setText((postStock ? "Đã nhập kho: " : "Đã lưu tạm: ") + code);
                        reloadPayments();
                        updatePaymentButtons();
                        if (onSaved != null) onSaved.run();
                        if (!GraphicsEnvironment.isHeadless()) {
                            if (postStock) {
                                PhieuNhapNhaCungCapInManHinh.showPreviewDialog(NhapNhaCungCapManHinh.this, lastSavedPrintData);
                            } else {
                                JOptionPane.showMessageDialog(NhapNhaCungCapManHinh.this,
                                        "Đã lưu tạm phiếu nhập!\nMã phiếu: " + code,
                                        "Nhập Từ Nhà Cung Cấp", JOptionPane.INFORMATION_MESSAGE);
                            }
                        }
                    }
                } catch (Exception ex) {
                    setActionsEnabled(true);
                    statusLabel.setText("Lỗi khi lưu phiếu nhập kho.");
                    showInfo("Không thể lưu phiếu nhập kho: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void openPrintPreview() {
        if (lastSavedPrintData == null) {
            showInfo("Vui lòng Lưu Tạm hoặc Nhập Kho trước khi in phiếu.");
            return;
        }
        PhieuNhapNhaCungCapInManHinh.showPreviewDialog(this, lastSavedPrintData);
    }

    private PhieuNhapNhaCungCapInManHinh.ReceiptPrintData buildPrintData(
            String code, Warehouse wh, Supplier sup, LocalDate docDate, LocalDate invDate, DraftReceipt draft) {
        Map<Integer, String> originByItemId = new HashMap<>();
        JComboBox<Object> drugBox = comboBox("Tên thuốc:");
        for (int i = 0; i < drugBox.getItemCount(); i++) {
            if (drugBox.getItemAt(i) instanceof DrugOption d) {
                originByItemId.put(d.id(), d.country() != null ? d.country() : "");
            }
        }
        List<PhieuNhapNhaCungCapInManHinh.LineData> printLines = new ArrayList<>();
        for (ReceiptLine l : draft.lines()) {
            printLines.add(new PhieuNhapNhaCungCapInManHinh.LineData(
                    l.itemName(),
                    l.unit(),
                    originByItemId.getOrDefault(l.itemId(), ""),
                    l.batch(),
                    l.expiry(),
                    l.quantity(),
                    l.realUnitCost(),
                    l.amount()));
        }
        return new PhieuNhapNhaCungCapInManHinh.ReceiptPrintData(
                wh != null ? wh.name() : "NHÀ THUỐC",
                code,
                docDate,
                sup != null ? sup.name() : "",
                draft.invoiceNumber(),
                invDate,
                textField("Người lập:").getText().trim(),
                printLines,
                draft.total());
    }

    /** Mở hộp thoại xem và thêm nhanh Nhà Cung Cấp vào CSDL. */
    public void openSupplierDirectoryDialog() {
        if (GraphicsEnvironment.isHeadless() || service == null) return;
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Danh Sách Nhà Cung Cấp", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Tên Nhà Cung Cấp", "MST", "Điện Thoại", "Email", "Địa Chỉ"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tbl = new JTable(model);
        tbl.setRowHeight(24);
        root.add(new JScrollPane(tbl), BorderLayout.CENTER);

        JTextField txtName = new JTextField();
        JTextField txtTax = new JTextField();
        JTextField txtPhone = new JTextField();
        JTextField txtEmail = new JTextField();
        JTextField txtAddress = new JTextField();
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Tên nhà cung cấp (*):")); form.add(txtName);
        form.add(new JLabel("Mã số thuế:")); form.add(txtTax);
        form.add(new JLabel("Số điện thoại:")); form.add(txtPhone);
        form.add(new JLabel("Email:")); form.add(txtEmail);
        form.add(new JLabel("Địa chỉ:")); form.add(txtAddress);

        JButton btnAdd = new JButton("Thêm Nhà Cung Cấp");
        JButton btnClose = new JButton("Đóng");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnAdd);
        buttons.add(btnClose);
        JPanel south = new JPanel(new BorderLayout(6, 6));
        south.add(form, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);

        Runnable loadRows = () -> {
            model.setRowCount(0);
            for (Supplier s : service.suppliers(auth)) {
                model.addRow(new Object[]{s.id(), s.name(), s.taxCode(), s.phone(), s.email(), s.address()});
            }
        };
        loadRows.run();

        btnAdd.addActionListener(e -> {
            KetQua<Supplier> saved = service.saveSupplier(
                    new Supplier(0, txtName.getText().trim(), txtTax.getText().trim(),
                            txtPhone.getText().trim(), txtEmail.getText().trim(), txtAddress.getText().trim()), auth);
            if (saved.isFailure()) {
                JOptionPane.showMessageDialog(dialog, saved.getError().orElseThrow().message(), "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Supplier created = saved.valueOrThrow();
            loadRows.run();
            reloadSupplierCombo(service.suppliers(auth), created.id());
            txtName.setText("");
            txtTax.setText("");
            txtPhone.setText("");
            txtEmail.setText("");
            txtAddress.setText("");
        });
        btnClose.addActionListener(e -> dialog.dispose());

        dialog.setContentPane(root);
        dialog.setSize(720, 440);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private LocalDate parseExpiry() {
        String d = expiryDayField.getText().trim();
        String m = expiryMonthField.getText().trim();
        String y = expiryYearField.getText().trim();
        if (d.isEmpty() || m.isEmpty() || y.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ Hạn sử dụng (Ngày / Tháng / Năm).");
        }
        try {
            if (d.length() == 1) d = "0" + d;
            if (m.length() == 1) m = "0" + m;
            if (y.length() == 2) y = "20" + y;
            return LocalDate.parse(d + "/" + m + "/" + y, DATE_FMT);
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Hạn sử dụng không hợp lệ (nhập dd / MM / yyyy).");
        }
    }

    private void setActionsEnabled(boolean enabled) {
        saveDraft.setEnabled(enabled && service != null);
        importStock.setEnabled(enabled && service != null);
        if (addLineBtn != null) addLineBtn.setEnabled(enabled);
        if (removeLineBtn != null) removeLineBtn.setEnabled(enabled);
    }

    private boolean sameSession() {
        if (auth != null && auth.equals(KhamBenhPhienNguCanh.current())) return true;
        showInfo("Phiên làm việc đã thay đổi. Vui lòng mở lại màn hình.");
        return false;
    }

    private <T> void async(Callable<T> work, Consumer<T> onDone) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return work.call(); }
            @Override protected void done() {
                if (!sameSession()) return;
                try { onDone.accept(get()); }
                catch (Exception ex) { statusLabel.setText("Không tải được dữ liệu từ CSDL."); }
            }
        }.execute();
    }

    @SuppressWarnings("unchecked")
    private JComboBox<Object> comboBox(String key) { return (JComboBox<Object>) fields.get(key); }
    private JTextField textField(String key) { return (JTextField) fields.get(key); }
    private void setText(String key, String value) { textField(key).setText(Objects.toString(value, "")); }

    private static BigDecimal parseDecimal(String raw) {
        String clean = Objects.toString(raw, "").trim().replace(",", "");
        if (clean.isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(clean);
    }

    private static String formatDecimal(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    private static DocumentListener docListener(Runnable r) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        };
    }

    private void showInfo(String msg) {
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(this, msg, "Nhập Từ Nhà Cung Cấp", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Đặt nhãn và ô nhập theo tọa độ tham chiếu, co giãn chiều ngang cùng cửa sổ. */
    private void field(MauPanel p, String name, JComponent control, int x, int y, int labelWidth, int width, int height) {
        p.place(label(name), x, y, labelWidth, height);
        p.place(control, x + labelWidth, y, width, height);
        fields.put(name, control);
    }

    private static JLabel label(String title) {
        JLabel l = new JLabel(title);
        l.setFont(CHU);
        l.setForeground(Color.BLACK);
        return l;
    }

    private static JLabel amount(Color color) {
        JLabel l = label("0đ");
        l.setFont(CHU.deriveFont(16f));
        l.setForeground(color);
        l.setHorizontalAlignment(SwingConstants.RIGHT);
        l.setPreferredSize(new Dimension(101, 25));
        return l;
    }

    private static String today() { return LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }

    private static JTextField text(String value) {
        JTextField t = new JTextField(value);
        t.setFont(CHU);
        t.setBackground(Color.WHITE);
        t.setBorder(new CompoundBorder(new LineBorder(VIEN), new EmptyBorder(0, 4, 0, 3)));
        return t;
    }

    private static JTextField number() {
        JTextField t = text("0");
        t.setHorizontalAlignment(SwingConstants.CENTER);
        return t;
    }

    /** Ô ngày giữ đủ ngày/tháng/năm trong chiều rộng của bản thiết kế. */
    private static JPanel dateBox(JTextField value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new LineBorder(VIEN));
        value.setBorder(new EmptyBorder(0, 2, 0, 0));
        p.add(value, BorderLayout.CENTER);
        JLabel arrow = new JLabel(NhaThuocBieuTuong.createSmallArrowDownIcon());
        arrow.setPreferredSize(new Dimension(14, 20));
        arrow.setBorder(new MatteBorder(0, 1, 0, 0, VIEN));
        p.add(arrow, BorderLayout.EAST);
        return p;
    }

    private static JComboBox<Object> combo(String value) {
        JComboBox<Object> c = new JComboBox<>(value.isEmpty() ? new Object[]{} : new Object[]{value});
        c.setFont(CHU);
        c.setBackground(Color.WHITE);
        c.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                JButton b = new javax.swing.plaf.basic.BasicArrowButton(SwingConstants.SOUTH, Color.WHITE, VIEN, Color.GRAY, Color.WHITE);
                b.setPreferredSize(new Dimension(14, 18));
                return b;
            }
        });
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        renderer.setBorder(new EmptyBorder(0, 2, 0, 0));
        c.setRenderer(renderer);
        if (value.matches("\\d{2}/\\d{2}/\\d{4}")) c.setFont(CHU.deriveFont(12f));
        c.setBorder(new LineBorder(VIEN));
        return c;
    }

    private static JTable table(String[] columns, int[] widths) {
        JTable t = new JTable(new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        });
        t.setFont(CHU);
        t.setRowHeight(27);
        t.setBackground(Color.WHITE);
        t.setGridColor(new Color(230, 230, 230));
        t.setFillsViewportHeight(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        t.getTableHeader().setFont(CHU.deriveFont(12f));
        t.getTableHeader().setPreferredSize(new Dimension(100, 27));
        t.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer header = new DefaultTableCellRenderer();
        header.setFont(CHU);
        header.setBackground(Color.WHITE);
        header.setHorizontalAlignment(SwingConstants.CENTER);
        header.setBorder(new MatteBorder(0, 0, 1, 1, new Color(232, 232, 232)));
        t.getTableHeader().setDefaultRenderer(header);
        for (int i = 0; i < widths.length; i++) t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        if (columns.length > 3) {
            DefaultTableCellRenderer nameHeader = new DefaultTableCellRenderer();
            nameHeader.setHorizontalAlignment(SwingConstants.LEFT);
            nameHeader.setBackground(Color.WHITE);
            nameHeader.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 1, new Color(232, 232, 232)), new EmptyBorder(0, 4, 0, 0)));
            t.getColumnModel().getColumn(2).setHeaderRenderer(nameHeader);
        }
        return t;
    }

    private static JScrollPane scroll(JTable t, boolean horizontal) {
        JScrollPane s = new JScrollPane(t, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, horizontal ? JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS : JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        s.setColumnHeaderView(t.getTableHeader());
        s.setBorder(new LineBorder(VIEN));
        s.getViewport().setBackground(Color.WHITE);
        s.getHorizontalScrollBar().setPreferredSize(new Dimension(100, 17));
        return s;
    }

    static JButton button(String title, String icon) {
        JButton b = new JButton(title, new PhieuBieuTuong(icon));
        b.setFont(CHU);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorder(new EmptyBorder(0, 0, 0, 0));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JButton paymentButton(String title, String type) {
        JButton b = button(title, type);
        b.setFont(CHU.deriveFont(12f));
        Icon original = b.getIcon();
        b.setIcon(new Icon() {
            public int getIconWidth() { return 15; }
            public int getIconHeight() { return 16; }
            public void paintIcon(Component c, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.translate(x, y);
                g.scale(.56, .56);
                original.paintIcon(c, g, 0, 0);
                g.dispose();
            }
        });
        return b;
    }

    public JTable getDrugTable() { return drugTable; }
    public JTable getPaymentTable() { return paymentTable; }

    /** Mỗi vùng giữ tọa độ thiết kế riêng, tránh chồng ô khi đổi kích thước cửa sổ. */
    static final class MauPanel extends JPanel {
        private final int referenceWidth;
        MauPanel(int height) { this(height, 1580); }
        MauPanel(int height, int width) {
            super(null);
            referenceWidth = width;
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(Math.min(width, 1240), height));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        }

        void place(JComponent c, int x, int y, int w, int h) {
            c.putClientProperty("khungMau", new Rectangle(x, y, w, h));
            add(c);
        }

        @Override public void doLayout() {
            double scale = getWidth() / (double) referenceWidth;
            for (Component c : getComponents()) {
                Rectangle r = (Rectangle) ((JComponent) c).getClientProperty("khungMau");
                c.setBounds((int) Math.round(r.x * scale), r.y, (int) Math.round(r.width * scale), r.height);
            }
        }
    }

    private record PhieuBieuTuong(String type) implements Icon {
        public int getIconWidth() { return type.equals("menu") ? 30 : 26; }
        public int getIconHeight() { return 27; }
        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(3));
            g.setColor(new Color(0, 137, 218));
            switch (type) {
                case "menu" -> { for (int yy : new int[]{5, 13, 21}) g.drawLine(2, yy, 28, yy); }
                case "check" -> { g.setColor(new Color(20, 25, 220)); g.drawPolyline(new int[]{2, 10, 24}, new int[]{13, 21, 5}, 3); }
                case "save" -> { g.setColor(new Color(130, 125, 170)); g.drawRect(2, 2, 22, 23); g.drawRect(8, 3, 10, 7); g.drawRect(6, 16, 14, 8); }
                case "plus" -> { g.setStroke(new BasicStroke(2)); g.drawLine(7, 6, 7, 20); g.drawLine(0, 13, 14, 13); }
                default -> { g.setColor(Color.GRAY); g.setStroke(new BasicStroke(2)); g.drawLine(5, 7, 17, 19); g.drawLine(17, 7, 5, 19); }
            }
            g.dispose();
        }
    }
}
