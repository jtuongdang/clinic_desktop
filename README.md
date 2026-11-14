# Clinic Management Desktop

Ứng dụng desktop quản lý phòng khám xây dựng bằng Java 17, Maven multi-module và Swing.

## Cấu hình SQL Server cho môi trường phát triển

1. Tạo CSDL SQL Server và cấu hình `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, hoặc `DB_CONNECTION_STRING` trong môi trường local. Không commit mật khẩu hay connection string.
2. Khi chạy từ IntelliJ, có thể sao chép `clinic-infrastructure/src/main/resources/db.properties.example` thành `db.properties` trong cùng thư mục. File thật `db.properties` bị Git bỏ qua.
3. Build artifact desktop từ thư mục gốc: `mvn -pl clinic-desktop -am package`.
4. Chạy migration/seed trước khi mở ứng dụng:

   ```bash
   java -cp clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar \
     vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuDiCuChay
   ```

5. Mở desktop:

   ```bash
   java -jar clinic-desktop/target/clinic-desktop-1.0.0-SNAPSHOT.jar
   ```

Artifact shaded loại `db.properties` khỏi JAR để tránh đóng gói cấu hình bí mật; `db.properties.example` chỉ là mẫu. Vì vậy, khi chạy JAR cần cung cấp cấu hình bằng biến môi trường. Ví dụ trong zsh (thay placeholder bằng giá trị môi trường local của bạn):

```bash
export DB_URL='jdbc:sqlserver://<host>:1433;databaseName=<database>'
export DB_USERNAME='<username>'
export DB_PASSWORD='<password>'
```

Có thể dùng `DB_CONNECTION_STRING` thay cho ba biến `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`. Khi chạy bằng IntelliJ, cấu hình biến môi trường trong Run Configuration hoặc dùng file local đã nêu ở bước 2.

Migration seed tạo tài khoản demo `admin` và `tiennv`, mật khẩu ban đầu `Password123!`. Đây là thông tin công khai dành riêng cho môi trường demo; hãy đổi mật khẩu và dùng secret riêng trước khi kết nối dữ liệu thật.

## Tài liệu dự án

- `PROJECT_COMPLETENESS_REPORT.md`: đánh giá mức độ hoàn thiện.
- `docs/ke-hoach-trien-khai/README.md`: kế hoạch theo đợt và bản đồ phân hệ/model/màn hình.
- `docs/ke-hoach-trien-khai/03-model-task-board.md`: task board theo model, dependency, trạng thái và bằng chứng.
- `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`, `UI_CODING_STANDARDS.md`: phân tích Domain, schema và quy chuẩn UI.
