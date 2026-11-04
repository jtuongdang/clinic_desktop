# Báo cáo đánh giá mức độ hoàn thiện project

**Project:** Clinic Management Desktop (Java Swing / Maven)  
**Ngày rà soát:** 04/11/2026  
**Phạm vi:** Mã nguồn, cấu trúc module, cấu hình, migration, tài liệu và các điểm nối giữa UI–nghiệp vụ–CSDL. Đây là đánh giá tĩnh; trong lượt rà soát này không chạy build hoặc test.

## 1. Kết luận điều hành

Project đang ở mức **prototype giao diện có mô hình nghiệp vụ và schema tương đối rộng**, chưa đạt mức ứng dụng quản lý phòng khám có thể vận hành với dữ liệu thật.

| Khía cạnh | Đánh giá | Nhận xét |
|---|---:|---|
| Phạm vi UI/UX | Khá | Có nhiều màn hình và thành phần cho các luồng tiếp đón, khám, dược, viện phí, CLS, quản trị. |
| Mô hình Domain | Khá | Có nhiều entity, enum, exception và một số domain service chuyên biệt. |
| CSDL và migration | Trung bình | Có migration SQL Server, seed data và `DatabaseMigrationRunner`; chưa có persistence repository để ứng dụng đọc ghi domain model. |
| Nghiệp vụ Application | Thấp | Đã có khung Result/Command/Query và use case xác thực; các use case nghiệp vụ khác chưa có. |
| Truy cập dữ liệu | Thấp | Có transaction manager/unit of work và repository nhân viên; các repository còn lại chưa triển khai. |
| Xác thực và phân quyền | Đang hoàn thiện | Login kiểm tra tài khoản seed và role từ DB; chưa có quản lý tài khoản/mật khẩu và kiểm tra quyền toàn hệ thống. |
| Kiểm thử tự động | Chưa có | Bốn thư mục `src/test` chỉ chứa `.gitkeep`. |
| Sẵn sàng production | Chưa đạt | Dữ liệu giả, thông tin đăng nhập mẫu, thiếu nối nghiệp vụ–CSDL và kiểm chứng vận hành. |

**Ước lượng tổng thể:** khoảng **35% hoàn thiện nếu mục tiêu là bản demo UI**, và **15–20% nếu mục tiêu là hệ thống nghiệp vụ có thể triển khai thực tế**. Đây là ước lượng định tính dựa trên phạm vi code hiện có, không phải phép đo tiến độ chính thức.

## 2. Quy mô và cấu trúc hiện có

- Maven multi-module gồm `clinic-domain`, `clinic-application`, `clinic-infrastructure`, `clinic-desktop`, chạy trên Java 17.
- Rà soát đếm được khoảng **137 file Java trong Domain** và **82 file Java trong Desktop**.
- Domain đã chia các miền bệnh nhân, khám bệnh, dịch vụ, đơn thuốc, kho, viện phí, nhân sự, payroll, cơ sở và hệ thống; có repository interfaces, event contracts, domain exceptions và một số service nghiệp vụ.
- UI có 12 nhóm module chính; riêng `examination` có nhiều panel, tab, dialog và print preview.
- Migration `V1__Initial_Clinic_Schema.sql` khai báo schema rộng (43 nhóm bảng/khối theo chú thích), `V2__Seed_Initial_Data.sql` tạo dữ liệu mẫu.
- Có tài liệu phân tích Domain, ERD, chuẩn UI và mapping UI–Domain. Tuy nhiên, một số kết luận trong tài liệu mô tả trạng thái mục tiêu như thể đã triển khai; cần đọc đối chiếu với mã thực tế.

## 3. Điểm đã làm tốt

1. **Phạm vi nghiệp vụ được phác thảo rộng:** cấu trúc Domain và schema bao phủ nhiều khái niệm thường gặp như bệnh nhân, BHYT, lượt khám, chỉ định, kho theo lô, hóa đơn, nhân sự và quyền.
2. **Tổ chức giao diện có chủ đích:** dùng Maven module, chia panel theo phân hệ, có khung dashboard/MDI, component tái sử dụng và nhiều form chuyên biệt.
3. **Có một số quy tắc miền cụ thể:** ví dụ `FefoAllocationService`, `ClinicalDrugSafetyService`, exception tồn kho và khóa EMR. Cần thêm kiểm thử và tích hợp các quy tắc này vào use case thực tế.
4. **Có migration, seed data và migration runner:** `DatabaseMigrationRunner` theo dõi các script V1/V2 và áp dụng script chưa chạy. Cần bổ sung kiểm chứng trên DB sạch/DB đã có dữ liệu, chính sách lỗi/khôi phục và quản lý version khi thêm migration.

## 4. Khoảng trống và rủi ro chính

### P0 — Chưa an toàn để dùng với dữ liệu hoặc người dùng thật

1. **Tài khoản seed dùng mật khẩu mặc định.** `admin` và `tiennv` xác thực bằng PBKDF2 sau khi áp dụng V2/V3; mật khẩu demo ban đầu là `Password123!`. Đổi mật khẩu trước khi dùng với dữ liệu thật. Tham khảo [V2__Seed_Initial_Data.sql](clinic-infrastructure/src/main/resources/migrations/V2__Seed_Initial_Data.sql) và [LoginForm.java](clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/auth/LoginForm.java).
2. **Thông tin CSDL mẫu được để trong resource.** `db.properties` chứa user `sa` và mật khẩu mẫu dễ đoán; cấu hình JDBC bật `trustServerCertificate=true`. Chỉ phù hợp môi trường phát triển cô lập; không đóng gói cấu hình này vào bản phát hành. Tham khảo [db.properties](clinic-infrastructure/src/main/resources/db.properties).
3. **Seed chứa tài khoản mẫu và thông tin nhận diện giả lập.** Cần xác định rõ seed chỉ chạy ở môi trường demo và thay đổi/loại bỏ trước khi triển khai. Tham khảo [V2__Seed_Initial_Data.sql](clinic-infrastructure/src/main/resources/migrations/V2__Seed_Initial_Data.sql).
4. **Phân quyền nghiệp vụ toàn hệ thống chưa hoàn tất.** Login lấy role từ DB và không cho người dùng tự chọn role; tuy vậy chưa có kiểm tra quyền ở mọi use case nhạy cảm.

### P1 — Thiếu phần nối để trở thành ứng dụng nghiệp vụ

1. **Application layer mới có nền tảng và xác thực.** Đã có `Result<T>`, Command/Query contracts, password verification và use case đăng nhập; phần lớn luồng khám, tiếp đón, kho và viện phí vẫn thiếu application service.
2. **Persistence mới nối model nhân viên.** Có `DatabaseConnection`, migration runner, JDBC transaction manager/unit of work và `NhanVienRepositoryImpl`; repository implementations cho bệnh nhân, lượt khám, đơn thuốc, kho, viện phí vẫn chưa có.
3. **UI đang hiển thị/nhận dữ liệu tĩnh.** Có dữ liệu bệnh nhân mẫu ở `DoctorStationPanel`, dữ liệu mẫu ở `CatalogPanel`, `ParaclinicalOrderPanel`, `BillingInvoicePanel`, và bảng dược/khám nạp dòng mẫu. Do đó, hình thức UI không phản ánh độ hoàn thiện xử lý nghiệp vụ.
4. **Thiếu vòng đời nghiệp vụ được lưu và phục hồi.** Chưa thấy đường đi hoàn chỉnh từ thao tác UI → application use case → repository → CSDL → cập nhật lại UI cho các luồng chính.
5. **Báo cáo, BHYT và tích hợp thiết bị/cổng ngoài** chủ yếu mới có mô tả hoặc UI; chưa thấy client tích hợp, hợp đồng trao đổi, retry, log, hoặc kiểm thử liên thông.

### P2 — Độ tin cậy, bảo trì và hoàn thiện sản phẩm

1. **Chưa có test case thực chất.** Các thư mục test của 4 module chỉ chứa `.gitkeep`; không có kiểm thử cho rule Domain, repository, login, migration hay luồng Swing.
2. **Chưa có hướng dẫn chạy dự án ở mức onboarding.** Ở root có tài liệu phân tích/thiết kế nhưng chưa thấy README mô tả yêu cầu môi trường, cấu hình CSDL, chạy migration, chạy ứng dụng và tài khoản demo.
3. **Tài liệu có chỗ lệch giữa tuyên bố và hiện thực.** `DOMAIN_ANALYSIS.md` và `docs/UI_DOMAIN_MAPPING_ANALYSIS.md` mô tả khá nhiều service, repository, chức năng và độ tương thích như đã hoàn tất; mã hiện tại lại thiếu application/persistence và có dữ liệu mẫu. Cần phân biệt rõ “đã triển khai”, “UI prototype” và “đề xuất”.
4. **Một số chuẩn UI chưa nhất quán với mã.** `UI_CODING_STANDARDS.md` cấm emoji làm icon chính và yêu cầu tác vụ dài chạy ngoài EDT, nhưng mã dashboard/ribbon có emoji; cần rà lại mọi handler để bảo đảm truy vấn/in ấn không làm treo giao diện.
5. **Chưa xác định rõ trạng thái build hiện tại.** Có thư mục `target` được tạo trước đó, nhưng báo cáo này không chạy build nên không kết luận project biên dịch thành công ở phiên bản hiện tại.

## 5. Ma trận mức độ hoàn thiện theo luồng

| Luồng | UI | Domain/schema | Nghiệp vụ + lưu trữ | Nhận định |
|---|---:|---:|---:|---|
| Đăng nhập / phiên / quyền | Có form | Có entity và bảng | Không có xác thực thực | Demo; ưu tiên xử lý đầu tiên. |
| Tiếp đón / hồ sơ bệnh nhân | Nhiều component | Có entity và trường dữ liệu | Chưa có use case/repository | UI và mô hình có nền tảng, chưa dùng dữ liệu thật. |
| Khám / EMR / chỉ định | Nhiều màn hình | Khá rộng | Chưa thấy luồng lưu/khóa EMR hoàn chỉnh | Prototype UI; rủi ro cao vì dữ liệu y tế. |
| Cận lâm sàng | Có panel | Có service/result models và bảng | Chưa thấy kết nối máy/cSDL | Mới thể hiện quy trình. |
| Kê đơn / cấp phát / FEFO | Có màn hình; có rule FEFO | Có entity và service miền | Chưa có transaction kho thực tế | Chưa dùng trong vận hành. |
| Viện phí / thanh toán | Có panel và mẫu in | Có entity và schema | Chưa thấy tích hợp thu tiền/hóa đơn thực | Demo dữ liệu. |
| Danh mục / báo cáo / cấu hình | Có màn hình | Một phần entity/schema | Thiếu CRUD và truy vấn thật | Khung chức năng. |
| In/xuất tài liệu | Có preview và mã DOCX | Có export service | Chưa thấy nút UI gọi `DocxManager` | Có thành phần nhưng chưa tích hợp vào luồng chính. |

## 6. Lộ trình đề xuất

### Giai đoạn 1 — Làm an toàn môi trường demo

- Tách cấu hình dev/demo/prod; bỏ mật khẩu mặc định khỏi resource, ưu tiên biến môi trường hoặc secret store.
- Gỡ tài khoản/mật khẩu điền sẵn trên login; không cho phép vào hệ thống nếu chưa xác thực.
- Gắn nhãn rõ dữ liệu giả; không dùng dữ liệu bệnh nhân thật trong seed hoặc ảnh chụp demo.
- Ghi rõ điều kiện chỉ chạy trên dữ liệu giả cho đến khi hoàn tất xác thực và lưu trữ.

### Giai đoạn 2 — Dựng vertical slice đầu tiên

- Chọn một luồng nhỏ, chẳng hạn **đăng nhập → tra cứu bệnh nhân → đăng ký lượt khám**.
- Viết application use case và validation, repository interface phù hợp, JDBC repository implementation và transaction handling.
- Nối UI vào use case; xóa dữ liệu mock tương ứng sau khi có nguồn dữ liệu thật.
- Thêm test cho validation/domain rule và repository trên CSDL test.

### Giai đoạn 3 — Hoàn chỉnh các luồng cốt lõi

- Lần lượt hoàn thiện EMR/khóa hồ sơ, chỉ định và kết quả CLS, kê đơn/cấp phát FEFO, hóa đơn/phiếu thu.
- Bổ sung kiểm tra quyền ở tầng application, audit log, xử lý lỗi/timeout và đồng bộ trạng thái UI.
- Xác nhận schema, constraint/index, tính idempotent của migration; thiết lập sao lưu và phục hồi dữ liệu.

### Giai đoạn 4 — Chất lượng phát hành

- Bổ sung test tự động, quy trình build/release, cấu hình môi trường, hướng dẫn cài đặt/vận hành và logging.
- Đánh giá khả năng chạy nhiều người dùng đồng thời, kiểm soát giao dịch tồn kho/thu tiền và tác vụ SwingWorker.
- Kiểm tra UI theo độ phân giải và chuẩn đã nêu trong `UI_CODING_STANDARDS.md`; xác nhận mẫu in với bên nghiệp vụ trước khi dùng thực tế.

## 7. Tiêu chí tối thiểu trước khi coi là MVP dùng dữ liệu thật

- Đăng nhập xác thực bằng nguồn danh tính thật; role/quyền được kiểm tra ở tầng nghiệp vụ.
- Có cấu hình triển khai an toàn, không chứa thông tin mặc định/secret trong artifact.
- Tối thiểu một luồng end-to-end ghi/đọc CSDL có transaction và test.
- Không còn dữ liệu giả trong các màn hình được đưa vào MVP.
- Migration có quy trình chạy, versioning và kiểm chứng trên CSDL mới cũng như CSDL đã có dữ liệu.
- Có audit log tối thiểu cho thao tác hồ sơ, đơn thuốc, kho và viện phí; có sao lưu/khôi phục được xác nhận.
- Có hướng dẫn cài đặt, cấu hình, chạy, khôi phục lỗi và giới hạn đã biết.

## 8. Giới hạn của đánh giá

- Đánh giá dựa trên file hiện có trong workspace; không có lịch sử commit vì thư mục làm việc không phải Git repository.
- Không chạy build, test, khởi động ứng dụng hoặc kết nối tới SQL Server.
- Không đánh giá tính đúng pháp lý của biểu mẫu y tế, BHYT, hóa đơn điện tử hoặc tích hợp cơ quan bên ngoài; các phần này cần đối chiếu quy định và kiểm thử nghiệp vụ riêng trước khi triển khai.

