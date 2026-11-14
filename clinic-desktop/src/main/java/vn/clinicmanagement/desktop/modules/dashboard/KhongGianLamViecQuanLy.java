package vn.clinicmanagement.desktop.modules.dashboard;

import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.core.components.DuoiThe;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Quản lý không gian làm việc đa tab (MDI Workspace Manager)
 * Thiết kế cho hệ thống Enterprise hàng trăm Form:
 * - Lazy load và tái sử dụng (reuse) hoặc giải phóng bộ nhớ khi đóng tab
 * - Quản lý vòng đời BaseViewPanel (onShow, onHide, canClose)
 * - Tự động đồng bộ giữa CardLayout hiển thị, thanh Bottom Tab và Ribbon Menu
 */
public class KhongGianLamViecQuanLy {

    private final JPanel centerContainer;
    private final CardLayout cardLayout;
    /** Trường `tabStripPanel`: the `Strip` màn hình. */
    private final JPanel tabStripPanel;

    /** Trường `openViews`: mở `Views`. */
    private final Map<String, CoSoManHinhManHinh> openViews = new LinkedHashMap<>();
    /** Trường `openTabs`: mở `Tabs`. */
    private final Map<String, DuoiThe> openTabs = new LinkedHashMap<>();
    /** Trường `currentViewId`: hientai màn hình mã. */
    private String currentViewId = null;
    /** Trường `onTabActivatedListener`: tải the `Activated` bolangnghe. */
    private Consumer<String> onTabActivatedListener;

    public KhongGianLamViecQuanLy(JPanel centerContainer, CardLayout cardLayout, JPanel tabStripPanel) {
        this.centerContainer = centerContainer;
        this.cardLayout = cardLayout;
        this.tabStripPanel = tabStripPanel;
    }

    /** Hàm `setOnTabActivatedListener`: gán tải the `Activated` bolangnghe. */
    public void setOnTabActivatedListener(Consumer<String> listener) {
        this.onTabActivatedListener = listener;
    }

    /**
     * Mở một form/view nghiệp vụ bất kỳ
     */
    public void openView(CoSoManHinhManHinh view) {
        if (view == null) return;
        String id = view.getViewKey();

        if (openViews.containsKey(id)) {
            // Đã mở -> Chỉ cần activate tab đó
            activateView(id);
            return;
        }

        // Đăng ký panel vào CardLayout
        openViews.put(id, view);
        centerContainer.add(view, id);

        // Tạo Bottom Tab tương ứng
        DuoiThe tab = new DuoiThe(
                view.getViewTitle(),
                id,
                true,
                this::activateView,
                this::closeView
        );
        openTabs.put(id, tab);
        tabStripPanel.add(tab);

        tabStripPanel.revalidate();
        tabStripPanel.repaint();

        // Kích hoạt hiển thị
        activateView(id);
    }

    /**
     * Kích hoạt chuyển đổi tab
     */
    public void activateView(String viewId) {
        if (!openViews.containsKey(viewId)) return;

        // Báo onHide cho view trước đó nếu có
        if (currentViewId != null && !currentViewId.equals(viewId)) {
            CoSoManHinhManHinh prev = openViews.get(currentViewId);
            if (prev != null) prev.onHide();
        }

        currentViewId = viewId;
        cardLayout.show(centerContainer, viewId);

        // Cập nhật trạng thái active/inactive cho các tab
        for (Map.Entry<String, DuoiThe> entry : openTabs.entrySet()) {
            entry.getValue().setSelected(entry.getKey().equals(viewId));
        }

        // Báo onShow cho view được active
        CoSoManHinhManHinh active = openViews.get(viewId);
        if (active != null) {
            active.onShow();
        }

        // Báo listener để đồng bộ Ribbon Menu
        if (onTabActivatedListener != null) {
            onTabActivatedListener.accept(viewId);
        }
    }

    /**
     * Đóng tab nghiệp vụ
     */
    public void closeView(String viewId) {
        if ("home".equalsIgnoreCase(viewId)) {
            return; // Trang chủ mặc định không đóng
        }

        CoSoManHinhManHinh view = openViews.get(viewId);
        if (view == null) return;

        if (!view.canClose()) {
            return; // Chưa lưu dữ liệu hoặc view từ chối đóng
        }

        view.onHide();

        // Gỡ khỏi giao diện
        centerContainer.remove(view);
        DuoiThe tab = openTabs.remove(viewId);
        if (tab != null) {
            tabStripPanel.remove(tab);
        }
        openViews.remove(viewId);

        tabStripPanel.revalidate();
        tabStripPanel.repaint();

        // Nếu vừa đóng tab đang active, active tab gần nhất hoặc Home
        if (viewId.equals(currentViewId)) {
            String nextId = openViews.keySet().stream().reduce((first, second) -> second).orElse("home");
            activateView(nextId);
        }
    }

    /** Hàm `getActiveView`: lấy hoạt động màn hình. */
    public CoSoManHinhManHinh getActiveView() {
        return currentViewId != null ? openViews.get(currentViewId) : null;
    }

    /** Hàm `isViewOpen`: là màn hình mở. */
    public boolean isViewOpen(String viewId) {
        return openViews.containsKey(viewId);
    }
}
