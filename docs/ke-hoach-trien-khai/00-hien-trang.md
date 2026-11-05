# 00 — Hiện trạng và nguyên tắc phân tích

**Cập nhật:** 05/11/2026
**Phạm vi:** Clinic Management Desktop — Java, Maven, JDBC, SQL Server, Java Swing.

> Trạng thái dưới đây phản ánh mã nguồn trong checkout tại ngày cập nhật. Một phần luồng Tiếp đón đang là thay đổi chưa commit; “có trong checkout” không đồng nghĩa đã được build, kiểm thử tích hợp hoặc nghiệm thu.

## Hiện trạng theo lớp

| Lớp | Bằng chứng trong source | Đánh giá hiện tại |
|---|---|---|
| `clinic-domain` | Các model chia theo `patient`, `identity`, `facility`, `appointment`, `examination`, `prescription`, `inventory`, `billing`, `service`, `system` và các miền khác. | Nền model rộng; từng invariant và nghiệp vụ chưa thể xem là đã hoàn thiện chỉ vì có entity. |
| `clinic-application` | Có `Result`, `Command`/`Query`, handlers xác thực và Tiếp đón, cùng `TransactionBoundary`. | Đã có use case cho đăng nhập và đăng ký/tìm kiếm bệnh nhân, hàng đợi; cần test và mở rộng cho các luồng còn lại. |
| `clinic-infrastructure` | Có JDBC connection, migration runner, `JdbcTransactionManager`, `JdbcUnitOfWork`, repository nhân viên, bệnh nhân, bảo hiểm, phòng khám và hàng đợi. | Có vertical slice persistence cho xác thực/Tiếp đón; migration/schema thực tế phải được xác minh trên DB thử nghiệm khi thay đổi. |
| `clinic-desktop` | Có Swing UI cho auth, dashboard, reception, doctor/examination, paraclinical, pharmacy, billing, catalog, reports và system. | Độ hoàn thiện khác nhau theo phân hệ. UI prototype hoặc dữ liệu mẫu chưa chứng minh luồng nghiệp vụ end-to-end. |
| Kiểm thử | Các `src/test` hiện chỉ có `.gitkeep`, chưa có test tự động của Clinic modules trong checkout. | Cần bổ sung test và chạy `mvn clean verify`; chưa xác nhận build trong lần rà soát này. |

## Luồng đã có trong checkout

- **Xác thực:** `AuthenticationCompositionRoot` nối `LoginForm` tới application handler và `NhanVienRepositoryImpl`.
- **Tiếp đón:** `ReceptionCompositionRoot` nối form với use cases đăng ký/tìm bệnh nhân và hàng đợi. Đăng ký tạo/cập nhật bệnh nhân, thông tin bảo hiểm và lượt khám trong một transaction theo code hiện có.
- Các thay đổi Tiếp đón trên đang chưa commit. Cần kiểm thử, review và đồng bộ tài liệu trước khi coi Đợt 1 đã nghiệm thu hoặc giao Agent từ branch/commit khác.

## Nguyên tắc đánh giá hoàn thiện

1. Đánh giá theo luồng **màn hình → application use case → domain rule → repository/JDBC → CSDL → đọc lại/hiển thị**, không dựa riêng vào sự tồn tại của model hoặc panel.
2. Chỉ đánh dấu hoàn tất khi có tiêu chí nghiệp vụ, xử lý lỗi, kiểm thử phù hợp và bằng chứng chạy được. Ghi riêng trạng thái code local, commit, test và nghiệm thu.
3. UI không gọi JDBC trực tiếp. I/O và tác vụ dài chạy ngoài Swing EDT; cập nhật UI trên EDT.
4. Các thao tác ghi nhiều bảng phải dùng transaction, có rollback và test tính nguyên tử.
5. Dùng dữ liệu tổng hợp trong kiểm thử; không đưa dữ liệu bệnh nhân thật vào fixture, log hoặc báo cáo phát triển.
6. Quy tắc BHYT, thanh toán, thuốc, quyền truy cập và biểu mẫu pháp quy phải được xác nhận với người phụ trách nghiệp vụ trước khi tuyên bố tuân thủ.

## Nguồn đối chiếu

- `clinic-domain/src/main/java/vn/clinicmanagement/domain/`
- `clinic-application/src/main/java/vn/clinicmanagement/application/`
- `clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/`
- `clinic-infrastructure/src/main/resources/migrations/`
- `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/`
- `README.md`, `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`, `UI_CODING_STANDARDS.md`
