package vn.clinicmanagement.desktop.modules.paraclinical;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.cls.XetNghiemKetQuaMatHangDauVao;
import vn.clinicmanagement.application.cls.record.RecordDichVuKetQuaCommand;
import vn.clinicmanagement.application.cls.record.RecordDichVuKetQuaBienNhan;
import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.desktop.modules.paraclinical.components.XetNghiemKetQuaBoCuc;
import vn.clinicmanagement.desktop.modules.paraclinical.model.PhieuXetNghiem;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiTietKetQuaXetNghiem;
import vn.clinicmanagement.domain.service.ThietLapThucHienDichVu;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/** Separate laboratory result workspace; specimen collection has its own view key and layout. */
public final class XetNghiemKetQuaManHinh extends CoSoManHinhManHinh {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private record ResultRowBinding(PhieuXetNghiem.DongXetNghiem test, ChiTietKetQuaXetNghiem detail) {}
    private final CanLamSangGhepNoiGoc.Services services;
    private final Map<Integer,UUID> consumptionOperationKeys=new HashMap<>();
    private final XetNghiemKetQuaBoCuc ui;
    private final List<PhieuXetNghiem> orders = new ArrayList<>();
    private final List<PhieuXetNghiem> filtered = new ArrayList<>();
    private final List<ResultRowBinding> rowBindings = new ArrayList<>();
    private LocalDate from = LocalDate.now(ZONE), to = from;
    private PhieuXetNghiem selected;
    private boolean loading, changingFilters, expanded;
    private long selectionVersion;
    private SwingWorker<List<PhieuXetNghiem>, Void> loader;
    private record ResultDetails(Map<Integer, List<ChiTietKetQuaXetNghiem>> rows,
            Map<Integer, ThietLapThucHienDichVu> settings) {}
    private SwingWorker<ResultDetails, Void> detailLoader;
    private Map<Integer, ThietLapThucHienDichVu> executionSettings = Map.of();
    private boolean detailsLoaded;

    public XetNghiemKetQuaManHinh() {
        this(GraphicsEnvironment.isHeadless() || System.getProperty("surefire.test.class.path") != null
                ? CanLamSangGhepNoiGoc.createDefault() : CanLamSangGhepNoiGoc.createProduction());
    }
    public XetNghiemKetQuaManHinh(CanLamSangGhepNoiGoc.Services services) {
        super("lab", "Xét Nghiệm", "");
        this.services = Objects.requireNonNull(services);
        ui = new XetNghiemKetQuaBoCuc(this);
        installRenderers(); bindActions();
    }
    @Override public void doLayout() {
        if (ui != null) ui.resize(this);
        super.doLayout();
        if (ui != null && expanded) ui.split.setDividerLocation(0);
    }

    private void bindActions() {
        ui.reload.addActionListener(e -> refreshData());
        ui.dateType.addActionListener(e -> refreshData());
        ui.status.addActionListener(e -> { if (!changingFilters) applyFilters(); });
        ui.outpatient.addActionListener(e -> applyFilters()); ui.inpatient.addActionListener(e -> applyFilters());
        ui.room.addActionListener(e -> { if (!changingFilters) applyFilters(); });
        ui.clear.addActionListener(e -> ui.keyword.setText(""));
        ui.keyword.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });
        ui.search.addActionListener(e -> { ui.keyword.requestFocusInWindow(); ui.keyword.selectAll(); });
        ui.waiting.addActionListener(e -> ui.status.setSelectedIndex(1));
        ui.queue.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = ui.queue.getSelectedRow(); selectOrder(row < 0 || row >= filtered.size() ? null : filtered.get(row));
            }
        });
        ui.save.setToolTipText("Lưu kết quả và kết luận đang nhập trên bảng vào CSDL");
        ui.save.addActionListener(e -> saveDraftOrResults(false));
        ui.execute.addActionListener(e -> startExecution());
        ui.runFormula.addActionListener(e -> showResultConfiguration());
        ui.cancelExecute.addActionListener(e -> cancelExecutionAction());
        ui.approveResult.addActionListener(e -> approveResultsAction());
        ui.unapproveResult.addActionListener(e -> unapproveOrRecallAction());
        ui.releaseResult.addActionListener(e -> releaseResultsAction());
        ui.printResultFooter.addActionListener(e -> printResults());
        ui.results.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (ui.isExecutingMode()) {
                        openEntry();
                    } else if (selected != null && selected.statusEnum().isSampleCollectedWaitingExec()) {
                        startExecution();
                    }
                }
            }
        });
        ui.expand.addActionListener(e -> {
            expanded = !expanded; ui.split.getLeftComponent().setVisible(!expanded);
            ui.expand.setText(expanded ? "Thu Nhỏ" : "Phóng To"); revalidate();
        });
        ui.print.addActionListener(e -> printResults());
        ui.history.addActionListener(e -> showHistory(false)); ui.chart.addActionListener(e -> showHistory(true));
        JPopupMenu menu = new JPopupMenu();
        JMenuItem today = new JMenuItem("Phiếu hôm nay"); today.addActionListener(e -> setDateRange(LocalDate.now(ZONE), LocalDate.now(ZONE)));
        JMenuItem week = new JMenuItem("Phiếu 7 ngày gần đây"); week.addActionListener(e -> setDateRange(LocalDate.now(ZONE).minusDays(6), LocalDate.now(ZONE)));
        JMenuItem date = new JMenuItem("Chọn khoảng ngày..."); date.addActionListener(e -> chooseDates());
        menu.add(today); menu.add(week); menu.add(date);
        ui.menu.addActionListener(e -> menu.show(ui.menu, 0, ui.menu.getHeight()));
        shortcut("F3", "print", this::printResults); shortcut("F5", "reload", this::refreshData);
        shortcut("control F", "find", () -> ui.keyword.requestFocusInWindow());
    }
    private void shortcut(String key, String name, Runnable task) {
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key), name);
        getActionMap().put(name, new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { task.run(); } });
    }
    public void setDateRange(LocalDate from, LocalDate to) {
        this.from = Objects.requireNonNull(from); this.to = Objects.requireNonNull(to);
        if (from.isAfter(to)) { this.from = to; this.to = from; }
        refreshData();
    }
    private void chooseDates() {
        JTextField start = new JTextField(from.toString()), end = new JTextField(to.toString());
        JPanel panel = new JPanel(new GridLayout(2, 2, 6, 6));
        panel.add(new JLabel("Từ ngày (yyyy-MM-dd)")); panel.add(start); panel.add(new JLabel("Đến ngày (yyyy-MM-dd)")); panel.add(end);
        if (JOptionPane.showConfirmDialog(this, panel, "Khoảng ngày xét nghiệm", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try { setDateRange(LocalDate.parse(start.getText().trim()), LocalDate.parse(end.getText().trim())); }
            catch (DateTimeException ex) { ui.feedback.setText("Ngày không hợp lệ. Nhập theo định dạng yyyy-MM-dd."); }
        }
    }
    @Override public void refreshData() {
        if (loading) return;
        var actor = KhamBenhPhienNguCanh.current();
        if (actor == null || !actor.isValid()) { orders.clear(); applyFilters(); return; }
        loading = true; ui.feedback.setText("Đang tải phiếu xét nghiệm...");
        LocalDate start = from, end = to; int dateType = ui.dateType.getSelectedIndex();
        loader = new SwingWorker<>() {
            @Override protected List<PhieuXetNghiem> doInBackground() { return XetNghiemDuLieuNguon.loadCollectedForLab(actor.facilityId(), start, end, dateType); }
            @Override protected void done() {
                loading = false; if (isCancelled()) return;
                try {
                    var current = KhamBenhPhienNguCanh.current();
                    if (current == null || !current.equals(actor)) { orders.clear(); applyFilters(); return; }
                    orders.clear(); orders.addAll(get()); updateRooms(); applyFilters(); ui.feedback.setText(" ");
                } catch (Exception ex) { orders.clear(); applyFilters(); ui.feedback.setText("Không tải được phiếu xét nghiệm. Nhấn F5 để thử lại."); }
            }
        }; loader.execute();
    }
    private void updateRooms() {
        changingFilters = true;
        Object room = ui.room.getSelectedItem();
        ui.room.removeAllItems(); ui.room.addItem("(Tất cả phòng xét nghiệm)");
        orders.stream().map(PhieuXetNghiem::roomName).filter(s -> !s.isBlank()).distinct().sorted().forEach(ui.room::addItem);
        if (room != null) for (int i = 0; i < ui.room.getItemCount(); i++) if (room.equals(ui.room.getItemAt(i))) ui.room.setSelectedIndex(i);
        changingFilters = false;
    }
    private void applyFilters() {
        Integer id = selected == null ? null : selected.orderId();
        String keyword = ui.keyword.getText().strip().toLowerCase(Locale.ROOT);
        filtered.clear();
        int collectedTotal = 0;
        for (var item : orders) {
            TrangThaiChiDinhDichVu st = item.statusEnum();
            if (!st.isVisibleInLabWorkspace()) continue;
            collectedTotal++;
            if (item.isInpatient() ? !ui.inpatient.isSelected() : !ui.outpatient.isSelected()) continue;
            int state = ui.status.getSelectedIndex();
            if (state == 1 && !st.isSampleCollectedWaitingExec()) continue;
            if (state == 2 && !st.isExecuting()) continue;
            if (state == 3 && !st.isApproved()) continue;
            if (state == 4 && !st.isResultReleased()) continue;
            if (state == 5 && !st.isCancelled()) continue;
            if (ui.room.getSelectedIndex() > 0 && !item.roomName().equals(ui.room.getSelectedItem())) continue;
            if (!(item.patientName()+" "+item.patientCode()+" "+item.barcode()+" "+item.orderCode()).toLowerCase(Locale.ROOT).contains(keyword)) continue;
            filtered.add(item);
        }
        changingFilters = true;
        int state = Math.max(0, Math.min(5, ui.status.getSelectedIndex()));
        ui.status.setModel(new DefaultComboBoxModel<>(new String[]{
                "(Tất cả phiếu) - " + collectedTotal,
                TrangThaiChiDinhDichVu.DA_LAY_MAU.getDescription(),
                TrangThaiChiDinhDichVu.DANG_THUC_HIEN.getDescription(),
                TrangThaiChiDinhDichVu.DA_DUYET_KET_QUA.getDescription(),
                TrangThaiChiDinhDichVu.DA_CO_KET_QUA.getDescription(),
                TrangThaiChiDinhDichVu.DA_HUY.getDescription()
        }));
        ui.status.setSelectedIndex(state); changingFilters = false;
        ui.queueModel.setRowCount(0); int selectedIndex = -1;
        for (int i=0; i<filtered.size(); i++) {
            var item = filtered.get(i); ui.queueModel.addRow(new Object[]{item, item.barcode(), item, item.queueNumber() > 0 ? item.queueNumber() : (i + 1)});
            if (Objects.equals(id, item.orderId())) selectedIndex = i;
        }
        if (!filtered.isEmpty()) ui.queue.setRowSelectionInterval(Math.max(0, selectedIndex), Math.max(0, selectedIndex)); else selectOrder(null);
    }
    private void selectOrder(PhieuXetNghiem item) {
        selected = item; long version = ++selectionVersion;
        detailsLoaded = false;
        executionSettings = Map.of();
        if (detailLoader != null) detailLoader.cancel(true);
        for (JLabel label : List.of(ui.patient, ui.insurance, ui.address, ui.insuranceType, ui.insuranceRate, ui.orderCode,
                ui.executionCode, ui.barcode, ui.diagnosis, ui.ordered, ui.collected, ui.performed)) label.setText(" ");
        ui.conclusion.setText("");
        ui.resultModel.setRowCount(0);
        rowBindings.clear();
        TrangThaiChiDinhDichVu st = item == null ? null : item.statusEnum();
        boolean active = st != null && !st.isCancelled();
        boolean isWaitingExec = active && st.isSampleCollectedWaitingExec();
        boolean isExecuting = active && st.isExecuting();
        boolean isApproved = active && st.isApproved();
        boolean isReleased = active && st.isResultReleased();
        ui.setWorkflowStage(st);
        ui.results.setEnabled(false);
        ui.save.setEnabled(false);
        ui.execute.setEnabled(isWaitingExec);
        ui.runFormula.setEnabled(false);
        ui.cancelExecute.setEnabled(isExecuting);
        ui.approveResult.setEnabled(false);
        ui.unapproveResult.setEnabled(isApproved || isReleased);
        ui.releaseResult.setEnabled(isApproved);
        ui.printResultFooter.setEnabled(false);
        ui.print.setEnabled(false);
        ui.history.setEnabled(item != null);
        ui.chart.setEnabled(item != null);
        if (item == null) return;
        String age = "";
        if (item.birthDate() != null) {
            long months = java.time.temporal.ChronoUnit.MONTHS.between(item.birthDate(), LocalDate.now(ZONE));
            age = " | " + item.birthDate().getYear() + " (" + (months < 72 ? months + " tháng tuổi" : months/12 + " tuổi") + ")";
        }
        String gender = "M".equals(item.gender()) ? "Nam" : "F".equals(item.gender()) ? "Nữ" : item.gender();
        ui.patient.setText(item.patientName().toUpperCase(Locale.ROOT)+" | "+item.patientCode()+age+" | "+gender);
        ui.insurance.setText("Mã VP : " + item.visitId() + " | " + (item.bhytNumber().isBlank() ? "Viện phí" : item.bhytNumber())
                + (item.bhytExpiry() == null ? "" : " | Còn " + java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(ZONE), item.bhytExpiry()) + " ngày"));
        ui.address.setText(item.address()); ui.insuranceType.setText(item.isBhyt() ? "BHYT" : "VP");
        ui.insuranceRate.setText(item.isBhyt() ? (item.bhytRate() > 0 ? item.bhytRate() : 80) + "%" : "");
        ui.orderCode.setText(item.orderCode());
        boolean hasExecution = isExecuting || isApproved || isReleased;
        ui.executionCode.setText(hasExecution ? formatExecutionCode(item) : " ");
        ui.barcode.setText(item.barcode()); ui.diagnosis.setText(item.diagnosis().isBlank() ? "—" : item.diagnosis());
        String ordBy = item.orderedByName().isBlank() ? "Quản Trị Hệ Thống" : item.orderedByName();
        String ordTime = item.orderedAt() == null ? "" : TIME.format(item.orderedAt());
        ui.ordered.setText("<html>" + html(ordBy)
                + (ordTime.isEmpty() ? "" : " CĐ lúc <b>" + html(ordTime) + " (YL: " + html(ordTime) + ")</b>")
                + " tại " + html(item.departmentName()) + "</html>");
        String collector = item.collectorName() != null && !item.collectorName().isBlank()
                ? item.collectorName() : (item.performerName().isBlank() ? ordBy : item.performerName());
        String collectPlace = item.collectPlace() != null && !item.collectPlace().isBlank() ? item.collectPlace() : item.roomName();
        LocalDateTime colTime = item.collectedAt() != null ? item.collectedAt() : item.orderedAt();
        ui.collected.setText(colTime == null ? " "
                : "<html>" + html(collector) + " lấy mẫu lúc <b>" + html(TIME.format(colTime)) + "</b> tại " + html(collectPlace) + "</html>");
        String groupUpper = (item.testGroupName() != null && !item.testGroupName().isBlank()
                ? item.testGroupName() : "HÓA SINH").toUpperCase(Locale.ROOT);
        String performer = !item.performerName().isBlank() ? item.performerName() : collector;
        LocalDateTime execTime = item.resultAt() != null ? item.resultAt()
                : (item.collectEndAt() != null ? item.collectEndAt() : (colTime != null ? colTime : LocalDateTime.now(ZONE)));
        if (isReleased) {
            ui.performed.setText("<html>" + html(performer) + " trả kết quả lúc <b>" + html(TIME.format(execTime))
                    + "</b> tại XÉT NGHIỆM " + html(groupUpper) + "</html>");
        } else if (isApproved) {
            ui.performed.setText("<html>" + html(performer) + " duyệt kết quả lúc <b>" + html(TIME.format(execTime))
                    + "</b> tại XÉT NGHIỆM " + html(groupUpper) + "</html>");
        } else if (isExecuting) {
            ui.performed.setText("<html>" + html(performer) + " thực hiện lúc <b>" + html(TIME.format(execTime))
                    + "</b> tại XÉT NGHIỆM " + html(groupUpper) + "</html>");
        } else {
            ui.performed.setText(" ");
        }
        ui.conclusion.setText(Objects.toString(item.conclusionText(), ""));
        ui.feedback.setText("Phiếu " + item.orderCode() + " | Barcode: " + item.barcode() + " | Trạng thái: " + st.getDescription());
        populateResults(item, Map.of());
        detailLoader = new SwingWorker<>() {
            @Override protected ResultDetails doInBackground() {
                Map<Integer, List<ChiTietKetQuaXetNghiem>> map = new LinkedHashMap<>();
                Map<Integer, ThietLapThucHienDichVu> settings = new LinkedHashMap<>();
                List<PhieuXetNghiem.DongXetNghiem> tests = item.tests() != null && !item.tests().isEmpty()
                        ? item.tests()
                        : List.of(new PhieuXetNghiem.DongXetNghiem(item.orderId(), "", item.serviceName(),
                                item.quantity(), item.specimenType(), item.note(), item.statusEnum(),
                                item.resultText(), item.conclusionText(), item.resultAt(), item.performerName()));
                for (var t : tests) {
                    map.put(t.orderId(), services.serviceRepository().getKetQuaByChiDinh(t.orderId()));
                    settings.put(t.orderId(), services.serviceRepository().getExecutionSettings(t.orderId()));
                }
                return new ResultDetails(map, settings);
            }
            @Override protected void done() {
                if (isCancelled() || version != selectionVersion) return;
                try {
                    var details = get();
                    executionSettings = details.settings();
                    populateResults(item, details.rows());
                    detailsLoaded = true;
                    ui.results.setEnabled(true);
                    ui.save.setEnabled(isExecuting);
                    ui.approveResult.setEnabled(isExecuting);
                    ui.runFormula.setEnabled(true);
                    ui.print.setEnabled(canPrintResult());
                    ui.printResultFooter.setEnabled(isReleased && canPrintResult());
                }
                catch (Exception ex) { ui.feedback.setText("Không tải được chi tiết kết quả. Nhấn F5 để thử lại."); }
            }
        }; detailLoader.execute();
    }

    private static String formatExecutionCode(PhieuXetNghiem item) {
        String grp = Objects.toString(item.testGroupName(), "").toLowerCase(Locale.ROOT);
        String prefix = "xnhs";
        if (grp.contains("huyết") || grp.contains("máu")) prefix = "xnhh";
        else if (grp.contains("nước tiểu")) prefix = "xnnt";
        else if (grp.contains("miễn dịch")) prefix = "xnmd";
        else if (grp.contains("vi sinh")) prefix = "xnvs";
        String dayPart = (item.orderedAt() != null ? item.orderedAt().toLocalDate() : LocalDate.now(ZONE))
                .format(DateTimeFormatter.ofPattern("yyMMdd"));
        int suffix = Math.max(1, item.orderId() % 100);
        return prefix + dayPart + "." + suffix;
    }

    private void populateResults(PhieuXetNghiem item, Map<Integer, List<ChiTietKetQuaXetNghiem>> detailsByOrder) {
        if (ui.results.isEditing() && ui.results.getCellEditor() != null) {
            ui.results.getCellEditor().cancelCellEditing();
        }
        ui.resultModel.setRowCount(0);
        rowBindings.clear();
        String grpTitle = item.testGroupName() != null && !item.testGroupName().isBlank()
                ? item.testGroupName()
                : (item.specimenType().isBlank() ? "Hóa sinh" : item.specimenType());
        ui.resultModel.addRow(new Object[]{"1. " + grpTitle, "", "", "", "", "", "", ""});
        rowBindings.add(null);

        List<PhieuXetNghiem.DongXetNghiem> tests = item.tests() != null && !item.tests().isEmpty()
                ? item.tests()
                : List.of(new PhieuXetNghiem.DongXetNghiem(item.orderId(), "", item.serviceName(),
                        item.quantity(), item.specimenType(), item.note(), item.statusEnum(),
                        item.resultText(), item.conclusionText(), item.resultAt(), item.performerName()));

        TrangThaiChiDinhDichVu orderState = item.statusEnum();
        for (var test : tests) {
            List<ChiTietKetQuaXetNghiem> details = detailsByOrder != null
                    ? detailsByOrder.getOrDefault(test.orderId(), List.of()) : List.of();
            String qty = test.quantity() != null ? test.quantity().stripTrailingZeros().toPlainString() : "1";
            String spec = test.specimenType() != null && !test.specimenType().isBlank()
                    ? test.specimenType() : item.specimenType();
            String perf = (orderState.isResultReleased() || orderState.isApproved())
                    ? (!test.performerName().isBlank() ? test.performerName() : item.performerName())
                    : "";
            String code = test.serviceCode() != null && !test.serviceCode().isBlank()
                    ? test.serviceCode() : ("XN-" + test.orderId());

            if (details.isEmpty()) {
                var settings = executionSettings.getOrDefault(test.orderId(), ThietLapThucHienDichVu.MAC_DINH);
                String unit = test.resultText() == null || test.resultText().isBlank() ? settings.donViXetNghiem() : "";
                String noteOrConc = test.conclusionText() != null && !test.conclusionText().isBlank()
                        ? test.conclusionText() : Objects.toString(test.note(), "");
                ui.resultModel.addRow(new Object[]{
                        test.serviceName(),
                        qty,
                        Objects.toString(test.resultText(), ""),
                        unit,
                        spec,
                        noteOrConc,
                        perf,
                        code
                });
                rowBindings.add(new ResultRowBinding(test, null));
            } else {
                for (var line : details) {
                    String displayName = details.size() > 1
                            ? line.getTenChiSo()
                            : (line.getTenChiSo() != null && !line.getTenChiSo().isBlank() ? line.getTenChiSo() : test.serviceName());
                    String unit = Objects.toString(line.getDonViDo(), "");
                    String evalNote = formatDetailNote(line);
                    ui.resultModel.addRow(new Object[]{
                            displayName,
                            qty,
                            Objects.toString(line.getGiaTri(), ""),
                            unit,
                            spec,
                            evalNote,
                            perf,
                            code
                    });
                    rowBindings.add(new ResultRowBinding(test, line));
                }
            }
        }
        ui.results.setRowHeight(0, 23);
        if (ui.resultModel.getRowCount() > 1) {
            ui.results.setRowSelectionInterval(1, 1);
        }
    }

    private static String formatDetailNote(ChiTietKetQuaXetNghiem line) {
        String ref = Objects.toString(line.getKhoangThamChieu(), "").trim();
        String eval = Objects.toString(line.getDanhGia(), "").trim();
        if (!ref.isEmpty() && !eval.isEmpty() && !eval.contains(ref)) {
            return eval + " (CSBT: " + ref + ")";
        }
        return !eval.isEmpty() ? eval : ref;
    }



    private void startExecution() {
        if (selected == null || !selected.statusEnum().isSampleCollectedWaitingExec()) return;
        PhieuXetNghiem current = selected;
        var auth = KhamBenhPhienNguCanh.current();
        int empId = auth != null && auth.isValid() ? auth.employeeId() : 1;
        ui.execute.setEnabled(false);
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return XetNghiemDuLieuNguon.markExecuting(current.visitId(), current.barcode(), current.orderId(), empId);
            }
            @Override protected void done() {
                LocalDateTime now = LocalDateTime.now(ZONE);
                List<PhieuXetNghiem.DongXetNghiem> updatedLines = current.tests().stream()
                        .map(t -> new PhieuXetNghiem.DongXetNghiem(
                                t.orderId(), t.serviceCode(), t.serviceName(), t.quantity(),
                                t.specimenType(), t.note(), TrangThaiChiDinhDichVu.DANG_THUC_HIEN,
                                t.resultText(), t.conclusionText(), t.resultAt(), t.performerName()))
                        .toList();
                PhieuXetNghiem updated = new PhieuXetNghiem(
                        current.orderId(), current.visitId(), current.orderCode(), current.barcode(),
                        current.queueNumber(), current.patientCode(), current.patientName(),
                        current.birthDate(), current.gender(), current.address(), current.subject(),
                        current.bhytNumber(), current.bhytRate(), current.bhytExpiry(),
                        current.treatmentType(), current.departmentName(), current.roomName(),
                        current.diagnosis(), current.serviceName(), current.quantity(),
                        current.specimenType(), current.note(), TrangThaiChiDinhDichVu.DANG_THUC_HIEN,
                        current.orderedAt(), current.collectedAt() != null ? current.collectedAt() : now,
                        current.resultAt(), current.orderedByName(),
                        current.performerName().isBlank() ? ( current.collectorName().isBlank() ? "Quản Trị Hệ Thống" : current.collectorName() ) : current.performerName(),
                        current.resultText(), current.conclusionText(),
                        current.collectPlace(), current.testGroupName(),
                        now, current.collectorName(), current.paid(), updatedLines);
                replaceInMemoryOrder(current.orderId(), updated);
                selectOrder(updated);
                ui.queue.repaint();
                if (ui.resultModel.getRowCount() > 1) {
                    ui.results.setRowSelectionInterval(1, 1);
                }
            }
        }.execute();
    }

    private void cancelExecutionAction() {
        if (selected == null) return;
        if (ui.results.isEditing() && ui.results.getCellEditor() != null) {
            ui.results.getCellEditor().cancelCellEditing();
        }
        PhieuXetNghiem current = selected;
        var auth = KhamBenhPhienNguCanh.current();
        int empId = auth != null && auth.isValid() ? auth.employeeId() : 1;
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return XetNghiemDuLieuNguon.cancelExecution(current.visitId(), current.barcode(), current.orderId(), empId);
            }
            @Override protected void done() {
                List<PhieuXetNghiem.DongXetNghiem> updatedLines = current.tests().stream()
                        .map(t -> new PhieuXetNghiem.DongXetNghiem(
                                t.orderId(), t.serviceCode(), t.serviceName(), t.quantity(),
                                t.specimenType(), t.note(), TrangThaiChiDinhDichVu.DA_LAY_MAU,
                                t.resultText(), t.conclusionText(), null, t.performerName()))
                        .toList();
                PhieuXetNghiem reverted = new PhieuXetNghiem(
                        current.orderId(), current.visitId(), current.orderCode(), current.barcode(),
                        current.queueNumber(), current.patientCode(), current.patientName(),
                        current.birthDate(), current.gender(), current.address(), current.subject(),
                        current.bhytNumber(), current.bhytRate(), current.bhytExpiry(),
                        current.treatmentType(), current.departmentName(), current.roomName(),
                        current.diagnosis(), current.serviceName(), current.quantity(),
                        current.specimenType(), current.note(), TrangThaiChiDinhDichVu.DA_LAY_MAU,
                        current.orderedAt(), current.collectedAt(), null,
                        current.orderedByName(), current.performerName(),
                        current.resultText(), current.conclusionText(),
                        current.collectPlace(), current.testGroupName(),
                        current.collectEndAt(), current.collectorName(), current.paid(), updatedLines);
                replaceInMemoryOrder(current.orderId(), reverted);
                selectOrder(reverted);
                ui.queue.repaint();
            }
        }.execute();
    }

    private void showResultConfiguration() {
        if (!detailsLoaded || selected == null) return;
        int row = ui.results.getSelectedRow();
        ResultRowBinding binding = row >= 0 && row < rowBindings.size() ? rowBindings.get(row) : null;
        int orderId = binding == null ? selected.orderId() : binding.test().orderId();
        CanLamSangKetQuaNhapLieuHopThoai.showConfiguration(this,
                executionSettings.getOrDefault(orderId, ThietLapThucHienDichVu.MAC_DINH));
    }

    private void approveResultsAction() {
        saveDraftOrResults(true);
    }

    private void saveDraftOrResults(boolean approveFinal) {
        if (!detailsLoaded || selected == null || !selected.statusEnum().isExecuting()) return;
        if (ui.results.isEditing() && ui.results.getCellEditor() != null) {
            ui.results.getCellEditor().stopCellEditing();
        }
        Map<Integer, List<XetNghiemKetQuaMatHangDauVao>> itemsByOrder = new LinkedHashMap<>();
        Map<Integer, String> noteByOrder = new LinkedHashMap<>();
        for (int r = 1; r < ui.resultModel.getRowCount() && r < rowBindings.size(); r++) {
            ResultRowBinding binding = rowBindings.get(r);
            if (binding == null || binding.test() == null) continue;
            String indicator = Objects.toString(ui.resultModel.getValueAt(r, 0), "").trim();
            String val = Objects.toString(ui.resultModel.getValueAt(r, 2), "").trim();
            String unit = Objects.toString(ui.resultModel.getValueAt(r, 3), "").trim();
            String note = Objects.toString(ui.resultModel.getValueAt(r, 5), "").trim();
            if (!val.isEmpty()) {
                String ref = binding.detail() == null ? "" : Objects.toString(binding.detail().getKhoangThamChieu(), "");
                String eval = note;
                itemsByOrder.computeIfAbsent(binding.test().orderId(), k -> new ArrayList<>())
                        .add(new XetNghiemKetQuaMatHangDauVao(indicator, val, unit, ref, eval));
                if (!note.isEmpty()) {
                    noteByOrder.putIfAbsent(binding.test().orderId(), note);
                }
            }
        }
        if (itemsByOrder.isEmpty()) {
            if (approveFinal && !GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng nhập kết quả xét nghiệm vào ô Kết Quả trước khi duyệt.",
                        "Duyệt Kết Quả Xét Nghiệm", JOptionPane.WARNING_MESSAGE);
                return;
            }
            ui.feedback.setText("Nhập kết quả trước khi lưu tạm.");
            return;
        }
        PhanQuyenNguCanh auth = KhamBenhPhienNguCanh.current();
        if (auth == null || !auth.isValid()) {
            ui.feedback.setText("Phiên làm việc không hợp lệ. Vui lòng đăng nhập lại.");
            return;
        }
        String conclusionInput = ui.conclusion.getText() == null ? "" : ui.conclusion.getText().trim();
        ui.save.setEnabled(false);
        ui.approveResult.setEnabled(false);
        ui.feedback.setText(approveFinal ? "Đang duyệt và trả kết quả xét nghiệm..." : "Đang lưu tạm kết quả xét nghiệm vào CSDL...");
        PhieuXetNghiem currentOrder = selected;
        if (!approveFinal) {
            postResults(itemsByOrder,noteByOrder,conclusionInput,auth,currentOrder,false,Map.of());
            return;
        }
        new SwingWorker<Map<Integer,List<vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi.CauHinh>>,Void>(){
            protected Map<Integer,List<vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi.CauHinh>> doInBackground(){
                Map<Integer,List<vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi.CauHinh>> result=new LinkedHashMap<>();
                for(int orderId:itemsByOrder.keySet())result.put(orderId,services.recordResult().getConsumptionDefinition(auth,orderId));return result;
            }
            protected void done(){try{
                Map<Integer,List<vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi.CauHinh>> definitions=get();
                Map<Integer,List<vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi.SoLuongThucTe>> actual=new LinkedHashMap<>();
                for(var entry:definitions.entrySet()){
                    var accepted=TieuHaoDichVuNhapLieu.ask(XetNghiemKetQuaManHinh.this,entry.getValue());
                    if(accepted==null){ui.save.setEnabled(ui.isExecutingMode());ui.approveResult.setEnabled(ui.isExecutingMode());ui.feedback.setText("Đã hủy nhập lượng tiêu hao; kết quả chưa lưu.");return;}
                    actual.put(entry.getKey(),accepted);
                }
                postResults(itemsByOrder,noteByOrder,conclusionInput,auth,currentOrder,approveFinal,actual);
            }catch(Exception ex){ui.save.setEnabled(ui.isExecutingMode());ui.approveResult.setEnabled(ui.isExecutingMode());ui.feedback.setText("Không tải được cấu hình tiêu hao: "+ex.getMessage());}}
        }.execute();
    }

    private void postResults(Map<Integer,List<XetNghiemKetQuaMatHangDauVao>> itemsByOrder,Map<Integer,String> noteByOrder,
            String conclusionInput,PhanQuyenNguCanh auth,PhieuXetNghiem currentOrder,boolean approveFinal,
            Map<Integer,List<vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi.SoLuongThucTe>> actualByOrder){
        List<String> notices = new ArrayList<>();
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() {
                int savedCount = 0;
                KiemToanTacNhanNguCanh audit = new KiemToanTacNhanNguCanh(
                        auth.employeeId(), auth.roleId(), auth.roleCode(),
                        auth.facilityId(), auth.departmentId(), "DESKTOP", UUID.randomUUID());
                for (var entry : itemsByOrder.entrySet()) {
                    int ordId = entry.getKey();
                    List<XetNghiemKetQuaMatHangDauVao> items = entry.getValue();
                    String conclusion = !conclusionInput.isEmpty()
                            ? conclusionInput
                            : noteByOrder.getOrDefault(ordId, "");
                    RecordDichVuKetQuaCommand cmd = new RecordDichVuKetQuaCommand(
                            ordId, "", conclusion, "", items, auth, audit,actualByOrder.getOrDefault(ordId,List.of()),
                            consumptionOperationKeys.computeIfAbsent(ordId,ignored->UUID.randomUUID()), approveFinal);
                    var res = services.recordResult().handle(cmd);
                    if (res != null && res.isSuccess()) {
                        if (approveFinal) {
                            String consumptionStatus = res.getValue().map(RecordDichVuKetQuaBienNhan::consumptionStatus).orElse("");
                            if ("PENDING".equals(consumptionStatus)) notices.add("CLS-" + ordId + ": tiêu hao chờ bổ sung tồn tủ trực.");
                            if ("NO_DEFINITION".equals(consumptionStatus)) notices.add("CLS-" + ordId + ": chưa có định mức tiêu hao.");
                            if ("NOT_TRIGGERED".equals(consumptionStatus)) notices.add("CLS-" + ordId + ": cấu hình thời điểm cũ chưa hỗ trợ trừ kho.");
                            XetNghiemDuLieuNguon.markApprovedStatusAfterApprove(ordId, auth.employeeId());
                        } else {
                            XetNghiemDuLieuNguon.keepExecutingStatusAfterDraftSave(ordId, auth.employeeId());
                        }
                        savedCount++;
                    } else {
                        notices.add("CLS-" + ordId + ": " + (res == null ? "Không lưu được kết quả." :
                                res.getError().map(e -> e.message()).orElse("Không lưu được kết quả.")));
                    }
                }
                return savedCount;
            }
            @Override protected void done() {
                ui.save.setEnabled(ui.isExecutingMode());
                ui.approveResult.setEnabled(ui.isExecutingMode());
                try {
                    int cnt = get();
                    if (cnt > 0) {
                        ui.feedback.setText(approveFinal
                                ? "Đã duyệt kết quả cho phiếu " + currentOrder.orderCode() + ". Nhấn [Trả Kết Quả] để hoàn tất trả kết quả."
                                : "Đã lưu tạm kết quả phiếu " + currentOrder.orderCode() + " vào CSDL.");
                        refreshData();
                    } else {
                        ui.feedback.setText("Không thể lưu kết quả xét nghiệm. Kiểm tra quyền hạn hoặc trạng thái phiếu.");
                    }
                    if (!notices.isEmpty()) {
                        ui.feedback.setText("Đã lưu " + cnt + "/" + itemsByOrder.size() + " chỉ định. " + String.join(" ", notices));
                    }
                } catch (Exception ex) {
                    ui.feedback.setText("Lỗi khi lưu kết quả: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void releaseResultsAction() {
        if (selected == null || !selected.statusEnum().isApproved()) return;
        PhieuXetNghiem current = selected;
        var auth = KhamBenhPhienNguCanh.current();
        int empId = auth != null && auth.isValid() ? auth.employeeId() : 1;
        ui.releaseResult.setEnabled(false);
        ui.feedback.setText("Đang trả kết quả xét nghiệm cho phiếu " + current.orderCode() + "...");
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return XetNghiemDuLieuNguon.releaseApprovedResult(
                        current.visitId(), current.barcode(), current.orderId(), empId);
            }
            @Override protected void done() {
                LocalDateTime now = LocalDateTime.now(ZONE);
                List<PhieuXetNghiem.DongXetNghiem> updatedLines = current.tests().stream()
                        .map(t -> new PhieuXetNghiem.DongXetNghiem(
                                t.orderId(), t.serviceCode(), t.serviceName(), t.quantity(),
                                t.specimenType(), t.note(), TrangThaiChiDinhDichVu.DA_CO_KET_QUA,
                                t.resultText(), t.conclusionText(), now, t.performerName()))
                        .toList();
                PhieuXetNghiem released = new PhieuXetNghiem(
                        current.orderId(), current.visitId(), current.orderCode(), current.barcode(),
                        current.queueNumber(), current.patientCode(), current.patientName(),
                        current.birthDate(), current.gender(), current.address(), current.subject(),
                        current.bhytNumber(), current.bhytRate(), current.bhytExpiry(),
                        current.treatmentType(), current.departmentName(), current.roomName(),
                        current.diagnosis(), current.serviceName(), current.quantity(),
                        current.specimenType(), current.note(), TrangThaiChiDinhDichVu.DA_CO_KET_QUA,
                        current.orderedAt(), current.collectedAt(), now,
                        current.orderedByName(), current.performerName(),
                        current.resultText(), current.conclusionText(),
                        current.collectPlace(), current.testGroupName(),
                        current.collectEndAt(), current.collectorName(), current.paid(), updatedLines);
                replaceInMemoryOrder(current.orderId(), released);
                selectOrder(released);
                ui.queue.repaint();
                ui.feedback.setText("Đã trả kết quả chính thức cho phiếu " + current.orderCode() + " (Bác sĩ đã nhận được kết quả).");
                refreshData();
            }
        }.execute();
    }

    private void unapproveOrRecallAction() {
        if (selected == null) return;
        PhieuXetNghiem current = selected;
        var auth = KhamBenhPhienNguCanh.current();
        int empId = auth != null && auth.isValid() ? auth.employeeId() : 1;
        ui.unapproveResult.setEnabled(false);
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return XetNghiemDuLieuNguon.unapproveToExecuting(
                        current.visitId(), current.barcode(), current.orderId(), empId);
            }
            @Override protected void done() {
                List<PhieuXetNghiem.DongXetNghiem> updatedLines = current.tests().stream()
                        .map(t -> new PhieuXetNghiem.DongXetNghiem(
                                t.orderId(), t.serviceCode(), t.serviceName(), t.quantity(),
                                t.specimenType(), t.note(), TrangThaiChiDinhDichVu.DANG_THUC_HIEN,
                                t.resultText(), t.conclusionText(), null, t.performerName()))
                        .toList();
                PhieuXetNghiem reopened = new PhieuXetNghiem(
                        current.orderId(), current.visitId(), current.orderCode(), current.barcode(),
                        current.queueNumber(), current.patientCode(), current.patientName(),
                        current.birthDate(), current.gender(), current.address(), current.subject(),
                        current.bhytNumber(), current.bhytRate(), current.bhytExpiry(),
                        current.treatmentType(), current.departmentName(), current.roomName(),
                        current.diagnosis(), current.serviceName(), current.quantity(),
                        current.specimenType(), current.note(), TrangThaiChiDinhDichVu.DANG_THUC_HIEN,
                        current.orderedAt(), current.collectedAt(), null,
                        current.orderedByName(), current.performerName(),
                        current.resultText(), current.conclusionText(),
                        current.collectPlace(), current.testGroupName(),
                        current.collectEndAt(), current.collectorName(), current.paid(), updatedLines);
                replaceInMemoryOrder(current.orderId(), reopened);
                selectOrder(reopened);
                ui.queue.repaint();
                ui.feedback.setText("Đã mở lại phiếu " + current.orderCode() + " về chế độ Đang thực hiện để chỉnh sửa.");
            }
        }.execute();
    }

    private void replaceInMemoryOrder(int orderId, PhieuXetNghiem updated) {
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i).orderId() == orderId) {
                orders.set(i, updated);
            }
        }
        for (int i = 0; i < filtered.size(); i++) {
            if (filtered.get(i).orderId() == orderId) {
                filtered.set(i, updated);
                if (i < ui.queueModel.getRowCount()) {
                    ui.queueModel.setValueAt(updated, i, 0);
                    ui.queueModel.setValueAt(updated, i, 2);
                }
            }
        }
    }

    private void openEntry() {
        if (selected == null || selected.statusEnum().isCancelled() || GraphicsEnvironment.isHeadless()) return;
        int row = ui.results.getSelectedRow();
        PhieuXetNghiem.DongXetNghiem targetTest = null;
        if (row >= 1 && row < rowBindings.size() && rowBindings.get(row) != null) {
            targetTest = rowBindings.get(row).test();
        } else if (rowBindings.size() > 1 && rowBindings.get(1) != null) {
            targetTest = rowBindings.get(1).test();
        }
        int targetOrderId = targetTest != null ? targetTest.orderId() : selected.orderId();
        String targetServiceName = targetTest != null ? targetTest.serviceName() : selected.serviceName();
        new CanLamSangKetQuaNhapLieuHopThoai(
                SwingUtilities.getWindowAncestor(this),
                targetOrderId,
                selected.patientName(),
                targetServiceName,
                services,
                this::refreshData).setVisible(true);
    }

    private boolean canPrintResult() {
        if (!detailsLoaded || selected == null) return false;
        return executionSettings.size() != 1
                || !executionSettings.values().iterator().next().khongInPhieuRieng();
    }

    private void printResults() {
        if (selected == null || GraphicsEnvironment.isHeadless()) return;
        if (!canPrintResult()) {
            ui.feedback.setText(detailsLoaded ? "Dịch vụ được cấu hình không in phiếu kết quả riêng."
                    : "Chờ tải xong dữ liệu trước khi in.");
            return;
        }
        try { ui.results.print(JTable.PrintMode.FIT_WIDTH, new MessageFormat("KẾT QUẢ XÉT NGHIỆM - " + selected.orderCode()), new MessageFormat("{0}")); }
        catch (PrinterException ex) { ui.feedback.setText("Không thể in kết quả xét nghiệm."); }
    }
    private List<XetNghiemDuLieuNguon.LichSuKetQuaDong> historyRows() {
        if (selected == null) return List.of();
        var actor = KhamBenhPhienNguCanh.current();
        if (actor != null && actor.isValid()) {
            List<XetNghiemDuLieuNguon.LichSuKetQuaDong> dbRows =
                    XetNghiemDuLieuNguon.loadPatientHistory(actor.facilityId(), selected.patientCode());
            if (!dbRows.isEmpty()) return dbRows;
        }
        List<XetNghiemDuLieuNguon.LichSuKetQuaDong> fallback = new ArrayList<>();
        for (PhieuXetNghiem p : orders) {
            if (!p.patientCode().equals(selected.patientCode())) continue;
            if (p.tests() != null && !p.tests().isEmpty()) {
                for (var t : p.tests()) {
                    if (!t.resultText().isBlank()) {
                        fallback.add(new XetNghiemDuLieuNguon.LichSuKetQuaDong(
                                p.orderedAt(), p.orderCode(), t.serviceName(), t.serviceName(),
                                t.resultText(), "", "", t.conclusionText()));
                    }
                }
            } else if (!p.resultText().isBlank()) {
                fallback.add(new XetNghiemDuLieuNguon.LichSuKetQuaDong(
                        p.orderedAt(), p.orderCode(), p.serviceName(), p.serviceName(),
                        p.resultText(), "", "", p.conclusionText()));
            }
        }
        fallback.sort(Comparator.comparing(XetNghiemDuLieuNguon.LichSuKetQuaDong::orderedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return fallback;
    }
    private static Double numeric(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String token = value.strip().split("\\s+")[0].replace(',', '.');
            double v = Double.parseDouble(token);
            return Double.isFinite(v) ? v : null;
        } catch (NumberFormatException ex) { return null; }
    }
    private void showHistory(boolean chart) {
        if (GraphicsEnvironment.isHeadless() || selected == null) return;
        var rows = historyRows();
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                (chart ? "Biểu đồ diễn biến kết quả xét nghiệm - " : "Lịch sử kết quả xét nghiệm - ") + selected.patientName(),
                Dialog.ModalityType.MODELESS);
        if (chart) {
            var points = rows.stream().map(XetNghiemDuLieuNguon.LichSuKetQuaDong::resultValue)
                    .map(XetNghiemKetQuaManHinh::numeric).filter(Objects::nonNull).toList();
            if (points.size() < 2) {
                JOptionPane.showMessageDialog(this,
                        "Cần ít nhất 2 kết quả định lượng (số) của bệnh nhân " + selected.patientName() + " để vẽ biểu đồ diễn biến.",
                        "Biểu Đồ Xét Nghiệm", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            dialog.add(new JPanel() {
                @Override protected void paintComponent(Graphics graphics) {
                    super.paintComponent(graphics); Graphics2D g = (Graphics2D)graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    double min = Collections.min(points), span = Math.max(.001, Collections.max(points) - min);
                    int previousX = 0, previousY = 0; g.setColor(XetNghiemKetQuaBoCuc.BLUE); g.setStroke(new BasicStroke(2));
                    for (int i=0; i<points.size(); i++) {
                        int x = 40 + i*(getWidth()-80)/(points.size()-1), y = getHeight()-50-(int)((points.get(i)-min)/span*(getHeight()-100));
                        if (i>0) g.drawLine(previousX, previousY, x, y);
                        g.fillOval(x-4,y-4,8,8); g.drawString(String.valueOf(points.get(i)), x, y-10); previousX=x; previousY=y;
                    } g.dispose();
                }
            });
        } else {
            DefaultTableModel model = new DefaultTableModel(
                    new String[]{"Ngày chỉ định", "Mã phiếu", "Xét nghiệm / Chỉ số", "Kết quả", "Đơn vị", "Tham chiếu", "Đánh giá"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            for (var row : rows) {
                model.addRow(new Object[]{
                        row.orderedAt() == null ? "" : TIME.format(row.orderedAt()),
                        row.orderCode(),
                        row.indicatorName(),
                        row.resultValue(),
                        row.unit(),
                        row.referenceRange(),
                        row.evaluation()
                });
            }
            JTable tbl = new JTable(model);
            tbl.setRowHeight(24);
            tbl.setFont(UngDungPhongChu.BODY);
            tbl.getTableHeader().setFont(UngDungPhongChu.BODY_BOLD);
            dialog.add(new JScrollPane(tbl));
        }
        dialog.setSize(820, 420); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
    }
    private void installRenderers() {
        ui.queue.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(table, "", s, false, r, c); setFont(table.getFont());
                if (value instanceof PhieuXetNghiem item) setText("<html><b>"+html(item.patientName().toUpperCase(Locale.ROOT))+"</b><br>"+html(item.patientCode()+" - "+item.shortSubjectTag())+"</html>");
                setBorder(new EmptyBorder(0, 7, 0, 0)); return this;
            }
        });
        DefaultTableCellRenderer centered = new DefaultTableCellRenderer(); centered.setHorizontalAlignment(SwingConstants.CENTER);
        ui.queue.getColumnModel().getColumn(1).setCellRenderer(centered); ui.queue.getColumnModel().getColumn(3).setCellRenderer(centered);
        ui.queue.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            private Color dotColor = new Color(143, 0, 177);
            private boolean drawDot;
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, "", false, false, r, c);
                setBackground(Color.WHITE);
                drawDot = v instanceof PhieuXetNghiem;
                if (v instanceof PhieuXetNghiem item) {
                    dotColor = switch (item.statusEnum()) {
                        case DA_CO_KET_QUA -> new Color(26, 148, 66);
                        case DA_DUYET_KET_QUA -> new Color(14, 118, 212);
                        case DANG_THUC_HIEN -> new Color(235, 118, 5);
                        case DA_HUY -> new Color(150, 150, 150);
                        default -> new Color(143, 0, 177);
                    };
                }
                return this;
            }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (!drawDot) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(dotColor);
                int d = 13;
                g2.fillOval((getWidth() - d) / 2, (getHeight() - d) / 2, d, d);
                g2.dispose();
            }
        });
        DefaultTableCellRenderer result = new DefaultTableCellRenderer() {
            private boolean gutter;
            private Color gutterDotColor;
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (gutter) {
                    int gw = 24;
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, gw, getHeight());
                    if (gutterDotColor != null) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(gutterDotColor);
                        int d = 13;
                        g2.fillOval((gw - d) / 2, (getHeight() - d) / 2, d, d);
                        g2.dispose();
                    }
                }
            }
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean s, boolean f, int r, int c) {
                boolean exec = XetNghiemKetQuaManHinh.this.ui.isExecutingMode();
                super.getTableCellRendererComponent(table, value, r != 0 && s && !exec, false, r, c);
                setFont(table.getFont());
                gutter = c == 0 && r > 0;
                gutterDotColor = null;
                if (gutter && selected != null) {
                    gutterDotColor = switch (selected.statusEnum()) {
                        case DANG_THUC_HIEN -> new Color(235, 118, 5);
                        case DA_DUYET_KET_QUA -> new Color(14, 118, 212);
                        case DA_CO_KET_QUA -> new Color(26, 148, 66);
                        case DA_HUY -> new Color(150, 150, 150);
                        default -> exec ? new Color(235, 118, 5) : new Color(143, 0, 177);
                    };
                }
                if (r == 0) {
                    setBackground(Color.WHITE);
                    setForeground(XetNghiemKetQuaBoCuc.BLUE);
                } else if (exec || s) {
                    setBackground(exec ? new Color(238, 238, 238) : XetNghiemKetQuaBoCuc.SELECT);
                    setForeground(exec ? Color.BLACK : Color.WHITE);
                } else {
                    setBackground(Color.WHITE);
                    setForeground(Color.BLACK);
                }
                setHorizontalAlignment(c > 0 && c < 4 ? SwingConstants.CENTER : SwingConstants.LEFT);
                setBorder(new EmptyBorder(0, c == 0 ? (r == 0 ? 10 : 28) : 4, 0, 0));
                return this;
            }
        };
        for (int c = 0; c < ui.results.getColumnCount(); c++) {
            ui.results.getColumnModel().getColumn(c).setCellRenderer(result);
        }
        // Custom renderer & editor for Column 2 (Kết Quả) with pale-yellow input box + [....] button in Executing Mode
        ui.results.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            private final JPanel boxPanel = new JPanel(new BorderLayout(1, 0));
            private final JLabel valLabel = new JLabel("", SwingConstants.CENTER);
            private final JLabel dotsBtn = new JLabel("....", SwingConstants.CENTER);
            {
                boxPanel.setBorder(new EmptyBorder(2, 4, 2, 4));
                valLabel.setOpaque(true);
                valLabel.setBackground(new Color(255, 255, 185));
                valLabel.setBorder(new LineBorder(new Color(210, 210, 155), 1));
                valLabel.setFont(UngDungPhongChu.XET_NGHIEM);
                dotsBtn.setOpaque(true);
                dotsBtn.setBackground(new Color(245, 245, 245));
                dotsBtn.setBorder(new LineBorder(new Color(175, 175, 175), 1));
                dotsBtn.setPreferredSize(new Dimension(22, 20));
                dotsBtn.setFont(UngDungPhongChu.SMALL_BOLD);
                boxPanel.add(valLabel, BorderLayout.CENTER);
                boxPanel.add(dotsBtn, BorderLayout.EAST);
            }
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean s, boolean f, int r, int c) {
                if (r > 0 && XetNghiemKetQuaManHinh.this.ui.isExecutingMode()) {
                    boxPanel.setBackground(new Color(238, 238, 238));
                    valLabel.setText(Objects.toString(value, ""));
                    return boxPanel;
                }
                return result.getTableCellRendererComponent(table, value, s, f, r, c);
            }
        });
        ui.results.getColumnModel().getColumn(2).setCellEditor(new ResultBoxCellEditor());
    }

    private final class ResultBoxCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new BorderLayout(1, 0));
        private final JTextField textField = new JTextField();
        private final JButton dotsButton = new JButton("....");

        ResultBoxCellEditor() {
            panel.setBackground(new Color(238, 238, 238));
            panel.setBorder(new EmptyBorder(2, 4, 2, 4));
            textField.setBackground(new Color(255, 255, 185));
            textField.setFont(UngDungPhongChu.XET_NGHIEM);
            textField.setHorizontalAlignment(SwingConstants.CENTER);
            textField.setBorder(new LineBorder(new Color(195, 195, 130), 1));
            textField.addActionListener(e -> stopCellEditing());
            dotsButton.setFont(UngDungPhongChu.SMALL_BOLD);
            dotsButton.setMargin(new Insets(0, 0, 0, 0));
            dotsButton.setPreferredSize(new Dimension(22, 20));
            dotsButton.setFocusPainted(false);
            dotsButton.setBackground(new Color(245, 245, 245));
            dotsButton.setBorder(new LineBorder(new Color(175, 175, 175), 1));
            dotsButton.addActionListener(e -> {
                stopCellEditing();
                openEntry();
            });
            panel.add(textField, BorderLayout.CENTER);
            panel.add(dotsButton, BorderLayout.EAST);
        }

        @Override public Object getCellEditorValue() {
            return textField.getText().trim();
        }

        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            textField.setText(Objects.toString(value, ""));
            SwingUtilities.invokeLater(textField::selectAll);
            return panel;
        }
    }
    private static String html(String text) { return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    @Override public void onHide() {
        if (loader != null) loader.cancel(true); if (detailLoader != null) detailLoader.cancel(true);
        loading = false; selectionVersion++;
    }
}
