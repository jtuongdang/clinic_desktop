package vn.clinicmanagement.desktop.modules.doctor;

import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationService;
import vn.clinicmanagement.application.patienthistory.GetPatientHistoryHandler;
import vn.clinicmanagement.application.reception.GetDoctorQueueHandler;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.session.UserSession;
import vn.clinicmanagement.infrastructure.persistence.DatabaseConnection;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;
import vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;
import vn.clinicmanagement.infrastructure.persistence.identity.NhanVienRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.patient.TienSuBenhRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.reception.ReceptionQueueRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.system.JdbcAuditRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.system.SystemRepositoryImpl;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/** Builds the guarded, audited JDBC reads used by the Doctor station. */
public final class DoctorCompositionRoot {
    private DoctorCompositionRoot() {}

    private static final AuditWritePort FALLBACK_AUDIT = new AuditWritePort() {
        @Override public void appendInCurrentTransaction(SystemAuditEntry entry) {}
        @Override public void appendIndependently(SystemAuditEntry entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {}
        @Override public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {}
    };

    public static Services createDefault() {
        SessionLifecycleService lifecycle = new SessionLifecycleService(
                UserSession.getInstance(),
                new SessionTimeoutPolicy(Duration.ofMinutes(15), Duration.ofHours(12)),
                Clock.systemUTC(),
                FALLBACK_AUDIT);
        return create(lifecycle);
    }

    public static Services create(SessionLifecycleService sessionLifecycle) {
        Objects.requireNonNull(sessionLifecycle, "sessionLifecycle");
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = new JdbcTransactionManager(unitOfWork);
        JdbcConnectionProvider connections = new JdbcConnectionProvider(transactions);
        JdbcAuditRepositoryImpl audit = new JdbcAuditRepositoryImpl(
                connections, unitOfWork, DatabaseConnection::getConnection);
        AuthorizationService authorization = new AuthorizationService(
                sessionLifecycle,
                new NhanVienRepositoryImpl(connections, unitOfWork),
                new SystemRepositoryImpl(connections, unitOfWork));
        return new Services(
                new GetDoctorQueueHandler(
                        new ReceptionQueueRepositoryImpl(connections, unitOfWork), authorization, audit),
                new GetPatientHistoryHandler(
                        new TienSuBenhRepositoryImpl(connections), authorization, audit));
    }

    public record Services(
            GetDoctorQueueHandler doctorQueue,
            GetPatientHistoryHandler patientHistory) {
        public Services {
            Objects.requireNonNull(doctorQueue, "doctorQueue");
            Objects.requireNonNull(patientHistory, "patientHistory");
        }
    }
}
