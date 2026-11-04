# 00 — Hiện trạng và nguyên tắc phân tích

**Cập nhật:** 04/11/2026 (Phiên cập nhật sau Migration CSDL SQL Server & Nâng cấp Domain)  
**Mục đích:** Chốt điểm xuất phát chính xác theo thực tế mã nguồn và CSDL để điều phối kế hoạch triển khai không bị chệch hướng.

## Tóm tắt hiện trạng thực tế

- **Kiến trúc:** Dự án Maven multi-module Java 17 chuẩn Clean Architecture: `clinic-domain`, `clinic-application`, `clinic-infrastructure`, `clinic-desktop`.
- **Domain Layer (`clinic-domain`):**
  - Đã chuẩn hóa 17 subpackages, 129 files, 34 enums, bao quát toàn bộ thực thể y tế.
  - Vừa được bổ sung 3 Enums thực chiến: `KetQuaDieuTri`, `HuongXuTri`, `DoiTuongKham` cùng hơn 30 trường dữ liệu y tế chuyên sâu (`BenhNhan`, `BaoHiem`, `LuotKham`, `ChanDoan`, `ChiTietDonThuoc`, `ChiDinhDichVu`, `HoaDon`, `Item`).
  - Đầy đủ Repository Interfaces và Domain Services (`FefoAllocationService`, `ClinicalDrugSafetyService`,...).
- **Cơ sở dữ liệu & Migration (`clinic-infrastructure`):**
  - **ĐÃ THỰC THI THÀNH CÔNG 100% TRÊN MICROSOFT SQL SERVER (`clinicDb`):**
    + **49 bảng** dữ liệu quan hệ hoàn chỉnh.
    + **80 khóa ngoại (Foreign Keys)** ràng buộc toàn vẹn quan hệ cha-con.
    + Cơ chế Idempotent Tracking qua bảng `__schema_migrations`.
    + Dữ liệu ban đầu (Seed Data): Đầy đủ cơ sở, 5 khoa, 8 phòng khám, 2 tài khoản nhân viên (`admin`, `tiennv`), danh mục ICD-10, dịch vụ kỹ thuật, thuốc và tồn kho theo lô FEFO.
  - Có sẵn `DatabaseConnection` quản lý kết nối JDBC và `DatabaseMigrationRunner` tự động chạy script khi khởi động.
- **Desktop UI Layer (`clinic-desktop`):**
  - Đã có khung giao diện Swing phong phú: `LoginForm`, `ReceptionPanel`, `PatientRegistrationFormPanel`, `DoctorStationPanel`, `PharmacyDispensePanel`, `BillingInvoicePanel`, `ClinicReportPanel`, `SystemConfigPanel`.
  - **Module xuất tài liệu DOCX (`core/docx`):** Đã hoàn thiện 100% bộ engine OpenXML thuần Java (Zero-dependency) gồm `DocxDocument`, `DocxTable`, `DocxExportService`, `DocxManager`, sẵn sàng gắn nút xuất đơn thuốc, bệnh án, giấy hẹn.
  - *Khoảng cách hiện tại:* Các panel giao diện chủ yếu dùng mock data hoặc danh sách cố định, chưa nối vào use cases thực tế.
- **Application Layer (`clinic-application`):**
  - Hiện đang là thư mục trống trong `src/main/java`, cần khẩn trương xây dựng Command/Query Handlers, DTOs, và Transaction Boundaries.

## Mức độ theo lớp

| Lớp | Trạng thái thực tế | Việc cần làm tiếp theo |
|---|---|---|
| **Domain** | Đã nâng cấp toàn diện các trường thực chiến y tế và enums | Hoàn thiện unit test kiểm chứng Business Invariants (FEFO, Drug Safety, EMR Lock). |
| **CSDL (SQL Server)** | **Hoàn thành 100% Migration (49 bảng, 80 FKs, Seed Data)** | Bảo trì các script migration bổ sung (nếu có thay đổi schema trong tương lai). |
| **Infrastructure** | Có `DatabaseConnection` và `DatabaseMigrationRunner` đã chạy tốt | Xây dựng JDBC Repositories implementations, Unit of Work / Transaction Manager qua JDBC. |
| **Application** | Chưa có code | Xây dựng Use cases, DTOs, Validation, Authorization và điều phối giao dịch. |
| **Desktop** | Nhiều UI prototype + Đã xong core DOCX export engine | Thay thế Mock Data bằng `SwingWorker` gọi Application Services, hiển thị Loading/Error. |
| **Test/Release** | Test source hiện chỉ có `.gitkeep` | Viết Integration tests cho Repository và End-to-end slice test. |

## Nguyên tắc nối model với màn hình & Thiết kế kỹ thuật

1. **Nguyên tắc Clean Architecture:**
   - Mọi thao tác ghi (Write) và đọc (Read) từ Swing UI bắt buộc phải đi qua Application Service (`clinic-application`).
   - Tuyệt đối **KHÔNG** cho phép UI gọi trực tiếp JDBC Connection hoặc tự áp đặt quy tắc nghiệp vụ Domain.
2. **Quy tắc luồng xử lý UI (Swing Concurrency & EDT Safety):**
   - Mọi thao tác truy vấn CSDL hoặc chạy tác vụ nặng **bắt buộc phải chạy nền** thông qua `SwingWorker` hoặc `CompletableFuture`.
   - Tuyệt đối không block Event Dispatch Thread (EDT) làm đơ giao diện Swing. Mọi panel phải có trạng thái trực quan: Loading spinner/progress bar, Empty state, và Error notification.
3. **Quản lý Transaction (Unit of Work):**
   - Với các ca ghi đa bảng (ví dụ: Xuất kho = trừ `TonKho` + tạo `PhieuXuatKho` + ghi `InventoryTransaction`), Application layer quản lý Transaction Scope thông qua Connection gắn với `ThreadLocal` hoặc Connection context để đảm bảo tính nguyên tử (ACID).
4. **Phân định Aggregate Root vs Form con:**
   - Model con (`ChiTietDonThuoc`, `ChiTietHoaDon`, `ChiTietNhapKho`) nằm trực tiếp trong aggregate cha và form thao tác chính, không mở cửa sổ CRUD độc lập.
5. **Đồng bộ dữ liệu Form - Domain:**
   - Các trường mới bổ sung (ví dụ: 15 trường trên màn hình Đăng ký bệnh nhân) phải được map 1-1 chính xác vào DTO và Entity trước khi lưu xuống SQL Server.

## Nguồn đối chiếu trong workspace

- `clinic-domain/src/main/java/vn/clinicmanagement/domain/**`
- `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/**`
- `clinic-application/src/main/java`
- `clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/**`
- `clinic-infrastructure/src/main/resources/migrations/V1__Initial_Clinic_Schema.sql`
- `clinic-infrastructure/src/main/resources/migrations/V2__Seed_Initial_Data.sql`
- `UI_CODING_STANDARDS.md`, `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`, `docs/UI_DOMAIN_MAPPING_ANALYSIS.md`
