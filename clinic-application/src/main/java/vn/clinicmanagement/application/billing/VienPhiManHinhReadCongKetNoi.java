package vn.clinicmanagement.application.billing;
import java.time.*;
import java.util.*;
public interface VienPhiManHinhReadCongKetNoi {
    Optional<ThongTin> getByVisit(int visitId, int facilityId);
    record ThongTin(String patientCode, String patientName, LocalDate birthDate, String gender,
            String address, String phone, String department, String room, String subject,
            String visitStatus, LocalDateTime admittedAt, LocalDateTime dischargedAt,
            Map<Integer,String> collectors, String treatment, String registrationType,
            String bhytNumber, LocalDate bhytExpiry, String diagnosis, String facilityName) {
        public ThongTin(String patientCode,String patientName,LocalDate birthDate,String gender,String address,String phone,
                String department,String room,String subject,String visitStatus,LocalDateTime admittedAt,LocalDateTime dischargedAt,Map<Integer,String> collectors){
            this(patientCode,patientName,birthDate,gender,address,phone,department,room,subject,visitStatus,admittedAt,dischargedAt,collectors,"NGOAI_TRU","Khám bệnh","",null,"","");
        }
        public ThongTin(String patientCode, String patientName, LocalDate birthDate, String gender,
                String address, String phone, String department, String room, String subject,
                String visitStatus, LocalDateTime admittedAt, LocalDateTime dischargedAt,
                Map<Integer,String> collectors, String treatment, String registrationType) {
            this(patientCode,patientName,birthDate,gender,address,phone,department,room,subject,visitStatus,admittedAt,dischargedAt,collectors,treatment,registrationType,"",null,"","");
        }
    }
}
