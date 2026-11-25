package vn.clinicmanagement.desktop.modules.system.services;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.text.JTextComponent;

/** Các tab dùng chung trường dữ liệu với cửa sổ Cập nhật dịch vụ. */
final class DichVuCauHinhTabForm extends JPanel {
    private final JPanel content = new JPanel();
    private final List<JTextComponent> textInputs = new ArrayList<>();
    private final List<JCheckBox> options = new ArrayList<>();

    private DichVuCauHinhTabForm(String name) {
        super(new BorderLayout());
        setName(name);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        add(new JScrollPane(content), BorderLayout.CENTER);
    }

    static DichVuCauHinhTabForm prices(Map<String, JComponent> fields) {
        var tab = new DichVuCauHinhTabForm("serviceRequestPriceConfiguration");
        var prices = tab.section("Đơn giá (đồng)", 4);
        tab.addNumber(prices, fields, "insurancePrice", "Giá BHYT");
        tab.addNumber(prices, fields, "hospitalPrice", "Giá viện phí");
        tab.addNumber(prices, fields, "servicePrice", "Giá dịch vụ");
        tab.addNumber(prices, fields, "foreignPrice", "Giá nước ngoài");

        var coverage = tab.section("Thanh toán và định mức", 3);
        tab.addNumber(coverage, fields, "inNetworkPercent", "Tỷ lệ thanh toán đúng tuyến (%)");
        tab.addNumber(coverage, fields, "outNetworkPercent", "Tỷ lệ thanh toán trái tuyến (%)");
        tab.addNumber(coverage, fields, "insuranceLimit", "Định mức BHYT trả");
        tab.addNumber(coverage, fields, "materialLimit", "Định mức vật tư");
        tab.addNumber(coverage, fields, "drugLimit", "Định mức thuốc");

        var policy = tab.section("Thông tin áp giá", 3);
        tab.addText(policy, fields, "effectiveDate", "Ngày áp giá");
        tab.addText(policy, fields, "priceBasis", "Hình thức áp giá");
        tab.addText(policy, fields, "insuranceCostGroup", "Nhóm chi phí BHYT");
        tab.addText(policy, fields, "accountGroup", "Nhóm tài khoản thu tiền");
        tab.addText(policy, fields, "insuredPatientTypes", "Đối tượng được BHYT thanh toán");
        tab.addText(policy, fields, "paymentExemptServices", "Dịch vụ chỉ định được miễn thanh toán");

        var permissions = tab.section("Tùy chọn thu phí", 2);
        tab.addOption(permissions, fields, "editPriceAtOrder", "Cho sửa giá khi chỉ định");
        tab.addOption(permissions, fields, "editPriceAtExecution", "Cho sửa giá khi thực hiện");
        tab.addOption(permissions, fields, "allowDiscount", "Cho phép miễn giảm");
        tab.addOption(permissions, fields, "multiplePayments", "Thu tiền nhiều lần");
        return tab;
    }

    static DichVuCauHinhTabForm otherInformation(Map<String, JComponent> fields) {
        var tab = new DichVuCauHinhTabForm("serviceOtherInformation");
        var catalog = tab.section("Phân loại và báo cáo", 3);
        tab.addText(catalog, fields, "ministryType", "Loại dịch vụ (Bộ Y tế)");
        tab.addText(catalog, fields, "reportGroup", "Nhóm báo cáo");
        tab.addText(catalog, fields, "icd9", "Mã ICD-9");
        tab.addText(catalog, fields, "specialtyCode", "Mã khám chuyên khoa");
        tab.addText(catalog, fields, "imagingName", "Tên chẩn đoán hình ảnh");
        tab.addText(catalog, fields, "labAbbreviation", "Viết tắt xét nghiệm");

        var execution = tab.section("Chỉ định và thực hiện", 3);
        tab.addText(execution, fields, "orderRooms", "Phòng chỉ định");
        tab.addText(execution, fields, "executionRooms", "Phòng thực hiện");
        tab.addText(execution, fields, "receptionRooms", "Phòng đón tiếp hoặc lấy mẫu");
        tab.addText(execution, fields, "executionArea", "Khu thực hiện");
        tab.addText(execution, fields, "orderTemplate", "Mẫu phiếu chỉ định");
        tab.addText(execution, fields, "serviceStages", "Công đoạn");

        var options = tab.section("Tùy chọn thực hiện", 2);
        tab.addOption(options, fields, "editExecutionQuantity", "Cho sửa số lượng khi thực hiện");
        tab.addOption(options, fields, "consultation", "Cần hội chẩn");
        tab.addOption(options, fields, "consultationRecord", "Yêu cầu biên bản hội chẩn");
        tab.addOption(options, fields, "blockCancellation", "Chặn hủy sau khi chỉ định");
        tab.addOption(options, fields, "allowReplacement", "Cho thay thế dịch vụ");
        tab.addOption(options, fields, "locked", "Khóa dịch vụ");

        var notes = tab.section("Hướng dẫn và ghi chú", 3);
        tab.addNotes(notes, fields, "note", "Ghi chú dịch vụ", true);
        tab.addNotes(notes, fields, "preServiceInstructions", "Hướng dẫn chuẩn bị", false);
        tab.addNotes(notes, fields, "postServiceInstructions", "Hướng dẫn sau thực hiện", false);
        return tab;
    }

    void setEditable(boolean editable) {
        textInputs.forEach(input -> input.setEditable(editable));
        options.forEach(option -> option.setEnabled(editable));
    }

    private JPanel section(String title, int columns) {
        if (content.getComponentCount() > 0) content.add(Box.createVerticalStrut(10));
        var panel = new JPanel(new GridLayout(0, columns, 12, 8));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        content.add(panel);
        return panel;
    }

    private void addNumber(JPanel section, Map<String, JComponent> fields, String key, String label) {
        addText(section, fields, key, label).setHorizontalAlignment(SwingConstants.RIGHT);
    }

    private JTextField addText(JPanel section, Map<String, JComponent> fields, String key, String label) {
        var input = new JTextField(14);
        input.setDocument(textSource(fields.get(key)).getDocument());
        addLabeled(section, input, input, key, label);
        return input;
    }

    private void addNotes(JPanel section, Map<String, JComponent> fields, String key, String label, boolean shared) {
        var input = new JTextArea(4, 18);
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        if (shared) input.setDocument(textSource(fields.get(key)).getDocument());
        else fields.put(key, input);
        addLabeled(section, input, new JScrollPane(input), key, label);
    }

    private void addLabeled(JPanel section, JTextComponent input, JComponent editor, String key, String label) {
        input.setName(getName() + "." + key);
        var caption = new JLabel(label);
        caption.setLabelFor(input);
        var field = new JPanel(new BorderLayout(0, 3));
        field.add(caption, BorderLayout.NORTH);
        field.add(editor, BorderLayout.CENTER);
        textInputs.add(input);
        section.add(field);
    }

    private void addOption(JPanel section, Map<String, JComponent> fields, String key, String label) {
        var input = new JCheckBox(label);
        input.setName(getName() + "." + key);
        input.setModel(((JCheckBox) fields.get(key)).getModel());
        options.add(input);
        section.add(input);
    }

    private static JTextComponent textSource(JComponent component) {
        if (component instanceof JTextComponent input) return input;
        var combo = (JComboBox<?>) component;
        return (JTextComponent) combo.getEditor().getEditorComponent();
    }
}
