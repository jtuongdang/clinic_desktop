# Phase 0: Packaged-JAR Database Configuration — Remaining Decisions Proposal
## Đề xuất Quyết định Kỹ thuật: Hoàn thiện Cấu hình Cơ sở Dữ liệu cho Artifact Đóng gói (JAR)

> **Tài liệu đề xuất kỹ thuật (Non-binding technical proposal)**  
> **Mã hợp đồng tham chiếu:** `PHASE0-PHASE3-REVIEW-AND-TRACKING-DOCS-01`  
> **Trạng thái:** A1 interface RESOLVED theo addendum Owner-approved 2026-11-06 bên dưới; các ngữ nghĩa còn lại vẫn OPEN / NON-BINDING.
> **Mục tiêu:** Cung cấp cơ sở phân tích độc lập, đối soát mã nguồn và so sánh phương án cho các chi tiết cấu hình CSDL còn lại của artifact đóng gói `clinic-desktop.jar` thuộc Phase 0, không tự ý biến khuyến nghị thành quy định chính thức.

---

## Addendum Owner-approved — 2026-11-06: Chốt giao diện chọn file ngoài

**Contract:** `PHASE0-DB-CONFIG-EXPLICIT-PATH-OWNER-DECISION-01` — Owner-confirmed 2026-11-06; ID `d31d0af7-8ab7-47b6-a114-12c8ea42fbe6`. Chấp nhận contract này trong phạm vi ghi nhận tài liệu; chỉ chốt giao diện chọn file. Addendum này ưu tiên hơn các snapshot bên dưới khi nói về A1, không phê duyệt các khuyến nghị ngữ nghĩa đi kèm.

### A1 interface — RESOLVED

- Dùng biến môi trường của tiến trình **`CLINIC_DB_CONFIG_FILE`** để chỉ định **đường dẫn filesystem tuyệt đối, tường minh** tới external properties file cho cả IntelliJ và packaged-JAR launch.
- Không tự dò cấu hình trong CWD, thư mục chứa JAR, classpath hoặc các thư mục lân cận; không suy ra file từ vị trí khởi chạy.
- Khi selector **vắng mặt (absent)**, deployment profile có thể resolve environment/secret-injection values mà không dùng file ngoài, tùy các quyết định resolver còn mở. Không đồng nhất absent với selector có mặt nhưng blank.
- Selector là điều khiển đường dẫn, **không phải DB credential**. Các lựa chọn JVM `-Dclinic.config.file`, CLI `--db-config-file`, UI selector và dual-selector ở các snapshot/ví dụ cũ là **SUPERSEDED / NOT SELECTED** cho A1; không còn là câu hỏi Owner cần chọn.

### Các quyết định đã duyệt được giữ cố định

- Giữ support cả environment/secret injection và external properties file. Theo `PHASE0-DB-CONFIG-SOURCE-PRECEDENCE-OWNER-DECISION-01`, **environment/secret injection thắng trên từng key có ở cả hai nguồn; file chỉ cấp key vắng mặt trong environment**. Không mở lại source coverage hoặc precedence này.
- SQL Server bắt buộc mã hóa và xác thực chứng chỉ cùng danh tính/hostname máy chủ; chứng chỉ invalid, untrusted, expired hoặc identity-mismatched phải fail-closed. Không cho `trustServerCertificate` bypass, tắt mã hóa hay fallback làm yếu TLS. Chi tiết hiện thực và bằng chứng vẫn cần riêng.

### Still OPEN — không suy ra policy từ implementation

- Selector có mặt nhưng blank/whitespace, relative; file được chọn missing, unreadable hoặc malformed; có cho fallback sau lỗi selected-file hay không. Yêu cầu absolute path đã chốt, nhưng cách xử lý input vi phạm chưa chốt; trim/normalization cũng chưa được quyết định.
- Cú pháp/parser properties, duplicate keys và unknown keys.
- Combined `DB_CONNECTION_STRING` so với split values, kể cả kết hợp chéo nguồn (**A2**); per-key env-over-file không tự quyết định thứ tự này.
- Blank/partial credentials, bảo toàn secret và integrated authentication (**A3–A4**).
- `DB_DRIVER` / `db.driver` (**A6**); URL construction và default database (**A7**).
- File-only secrets, phân chia secret giữa nguồn, redaction và quyền file (**A8**).

Các khuyến nghị A1/A3/A5 cũ về reject blank/relative, không trim, fail-closed file errors hay fallback chỉ là phương án chưa duyệt. A2–A8 vẫn OPEN; phần error/syntax của selector tiếp tục OPEN dù A1 interface đã RESOLVED. Không lấy blank fallback, combined precedence hay hành vi khác của code hiện tại làm policy.

**Phase 0 release readiness / packaged-JAR readiness: OPEN. `SEC-01`: OPEN; downstream DEFAULT-DENY.** Ghi nhận này không cấp phép hoặc tuyên bố đã có configuration implementation, packaged release, launch behavior, DB connection, migration hay runtime/TLS acceptance. Không đổi code, packaging, launch scripts, runtime, tests hoặc DB.

### Guidance, RAG-first và kiểm định của lần ghi nhận selector

- Đọc Mandatory guidance được cung cấp trước khi sửa: `docs/PROJECT_CONVENTIONS.md` (Orchestrator baseline), `README.md`, các phần trích bị cắt của `DOMAIN_ANALYSIS.md`, `ERD_DIAGRAM.md`; supplied retrieved excerpts của `phase3-appointment-owner-decisions.md`, `phase3-visit-transfer-contract-review.md` và proposal này dưới `docs/ke-hoach-trien-khai/`. Phase 3 excerpts chỉ là bối cảnh retrieval/guidance, không quyết định DB policy.
- Đọc trực tiếp `README.md`, `docs/ke-hoach-trien-khai/README.md`, toàn bộ proposal này; đối chiếu các đoạn quyết định/evidence liên quan trong `phase0-deployment-config-tls-decision-proposal.md`, `phase0-db-config-resolution-contract.md`, `phase0-db-config-review.md`. Không tìm thấy `AGENTS.md` ở các thư mục cha/scope; checkout không có `CONTRIBUTING.md`, `docs/PROJECT_CONVENTIONS.md`, `docs/DEVELOPMENT_CONVENTIONS.md`, `docs/README.md` hoặc `ARCHITECTURE.md`.
- Read-only inspection: `DatabaseConfiguration.java`, `DatabaseConnection.java`, `DatabaseConfigurationTest.java`, desktop `Main.java` và phần Shade của `clinic-desktop/pom.xml`. Đây là kiểm tra nguồn để tách facts khỏi policy, không phải kiểm chứng runtime/packaging. Không mở secret config, dump environment, gọi JDBC/DB hoặc chạy launch.
- **RAG first:** lệnh đầu tiên trước repository reads/searches là `/Users/jtuong/Documents/ClinicOrchestrator/.venv/bin/python /Users/jtuong/Documents/ClinicOrchestrator/scripts/rag_search.py --repo clinic-desktop --limit 8 'PHASE0-DB-CONFIG-EXPLICIT-PATH-OWNER-DECISION-01 CLINIC_DB_CONFIG_FILE absolute path A1 remaining decisions environment file precedence TLS certificate validation'`. Exit **1**, `sqlite3.OperationalError: unable to open database file` tại `PRAGMA journal_mode=WAL`; không trả excerpt mới. Thứ tự RAG-first **PASS**, live retrieval **BLOCKED**; dùng supplied excerpts và targeted local reads. Không sửa/copy/move/delete cache/index hay né sandbox.
- Kiểm định lần này: `git diff --check` và inline Python read-only assertions cho scope một file, decision/open-semantics coverage, bảo toàn nội dung snapshot và balanced fences. Không chạy build/application tests vì task chỉ sửa tài liệu và cấm thay runtime/tests. Kết quả ở mục 5–6 là lịch sử của các vòng trước.

---

## 1. Bối cảnh và Các Tiền đề đã được Owner phê duyệt (Baseline & Approved Facts)

Tài liệu này kế thừa hợp đồng `PHASE0-PHASE3-REVIEW-AND-TRACKING-DOCS-01` và các báo cáo đối soát trước đó ([`phase0-db-config-review.md`](phase0-db-config-review.md), [`phase0-db-config-resolution-contract.md`](phase0-db-config-resolution-contract.md)). Các quyết định Owner hiện hành được ghi tại [`phase0-deployment-config-tls-decision-proposal.md`](phase0-deployment-config-tls-decision-proposal.md), theo `PHASE0-DB-DEPLOY-DECISION-RECON-02` và `PHASE0-DB-CONFIG-SOURCE-PRECEDENCE-OWNER-DECISION-01`:

1. **Bắt buộc hỗ trợ song song hai kênh nạp cấu hình:** Triển khai desktop dạng packaged-JAR bắt buộc phải hỗ trợ cả hai nguồn: file properties bên ngoài (`external properties file`) và biến môi trường/secret injection (`environment variables`).
2. **Quy tắc ưu tiên cấp trường (Per-key Environment Precedence):** Biến môi trường có quyền ưu tiên cao hơn trên từng trường đơn lẻ (`per-key environment wins`); file properties đóng vai trò là giá trị nền và bù đắp các trường còn thiếu (`file fills keys absent from environment`).
3. **Chính sách TLS bắt buộc mã hóa và xác thực chứng chỉ (Mandatory Certificate-Validating TLS):** Kết nối CSDL bắt buộc mã hóa và xác thực cả chứng chỉ lẫn danh tính máy chủ. Chứng chỉ không hợp lệ, không được tin cậy, hết hạn hoặc không khớp danh tính máy chủ phải làm kết nối thất bại (fail-closed); không fallback hoặc bypass theo môi trường. `encrypt=true;trustServerCertificate=false` là một cách biểu đạt JDBC SQL Server có thể dùng để hiện thực kết quả đã duyệt, không phải URL hay phương án truststore đã chốt; vẫn cần chứng minh đáp ứng đầy đủ chính sách.
4. **Phạm vi tài liệu:** `SEC-01` tiếp tục OPEN; các workflow hạ nguồn còn bị chặn tiếp tục DEFAULT-DENY. Không thực hiện thay đổi mã nguồn Java, bộ kiểm thử, schema migration, phân quyền hoặc runtime. Giữ nguyên script `run-app.sh`.

---

## 1A. Addendum — 2026-11-06: Hồ sơ vận hành tối thiểu trình Owner

**Snapshot đề xuất trước quyết định selector; NON-BINDING.** A1 interface và các alternative đã được addendum Owner-approved đầu tài liệu supersede; A2–A8 và ngữ nghĩa lỗi selector vẫn OPEN. Giữ nguyên các khuyến nghị/ví dụ dưới đây để truy vết, không coi toàn bộ hồ sơ tối thiểu đã được duyệt. Căn cứ: [resolution contract](phase0-db-config-resolution-contract.md), [deployment/TLS decisions](phase0-deployment-config-tls-decision-proposal.md), [DB config review](phase0-db-config-review.md), retrieved excerpts được cung cấp và đối chiếu source của vòng trước. Các câu trong hồ sơ dưới đây mô tả **phương án tương lai cần quyết định ngữ nghĩa và task riêng hiện thực**, không phải tính năng đang có.

### Chính sách đã duyệt — RESOLVED, không xin duyệt lại

- `PHASE0-DB-DEPLOY-DECISION-RECON-02` (2026-11-06): hỗ trợ cả environment/secret injection và external properties qua đường dẫn/giao diện tường minh. Không phê duyệt tự dò CWD hay thư mục JAR.
- `PHASE0-DB-CONFIG-SOURCE-PRECEDENCE-OWNER-DECISION-01`: **environment thắng theo từng khóa; file chỉ cấp khóa vắng mặt trong environment**. Đây là quyết định đã giải quyết, không phải lựa chọn trong bảng dưới. Quy tắc này chưa quyết định hai biểu diễn combined/split hoặc nghĩa của blank.
- SQL Server bắt buộc mã hóa, xác thực chuỗi chứng chỉ, thời hạn và danh tính/hostname; lỗi phải fail-closed. Không có phương án dùng `trustServerCertificate=true`, tắt mã hóa, driver-default fallback hay bypass theo môi trường. JDBC options và trust-anchor cụ thể còn cần đặc tả/kiểm chứng; không mở lại chính sách này.

### A1. Snapshot giao diện chọn file — interface RESOLVED; alternatives SUPERSEDED

**Bảng lịch sử:** chỉ env selector absolute-path được chọn theo addendum Owner-approved; JVM/CLI/UI là SUPERSEDED / NOT SELECTED. Các quy tắc blank/relative/trim/fallback trong cột khuyến nghị cũ chưa được duyệt.

| Lựa chọn tường minh | Lợi ích và chi phí | Khuyến nghị hiện hành |
|---|---|---|
| `CLINIC_DB_CONFIG_FILE=<absolute-path>` | Dùng cùng cách trong IntelliJ Environment variables và launcher JAR; chỉ một selector nên không cần precedence giữa selector. Cần quản lý environment kế thừa. | **Chọn cho hồ sơ tối thiểu.** Không đặt biến thì chạy env-only; biến đã đặt nhưng rỗng/khoảng trắng hoặc đường dẫn tương đối thì lỗi. Không trim đường dẫn; khoảng trắng hợp lệ trong tên file được giữ nguyên. |
| JVM `-Dclinic.config.file=<absolute-path>` | Dễ cố định trong IntelliJ VM options hoặc launcher; phải đặt trước `-jar`. | Thay thế env selector nếu Owner ưu tiên JVM. Nếu hỗ trợ đồng thời, đề xuất từ chối hai selector có giá trị khác nhau, thay vì âm thầm chọn một. |
| CLI `--db-config-file=<absolute-path>` | Dễ nhìn trong Program arguments; cần parser ứng dụng, đặt sau tên JAR. | Chưa cần cho bản tối thiểu; phải định nghĩa lỗi argument và xung đột selector nếu chọn. |
| UI selector tường minh trước kết nối | Thuận tiện cho người dùng desktop; cần luồng browse/cancel/validation, lưu lựa chọn và quyền truy cập. | Hoãn; cancel thì không kết nối, không tự chọn file khác. Việc lưu path và cấp secret cần quyết định riêng. |

Mọi lựa chọn chỉ đọc đúng file đã chỉ định; không suy ra `./db.properties`, file cạnh JAR, đường dẫn mặc định hay file vừa dùng. Hồ sơ tối thiểu đề xuất dùng cùng resolver/env/path trong IDE và JAR, không thêm classpath fallback ngầm trong hồ sơ mới. Classpath dev hiện hữu là bằng chứng tương thích, không phải external-file interface đã duyệt. Chưa có interface A1 nào được hiện thực.

### A2. Combined và split — không suy ra precedence từ per-key

**Bằng chứng:** hiện chỉ `DB_CONNECTION_STRING` có kênh combined; `db.connectionString` trong file chưa được resolver đọc. Các ví dụ file combined ở mục 3.2 là phương án tương lai, không phải tính năng hiện tại.

| Lựa chọn Owner | Hệ quả vận hành |
|---|---|
| Giữ combined luôn thắng như runtime hiện tại | Dễ tương thích, nhưng split secret có thể bị bỏ qua. Không được mô tả đây là quyết định Owner hay hệ quả của env-over-file. |
| Source-aware: env combined > env split > file combined > file split | Cần đặc tả việc parse/merge URL và credentials. Chỉ có env password không đủ để dựng URL từ file combined; không được ngầm trích/chắp secret. Chưa có khóa file combined được duyệt. |
| **Từ chối combined/split đồng thời — khuyến nghị** | Giải quyết từng split key theo env-over-file, sau đó nếu có `DB_CONNECTION_STRING` cùng bất kỳ split key nào từ env/file thì báo lỗi trước JDBC. Không chọn chuỗi thắng ngầm và không merge credentials vào URL. |

Hồ sơ tối thiểu dùng split `DB_URL`/`db.url`, `DB_USERNAME`/`db.username`, `DB_PASSWORD`/`db.password`. Để giữ một đường tương thích rõ ràng, đề xuất cho phép `DB_CONNECTION_STRING` **đứng riêng**, với URL và xác thực hoàn chỉnh, cùng kiểm tra TLS và redaction; không có split key ở bất kỳ nguồn nào. `db.connectionString` chưa được bổ sung: đề xuất báo khóa không hỗ trợ, thay vì bỏ qua. Nếu Owner muốn file combined, cần duyệt tên khóa và ma trận xung đột riêng. Các khóa khác nhau không tự ghi đè nhau; môi trường luôn thắng đúng khóa tương ứng theo chính sách đã duyệt.

### A3–A8. Những ngữ nghĩa còn lại — lựa chọn và khuyến nghị

| ID / Owner cần quyết định | Đề xuất cho hồ sơ tối thiểu | Lựa chọn khác và đánh đổi |
|---|---|---|
| **A3 — absent, empty, whitespace** | Absent là không có key. Env có key rỗng hoặc whitespace-only là invalid, không fallback xuống file. Giá trị file được chọn sau per-key resolution cũng theo quy tắc này. URL/username không được có khoảng trắng bao quanh; báo lỗi thay vì tự trim. Password không rỗng/whitespace-only được giữ nguyên ký tự, kể cả khoảng trắng đầu/cuối; không trim secret. | Giữ blank-as-absent như hiện tại thuận tiện cho template, nhưng có thể che lỗi injection; hoặc cho empty password có chủ đích với chế độ xác thực riêng. Cả hai chưa được duyệt. |
| **A4 — partial credentials / integrated auth** | Profile split chỉ dùng SQL username/password, cả hai đều bắt buộc sau per-key merge. Username từ file + password từ env là hợp lệ; chỉ có một hoặc cả hai vắng mặt đều lỗi trong profile này. Không suy ra integrated auth từ thiếu credentials hoặc username rỗng. Combined đứng riêng cũng cần xác thực SQL đầy đủ. | Hỗ trợ integrated auth như profile tường minh riêng, không username/password, với điều kiện JDK/driver/OS/identity và kiểm chứng riêng. Không áp một quy tắc credentials chung cho mọi cơ chế auth; chưa duyệt integrated/managed identity. |
| **A5 — file được chọn không tồn tại, không đọc được, malformed** | Fail-closed trước JDBC, kể cả env đã đủ; không dùng nội dung đọc dở, không bỏ file để fallback env/classpath. Phân biệt không chọn file (env-only được xét) với đã chọn file lỗi. Đề xuất Java properties theo `Properties.load(InputStream)` hiện hữu, quy tắc escape rõ ràng; lỗi parse, duplicate key và khóa ngoài schema được báo lỗi an toàn. | Cho fallback env có cảnh báo giảm gián đoạn nhưng che lỗi chọn file; không khuyến nghị. Định nghĩa malformed phải gồm lỗi cú pháp và validation schema, không giả định mọi dòng sai đều bị Java Properties từ chối. |
| **A6 — `DB_DRIVER` / `db.driver` đang accepted nhưng unused** | Đề xuất deprecate rồi remove trong task riêng; hồ sơ mới không quảng bá setting này, key còn tồn tại phải báo migration error, không âm thầm bỏ qua. Xác minh driver SQL Server đã đóng gói/đăng ký, driver không có thì lỗi an toàn. | Nếu có nhu cầu driver class cụ thể, định nghĩa `Class.forName` trước handoff, kiểm tra class phù hợp, lỗi load phải dừng và redacted. Không cho cấu hình tải JAR/code từ xa. Không coi accepted hiện tại là explicit loading đã hỗ trợ. |
| **A7 — dựng URL / database mặc định** | Yêu cầu URL SQL Server hoàn chỉnh, host/port và `databaseName` tường minh; không tự dựng từ host, không tự chọn `clinicDb`, `master` hay database mặc định của login. Không credentials trong split URL; không tự nối thêm password/TLS options hoặc cho duplicate/conflicting options. Kiểm tra TLS options/identity theo policy đã duyệt; cú pháp cần validator đặc thù được đặc tả riêng. | Dựng URL từ host/port/database có thể thân thiện hơn nhưng thêm schema, escaping và precedence; dùng database mặc định của tài khoản ít cấu hình hơn nhưng dễ sai đích. Chưa chọn mặc định nào. |
| **A8 — secret split, redaction, permissions** | File chủ yếu chứa URL/username, password do secret injection vào `DB_PASSWORD`; file được phép cấp password khi env không có nếu Owner chọn biến thể đó. Không log full URL/connection string, username, password, truststore secret, file contents hoặc raw JDBC/parse exception. Chỉ báo mã lỗi, key và loại nguồn, không giá trị/path đầy đủ. | File chứa secret giúp vận hành offline nhưng tăng trách nhiệm ACL/rotation/backup. Managed secret service cần scope riêng; env không tự bảo đảm bí mật. Secret split và quy tắc file cụ thể vẫn chờ Owner. |

Schema file tối thiểu đề xuất là `db.url`, `db.username`, `db.password`; không đưa `db.driver`, `db.pool.maxSize` hay khóa combined chưa có consumer vào ví dụ mới. Giá trị properties được decode theo parser đã chọn trước validation; nếu file chứa secret với dấu cách/backslash/ký tự escape, phải có hướng dẫn encode và test bảo toàn giá trị, không sao chép quy tắc shell sang properties. Chỉ kiểm tra completeness/schema không chứng minh URL hay handshake hợp lệ.

**Hướng dẫn quyền file đề xuất, chưa phải chính sách đã duyệt:** đặt file ngoài repo/artifact tại thư mục do quản trị kiểm soát; runtime user chỉ cần read, quyền write thuộc người quản trị phù hợp. Unix có thể dùng file mode `0600` cho file thuộc runtime user hoặc `0640` với nhóm riêng có kiểm soát; bảo vệ cả thư mục cha khỏi người không được phép sửa. Windows dùng ACL giới hạn tài khoản ứng dụng và quản trị, bỏ quyền đọc/ghi rộng. Không coi `.gitignore` là kiểm soát truy cập; không đồng bộ file secret vào repo, ảnh chụp, backup/chia sẻ rộng. Không sửa permission trong task này.

Trust-anchor đề xuất được quản trị trong truststore của runtime Java đã xác minh, cùng hostname khớp chứng chỉ. Truststore riêng là lựa chọn kỹ thuật thay thế cần kiểm chứng; không mặc định OS trust store đồng nhất Java truststore. Không đặt secret thật trong JVM/CLI arguments. Bất kỳ profile nào cũng phải đáp ứng TLS đã duyệt; không biến trust-anchor provisioning thành câu hỏi có cho bypass hay không.

### Ví dụ IntelliJ và JAR — chỉ placeholder tổng hợp, không chạy trong task này

**Ví dụ lịch sử / alternatives:** ví dụ env selector minh họa giao diện đã chọn, không duyệt ngữ nghĩa credentials/errors đi kèm. Ví dụ JVM/CLI và UI selector là SUPERSEDED / NOT SELECTED; các câu “nếu Owner chọn” bên dưới chỉ lưu bối cảnh cũ.

Các ví dụ mô tả hồ sơ đề xuất sau khi được duyệt/hiện thực. **Loader hiện tại không đọc `CLINIC_DB_CONFIG_FILE`, JVM property hay CLI selector; các lệnh file dưới đây chưa hoạt động với JAR hiện tại.** Các đường dẫn là giả định tuyệt đối do operator chọn, không phải default hoặc discovery rule. URL minh họa `encrypt=true;trustServerCertificate=false` chưa chứng minh chain/identity validation; cần trust-anchor đúng và kiểm chứng độc lập.

File giả định `/opt/clinic-synthetic/config/db.properties`:

```properties
db.url=jdbc:sqlserver://db.synthetic.invalid:1433;databaseName=synthetic_clinic;encrypt=true;trustServerCertificate=false
db.username=synthetic_user
```

- **IntelliJ, hồ sơ file + injected password:** Main class `vn.clinicmanagement.desktop.Main`, JDK 17; Environment variables: `CLINIC_DB_CONFIG_FILE=/opt/clinic-synthetic/config/db.properties`, `DB_PASSWORD=<synthetic-password-marker>`. Bỏ `DB_CONNECTION_STRING`, `DB_URL`, `DB_USERNAME` kế thừa ngoài ý muốn để ví dụ dùng URL/user trong file. VM options và Program arguments không chứa secret. Working directory không tham gia chọn file.
- **IntelliJ, env-only:** không đặt selector; Environment variables chứa `DB_URL` như URL mẫu, `DB_USERNAME=synthetic_user`, `DB_PASSWORD=<synthetic-password-marker>`; không đặt `DB_CONNECTION_STRING`. Không dùng classpath `db.properties` làm fallback trong hồ sơ đề xuất.
- **IntelliJ, các alternative chưa chọn:** nếu Owner chọn JVM interface, đặt `-Dclinic.config.file=/opt/clinic-synthetic/config/db.properties` trong VM options; nếu chọn CLI interface, đặt `--db-config-file=/opt/clinic-synthetic/config/db.properties` trong Program arguments. Chọn đúng một selector, không tự cộng cả ba.

JAR, file + environment (sau task hiện thực riêng):

```bash
# Chỉ là marker tổng hợp; secret thật do launcher/secret injector cấp, không gõ vào history.
CLINIC_DB_CONFIG_FILE='/opt/clinic-synthetic/config/db.properties' \
DB_PASSWORD='<synthetic-password-marker>' \
java -jar '/opt/clinic-synthetic/clinic-desktop.jar'
```

JAR, env-only (cú pháp env hiện có; không phải bằng chứng release/TLS readiness):

```bash
# Không đặt selector hoặc DB_CONNECTION_STRING; không dựa vào config kế thừa.
DB_URL='jdbc:sqlserver://db.synthetic.invalid:1433;databaseName=synthetic_clinic;encrypt=true;trustServerCertificate=false' \
DB_USERNAME='synthetic_user' \
DB_PASSWORD='<synthetic-password-marker>' \
java -jar '/opt/clinic-synthetic/clinic-desktop.jar'
```

Alternative selector, chỉ nếu được Owner chọn và hiện thực, với password đã được inject vào environment:

```bash
java '-Dclinic.config.file=/opt/clinic-synthetic/config/db.properties' \
  -jar '/opt/clinic-synthetic/clinic-desktop.jar'
java -jar '/opt/clinic-synthetic/clinic-desktop.jar' \
  '--db-config-file=/opt/clinic-synthetic/config/db.properties'
```

Không đặt selector khác đồng thời trong các ví dụ alternative. Ví dụ không cấp mật khẩu trên command-line arguments, không chạy migration hay kết nối DB. Environment cũng có thể bị lộ qua process diagnostics/IDE configuration: hạn chế tài khoản truy cập, không bật shell tracing hoặc dump env, không chia sẻ Run Configuration chứa secret thật. Không lưu credentials qua UI selector theo mặc định.

### Quyết định Owner và evidence còn thiếu

Owner cần ghi lựa chọn **A1–A8**, ngoại lệ và phạm vi profile, cùng task triển khai riêng; chưa mục nào trong addendum này là approval. Khuyến nghị nhất quán là env selector duy nhất + split SQL credentials + password injection + không database mặc định + reject ambiguity/blank/partial/file errors + remove unused driver setting + diagnostics redacted. File-only secret là biến thể chờ chọn, không phải secret behavior ngầm được duyệt.

Sau phê duyệt, acceptance tương lai cần kiểm tra bằng synthetic/fake JDBC: từng key env thắng/file bù thiếu; mixed combined/split; empty/whitespace và bảo toàn secret; partial/integrated tách biệt; file missing/unreadable/malformed/duplicate/unknown key không handoff; selector absolute/no discovery; database rõ ràng; driver và redaction ở mọi đường lỗi. Artifact/launch và TLS handshake với chain/expiry/hostname sai cần bằng chứng riêng trong môi trường được ủy quyền. Không thực hiện hay tuyên bố các kiểm thử này trong task tài liệu.

**Phase 0 release readiness vẫn OPEN; `SEC-01` OPEN; downstream DEFAULT-DENY.** Không đổi code, packaging, runtime, launch scripts, tests hoặc database. Chính sách đã duyệt ở trên được giữ nguyên; hoàn thành proposal không đóng gate.

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

**Snapshot các phương án trước addendum 2026-11-06:** giữ để truy vết, không phải bộ khuyến nghị hiện hành. Riêng giao diện A1 ở mục 3.1 đã RESOLVED bởi addendum Owner-approved đầu tài liệu: env selector được chọn; JVM/dual/CLI/UI alternatives SUPERSEDED / NOT SELECTED. Các nội dung khác vẫn là phương án non-binding; mục 1A giữ hồ sơ đề xuất ngữ nghĩa chưa duyệt. Không đọc các lựa chọn cũ như approval.

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

## 4. Danh mục quyết định Owner còn lại (hiện hành)

Bộ câu hỏi A1–A8 được cập nhật theo addendum Owner-approved đầu tài liệu; chỉ interface A1 đã chốt, các ngữ nghĩa khác vẫn OPEN:

1. **A1 interface — RESOLVED:** `CLINIC_DB_CONFIG_FILE` chỉ định absolute path cho IntelliJ/JAR; JVM/CLI/UI/dual-selector SUPERSEDED / NOT SELECTED. Blank/relative, lỗi selected-file và fallback vẫn OPEN cùng A3/A5; không còn câu hỏi chọn interface.
2. **A2:** Chọn rejection combined/split được khuyến nghị hoặc đặc tả precedence/merge khác; quyết định có thêm file combined key hay không.
3. **A3:** Chọn nghĩa absent/empty/whitespace trong env và file, cùng việc bảo toàn password.
4. **A4:** Chọn SQL-only profile và credential pair, hoặc phạm vi integrated auth tường minh riêng; không suy ra auth từ credentials thiếu.
5. **A5:** Chọn fail-closed cho file được chọn lỗi, schema/parser và lỗi duplicate/unknown key, hoặc fallback tường minh khác.
6. **A6:** Chọn deprecate/remove setting driver unused hoặc explicit JDBC class loading có kiểm soát lỗi.
7. **A7:** Chọn URL hoàn chỉnh/database tường minh hoặc schema dựng URL có quy tắc escaping/default rõ ràng.
8. **A8:** Chọn secret split, redaction/file permissions; kỹ thuật trust-anchor/hostname phải chứng minh TLS đã duyệt, không xin duyệt lại mã hóa hay certificate validation.

Không câu nào mở lại support hai nguồn, env-over-file per-key hoặc yêu cầu TLS/certificate identity. Quyết định Owner mới chỉ chốt A1 interface, không duyệt các khuyến nghị A2–A8.

---

## 5. Nhật ký Thực thi và Đối soát Hướng dẫn (Guidance & Verification Record)

Mục 5.1–5.3 và mục 6 là nhật ký các vòng sửa trước; kết quả đó không phải kiểm định lần ghi nhận selector. Xem addendum Owner-approved đầu tài liệu cho lần hiện tại.

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

## 6. Nhật ký addendum — 2026-11-06 (lịch sử trước quyết định selector)

### Guidance và kiểm tra hiện trạng

- Đọc Mandatory guidance được cung cấp trước khi sửa: `docs/PROJECT_CONVENTIONS.md` (baseline được cung cấp, không có bản checkout), `README.md`, phần trích đã bị cắt của `DOMAIN_ANALYSIS.md` và `ERD_DIAGRAM.md`.
- Đọc supplied retrieved excerpts: `phase0-deployment-config-tls-decision-proposal.md`, `phase0-db-config-resolution-contract.md`, proposal này, `phase3-visit-transfer-contract-review.md`, `phase3-transfer-linked-work-eligibility-review.md` (đều dưới `docs/ke-hoach-trien-khai/`). Excerpts Phase 3 chỉ cung cấp bối cảnh guidance/retrieval, không quyết định DB policy.
- Đọc trực tiếp `README.md`, `docs/ke-hoach-trien-khai/README.md`, proposal này, `phase0-db-config-resolution-contract.md`, `phase0-deployment-config-tls-decision-proposal.md` và `phase0-db-config-review.md`. Các nhận định TLS/nguồn chưa chốt trong review/contract cũ là lịch sử, đã được Owner decisions mới supersede đúng phạm vi; không mở lại.
- Kiểm tra đường dẫn cha và thư mục scope: không tìm thấy `AGENTS.md`. Inventory không có `CONTRIBUTING.md`, `docs/PROJECT_CONVENTIONS.md`, `docs/DEVELOPMENT_CONVENTIONS.md` hoặc `ARCHITECTURE.md`; không suy ra guidance chưa đọc.
- Read-only implementation inspection: `DatabaseConfiguration.java`, `DatabaseConnection.java`, `DatabaseConfigurationTest.java`, `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/Main.java`, phần Shade của `clinic-desktop/pom.xml`. Resolver hiện hữu xác nhận blank fallback, trim split env, combined thắng, driver unused và diagnostic presence-only; Main chưa có CLI/file selector. Không mở file secret, không đọc process environment, không gọi JDBC/DB.

### RAG-first và kết quả kiểm định

Lệnh đầu tiên, trước repository reads/searches:

```bash
/Users/jtuong/Documents/ClinicOrchestrator/.venv/bin/python /Users/jtuong/Documents/ClinicOrchestrator/scripts/rag_search.py --repo clinic-desktop --limit 8 'phase0-db-config remaining decisions resolution contract deployment TLS DB_CONNECTION_STRING DB_DRIVER explicit file environment precedence'
```

Exit **1**, `sqlite3.OperationalError: unable to open database file` tại `PRAGMA journal_mode=WAL`; không trả excerpt mới. Thứ tự RAG-first **PASS**; live RAG retrieval **BLOCKED**, không thể báo tiêu chí RAG thành công hoàn toàn. Dùng supplied retrieved excerpts và targeted local reads làm fallback; không sửa/copy/move/delete shared cache/index hoặc né sandbox.

- `git diff --check`: **PASS**, exit 0, không output.
- Inline Python read-only assertions: **PASS**, exit 0; `git diff --name-only HEAD` chỉ có file proposal allowlist, không untracked files; kiểm tra coverage A1–A8/policy/gates, local Markdown targets/line-anchor bounds và balanced fences. Cả 3 shell blocks mới trong addendum được kiểm tra bằng `zsh -n` (syntax-only), không thực thi lệnh launch. Không chạy build/application tests: task chỉ sửa tài liệu và cấm đổi runtime/tests.
- Acceptance: **PASS** cho scope một file + dated addendum, policy đã resolved, lựa chọn còn lại/khuyến nghị/ví dụ synthetic, không approval ngầm và không thay code/package/runtime/tests. **PASS** cho diff check và thứ tự RAG-first; **BLOCKED** cho live RAG retrieval như trên. Phase 0 readiness vẫn OPEN, `SEC-01` OPEN và downstream DEFAULT-DENY; chưa có acceptance runtime/TLS mới.
