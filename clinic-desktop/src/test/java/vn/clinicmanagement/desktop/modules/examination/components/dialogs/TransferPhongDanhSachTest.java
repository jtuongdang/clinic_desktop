package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;
import vn.clinicmanagement.domain.facility.*;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;
import java.lang.reflect.Proxy;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TransferPhongDanhSachTest {
    static PhongKham room(int id,int department,int facility,String name){var site=new CoSo();site.setId(facility);var dept=new Khoa();dept.setId(department);dept.setMaCoSo(facility);dept.setTenKhoa("Khoa "+department);dept.setCoSo(site);var room=new PhongKham();room.setId(id);room.setTenPhong(name);room.setMaKhoa(department);room.setKhoa(dept);return room;}
    @Test void identicalRoomNamesStillResolveTheSelectedRoomId(){var first=room(1,10,5,"Phòng khám 1");var second=room(2,11,5,"Phòng khám 1");var options=List.of(first,second);var model=TransferPhongSelectHopThoai.transferRoomModel(options);assertEquals("Khoa 10 > Phòng khám 1",model.getElementAt(0));assertEquals("Khoa 11 > Phòng khám 1",model.getElementAt(1));assertSame(second,TransferPhongSelectHopThoai.roomAt(options,1));assertNull(TransferPhongSelectHopThoai.roomAt(options,-1));}
    @Test void transferChoicesCanSpanDepartmentsButStayInCurrentFacility(){var first=room(1,10,5,"Phòng 1");var second=room(2,11,5,"Phòng 2");var foreign=room(3,12,6,"Phòng 3");var inactive=room(4,10,5,"Phòng 4");inactive.setHoatDong(false);IPhongKhamRepository repository=(IPhongKhamRepository)Proxy.newProxyInstance(IPhongKhamRepository.class.getClassLoader(),new Class[]{IPhongKhamRepository.class},(p,m,a)->m.getName().equals("getActiveRooms")?List.of(first,second,foreign,inactive):null);var queue=new KhamBenhGhepNoiGoc.InMemoryExamQueuePort(null,repository);assertEquals(List.of(first,second),queue.getActiveClinicRooms(5));assertTrue(queue.getActiveClinicRooms(0).isEmpty());}
}
