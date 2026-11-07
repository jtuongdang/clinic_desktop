package vn.clinicmanagement.desktop.modules.reports;

import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.reports.GetInventoryReportHandler;
import vn.clinicmanagement.application.reports.GetRevenueReportHandler;
import vn.clinicmanagement.application.reports.GetVisitStatisticsHandler;
import vn.clinicmanagement.application.reports.IInventoryReportDataSource;
import vn.clinicmanagement.application.reports.IRevenueReportDataSource;
import vn.clinicmanagement.application.reports.IVisitStatisticsDataSource;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;

/**
 * Composition Root cho phân hệ Báo cáo Thống kê & Phân tích (Reports & Analytics).
 * Khởi tạo và liên kết các Query Handlers theo Clean Architecture.
 * Tuân thủ Hợp đồng Cách ly 18847587: 100% in-memory / zero dependency tới SQL Server.
 */
public final class ReportCompositionRoot {

    private static final AuditWritePort NOOP_AUDIT = new AuditWritePort() {
        @Override public void appendInCurrentTransaction(SystemAuditEntry entry) {}
        @Override public void appendIndependently(SystemAuditEntry entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {}
        @Override public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {}
    };

    private ReportCompositionRoot() {
        // Static factory only
    }

    public static final class Services {
        private final GetVisitStatisticsHandler visitStatisticsHandler;
        private final GetRevenueReportHandler revenueReportHandler;
        private final GetInventoryReportHandler inventoryReportHandler;

        public Services(
                GetVisitStatisticsHandler visitStatisticsHandler,
                GetRevenueReportHandler revenueReportHandler,
                GetInventoryReportHandler inventoryReportHandler) {
            this.visitStatisticsHandler = Objects.requireNonNull(visitStatisticsHandler, "visitStatisticsHandler");
            this.revenueReportHandler = Objects.requireNonNull(revenueReportHandler, "revenueReportHandler");
            this.inventoryReportHandler = Objects.requireNonNull(inventoryReportHandler, "inventoryReportHandler");
        }

        public GetVisitStatisticsHandler getVisitStatisticsHandler() {
            return visitStatisticsHandler;
        }

        public GetRevenueReportHandler getRevenueReportHandler() {
            return revenueReportHandler;
        }

        public GetInventoryReportHandler getInventoryReportHandler() {
            return inventoryReportHandler;
        }
    }

    public static Services create(
            GetVisitStatisticsHandler visitStatisticsHandler,
            GetRevenueReportHandler revenueReportHandler,
            GetInventoryReportHandler inventoryReportHandler) {
        return new Services(visitStatisticsHandler, revenueReportHandler, inventoryReportHandler);
    }

    public static Services create(
            IVisitStatisticsDataSource visitDataSource,
            IRevenueReportDataSource revenueDataSource,
            IInventoryReportDataSource inventoryDataSource,
            AuditWritePort auditPort) {
        AuditWritePort audit = auditPort != null ? auditPort : NOOP_AUDIT;
        return new Services(
                new GetVisitStatisticsHandler(visitDataSource != null ? visitDataSource : (f, t, fa, d) -> Collections.emptyList(), audit),
                new GetRevenueReportHandler(revenueDataSource != null ? revenueDataSource : (f, t, fa, d) -> Collections.emptyList(), audit),
                new GetInventoryReportHandler(inventoryDataSource != null ? inventoryDataSource : createEmptyInventorySource(), audit)
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

    public static Services create(SessionLifecycleService sessionLifecycle) {
        return createDefault();
    }

    private static IInventoryReportDataSource createEmptyInventorySource() {
        return new IInventoryReportDataSource() {
            @Override
            public java.util.List<vn.clinicmanagement.domain.inventory.TonKho> getBatches(Integer warehouseId) {
                return Collections.emptyList();
            }

            @Override
            public vn.clinicmanagement.domain.inventory.Item getItemById(int itemId) {
                return null;
            }

            @Override
            public java.util.List<vn.clinicmanagement.domain.inventory.Item> getAllItems() {
                return Collections.emptyList();
            }
        };
    }
}
