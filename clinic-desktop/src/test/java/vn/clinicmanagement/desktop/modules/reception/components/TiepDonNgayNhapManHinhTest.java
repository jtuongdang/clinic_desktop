package vn.clinicmanagement.desktop.modules.reception.components;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import static org.junit.jupiter.api.Assertions.*;

class TiepDonNgayNhapManHinhTest {
    @Test void dongBoNhapNapVaXoaNgay() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTextField model = new JTextField();
            TiepDonNgayNhapManHinh view = new TiepDonNgayNhapManHinh(model);
            JTextField day = (JTextField) view.getComponent(0);
            JTextField month = (JTextField) view.getComponent(1);
            JTextField year = (JTextField) view.getComponent(2);
            day.setText("16"); month.setText("11"); year.setText("2026");
            assertEquals("16/11/2026", model.getText());
            model.setText("01/02/2027");
            assertEquals("01", day.getText());
            assertEquals("02", month.getText());
            assertEquals("2027", year.getText());
            model.setText("");
            assertEquals("", day.getText());
            assertEquals("", month.getText());
            assertEquals("", year.getText());
            day.setText("31");
            assertEquals("31//", model.getText(), "Ngày thiếu thành phần phải được giữ để nghiệp vụ báo lỗi");
        });
    }
}
