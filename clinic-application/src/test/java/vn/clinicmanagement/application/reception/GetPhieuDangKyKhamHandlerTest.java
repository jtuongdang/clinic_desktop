package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.reception.get.GetPhieuDangKyKhamHandler;
import vn.clinicmanagement.application.reception.get.GetPhieuDangKyKhamQuery;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.*;
import vn.clinicmanagement.domain.identity.*;
import vn.clinicmanagement.domain.repositories.*;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;
import vn.clinicmanagement.domain.common.enums.*;
import java.util.*;
import java.lang.reflect.Proxy;
import static org.junit.jupiter.api.Assertions.*;

class GetPhieuDangKyKhamHandlerTest {
    @Test void thieuQuyenInKhongDocDuLieuVaKhongTuDungQuyenXem() {
        Fixture f=new Fixture(); f.grants=List.of(f.grant(HanhDongQuyen.XEM));
        assertTrue(f.handler().handle(f.query(HanhDongQuyen.IN)).isFailure()); assertEquals(0,f.reads);
        assertTrue(f.handler().handle(f.query(HanhDongQuyen.XUAT_FILE)).isFailure());assertEquals(0,f.reads);
    }
    @Test void actorSaiHoacPhienHetHanKhongTruyVan() {
        Fixture f=new Fixture();f.active=false;assertTrue(f.handler().handle(f.query(HanhDongQuyen.XEM)).isFailure());assertEquals(0,f.reads);
        f.active=true;
        var wrong=new KiemToanTacNhanNguCanh(99,2,"RECEPTIONIST",3,4,"TEST",UUID.randomUUID());
        assertTrue(f.handler().handle(new GetPhieuDangKyKhamQuery(15,f.context,wrong)).isFailure());assertEquals(0,f.reads);
    }
    @Test void truyenDungMaLuotKhamVaPhamViVaKhongTraDuLieuNeuGhiAuditThatBai() {
        Fixture f=new Fixture();assertTrue(f.handler().handle(f.query(HanhDongQuyen.XEM)).isSuccess());
        assertEquals(List.of(15,3,4),f.request);assertEquals(1,f.reads);
        f.auditFails=true;assertTrue(f.handler().handle(f.query(HanhDongQuyen.IN)).isFailure());
    }
    @Test void khongCoLuotKhamTrongPhamViThiKhongTraDuLieu() {
        Fixture f=new Fixture();f.present=false;assertTrue(f.handler().handle(f.query(HanhDongQuyen.XEM)).isFailure());
        assertTrue(f.handler().handle(new GetPhieuDangKyKhamQuery(0,f.context,f.actor)).isFailure());
    }
    static class Fixture {
        UUID session=UUID.randomUUID();
        PhanQuyenNguCanh context=new PhanQuyenNguCanh(1,2,"RECEPTIONIST",3,4,session);
        KiemToanTacNhanNguCanh actor=new KiemToanTacNhanNguCanh(1,2,"RECEPTIONIST",3,4,"TEST",UUID.randomUUID());
        List<MaTranPhanQuyen> grants=List.of(grant(HanhDongQuyen.XEM),grant(HanhDongQuyen.IN),grant(HanhDongQuyen.XUAT_FILE));
        int reads;List<Integer> request;boolean active=true,auditFails,present=true;
        MaTranPhanQuyen grant(HanhDongQuyen action) {var grant=new MaTranPhanQuyen();grant.setMaVaiTro(2);grant.setPhanHe("RECEPTION");grant.setHanhDong(action);grant.setPhamViDuLieu(PhamViDuLieu.KHOA_PHONG);grant.setDuocPhep(true);return grant;}
        GetPhieuDangKyKhamQuery query(HanhDongQuyen action){return new GetPhieuDangKyKhamQuery(15,context,actor,action);}
        GetPhieuDangKyKhamHandler handler() {
            var employee=new NhanVien();employee.setId(1);var role=new VaiTro();role.setId(2);role.setTenVaiTro("RECEPTIONIST");
            var employees=(INhanVienRepository)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{INhanVienRepository.class},(p,m,a)->switch(m.getName()){
                case "getById"->Optional.of(employee);case "getAssignedRoles"->List.of(role);case "getAssignedWorkScopes"->List.of(new NhanVienWorkPhamVi(3,"TEST",4,"TEST"));default->throw new UnsupportedOperationException();});
            IHeThongRepository system=new IHeThongRepository(){public List<MaTranPhanQuyen> getMatrixByRoleId(int id){return grants;}public Optional<String> getSystemParameterValue(String key){return Optional.empty();}};
            var auth=new PhanQuyenDichVu(id->Optional.of(new HoatDongPhienCongKetNoi.ActiveSession(session,1,2,"RECEPTIONIST",3,4,active,true,true)),employees,system);
            var audit=(KiemToanWriteCongKetNoi)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{KiemToanWriteCongKetNoi.class},(p,m,a)->{if(auditFails)throw new IllegalStateException("synthetic audit failure");return null;});
            return new GetPhieuDangKyKhamHandler((visit,facility,dept)->{reads++;request=List.of(visit,facility,dept);return present?Optional.of(new PhieuDangKyKhamDuLieu(visit,"TEST","","","","",null,"","","",null,null,null,List.of())):Optional.empty();},auth,audit);
        }
    }
}
