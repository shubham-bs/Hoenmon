package com.homeenergymonitor.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.homeenergymonitor.web.dto.MeterReadingDto;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class TelemetrySseBroadcasterTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-01T12:00:00Z"), ZoneOffset.UTC);
    private final TelemetrySseBroadcaster broadcaster = new TelemetrySseBroadcaster(clock);

    private static MeterReadingDto reading() {
        return new MeterReadingDto(1L, "SIM-0001", Instant.parse("2026-03-01T12:00:00Z"),
                1.25, 230.1, 5.72, 50.0, 0.95, 12.5, 0.0, "SIMULATED", true);
    }

    @Test
    void publishDeliversToEverySubscriber() throws Exception {
        SseEmitter first = mock(SseEmitter.class);
        SseEmitter second = mock(SseEmitter.class);
        broadcaster.register(first);
        broadcaster.register(second);

        broadcaster.publish(reading());

        verify(first).send(any(SseEmitter.SseEventBuilder.class));
        verify(second).send(any(SseEmitter.SseEventBuilder.class));
        assertThat(broadcaster.subscriberCount()).isEqualTo(2);
    }

    @Test
    void failedSubscriberIsRemovedWithoutAffectingOthers() throws Exception {
        SseEmitter broken = mock(SseEmitter.class);
        SseEmitter healthy = mock(SseEmitter.class);
        doThrow(new IOException("client gone")).when(broken).send(any(SseEmitter.SseEventBuilder.class));
        broadcaster.register(broken);
        broadcaster.register(healthy);

        broadcaster.publish(reading());

        verify(healthy).send(any(SseEmitter.SseEventBuilder.class));
        verify(broken).completeWithError(any(IOException.class));
        assertThat(broadcaster.subscriberCount()).isEqualTo(1);
    }

    @Test
    void heartbeatRemovesDeadSubscriber() throws Exception {
        SseEmitter dead = mock(SseEmitter.class);
        doThrow(new IOException("gone")).when(dead).send(any(SseEmitter.SseEventBuilder.class));
        broadcaster.register(dead);

        broadcaster.heartbeat();

        assertThat(broadcaster.subscriberCount()).isZero();
    }

    @Test
    void shutdownCompletesAndClearsAllSubscribers() throws Exception {
        SseEmitter first = mock(SseEmitter.class);
        SseEmitter second = mock(SseEmitter.class);
        broadcaster.register(first);
        broadcaster.register(second);

        broadcaster.shutdown();

        verify(first).complete();
        verify(second).complete();
        assertThat(broadcaster.subscriberCount()).isZero();
        broadcaster.publish(reading());
        verify(first, never()).send(any(SseEmitter.SseEventBuilder.class));
    }
}
