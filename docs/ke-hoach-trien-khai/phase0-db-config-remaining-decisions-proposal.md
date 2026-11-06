# Phase 0: Packaged-JAR Database Configuration — Remaining Decisions Proposal
## Đề xuất Quyết định Kỹ thuật: Hoàn thiện Cấu hình Cơ sở Dữ liệu cho Artifact Đóng gói (JAR)

> **Tài liệu đề xuất kỹ thuật (Non-binding technical proposal)**  
> **Mã hợp đồng tham chiếu:** `PHASE0-PHASE3-REVIEW-AND-TRACKING-DOCS-01`  
> **Trạng thái:** Dự thảo đề xuất kỹ thuật — Chờ phê duyệt của Owner (DRAFT / NON-BINDING)  
> **Mục tiêu:** Cung cấp cơ sở phân tích độc lập, đối soát mã nguồn và so sánh phương án cho các chi tiết cấu hình CSDL còn lại của artifact đóng gói `clinic-desktop.jar` thuộc Phase 0, không tự ý biến khuyến nghị thành quy định chính thức.

---

## 1. Bối cảnh và Các Tiền đề đã được Owner phê duyệt (Baseline & Approved Facts)

Tài liệu này kế thừa và tuân thủ tuyệt đối các nguyên tắc nền tảng đã được phê duyệt trong hợp đồng `PHASE0-PHASE3-REVIEW-AND-TRACKING-DOCS-01` và các báo cáo đối soát trước đó ([`phase0-db-config-review.md`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/docs/ke-hoach-trien-khai/phase0-db-config-review.md), [`phase0-db-config-resolution-contract.md`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/docs/ke-hoach-trien-khai/phase0-db-config-resolution-contract.md)):

1. **Bắt buộc hỗ trợ song song hai kênh nạp cấu hình:** Triển khai desktop dạng packaged-JAR bắt buộc phải hỗ trợ cả hai nguồn: file properties bên ngoài (`external properties file`) và biến môi trường/secret injection (`environment variables`).
2. **Quy tắc ưu tiên cấp trường (Per-key Environment Precedence):** Biến môi trường có quyền ưu tiên cao hơn trên từng trường đơn lẻ (`per-key environment wins`); file properties đóng vai trò là giá trị nền và bù đắp các trường còn thiếu (`file fills keys absent from environment`).
3. **Chính sách TLS bắt buộc mã hóa và xác thực chứng chỉ (Mandatory Certificate-Validating TLS):** Kết nối CSDL bắt buộc phải mã hóa và bắt buộc phải kiểm tra tính hợp lệ của chứng chỉ số cùng danh tính máy chủ (`encrypt=true; trustServerCertificate=false`), áp dụng nguyên tắc đóng khi lỗi (fail-closed, không fallback về kết nối không mã hóa hoặc tự tin cậy chứng chỉ).
4. **Không thay đổi hành vi runtime / An toàn tuyệt đối:** Cổng an ninh `SEC-01` và toàn bộ các cổng hạ nguồn tiếp tục ở trạng thái ĐÓNG / Mặc định từ chối (`OPEN / DEFAULT-DENY`). Không thực hiện bất kỳ thay đổi nào đối với mã nguồn Java, bộ kiểm thử, schema migration, phân quyền hoặc runtime. Giữ nguyên script `run-app.sh`.

---

## 2. Bằng chứng Mã nguồn và Hiện trạng Kiến trúc (Source Evidence & Proven Facts)

Dựa trên kiểm tra mã nguồn tại repository:

### 2.1. Đóng gói Artifact và Loại trừ Bí mật (Packaging Evidence)
- Trong [`clinic-desktop/pom.xml:46-56`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-desktop/pom.xml#L46-L56), cấu hình `maven-shade-plugin` thiết lập bộ lọc loại trừ tệp có tên `db.properties` khỏi tệp JAR phân phối (`<exclude>db.properties</exclude>`), đồng thời loại bỏ chữ ký `META-INF/*.SF`.
- Tệp mẫu [`clinic-infrastructure/src/main/resources/db.properties.example:1-7`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/main/resources/db.properties.example#L1-L7) vẫn được đóng gói trong JAR, nhưng chỉ chứa placeholder và tham số mẫu cục bộ (`encrypt=true;trustServerCertificate=true`).

### 2.2. Trình nạp Cấu hình Hiện tại (Loader & Resolver Implementation)
- Trong [`clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java:29-41`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java#L29-L41), phương thức `loadProperties()` chỉ thực hiện nạp tệp tài nguyên từ classpath thông qua `DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties")`. Lớp này hoàn toàn không có tham số hay cơ chế nạp file từ hệ thống tệp bên ngoài (`filesystem path`). Mọi lỗi đọc hoặc định dạng tệp đều bị nuốt lặng lẽ (`catch (Exception ignored) { properties.clear(); }`).
- Trong [`clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java:22-35`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L22-L35), phương thức `resolve()` xử lý ưu tiên giữa `Properties` và `Map<String, String> environment`:
  - `override()` ([dòng 80-82](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L80-L82)): nếu biến môi trường không rỗng (`nonblank`), giá trị môi trường sau khi cắt khoảng trắng thừa (`trim()`) sẽ ghi đè giá trị trong property.
  - `nonblank()` ([dòng 84-86](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L84-L86)): kiểm tra `value != null && !value.isBlank()`. Các biến môi trường rỗng hoặc chỉ có khoảng trắng được coi là vắng mặt (`absent`) và bị bỏ qua, để lộ giá trị trong properties.
  - Biến môi trường `DB_CONNECTION_STRING` ([dòng 26, 32-34](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L26-L34)) được giữ nguyên vẹn và giành quyền ưu tiên tuyệt đối khi xác định `ConnectionTarget` ([dòng 103-106](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L103-L106)).

### 2.3. Trạng thái Trường `DB_DRIVER` và Đăng ký Driver
- Trường `DB_DRIVER` và khóa `db.driver` được đọc và gán vào `ResolvedConfiguration.driver()` ([`DatabaseConfiguration.java:31`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L31)), nhưng không hề được tham chiếu hay sử dụng trong luồng mở kết nối ([`DatabaseConnection.java:76-98`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java#L76-L98)).
- Trình quản lý kết nối tự suy luận driver thông qua tiền tố chuỗi URL bằng phương thức `autoRegisterDriver(url)` ([`DatabaseConnection.java:115-129`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java#L115-L129)) hoặc cơ chế `ServiceLoader` chuẩn của JDBC 4.0+.

### 2.4. Khử vết Dữ liệu Nhạy cảm (Redaction)
- Các record nội bộ `ResolvedConfiguration` và `ConnectionTarget` đã cài đè phương thức `toString()` ([`DatabaseConfiguration.java:94-101, 118-125`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L94-L125)) để chỉ in các cờ boolean (`hasUrl=true`, `hasPassword=true`), ngăn chặn việc lộ chuỗi kết nối và mật khẩu trong log hệ thống.
- Các bài kiểm thử [`DatabaseConnectionTest.java:76-94`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnectionTest.java#L76-L94) và [`DatabaseConfigurationTest.java:87-101`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfigurationTest.java#L87-L101) đã sử dụng chuỗi sentinel giả lập (`synthetic-password-marker`, `synthetic-secret-marker`) để chứng minh tính an toàn khi in lỗi hoặc chẩn đoán.

---

## 3. Phân tích So sánh Các Quyết định Kỹ thuật Còn lại (Options, Tradeoffs & Recommendations)

Phần này phân tách tường minh giữa **Sự thật đã chứng minh (Facts)**, **Suy luận kỹ thuật (Inferences)**, **Các phương án (Options)**, **Đánh đổi (Tradeoffs)**, và **Đề xuất khuyến nghị phi ràng buộc (Non-binding Recommendation)**.

```
+---------------------------------------------------------------------------------------------------+
|                                CÁC QUYẾT ĐỊNH CẤU HÌNH CẦN THỐNG NHẤT                              |
+---------------------------------------------------------------------------------------------------+
| 1. Giao diện chỉ định file cấu hình ngoài (JVM System Property vs Dedicated Env vs CLI arg)       |
| 2. Thứ tự ưu tiên DB_CONNECTION_STRING và bộ 3 trường phân tách (Split URL/User/Password)          |
| 3. Ngữ nghĩa của biến môi trường rỗng/khoảng trắng (Vắng mặt / Absent vs Không hợp lệ / Invalid)  |
| 4. Các tổ hợp xác thực JDBC và thông tin xác thực một phần hợp lệ (Partial Credentials)           |
| 5. Ứng xử khi file cấu hình chỉ định bị lỗi/không đọc được (Fail-Closed vs Permissive Fallback)     |
| 6. Định hướng xử lý biến DB_DRIVER không sử dụng (Loại bỏ vs Kích hoạt Class.forName)             |
| 7. Cung cấp Trust-Anchor TLS chuẩn chỉ và khử vết bí mật trong cấu hình SSL/TLS                   |
+---------------------------------------------------------------------------------------------------+
```

---

### 3.1. Quyết định 1: Giao diện Chỉ định File Cấu hình Ngoài (Explicit External-File Selection)

#### a. Sự thật (Facts)
- Mã nguồn hiện hành chỉ nạp `db.properties` từ classpath ([`DatabaseConnection.java:31`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java#L31)).
- Tệp JAR đóng gói loại trừ `db.properties` ([`clinic-desktop/pom.xml:49`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-desktop/pom.xml#L49)). Do đó, khi chạy packaged JAR ngoài môi trường phát triển, ứng dụng hoàn toàn không thể nạp file properties nếu không có cơ chế chỉ định tường minh.

#### b. Suy luận kỹ thuật (Inferences)
- Việc tự động dò tìm file ngầm định ở thư mục làm việc hiện tại (`Current Working Directory - CWD`) hoặc thư mục cùng cấp với file JAR (`Adjacent-JAR discovery`) tiềm ẩn nguy cơ bảo mật nghiêm trọng (CWE-426: Untrusted Search Path):
  - Người dùng mở ứng dụng từ shortcut, desktop hoặc terminal khác nhau có thể vô tình nạp một file cấu hình lạ nằm tại CWD.
  - Kẻ tấn công trên máy dùng chung có thể đặt file `db.properties` độc hại tại thư mục khởi chạy để đánh lừa ứng dụng kết nối tới CSDL mạo danh.
  - Do đó, quy tắc an toàn bắt buộc: **Không hỗ trợ tự dò tìm ngầm định (No implicit adjacent-JAR/CWD discovery)**; đường dẫn file properties phải được chỉ định **tường minh (explicit)**.

#### c. Các phương án so sánh (Options)
- **Phương án 1A — Chỉ định qua Thuộc tính Hệ thống JVM (Explicit JVM System Property):**  
  Ví dụ: `java -Dclinic.config.file=/opt/clinic/db.properties -jar clinic-desktop.jar`.  
  *Ưu điểm:* Chuẩn mực Java truyền thống; tách biệt rõ ràng giữa tham số JVM và tham số ứng dụng; quản trị viên có thể cấu hình cố định trong script khởi chạy hoặc file cấu hình service.  
  *Nhược điểm:* Đòi hỏi quyền kiểm soát dòng lệnh JVM khi khởi chạy.
- **Phương án 1B — Chỉ định qua Biến Môi trường Chuyên dụng (Dedicated Environment Variable):**  
  Ví dụ: `export CLINIC_DB_CONFIG_FILE=/etc/clinic/db.properties`.  
  *Ưu điểm:* Rất tự nhiên trong môi trường container hóa hoặc triển khai desktop phân phối qua group policy/profile script của hệ điều hành. Không cần can thiệp chuỗi lệnh `java -jar`.  
  *Nhược điểm:* Biến môi trường có thể bị rò rỉ qua các tiến trình con nếu không được cô lập cẩn thận.
- **Phương án 1C — Hỗ trợ Cả Hai theo Thứ tự Ưu tiên Rõ ràng (Dual Explicit Interface):**  
  Kiểm tra `-Dclinic.config.file` trước; nếu không có thì kiểm tra `CLINIC_DB_CONFIG_FILE`. Tuyệt đối không fallback về CWD hay thư mục chứa JAR nếu cả hai đều vắng mặt.  
  *Ưu điểm:* Linh hoạt tối đa cho cả quản trị viên hệ thống (dùng env) và người viết script đóng gói (dùng `-D`).  
  *Nhược điểm:* Cần tài liệu hóa thứ tự ưu tiên giữa hai giao diện để tránh nhầm lẫn.

#### d. Đánh đổi và Đề xuất khuyến nghị (Tradeoffs & Recommendation)
- **Đề xuất (Recommendation):** Áp dụng **Phương án 1C (Dual Explicit Interface)** với quy tắc ưu tiên:
  1. Kiểm tra System Property `-Dclinic.config.file` (ưu tiên cao nhất nếu được chỉ định).
  2. Nếu không có, kiểm tra Environment Variable `CLINIC_DB_CONFIG_FILE`.
  3. Nếu cả hai đều không được đặt: coi như không sử dụng file ngoài; chỉ nạp cấu hình từ các biến môi trường trực tiếp (`DB_URL`, `DB_USERNAME`, v.v.) hoặc classpath (nếu đang chạy dev trong IDE).
  4. **CẤM TUYỆT ĐỐI:** Không quét `./db.properties` tại CWD và không quét `db.properties` nằm cùng thư mục với JAR.

---

### 3.2. Quyết định 2: Thứ tự Ưu tiên giữa `DB_CONNECTION_STRING` và Bộ Ba Trường Phân tách (Combined vs Split Precedence)

#### a. Sự thật (Facts)
- Hiện tại, `DatabaseConfiguration.java:26-34, 103-106` cho phép `DB_CONNECTION_STRING` (khi khác rỗng) giành quyền ưu tiên tuyệt đối trước bộ ba trường `url`, `username`, `password`.
- Phương thức `DatabaseConfiguration.override()` chỉ áp dụng cho từng trường phân tách (`db.url` vs `DB_URL`, v.v.), không trộn lẫn chuỗi kết nối gộp với các trường phân tách.

#### b. Suy luận kỹ thuật (Inferences)
- Khi nhà vận hành cấu hình cả chuỗi đầy đủ và các trường phân tách, có thể phát sinh hai tình huống xung đột:
  - Tình huống 1: Chuỗi kết nối đầy đủ nằm trong file cấu hình nền, nhưng biến môi trường chứa `DB_PASSWORD` mới (để xoay vòng mật khẩu bí mật). Nếu chuỗi đầy đủ luôn thắng, mật khẩu mới trong môi trường sẽ bị phớt lờ, dẫn đến lỗi kết nối khó chẩn đoán.
  - Tình huống 2: Biến môi trường chứa cả `DB_CONNECTION_STRING` và `DB_URL` với các máy chủ CSDL khác nhau.

#### c. Các phương án so sánh (Options)
- **Phương án 2A — Giữ nguyên Thiết kế Hiện hành (Full String Absolute Win):**  
  `DB_CONNECTION_STRING` (nếu có giá trị) luôn giành quyền ưu tiên trước bộ ba trường `URL/User/Password`, bất kể nguồn gốc của nó.  
  *Ưu điểm:* Tương thích ngược 100% với mã nguồn hiện tại của `DatabaseConfiguration.java`; hành vi đơn giản, dễ giải thích.  
  *Nhược điểm:* Ngăn cản việc dùng biến môi trường để ghi đè từng phần (ví dụ chỉ ghi đè mật khẩu) khi file nền cung cấp chuỗi kết nối đầy đủ.
- **Phương án 2B — Phân tầng Ưu tiên theo Nguồn (Source-Aware Precedence):**  
  Tầng Môi trường có quyền ưu tiên cao hơn Tầng File Properties. Trong cùng một tầng, Chuỗi kết nối đầy đủ ưu tiên hơn Trường phân tách. Cụ thể:  
  `Env DB_CONNECTION_STRING` > `Env Split (DB_URL/User/Pass)` > `File Connection String` > `File Split (db.url/username/password)`.  
  *Ưu điểm:* Giải quyết được bài toán vận hành thực tế: nếu file nền khai báo chuỗi kết nối, nhưng biến môi trường inject `DB_URL` hoặc `DB_PASSWORD`, cấu hình môi trường sẽ chiến thắng.  
  *Nhược điểm:* Đòi hỏi bổ sung logic phân tầng nguồn trong bộ giải quyết (`resolver`).
- **Phương án 2C — Loại trừ Lẫn nhau / Báo lỗi khi Xung đột (Mutual Exclusivity / Fail-Closed):**  
  Bộ giải quyết từ chối khởi động nếu phát hiện cả `DB_CONNECTION_STRING` và bất kỳ trường phân tách nào (`DB_URL`, `DB_USERNAME`) cùng được cung cấp ở cùng một mức cấu hình.  
  *Ưu điểm:* Loại bỏ hoàn toàn sự nhập nhằng; buộc quản trị viên phải chọn 1 trong 2 phong cách cấu hình.  
  *Nhược điểm:* Quá cứng nhắc, có thể làm gãy các template triển khai tự động (nơi một số biến rác mặc định được inject sẵn).

#### d. Đánh đổi và Đề xuất khuyến nghị (Tradeoffs & Recommendation)
- **Đề xuất (Recommendation):** Áp dụng **Phương án 2B (Source-Aware Precedence)**. Đây là phương án dung hòa tối ưu, bảo toàn nguyên tắc nền tảng *"Per-key environment wins and file fills keys absent from environment"*: Môi trường luôn có quyền tối thượng; cấu hình trong file chỉ đóng vai trò nền dự phòng.

---

### 3.3. Quyết định 3: Ngữ nghĩa của Biến Môi trường Rỗng hoặc Khoảng trắng (Blank Environment Semantics: Absent vs Invalid)

#### a. Sự thật (Facts)
- Trong [`DatabaseConfiguration.java:80-86`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L80-L86), hàm `override` sử dụng `nonblank(environmentValue)`:
  ```java
  private static String override(String propertyValue, String environmentValue) {
      return nonblank(environmentValue) ? environmentValue.trim() : propertyValue;
  }
  private static boolean nonblank(String value) {
      return value != null && !value.isBlank();
  }
  ```
- Nếu một biến môi trường được đặt là chuỗi rỗng (`DB_PASSWORD=""`) hoặc chỉ chứa khoảng trắng (`"   "`), nó được xem là `nonblank() == false`. Kết quả là giá trị trong file properties được giữ nguyên (`propertyValue`).

#### b. Suy luận kỹ thuật (Inferences)
- Trong triển khai tự động (Docker, Kubernetes, CI/CD, systemd):
  - Khi một secret injection bị lỗi (ví dụ vault secret chưa mount kịp hoặc cấu hình trống), biến môi trường thường có giá trị là chuỗi rỗng `""`.
  - Nếu xem chuỗi rỗng là `absent` (vắng mặt), ứng dụng sẽ âm thầm lấy giá trị mặc định từ file properties. Nếu file properties chứa thông tin môi trường staging/dev, ứng dụng có thể kết nối nhầm cơ sở dữ liệu mà không hề cảnh báo!
  - Mặt khác, trong một số hệ thống nội bộ, mật khẩu rỗng (`empty password`) có thể là một giá trị có chủ đích (ví dụ tài khoản Windows Authentication hoặc dev database không mật khẩu).

#### c. Các phương án so sánh (Options)
- **Phương án 3A — Duy trì Hành vi Hiện tại (Permissive: Blank == Absent):**  
  Chuỗi rỗng hoặc chỉ có khoảng trắng được coi như không tồn tại, cho phép giá trị trong file hiển thị.  
  *Ưu điểm:* Linh hoạt với các script shell khởi chạy có định nghĩa sẵn biến nhưng chưa gán giá trị; không làm thay đổi các test hiện có ([`DatabaseConfigurationTest.java:45-60`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfigurationTest.java#L45-L60)).  
  *Nhược điểm:* Nguy cơ âm thầm dùng giá trị fallback của file khi secret injection bị hỏng; không có cách nào dùng biến môi trường để "xóa" (clear) một giá trị đã lỡ đặt trong file properties.
- **Phương án 3B — Kiểm soát Nghiêm ngặt (Fail-Closed: Blank == Invalid / Error):**  
  Nếu một biến môi trường cấu hình DB quan trọng (`DB_URL`, `DB_CONNECTION_STRING`, `DB_USERNAME`) được cung cấp nhưng có giá trị rỗng/khoảng trắng, coi đó là cấu hình sai và ném lỗi khởi động ngay lập tức.  
  *Ưu điểm:* Phát hiện ngay các lỗi tiêm bí mật của hệ thống CI/CD/CDKT; an toàn tối đa.  
  *Nhược điểm:* Chặn đứng các trường hợp cố ý truyền chuỗi rỗng; có thể gây lỗi nếu công cụ deploy tự động gắn biến rỗng.
- **Phương án 3C — Phân tách theo Bản chất Trường (Key-Specific Semantics):**  
  - Đối với `DB_URL` và `DB_CONNECTION_STRING`: Giá trị rỗng/khoảng trắng là **Không hợp lệ (Invalid - Fail Closed)** vì URL không thể rỗng.  
  - Đối với `DB_PASSWORD`: Giá trị rỗng (`""`) được xem là mật khẩu rỗng có chủ đích (nếu DB cho phép), phân biệt rõ với `null` (không khai báo biến). Nếu chỉ toàn khoảng trắng (`"   "`), coi là lỗi định dạng.  
  - Đối với `DB_USERNAME`: Giá trị rỗng được coi là kết nối không dùng xác thực tường minh (nhường quyền cho Windows Integrated Auth).

#### d. Đánh đổi và Đề xuất khuyến nghị (Tradeoffs & Recommendation)
- **Đề xuất (Recommendation):** Áp dụng **Phương án 3C (Key-Specific Semantics)** kết hợp nguyên tắc phòng thủ: Đối với URL/Connection String, chuỗi rỗng phải báo lỗi (`Invalid`); đối với tài khoản/mật khẩu, phân biệt rõ giữa việc "không đặt biến" (`null` -> fallback lấy từ file) và việc "đặt biến rỗng" (`""` -> ghi đè giá trị rỗng hoặc báo lỗi cấu hình nếu phương thức xác thực đòi hỏi mật khẩu).

---

### 3.4. Quyết định 4: Các Tổ hợp Xác thực JDBC và Thông tin Xác thực Một phần (Valid Partial Credential / JDBC Auth Combinations)

#### a. Sự thật (Facts)
- Trong [`DatabaseConfiguration.java:107-113`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L107-L113):
  ```java
  if (url != null && !url.isEmpty()) {
      if (username != null && !username.isEmpty()) {
          return new ConnectionTarget(ConnectionMode.URL_WITH_CREDENTIALS, url, username, password);
      }
      return new ConnectionTarget(ConnectionMode.URL_ONLY, url, null, null);
  }
  ```
- Nếu `username` có giá trị nhưng `password` là `null`, mã nguồn vẫn chuyển sang chế độ `URL_WITH_CREDENTIALS` và gọi `DriverManager.getConnection(url, username, null)`.
- Nếu `password` có giá trị nhưng `username` là `null` hoặc rỗng, mã nguồn lại chuyển sang chế độ `URL_ONLY` và **vứt bỏ hoàn toàn mật khẩu** mà không cảnh báo!

#### b. Suy luận kỹ thuật (Inferences)
- Trình điều khiển SQL Server (`mssql-jdbc`) hỗ trợ nhiều chế độ xác thực khác nhau:
  1. **SQL Server Authentication:** Bắt buộc phải có cặp đôi hoàn chỉnh: Cả `username` và `password` đều phải có giá trị.
  2. **Integrated Security (Windows Authentication):** URL chứa `integratedSecurity=true`. Không được truyền `username` và `password` qua JDBC overloads vì driver sẽ sử dụng Kerberos/NTLM của tiến trình hệ điều hành.
  3. **Azure Active Directory / Managed Identity:** URL chứa `authentication=ActiveDirectoryIntegrated` hoặc `authentication=ActiveDirectoryMSI`.
- Việc cho phép cấu hình "nửa vời" (chỉ có username mà không có password, hoặc chỉ có password mà không có username) hầu hết là do lỗi của người vận hành (quên copy một biến). Việc âm thầm nuốt mất password hoặc truyền password null xuống JDBC là hành vi nguy hiểm.

#### c. Các phương án so sánh (Options)
- **Phương án 4A — Nguyên tắc Cặp Đôi Bắt buộc (Strict Credential Pair Requirement):**  
  Đối với cấu hình phân tách (`split configuration`): `username` và `password` bắt buộc phải cùng xuất hiện (cả hai đều có giá trị) HOẶC cùng vắng mặt (cả hai đều `null`/rỗng). Nếu chỉ có một trong hai trường có giá trị, bộ giải quyết lập tức từ chối và báo lỗi `IncompleteCredentialsException`.  
  *Ưu điểm:* Cực kỳ an toàn; ngăn chặn triệt để lỗi đánh máy hoặc mất cấu hình từng phần. Phù hợp 100% với chế độ SQL Authentication.  
  *Nhược điểm:* Chặn trường hợp hy hữu khi người dùng cố tình tạo tài khoản SQL Server không có mật khẩu (tuy nhiên thực tiễn an ninh y tế không bao giờ cho phép tài khoản DB không mật khẩu).
- **Phương án 4B — Nhận diện theo Chế độ Xác thực của URL (Authentication-Mode-Aware Validation):**  
  Kiểm tra chuỗi URL:
  - Nếu URL chứa `integratedSecurity=true` hoặc `authentication=ActiveDirectory...`: Bắt buộc cả username và password phải vắng mặt. Nếu có truyền credentials, báo lỗi cấu hình thừa.
  - Nếu URL không chứa các tham số trên: Áp dụng quy tắc cặp đôi bắt buộc như Phương án 4A.  
  *Ưu điểm:* Hoàn hảo về mặt kỹ thuật; kiểm soát chính xác theo từng cơ chế xác thực của Microsoft SQL Server.  
  *Nhược điểm:* Bộ giải quyết cần phân tích cú pháp chuỗi URL (URL parsing), làm tăng độ phức tạp của resolver.

#### d. Đánh đổi và Đề xuất khuyến nghị (Tradeoffs & Recommendation)
- **Đề xuất (Recommendation):** Áp dụng **Phương án 4A (Strict Credential Pair Requirement)** ở mức tầng cấu hình cơ sở (resolver). Đây là giải pháp đơn giản, không phụ thuộc vào phương ngữ URL của từng loại CSDL, bảo vệ phòng khám khỏi các lỗi mất mát cấu hình nhạy cảm.

---

### 3.5. Quyết định 5: Ứng xử khi File Cấu hình Chỉ định Bị Lỗi hoặc Không Đọc được (Unreadable/Malformed File Behavior)

#### a. Sự thật (Facts)
- Trong [`DatabaseConnection.java:35-39`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java#L35-L39), mọi ngoại lệ khi nạp tệp properties (như không tìm thấy tệp, lỗi quyền đọc `AccessDeniedException`, lỗi cú pháp properties) đều bị bỏ qua:
  ```java
  try {
      properties.load(input);
  } catch (Exception ignored) {
      properties.clear();
  }
  ```

#### b. Suy luận kỹ thuật (Inferences)
- Khi nhà vận hành đã chủ động chỉ định một đường dẫn tệp thông qua `-Dclinic.config.file=/path/to/db.properties` hoặc biến `CLINIC_DB_CONFIG_FILE`, điều đó thể hiện **ý đồ rõ ràng** rằng ứng dụng phải sử dụng cấu hình từ tệp này.
- Nếu tệp đó không tồn tại, bị sai quyền hạn hệ thống tệp (permissions), hoặc bị hỏng cú pháp mà ứng dụng vẫn lặng lẽ bỏ qua và khởi động tiếp bằng các giá trị ngầm định, ứng dụng có thể rơi vào trạng thái hoạt động sai lệch nghiêm trọng hoặc kết nối nhầm CSDL, vi phạm trực tiếp nguyên tắc phòng thủ theo chiều sâu và `fail-closed`.

#### c. Các phương án so sánh (Options)
- **Phương án 5A — Đóng khi Lỗi Tuyệt đối (Strict Fail-Closed for Explicit File):**  
  Nếu người dùng đã chỉ định file tường minh:
  - Nếu file không tồn tại: Ném lỗi `ConfigurationException("Tệp cấu hình chỉ định không tồn tại: [REDACTED_PATH_INFO]")` và dừng khởi động ngay.
  - Nếu không có quyền đọc (I/O error): Ném lỗi và dừng ngay.
  - Nếu file bị sai cú pháp (malformed): Ném lỗi và dừng ngay.  
  *Ưu điểm:* Tuân thủ nguyên tắc fail-closed; không bao giờ để ứng dụng chạy trong tình trạng cấu hình không như mong đợi.  
  *Nhược điểm:* Đòi hỏi phải quản lý quyền truy cập file cẩn thận trên máy trạm người dùng.
- **Phương án 5B — Cho phép Bỏ qua nếu có Biến Môi trường Thay thế (Permissive Fallback with Warning):**  
  Nếu đọc file lỗi nhưng các biến môi trường `DB_URL` và `DB_USERNAME` vẫn đầy đủ, ghi cảnh báo ra log và tiếp tục chạy.  
  *Ưu điểm:* Giúp ứng dụng bền bỉ hơn khi triển khai trên các môi trường hỗn hợp.  
  *Nhược điểm:* Tiềm ẩn rủi ro che giấu lỗi cấu hình; vi phạm nguyên tắc chỉ định tường minh.

#### d. Đánh đổi và Đề xuất khuyến nghị (Tradeoffs & Recommendation)
- **Đề xuất (Recommendation):** Áp dụng **Phương án 5A (Strict Fail-Closed)**: Khi một đường dẫn file cấu hình được chỉ định rõ ràng, mọi lỗi liên quan đến việc định vị, phân quyền đọc và giải mã file đó PHẢI làm ứng dụng dừng lại ngay lập tức (fail-closed), tuyệt đối không tự ý fallback.

---

### 3.6. Quyết định 6: Định hướng đối với Biến `DB_DRIVER` và Khóa `db.driver` (Unused Driver Configuration)

#### a. Sự thật (Facts)
- Hiện tại `DB_DRIVER` và `db.driver` được nạp vào bộ nhớ nhưng không được bất kỳ dòng mã nào sử dụng ([`DatabaseConnection.java:23-28, 76-86`](file:///Users/jtuong/Documents/ClinicOrchestrator/data/worktrees/clinic-desktop/aa79d279-df83-4507-a4a0-ef97af6c5250/clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java#L23-L28)).
- Trình điều khiển JDBC hiện đại (từ chuẩn JDBC 4.0 trở đi) tự động đăng ký với `DriverManager` thông qua cơ chế `META-INF/services/java.sql.Driver` nằm trong chính file JAR của driver (chẳng hạn `mssql-jdbc-12.6.1.jre11.jar`).
- Phương thức `autoRegisterDriver(url)` trong `DatabaseConnection.java:115-129` cũng đã tự động gọi `Class.forName()` cho 4 loại CSDL phổ biến (SQL Server, Postgres, MySQL, Oracle).

#### b. Suy luận kỹ thuật (Inferences)
- Việc duy trì biến `DB_DRIVER` trong tài liệu hướng dẫn và file mẫu `db.properties.example` tạo ra sự hiểu lầm cho người vận hành rằng biến này có tác dụng điều khiển driver, trong khi thực tế nó hoàn toàn vô dụng.
- Nếu người dùng truyền một driver tùy biến (ví dụ driver proxy, driver audit p6spy, driver giám sát APM) qua `DB_DRIVER`, ứng dụng hiện tại sẽ bỏ qua và không nạp driver đó, dẫn đến lỗi `No suitable driver found`.

#### c. Các phương án so sánh (Options)
- **Phương án 6A — Chính thức Khai tử và Loại bỏ (Deprecate and Remove):**  
  Đánh dấu deprecate và loại bỏ `DB_DRIVER` / `db.driver` khỏi file mẫu `db.properties.example`, README và bộ nạp cấu hình. Giao toàn quyền đăng ký driver cho cơ chế JDBC 4+ SPI.  
  *Ưu điểm:* Làm sạch mã nguồn; loại bỏ các trường thừa không có tác dụng; giảm tải gánh nặng kiểm thử.  
  *Nhược điểm:* Nếu trong tương lai cần hỗ trợ driver ngoài hoặc driver không có SPI, sẽ cần viết lại.
- **Phương án 6B — Hiện thực hóa Đầy đủ Chức năng (Activate Explicit Driver Loading):**  
  Nếu `driver` được cấu hình, trước khi kết nối, gọi `Class.forName(driver)` để nạp lớp driver đó một cách chủ động. Nếu nạp thất bại, báo lỗi `ClassNotFoundException` rõ ràng thay vì nuốt lỗi.  
  *Ưu điểm:* Tôn trọng cấu hình của người dùng; hỗ trợ tốt các driver tùy biến hoặc driver proxy (như P6Spy, AWS Secrets JDBC wrapper).  
  *Nhược điểm:* Thêm một nhánh xử lý ngoại lệ; có thể gây lỗi nếu người dùng điền sai tên class driver dù driver mặc định qua SPI vẫn hoạt động tốt.

#### d. Đánh đổi và Đề xuất khuyến nghị (Tradeoffs & Recommendation)
- **Đề xuất (Recommendation):** Áp dụng **Phương án 6A (Deprecate and Remove)** cho dự án hiện tại vì hệ thống được thiết kế cố định chạy với Microsoft SQL Server (hoặc các CSDL chuẩn đã được bao phủ bởi SPI và `autoRegisterDriver`). Việc loại bỏ trường này giúp mã nguồn gọn gàng, minh bạch và loại bỏ nợ kỹ thuật.

---

### 3.7. Quyết định 7: Cung cấp Trust-Anchor TLS Hợp chuẩn và Khử vết Bí mật (Compliant TLS Trust-Anchor Provisioning & Secret Redaction)

#### a. Sự thật (Facts)
- Tệp mẫu `db.properties.example:3` đang cấu hình:
  `db.url=jdbc:sqlserver://localhost:1433;databaseName=clinicDb;encrypt=true;trustServerCertificate=true`
- Tham số `trustServerCertificate=true` ra lệnh cho JDBC driver **tin tưởng mù quáng mọi chứng chỉ SSL/TLS** do SQL Server gửi về, hoàn toàn không kiểm tra chuỗi CA (Certificate Authority) và không kiểm tra tên miền máy chủ (`hostNameInCertificate`), khiến kết nối đối mặt với nguy cơ tấn công Xen giữa (Man-in-the-Middle - MitM).
- Yêu cầu bắt buộc của Owner: **TLS phải mã hóa và bắt buộc phải kiểm tra tính hợp lệ của chứng chỉ cùng định danh máy chủ (`encrypt=true; trustServerCertificate=false`), theo nguyên tắc fail-closed.**

#### b. Suy luận kỹ thuật (Inferences)
- Trong môi trường bệnh viện/phòng khám thực tế:
  - Máy chủ CSDL SQL Server thường sử dụng chứng chỉ số do PKI nội bộ (Enterprise Private CA) cấp phát, hoặc chứng chỉ số tự ký (Self-signed).
  - Để máy trạm Java Swing (`clinic-desktop.jar`) có thể xác thực được chứng chỉ này khi `trustServerCertificate=false`, JVM trên máy trạm bắt buộc phải được cung cấp **Điểm neo tin cậy (Trust-Anchor)**, tức là chứng chỉ Root CA / Intermediate CA của bệnh viện.
  - Nếu không có cơ chế cung cấp trust-anchor chuẩn hóa, ứng dụng sẽ bị từ chối kết nối ngay lập tức với lỗi `PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException`.

#### c. Các phương án cung cấp Trust-Anchor (Options for Trust-Anchor Provisioning)
- **Phương án 7A — Sử dụng JVM System Truststore Mặc định (`cacerts` của Hệ điều hành / Java):**  
  Quản trị viên hạ tầng cài đặt chứng chỉ Root CA của bệnh viện trực tiếp vào tệp `cacerts` chuẩn của JDK/JRE (`$JAVA_HOME/lib/security/cacerts`) trên từng máy trạm. Chuỗi JDBC URL chỉ cần đặt:  
  `encrypt=true;trustServerCertificate=false;hostNameInCertificate=db.clinic.local;`  
  *Ưu điểm:* Chuẩn mực cao nhất của môi trường doanh nghiệp; không cần nhúng đường dẫn hay mật khẩu truststore vào cấu hình ứng dụng; an toàn tuyệt đối.  
  *Nhược điểm:* Đòi hỏi quyền quản trị viên (Admin/Root) trên máy trạm khi cài đặt môi trường Java ban đầu.
- **Phương án 7B — Chỉ định Truststore Riêng qua Tham số Tiêu chuẩn của JVM:**  
  Khởi chạy ứng dụng với các tham số SSL chuẩn của Java:  
  `java -Djavax.net.ssl.trustStore=/etc/clinic/certs/clinic-truststore.p12 -Djavax.net.ssl.trustStorePassword=... -jar clinic-desktop.jar`  
  *Ưu điểm:* Không can thiệp vào `cacerts` chung của hệ điều hành; linh hoạt theo từng ứng dụng.  
  *Nhược điểm:* Mật khẩu truststore có thể hiển thị trong danh sách tiến trình (`ps aux`) trừ khi được bảo vệ.
- **Phương án 7C — Sử dụng Thuộc tính Cấu hình Riêng của Microsoft SQL Server JDBC Driver:**  
  Khai báo trực tiếp đường dẫn truststore trong chuỗi kết nối hoặc file properties:  
  `db.url=jdbc:sqlserver://db.clinic.local:1433;databaseName=clinicDb;encrypt=true;trustServerCertificate=false;trustStore=/etc/clinic/certs/clinic-truststore.p12;trustStorePassword=...`  
  *Ưu điểm:* Cô lập hoàn toàn trong lớp kết nối JDBC của SQL Server, không ảnh hưởng tới các kết nối TLS khác của JVM (ví dụ gọi REST API ngoài nếu có sau này).  
  *Nhược điểm:* Tham số `trustStorePassword` trở thành một bí mật cấu hình cần phải khử vết (`redact`) trong toàn bộ log, exception và toString của resolver.

#### d. Yêu cầu Khử vết Bí mật (Secret Redaction Safeguards)
Bất kể chọn phương án nào, để đảm bảo an ninh thông tin bệnh viện:
1. **Khử vết chuỗi kết nối và tham số TLS:** Mọi log lỗi, chẩn đoán khởi động và ngoại lệ JDBC ném ra EDT tuyệt đối không được chứa mật khẩu CSDL hoặc `trustStorePassword`.
2. **Cấm dùng dữ liệu thật trong kiểm thử:** Toàn bộ test tự động chỉ sử dụng các chuỗi giả lập danh định (`synthetic-truststore-marker`, `synthetic-password-marker`).
3. **Cơ chế chốt chặn (Enforcement Gate) trong Resolver:** Đề xuất bổ sung một validator trong `DatabaseConfigurationResolver`: Nếu phát hiện URL chứa `trustServerCertificate=true` ở profile sản xuất (`production`), resolver lập tức từ chối và cảnh báo vi phạm chính sách an ninh SEC-01.

#### e. Đánh đổi và Đề xuất khuyến nghị (Tradeoffs & Recommendation)
- **Đề xuất (Recommendation):** 
  - Khuyến nghị **Phương án 7A (Standard JVM Truststore)** làm kênh chính sách tiêu chuẩn cho triển khai phòng khám, kết hợp hỗ trợ **Phương án 7B** qua script khởi chạy.
  - Trong chuỗi JDBC, bắt buộc chuẩn hóa: `encrypt=true;trustServerCertificate=false;hostNameInCertificate=<FQDN_CUA_MAY_CHU_DB>`.
  - Nghiêm cấm đưa `trustServerCertificate=true` vào môi trường vận hành thực tế.

---

## 4. Danh mục Câu hỏi Trực tiếp Xin Ý kiến Chỉ đạo của Owner (Exact Owner Questions)

Để không tự ý biến các suy luận và khuyến nghị thành chính sách dự án khi chưa có phê duyệt, dưới đây là danh sách **các câu hỏi quyết định cụ thể** kính trình Owner xem xét và phê duyệt chính thức:

### Câu hỏi 1: Về Giao diện Chỉ định File Cấu hình Ngoài
> *"Owner có đồng ý phê duyệt việc sử dụng đồng thời cả System Property `-Dclinic.config.file=<path>` và Biến môi trường `CLINIC_DB_CONFIG_FILE=<path>` để chỉ định file properties ngoài cho packaged JAR, với cam kết tuyệt đối không tự động dò tìm ngầm định tại thư mục làm việc (CWD) hay thư mục chứa JAR hay không?"*

### Câu hỏi 2: Về Thứ tự Ưu tiên giữa `DB_CONNECTION_STRING` và Biến Môi trường Phân tách
> *"Khi xảy ra xung đột cấu hình, Owner có đồng ý áp dụng quy tắc phân tầng: Biến môi trường phân tách (`DB_URL`, `DB_PASSWORD`) có quyền ghi đè lên chuỗi kết nối đầy đủ `db.connectionString` được khai báo trong file properties nền hay không?"*

### Câu hỏi 3: Về Ngữ nghĩa của Biến Môi trường Rỗng (Blank Semantics)
> *"Owner có đồng ý nguyên tắc an toàn: Biến môi trường `DB_URL` hoặc `DB_CONNECTION_STRING` nếu được khai báo nhưng để trống/chỉ có khoảng trắng thì phải bị coi là Không hợp lệ (Invalid - Fail Closed) và chặn khởi động ngay lập tức, thay vì âm thầm bỏ qua để lấy giá trị trong file hay không?"*

### Câu hỏi 4: Về Ràng buộc Cặp đôi Tài khoản / Mật khẩu (Credential Pairs)
> *"Owner có đồng ý quy định chặt chẽ: Đối với xác thực CSDL dạng phân tách, bắt buộc cả `username` và `password` phải cùng có mặt (hoặc cùng vắng mặt đối với Windows Integrated Authentication), việc chỉ cung cấp một trong hai trường sẽ bị coi là lỗi cấu hình thiếu sót và từ chối kết nối hay không?"*

### Câu hỏi 5: Về Hành vi khi File Chỉ định Bị Lỗi (File Load Failures)
> *"Owner có phê duyệt nguyên tắc Đóng khi Lỗi (Fail-Closed): Nếu người dùng/quản trị viên đã chỉ định rõ đường dẫn file cấu hình qua `-Dclinic.config.file` hoặc `CLINIC_DB_CONFIG_FILE`, nhưng file đó không tồn tại, không đọc được hoặc sai cú pháp, ứng dụng PHẢI dừng khởi động ngay lập tức, tuyệt đối không âm thầm chạy tiếp bằng giá trị môi trường hay không?"*

### Câu hỏi 6: Về Định hướng Xử lý `DB_DRIVER`
> *"Owner có đồng ý loại bỏ hoàn toàn biến `DB_DRIVER` và khóa `db.driver` khỏi tài liệu hướng dẫn, file mẫu và bộ giải quyết cấu hình để đơn giản hóa hệ thống và dựa hoàn toàn vào cơ chế nạp tự động JDBC 4+ SPI của `mssql-jdbc` hay không?"*

### Câu hỏi 7: Về Phương án Cung cấp Điểm neo Tin cậy TLS (Trust-Anchor Provisioning)
> *"Owner có phê duyệt định hướng hạ tầng: Máy trạm triển khai sẽ sử dụng chứng chỉ CA được cài đặt sẵn vào Java Truststore chuẩn (`cacerts` hoặc `-Djavax.net.ssl.trustStore`), kết hợp tham số bắt buộc `encrypt=true;trustServerCertificate=false` trong chuỗi kết nối, và từ chối mọi cấu hình chấp nhận chứng chỉ không kiểm tra hay không?"*

---

## 5. Nhật ký Thực thi và Đối soát Hướng dẫn (Guidance & Verification Record)

### 5.1. Báo cáo Thực thi RAG (RAG Execution Attempt)
- **Lệnh thực thi ban đầu:**
  ```bash
  /Users/jtuong/Documents/ClinicOrchestrator/.venv/bin/python /Users/jtuong/Documents/ClinicOrchestrator/scripts/rag_search.py --repo clinic-desktop --limit 5 'database configuration db.properties DB_URL DB_CONNECTION_STRING TLS truststore packaged jar'
  ```
- **Kết quả thực tế / Trạng thái:** Lệnh bị chặn bởi chính sách sandbox của môi trường (`Permission denied for command ... Matches user-configured deny rule`).
- **Xử lý bù đắp:** Tương tự như các tác vụ đã thực hiện thành công trước đó trong dự án, toàn bộ ngữ cảnh RAG cốt lõi, hợp đồng đã duyệt `PHASE0-PHASE3-REVIEW-AND-TRACKING-DOCS-01`, và các trích đoạn tài liệu nền tảng đã được Orchestrator cung cấp đầy đủ trong task package. Mọi trích dẫn mã nguồn và số dòng đã được xác minh trực tiếp bằng công cụ đọc tệp độc lập trên hệ thống tệp worktree.

### 5.2. Danh sách Tài liệu Hướng dẫn Bắt buộc Đã đọc (Guidance Files Read)
1. Orchestrator baseline: `docs/PROJECT_CONVENTIONS.md` (cung cấp trong ngữ cảnh task).
2. `README.md` (ngữ cảnh task và dòng 1-19 tại worktree).
3. `DOMAIN_ANALYSIS.md` (ngữ cảnh task).
4. `ERD_DIAGRAM.md` (ngữ cảnh task).
5. `docs/ke-hoach-trien-khai/phase3-transfer-linked-work-eligibility-review.md` (retrieved passage).
6. `docs/ke-hoach-trien-khai/phase3-transfer-reason-policy-proposal.md` (retrieved passage).
7. `docs/ke-hoach-trien-khai/phase3-transfer-domain-invariant-design.md` (retrieved passage).
8. `docs/ke-hoach-trien-khai/phase3-transfer-queue-allocation-proposal.md` (retrieved passage).
9. `docs/ke-hoach-trien-khai/phase3-transfer-queue-history-persistence-proposal.md` (retrieved passage).
10. `docs/ke-hoach-trien-khai/phase0-domain-events-review.md` (retrieved passage).
11. `docs/ke-hoach-trien-khai/03-model-task-board.md` (đọc trực tiếp worktree).
12. `docs/ke-hoach-trien-khai/phase0-db-config-resolution-contract.md` (đọc trực tiếp worktree).
13. `docs/ke-hoach-trien-khai/phase0-db-config-review.md` (đọc trực tiếp worktree).

### 5.3. Tuân thủ Ranh giới và Phạm vi Tệp (Scope & Boundary Compliance)
- **Tệp duy nhất được tạo mới:** `docs/ke-hoach-trien-khai/phase0-db-config-remaining-decisions-proposal.md`.
- **Không chỉnh sửa:** Không có bất kỳ tệp mã nguồn Java, test, file cấu hình, POM, script hay migration nào bị thay đổi. Script `run-app.sh` được bảo toàn nguyên vẹn.
- **An toàn dữ liệu:** Không truy cập cơ sở dữ liệu thật; không sử dụng thông tin bệnh nhân hay thông tin định danh thật. Mọi ví dụ và kiểm thử tham chiếu đều sử dụng dữ liệu giả lập danh định (`synthetic`).
