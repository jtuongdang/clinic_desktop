package vn.clinicmanagement.application.reception.register;

import vn.clinicmanagement.application.reception.DangKyBienNhan;
import vn.clinicmanagement.application.reception.TiepDonHangDoiCongKetNoi;

import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenQuyetDinh;
import vn.clinicmanagement.application.authorization.PhanQuyenYeuCau;
import vn.clinicmanagement.application.authorization.PhanQuyenDichVu;
import vn.clinicmanagement.application.common.UngDungThatBaiChanDoanLoi;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.domain.billing.ChiTietHoaDon;
import vn.clinicmanagement.domain.billing.CongNoBenhNhan;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;
import vn.clinicmanagement.domain.common.enums.TrangThaiCongNo;
import vn.clinicmanagement.domain.common.valueobjects.NgayKhoang;
import vn.clinicmanagement.domain.common.valueobjects.DienThoaiSo;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.patient.BenhNhanDinhDanhXungDotNgoaiLe;
import vn.clinicmanagement.domain.repositories.IBaoHiemRepository;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;
import vn.clinicmanagement.domain.repositories.IBenhNhanCoSoRepository;
import vn.clinicmanagement.domain.repositories.IChiDinhDichVuRepository;
import vn.clinicmanagement.domain.repositories.IHoaDonRepository;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;
import vn.clinicmanagement.domain.service.DanhMucDichVu;

import java.time.Instant;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class RegisterBenhNhanHandler implements CommandHandler<RegisterBenhNhanCommand, DangKyBienNhan> {
    private final IBenhNhanRepository patients;
    private final IBenhNhanCoSoRepository patientFacilities;
    private final IBaoHiemRepository insuranceCards;
    private final IPhongKhamRepository rooms;
    private final TiepDonHangDoiCongKetNoi receptionQueue;
    private final GiaoDichRanhGioi transactions;
    private final KiemToanWriteCongKetNoi audit;
    private final PhanQuyenDichVu authorization;
    private final IChiDinhDichVuRepository serviceRepository;
    /** Kho hóa đơn được dùng để lập khoản phí đăng ký trong cùng giao dịch tiếp đón. */
    private final IHoaDonRepository invoiceRepository;

    public RegisterBenhNhanHandler(IBenhNhanRepository patients, IBenhNhanCoSoRepository patientFacilities,
                                  IBaoHiemRepository insuranceCards,
                                  IPhongKhamRepository rooms, TiepDonHangDoiCongKetNoi receptionQueue,
                                  GiaoDichRanhGioi transactions, KiemToanWriteCongKetNoi audit,
                                  PhanQuyenDichVu authorization) {
        this(patients, patientFacilities, insuranceCards, rooms, receptionQueue,
                transactions, audit, authorization, null, null);
    }

    public RegisterBenhNhanHandler(IBenhNhanRepository patients, IBenhNhanCoSoRepository patientFacilities,
                                  IBaoHiemRepository insuranceCards,
                                  IPhongKhamRepository rooms, TiepDonHangDoiCongKetNoi receptionQueue,
                                  GiaoDichRanhGioi transactions, KiemToanWriteCongKetNoi audit,
                                  PhanQuyenDichVu authorization, IChiDinhDichVuRepository serviceRepository) {
        this(patients, patientFacilities, insuranceCards, rooms, receptionQueue, transactions,
                audit, authorization, serviceRepository, null);
    }

    public RegisterBenhNhanHandler(IBenhNhanRepository patients, IBenhNhanCoSoRepository patientFacilities,
                                  IBaoHiemRepository insuranceCards, IPhongKhamRepository rooms,
                                  TiepDonHangDoiCongKetNoi receptionQueue, GiaoDichRanhGioi transactions,
                                  KiemToanWriteCongKetNoi audit, PhanQuyenDichVu authorization,
                                  IChiDinhDichVuRepository serviceRepository, IHoaDonRepository invoiceRepository) {
        this.patients = patients;
        this.patientFacilities = patientFacilities;
        this.insuranceCards = insuranceCards;
        this.rooms = rooms;
        this.receptionQueue = receptionQueue;
        this.transactions = transactions;
        this.audit = audit;
        this.authorization = authorization;
        this.serviceRepository = serviceRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public KetQua<DangKyBienNhan> handle(RegisterBenhNhanCommand command) {
        String validationError = validate(command);
        if (validationError != null) return KetQua.failure("PATIENT_VALIDATION", validationError);

        PhanQuyenNguCanh context = command.authorizationContext();
        boolean existing = isExistingPatient(command.patient());
        HanhDongQuyen patientAction = existing ? HanhDongQuyen.SUA : HanhDongQuyen.TAO;
        if (!isAuthorized(context, "PATIENT", patientAction)
                || !isAuthorized(context, "RECEPTION", HanhDongQuyen.TAO)) {
            return deny(context, command.auditActorContext(), KiemToanLyDoMa.AUTH_FORBIDDEN);
        }

        return transactions.inTransaction(() -> {
            try {
                BenhNhan patient = command.patient();
                PhongKham room = rooms.getById(command.roomId()).orElse(null);
                if (room == null || !room.isHoatDong() || room.getKhoa() == null || room.getKhoa().getId() == null
                        || room.getKhoa().getCoSo() == null || room.getKhoa().getCoSo().getId() == null) {
                    return KetQua.failure("ROOM_UNAVAILABLE", "Phòng khám không hoạt động hoặc chưa gắn với cơ sở/khoa.");
                }
                if (room.getKhoa().getCoSo().getId() != context.facilityId()
                        || room.getKhoa().getId() != context.departmentId()
                        || room.getMaKhoa() != context.departmentId()) {
                    return deny(context, command.auditActorContext(), KiemToanLyDoMa.SCOPE_MISMATCH);
                }

                Integer serviceId = command.serviceId();
                if (serviceRepository == null) return KetQua.failure("SERVICE_UNAVAILABLE", "Chưa kết nối được danh mục dịch vụ khám. Vui lòng thử tải lại.");
                if (serviceId != null) {
                    DanhMucDichVu service;
                    try { service = serviceRepository.getDichVuById(serviceId).orElse(null); }
                    catch (RuntimeException error) {
                        UngDungThatBaiChanDoanLoi.logUnexpectedFailure("RECEPTION_VALIDATE_SERVICE", "SERVICE_CATALOG_FAILED",
                                command.auditActorContext().correlationId().toString(), error);
                        return KetQua.failure("SERVICE_CATALOG_FAILED", "Không thể kiểm tra danh mục dịch vụ khám. Vui lòng bấm Làm Mới và thử lại.");
                    }
                    if (service == null || !service.isHoatDong() || service.getLoaiDichVu() != LoaiDichVu.KHAM) {
                        return KetQua.failure("SERVICE_UNAVAILABLE",
                                "Dịch vụ khám không tồn tại, không đúng loại khám hoặc đã ngừng hoạt động.");
                    }
                }

                if (existing) {
                    var scopedPatient = patients.getByIdInScope(patient.getId(), context.facilityId(), context.departmentId());
                    if (scopedPatient.isEmpty()) {
                        return deny(context, command.auditActorContext(), KiemToanLyDoMa.SCOPE_MISMATCH);
                    }
                    patient = scopedPatient.get();
                    applyPatientForm(patient, command.patient());
                    patient.setUpdatedBy(command.createdBy());
                    patients.update(patient);
                } else {
                    patient.setDienThoai(normalizeOptionalPhone(patient.getDienThoai()));
                    patient.setNguoiLienHeDienThoai(normalizeOptionalPhone(patient.getNguoiLienHeDienThoai()));
                    patient.setCreatedBy(command.createdBy());
                    patients.add(patient);
                }

                if (!hasPositiveId(patient.getId())) return invalidStoredId("hồ sơ người bệnh");
                appendAudit(command.auditActorContext(), context,
                        existing ? KiemToanHanhDongMa.PATIENT_UPDATED : KiemToanHanhDongMa.PATIENT_CREATED,
                        KiemToanTaiNguyenLoai.PATIENT, patient.getId());

                BaoHiem submittedCard = command.insurance();
                if (hasInsuranceNumber(submittedCard)) {
                    var currentCard = insuranceCards.getLatestByPatientId(patient.getId());
                    if (currentCard.isPresent()) {
                        BaoHiem card = currentCard.get();
                        if (!hasPositiveId(card.getId())) return invalidStoredId("thẻ BHYT");
                        applyInsuranceForm(card, submittedCard);
                        card.setUpdatedBy(command.createdBy());
                        insuranceCards.update(card);
                        appendAudit(command.auditActorContext(), context, KiemToanHanhDongMa.INSURANCE_UPDATED,
                                KiemToanTaiNguyenLoai.INSURANCE, card.getId());
                    } else {
                        submittedCard.setMaBenhNhan(patient.getId());
                        submittedCard.setCreatedBy(command.createdBy());
                        insuranceCards.add(submittedCard);
                        if (!hasPositiveId(submittedCard.getId())) return invalidStoredId("thẻ BHYT");
                        appendAudit(command.auditActorContext(), context, KiemToanHanhDongMa.INSURANCE_CREATED,
                                KiemToanTaiNguyenLoai.INSURANCE, submittedCard.getId());
                    }
                }

                var visit = receptionQueue.registerVisit(patient.getId(), context.facilityId(), context.departmentId(),
                        command.roomId(), serviceId, command.registrationType(), command.reason(), command.payerType(),
                        command.priority(), command.createdBy());
                if (!hasPositiveId(visit.getId())) return invalidStoredId("lượt khám");
                appendAudit(command.auditActorContext(), context, KiemToanHanhDongMa.VISIT_REGISTERED,
                        KiemToanTaiNguyenLoai.VISIT, visit.getId());

                patientFacilities.ensureActiveLink(patient.getId(), context.facilityId(), context.employeeId());

                if (invoiceRepository != null) {
                    var snapshot = receptionQueue.getRegistrationPrice(visit.getId(), serviceId)
                            .orElseThrow(() -> new IllegalStateException("Không tìm thấy giá dịch vụ đã chốt."));
                    if (snapshot.unitPrice() == null || snapshot.unitPrice().signum() < 0
                            || snapshot.serviceName() == null || snapshot.serviceName().isBlank()) {
                        throw new IllegalStateException("Giá hoặc tên dịch vụ đã chốt không hợp lệ.");
                    }
                    createRegistrationInvoice(visit.getId(), patient.getId(), context.facilityId(),
                            snapshot.serviceId(), snapshot.serviceName(), snapshot.unitPrice(), command.createdBy(),
                            command.auditActorContext(), context);
                }

                return KetQua.success(new DangKyBienNhan(patient.getId(), patient.getMaBenhNhanFormat(),
                        visit.getId(), visit.getSoThuTu() == null ? 0 : visit.getSoThuTu(), visit.getSoThuTuDisplay()));
            } catch (BenhNhanDinhDanhXungDotNgoaiLe ex) {
                return registrationFailure();
            } catch (RuntimeException ex) {
                UngDungThatBaiChanDoanLoi.logUnexpectedFailure("RECEPTION_REGISTER_PATIENT",
                        "PATIENT_REGISTRATION_FAILED", command.auditActorContext().correlationId().toString(), ex);
                return KetQua.failure("PATIENT_REGISTRATION_FAILED", "Không thể lưu đăng ký. Dữ liệu chưa được ghi; hãy thử lại.");
            }
        });
    }

    /** Lập hóa đơn phí khám từ đúng bản chụp dịch vụ đã ghi cùng lượt khám; không tạo phiếu thu tại đây. */
    private void createRegistrationInvoice(int visitId, int patientId, int facilityId, int serviceId,
                                           String serviceName, BigDecimal unitPrice, int createdBy,
                                           vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh auditActor,
                                           PhanQuyenNguCanh actor) {
        if (invoiceRepository.getByLuotKham(visitId).isPresent()) {
            throw new IllegalStateException("Lượt khám đã có hóa đơn; từ chối tạo hóa đơn trùng.");
        }
        LocalDateTime now = LocalDateTime.now();
        BigDecimal total = unitPrice;
        HoaDon invoice = new HoaDon();
        invoice.setSoHoaDon("DK" + now.format(DateTimeFormatter.ofPattern("yyMMdd"))
                + String.format("%08d", visitId));
        invoice.setMaLuotKham(visitId);
        invoice.setMaBenhNhan(patientId);
        invoice.setMaCoSo(facilityId);
        invoice.setNgayLap(now);
        invoice.setTongTien(total);
        // Quyền lợi BHYT được quyết toán ở nghiệp vụ viện phí; khi đăng ký chưa ghi nhận tiền bảo hiểm.
        invoice.setSoTienBaoHiemChiTra(BigDecimal.ZERO);
        invoice.setSoTienBenhNhanChiTra(total);
        invoice.setDaThanhToan(BigDecimal.ZERO);
        invoice.setSoTienTamUng(BigDecimal.ZERO);
        invoice.setSoTienGiamGia(BigDecimal.ZERO);
        invoice.setChietKhauPhanTram(BigDecimal.ZERO);
        invoice.setLyDoGiamGia("");
        invoice.setSoTienHoanTra(BigDecimal.ZERO);
        invoice.setTrangThai(total.signum() == 0 ? TrangThaiHoaDon.DA_THANH_TOAN : TrangThaiHoaDon.CHO_THANH_TOAN);
        invoice.setGhiChu("Phí dịch vụ khám đã chốt lúc đăng ký; chưa ghi nhận thu tiền.");
        invoice.setCreatedBy(createdBy);

        ChiTietHoaDon detail = new ChiTietHoaDon();
        detail.setLoaiDoiTuong("DICH_VU_KHAM");
        detail.setMaDoiTuong(serviceId);
        detail.setMoTa(serviceName);
        detail.setSoLuong(BigDecimal.ONE);
        detail.setDonGia(unitPrice);
        invoice.addChiTiet(detail);

        HoaDon saved = invoiceRepository.save(invoice);
        if (saved == null || saved.getId() == null || saved.getId() <= 0) {
            throw new IllegalStateException("Không nhận được mã hóa đơn sau khi lưu.");
        }
        if (total.signum() > 0) {
            CongNoBenhNhan debt = new CongNoBenhNhan();
            debt.setMaHoaDon(saved.getId());
            debt.setMaBenhNhan(patientId);
            debt.setSoTienNo(total);
            debt.setSoTienDaTra(BigDecimal.ZERO);
            debt.setHanThanhToan(LocalDate.now().plusDays(30));
            debt.setTrangThai(TrangThaiCongNo.DANG_NO);
            debt.setCreatedBy(createdBy);
            invoiceRepository.saveCongNo(debt);
        }
        audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(
                actor.employeeId(), actor.roleId(), actor.roleCode(), actor.facilityId(), actor.departmentId(),
                KiemToanHanhDongMa.BILL_INVOICE_CREATED, KiemToanTaiNguyenLoai.INVOICE, saved.getId(),
                KiemToanKetQua.SUCCESS, null, Instant.now(), auditActor.workstationId(), auditActor.correlationId()));
    }

    /** Lỗi định danh của dữ liệu lưu trữ không phải lỗi thiếu trường trên form. */
    private static KetQua<DangKyBienNhan> invalidStoredId(String entity) {
        return KetQua.failure("REGISTRATION_DATA_INVALID", "Dữ liệu " + entity
                + " đang lưu có mã định danh không hợp lệ. Cần sửa dữ liệu hệ thống trước khi đăng ký lại.");
    }

    private static boolean hasPositiveId(Integer id) { return id != null && id > 0; }

    private static void applyPatientForm(BenhNhan patient, BenhNhan form) {
        // Mã bệnh nhân được cấp khi tạo mới và không thuộc nội dung cập nhật của repository.
        patient.setHo(form.getHo());
        patient.setTen(form.getTen());
        patient.setNgaySinh(form.getNgaySinh());
        patient.setGioiTinh(form.getGioiTinh());
        patient.setNoiSinh(form.getNoiSinh());
        patient.setSoNhaThonPho(form.getSoNhaThonPho());
        patient.setMaXa(form.getMaXa());
        patient.setDiaChi(form.getDiaChi());
        patient.setNgheNghiep(form.getNgheNghiep());
        patient.setDienThoai(normalizeOptionalPhone(form.getDienThoai()));
        patient.setEmail(form.getEmail());
        patient.setDanToc(form.getDanToc());
        patient.setQuocTich(form.getQuocTich());
        patient.setSoCCCD(form.getSoCCCD());
        patient.setNgayCapCCCD(form.getNgayCapCCCD());
        patient.setNoiCapCCCD(form.getNoiCapCCCD());
        patient.setNoiLamViec(form.getNoiLamViec());
    }

    private static void applyInsuranceForm(BaoHiem card, BaoHiem form) {
        card.setSoBaoHiem(form.getSoBaoHiem());
        card.setMaNoiSong(form.getMaNoiSong());
        card.setMaNoiDangKyKCB(form.getMaNoiDangKyKCB());
        card.setNoiDangKyKCB(form.getNoiDangKyKCB());
        card.setNgayBatDau(form.getNgayBatDau());
        card.setNgayHetHan(form.getNgayHetHan());
        card.setDiaChiThe(form.getDiaChiThe());
        card.setDungTuyen(form.isDungTuyen());
        card.setMucHuong(form.getMucHuong());
        card.setCoGiayKhaiSinh(form.isCoGiayKhaiSinh());
        card.setKhongGiuThe(form.isKhongGiuThe());
        card.setMienCungChiTra(form.isMienCungChiTra());
        card.setNgayMienCungChiTra(form.getNgayMienCungChiTra());
        card.setNgayDu5NamLienTuc(form.getNgayDu5NamLienTuc());
    }

    private boolean isAuthorized(PhanQuyenNguCanh context, String module, HanhDongQuyen action) {
        PhanQuyenQuyetDinh decision = authorization.evaluate(new PhanQuyenYeuCau(
                context, module, action, context.facilityId(), context.departmentId()));
        return decision.allowed();
    }

    private KetQua<DangKyBienNhan> deny(PhanQuyenNguCanh context,
                                             vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh auditActor,
                                             KiemToanLyDoMa reasonCode) {
        try {
            audit.appendIndependently(new HeThongKiemToanNhapLieu(
                    context.employeeId(), context.roleId(), context.roleCode(), context.facilityId(),
                    context.departmentId(), KiemToanHanhDongMa.AUTH_ACCESS_DENIED, null, null,
                    KiemToanKetQua.DENIED, reasonCode, Instant.now(),
                    auditActor.workstationId(), auditActor.correlationId()));
        } catch (RuntimeException ignored) {
            // Yêu cầu vẫn bị từ chối nếu không thể ghi kiểm toán độc lập.
        }
        String code = reasonCode == KiemToanLyDoMa.SCOPE_MISMATCH
                ? "AUTH_SCOPE_MISMATCH" : "AUTHORIZATION_DENIED";
        return KetQua.failure(code, "Không được phép thực hiện đăng ký trong phiên hiện tại.");
    }

    private static KetQua<DangKyBienNhan> registrationFailure() {
        return KetQua.failure("PATIENT_REGISTRATION_FAILED",
                "Không thể hoàn tất đăng ký người bệnh. Vui lòng kiểm tra thông tin và thử lại.");
    }

    private void appendAudit(vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh auditActor,
                             PhanQuyenNguCanh actor, KiemToanHanhDongMa actionCode,
                             KiemToanTaiNguyenLoai resourceType, Integer resourceId) {
        audit.appendInCurrentTransaction(new HeThongKiemToanNhapLieu(
                actor.employeeId(), actor.roleId(), actor.roleCode(), actor.facilityId(), actor.departmentId(),
                actionCode, resourceType, resourceId, KiemToanKetQua.SUCCESS, null, Instant.now(),
                auditActor.workstationId(), auditActor.correlationId()));
    }

    private static String validate(RegisterBenhNhanCommand command) {
        if (command == null || command.patient() == null) return "Thiếu dữ liệu người bệnh.";
        BenhNhan patient = command.patient();
        if (patient.getHo() == null || patient.getHo().isBlank() || patient.getTen() == null || patient.getTen().isBlank())
            return "Vui lòng nhập họ và tên người bệnh.";
        if (patient.getNgaySinh() == null || patient.getNgaySinh().isAfter(LocalDate.now()))
            return "Ngày sinh không hợp lệ.";
        if (patient.getGioiTinh() == null) return "Vui lòng chọn giới tính.";
        if (!isValidOptionalPhone(patient.getDienThoai())
                || (!isExistingPatient(patient) && !isValidOptionalPhone(patient.getNguoiLienHeDienThoai()))) {
            return "Số điện thoại người bệnh hoặc liên hệ khẩn cấp không hợp lệ.";
        }
        if (command.roomId() <= 0) return "Vui lòng chọn phòng khám.";
        if (command.serviceId() == null) return "Vui lòng chọn dịch vụ khám trước khi đăng ký.";
        if (command.serviceId() <= 0) return "Mã dịch vụ khám không hợp lệ.";
        if (command.registrationType() == null || command.registrationType().isBlank()) return "Vui lòng chọn loại đăng ký khám.";
        if (command.payerType() == null) return "Vui lòng chọn đối tượng khám.";
        if (hasInsuranceNumber(command.insurance())) {
            BaoHiem card = command.insurance();
            if (card.getNgayHetHan() == null) return "Vui lòng nhập hạn thẻ BHYT.";
            if (card.getMucHuong() < 0 || card.getMucHuong() > 100) return "Mức hưởng BHYT phải từ 0 đến 100%.";
            try {
                // Tiếp đón ghi nhận khoảng thời gian trên thẻ; việc xác định quyền lợi bảo hiểm được xử lý riêng.
                new NgayKhoang(card.getNgayBatDau(), card.getNgayHetHan());
            } catch (IllegalArgumentException ignored) {
                return "Ngày bắt đầu thẻ BHYT phải trước hoặc bằng ngày hết hạn.";
            }
        } else if (command.payerType() == DoiTuongKham.BHYT) {
            return "Đối tượng BHYT cần có số thẻ và hạn thẻ.";
        }
        return null;
    }

    private static boolean isExistingPatient(BenhNhan patient) {
        return patient.getId() != null && patient.getId() > 0;
    }

    private static boolean hasInsuranceNumber(BaoHiem insurance) {
        return insurance != null && insurance.getSoBaoHiem() != null && !insurance.getSoBaoHiem().isBlank();
    }

    private static boolean isValidOptionalPhone(String value) {
        if (value == null || value.isBlank()) return true;
        try {
            new DienThoaiSo(value);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static String normalizeOptionalPhone(String value) {
        return value == null || value.isBlank() ? value : new DienThoaiSo(value).getValue();
    }
}
