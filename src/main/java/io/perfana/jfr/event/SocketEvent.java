/*
 * Copyright (C) 2023 Peter Paul Bakker - Perfana
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.perfana.jfr.event;

import io.perfana.jfr.*;
import jdk.jfr.consumer.RecordedEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SocketEvent implements OnJfrEvent, JfrEventProvider {

    private static final Logger log = Logger.getLogger(SocketEvent.class);

    public static final String JDK_SOCKET_READ = "jdk.SocketRead";
    public static final String JDK_SOCKET_WRITE = "jdk.SocketWrite";
    private final JfrEventProcessor eventProcessor;

    private final AtomicLong lastWriteReportTimestamp = new AtomicLong(0);
    private final AtomicLong lastReadReportTimestamp = new AtomicLong(0);

    private final Map<TotalBytesHostKey, AtomicLong> totalWriteBytesPerHost = new ConcurrentHashMap<>();
    private final Map<TotalBytesHostKey, AtomicLong> totalReadBytesPerHost = new ConcurrentHashMap<>();

    private static final long reportIntervalMs = 2000;

    public SocketEvent(JfrEventProcessor eventProcessor) {
        if (eventProcessor == null) throw new IllegalArgumentException("eventProcessor must not be null");
        this.eventProcessor = eventProcessor;
    }

    private record TotalBytesHostKey(@Nonnull String host, @Nonnull String address, String port) {
        TotalBytesHostKey(String host, String address, int port) {
            this(host, address, checkDynamicPorts(port));
        }

        @NotNull
        private static String checkDynamicPorts(int port) {
            // IANA ephemeral ports are from 49152 to 65535, but some (linux) use 32768 to 61000
            // but found as low as 28006 in the wild on linux container with Java 21
            return port >= 24_000 && port <= 65_535 ? "dynamic" : String.valueOf(port);
        }
    }

    @Override
    public void onEvent(RecordedEvent event) {
        String name = event.getEventType().getName();

        if (name.equals(JDK_SOCKET_READ)) {
            reportSocketRead(event);
        } else if (name.equals(JDK_SOCKET_WRITE)) {
            reportSocketWrite(event);
        } else {
            log.debug("Not implemented event: %s", name);
        }

    }

    private void reportSocketRead(RecordedEvent event) {
        Instant startTime = event.getStartTime();

        String name = event.getEventType().getName();
        String threadName = event.getThread().getJavaName();
        long durationNs = event.getLong("duration");
        long bytesRead = event.getLong("bytesRead");
        String host = event.getString("host");
        String address = event.getString("address");
        int port = event.getInt("port");

        Map<String, Object> extraFields = Map.of(
                "threadName", threadName,
                "durationNs", durationNs,
                "host", host,
                "address", address,
                "port", port
        );

        log.trace("Socket read: %s %s %d %s",
                (startTime == null ? "<no-start-time>" : startTime),
                name,
                bytesRead,
                extraFields);

        TotalBytesHostKey hostKey = new TotalBytesHostKey(host, address, port);

        processEvent(lastReadReportTimestamp, "read", totalReadBytesPerHost, hostKey, bytesRead);

    }

    private void reportSocketWrite(RecordedEvent event) {
        Instant startTime = event.getStartTime();

        String name = event.getEventType().getName();
        String threadName = event.getThread().getJavaName();
        long durationNs = event.getLong("duration");
        long bytesWritten = event.getLong("bytesWritten");
        String host = event.getString("host");
        String address = event.getString("address");
        int port = event.getInt("port");

        Map<String, Object> extraFields = Map.of(
                "threadName", threadName,
                "durationNs", durationNs,
                "host", host,
                "address", address,
                "port", port
        );

        log.trace("Socket write: %s %s %d %s",
                (startTime == null ? "<no-start-time>" : startTime),
                name,
                bytesWritten,
                extraFields);

        TotalBytesHostKey hostKey = new TotalBytesHostKey(host, address, port);

        processEvent(lastWriteReportTimestamp, "write", totalWriteBytesPerHost, hostKey, bytesWritten);

    }

    private void processEvent(AtomicLong lastReportTimestamp, String readOrWrite, Map<TotalBytesHostKey, AtomicLong> totalBytesPerHost, TotalBytesHostKey hostKey, long bytes) {

        if (bytes != 0) {
            totalBytesPerHost.computeIfAbsent(hostKey, k -> new AtomicLong(0)).addAndGet(bytes);
        }

        long now = System.currentTimeMillis();

        long timePeriodMs = now - lastReportTimestamp.get();

        if (timePeriodMs > reportIntervalMs) {

            lastReportTimestamp.set(now);

            log.debug("totalBytesPerHost (%s) before: %s", readOrWrite, totalBytesPerHost);

            Instant timestampNow = Instant.now();

            totalBytesPerHost.forEach((key, value) -> {
                long totalBytesInPeriod = value.getAndSet(0);

                if (totalBytesInPeriod != 0) {
                    long bytesRate = totalBytesInPeriod / (timePeriodMs / 1000);

                    log.debug("Total %s bytes for %s: %d bytes, %s bytes rate: %d bytes/s",
                            readOrWrite,
                            key,
                            totalBytesInPeriod,
                            readOrWrite,
                            bytesRate);

                    Map<String, String> tags = Map.of(
                            "host", key.host(),
                            "address", key.address(),
                            "port", key.port()
                    );

                    ProcessedJfrEvent processedEvent = ProcessedJfrEvent.of(
                            timestampNow,
                            "socket-" + readOrWrite + "-rate-bytes",
                            tags,
                            "bytes",
                            bytesRate);

                    eventProcessor.processEvent(processedEvent);
                }
            });

            log.debug("totalBytesPerHost (%s) after: %s", readOrWrite, totalBytesPerHost);
        }
    }

    @Override
    public List<JfrEventSettings> getEventSettings() {
        JfrEventSettings readEvent = JfrEventSettings.of(JDK_SOCKET_READ, this);
        JfrEventSettings writeEvent = JfrEventSettings.of(JDK_SOCKET_WRITE, this);
        return List.of(readEvent, writeEvent);
    }
}
