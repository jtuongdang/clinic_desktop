package vn.clinicmanagement.application.reception;

import java.util.List;

/** Trường `queue`: hangdoi. */
public record TiepDonHangDoiAnhChup(List<TiepDonHangDoiMatHang> queue,
                                     /** Trường `rooms`: phòng. */
                                     List<PhongKhamPhongHangDoiTongHop> rooms) {
    public TiepDonHangDoiAnhChup {
        queue = queue == null ? List.of() : List.copyOf(queue);
        rooms = rooms == null ? List.of() : List.copyOf(rooms);
    }
}
