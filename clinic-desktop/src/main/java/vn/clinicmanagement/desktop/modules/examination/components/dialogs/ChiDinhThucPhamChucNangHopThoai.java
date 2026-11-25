package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort;
import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.BaoCaoKetQua;
import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.FunctionalFoodConsultData;
import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.FunctionalFoodItem;
import vn.clinicmanagement.application.prescription.DonThuocMatHangDauVao;
import vn.clinicmanagement.application.prescription.prescribe.PrescribeThuocCommand;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.desktop.modules.examination.components.print.PhieuTuVanThucPhamChucNangInHopThoai;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Hộp thoại Kê / Tư vấn Thực phẩm chức năng và xem Báo cáo thống kê TPCN. */
public final class ChiDinhThucPhamChucNangHopThoai extends JPanel {

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0");

    private final KhamBenhGhepNoiGoc.Services services;
    private final NhaThuocBaoCaoReadPort reportPort;
    private final int visitId;
    private final int doctorId;
    private final String patientName;
    private final String patientCode;

    private final JComboBox<FunctionalFoodItem> cboItems = new JComboBox<>();
    private final JSpinner spQty = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    private final JTextField txtDosage = new JTextField("1 viên/lần", 12);
    private final JTextField txtFreq = new JTextField("2 lần/ngày", 12);
    private final JTextField txtGuide = new JTextField("Uống sau ăn", 16);
    private final JTextField txtAdvice = new JTextField("Uống đủ nước, nghỉ ngơi hợp lý.", 28);
    private final DefaultTableModel selectedModel = new DefaultTableModel(
            new String[]{"Mã", "Tên thực phẩm chức năng", "ĐVT", "SL", "Đơn giá", "Thành tiền", "Cách dùng"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblSelected = new JTable(selectedModel);
    private final List<FunctionalFoodItem> itemCatalog = new ArrayList<>();
    private final List<DonThuocMatHangDauVao> draftLines = new ArrayList<>();
    private final JLabel lblStatus = new JLabel("Đang tải danh mục thực phẩm chức năng...");

    public ChiDinhThucPhamChucNangHopThoai(
            KhamBenhGhepNoiGoc.Services services,
            NhaThuocBaoCaoReadPort reportPort,
            int visitId,
            int doctorId,
            String patientName,
            String patientCode
    ) {
        this.services = services;
        this.reportPort = reportPort;
        this.visitId = visitId;
        this.doctorId = doctorId;
        this.patientName = Objects.toString(patientName, "");
        this.patientCode = Objects.toString(patientCode, "");

        setLayout(new BorderLayout(0, 8));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(12, 14, 12, 14));
        setPreferredSize(new Dimension(920, 560));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        cboItems.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof FunctionalFoodItem item) {
                    setText(item.code() + " - " + item.name() + " (" + item.unit() + " | Tồn: "
                            + MONEY_FMT.format(item.stock()) + " | " + MONEY_FMT.format(item.defaultPrice()) + "đ)");
                }
                return this;
            }
        });

        loadItems();
    }

    public JComboBox<FunctionalFoodItem> getCboItems() {
        return cboItems;
    }

    public JTable getTblSelected() {
        return tblSelected;
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel title = new JLabel("CHỈ ĐỊNH / TƯ VẤN THỰC PHẨM CHỨC NĂNG");
        title.setFont(UngDungPhongChu.BODY_BOLD.deriveFont(15f));
        title.setForeground(new Color(10, 86, 179));
        JLabel sub = new JLabel("Bệnh nhân: " + patientName + " (" + patientCode + ")   |   Lượt khám #" + visitId);
        sub.setFont(UngDungPhongChu.BODY);
        p.add(title, BorderLayout.NORTH);
        p.add(sub, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);

        JPanel entry = new JPanel(new GridBagLayout());
        entry.setBackground(new Color(246, 249, 253));
        entry.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(189, 202, 215)),
                new EmptyBorder(8, 10, 8, 10)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 4, 3, 6);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; entry.add(new JLabel("Sản phẩm TPCN:"), g);
        g.gridx = 1; g.gridwidth = 4; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
        entry.add(cboItems, g);

        JButton btnAdd = new JButton("+ Thêm");
        btnAdd.addActionListener(e -> addDraftLine());
        g.gridx = 5; g.gridwidth = 1; g.fill = GridBagConstraints.NONE; g.weightx = 0;
        entry.add(btnAdd, g);

        g.gridx = 0; g.gridy = 1; entry.add(new JLabel("Số lượng:"), g);
        g.gridx = 1; entry.add(spQty, g);
        g.gridx = 2; entry.add(new JLabel("Liều dùng:"), g);
        g.gridx = 3; entry.add(txtDosage, g);
        g.gridx = 4; entry.add(new JLabel("Tần suất:"), g);
        g.gridx = 5; entry.add(txtFreq, g);

        g.gridx = 0; g.gridy = 2; entry.add(new JLabel("Hướng dẫn:"), g);
        g.gridx = 1; g.gridwidth = 2; g.fill = GridBagConstraints.HORIZONTAL;
        entry.add(txtGuide, g);
        g.gridx = 3; g.gridwidth = 1; g.fill = GridBagConstraints.NONE;
        entry.add(new JLabel("Lời dặn chung:"), g);
        g.gridx = 4; g.gridwidth = 2; g.fill = GridBagConstraints.HORIZONTAL;
        entry.add(txtAdvice, g);

        center.add(entry, BorderLayout.NORTH);

        tblSelected.setRowHeight(24);
        tblSelected.setFont(UngDungPhongChu.BODY);
        center.add(new JScrollPane(tblSelected), BorderLayout.CENTER);
        return center;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        lblStatus.setFont(UngDungPhongChu.BODY);
        footer.add(lblStatus, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        JButton btnSave = new JButton("Lưu Chỉ Định TPCN");
        JButton btnPrint = new JButton("In Phiếu Tư Vấn");
        JButton btnReport = new JButton("Báo Cáo Thống Kê TPCN");
        btnSave.addActionListener(e -> saveConsultation());
        btnPrint.addActionListener(e -> openPrintPreview());
        btnReport.addActionListener(e -> openSummaryReport());
        actions.add(btnReport);
        actions.add(btnPrint);
        actions.add(btnSave);
        footer.add(actions, BorderLayout.EAST);
        return footer;
    }

    private void loadItems() {
        var auth = KhamBenhPhienNguCanh.current();
        if (reportPort == null || auth == null) {
            lblStatus.setText("Cần phiên đăng nhập và kết nối CSDL để tải danh mục TPCN.");
            return;
        }
        new SwingWorker<List<FunctionalFoodItem>, Void>() {
            @Override
            protected List<FunctionalFoodItem> doInBackground() {
                return reportPort.listFunctionalFoodItems(auth.facilityId());
            }

            @Override
            protected void done() {
                try {
                    itemCatalog.clear();
                    cboItems.removeAllItems();
                    List<FunctionalFoodItem> list = get();
                    itemCatalog.addAll(list);
                    for (FunctionalFoodItem item : list) {
                        cboItems.addItem(item);
                    }
                    lblStatus.setText("Đã tải " + list.size() + " sản phẩm thực phẩm chức năng.");
                } catch (Exception ex) {
                    lblStatus.setText("Lỗi tải danh mục TPCN: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void addDraftLine() {
        if (!(cboItems.getSelectedItem() instanceof FunctionalFoodItem item)) {
            return;
        }
        int qty = ((Number) spQty.getValue()).intValue();
        BigDecimal q = BigDecimal.valueOf(qty);
        BigDecimal amt = item.defaultPrice().multiply(q);
        String guide = (txtDosage.getText().trim() + " - " + txtFreq.getText().trim() + " - " + txtGuide.getText().trim()).trim();

        draftLines.add(new DonThuocMatHangDauVao(
                item.id(), q, item.defaultPrice(),
                txtDosage.getText().trim(), "Uống", "30 ngày", 30,
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                txtGuide.getText().trim()));
        selectedModel.addRow(new Object[]{
                item.code(), item.name(), item.unit(), qty,
                MONEY_FMT.format(item.defaultPrice()), MONEY_FMT.format(amt), guide
        });
    }

    private void saveConsultation() {
        if (draftLines.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng thêm ít nhất 1 sản phẩm thực phẩm chức năng.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        var auth = KhamBenhPhienNguCanh.current();
        if (services == null || auth == null || visitId <= 0) {
            JOptionPane.showMessageDialog(this, "Chưa có lượt khám hoặc phiên đăng nhập hợp lệ.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int doc = doctorId > 0 ? doctorId : auth.employeeId();
        PrescribeThuocCommand cmd = new PrescribeThuocCommand(
                visitId, doc, auth.facilityId(), null,
                "Tư vấn Thực phẩm chức năng", txtAdvice.getText().trim(), true,
                List.copyOf(draftLines), auth);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                var res = KhamBenhGhepNoiGoc.prescribeWithAutoStart(services, cmd);
                return res.isSuccess() ? null : res.getError().map(e -> e.message()).orElse("Không thể lưu chỉ định TPCN.");
            }

            @Override
            protected void done() {
                try {
                    String err = get();
                    if (err != null) {
                        JOptionPane.showMessageDialog(ChiDinhThucPhamChucNangHopThoai.this, err, "Lỗi lưu TPCN", JOptionPane.ERROR_MESSAGE);
                    } else {
                        lblStatus.setText("Đã lưu chỉ định thực phẩm chức năng xuống CSDL.");
                        JOptionPane.showMessageDialog(ChiDinhThucPhamChucNangHopThoai.this, "Đã lưu chỉ định thực phẩm chức năng thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ChiDinhThucPhamChucNangHopThoai.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void openPrintPreview() {
        var auth = KhamBenhPhienNguCanh.current();
        if (reportPort == null || auth == null) return;
        new SwingWorker<FunctionalFoodConsultData, Void>() {
            @Override
            protected FunctionalFoodConsultData doInBackground() {
                return reportPort.getFunctionalFoodConsultation(visitId, auth.facilityId());
            }

            @Override
            protected void done() {
                try {
                    FunctionalFoodConsultData data = get();
                    if (data == null || data.lines().isEmpty()) {
                        JOptionPane.showMessageDialog(ChiDinhThucPhamChucNangHopThoai.this,
                                "Lượt khám này chưa có dữ liệu tư vấn Thực phẩm chức năng.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    if (!GraphicsEnvironment.isHeadless()) {
                        new PhieuTuVanThucPhamChucNangInHopThoai(
                                SwingUtilities.getWindowAncestor(ChiDinhThucPhamChucNangHopThoai.this), data).setVisible(true);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ChiDinhThucPhamChucNangHopThoai.this, "Lỗi tải phiếu tư vấn: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void openSummaryReport() {
        var auth = KhamBenhPhienNguCanh.current();
        if (reportPort == null || auth == null) return;
        LocalDateTime from = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime to = LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 59)).plusMinutes(1);
        new SwingWorker<BaoCaoKetQua, Void>() {
            @Override
            protected BaoCaoKetQua doInBackground() {
                return reportPort.getFunctionalFoodReport(auth.facilityId(), from, to);
            }

            @Override
            protected void done() {
                try {
                    BaoCaoKetQua res = get();
                    var model = new DefaultTableModel(res.columns().toArray(), 0) {
                        @Override public boolean isCellEditable(int r, int c) { return false; }
                    };
                    for (var row : res.rows()) model.addRow(row.toArray());
                    JTable tbl = new JTable(model);
                    tbl.setRowHeight(24);
                    tbl.setAutoCreateRowSorter(true);
                    JPanel p = new JPanel(new BorderLayout(0, 6));
                    p.setPreferredSize(new Dimension(980, 480));
                    p.add(new JLabel(res.title() + "   —   " + res.summaryText()), BorderLayout.NORTH);
                    p.add(new JScrollPane(tbl), BorderLayout.CENTER);
                    JOptionPane.showMessageDialog(ChiDinhThucPhamChucNangHopThoai.this, p, "Báo Cáo Thống Kê Thực Phẩm Chức Năng", JOptionPane.PLAIN_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ChiDinhThucPhamChucNangHopThoai.this, "Không thể tải báo cáo TPCN: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
