# Đề xuất chính sách bảo mật Phase 2

**Trạng thái:** Chủ dự án đã duyệt role/scope/session/audit policy dưới đây ngày 05/11/2026. Chưa triển khai code. Chính sách khóa tài khoản sau đăng nhập sai vẫn chờ quyết định riêng.
**Căn cứ:** source review tại `phase2-auth-persistence-review.md`, `phase2-desktop-session-review.md`, `phase2-domain-audit-review.md` và lựa chọn của chủ dự án ngày 05/11/2026.

## Đã được chủ dự án xác nhận

| Chủ đề | Quyết định |
|---|---|
| Nhiều vai trò | Mỗi phiên chọn đúng một vai trò đã được gán cho tài khoản. Role chưa được cấp quyền phải bị từ chối mặc định. |
| Phạm vi dữ liệu | Mỗi phiên chọn đúng một cặp cơ sở/khoa từ assignment hợp lệ của tài khoản; mọi truy vấn/command phải khớp cả cơ sở lẫn khoa. Không cấp quyền xem liên khoa hoặc toàn hệ thống. |
| Phiên làm việc | Tự khóa sau 15 phút không thao tác; hết phiên sau tối đa 12 giờ. Auto-lock che workspace, giữ bản nháp và yêu cầu xác thực lại cùng tài khoản/vai trò. |
| Audit | Ghi nhận event set được liệt kê dưới đây; audit thay đổi nghiệp vụ trong cùng transaction, lỗi audit làm rollback; chỉ append; không purge tự động khi chưa chốt retention. Theo DEC-AUTHZ-10, `AUDIT_READ` bị deny cho mọi role và không có audit viewer/query được cấp quyền. |

Các quyết định này là contract đầu vào cho Phase 2. Trong phiên, mọi Command/Query nhạy cảm vẫn phải được Application xác thực lại theo actor, role và cặp cơ sở/khoa đã chọn; UI không phải ranh giới phân quyền.

Ngày 05/11/2026, Chủ dự án duyệt ma trận role/action tại [phase2-role-permission-matrix-approval.md](phase2-role-permission-matrix-approval.md). Scope đã được chốt nghiêm ngặt theo `KHOA_PHONG`; không áp dụng các dòng `CO_SO` như một ngoại lệ liên khoa. Những thao tác chưa được quyết định riêng vẫn `DENY`.

## Quy tắc đã duyệt để triển khai

### 1. Chọn phạm vi làm việc khi đăng nhập

Sau xác thực và chọn một role, người dùng chọn một cặp cơ sở–khoa được gán. Nếu chỉ có một cặp hợp lệ, UI có thể chọn sẵn cặp đó. Danh sách lấy từ assignment trong CSDL; người dùng không thể tự thêm scope. Phòng khám được chọn trong cặp cơ sở/khoa; Application xác nhận phòng thuộc scope trước thao tác.

### 2. Timeout, khóa phiên và đăng xuất

- Tự khóa ứng dụng sau **15 phút không thao tác**; cấu hình qua `ThamSoHeThong`, không hardcode.
- Giới hạn phiên tối đa **12 giờ**; hết hạn yêu cầu đăng nhập lại.
- Khi auto-lock, che toàn bộ workspace và yêu cầu xác thực lại cùng tài khoản/vai trò trước khi tiếp tục; không đóng form hoặc xóa bản nháp chưa lưu. Hạn chế rủi ro lộ dữ liệu khi máy dùng chung.
- Đăng xuất chủ động xóa `UserSession`, đóng workspace và quay về `LoginForm`; cảnh báo rõ nếu form đang có dữ liệu chưa lưu.
- Vô hiệu hóa tài khoản hoặc thu hồi role/scope có hiệu lực với thao tác nhạy cảm kế tiếp; Application kiểm tra trạng thái hiện hành, không tin dữ liệu role/scope do UI gửi lên.

### 3. Sự kiện và dữ liệu Audit

Ghi nhận các nhóm sự kiện đã duyệt:

- Đăng nhập thành công/thất bại, đăng xuất, timeout/thu hồi phiên.
- Từ chối quyền; xem/in/xuất hồ sơ bệnh nhân hoặc bệnh án.
- Tạo/sửa/xóa mềm dữ liệu bệnh nhân; sửa, ký, khóa hoặc mở khóa bệnh án.
- Kê/duyệt/cấp phát thuốc; nhập/xuất/điều chỉnh tồn; lập hóa đơn, thu, hoàn tiền.
- Thay đổi tài khoản, role, assignment cơ sở/khoa, ma trận quyền và cấu hình bảo mật.

Mỗi bản ghi audit lưu actor ID, role hiệu lực, cơ sở/khoa hiệu lực, mã hành động, loại và ID bản ghi đích, UTC time, kết quả, mã lý do khi cần, workstation/correlation ID. Không ghi password/hash/token/connection string, CCCD/BHYT đầy đủ hoặc nội dung lâm sàng tự do. Audit hồ sơ chỉ giữ định danh lượt khám cần thiết và mã hành động/lý do truy cập.

Tra cứu audit chỉ cho phép qua quyền riêng, mặc định từ chối. Ma trận được duyệt vẫn giữ `AUDIT_READ` là `DENY` cho mọi role; chưa triển khai reader hoặc cấp quyền này.

### 4. Tính nguyên tử, bất biến và thời hạn lưu Audit

Với thay đổi nghiệp vụ, ghi audit trong cùng transaction; nếu ghi audit thất bại thì rollback. Audit chỉ append, ứng dụng không cho sửa/xóa. Không purge tự động cho tới khi có retention được phê duyệt. Thời hạn lưu và role cụ thể có quyền tra cứu vẫn cần chốt trước khi cấu hình retention/cấp quyền.

## Chính sách còn cần quyết định riêng

### Khóa tài khoản khi đăng nhập sai

Tài liệu giao diện có nêu ngưỡng 5 lần sai; ngưỡng, thời gian khóa, cách mở khóa và sự kiện audit cho lần đăng nhập thất bại chưa được duyệt. Không triển khai khóa tài khoản hoặc migration cho chính sách này trước khi chủ dự án chốt.

## Contract và kiểm thử sau khi duyệt

1. Dùng context bất biến do Application tạo sau xác thực, gồm employee ID, một active role, assignments hợp lệ và scope làm việc đã chọn. Từ chối context thiếu/sai; không lấy identity từ setter UI làm nguồn tin cậy.
2. Chuẩn hóa enums/domain/schema cho action và data scope; seed ma trận quyền tối thiểu theo policy đã duyệt. Bổ sung repository JDBC cho ma trận và audit.
3. Áp dụng default-deny authorization và filter scope trong Application/query/repository cần thiết; UI chỉ phản ánh quyền để hỗ trợ thao tác.
4. Nối session lifecycle vào `LoginForm`, `UserSession` và dashboard; kiểm thử login/logout/idle timeout/absolute expiry/revocation bằng clock giả lập.
5. Dùng synthetic fixtures để kiểm thử multi-role selection, role không được cấp, scope chéo cơ sở/khoa, audit event completeness/redaction/immutability, transaction rollback khi audit lỗi và truy vấn audit có phân quyền.
6. Chạy SQL Server integration tests trên database synthetic riêng; không dùng database hoặc hồ sơ bệnh nhân thật.

## Chốt trạng thái

Các policy role/scope/session/audit đã được duyệt và có thể đưa vào implementation contract. Phase 2 vẫn đang triển khai; login lockout và audit retention cụ thể chưa được quyết định. Theo DEC-AUTHZ-10, `AUDIT_READ` bị deny cho mọi role, nên không có role nào được cấp quyền tra cứu audit; tiếp tục không purge tự động và không triển khai lockout.
