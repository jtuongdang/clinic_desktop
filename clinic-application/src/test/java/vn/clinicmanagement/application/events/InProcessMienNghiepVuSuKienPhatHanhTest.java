package vn.clinicmanagement.application.events;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.common.events.IMienNghiepVuSuKien;
import vn.clinicmanagement.domain.common.events.IMienNghiepVuSuKienHandler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InProcessMienNghiepVuSuKienPhatHanhTest {
    @Test
    void publishesOnlyToHandlersRegisteredForTheExactEventClass() {
        InProcessMienNghiepVuSuKienPhatHanh publisher = new InProcessMienNghiepVuSuKienPhatHanh();
        List<String> calls = new ArrayList<>();
        publisher.registerHandler(SyntheticEvent.class, event -> calls.add("base"));
        publisher.registerHandler(SyntheticChildEvent.class, event -> calls.add("child"));

        publisher.publish(new SyntheticChildEvent());
        publisher.publish(new SyntheticEvent());

        assertEquals(List.of("child", "base"), calls);
    }

    @Test
    void publishesHandlersInRegistrationOrderOnTheCallingThread() {
        InProcessMienNghiepVuSuKienPhatHanh publisher = new InProcessMienNghiepVuSuKienPhatHanh();
        List<Integer> calls = new ArrayList<>();
        Thread publishingThread = Thread.currentThread();
        List<Thread> handlerThreads = new ArrayList<>();
        publisher.registerHandler(SyntheticEvent.class, event -> {
            calls.add(1);
            handlerThreads.add(Thread.currentThread());
        });
        publisher.registerHandler(SyntheticEvent.class, event -> {
            calls.add(2);
            handlerThreads.add(Thread.currentThread());
        });

        publisher.publish(new SyntheticEvent());

        assertEquals(List.of(1, 2), calls);
        assertEquals(List.of(publishingThread, publishingThread), handlerThreads);
    }

    @Test
    void publishingWithoutHandlersIsANoOp() {
        new InProcessMienNghiepVuSuKienPhatHanh().publish(new SyntheticEvent());
    }

    @Test
    void rejectsNullRegistrationArgumentsAndEvents() {
        InProcessMienNghiepVuSuKienPhatHanh publisher = new InProcessMienNghiepVuSuKienPhatHanh();

        assertThrows(NullPointerException.class, () -> publisher.registerHandler(null, handler -> { }));
        assertThrows(NullPointerException.class,
                () -> publisher.registerHandler(SyntheticEvent.class, (IMienNghiepVuSuKienHandler<SyntheticEvent>) null));
        assertThrows(NullPointerException.class, () -> publisher.publish(null));
    }

    @Test
    void propagatesTheOriginalHandlerFailureAndStopsDispatch() {
        InProcessMienNghiepVuSuKienPhatHanh publisher = new InProcessMienNghiepVuSuKienPhatHanh();
        IllegalStateException failure = new IllegalStateException("synthetic handler failure");
        List<String> calls = new ArrayList<>();
        publisher.registerHandler(SyntheticEvent.class, event -> {
            calls.add("failing");
            throw failure;
        });
        publisher.registerHandler(SyntheticEvent.class, event -> calls.add("later"));

        IllegalStateException propagated = assertThrows(
                IllegalStateException.class, () -> publisher.publish(new SyntheticEvent()));

        assertSame(failure, propagated);
        assertEquals(List.of("failing"), calls);
    }

    @Test
    void publisherInstancesDoNotShareRegistrations() {
        InProcessMienNghiepVuSuKienPhatHanh firstPublisher = new InProcessMienNghiepVuSuKienPhatHanh();
        InProcessMienNghiepVuSuKienPhatHanh secondPublisher = new InProcessMienNghiepVuSuKienPhatHanh();
        List<String> calls = new ArrayList<>();
        firstPublisher.registerHandler(SyntheticEvent.class, event -> calls.add("first"));

        firstPublisher.publish(new SyntheticEvent());
        secondPublisher.publish(new SyntheticEvent());

        assertEquals(List.of("first"), calls);
    }

    private static class SyntheticEvent implements IMienNghiepVuSuKien {
        @Override
        public UUID getEventId() {
            return UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        }

        @Override
        public LocalDateTime getOccurredOn() {
            return LocalDateTime.of(2026, 1, 1, 0, 0);
        }
    }

    private static final class SyntheticChildEvent extends SyntheticEvent {
    }
}
