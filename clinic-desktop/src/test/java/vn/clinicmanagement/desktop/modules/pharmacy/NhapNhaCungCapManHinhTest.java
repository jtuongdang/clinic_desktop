package vn.clinicmanagement.desktop.modules.pharmacy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.inventory.receive.NhapNhaCungCap;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.core.session.PhienThuNganKiemThu;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhapNhaCungCapManHinh;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class NhapNhaCungCapManHinhTest {
    @AfterEach
    void logout() {
        NguoiDungPhien.getInstance().clearSession();
    }

    @Test
    void addsDrugLineCalculatesTotalAndPostsReceipt() throws Exception {
        PhienThuNganKiemThu.login("PHARMACIST");
        AtomicReference<NhapNhaCungCap.DraftReceipt> savedDraft = new AtomicReference<>();
        CountDownLatch refreshed = new CountDownLatch(1);

        NhapNhaCungCap service = new NhapNhaCungCap() {
            public List<Warehouse> warehouses(PhanQuyenNguCanh auth) {
                return List.of(new Warehouse(1, "Kho Nhà Thuốc"));
            }

            public List<Supplier> suppliers(PhanQuyenNguCanh auth) {
                return List.of(new Supplier(5, "Dược Hậu Giang", "1800156", "02923", "dhg@test.vn", "Cần Thơ"));
            }

            public KetQua<Supplier> saveSupplier(Supplier supplier, PhanQuyenNguCanh auth) {
                return KetQua.success(supplier);
            }

            public List<DrugOption> drugs(PhanQuyenNguCanh auth) {
                return List.of(new DrugOption(9, "PARA500", "Paracetamol 500mg", "Viên", "500mg", "500mg",
                        "Việt Nam", "DHG", "VD-001", "Giảm đau", BigDecimal.valueOf(1000), BigDecimal.valueOf(1500)));
            }

            public KetQua<String> save(DraftReceipt receipt, PhanQuyenNguCanh auth) {
                savedDraft.set(receipt);
                return KetQua.success("PNK-TEST-01");
            }
        };

        NhapNhaCungCapManHinh[] holder = {null};
        SwingUtilities.invokeAndWait(() -> holder[0] = new NhapNhaCungCapManHinh(service, 1, refreshed::countDown));
        for (int i = 0; i < 100; i++) {
            boolean[] ready = {false};
            SwingUtilities.invokeAndWait(() -> {
                var ncc = findField(holder[0], JComboBox.class, "Tên NCC:");
                var thuoc = findField(holder[0], JComboBox.class, "Tên thuốc:");
                ready[0] = ncc != null && ncc.getItemCount() > 0 && thuoc != null && thuoc.getItemCount() > 0;
            });
            if (ready[0]) break;
            Thread.sleep(20);
        }

        SwingUtilities.invokeAndWait(() -> {
            var p = holder[0];
            p.setSize(1240, 680);
            setFieldNearLabel(p, "SL nhập:", "10");
            setFieldNearLabel(p, "Giá mua:", "2000");
            setFieldNearLabel(p, "Số lô:", "LO-99");
            setExpiry(p, "31", "12", "2028");
            findButton(p, "Thêm Dòng (Ctrl+A)").doClick();

            assertEquals(1, p.getDrugTable().getRowCount());
            findButton(p, "Nhập Kho").doClick();
        });

        assertTrue(refreshed.await(5, TimeUnit.SECONDS));
        assertNotNull(savedDraft.get());
        assertTrue(savedDraft.get().postStock());
        assertEquals(0, new BigDecimal("20000.00").compareTo(savedDraft.get().total()));
        assertEquals("LO-99", savedDraft.get().lines().get(0).batch());
    }

    private static JButton findButton(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton b && text.equals(b.getText())) return b;
            if (c instanceof Container nested) {
                JButton found = findButton(nested, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends JComponent> T findField(Container root, Class<T> type, String label) {
        Component[] comps = root.getComponents();
        for (int i = 0; i < comps.length - 1; i++) {
            if (comps[i] instanceof JLabel l && label.equals(l.getText()) && type.isInstance(comps[i + 1])) {
                return type.cast(comps[i + 1]);
            }
            if (comps[i] instanceof Container nested) {
                T found = findField(nested, type, label);
                if (found != null) return found;
            }
        }
        if (comps.length > 0 && comps[comps.length - 1] instanceof Container nested) {
            return findField(nested, type, label);
        }
        return null;
    }

    private static void setFieldNearLabel(Container root, String label, String value) {
        JTextField f = findField(root, JTextField.class, label);
        assertNotNull(f, "Missing field for " + label);
        f.setText(value);
    }

    private static void setExpiry(Container root, String d, String m, String y) {
        JPanel p = findField(root, JPanel.class, "Hạn SD:");
        assertNotNull(p);
        ((JTextField) p.getComponent(0)).setText(d);
        ((JTextField) p.getComponent(1)).setText(m);
        ((JTextField) p.getComponent(2)).setText(y);
    }
}
