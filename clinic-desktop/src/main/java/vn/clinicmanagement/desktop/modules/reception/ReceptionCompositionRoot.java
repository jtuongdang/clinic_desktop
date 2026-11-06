package vn.clinicmanagement.desktop.modules.reception;

import vn.clinicmanagement.application.authorization.AuthorizationService;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.reception.*;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.infrastructure.persistence.*;
import vn.clinicmanagement.infrastructure.persistence.facility.PhongKhamRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.identity.NhanVienRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.patient.BaoHiemRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.patient.BenhNhanCoSoRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.patient.BenhNhanRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.reception.ReceptionQueueRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.system.JdbcAuditRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.system.SystemRepositoryImpl;

/** Shared JDBC adapters for reception use cases; repositories participate in one UoW. */
final class ReceptionCompositionRoot {
    private ReceptionCompositionRoot() {}

    static Services create(SessionLifecycleService sessionLifecycle) {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = new JdbcTransactionManager(unitOfWork);
        JdbcConnectionProvider connections = new JdbcConnectionProvider(transactions);
        var patients = new BenhNhanRepositoryImpl(connections, unitOfWork);
        var patientFacilities = new BenhNhanCoSoRepositoryImpl(connections, unitOfWork);
        var insurance = new BaoHiemRepositoryImpl(connections, unitOfWork);
        var rooms = new PhongKhamRepositoryImpl(connections);
        var queue = new ReceptionQueueRepositoryImpl(connections, unitOfWork);
        var audit = new JdbcAuditRepositoryImpl(connections, unitOfWork, DatabaseConnection::getConnection);
        var authorization = new AuthorizationService(sessionLifecycle,
                new NhanVienRepositoryImpl(connections, unitOfWork),
                new SystemRepositoryImpl(connections, unitOfWork));
        return new Services(rooms, new RegisterPatientHandler(
                patients, patientFacilities, insurance, rooms, queue, transactions, audit, authorization),
                new SearchPatientsHandler(patients, insurance, authorization, audit),
                new GetReceptionQueueHandler(queue, authorization, audit),
                new GetActiveReceptionRoomsHandler(rooms, authorization, audit));
    }

    record Services(PhongKhamRepositoryImpl rooms,
                    CommandHandler<RegisterPatientCommand, RegistrationReceipt> registerPatient,
                    QueryHandler<SearchPatientsQuery, java.util.List<PatientSearchItem>> searchPatients,
                    QueryHandler<GetReceptionQueueQuery, ReceptionQueueSnapshot> getQueue,
                    QueryHandler<GetActiveReceptionRoomsQuery, java.util.List<PhongKham>> getActiveRooms) {}
}
