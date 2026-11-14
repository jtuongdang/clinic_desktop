package vn.clinicmanagement.desktop.modules.doctor.model;

/**
 * Đối tượng DTO biểu diễn thông tin bệnh nhân đang trong hàng đợi khám và điều trị
 */
public class BenhNhanKhamDuLieu {
    /** Trường `name`: tên. */
    private String name;
    /** Trường `code`: mã. */
    private String code;
    private String gender;
    private String birthYear;
    private String age;
    private String address;
    /** Trường `bhytCode`: `bhyt` mã. */
    private String bhytCode;
    private String bhytRate;
    /** Trường `expireDate`: `expire` ngày. */
    private String expireDate;
    /** Trường `admissionDate`: `admission` ngày. */
    private String admissionDate;
    /** Trường `doctor`: bacsi. */
    private String doctor;
    /** Trường `department`: khoa. */
    private String department;
    /** Trường `diagnosis`: chandoan. */
    private String diagnosis;
    /** Trường `icdCode`: icd mã. */
    private String icdCode;

    public BenhNhanKhamDuLieu(String name, String code, String gender, String birthYear, String age,
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

    /** Hàm `getName`: lấy tên. */
    public String getName() { return name; }
    /** Hàm `getCode`: lấy mã. */
    public String getCode() { return code; }
    /** Hàm `getGender`: lấy `Gender`. */
    public String getGender() { return gender; }
    /** Hàm `getBirthYear`: lấy `Birth` `Year`. */
    public String getBirthYear() { return birthYear; }
    /** Hàm `getAge`: lấy `Age`. */
    public String getAge() { return age; }
    /** Hàm `getAddress`: lấy `Address`. */
    public String getAddress() { return address; }
    /** Hàm `getBhytCode`: lấy `Bhyt` mã. */
    public String getBhytCode() { return bhytCode; }
    /** Hàm `getBhytRate`: lấy `Bhyt` `Rate`. */
    public String getBhytRate() { return bhytRate; }
    /** Hàm `getExpireDate`: lấy `Expire` ngày. */
    public String getExpireDate() { return expireDate; }
    /** Hàm `getAdmissionDate`: lấy `Admission` ngày. */
    public String getAdmissionDate() { return admissionDate; }
    /** Hàm `getDoctor`: lấy bacsi. */
    public String getDoctor() { return doctor; }
    /** Hàm `getDepartment`: lấy khoa. */
    public String getDepartment() { return department; }
    /** Hàm `getDiagnosis`: lấy chandoan. */
    public String getDiagnosis() { return diagnosis; }
    /** Hàm `getIcdCode`: lấy icd mã. */
    public String getIcdCode() { return icdCode; }

    /** Hàm `getShortCode`: lấy `Short` mã. */
    public String getShortCode() {
        if (code != null && code.contains(" - ")) {
            return code.split(" - ")[0];
        }
        return code;
    }
}
