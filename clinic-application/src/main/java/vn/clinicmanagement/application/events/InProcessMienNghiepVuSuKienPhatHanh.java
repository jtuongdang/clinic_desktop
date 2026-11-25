package vn.clinicmanagement.application.events;

import vn.clinicmanagement.domain.common.events.IMienNghiepVuSuKien;
import vn.clinicmanagement.domain.common.events.IMienNghiepVuSuKienHandler;
import vn.clinicmanagement.domain.common.events.IMienNghiepVuSuKienPhatHanh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Phát sự kiện miền đồng bộ tới các handler đã đăng ký đúng lớp sự kiện. Đăng ký chỉ thuộc publisher hiện tại và được thực hiện theo thứ tự đăng ký.
 */
public final class InProcessMienNghiepVuSuKienPhatHanh implements IMienNghiepVuSuKienPhatHanh {
    private final Map<Class<? extends IMienNghiepVuSuKien>, List<Consumer<IMienNghiepVuSuKien>>> handlersByEventType =
            new HashMap<>();

    /**
     * Đăng ký handler có kiểu cho sự kiện có lớp lúc chạy đúng bằng {@code eventType}. @throws NullPointerException nếu {@code eventType} hoặc {@code handler} là {@code null}.
     */
    public <T extends IMienNghiepVuSuKien> void registerHandler(
            Class<T> eventType,
            IMienNghiepVuSuKienHandler<? super T> handler) {
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(handler, "handler must not be null");

        handlersByEventType.computeIfAbsent(eventType, ignored -> new ArrayList<>())
                .add(event -> handler.handle(eventType.cast(event)));
    }

    /**
     * Phát đồng bộ trên luồng gọi. Ngoại lệ từ handler được giữ nguyên và dừng việc phát; sự kiện không có đăng ký đúng lớp sẽ bị bỏ qua. @throws NullPointerException nếu {@code event} là {@code null}.
     */
    @Override
    public void publish(IMienNghiepVuSuKien event) {
        Objects.requireNonNull(event, "event must not be null");

        List<Consumer<IMienNghiepVuSuKien>> handlers = handlersByEventType.get(event.getClass());
        if (handlers == null) {
            return;
        }

        for (Consumer<IMienNghiepVuSuKien> handler : handlers) {
            handler.accept(event);
        }
    }
}
