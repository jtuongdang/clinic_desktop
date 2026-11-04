# 02 — Các đợt triển khai

Lộ trình được chia theo dependency để mỗi đợt có thể nghiệm thu thành một vertical slice.

---

## Đợt 0 — Nền tảng chạy, CSDL, Migration và Khung giao dịch

**Trạng thái:** `[~] Đang làm (Hoàn thành 70% - Đã xong CSDL, Migration & Core DOCX)`  
**Phụ thuộc:** Không.

### Việc đã hoàn thành:
- [x] **CSDL SQL Server:** Khởi tạo thành công 100% CSDL `clinicDb` trên Microsoft SQL Server: **49 bảng**, **80 khóa ngoại (Foreign Keys)**, Non-clustered Indexes.
- [x] **Migration Runner:** Xây dựng `DatabaseMigrationRunner` và các script `V1__Initial_Clinic_Schema.sql`, `V2__Seed_Initial_Data.sql`, cơ chế idempotent theo dõi qua `__schema_migrations`.
- [x] **Seed Data Chuẩn:** Nạp dữ liệu danh mục ban đầu (Cơ sở, 5 Khoa, 8 Phòng khám, tài khoản `admin` & `tiennv`, thuốc, ICD-10, dịch vụ).
- [x] **Nâng cấp Domain:** Đã bổ sung 3 Enums (`KetQuaDieuTri`, `HuongXuTri`, `DoiTuongKham`) và hơn 30 trường y tế chuyên sâu.
- [x] **Core Xuất Tài Liệu DOCX:** Đã xây dựng trọn vẹn bộ thư viện OpenXML thuần Java (`DocxDocument`, `DocxTable`, `DocxExportService`, `DocxManager`) không phụ thuộc Apache POI cồng kềnh.

### Việc cần hoàn tất để kết thúc Đợt 0:
- [ ] **Khung Application cơ sở (`clinic-application`):**
  - Tạo cấu trúc package: `commands/`, `queries/`, `dtos/`, `common/Result.java`.
- [ ] **Quản lý Giao dịch (Unit of Work):**
  - Xây dựng `TransactionManager` / `UnitOfWork` bọc `java.sql.Connection` quản lý `setAutoCommit(false)`, `commit()`, `rollback()` qua JDBC thuần.
- [ ] **Quy chuẩn Concurrency Desktop (SwingWorker):**
  - Xây dựng lớp trợ giúp `AsyncActionExecutor` hoặc Base SwingWorker xử lý: Bắt đầu ➔ Bật Loading Indicator ➔ Chạy ngầm ➔ Trả kết quả lên EDT ➔ Tắt Loading.
- [ ] **Xác thực Đăng nhập thật (`LoginForm`):**
  - Tạo `LoginCommand` và `NhanVienRepositoryImpl` xác thực tài khoản SQL Server bằng mã hóa BCrypt; chấm dứt tình trạng đăng nhập giả lập.

**Qua đợt khi:** Đăng nhập thành công bằng tài khoản `admin`/`tiennv` lưu trong SQL Server; kết nối và transaction JDBC hoạt động chuẩn.

---

## Đợt 1 — Định danh, danh mục nền và đăng ký bệnh nhân

**Trạng thái:** `[ ] Chưa làm`  
**Phụ thuộc:** Đợt 0.

**Model:** `NhanVien`, `VaiTro`, `BenhNhan`, `BaoHiem`, `BenhNhanCoSo`, `CoSo`, `Khoa`, `PhongKham`, `DanhMucDichVu`, `ICD`, `DoiTuongKham`.  
**UI:** `LoginForm`, `ReceptionPanel`, `PatientRegistrationFormPanel`, `CatalogPanel`.

### Chi tiết công việc:
- [ ] **Tầng Repository (`clinic-infrastructure`):**
  - `NhanVienRepositoryImpl`, `BenhNhanRepositoryImpl`, `BaoHiemRepositoryImpl`, `DanhMucDichVuRepositoryImpl`, `ICDRepositoryImpl`, `PhongKhamRepositoryImpl`.
- [ ] **Tầng Use Cases (`clinic-application`):**
  - `RegisterPatientCommand` & `UpdatePatientCommand`: Lưu trọn vẹn **15 trường thông tin y tế thực chiến** (Đối tượng, BHYT 5 năm, người liên hệ, địa chỉ 3 cấp, nghề nghiệp,...).
  - `SearchPatientsQuery`: Tra cứu tức thì theo CCCD / Số điện thoại / Mã BN / Họ tên; thuật toán cảnh báo trùng hồ sơ.
  - `GetActiveRoomsQuery` & `GetCatalogsQuery`: Nạp danh mục phòng khám và bảng giá dịch vụ vào Cache bộ nhớ khi app khởi động.
- [ ] **Tích hợp Desktop Swing (`clinic-desktop`):**
  - Nối `PatientRegistrationFormPanel` vào `RegisterPatientCommand` qua `SwingWorker`.
  - Thay thế danh sách phòng khám fix cứng bằng dữ liệu từ bảng `PhongKham` SQL Server.

**Qua đợt khi:** Đăng ký bệnh nhân mới từ form Desktop, khởi động lại ứng dụng dữ liệu vẫn hiển thị chính xác trên danh sách tìm kiếm.

---

## Đợt 2 — Tiếp đón, lịch hẹn và hàng đợi lượt khám

**Trạng thái:** `[ ] Chưa làm`  
**Phụ thuộc:** Đợt 1.

**Model:** `LichHen`, `LuotKham`, `SinhHieu`, `NguonLich`, `TrangThaiLuotKham`.  
**UI:** `ReceptionPanel`, `ClinicRoomStatusPanel`, dialog Lịch hẹn.

- [ ] `CreateEncounterCommand`: Tiếp nhận bệnh nhân (Walk-in hoặc từ Lịch hẹn), cấp số thứ tự khám an toàn (Thread-safe sequence).
- [ ] `RecordVitalSignsCommand`: Nhập mạch, nhiệt độ, huyết áp, nhịp thở, SpO2, chiều cao, cân nặng.
- [ ] Nối bảng hàng đợi phòng khám với cơ chế tự động làm mới (Periodic Refresh hoặc Trigger Event).
- [ ] Chuyển trạng thái lượt khám: `CHO_KHAM` ➔ `DANG_KHAM` ➔ `CHUYEN_PHONG` ➔ `HOAN_TAT`.

**Qua đợt khi:** Tạo lượt khám mới tại bàn tiếp đón, bệnh nhân xuất hiện ngay lập tức trong danh sách chờ của Bàn khám chuyên khoa tương ứng.

---

## Đợt 3 — Khám lâm sàng, EMR và kê đơn

**Trạng thái:** `[ ] Chưa làm`  
**Phụ thuộc:** Đợt 2.

**Model:** `LuotKham`, `ChanDoan`, `ICD`, `SinhHieu`, `TienSuBenh`, `DonThuoc`, `ChiTietDonThuoc`, `KetQuaDieuTri`, `HuongXuTri`.  
**UI:** `DoctorStationPanel`, `ExaminationPanel`, `MedicalRecordTabPanel`, `PrescriptionOrderPanel`.

- [ ] Nạp thông tin bệnh nhân đang được gọi khám lên giao diện Bác sĩ.
- [ ] `SaveDraftEmrCommand`: Lưu nháp diễn tiến bệnh, tiền sử, triệu chứng lâm sàng.
- [ ] Tìm kiếm và chọn mã chẩn đoán chính / chẩn đoán phụ từ bảng `ICD`.
- [ ] Kê đơn thuốc: Tìm kiếm thuốc từ kho thật, kiểm tra an toàn qua `ClinicalDrugSafetyService` (cảnh báo dị ứng, tương tác).
- [ ] `SignAndLockEmrCommand`: Ký số / Hoàn thành khám, cập nhật `KetQuaDieuTri`, `HuongXuTri`, khóa bệnh án chống sửa đổi trái phép.
- [ ] **Gắn nút xuất tài liệu DOCX:** Tích hợp `DocxExportService` cho phép in Đơn thuốc điện tử và Bệnh án ngoại trú ra file `.docx`.

**Qua đợt khi:** Bác sĩ khám xong, kê đơn, khóa bệnh án, xuất file đơn thuốc `.docx` chuẩn Bộ Y Tế và lưu thành công vào CSDL.

---

## Đợt 4 — Chỉ định dịch vụ, CLS và kết quả

**Trạng thái:** `[ ] Chưa làm`  
**Phụ thuộc:** Đợt 3 và danh mục dịch vụ của Đợt 1.

**Model:** `DanhMucDichVu`, `ChiDinhDichVu`, `ChiTietKetQuaXetNghiem`, `DinhMucVatTuDichVu`.  
**UI:** `ServiceOrderPanel`, `LabOrderTabPanel`, `ParaclinicalOrderPanel`.

- [ ] Bác sĩ chỉ định các dịch vụ kỹ thuật (Xét nghiệm máu, X-Quang, Siêu âm).
- [ ] Màn hình phòng xét nghiệm/CĐHA tiếp nhận chỉ định, cập nhật trạng thái `DANG_THUC_HIEN`.
- [ ] Nhập trị số xét nghiệm, kết luận chẩn đoán hình ảnh và đính kèm đường dẫn ảnh chụp.
- [ ] Duyệt kết quả và đẩy ngược trở lại màn hình Bác sĩ điều trị.

**Qua đợt khi:** Bác sĩ nhận được thông báo có kết quả CLS và xem được kết quả xét nghiệm ngay trên hồ sơ khám bệnh.

---

## Đợt 5 — Kho Dược, cấp phát thuốc (FEFO) và quản lý tồn kho

**Trạng thái:** `[ ] Chưa làm`  
**Phụ thuộc:** Đợt 3, danh mục thuốc/vật tư và Unit of Work của Đợt 0.

**Model:** `Item`, `Kho`, `TonKho`, `PhieuNhapKho`, `ChiTietNhapKho`, `PhieuXuatKho`, `ChiTietXuatKho`, `InventoryTransaction`, `FefoAllocationService`.  
**UI:** `PharmacyDispensePanel`, `PharmacyTicketTablePanel`.

- [ ] Nhà thuốc tiếp nhận đơn thuốc đã được bác sĩ duyệt hoặc hóa đơn đã thanh toán.
- [ ] Thực thi thuật toán **FEFO** tự động trừ số lượng từ các lô có hạn dùng gần nhất trước.
- [ ] Quản lý giao dịch nguyên tử (Atomic): Cập nhật `TonKho` + Tạo `PhieuXuatKho` + Ghi `InventoryTransaction` trong cùng một Transaction JDBC.
- [ ] Cảnh báo thuốc hết hạn hoặc tồn kho dưới định mức an toàn.

**Qua đợt khi:** Cấp phát đơn thuốc thành công, tồn kho trong bảng `TonKho` bị trừ chính xác theo từng lô và không xảy ra bán âm kho.

---

## Đợt 6 — Viện phí, thu ngân, hóa đơn và công nợ

**Trạng thái:** `[ ] Chưa làm`  
**Phụ thuộc:** Đợt 3, 4, 5.

**Model:** `HoaDon`, `ChiTietHoaDon`, `PhieuThu`, `CongNoBenhNhan`.  
**UI:** `BillingInvoicePanel`, `MedicalBillingSlipPanel`.

- [ ] Tự động gom toàn bộ chi phí (Tiền khám + Tiền CLS + Tiền thuốc) thành Hóa đơn viện phí `HoaDon`.
- [ ] Tính toán tự động quyền lợi BHYT (ví dụ: đúng tuyến 80%, 100%) và số tiền bệnh nhân cùng chi trả.
- [ ] Thu ngân xác nhận thu tiền, ghi nhận `PhieuThu`, in hóa đơn viện phí.
- [ ] Quản lý số dư tạm ứng và ghi nhận nợ vào `CongNoBenhNhan`.

**Qua đợt khi:** Thu ngân in được phiếu thu chuẩn, doanh thu và công nợ đối soát khớp 100% với các dịch vụ đã thực hiện.

---

## Đợt 7 — Quản trị nhân sự, payroll, báo cáo tổng hợp và đóng gói

**Trạng thái:** `[ ] Chưa làm`  
**Phụ thuộc:** Đợt 0–6.

**Model:** `NhanVien`, `ChamCong`, `KyLuong`, `BangLuong`, `NhatKyHeThong`.  
**UI:** `SystemConfigPanel`, `ClinicReportPanel`, phân hệ chấm công.

- [ ] Chấm công nhân sự theo ca làm việc, tính toán bảng lương tự động từ ngày công và hoa hồng dịch vụ.
- [ ] Báo cáo hoạt động khám chữa bệnh: Lượt khám, cơ cấu bệnh tật ICD-10.
- [ ] Báo cáo tài chính doanh thu, viện phí, BHYT.
- [ ] Báo cáo kho dược: Báo cáo Nhập - Xuất - Tồn, thẻ kho chi tiết.
- [ ] Đóng gói cài đặt ứng dụng Desktop (.jar / native installer) kèm hướng dẫn vận hành.
