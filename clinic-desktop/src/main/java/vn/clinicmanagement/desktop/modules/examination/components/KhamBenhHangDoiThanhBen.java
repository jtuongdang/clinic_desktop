package vn.clinicmanagement.desktop.modules.examination.components;

import static vn.clinicmanagement.desktop.modules.examination.components.KhamBenhManHinhIconVaRenderer.*;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhManHinh.AdminPatientItem;
import vn.clinicmanagement.domain.facility.Khoa;
import vn.clinicmanagement.domain.facility.PhongKham;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Cột danh sách bệnh nhân hàng đợi bên trái của màn hình Khám Bệnh.
 */
public final class KhamBenhHangDoiThanhBen extends JPanel {

    @FunctionalInterface
    public interface ContextMenuHandler {
        void onShowContextMenu(Component invoker, int x, int y, int selectedRow, AdminPatientItem patient);
    }

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter SHORT_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final DateTimeFormatter INPUT_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private final List<AdminPatientItem> allLoadedPatients = new ArrayList<>();
    private final List<AdminPatientItem> patientList = new ArrayList<>();
    private final DefaultTableModel queueModel;
    private final JTable queueTable;
    private final JLabel lblTuyChon;
    private final JLabel lblTabPatients;
    private final JLabel lblFilter;
    private final JPanel searchBarPanel;
    private final JTextField txtQuickSearch;
    private final JLabel lblClinicSelection;
    private final JButton btnDepartmentDrop;
    private final Consumer<AdminPatientItem> onPatientSelected;
    private LocalDate fromDate = LocalDate.now(BUSINESS_ZONE);
    private LocalDate toDate = LocalDate.now(BUSINESS_ZONE);
    private String statusFilter = "ALL";
    private String payerFilter = "ALL";
    private String searchKeyword = "";
    private Runnable onOpenDepartmentFilter;
    private Runnable onOpenRoomFilter;
    private Runnable onDateRangeChanged;

    public KhamBenhHangDoiThanhBen(
            Consumer<AdminPatientItem> onPatientSelected,
            ContextMenuHandler onContextMenu) {
        this(onPatientSelected, onContextMenu, null, null);
    }

    public KhamBenhHangDoiThanhBen(
            Consumer<AdminPatientItem> onPatientSelected,
            ContextMenuHandler onContextMenu,
            Runnable onOpenDepartmentFilter,
            Runnable onOpenRoomFilter) {
        this.onPatientSelected = onPatientSelected;
        this.onOpenDepartmentFilter = onOpenDepartmentFilter;
        this.onOpenRoomFilter = onOpenRoomFilter;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(195, 204, 218)));
        setPreferredSize(new Dimension(300, 100));

        // Thanh tiêu đề con: [◀] Tùy Chọn | [Tất cả bệnh nhân (N)]
        JPanel leftSubHeader = new JPanel(new BorderLayout());
        leftSubHeader.setBackground(new Color(235, 241, 250));
        leftSubHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 210, 224)));
        leftSubHeader.setPreferredSize(new Dimension(300, 28));

        JPanel leftTuyChonBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
        leftTuyChonBox.setOpaque(false);
        leftTuyChonBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        leftTuyChonBox.setToolTipText("Lọc theo trạng thái khám và đối tượng");
        leftTuyChonBox.add(new JLabel(new LeftTriangleIcon()));
        lblTuyChon = new JLabel("Tùy Chọn");
        lblTuyChon.setFont(UngDungPhongChu.SMALL);
        lblTuyChon.setForeground(new Color(30, 30, 30));
        leftTuyChonBox.add(lblTuyChon);
        leftTuyChonBox.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                showOptionsMenu(leftTuyChonBox);
            }
        });

        JPanel tabPatients = new JPanel(new BorderLayout());
        tabPatients.setBackground(new Color(254, 240, 205));
        tabPatients.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 0, new Color(220, 185, 120)));
        lblTabPatients = new JLabel("Tất cả bệnh nhân (0)", SwingConstants.CENTER);
        lblTabPatients.setFont(UngDungPhongChu.BODY_BOLD);
        lblTabPatients.setForeground(new Color(20, 20, 20));
        tabPatients.add(lblTabPatients, BorderLayout.CENTER);

        leftSubHeader.add(leftTuyChonBox, BorderLayout.WEST);
        leftSubHeader.add(tabPatients, BorderLayout.CENTER);

        // Thanh bộ lọc ngày động (bấm để chọn khoảng ngày; bấm kính lúp để tìm nhanh)
        JPanel filterHeader = new JPanel(new BorderLayout(4, 0));
        filterHeader.setBackground(new Color(255, 255, 208));
        filterHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(195, 205, 185)),
                new EmptyBorder(2, 6, 2, 6)
        ));
        filterHeader.setPreferredSize(new Dimension(300, 24));

        lblFilter = new JLabel("", SwingConstants.LEFT);
        lblFilter.setFont(UngDungPhongChu.SMALL);
        lblFilter.setForeground(new Color(20, 20, 20));
        lblFilter.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblFilter.setToolTipText("Bấm để chọn khoảng ngày khám (Hôm nay, Hôm qua, 7 ngày, 30 ngày, Tùy chọn...)");
        lblFilter.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                showDateRangeMenu(filterHeader);
            }
        });
        filterHeader.add(lblFilter, BorderLayout.CENTER);

        JLabel lblMagnifier = new JLabel(new MiniMagnifierIcon());
        lblMagnifier.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblMagnifier.setToolTipText("Tìm nhanh theo tên, mã bệnh nhân hoặc số điện thoại");
        lblMagnifier.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                toggleQuickSearchBar();
            }
        });
        filterHeader.add(lblMagnifier, BorderLayout.EAST);

        // Thanh tìm nhanh ẩn/hiện khi bấm kính lúp
        searchBarPanel = new JPanel(new BorderLayout(4, 0));
        searchBarPanel.setBackground(new Color(250, 252, 255));
        searchBarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(195, 205, 220)),
                new EmptyBorder(2, 6, 2, 4)
        ));
        searchBarPanel.setPreferredSize(new Dimension(300, 26));
        txtQuickSearch = new JTextField();
        txtQuickSearch.setFont(UngDungPhongChu.SMALL);
        txtQuickSearch.setToolTipText("Nhập tên, mã BN hoặc SĐT để lọc nhanh");
        txtQuickSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onQuickSearchChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { onQuickSearchChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onQuickSearchChanged(); }
        });
        JButton btnClearSearch = new JButton("✕");
        btnClearSearch.setFont(UngDungPhongChu.SMALL);
        btnClearSearch.setMargin(new Insets(0, 4, 0, 4));
        btnClearSearch.setFocusable(false);
        btnClearSearch.setToolTipText("Đóng tìm nhanh");
        btnClearSearch.addActionListener(e -> {
            txtQuickSearch.setText("");
            searchBarPanel.setVisible(false);
            revalidate();
        });
        searchBarPanel.add(txtQuickSearch, BorderLayout.CENTER);
        searchBarPanel.add(btnClearSearch, BorderLayout.EAST);
        searchBarPanel.setVisible(false);

        JPanel leftNorth = new JPanel();
        leftNorth.setLayout(new BoxLayout(leftNorth, BoxLayout.Y_AXIS));
        leftNorth.add(leftSubHeader);
        leftNorth.add(filterHeader);
        leftNorth.add(searchBarPanel);
        add(leftNorth, BorderLayout.NORTH);

        // Bảng danh sách hàng đợi
        queueModel = new DefaultTableModel(new String[]{"STT", "Tên Bệnh Nhân", "Tuổi"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        queueTable = new JTable(queueModel);
        queueTable.setRowHeight(47);
        queueTable.setShowHorizontalLines(true);
        queueTable.setShowVerticalLines(false);
        queueTable.setGridColor(new Color(238, 241, 246));
        queueTable.setIntercellSpacing(new Dimension(0, 1));
        queueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        queueTable.setBackground(Color.WHITE);

        JTableHeader qHeader = queueTable.getTableHeader();
        qHeader.setFont(UngDungPhongChu.SMALL_BOLD);
        qHeader.setPreferredSize(new Dimension(300, 23));
        qHeader.setBackground(new Color(242, 245, 250));
        qHeader.setForeground(new Color(25, 25, 25));
        qHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 212, 222)));

        queueTable.getColumnModel().getColumn(0).setPreferredWidth(44);
        queueTable.getColumnModel().getColumn(0).setMaxWidth(48);
        queueTable.getColumnModel().getColumn(1).setPreferredWidth(212);
        queueTable.getColumnModel().getColumn(2).setPreferredWidth(42);
        queueTable.getColumnModel().getColumn(2).setMaxWidth(46);

        queueTable.getColumnModel().getColumn(0).setCellRenderer(new SttCellRenderer());
        queueTable.getColumnModel().getColumn(1).setCellRenderer(new PatientNameCellRenderer());

        DefaultTableCellRenderer ageRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(UngDungPhongChu.SMALL);
                lbl.setForeground(isSelected ? Color.WHITE : new Color(40, 40, 40));
                lbl.setBackground(isSelected ? new Color(0, 114, 198) : Color.WHITE);
                return lbl;
            }
        };
        queueTable.getColumnModel().getColumn(2).setCellRenderer(ageRenderer);

        queueTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = queueTable.getSelectedRow();
                if (row >= 0 && row < patientList.size() && onPatientSelected != null) {
                    onPatientSelected.accept(patientList.get(row));
                }
            }
        });

        queueTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }

            private void checkPopup(MouseEvent e) {
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    int row = queueTable.rowAtPoint(e.getPoint());
                    AdminPatientItem clickedPatient = null;
                    if (row >= 0 && row < patientList.size()) {
                        queueTable.setRowSelectionInterval(row, row);
                        clickedPatient = patientList.get(row);
                        if (onPatientSelected != null) {
                            onPatientSelected.accept(clickedPatient);
                        }
                    } else {
                        int sel = queueTable.getSelectedRow();
                        if (sel >= 0 && sel < patientList.size()) {
                            clickedPatient = patientList.get(sel);
                        }
                    }
                    if (onContextMenu != null) {
                        onContextMenu.onShowContextMenu(e.getComponent(), e.getX(), e.getY(),
                                queueTable.getSelectedRow(), clickedPatient);
                    }
                }
            }
        });

        JScrollPane queueScroll = new JScrollPane(queueTable);
        queueScroll.setColumnHeaderView(queueTable.getTableHeader());
        queueScroll.getViewport().setBackground(Color.WHITE);
        queueScroll.setBorder(BorderFactory.createEmptyBorder());
        add(queueScroll, BorderLayout.CENTER);

        // Footer: Ô chọn phòng khám
        JPanel leftFooter = new JPanel(new BorderLayout());
        leftFooter.setBackground(Color.WHITE);
        leftFooter.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(185, 192, 202)));
        leftFooter.setPreferredSize(new Dimension(300, 24));

        lblClinicSelection = new JLabel("(Tất cả phòng khám)");
        lblClinicSelection.setFont(UngDungPhongChu.SMALL);
        lblClinicSelection.setForeground(new Color(145, 35, 35));
        lblClinicSelection.setBorder(new EmptyBorder(0, 6, 0, 4));
        lblClinicSelection.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblClinicSelection.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (onOpenRoomFilter != null) onOpenRoomFilter.run();
            }
        });

        btnDepartmentDrop = new JButton("▾");
        btnDepartmentDrop.setFont(UngDungPhongChu.SMALL);
        btnDepartmentDrop.setForeground(new Color(90, 90, 90));
        btnDepartmentDrop.setOpaque(true);
        btnDepartmentDrop.setBackground(new Color(238, 240, 244));
        btnDepartmentDrop.setFocusPainted(false);
        btnDepartmentDrop.setPreferredSize(new Dimension(24, 22));
        btnDepartmentDrop.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(195, 200, 208)));
        btnDepartmentDrop.addActionListener(e -> {
            if (onOpenDepartmentFilter != null) onOpenDepartmentFilter.run();
        });

        leftFooter.add(lblClinicSelection, BorderLayout.CENTER);
        leftFooter.add(btnDepartmentDrop, BorderLayout.EAST);
        add(leftFooter, BorderLayout.SOUTH);

        updateSummaryLabels();
    }

    public void setClinicFilterHandlers(Runnable onOpenDepartmentFilter, Runnable onOpenRoomFilter) {
        this.onOpenDepartmentFilter = onOpenDepartmentFilter;
        this.onOpenRoomFilter = onOpenRoomFilter;
    }

    public void setClinicSelectionLabel(String text) {
        lblClinicSelection.setText(text == null || text.isBlank() ? "(Tất cả phòng khám)" : text);
    }

    public void showDepartmentMenu(List<Khoa> departments, int selectedDepartmentId, boolean allowAllDepartments,
                                   Consumer<Integer> onDepartmentSelected) {
        JPopupMenu menu = new JPopupMenu();
        if (allowAllDepartments) {
            JMenuItem all = new JMenuItem("Tất cả khoa được phân công");
            all.setSelected(selectedDepartmentId == 0);
            all.addActionListener(e -> onDepartmentSelected.accept(0));
            menu.add(all);
            menu.addSeparator();
        }
        if (departments != null) {
            departments.stream()
                    .filter(department -> department != null && department.getId() != null)
                    .forEach(department -> {
                        JMenuItem item = new JMenuItem(department.getTenKhoa());
                        item.setEnabled(department.getId() != selectedDepartmentId);
                        item.addActionListener(e -> onDepartmentSelected.accept(department.getId()));
                        menu.add(item);
                    });
        }
        menu.show(btnDepartmentDrop, 0, -menu.getPreferredSize().height);
    }

    public void showRoomMenu(List<PhongKham> rooms, int selectedRoomId,
                             Consumer<Integer> onRoomSelected) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem all = new JMenuItem("Tất cả phòng trong khoa");
        all.setEnabled(selectedRoomId != 0);
        all.addActionListener(e -> onRoomSelected.accept(0));
        menu.add(all);
        menu.addSeparator();
        if (rooms != null) {
            rooms.stream()
                    .filter(room -> room != null && room.getId() != null)
                    .forEach(room -> {
                        JMenuItem item = new JMenuItem(room.getTenPhong());
                        item.setEnabled(room.getId() != selectedRoomId);
                        item.addActionListener(e -> onRoomSelected.accept(room.getId()));
                        menu.add(item);
                    });
        }
        menu.show(lblClinicSelection, 0, -menu.getPreferredSize().height);
    }

    public void setOnDateRangeChanged(Runnable onDateRangeChanged) {
        this.onDateRangeChanged = onDateRangeChanged;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setDateRange(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate start = from != null ? from : today;
        LocalDate end = to != null ? to : start;
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        this.fromDate = start;
        this.toDate = end;
        updateSummaryLabels();
        if (onDateRangeChanged != null) {
            onDateRangeChanged.run();
        }
    }

    private void showDateRangeMenu(Component invoker) {
        JPopupMenu menu = new JPopupMenu();
        LocalDate today = LocalDate.now(BUSINESS_ZONE);

        JMenuItem miToday = new JMenuItem("Hôm nay (" + today.format(SHORT_DATE_FMT) + ")");
        miToday.addActionListener(e -> setDateRange(today, today));
        menu.add(miToday);

        LocalDate yesterday = today.minusDays(1);
        JMenuItem miYesterday = new JMenuItem("Hôm qua (" + yesterday.format(SHORT_DATE_FMT) + ")");
        miYesterday.addActionListener(e -> setDateRange(yesterday, yesterday));
        menu.add(miYesterday);

        JMenuItem mi7Days = new JMenuItem("7 ngày gần đây");
        mi7Days.addActionListener(e -> setDateRange(today.minusDays(6), today));
        menu.add(mi7Days);

        JMenuItem mi30Days = new JMenuItem("30 ngày gần đây");
        mi30Days.addActionListener(e -> setDateRange(today.minusDays(29), today));
        menu.add(mi30Days);

        menu.addSeparator();
        JMenuItem miCustom = new JMenuItem("Chọn khoảng ngày...");
        miCustom.addActionListener(e -> promptCustomDateRange());
        menu.add(miCustom);

        menu.show(invoker, 0, invoker.getHeight());
    }

    private void promptCustomDateRange() {
        if (GraphicsEnvironment.isHeadless()) return;
        JTextField txtFrom = new JTextField(fromDate.format(INPUT_DATE_FMT), 12);
        JTextField txtTo = new JTextField(toDate.format(INPUT_DATE_FMT), 12);
        JPanel panel = new JPanel(new GridLayout(2, 2, 6, 6));
        panel.add(new JLabel("Từ ngày (dd/MM/yyyy):"));
        panel.add(txtFrom);
        panel.add(new JLabel("Đến ngày (dd/MM/yyyy):"));
        panel.add(txtTo);
        int opt = JOptionPane.showConfirmDialog(this, panel, "Lọc Hàng Đợi Theo Ngày",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;
        try {
            LocalDate start = LocalDate.parse(txtFrom.getText().trim(), INPUT_DATE_FMT);
            LocalDate end = LocalDate.parse(txtTo.getText().trim(), INPUT_DATE_FMT);
            setDateRange(start, end);
        } catch (DateTimeException ex) {
            JOptionPane.showMessageDialog(this, "Ngày không hợp lệ. Vui lòng nhập theo định dạng dd/MM/yyyy.",
                    "Lọc Hàng Đợi", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showOptionsMenu(Component invoker) {
        JPopupMenu menu = new JPopupMenu();

        JLabel hdrStatus = new JLabel("  Trạng thái khám");
        hdrStatus.setFont(UngDungPhongChu.SMALL_BOLD);
        hdrStatus.setForeground(new Color(80, 95, 115));
        menu.add(hdrStatus);

        ButtonGroup statusGroup = new ButtonGroup();
        addRadioOption(menu, statusGroup, "Tất cả trạng thái", "ALL".equals(statusFilter), () -> {
            statusFilter = "ALL";
            applyLocalFilters();
        });
        addRadioOption(menu, statusGroup, "Chờ khám", "CHO_KHAM".equals(statusFilter), () -> {
            statusFilter = "CHO_KHAM";
            applyLocalFilters();
        });
        addRadioOption(menu, statusGroup, "Đang khám", "DANG_KHAM".equals(statusFilter), () -> {
            statusFilter = "DANG_KHAM";
            applyLocalFilters();
        });
        addRadioOption(menu, statusGroup, "Đã khám / Hoàn thành", "DA_KHAM".equals(statusFilter), () -> {
            statusFilter = "DA_KHAM";
            applyLocalFilters();
        });

        menu.addSeparator();
        JLabel hdrPayer = new JLabel("  Đối tượng");
        hdrPayer.setFont(UngDungPhongChu.SMALL_BOLD);
        hdrPayer.setForeground(new Color(80, 95, 115));
        menu.add(hdrPayer);

        ButtonGroup payerGroup = new ButtonGroup();
        addRadioOption(menu, payerGroup, "Tất cả đối tượng", "ALL".equals(payerFilter), () -> {
            payerFilter = "ALL";
            applyLocalFilters();
        });
        addRadioOption(menu, payerGroup, "BHYT", "BHYT".equals(payerFilter), () -> {
            payerFilter = "BHYT";
            applyLocalFilters();
        });
        addRadioOption(menu, payerGroup, "Viện phí / Yêu cầu", "VIEN_PHI".equals(payerFilter), () -> {
            payerFilter = "VIEN_PHI";
            applyLocalFilters();
        });

        menu.addSeparator();
        JMenuItem miReset = new JMenuItem("Đặt lại bộ lọc (Hôm nay)");
        miReset.addActionListener(e -> {
            statusFilter = "ALL";
            payerFilter = "ALL";
            searchKeyword = "";
            txtQuickSearch.setText("");
            searchBarPanel.setVisible(false);
            LocalDate today = LocalDate.now(BUSINESS_ZONE);
            if (!today.equals(fromDate) || !today.equals(toDate)) {
                setDateRange(today, today);
            } else {
                applyLocalFilters();
            }
        });
        menu.add(miReset);

        menu.show(invoker, 0, invoker.getHeight());
    }

    private static void addRadioOption(JPopupMenu menu, ButtonGroup group, String label,
                                       boolean selected, Runnable action) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, selected);
        item.addActionListener(e -> action.run());
        group.add(item);
        menu.add(item);
    }

    private void toggleQuickSearchBar() {
        boolean show = !searchBarPanel.isVisible();
        searchBarPanel.setVisible(show);
        if (show) {
            txtQuickSearch.requestFocusInWindow();
        } else if (!searchKeyword.isEmpty()) {
            txtQuickSearch.setText("");
        }
        revalidate();
    }

    private void onQuickSearchChanged() {
        searchKeyword = txtQuickSearch.getText() == null ? "" : txtQuickSearch.getText().trim().toLowerCase(Locale.ROOT);
        applyLocalFilters();
    }

    private void applyLocalFilters() {
        AdminPatientItem current = getSelectedPatient();
        String preferred = current != null ? current.invoiceCode : null;
        int target = rebuildVisibleList(preferred);
        if (target >= 0) {
            selectByIndex(target);
        } else if (!patientList.isEmpty()) {
            selectByIndex(0);
            if (onPatientSelected != null) {
                onPatientSelected.accept(patientList.get(0));
            }
        }
    }

    private boolean matchesLocalFilters(AdminPatientItem item) {
        if (!"ALL".equals(statusFilter)) {
            String st = item.visitStatus == null ? "" : item.visitStatus.trim().toUpperCase(Locale.ROOT);
            boolean ok = switch (statusFilter) {
                case "CHO_KHAM" -> st.isEmpty() || "CHO_KHAM".equals(st) || "TIEP_NHAN".equals(st);
                case "DANG_KHAM" -> "DANG_KHAM".equals(st) || "CHO_CLS".equals(st);
                case "DA_KHAM" -> "DA_KHAM".equals(st) || "KET_LUAN".equals(st)
                        || "HOAN_THANH".equals(st) || "HOAN_TAT".equals(st);
                default -> true;
            };
            if (!ok) return false;
        }
        if (!"ALL".equals(payerFilter)) {
            boolean isBhyt = item.requestType != null
                    && (item.requestType.contains("BHYT") || item.requestType.contains("hiểm"));
            if ("BHYT".equals(payerFilter) && !isBhyt) return false;
            if ("VIEN_PHI".equals(payerFilter) && isBhyt) return false;
        }
        if (!searchKeyword.isEmpty()) {
            String hay = (safeLower(item.name) + " " + safeLower(item.code) + " "
                    + safeLower(item.phone) + " " + safeLower(item.stt));
            if (!hay.contains(searchKeyword)) return false;
        }
        return true;
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    /** Cập nhật toàn bộ danh sách bệnh nhân và giữ nguyên lựa chọn theo mã phiếu nếu còn trong danh sách. */
    public int setPatients(List<AdminPatientItem> items, String preferredInvoiceCode) {
        allLoadedPatients.clear();
        if (items != null) {
            allLoadedPatients.addAll(items);
        }
        return rebuildVisibleList(preferredInvoiceCode);
    }

    private int rebuildVisibleList(String preferredInvoiceCode) {
        patientList.clear();
        for (AdminPatientItem item : allLoadedPatients) {
            if (matchesLocalFilters(item)) {
                patientList.add(item);
            }
        }
        queueModel.setRowCount(0);
        int target = -1;
        for (int i = 0; i < patientList.size(); i++) {
            AdminPatientItem item = patientList.get(i);
            queueModel.addRow(new Object[]{item, item, item.queueAge});
            if (preferredInvoiceCode != null && preferredInvoiceCode.equals(item.invoiceCode)) {
                target = i;
            }
        }
        updateSummaryLabels();
        return target;
    }

    public void updateSummaryLabels() {
        boolean hasLocalFilter = !"ALL".equals(statusFilter) || !"ALL".equals(payerFilter) || !searchKeyword.isEmpty();
        lblTuyChon.setText(hasLocalFilter ? "Tùy Chọn •" : "Tùy Chọn");
        lblTuyChon.setForeground(hasLocalFilter ? new Color(0, 95, 175) : new Color(30, 30, 30));
        lblTabPatients.setText("Tất cả bệnh nhân (" + patientList.size() + ")");
        long bhCount = patientList.stream()
                .filter(item -> item.requestType != null
                        && (item.requestType.contains("BHYT") || item.requestType.contains("hiểm")))
                .count();
        long vpCount = patientList.size() - bhCount;
        String fromText = fromDate.format(SHORT_DATE_FMT);
        String toText = toDate.format(SHORT_DATE_FMT);
        lblFilter.setText(fromText + " >> " + toText + " - BH(" + bhCount + ") VP(" + vpCount + ")");
    }

    public boolean selectByIndex(int index) {
        if (index >= 0 && index < patientList.size()) {
            queueTable.setRowSelectionInterval(index, index);
            return true;
        }
        return false;
    }

    public AdminPatientItem getSelectedPatient() {
        int row = queueTable.getSelectedRow();
        return row >= 0 && row < patientList.size() ? patientList.get(row) : null;
    }

    public List<AdminPatientItem> getPatientList() {
        return List.copyOf(patientList);
    }

    public JTable getQueueTable() {
        return queueTable;
    }

    public String getTabPatientsText() {
        return lblTabPatients.getText();
    }

    public String getFilterSummaryText() {
        return lblFilter.getText();
    }
}
