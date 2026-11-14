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
 * Synchronously dispatches domain events to handlers registered for the event's exact class.
 * Registrations are local to this publisher instance and run in registration order.
 */
public final class InProcessMienNghiepVuSuKienPhatHanh implements IMienNghiepVuSuKienPhatHanh {
    /** Trường `handlersByEventType`: `handlers` theo sukien loai. */
    private final Map<Class<? extends IMienNghiepVuSuKien>, List<Consumer<IMienNghiepVuSuKien>>> handlersByEventType =
            new HashMap<>();

    /**
     * Registers a typed handler for events whose runtime class is exactly {@code eventType}.
     *
     * @throws NullPointerException if {@code eventType} or {@code handler} is null
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
     * Publishes synchronously on the calling thread. A handler exception propagates unchanged
     * and stops dispatch. An event with no exact-class registrations is ignored.
     *
     * @throws NullPointerException if {@code event} is null
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
