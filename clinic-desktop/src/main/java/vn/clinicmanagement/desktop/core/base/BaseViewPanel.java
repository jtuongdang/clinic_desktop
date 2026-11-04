package vn.clinicmanagement.desktop.core.base;

import javax.swing.*;
import java.awt.*;

/**
 * Lớp trừu tượng cơ sở cho toàn bộ các Form / Panel trong hệ thống.
 * Chuẩn hóa vòng đời (Lifecycle) khi mở, đóng, ẩn, hiện và tải dữ liệu.
 */
public abstract class BaseViewPanel extends JPanel {
    private final String viewKey;
    private final String viewTitle;
    private final String iconEmoji;

    protected BaseViewPanel(String viewKey, String viewTitle, String iconEmoji) {
        this.viewKey = viewKey;
        this.viewTitle = viewTitle;
        this.iconEmoji = iconEmoji;
        setLayout(new BorderLayout());
    }

    public String getViewKey() { return viewKey; }
    public String getViewTitle() { return viewTitle; }
    public String getIconEmoji() { return iconEmoji; }

    /**
     * Được gọi khi form bắt đầu hiển thị lên màn hình.
     */
    public void onShow() {
        refreshData();
    }

    /**
     * Được gọi khi form bị ẩn đi hoặc người dùng chuyển tab khác.
     */
    public void onHide() {}

    /**
     * Tải mới dữ liệu nghiệp vụ của form.
     */
    public abstract void refreshData();

    /**
     * Kiểm tra có thể đóng tab an toàn không (ví dụ: cảnh báo chưa lưu dữ liệu).
     */
    public boolean canClose() {
        return true;
    }
}
