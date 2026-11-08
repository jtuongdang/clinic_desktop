# HỢP ĐỒNG TRIỂN KHAI TÍCH HỢP GIAO DIỆN VỚI BUSINESS LOGIC & CƠ SỞ DỮ LIỆU
**DỰ ÁN HỆ THỐNG Y TẾ THÔNG MINH EHC (CLINIC MANAGEMENT SYSTEM)**
**Phiên bản:** 1.0.0 | **Ngày lập:** 08/11/2026 | **Nhánh:** `feature/patient-reception-workflow`

---

## 1. MỤC TIÊU & BỐI CẢNH
Hệ thống giao diện người dùng Swing Desktop (`clinic-desktop`) đã hoàn thiện 100% thiết kế giao diện chuẩn EHC (Pixel-perfect). Tầng ứng dụng (`clinic-application`) và hạ tầng (`clinic-infrastructure`) đã triển khai đầy đủ các Handlers, Repositories, Domain Entities và CSDL `clinicDb`.

Hợp đồng này quy định chi tiết cam kết, lộ trình, quy chuẩn kỹ thuật và tiêu chí nghiệm thu để **xóa bỏ toàn bộ các điểm mô phỏng (Mock/Stub/Fake Action)**, đấu nối trực tiếp 100% các màn hình còn dang dở vào Tầng nghiệp vụ và CSDL thực tế.

---

## 2. PHẠM VI HỢP ĐỒNG (SCOPE OF CONTRACT)

Hợp đồng bao gồm 4 gói tích hợp chính tương ứng với các phân hệ chưa hoàn tất đấu nối:

| Gói | Phân hệ | Màn hình trọng tâm | Tầng nghiệp vụ (Handlers có sẵn) | Trạng thái cam kết |
| :---: | :--- | :--- | :--- | :---: |
| **GÓI 1** | **Khám Bệnh & Bác Sĩ** (`examination` & `doctor`) | • `DoctorStationPanel` / `DoctorActionBar`<br>• `TreatmentResolutionTabPanel`<br>• `PrescriptionOrderPanel` / `PrescriptionOrderTabPanel`<br>• `ServiceOrderPanel` / `LabOrderTabPanel`<br>• `MedicalRecordTabPanel`<br>• `TransferRoomSelectDialog` | • `StartExamHandler`<br>• `AddDiagnosisHandler`<br>• `CompleteExamHandler`<br>• `PrescribeMedicationHandler`<br>• `OrderServiceHandler`<br>• `UpdateEmrClinicalRecordHandler`<br>• `TransferVisitHandler` | **Ưu tiên 1 (Core)** |
| **GÓI 2** | **Nhà Thuốc & Kho Dược** (`pharmacy`) | • `PharmacyDispensePanel`<br>• `PharmacyCategoryTreePanel`<br>• `PharmacyTicketTablePanel` | • `DispensePrescriptionHandler`<br>• `ReceiveWarehouseStockHandler`<br>• `GetStockAvailabilityHandler` | **Ưu tiên 2** |
| **GÓI 3** | **Cận Lâm Sàng & CĐHA** (`paraclinical`) | • `ParaclinicalOrderPanel`<br>• Form nhập kết quả xét nghiệm | • `RecordServiceResultHandler`<br>• `GetServicesByVisitHandler`<br>• `CancelServiceOrderHandler` | **Ưu tiên 2** |
| **GÓI 4** | **Danh Mục & Hệ Thống** (`catalog` & `system`) | • `CatalogPanel` (CRUD)<br>• `SystemConfigPanel` | • Quản lý danh mục ICD, dịch vụ, thuốc<br>• `SystemRepository` đọc/ghi cấu hình | **Ưu tiên 3** |

---

## 3. CƠ CẤU TÀI LIỆU HỢP ĐỒNG
Thư mục hợp đồng bao gồm các tài liệu đặc tả thành phần:
- [01-hop-dong-kham-chua-benh-examination.md](file:///Users/jtuong/Documents/desktop_clinic/desktop/docs/hop-dong-tich-hop-giao-dien/01-hop-dong-kham-chua-benh-examination.md): Đặc tả hợp đồng phân hệ Khám Bệnh, Kê Đơn, Chỉ Định CLS, Bệnh Án.
- [02-hop-dong-duoc-kho-pharmacy.md](file:///Users/jtuong/Documents/desktop_clinic/desktop/docs/hop-dong-tich-hop-giao-dien/02-hop-dong-duoc-kho-pharmacy.md): Đặc tả hợp đồng phân hệ Kho Dược, Cấp Phát Thuốc, Quản Lý Tồn Kho.
- [03-hop-dong-can-lam-sang-paraclinical.md](file:///Users/jtuong/Documents/desktop_clinic/desktop/docs/hop-dong-tich-hop-giao-dien/03-hop-dong-can-lam-sang-paraclinical.md): Đặc tả hợp đồng phân hệ Cận Lâm Sàng, Nhập Kết Quả Xét Nghiệm/CĐHA.
- [04-hop-dong-danh-muc-he-thong-catalog-system.md](file:///Users/jtuong/Documents/desktop_clinic/desktop/docs/hop-dong-tich-hop-giao-dien/04-hop-dong-danh-muc-he-thong-catalog-system.md): Đặc tả hợp đồng Danh Mục Dùng Chung và Quản Trị Cấu Hình Hệ Thống.
- [05-tieu-chuan-ky-thuat-va-nghiem-thu.md](file:///Users/jtuong/Documents/desktop_clinic/desktop/docs/hop-dong-tich-hop-giao-dien/05-tieu-chuan-ky-thuat-va-nghiem-thu.md): Quy chuẩn kỹ thuật (Clean Architecture, SwingWorker, Transaction, Audit) và Biên bản nghiệm thu DoD.

---

## 4. NGUYÊN TẮC BẤT DI BẤT DỊCH (NON-NEGOTIABLE PRINCIPLES)
1. **Tuyệt đối không chạy I/O / JDBC trên Event Dispatch Thread (EDT):** Mọi thao tác ghi/đọc CSDL phải chạy qua `SwingWorker` trong nền kèm theo quản lý vòng đời view (`cancelInFlightWorkers` và `viewSessionEpoch`).
2. **Tuân thủ phân quyền và kiểm toán:** Tất cả các thao tác nghiệp vụ phải thông qua `AuthorizationContext` và ghi `AuditActorContext` đầy đủ.
3. **Bảo tồn toàn vẹn giao diện (Pixel-Perfect UI Preservation):** Không làm vỡ layout, màu sắc, font chữ và các thành phần giao diện đã nghiệm thu.
4. **Không làm gãy regression tests:** Toàn bộ 174 bài test hiện tại trong bộ kiểm thử Maven phải tiếp tục `BUILD SUCCESS`.
