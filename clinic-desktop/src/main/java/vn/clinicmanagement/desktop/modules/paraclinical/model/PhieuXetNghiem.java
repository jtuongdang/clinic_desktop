package vn.clinicmanagement.desktop.modules.paraclinical.model;

import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Read model shared by specimen collection and laboratory result workspaces. */
public record PhieuXetNghiem(
            int orderId,
            int visitId,
            String orderCode,
            String barcode,
            int queueNumber,
            String patientCode,
            String patientName,
            LocalDate birthDate,
            String gender,
            String address,
            String subject,
            String bhytNumber,
            int bhytRate,
            LocalDate bhytExpiry,
            String treatmentType,
            String departmentName,
            String roomName,
            String diagnosis,
            String serviceName,
            BigDecimal quantity,
            String specimenType,
            String note,
            String status,
            LocalDateTime orderedAt,
            LocalDateTime collectedAt,
            LocalDateTime resultAt,
            String orderedByName,
            String performerName,
            String resultText,
            String conclusionText,
            String collectPlace,
            String testGroupName,
            LocalDateTime collectEndAt,
            String collectorName,
            boolean paid,
            List<DongXetNghiem> tests) {

        public record DongXetNghiem(
                int orderId,
                String serviceCode,
                String serviceName,
                BigDecimal quantity,
                String specimenType,
                String note,
                String status,
                String resultText,
                String conclusionText,
                LocalDateTime resultAt,
                String performerName) {
            public DongXetNghiem(
                    int orderId,
                    String serviceCode,
                    String serviceName,
                    BigDecimal quantity,
                    String specimenType,
                    String note,
                    TrangThaiChiDinhDichVu statusEnum,
                    String resultText,
                    String conclusionText,
                    LocalDateTime resultAt,
                    String performerName) {
                this(orderId, serviceCode, serviceName, quantity, specimenType, note,
                        (statusEnum != null ? statusEnum : TrangThaiChiDinhDichVu.CHO_THUC_HIEN).name(),
                        resultText, conclusionText, resultAt, performerName);
            }

            public DongXetNghiem(
                    int orderId,
                    String serviceName,
                    BigDecimal quantity,
                    String specimenType,
                    String note,
                    String status,
                    String resultText,
                    String conclusionText) {
                this(orderId, "", serviceName, quantity, specimenType, note, status, resultText, conclusionText, null, "");
            }

            public TrangThaiChiDinhDichVu statusEnum() {
                return TrangThaiChiDinhDichVu.fromCode(status);
            }
        }

        public PhieuXetNghiem(
                int orderId,
                int visitId,
                String orderCode,
                String barcode,
                int queueNumber,
                String patientCode,
                String patientName,
                LocalDate birthDate,
                String gender,
                String address,
                String subject,
                String bhytNumber,
                int bhytRate,
                LocalDate bhytExpiry,
                String treatmentType,
                String departmentName,
                String roomName,
                String diagnosis,
                String serviceName,
                BigDecimal quantity,
                String specimenType,
                String note,
                TrangThaiChiDinhDichVu statusEnum,
                LocalDateTime orderedAt,
                LocalDateTime collectedAt,
                LocalDateTime resultAt,
                String orderedByName,
                String performerName,
                String resultText,
                String conclusionText,
                String collectPlace,
                String testGroupName,
                LocalDateTime collectEndAt,
                String collectorName,
                boolean paid,
                List<DongXetNghiem> tests) {
            this(orderId, visitId, orderCode, barcode, queueNumber, patientCode, patientName, birthDate, gender, address,
                    subject, bhytNumber, bhytRate, bhytExpiry, treatmentType, departmentName, roomName, diagnosis,
                    serviceName, quantity, specimenType, note,
                    (statusEnum != null ? statusEnum : TrangThaiChiDinhDichVu.CHO_THUC_HIEN).name(),
                    orderedAt, collectedAt, resultAt, orderedByName, performerName, resultText, conclusionText,
                    collectPlace, testGroupName, collectEndAt, collectorName, paid, tests);
        }

        public TrangThaiChiDinhDichVu statusEnum() {
            return TrangThaiChiDinhDichVu.fromCode(status);
        }

        public PhieuXetNghiem(
                int orderId, int visitId, String orderCode, String barcode, int queueNumber,
                String patientCode, String patientName, LocalDate birthDate, String gender, String address,
                String subject, String bhytNumber, int bhytRate, LocalDate bhytExpiry, String treatmentType,
                String departmentName, String roomName, String diagnosis, String serviceName, BigDecimal quantity,
                String specimenType, String note, String status, LocalDateTime orderedAt, LocalDateTime collectedAt,
                LocalDateTime resultAt, String orderedByName, String performerName, String resultText, String conclusionText) {
            this(orderId, visitId, orderCode, barcode, queueNumber, patientCode, patientName, birthDate, gender, address,
                    subject, bhytNumber, bhytRate, bhytExpiry, treatmentType, departmentName, roomName, diagnosis,
                    serviceName, quantity, specimenType, note, status, orderedAt, collectedAt, resultAt,
                    orderedByName, performerName, resultText, conclusionText,
                    roomName, "Hóa sinh", resultAt, performerName, true,
                    List.of(new DongXetNghiem(orderId, serviceName, quantity, specimenType, note, status, resultText, conclusionText)));
        }

        public boolean isBhyt() { return "BHYT".equalsIgnoreCase(subject); }
        public boolean isInpatient() { return "NOI_TRU".equalsIgnoreCase(treatmentType); }

        public String shortSubjectTag() {
            String p = isBhyt() ? "BHYT" : "Viện phí";
            String t = isInpatient() ? "NT" : "KB";
            return p + " - " + t;
        }
    }
