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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static io.perfana.jfr.JfrUtil.*;

public class MonitorEvent implements OnJfrEvent, JfrEventProvider {

    private static final Logger log = Logger.getLogger(MonitorEvent.class);

    public static final String JDK_JAVA_MONITOR_WAIT = "jdk.JavaMonitorWait";
    public static final String JDK_JAVA_MONITOR_ENTER = "jdk.JavaMonitorEnter";
    private final JfrEventProcessor eventProcessor;

    private final long minimumDurationNs = Duration.ofMillis(10).toNanos();

    public MonitorEvent(JfrEventProcessor eventProcessor) {
        if (eventProcessor == null) throw new IllegalArgumentException("eventProcessor must not be null");
        this.eventProcessor = eventProcessor;
    }

    @Override
    public void onEvent(RecordedEvent event) {
        String name = event.getEventType().getName();
        Instant startTime = event.getStartTime();
        long durationNs = event.getLong("duration");
        String monitorClass = event.getClass("monitorClass").getName();

        long address = event.getValue("address");
        // prefix with 0x to prevent interpretation as number by InfluxDB
        String addressAsHex = "0x" + Long.toHexString(address);

        log.trace("%s %s %d %s %s", (startTime == null ? "<no-start-time>" : startTime), name, durationNs, monitorClass, addressAsHex);

        if (durationNs > minimumDurationNs) {

            if (event.getStackTrace() == null) {
                log.error("No stack trace available for monitor wait of %d nanoseconds of monitorClass '%s'", durationNs, monitorClass);
                return;
            }

            List<String> stackTrace = translateStacktrace(event);

            String firstStack = stackTrace.isEmpty() ? "<none>" : stackTrace.get(0);
            log.debug("Found monitor wait of %d nanoseconds of '%s' in '%s'", durationNs, monitorClass, firstStack);

            String threadName = nullSafeGetThreadJavaName(event, "eventThread");

            if (name.equals(JDK_JAVA_MONITOR_WAIT)) {

                if (threadName.startsWith("JFR Event Stream") || threadName.startsWith("Finalizer")) {
                    log.debug("Ignoring monitor wait of %d nanoseconds of thread '%s'", durationNs, threadName);
                    return;
                }

                String notifier = nullSafeGetThreadJavaName(event,"notifier");
                long timeout = event.getLong("timeout");
                boolean timedOut = event.getBoolean("timedOut");

                Map<String, Object> extraFields = Map.of(
                        "monitor-class", monitorClass,
                        "thread", threadName,
                        "address", addressAsHex,
                        "notifier", notifier,
                        "timeout", timeout,
                        "timed-out", String.valueOf(timedOut)
                );

                reportMonitor("java-monitor-wait", durationNs, startTime, extraFields, stackTrace);

            } else if (name.equals(JDK_JAVA_MONITOR_ENTER)) {

                String previousOwner = nullSafeGetThreadJavaName(event, "previousOwner");

                Map<String, Object> extraFields = Map.of(
                        "monitor-class", monitorClass,
                        "thread", threadName,
                        "address", addressAsHex,
                        "previous-owner", previousOwner
                );

                reportMonitor("java-monitor-enter", durationNs, startTime, extraFields, stackTrace);

            } else {
                log.error("Unknown monitor event '%s'", name);
            }
        }
    }

    private void reportMonitor(String measurementName, long duration, Instant startTime, Map<String, Object> extraFields, List<String> stackTrace) {
            ProcessedJfrEvent processedEvent = ProcessedJfrEvent.of(
                    startTime,
                    measurementName,
                    "duration-ns",
                    duration,
                    extraFields,
                    stackTrace);

            eventProcessor.processEvent(processedEvent);
    }

    @Override
    public List<JfrEventSettings> getEventSettings() {

        JfrEventSettings monitorWait = JfrEventSettings.of(JDK_JAVA_MONITOR_WAIT, this)
                .withThreshold(Duration.ofNanos(minimumDurationNs));

        JfrEventSettings monitorEnter = JfrEventSettings.of(JDK_JAVA_MONITOR_ENTER, this)
                .withThreshold(Duration.ofNanos(minimumDurationNs));

        return List.of(monitorWait, monitorEnter);
    }
}
