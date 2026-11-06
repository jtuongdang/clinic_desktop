package vn.clinicmanagement.desktop.modules.reception.components;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ReceptionActionBindingsTest {

    @Test
    void toolbarAndFooterExposeReplaceableActionsAndSavingState() throws Exception {
        onEdt(() -> {
            AtomicInteger refreshCount = new AtomicInteger();
            AtomicInteger newCount = new AtomicInteger();
            AtomicInteger saveCount = new AtomicInteger();

            ReceptionTopToolbar toolbar = new ReceptionTopToolbar();
            toolbar.setOnRefresh(refreshCount::incrementAndGet);
            toolbar.setOnNew(newCount::incrementAndGet);
            findButton(toolbar, "Làm Mới").doClick();
            findButton(toolbar, "Bệnh Nhân Mới").doClick();

            ReceptionFooterBar footer = new ReceptionFooterBar();
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
