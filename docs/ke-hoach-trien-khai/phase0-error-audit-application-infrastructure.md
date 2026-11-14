# PLAT-ERROR-02A-R1 — Audit Application và Infrastructure

**Phạm vi:** chỉ `clinic-application/src/main/java` và `clinic-infrastructure/src/main/java`. Đây là audit tĩnh, report-only; không kiểm tra Swing, không truy cập database, dữ liệu bệnh nhân, secrets hoặc dịch vụ ngoài. Không sửa code/test.

## Kết quả

| ID | Phân loại | Bằng chứng và hành vi quan sát được | Khắc phục giới hạn và test tổng hợp đề xuất |
|---|---|---|---|
| APP-ERR-01 | **Risk — thiếu chẩn đoán giới hạn** | `clinic-application/src/main/java/vn/clinicmanagement/application/authorization/PhanQuyenDichVu.java:91-107`: lỗi runtime từ truy vấn ma trận quyền hoặc các dependency trong khối `try` bị bắt chung, sau đó trả `PhanQuyenQuyetDinh.deny(DEPENDENCY_FAILURE)` tại dòng 106–107 mà không ghi log. `clinic-application/src/main/java/vn/clinicmanagement/application/authorization/DieuHuongQuyenHandler.java:35-43`: lỗi runtime từ repository bị chuyển thành `NAVIGATION_PERMISSIONS_UNAVAILABLE` với thông điệp cố định, nhưng nguyên nhân bị bỏ và không có chẩn đoán. Hành vi từ chối mặc định và nội dung trả về an toàn; khi repository hỏng, người vận hành không nhận được loại exception/stack giới hạn để phân biệt lỗi dependency với từ chối chính sách. | Bổ sung chẩn đoán dùng formatter đã có tại `clinic-application/src/main/java/vn/clinicmanagement/application/common/UngDungThatBaiChanDoanLoi.java:27-39,42-77` ở đúng hai boundary trên. Chỉ truyền operation/result code và correlation UUID; giữ nguyên deny/result hiện tại, không ghi exception message hay dữ liệu yêu cầu. Test synthetic cho repository ném `RuntimeException` trong mỗi boundary: xác nhận kết quả từ chối/mã/thông điệp không đổi, log có operation/result code và type/stack bị giới hạn, không chứa message marker tổng hợp; đồng thời xác nhận lỗi logger không đổi kết quả. |

## Không phát hiện lỗi hiện hành trong các phần đã kiểm tra

- **Hợp đồng `KetQua`: không finding.** `clinic-application/src/main/java/vn/clinicmanagement/application/common/KetQua.java:6-27,34-38` giữ cặp `code`/`message` bắt buộc cho failure và API success/failure rõ ràng. Các handler trong scope trả mã ổn định và thông điệp cố định cho các lỗi dependency đã kiểm tra; chưa thấy bằng chứng để kết luận hợp đồng chung làm lộ exception text.
- **Dịch exception JDBC: không finding.** Ví dụ `clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/system/HeThongRepository.java:58-60,77-79` và `clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/facility/PhongKhamRepository.java:68` chuyển `SQLException` thành thông điệp cố định, giữ exception làm cause và không đưa message SQL vào chuỗi mới. `clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/patient/BaoHiemRepository.java:107-112` cũng dùng thông điệp cố định. Việc ghi chẩn đoán nên thuộc application boundary, nơi có operation/result code; không đề xuất log tại từng repository.
- **Fallback timeout phiên: không finding.** `clinic-application/src/main/java/vn/clinicmanagement/application/session/PhienHetGioChinhSach.java:26-40,43-55` dùng default hữu hạn khi đọc/tham số không hợp lệ và cảnh báo theo key đã kiểm soát, không ghi giá trị cấu hình.
- **Completed reception/auth diagnostics — kiểm tra hồi quy, không coi là defect.** Ví dụ handler reception ghi diagnostic giới hạn trước khi trả lỗi generic tại `clinic-application/src/main/java/vn/clinicmanagement/application/reception/GetHoatDongTiepDonPhongHandler.java:38-46`; authentication cũng dùng formatter và lỗi generic tại `clinic-application/src/main/java/vn/clinicmanagement/application/auth/AuthenticateNguoiDungHandler.java:109-112`. Các task `PLAT-ERROR-01A` và `PLAT-ERROR-AUTH-01` được đánh dấu complete trong task board.
- **Completed transaction/migration/config diagnostics — không coi là defect hiện hành.** `clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/JdbcGiaoDichQuanLy.java:47-72` trả lỗi generic và dọn transaction state; `CoSoDuLieuDiCuChay.java:97-100,205-220` in thông báo lỗi đã giới hạn. Task board ghi `PLAT-TXN-02`, `REL-MIGRATION-CLI-01` và các task cấu hình liên quan đã xử lý những boundary này. `CoSoDuLieuKetNoi.java:135-159` kiểm tra kết nối bằng kết quả boolean và thông báo cố định, không in exception message.

## Đối chiếu tài liệu lịch sử và task board

`docs/ke-hoach-trien-khai/phase0-error-mapping-review.md` được xem là snapshot lịch sử theo chỉ dẫn, không dùng làm bằng chứng cho lỗi hiện hành. Đã đối chiếu `docs/ke-hoach-trien-khai/03-model-task-board.md`: các lát cắt PLAT-ERROR-UI-01, PLAT-ERROR-01A, PLAT-ERROR-AUTH-01, PLAT-ERROR-DOCX-01, PLAT-ERROR-STARTUP-01, PLAT-ERROR-SWING-01, PLAT-TXN-02 và REL-MIGRATION-CLI-01 được ghi complete; audit này không mở lại các lỗi đã hoàn tất. Đây không phải nghiệm thu toàn diện PLAT-ERROR-01.

## Hướng dẫn đã đọc

- `docs/PROJECT_CONVENTIONS.md` (nội dung được cung cấp trong task; không có bản sao trong checkout)
- `README.md`
- `DOMAIN_ANALYSIS.md`
- `ERD_DIAGRAM.md`
- `UI_CODING_STANDARDS.md`
- `docs/ke-hoach-trien-khai/README.md`
- `docs/ke-hoach-trien-khai/00-hien-trang.md`
- `docs/ke-hoach-trien-khai/03-model-task-board.md`
- `docs/ke-hoach-trien-khai/phase0-error-mapping-review.md` (chỉ để đối chiếu lịch sử)
- `docs/ke-hoach-trien-khai/phan-he/03-tiep-don-benh-nhan-nhan-su.md`
- Không tìm thấy `AGENTS.md`, `docs/DEVELOPMENT_CONVENTIONS.md` hoặc `docs/README.md` trong checkout.

## Kiểm định và giới hạn

- Audit source tĩnh: chỉ đọc Java trong hai thư mục được giao; không gọi database, patient data, secrets hay external service.
- `java -version` mặc định là JDK 27; xác nhận JDK 17.0.17 có sẵn qua `/usr/libexec/java_home -V`.
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.17/libexec/openjdk.jdk/Contents/Home PATH="$JAVA_HOME/bin:$PATH" mvn -Djava.awt.headless=true clean verify` — **blocked trước build/test** (exit 1): Maven không mở được lock trong shared `~/.m2/repository/.locks` cho `maven-clean-plugin:3.5.0` sau 5 lần thử. Không di chuyển, sao chép hoặc xóa cache; chưa có kết quả compile/test. Không chạy migration hay test yêu cầu SQL Server opt-in.
- `git diff --check` — pass.
- Chỉ file báo cáo được giao được phép thay đổi.
