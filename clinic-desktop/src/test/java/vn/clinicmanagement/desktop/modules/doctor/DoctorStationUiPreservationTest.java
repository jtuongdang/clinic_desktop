package vn.clinicmanagement.desktop.modules.doctor;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.modules.doctor.components.DoctorActionBar;
import vn.clinicmanagement.desktop.modules.doctor.components.DoctorHistoryPanel;

import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoctorStationUiPreservationTest {
    private static final List<String> HISTORY_COLUMNS = List.of(
            "Ngày Vào", "Hình Thức Vào", "Khoa", "Phòng",
            "Ngày Ra", "Xử Trí", "Dịch", "Chẩn Đoán");
    private static final List<String> ACTION_BUTTONS = List.of(
            "Menu", "In", "Làm mới", "Tìm Kiếm", "Bệnh Án",
            "Dịch Vụ", "Thuốc/VT", "Phác Đồ ĐT", "Thanh Toán");
    private static final List<String> TABS = List.of(
            "Bệnh Nhân", "Công Khám(1)", "Thuốc(2)", "Xử Trí", "Phản Hồi");

    @Test
    void historyTableKeepsOriginalReadOnlyEightColumnSchema() {
        DoctorHistoryPanel historyPanel = new DoctorHistoryPanel();
        List<JTable> tables = descendantsOfType(historyPanel, JTable.class);

        assertEquals(1, tables.size());
        TableModel model = tables.get(0).getModel();
        assertEquals(HISTORY_COLUMNS.size(), model.getColumnCount());
        for (int column = 0; column < HISTORY_COLUMNS.size(); column++) {
            assertEquals(HISTORY_COLUMNS.get(column), model.getColumnName(column));
            assertFalse(model.isCellEditable(0, column));
        }
    }

    @Test
    void actionBarKeepsAllOriginalButtonsInOrder() {
        DoctorActionBar actionBar = new DoctorActionBar();
        List<JButton> buttons = descendantsOfType(actionBar, JButton.class);

        assertEquals(ACTION_BUTTONS.size(), buttons.size());
        for (int index = 0; index < ACTION_BUTTONS.size(); index++) {
            assertTrue(buttons.get(index).getText().contains(">" + ACTION_BUTTONS.get(index) + "</span>"));
        }
    }

    @Test
    void stationKeepsOriginalHorizontalSplitGeometryAndFiveTabs() {
        DoctorStationPanel stationPanel = new DoctorStationPanel();
        List<JSplitPane> splitPanes = descendantsOfType(stationPanel, JSplitPane.class);
        List<JTabbedPane> tabbedPanes = descendantsOfType(stationPanel, JTabbedPane.class);

        assertEquals(1, splitPanes.size());
        JSplitPane splitPane = splitPanes.get(0);
        assertEquals(JSplitPane.HORIZONTAL_SPLIT, splitPane.getOrientation());
        assertEquals(250, splitPane.getDividerLocation());
        assertEquals(4, splitPane.getDividerSize());

        assertEquals(1, tabbedPanes.size());
        JTabbedPane tabs = tabbedPanes.get(0);
        assertEquals(TABS.size(), tabs.getTabCount());
        for (int index = 0; index < TABS.size(); index++) {
            assertEquals(TABS.get(index), tabs.getTitleAt(index));
        }
    }

    private static <T extends Component> List<T> descendantsOfType(Container root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                matches.add(type.cast(component));
            }
            if (component instanceof Container) {
                matches.addAll(descendantsOfType((Container) component, type));
            }
        }
        return matches;
    }
}
