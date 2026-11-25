package vn.clinicmanagement.desktop.modules.hr.components;

import vn.clinicmanagement.application.hr.NhanSuReadPort;
import vn.clinicmanagement.application.hr.NhanSuReadPort.*;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.desktop.modules.print.hr.PhieuLuongInHopThoai;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static vn.clinicmanagement.desktop.modules.hr.components.NhanSuBangHoTro.*;

/**
 * Panel quản lý 2 cấp cho Tiền Lương: Danh sách Kỳ Lương -> Chi tiết Bảng Lương từng nhân viên,
 * xem chấm công trong kỳ, điều chỉnh dòng lương và in phiếu lương cá nhân / toàn kỳ.
 */
public final class NhanSuTienLuongPanel extends JPanel {
    private final NhanSuReadPort readPort;
    private final JComboBox<PayrollPeriodOption> cboPeriod;
    private final Consumer<String> onStatus;
    private final Consumer<String> onSectionTitle;
    private final Consumer<String> onTotalPayrollText;
    private final BiConsumer<Integer, Integer> onOpenAttendanceMatrix;

    private final DefaultTableModel payPeriodModel = readOnlyModel(new String[]{
            "STT", "Tên kỳ lương", "Tháng/Năm", "Từ ngày", "Đến ngày", "Số nhân viên", "Tổng quỹ lương", "Trạng thái"});
    private final DefaultTableModel payModel = readOnlyModel(new String[]{
            "STT", "Mã NV", "Nhân viên", "Chức vụ", "Lương cơ bản", "Hệ số", "Phụ cấp", "Thù lao khám", "Khấu trừ", "Thực lĩnh"});
    private final JTable tblPayrollPeriods = createStyledTable(payPeriodModel);
    private final JTable tblPayroll = createStyledTable(payModel);
    private final CardLayout payrollCards = new CardLayout();
    private final JLabel lblPayrollDetailHeader = new JLabel("CHI TIẾT BẢNG LƯƠNG");

    private List<PayrollPeriodSummary> allPayrollPeriods = List.of();
    private List<PayrollPeriodSummary> displayedPayrollPeriods = List.of();
    private List<PayrollRow> allPayroll = List.of();
    private List<PayrollRow> displayedPayroll = List.of();
    private boolean viewingPayrollDetail;
    private String lastKw = "";

    public NhanSuTienLuongPanel(
            NhanSuReadPort readPort,
            JComboBox<PayrollPeriodOption> cboPeriod,
            Consumer<String> onStatus,
            Consumer<String> onSectionTitle,
            Consumer<String> onTotalPayrollText,
            BiConsumer<Integer, Integer> onOpenAttendanceMatrix) {
        this.readPort = Objects.requireNonNull(readPort, "readPort");
        this.cboPeriod = Objects.requireNonNull(cboPeriod, "cboPeriod");
        this.onStatus = onStatus;
        this.onSectionTitle = onSectionTitle;
        this.onTotalPayrollText = onTotalPayrollText;
        this.onOpenAttendanceMatrix = onOpenAttendanceMatrix;
        setLayout(payrollCards);
        buildViews();
    }

    private void buildViews() {
        JPanel periodsPane = new JPanel(new BorderLayout());
        periodsPane.setBackground(Color.WHITE);
        JLabel hint = new JLabel("Chọn kỳ lương và nhấp đúp để mở bảng lương chi tiết từng nhân viên");
        hint.setFont(UngDungPhongChu.SMALL);
        hint.setForeground(new Color(65, 80, 95));
        hint.setBackground(new Color(245, 248, 252));
        hint.setOpaque(true);
        hint.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 228, 238)),
                new EmptyBorder(4, 8, 4, 8)));
        periodsPane.add(hint, BorderLayout.NORTH);
        periodsPane.add(scrollOf(tblPayrollPeriods), BorderLayout.CENTER);

        JPopupMenu periodMenu = new JPopupMenu();
        addMenuItem(periodMenu, "Xem Chi Tiết Bảng Lương", this::openSelectedPayrollPeriodDetail);
        addMenuItem(periodMenu, "In Bảng Lương Toàn Kỳ", this::showPeriodPayrollPrintDialog);
        addMenuItem(periodMenu, "Chốt Kỳ Lương", this::lockPayroll);
        addMenuItem(periodMenu, "Mở Khóa Kỳ Lương", this::reopenPayroll);
        tblPayrollPeriods.setComponentPopupMenu(periodMenu);
        tblPayrollPeriods.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    openSelectedPayrollPeriodDetail();
                }
            }
        });

        JPanel detailPane = new JPanel(new BorderLayout());
        detailPane.setBackground(Color.WHITE);
        JPanel detailBar = new JPanel(new BorderLayout());
        detailBar.setBackground(new Color(245, 248, 252));
        detailBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 228, 238)));

        JPanel detailLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        detailLeft.setOpaque(false);
        JButton btnBack = new JButton("← Quay lại DS Kỳ Lương");
        btnBack.setFont(UngDungPhongChu.BODY_BOLD);
        btnBack.setFocusPainted(false);
        btnBack.addActionListener(e -> showPayrollPeriodsOverview(lastKw));
        lblPayrollDetailHeader.setFont(UngDungPhongChu.BODY_BOLD);
        lblPayrollDetailHeader.setForeground(new Color(10, 86, 179));
        detailLeft.add(btnBack);
        detailLeft.add(lblPayrollDetailHeader);
        detailBar.add(detailLeft, BorderLayout.WEST);

        JPanel detailRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        detailRight.setOpaque(false);
        JButton btnViewAttDetail = new JButton("Xem Chi Tiết Chấm Công");
        btnViewAttDetail.setFont(UngDungPhongChu.BODY_BOLD);
        btnViewAttDetail.setFocusPainted(false);
        btnViewAttDetail.addActionListener(e -> showPayrollEmployeeAttendanceDialog());
        JButton btnOpenAttMatrix = new JButton("Ma Trận Chấm Công");
        btnOpenAttMatrix.setFont(UngDungPhongChu.BODY);
        btnOpenAttMatrix.setFocusPainted(false);
        btnOpenAttMatrix.addActionListener(e -> {
            PayrollPeriodOption p = resolveTargetPeriod();
            if (p != null && onOpenAttendanceMatrix != null) {
                onOpenAttendanceMatrix.accept(p.month(), p.year());
            }
        });
        JButton btnPrintPeriod = new JButton("In Bảng Lương Toàn Kỳ");
        btnPrintPeriod.setFont(UngDungPhongChu.BODY_BOLD);
        btnPrintPeriod.setForeground(new Color(18, 92, 166));
        btnPrintPeriod.setFocusPainted(false);
        btnPrintPeriod.addActionListener(e -> showPeriodPayrollPrintDialog());
        detailRight.add(btnViewAttDetail);
        detailRight.add(btnOpenAttMatrix);
        detailRight.add(btnPrintPeriod);
        detailBar.add(detailRight, BorderLayout.EAST);

        JPopupMenu payMenu = new JPopupMenu();
        addMenuItem(payMenu, "Xem Chi Tiết Chấm Công Kỳ Này", this::showPayrollEmployeeAttendanceDialog);
        addMenuItem(payMenu, "Điều Chỉnh Thù Lao / Khấu Trừ / Phụ Cấp", this::openAdjustPayrollLineDialog);
        addMenuItem(payMenu, "Xem / In Phiếu Lương Cá Nhân", this::showPayslipDialog);
        tblPayroll.setComponentPopupMenu(payMenu);
        tblPayroll.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    openAdjustPayrollLineDialog();
                }
            }
        });

        detailPane.add(detailBar, BorderLayout.NORTH);
        detailPane.add(scrollOf(tblPayroll), BorderLayout.CENTER);

        add(periodsPane, "PERIODS");
        add(detailPane, "DETAIL");
        payrollCards.show(this, "PERIODS");
    }

    public JTable getTblPayrollPeriods() { return tblPayrollPeriods; }
    public JTable getTblPayroll() { return tblPayroll; }
    public JTable getActiveTable() { return viewingPayrollDetail ? tblPayroll : tblPayrollPeriods; }

    public void showPayrollPeriodsOverview(String kw) {
        this.lastKw = kw;
        viewingPayrollDetail = false;
        payrollCards.show(this, "PERIODS");
        if (onSectionTitle != null) onSectionTitle.accept("DANH SÁCH KỲ LƯƠNG");
        renderPayrollPeriods(kw);
    }

    public void applyFilter(String kw) {
        this.lastKw = kw;
        if (viewingPayrollDetail) renderPayroll(kw);
        else renderPayrollPeriods(kw);
    }

    public void openSelectedPayrollPeriodDetail() {
        int viewRow = tblPayrollPeriods.getSelectedRow();
        if (viewRow < 0) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một kỳ lương.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
            return;
        }
        int modelRow = tblPayrollPeriods.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= displayedPayrollPeriods.size()) return;
        PayrollPeriodSummary summary = displayedPayrollPeriods.get(modelRow);
        for (int i = 0; i < cboPeriod.getItemCount(); i++) {
            PayrollPeriodOption opt = cboPeriod.getItemAt(i);
            if (opt.id() == summary.id()) {
                cboPeriod.setSelectedIndex(i);
                break;
            }
        }
        viewingPayrollDetail = true;
        lblPayrollDetailHeader.setText(summary.name() + (summary.closed() ? " (Đã chốt)" : " (Đang mở)")
                + "  •  Tổng quỹ: " + MONEY_FMT.format(summary.totalNetSalary()) + "đ");
        if (onSectionTitle != null) onSectionTitle.accept("CHI TIẾT BẢNG LƯƠNG - " + summary.name().toUpperCase(Locale.ROOT));
        payrollCards.show(this, "DETAIL");
        loadPayrollLines(lastKw);
    }

    public void loadPayrollPeriods(String kw) {
        this.lastKw = kw;
        new SwingWorker<List<PayrollPeriodSummary>, Void>() {
            @Override protected List<PayrollPeriodSummary> doInBackground() { return readPort.listPayrollPeriodSummaries(); }
            @Override protected void done() {
                try {
                    allPayrollPeriods = get();
                    Integer prevSelectedId = cboPeriod.getSelectedItem() instanceof PayrollPeriodOption p ? p.id() : null;
                    cboPeriod.removeAllItems();
                    int selIdx = 0;
                    for (int i = 0; i < allPayrollPeriods.size(); i++) {
                        PayrollPeriodSummary s = allPayrollPeriods.get(i);
                        cboPeriod.addItem(new PayrollPeriodOption(s.id(), s.name(), s.month(), s.year(), s.closed()));
                        if (prevSelectedId != null && prevSelectedId == s.id()) selIdx = i;
                    }
                    if (cboPeriod.getItemCount() > 0) cboPeriod.setSelectedIndex(selIdx);
                    renderPayrollPeriods(lastKw);
                    loadPayrollLines(lastKw);
                } catch (Exception ex) {
                    if (onStatus != null) onStatus.accept("Không thể tải kỳ lương: " + ex.getMessage());
                }
            }
        }.execute();
    }

    public void loadPayrollLines(String kw) {
        this.lastKw = kw;
        if (!(cboPeriod.getSelectedItem() instanceof PayrollPeriodOption period)) {
            allPayroll = List.of();
            payModel.setRowCount(0);
            if (onTotalPayrollText != null) onTotalPayrollText.accept("0đ");
            return;
        }
        new SwingWorker<List<PayrollRow>, Void>() {
            @Override protected List<PayrollRow> doInBackground() { return readPort.listPayroll(period.id()); }
            @Override protected void done() {
                try {
                    allPayroll = get();
                    BigDecimal sum = allPayroll.stream().map(PayrollRow::netSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
                    if (onTotalPayrollText != null) onTotalPayrollText.accept(MONEY_FMT.format(sum) + "đ");
                    renderPayroll(lastKw);
                } catch (Exception ex) {
                    if (onStatus != null) onStatus.accept("Không thể tải chi tiết lương: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void renderPayrollPeriods(String kw) {
        payPeriodModel.setRowCount(0);
        List<PayrollPeriodSummary> filtered = new ArrayList<>();
        int stt = 1;
        for (PayrollPeriodSummary s : allPayrollPeriods) {
            String hay = (s.name() + " " + s.month() + "/" + s.year() + " " + (s.closed() ? "đã chốt" : "đang mở")).toLowerCase(Locale.ROOT);
            if (!kw.isBlank() && !hay.contains(kw)) continue;
            filtered.add(s);
            payPeriodModel.addRow(new Object[]{
                    stt++,
                    s.name(),
                    String.format("%02d/%d", s.month(), s.year()),
                    s.fromDate() != null ? s.fromDate().format(DATE_FMT) : "",
                    s.toDate() != null ? s.toDate().format(DATE_FMT) : "",
                    s.employeeCount(),
                    MONEY_FMT.format(s.totalNetSalary()) + "đ",
                    s.closed() ? "Đã chốt" : "Đang mở"
            });
        }
        displayedPayrollPeriods = List.copyOf(filtered);
    }

    private void renderPayroll(String kw) {
        payModel.setRowCount(0);
        List<PayrollRow> filtered = new ArrayList<>();
        int stt = 1;
        for (PayrollRow r : allPayroll) {
            String hay = (r.maNhanVien() + " " + r.fullName() + " " + r.positionName()).toLowerCase(Locale.ROOT);
            if (!kw.isBlank() && !hay.contains(kw)) continue;
            filtered.add(r);
            payModel.addRow(new Object[]{
                    stt++, r.maNhanVien(), r.fullName(), r.positionName(),
                    MONEY_FMT.format(r.baseSalary()), r.factor(),
                    MONEY_FMT.format(r.allowance()), MONEY_FMT.format(r.examCommission()),
                    MONEY_FMT.format(r.deduction()), MONEY_FMT.format(r.netSalary())
            });
        }
        displayedPayroll = List.copyOf(filtered);
    }

    public void calculatePayroll(int m, int y) {
        var auth = KhamBenhPhienNguCanh.current();
        if (auth == null) return;
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                try { readPort.calculateOrCreatePayrollPeriod(m, y, auth.employeeId()); return null; }
                catch (Exception ex) { return ex.getMessage(); }
            }
            @Override protected void done() {
                try {
                    String err = get();
                    if (err != null && !GraphicsEnvironment.isHeadless()) JOptionPane.showMessageDialog(NhanSuTienLuongPanel.this, err, "Tính lương", JOptionPane.ERROR_MESSAGE);
                    else { loadPayrollPeriods(lastKw); if (onStatus != null) onStatus.accept("Đã tính lương tháng " + m + "/" + y + "."); }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void lockPayroll() {
        var auth = KhamBenhPhienNguCanh.current();
        PayrollPeriodOption period = resolveTargetPeriod();
        if (auth == null || period == null || period.closed()) return;
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                try { readPort.lockPayrollPeriod(period.id(), auth.employeeId()); return null; }
                catch (Exception ex) { return ex.getMessage(); }
            }
            @Override protected void done() {
                try {
                    String err = get();
                    if (err != null && !GraphicsEnvironment.isHeadless()) JOptionPane.showMessageDialog(NhanSuTienLuongPanel.this, err, "Khóa kỳ lương", JOptionPane.ERROR_MESSAGE);
                    else { loadPayrollPeriods(lastKw); if (onStatus != null) onStatus.accept("Đã chốt kỳ lương " + period.name()); }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void reopenPayroll() {
        var auth = KhamBenhPhienNguCanh.current();
        PayrollPeriodOption period = resolveTargetPeriod();
        if (auth == null || period == null || !period.closed()) return;
        if (!GraphicsEnvironment.isHeadless() && JOptionPane.showConfirmDialog(this, "Mở khóa kỳ lương " + period.name() + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                try { readPort.reopenPayrollPeriod(period.id(), auth.employeeId()); return null; }
                catch (Exception ex) { return ex.getMessage(); }
            }
            @Override protected void done() {
                try {
                    String err = get();
                    if (err != null && !GraphicsEnvironment.isHeadless()) JOptionPane.showMessageDialog(NhanSuTienLuongPanel.this, err, "Mở khóa kỳ lương", JOptionPane.ERROR_MESSAGE);
                    else { loadPayrollPeriods(lastKw); if (onStatus != null) onStatus.accept("Đã mở khóa kỳ lương " + period.name()); }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void openAdjustPayrollLineDialog() {
        if (GraphicsEnvironment.isHeadless()) return;
        var auth = KhamBenhPhienNguCanh.current();
        int viewRow = tblPayroll.getSelectedRow();
        if (auth == null || viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một dòng lương.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = tblPayroll.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= displayedPayroll.size()) return;
        PayrollRow row = displayedPayroll.get(modelRow);

        JTextField txtAllow = new JTextField(row.allowance().toPlainString(), 14);
        JTextField txtComm = new JTextField(row.examCommission().toPlainString(), 14);
        JTextField txtDed = new JTextField(row.deduction().toPlainString(), 14);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.add(new JLabel("Nhân viên:")); form.add(new JLabel(row.maNhanVien() + " - " + row.fullName()));
        form.add(new JLabel("Lương cơ bản x Hệ số:")); form.add(new JLabel(MONEY_FMT.format(row.baseSalary()) + " x " + row.factor()));
        form.add(new JLabel("Phụ cấp (VNĐ):")); form.add(txtAllow);
        form.add(new JLabel("Thù lao khám (VNĐ):")); form.add(txtComm);
        form.add(new JLabel("Khấu trừ (VNĐ):")); form.add(txtDed);

        if (JOptionPane.showConfirmDialog(this, form, "Điều Chỉnh Dòng Lương", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            BigDecimal allow = new BigDecimal(txtAllow.getText().trim().replace(",", ""));
            BigDecimal comm = new BigDecimal(txtComm.getText().trim().replace(",", ""));
            BigDecimal ded = new BigDecimal(txtDed.getText().trim().replace(",", ""));
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() {
                    try { readPort.updatePayrollLine(row.id(), allow, comm, ded, auth.employeeId()); return null; }
                    catch (Exception ex) { return ex.getMessage(); }
                }
                @Override protected void done() {
                    try {
                        String err = get();
                        if (err != null) JOptionPane.showMessageDialog(NhanSuTienLuongPanel.this, err, "Lỗi", JOptionPane.ERROR_MESSAGE);
                        else { loadPayrollLines(lastKw); if (onStatus != null) onStatus.accept("Đã cập nhật dòng lương của " + row.fullName()); }
                    } catch (Exception ignored) {}
                }
            }.execute();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Số tiền không hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showPayrollEmployeeAttendanceDialog() {
        if (GraphicsEnvironment.isHeadless()) return;
        var auth = KhamBenhPhienNguCanh.current();
        PayrollPeriodOption period = resolveTargetPeriod();
        if (auth == null || period == null) return;

        PayrollRow selectedEmp = null;
        int viewRow = tblPayroll.getSelectedRow();
        if (viewRow >= 0) {
            int modelRow = tblPayroll.convertRowIndexToModel(viewRow);
            if (modelRow >= 0 && modelRow < displayedPayroll.size()) selectedEmp = displayedPayroll.get(modelRow);
        }
        final PayrollRow targetEmp = selectedEmp;
        String title = targetEmp != null
                ? "Chi Tiết Chấm Công - " + targetEmp.fullName() + " (" + period.month() + "/" + period.year() + ")"
                : "Chi Tiết Chấm Công Toàn Bộ Nhân Viên (" + period.month() + "/" + period.year() + ")";

        DefaultTableModel m = readOnlyModel(new String[]{
                "STT", "Mã NV", "Nhân viên", "Ngày", "Ca trực", "Giờ vào", "Giờ ra", "Số giờ làm", "Đi muộn", "Trạng thái"});
        JTable tbl = createStyledTable(m);
        JLabel lblSummary = new JLabel("Đang tải dữ liệu chấm công...");
        lblSummary.setFont(UngDungPhongChu.BODY_BOLD);
        lblSummary.setForeground(new Color(10, 86, 179));
        lblSummary.setBorder(new EmptyBorder(6, 10, 6, 10));

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.add(lblSummary, BorderLayout.NORTH);
        dialog.add(scrollOf(tbl), BorderLayout.CENTER);
        JButton btnClose = new JButton("Đóng");
        btnClose.addActionListener(e -> dialog.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        south.add(btnClose);
        dialog.add(south, BorderLayout.SOUTH);
        dialog.setSize(900, 480);
        dialog.setLocationRelativeTo(this);

        new SwingWorker<List<AttendanceRow>, Void>() {
            @Override protected List<AttendanceRow> doInBackground() {
                return readPort.listAttendance(auth.facilityId(), period.month(), period.year());
            }
            @Override protected void done() {
                try {
                    List<AttendanceRow> rows = get();
                    int stt = 1, onTime = 0, late = 0, early = 0;
                    BigDecimal totalHours = BigDecimal.ZERO, totalLate = BigDecimal.ZERO;
                    for (AttendanceRow r : rows) {
                        if (targetEmp != null && !targetEmp.maNhanVien().equals(r.maNhanVien())) continue;
                        m.addRow(new Object[]{
                                stt++, r.maNhanVien(), r.fullName(),
                                r.workDate() != null ? r.workDate().format(DATE_FMT) : "",
                                r.shiftName(),
                                r.checkIn() != null ? r.checkIn().format(DT_FMT) : "",
                                r.checkOut() != null ? r.checkOut().format(DT_FMT) : "",
                                r.hoursWorked(), r.lateHours(), r.status()
                        });
                        if ("DUNG_GIO".equals(r.status())) onTime++;
                        if ("DI_MUON".equals(r.status()) || "DI_MUON_VE_SOM".equals(r.status())) late++;
                        if ("VE_SOM".equals(r.status()) || "DI_MUON_VE_SOM".equals(r.status())) early++;
                        if (r.hoursWorked() != null) totalHours = totalHours.add(r.hoursWorked());
                        if (r.lateHours() != null) totalLate = totalLate.add(r.lateHours());
                    }
                    lblSummary.setText("Tổng ngày công: " + (stt - 1)
                            + "  |  Đúng giờ: " + onTime
                            + "  |  Đi muộn: " + late
                            + "  |  Về sớm: " + early
                            + "  |  Tổng giờ làm: " + totalHours + "h"
                            + "  |  Tổng giờ muộn: " + totalLate + "h");
                } catch (Exception ex) {
                    lblSummary.setText("Lỗi tải chấm công: " + ex.getMessage());
                }
            }
        }.execute();

        dialog.setVisible(true);
    }

    public void showPeriodPayrollPrintDialog() {
        if (GraphicsEnvironment.isHeadless()) return;
        PayrollPeriodOption period = resolveTargetPeriod();
        if (period == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một kỳ lương để in.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        new SwingWorker<List<PayrollRow>, Void>() {
            @Override protected List<PayrollRow> doInBackground() { return readPort.listPayroll(period.id()); }
            @Override protected void done() {
                try {
                    List<PayrollRow> rows = get();
                    if (rows.isEmpty()) {
                        JOptionPane.showMessageDialog(NhanSuTienLuongPanel.this, "Kỳ lương này chưa có dữ liệu nhân viên.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    PhieuLuongInHopThoai.showPeriodSummaryDialog(
                            SwingUtilities.getWindowAncestor(NhanSuTienLuongPanel.this),
                            period.toString(),
                            rows);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(NhanSuTienLuongPanel.this, "Không tải được bảng lương: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public void showPayslipDialog() {
        if (GraphicsEnvironment.isHeadless()) return;
        int viewRow = tblPayroll.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một nhân viên trong bảng lương.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = tblPayroll.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= displayedPayroll.size()) return;
        PayrollRow r = displayedPayroll.get(modelRow);
        PayrollPeriodOption period = resolveTargetPeriod();
        String periodName = period != null ? period.toString() : "";
        var auth = KhamBenhPhienNguCanh.current();
        new SwingWorker<BigDecimal[], Void>() {
            @Override protected BigDecimal[] doInBackground() {
                int days = 0;
                BigDecimal hours = BigDecimal.ZERO;
                if (auth != null && period != null) {
                    for (AttendanceRow a : readPort.listAttendance(auth.facilityId(), period.month(), period.year())) {
                        if (r.maNhanVien().equals(a.maNhanVien())) {
                            days++;
                            if (a.hoursWorked() != null) hours = hours.add(a.hoursWorked());
                        }
                    }
                }
                return new BigDecimal[]{BigDecimal.valueOf(days), hours};
            }
            @Override protected void done() {
                int days = 0;
                BigDecimal hours = BigDecimal.ZERO;
                try {
                    BigDecimal[] res = get();
                    days = res[0].intValue();
                    hours = res[1];
                } catch (Exception ignored) {}
                PhieuLuongInHopThoai.showDialog(SwingUtilities.getWindowAncestor(NhanSuTienLuongPanel.this), r, periodName, days, hours);
            }
        }.execute();
    }

    private PayrollPeriodOption resolveTargetPeriod() {
        if (!viewingPayrollDetail) {
            int viewRow = tblPayrollPeriods.getSelectedRow();
            if (viewRow >= 0) {
                int modelRow = tblPayrollPeriods.convertRowIndexToModel(viewRow);
                if (modelRow >= 0 && modelRow < displayedPayrollPeriods.size()) {
                    PayrollPeriodSummary s = displayedPayrollPeriods.get(modelRow);
                    return new PayrollPeriodOption(s.id(), s.name(), s.month(), s.year(), s.closed());
                }
            }
        }
        return cboPeriod.getSelectedItem() instanceof PayrollPeriodOption p ? p : null;
    }
}
