package vn.clinicmanagement.desktop.modules.reports;

import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.reports.GetKhoBaoCaoHandler;
import vn.clinicmanagement.application.reports.GetDoanhThuBaoCaoHandler;
import vn.clinicmanagement.application.reports.GetLuotKhamThongKeHandler;
import vn.clinicmanagement.application.reports.IKhoBaoCaoDuLieuNguon;
import vn.clinicmanagement.application.reports.IDoanhThuBaoCaoDuLieuNguon;
import vn.clinicmanagement.application.reports.ILuotKhamThongKeDuLieuNguon;
import vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;

/**
 * Composition Root cho phân hệ Báo cáo Thống kê & Phân tích (Reports & Analytics).
 * Khởi tạo và liên kết các Query Handlers theo Clean Architecture.
 * Tuân thủ Hợp đồng Cách ly 18847587: 100% in-memory / zero dependency tới SQL Server.
 */
public final class BaoCaoGhepNoiGoc {

    private static final KiemToanWriteCongKetNoi NOOP_AUDIT = new KiemToanWriteCongKetNoi() {
        /** Hàm `appendInCurrentTransaction`: thêm vào trong hientai giao dịch. */
        @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {}
        /** Hàm `appendIndependently`: thêm vào `Independently`. */
        @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {}
        /** Hàm `appendMedicalRecordInCurrentTransaction`: thêm vào yte ghi nhận trong hientai giao dịch. */
        @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}
        /** Hàm `appendMedicalRecordIndependently`: thêm vào yte ghi nhận `Independently`. */
        @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
    };

    private BaoCaoGhepNoiGoc() {
        // Static factory only
    }

    public static final class Services {
        /** Trường `visitStatisticsHandler`: luotkham thongke xử lý. */
        private final GetLuotKhamThongKeHandler visitStatisticsHandler;
        /** Trường `revenueReportHandler`: doanhthu baocao xử lý. */
        private final GetDoanhThuBaoCaoHandler revenueReportHandler;
        /** Trường `inventoryReportHandler`: kho baocao xử lý. */
        private final GetKhoBaoCaoHandler inventoryReportHandler;

        public Services(
                GetLuotKhamThongKeHandler visitStatisticsHandler,
                GetDoanhThuBaoCaoHandler revenueReportHandler,
                GetKhoBaoCaoHandler inventoryReportHandler) {
            this.visitStatisticsHandler = Objects.requireNonNull(visitStatisticsHandler, "visitStatisticsHandler");
            this.revenueReportHandler = Objects.requireNonNull(revenueReportHandler, "revenueReportHandler");
            this.inventoryReportHandler = Objects.requireNonNull(inventoryReportHandler, "inventoryReportHandler");
        }

        /** Hàm `getVisitStatisticsHandler`: lấy luotkham thongke xử lý. */
        public GetLuotKhamThongKeHandler getVisitStatisticsHandler() {
            return visitStatisticsHandler;
        }

        /** Hàm `getRevenueReportHandler`: lấy doanhthu baocao xử lý. */
        public GetDoanhThuBaoCaoHandler getRevenueReportHandler() {
            return revenueReportHandler;
        }

        /** Hàm `getInventoryReportHandler`: lấy kho baocao xử lý. */
        public GetKhoBaoCaoHandler getInventoryReportHandler() {
            return inventoryReportHandler;
        }
    }

    /** Hàm `create`: tạo. */
    public static Services create(
            GetLuotKhamThongKeHandler visitStatisticsHandler,
            GetDoanhThuBaoCaoHandler revenueReportHandler,
            GetKhoBaoCaoHandler inventoryReportHandler) {
        return new Services(visitStatisticsHandler, revenueReportHandler, inventoryReportHandler);
    }

    /** Hàm `create`: tạo. */
    public static Services create(
            ILuotKhamThongKeDuLieuNguon visitDataSource,
            IDoanhThuBaoCaoDuLieuNguon revenueDataSource,
            IKhoBaoCaoDuLieuNguon inventoryDataSource,
            KiemToanWriteCongKetNoi auditPort) {
        KiemToanWriteCongKetNoi audit = auditPort != null ? auditPort : NOOP_AUDIT;
        return new Services(
                new GetLuotKhamThongKeHandler(visitDataSource != null ? visitDataSource : (f, t, fa, d) -> Collections.emptyList(), audit),
                new GetDoanhThuBaoCaoHandler(revenueDataSource != null ? revenueDataSource : (f, t, fa, d) -> Collections.emptyList(), audit),
                new GetKhoBaoCaoHandler(inventoryDataSource != null ? inventoryDataSource : createEmptyInventorySource(), audit)
        );
    }

    /** Hàm `createDefault`: tạo macdinh. */
    public static Services createDefault() {
        return create(
                (fromDate, toDate, facilityId, deptId) -> Collections.emptyList(),
                (fromDate, toDate, facilityId, deptId) -> Collections.emptyList(),
                createEmptyInventorySource(),
                NOOP_AUDIT
        );
    }

    /** Hàm `create`: tạo. */
    public static Services create(PhienVongDoiDichVu sessionLifecycle) {
        return createDefault();
    }

    /** Hàm `createEmptyInventorySource`: tạo rỗng kho nguồn. */
    private static IKhoBaoCaoDuLieuNguon createEmptyInventorySource() {
        return new IKhoBaoCaoDuLieuNguon() {
            /** Hàm `getBatches`: lấy `Batches`. */
            @Override
            public java.util.List<vn.clinicmanagement.domain.inventory.TonKho> getBatches(Integer warehouseId) {
                return Collections.emptyList();
            }

            /** Hàm `getItemById`: lấy mathang theo mã. */
            @Override
            public vn.clinicmanagement.domain.inventory.MatHang getItemById(int itemId) {
                return null;
            }

            /** Hàm `getAllItems`: lấy `All` `Items`. */
            @Override
            public java.util.List<vn.clinicmanagement.domain.inventory.MatHang> getAllItems() {
                return Collections.emptyList();
            }
        };
    }
}
