package vn.clinicmanagement.desktop.modules.system.services;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.text.JTextComponent;

/** Mẫu và hướng dẫn dùng chung cho những lần thực hiện của một dịch vụ CLS. */
final class KetQuaClsCauHinhManHinh extends JPanel {
    private final JTextField template = new JTextField(20);
    private final JTextField templateType = new JTextField(20);
    private final JCheckBox noSeparateResult = new JCheckBox("Không in phiếu kết quả riêng");
    private final JCheckBox requireImages = new JCheckBox("Yêu cầu hình ảnh khi trả kết quả");
    private final JTextArea resultFormat = new JTextArea(5, 32);
    private final JTextArea conclusionTemplate = new JTextArea(5, 32);
    private final JTextArea reportInstructions = new JTextArea(4, 32);

    KetQuaClsCauHinhManHinh(Map<String, JComponent> fields) {
        super(new BorderLayout());
        setName("labResultConfiguration");
        template.setName("resultTemplateMirror");
        templateType.setName("resultTypeMirror");
        template.setDocument(((JTextField) fields.get("resultTemplate")).getDocument());
        var typeEditor = (JTextComponent) ((JComboBox<?>) fields.get("resultType")).getEditor().getEditorComponent();
        templateType.setDocument(typeEditor.getDocument());
        noSeparateResult.setModel(((JCheckBox) fields.get("noSeparateResult")).getModel());
        requireImages.setModel(((JCheckBox) fields.get("requireResultImages")).getModel());

        var content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        var report = new JPanel(new GridLayout(0, 2, 12, 8));
        report.setBorder(BorderFactory.createTitledBorder("Phiếu trả kết quả"));
        report.add(labeled("Mẫu phiếu trả kết quả", template, template));
        report.add(labeled("Loại mẫu kết quả", templateType, templateType));
        report.add(noSeparateResult);
        report.add(requireImages);
        content.add(report);
        content.add(Box.createVerticalStrut(12));

        var text = new JPanel(new GridLayout(0, 2, 12, 8));
        text.setBorder(BorderFactory.createTitledBorder("Nội dung mẫu"));
        text.add(textArea(fields, "clsResultFormat", "Bố cục nội dung kết quả", resultFormat));
        text.add(textArea(fields, "clsConclusionTemplate", "Mẫu kết luận", conclusionTemplate));
        text.add(textArea(fields, "clsReportInstructions", "Hướng dẫn nhập và trả kết quả", reportInstructions));
        content.add(text);
        add(new JScrollPane(content), BorderLayout.CENTER);
    }

    void setEditable(boolean editable) {
        for (JTextComponent input : List.of(template, templateType, resultFormat, conclusionTemplate, reportInstructions)) {
            input.setEditable(editable);
        }
        noSeparateResult.setEnabled(editable);
        requireImages.setEnabled(editable);
    }

    private static JPanel textArea(Map<String, JComponent> fields, String key, String label, JTextArea input) {
        input.setName(key);
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        fields.put(key, input);
        return labeled(label, input, new JScrollPane(input));
    }

    private static JPanel labeled(String label, JTextComponent input, JComponent editor) {
        var panel = new JPanel(new BorderLayout(0, 4));
        var caption = new JLabel(label);
        caption.setLabelFor(input);
        panel.add(caption, BorderLayout.NORTH);
        panel.add(editor, BorderLayout.CENTER);
        return panel;
    }
}
