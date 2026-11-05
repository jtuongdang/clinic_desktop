package vn.clinicmanagement.desktop.modules.reception;

import vn.clinicmanagement.application.reception.*;
import vn.clinicmanagement.infrastructure.persistence.*;
import vn.clinicmanagement.infrastructure.persistence.facility.PhongKhamRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.patient.BaoHiemRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.patient.BenhNhanRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.reception.ReceptionQueueRepositoryImpl;

/** Shared JDBC adapters for reception use cases; repositories participate in one UoW. */
final class ReceptionCompositionRoot {
    private ReceptionCompositionRoot() {}

    static Services create() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = new JdbcTransactionManager(unitOfWork);
        JdbcConnectionProvider connections = new JdbcConnectionProvider(transactions);
        var patients = new BenhNhanRepositoryImpl(connections, unitOfWork);
        var insurance = new BaoHiemRepositoryImpl(connections, unitOfWork);
        var rooms = new PhongKhamRepositoryImpl(connections);
        var queue = new ReceptionQueueRepositoryImpl(connections, unitOfWork);
        return new Services(rooms, new RegisterPatientHandler(patients, insurance, rooms, queue, transactions),
                new SearchPatientsHandler(patients, insurance), new GetReceptionQueueHandler(queue));
    }

    record Services(PhongKhamRepositoryImpl rooms, RegisterPatientHandler registerPatient,
                    SearchPatientsHandler searchPatients, GetReceptionQueueHandler getQueue) {}
}
