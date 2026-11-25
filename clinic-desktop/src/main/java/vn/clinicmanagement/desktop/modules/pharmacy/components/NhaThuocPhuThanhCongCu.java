package vn.clinicmanagement.desktop.modules.pharmacy.components;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

/**
 * Thanh công cụ phụ (Sub-Toolbar) trên đầu màn hình Quản lý Kho / Tủ Trực Vật Tư.
 * Khớp 100% tỷ lệ và màu sắc ảnh mẫu EHC.
 */
public class NhaThuocPhuThanhCongCu extends JPanel {

    private final JButton btnMenu;
    private final JButton btnReport;
    private final JButton btnList;
    private final JButton btnExport;
    public JButton getBtnExport(){return btnExport;}
    private final JButton btnCreate;
    private final JButton btnSearch;
    private final JTextField txtTicketCode;
    private final JTextField txtFromDate;
    private final JTextField txtToDate;

    private JComboBox<String> cboSearchMode;
    private JPopupMenu searchPresetMenu;
    private Consumer<String> searchListener;
    /** Hàm được gọi khi chọn một loại phiếu trong menu. */
    private Consumer<String> createTicketListener;
    private final NhaThuocTaoPhieuMenu createTicketMenu;
    private final NhaThuocDanhSachMenu listMenu;
    private Consumer<vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.DanhSach> listListener;
    private final NhaThuocBaoCaoMenu reportMenu;
    private Consumer<vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.LoaiBaoCao> reportListener;
    public NhaThuocDanhSachMenu getListMenu(){return listMenu;}
    public void setOnListSelected(Consumer<vn.clinicmanagement.application.inventory.NhaThuocDanhSachReadCongKetNoi.DanhSach> listener){listListener=listener;}
    public NhaThuocBaoCaoMenu getReportMenu(){return reportMenu;}
    public void setOnReportSelected(Consumer<vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.LoaiBaoCao> listener){reportListener=listener;}


    public NhaThuocPhuThanhCongCu() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1000, 58));
        setBackground(new Color(235, 241, 247));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(189, 202, 215)));

        // --- CỤM BÊN TRÁI: Menu, Báo Cáo, Danh Sách, Tạo Phiếu, Mã Phiếu ---
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);

        btnMenu = createToolbarButton("Menu", NhaThuocBieuTuong.createMenuIcon(), false);
        btnReport = createToolbarButton("Báo Cáo ▾", NhaThuocBieuTuong.createReportIcon(), false);
        reportMenu = new NhaThuocBaoCaoMenu(type -> {
            if (reportListener != null) {
                reportListener.accept(type);
            }
        });
        reportMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                btnReport.putClientProperty("menuTaoPhieuMo", true);
                btnReport.repaint();
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                btnReport.putClientProperty("menuTaoPhieuMo", false);
                btnReport.repaint();
            }
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                popupMenuWillBecomeInvisible(e);
            }
        });
        btnReport.addActionListener(e -> {
            if (reportMenu.isVisible()) {
                reportMenu.setVisible(false);
            } else if (btnReport.isShowing()) {
                reportMenu.show(btnReport, 0, btnReport.getHeight());
            }
        });
        btnReport.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DOWN"), "openReportMenu");
        btnReport.getActionMap().put("openReportMenu", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                btnReport.doClick();
            }
        });
        btnExport=createToolbarButton("Xuất Dữ Liệu",NhaThuocBieuTuong.createReportIcon(),false);
        btnExport.setPreferredSize(new Dimension(92,54));
        btnList = createToolbarButton("Danh Sách", NhaThuocBieuTuong.createListIcon(), false);
        listMenu=new NhaThuocDanhSachMenu(type->{if(listListener!=null)listListener.accept(type);});
        listMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener(){
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e){btnList.putClientProperty("menuTaoPhieuMo",true);btnList.repaint();}
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e){btnList.putClientProperty("menuTaoPhieuMo",false);btnList.repaint();}
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e){popupMenuWillBecomeInvisible(e);}
        });
        btnList.addActionListener(e->{if(listMenu.isVisible())listMenu.setVisible(false);else if(btnList.isShowing())listMenu.show(btnList,0,btnList.getHeight());});
        btnList.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DOWN"),"openList");
        btnList.getActionMap().put("openList",new AbstractAction(){public void actionPerformed(java.awt.event.ActionEvent e){btnList.doClick();}});
        // // Đang chọn (highlight)
        btnCreate = createToolbarButton("Tạo Phiếu ▾", NhaThuocBieuTuong.createPlusIcon(), false);
        btnCreate.setContentAreaFilled(false);
        btnCreate.getAccessibleContext().setAccessibleName("Tạo Phiếu");
        createTicketMenu = new NhaThuocTaoPhieuMenu(type -> {
            if (createTicketListener != null) createTicketListener.accept(type);
        });
        createTicketMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent event) { highlightCreate(true); }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent event) { highlightCreate(false); }
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent event) { highlightCreate(false); }
        });
        btnCreate.addActionListener(e -> {
            if (createTicketMenu.isVisible()) createTicketMenu.setVisible(false);
            else if (btnCreate.isShowing()) createTicketMenu.show(btnCreate, 0, btnCreate.getHeight());
        });
        btnCreate.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DOWN"), "openCreateMenu");
        btnCreate.getActionMap().put("openCreateMenu", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { btnCreate.doClick(); }
        });

        JLabel lblTicketCode = new JLabel("Mã phiếu:");
        lblTicketCode.setFont(UngDungPhongChu.BODY_BOLD);
        lblTicketCode.setForeground(new Color(20, 20, 20));
        lblTicketCode.setBorder(new EmptyBorder(0, 8, 0, 2));

        txtTicketCode = new JTextField();
        txtTicketCode.setFont(UngDungPhongChu.BODY);
        txtTicketCode.setPreferredSize(new Dimension(115, 24));
        txtTicketCode.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(165, 180, 196), 1),
                new EmptyBorder(1, 4, 1, 4)
        ));
        txtTicketCode.addActionListener(e -> triggerSearch());

        leftPanel.add(btnMenu);
        leftPanel.add(btnReport);
        leftPanel.add(btnExport);
        leftPanel.add(btnList);
        leftPanel.add(btnCreate);


        // --- CỤM BÊN PHẢI: Tìm theo, Mã phiếu, Từ ngày, Đến ngày, Nút Tìm Kiếm ▾ ---
        JPanel rightPanel = new JPanel(new BorderLayout(8, 0));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(new EmptyBorder(4, 4, 4, 6));

        JLabel lblSearchMode = new JLabel("Tìm theo:");
        lblSearchMode.setFont(UngDungPhongChu.BODY);
        lblSearchMode.setForeground(new Color(30, 30, 30));

        cboSearchMode = new JComboBox<>(new String[]{"Hiển thị tổng hợp", "Ngày tạo", "Ngày nhập xuất"});
        cboSearchMode.setFont(UngDungPhongChu.BODY);
        cboSearchMode.setBackground(Color.WHITE);
        cboSearchMode.setPreferredSize(new Dimension(140, 22));
        cboSearchMode.addActionListener(e -> triggerSearch());

        lblTicketCode.setBorder(new EmptyBorder(0, 0, 0, 0));
        txtTicketCode.setPreferredSize(new Dimension(140, 22));

        JLabel lblFrom = new JLabel("Từ ngày:");
        lblFrom.setFont(UngDungPhongChu.BODY);
        lblFrom.setForeground(new Color(30, 30, 30));

        txtFromDate = createDateTimeBox("00:00 " + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        JLabel lblTo = new JLabel("Đến ngày:");
        lblTo.setFont(UngDungPhongChu.BODY);
        lblTo.setForeground(new Color(30, 30, 30));

        txtToDate = createDateTimeBox("23:59 " + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        searchPresetMenu = buildSearchPresetMenu();
        btnSearch = createSearchButton();
        btnSearch.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) || e.getX() >= btnSearch.getWidth() - 22) {
                    e.consume();
                    btnSearch.putClientProperty("skipNextClick", Boolean.TRUE);
                    if (btnSearch.isShowing()) {
                        searchPresetMenu.show(btnSearch, btnSearch.getWidth() - searchPresetMenu.getPreferredSize().width, btnSearch.getHeight());
                    }
                }
            }
        });
        btnSearch.addActionListener(e -> {
            if (Boolean.TRUE.equals(btnSearch.getClientProperty("skipNextClick"))) {
                btnSearch.putClientProperty("skipNextClick", Boolean.FALSE);
                return;
            }
            triggerSearch();
        });
        btnSearch.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DOWN"), "openSearchPresets");
        btnSearch.getActionMap().put("openSearchPresets", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (btnSearch.isShowing()) {
                    searchPresetMenu.show(btnSearch, btnSearch.getWidth() - searchPresetMenu.getPreferredSize().width, btnSearch.getHeight());
                }
            }
        });

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(1, 4, 1, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;
        gbc.gridx = 0; fields.add(lblSearchMode, gbc);
        gbc.gridx = 1; fields.add(cboSearchMode, gbc);
        gbc.gridx = 2; fields.add(lblFrom, gbc);
        gbc.gridx = 3; fields.add(txtFromDate, gbc);
        gbc.gridy = 1;
        gbc.gridx = 0; fields.add(lblTicketCode, gbc);
        gbc.gridx = 1; fields.add(txtTicketCode, gbc);
        gbc.gridx = 2; fields.add(lblTo, gbc);
        gbc.gridx = 3; fields.add(txtToDate, gbc);
        rightPanel.add(fields, BorderLayout.CENTER);
        rightPanel.add(btnSearch, BorderLayout.EAST);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
    }

    /** Tô nền nút khi menu đang mở; tự bỏ chọn khi đóng hoặc nhấn Escape. */
    private void highlightCreate(boolean visible) {
        btnCreate.putClientProperty("menuTaoPhieuMo", visible);
        btnCreate.repaint();
    }

    /** Đăng ký xử lý khi chọn một loại phiếu, chưa gắn thao tác ghi dữ liệu. */
    public void setOnCreateTicketListener(Consumer<String> listener) {
        this.createTicketListener = listener;
    }

    public NhaThuocTaoPhieuMenu getCreateTicketMenu() {
        return createTicketMenu;
    }

    private JButton createToolbarButton(String text, Icon icon, boolean isSelected) {
        Icon largeIcon=new Icon(){
            public int getIconWidth(){return 24;}public int getIconHeight(){return 24;}
            public void paintIcon(Component c,Graphics g,int x,int y){var copy=(Graphics2D)g.create();copy.translate(x,y);copy.scale(24d/icon.getIconWidth(),24d/icon.getIconHeight());icon.paintIcon(c,copy,0,0);copy.dispose();}
        };
        JButton btn = new JButton(text, largeIcon) {
            @Override
            protected void paintComponent(Graphics g) {
                if (isSelected || Boolean.TRUE.equals(getClientProperty("menuTaoPhieuMo"))) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(220, 233, 246));
                    g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 4, 4));
                    g2.setColor(new Color(75, 145, 215));
                    g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 3, 4, 4));
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };

        btn.setFont(UngDungPhongChu.SMALL);
        btn.setForeground(new Color(25, 35, 45));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(!isSelected);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setIconTextGap(2);
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setPreferredSize(new Dimension(text.equals("Menu")?54:text.startsWith("Tạo")?88:80,54));
        btn.setMargin(new Insets(2, 6, 2, 6));

        if (!isSelected) {
            btn.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    btn.setBorder(BorderFactory.createCompoundBorder(
                            new LineBorder(new Color(185, 205, 225), 1),
                            new EmptyBorder(1, 5, 1, 5)
                    ));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    btn.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                }
            });
        }

        return btn;
    }

    private JTextField createDateTimeBox(String text) {
        JTextField tf = new JTextField(text);
        tf.setFont(UngDungPhongChu.BODY);
        tf.setEditable(true);
        tf.setPreferredSize(new Dimension(132, 22));
        tf.setBackground(Color.WHITE);

        // Icon mũi tên trỏ xuống bên phải — bấm để chọn nhanh ngày
        tf.setLayout(new BorderLayout());
        JLabel arrow = new JLabel(NhaThuocBieuTuong.createSmallArrowDownIcon());
        arrow.setCursor(new Cursor(Cursor.HAND_CURSOR));
        arrow.setBorder(new EmptyBorder(0, 2, 0, 4));
        arrow.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                pickDateForField(tf);
            }
        });
        tf.add(arrow, BorderLayout.EAST);
        tf.addActionListener(e -> triggerSearch());

        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(165, 180, 196), 1),
                new EmptyBorder(1, 6, 1, 18)
        ));
        return tf;
    }

    private void pickDateForField(JTextField tf) {
        if (GraphicsEnvironment.isHeadless()) return;
        var fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/uuuu")
                .withResolverStyle(java.time.format.ResolverStyle.STRICT);
        java.time.LocalDateTime current;
        try {
            current = java.time.LocalDateTime.parse(tf.getText().trim(), fmt);
        } catch (Exception ex) {
            current = java.time.LocalDateTime.now();
        }
        var spinner = new JSpinner(new SpinnerDateModel(java.sql.Timestamp.valueOf(current), null, null, java.util.Calendar.DAY_OF_MONTH));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "HH:mm dd/MM/yyyy"));
        if (JOptionPane.showConfirmDialog(this, spinner, "Chọn ngày giờ", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                spinner.commitEdit();
                java.util.Date val = (java.util.Date) spinner.getValue();
                tf.setText(new java.text.SimpleDateFormat("HH:mm dd/MM/yyyy").format(val));
                triggerSearch();
            } catch (java.text.ParseException ignored) {}
        }
    }

    private JPopupMenu buildSearchPresetMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Color.WHITE);
        menu.setBorder(new LineBorder(new Color(163, 163, 155)));
        addPreset(menu, "Hôm Nay");
        addPreset(menu, "Trong Tuần");
        addPreset(menu, "Trong Tháng");
        menu.addSeparator();
        addPreset(menu, "Hôm Qua");
        addPreset(menu, "7 Ngày Gần Đây");
        addPreset(menu, "30 Ngày Gần Đây");
        menu.addSeparator();
        addPreset(menu, "3 Ngày Tới");
        addPreset(menu, "7 Ngày Tới");
        return menu;
    }

    private void addPreset(JPopupMenu menu, String label) {
        JMenuItem item = new JMenuItem(label);
        item.setFont(UngDungPhongChu.BODY);
        item.setBackground(Color.WHITE);
        item.setPreferredSize(new Dimension(160, 24));
        item.addActionListener(e -> applyDatePreset(label));
        menu.add(item);
    }

    /** Áp dụng mốc thời gian nhanh và tự động tìm kiếm lại. */
    public void applyDatePreset(String preset) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate from;
        java.time.LocalDate to;
        switch (preset) {
            case "Trong Tuần" -> {
                from = today.with(java.time.DayOfWeek.MONDAY);
                to = today.with(java.time.DayOfWeek.SUNDAY);
            }
            case "Trong Tháng" -> {
                from = today.withDayOfMonth(1);
                to = today.withDayOfMonth(today.lengthOfMonth());
            }
            case "Hôm Qua" -> {
                from = today.minusDays(1);
                to = from;
            }
            case "7 Ngày Gần Đây" -> {
                from = today.minusDays(6);
                to = today;
            }
            case "30 Ngày Gần Đây" -> {
                from = today.minusDays(29);
                to = today;
            }
            case "3 Ngày Tới" -> {
                from = today;
                to = today.plusDays(2);
            }
            case "7 Ngày Tới" -> {
                from = today;
                to = today.plusDays(6);
            }
            default -> {
                from = today;
                to = today;
            }
        }
        var dateFmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        txtFromDate.setText("00:00 " + from.format(dateFmt));
        txtToDate.setText("23:59 " + to.format(dateFmt));
        triggerSearch();
    }

    private JButton createSearchButton() {
        JButton btn = new JButton("Tìm Kiếm", NhaThuocBieuTuong.createSearchIcon()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                GradientPaint gp = new GradientPaint(0, 0, new Color(248, 250, 252), 0, h, new Color(225, 234, 244));
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 4, 4));

                g2.setColor(new Color(155, 175, 198));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 4, 4));

                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(UngDungPhongChu.BODY_BOLD);
        btn.setForeground(new Color(25, 45, 75));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setIconTextGap(4);
        btn.setMargin(new Insets(2, 8, 2, 4));

        // Thêm mũi tên dropdown nhỏ ▾ bên phải chữ
        btn.setLayout(new BorderLayout());
        JLabel arrow = new JLabel(NhaThuocBieuTuong.createSmallArrowDownIcon());
        arrow.setBorder(new EmptyBorder(0, 0, 0, 6));
        btn.add(arrow, BorderLayout.EAST);
        btn.setPreferredSize(new Dimension(115, 26));

        return btn;
    }

    public void setOnSearchListener(Consumer<String> listener) {
        this.searchListener = listener;
    }

    private void triggerSearch() {
        if (searchListener != null) {
            searchListener.accept(getTicketCode());
        }
    }

    public String getTicketCode() {
        return txtTicketCode.getText();
    }

    public JButton getBtnMenu() {
        return btnMenu;
    }

    public JButton getBtnReport() {
        return btnReport;
    }

    public JButton getBtnList() {
        return btnList;
    }

    public JButton getBtnCreate() {
        return btnCreate;
    }

    public JButton getBtnSearch() {
        return btnSearch;
    }

    public JTextField getTxtTicketCode() {
        return txtTicketCode;
    }

    public JTextField getTxtFromDate() {
        return txtFromDate;
    }

    public JTextField getTxtToDate() {
        return txtToDate;
    }

    public JPopupMenu getSearchPresetMenu() {
        return searchPresetMenu;
    }

    public JComboBox<String> getCboSearchMode() {
        return cboSearchMode;
    }
}
