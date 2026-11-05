# Phase 2 — Implementation contract

**Trạng thái:** Hợp đồng kỹ thuật cho các policy được duyệt ngày 05/11/2026. Các task Phase 2 phải đọc file này trước khi sửa code.
**Policy nguồn:** [phase2-security-policy-proposal.md](phase2-security-policy-proposal.md).

## 1. Quyết định nghiệp vụ được duyệt

- Mỗi phiên chỉ có một role đã gán cho tài khoản; role/action không được phép thì từ chối mặc định.
- Mỗi phiên chọn một cặp cơ sở/khoa lấy từ assignment còn hiệu lực. Cặp hợp lệ phải đồng thời có assignment cơ sở, assignment khoa và quan hệ `Khoa.maCoSo = PhanQuyenCoSo.maCoSo`; cơ sở/khoa inactive hoặc soft-deleted không được chọn.
- Tự khóa sau 15 phút không thao tác; tuổi thọ tuyệt đối tối đa 12 giờ. Auto-lock che workspace, giữ draft, và chỉ mở lại sau xác thực cùng tài khoản/role. Logout chủ động cảnh báo draft chưa lưu, xóa context và đóng workspace.
- Thay đổi role/scope/account có hiệu lực ở thao tác nhạy cảm kế tiếp; Application phải kiểm tra trạng thái mới từ nguồn tin cậy.
- Audit cho các nhóm sự kiện ở policy source; thay đổi nghiệp vụ và audit phải cùng transaction. Audit write failure rollback cả thao tác nghiệp vụ. Audit chỉ append; không purge đến khi có retention được duyệt.
- Tra cứu audit yêu cầu quyền riêng `AUDIT_READ`, mặc định deny. Không tự gán quyền này cho role nào trong seed.
- Không ghi password/hash/token/connection string, CCCD/BHYT đầy đủ hoặc narrative lâm sàng tự do vào audit/log.

## 2. Ranh giới tin cậy

- Login UI chỉ thu credential và lựa chọn từ danh sách roles/scopes do Application trả về; UI không tự tạo role, employee ID hoặc scope.
- Tạo một immutable authenticated context sau khi credential, role assignment và scope assignment đều được xác minh. Context tối thiểu: employee ID, role ID/code, facility ID, department ID, issued-at, last-activity, absolute-expiry và session ID.
- Command/query nhạy cảm nhận context từ composition/application boundary. Không dùng setter công khai của UI như nguồn xác thực. Role/scope phải được xác nhận lại theo assignment/permission matrix trước tác vụ nhạy cảm.
- UI chỉ ẩn/hiện thao tác theo quyền để hỗ trợ người dùng; authorization luôn được kiểm tra ở Application.
- Mọi ví dụ, fixture, integration test dùng dữ liệu synthetic; không đọc hoặc ghi hồ sơ bệnh nhân thật.

## 3. Thứ tự thực hiện

1. **SEC-DATA-01 — assignment reads (complete, `2d062f8` + `0d91903`):** repository đọc assigned role IDs/codes và cặp cơ sở/khoa hợp lệ, kèm tên cơ sở/khoa làm nhãn lựa chọn; fake-JDBC tests pass. Chưa thay UI/login.
2. **SEC-LOGIN-01 — role/scope selection:** chỉ sau khi password hợp lệ, Authentication trả choices từ persistence (không mở query assignment trước auth). Nếu có nhiều role/scope thì yêu cầu chọn; nếu mỗi loại chỉ có một choice thì có thể chọn sẵn. Tạo context phiên chỉ sau khi đủ đúng một role và một cặp scope. Hủy chọn thì không mở dashboard/phiên. Tài khoản thiếu role hoặc scope hợp lệ bị từ chối.
3. **SEC-AUTHZ-01 — default-deny authorization:** map role/action từ `MaTranPhanQuyen`, validate context và scope tại Application boundary; test allow/deny, cross-scope, revoked assignment. Không cấp mặc định `AUDIT_READ`.
4. **SEC-SESSION-01 — lifecycle:** idle 15 phút, absolute 12 giờ, overlay giữ draft, re-auth cùng user/role, logout và thu hồi context; dùng `Clock`/timer giả cho test.
5. **SEC-AUDIT-01 — persistence and transaction:** migration bổ sung fields cần thiết, append-only write/read repositories, audit cùng transaction với thay đổi nhạy cảm, fail-closed, redaction và quyền `AUDIT_READ` default-deny. SQL Server integration chỉ trên DB synthetic riêng.
6. **SEC-UI-01 — screens and acceptance:** nối login, scope selector, lock overlay, logout confirmation và permission-aware navigation; headless Swing tests rồi desktop smoke check.

## 4. Gates và chính sách chưa duyệt

- `SEC-01` phải qua unit, persistence/transaction integration và UI acceptance trước EMR, prescription/pharmacy, billing hoặc mở rộng sang dữ liệu thật.
- Login lockout sau đăng nhập sai chưa được duyệt. Không thêm cột, trigger, counter, thời lượng khóa hoặc UI cho lockout cho đến khi có quyết định riêng.
- Audit retention cụ thể và role được cấp `AUDIT_READ` chưa được quyết định. Giữ no-purge và default-deny; không seed quyền đọc audit.
- Nếu source/schema hiện có không thể biểu đạt đúng policy, task phải dừng ở contract/report và báo cáo phương án trước khi mở rộng migration hoặc thay dữ liệu.
