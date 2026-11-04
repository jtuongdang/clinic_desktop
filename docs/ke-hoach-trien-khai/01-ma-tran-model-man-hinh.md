# 01 — Ma trận Domain model → Màn hình Desktop → Phần còn thiếu

**Cập nhật:** 04/11/2026 (Đồng bộ Domain nâng cấp & Bảng 49 CSDL SQL Server)  
**Mức ưu tiên:** P0 = chặn dùng dữ liệu thật/an toàn; P1 = luồng vận hành cốt lõi; P2 = mở rộng quản trị và báo cáo.  
**Ghi chú:** Trạng thái “đã có UI” chỉ nói có panel/component trong source; cần nối use case qua tầng Application và Repository thay vì dùng Mock data.

---

## 1. Ma trận Phân hệ & Ánh xạ Chi tiết

| Phân hệ | Domain Models chính (Đã nâng cấp) | Màn hình/component hiện có | Màn hình & Chức năng cần hoàn thiện | Ưu tiên |
|---|---|---|---|---|
| **Xác thực & Bảo mật** | `NhanVien`, `VaiTro`, `NhanVienVaiTro`, `MaTranPhanQuyen`, `PhanQuyenCoSo`, `NhatKyHeThong` | `LoginForm`, `SystemConfigPanel`, `UserSession` | • Xác thực BCrypt hash từ CSDL SQL Server thật<br>• Lưu phiên làm việc (`UserSession`) an toàn<br>• Đổi mật khẩu lần đầu<br>• Phân quyền hiển thị Menu theo Vai trò | **P0** |
| **Bệnh nhân & BHYT** | `BenhNhan`, `BenhNhanCoSo`, `BaoHiem`, `TienSuBenh`, `DoiTuongKham` | `ReceptionPanel`, `PatientRegistrationFormPanel`, `MedicalPopupChooser` | • Bổ sung **15 trường thực chiến** trên form tiếp nhận (xem mục 2)<br>• Tìm kiếm CCCD/SĐT/Mã BN trực tiếp từ DB<br>• Quản lý thẻ BHYT (Mã ĐKBĐ, Mức hưởng, Hạn dùng)<br>• Cảnh báo hồ sơ trùng lặp | **P1** |
| **Tiếp đón & Hàng đợi** | `LuotKham`, `SinhHieu`, `ChanDoan`, `DoiTuongKham` | `ReceptionPanel`, `ClinicRoomStatusPanel`, `ReceptionTopToolbar` | • Sinh mã lượt khám & Cấp số thứ tự hàng đợi theo phòng<br>• Hàng đợi thời gian thực (Chờ khám, Đang khám, Hoàn tất)<br>• Ghi nhận chỉ số sinh hiệu (Huyết áp, Mạch, SpO2, BMI)<br>• Chuyển phòng khám chuyên khoa | **P1** |
| **Khám bệnh & EMR** | `LuotKham`, `ChanDoan`, `ICD`, `SinhHieu`, `TienSuBenh`, `KetQuaDieuTri`, `HuongXuTri` | `DoctorStationPanel`, `ExaminationPanel`, `MedicalRecordTabPanel`, dialog khóa EMR | • Nạp dữ liệu bệnh nhân từ hàng đợi vào bàn khám<br>• Lưu nháp quá trình khám bệnh (Auto-save/Draft)<br>• Chẩn đoán ICD-10 chính & phụ từ bảng `ICD`<br>• Ghi nhận `KetQuaDieuTri` & `HuongXuTri`<br>• Ký số / Khóa bệnh án EMR (chống chỉnh sửa trái phép) | **P1** |
| **Kê đơn thuốc & Cảnh báo an toàn** | `DonThuoc`, `ChiTietDonThuoc`, `ClinicalDrugSafetyService`, `Item` | `PrescriptionOrderPanel`, `PrescriptionOrderTabPanel`, print preview | • Tìm kiếm thuốc thực từ bảng `Item` và `TonKho`<br>• Cảnh báo tương tác thuốc, quá liều, dị ứng bệnh nhân<br>• Ghi nhận cách dùng chi tiết: Sáng/Trưa/Chiều/Tối, Đường dùng<br>• Duyệt đơn thuốc ngoại trú | **P1** |
| **Chỉ định & Kết quả CLS** | `DanhMucDichVu`, `ChiDinhDichVu`, `ChiTietKetQuaXetNghiem`, `DinhMucVatTuDichVu` | `ServiceOrderPanel`, `LabOrderTabPanel`, `ParaclinicalOrderPanel` | • Chọn dịch vụ khám/xét nghiệm/CĐHA từ bảng `DanhMucDichVu`<br>• Quản lý trạng thái thực hiện (`CHO_THUC_HIEN` ➔ `HOAN_TAT`)<br>• Nhập chỉ số xét nghiệm và kết luận CĐHA<br>• Đính kèm kết quả trả về bàn khám bác sĩ | **P1** |
| **Kho Dược & Nhà thuốc** | `Item`, `Kho`, `TonKho`, `PhieuNhapKho`, `ChiTietNhapKho`, `PhieuXuatKho`, `ChiTietXuatKho`, `InventoryTransaction`, `FefoAllocationService` | `PharmacyDispensePanel`, `PharmacyTicketTablePanel`, `PharmacyCategoryTreePanel` | • Xuất bán / Cấp phát thuốc theo đơn bác sĩ<br>• Áp dụng thuật toán **FEFO** (First Expired, First Out) tự động trừ lô cận hạn nhất<br>• Ghi thẻ kho `InventoryTransaction` nguyên tử (Atomic)<br>• Cảnh báo hạn dùng và ngưỡng tồn kho an toàn | **P1** |
| **Viện phí & Thu ngân** | `HoaDon`, `ChiTietHoaDon`, `PhieuThu`, `CongNoBenhNhan` | `BillingInvoicePanel`, `MedicalBillingSlipPanel` | • Tự động tổng hợp chi phí khám, CLS, tiền thuốc<br>• Áp dụng tỷ lệ đồng chi trả BHYT theo quyền lợi<br>• Xuất phiếu thu tiền, in hóa đơn viện phí<br>• Quản lý tạm ứng và theo dõi công nợ bệnh nhân | **P1** |
| **Nhân sự & Chấm công** | `NhanVien`, `ChucVu`, `Khoa`, `CaTruc`, `ChamCong`, `LichLamViec` | Một phần trong `SystemConfigPanel` | • Quản lý hồ sơ bác sĩ/nhân viên y tế<br>• Phân ca làm việc và lịch trực theo tuần<br>• Chấm công hàng ngày và xét duyệt nghỉ phép | **P2** |
| **Tiền lương (Payroll)** | `KyLuong`, `BangLuong`, `ChiTietBangLuong` | Chưa có form chuyên biệt | • Cấu hình kỳ lương tháng và mức hoa hồng thủ thuật<br>• Tính lương tự động từ dữ liệu chấm công và chỉ định<br>• Duyệt, chốt bảng lương và xuất báo cáo | **P2** |
| **Cơ sở & Danh mục** | `CoSo`, `Khoa`, `PhongKham`, `ICD`, `DanhMucDichVu` | `CatalogPanel`, `SystemConfigPanel` | • CRUD danh mục ICD-10, dịch vụ kỹ thuật, thuốc<br>• Quản lý danh mục phòng khám, phân bổ bác sĩ trực<br>• Quản lý thông tin liên cơ sở | **P1/P2** |
| **Báo cáo & Thống kê** | Giao dịch từ Khám bệnh, Kho, Viện phí, Nhân sự | `ClinicReportPanel` | • Thống kê lượt khám theo bác sĩ/khoa/ngày<br>• Báo cáo doanh thu viện phí và BHYT<br>• Báo cáo tồn kho dược, thuốc cận hạn, xuất nhập tồn | **P2** |
| **Xuất tài liệu (DOCX / In ấn)** | `BenhNhan`, `LuotKham`, `DonThuoc`, `HoaDon` | Core DOCX engine trong `core/docx`, các Print Dialog | • **ĐÃ XONG ENGINE THUẦN JAVA:** `DocxDocument`, `DocxTable`, `DocxExportService`, `DocxManager`<br>• Nối các nút "In Đơn Thuốc", "In Bệnh Án", "In Giấy Hẹn" từ UI sang `DocxExportService` | **P1** |

---

## 2. Bảng Ánh xạ 15 Trường Y Tế Cần Bổ Sung Trên Màn Tiếp Đón / Đăng Ký

Đối chiếu giữa giao diện `PatientRegistrationFormPanel` và các thực thể Domain:

| STT | Trường thông tin trên Giao diện | Thuộc tính Domain Entity tương ứng | Kiểu dữ liệu / Ghi chú |
| :---: | :--- | :--- | :--- |
| 1 | **Đối tượng khám** | `LuotKham.doiTuongKham` | Enum `DoiTuongKham` (`BHYT`, `VIEN_PHI`, `YEU_CAU`, `MIEN_PHI`) |
| 2 | **Lý do đến khám** | `LuotKham.lyDoKham` | `String` (Ví dụ: Đau ngực, Sốt cao 3 ngày) |
| 3 | **Mã ĐKBĐ (Nơi ĐK ban đầu)** | `BaoHiem.maDkbd` | `String` (Mã KCB ban đầu theo quy chuẩn BHXH) |
| 4 | **Mức hưởng BHYT (%)** | `BaoHiem.mucHuong` | `Integer` (100%, 95%, 80%) |
| 5 | **Ngày miễn cùng chi trả** | `BaoHiem.ngayDu5Nam` | `LocalDate` (Thời điểm đủ 5 năm liên tục) |
| 6 | **Tuyến khám chữa bệnh** | `BaoHiem.dungTuyen` | `Boolean` (`true`: Đúng tuyến, `false`: Trái tuyến) |
| 7 | **Họ tên người liên hệ** | `BenhNhan.nguoiLienHe` | `String` (Thân nhân khi cấp cứu hoặc trẻ nhỏ) |
| 8 | **Mối quan hệ thân nhân** | `BenhNhan.moiQuanHeNguoiLienHe` | `String` (Bố, Mẹ, Vợ, Chồng, Con) |
| 9 | **SĐT người liên hệ** | `BenhNhan.soDienThoaiNguoiLienHe`| `String` |
| 10 | **Nghề nghiệp** | `BenhNhan.ngheNghiep` | `String` |
| 11 | **Dân tộc** | `BenhNhan.danToc` | `String` (Mặc định: "Kinh") |
| 12 | **Quốc tịch** | `BenhNhan.quocTich` | `String` (Mặc định: "Việt Nam") |
| 13 | **Địa chỉ chi tiết** | `BenhNhan.diaChi` | `String` (Số nhà, đường phố) |
| 14 | **Xã/Phường, Quận/Huyện, Tỉnh**| `BenhNhan.phuongXa`, `quanHuyen`, `tinhThanh` | Phục vụ báo cáo chuẩn Bộ Y Tế |
| 15 | **Số thẻ BHYT** | `BaoHiem.maThe` | Chuỗi 15 ký tự theo quy định BHXH VN |

---

## 3. Quy Tắc Kỹ Thuật Khi Triển Khai Màn Hình

1. **Không truy cập DB trực tiếp từ Swing:** Giao diện chỉ gọi các Use Case trong `clinic-application` thông qua Facade / Command Bus.
2. **Luồng bất đồng bộ bắt buộc (SwingWorker):** Tất cả các thao tác gọi Repository / SQL Server phải được đặt trong `SwingWorker.doInBackground()` để tránh treo giao diện người dùng. Cập nhật kết quả lên UI qua phương thức `done()` trên EDT.
3. **Cơ chế Cache Danh mục:** Các danh mục tĩnh hoặc ít thay đổi (`ICD`, `DanhMucDichVu`, `Khoa`, `PhongKham`) cần được nạp sẵn vào bộ nhớ Cache khi ứng dụng khởi động để tối ưu tốc độ mở màn hình tiếp đón và bàn khám.
