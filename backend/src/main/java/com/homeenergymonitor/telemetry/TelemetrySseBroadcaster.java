package com.homeenergymonitor.telemetry;

import com.homeenergymonitor.meter.MeterSource;
import com.homeenergymonitor.web.dto.MeterReadingDto;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * In-memory registry of SSE subscribers. A single instance serves the whole
 * modular monolith; no message broker is involved.
 *
 * <p>Each subscriber is isolated: a failed or disconnected client is removed and can
 * never stop telemetry generation or affect other subscribers.</p>
 */
@Component
public class TelemetrySseBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(TelemetrySseBroadcaster.class);
    private static final long EMITTER_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
    private final Clock clock;

    public TelemetrySseBroadcaster(Clock clock) {
        this.clock = clock;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);
        register(emitter);
        sendConnectedEvent(emitter);
        return emitter;
    }

    void register(SseEmitter emitter) {
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            completeQuietly(emitter);
        });
        emitter.onError(error -> emitters.remove(emitter));
    }

    public void publish(MeterReadingDto reading) {
        for (SseEmitter emitter : emitters) {
            try {
                send(emitter, SseEmitter.event()
                        .name("reading")
                        .id(String.valueOf(reading.id()))
                        .data(reading, MediaType.APPLICATION_JSON));
            } catch (Exception error) {
                discard(emitter, error);
            }
        }
    }

    public void heartbeat() {
        for (SseEmitter emitter : emitters) {
            try {
                send(emitter, SseEmitter.event().comment("keep-alive " + clock.instant()));
            } catch (Exception error) {
                discard(emitter, error);
            }
        }
    }

    public int subscriberCount() {
        return emitters.size();
    }

    private void sendConnectedEvent(SseEmitter emitter) {
        try {
            Map<String, Object> hello = new LinkedHashMap<>();
            hello.put("type", "connected");
            hello.put("source", MeterSource.SIMULATED.name());
            hello.put("demo", true);
            hello.put("timestamp", clock.instant().toString());
            hello.put("subscribers", emitters.size());
            send(emitter, SseEmitter.event()
                    .name("connected")
                    .data(hello, MediaType.APPLICATION_JSON));
        } catch (Exception error) {
            discard(emitter, error);
        }
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) throws IOException {
        synchronized (emitter) {
            emitter.send(event);
        }
    }

    private void discard(SseEmitter emitter, Exception error) {
        emitters.remove(emitter);
        log.debug("Removing SSE subscriber after send failure: {}", error.toString());
        try {
            emitter.completeWithError(error);
        } catch (Exception ignored) {
            // Emitter may already be completed; nothing else to do.
        }
    }

    private void completeQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // Already completed.
        }
    }

    @PreDestroy
    public void shutdown() {
        for (SseEmitter emitter : emitters) {
            completeQuietly(emitter);
        }
        emitters.clear();
    }
}
