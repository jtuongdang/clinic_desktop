# SEC-AUTHZ-01A — Contract nền tảng ma trận quyền

**Trạng thái:** Được phép triển khai theo ma trận Owner đã duyệt tại [phase2-role-permission-matrix-approval.md](phase2-role-permission-matrix-approval.md). Đây là bước nền; task này chưa cấp role grant và chưa bật guard.

## Phạm vi

- Mở rộng `HanhDongQuyen` thêm `IN`, `XUAT_FILE`, `MO_KHOA_EMR`, `AUDIT_READ` với value số ổn định, không đổi giá trị hiện có.
- Đặt default của `MaTranPhanQuyen` trong domain thành `PhamViDuLieu.KHOA_PHONG` và `duocPhep=false`.
- Tạo migration V7 idempotent sau V6 để đặt SQL default `phamViDuLieu='KHOA_PHONG'`, `duocPhep=0`; thêm unique filtered index cho một assignment hoạt động trên mỗi `(maVaiTro, phanHe, hanhDong)`; cấm mọi row active được phép có scope khác `KHOA_PHONG`.
- Giữ các enum scope legacy để đọc dữ liệu cũ, nhưng không coi `TOAN_HE_THONG`, `CO_SO`, `CHI_NHANH` là grant hợp lệ. Migration không sửa/xóa/thu hẹp dữ liệu quyền đang tồn tại; nếu phát hiện row active không an toàn hoặc duplicate, migration phải dừng và báo rõ.
- Bổ sung unit/fake-JDBC tests và cập nhật fresh-database SQL Server opt-in test xác nhận V7, defaults, constraint/index, thứ tự migration và không tự seed quyền.

## Ngoài phạm vi

- Không seed ma trận ALLOW; không tạo user/role mới.
- Không triển khai `PhanQuyenDichVu`, guard, audit reader, permission UI hoặc tự cấp `AUDIT_READ`.
- Không chạy SQL Server opt-in trong agent execution; root chỉ chạy trên database `clinic_test_*` riêng.
- Không kết nối `clinicDb`, không sửa migration lịch sử, không cập nhật hoặc xóa permission rows.

## Acceptance

1. Giá trị số hiện tại của 5 `HanhDongQuyen` không đổi; 4 action mới round-trip qua `fromValue`.
2. Domain và SQL mặc định fail-closed (`duocPhep=false`, `KHOA_PHONG`).
3. Migration chạy sau V6, idempotent theo convention hiện có và không cấp quyền.
4. SQL constraint chỉ cho phép row `duocPhep=1` với `phamViDuLieu='KHOA_PHONG'`; unique index ngăn hai row active trùng role/module/action.
5. Legacy unsafe/duplicate rows làm migration fail rõ ràng, không tự sửa data.
6. `git diff --check` và JDK 17 `mvn -Djava.awt.headless=true clean verify` pass; SQL Server opt-in vẫn skip mặc định.

