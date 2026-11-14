# Báo cáo hiện trạng và mức độ hoàn thiện project

**Project:** Clinic Management Desktop (Java 17, Maven multi-module, Swing, JDBC/SQL Server)
**Ngày rà soát:** 06/11/2026
**Phạm vi:** Source hiện có trong checkout, hướng dẫn chạy, kế hoạch theo phase/task và bằng chứng kiểm định được ghi nhận đến ngày rà soát. Báo cáo tách riêng trạng thái triển khai trong source, kiểm định Maven mặc định, kiểm tra SQL Server opt-in trên dữ liệu tổng hợp và những phần chưa được quan sát/nghiệm thu. Đây không phải tuyên bố phase hoàn tất hay đánh giá sẵn sàng production.

> Báo cáo 04/11/2026 là snapshot lịch sử, rà soát tĩnh và không chạy build/test. Các kết luận trong snapshot đó về việc không có test, không có persistence, thiếu README onboarding và chỉ có login demo đã lỗi thời; nội dung dưới đây thay thế chúng theo source và bằng chứng mới.

## 1. Kết luận điều hành

Project đã vượt qua trạng thái “chỉ có UI tĩnh”: có kiểm thử tự động, xác thực tài khoản, nền tảng role/scope/authorization/audit một phần, và luồng tiếp đón có lưu/đọc dữ liệu bệnh nhân, BHYT, liên kết cơ sở, lượt khám và hàng đợi. Mức độ hoàn thiện còn khác biệt lớn giữa các phân hệ. Lượt kiểm định Maven xác nhận build và test mặc định; task board ghi nhận riêng các lượt SQL Server opt-in có phạm vi migration, persistence và audit trên database tổng hợp. Các bằng chứng này không xác nhận toàn bộ desktop UI, mọi luồng nghiệp vụ, hoặc khả năng vận hành với dữ liệu thật.

| Khía cạnh | Hiện trạng đã xác minh | Giới hạn chính |
|---|---|---|
| Nền tảng và build | Baseline ngày 06/11/2026: JDK 17 `mvn -Djava.awt.headless=true clean verify` kết thúc `BUILD SUCCESS`. | Chưa có nghiệm thu phát hành/vận hành; không suy ra sẵn sàng production. |
| Xác thực, phiên và quyền | Có kiểm tra thông tin đăng nhập, role/scope được gán, hoàn tất phiên, evaluator fail-closed và một số luồng tiếp đón có kiểm tra quyền/audit. | Authorization chưa phủ toàn hệ thống; policy/runtime grant và các quyết định còn mở vẫn chặn những luồng nhạy cảm. |
| Tiếp đón và bệnh nhân | Có handlers, JDBC repositories, giao diện nối qua composition root, tìm kiếm có scope, ghi/đọc bệnh nhân, BHYT và lượt khám/hàng đợi. | Chỉ là vertical slice đang tiến hành; UI-to-database E2E đầy đủ và nghiệm thu Phase 1 còn thiếu. |
| Tiền sử và EMR | Có query đọc và command tạo/sửa ba loại tiền sử đã duyệt; cả hai được giới hạn theo DOCTOR/scope và audit. SQL Server opt-in persistence/rollback acceptance đã pass. | Doctor UI chưa nối; EMR chưa có luồng end-to-end được chấp nhận. |
| Các phân hệ khác | Có nhiều model, migration, panel, dialog hoặc bản xem trước. | Khám, CLS, kê đơn/kho, viện phí, danh mục, báo cáo, HR/quản trị chủ yếu chưa có application-to-database workflow được nghiệm thu; một số UI dùng dữ liệu tĩnh. |
| Kiểm thử | Baseline mặc định mới nhất JDK 17 ngày 06/11/2026: **305 tests, 0 failures, 0 errors, 5 SQL Server opt-in skips** (65 Surefire XML reports). | Năm test SQL Server opt-in không chạy trong lượt mặc định; UI chưa được quan sát trực tiếp. |

## 2. Bằng chứng và cách diễn giải

### Kiểm định mặc định

Baseline root mới nhất được ghi nhận ngày 06/11/2026 trên JDK 17, lệnh `mvn -Djava.awt.headless=true clean verify`: **BUILD SUCCESS**, 305 tests, 0 failures, 0 errors, 5 SQL Server opt-in skips, trên bốn module có test (`clinic-domain`, `clinic-application`, `clinic-infrastructure`, `clinic-desktop`; 65 Surefire XML reports). Snapshot root trước đó 295/0/0/4 (63 reports), 283/0/0/4 (61 reports) và 269/0/0/4 (58 reports) được giữ làm bằng chứng lịch sử; snapshot cũ hơn 262/0/0/4 cũng là lịch sử. Năm test bị skip là `MoiCoSoDuLieuDiCuSQLMayChuTest`, `KiemToanSQLMayChuLuuTruTest`, `TiepDonSQLMayChuLuuTruTest`, `TiepDonSQLMayChuXuLyKhoiDongLaiTest` và `BenhNhanTienSuSQLMayChuLuuTruTest`; chúng **không chạy** trong Maven verify mặc định. Các lượt SQL Server opt-in được ghi riêng và không thuộc baseline này. Bảy focused tests của `VIS-STATE-START-TEST-01` kiểm tra một transition `CHO_KHAM` → `DANG_KHAM` hợp lệ và sáu trạng thái enum còn lại bị từ chối mà không mutation. Sáu test tổng hợp của `InXuLyMienNghiepVuSuKienPhatHanh` kiểm tra dispatcher độc lập; hai test `TruuTuongMienNghiepVuSuKien` kiểm tra UTC timestamp conversion từ clock không UTC và null-clock rejection. Tám invocation mới của `EMR-LOCK-INPUT-01` kiểm tra invariant đầu vào domain cho `LuotKham.lockEmr()` trong task boundary 23 test `clinic-domain`; chúng không xác nhận quyền người khóa hoặc workflow. Không có event consumer/storage wiring, aggregate event collection, transaction-bound dispatch, real handler wiring, retry hay outbox. Kết quả này xác nhận build/test suite mặc định, không xác nhận toàn bộ SQL Server hay giao diện desktop đang hiển thị đúng.

### SQL Server opt-in riêng trên dữ liệu tổng hợp

- `MoiCoSoDuLieuDiCuSQLMayChuTest` pass **1/1** trên database tổng hợp mới `clinic_test_v9_20261106`, áp dụng migration V1–V9 và kiểm tra ràng buộc liên kết bệnh nhân–cơ sở của V9. Lượt legacy-duplicate preflight runtime mới cũng pass **1/1** trên database SQL Server rỗng disposable mới `clinic_test_v9_preflight_20261106` bằng lệnh `mvn -pl clinic-infrastructure -am -Djava.awt.headless=true -Dclinic.sqlserver.migration.enabled=true -Dclinic.sqlserver.test.database=clinic_test_v9_preflight_20261106 -Dtest=MoiCoSoDuLieuDiCuSQLMayChuTest -Dsurefire.failIfNoSpecifiedTests=false test`. Cùng lượt này áp dụng V1–V9; kiểm tra active uniqueness và soft-delete; tạo duplicate legacy tổng hợp trước V9; xác nhận preflight failure giữ nguyên hai row và không tạo tracking/index; rồi soft-delete một row và retry V9 thành công. Đây là SQL Server opt-in độc lập, không thuộc 304 test của default verify và không phải production migration rehearsal. Chỉ dùng synthetic data; không database hiện hữu nào bị sửa hoặc drop.
- `MoiCoSoDuLieuDiCuSQLMayChuTest` V10 acceptance pass **1/1** trên disposable `clinic_test_v10_history_scope_final_20261106`: chạy V1–V10, giữ row tiền sử legacy ở null/null, từ chối insert mới thiếu scope (SQL error 51010), từ chối pair khoa/cơ sở không khớp và nhận pair đúng. Synthetic fixtures được cleanup. Hai database disposable từ các lần thử assertion trước (`clinic_test_v10_history_scope_20261106`, `clinic_test_v10_history_scope_retry_20261106`) được giữ nguyên, không drop; các lượt đó dừng ở assertion và không còn fixture bệnh nhân/tiền sử. Đây là opt-in riêng, ngoài default verify; không dùng `clinicDb` hay dữ liệu thật.
- `BenhNhanTienSuSQLMayChuLuuTruTest` pass **1/1** trên database tổng hợp mới `clinic_test_patient_history_20261106`. V1–V10 được áp dụng và xác nhận trước khi test chạy. Acceptance bao phủ insert qua trigger V10 với identity trả về, `createdAt` UTC, lọc theo bệnh nhân+cặp cơ sở/khoa+3 loại đang hoạt động, update/version, audit đọc `PATIENT_VIEW`/`PATIENT` và rollback cập nhật khi trigger tổng hợp từ chối audit. Fixture bệnh nhân/lượt khám/tiền sử và trigger test được dọn; database được giữ nguyên. Không dùng `clinicDb` hay dữ liệu bệnh nhân thật.
- `TiepDonSQLMayChuLuuTruTest` pass **1/1** riêng trên database tổng hợp nói trên. Bài test mới điền `BenhNhanDangKyBieuMauManHinh` trên Swing EDT, tạo lệnh qua converter thật, gọi composition/handler thật, rồi tìm và đọc lại toàn bộ trường bệnh nhân/BHYT được form hỗ trợ, liên kết cơ sở và lượt trong queue. Fixture dọn các dòng do nó tạo bằng soft delete.
- `KiemToanSQLMayChuLuuTruTest` pass **1/1** trên database tổng hợp `clinic_test_audit_20261105`: task board ghi nhận kiểm tra append-only V6 và rollback các ghi patient/insurance/visit khi audit insert bị từ chối. Đây là scope hẹp, không cấp quyền `AUDIT_READ`.
- Các lượt trên là kiểm tra opt-in độc lập được ghi nhận trong `docs/ke-hoach-trien-khai/03-model-task-board.md`, không thuộc 283 test mặc định và không được chạy lại trong lần cập nhật tài liệu này. Chúng không xác minh toàn bộ `TiepDonManHinh` hoặc mọi trường hợp nghiệp vụ; database được nêu đều là database tổng hợp, không phải `clinicDb`. Lần cập nhật này không kết nối database.

### UI và nghiệm thu

Các test Swing/headless có bằng chứng opt-in nối form đăng ký với handler/persistence trên SQL Server tổng hợp; bằng chứng preflight V9 ở trên là test migration riêng, không thay thế UI acceptance. Chúng không chứng minh cửa sổ thực tế đã được nhìn thấy, layout hiển thị đúng, hay `TiepDonManHinh` button/worker/dialog chạy trọn vẹn. Kế hoạch hiện trạng ghi cửa sổ Swing chưa được quan sát trực tiếp; full UI visual/interaction acceptance còn mở. Không có kết luận thị giác về UI trong báo cáo này.

## 3. Hiện trạng theo lớp và luồng

### Nền tảng, xác thực và phân quyền

- Có application handler xác thực mật khẩu đã băm, tải role và work scope đã gán; hoàn tất login kiểm tra lại lựa chọn/assignment và ghi audit. Repository JDBC đọc nhân viên, role và scope.
- Có `PhanQuyenDichVu` ở application layer với nguyên tắc default-deny, kiểm tra phiên/nhân viên/role/scope và grant theo module/action. Một số luồng đọc/ghi tiếp đón được bảo vệ bằng evaluator và audit; UI có điều hướng nhận biết quyền nhưng visual smoke chưa thực hiện.
- Các error boundary của workflow hiện được nối đã được audit; diagnostics chỉ ghi operation/result code, exception type và stack giới hạn, không ghi exception message hay dữ liệu nghiệp vụ. Application authorization, login và dashboard worker failures giữ nguyên default-deny/result hoặc thông báo an toàn. Đây không chứng nhận các workflow chưa triển khai; mapping/error test tiếp tục là acceptance bắt buộc cho task mới.
- Đây là nền tảng đang triển khai, không phải authorization hoàn chỉnh. Theo task board, các luồng còn lại chưa được bảo vệ/kiểm chứng đồng đều; các policy như quyền audit-read, lockout, retention, một số phân quyền theo nghiệp vụ và quyền hậu khóa còn chờ quyết định hoặc implementation. Gate Phase 2 chưa đạt; không coi các thao tác EMR, prescription/pharmacy hay billing là đã mở an toàn.
- Cấu hình môi trường và đóng gói có cải thiện, song quy trình cung cấp/luân chuyển secrets, chính sách TLS, backup/restore và release operations chưa được nghiệm thu. Báo cáo không đọc cấu hình database local hoặc giá trị môi trường.

### Bệnh nhân và tiếp đón

- `DangKyBenhNhanXuLy`, `TimKiemBenhNhanXuLy`, `LayTiepDonHangDoiXuLy` và `TiepDonGhepNoiGoc` nối UI, application, transaction, audit và JDBC adapters.
- Có lưu/cập nhật và đọc lại hồ sơ bệnh nhân, BHYT, liên kết bệnh nhân–cơ sở, lượt khám/hàng đợi; truy vấn tiếp đón được giới hạn theo cặp cơ sở/khoa trong các luồng đã nối. Có tests unit/headless và bằng chứng SQL Server opt-in nêu ở trên.
- Đây là slice có persistence thực, khác với các panel chỉ hiển thị mẫu. Phase 1 vẫn `[~]`: headless form-to-SQL acceptance và các audit/scope cụ thể đã có; full `TiepDonManHinh` visual/interaction workflow, queue lifecycle/chuyển phòng, tiền sử có guard/audit và nghiệm thu nghiệp vụ còn mở. Bằng chứng opt-in không đóng phase.

### Tiền sử bệnh và khám/EMR

- `TienSuBenh`, trường EMR trong `LuotKham`, schema và một số màn hình/model hiện diện không đồng nghĩa workflow đã triển khai.
- `EMR-LOCK-INPUT-01` chỉ xác nhận domain invariant: `LuotKham.lockEmr` từ chối chữ ký null/blank và `doctorId <= 0` mà không mutation/event, nhận chữ ký nonblank cùng ID dương tổng hợp một lần, rồi từ chối khóa lặp. Đây không xác nhận ai được phép khóa, không bật `EMR-01` và không nối persistence/UI/audit/workflow.
- Có application query/JDBC read và command/JDBC create-update cho ba loại tiền sử đã duyệt. DOCTOR, active exact facility+department, patient-in-scope và audit được kiểm soát; read audit commit-before-response, write audit cùng transaction. SQL Server persistence/rollback acceptance đã pass riêng trên DB tổng hợp mới. Doctor UI chưa được nối; legacy rows không có provenance vẫn bị chặn. Encounter binding, post-lock edits, delete và loại tiền sử khác vẫn chưa được duyệt.
- Lưu nháp, chẩn đoán, sinh hiệu, ký/khóa/mở EMR và lịch sử audit chưa có bằng chứng end-to-end được nghiệm thu; Phase 3 và Phase 4 chưa hoàn tất.

### Các phân hệ sau và prototype UI

- CLS, pharmacy/inventory, billing, catalogs, reports, HR/payroll và extended administration có thể có domain models, schema, panel hoặc dialog; mức hiện diện đó không chứng minh CRUD, transaction hoặc truy vấn thật.
- Task board ghi các luồng này phần lớn planned/blocked hoặc chưa có acceptance. Ví dụ panel CLS, báo cáo và bàn khám bác sĩ còn dùng nội dung mẫu/tĩnh; không xem các mẫu đó là dữ liệu lưu trong hệ thống. DOCX/print preview cũng chưa được xác nhận nối với dữ liệu persist và biểu mẫu nghiệp vụ được duyệt.
- Phase 5–9 không được tuyên bố hoàn tất. CI workflow có source local nhưng chưa có bằng chứng GitHub-hosted run; release/operations và tích hợp ngoài còn mở.

## 4. README và hướng dẫn sử dụng

Root `README.md` có hướng dẫn onboarding và hiện chỉ rõ lệnh build artifact desktop, chạy migration runner từ shaded JAR và mở ứng dụng bằng `java -jar`. Cấu hình local qua IntelliJ hoặc biến môi trường được phân biệt; artifact đã được kiểm tra có `KhoiDong-Class`, có migration runner và chỉ chứa `db.properties.example` (không chứa `db.properties`). Đây là kiểm tra cấu trúc artifact, không phải một lượt khởi động ứng dụng hoặc migration trên database. README không thay thế runbook triển khai/vận hành. Tài liệu README có nhắc tài khoản/mật khẩu seed demo; báo cáo này không sao chép giá trị đó.

## 5. Trạng thái roadmap

Roadmap hiện có 10 phase, đánh số 0–9. Phase 0 nền tảng và Phase 1 tiếp đón đang ở trạng thái tiến hành; Phase 2 authorization/audit baseline đang tiến hành nhưng là gate chưa đạt cho luồng nhạy cảm; Phase 3–9 chưa có đủ bằng chứng nghiệm thu end-to-end. Status của task cụ thể không đồng nghĩa phase tương ứng đã hoàn tất. Chi tiết dependency, quyết định và tiêu chí còn lại nằm trong [lộ trình phase](docs/ke-hoach-trien-khai/02-cac-dot-trien-khai.md) và [model task board](docs/ke-hoach-trien-khai/03-model-task-board.md).

## 6. Giới hạn của đánh giá

- Báo cáo đối chiếu source và các tài liệu/bằng chứng được dẫn trong kế hoạch tính đến 06/11/2026 trên checkout local của nhánh `feature/patient-reception-workflow`; task worktrees đã được tích hợp cục bộ. Không có bằng chứng push, phát hành hoặc GitHub-hosted CI run.
- Maven verify được phân biệt với các lượt SQL Server opt-in được task board ghi nhận; không kết nối database trong lần cập nhật báo cáo này.
- Không quan sát cửa sổ Swing trực tiếp; không đánh giá trực quan layout hay thao tác toàn desktop.
- Không đánh giá tuân thủ pháp lý/nghiệp vụ y tế, BHYT, hóa đơn, an toàn thuốc, tích hợp ngoài hoặc khả năng vận hành production. Không có kết luận “production ready”.

## 7. Nguồn đối chiếu chính

- [Hiện trạng ngày 06/11/2026](docs/ke-hoach-trien-khai/00-hien-trang.md)
- [Lộ trình theo phase](docs/ke-hoach-trien-khai/02-cac-dot-trien-khai.md)
- [Model task board và bằng chứng](docs/ke-hoach-trien-khai/03-model-task-board.md)
- `docs/ke-hoach-trien-khai/phase1-tiensuben-review.md`, `phase2-authz-enforcement-review.md`, `phase2-read-audit-decision.md` và các review/policy được các tài liệu trên dẫn chiếu
- `README.md`, `clinic-application` authentication/authorization/reception handlers, `clinic-infrastructure` JDBC repositories/migrations, và `clinic-desktop` reception composition/UI/tests

## 8. Guidance đã đọc cho lần cập nhật này

- Orchestrator baseline `docs/PROJECT_CONVENTIONS.md` (nội dung được cung cấp trong task; không có bản sao tại repository path đó)
- `README.md`
- `DOMAIN_ANALYSIS.md`
- `ERD_DIAGRAM.md`
- `UI_CODING_STANDARDS.md`
- `docs/ke-hoach-trien-khai/README.md`
- `docs/ke-hoach-trien-khai/02-cac-dot-trien-khai.md`
- `docs/ke-hoach-trien-khai/03-model-task-board.md`
