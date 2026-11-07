# BIÊN BẢN BÀN GIAO KỸ THUẬT & HƯỚNG DẪN KIỂM THỬ HỆ THỐNG DÀNH CHO OWNER
> **Hệ thống:** Phần Mềm Quản Lý Bệnh Viện EHC HIS (Clinic Management Desktop - Swing & Clean Architecture)  
> **Thời điểm xác lập:** 20:55 ngày 07/11/2026 (Giờ Việt Nam - Asia/Ho_Chi_Minh)  
> **Người lập bàn giao:** **`leaderANTI`** (Autonomous Tech Lead & Background Controller)  
> **Người nhận bàn giao:** **Dự án Owner**  
> **Mục đích:** Bàn giao toàn bộ kết quả phát triển từ Phase 1 đến Phase 9, gói thực thi hoàn chỉnh và quy trình hướng dẫn kiểm thử thực nghiệm (Visual Smoke Test) để Owner trực tiếp trải nghiệm, đánh giá và chỉ đạo bước đi tiếp theo.

---

## 1. Hiện Trạng Kỹ Thuật & Chỉ Số Đóng Gói

| Hạng mục kỹ thuật | Giá trị xác thực | Đánh giá an toàn & Ghi chú |
|---|---|---|
| **Nhánh Git làm việc** | `feature/patient-reception-workflow` | Toàn bộ commits đã được tích hợp sạch sẽ trên nhánh chính |
| **Commit HEAD** | `f021bd6` | *"[leaderANTI] docs: record DOC-01 and DOC-02 dynamic print wiring completion with 1131 passing tests"* |
| **Trạng thái Build Reactor** | **BUILD SUCCESS** across 5 modules | Thời gian build nhanh: ~10.4 giây trên JDK 17 |
| **Tổng số bài test tự động** | **1,131 tests run (100% PASS)** | `0 failures`, `0 errors`, `6 expected opt-in skips` |
| **Phân bổ bài test theo module** | • `clinic-domain`: **235 tests**<br>• `clinic-application`: **593 tests**<br>• `clinic-infrastructure`: **129 tests** (4 opt-in skips)<br>• `clinic-desktop`: **174 tests** (2 opt-in skips) | Đạt độ phủ test cao nhất từ trước đến nay của toàn dự án |
| **Gói thực thi (Fat Uber JAR)** | `clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar` | **Tuyệt đối an toàn:** Đã xác minh 0 file `db.properties` hoặc credentials rò rỉ trong JAR |
| **Cơ sở dữ liệu Production** | `localhost:1433` (`clinicDb` trên Microsoft SQL Server) | **Bảo toàn 100%:** Tuân thủ Hợp đồng Cách ly `18847587`, toàn bộ test chạy in-memory mocks, CSDL thật sẵn sàng cho Owner test |
| **Flyway Migrations** | `V1` đến `V11` | Đầy đủ cấu trúc bảng, vai trò, phân quyền, tham số hệ thống và phòng chống brute-force |

---

## 2. Hướng Dẫn Khởi Động Ứng Dụng Dành Cho Owner

Owner có thể khởi chạy ứng dụng Swing Desktop trực quan bằng 2 phương thức thuận tiện:

### Cách 1: Sử dụng Shell Script (Khuyên dùng — 1 thao tác)
Mở Terminal tại thư mục gốc của dự án desktop và chạy:
```bash
cd /Users/jtuong/Documents/desktop_clinic/desktop
./run-app.sh
```
*Script đã được cấp quyền thực thi `chmod +x` và tự động nhận diện gói Fat JAR `clinic-desktop-1.0.0-SNAPSHOT.jar` với đầy đủ Look and Feel FlatLaf và kết nối CSDL.*

### Cách 2: Khởi chạy trực tiếp gói Fat JAR qua Java
```bash
cd /Users/jtuong/Documents/desktop_clinic/desktop
java -jar clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar
```

---

## 3. Thông Tin Tài Khoản Đăng Nhập Mẫu (Seed Data)

Các tài khoản dưới đây đã được mã hóa chuẩn PBKDF2-HMAC-SHA256 trong CSDL qua migration `V2` và `V3`:

| Tài khoản (Username) | Mật khẩu (Password) | Vai trò (Role) | Chức năng chính trên giao diện |
|---|---|---|---|
| **`admin`** | `Password123!` | `ADMIN` (Quản trị viên) | Toàn quyền truy cập mọi phân hệ: Tiếp đón, Bàn khám, Kho dược, Viện phí, Quản trị nhân sự, Báo cáo thống kê, Cấu hình hệ thống. |
| **`tiennv`** | `Password123!` | `DOCTOR` (Bác sĩ điều trị) | Truy cập Bàn khám Bác sĩ (`DoctorStationPanel`), khám bệnh, kê đơn thuốc, chỉ định cận lâm sàng, xem lịch sử bệnh án. |

*(Lưu ý: Hệ thống được trang bị cơ chế chống dò mật khẩu `V11__Add_Account_Login_Throttle`. Nếu đăng nhập sai quá số lần quy định, tài khoản sẽ tự động bị khóa tạm thời theo chính sách bảo mật).*

---

## 4. Kịch Bản Kiểm Thử Trực Quan (Visual Smoke Test Guide)

Khi màn hình chính hiển thị, Owner có thể tiến hành kiểm thử các luồng nghiệp vụ thực tế theo từng phân hệ:

### Phân hệ 1: Tiếp Đón & Đăng Ký Khám Ngoại Trú (`ReceptionPanel`)
1. **Tìm kiếm bệnh nhân:** Nhập số CCCD hoặc Số điện thoại vào ô tìm kiếm -> Nhấn phím `Enter` hoặc click nút **Tìm kiếm**.
2. **Tiếp nhận bệnh nhân mới:**
   - Điền thông tin Họ tên, Ngày sinh/Tuổi, Giới tính, Địa chỉ, Số BHYT.
   - Chọn buồng khám/phòng khám (ví dụ: `PK Nhi 1`, `PK Nhi 2`...).
   - Nhấn **F1** (hoặc nút **Lưu / Tiếp nhận**) -> Hệ thống tự động tạo hồ sơ bệnh nhân, cấp mã lượt khám và đẩy bệnh nhân vào hàng đợi khám của buồng tương ứng.
3. **Phím tắt nhanh:** Nhấn **F2** để làm mới form (`Reset`).

### Phân hệ 2: Bàn Khám Bác Sĩ & Điều Trị Lâm Sàng (`DoctorStationPanel`)
1. **Danh sách hàng đợi khám:** Kiểm tra danh sách bệnh nhân chờ khám hiển thị từ CSDL thực tế (đã triệt tiêu vĩnh viễn dữ liệu tĩnh `TEST BN 060`/`033`).
2. **Chọn bệnh nhân khám:** Click đúp vào bệnh nhân trong danh sách để nạp thông tin lượt khám vào các tab lâm sàng:
   - **Tab Sinh hiệu & Thể trạng:** Xem/nhập Mạch, Nhiệt độ, Huyết áp, SpO2, Cân nặng, Chiều cao.
   - **Tab Bệnh sử & Triệu chứng:** Xem tiền sử bệnh, nhập lý do khám, triệu chứng lâm sàng.
   - **Tab Chẩn đoán:** Chọn mã bệnh chính/phụ theo danh mục ICD-10 (ví dụ: `J00`, `J02`, `K29`...).
   - **Tab Chỉ định Cận lâm sàng (CLS):** Thêm chỉ định dịch vụ kỹ thuật (Xét nghiệm máu CTM, Chụp X-quang, Siêu âm...).
   - **Tab Kê đơn thuốc:** Chọn thuốc từ kho dược, nhập số lượng, đường dùng, liều lượng. Hệ thống có cơ chế kiểm tra an toàn tương tác thuốc và dị ứng.

### Phân hệ 3: Biểu Mẫu & Xem Trước Bản In (Print Preview & Export)
Owner kiểm tra các hộp thoại in ấn biểu mẫu y tế tại menu in của Bàn khám hoặc phím chức năng:
1. **Đơn thuốc (`PrescriptionPrintPreviewDialog`):**
   - Đã được đấu nối dữ liệu động thực tế theo lượt khám.
   - Không còn xuất hiện chuỗi giả lập tĩnh (`"TEST1"`, `"22000324"`, `"tiennv"`).
   - Nếu chưa có đơn thuốc, giao diện hiển thị thông báo rỗng an toàn *"Chưa có dữ liệu đơn thuốc."*
2. **Bệnh án Ngoại trú (`MedicalRecordPrintPreviewDialog`):**
   - Nạp thông tin bệnh nhân, quá trình bệnh lý, kết luận chẩn đoán thực tế.
3. **Phiếu chỉ định Cận lâm sàng (`ClinicalOrderPrintPreviewDialog`):**
   - Hiển thị danh sách dịch vụ kỹ thuật bác sĩ đã chỉ định, bác sĩ chỉ định và phòng thực hiện.
4. **Bảng kê Chi phí / Viện phí (`MedicalBillingSlipDialog`):**
   - Hiển thị bảng chi tiết các khoản chi phí khám, xét nghiệm, thuốc, tiền BHYT chi trả và người bệnh cùng chi trả.
5. **Xuất file tài liệu Word (`DocxExportService`):**
   - Click nút **Xuất Word / Docx** -> Tài liệu được tạo sạch sẽ dưới định dạng `.docx` chuẩn OpenXML.

### Phân hệ 4: Thu Ngân & Viện Phí (`BillingInvoicePanel`)
1. Chọn lượt khám cần thanh toán.
2. Kiểm tra tổng chi phí điều trị phát sinh từ khám bệnh, thuốc và cận lâm sàng.
3. Áp dụng khấu trừ BHYT và tạm ứng (nếu có).
4. Nhấn **Thu tiền / Phát hành hóa đơn** -> Ghi nhận thanh toán và cập nhật công nợ bệnh nhân.

### Phân hệ 5: Báo Cáo & Thống Kê Quản Trị (`ClinicReportPanel`)
1. Chọn khoảng thời gian từ ngày đến ngày.
2. Chọn loại báo cáo trong ComboBox:
   - Báo cáo Doanh thu Viện phí
   - Thống kê Lượt khám & Bệnh nhân
   - Báo cáo Xuất nhập tồn Kho dược
3. Nhấn **Xem báo cáo** -> Dữ liệu được tính toán động từ backend handlers, loại bỏ hoàn toàn các con số tĩnh demo (`"1,240"`, `"248,000,000"`).

---

## 5. Tình Trạng Các Cổng Kiểm Soát An Ninh (Security Exit Gates)

Theo đúng quy chuẩn phân quyền và an toàn dữ liệu, các cổng an ninh đang được giữ ở trạng thái:

- **`SEC-01` (Security Exit Gate):** Duy trì **`[~] In progress` (`default-deny`)**. Cổng sẽ chính thức đóng (`Closed / Accepted`) sau khi Owner hoàn tất visual smoke test xác thực đăng nhập và điều hướng vai trò.
- **`PAT-02` (Patient Data Safety Gate):** Duy trì **`[~] In progress`**. Xác nhận dữ liệu cá nhân bệnh nhân không bị rò rỉ trong log hay ngoại lệ.
- **`REC-01` (Reception Workflow Gate):** Duy trì **`[~] In progress`**. Xác nhận luồng tiếp đón hoạt động chuẩn xác trên môi trường thật của Owner.

---

## 6. Trạng Thái Hoạt Động Của Đội Ngũ AI & Cam Kết

1. **Chuyển sang chế độ STANDBY / MONITOR:**
   - Hệ thống AI thợ code (`copilot-cli`, `antigravity-cli`, `codex-local`) và `leaderANTI` tạm dừng việc tạo thêm commits mới trên nhánh `feature/patient-reception-workflow` trong thời gian Owner tiến hành test hệ thống.
   - Không tự ý can thiệp hay sửa đổi CSDL `clinicDb`.
2. **Sẵn sàng tiếp nhận phản hồi:**
   - Khi Owner hoàn thành đợt kiểm thử thực tế và có các ghi chú chỉnh sửa, tinh chỉnh giao diện hoặc mở thêm hợp đồng nghiệp vụ tiếp theo, **`leaderANTI`** sẽ lập tức kích hoạt lại toàn bộ đội ngũ thợ code để triển khai tức thì.

---
*Biên bản được lập và lưu vết tại `docs/ke-hoach-trien-khai/BIEN_BAN_BAN_GIAO_TEST_HE_THONG_LEADER_ANTI.md`.*  
**Trân trọng kính báo và bàn giao hệ thống cho Dự án Owner!**  
**Tech Lead ký tên:** `leaderANTI`
