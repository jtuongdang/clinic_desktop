package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.repositories.IBaoHiemRepository;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;

import java.util.List;

public final class SearchPatientsHandler implements QueryHandler<SearchPatientsQuery, List<PatientSearchItem>> {
    private final IBenhNhanRepository patients;
    private final IBaoHiemRepository insuranceCards;

    public SearchPatientsHandler(IBenhNhanRepository patients, IBaoHiemRepository insuranceCards) {
        this.patients = patients;
        this.insuranceCards = insuranceCards;
    }

    @Override
    public Result<List<PatientSearchItem>> handle(SearchPatientsQuery query) {
        if (query == null || query.keyword() == null || query.keyword().isBlank())
            return Result.failure("SEARCH_KEYWORD_REQUIRED", "Nhập mã bệnh nhân, CCCD, số điện thoại hoặc họ tên để tìm.");
        try {
            List<PatientSearchItem> matches = patients.search(query.keyword().trim()).stream().limit(50)
                    .map(patient -> new PatientSearchItem(patient,
                            insuranceCards.getLatestByPatientId(patient.getId()).orElse(null)))
                    .toList();
            return Result.success(matches);
        } catch (RuntimeException ex) {
            return Result.failure("PATIENT_SEARCH_FAILED", "Không thể tìm hồ sơ lúc này. Hãy kiểm tra kết nối cơ sở dữ liệu.");
        }
    }
}
