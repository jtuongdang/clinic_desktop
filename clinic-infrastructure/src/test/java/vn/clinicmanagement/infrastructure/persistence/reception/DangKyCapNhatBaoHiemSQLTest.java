package vn.clinicmanagement.infrastructure.persistence.reception;

import vn.clinicmanagement.application.reception.register.RegisterBenhNhanCommand;
import vn.clinicmanagement.application.reception.register.RegisterBenhNhanHandler;

import org.junit.jupiter.api.*;
import vn.clinicmanagement.application.reception.*;
import vn.clinicmanagement.application.authorization.*;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.domain.repositories.*;
import vn.clinicmanagement.domain.identity.*;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;
import vn.clinicmanagement.domain.common.enums.*;
import vn.clinicmanagement.infrastructure.persistence.*;
import vn.clinicmanagement.infrastructure.persistence.patient.*;
import vn.clinicmanagement.infrastructure.persistence.facility.PhongKhamRepository;
import vn.clinicmanagement.infrastructure.persistence.service.ChiDinhDichVuRepository;
import vn.clinicmanagement.infrastructure.persistence.system.JdbcKiemToanRepository;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Chạy trên SQL Server đã có hồ sơ/BHYT, cập nhật và mở lượt khám rồi rollback toàn bộ dữ liệu thử. */
class DangKyCapNhatBaoHiemSQLTest {
    @Test void capNhatHoSoVaTheBhytGhiAuditVaDangKyThanhCong() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("clinic.registration.sql"));
        try(Connection connection=CoSoDuLieuKetNoi.getConnection()) {
            connection.setAutoCommit(false);
            var uow=new JdbcUnitOfWork();var transactions=new JdbcGiaoDichQuanLy(uow);
            Field currentField=JdbcGiaoDichQuanLy.class.getDeclaredField("currentConnection");currentField.setAccessible(true);
            @SuppressWarnings("unchecked") ThreadLocal<Connection> current=(ThreadLocal<Connection>)currentField.get(transactions);
            current.set(connection);
            try {
                int patientId,facility,department,room,employeeId,roleId,serviceId;
                try(Statement s=connection.createStatement();ResultSet r=s.executeQuery("SELECT TOP 1 l.maBenhNhan,l.maCoSo,l.maKhoa,l.maPhongKham FROM LuotKham l JOIN BenhNhan p ON p.id=l.maBenhNhan AND p.isDeleted=0 JOIN BaoHiem b ON b.maBenhNhan=p.id AND b.isDeleted=0 JOIN PhongKham r ON r.id=l.maPhongKham AND r.hoatDong=1 AND r.isDeleted=0 WHERE l.isDeleted=0 ORDER BY p.id,l.id DESC")) {
                    assertTrue(r.next(),"Cần có hồ sơ và thẻ BHYT để kiểm tra nhánh cập nhật");patientId=r.getInt(1);facility=r.getInt(2);department=r.getInt(3);room=r.getInt(4);
                }
                employeeId=scalar(connection,"SELECT TOP 1 id FROM NhanVien WHERE hoatDong=1 AND isDeleted=0 ORDER BY id");
                roleId=scalar(connection,"SELECT TOP 1 id FROM VaiTro WHERE tenVaiTro='RECEPTIONIST' AND isDeleted=0");
                serviceId=scalar(connection,"SELECT TOP 1 id FROM DanhMucDichVu WHERE loaiDichVu IN ('KHAM','KHAM_BENH') AND hoatDong=1 AND isDeleted=0 ORDER BY id");
                UUID session=UUID.randomUUID(),correlation=UUID.randomUUID();
                var context=new PhanQuyenNguCanh(employeeId,roleId,"RECEPTIONIST",facility,department,session);
                var actor=new KiemToanTacNhanNguCanh(employeeId,roleId,"RECEPTIONIST",facility,department,"SQL-REG-REPAIR",correlation);
                var employee=new NhanVien();employee.setId(employeeId);var role=new VaiTro();role.setId(roleId);role.setTenVaiTro("RECEPTIONIST");
                var employees=(INhanVienRepository)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{INhanVienRepository.class},(p,m,a)->switch(m.getName()){
                    case "getById"->Optional.of(employee);case "getAssignedRoles"->List.of(role);case "getAssignedWorkScopes"->List.of(new NhanVienWorkPhamVi(facility,"TEST",department,"TEST"));default->throw new UnsupportedOperationException(m.getName());});
                IHeThongRepository system=new IHeThongRepository(){
                    public List<MaTranPhanQuyen> getMatrixByRoleId(int id){return List.of(grant(roleId,"PATIENT",HanhDongQuyen.SUA),grant(roleId,"RECEPTION",HanhDongQuyen.TAO));}
                    public Optional<String> getSystemParameterValue(String key){return Optional.empty();}
                };
                var auth=new PhanQuyenDichVu(id->Optional.of(new HoatDongPhienCongKetNoi.ActiveSession(session,employeeId,roleId,"RECEPTIONIST",facility,department,true,true,true)),employees,system);
                var connections=new JdbcKetNoiNhaCungCap(transactions);
                var patients=new BenhNhanRepository(connections,uow);var cards=new BaoHiemRepository(connections,uow);
                var patient=patients.getByIdInScope(patientId,facility,department).orElseThrow();
                var card=cards.getLatestByPatientId(patientId).orElseThrow();assertTrue(card.getId()>0);
                var audit=new JdbcKiemToanRepository(connections,uow,CoSoDuLieuKetNoi::getConnection);
                var handler=new RegisterBenhNhanHandler(patients,new BenhNhanCoSoRepository(connections,uow),cards,
                        new PhongKhamRepository(connections),new TiepDonHangDoiRepository(connections,uow),transactions,audit,auth,new ChiDinhDichVuRepository(connections,uow));
                var result=handler.handle(new RegisterBenhNhanCommand(patient,card,room,serviceId,"Khám bệnh","SQL rollback verification",DoiTuongKham.THU_PHI,false,employeeId,context,actor));
                assertTrue(result.isSuccess(),()->result.getError().map(e->e.code()).orElse("No result"));
                int visit=result.valueOrThrow().visitId();assertTrue(visit>0);
                assertTrue(new PhieuDangKyKhamRepository(connections).getByVisit(visit,facility,department).orElseThrow().hasConfirmedPrice());
                try(var ps=connection.prepareStatement("SELECT COUNT(*) FROM NhatKyHeThong WHERE correlationId=? AND hanhDong IN ('PATIENT_UPDATED','INSURANCE_UPDATED','VISIT_REGISTERED')")){
                    ps.setObject(1,correlation);try(ResultSet r=ps.executeQuery()){r.next();assertEquals(3,r.getInt(1));}
                }
            } finally {connection.rollback();current.remove();}
        }
    }
    private static int scalar(Connection c,String sql)throws SQLException{try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){if(!r.next())throw new SQLException("Thiếu danh mục kiểm thử");return r.getInt(1);}}
    private static MaTranPhanQuyen grant(int role,String module,HanhDongQuyen action){var g=new MaTranPhanQuyen();g.setMaVaiTro(role);g.setPhanHe(module);g.setHanhDong(action);g.setPhamViDuLieu(PhamViDuLieu.KHOA_PHONG);g.setDuocPhep(true);return g;}
}
