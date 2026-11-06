package vn.clinicmanagement.desktop.modules.doctor.model;

/**
 * Đối tượng DTO biểu diễn thông tin bệnh nhân đang trong hàng đợi khám và điều trị
 */
public class PatientExamDto {
    private String name;
    private String code;
    private String gender;
    private String birthYear;
    private String age;
    private String address;
    private String bhytCode;
    private String bhytRate;
    private String expireDate;
    private String admissionDate;
    private String doctor;
    private String department;
    private String diagnosis;
    private String icdCode;

    public PatientExamDto(String name, String code, String gender, String birthYear, String age,
                          String address, String bhytCode, String bhytRate, String expireDate,
                          String admissionDate, String doctor, String department, String diagnosis, String icdCode) {
        this.name = name;
        this.code = code;
        this.gender = gender;
        this.birthYear = birthYear;
        this.age = age;
        this.address = address;
        this.bhytCode = bhytCode;
        this.bhytRate = bhytRate;
        this.expireDate = expireDate;
        this.admissionDate = admissionDate;
        this.doctor = doctor;
        this.department = department;
        this.diagnosis = diagnosis;
        this.icdCode = icdCode;
    }

    public String getName() { return name; }
    public String getCode() { return code; }
    public String getGender() { return gender; }
    public String getBirthYear() { return birthYear; }
    public String getAge() { return age; }
    public String getAddress() { return address; }
    public String getBhytCode() { return bhytCode; }
    public String getBhytRate() { return bhytRate; }
    public String getExpireDate() { return expireDate; }
    public String getAdmissionDate() { return admissionDate; }
    public String getDoctor() { return doctor; }
    public String getDepartment() { return department; }
    public String getDiagnosis() { return diagnosis; }
    public String getIcdCode() { return icdCode; }

    public String getShortCode() {
        if (code != null && code.contains(" - ")) {
            return code.split(" - ")[0];
        }
        return code;
    }
}
