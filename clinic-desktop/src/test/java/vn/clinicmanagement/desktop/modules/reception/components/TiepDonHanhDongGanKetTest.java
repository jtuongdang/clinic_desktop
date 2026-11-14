package vn.clinicmanagement.desktop.modules.reception.components;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TiepDonHanhDongGanKetTest {

    @Test
    void toolbarAndFooterExposeReplaceableActionsAndSavingState() throws Exception {
        onEdt(() -> {
            AtomicInteger refreshCount = new AtomicInteger();
            AtomicInteger newCount = new AtomicInteger();
            AtomicInteger saveCount = new AtomicInteger();

            TiepDonTrenThanhCongCu toolbar = new TiepDonTrenThanhCongCu();
            toolbar.setOnRefresh(refreshCount::incrementAndGet);
            toolbar.setOnNew(newCount::incrementAndGet);
            findButton(toolbar, "Làm Mới").doClick();
            findButton(toolbar, "Bệnh Nhân Mới").doClick();

            TiepDonChanTrangThanh footer = new TiepDonChanTrangThanh();
            footer.setOnSave(saveCount::incrementAndGet);
            footer.setOnNew(newCount::incrementAndGet);
            JButton save = findButtonContaining(footer, "Lưu Lại");
            footer.setSaving(true);
            assertFalse(save.isEnabled());
            save.doClick();
            assertEquals(0, saveCount.get());
            footer.setSaving(false);
            save.doClick();
            findButtonContaining(footer, "Bệnh Nhân Mới").doClick();

            assertEquals(1, refreshCount.get());
            assertEquals(2, newCount.get());
            assertEquals(1, saveCount.get());
            return null;
        });
    }

    @Test
    void footerBarBindsF1AndF2ShortcutsRespectingSavingState() throws Exception {
        onEdt(() -> {
            AtomicInteger newCount = new AtomicInteger();
            AtomicInteger saveCount = new AtomicInteger();

            TiepDonChanTrangThanh footer = new TiepDonChanTrangThanh();
            footer.setOnNew(newCount::incrementAndGet);
            footer.setOnSave(saveCount::incrementAndGet);

            InputMap inputMap = footer.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = footer.getActionMap();

            KeyStroke f1Key = KeyStroke.getKeyStroke("F1");
            KeyStroke f2Key = KeyStroke.getKeyStroke("F2");

            Object f1ActionKey = inputMap.get(f1Key);
            Object f2ActionKey = inputMap.get(f2Key);
            assertNotNull(f1ActionKey, "F1 key must be bound in WHEN_IN_FOCUSED_WINDOW InputMap");
            assertNotNull(f2ActionKey, "F2 key must be bound in WHEN_IN_FOCUSED_WINDOW InputMap");

            Action f1Action = actionMap.get(f1ActionKey);
            Action f2Action = actionMap.get(f2ActionKey);
            assertNotNull(f1Action, "ActionMap must contain an action for F1");
            assertNotNull(f2Action, "ActionMap must contain an action for F2");

            // F1 triggers onNew
            f1Action.actionPerformed(new ActionEvent(footer, ActionEvent.ACTION_PERFORMED, "F1"));
            assertEquals(1, newCount.get(), "F1 shortcut should trigger onNew");

            // F2 triggers onSave when enabled
            f2Action.actionPerformed(new ActionEvent(footer, ActionEvent.ACTION_PERFORMED, "F2"));
            assertEquals(1, saveCount.get(), "F2 shortcut should trigger onSave when not saving");

            // F2 does not trigger onSave when saving is true
            footer.setSaving(true);
            f2Action.actionPerformed(new ActionEvent(footer, ActionEvent.ACTION_PERFORMED, "F2"));
            assertEquals(1, saveCount.get(), "F2 shortcut should not trigger onSave when saving");

            // F2 triggers onSave again when saving is reset to false
            footer.setSaving(false);
            f2Action.actionPerformed(new ActionEvent(footer, ActionEvent.ACTION_PERFORMED, "F2"));
            assertEquals(2, saveCount.get(), "F2 shortcut should trigger onSave once saving state is cleared");

            return null;
        });
    }

    private static JButton findButton(Container container, String text) {
        return findButtons(container).stream()
                .filter(button -> text.equals(button.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing button: " + text));
    }

    private static JButton findButtonContaining(Container container, String text) {
        return findButtons(container).stream()
                .filter(button -> button.getText() != null && button.getText().contains(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing button containing: " + text));
    }

    private static java.util.List<JButton> findButtons(Container container) {
        java.util.List<JButton> buttons = new java.util.ArrayList<>();
        for (Component component : container.getComponents()) {
            if (component instanceof JButton button) buttons.add(button);
            if (component instanceof Container child) buttons.addAll(findButtons(child));
        }
        return buttons;
    }

    private static <T> T onEdt(Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }
}
