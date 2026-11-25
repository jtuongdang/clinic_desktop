package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.examination.add.AddChanDoanCommand;
import vn.clinicmanagement.application.examination.complete.CompleteKhamCommand;
import vn.clinicmanagement.application.examination.transfer.TransferLuotKhamCommand;
import vn.clinicmanagement.application.examination.update.UpdateEMRLamSangRecordCommand;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.RaVienTongHopHopThoai;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.ICDDanhMucLuaChonHopThoai;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.ThuPhuChanDoanHopThoai;
import vn.clinicmanagement.desktop.modules.print.examination.TheoTiepLichHenPrintHopThoai;
import vn.clinicmanagement.domain.common.enums.HinhThucDieuTri;
import vn.clinicmanagement.domain.common.enums.HuongXuTri;
import vn.clinicmanagement.domain.common.enums.KetQuaDieuTri;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.ChanDoan;
import vn.clinicmanagement.domain.examination.ICD;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.examination.TransferLyDoMa;
import vn.clinicmanagement.domain.facility.PhongKham;

import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Giao diện tab "Xử Trí" trong phân hệ Khám Bệnh.
 * Khớp 100% chi tiết trong ảnh chụp thực tế media_1793373144739_058baf99.png:
 * 1. Khung trên bên trái:
 *    - Tiêu đề: "Chẩn đoán xác định" (trái), link xanh "Chọn chẩn đoán" (phải)
 *    - Ô nhập đa dòng text area lớn viền mảnh
 * 2. Khung trên bên phải:
 *    - Hàng 1: "Bệnh chính - ICD10" -> Ô mã ICD10 (A02), Ô tên bệnh (Nhiễm salmonella khác) có nút tìm kiếm/xổ
 *    - Hàng 2: "Bệnh kèm theo - ICD10" -> Tiêu đề và link xanh "Cập nhật chẩn đoán"
 *    - Danh sách bệnh kèm theo: Dòng 1 chọn nổi bật nền xanh đậm (A02 - Nhiễm salmonella khác)
 *    - Cột nút thao tác bên phải: Nút "Bệnh kèm Theo" với biểu tượng sổ bệnh án, nút tài liệu
 * 3. Khung dưới:
 *    - Hàng 1: Thời gian xử trí ( :   /  /     ▾), Kết quả điều trị (Combobox ▾)
 *    - Hàng 2: Hình thức xử trí (Combobox ▾)
 *    - Hàng 3: Lời dặn của bác sĩ -> Text area lớn
 *    - Nút [💾 Lưu] góc phải dưới cùng
 */
public class DieuTriXuTriTheManHinh extends JPanel {

    private JTextArea txtChanDoanXacDinh;
    private JTextField txtMaIcdChinh;
    private JTextField txtTenIcdChinh;
    private DefaultListModel<String> secondaryIcdModel;
    private JList<String> listSecondaryIcd;
    private JTextArea txtTrieuChung;
    private JTextArea txtKhamLamSang;
    private JTextField txtThoiGianXuTri;
    private JTextField txtNgayHenTaiKham;
    private JTextField txtSoNgayTaiKham;
    private LocalDate selectedReexamDate;
    private boolean updatingReexamFields = false;
    private boolean suppressDispositionDialog = false;
    private Runnable onDispositionSavedCallback;
    private JComboBox<String> cboKetQuaDieuTri;
    private JComboBox<String> cboHinhThucXuTri;
    private JPanel pnlKhoaDieuTri;
    private JPanel pnlSoNgayNghi;
    private JTextField txtSoNgayNghiBhxh;
    private JPanel pnlInstructionsSpacer;
    private JLabel lblKhoa;
    private JComboBox<String> cboKhoaDieuTri;
    private final List<PhongKham> transferRoomOptions = new ArrayList<>();
    private JTextArea txtLoiDanBacSi;
    private JButton btnSave;
    private JLabel lblSaveStatus;

    private final KhamBenhGhepNoiGoc.Services services;
    private int currentVisitId = 1;
    private int currentDoctorId = 1;
    private boolean currentVisitCompleted = false;
    private boolean currentVisitEmrLocked = false;
    private boolean currentVisitCancelled = false;

    public static final String[] INPATIENT_DEPARTMENTS = {
            "Gây mê hồi sức",
            "Khoa Bệnh nhiệt đới - Tiêu hóa nhi",
            "Khoa Hỗ trợ sinh sản - nam học",
            "Khoa Hồi sức tích cực",
            "Khoa khám bệnh",
            "Khoa chuyển khoa",
            "Khoa lưu chuyển khoa",
            "Khoa Lưu",
            "Khoa Ngoại nhi",
            "Khoa nội nhi tổng hợp",
            "Khoa phụ",
            "Khoa sản bệnh",
            "Khoa Lân tinh",
            "Khoa Sơ sinh",
            "Ngoại trú RHM",
            "Ngoại trú tâm thần"
    };

    public static final String[] OUTPATIENT_DEPARTMENTS = {
            "Khoa Da liễu",
            "Khoa Điều trị Covid",
            "Khoa Hồi sức cấp cứu",
            "Khoa Mắt",
            "Khoa Ngoại tổng hợp",
            "Khoa Nhi",
            "Khoa Nội",
            "Khoa Phẫu thuật - Gây mê hồi sức",
            "Khoa Răng - Hàm - Mặt",
            "Khoa Sản",
            "Khoa Tai - Mũi - Họng",
            "Khoa Thận nhân tạo",
            "Khoa Truyền nhiễm",
            "Khoa Y học cổ truyền"
    };

    public DieuTriXuTriTheManHinh() {
        this(resolveDefaultServices());
    }

    public DieuTriXuTriTheManHinh(KhamBenhGhepNoiGoc.Services services) {
        this.services = services != null ? services : resolveDefaultServices();
        initUI();
    }

    private static KhamBenhGhepNoiGoc.Services resolveDefaultServices() {
        try {
            return KhamBenhGhepNoiGoc.createDefault();
        } catch (Exception ex) {
            return null;
        }
    }

    public void setVisitContext(int visitId, int doctorId) {
        boolean visitChanged = this.currentVisitId != visitId;
        this.currentVisitId = visitId;
        this.currentDoctorId = doctorId;
        if (txtThoiGianXuTri != null) {
            txtThoiGianXuTri.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
        }
        if (visitChanged) {
            currentVisitCompleted = false;
            currentVisitEmrLocked = false;
            currentVisitCancelled = false;
            updateSaveButtonState();
            setReexamDate(null);
            if (secondaryIcdModel != null) secondaryIcdModel.clear();
            if (txtTrieuChung != null) {
                txtTrieuChung.setText("Triệu chứng ...");
                txtTrieuChung.setForeground(new Color(120, 130, 145));
            }
            if (txtKhamLamSang != null) {
                txtKhamLamSang.setText("Khám lâm sàng ...");
                txtKhamLamSang.setForeground(new Color(120, 130, 145));
            }
            if (txtSoNgayNghiBhxh != null) txtSoNgayNghiBhxh.setText("0");
            suppressDispositionDialog = true;
            try {
                if (cboKetQuaDieuTri != null) cboKetQuaDieuTri.setSelectedItem("Không thay đổi");
                if (cboHinhThucXuTri != null) cboHinhThucXuTri.setSelectedItem("Cấp toa cho về");
                if (txtLoiDanBacSi != null) txtLoiDanBacSi.setText("Cấp toa cho về");
            } finally {
                suppressDispositionDialog = false;
            }
        }
        loadVisitDetailFromDatabase(visitId);
    }

    private void loadVisitDetailFromDatabase(int visitId) {
        if (visitId <= 0 || services == null || services.examQueue() == null) return;
        new SwingWorker<java.util.Optional<LuotKham>, Void>() {
            @Override
            protected java.util.Optional<LuotKham> doInBackground() {
                try {
                    return services.examQueue().getVisitDetail(visitId);
                } catch (Exception ex) {
                    return java.util.Optional.empty();
                }
            }

            @Override
            protected void done() {
                if (visitId != currentVisitId) return;
                try {
                    get().ifPresent(DieuTriXuTriTheManHinh.this::applyVisitDetail);
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    /** Nạp dữ liệu xử trí, lâm sàng và bệnh kèm theo đã lưu của `LuotKham` lên tab Xử Trí. */
    public void applyVisitDetail(LuotKham visit) {
        if (visit == null) return;
        if (visit.getChanDoans() != null && secondaryIcdModel != null) {
            secondaryIcdModel.clear();
            for (ChanDoan cd : visit.getChanDoans()) {
                if (cd == null || cd.isDeleted()) continue;
                if ("KEM_THEO".equalsIgnoreCase(cd.getLoaiChanDoan()) && !cd.isLaChinh()) {
                    String code = cd.getIcd() != null && cd.getIcd().getMaICD() != null ? cd.getIcd().getMaICD().trim() : "";
                    String text = cd.getChanDoanText() != null ? cd.getChanDoanText().trim() : "";
                    String display = !code.isEmpty() && !text.isEmpty() ? code + " - " + text : text;
                    if (!display.isEmpty() && !secondaryIcdModel.contains(display)) {
                        secondaryIcdModel.addElement(display);
                    }
                }
            }
            if (secondaryIcdModel.getSize() > 0 && listSecondaryIcd != null) {
                listSecondaryIcd.setSelectedIndex(0);
            }
        }
        if (txtTrieuChung != null && visit.getQuaTrinhBenhLy() != null && !visit.getQuaTrinhBenhLy().isBlank()) {
            txtTrieuChung.setText(visit.getQuaTrinhBenhLy().trim());
            txtTrieuChung.setForeground(new Color(25, 30, 40));
        }
        if (txtKhamLamSang != null && visit.getKhamBoPhan() != null && !visit.getKhamBoPhan().isBlank()) {
            txtKhamLamSang.setText(visit.getKhamBoPhan().trim());
            txtKhamLamSang.setForeground(new Color(25, 30, 40));
        }
        if (txtThoiGianXuTri != null && visit.getThoiGianKetThucKham() != null) {
            txtThoiGianXuTri.setText(visit.getThoiGianKetThucKham().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
        }
        if (txtLoiDanBacSi != null && visit.getLoiDanBacSi() != null && !visit.getLoiDanBacSi().isBlank()) {
            txtLoiDanBacSi.setText(visit.getLoiDanBacSi().trim());
        }
        if (visit.getNgayHenTaiKham() != null) {
            setReexamDate(visit.getNgayHenTaiKham());
        }
        if (txtSoNgayNghiBhxh != null && visit.getSoNgayNghiHuongBHXH() != null) {
            txtSoNgayNghiBhxh.setText(String.valueOf(visit.getSoNgayNghiHuongBHXH()));
        }
        suppressDispositionDialog = true;
        try {
            if (cboKetQuaDieuTri != null && visit.getKetQuaDieuTri() != null) {
                String kqLabel = switch (visit.getKetQuaDieuTri()) {
                    case KHOI -> "Khỏi";
                    case DO_GIAM -> "Đỡ, giảm";
                    case KHONG_THAY_DOI -> "Không thay đổi";
                    case NANG_HON -> "Nặng hơn";
                    case TU_VONG -> "Tử vong";
                };
                cboKetQuaDieuTri.setSelectedItem(kqLabel);
            }
            if (cboHinhThucXuTri != null && visit.getHuongXuTri() != null) {
                String hxtLabel = switch (visit.getHuongXuTri()) {
                    case CHO_VE_TOA, HEN_TAI_KHAM -> (visit.getHuongDieuTri() != null
                            && visit.getHuongDieuTri().startsWith("Điều trị ngoại trú"))
                            ? "Điều trị ngoại trú" : "Cấp toa cho về";
                    case NHAP_VIEN_NOI_TRU -> "Nhập viện";
                    case CHUYEN_TUYEN_TREN -> "Chuyển tuyến";
                    case CHUYEN_CHUYEN_KHOA -> "Chuyển phòng khám";
                    case BO_KHAM -> "Bỏ khám";
                    case TU_VONG -> "Tử vong";
                    case CAP_GIAY_TO -> "CẤP GIẤY TỜ";
                };
                cboHinhThucXuTri.setSelectedItem(hxtLabel);
                if (visit.getHuongDieuTri() != null && cboKhoaDieuTri != null) {
                    String prefixOut = "Điều trị ngoại trú - ";
                    String prefixIn = "Nhập viện - ";
                    if (visit.getHuongDieuTri().startsWith(prefixOut)) {
                        cboKhoaDieuTri.setSelectedItem(visit.getHuongDieuTri().substring(prefixOut.length()));
                    } else if (visit.getHuongDieuTri().startsWith(prefixIn)) {
                        cboKhoaDieuTri.setSelectedItem(visit.getHuongDieuTri().substring(prefixIn.length()));
                    }
                }
            }
        } finally {
            suppressDispositionDialog = false;
        }
        currentVisitCompleted = visit.getTrangThai() == TrangThaiLuotKham.HOAN_THANH;
        currentVisitEmrLocked = visit.isEmrLocked();
        currentVisitCancelled = visit.getTrangThai() == TrangThaiLuotKham.DA_HUY;
        updateSaveButtonState();
    }

    public void setOnDispositionSavedCallback(Runnable callback) {
        this.onDispositionSavedCallback = callback;
    }

    public KhamBenhGhepNoiGoc.Services getServices() {
        return services;
    }

    public int getCurrentVisitId() {
        return currentVisitId;
    }

    public int getCurrentDoctorId() {
        return currentDoctorId;
    }

    public JButton getBtnSave() {
        return btnSave;
    }

    public JTextArea getTxtChanDoanXacDinh() {
        return txtChanDoanXacDinh;
    }

    public JTextField getTxtMaIcdChinh() {
        return txtMaIcdChinh;
    }

    public JTextField getTxtTenIcdChinh() {
        return txtTenIcdChinh;
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 8));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(4, 4, 4, 4));

        // Phân tách 2 khu vực: Nửa trên (Chẩn đoán & ICD10) và Nửa dưới (Phương án xử trí & Lời dặn)
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(Color.WHITE);

        mainContent.add(buildTopDiagnosisSection());
        mainContent.add(Box.createVerticalStrut(8));
        mainContent.add(buildBottomDispositionSection());
        mainContent.add(Box.createVerticalGlue());

        add(mainContent, BorderLayout.CENTER);
        add(buildFooterActions(), BorderLayout.SOUTH);
    }

    /**
     * Nửa trên: Gồm 2 cột (Chẩn đoán xác định ~30%, ICD-10 chính & kèm theo ~70%)
     * Căn chỉnh khớp 1:1 theo ảnh thực tế media_1793376204973_a6f871f0.png
     */
    private JPanel buildTopDiagnosisSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel diagnosisPanel = buildDefinitiveDiagnosisPanel();
        JPanel icdPanel = buildIcd10SectionPanel();
        diagnosisPanel.setMinimumSize(new Dimension(0, 0));
        icdPanel.setMinimumSize(new Dimension(0, 0));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, diagnosisPanel, icdPanel);
        splitPane.setBorder(null);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerSize(4);
        splitPane.setResizeWeight(0.30);
        splitPane.setMinimumSize(new Dimension(0, 0));
        splitPane.addComponentListener(new ComponentAdapter() {
            private boolean initialized;

            @Override
            public void componentResized(ComponentEvent e) {
                if (!initialized && splitPane.getWidth() > 0) {
                    splitPane.setDividerLocation(0.30);
                    initialized = true;
                }
            }
        });
        panel.add(splitPane, BorderLayout.CENTER);

        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 237));
        panel.setPreferredSize(new Dimension(1000, 237));

        return panel;
    }

    private JPanel buildDefinitiveDiagnosisPanel() {
        JPanel pnl = new JPanel(new BorderLayout(0, 4));
        pnl.setBackground(Color.WHITE);

        // Tiêu đề hàng: "Chẩn đoán xác định" bên trái, link xanh "Chọn chẩn đoán" bên phải
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("Chẩn đoán xác định");
        lblTitle.setFont(UngDungPhongChu.SMALL);
        lblTitle.setForeground(new Color(25, 30, 40));

        JLabel lblSelectDiag = new JLabel("Chọn chẩn đoán");
        lblSelectDiag.setFont(UngDungPhongChu.SMALL);
        lblSelectDiag.setForeground(new Color(0, 102, 204));
        lblSelectDiag.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblSelectDiag.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openSelectDiagnosisPopup(lblSelectDiag);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                lblSelectDiag.setText("<html><u>Chọn chẩn đoán</u></html>");
            }
            @Override
            public void mouseExited(MouseEvent e) {
                lblSelectDiag.setText("Chọn chẩn đoán");
            }
        });

        header.add(lblTitle, BorderLayout.WEST);
        header.add(lblSelectDiag, BorderLayout.EAST);
        pnl.add(header, BorderLayout.NORTH);

        // Vùng nhập văn bản chẩn đoán xác định
        txtChanDoanXacDinh = new JTextArea();
        txtChanDoanXacDinh.setFont(UngDungPhongChu.SMALL);
        txtChanDoanXacDinh.setLineWrap(true);
        txtChanDoanXacDinh.setWrapStyleWord(true);
        txtChanDoanXacDinh.setBorder(new EmptyBorder(4, 6, 4, 6));

        JScrollPane scroll = new JScrollPane(txtChanDoanXacDinh);
        scroll.setBorder(new LineBorder(new Color(195, 205, 220), 1));
        scroll.setPreferredSize(new Dimension(280, 220));
        pnl.add(scroll, BorderLayout.CENTER);

        return pnl;
    }

    private JPanel buildIcd10SectionPanel() {
        JPanel pnl = new JPanel(new BorderLayout(0, 6));
        pnl.setBackground(Color.WHITE);

        // 1. Phần Bệnh chính - ICD10 (Khớp media_1793376950126_1a3e6673.png)
        JPanel mainIcdPnl = new JPanel(new BorderLayout(0, 3));
        mainIcdPnl.setBackground(Color.WHITE);

        JLabel lblMainIcd = new JLabel("Bệnh chính - ICD10");
        lblMainIcd.setFont(UngDungPhongChu.SMALL);
        lblMainIcd.setForeground(new Color(25, 30, 40));
        mainIcdPnl.add(lblMainIcd, BorderLayout.NORTH);

        JPanel mainIcdInputs = new JPanel(new BorderLayout(4, 0));
        mainIcdInputs.setBackground(Color.WHITE);

        JPanel codeContainer = new JPanel(new BorderLayout());
        codeContainer.setBackground(Color.WHITE);
        codeContainer.setBorder(new LineBorder(new Color(185, 195, 210), 1));
        codeContainer.setPreferredSize(new Dimension(78, 24));

        txtMaIcdChinh = new JTextField();
        txtMaIcdChinh.setFont(UngDungPhongChu.SMALL_BOLD);
        txtMaIcdChinh.setHorizontalAlignment(SwingConstants.CENTER);
        txtMaIcdChinh.setBorder(new EmptyBorder(2, 4, 2, 2));
        txtMaIcdChinh.addActionListener(e -> openIcdCatalogDialog(true));

        JButton btnIcdDots = new JButton("...");
        btnIcdDots.setFont(UngDungPhongChu.SMALL_BOLD);
        btnIcdDots.setPreferredSize(new Dimension(22, 22));
        btnIcdDots.setBackground(new Color(245, 247, 250));
        btnIcdDots.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(205, 214, 226)));
        btnIcdDots.setFocusPainted(false);
        btnIcdDots.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnIcdDots.addActionListener(e -> openIcdCatalogDialog(true));

        codeContainer.add(txtMaIcdChinh, BorderLayout.CENTER);
        codeContainer.add(btnIcdDots, BorderLayout.EAST);

        JPanel nameContainer = new JPanel(new BorderLayout());
        nameContainer.setBackground(Color.WHITE);
        nameContainer.setBorder(new LineBorder(new Color(185, 195, 210), 1));

        txtTenIcdChinh = new JTextField();
        txtTenIcdChinh.setFont(UngDungPhongChu.SMALL_BOLD);
        txtTenIcdChinh.setBorder(new EmptyBorder(2, 6, 2, 4));
        txtTenIcdChinh.addActionListener(e -> openIcdCatalogDialog(true));

        JButton btnSearchMainIcd = new JButton("▾");
        btnSearchMainIcd.setFont(UngDungPhongChu.SMALL);
        btnSearchMainIcd.setPreferredSize(new Dimension(18, 22));
        btnSearchMainIcd.setBackground(new Color(245, 247, 250));
        btnSearchMainIcd.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(205, 214, 226)));
        btnSearchMainIcd.setFocusPainted(false);
        btnSearchMainIcd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSearchMainIcd.addActionListener(e -> openIcdCatalogDialog(true));

        nameContainer.add(txtTenIcdChinh, BorderLayout.CENTER);
        nameContainer.add(btnSearchMainIcd, BorderLayout.EAST);

        mainIcdInputs.add(codeContainer, BorderLayout.WEST);
        mainIcdInputs.add(nameContainer, BorderLayout.CENTER);
        mainIcdPnl.add(mainIcdInputs, BorderLayout.CENTER);

        // 2. Container giữa: Bệnh kèm theo + Triệu chứng + Khám lâm sàng + Nút Bệnh kèm Theo
        JPanel middlePnl = new JPanel(new BorderLayout(4, 0));
        middlePnl.setBackground(Color.WHITE);

        JPanel fieldsCol = new JPanel(new GridLayout(3, 1, 0, 4));
        fieldsCol.setBackground(Color.WHITE);

        // 2a. Bệnh kèm theo - ICD10
        JPanel pnlSec = new JPanel(new BorderLayout(0, 2));
        pnlSec.setBackground(Color.WHITE);

        JPanel secHeader = new JPanel(new BorderLayout());
        secHeader.setBackground(Color.WHITE);

        JLabel lblSecIcd = new JLabel("Bệnh kèm theo - ICD10");
        lblSecIcd.setFont(UngDungPhongChu.SMALL);
        lblSecIcd.setForeground(new Color(25, 30, 40));

        JLabel lblUpdateDiag = new JLabel("Cập nhật chẩn đoán");
        lblUpdateDiag.setFont(UngDungPhongChu.SMALL);
        lblUpdateDiag.setForeground(new Color(0, 102, 204));
        lblUpdateDiag.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblUpdateDiag.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                syncDiagnosesToText();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                lblUpdateDiag.setText("<html><u>Cập nhật chẩn đoán</u></html>");
            }
            @Override
            public void mouseExited(MouseEvent e) {
                lblUpdateDiag.setText("Cập nhật chẩn đoán");
            }
        });

        secHeader.add(lblSecIcd, BorderLayout.WEST);
        secHeader.add(lblUpdateDiag, BorderLayout.EAST);

        secondaryIcdModel = new DefaultListModel<>();

        listSecondaryIcd = new JList<>(secondaryIcdModel);
        listSecondaryIcd.setFont(UngDungPhongChu.SMALL_BOLD);
        listSecondaryIcd.setForeground(new Color(0, 51, 153));
        listSecondaryIcd.setBackground(Color.WHITE);
        listSecondaryIcd.setBorder(new EmptyBorder(1, 4, 1, 4));

        JScrollPane secScroll = new JScrollPane(listSecondaryIcd);
        secScroll.setBorder(new LineBorder(new Color(195, 205, 220), 1));
        secScroll.setPreferredSize(new Dimension(1, 22));
        secScroll.setMinimumSize(new Dimension(1, 22));
        secScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        pnlSec.setLayout(new BoxLayout(pnlSec, BoxLayout.Y_AXIS));
        secHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, secHeader.getPreferredSize().height));
        pnlSec.add(secHeader);
        pnlSec.add(secScroll);
        pnlSec.add(Box.createVerticalGlue());
        fieldsCol.add(pnlSec);

        // 2b. Triệu chứng:
        JPanel pnlTc = new JPanel(new BorderLayout(0, 2));
        pnlTc.setBackground(Color.WHITE);

        JLabel lblTrieuChung = new JLabel("Triệu chứng:");
        lblTrieuChung.setFont(UngDungPhongChu.SMALL);
        lblTrieuChung.setForeground(new Color(25, 30, 40));

        txtTrieuChung = new JTextArea("Triệu chứng ...");
        txtTrieuChung.setFont(UngDungPhongChu.SMALL);
        txtTrieuChung.setForeground(new Color(120, 130, 145));
        txtTrieuChung.setLineWrap(true);
        txtTrieuChung.setWrapStyleWord(true);
        txtTrieuChung.setBorder(new EmptyBorder(2, 4, 2, 4));

        JScrollPane scrollTrieuChung = new JScrollPane(txtTrieuChung);
        scrollTrieuChung.setBorder(new LineBorder(new Color(195, 205, 220), 1));
        scrollTrieuChung.setPreferredSize(new Dimension(1, 22));
        scrollTrieuChung.setMinimumSize(new Dimension(1, 22));
        scrollTrieuChung.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        pnlTc.setLayout(new BoxLayout(pnlTc, BoxLayout.Y_AXIS));
        lblTrieuChung.setMaximumSize(new Dimension(Integer.MAX_VALUE, lblTrieuChung.getPreferredSize().height));
        pnlTc.add(lblTrieuChung);
        pnlTc.add(scrollTrieuChung);
        pnlTc.add(Box.createVerticalGlue());
        fieldsCol.add(pnlTc);

        // 2c. Khám lâm sàng:
        JPanel pnlKls = new JPanel(new BorderLayout(0, 2));
        pnlKls.setBackground(Color.WHITE);

        JLabel lblKhamLamSang = new JLabel("Khám lâm sàng:");
        lblKhamLamSang.setFont(UngDungPhongChu.SMALL);
        lblKhamLamSang.setForeground(new Color(25, 30, 40));

        txtKhamLamSang = new JTextArea("Khám lâm sàng ...");
        txtKhamLamSang.setFont(UngDungPhongChu.SMALL);
        txtKhamLamSang.setForeground(new Color(120, 130, 145));
        txtKhamLamSang.setLineWrap(true);
        txtKhamLamSang.setWrapStyleWord(true);
        txtKhamLamSang.setBorder(new EmptyBorder(2, 4, 2, 4));

        JScrollPane scrollKhamLamSang = new JScrollPane(txtKhamLamSang);
        scrollKhamLamSang.setBorder(new LineBorder(new Color(195, 205, 220), 1));
        scrollKhamLamSang.setPreferredSize(new Dimension(1, 22));
        scrollKhamLamSang.setMinimumSize(new Dimension(1, 22));
        scrollKhamLamSang.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        pnlKls.setLayout(new BoxLayout(pnlKls, BoxLayout.Y_AXIS));
        lblKhamLamSang.setMaximumSize(new Dimension(Integer.MAX_VALUE, lblKhamLamSang.getPreferredSize().height));
        pnlKls.add(lblKhamLamSang);
        pnlKls.add(scrollKhamLamSang);
        pnlKls.add(Box.createVerticalGlue());
        fieldsCol.add(pnlKls);

        middlePnl.add(fieldsCol, BorderLayout.CENTER);

        // 2d. Cột nút bên phải: Khớp 1:1 media_1793376950126_1a3e6673.png nút [Bệnh kèm Theo]
        JPanel actionCol = new JPanel();
        actionCol.setLayout(new BoxLayout(actionCol, BoxLayout.Y_AXIS));
        actionCol.setBackground(Color.WHITE);
        actionCol.setPreferredSize(new Dimension(58, 184));
        actionCol.add(Box.createVerticalStrut(11));
        JButton btnAddSec = new JButton("<html><center>Bệnh<br>kèm Theo</center></html>", new DocumentSheetIcon());
        btnAddSec.setFont(UngDungPhongChu.SMALL);
        btnAddSec.setForeground(new Color(30, 35, 45));
        btnAddSec.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnAddSec.setHorizontalTextPosition(SwingConstants.CENTER);
        btnAddSec.setBackground(new Color(246, 248, 252));
        btnAddSec.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(195, 205, 220), 1),
                new EmptyBorder(6, 2, 6, 2)
        ));
        btnAddSec.setFocusPainted(false);
        btnAddSec.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAddSec.setMaximumSize(new Dimension(58, 136));
        btnAddSec.setPreferredSize(new Dimension(58, 136));
        btnAddSec.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnAddSec.addActionListener(e -> openSecondaryDiagnosisDialog());

        actionCol.add(btnAddSec);
        actionCol.add(Box.createVerticalStrut(12));

        JButton btnOpenSecondaryDiagnosis = new JButton(new DiagnosisFolderIcon());
        btnOpenSecondaryDiagnosis.setToolTipText("Mở danh mục bệnh kèm theo");
        btnOpenSecondaryDiagnosis.getAccessibleContext().setAccessibleName("Mở danh mục bệnh kèm theo");
        btnOpenSecondaryDiagnosis.setBackground(new Color(246, 248, 252));
        btnOpenSecondaryDiagnosis.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(195, 205, 220), 1),
                new EmptyBorder(3, 2, 3, 2)
        ));
        btnOpenSecondaryDiagnosis.setFocusPainted(false);
        btnOpenSecondaryDiagnosis.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOpenSecondaryDiagnosis.setMaximumSize(new Dimension(58, 30));
        btnOpenSecondaryDiagnosis.setPreferredSize(new Dimension(58, 30));
        btnOpenSecondaryDiagnosis.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnOpenSecondaryDiagnosis.addActionListener(e -> openSecondaryDiagnosisDialog());
        actionCol.add(btnOpenSecondaryDiagnosis);

        middlePnl.add(actionCol, BorderLayout.EAST);

        pnl.add(mainIcdPnl, BorderLayout.NORTH);
        pnl.add(middlePnl, BorderLayout.CENTER);

        return pnl;
    }

    /**
     * Nửa dưới: Thời gian xử trí, Kết quả điều trị, Hình thức xử trí, Lời dặn của bác sĩ
     */
    private JPanel buildBottomDispositionSection() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBackground(Color.WHITE);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 0, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        // --- HÀNG 1: Thời gian xử trí & Kết quả điều trị ---
        // Cụm Thời gian xử trí
        JPanel pnlThoiGian = new JPanel(new BorderLayout(0, 2));
        pnlThoiGian.setBackground(Color.WHITE);
        pnlThoiGian.setPreferredSize(new Dimension(152, 40));
        pnlThoiGian.setMinimumSize(new Dimension(152, 40));
        pnlThoiGian.setMaximumSize(new Dimension(152, 40));
        JLabel lblThoiGian = new JLabel("Thời gian xử trí");
        lblThoiGian.setFont(UngDungPhongChu.SMALL);
        lblThoiGian.setForeground(new Color(30, 35, 45));

        JPanel thoiGianInput = new JPanel(new BorderLayout());
        thoiGianInput.setBackground(Color.WHITE);
        thoiGianInput.setBorder(new LineBorder(new Color(185, 195, 210), 1));
        thoiGianInput.setPreferredSize(new Dimension(152, 24));

        txtThoiGianXuTri = new JTextField("09:34 23/08/2022");
        txtThoiGianXuTri.setFont(UngDungPhongChu.SMALL);
        txtThoiGianXuTri.setBorder(new EmptyBorder(2, 4, 2, 4));

        JButton btnCal = new JButton("▾");
        btnCal.setFont(UngDungPhongChu.SMALL);
        btnCal.setPreferredSize(new Dimension(18, 22));
        btnCal.setBackground(new Color(245, 247, 250));
        btnCal.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(205, 214, 226)));
        btnCal.setFocusPainted(false);
        btnCal.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCal.addActionListener(e -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy");
            txtThoiGianXuTri.setText(sdf.format(new Date()));
        });

        thoiGianInput.add(txtThoiGianXuTri, BorderLayout.CENTER);
        thoiGianInput.add(btnCal, BorderLayout.EAST);

        pnlThoiGian.add(lblThoiGian, BorderLayout.NORTH);
        pnlThoiGian.add(thoiGianInput, BorderLayout.CENTER);

        c.gridx = 0; c.gridy = 0; c.weightx = 0.0;
        pnl.add(pnlThoiGian, c);

        // Cụm Kết quả điều trị
        JPanel pnlKetQua = new JPanel(new BorderLayout(0, 2));
        pnlKetQua.setBackground(Color.WHITE);
        pnlKetQua.setPreferredSize(new Dimension(152, 40));
        pnlKetQua.setMinimumSize(new Dimension(152, 40));
        pnlKetQua.setMaximumSize(new Dimension(152, 40));
        JLabel lblKetQua = new JLabel("Kết quả điều trị");
        lblKetQua.setFont(UngDungPhongChu.SMALL);
        lblKetQua.setForeground(new Color(30, 35, 45));

        cboKetQuaDieuTri = new JComboBox<>(new String[]{"", "Khỏi", "Đỡ, giảm", "Không thay đổi", "Nặng hơn", "Tử vong"});
        cboKetQuaDieuTri.setFont(UngDungPhongChu.SMALL);
        cboKetQuaDieuTri.setBackground(Color.WHITE);
        cboKetQuaDieuTri.setPreferredSize(new Dimension(152, 24));
        cboKetQuaDieuTri.setSelectedItem("Không thay đổi");

        pnlKetQua.add(lblKetQua, BorderLayout.NORTH);
        pnlKetQua.add(cboKetQuaDieuTri, BorderLayout.CENTER);

        c.gridx = 1; c.gridy = 0; c.weightx = 0.0;
        pnl.add(pnlKetQua, c);

        // Cụm Hẹn tái khám (Ngày tái khám + Sau N ngày -> lưu xuống LuotKham.ngayHenTaiKham)
        JPanel pnlHenTaiKham = buildReexamControlPanel();
        c.gridx = 2; c.gridy = 0; c.weightx = 1.0;
        pnl.add(pnlHenTaiKham, c);

        // --- HÀNG 2: Hình thức xử trí (Trái) & Tiến khoa / Ghi chú xử trí (Phải) ---
        JPanel pnlHinhThuc = new JPanel(new BorderLayout(0, 2));
        pnlHinhThuc.setBackground(Color.WHITE);
        pnlHinhThuc.setPreferredSize(new Dimension(152, 40));
        pnlHinhThuc.setMinimumSize(new Dimension(152, 40));
        pnlHinhThuc.setMaximumSize(new Dimension(152, 40));
        JLabel lblHinhThuc = new JLabel("Hình thức xử trí");
        lblHinhThuc.setFont(UngDungPhongChu.SMALL);
        lblHinhThuc.setForeground(new Color(30, 35, 45));

        // Danh mục hình thức xử trí chính xác tuyệt đối theo yêu cầu
        String[] dispositionOptions = {
                "Cấp toa cho về",
                "Chuyển phòng khám",
                "Điều trị ngoại trú",
                "Nhập viện",
                "Bỏ khám",
                "Chuyển tuyến",
                "Tử vong",
                "CẤP GIẤY TỜ"
        };
        cboHinhThucXuTri = new JComboBox<>(dispositionOptions);
        cboHinhThucXuTri.setFont(UngDungPhongChu.SMALL);
        cboHinhThucXuTri.setBackground(new Color(255, 255, 235)); // Màu kem nhạt khi focus
        cboHinhThucXuTri.setPreferredSize(new Dimension(152, 24));
        cboHinhThucXuTri.setSelectedItem("Cấp toa cho về");

        // Renderer cho popup danh sách: Mục đang được trỏ chuột / chọn được bôi màu vàng
        DefaultListCellRenderer dispRenderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setFont(UngDungPhongChu.SMALL);
                if (isSelected) {
                    lbl.setBackground(new Color(255, 238, 140)); // Màu vàng highlight khi trỏ chuột vào
                    lbl.setForeground(new Color(20, 25, 35));
                } else {
                    lbl.setBackground(Color.WHITE);
                    lbl.setForeground(new Color(30, 35, 45));
                }
                lbl.setBorder(new EmptyBorder(3, 8, 3, 8));
                return lbl;
            }
        };
        cboHinhThucXuTri.setRenderer(dispRenderer);

        pnlHinhThuc.add(lblHinhThuc, BorderLayout.NORTH);
        pnlHinhThuc.add(cboHinhThucXuTri, BorderLayout.CENTER);

        c.gridx = 0; c.gridy = 1; c.gridwidth = 1; c.weightx = 0.0;
        c.anchor = GridBagConstraints.NORTHWEST;

        // Khu vực bên phải hàng 2: Gồm cụm Tiến khoa (chỉ khi Nhập viện/Nội trú) và Ô ghi chú/lời dặn
        JPanel pnlRightDispArea = new JPanel(new BorderLayout(0, 4));
        pnlRightDispArea.setBackground(Color.WHITE);

        // Cụm Tiến khoa / Khoa ngoại trú (Xuất hiện khi chọn Nhập viện hoặc Điều trị ngoại trú)
        pnlKhoaDieuTri = new JPanel(new BorderLayout(0, 2));
        pnlKhoaDieuTri.setBackground(Color.WHITE);
        pnlKhoaDieuTri.setVisible(false); // Mặc định ẩn vì là "Cấp toa cho về"

        lblKhoa = new JLabel("Tên Khoa ngoại trú");
        lblKhoa.setFont(UngDungPhongChu.SMALL);
        lblKhoa.setForeground(new Color(30, 35, 45));

        cboKhoaDieuTri = new JComboBox<>(OUTPATIENT_DEPARTMENTS);
        cboKhoaDieuTri.setFont(UngDungPhongChu.SMALL);
        cboKhoaDieuTri.setBackground(new Color(255, 255, 245));
        cboKhoaDieuTri.setMaximumRowCount(16);
        cboKhoaDieuTri.setPreferredSize(new Dimension(500, 24));
        cboKhoaDieuTri.setSelectedItem("Khoa Da liễu");

        DefaultListCellRenderer deptRenderer = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setFont(UngDungPhongChu.SMALL);
                if (isSelected) {
                    lbl.setBackground(new Color(0, 120, 215));
                    lbl.setForeground(Color.WHITE);
                } else {
                    lbl.setBackground(Color.WHITE);
                    lbl.setForeground(new Color(30, 35, 45));
                }
                lbl.setBorder(new EmptyBorder(2, 6, 2, 6));
                return lbl;
            }
        };
        cboKhoaDieuTri.setRenderer(deptRenderer);
        pnlKhoaDieuTri.add(lblKhoa, BorderLayout.NORTH);
        pnlKhoaDieuTri.add(cboKhoaDieuTri, BorderLayout.CENTER);

        pnlSoNgayNghi = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        pnlSoNgayNghi.setBackground(Color.WHITE);
        pnlSoNgayNghi.setVisible(false);
        JLabel lblSoNgayNghi = new JLabel("Số ngày nghỉ hưởng BHXH / Giấy tờ (nếu có):");
        lblSoNgayNghi.setFont(UngDungPhongChu.SMALL);
        lblSoNgayNghi.setForeground(new Color(30, 35, 45));
        txtSoNgayNghiBhxh = new JTextField("0");
        txtSoNgayNghiBhxh.setFont(UngDungPhongChu.SMALL);
        txtSoNgayNghiBhxh.setHorizontalAlignment(SwingConstants.CENTER);
        txtSoNgayNghiBhxh.setPreferredSize(new Dimension(56, 24));
        JLabel lblNghiUnit = new JLabel("ngày");
        lblNghiUnit.setFont(UngDungPhongChu.SMALL);
        pnlSoNgayNghi.add(lblSoNgayNghi);
        pnlSoNgayNghi.add(txtSoNgayNghiBhxh);
        pnlSoNgayNghi.add(lblNghiUnit);

        // Ô văn bản ghi chú xử trí / lời dặn: Tự động hiển thị "Cấp toa cho về" khớp 100% media_1793376950126_1a3e6673.png
        txtLoiDanBacSi = new JTextArea("Cấp toa cho về", 4, 50);
        txtLoiDanBacSi.setFont(UngDungPhongChu.SMALL);
        txtLoiDanBacSi.setLineWrap(true);
        txtLoiDanBacSi.setWrapStyleWord(true);
        txtLoiDanBacSi.setBorder(new EmptyBorder(4, 6, 4, 6));

        JScrollPane scrollLoiDan = new JScrollPane(txtLoiDanBacSi);
        scrollLoiDan.setBorder(new LineBorder(new Color(195, 205, 220), 1));
        scrollLoiDan.setPreferredSize(new Dimension(650, 65));

        JLabel lblDoctorInstructions = new JLabel("Lời dặn của bác sĩ");
        lblDoctorInstructions.setFont(UngDungPhongChu.SMALL);
        lblDoctorInstructions.setForeground(new Color(30, 35, 45));
        lblDoctorInstructions.setHorizontalAlignment(SwingConstants.LEFT);

        JPanel pnlDoctorInstructions = new JPanel(new BorderLayout());
        pnlDoctorInstructions.setBackground(Color.WHITE);
        pnlDoctorInstructions.setMaximumSize(new Dimension(Integer.MAX_VALUE, lblDoctorInstructions.getPreferredSize().height));
        pnlDoctorInstructions.add(lblDoctorInstructions, BorderLayout.WEST);

        JPanel pnlLeftDisposition = new JPanel();
        pnlLeftDisposition.setLayout(new BoxLayout(pnlLeftDisposition, BoxLayout.Y_AXIS));
        pnlLeftDisposition.setBackground(Color.WHITE);
        pnlLeftDisposition.setPreferredSize(new Dimension(152, 94));
        pnlLeftDisposition.setMinimumSize(new Dimension(152, 94));
        pnlLeftDisposition.setMaximumSize(new Dimension(152, 94));
        pnlLeftDisposition.add(pnlHinhThuc);
        pnlLeftDisposition.add(Box.createVerticalStrut(4));
        pnlDoctorInstructions.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlLeftDisposition.add(pnlDoctorInstructions);

        pnlRightDispArea.setLayout(new BoxLayout(pnlRightDispArea, BoxLayout.Y_AXIS));
        pnlInstructionsSpacer = new JPanel();
        pnlInstructionsSpacer.setOpaque(false);
        pnlInstructionsSpacer.setPreferredSize(new Dimension(1, 42));
        pnlInstructionsSpacer.setMinimumSize(new Dimension(1, 42));
        pnlInstructionsSpacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        pnlRightDispArea.add(pnlInstructionsSpacer);
        pnlRightDispArea.add(pnlKhoaDieuTri);
        pnlRightDispArea.add(pnlSoNgayNghi);
        pnlRightDispArea.add(scrollLoiDan);

        txtLoiDanBacSi.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && "Cấp toa cho về".equals(cboHinhThucXuTri.getSelectedItem())) {
                    openDischargeSummaryDialog();
                }
            }
        });

        cboHinhThucXuTri.addActionListener(e -> {
            String selected = (String) cboHinhThucXuTri.getSelectedItem();
            boolean allowReexam = !"Tử vong".equals(selected) && !"Bỏ khám".equals(selected);
            setReexamInputsEnabled(allowReexam);
            if ("Điều trị ngoại trú".equals(selected)) {
                lblKhoa.setText("Tên Khoa ngoại trú");
                pnlKhoaDieuTri.setVisible(true);
                pnlSoNgayNghi.setVisible(false);
                pnlInstructionsSpacer.setVisible(false);
                cboKhoaDieuTri.setModel(new DefaultComboBoxModel<>(OUTPATIENT_DEPARTMENTS));
                cboKhoaDieuTri.setSelectedItem("Khoa Da liễu");
                if ("Cấp toa cho về".equals(txtLoiDanBacSi.getText().trim())) {
                    txtLoiDanBacSi.setText("");
                }
            } else if ("Nhập viện".equals(selected)) {
                lblKhoa.setText("Tiến khoa xử trí");
                pnlKhoaDieuTri.setVisible(true);
                pnlSoNgayNghi.setVisible(false);
                pnlInstructionsSpacer.setVisible(false);
                cboKhoaDieuTri.setModel(new DefaultComboBoxModel<>(INPATIENT_DEPARTMENTS));
                cboKhoaDieuTri.setSelectedItem("Gây mê hồi sức");
                if ("Cấp toa cho về".equals(txtLoiDanBacSi.getText().trim())) {
                    txtLoiDanBacSi.setText("");
                }
            } else if ("Chuyển phòng khám".equals(selected)) {
                lblKhoa.setText("Phòng khám chuyển đến");
                pnlKhoaDieuTri.setVisible(true);
                pnlSoNgayNghi.setVisible(false);
                pnlInstructionsSpacer.setVisible(false);
                populateTransferRoomsDropdown();
                if ("Cấp toa cho về".equals(txtLoiDanBacSi.getText().trim())) {
                    txtLoiDanBacSi.setText("");
                }
            } else if ("CẤP GIẤY TỜ".equals(selected)) {
                pnlKhoaDieuTri.setVisible(false);
                pnlSoNgayNghi.setVisible(true);
                pnlInstructionsSpacer.setVisible(false);
                if ("Cấp toa cho về".equals(txtLoiDanBacSi.getText().trim())) {
                    txtLoiDanBacSi.setText("");
                }
            } else if ("Tử vong".equals(selected)) {
                pnlKhoaDieuTri.setVisible(false);
                pnlSoNgayNghi.setVisible(false);
                pnlInstructionsSpacer.setVisible(true);
                if (cboKetQuaDieuTri != null) {
                    cboKetQuaDieuTri.setSelectedItem("Tử vong");
                }
                setReexamDate(null);
            } else if ("Bỏ khám".equals(selected)) {
                pnlKhoaDieuTri.setVisible(false);
                pnlSoNgayNghi.setVisible(false);
                pnlInstructionsSpacer.setVisible(true);
                setReexamDate(null);
            } else if ("Cấp toa cho về".equals(selected)) {
                pnlKhoaDieuTri.setVisible(false);
                pnlSoNgayNghi.setVisible(false);
                pnlInstructionsSpacer.setVisible(true);
                if (txtLoiDanBacSi.getText().trim().isEmpty() || "Nhập viện".equals(txtLoiDanBacSi.getText().trim())) {
                    txtLoiDanBacSi.setText("Cấp toa cho về");
                }
                if (!suppressDispositionDialog) {
                    openDischargeSummaryDialog();
                }
            } else {
                pnlKhoaDieuTri.setVisible(false);
                pnlSoNgayNghi.setVisible(false);
                pnlInstructionsSpacer.setVisible(true);
            }
            pnl.revalidate();
            pnl.repaint();
        });

        c.gridx = 0; c.gridy = 1; c.gridwidth = 1; c.weightx = 0.0; c.fill = GridBagConstraints.HORIZONTAL;
        pnl.add(pnlLeftDisposition, c);

        c.gridx = 1; c.gridy = 1; c.gridwidth = 2; c.weightx = 1.0; c.fill = GridBagConstraints.BOTH;
        pnl.add(pnlRightDispArea, c);

        pnl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        pnl.setPreferredSize(new Dimension(1000, 190));

        return pnl;
    }

    /**
     * Nút thao tác dưới cùng bên phải: [💾 Lưu]
     */
    private JPanel buildFooterActions() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setBackground(Color.WHITE);

        lblSaveStatus = new JLabel("");
        lblSaveStatus.setFont(UngDungPhongChu.SMALL);
        lblSaveStatus.setForeground(new Color(180, 75, 25));

        btnSave = new JButton("Lưu", new SaveDiskIcon());
        btnSave.setFont(UngDungPhongChu.SMALL_BOLD);
        btnSave.setForeground(new Color(25, 30, 40));
        btnSave.setBackground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.setToolTipText("Lưu xử trí, hoàn thành lượt khám và khóa bệnh án");
        btnSave.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(3, 10, 3, 10)
        ));
        btnSave.addActionListener(e -> handleSaveDisposition());

        footer.add(lblSaveStatus);
        footer.add(btnSave);
        updateSaveButtonState();
        return footer;
    }

    private void updateSaveButtonState() {
        if (btnSave == null) return;
        if (currentVisitEmrLocked || currentVisitCompleted || currentVisitCancelled) {
            btnSave.setEnabled(false);
            if (currentVisitCancelled) {
                btnSave.setText("Đã hủy khám");
                if (lblSaveStatus != null) {
                    lblSaveStatus.setText("Lượt khám đã hủy.");
                }
            } else {
                btnSave.setText(currentVisitEmrLocked ? "Đã lưu & khóa bệnh án" : "Đã lưu xử trí");
                if (lblSaveStatus != null) {
                    lblSaveStatus.setText("Đã lưu xử trí. Chuột phải lượt khám → \"Mở Lại Bệnh Án\" để sửa.");
                }
            }
        } else {
            btnSave.setEnabled(true);
            btnSave.setText("Lưu");
            if (lblSaveStatus != null) {
                lblSaveStatus.setText("");
            }
        }
    }

    private void syncDiagnosesToText() {
        StringBuilder sb = new StringBuilder();
        String mainIcdCode = txtMaIcdChinh.getText().trim();
        String mainIcdName = txtTenIcdChinh.getText().trim();
        if (!mainIcdName.isEmpty()) {
            sb.append("Bệnh chính: ").append(mainIcdName);
            if (!mainIcdCode.isEmpty()) {
                sb.append(" (").append(mainIcdCode).append(")");
            }
            sb.append("\n");
        }

        if (secondaryIcdModel.getSize() > 0) {
            sb.append("Bệnh kèm theo:\n");
            for (int i = 0; i < secondaryIcdModel.getSize(); i++) {
                sb.append("- ").append(secondaryIcdModel.getElementAt(i)).append("\n");
            }
        }

        if (txtTrieuChung != null) {
            String tc = txtTrieuChung.getText().trim();
            if (!tc.isEmpty() && !tc.equals("Triệu chứng ...")) {
                sb.append("Triệu chứng: ").append(tc).append("\n");
            }
        }

        if (txtKhamLamSang != null) {
            String kls = txtKhamLamSang.getText().trim();
            if (!kls.isEmpty() && !kls.equals("Khám lâm sàng ...")) {
                sb.append("Khám lâm sàng: ").append(kls).append("\n");
            }
        }

        txtChanDoanXacDinh.setText(sb.toString().trim());
    }

    private void openSelectDiagnosisPopup(Component anchor) {
        JPopupMenu menu = new JPopupMenu();
        menu.setFont(UngDungPhongChu.SMALL);

        List<ICD> catalog = fetchIcdCatalogFromServices();
        if (!catalog.isEmpty()) {
            int limit = Math.min(12, catalog.size());
            for (int i = 0; i < limit; i++) {
                ICD icd = catalog.get(i);
                String code = icd.getMaICD() != null ? icd.getMaICD().trim() : "";
                String name = icd.getTenBenh() != null ? icd.getTenBenh().trim() : "";
                String label = code.isEmpty() ? name : (name + " (" + code + ")");
                JMenuItem item = new JMenuItem(label);
                item.setFont(UngDungPhongChu.SMALL);
                item.addActionListener(e -> {
                    applyMainDiagnosis(code, name);
                });
                menu.add(item);
            }
        } else {
            String[] samples = {
                    "Nhiễm khuẩn đường ruột (A09)",
                    "Viêm phế quản cấp (J20)",
                    "Viêm họng cấp (J02)",
                    "Sốt xuất huyết Dengue (A90)",
                    "Viêm dạ dày ruột cấp (K52.9)"
            };
            for (String sample : samples) {
                JMenuItem item = new JMenuItem(sample);
                item.setFont(UngDungPhongChu.SMALL);
                item.addActionListener(e -> {
                    String cur = txtChanDoanXacDinh.getText();
                    if (cur.isEmpty()) {
                        txtChanDoanXacDinh.setText(sample);
                    } else {
                        txtChanDoanXacDinh.setText(cur + "\n" + sample);
                    }
                });
                menu.add(item);
            }
        }

        menu.show(anchor, 0, anchor.getHeight());
    }

    public void openIcdCatalogDialog(boolean isMain) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        String codeText = txtMaIcdChinh != null ? txtMaIcdChinh.getText().trim() : "";
        String nameText = txtTenIcdChinh != null ? txtTenIcdChinh.getText().trim() : "";
        String initial = isMain ? (!codeText.isEmpty() ? codeText : nameText) : "";
        ICDDanhMucLuaChonHopThoai dlg = new ICDDanhMucLuaChonHopThoai(
                owner, initial, fetchIcdCatalogFromServices(), (code, name) -> {
            if (isMain) {
                applyMainDiagnosis(code, name);
            } else {
                String full = code + " - " + name;
                if (!secondaryIcdModel.contains(full)) {
                    secondaryIcdModel.addElement(full);
                }
            }
        });
        dlg.setVisible(true);
    }

    private void applyMainDiagnosis(String code, String name) {
        String c = code != null ? code.trim() : "";
        String n = name != null ? name.trim() : "";
        if (txtMaIcdChinh != null) txtMaIcdChinh.setText(c);
        if (txtTenIcdChinh != null) txtTenIcdChinh.setText(n);
        if (txtChanDoanXacDinh != null) {
            if (!n.isEmpty() && !c.isEmpty()) {
                txtChanDoanXacDinh.setText(n + " (" + c + ")");
            } else {
                txtChanDoanXacDinh.setText(!n.isEmpty() ? n : c);
            }
        }
    }

    public void openSecondaryDiagnosisDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        List<String> current = new ArrayList<>();
        for (int i = 0; i < secondaryIcdModel.getSize(); i++) {
            current.add(secondaryIcdModel.getElementAt(i));
        }

        ThuPhuChanDoanHopThoai dlg = new ThuPhuChanDoanHopThoai(owner, current, (updatedList) -> {
            secondaryIcdModel.clear();
            for (String item : updatedList) {
                secondaryIcdModel.addElement(item);
            }
            if (secondaryIcdModel.getSize() > 0) {
                listSecondaryIcd.setSelectedIndex(0);
            }
        });
        dlg.setVisible(true);
    }

    public void openDischargeSummaryDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        RaVienTongHopHopThoai dlg = new RaVienTongHopHopThoai(owner, data -> {
            if (data != null) {
                StringBuilder sb = new StringBuilder();
                if (data.tinhTrangRaVien != null && !data.tinhTrangRaVien.isEmpty()) {
                    sb.append("Tình trạng: ").append(data.tinhTrangRaVien).append(". ");
                }
                if (data.henTaiKham != null && !data.henTaiKham.isEmpty() && !"Chọn ngày".equals(data.henTaiKham)) {
                    sb.append("Hẹn tái khám: ").append(data.henTaiKham).append(". ");
                    LocalDate parsedReexam = parseReexamDate(data.henTaiKham);
                    if (parsedReexam != null) {
                        setReexamDate(parsedReexam);
                    }
                }
                if (sb.length() > 0) {
                    txtLoiDanBacSi.setText("Cấp toa cho về - " + sb.toString().trim());
                } else {
                    txtLoiDanBacSi.setText("Cấp toa cho về");
                }
            }
        });
        dlg.setVisible(true);
    }

    /**
     * Cấu hình trạng thái "Cấp toa cho về" khớp 100% media_1793376950126_1a3e6673.png
     */
    public void setCapToaChoVeState(String thoiGian, String ketQua, String maIcd, String tenIcd, String secIcd, String trieuChung, String khamLamSang, String loiDan) {
        if (thoiGian != null) txtThoiGianXuTri.setText(thoiGian);
        if (ketQua != null) cboKetQuaDieuTri.setSelectedItem(ketQua);
        if (maIcd != null) txtMaIcdChinh.setText(maIcd);
        if (tenIcd != null) txtTenIcdChinh.setText(tenIcd);
        if (secIcd != null) {
            secondaryIcdModel.clear();
            secondaryIcdModel.addElement(secIcd);
            listSecondaryIcd.setSelectedIndex(0);
        }
        if (trieuChung != null && txtTrieuChung != null) txtTrieuChung.setText(trieuChung);
        if (khamLamSang != null && txtKhamLamSang != null) txtKhamLamSang.setText(khamLamSang);
        if (loiDan != null) txtLoiDanBacSi.setText(loiDan);

        cboHinhThucXuTri.setSelectedItem("Cấp toa cho về");
        if (pnlKhoaDieuTri != null) {
            pnlKhoaDieuTri.setVisible(false);
        }
    }

    public void setAdmissionState(String thoiGian, String ketQua, String maIcd, String tenIcd, String secIcd, String dept) {
        if (thoiGian != null) txtThoiGianXuTri.setText(thoiGian);
        if (ketQua != null) cboKetQuaDieuTri.setSelectedItem(ketQua);
        if (maIcd != null) txtMaIcdChinh.setText(maIcd);
        if (tenIcd != null) txtTenIcdChinh.setText(tenIcd);
        if (secIcd != null) {
            secondaryIcdModel.clear();
            secondaryIcdModel.addElement(secIcd);
            listSecondaryIcd.setSelectedIndex(0);
        }
        cboHinhThucXuTri.setSelectedItem("Nhập viện");
        if (pnlKhoaDieuTri != null) {
            pnlKhoaDieuTri.setVisible(true);
        }
        if (dept != null && cboKhoaDieuTri != null) {
            cboKhoaDieuTri.setSelectedItem(dept);
        }
    }

    /**
     * Cấu hình trạng thái "Điều trị ngoại trú" khớp 100% media_1793379640679_89d5bbca.jpg
     */
    public void setOutpatientState(String thoiGian, String ketQua, String maIcd, String tenIcd, String khoaNgoaiTru, String loiDan) {
        if (thoiGian != null) txtThoiGianXuTri.setText(thoiGian);
        if (ketQua != null) cboKetQuaDieuTri.setSelectedItem(ketQua);
        if (maIcd != null) txtMaIcdChinh.setText(maIcd);
        if (tenIcd != null) txtTenIcdChinh.setText(tenIcd);
        if (loiDan != null) txtLoiDanBacSi.setText(loiDan);

        cboHinhThucXuTri.setSelectedItem("Điều trị ngoại trú");
        if (lblKhoa != null) {
            lblKhoa.setText("Tên Khoa ngoại trú");
        }
        if (pnlKhoaDieuTri != null) {
            pnlKhoaDieuTri.setVisible(true);
        }
        if (cboKhoaDieuTri != null) {
            cboKhoaDieuTri.setModel(new DefaultComboBoxModel<>(OUTPATIENT_DEPARTMENTS));
            if (khoaNgoaiTru != null) {
                cboKhoaDieuTri.setSelectedItem(khoaNgoaiTru);
            }
        }
    }

    public JComboBox<String> getCboHinhThucXuTri() {
        return cboHinhThucXuTri;
    }

    public JComboBox<String> getCboKhoaDieuTri() {
        return cboKhoaDieuTri;
    }

    public JPanel getPnlKhoaDieuTri() {
        return pnlKhoaDieuTri;
    }

    public JTextArea getTxtTrieuChung() {
        return txtTrieuChung;
    }

    public JTextArea getTxtKhamLamSang() {
        return txtKhamLamSang;
    }

    public JTextArea getTxtLoiDanBacSi() {
        return txtLoiDanBacSi;
    }

    private String currentPatientCode = "22001770";
    private String currentPatientName = "NGUYỄN THỊ LIÊN";
    private String currentPatientAge = "38";
    private String currentPatientGender = "Nữ";
    private String currentPatientAddress = "Xã Mường Lai, Huyện Lục Yên, Tỉnh Yên Bái";
    private String currentDoctorName = "bieuCBT";

    public void setPatientInfo(String code, String name, String age, String gender, String address, String doctor) {
        setPatientInfo(code, name, age, gender, address, doctor, null);
    }

    /** Nạp đầy đủ thông tin bệnh nhân kèm chẩn đoán hiện tại từ CSDL (`ChanDoan` / `ICD`). */
    public void setPatientInfo(String code, String name, String age, String gender,
                               String address, String doctor, String diagnosisDisplay) {
        if (code != null) this.currentPatientCode = code;
        if (name != null) this.currentPatientName = name;
        if (age != null) this.currentPatientAge = age;
        if (gender != null) this.currentPatientGender = gender;
        if (address != null) this.currentPatientAddress = address;
        if (doctor != null) this.currentDoctorName = doctor;
        if (diagnosisDisplay != null) {
            setPatientDiagnosis(diagnosisDisplay);
        }
    }

    public void setPatientDiagnosis(String diagnosisDisplay) {
        String diag = diagnosisDisplay != null ? diagnosisDisplay.trim() : "";
        if (diag.isEmpty()) {
            applyMainDiagnosis("", "");
            return;
        }
        int sep = diag.indexOf(" - ");
        if (sep > 0) {
            applyMainDiagnosis(diag.substring(0, sep).trim(), diag.substring(sep + 3).trim());
            return;
        }
        int dash = diag.indexOf('-');
        if (dash > 0 && dash <= 8 && !diag.substring(0, dash).contains(" ")) {
            applyMainDiagnosis(diag.substring(0, dash).trim(), diag.substring(dash + 1).trim());
            return;
        }
        for (ICD icd : fetchIcdCatalogFromServices()) {
            if (icd != null && icd.getMaICD() != null && diag.equalsIgnoreCase(icd.getMaICD().trim())) {
                applyMainDiagnosis(icd.getMaICD().trim(), icd.getTenBenh() != null ? icd.getTenBenh().trim() : "");
                return;
            }
        }
        applyMainDiagnosis("", diag);
    }

    private static final DateTimeFormatter REEXAM_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private JPanel buildReexamControlPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 2));
        wrapper.setBackground(Color.WHITE);

        JLabel lbl = new JLabel("Hẹn tái khám (tùy chọn)");
        lbl.setFont(UngDungPhongChu.SMALL);
        lbl.setForeground(new Color(30, 35, 45));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setOpaque(false);

        JPanel dateContainer = new JPanel(new BorderLayout());
        dateContainer.setBackground(Color.WHITE);
        dateContainer.setBorder(new LineBorder(new Color(185, 195, 210), 1));
        dateContainer.setPreferredSize(new Dimension(112, 24));

        txtNgayHenTaiKham = new JTextField();
        txtNgayHenTaiKham.setFont(UngDungPhongChu.SMALL);
        txtNgayHenTaiKham.setHorizontalAlignment(SwingConstants.CENTER);
        txtNgayHenTaiKham.setBorder(new EmptyBorder(2, 4, 2, 4));
        txtNgayHenTaiKham.setToolTipText("Định dạng dd/MM/yyyy (VD: 25/11/2026). Để trống nếu không hẹn tái khám.");

        JButton btnQuickReexam = new JButton("▾");
        btnQuickReexam.setFont(UngDungPhongChu.SMALL);
        btnQuickReexam.setPreferredSize(new Dimension(18, 22));
        btnQuickReexam.setBackground(new Color(245, 247, 250));
        btnQuickReexam.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(205, 214, 226)));
        btnQuickReexam.setFocusPainted(false);
        btnQuickReexam.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnQuickReexam.addActionListener(e -> showReexamQuickMenu(btnQuickReexam));

        dateContainer.add(txtNgayHenTaiKham, BorderLayout.CENTER);
        dateContainer.add(btnQuickReexam, BorderLayout.EAST);

        JLabel lblSau = new JLabel("Sau:");
        lblSau.setFont(UngDungPhongChu.SMALL);
        txtSoNgayTaiKham = new JTextField("0");
        txtSoNgayTaiKham.setFont(UngDungPhongChu.SMALL);
        txtSoNgayTaiKham.setPreferredSize(new Dimension(34, 24));
        txtSoNgayTaiKham.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel lblNgayRed = new JLabel("ngày");
        lblNgayRed.setFont(UngDungPhongChu.SMALL_BOLD);
        lblNgayRed.setForeground(new Color(180, 25, 25));

        row.add(dateContainer);
        row.add(Box.createHorizontalStrut(4));
        row.add(lblSau);
        row.add(txtSoNgayTaiKham);
        row.add(lblNgayRed);

        wrapper.add(lbl, BorderLayout.NORTH);
        wrapper.add(row, BorderLayout.CENTER);
        wireReexamFieldListeners();
        return wrapper;
    }

    private void showReexamQuickMenu(Component anchor) {
        JPopupMenu menu = new JPopupMenu();
        int[] offsets = {2, 7, 14, 30};
        String[] labels = {"2 ngày sau", "1 tuần sau (7 ngày)", "2 tuần sau (14 ngày)", "1 tháng sau (30 ngày)"};
        for (int i = 0; i < offsets.length; i++) {
            int days = offsets[i];
            JMenuItem item = new JMenuItem(labels[i]);
            item.setFont(UngDungPhongChu.SMALL);
            item.addActionListener(e -> setReexamDate(LocalDate.now().plusDays(days)));
            menu.add(item);
        }
        menu.addSeparator();
        JMenuItem clearItem = new JMenuItem("Xóa ngày hẹn");
        clearItem.setFont(UngDungPhongChu.SMALL);
        clearItem.addActionListener(e -> setReexamDate(null));
        menu.add(clearItem);
        menu.show(anchor, 0, anchor.getHeight());
    }

    private void wireReexamFieldListeners() {
        txtSoNgayTaiKham.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { syncFromDays(); }
            @Override public void removeUpdate(DocumentEvent e) { syncFromDays(); }
            @Override public void changedUpdate(DocumentEvent e) { syncFromDays(); }

            private void syncFromDays() {
                if (updatingReexamFields) return;
                String raw = txtSoNgayTaiKham.getText().trim();
                updatingReexamFields = true;
                try {
                    if (raw.isEmpty() || "0".equals(raw)) {
                        selectedReexamDate = null;
                        txtNgayHenTaiKham.setText("");
                        return;
                    }
                    int days = Integer.parseInt(raw);
                    if (days > 0) {
                        LocalDate target = LocalDate.now().plusDays(days);
                        selectedReexamDate = target;
                        txtNgayHenTaiKham.setText(target.format(REEXAM_FMT));
                    } else {
                        selectedReexamDate = null;
                        txtNgayHenTaiKham.setText("");
                    }
                } catch (NumberFormatException ignored) {
                } finally {
                    updatingReexamFields = false;
                }
            }
        });

        txtNgayHenTaiKham.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { syncFromDate(); }
            @Override public void removeUpdate(DocumentEvent e) { syncFromDate(); }
            @Override public void changedUpdate(DocumentEvent e) { syncFromDate(); }

            private void syncFromDate() {
                if (updatingReexamFields) return;
                String raw = txtNgayHenTaiKham.getText().trim();
                updatingReexamFields = true;
                try {
                    if (raw.isEmpty()) {
                        selectedReexamDate = null;
                        txtSoNgayTaiKham.setText("0");
                        return;
                    }
                    LocalDate parsed = parseReexamDate(raw);
                    if (parsed != null) {
                        selectedReexamDate = parsed;
                        long diff = Math.max(0L, ChronoUnit.DAYS.between(LocalDate.now(), parsed));
                        txtSoNgayTaiKham.setText(String.valueOf(diff));
                    }
                } finally {
                    updatingReexamFields = false;
                }
            }
        });
    }

    public void setReexamDate(LocalDate date) {
        this.selectedReexamDate = date;
        updatingReexamFields = true;
        try {
            if (txtNgayHenTaiKham != null) {
                txtNgayHenTaiKham.setText(date != null ? date.format(REEXAM_FMT) : "");
            }
            if (txtSoNgayTaiKham != null) {
                long diff = date != null ? Math.max(0L, ChronoUnit.DAYS.between(LocalDate.now(), date)) : 0L;
                txtSoNgayTaiKham.setText(String.valueOf(diff));
            }
        } finally {
            updatingReexamFields = false;
        }
    }

    public LocalDate resolveReexamDateForSave() {
        if (txtNgayHenTaiKham != null && !txtNgayHenTaiKham.getText().trim().isEmpty()) {
            LocalDate parsed = parseReexamDate(txtNgayHenTaiKham.getText().trim());
            if (parsed != null) return parsed;
        }
        if (txtSoNgayTaiKham != null) {
            try {
                int days = Integer.parseInt(txtSoNgayTaiKham.getText().trim());
                if (days > 0) return LocalDate.now().plusDays(days);
            } catch (NumberFormatException ignored) {
            }
        }
        return selectedReexamDate;
    }

    static LocalDate parseReexamDate(String text) {
        if (text == null || text.isBlank()) return null;
        String clean = text.trim();
        for (DateTimeFormatter fmt : new DateTimeFormatter[]{REEXAM_FMT, DateTimeFormatter.ISO_LOCAL_DATE}) {
            try {
                return LocalDate.parse(clean, fmt);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private List<ICD> fetchIcdCatalogFromServices() {
        if (services == null || services.examQueue() == null) return List.of();
        try {
            return services.examQueue().searchIcdCatalog("");
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private Integer resolveIcdId(String code) {
        if (code == null || code.isBlank()) return null;
        String q = code.trim();
        for (ICD icd : fetchIcdCatalogFromServices()) {
            if (icd != null && icd.getMaICD() != null && q.equalsIgnoreCase(icd.getMaICD().trim())) {
                return icd.getId();
            }
        }
        return null;
    }

    public JTextField getTxtNgayHenTaiKham() { return txtNgayHenTaiKham; }
    public JTextField getTxtSoNgayTaiKham() { return txtSoNgayTaiKham; }
    public LocalDate getSelectedReexamDate() { return selectedReexamDate; }

    public void openFollowUpAppointmentDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        String instructions = txtLoiDanBacSi != null ? txtLoiDanBacSi.getText().trim() : "";
        LocalDate reexam = resolveReexamDateForSave();
        LocalDate displayDate = reexam != null ? reexam : LocalDate.now().plusDays(7);
        LocalDate today = LocalDate.now();
        String appointmentTime = String.format("08 giờ 00 ngày %02d tháng %02d năm %04d",
                displayDate.getDayOfMonth(), displayDate.getMonthValue(), displayDate.getYear());
        String dateStr = String.format("Ngày %02d tháng %02d năm %04d",
                today.getDayOfMonth(), today.getMonthValue(), today.getYear());
        TheoTiepLichHenPrintHopThoai dlg = new TheoTiepLichHenPrintHopThoai(
                owner,
                currentPatientCode,
                currentPatientName,
                currentPatientAge,
                currentPatientGender,
                currentPatientAddress,
                appointmentTime,
                instructions,
                dateStr,
                currentDoctorName
        );
        dlg.setVisible(true);
    }

    private void setReexamInputsEnabled(boolean enabled) {
        if (txtNgayHenTaiKham != null) txtNgayHenTaiKham.setEnabled(enabled);
        if (txtSoNgayTaiKham != null) txtSoNgayTaiKham.setEnabled(enabled);
    }

    private void populateTransferRoomsDropdown() {
        transferRoomOptions.clear();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        if (services != null && services.examQueue() != null) {
            try {
                var context = vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh.current();
                for (PhongKham room : services.examQueue().getActiveClinicRooms(context != null ? context.facilityId() : 0)) {
                    if (room == null || room.getId() == null) continue;
                    transferRoomOptions.add(room);
                    String dept = room.getKhoa() != null && room.getKhoa().getTenKhoa() != null
                            ? " (" + room.getKhoa().getTenKhoa() + ")" : "";
                    model.addElement(room.getId() + " - " + (room.getTenPhong() != null ? room.getTenPhong() : "Phòng #" + room.getId()) + dept);
                }
            } catch (Exception ignored) {
            }
        }
        if (model.getSize() == 0) {
            model.addElement("Chưa có phòng khám hoạt động");
        }
        cboKhoaDieuTri.setModel(model);
    }

    private Integer resolveSelectedTransferRoomId() {
        if (!transferRoomOptions.isEmpty() && cboKhoaDieuTri != null && cboKhoaDieuTri.getSelectedIndex() >= 0
                && cboKhoaDieuTri.getSelectedIndex() < transferRoomOptions.size()) {
            return transferRoomOptions.get(cboKhoaDieuTri.getSelectedIndex()).getId();
        }
        return null;
    }

    private void handleSaveDisposition() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy");
        if (txtThoiGianXuTri != null && txtThoiGianXuTri.getText().trim().equals(":   /  /")) {
            txtThoiGianXuTri.setText(sdf.format(new Date()));
        }

        if (services == null || currentVisitId <= 0 ||
                vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh.current() == null) {
            JOptionPane.showMessageDialog(this, "Chưa có lượt khám hoặc phiên bác sĩ hợp lệ.",
                    "Không thể lưu xử trí", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (currentVisitEmrLocked || currentVisitCompleted || currentVisitCancelled) {
            JOptionPane.showMessageDialog(this,
                    "Lượt khám này đã lưu xử trí / khóa bệnh án, không thể lưu tiếp.\n"
                            + "Nếu cần chỉnh sửa hoặc xử trí lại, vui lòng chuột phải vào lượt khám và chọn \"Mở Lại Bệnh Án\".",
                    "Bệnh án đã lưu xử trí", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (txtNgayHenTaiKham != null && !txtNgayHenTaiKham.getText().trim().isEmpty()
                && parseReexamDate(txtNgayHenTaiKham.getText().trim()) == null) {
            JOptionPane.showMessageDialog(this, "Ngày hẹn tái khám chưa đúng định dạng dd/MM/yyyy.",
                    "Ngày tái khám không hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDateTime dispositionTime = null;
        if (txtThoiGianXuTri != null && !txtThoiGianXuTri.getText().trim().isEmpty()) {
            try {
                dispositionTime = LocalDateTime.parse(txtThoiGianXuTri.getText().trim(), DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Thời gian xử trí chưa đúng định dạng HH:mm dd/MM/yyyy.",
                        "Thời gian xử trí không hợp lệ", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        String mainDiagnosis = (txtChanDoanXacDinh != null && !txtChanDoanXacDinh.getText().isBlank())
                ? txtChanDoanXacDinh.getText().trim()
                : (!txtTenIcdChinh.getText().isBlank() ? txtTenIcdChinh.getText().trim() : "Khám bệnh");
        String doctorInstructions = txtLoiDanBacSi != null ? txtLoiDanBacSi.getText().trim() : "";
        String disposition = cboHinhThucXuTri != null ? (String) cboHinhThucXuTri.getSelectedItem() : "Cấp toa cho về";
        boolean isTransferRoom = "Chuyển phòng khám".equalsIgnoreCase(disposition);
        boolean isCompleteExam = !isTransferRoom;

        KetQuaDieuTri result = switch (String.valueOf(cboKetQuaDieuTri.getSelectedItem())) {
            case "Khỏi" -> KetQuaDieuTri.KHOI;
            case "Đỡ, giảm" -> KetQuaDieuTri.DO_GIAM;
            case "Không thay đổi" -> KetQuaDieuTri.KHONG_THAY_DOI;
            case "Nặng hơn" -> KetQuaDieuTri.NANG_HON;
            case "Tử vong" -> KetQuaDieuTri.TU_VONG;
            default -> null;
        };
        HuongXuTri direction = switch (disposition) {
            case "Cấp toa cho về", "Điều trị ngoại trú" -> HuongXuTri.CHO_VE_TOA;
            case "Nhập viện" -> HuongXuTri.NHAP_VIEN_NOI_TRU;
            case "Chuyển tuyến" -> HuongXuTri.CHUYEN_TUYEN_TREN;
            case "Chuyển phòng khám" -> HuongXuTri.CHUYEN_CHUYEN_KHOA;
            case "Bỏ khám" -> HuongXuTri.BO_KHAM;
            case "Tử vong" -> HuongXuTri.TU_VONG;
            case "CẤP GIẤY TỜ" -> HuongXuTri.CAP_GIAY_TO;
            default -> null;
        };
        if (currentDoctorId <= 0 || direction == null) {
            JOptionPane.showMessageDialog(this, "Bác sĩ hoặc hình thức xử trí chưa hợp lệ.",
                    "Không thể lưu xử trí", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (direction == HuongXuTri.TU_VONG) {
            result = KetQuaDieuTri.TU_VONG;
        }

        Integer transferRoomId = isTransferRoom ? resolveSelectedTransferRoomId() : null;
        if (isTransferRoom && (transferRoomId == null || transferRoomId <= 0)) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn phòng khám chuyển đến.",
                    "Thiếu phòng khám đích", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Integer bhxhDays = null;
        if (direction == HuongXuTri.CAP_GIAY_TO && txtSoNgayNghiBhxh != null && !txtSoNgayNghiBhxh.getText().trim().isEmpty()) {
            try {
                int d = Integer.parseInt(txtSoNgayNghiBhxh.getText().trim());
                if (d < 0) {
                    JOptionPane.showMessageDialog(this, "Số ngày nghỉ hưởng BHXH phải >= 0.",
                            "Dữ liệu không hợp lệ", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (d > 0) bhxhDays = d;
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Số ngày nghỉ hưởng BHXH phải là số nguyên.",
                        "Dữ liệu không hợp lệ", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        HinhThucDieuTri treatmentType = "Nhập viện".equals(disposition)
                ? HinhThucDieuTri.NOI_TRU : HinhThucDieuTri.NGOAI_TRU;
        String treatmentDirectionText = null;
        if (("Điều trị ngoại trú".equals(disposition) || "Nhập viện".equals(disposition))
                && cboKhoaDieuTri != null && cboKhoaDieuTri.getSelectedItem() != null) {
            treatmentDirectionText = disposition + " - " + cboKhoaDieuTri.getSelectedItem().toString().trim();
        }

        LocalDate reexamDate = (direction == HuongXuTri.TU_VONG || direction == HuongXuTri.BO_KHAM)
                ? null : resolveReexamDateForSave();
        Integer mainIcdId = resolveIcdId(txtMaIcdChinh != null ? txtMaIcdChinh.getText() : "");
        var auth = vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh.current();

        AddChanDoanCommand diagCmd = new AddChanDoanCommand(
                currentVisitId,
                currentDoctorId,
                mainIcdId,
                mainDiagnosis,
                true,
                "XAC_DINH",
                auth
        );

        List<AddChanDoanCommand> secondaryCmds = new ArrayList<>();
        if (secondaryIcdModel != null) {
            for (int i = 0; i < secondaryIcdModel.getSize(); i++) {
                String entry = secondaryIcdModel.getElementAt(i);
                if (entry == null || entry.isBlank()) continue;
                String secCode = "";
                String secName = entry.trim();
                int sep = secName.indexOf(" - ");
                if (sep > 0) {
                    secCode = secName.substring(0, sep).trim();
                    secName = secName.substring(sep + 3).trim();
                } else {
                    int dash = secName.indexOf('-');
                    if (dash > 0 && dash <= 8 && !secName.substring(0, dash).contains(" ")) {
                        secCode = secName.substring(0, dash).trim();
                        secName = secName.substring(dash + 1).trim();
                    }
                }
                if (!secName.isEmpty()) {
                    secondaryCmds.add(new AddChanDoanCommand(
                            currentVisitId, currentDoctorId, resolveIcdId(secCode),
                            secName, false, "KEM_THEO", auth));
                }
            }
        }

        String symptomsText = cleanPlaceholder(txtTrieuChung, "Triệu chứng ...");
        String clinicalExamText = cleanPlaceholder(txtKhamLamSang, "Khám lâm sàng ...");
        UpdateEMRLamSangRecordCommand clinicalCmd = (symptomsText != null || clinicalExamText != null)
                ? new UpdateEMRLamSangRecordCommand(
                        currentVisitId, currentDoctorId,
                        symptomsText, null, null, null, clinicalExamText,
                        null, null, null, auth)
                : null;

        TransferLuotKhamCommand transferCmd = isTransferRoom
                ? new TransferLuotKhamCommand(currentVisitId, transferRoomId, TransferLyDoMa.CLINICAL_SPECIALTY_REFERRAL, doctorInstructions, auth)
                : null;

        CompleteKhamCommand completeCmd = isCompleteExam
                ? new CompleteKhamCommand(
                        currentVisitId, currentDoctorId, result, direction,
                        doctorInstructions, reexamDate, dispositionTime,
                        treatmentType, treatmentDirectionText, bhxhDays, auth)
                : null;

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try {
                    return KhamBenhGhepNoiGoc.completeDispositionWithAutoStart(
                            services, diagCmd, secondaryCmds, clinicalCmd, completeCmd, transferCmd, isCompleteExam);
                } catch (Exception ex) {
                    return "Lưu xử trí thất bại. Vui lòng thử lại.";
                }
            }
            @Override
            protected void done() {
                try {
                    String error = get();
                    if (error != null) {
                        JOptionPane.showMessageDialog(DieuTriXuTriTheManHinh.this, error,
                                "Không thể lưu xử trí", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    if (onDispositionSavedCallback != null) {
                        onDispositionSavedCallback.run();
                    }
                    loadVisitDetailFromDatabase(currentVisitId);
                    if (isTransferRoom) {
                        if (!GraphicsEnvironment.isHeadless()) {
                            String roomLabel = cboKhoaDieuTri != null && cboKhoaDieuTri.getSelectedItem() != null
                                    ? cboKhoaDieuTri.getSelectedItem().toString().trim() : "";
                            Window owner = SwingUtilities.getWindowAncestor(DieuTriXuTriTheManHinh.this);
                            new vn.clinicmanagement.desktop.modules.print.examination.TransferPhieuPrintPreviewHopThoai(
                                    owner, currentPatientName, currentPatientCode, roomLabel).setVisible(true);
                        }
                    } else if (isCompleteExam && direction == HuongXuTri.NHAP_VIEN_NOI_TRU) {
                        String dept = cboKhoaDieuTri != null && cboKhoaDieuTri.getSelectedItem() != null
                                ? cboKhoaDieuTri.getSelectedItem().toString().trim() : "";
                        openAdmissionFormDialog(dept, mainDiagnosis);
                    } else if (isCompleteExam
                            && direction != HuongXuTri.TU_VONG
                            && direction != HuongXuTri.BO_KHAM
                            && ("Cấp toa cho về".equals(disposition) || reexamDate != null)) {
                        openFollowUpAppointmentDialog();
                    } else {
                        JOptionPane.showMessageDialog(DieuTriXuTriTheManHinh.this,
                                isCompleteExam && direction != HuongXuTri.BO_KHAM
                                        ? "Đã lưu xử trí, hoàn thành lượt khám và khóa bệnh án."
                                        : "Đã lưu thông tin xử trí vào CSDL thành công.",
                                "Lưu Xử Trí", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DieuTriXuTriTheManHinh.this,
                            "Lưu xử trí thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void openAdmissionFormDialog(String targetDepartment, String primaryDiagnosis) {
        if (GraphicsEnvironment.isHeadless()) return;
        LuotKham visit = null;
        if (services != null && services.examQueue() != null && currentVisitId > 0) {
            try {
                visit = services.examQueue().getVisitDetail(currentVisitId).orElse(null);
            } catch (Exception ignored) {}
        }
        var data = vn.clinicmanagement.desktop.modules.print.examination.PhieuKhamVaoVienPrintManHinh.fromVisit(
                visit,
                currentPatientCode,
                currentPatientName,
                currentPatientAge,
                currentPatientGender,
                currentPatientAddress,
                primaryDiagnosis,
                targetDepartment,
                "PK Khám Bệnh");
        Window owner = SwingUtilities.getWindowAncestor(this);
        new vn.clinicmanagement.desktop.modules.print.examination.PhieuKhamVaoVienPrintHopThoai(owner, data).setVisible(true);
    }

    private static String cleanPlaceholder(JTextArea area, String placeholder) {
        if (area == null) return null;
        String text = area.getText().trim();
        return (text.isEmpty() || text.equals(placeholder)) ? null : text;
    }

    public static class DocumentSheetIcon implements Icon {
        @Override public int getIconWidth() { return 24; }
        @Override public int getIconHeight() { return 28; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = 18;
            int h = 22;
            int fold = 5;
            int px = x + 3;
            int py = y + 3;

            // Tờ giấy trắng viền xám xanh
            Polygon p = new Polygon();
            p.addPoint(px, py);
            p.addPoint(px + w - fold, py);
            p.addPoint(px + w, py + fold);
            p.addPoint(px + w, py + h);
            p.addPoint(px, py + h);

            g2.setColor(Color.WHITE);
            g2.fillPolygon(p);
            g2.setColor(new Color(160, 180, 205));
            g2.drawPolygon(p);

            // Góc gấp hình tam giác
            Polygon foldPoly = new Polygon();
            foldPoly.addPoint(px + w - fold, py);
            foldPoly.addPoint(px + w - fold, py + fold);
            foldPoly.addPoint(px + w, py + fold);
            g2.setColor(new Color(230, 240, 252));
            g2.fillPolygon(foldPoly);
            g2.setColor(new Color(160, 180, 205));
            g2.drawPolygon(foldPoly);

            // Cây bút vẽ xiên góc dưới phải khớp media_1793376950126_1a3e6673.png
            g2.setColor(new Color(0, 102, 204));
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(px + w - 4, py + h - 2, px + w + 1, py + h - 7);
            g2.setColor(new Color(40, 60, 90));
            g2.drawLine(px + w - 5, py + h - 1, px + w - 4, py + h - 2);

            g2.dispose();
        }
    }

    public static class DiagnosisFolderIcon implements Icon {
        @Override public int getIconWidth() { return 20; }
        @Override public int getIconHeight() { return 16; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(225, 238, 250));
            g2.fillRoundRect(x + 1, y + 4, 18, 11, 2, 2);
            g2.setColor(new Color(125, 160, 195));
            g2.drawRoundRect(x + 1, y + 4, 18, 11, 2, 2);
            g2.setColor(new Color(190, 218, 242));
            g2.fillRect(x + 3, y + 2, 7, 4);
            g2.setColor(new Color(125, 160, 195));
            g2.drawLine(x + 3, y + 2, x + 10, y + 2);
            g2.drawLine(x + 10, y + 2, x + 12, y + 4);
            g2.dispose();
        }
    }

    public static class SaveDiskIcon implements Icon {
        @Override public int getIconWidth() { return 15; }
        @Override public int getIconHeight() { return 15; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Đĩa mềm màu tím violet chuẩn biểu tượng Lưu trong ảnh thực tế
            g2.setColor(new Color(110, 60, 140));
            g2.fillRoundRect(x + 1, y + 1, 13, 13, 2, 2);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 4, y + 2, 7, 4);
            g2.fillRect(x + 3, y + 8, 9, 5);
            g2.setColor(new Color(110, 60, 140));
            g2.fillRect(x + 7, y + 2, 2, 3);

            g2.dispose();
        }
    }
}
