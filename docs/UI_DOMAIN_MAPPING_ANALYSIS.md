# Phân tích ánh xạ UI Desktop – Domain/Application

**Cập nhật:** 05/11/2026
**Phạm vi:** mã nguồn Clinic Management Desktop hiện có. Đây là bản phân tích trạng thái, không phải cam kết rằng toàn bộ chức năng đã hoàn thành.

## 1. Kiến trúc hiện tại

```text
clinic-desktop (Swing)
    -> clinic-application (commands, queries, handlers, KetQua)
        -> clinic-domain (models, value objects, repository ports, business rules)
clinic-infrastructure (JDBC repositories, migrations, transactions)
```

Đối chiếu theo module/package thực tế. Màn hình prototype hoặc domain model chưa đủ để xác nhận use case đang hoạt động end-to-end.

## 2. Ánh xạ luồng đã nối trong checkout

| Luồng | UI / entry point | Application | Infrastructure / domain | Trạng thái |
|---|---|---|---|---|
| Đăng nhập | `modules/auth/DangNhapBieuMau`, `XacThucGhepNoiGoc` | `AuthenticateNguoiDungCommand`, handler, `MatKhauBoBam` | `NhanVienRepository`, `INhanVienRepository` | Có wiring trong source; cần kiểm thử bảo mật, lỗi kết nối, quyền và session. |
| Đăng ký/tìm bệnh nhân | `modules/reception/TiepDonManHinh`, `BenhNhanDangKyBieuMauManHinh` | `RegisterBenhNhanCommand`/handler, `SearchBenhNhanQuery`/handler | Patient/insurance/room repositories; `BenhNhan`, `BaoHiem` | Có trong checkout hiện tại; các thay đổi Tiếp đón chưa commit và cần kiểm thử DB. |
| Hàng đợi tiếp đón | `TiepDonManHinh`, `PhongKhamPhongTrangThaiManHinh` | `GetTiepDonHangDoiQuery`, `TiepDonHangDoiCongKetNoi` | `TiepDonHangDoiRepository`, `LuotKham` | Tạo lượt khám/đọc hàng đợi có trong flow đăng ký; trạng thái gọi khám, refresh và tranh chấp cần nghiệm thu riêng. |

## 3. Các phân hệ có UI/model nhưng chưa chứng minh luồng persistence

| Phân hệ | UI hiện có | Domain liên quan | Cần xác minh/triển khai |
|---|---|---|---|
| Bác sĩ/EMR | `BacSiTramManHinh`, `KhamBenhManHinh`, các tabs/dialogs | `LuotKham`, `ChanDoan`, `SinhHieu`, `TienSuBenh`, ICD | Nạp queue thật; lưu nháp; kiểm soát chuyển trạng thái; lưu và khóa hồ sơ có audit. |
| Cận lâm sàng | `CanLamSangYeuCauManHinh`, `XetNghiemYeuCauTheManHinh`, `DichVuYeuCauManHinh` | `ChiDinhDichVu`, kết quả xét nghiệm, danh mục dịch vụ | Tạo chỉ định, nhập/duyệt kết quả, trả kết quả cho bàn khám. |
| Nhà thuốc/kho | `NhaThuocDispenseManHinh` và components | `DonThuoc`, `MatHang`, `TonKho`, phiếu nhập/xuất, FEFO | Nối đơn đã duyệt, tồn theo lô, transaction xuất kho, kiểm kê và audit. |
| Viện phí | `VienPhiHoaDonManHinh`, phiếu/print panels | `HoaDon`, `ChiTietHoaDon`, `PhieuThu`, công nợ | Nguồn chi phí, tính toán được nghiệp vụ duyệt, thu/hoàn và đối soát. |
| Danh mục/báo cáo/quản trị | `DanhMucManHinh`, `PhongKhamBaoCaoManHinh`, `HeThongCauHinhManHinh` | Catalog, identity, system, các giao dịch nghiệp vụ | CRUD có quyền, định nghĩa báo cáo, phạm vi dữ liệu và audit. |

Danh sách này là sơ đồ điều tra theo source hiện tại, không khẳng định mọi class UI đã hoàn thiện hoặc đang dùng dữ liệu thật. Theo dõi backlog chi tiết tại [kế hoạch triển khai](ke-hoach-trien-khai/README.md).

## 4. Nguyên tắc ánh xạ form

Với mỗi trường UI cần xác định: component → field trong command/query DTO → model/domain rule → cột DB → cách đọc lại. Cần ghi riêng nullable, chuẩn hóa, validation, quyền xem/sửa và thông báo lỗi. Không suy ra mapping chỉ từ tên trường tương tự.

Với form Tiếp đón, kiểm thử mapping tối thiểu cần bao gồm thông tin hành chính bệnh nhân, CCCD/liên hệ, thông tin thẻ bảo hiểm khi áp dụng, phòng khám/đối tượng/lý do khám và lượt khám phát sinh. Dùng dữ liệu tổng hợp.

## 5. Quy tắc kỹ thuật và giới hạn

- UI không gọi repository/JDBC trực tiếp.
- DB I/O không chạy trên Swing Event Dispatch Thread; cập nhật UI trên EDT.
- Ghi nhiều aggregate/bảng trong một nghiệp vụ phải có transaction boundary và kiểm thử rollback.
- Unique constraint, foreign key và transaction ở DB bảo vệ các invariant cần an toàn khi chạy đồng thời.
- Dữ liệu y tế, BHYT, thanh toán, thuốc và biểu mẫu pháp quy cần xác nhận từ người phụ trách nghiệp vụ; tài liệu này không chứng nhận tuân thủ quy định.
- DOCX engine tồn tại tại `clinic-desktop/.../core/docx`; việc tạo file không tự chứng minh nội dung biểu mẫu đúng hoặc đã được phê duyệt.

## 6. Cập nhật trạng thái

Phần Tiếp đón đang có code local nhưng chưa commit theo trạng thái checkout khi cập nhật tài liệu. Chỉ chuyển trạng thái sang `[x] Hoàn tất` sau review, build/test phù hợp, kiểm thử persistence/rollback và nghiệm thu các tiêu chí tại `docs/ke-hoach-trien-khai/phan-he/03-tiep-don-benh-nhan-nhan-su.md`.
