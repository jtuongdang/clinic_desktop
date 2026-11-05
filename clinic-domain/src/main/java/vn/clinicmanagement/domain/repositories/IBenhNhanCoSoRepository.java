package vn.clinicmanagement.domain.repositories;

/** Persistence port for maintaining a patient's active facility association. */
public interface IBenhNhanCoSoRepository {
    /**
     * Ensures an active patient-facility association exists.
     *
     * @return true only when a new association was inserted
     */
    boolean ensureActiveLink(int patientId, int facilityId, int createdBy);
}
