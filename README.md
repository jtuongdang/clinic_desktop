# Clinic Management Desktop

Ứng dụng desktop quản lý phòng khám xây dựng bằng Java 17, Maven multi-module và Swing.

## Cấu hình SQL Server cho môi trường phát triển

1. Tạo CSDL SQL Server và cấu hình `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, hoặc `DB_CONNECTION_STRING`.
2. Có thể sao chép `clinic-infrastructure/src/main/resources/db.properties.example` thành `db.properties` trong cùng thư mục để cấu hình local. File `db.properties` bị Git bỏ qua.
3. Áp dụng schema/seed bằng `vn.clinicmanagement.infrastructure.persistence.DatabaseMigrationRunner` trước khi chạy ứng dụng.
4. Build bằng Maven từ thư mục gốc: `mvn package`.

Migration seed tạo tài khoản demo `admin` và `tiennv`, mật khẩu ban đầu `Password123!`. Đây là thông tin công khai dành riêng cho môi trường demo; hãy đổi mật khẩu và dùng secret riêng trước khi kết nối dữ liệu thật.

## Tài liệu dự án

- `PROJECT_COMPLETENESS_REPORT.md`: đánh giá mức độ hoàn thiện.
- `docs/ke-hoach-trien-khai/README.md`: kế hoạch theo đợt và bản đồ phân hệ/model/màn hình.
- `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`, `UI_CODING_STANDARDS.md`: phân tích Domain, schema và quy chuẩn UI.
