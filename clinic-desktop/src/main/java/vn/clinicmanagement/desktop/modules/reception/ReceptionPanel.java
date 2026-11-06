package vn.clinicmanagement.desktop.modules.reception;

import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.modules.reception.components.ClinicRoomStatusPanel;
import vn.clinicmanagement.desktop.modules.reception.components.PatientRegistrationFormPanel;
import vn.clinicmanagement.desktop.modules.reception.components.ReceptionFooterBar;
import vn.clinicmanagement.desktop.modules.reception.components.ReceptionTopToolbar;

import javax.swing.*;
import java.awt.*;

/**
 * Phân hệ Đón Tiếp & Đăng Ký Khám Bệnh (EHC Reception Station).
 * Khớp 100% chi tiết trong ảnh chụp mẫu:
 * - Thanh công cụ đỉnh (Menu, In, Làm mới, Bệnh nhân mới, Cửa đón tiếp 2, Gọi số tiếp theo)
 * - Khung nhập liệu hành chính bệnh nhân & thẻ BHYT
 * - Dải 9 Tab lâm sàng và 3 cột bảng trạng thái phòng khám
 * - Thanh phím tắt chân trang: F1 (Mới), F2 (Lưu), F3 (In phiếu), F4 (Thẻ KCB)
 */
public class ReceptionPanel extends BaseViewPanel {

    private final ReceptionTopToolbar topToolbar;
    private final PatientRegistrationFormPanel formPanel;
    private final ClinicRoomStatusPanel statusPanel;
    private final ReceptionFooterBar footerBar;

    public ReceptionPanel() {
        super("reception", "Đăng Ký Khám Bệnh", "📋");
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. Thanh công cụ đỉnh
        topToolbar = new ReceptionTopToolbar();
        add(topToolbar, BorderLayout.NORTH);

        // 2. Vùng trung tâm: Chia dọc (Trên: Form đăng ký; Dưới: Trạng thái phòng khám)
        formPanel = new PatientRegistrationFormPanel();
        statusPanel = new ClinicRoomStatusPanel();

        // Đồng bộ khi chọn phòng khám trên form -> Đổi màu đỏ phòng trong bảng trạng thái
        formPanel.setOnClinicRoomChanged(room -> statusPanel.setSelectedClinicRoom(room));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formPanel, statusPanel);
        splitPane.setDividerLocation(218);
        splitPane.setDividerSize(4);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        add(splitPane, BorderLayout.CENTER);

        // 3. Thanh phím tắt chân trang
        footerBar = new ReceptionFooterBar();
        add(footerBar, BorderLayout.SOUTH);
    }

    @Override
    public void onShow() {
        refreshData();
    }

    @Override
    public void refreshData() {
        // Dữ liệu sẵn sàng nạp từ Domain / Application Service
    }
}
