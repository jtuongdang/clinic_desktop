# SEC-AUTHZ-02A — Contract evaluator quyền mặc định từ chối

**Policy nguồn:** [ma trận đã được Owner duyệt](phase2-role-permission-matrix-approval.md), [security policy](phase2-security-policy-proposal.md), [source map scope](phase2-authz-enforcement-review.md).

## Mục tiêu

Tạo Application service dùng chung để quyết định một actor có được thực hiện action trên tài nguyên thuộc scope nào không. Service chạy ở `clinic-application`, độc lập Swing, và mặc định từ chối khi thiếu context, assignment không còn hợp lệ, session không hoạt động, ma trận không có grant phù hợp hoặc dependency lỗi.

Task này chỉ tạo evaluator/contract và unit tests; chưa gắn vào reception handler, chưa thay đổi query/repository filters và chưa seed quyền. Không mở `AUDIT_READ` hoặc action đang chờ `DEC-AUTHZ-*`.

## Contract kỹ thuật

- `PhanQuyenNguCanh` bất biến: employee ID, role ID/code, đúng một facility ID + department ID, session UUID. Không chứa username, password/hash, bệnh nhân/CCCD/BHYT, free text hay dữ liệu UI.
- `PhanQuyenYeuCau`: context, module code, `HanhDongQuyen`, và target facility/department IDs của tài nguyên.
- `PhanQuyenQuyetDinh`: allow/deny cùng mã lý do ổn định, không đưa dữ liệu định danh vào thông báo.
- `PhanQuyenDichVu` phải kiểm tra mỗi lần gọi:
  1. context/request hợp lệ;
  2. session adapter xác nhận cùng session ID, employee, role và pair vẫn active/unlocked/chưa timeout;
  3. employee còn active/non-deleted;
  4. role hiện tại còn được gán cho employee và role code khớp;
  5. facility/department pair vẫn được gán và `department.facilityId == facilityId`;
  6. target pair bằng chính xác cả facility và department của context;
  7. có duy nhất assignment active tương ứng module/action, `duocPhep=true`, `PhamViDuLieu.KHOA_PHONG`.
- `AUDIT_READ` luôn deny, kể cả DB có row allow. Các scope legacy `TOAN_HE_THONG`, `CO_SO`, `CHI_NHANH` không được mở rộng quyền. Missing/duplicate/unknown permission hoặc exception ở dependency đều deny.
- Application nhận dependency qua port; có thể tái dùng repository hiện hữu cho role/scope/matrix, nhưng không trả `NhanVien` hoặc password hash khỏi service. Session validity cần port riêng; task này không tin vào boolean hoặc ID do UI tự khai.
- Decision không ghi audit trong task này; handler integration cần nối audit-denial event và phải bảo đảm không ghi PII.

## Ngoài phạm vi

- Không nối evaluator vào use cases, không sửa `NguoiDungPhien`, LoginForm, reception UI, repository SQL hoặc schema.
- Không seed ALLOW/DENY rows, không tạo màn hình quản trị quyền, không cho phép sửa permission matrix.
- Không triển khai audit reader, retention/purge, login lockout hoặc unresolved DEC actions.
- Không SQL Server connection trong agent task; chỉ synthetic unit tests.

## Acceptance

- Tests cho allow đúng role/action/session/assignment/target pair; deny target khác khoa cùng cơ sở; deny cơ sở khác.
- Deny missing/expired/locked/mismatched session, employee inactive, role/scope revoked hoặc pair sai facility.
- Deny missing permission, false permission, duplicate permission rows, mọi legacy wide scope, `AUDIT_READ` kể cả role ADMIN, unknown module/action/null input.
- Dependency exception trả deny an toàn; quyết định và message không chứa username, patient identifiers, insurance identifiers hoặc free text.
- Tất cả tests chỉ dùng synthetic fixtures; JDK 17 `mvn -Djava.awt.headless=true clean verify` và `git diff --check` pass.
