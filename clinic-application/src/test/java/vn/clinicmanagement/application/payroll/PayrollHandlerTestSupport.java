package vn.clinicmanagement.application.payroll;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.domain.payroll.BangLuong;
import vn.clinicmanagement.domain.payroll.KyLuong;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class PayrollHandlerTestSupport {
    private PayrollHandlerTestSupport() {}

    static KyLuong period(int id, int month, int year, boolean locked) {
        KyLuong period = new KyLuong();
        period.setId(id);
        period.setTenKy(month + "/" + year);
        period.setThang(month);
        period.setNam(year);
        period.setDaChot(locked);
        return period;
    }

    static AuthorizationContext authorization(String roleCode) {
        return new AuthorizationContext(10, 2, roleCode, 1, 3, UUID.randomUUID());
    }

    static AuditActorContext actor(String roleCode) {
        return new AuditActorContext(10, 2, roleCode, 1, 3, "PAYROLL-TEST", UUID.randomUUID());
    }

    static final class InMemoryPayrollRepository implements PayrollRepository {
        final Map<Integer, KyLuong> periods = new HashMap<>();
        final List<BangLuong> payrolls = new ArrayList<>();
        private int payrollIdSequence = 1;

        @Override
        public Optional<KyLuong> findPeriodById(int periodId) {
            return Optional.ofNullable(periods.get(periodId));
        }

        @Override
        public Optional<KyLuong> findPeriodByMonthAndYear(int month, int year) {
            return periods.values().stream()
                    .filter(period -> period.getThang() == month && period.getNam() == year)
                    .findFirst();
        }

        @Override
        public KyLuong savePeriod(KyLuong period) {
            periods.put(period.getId(), period);
            return period;
        }

        @Override
        public BangLuong savePayroll(BangLuong payroll) {
            payroll.setId(payrollIdSequence++);
            payrolls.add(payroll);
            return payroll;
        }

        @Override
        public List<BangLuong> findPayrollByPeriodId(int periodId) {
            return payrolls.stream().filter(payroll -> payroll.getMaKyLuong() == periodId).toList();
        }
    }

    static final class RecordingPayrollAuditPort implements PayrollAuditWritePort {
        final List<PayrollPeriodLockedAudit> entries = new ArrayList<>();

        @Override
        public void appendIndependently(PayrollPeriodLockedAudit entry) {
            entries.add(entry);
        }
    }
}
