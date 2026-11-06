# Phase 0: Packaged-JAR Database Configuration — Remaining Decisions Proposal
## Đề xuất Quyết định Kỹ thuật: Hoàn thiện Cấu hình Cơ sở Dữ liệu cho Artifact Đóng gói (JAR)

> **Tài liệu đề xuất kỹ thuật (Non-binding technical proposal)**  
> **Mã hợp đồng tham chiếu:** `PHASE0-PHASE3-REVIEW-AND-TRACKING-DOCS-01`  
> **Trạng thái:** Dự thảo đề xuất kỹ thuật — Chờ phê duyệt của Owner (DRAFT / NON-BINDING)  
> **Mục tiêu:** Cung cấp cơ sở phân tích độc lập, đối soát mã nguồn và so sánh phương án cho các chi tiết cấu hình CSDL còn lại của artifact đóng gói `clinic-desktop.jar` thuộc Phase 0, không tự ý biến khuyến nghị thành quy định chính thức.

---

## 1. Bối cảnh và Các Tiền đề đã được Owner phê duyệt (Baseline & Approved Facts)

Tài liệu này kế thừa hợp đồng `PHASE0-PHASE3-REVIEW-AND-TRACKING-DOCS-01` và các báo cáo đối soát trước đó ([`phase0-db-config-review.md`](phase0-db-config-review.md), [`phase0-db-config-resolution-contract.md`](phase0-db-config-resolution-contract.md)). Các quyết định Owner hiện hành được ghi tại [`phase0-deployment-config-tls-decision-proposal.md`](phase0-deployment-config-tls-decision-proposal.md), theo `PHASE0-DB-DEPLOY-DECISION-RECON-02` và `PHASE0-DB-CONFIG-SOURCE-PRECEDENCE-OWNER-DECISION-01`:

1. **Bắt buộc hỗ trợ song song hai kênh nạp cấu hình:** Triển khai desktop dạng packaged-JAR bắt buộc phải hỗ trợ cả hai nguồn: file properties bên ngoài (`external properties file`) và biến môi trường/secret injection (`environment variables`).
2. **Quy tắc ưu tiên cấp trường (Per-key Environment Precedence):** Biến môi trường có quyền ưu tiên cao hơn trên từng trường đơn lẻ (`per-key environment wins`); file properties đóng vai trò là giá trị nền và bù đắp các trường còn thiếu (`file fills keys absent from environment`).
3. **Chính sách TLS bắt buộc mã hóa và xác thực chứng chỉ (Mandatory Certificate-Validating TLS):** Kết nối CSDL bắt buộc mã hóa và xác thực cả chứng chỉ lẫn danh tính máy chủ. Chứng chỉ không hợp lệ, không được tin cậy, hết hạn hoặc không khớp danh tính máy chủ phải làm kết nối thất bại (fail-closed); không fallback hoặc bypass theo môi trường. `encrypt=true;trustServerCertificate=false` là một cách biểu đạt JDBC SQL Server có thể dùng để hiện thực kết quả đã duyệt, không phải URL hay phương án truststore đã chốt; vẫn cần chứng minh đáp ứng đầy đủ chính sách.
4. **Phạm vi tài liệu:** `SEC-01` tiếp tục OPEN; các workflow hạ nguồn còn bị chặn tiếp tục DEFAULT-DENY. Không thực hiện thay đổi mã nguồn Java, bộ kiểm thử, schema migration, phân quyền hoặc runtime. Giữ nguyên script `run-app.sh`.

---

## 2. Bằng chứng Mã nguồn và Hiện trạng Kiến trúc (Source Evidence & Proven Facts)

Dựa trên kiểm tra mã nguồn tại repository:

### 2.1. Đóng gói Artifact và Loại trừ Bí mật (Packaging Evidence)
- Trong [`clinic-desktop/pom.xml:46-56`](../../clinic-desktop/pom.xml#L46-L56), cấu hình `maven-shade-plugin` thiết lập bộ lọc loại trừ tệp có tên `db.properties` khỏi tệp JAR phân phối (`<exclude>db.properties</exclude>`), đồng thời loại bỏ chữ ký `META-INF/*.SF`.
- Tệp mẫu [`clinic-infrastructure/src/main/resources/db.properties.example:1-7`](../../clinic-infrastructure/src/main/resources/db.properties.example#L1-L7) vẫn được đóng gói trong JAR, nhưng chỉ chứa placeholder và tham số mẫu cục bộ (`encrypt=true;trustServerCertificate=true`).

### 2.2. Trình nạp Cấu hình Hiện tại (Loader & Resolver Implementation)
- Trong [`clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java:29-41`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java#L29-L41), phương thức `loadProperties()` chỉ thực hiện nạp tệp tài nguyên từ classpath thông qua `DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties")`. Lớp này hoàn toàn không có tham số hay cơ chế nạp file từ hệ thống tệp bên ngoài (`filesystem path`). Mọi lỗi đọc hoặc định dạng tệp đều bị nuốt lặng lẽ (`catch (Exception ignored) { properties.clear(); }`).
- Trong [`clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java:22-35`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L22-L35), phương thức `resolve()` xử lý ưu tiên giữa `Properties` và `Map<String, String> environment`:
  - `override()` ([dòng 80-82](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L80-L82)): nếu biến môi trường không rỗng (`nonblank`), giá trị môi trường sau khi cắt khoảng trắng thừa (`trim()`) sẽ ghi đè giá trị trong property.
  - `nonblank()` ([dòng 84-86](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L84-L86)): kiểm tra `value != null && !value.isBlank()`. Các biến môi trường rỗng hoặc chỉ có khoảng trắng được coi là vắng mặt (`absent`) và bị bỏ qua, để lộ giá trị trong properties.
  - Biến môi trường `DB_CONNECTION_STRING` ([dòng 26, 32-34](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L26-L34)) được giữ nguyên vẹn và giành quyền ưu tiên tuyệt đối khi xác định `ConnectionTarget` ([dòng 103-106](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L103-L106)).

### 2.3. Trạng thái Trường `DB_DRIVER` và Đăng ký Driver
- Trường `DB_DRIVER` và khóa `db.driver` được đọc và gán vào `ResolvedConfiguration.driver()` ([`DatabaseConfiguration.java:31`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L31)), nhưng không hề được tham chiếu hay sử dụng trong luồng mở kết nối ([`DatabaseConnection.java:76-98`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java#L76-L98)).
- Trình quản lý kết nối tự suy luận driver thông qua tiền tố chuỗi URL bằng phương thức `autoRegisterDriver(url)` ([`DatabaseConnection.java:115-129`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java#L115-L129)) hoặc cơ chế `ServiceLoader` chuẩn của JDBC 4.0+.

### 2.4. Khử vết Dữ liệu Nhạy cảm (Redaction)
- Các record nội bộ `ResolvedConfiguration` và `ConnectionTarget` đã cài đè phương thức `toString()` ([`DatabaseConfiguration.java:94-101, 118-125`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L94-L125)) để chỉ in trạng thái hiện diện của giá trị và chế độ kết nối, không in URL hay mật khẩu trong hai biểu diễn này. Điều đó không chứng minh mọi log hoặc ngoại lệ ở các caller đều đã được khử vết.
- Các bài kiểm thử [`DatabaseConnectionTest.java:76-114`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnectionTest.java#L76-L114) và [`DatabaseConfigurationTest.java:87-101`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfigurationTest.java#L87-L101) kiểm tra diagnostic của `testConnection` và hai biểu diễn record bằng dữ liệu tổng hợp, gồm `synthetic-password-marker`, `synthetic-secret-marker`. Phạm vi kiểm tra này không bảo đảm an toàn cho mọi đường log/lỗi.

---

## 3. Phân tích So sánh Các Quyết định Kỹ thuật Còn lại (Options, Tradeoffs & Recommendations)

Phần này phân tách tường minh giữa **Sự thật đã chứng minh (Facts)**, **Suy luận kỹ thuật (Inferences)**, **Các phương án (Options)**, **Đánh đổi (Tradeoffs)**, và **Đề xuất khuyến nghị phi ràng buộc (Non-binding Recommendation)**.

Các lựa chọn và khuyến nghị dưới đây đều chờ quyết định triển khai; từ ngữ yêu cầu bên trong một phương án chỉ mô tả hành vi được đề xuất. Chúng không bổ sung chính sách đã duyệt ở mục 1. Quy tắc env-over-file theo từng khóa đã được chấp thuận; thứ tự combined/split, ngữ nghĩa blank/partial, giao diện file, lỗi đọc file, driver và cách hiện thực TLS vẫn chưa chốt.

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
- Mã nguồn hiện hành chỉ nạp `db.properties` từ classpath ([`DatabaseConnection.java:31`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java#L31)).
- Tệp JAR đóng gói loại trừ `db.properties` ([`clinic-desktop/pom.xml:49`](../../clinic-desktop/pom.xml#L49)). Do đó, khi chạy packaged JAR ngoài môi trường phát triển, loader hiện tại không đọc file properties ngoài classpath; đây chưa phải bằng chứng về mọi cách thiết lập classpath khi triển khai.

#### b. Suy luận kỹ thuật (Inferences)
- Việc tự động dò tìm file ở thư mục làm việc hiện tại (`Current Working Directory - CWD`) hoặc cùng thư mục với JAR (`Adjacent-JAR discovery`) có thể chọn nhầm cấu hình nếu nội dung hay quyền ghi tại đó không được kiểm soát:
  - Người dùng mở ứng dụng từ shortcut, desktop hoặc terminal khác nhau có thể vô tình nạp một file cấu hình lạ nằm tại CWD.
  - Nếu người khác có quyền ghi vào thư mục được dò, họ có thể thay nội dung file được chọn. Đây là tình huống giả định cần xét theo quyền truy cập thực tế, không phải kết luận đã xác minh về lỗ hổng hay phân loại CWE của ứng dụng hiện tại.
  - [Quyết định Owner hiện hành](phase0-deployment-config-tls-decision-proposal.md) yêu cầu giao diện/đường dẫn tường minh và không phê duyệt implicit adjacent-JAR/CWD discovery; giao diện cụ thể vẫn chưa chốt.

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
  4. Giữ ranh giới đã duyệt: không tự dò `./db.properties` tại CWD hay cùng thư mục với JAR; tên và thứ tự hai giao diện trên vẫn chỉ là đề xuất.

---

### 3.2. Quyết định 2: Thứ tự Ưu tiên giữa `DB_CONNECTION_STRING` và Bộ Ba Trường Phân tách (Combined vs Split Precedence)

#### a. Sự thật (Facts)
- Hiện tại, [`DatabaseConfiguration.java:26-34, 103-106`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L26-L34) (xem thêm [103-106](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L103-L106)) cho phép `DB_CONNECTION_STRING` (khi khác rỗng) giành quyền ưu tiên tuyệt đối trước bộ ba trường `url`, `username`, `password`.
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
- Trong [`DatabaseConfiguration.java:80-86`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L80-L86), hàm `override` sử dụng `nonblank(environmentValue)`:
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
  *Ưu điểm:* Linh hoạt với các script shell khởi chạy có định nghĩa sẵn biến nhưng chưa gán giá trị; không làm thay đổi các test hiện có ([`DatabaseConfigurationTest.java:45-60`](../../clinic-infrastructure/src/test/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfigurationTest.java#L45-L60)).\
  *Nhược điểm:* Nguy cơ âm thầm dùng giá trị fallback của file khi secret injection bị hỏng; không có cách nào dùng biến môi trường để "xóa" (clear) một giá trị đã lỡ đặt trong file properties.
- **Phương án 3B — Kiểm soát Nghiêm ngặt (Fail-Closed: Blank == Invalid / Error):**  
  Nếu một biến môi trường cấu hình DB quan trọng (`DB_URL`, `DB_CONNECTION_STRING`, `DB_USERNAME`) được cung cấp nhưng có giá trị rỗng/khoảng trắng, coi đó là cấu hình sai và ném lỗi khởi động ngay lập tức.  
  *Ưu điểm:* Phát hiện giá trị rỗng ở các khóa được kiểm tra; cần xác định rõ khóa và trường hợp hợp lệ.\
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
- Trong [`DatabaseConfiguration.java:107-113`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L107-L113):
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
  1. **SQL Server Authentication:** Cần xác định các tổ hợp username/password được hỗ trợ trước khi áp dụng quy tắc cặp đôi.
  2. **Integrated Security (Windows Authentication):** `integratedSecurity=true` là ví dụ tùy chọn cần đối chiếu với driver/runtime và cơ chế xác thực được chọn; không suy ra quy tắc credentials chỉ từ tên tùy chọn.
  3. **Azure Active Directory / Managed Identity:** URL chứa `authentication=ActiveDirectoryIntegrated` hoặc `authentication=ActiveDirectoryMSI`.
- Cấu hình chỉ có username hoặc chỉ có password có thể do thiếu một biến hoặc là có chủ đích. Cần xác định tổ hợp hợp lệ và cách báo lỗi cho từng chế độ xác thực; nguồn hiện tại không chứng minh ý định của người vận hành.

#### c. Các phương án so sánh (Options)
- **Phương án 4A — Nguyên tắc Cặp Đôi Bắt buộc (Strict Credential Pair Requirement):**  
  Đối với cấu hình phân tách (`split configuration`): `username` và `password` bắt buộc phải cùng xuất hiện (cả hai đều có giá trị) HOẶC cùng vắng mặt (cả hai đều `null`/rỗng). Nếu chỉ có một trong hai trường có giá trị, bộ giải quyết lập tức từ chối và báo lỗi `IncompleteCredentialsException`.  
  *Ưu điểm:* Có thể phát hiện cặp credentials bị thiếu một trường theo quy tắc đã chọn.\
  *Nhược điểm:* Có thể từ chối tổ hợp credentials có chủ đích; cần đối chiếu với các chế độ xác thực được hỗ trợ. Tài liệu này không xác lập quy định về tài khoản không mật khẩu.
- **Phương án 4B — Nhận diện theo Chế độ Xác thực của URL (Authentication-Mode-Aware Validation):**  
  Kiểm tra chuỗi URL:
  - Nếu URL chứa `integratedSecurity=true` hoặc `authentication=ActiveDirectory...`: Bắt buộc cả username và password phải vắng mặt. Nếu có truyền credentials, báo lỗi cấu hình thừa.
  - Nếu URL không chứa các tham số trên: Áp dụng quy tắc cặp đôi bắt buộc như Phương án 4A.  
  *Ưu điểm:* Có thể diễn đạt validation theo chế độ xác thực; quy tắc nhận diện và tổ hợp credentials cần được kiểm chứng với driver được chọn.\
  *Nhược điểm:* Bộ giải quyết cần phân tích cú pháp chuỗi URL (URL parsing), làm tăng độ phức tạp của resolver.

#### d. Đánh đổi và Đề xuất khuyến nghị (Tradeoffs & Recommendation)
- **Đề xuất (Recommendation):** Áp dụng **Phương án 4A (Strict Credential Pair Requirement)** ở mức tầng cấu hình cơ sở (resolver). Quy tắc này đơn giản ở tầng resolver; cần xác nhận tương thích với các chế độ xác thực trước khi triển khai.

---

### 3.5. Quyết định 5: Ứng xử khi File Cấu hình Chỉ định Bị Lỗi hoặc Không Đọc được (Unreadable/Malformed File Behavior)

#### a. Sự thật (Facts)
- Trong [`DatabaseConnection.java:29-41`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java#L29-L41), resource classpath vắng mặt được bỏ qua và ngoại lệ khi đọc/parse được catch. Đây chưa phải cơ chế đọc file ngoài hay bằng chứng về lỗi quyền trên một đường dẫn ngoài:
  ```java
  try {
      properties.load(input);
  } catch (Exception ignored) {
      properties.clear();
  }
  ```

#### b. Suy luận kỹ thuật (Inferences)
- Khi nhà vận hành đã chủ động chỉ định một đường dẫn tệp thông qua `-Dclinic.config.file=/path/to/db.properties` hoặc biến `CLINIC_DB_CONFIG_FILE`, điều đó thể hiện **ý đồ rõ ràng** rằng ứng dụng phải sử dụng cấu hình từ tệp này.
- Nếu tệp đó không tồn tại, không đọc được hoặc hỏng cú pháp mà ứng dụng vẫn bỏ qua và dùng cấu hình khác, ứng dụng có thể kết nối nhầm CSDL. Đây là rủi ro vận hành giả định; fail-closed khi đọc file được chọn vẫn là phương án chờ quyết định, khác với kết quả fail-closed TLS đã duyệt.

#### c. Các phương án so sánh (Options)
- **Phương án 5A — Đóng khi Lỗi Tuyệt đối (Strict Fail-Closed for Explicit File):**  
  Nếu người dùng đã chỉ định file tường minh:
  - Nếu file không tồn tại: Ném lỗi `ConfigurationException("Tệp cấu hình chỉ định không tồn tại: [REDACTED_PATH_INFO]")` và dừng khởi động ngay.
  - Nếu không có quyền đọc (I/O error): Ném lỗi và dừng ngay.
  - Nếu file bị sai cú pháp (malformed): Ném lỗi và dừng ngay.  
  *Ưu điểm:* Dừng khi phát hiện lỗi đọc/parse file được chọn; không bảo đảm phát hiện mọi cấu hình sai.\
  *Nhược điểm:* Đòi hỏi phải quản lý quyền truy cập file cẩn thận trên máy trạm người dùng.
- **Phương án 5B — Cho phép Bỏ qua nếu có Biến Môi trường Thay thế (Permissive Fallback with Warning):**  
  Nếu đọc file lỗi nhưng các biến môi trường `DB_URL` và `DB_USERNAME` vẫn đầy đủ, ghi cảnh báo ra log và tiếp tục chạy.  
  *Ưu điểm:* Giúp ứng dụng bền bỉ hơn khi triển khai trên các môi trường hỗn hợp.  
  *Nhược điểm:* Có thể che giấu lỗi file đã được chỉ định; cần Owner quyết định có cho phép fallback này hay không.

#### d. Đánh đổi và Đề xuất khuyến nghị (Tradeoffs & Recommendation)
- **Đề xuất chờ quyết định (Recommendation):** Chọn **Phương án 5A (Strict Fail-Closed)** để dừng khi không tìm thấy, không đọc được hoặc không parse được file đã chỉ định. Đây chưa phải hành vi runtime hay chính sách lỗi file được phê duyệt.

---

### 3.6. Quyết định 6: Định hướng đối với Biến `DB_DRIVER` và Khóa `db.driver` (Unused Driver Configuration)

#### a. Sự thật (Facts)
- Hiện tại `DB_DRIVER` và `db.driver` được nạp vào cấu hình ([`DatabaseConfiguration.java:31`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConfiguration.java#L31)) nhưng không được luồng mở kết nối sử dụng ([`DatabaseConnection.java:76-98`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java#L76-L98)).
- Trình điều khiển JDBC hiện đại (từ chuẩn JDBC 4.0 trở đi) tự động đăng ký với `DriverManager` thông qua cơ chế `META-INF/services/java.sql.Driver` nằm trong chính file JAR của driver (chẳng hạn `mssql-jdbc-12.6.1.jre11.jar`).
- Phương thức `autoRegisterDriver(url)` trong [`DatabaseConnection.java:115-129`](../../clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseConnection.java#L115-L129) cũng đã tự động gọi `Class.forName()` cho 4 loại CSDL phổ biến (SQL Server, Postgres, MySQL, Oracle).

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

### 3.7. Quyết định 7: Cung cấp Trust-Anchor TLS và Khử vết Bí mật (TLS Trust-Anchor Provisioning & Secret Redaction)

#### a. Sự thật (Facts)
- [Tệp mẫu `db.properties.example:3`](../../clinic-infrastructure/src/main/resources/db.properties.example#L3) đang cấu hình:
  `db.url=jdbc:sqlserver://localhost:1433;databaseName=clinicDb;encrypt=true;trustServerCertificate=true`
- [Quyết định Owner hiện hành](phase0-deployment-config-tls-decision-proposal.md) không phê duyệt `trustServerCertificate=true`, tắt mã hóa, fallback theo mặc định driver hoặc bypass theo môi trường. Tệp mẫu không phải chính sách triển khai.
- **Kết quả bắt buộc đã duyệt:** Mã hóa truyền tải; xác thực chứng chỉ và danh tính máy chủ; fail-closed với chứng chỉ không hợp lệ, không được tin cậy, hết hạn hoặc không khớp danh tính. **Cách hiện thực chưa chốt:** `encrypt=true;trustServerCertificate=false` là một cách biểu đạt có thể dùng với SQL Server JDBC; riêng chuỗi tùy chọn này chưa chứng minh kết quả TLS đã được đáp ứng. Cách dựng URL, truststore và xác thực hostname cần được chọn và kiểm chứng riêng.

#### b. Suy luận kỹ thuật (Inferences)
- Nếu triển khai chọn CA nội bộ, cần xác định cách cung cấp trust-anchor phù hợp cho runtime/driver được chọn. Không mặc định mọi phòng khám đều dùng CA nội bộ hay chứng chỉ tự ký.
- Thiếu trust-anchor phù hợp có thể làm xác thực thất bại. Không khẳng định một thông báo lỗi cụ thể sẽ xuất hiện trong mọi runtime; chưa có kiểm thử handshake trong phạm vi tài liệu này.

#### c. Các phương án cung cấp Trust-Anchor (Options for Trust-Anchor Provisioning)
Các ví dụ dưới đây chỉ dùng placeholder; chưa phải lệnh triển khai đã duyệt. Cần kiểm chứng với phiên bản JDK/JDBC, loại truststore và cách cấp bí mật được chọn.

- **Phương án 7A — Truststore mặc định của Java/JVM (`cacerts`):**\
  Quản trị trust-anchor trong truststore của bản phân phối JDK/JRE đang dùng, ví dụ `cacerts` tại `$JAVA_HOME/lib/security/cacerts`. `cacerts` ở đây là truststore của Java/JVM; không đồng nhất nó với OS trust store hoặc giả định cập nhật OS trust store sẽ tự cập nhật Java. Cần kiểm tra cấu hình runtime thực tế. Ví dụ tùy chọn JDBC có thể xem xét:\
  `encrypt=true;trustServerCertificate=false;hostNameInCertificate=db.clinic.local;`  
  *Ưu điểm:* Có thể quản trị trust-anchor cùng môi trường Java mà không đặt đường dẫn truststore trong URL ứng dụng.\
  *Nhược điểm:* Cần quản lý quyền sửa, cập nhật JDK và vòng đời chứng chỉ trên từng máy; không bảo đảm an toàn chỉ nhờ chọn `cacerts`.
- **Phương án 7B — Truststore riêng qua tham số JVM:**\
  Ví dụ:\
  `java -Djavax.net.ssl.trustStore=/etc/clinic/certs/clinic-truststore.p12 -Djavax.net.ssl.trustStorePassword=<synthetic-truststore-marker> -jar clinic-desktop.jar`\
  *Ưu điểm:* Có thể dùng truststore riêng mà không sửa `cacerts` chung của Java.\
  *Nhược điểm:* Cần xác định phạm vi ảnh hưởng trong JVM và cách cấp mật khẩu tránh lộ qua dòng lệnh/diagnostic; ví dụ này không phải hướng dẫn cấp secret cho triển khai thực tế.
- **Phương án 7C — Thuộc tính truststore của SQL Server JDBC:**\
  Ví dụ URL cần kiểm chứng trước khi triển khai:\
  `db.url=jdbc:sqlserver://db.clinic.local:1433;databaseName=clinicDb;encrypt=true;trustServerCertificate=false;trustStore=/etc/clinic/certs/clinic-truststore.p12;trustStorePassword=<synthetic-truststore-marker>`\
  *Ưu điểm:* Có thể chỉ định truststore tại cấu hình JDBC thay vì cấu hình JVM chung.\
  *Nhược điểm:* Cần kiểm chứng phạm vi áp dụng của driver và bảo vệ/khử vết `trustStorePassword` khi URL đi qua resolver, log và exception; không suy ra mức cô lập tuyệt đối.

#### d. Khử vết Bí mật và Đề xuất Kiểm chứng (Secret Redaction & Proposed Verification)
1. Không đưa mật khẩu CSDL hoặc `trustStorePassword` vào log/chẩn đoán. Bằng chứng hiện có ở mục 2.4 chỉ bao phủ các đường đã nêu; cần kiểm chứng riêng các đường lỗi sau khi chọn cách triển khai.
2. Kiểm thử tương lai chỉ dùng marker tổng hợp như `synthetic-truststore-marker`, `synthetic-password-marker`; không dùng dữ liệu thật.
3. Có thể xem xét validation cấu hình trước JDBC handoff; vị trí validator, cách parse và thông báo lỗi chưa chốt. Bất kỳ cách hiện thực nào cũng phải giữ kết quả TLS đã duyệt cho mọi nguồn cấu hình và môi trường được hỗ trợ; không giới hạn chính sách vào một profile `production` tự đặt, không coi validation URL là bằng chứng handshake.

#### e. Đánh đổi và Đề xuất khuyến nghị (Tradeoffs & Recommendation)
- Đề xuất xem xét **7A**, kết hợp **7B** khi cần truststore riêng; **7C** vẫn là lựa chọn để đánh giá. Chưa chọn phương án nào làm chính sách tiêu chuẩn.
- `encrypt=true;trustServerCertificate=false` và `hostNameInCertificate=<FQDN_CUA_MAY_CHU_DB>` là ví dụ JDBC để đánh giá; URL, cách xác thực hostname và truststore cụ thể vẫn non-binding. Chọn và kiểm thử riêng để chứng minh mã hóa, xác thực chứng chỉ/danh tính và fail-closed.
- Không phương án nào được nới lỏng chính sách TLS đã duyệt hoặc sử dụng `trustServerCertificate=true` làm bypass.

---

## 4. Danh mục Câu hỏi Trực tiếp Xin Ý kiến Chỉ đạo của Owner (Exact Owner Questions)

Giữ nguyên bảy câu hỏi gốc dưới đây để Owner xem xét các chi tiết còn lại. Các câu hỏi không mở lại kết quả TLS hay env-over-file per-key đã duyệt. Riêng câu 7, truststore, URL và tham số JDBC cụ thể vẫn là ví dụ chờ chọn cách triển khai; chữ “bắt buộc” trong câu hỏi không có nghĩa chuỗi tùy chọn đó đã được phê duyệt làm cú pháp duy nhất.

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

### 5.1. RAG-first — Vòng sửa tài liệu 2026-11-06

Trước khi mở tài liệu repository hoặc chạy `rg`/`grep`, đã thực thi:

```bash
/Users/jtuong/Documents/ClinicOrchestrator/.venv/bin/python /Users/jtuong/Documents/ClinicOrchestrator/scripts/rag_search.py "Phase 0 cấu hình DB TLS JDBC truststore cacerts env-over-file remaining decisions proposal portable citations" --limit 5
```

Lệnh thoát **1**, `sqlite3.OperationalError: unable to open database file`, tại bước mở index/configuration của Orchestrator (`PRAGMA journal_mode=WAL`); không trả về excerpt mới. Dùng các retrieved excerpts được cung cấp trong task làm hướng dẫn chính, sau đó đối chiếu quyết định Owner và source hiện tại. Không thay đổi index/cache, không né sandbox hoặc kết nối clinic DB.

### 5.2. Tài liệu Hướng dẫn Đã đọc trong Vòng sửa này

- Orchestrator baseline `docs/PROJECT_CONVENTIONS.md` (nội dung được cung cấp; không có bản trong checkout).
- [README.md](../../README.md); [DOMAIN_ANALYSIS.md](../../DOMAIN_ANALYSIS.md) và [ERD_DIAGRAM.md](../../ERD_DIAGRAM.md) (hai tài liệu sau: phần hướng dẫn được cung cấp, đã bị cắt theo giới hạn task).
- Các retrieved passages được cung cấp: [proposal hiện tại](phase0-db-config-remaining-decisions-proposal.md), [linked-work eligibility](phase3-transfer-linked-work-eligibility-review.md), [transfer-reason proposal](phase3-transfer-reason-policy-proposal.md), [domain-invariant design](phase3-transfer-domain-invariant-design.md), [queue-allocation proposal](phase3-transfer-queue-allocation-proposal.md).
- Đọc trực tiếp: [index kế hoạch](README.md) (phần đầu), [DB config review](phase0-db-config-review.md), [resolution contract](phase0-db-config-resolution-contract.md), [Owner deployment/TLS decisions](phase0-deployment-config-tls-decision-proposal.md), [model task board](03-model-task-board.md) (các đoạn quyết định/gate liên quan).
- Đã kiểm tra hướng dẫn tại các thư mục cha và thư mục chứa file: không tìm thấy `AGENTS.md`; không có `CONTRIBUTING.md`, `docs/DEVELOPMENT_CONVENTIONS.md` hay `ARCHITECTURE.md` trong danh sách file hướng dẫn checkout.

Áp dụng contract `PHASE0-PHASE3-DOC-REVIEW-CORRECTIONS-02`: chỉ sửa proposal này; giữ các quyết định triển khai non-binding, env-over-file per-key đã duyệt, `SEC-01` OPEN và downstream DEFAULT-DENY.

### 5.3. Phạm vi và Kiểm định

- Chỉ sửa [proposal này](phase0-db-config-remaining-decisions-proposal.md). Không tạo file; không đổi source, tests, schema, permission, audit, runtime, task board hay `run-app.sh`. Không push, PR hoặc merge.
- Không chạy build/application tests hay SQL; đây là sửa tài liệu. Chỉ đọc source/tests mẫu với dữ liệu tổng hợp, không mở config bí mật hoặc truy cập dữ liệu bệnh nhân.
- Kiểm định tài liệu: `git diff --check`; Python read-only kiểm tra local Markdown targets và line-anchor bounds trong feature checkout, không có file URI/worker path, đối chiếu nguyên văn bảy câu hỏi Owner với `HEAD`, và kiểm tra `git status --short` chỉ có file allowlist.
- Kết quả: `git diff --check` exit 0, không có diagnostic; Python exit 0 — 45 liên kết tương đối đều có target tồn tại, 26 line anchors hợp lệ, bảy câu hỏi Owner không đổi, không có file URI/worker path hay nhãn CWE bị loại bỏ. Scope chỉ có file allowlist; chính sách TLS, env-over-file per-key, trạng thái non-binding và `SEC-01` OPEN/default-deny được giữ. Đây là kiểm định tài liệu, không phải nghiệm thu runtime/TLS handshake.
