# Đề xuất chính sách bảo mật Phase 2

**Trạng thái:** Bản đề xuất, chờ chủ dự án duyệt. Chưa triển khai policy/code.
**Căn cứ:** source review tại `phase2-auth-persistence-review.md`, `phase2-desktop-session-review.md`, `phase2-domain-audit-review.md` và lựa chọn của chủ dự án ngày 05/11/2026.

## Đã được chủ dự án xác nhận

| Chủ đề | Quyết định |
|---|---|
| Nhiều vai trò | Mỗi phiên chọn đúng một vai trò đã được gán cho tài khoản. Role chưa được cấp quyền phải bị từ chối mặc định. |
| Phạm vi dữ liệu | Chỉ truy cập cơ sở/khoa đã được gán cho tài khoản; chưa cấp quyền xem toàn hệ thống. |

Hai quyết định trên là contract đầu vào đã được xác nhận. Việc có bắt buộc chọn một cơ sở/khoa làm ngữ cảnh làm việc cho mỗi phiên hay không vẫn cần quyết định; dù chọn phương án nào, thao tác chỉ được phép trên assignment hợp lệ của tài khoản.

## Nội dung cần duyệt

### 1. Chọn phạm vi làm việc khi đăng nhập

**Đề xuất:** Sau khi xác thực, nếu tài khoản có nhiều cơ sở/khoa được gán, yêu cầu chọn một cặp cơ sở–khoa đang làm việc cho phiên đó. Nếu chỉ có một cặp thì tự chọn. Danh sách lựa chọn lấy từ assignment trong CSDL; UI không thể tự thêm scope. Mỗi Command/Query nhạy cảm phải được Application kiểm tra lại actor, role, cơ sở/khoa và tài nguyên đích.

**Cần duyệt:** Khi một tài khoản có nhiều assignment hợp lệ, phiên cần chọn một cơ sở/khoa đang làm việc hay được thao tác trên mọi assignment đã cấp? Phòng khám là một phần của scope bắt buộc hay chỉ là lựa chọn vận hành?

### 2. Timeout, khóa phiên và đăng xuất

**Đề xuất:**

- Tự khóa ứng dụng sau **15 phút không thao tác**; cấu hình qua `ThamSoHeThong`, không hardcode.
- Giới hạn phiên tối đa **12 giờ**; hết hạn yêu cầu đăng nhập lại.
- Khi auto-lock, che toàn bộ workspace và yêu cầu xác thực lại cùng tài khoản/vai trò trước khi tiếp tục; không đóng form hoặc xóa bản nháp chưa lưu. Hạn chế rủi ro lộ dữ liệu khi máy dùng chung.
- Đăng xuất chủ động xóa `UserSession`, đóng workspace và quay về `LoginForm`; cảnh báo rõ nếu form đang có dữ liệu chưa lưu.
- Vô hiệu hóa tài khoản hoặc thu hồi role/scope có hiệu lực với thao tác nhạy cảm kế tiếp; Application kiểm tra trạng thái hiện hành, không tin dữ liệu role/scope do UI gửi lên.

**Cần duyệt:** 15 phút / 12 giờ có phù hợp ca làm việc không? Auto-lock giữ form nháp sau khi che màn hình hay xóa form nháp khi phiên hết hạn?

### 3. Sự kiện và dữ liệu Audit

**Đề xuất ghi nhận:**

- Đăng nhập thành công/thất bại, đăng xuất, timeout/thu hồi phiên.
- Từ chối quyền; xem/in/xuất hồ sơ bệnh nhân hoặc bệnh án.
- Tạo/sửa/xóa mềm dữ liệu bệnh nhân; sửa, ký, khóa hoặc mở khóa bệnh án.
- Kê/duyệt/cấp phát thuốc; nhập/xuất/điều chỉnh tồn; lập hóa đơn, thu, hoàn tiền.
- Thay đổi tài khoản, role, assignment cơ sở/khoa, ma trận quyền và cấu hình bảo mật.

**Trường đề xuất:** actor ID, role hiệu lực, cơ sở/khoa hiệu lực, mã hành động, loại và ID bản ghi đích, thời điểm UTC, kết quả, mã lý do khi cần, workstation/correlation ID. Không ghi password/hash/token/connection string, CCCD/BHYT đầy đủ, hoặc nội dung lâm sàng tự do vào log hệ thống. Audit hồ sơ chỉ giữ định danh lượt khám cần thiết và mã hành động/lý do truy cập.

**Cần duyệt:** Danh sách sự kiện trên có đủ không? Xem hồ sơ cần bắt buộc nhập lý do hay chỉ ghi actor/action/time? Ai được phép tra cứu audit?

### 4. Tính nguyên tử, bất biến và thời hạn lưu Audit

**Đề xuất:** Với thay đổi nghiệp vụ, ghi audit trong cùng transaction; nếu ghi audit thất bại thì rollback thay đổi. Audit chỉ thêm mới, không cho CRUD/sửa/xóa qua ứng dụng; trang tra cứu chỉ đọc và có kiểm tra quyền riêng. Chưa tự động purge audit cho đến khi có thời hạn được phê duyệt.

**Cần duyệt:** Có chấp nhận fail-closed (audit lỗi thì giao dịch sửa dữ liệu cũng thất bại) không? Cần lưu audit trong bao lâu và nhóm nào được tra cứu? Thời hạn phải được chủ dự án/đầu mối tuân thủ xác nhận trước khi cấu hình purge.

### 5. Khóa tài khoản khi đăng nhập sai

**Đề xuất chờ xác nhận:** Tài liệu giao diện có nêu ngưỡng 5 lần sai; chưa đủ căn cứ để tự áp dụng vào hệ thống. Cần chốt ngưỡng, thời gian khóa, cách mở khóa và việc có ghi audit mỗi lần sai trước khi tạo migration/use case.

## Contract và kiểm thử sau khi duyệt

1. Dùng context bất biến do Application tạo sau xác thực, gồm employee ID, một active role, assignments hợp lệ và scope làm việc đã chọn. Từ chối context thiếu/sai; không lấy identity từ setter UI làm nguồn tin cậy.
2. Chuẩn hóa enums/domain/schema cho action và data scope; seed ma trận quyền tối thiểu theo policy đã duyệt. Bổ sung repository JDBC cho ma trận và audit.
3. Áp dụng default-deny authorization và filter scope trong Application/query/repository cần thiết; UI chỉ phản ánh quyền để hỗ trợ thao tác.
4. Nối session lifecycle vào `LoginForm`, `UserSession` và dashboard; kiểm thử login/logout/idle timeout/absolute expiry/revocation bằng clock giả lập.
5. Dùng synthetic fixtures để kiểm thử multi-role selection, role không được cấp, scope chéo cơ sở/khoa, audit event completeness/redaction/immutability, transaction rollback khi audit lỗi và truy vấn audit có phân quyền.
6. Chạy SQL Server integration tests trên database synthetic riêng; không dùng database hoặc hồ sơ bệnh nhân thật.

## Chốt trạng thái

Phase 2 vẫn chưa đạt gate. Chỉ hai quyết định ở phần “Đã được chủ dự án xác nhận” được xem là hiện hành. Các giá trị và quy tắc chi tiết còn mở phải được duyệt hoặc chỉnh sửa trước khi đưa vào implementation contract.
