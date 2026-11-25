package vn.clinicmanagement.desktop.modules.examination;

import vn.clinicmanagement.desktop.modules.examination.components.KhamBenhHangDoiThanhBen;
import vn.clinicmanagement.desktop.modules.examination.components.KhamBenhManHinhIconVaRenderer;
import static vn.clinicmanagement.desktop.modules.examination.components.KhamBenhManHinhIconVaRenderer.*;
import vn.clinicmanagement.desktop.modules.examination.components.print.LamSangYeuCauPrintPreviewHopThoai;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.KhamBenhBenhNhanTheManHinh;

import vn.clinicmanagement.desktop.modules.examination.components.tabs.DieuTriXuTriTheManHinh;

import vn.clinicmanagement.desktop.modules.examination.components.print.TransferPhieuPrintPreviewHopThoai;

import vn.clinicmanagement.desktop.modules.examination.components.dialogs.TransferPhongSelectHopThoai;

import vn.clinicmanagement.desktop.modules.examination.components.dialogs.TransferPhongConfirmHopThoai;

import vn.clinicmanagement.desktop.modules.examination.components.tabs.VatTuYeuCauTheManHinh;

import vn.clinicmanagement.desktop.modules.examination.components.dialogs.DichVuYeuCauManHinh;

import vn.clinicmanagement.desktop.modules.examination.components.dialogs.DichVuYeuCauHopThoai;

import vn.clinicmanagement.desktop.modules.examination.components.menus.PrintBaoCaoBatLenMenu;

import vn.clinicmanagement.desktop.modules.examination.components.menus.BenhNhanHangDoiNguCanhMenu;

import vn.clinicmanagement.desktop.modules.examination.components.tabs.ChuyenKhoaYeuCauTheManHinh;

import vn.clinicmanagement.desktop.modules.examination.components.tabs.KhamBenhLamSangTheManHinh;

import vn.clinicmanagement.desktop.modules.examination.components.tabs.YTeRecordTheManHinh;

import vn.clinicmanagement.desktop.modules.examination.components.tabs.DonThuocYeuCauTheManHinh;

import vn.clinicmanagement.desktop.modules.examination.components.tabs.DonThuocYeuCauManHinh;

import vn.clinicmanagement.desktop.modules.examination.components.menus.ThuocVatTuBatLenMenu;

import vn.clinicmanagement.desktop.modules.examination.components.dialogs.ReopenYTeRecordHopThoai;

import vn.clinicmanagement.desktop.modules.examination.components.dialogs.YTeVatTuYeuCauManHinh;

import vn.clinicmanagement.desktop.modules.examination.components.dialogs.YTeVatTuYeuCauHopThoai;

import vn.clinicmanagement.desktop.modules.examination.components.dialogs.YTeRecordLoaiHopThoai;

import vn.clinicmanagement.desktop.modules.examination.components.print.YTeRecordPrintPreviewHopThoai;

import vn.clinicmanagement.desktop.modules.examination.components.print.YTeVienPhiPhieuHopThoai;

import vn.clinicmanagement.desktop.modules.examination.components.tabs.XetNghiemYeuCauTheManHinh;

import vn.clinicmanagement.desktop.modules.examination.components.menus.KhamBenhMenuBatLenMenu;

import vn.clinicmanagement.application.examination.KhamBenhHangDoiCongKetNoi;
import vn.clinicmanagement.application.examination.KhamBenhHangDoiMatHang;
import vn.clinicmanagement.application.examination.KhamBenhLichSuMatHang;
import vn.clinicmanagement.domain.facility.Khoa;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;

/**
 * Phân hệ Khám Bệnh - Màn hình điều phối không gian làm việc ngoại trú.
 * Dữ liệu hàng đợi và lịch sử khám đi qua cổng `KhamBenhHangDoiCongKetNoi` trên nền `SwingWorker`;
 * các khu vực hiển thị con được tách sang `KhamBenhHangDoiThanhBen` và `KhamBenhBenhNhanTheManHinh`.
 */
public class KhamBenhManHinh extends CoSoManHinhManHinh {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public static class AdminPatientItem {
        public final String stt;
        public final String name;
        public final String code;
        public final String requestType;
        public final String queueAge;
        public final boolean hasOrangeDot;
        public final String gender;
        public final String birthYear;
        public final String bannerAge;
        public final String formAge;
        public final String birthDate;
        public final String address;
        public final String bannerAddress;
        public final String phone;
        public final String medicalRecordCode;
        public final String admissionDate;
        public final String department;
        public final String doctor;
        public final String diagnosis;
        public final String invoiceCode;
        public final int patientId;
        public final int visitId;
        public final String visitStatus;
        public final String registrationType;
        public final String departmentName;
        public final String clinicRoomName;
        public final String ethnicity;
        public final String occupation;
        public final String nationality;
        public final String cccd;
        public final String bhytNumber;
        public final int bhytRate;
        public final String dischargeTime;

        public AdminPatientItem(String stt, String name, String code, String requestType, String queueAge,
                                boolean hasOrangeDot, String gender, String birthYear, String bannerAge, String formAge,
                                String birthDate, String address, String bannerAddress, String phone,
                                String medicalRecordCode, String admissionDate, String department, String doctor,
                                String diagnosis, String invoiceCode) {
            this(stt, name, code, requestType, queueAge, hasOrangeDot, gender, birthYear, bannerAge, formAge,
                    birthDate, address, bannerAddress, phone, medicalRecordCode, admissionDate, department, doctor,
                    diagnosis, invoiceCode, 0, parseVisitIdFromInvoice(invoiceCode),
                    hasOrangeDot ? "CHO_KHAM" : "HOAN_THANH", "Khám bệnh", "Khoa Khám Bệnh", "",
                    "Kinh", "", "Việt Nam", "", "", 0, "");
        }

        public AdminPatientItem(String stt, String name, String code, String requestType, String queueAge,
                                boolean hasOrangeDot, String gender, String birthYear, String bannerAge, String formAge,
                                String birthDate, String address, String bannerAddress, String phone,
                                String medicalRecordCode, String admissionDate, String department, String doctor,
                                String diagnosis, String invoiceCode, int patientId, int visitId, String visitStatus,
                                String registrationType, String departmentName, String clinicRoomName,
                                String ethnicity, String occupation, String nationality, String cccd,
                                String bhytNumber, int bhytRate, String dischargeTime) {
            this.stt = stt;
            this.name = name;
            this.code = code;
            this.requestType = requestType;
            this.queueAge = queueAge;
            this.hasOrangeDot = hasOrangeDot;
            this.gender = gender;
            this.birthYear = birthYear;
            this.bannerAge = bannerAge;
            this.formAge = formAge;
            this.birthDate = birthDate;
            this.address = address;
            this.bannerAddress = bannerAddress;
            this.phone = phone;
            this.medicalRecordCode = medicalRecordCode;
            this.admissionDate = admissionDate;
            this.department = department;
            this.doctor = doctor;
            this.diagnosis = diagnosis;
            this.invoiceCode = invoiceCode;
            this.patientId = patientId;
            this.visitId = visitId;
            this.visitStatus = visitStatus != null ? visitStatus : (hasOrangeDot ? "CHO_KHAM" : "HOAN_THANH");
            this.registrationType = registrationType;
            this.departmentName = departmentName;
            this.clinicRoomName = clinicRoomName;
            this.ethnicity = ethnicity;
            this.occupation = occupation;
            this.nationality = nationality;
            this.cccd = cccd;
            this.bhytNumber = bhytNumber;
            this.bhytRate = bhytRate;
            this.dischargeTime = dischargeTime;
        }
    }

    private final KhamBenhGhepNoiGoc.Services services;
    private final AtomicLong queueRefreshSequence = new AtomicLong();
    private final AtomicLong historyLoadSequence = new AtomicLong();
    private final AtomicLong filterMenuSequence = new AtomicLong();
    private int selectedDepartmentId;
    private int selectedRoomId;
    private String selectedDepartmentName;
    private String selectedRoomName;
    private final Map<Integer, String> authorizedDepartmentNames = new HashMap<>();
    private final Map<Integer, String> availableRoomNames = new HashMap<>();

    private CardLayout cardLayout;
    private JPanel cardContainer;
    private KhamBenhHangDoiThanhBen queueSidebar;
    private KhamBenhBenhNhanTheManHinh patientTabPanel;
    private KhamBenhLamSangTheManHinh clinicalExamTabPanel;
    private YTeVatTuYeuCauManHinh suppliesOrderPanel;
    private DonThuocYeuCauManHinh prescriptionOrderPanel;
    private DichVuYeuCauManHinh serviceOrderPanel;

    private JTabbedPane clinicalTabs;
    private VatTuYeuCauTheManHinh suppliesTabPanel;
    private DonThuocYeuCauTheManHinh prescriptionTabPanel;
    private XetNghiemYeuCauTheManHinh labOrderTabPanel;
    private ChuyenKhoaYeuCauTheManHinh specialtyOrderTabPanel;
    private YTeRecordTheManHinh medicalRecordTabPanel;
    private DieuTriXuTriTheManHinh treatmentResolutionTabPanel;

    private JLabel lblBannerLine1;
    private JLabel lblBannerReqTag;
    private JLabel lblBannerLine2;
    private JLabel lblBannerLine3;

    public KhamBenhManHinh() {
        this(resolveDefaultServices());
    }

    public KhamBenhManHinh(KhamBenhGhepNoiGoc.Services services) {
        super("examination", "Khám Bệnh", "🩺");
        this.services = services != null ? services : resolveDefaultServices();
        var loginContext = KhamBenhPhienNguCanh.current();
        if (loginContext != null && "DOCTOR".equalsIgnoreCase(loginContext.roleCode())) {
            selectedDepartmentId = loginContext.departmentId();
            selectedDepartmentName = vn.clinicmanagement.desktop.core.session.NguoiDungPhien.getInstance().getDepartmentName();
        }
        buildUI();
        updateClinicSelectionLabel();
        applyQueueItems(fetchQueueItemsSafely(selectedDepartmentId, selectedRoomId), null);
    }

    private static KhamBenhGhepNoiGoc.Services resolveDefaultServices() {
        try {
            return KhamBenhGhepNoiGoc.createProduction();
        } catch (Exception ex) {
            return null;
        }
    }

    static int parseVisitIdFromInvoice(String invoiceCode) {
        if (invoiceCode != null && invoiceCode.startsWith("VP-")) {
            try {
                return Integer.parseInt(invoiceCode.substring(3).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    public static boolean isCompletedOrCancelledVisitStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "DA_KHAM", "KET_LUAN", "HOAN_THANH", "HOAN_TAT", "DA_HUY" -> true;
            default -> false;
        };
    }

    public static String formatVisitStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Chờ khám";
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "CHO_KHAM" -> "Chờ khám";
            case "DANG_KHAM" -> "Đang khám";
            case "CHO_CLS" -> "Chờ cận lâm sàng";
            case "DA_KHAM" -> "Đã khám";
            case "KET_LUAN" -> "Kết luận";
            case "HOAN_THANH", "HOAN_TAT" -> "Hoàn thành";
            case "DA_HUY" -> "Đã hủy";
            default -> status.trim();
        };
    }

    private static String formatPayerType(String doiTuongKham) {
        if ("BHYT".equalsIgnoreCase(doiTuongKham)) {
            return "BHYT";
        }
        if ("DICH_VU_YEU_CAU".equalsIgnoreCase(doiTuongKham)) {
            return "Yêu cầu";
        }
        return "Viện phí";
    }

    private List<AdminPatientItem> fetchQueueItemsSafely(int departmentId, int roomId) {
        if (services == null) {
            return List.of();
        }
        KhamBenhHangDoiCongKetNoi examQueue = services.examQueue();
        var context = KhamBenhPhienNguCanh.current();
        int facilityId = context != null ? context.facilityId() : 0;
        if (facilityId <= 0) {
            return List.of();
        }
        if ("DOCTOR".equalsIgnoreCase(context.roleCode())) {
            departmentId = context.departmentId();
        }
        try {
            LocalDate fromDate = queueSidebar != null ? queueSidebar.getFromDate() : LocalDate.now();
            LocalDate toDate = queueSidebar != null ? queueSidebar.getToDate() : LocalDate.now();
            List<KhamBenhHangDoiMatHang> rawItems = examQueue.getQueue(facilityId,
                    context.employeeId(), context.roleCode(), departmentId, roomId, fromDate, toDate);
            List<AdminPatientItem> items = new ArrayList<>(rawItems.size());
            int stt = 1;
            int currentYear = LocalDate.now().getYear();
            for (KhamBenhHangDoiMatHang raw : rawItems) {
                int birthYear = raw.birthDate() != null ? raw.birthDate().getYear() : 1990;
                int age = currentYear - birthYear;
                String genderStr = "F".equalsIgnoreCase(raw.genderCode()) ? "Nữ"
                        : ("O".equalsIgnoreCase(raw.genderCode()) ? "Khác" : "Nam");
                String admissionTime = raw.admissionTime() != null ? raw.admissionTime().format(DATE_TIME_FORMATTER) : "";
                String dischargeTime = raw.dischargeTime() != null ? raw.dischargeTime().format(DATE_TIME_FORMATTER) : "";
                String deptName = raw.departmentName().isBlank() ? "Khoa Khám Bệnh" : raw.departmentName();
                String dept = raw.clinicRoomName().isBlank() ? deptName : deptName + " > " + raw.clinicRoomName();
                String payerLabel = formatPayerType(raw.payerType());
                String bhytNo = "BHYT".equals(payerLabel) ? raw.bhytNumber() : "";
                int bhytRate = "BHYT".equals(payerLabel) ? raw.bhytRate() : 0;
                int idRef = raw.visitId() > 0 ? raw.visitId() : raw.patientId();

                items.add(new AdminPatientItem(
                        raw.queueNumberDisplay() != null ? raw.queueNumberDisplay() : String.valueOf(stt),
                        raw.fullName(),
                        raw.patientCode() != null ? raw.patientCode() : "BN-" + raw.patientId(),
                        payerLabel,
                        String.valueOf(age),
                        !isCompletedOrCancelledVisitStatus(raw.visitStatus()),
                        genderStr,
                        String.valueOf(birthYear),
                        age + " tuổi",
                        String.valueOf(age),
                        raw.birthDate() != null ? raw.birthDate().toString() : String.valueOf(birthYear),
                        raw.address(),
                        raw.address(),
                        raw.phone(),
                        "BA-" + idRef,
                        admissionTime,
                        dept,
                        raw.doctorName().isBlank() ? "Chưa phân công" : raw.doctorName(),
                        raw.reason(),
                        "VP-" + idRef,
                        raw.patientId(),
                        raw.visitId(),
                        raw.visitStatus(),
                        raw.registrationType().isBlank() ? "Khám bệnh" : raw.registrationType(),
                        deptName,
                        raw.clinicRoomName(),
                        raw.ethnicity(),
                        raw.occupation(),
                        raw.nationality(),
                        raw.cccd(),
                        bhytNo,
                        bhytRate,
                        dischargeTime
                ));
                stt++;
            }
            return items;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);

        JPanel mainExamView = new JPanel(new BorderLayout());
        mainExamView.setBackground(Color.WHITE);

        // 1. Thanh công cụ phụ + Banner bệnh nhân
        JPanel topToolbar = buildTopToolbar();
        mainExamView.add(topToolbar, BorderLayout.NORTH);

        // 2. Main split: Hàng đợi bên trái (300px) + Hồ sơ khám bên phải
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.24);
        splitPane.setDividerSize(3);
        splitPane.setBorder(null);

        queueSidebar = new KhamBenhHangDoiThanhBen(
                this::selectPatient,
                (invoker, x, y, row, patient) -> showQueueContextMenu(invoker, x, y),
                this::openDepartmentFilter,
                this::openRoomFilter
        );
        queueSidebar.setOnDateRangeChanged(this::refreshData);
        splitPane.setLeftComponent(queueSidebar);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(new Color(224, 235, 248));

        clinicalTabs = KhamBenhManHinhIconVaRenderer.createStyledTabbedPane();
        patientTabPanel = new KhamBenhBenhNhanTheManHinh();
        clinicalTabs.addTab("Bệnh Nhân", new SmallFormIcon(), patientTabPanel);
        clinicalExamTabPanel = new KhamBenhLamSangTheManHinh(services);
        clinicalExamTabPanel.setOnSavedCallback(() -> refreshData(clinicalExamTabPanel));
        clinicalExamTabPanel.setOnTreatmentSheetCallback(this::openTreatmentSheetDialog);
        clinicalTabs.addTab("Khám Bệnh", clinicalExamTabPanel);
        medicalRecordTabPanel = new YTeRecordTheManHinh(services);
        clinicalTabs.addTab("Bệnh Án", medicalRecordTabPanel);
        clinicalTabs.addTab("Công Khám(1)", createPlaceholderTab("Dịch vụ công khám chuyên khoa"));
        labOrderTabPanel = new XetNghiemYeuCauTheManHinh(() -> cardLayout.show(cardContainer, "SERVICE_ORDER"), services);
        labOrderTabPanel.setOnLabOrderCountChanged(count -> {
            int tabIndex = clinicalTabs.indexOfComponent(labOrderTabPanel);
            if (tabIndex >= 0) {
                clinicalTabs.setTitleAt(tabIndex, "Xét Nghiệm(" + count + ")");
            }
        });
        labOrderTabPanel.setOnOrdersChangedCallback(() -> refreshData(labOrderTabPanel));
        clinicalTabs.addTab("Xét Nghiệm(0)", labOrderTabPanel);
        clinicalTabs.addTab("CDHA(2)", createPlaceholderTab("Chẩn đoán hình ảnh: Siêu âm, X-Quang"));
        specialtyOrderTabPanel = new ChuyenKhoaYeuCauTheManHinh(services);
        specialtyOrderTabPanel.setOnOrderCountChanged(count -> {
            int tabIndex = clinicalTabs.indexOfComponent(specialtyOrderTabPanel);
            if (tabIndex >= 0) {
                clinicalTabs.setTitleAt(tabIndex, "Chuyên khoa(" + count + ")");
            }
        });
        specialtyOrderTabPanel.setOnOrdersChangedCallback(() -> refreshData(specialtyOrderTabPanel));
        clinicalTabs.addTab("Chuyên khoa(0)", specialtyOrderTabPanel);
        prescriptionTabPanel = new DonThuocYeuCauTheManHinh(services);
        prescriptionTabPanel.setOnPrescriptionCountChanged(count -> {
            int tabIndex = clinicalTabs.indexOfComponent(prescriptionTabPanel);
            if (tabIndex >= 0) {
                clinicalTabs.setTitleAt(tabIndex, "Thuốc(" + count + ")");
            }
        });
        prescriptionTabPanel.setOnPrescriptionsChangedCallback(() -> refreshData(prescriptionTabPanel));
        clinicalTabs.addTab("Thuốc(0)", prescriptionTabPanel);
        suppliesTabPanel = new VatTuYeuCauTheManHinh(services);
        suppliesTabPanel.setOnSupplyCountChanged(count -> {
            int tabIndex = clinicalTabs.indexOfComponent(suppliesTabPanel);
            if (tabIndex >= 0) {
                clinicalTabs.setTitleAt(tabIndex, "Vật Tư(" + count + ")");
            }
        });
        suppliesTabPanel.setOnSuppliesChangedCallback(() -> refreshData(suppliesTabPanel));
        clinicalTabs.addTab("Vật Tư(0)", suppliesTabPanel);
        treatmentResolutionTabPanel = new DieuTriXuTriTheManHinh(services);
        treatmentResolutionTabPanel.setOnDispositionSavedCallback(() -> refreshData(treatmentResolutionTabPanel));
        clinicalTabs.addTab("Xử Trí", treatmentResolutionTabPanel);
        clinicalTabs.addTab("BA Điện Tử(2)", createPlaceholderTab("Hồ sơ Bệnh Án Điện Tử (EMR Version 2.0)"));

        rightPanel.add(clinicalTabs, BorderLayout.CENTER);
        splitPane.setRightComponent(rightPanel);
        mainExamView.add(splitPane, BorderLayout.CENTER);

        // 3. Các màn hình chỉ định lồng trong CardLayout
        suppliesOrderPanel = new YTeVatTuYeuCauManHinh(() -> {
            cardLayout.show(cardContainer, "EXAM_MAIN");
            refreshData(suppliesTabPanel);
            clinicalTabs.setSelectedComponent(suppliesTabPanel);
        }, services);
        suppliesOrderPanel.setOnSuppliesSavedCallback(supplies -> {
            if (suppliesTabPanel != null) {
                suppliesTabPanel.loadSuppliesForVisit(
                        suppliesOrderPanel.getPatient() != null ? suppliesOrderPanel.getPatient().visitId : 0);
            }
        });

        prescriptionOrderPanel = new DonThuocYeuCauManHinh(() -> {
            cardLayout.show(cardContainer, "EXAM_MAIN");
            refreshData(prescriptionTabPanel);
            clinicalTabs.setSelectedComponent(prescriptionTabPanel);
        }, services);

        serviceOrderPanel = new DichVuYeuCauManHinh(() -> {
            cardLayout.show(cardContainer, "EXAM_MAIN");
            refreshData(labOrderTabPanel);
            clinicalTabs.setSelectedComponent(labOrderTabPanel);
        }, services);

        cardContainer.add(mainExamView, "EXAM_MAIN");
        cardContainer.add(suppliesOrderPanel, "SUPPLIES_ORDER");
        cardContainer.add(prescriptionOrderPanel, "PRESCRIPTION_ORDER");
        cardContainer.add(serviceOrderPanel, "SERVICE_ORDER");

        int shortcutMask = resolveShortcutMask(
                GraphicsEnvironment.isHeadless(),
                () -> Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        KeyStroke ctrlT = KeyStroke.getKeyStroke('T', shortcutMask);
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ctrlT, "openPrescriptionOrder");
        getActionMap().put("openPrescriptionOrder", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cardLayout.show(cardContainer, "PRESCRIPTION_ORDER");
            }
        });

        add(cardContainer, BorderLayout.CENTER);
    }

    private JPanel buildTopToolbar() {
        JPanel topToolbar = new JPanel(new BorderLayout());
        topToolbar.setBackground(new Color(246, 248, 252));
        topToolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 214, 226)));
        topToolbar.setPreferredSize(new Dimension(100, 56));

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        leftActions.setOpaque(false);
        leftActions.add(createToolbarButton("Menu", "menu", false));
        leftActions.add(createToolbarButton("In", "print", true));
        leftActions.add(createToolbarButton("Làm mới", "refresh", false));
        leftActions.add(createToolbarButton("Tìm Kiếm", "search", false));
        leftActions.add(createToolbarButton("Dịch Vụ", "service", true));
        leftActions.add(createToolbarButton("Thuốc/VT", "medicine", true));
        leftActions.add(createToolbarButton("Thanh Toán", "payment", false));
        leftActions.add(createToolbarButton("Màn Hình Chờ", "waiting", false));
        topToolbar.add(leftActions, BorderLayout.WEST);

        JPanel headerPatientBanner = new JPanel(new BorderLayout(0, 0));
        headerPatientBanner.setOpaque(false);
        headerPatientBanner.setBorder(new EmptyBorder(2, 8, 2, 10));

        JPanel bannerTextPanel = new JPanel(new GridLayout(3, 1, 0, 0));
        bannerTextPanel.setOpaque(false);
        bannerTextPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(205, 214, 226)),
                new EmptyBorder(0, 0, 0, 10)
        ));

        lblBannerLine1 = new JLabel("", SwingConstants.RIGHT);
        lblBannerLine1.setFont(UngDungPhongChu.SECTION);
        lblBannerLine1.setForeground(new Color(125, 15, 15));

        lblBannerLine2 = new JLabel("", SwingConstants.RIGHT);
        lblBannerLine2.setFont(UngDungPhongChu.BODY);
        lblBannerLine2.setForeground(new Color(125, 15, 15));

        lblBannerLine3 = new JLabel("", SwingConstants.RIGHT);
        lblBannerLine3.setFont(UngDungPhongChu.BODY);
        lblBannerLine3.setForeground(new Color(125, 15, 15));

        bannerTextPanel.add(lblBannerLine1);
        bannerTextPanel.add(lblBannerLine2);
        bannerTextPanel.add(lblBannerLine3);

        lblBannerReqTag = new JLabel("", SwingConstants.CENTER);
        lblBannerReqTag.setFont(UngDungPhongChu.BODY_BOLD);
        lblBannerReqTag.setForeground(new Color(125, 15, 15));
        lblBannerReqTag.setBorder(new EmptyBorder(0, 10, 0, 4));

        headerPatientBanner.add(bannerTextPanel, BorderLayout.CENTER);
        headerPatientBanner.add(lblBannerReqTag, BorderLayout.EAST);
        topToolbar.add(headerPatientBanner, BorderLayout.EAST);
        return topToolbar;
    }

    static int resolveShortcutMask(boolean headless, IntSupplier platformMaskSupplier) {
        if (headless) {
            return InputEvent.CTRL_DOWN_MASK;
        }
        try {
            return platformMaskSupplier.getAsInt();
        } catch (RuntimeException ex) {
            return InputEvent.CTRL_DOWN_MASK;
        }
    }

    private void applyQueueItems(List<AdminPatientItem> items, String preferredInvoiceCode) {
        if (queueSidebar == null) return;
        int target = queueSidebar.setPatients(items, preferredInvoiceCode);
        if (!items.isEmpty()) {
            selectPatientByIndex(target >= 0 ? target : 0);
        } else {
            lblBannerLine1.setText("Không có bệnh nhân trong danh sách khám");
            lblBannerLine2.setText("");
            lblBannerLine3.setText("");
            lblBannerReqTag.setText("");
            if (patientTabPanel != null) {
                patientTabPanel.clearHistory();
            }
            if (labOrderTabPanel != null) {
                labOrderTabPanel.setPatientContext(null);
                labOrderTabPanel.loadServicesForVisit(0);
            }
        }
    }

    private void selectPatient(AdminPatientItem p) {
        if (p == null) return;

        boolean isBhyt = p.requestType != null && (p.requestType.contains("BHYT") || p.requestType.contains("hiểm"));
        lblBannerLine1.setText(p.name + " | " + p.code + " | " + p.birthYear + " (" + p.bannerAge + ") | " + p.gender);
        lblBannerLine2.setText("VP: " + p.invoiceCode + " | " + p.requestType);
        lblBannerLine3.setText(p.bannerAddress + " | " + p.phone);
        String rateHtml = p.bhytRate > 0 ? "<br><font color='#0044CC'><b>" + p.bhytRate + "%</b></font>" : "";
        lblBannerReqTag.setText(isBhyt
                ? "<html><center><font color='#800000'><b>BHYT</b></font>" + rateHtml + "</center></html>"
                : "<html><center><font color='#800000'><b>" + p.requestType + "</b></font></center></html>");

        if (patientTabPanel != null) {
            patientTabPanel.bindPatient(p);
        }
        if (treatmentResolutionTabPanel != null) {
            treatmentResolutionTabPanel.setPatientInfo(
                    p.code, p.name, p.formAge, p.gender, p.address,
                    p.doctor != null ? p.doctor : "Chưa phân công",
                    p.diagnosis != null ? p.diagnosis : "");
        }

        loadPatientHistory(p);

        int visitId = p.visitId > 0 ? p.visitId : 0;
        if (visitId <= 0) {
            visitId = parseVisitIdFromInvoice(p.invoiceCode);
        }
        if (visitId <= 0 && p.code != null && p.code.contains("-")) {
            try {
                visitId = Integer.parseInt(p.code.substring(p.code.lastIndexOf('-') + 1));
            } catch (NumberFormatException ignored) {
            }
        }

        var context = KhamBenhPhienNguCanh.current();
        int doctorId = context != null
                && ("DOCTOR".equals(context.roleCode()) || "ADMIN".equals(context.roleCode()))
                ? context.employeeId()
                : 0;
        int facilityId = context != null ? context.facilityId() : 0;
        if (prescriptionOrderPanel != null) {
            prescriptionOrderPanel.setVisitContext(visitId, doctorId, facilityId);
            prescriptionOrderPanel.setPatientContext(p);
        }
        if (prescriptionTabPanel != null) {
            prescriptionTabPanel.setPatientContext(p);
            prescriptionTabPanel.setVisitContext(visitId, doctorId);
        }
        if (serviceOrderPanel != null) {
            serviceOrderPanel.setVisitContext(visitId, doctorId, facilityId);
            serviceOrderPanel.setPatientContext(p);
        }
        if (labOrderTabPanel != null) {
            labOrderTabPanel.setPatientContext(p);
            labOrderTabPanel.loadServicesForVisit(visitId);
        }
        if (specialtyOrderTabPanel != null) {
            specialtyOrderTabPanel.setPatientContext(p);
            specialtyOrderTabPanel.loadServicesForVisit(visitId);
        }
        if (treatmentResolutionTabPanel != null) {
            treatmentResolutionTabPanel.setVisitContext(visitId, doctorId);
        }
        if (clinicalExamTabPanel != null) {
            clinicalExamTabPanel.setVisitContext(visitId, doctorId);
        }
        if (medicalRecordTabPanel != null) {
            medicalRecordTabPanel.setVisitContext(visitId, doctorId);
        }
        if (suppliesTabPanel != null) {
            suppliesTabPanel.setPatient(p);
            suppliesTabPanel.setVisitContext(visitId, doctorId);
        }
        if (suppliesOrderPanel != null) {
            suppliesOrderPanel.setVisitContext(visitId, doctorId, facilityId);
            suppliesOrderPanel.setPatient(p);
        }
    }

    private void loadPatientHistory(AdminPatientItem p) {
        if (patientTabPanel == null || p == null) return;
        final long sequence = historyLoadSequence.incrementAndGet();
        if (services == null || p.patientId <= 0) {
            patientTabPanel.setVisitHistory(p, List.of());
            return;
        }
        // Hiển thị nhanh trạng thái hiện tại, đồng thời tải toàn bộ lịch sử lượt khám trên luồng nền
        patientTabPanel.setVisitHistory(p, List.of());
        new SwingWorker<List<KhamBenhLichSuMatHang>, Void>() {
            @Override
            protected List<KhamBenhLichSuMatHang> doInBackground() {
                try {
                    return services.examQueue().getPatientVisitHistory(p.patientId);
                } catch (Exception ex) {
                    return List.of();
                }
            }

            @Override
            protected void done() {
                if (sequence != historyLoadSequence.get()) return;
                try {
                    List<KhamBenhLichSuMatHang> rows = get();
                    if (rows != null && !rows.isEmpty()) {
                        patientTabPanel.setVisitHistory(p, rows);
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    public void selectPatientByIndex(int index) {
        if (queueSidebar != null && queueSidebar.selectByIndex(index)) {
            selectPatient(queueSidebar.getSelectedPatient());
        }
    }

    public void selectPatientByName(String name) {
        if (name == null || queueSidebar == null) return;
        List<AdminPatientItem> items = queueSidebar.getPatientList();
        for (int i = 0; i < items.size(); i++) {
            if (name.equalsIgnoreCase(items.get(i).name)) {
                selectPatientByIndex(i);
                return;
            }
        }
    }

    public void showQueueContextMenu(Component comp, int x, int y) {
        AdminPatientItem patient = queueSidebar != null ? queueSidebar.getSelectedPatient() : null;
        String name = patient != null ? patient.name : "Bệnh nhân";
        String code = patient != null ? patient.code : "";
        boolean isExamined = patient != null && !patient.hasOrangeDot;

        BenhNhanHangDoiNguCanhMenu menu = new BenhNhanHangDoiNguCanhMenu(name, code, isExamined, action -> {
            handlePatientQueueAction(action, patient);
        });
        menu.show(comp, x, y);
    }

    private void handlePatientQueueAction(String action, AdminPatientItem patient) {
        String name = patient != null ? patient.name : "Bệnh nhân";
        String code = patient != null ? patient.code : "";

        if ("Bệnh Án".equalsIgnoreCase(action)) {
            selectMedicalRecordTab();
        } else if ("Mở Lại Bệnh Án".equalsIgnoreCase(action)) {
            Window owner = SwingUtilities.getWindowAncestor(this);
            ReopenYTeRecordHopThoai dlg = new ReopenYTeRecordHopThoai(owner, patient, req -> {
                if ("Mở lại bệnh án, huỷ xử trí".equals(req.reopenType)
                        || "Mở lại bệnh án do yêu cầu".equals(req.reopenType)
                        || "Mở tạm để sửa chữa hồ sơ (có cho xử trí lại)".equals(req.reopenType)) {
                    selectTreatmentTab();
                    refreshData(treatmentResolutionTabPanel);
                } else {
                    selectMedicalRecordTab();
                    refreshData(medicalRecordTabPanel);
                }
            });
            dlg.setVisible(true);
        } else if ("Lịch Sử Khám Bệnh".equalsIgnoreCase(action)) {
            selectMedicalRecordTab();
            JOptionPane.showMessageDialog(this,
                    "Đang mở Lịch Sử Khám Bệnh của bệnh nhân: " + name + " (" + code + ")",
                    "Lịch Sử Khám Bệnh", JOptionPane.INFORMATION_MESSAGE);
        } else if ("Chuyển Bệnh Nhân Sang Phòng Khác".equalsIgnoreCase(action)) {
            if (patient == null || patient.visitId <= 0 || services == null || services.transferVisit() == null) {
                JOptionPane.showMessageDialog(this, "Chưa chọn lượt khám hợp lệ để chuyển phòng.",
                        "Chuyển Phòng Khám", JOptionPane.WARNING_MESSAGE);
                return;
            }
            var auth = KhamBenhPhienNguCanh.current();
            if (auth == null || !auth.isValid()) {
                JOptionPane.showMessageDialog(this, "Phiên làm việc không hợp lệ.",
                        "Chuyển Phòng Khám", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (TransferPhongConfirmHopThoai.showConfirm(this)) {
                List<PhongKham> activeRooms;
                try {
                    activeRooms = services.examQueue().getActiveClinicRooms(auth.facilityId());
                } catch (RuntimeException ex) {
                    JOptionPane.showMessageDialog(this, "Không tải được danh sách phòng khám. Vui lòng thử lại.",
                            "Chuyển Phòng Khám", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (activeRooms.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Không có phòng khám đang hoạt động trong cơ sở hiện tại.",
                            "Chuyển Phòng Khám", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                var chosenRoom = TransferPhongSelectHopThoai.selectRoom(this, activeRooms);
                if (chosenRoom != null) {
                    Window owner = SwingUtilities.getWindowAncestor(this);
                    new SwingWorker<String, Void>() {
                        @Override protected String doInBackground() {
                            var res = services.transferVisit().handle(new vn.clinicmanagement.application.examination.transfer.TransferLuotKhamCommand(
                                    patient.visitId,
                                    chosenRoom.getId(),
                                    vn.clinicmanagement.domain.examination.TransferLyDoMa.CLINICAL_SPECIALTY_REFERRAL,
                                    "Chuyển phòng khám từ danh sách chờ",
                                    auth));
                            if (res != null && res.isSuccess()) return null;
                            return res == null ? "Không nhận được kết quả chuyển phòng."
                                    : res.getError().map(err -> err.message()).orElse("Không thể chuyển phòng khám.");
                        }
                        @Override protected void done() {
                            try {
                                String err = get();
                                if (err != null) {
                                    JOptionPane.showMessageDialog(KhamBenhManHinh.this, err,
                                            "Không thể chuyển phòng khám", JOptionPane.ERROR_MESSAGE);
                                    return;
                                }
                                refreshData(null);
                                new TransferPhieuPrintPreviewHopThoai(owner, name, code, chosenRoom.getTenPhong()).setVisible(true);
                            } catch (Exception ignored) {}
                        }
                    }.execute();
                }
            }
        } else if ("Hủy tiếp đón".equalsIgnoreCase(action)) {
            handleCancelReception(patient);
        } else if ("Gọi Bệnh Nhân".equalsIgnoreCase(action)) {
            handleCallPatient(patient);
        } else if ("Đặt Hẹn".equalsIgnoreCase(action)) {
            handleBookAppointment(patient);
        } else if ("Sửa Thông Tin Bố, Mẹ, NND".equalsIgnoreCase(action)) {
            handleEditGuardianInfo(patient);
        } else if ("Thông Tin Ghi Chú".equalsIgnoreCase(action)) {
            handleVisitNote(patient);
        } else if ("Lịch sử khám lại".equalsIgnoreCase(action)) {
            handleReexamHistory(patient);
        } else if ("In vòng tay người bệnh".equalsIgnoreCase(action)) {
            handlePrintPatientBandOrCard(patient, true);
        } else if ("In thẻ khám bệnh".equalsIgnoreCase(action)) {
            handlePrintPatientBandOrCard(patient, false);
        } else if (action != null && action.startsWith("Đổi đối tượng khám")) {
            handleChangeVisitSubject(patient);
        } else if (action != null && action.startsWith("Đồng bộ dữ liệu EMR")) {
            handleSyncEmr(patient);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Đang thực hiện chức năng: " + action + "\n▶ Bệnh nhân: " + name + " (" + code + ")",
                    "Thao Tác Bệnh Nhân", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private vn.clinicmanagement.application.examination.KhamBenhQueueActionsPort queueActionsPort;

    public void setQueueActionsPort(vn.clinicmanagement.application.examination.KhamBenhQueueActionsPort port) {
        this.queueActionsPort = port;
    }

    private vn.clinicmanagement.application.examination.KhamBenhQueueActionsPort resolveQueueActionsPort() {
        if (queueActionsPort != null) return queueActionsPort;
        if (GraphicsEnvironment.isHeadless() || System.getProperty("surefire.test.class.path") != null) return null;
        return new vn.clinicmanagement.infrastructure.persistence.examination.KhamBenhQueueActionsRepository(
                new vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap(
                        new vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy(
                                new vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork())));
    }

    public vn.clinicmanagement.desktop.modules.examination.components.dialogs.QuanLyBuongGiuongNoiTruHopThoai createInpatientBedPanel() {
        return new vn.clinicmanagement.desktop.modules.examination.components.dialogs.QuanLyBuongGiuongNoiTruHopThoai(
                resolveQueueActionsPort());
    }

    public void openInpatientBedDialog() {
        if (GraphicsEnvironment.isHeadless()) return;
        var panel = createInpatientBedPanel();
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                "EHC - Quản Lý Buồng / Giường Nội Trú", Dialog.ModalityType.MODELESS);
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dlg.setContentPane(panel);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void handleCallPatient(AdminPatientItem patient) {
        var port = resolveQueueActionsPort();
        var auth = KhamBenhPhienNguCanh.current();
        if (patient == null || patient.visitId <= 0 || port == null || auth == null) return;
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                try { port.callPatientToRoom(patient.visitId, auth.facilityId(), auth.employeeId()); return null; }
                catch (Exception ex) { return ex.getMessage(); }
            }
            @Override protected void done() {
                try {
                    String err = get();
                    if (err != null) JOptionPane.showMessageDialog(KhamBenhManHinh.this, err, "Gọi Bệnh Nhân", JOptionPane.ERROR_MESSAGE);
                    else {
                        refreshData(null);
                        JOptionPane.showMessageDialog(KhamBenhManHinh.this,
                                "Đã gọi bệnh nhân " + patient.name + " (" + patient.code + ") vào phòng khám.",
                                "Gọi Bệnh Nhân", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void handleBookAppointment(AdminPatientItem patient) {
        var port = resolveQueueActionsPort();
        var auth = KhamBenhPhienNguCanh.current();
        if (patient == null || patient.patientId <= 0 || port == null || auth == null || GraphicsEnvironment.isHeadless()) return;
        JTextField txtDate = new JTextField(java.time.LocalDate.now().plusDays(7).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), 12);
        JTextField txtReason = new JTextField("Tái khám", 20);
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 6));
        p.add(new JLabel("Bệnh nhân:")); p.add(new JLabel(patient.name + " (" + patient.code + ")"));
        p.add(new JLabel("Ngày hẹn (dd/MM/yyyy):")); p.add(txtDate);
        p.add(new JLabel("Lý do hẹn:")); p.add(txtReason);
        if (JOptionPane.showConfirmDialog(this, p, "Đặt Hẹn Tái Khám", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        java.time.LocalDateTime apptTime;
        try {
            apptTime = java.time.LocalDate.parse(txtDate.getText().trim(), java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")).atTime(8, 0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ngày hẹn phải có dạng dd/MM/yyyy.", "Kiểm tra ngày", JOptionPane.WARNING_MESSAGE);
            return;
        }
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                try { port.bookFollowUpAppointment(patient.visitId, patient.patientId, auth.employeeId(), auth.facilityId(), apptTime, txtReason.getText().trim()); return null; }
                catch (Exception ex) { return ex.getMessage(); }
            }
            @Override protected void done() {
                try {
                    String err = get();
                    if (err != null) JOptionPane.showMessageDialog(KhamBenhManHinh.this, err, "Lỗi đặt hẹn", JOptionPane.ERROR_MESSAGE);
                    else JOptionPane.showMessageDialog(KhamBenhManHinh.this, "Đã lưu lịch hẹn tái khám vào CSDL.", "Đặt Hẹn", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void handleEditGuardianInfo(AdminPatientItem patient) {
        var port = resolveQueueActionsPort();
        var auth = KhamBenhPhienNguCanh.current();
        if (patient == null || patient.patientId <= 0 || port == null || auth == null || GraphicsEnvironment.isHeadless()) return;
        new SwingWorker<vn.clinicmanagement.application.examination.KhamBenhQueueActionsPort.GuardianInfo, Void>() {
            @Override protected vn.clinicmanagement.application.examination.KhamBenhQueueActionsPort.GuardianInfo doInBackground() {
                return port.getGuardianInfo(patient.patientId);
            }
            @Override protected void done() {
                try {
                    var current = get();
                    JTextField txtName = new JTextField(current.contactName(), 20);
                    JTextField txtPhone = new JTextField(current.contactPhone(), 15);
                    JPanel p = new JPanel(new GridLayout(0, 2, 8, 6));
                    p.add(new JLabel("Họ tên Bố/Mẹ/NND:")); p.add(txtName);
                    p.add(new JLabel("SĐT liên hệ:")); p.add(txtPhone);
                    if (JOptionPane.showConfirmDialog(KhamBenhManHinh.this, p, "Sửa Thông Tin Bố, Mẹ, NND", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                        var next = new vn.clinicmanagement.application.examination.KhamBenhQueueActionsPort.GuardianInfo(txtName.getText().trim(), txtPhone.getText().trim());
                        port.updateGuardianInfo(patient.patientId, next, auth.employeeId());
                        JOptionPane.showMessageDialog(KhamBenhManHinh.this, "Đã cập nhật thông tin người thân.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(KhamBenhManHinh.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleVisitNote(AdminPatientItem patient) {
        var port = resolveQueueActionsPort();
        var auth = KhamBenhPhienNguCanh.current();
        if (patient == null || patient.visitId <= 0 || port == null || auth == null || GraphicsEnvironment.isHeadless()) return;
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() { return port.getVisitNote(patient.visitId); }
            @Override protected void done() {
                try {
                    JTextArea area = new JTextArea(get(), 5, 32);
                    area.setLineWrap(true); area.setWrapStyleWord(true);
                    if (JOptionPane.showConfirmDialog(KhamBenhManHinh.this, new JScrollPane(area), "Thông Tin Ghi Chú - " + patient.name, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                        port.updateVisitNote(patient.visitId, area.getText().trim(), auth.employeeId());
                        JOptionPane.showMessageDialog(KhamBenhManHinh.this, "Đã lưu ghi chú lượt khám.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(KhamBenhManHinh.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleReexamHistory(AdminPatientItem patient) {
        var port = resolveQueueActionsPort();
        var auth = KhamBenhPhienNguCanh.current();
        if (patient == null || patient.patientId <= 0 || port == null || auth == null || GraphicsEnvironment.isHeadless()) return;
        new SwingWorker<List<vn.clinicmanagement.application.examination.KhamBenhQueueActionsPort.ReexamHistoryRow>, Void>() {
            @Override protected List<vn.clinicmanagement.application.examination.KhamBenhQueueActionsPort.ReexamHistoryRow> doInBackground() {
                return port.listReexamHistory(patient.patientId, auth.facilityId());
            }
            @Override protected void done() {
                try {
                    var list = get();
                    var tm = new javax.swing.table.DefaultTableModel(new String[]{"Mã LK", "Ngày khám", "Phòng khám", "Chẩn đoán", "Trạng thái"}, 0);
                    var fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
                    for (var r : list) {
                        tm.addRow(new Object[]{r.visitCode(), r.visitedAt() != null ? r.visitedAt().format(fmt) : "", r.roomName(), r.diagnosis(), r.status()});
                    }
                    JTable tbl = new JTable(tm); tbl.setRowHeight(24);
                    JScrollPane sp = new JScrollPane(tbl); sp.setPreferredSize(new Dimension(700, 300));
                    JOptionPane.showMessageDialog(KhamBenhManHinh.this, sp, "Lịch sử khám lại - " + patient.name, JOptionPane.PLAIN_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(KhamBenhManHinh.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handlePrintPatientBandOrCard(AdminPatientItem patient, boolean wristband) {
        var port = resolveQueueActionsPort();
        var auth = KhamBenhPhienNguCanh.current();
        if (patient == null || patient.visitId <= 0 || port == null || auth == null || GraphicsEnvironment.isHeadless()) return;
        new SwingWorker<vn.clinicmanagement.application.examination.KhamBenhQueueActionsPort.PatientCardData, Void>() {
            @Override protected vn.clinicmanagement.application.examination.KhamBenhQueueActionsPort.PatientCardData doInBackground() {
                return port.getPatientCardData(patient.visitId, auth.facilityId());
            }
            @Override protected void done() {
                try {
                    var d = get();
                    if (d == null) return;
                    String title = wristband ? "IN VÒNG TAY NGƯỜI BỆNH" : "IN THẺ KHÁM BỆNH";
                    JTextArea preview = new JTextArea(String.format(
                            "%s%n----------------------------------------%nMã BN: %s   |   Mã LK: %s%nHọ tên: %s (%s)%nNgày sinh: %s%nKhoa/Phòng: %s > %s%nĐối tượng: %s %s%nĐịa chỉ: %s",
                            title, d.patientCode(), d.visitCode(), d.patientName(), d.gender(),
                            d.birthDate() != null ? d.birthDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "",
                            d.departmentName(), d.roomName(), d.subjectType(),
                            d.bhytNumber().isBlank() ? "" : "(" + d.bhytNumber() + ")", d.address()));
                    preview.setEditable(false); preview.setFont(new Font("Monospaced", Font.PLAIN, 13));
                    int res = JOptionPane.showOptionDialog(KhamBenhManHinh.this, new JScrollPane(preview), title,
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[]{"In ngay", "Đóng"}, "In ngay");
                    if (res == 0) preview.print();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(KhamBenhManHinh.this, "Lỗi in: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleChangeVisitSubject(AdminPatientItem patient) {
        var port = resolveQueueActionsPort();
        var auth = KhamBenhPhienNguCanh.current();
        if (patient == null || patient.visitId <= 0 || port == null || auth == null || GraphicsEnvironment.isHeadless()) return;
        String[] options = {"BHYT", "THU_PHI", "YEU_CAU"};
        String picked = (String) JOptionPane.showInputDialog(this, "Chọn đối tượng khám mới cho " + patient.name + ":",
                "Đổi Đối Tượng Khám", JOptionPane.QUESTION_MESSAGE, null, options, "BHYT");
        if (picked == null || picked.isBlank()) return;
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                try { port.changeVisitSubject(patient.visitId, picked, auth.employeeId()); return null; }
                catch (Exception ex) { return ex.getMessage(); }
            }
            @Override protected void done() {
                try {
                    String err = get();
                    if (err != null) JOptionPane.showMessageDialog(KhamBenhManHinh.this, err, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    else {
                        refreshData(null);
                        JOptionPane.showMessageDialog(KhamBenhManHinh.this, "Đã đổi đối tượng khám sang " + picked + ".", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void handleSyncEmr(AdminPatientItem patient) {
        var port = resolveQueueActionsPort();
        var auth = KhamBenhPhienNguCanh.current();
        if (patient == null || patient.visitId <= 0 || port == null || auth == null || GraphicsEnvironment.isHeadless()) return;
        new SwingWorker<vn.clinicmanagement.application.examination.KhamBenhQueueActionsPort.EmrSyncSummary, Void>() {
            @Override protected vn.clinicmanagement.application.examination.KhamBenhQueueActionsPort.EmrSyncSummary doInBackground() {
                return port.syncEmrSummary(patient.visitId, auth.facilityId(), auth.employeeId());
            }
            @Override protected void done() {
                try {
                    var s = get();
                    JOptionPane.showMessageDialog(KhamBenhManHinh.this,
                            String.format("Đồng bộ EMR thành công cho lượt khám #%d:%n- Sinh hiệu: %d%n- Chẩn đoán: %d%n- Chỉ định CLS: %d%n- Đơn thuốc: %d",
                                    s.visitId(), s.vitalsCount(), s.diagnosisCount(), s.serviceOrderCount(), s.prescriptionCount()),
                            "Đồng Bộ EMR", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(KhamBenhManHinh.this, "Lỗi đồng bộ EMR: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleCancelReception(AdminPatientItem patient) {
        if (patient == null || patient.visitId <= 0 || services == null || services.cancelVisit() == null) {
            JOptionPane.showMessageDialog(this, "Chưa chọn lượt khám hợp lệ để hủy tiếp đón.",
                    "Hủy Tiếp Đón", JOptionPane.WARNING_MESSAGE);
            return;
        }
        var auth = KhamBenhPhienNguCanh.current();
        if (auth == null || !auth.isValid()) {
            JOptionPane.showMessageDialog(this, "Phiên làm việc không hợp lệ.",
                    "Hủy Tiếp Đón", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (GraphicsEnvironment.isHeadless()) return;
        String reason = JOptionPane.showInputDialog(this,
                "Nhập lý do hủy tiếp đón cho bệnh nhân " + patient.name + " (" + patient.code + "):",
                "Hủy Tiếp Đón", JOptionPane.QUESTION_MESSAGE);
        if (reason == null || reason.isBlank()) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Xác nhận hủy tiếp đón lượt khám của bệnh nhân " + patient.name + "?",
                "Xác Nhận Hủy Tiếp Đón", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return executeCancelReception(patient.visitId, reason.trim());
            }

            @Override
            protected void done() {
                try {
                    String err = get();
                    if (err != null) {
                        JOptionPane.showMessageDialog(KhamBenhManHinh.this, err,
                                "Không thể hủy tiếp đón", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    refreshData(null);
                    JOptionPane.showMessageDialog(KhamBenhManHinh.this,
                            "Đã hủy tiếp đón thành công cho bệnh nhân: " + patient.name,
                            "Hủy Tiếp Đón", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(KhamBenhManHinh.this,
                            "Không thể hủy tiếp đón: " + ex.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    String executeCancelReception(int visitId, String reason) {
        var auth = KhamBenhPhienNguCanh.current();
        if (services == null || services.cancelVisit() == null || auth == null || !auth.isValid()) {
            return "Phiên làm việc hoặc dịch vụ hủy tiếp đón không hợp lệ.";
        }
        String ws = vn.clinicmanagement.desktop.core.session.NguoiDungPhien.getInstance().getWorkstationId();
        var actor = new vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh(
                auth.employeeId(), auth.roleId(), auth.roleCode(), auth.facilityId(), auth.departmentId(),
                ws != null && !ws.isBlank() ? ws : "DESKTOP", java.util.UUID.randomUUID());
        var res = services.cancelVisit().handle(
                new vn.clinicmanagement.application.examination.cancel.CancelLuotKhamCommand(
                        visitId, reason, auth, actor));
        if (res != null && res.isSuccess()) {
            return null;
        }
        return res == null ? "Không nhận được kết quả hủy tiếp đón."
                : res.getError().map(err -> err.message()).orElse("Không thể hủy tiếp đón.");
    }

    private JButton createToolbarButton(String text, String iconType, boolean hasDropdownArrow) {
        JButton btn = new JButton();
        btn.setLayout(new BorderLayout(0, 1));
        btn.setContentAreaFilled(false);
        btn.setBorder(new EmptyBorder(3, 7, 3, 7));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lblIcon = new JLabel(new SubToolbarVectorIcon(iconType), SwingConstants.CENTER);
        JLabel lblText = new JLabel(hasDropdownArrow ? text + " ▾" : text, SwingConstants.CENTER);
        lblText.setFont(UngDungPhongChu.SMALL);
        lblText.setForeground(new Color(35, 35, 35));

        btn.add(lblIcon, BorderLayout.CENTER);
        btn.add(lblText, BorderLayout.SOUTH);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(228, 236, 248));
                btn.setOpaque(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setOpaque(false);
            }
        });

        if ("Menu".equalsIgnoreCase(text)) {
            KhamBenhMenuBatLenMenu menuPopup = new KhamBenhMenuBatLenMenu(btn, this::handleMenuAction);
            btn.addActionListener(e -> {
                if (btn.isShowing()) {
                    menuPopup.show(btn, 0, btn.getHeight());
                }
            });
        } else if ("Làm mới".equalsIgnoreCase(text)) {
            btn.addActionListener(e -> refreshData());
        } else if ("Tìm Kiếm".equalsIgnoreCase(text)) {
            btn.addActionListener(e -> searchPatientInQueue());
        } else if ("In".equalsIgnoreCase(text)) {
            PrintBaoCaoBatLenMenu printMenu = new PrintBaoCaoBatLenMenu(btn, this::handlePrintAction);
            btn.addActionListener(e -> printMenu.show(btn, 0, btn.getHeight()));
        } else if ("Thuốc/VT".equalsIgnoreCase(text)) {
            ThuocVatTuBatLenMenu medMenu = new ThuocVatTuBatLenMenu(btn, this::handleMedicineAction);
            btn.addActionListener(e -> medMenu.show(btn, 0, btn.getHeight()));
        } else if ("Thanh Toán".equalsIgnoreCase(text)) {
            btn.setName("examination.payment");
            btn.addActionListener(e -> vn.clinicmanagement.desktop.modules.billing.examination.ThanhToanKhamBenhHopThoai.showForVisit(
                    this, queueSidebar != null ? queueSidebar.getSelectedPatient() : null));
        } else if ("Dịch Vụ".equalsIgnoreCase(text)) {
            btn.addActionListener(e -> cardLayout.show(cardContainer, "SERVICE_ORDER"));
        }
        return btn;
    }

    private void searchPatientInQueue() {
        if (queueSidebar == null) return;
        String keyword = JOptionPane.showInputDialog(this, "Nhập tên hoặc mã bệnh nhân:");
        if (keyword == null || keyword.isBlank()) return;
        String query = keyword.trim().toLowerCase(Locale.ROOT);
        List<AdminPatientItem> items = queueSidebar.getPatientList();
        for (int i = 0; i < items.size(); i++) {
            AdminPatientItem item = items.get(i);
            if (item.name.toLowerCase(Locale.ROOT).contains(query)
                    || item.code.toLowerCase(Locale.ROOT).contains(query)) {
                selectPatientByIndex(i);
                return;
            }
        }
        JOptionPane.showMessageDialog(this, "Không tìm thấy bệnh nhân trong danh sách hiện tại.");
    }

    private void handlePrintAction(String reportTitle) {
        if (reportTitle != null && reportTitle.contains("Đăng Ký Khám Bệnh, Chữa Bệnh")) {
            AdminPatientItem curP = currentOrFirstPatient();
            int vid = curP != null ? curP.visitId : 0;
            Window owner = SwingUtilities.getWindowAncestor(this);
            if (vid > 0) {
                new SwingWorker<vn.clinicmanagement.desktop.modules.examination.components.print.YTeVienPhiPhieuManHinh.BillingSlipData, Void>() {
                    @Override protected vn.clinicmanagement.desktop.modules.examination.components.print.YTeVienPhiPhieuManHinh.BillingSlipData doInBackground() {
                        return vn.clinicmanagement.desktop.modules.examination.components.print.YTeVienPhiPhieuManHinh.loadFromDatabase(vid);
                    }
                    @Override protected void done() {
                        try {
                            new YTeVienPhiPhieuHopThoai(owner, get()).setVisible(true);
                        } catch (Exception ex) {
                            new YTeVienPhiPhieuHopThoai(owner).setVisible(true);
                        }
                    }
                }.execute();
            } else {
                new YTeVienPhiPhieuHopThoai(owner).setVisible(true);
            }
        } else if (reportTitle != null && reportTitle.contains("Bệnh Án Khám Bệnh")) {
            AdminPatientItem curP = currentOrFirstPatient();
            int vid = curP != null ? curP.visitId : 0;
            Window owner = SwingUtilities.getWindowAncestor(this);
            if (vid > 0) {
                new SwingWorker<vn.clinicmanagement.desktop.modules.examination.components.print.YTeRecordPrintPreviewManHinh.MedicalRecordData, Void>() {
                    @Override protected vn.clinicmanagement.desktop.modules.examination.components.print.YTeRecordPrintPreviewManHinh.MedicalRecordData doInBackground() {
                        return vn.clinicmanagement.desktop.modules.examination.components.print.YTeRecordPrintPreviewManHinh.loadFromDatabase(vid);
                    }
                    @Override protected void done() {
                        try {
                            new YTeRecordPrintPreviewHopThoai(owner, get()).setVisible(true);
                        } catch (Exception ex) {
                            new YTeRecordPrintPreviewHopThoai(owner).setVisible(true);
                        }
                    }
                }.execute();
            } else {
                new YTeRecordPrintPreviewHopThoai(owner).setVisible(true);
            }
        } else if (reportTitle != null && reportTitle.contains("Phiếu Chỉ Định")) {
            if (labOrderTabPanel != null) {
                labOrderTabPanel.openClinicalOrderPrintDialog();
            } else {
                new LamSangYeuCauPrintPreviewHopThoai(SwingUtilities.getWindowAncestor(this)).setVisible(true);
            }
        } else if (!GraphicsEnvironment.isHeadless()) {
            AdminPatientItem curP = currentOrFirstPatient();
            String pInfo = curP != null ? curP.name + " | " + curP.code + " (" + curP.requestType + ")" : "Bệnh nhân hiện tại";
            String pDoc = curP != null && curP.doctor != null ? curP.doctor : "Chưa phân công";
            String pDept = curP != null && curP.department != null ? curP.department : "Khoa Khám Bệnh";
            JOptionPane.showMessageDialog(this,
                    "Hệ thống đang chuẩn bị xuất bản in:\n"
                            + "▶ " + reportTitle + "\n\n"
                            + "Bệnh nhân: " + pInfo + "\n"
                            + "Bác sĩ chỉ định: " + pDoc + "\n"
                            + "Khoa phòng: " + pDept,
                    "Xuất & In Phiếu Khám Bệnh EHC", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort tpcnReportPort;

    public void setTpcnReportPort(vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort tpcnReportPort) {
        this.tpcnReportPort = tpcnReportPort;
    }

    public vn.clinicmanagement.desktop.modules.examination.components.dialogs.ChiDinhThucPhamChucNangHopThoai createFunctionalFoodPanel() {
        AdminPatientItem curP = currentOrFirstPatient();
        int visitId = curP != null ? curP.visitId : 0;
        var auth = KhamBenhPhienNguCanh.current();
        int docId = auth != null ? auth.employeeId() : 0;
        String pName = curP != null ? curP.name : "";
        String pCode = curP != null ? curP.code : "";
        var port = tpcnReportPort != null ? tpcnReportPort
                : (GraphicsEnvironment.isHeadless() || System.getProperty("surefire.test.class.path") != null
                ? null
                : new vn.clinicmanagement.infrastructure.persistence.inventory.NhaThuocBaoCaoRepository(
                        new vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap(
                                new vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy(
                                        new vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork()))));
        return new vn.clinicmanagement.desktop.modules.examination.components.dialogs.ChiDinhThucPhamChucNangHopThoai(
                services, port, visitId, docId, pName, pCode);
    }

    private void handleMedicineAction(String actionTitle) {
        if (actionTitle != null && actionTitle.contains("Chỉ Định Vật Tư")) {
            cardLayout.show(cardContainer, "SUPPLIES_ORDER");
        } else if (actionTitle != null && actionTitle.contains("Chỉ Định Thuốc")) {
            cardLayout.show(cardContainer, "PRESCRIPTION_ORDER");
        } else if (actionTitle != null && actionTitle.contains("Thực Phẩm Chức Năng")) {
            if (GraphicsEnvironment.isHeadless()) return;
            var panel = createFunctionalFoodPanel();
            JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                    "EHC - Chỉ Định Thực Phẩm Chức Năng", Dialog.ModalityType.MODELESS);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setContentPane(panel);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        } else {
            AdminPatientItem curP = currentOrFirstPatient();
            String pInfo = curP != null ? curP.name + " | " + curP.code + " (" + curP.requestType + ")" : "Bệnh nhân hiện tại";
            String pDoc = curP != null && curP.doctor != null ? curP.doctor : "Chưa phân công";
            String pDept = curP != null && curP.department != null ? curP.department : "Khoa Khám Bệnh";
            JOptionPane.showMessageDialog(this,
                    "Hệ thống đang mở phân hệ:\n"
                            + "▶ " + actionTitle + "\n\n"
                            + "Bệnh nhân: " + pInfo + "\n"
                            + "Bác sĩ chỉ định: " + pDoc + "\n"
                            + "Khoa phòng: " + pDept,
                    "Quản Lý Thuốc & Vật Tư Y Tế", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private AdminPatientItem currentOrFirstPatient() {
        if (queueSidebar == null) return null;
        AdminPatientItem selected = queueSidebar.getSelectedPatient();
        if (selected != null) return selected;
        List<AdminPatientItem> items = queueSidebar.getPatientList();
        return items.isEmpty() ? null : items.get(0);
    }

    private void handleMenuAction(String actionTitle) {
        if ("Đóng".equalsIgnoreCase(actionTitle)) return;
        if ("Quản Lý Buồng / Giường Nội Trú".equalsIgnoreCase(actionTitle)) {
            openInpatientBedDialog();
            return;
        }
        if ("Chuyển Loại Bệnh Án".equalsIgnoreCase(actionTitle)) {
            Window win = SwingUtilities.getWindowAncestor(this);
            new YTeRecordLoaiHopThoai(win, selectedType -> {
                if (patientTabPanel != null) {
                    patientTabPanel.setRecordType(selectedType);
                }
            }).setVisible(true);
            return;
        }
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(this,
                    "Chức năng: " + actionTitle + "\nĐang được kích hoạt trong phân hệ Khám Bệnh.",
                    actionTitle, JOptionPane.INFORMATION_MESSAGE);
        }
    }

    @Override
    public void onShow() {
        refreshData();
    }

    @Override
    public void refreshData() {
        refreshData(null);
    }

    public void refreshData(Component tabToSelectAfterReload) {
        if (queueSidebar == null) return;
        AdminPatientItem current = queueSidebar.getSelectedPatient();
        String preferredInvoiceCode = current != null ? current.invoiceCode : null;
        int departmentId = selectedDepartmentId;
        int roomId = selectedRoomId;
        long sequence = queueRefreshSequence.incrementAndGet();
        new SwingWorker<List<AdminPatientItem>, Void>() {
            @Override
            protected List<AdminPatientItem> doInBackground() {
                return fetchQueueItemsSafely(departmentId, roomId);
            }

            @Override
            protected void done() {
                if (sequence != queueRefreshSequence.get()) return;
                try {
                    applyQueueItems(get(), preferredInvoiceCode);
                    if (tabToSelectAfterReload != null && clinicalTabs != null) {
                        clinicalTabs.setSelectedComponent(tabToSelectAfterReload);
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void openDepartmentFilter() {
        var context = KhamBenhPhienNguCanh.current();
        if (context == null || services == null || services.examQueue() == null) return;
        long sequence = filterMenuSequence.incrementAndGet();
        new SwingWorker<List<Khoa>, Void>() {
            @Override protected List<Khoa> doInBackground() {
                return services.examQueue().getAuthorizedDepartments(context.facilityId(), context.employeeId());
            }

            @Override protected void done() {
                if (sequence != filterMenuSequence.get() || queueSidebar == null || !queueSidebar.isShowing()) return;
                try {
                    List<Khoa> departments = get();
                    boolean doctor = "DOCTOR".equalsIgnoreCase(context.roleCode());
                    if (doctor) {
                        departments = departments.stream()
                                .filter(department -> department.getId() != null
                                        && department.getId() == context.departmentId())
                                .toList();
                    }
                    authorizedDepartmentNames.clear();
                    for (Khoa department : departments) {
                        if (department.getId() != null) authorizedDepartmentNames.put(department.getId(), department.getTenKhoa());
                    }
                    selectedDepartmentName = authorizedDepartmentNames.getOrDefault(selectedDepartmentId, selectedDepartmentName);
                    queueSidebar.showDepartmentMenu(departments, selectedDepartmentId, !doctor,
                            KhamBenhManHinh.this::selectDepartmentFilter);
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void selectDepartmentFilter(int departmentId) {
        var context = KhamBenhPhienNguCanh.current();
        if (context == null || ("DOCTOR".equalsIgnoreCase(context.roleCode())
                && departmentId != context.departmentId())) return;
        selectedDepartmentId = departmentId;
        selectedRoomId = 0;
        selectedRoomName = null;
        selectedDepartmentName = departmentId == 0 ? null : authorizedDepartmentNames.get(departmentId);
        updateClinicSelectionLabel();
        refreshData();
        if (departmentId > 0) openRoomFilter();
    }

    private void openRoomFilter() {
        if (selectedDepartmentId <= 0) {
            openDepartmentFilter();
            return;
        }
        var context = KhamBenhPhienNguCanh.current();
        if (context == null || services == null || services.examQueue() == null) return;
        int departmentId = selectedDepartmentId;
        long sequence = filterMenuSequence.incrementAndGet();
        new SwingWorker<List<PhongKham>, Void>() {
            @Override protected List<PhongKham> doInBackground() {
                return services.examQueue().getActiveClinicRooms(context.facilityId(), departmentId);
            }

            @Override protected void done() {
                if (sequence != filterMenuSequence.get() || queueSidebar == null || !queueSidebar.isShowing()
                        || departmentId != selectedDepartmentId) return;
                try {
                    List<PhongKham> rooms = get();
                    availableRoomNames.clear();
                    for (PhongKham room : rooms) {
                        if (room.getId() != null) availableRoomNames.put(room.getId(), room.getTenPhong());
                    }
                    selectedRoomName = availableRoomNames.getOrDefault(selectedRoomId, selectedRoomName);
                    queueSidebar.showRoomMenu(rooms, selectedRoomId, KhamBenhManHinh.this::selectRoomFilter);
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void selectRoomFilter(int roomId) {
        selectedRoomId = roomId;
        selectedRoomName = roomId == 0 ? null : availableRoomNames.get(roomId);
        updateClinicSelectionLabel();
        refreshData();
    }

    private void updateClinicSelectionLabel() {
        if (queueSidebar == null) return;
        if (selectedDepartmentId == 0) {
            queueSidebar.setClinicSelectionLabel("(Tất cả phòng khám)");
        } else if (selectedRoomId == 0) {
            queueSidebar.setClinicSelectionLabel("(" + (selectedDepartmentName == null ? "Khoa đã chọn" : selectedDepartmentName)
                    + " · Tất cả phòng)");
        } else {
            queueSidebar.setClinicSelectionLabel("(" + (selectedDepartmentName == null ? "Khoa đã chọn" : selectedDepartmentName)
                    + " · " + (selectedRoomName == null ? "Phòng đã chọn" : selectedRoomName) + ")");
        }
    }

    private static JPanel createPlaceholderTab(String desc) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        JLabel lbl = new JLabel(desc, SwingConstants.CENTER);
        lbl.setFont(UngDungPhongChu.BODY);
        lbl.setForeground(Color.GRAY);
        p.add(lbl, BorderLayout.CENTER);
        return p;
    }

    public XetNghiemYeuCauTheManHinh getLabOrderTabPanel() { return labOrderTabPanel; }
    public VatTuYeuCauTheManHinh getSuppliesTabPanel() { return suppliesTabPanel; }
    public DonThuocYeuCauTheManHinh getPrescriptionTabPanel() { return prescriptionTabPanel; }
    public DieuTriXuTriTheManHinh getTreatmentResolutionTabPanel() { return treatmentResolutionTabPanel; }
    public YTeRecordTheManHinh getMedicalRecordTabPanel() { return medicalRecordTabPanel; }
    private void openTreatmentSheetDialog() {
        if (GraphicsEnvironment.isHeadless()) return;
        AdminPatientItem cur = queueSidebar != null ? queueSidebar.getSelectedPatient() : null;
        if (cur == null || cur.visitId <= 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn lượt khám để xem/in Tờ Điều Trị.",
                    "Tờ Điều Trị", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (services == null || services.examQueue() == null) {
            JOptionPane.showMessageDialog(this, "Chưa kết nối dữ liệu tờ điều trị.", "Tờ Điều Trị", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        var actor = KhamBenhPhienNguCanh.current();
        vn.clinicmanagement.desktop.modules.examination.components.print.ToDieuTriPrintHopThoai.showDocument(
                owner, () -> vn.clinicmanagement.desktop.modules.examination.components.print.ToDieuTriDuLieuNap.load(services, cur, actor));
    }

    public void selectMedicalRecordTab() { clinicalTabs.setSelectedComponent(medicalRecordTabPanel); }
    public void selectTreatmentTab() { clinicalTabs.setSelectedComponent(treatmentResolutionTabPanel); }
    public DichVuYeuCauManHinh getServiceOrderPanel() { return serviceOrderPanel; }
    public JTabbedPane getClinicalTabs() { return clinicalTabs; }

    DefaultTableModel getHistoryModelForTest() { return patientTabPanel != null ? patientTabPanel.getHistoryModel() : null; }
    String getTabPatientsTextForTest() { return queueSidebar != null ? queueSidebar.getTabPatientsText() : ""; }
    String getFilterSummaryTextForTest() { return queueSidebar != null ? queueSidebar.getFilterSummaryText() : ""; }
    void selectPatientForTest(AdminPatientItem item) { selectPatient(item); }
}
