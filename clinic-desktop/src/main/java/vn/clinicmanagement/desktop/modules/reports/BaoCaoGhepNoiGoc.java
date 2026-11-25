package vn.clinicmanagement.desktop.modules.reports;

import vn.clinicmanagement.application.common.Query;

import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.reports.get.GetKhoBaoCaoHandler;
import vn.clinicmanagement.application.reports.get.GetDoanhThuBaoCaoHandler;
import vn.clinicmanagement.application.reports.get.GetLuotKhamThongKeHandler;
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
        @Override public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendIndependently(HeThongKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}
        @Override public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
    };

    private BaoCaoGhepNoiGoc() {
        // Chỉ dùng làm factory tĩnh.
    }

    public static final class Services {
        private final GetLuotKhamThongKeHandler visitStatisticsHandler;
        private final GetDoanhThuBaoCaoHandler revenueReportHandler;
        private final GetKhoBaoCaoHandler inventoryReportHandler;

        public Services(
                GetLuotKhamThongKeHandler visitStatisticsHandler,
                GetDoanhThuBaoCaoHandler revenueReportHandler,
                GetKhoBaoCaoHandler inventoryReportHandler) {
            this.visitStatisticsHandler = Objects.requireNonNull(visitStatisticsHandler, "visitStatisticsHandler");
            this.revenueReportHandler = Objects.requireNonNull(revenueReportHandler, "revenueReportHandler");
            this.inventoryReportHandler = Objects.requireNonNull(inventoryReportHandler, "inventoryReportHandler");
        }

        public GetLuotKhamThongKeHandler getVisitStatisticsHandler() {
            return visitStatisticsHandler;
        }

        public GetDoanhThuBaoCaoHandler getRevenueReportHandler() {
            return revenueReportHandler;
        }

        public GetKhoBaoCaoHandler getInventoryReportHandler() {
            return inventoryReportHandler;
        }
    }

    public static Services create(
            GetLuotKhamThongKeHandler visitStatisticsHandler,
            GetDoanhThuBaoCaoHandler revenueReportHandler,
            GetKhoBaoCaoHandler inventoryReportHandler) {
        return new Services(visitStatisticsHandler, revenueReportHandler, inventoryReportHandler);
    }

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

    public static Services createDefault() {
        return create(
                (fromDate, toDate, facilityId, deptId) -> Collections.emptyList(),
                (fromDate, toDate, facilityId, deptId) -> Collections.emptyList(),
                createEmptyInventorySource(),
                NOOP_AUDIT
        );
    }

    public static Services create(PhienVongDoiDichVu sessionLifecycle) {
        return createDefault();
    }

    private static IKhoBaoCaoDuLieuNguon createEmptyInventorySource() {
        return new IKhoBaoCaoDuLieuNguon() {
            @Override
            public java.util.List<vn.clinicmanagement.domain.inventory.TonKho> getBatches(Integer warehouseId) {
                return Collections.emptyList();
            }

            @Override
            public vn.clinicmanagement.domain.inventory.MatHang getItemById(int itemId) {
                return null;
            }

            @Override
            public java.util.List<vn.clinicmanagement.domain.inventory.MatHang> getAllItems() {
                return Collections.emptyList();
            }
        };
    }
}
