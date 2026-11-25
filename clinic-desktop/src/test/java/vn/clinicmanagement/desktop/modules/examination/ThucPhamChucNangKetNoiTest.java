package vn.clinicmanagement.desktop.modules.examination;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.core.session.PhienThuNganKiemThu;
import vn.clinicmanagement.desktop.modules.examination.components.dialogs.ChiDinhThucPhamChucNangHopThoai;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThucPhamChucNangKetNoiTest {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @AfterEach
    void logout() {
        NguoiDungPhien.getInstance().clearSession();
    }

    @Test
    void loadsFunctionalFoodCatalogAndReportViaPort() throws Exception {
        PhienThuNganKiemThu.login("DOCTOR");
        NhaThuocBaoCaoReadPort fakePort = new NhaThuocBaoCaoReadPort() {
            @Override
            public BaoCaoKetQua getReport(LoaiBaoCao type, int facilityId, Integer warehouseId, LocalDateTime from, LocalDateTime to) {
                return new BaoCaoKetQua(type.title(), List.of(), List.of(), "");
            }

            @Override
            public List<FunctionalFoodItem> listFunctionalFoodItems(int facilityId) {
                assertEquals(1, facilityId);
                return List.of(
                        new FunctionalFoodItem(901, "TPCN001", "Viên uống Bổ Mắt Lutein Gold", "Hộp", "Hộp 30 viên",
                                new BigDecimal("180000"), new BigDecimal("200")),
                        new FunctionalFoodItem(902, "TPCN002", "Hoạt Huyết Dưỡng Não Ginkgo Biloba", "Hộp", "Hộp 60 viên",
                                new BigDecimal("150000"), new BigDecimal("200"))
                );
            }
        };

        KhamBenhManHinh[] screen = new KhamBenhManHinh[1];
        ChiDinhThucPhamChucNangHopThoai[] panel = new ChiDinhThucPhamChucNangHopThoai[1];
        SwingUtilities.invokeAndWait(() -> {
            screen[0] = new KhamBenhManHinh(KhamBenhGhepNoiGoc.createDefault());
            screen[0].setTpcnReportPort(fakePort);
            panel[0] = screen[0].createFunctionalFoodPanel();
        });

        for (int i = 0; i < 100; i++) {
            Thread.sleep(20);
            int[] count = {0};
            SwingUtilities.invokeAndWait(() -> count[0] = panel[0].getCboItems().getItemCount());
            if (count[0] == 2) break;
        }

        SwingUtilities.invokeAndWait(() -> {
            assertEquals(2, panel[0].getCboItems().getItemCount());
            assertEquals("TPCN001", panel[0].getCboItems().getItemAt(0).code());
        });
    }
}
