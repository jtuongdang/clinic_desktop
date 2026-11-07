# Task pilot thực tế cho AI Agents — Clinic Management Desktop

**Ngày rà soát:** 2026-11-05
**Repository:** `desktop_clinic/desktop`
**Mục tiêu:** chuẩn bị các task nghiệp vụ nhỏ, có tiêu chí nghiệm thu và đủ độc lập để thử điều phối nhiều coding agent.

## 1. Kết luận rà soát

Luồng Tiếp đón hiện có các thành phần chính trong working tree: `RegisterPatientCommand`/handler, repository bệnh nhân, bảo hiểm, phòng khám, hàng đợi và form Swing. Handler kiểm tra CCCD trước khi insert, sau đó lưu hồ sơ, bảo hiểm và lượt khám trong transaction. Tuy nhiên:

- Kiểm tra CCCD hiện là check-then-insert; hai lượt đăng ký đồng thời vẫn có thể cùng vượt qua bước kiểm tra.
- SQL Server migration V1 tạo `IX_BenhNhan_CCCD` dạng index thường, chưa có unique constraint/index cho CCCD đang hoạt động.
- Handler bắt `RuntimeException` và trả thông báo chung, nên lỗi unique từ DB nếu bổ sung sau này cần được phân loại để người tiếp đón biết phải tìm/chọn hồ sơ hiện hữu.
- `PhoneNumber` có regex `[3|5|7|8|9]`; dấu `|` bên trong character class đang được hiểu là ký tự hợp lệ. Đây là lỗi validation độc lập, nhỏ và phù hợp làm task pilot.
- Trước lượt rà soát tài liệu, trạng thái Đợt 1 giữa roadmap và trang phân hệ không thống nhất. Đã đồng bộ trong lần cập nhật này về `[~]` cho đến khi code được commit, kiểm thử và nghiệm thu.

## 2. Điều kiện trước khi giao task cho Orchestrator

Checkout hiện có thay đổi chưa commit ở form Tiếp đón, application handlers, repository implementations và kế hoạch triển khai. Worktree của Orchestrator được tạo từ commit hiện tại, nên các Agent sẽ không nhìn thấy những thay đổi này nếu chưa có trong commit/branch nguồn.

**Không giao Task 2 hoặc Task 3 cho đến khi phần Tiếp đón hiện tại được commit vào branch nguồn mà Orchestrator sử dụng.** Không tự stage/commit thay người phát triển.

Task 1 chỉ dựa trên mã Domain đã có trong repository; có thể chạy độc lập nếu branch nguồn chứa `PhoneNumber` như checkout hiện tại.

## 3. Các task đề xuất

### Task 1 — Sửa và kiểm thử quy tắc số điện thoại

**Ưu tiên:** P1
**Phân hệ:** Domain / Định danh
**Mục tiêu nghiệp vụ:** chỉ chấp nhận số điện thoại di động Việt Nam theo quy tắc hiện có; không được chấp nhận ký tự `|` hoặc dữ liệu sai độ dài/đầu số.

**Phạm vi được sửa:**

- `clinic-domain/src/main/java/vn/clinicmanagement/domain/common/valueobjects/PhoneNumber.java`
- Test tương ứng dưới `clinic-domain/src/test/java/vn/clinicmanagement/domain/common/valueobjects/`

**Tiêu chí nghiệm thu:**

1. Chấp nhận các định dạng hợp lệ mà dự án đang chủ trương hỗ trợ (`0...` và `+84...`); khoảng trắng, dấu chấm, dấu gạch ngang được xử lý theo hành vi hiện có.
2. Từ chối `0|12345678`, số thiếu/thừa chữ số, đầu số không hợp lệ, chuỗi rỗng và null.
3. Test không dùng dữ liệu bệnh nhân thật; chỉ dùng số tổng hợp.
4. Không thay đổi form hoặc chuẩn hóa sang quy tắc viễn thông khác ngoài yêu cầu đã nêu.

**Kiểm tra:** `mvn -pl clinic-domain -am test`.

### Task 2 — Ngăn trùng CCCD khi tiếp đón đồng thời

**Ưu tiên:** P0
**Phân hệ:** Tiếp đón / Hồ sơ bệnh nhân / Persistence
**Mục tiêu nghiệp vụ:** một CCCD không thể gắn với hai hồ sơ bệnh nhân đang hoạt động, kể cả khi hai yêu cầu đăng ký chạy đồng thời.

**Hợp đồng cần thống nhất trước khi Agent code:**

- Quy tắc duy nhất áp dụng cho CCCD không rỗng của hồ sơ `isDeleted=0`.
- Không tự xóa, gộp hoặc sửa hồ sơ trùng đã tồn tại. Migration phải phát hiện trùng đang hoạt động và dừng với thông báo rõ, không ghi nhận migration thành công.
- Vi phạm unique từ SQL Server phải được nhận diện và ánh xạ thành lỗi nghiệp vụ `PATIENT_DUPLICATE_CCCD`, cùng hướng dẫn tìm hồ sơ hiện có.
- Giữ check trước khi ghi để UX phản hồi nhanh; unique index ở DB là lớp bảo vệ cuối cùng.

**Phạm vi dự kiến:**

- `clinic-infrastructure/src/main/resources/migrations/V4__Enforce_Active_Patient_CCCD_Uniqueness.sql`
- `clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/DatabaseMigrationRunner.java`
- `clinic-infrastructure/src/main/java/vn/clinicmanagement/infrastructure/persistence/patient/BenhNhanRepositoryImpl.java`
- `clinic-application/src/main/java/vn/clinicmanagement/application/reception/RegisterPatientHandler.java`
- Unit/integration tests liên quan ở `clinic-application` và `clinic-infrastructure`.

**Tiêu chí nghiệm thu:**

1. Migration được đăng ký tuần tự sau V3 và tạo filtered unique index cho CCCD đang hoạt động, bỏ qua null/blank theo quy tắc đã thống nhất.
2. Nếu DB đã có CCCD trùng đang hoạt động, migration fail an toàn, báo số lượng/mã hồ sơ cần xử lý nhưng không in đầy đủ CCCD và không xóa/gộp dữ liệu.
3. Hai thao tác đồng thời cùng CCCD không thể tạo hai hồ sơ active; một thao tác nhận kết quả trùng rõ ràng.
4. Lỗi DB không làm ghi dở hồ sơ/bảo hiểm/lượt khám; transaction rollback.
5. Có test mô phỏng duplicate và rollback. Nếu không có SQL Server test instance thì ghi rõ phần nào chỉ được kiểm tra bằng unit test; không tuyên bố đã kiểm thử integration DB.

**Kiểm tra:** `mvn -pl clinic-application,clinic-infrastructure -am verify`; integration test SQL Server chỉ chạy với DB thử nghiệm cô lập.

### Task 3 — Hoàn thiện UX nhận biết hồ sơ CCCD đã tồn tại

**Ưu tiên:** P1
**Phân hệ:** Tiếp đón / Desktop
**Phụ thuộc:** Task 2 và phần code Tiếp đón đã có trong branch nguồn.
**Mục tiêu nghiệp vụ:** nhân viên không mất dữ liệu đang nhập và có thể chuyển sang hồ sơ hiện hữu khi CCCD đã tồn tại.

**Phạm vi dự kiến:**

- `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/ReceptionPanel.java`
- `clinic-desktop/src/main/java/vn/clinicmanagement/desktop/modules/reception/components/PatientRegistrationFormPanel.java`
- Các test UI/model phù hợp nếu dự án đã có hạ tầng kiểm thử Swing.

**Tiêu chí nghiệm thu:**

1. Khi nhận `PATIENT_DUPLICATE_CCCD`, UI hiển thị hướng dẫn tìm hồ sơ hiện hữu, không hiện lỗi kỹ thuật SQL.
2. Dữ liệu form không bị xóa sau lỗi; không tạo lượt khám mới.
3. Tìm và chọn bệnh nhân hiện hữu nạp đúng dữ liệu được phép chỉnh sửa; thao tác tiếp nhận tạo tối đa một lượt khám.
4. Tác vụ DB không chạy trên EDT; giữ hành vi nền hiện có của SwingWorker.
5. Kiểm thử thủ công hoặc tự động dùng dữ liệu tổng hợp, ghi lại các bước và kết quả.

## 4. Cách phân chia 3 Agent

1. Giao Task 1 cho một Agent để có pilot độc lập, không chồng file.
2. Giao Task 2 cho một Agent sau khi phần Tiếp đón hiện tại đã nằm trong branch nguồn; thống nhất contract lỗi trước khi chạy.
3. Giao Task 3 cho Agent khác sau Task 2 được review/commit hoặc cung cấp contract đã chốt. Không cho Task 2 và Task 3 sửa đồng thời cùng `RegisterPatientHandler`/form.

Nếu muốn chạy 3 Agent cùng lúc ngay từ đầu, nên chọn 3 task không phụ thuộc nhau và có file scope tách biệt; bộ Task 1–3 ở trên **không hoàn toàn độc lập**, do Task 3 phụ thuộc kết quả Task 2.

## 5. Quy tắc dữ liệu và an toàn

- Chỉ dùng dữ liệu tổng hợp trong unit/integration test.
- Không đưa CCCD, số điện thoại, tên hoặc thông tin sức khỏe thật vào prompt, log, fixture hay Pull Request.
- Không tự chạy migration trên database clinic đang sử dụng; chỉ chạy trên DB test được cấu hình riêng.
- Không push hoặc tạo Pull Request trước khi chủ dự án yêu cầu quy trình đó; không merge tự động.

## 6. Trạng thái đề xuất

- [ ] Xác nhận/commit phần Tiếp đón hiện tại vào branch nguồn của Orchestrator.
- [ ] Chạy Task 1 làm pilot đầu tiên.
- [ ] Chốt quy tắc xử lý CCCD trùng với dữ liệu đã xóa mềm nếu nghiệp vụ cần khôi phục hồ sơ cũ.
- [ ] Chạy Task 2 trên DB SQL Server thử nghiệm.
- [ ] Sau Task 2, giao Task 3 và đồng bộ trạng thái tài liệu Đợt 1.
