package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.examination.add.AddChanDoanCommand;
import vn.clinicmanagement.application.examination.record.RecordSinhHieuDauHieuCommand;
import vn.clinicmanagement.application.examination.start.StartKhamCommand;
import vn.clinicmanagement.application.examination.update.UpdateEMRLamSangRecordCommand;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.ChanDoan;
import vn.clinicmanagement.domain.examination.ICD;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.examination.SinhHieu;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Tab "Khám Bệnh" - Ghi nhận lý do khám, tiền sử, quá trình bệnh lý, triệu chứng,
 * sinh hiệu (Mạch, Nhiệt độ, Huyết áp, SpO2, Nhịp thở, Cân nặng, Chiều cao, BMI),
 * khám toàn thân/bộ phận và chẩn đoán ban đầu.
 */
public class KhamBenhLamSangTheManHinh extends JPanel {

    private final KhamBenhGhepNoiGoc.Services services;
    private int currentVisitId = 0;
    private int currentDoctorId = 0;
    private boolean readOnlyLocked = false;
    private Runnable onSavedCallback;
    private Runnable onTreatmentSheetCallback;

    private JCheckBox chkCapSoMoi;
    private JTextField txtLyDoKham;
    private JCheckBox chkDiUng;
    private JTextArea txtTienSuDiUng;
    private JTextArea txtQuaTrinhBenhLy;
    private JTextArea txtTrieuChung;
    private JTextArea txtTienSuBanThan;
    private JTextArea txtTienSuGiaDinh;

    private JTextField txtMach;
    private JTextField txtNhietDo;
    private JTextField txtHuyetApTamThu;
    private JTextField txtHuyetApTamTruong;
    private JTextField txtSpo2;
    private JTextField txtNhipTho;
    private JTextField txtCanNang;
    private JTextField txtChieuCao;
    private JTextField txtBmi;

    private JTextArea txtKhamBenh;
    private JTextArea txtKhamBoPhan;

    private JComboBox<String> cboChanDoanBanDau;
    private DefaultListModel<String> secondaryModel;
    private JList<String> listSecondary;
    private List<ICD> icdCatalog = new ArrayList<>();

    private JButton btnGoiKhamGanNhat;
    private JButton btnToDieuTri;
    private JButton btnMauKhamBenh;
    private JButton btnSave;
    private JLabel lblStatus;

    private String loadedPrimaryDiagText = "";

    public KhamBenhLamSangTheManHinh(KhamBenhGhepNoiGoc.Services services) {
        this.services = services;
        initUI();
        loadIcdCatalog();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBackground(Color.WHITE);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(6, 10, 6, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int row = 0;

        // Row 0: Checkbox cấp số khám mới
        chkCapSoMoi = new JCheckBox("Bệnh nhân cấp số khám mới");
        chkCapSoMoi.setBackground(Color.WHITE);
        chkCapSoMoi.setFont(UngDungPhongChu.BODY);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0;
        formPanel.add(chkCapSoMoi, gbc);
        gbc.gridwidth = 1;

        // Row 1: Lý do khám
        txtLyDoKham = createTextField();
        addLabeledRow(formPanel, gbc, row++, "Lý do khám:", txtLyDoKham);

        // Row 2: Tiền sử dị ứng
        JPanel allergyPanel = new JPanel(new BorderLayout(8, 0));
        allergyPanel.setOpaque(false);
        chkDiUng = new JCheckBox("Dị ứng");
        chkDiUng.setBackground(Color.WHITE);
        chkDiUng.setFont(UngDungPhongChu.BODY);
        txtTienSuDiUng = createTextArea(2);
        txtTienSuDiUng.setEnabled(false);
        chkDiUng.addActionListener(e -> txtTienSuDiUng.setEnabled(!readOnlyLocked && chkDiUng.isSelected()));
        allergyPanel.add(chkDiUng, BorderLayout.WEST);
        allergyPanel.add(wrapArea(txtTienSuDiUng, 38), BorderLayout.CENTER);
        addLabeledRow(formPanel, gbc, row++, "Tiền sử dị ứng:", allergyPanel);

        // Row 3: Quá trình bệnh lý
        txtQuaTrinhBenhLy = createTextArea(3);
        addLabeledRow(formPanel, gbc, row++, "Quá trình bệnh lý:", wrapArea(txtQuaTrinhBenhLy, 52));

        // Row 4: Triệu chứng + nút chọn nhanh
        JPanel symptomPanel = new JPanel(new BorderLayout(6, 0));
        symptomPanel.setOpaque(false);
        txtTrieuChung = createTextArea(2);
        JButton btnSymptomTemplate = createSideIconButton("Chọn triệu chứng mẫu");
        btnSymptomTemplate.addActionListener(e -> insertQuickSymptom());
        symptomPanel.add(wrapArea(txtTrieuChung, 42), BorderLayout.CENTER);
        symptomPanel.add(btnSymptomTemplate, BorderLayout.EAST);
        addLabeledRow(formPanel, gbc, row++, "Triệu chứng:", symptomPanel);

        // Row 5: Tiền sử bản thân
        txtTienSuBanThan = createTextArea(2);
        addLabeledRow(formPanel, gbc, row++, "Tiền sử bản thân:", wrapArea(txtTienSuBanThan, 42));

        // Row 6: Tiền sử gia đình
        txtTienSuGiaDinh = createTextArea(2);
        addLabeledRow(formPanel, gbc, row++, "Tiền sử gia đình:", wrapArea(txtTienSuGiaDinh, 42));

        // Row 7-8: Sinh hiệu 2 hàng
        txtMach = createVitalField("0.00");
        txtNhietDo = createVitalField("0.00");
        txtHuyetApTamThu = createVitalField("0.00");
        txtHuyetApTamTruong = createVitalField("0.00");
        txtSpo2 = createVitalField("0.00");

        txtNhipTho = createVitalField("0.00");
        txtCanNang = createVitalField("0.00");
        txtChieuCao = createVitalField("0.00");
        txtBmi = createVitalField("");
        txtBmi.setEditable(false);
        txtBmi.setBackground(new Color(235, 238, 244));

        DocumentListener bmiListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateBmi(); }
            @Override public void removeUpdate(DocumentEvent e) { updateBmi(); }
            @Override public void changedUpdate(DocumentEvent e) { updateBmi(); }
        };
        txtCanNang.getDocument().addDocumentListener(bmiListener);
        txtChieuCao.getDocument().addDocumentListener(bmiListener);

        JPanel vitalsPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        vitalsPanel.setOpaque(false);
        vitalsPanel.setBorder(new EmptyBorder(3, 0, 3, 0));

        JPanel vitalsRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        vitalsRow1.setOpaque(false);
        vitalsRow1.add(createVitalLabel("Mạch (lần/phút):"));
        vitalsRow1.add(txtMach);
        vitalsRow1.add(createVitalLabel("Nhiệt độ (°C):"));
        vitalsRow1.add(txtNhietDo);
        vitalsRow1.add(createVitalLabel("Huyết áp (mmHg):"));
        vitalsRow1.add(txtHuyetApTamThu);
        vitalsRow1.add(new JLabel("/"));
        vitalsRow1.add(txtHuyetApTamTruong);
        vitalsRow1.add(createVitalLabel("SPO2:"));
        vitalsRow1.add(txtSpo2);

        JPanel vitalsRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        vitalsRow2.setOpaque(false);
        vitalsRow2.add(createVitalLabel("Nhịp thở (lần/phút):"));
        vitalsRow2.add(txtNhipTho);
        vitalsRow2.add(createVitalLabel("Cân nặng (Kg):"));
        vitalsRow2.add(txtCanNang);
        vitalsRow2.add(createVitalLabel("Chiều cao (cm):"));
        vitalsRow2.add(txtChieuCao);
        vitalsRow2.add(createVitalLabel("BMI:"));
        vitalsRow2.add(txtBmi);

        vitalsPanel.add(vitalsRow1);
        vitalsPanel.add(vitalsRow2);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0;
        formPanel.add(vitalsPanel, gbc);
        gbc.gridwidth = 1;

        // Row 9: Khám bệnh (toàn thân)
        txtKhamBenh = createTextArea(2);
        addLabeledRow(formPanel, gbc, row++, "Khám bệnh:", wrapArea(txtKhamBenh, 42));

        // Row 10: Khám các bộ phận
        txtKhamBoPhan = createTextArea(2);
        addLabeledRow(formPanel, gbc, row++, "Khám các bộ phận:", wrapArea(txtKhamBoPhan, 42));

        // Row 11: Chẩn đoán ban đầu
        cboChanDoanBanDau = new JComboBox<>();
        cboChanDoanBanDau.setEditable(true);
        cboChanDoanBanDau.setFont(UngDungPhongChu.BODY);
        cboChanDoanBanDau.setPreferredSize(new Dimension(280, 24));
        addLabeledRow(formPanel, gbc, row++, "Chẩn đoán ban đầu:", cboChanDoanBanDau);

        // Row 12: Link cập nhật chẩn đoán + danh sách bệnh kèm theo + nút Bệnh Kèm Theo
        JPanel secondaryPanel = new JPanel(new BorderLayout(8, 0));
        secondaryPanel.setOpaque(false);

        JButton btnSyncFromOrder = new JButton("<html><u>Cập nhật chẩn đoán<br>từ phiếu chỉ định</u></html>");
        btnSyncFromOrder.setFont(UngDungPhongChu.SMALL);
        btnSyncFromOrder.setForeground(new Color(20, 80, 175));
        btnSyncFromOrder.setContentAreaFilled(false);
        btnSyncFromOrder.setBorder(new EmptyBorder(4, 0, 4, 6));
        btnSyncFromOrder.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSyncFromOrder.setVerticalAlignment(SwingConstants.TOP);
        btnSyncFromOrder.addActionListener(e -> loadDiagnosisFromVisit());

        secondaryModel = new DefaultListModel<>();
        listSecondary = new JList<>(secondaryModel);
        listSecondary.setFont(UngDungPhongChu.BODY);
        JScrollPane secScroll = new JScrollPane(listSecondary);
        secScroll.setPreferredSize(new Dimension(420, 74));
        secScroll.setBorder(new LineBorder(new Color(195, 202, 212), 1));

        JButton btnBenhKemTheo = new JButton("<html><center>Bệnh<br>Kèm Theo</center></html>");
        btnBenhKemTheo.setFont(UngDungPhongChu.SMALL);
        btnBenhKemTheo.setBackground(new Color(246, 248, 252));
        btnBenhKemTheo.setFocusPainted(false);
        btnBenhKemTheo.setPreferredSize(new Dimension(76, 74));
        btnBenhKemTheo.setBorder(new LineBorder(new Color(185, 195, 210), 1));
        btnBenhKemTheo.addActionListener(e -> addSecondaryDiagnosisDialog());

        secondaryPanel.add(btnSyncFromOrder, BorderLayout.WEST);
        secondaryPanel.add(secScroll, BorderLayout.CENTER);
        secondaryPanel.add(btnBenhKemTheo, BorderLayout.EAST);

        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(secondaryPanel, gbc);

        JScrollPane mainScroll = new JScrollPane(formPanel);
        mainScroll.setBorder(null);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScroll, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        registerSaveShortcut();
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setBackground(new Color(248, 249, 252));
        footer.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 218, 228)),
                new EmptyBorder(6, 10, 6, 10)
        ));

        lblStatus = new JLabel("");
        lblStatus.setFont(UngDungPhongChu.SMALL);
        lblStatus.setForeground(new Color(180, 75, 25));

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightButtons.setOpaque(false);

        btnGoiKhamGanNhat = createFooterButton("Gọi Khám Gần Nhất", new Color(22, 130, 62));
        btnGoiKhamGanNhat.addActionListener(e -> loadNearestExamHistory());

        btnToDieuTri = createFooterButton("Tờ Điều Trị", new Color(30, 85, 145));
        btnToDieuTri.addActionListener(e -> {
            if (onTreatmentSheetCallback != null) onTreatmentSheetCallback.run();
        });

        btnMauKhamBenh = createFooterButton("Mẫu Khám Bệnh ▾", new Color(25, 105, 175));
        btnMauKhamBenh.addActionListener(e -> showClinicalTemplateMenu());

        btnSave = createFooterButton("Lưu (Ctrl+S)", new Color(25, 30, 40));
        btnSave.addActionListener(e -> handleSave());

        rightButtons.add(btnGoiKhamGanNhat);
        rightButtons.add(btnToDieuTri);
        rightButtons.add(btnMauKhamBenh);
        rightButtons.add(btnSave);

        footer.add(lblStatus, BorderLayout.WEST);
        footer.add(rightButtons, BorderLayout.EAST);
        return footer;
    }

    private void registerSaveShortcut() {
        int mask = GraphicsEnvironment.isHeadless()
                ? java.awt.event.InputEvent.CTRL_DOWN_MASK
                : Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, mask), "saveClinicalExam");
        getActionMap().put("saveClinicalExam", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { handleSave(); }
        });
    }

    public void setVisitContext(int visitId, int doctorId) {
        boolean changed = this.currentVisitId != visitId;
        this.currentVisitId = visitId;
        this.currentDoctorId = doctorId;
        if (changed) {
            clearForm();
        }
        loadDiagnosisFromVisit();
    }

    public void setOnSavedCallback(Runnable onSavedCallback) {
        this.onSavedCallback = onSavedCallback;
    }

    public void setOnTreatmentSheetCallback(Runnable onTreatmentSheetCallback) {
        this.onTreatmentSheetCallback = onTreatmentSheetCallback;
    }

    private void clearForm() {
        readOnlyLocked = false;
        loadedPrimaryDiagText = "";
        chkCapSoMoi.setSelected(false);
        txtLyDoKham.setText("");
        chkDiUng.setSelected(false);
        txtTienSuDiUng.setText("");
        txtTienSuDiUng.setEnabled(false);
        txtQuaTrinhBenhLy.setText("");
        txtTrieuChung.setText("");
        txtTienSuBanThan.setText("");
        txtTienSuGiaDinh.setText("");
        txtMach.setText("0.00");
        txtNhietDo.setText("0.00");
        txtHuyetApTamThu.setText("0.00");
        txtHuyetApTamTruong.setText("0.00");
        txtSpo2.setText("0.00");
        txtNhipTho.setText("0.00");
        txtCanNang.setText("0.00");
        txtChieuCao.setText("0.00");
        txtBmi.setText("");
        txtKhamBenh.setText("");
        txtKhamBoPhan.setText("");
        if (cboChanDoanBanDau.getItemCount() > 0) cboChanDoanBanDau.setSelectedItem("");
        secondaryModel.clear();
        updateLockState(false, false);
    }

    private void loadIcdCatalog() {
        if (services == null || services.examQueue() == null) return;
        new SwingWorker<List<ICD>, Void>() {
            @Override protected List<ICD> doInBackground() {
                try { return services.examQueue().searchIcdCatalog(""); }
                catch (Exception ex) { return List.of(); }
            }
            @Override protected void done() {
                try {
                    icdCatalog = get();
                    DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
                    model.addElement("");
                    for (ICD icd : icdCatalog) {
                        model.addElement(icd.getMaICD() + " - " + icd.getTenBenh());
                    }
                    Object cur = cboChanDoanBanDau.getEditor().getItem();
                    cboChanDoanBanDau.setModel(model);
                    if (cur != null) cboChanDoanBanDau.setSelectedItem(cur);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void loadDiagnosisFromVisit() {
        if (currentVisitId <= 0 || services == null || services.examQueue() == null) return;
        final int targetVisitId = currentVisitId;
        new SwingWorker<java.util.Optional<LuotKham>, Void>() {
            @Override protected java.util.Optional<LuotKham> doInBackground() {
                try { return services.examQueue().getVisitDetail(targetVisitId); }
                catch (Exception ex) { return java.util.Optional.empty(); }
            }
            @Override protected void done() {
                if (targetVisitId != currentVisitId) return;
                try {
                    get().ifPresent(KhamBenhLamSangTheManHinh.this::applyVisitDetail);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void applyVisitDetail(LuotKham visit) {
        if (visit == null) return;
        if (visit.getLyDoKham() != null) txtLyDoKham.setText(visit.getLyDoKham());
        if (visit.getQuaTrinhBenhLy() != null) txtQuaTrinhBenhLy.setText(visit.getQuaTrinhBenhLy());
        if (visit.getTienSuBanThan() != null) txtTienSuBanThan.setText(visit.getTienSuBanThan());
        if (visit.getTienSuGiaDinh() != null) txtTienSuGiaDinh.setText(visit.getTienSuGiaDinh());
        if (visit.getKhamToanThan() != null) txtKhamBenh.setText(visit.getKhamToanThan());
        if (visit.getKhamBoPhan() != null) txtKhamBoPhan.setText(visit.getKhamBoPhan());

        if (visit.getSinhHieus() != null && !visit.getSinhHieus().isEmpty()) {
            SinhHieu latest = visit.getSinhHieus().get(visit.getSinhHieus().size() - 1);
            if (latest.getMach() != null) txtMach.setText(String.valueOf(latest.getMach()));
            if (latest.getNhietDo() != null) txtNhietDo.setText(latest.getNhietDo().toPlainString());
            if (latest.getHuyetApTamThu() != null) txtHuyetApTamThu.setText(String.valueOf(latest.getHuyetApTamThu()));
            if (latest.getHuyetApTamTruong() != null) txtHuyetApTamTruong.setText(String.valueOf(latest.getHuyetApTamTruong()));
            if (latest.getNhipTho() != null) txtNhipTho.setText(String.valueOf(latest.getNhipTho()));
            if (latest.getCanNang() != null) txtCanNang.setText(latest.getCanNang().toPlainString());
            if (latest.getChieuCao() != null) txtChieuCao.setText(latest.getChieuCao().toPlainString());
            if (latest.getSpo2() != null) txtSpo2.setText(latest.getSpo2().toPlainString());
            updateBmi();
        }

        secondaryModel.clear();
        loadedPrimaryDiagText = "";
        if (visit.getChanDoans() != null) {
            for (ChanDoan cd : visit.getChanDoans()) {
                if (cd == null || cd.isDeleted()) continue;
                String code = cd.getIcd() != null && cd.getIcd().getMaICD() != null ? cd.getIcd().getMaICD().trim() : "";
                String text = cd.getChanDoanText() != null ? cd.getChanDoanText().trim() : "";
                String display = !code.isEmpty() && !text.startsWith(code) ? code + " - " + text : text;
                if (cd.isLaChinh()) {
                    loadedPrimaryDiagText = display;
                    cboChanDoanBanDau.setSelectedItem(display);
                } else if (!display.isEmpty() && !secondaryModel.contains(display)) {
                    secondaryModel.addElement(display);
                }
            }
        }

        boolean locked = visit.isEmrLocked() || visit.getTrangThai() == TrangThaiLuotKham.HOAN_THANH;
        boolean cancelled = visit.getTrangThai() == TrangThaiLuotKham.DA_HUY;
        updateLockState(locked, cancelled);
    }

    private void updateLockState(boolean locked, boolean cancelled) {
        readOnlyLocked = locked || cancelled;
        btnSave.setEnabled(!readOnlyLocked);
        txtTienSuDiUng.setEnabled(!readOnlyLocked && chkDiUng.isSelected());
        if (cancelled) {
            lblStatus.setText("Lượt khám đã hủy.");
        } else if (locked) {
            lblStatus.setText("Bệnh án đã hoàn thành/khóa. Chuột phải lượt khám → \"Mở Lại Bệnh Án\" để sửa.");
        } else {
            lblStatus.setText("");
        }
    }

    private void handleSave() {
        PhanQuyenNguCanh auth = KhamBenhPhienNguCanh.current();
        if (services == null || currentVisitId <= 0 || auth == null) {
            JOptionPane.showMessageDialog(this, "Chưa chọn lượt khám hoặc phiên bác sĩ hợp lệ.",
                    "Không thể lưu khám bệnh", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (readOnlyLocked) {
            JOptionPane.showMessageDialog(this,
                    "Bệnh án đã hoàn thành/khóa. Vui lòng chuột phải lượt khám chọn \"Mở Lại Bệnh Án\" trước khi sửa.",
                    "Bệnh án đã khóa", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Integer mach = parseIntPositive(txtMach.getText());
        BigDecimal nhietDo = parseDecimalPositive(txtNhietDo.getText());
        Integer haThu = parseIntPositive(txtHuyetApTamThu.getText());
        Integer haTruong = parseIntPositive(txtHuyetApTamTruong.getText());
        Integer nhipTho = parseIntPositive(txtNhipTho.getText());
        BigDecimal canNang = parseDecimalPositive(txtCanNang.getText());
        BigDecimal chieuCao = parseDecimalPositive(txtChieuCao.getText());
        BigDecimal spo2 = parseDecimalPositive(txtSpo2.getText());
        boolean hasVitals = mach != null || nhietDo != null || haThu != null || haTruong != null
                || nhipTho != null || canNang != null || chieuCao != null || spo2 != null;

        String benhLy = mergeDiseaseProgress();
        String tienSuBanThan = mergePersonalHistory();
        String tienSuGiaDinh = txtTienSuGiaDinh.getText().trim();
        String khamToanThan = txtKhamBenh.getText().trim();
        String khamBoPhan = txtKhamBoPhan.getText().trim();
        String diagText = Objects.toString(cboChanDoanBanDau.getEditor().getItem(), "").trim();

        btnSave.setEnabled(false);
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                try {
                    if (services.startExam() != null) {
                        services.startExam().handle(new StartKhamCommand(currentVisitId, currentDoctorId, auth));
                    }
                    if (hasVitals && services.recordVitalSigns() != null) {
                        var vRes = services.recordVitalSigns().handle(new RecordSinhHieuDauHieuCommand(
                                currentVisitId, mach, nhietDo, haThu, haTruong, nhipTho, canNang, chieuCao, spo2, auth));
                        if (vRes != null && !vRes.isSuccess()) {
                            return vRes.getError().map(err -> err.message()).orElse("Không thể lưu sinh hiệu.");
                        }
                    }
                    if (services.updateEmrClinicalRecord() != null) {
                        var emrRes = services.updateEmrClinicalRecord().handle(new UpdateEMRLamSangRecordCommand(
                                currentVisitId, currentDoctorId, benhLy, tienSuBanThan, tienSuGiaDinh,
                                khamToanThan, khamBoPhan, null, null, null, auth));
                        if (emrRes != null && !emrRes.isSuccess()) {
                            return emrRes.getError().map(err -> err.message()).orElse("Không thể lưu thông tin khám bệnh.");
                        }
                    }
                    if (!diagText.isEmpty() && !diagText.equalsIgnoreCase(loadedPrimaryDiagText)
                            && services.addDiagnosis() != null) {
                        Integer icdId = resolveIcdId(diagText);
                        var dRes = services.addDiagnosis().handle(new AddChanDoanCommand(
                                currentVisitId, currentDoctorId, icdId, diagText, true, "SO_BO", auth));
                        if (dRes != null && !dRes.isSuccess()) {
                            return dRes.getError().map(err -> err.message()).orElse("Không thể lưu chẩn đoán ban đầu.");
                        }
                        loadedPrimaryDiagText = diagText;
                    }
                    return null;
                } catch (Exception ex) {
                    return "Lỗi khi lưu thông tin khám bệnh: " + ex.getMessage();
                }
            }

            @Override protected void done() {
                btnSave.setEnabled(!readOnlyLocked);
                try {
                    String err = get();
                    if (err != null) {
                        JOptionPane.showMessageDialog(KhamBenhLamSangTheManHinh.this, err,
                                "Không thể lưu khám bệnh", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (onSavedCallback != null) onSavedCallback.run();
                    JOptionPane.showMessageDialog(KhamBenhLamSangTheManHinh.this,
                            "Đã lưu thông tin khám bệnh vào CSDL thành công.",
                            "Lưu Khám Bệnh", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private String mergeDiseaseProgress() {
        String lyDo = txtLyDoKham.getText().trim();
        String quaTrinh = txtQuaTrinhBenhLy.getText().trim();
        String trieuChung = txtTrieuChung.getText().trim();
        StringBuilder sb = new StringBuilder();
        if (!quaTrinh.isEmpty()) sb.append(quaTrinh);
        else if (!lyDo.isEmpty()) sb.append(lyDo);
        if (!trieuChung.isEmpty() && !sb.toString().contains(trieuChung)) {
            if (!sb.isEmpty()) sb.append("\nTriệu chứng: ");
            sb.append(trieuChung);
        }
        return sb.toString().trim();
    }

    private String mergePersonalHistory() {
        String base = txtTienSuBanThan.getText().trim();
        if (chkDiUng.isSelected()) {
            String allergy = txtTienSuDiUng.getText().trim();
            String note = "Dị ứng" + (allergy.isEmpty() ? "" : ": " + allergy);
            if (!base.contains(note)) {
                return base.isEmpty() ? note : base + "\n" + note;
            }
        }
        return base;
    }

    private Integer resolveIcdId(String text) {
        if (text == null || text.isBlank() || icdCatalog == null) return null;
        String prefix = text.contains(" - ") ? text.substring(0, text.indexOf(" - ")).trim() : text.trim();
        for (ICD icd : icdCatalog) {
            if (prefix.equalsIgnoreCase(icd.getMaICD())) {
                return icd.getId();
            }
        }
        return null;
    }

    private void updateBmi() {
        BigDecimal kg = parseDecimalPositive(txtCanNang.getText());
        BigDecimal cm = parseDecimalPositive(txtChieuCao.getText());
        if (kg != null && cm != null && cm.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal m = cm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal bmi = kg.divide(m.multiply(m), 2, RoundingMode.HALF_UP);
            txtBmi.setText(bmi.toPlainString());
        } else {
            txtBmi.setText("");
        }
    }

    private void insertQuickSymptom() {
        String[] samples = {"Sốt, ho khan", "Đau họng, nuốt vướng", "Ho có đờm, sổ mũi", "Đau đầu, chóng mặt", "Đau thượng vị, buồn nôn"};
        String picked = (String) JOptionPane.showInputDialog(this, "Chọn triệu chứng mẫu:", "Triệu chứng",
                JOptionPane.PLAIN_MESSAGE, null, samples, samples[0]);
        if (picked != null) {
            String cur = txtTrieuChung.getText().trim();
            txtTrieuChung.setText(cur.isEmpty() ? picked : cur + "; " + picked);
        }
    }

    private void addSecondaryDiagnosisDialog() {
        if (icdCatalog == null || icdCatalog.isEmpty()) return;
        String[] options = icdCatalog.stream().map(i -> i.getMaICD() + " - " + i.getTenBenh()).toArray(String[]::new);
        String picked = (String) JOptionPane.showInputDialog(this, "Chọn bệnh kèm theo:", "Bệnh Kèm Theo",
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (picked != null && !secondaryModel.contains(picked)) {
            secondaryModel.addElement(picked);
        }
    }

    private void showClinicalTemplateMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem m1 = new JMenuItem("Mẫu khám hô hấp trên");
        m1.addActionListener(e -> {
            txtLyDoKham.setText("Ho, đau rát họng");
            txtQuaTrinhBenhLy.setText("Bệnh khởi phát 2 ngày nay với triệu chứng ho khan, rát họng, không sốt cao.");
            txtKhamBenh.setText("Tỉnh, tiếp xúc tốt, da niêm hồng, không phù, không xuất huyết dưới da.");
            txtKhamBoPhan.setText("Họng xung huyết nhẹ, phổi thông khí đều hai bên, chưa nghe rale.");
        });
        JMenuItem m2 = new JMenuItem("Mẫu khám tiêu hóa");
        m2.addActionListener(e -> {
            txtLyDoKham.setText("Đau bụng vùng thượng vị");
            txtQuaTrinhBenhLy.setText("Đau âm ỉ thượng vị sau ăn, kèm ợ hơi, không nôn.");
            txtKhamBenh.setText("Tỉnh, tiếp xúc tốt, sinh hiệu ổn định.");
            txtKhamBoPhan.setText("Bụng mềm, ấn tức nhẹ thượng vị, gan lách không to.");
        });
        menu.add(m1);
        menu.add(m2);
        menu.show(btnMauKhamBenh, 0, -menu.getPreferredSize().height);
    }

    private void loadNearestExamHistory() {
        if (services == null || services.examQueue() == null || currentVisitId <= 0) return;
        loadDiagnosisFromVisit();
    }

    private static Integer parseIntPositive(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            double v = Double.parseDouble(s.trim().replace(',', '.'));
            return v > 0 ? (int) Math.round(v) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static BigDecimal parseDecimalPositive(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            BigDecimal v = new BigDecimal(s.trim().replace(',', '.'));
            return v.compareTo(BigDecimal.ZERO) > 0 ? v : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static void addLabeledRow(JPanel panel, GridBagConstraints gbc, int row, String label, Component field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel lbl = new JLabel(label);
        lbl.setFont(UngDungPhongChu.BODY);
        lbl.setPreferredSize(new Dimension(128, 22));
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    private static JTextField createTextField() {
        JTextField tf = new JTextField();
        tf.setFont(UngDungPhongChu.BODY);
        tf.setPreferredSize(new Dimension(240, 24));
        tf.setBorder(new CompoundBorder(new LineBorder(new Color(195, 202, 212), 1), new EmptyBorder(2, 6, 2, 6)));
        return tf;
    }

    private static JTextArea createTextArea(int rows) {
        JTextArea ta = new JTextArea(rows, 20);
        ta.setFont(UngDungPhongChu.BODY);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBorder(new EmptyBorder(3, 6, 3, 6));
        return ta;
    }

    private static JScrollPane wrapArea(JTextArea ta, int height) {
        JScrollPane sp = new JScrollPane(ta);
        sp.setBorder(new LineBorder(new Color(195, 202, 212), 1));
        sp.setPreferredSize(new Dimension(240, height));
        return sp;
    }

    private static JLabel createVitalLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UngDungPhongChu.BODY);
        return lbl;
    }

    private static JTextField createVitalField(String initial) {
        JTextField tf = new JTextField(initial);
        tf.setFont(UngDungPhongChu.BODY);
        tf.setHorizontalAlignment(SwingConstants.RIGHT);
        tf.setPreferredSize(new Dimension(56, 22));
        tf.setBorder(new CompoundBorder(new LineBorder(new Color(195, 202, 212), 1), new EmptyBorder(1, 4, 1, 4)));
        return tf;
    }

    private static JButton createSideIconButton(String tooltip) {
        JButton btn = new JButton("✎");
        btn.setFont(UngDungPhongChu.BODY_BOLD);
        btn.setToolTipText(tooltip);
        btn.setPreferredSize(new Dimension(34, 42));
        btn.setBackground(new Color(246, 248, 252));
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(new Color(185, 195, 210), 1));
        return btn;
    }

    private static JButton createFooterButton(String text, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(UngDungPhongChu.SMALL_BOLD);
        btn.setForeground(fg);
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new CompoundBorder(new LineBorder(new Color(185, 195, 210), 1), new EmptyBorder(4, 10, 4, 10)));
        return btn;
    }
}
