package vn.clinicmanagement.domain.repositories;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;
import java.util.List;

public interface ISystemRepository {
    List<MaTranPhanQuyen> getMatrixByRoleId(int roleId);
}
