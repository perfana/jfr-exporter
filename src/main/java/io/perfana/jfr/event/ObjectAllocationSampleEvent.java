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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ObjectAllocationSampleEvent implements OnJfrEvent, JfrEventProvider {

    private static final Logger log = Logger.getLogger(ObjectAllocationSampleEvent.class);

    public static final String JDK_OBJECT_ALLOCATION_SAMPLE = "jdk.ObjectAllocationSample";
    private final JfrEventProcessor eventProcessor;
    private final long bigAllocationSizeBytes;

    private final AtomicLong totalAllocationsBytes = new AtomicLong(0);
    private final AtomicLong lastAllocationRateReport = new AtomicLong(0);

    private static final long reportIntervalMs = 2000;

    public ObjectAllocationSampleEvent(JfrEventProcessor eventProcessor, long thresholdSizeBytes) {
        if (eventProcessor == null) throw new IllegalArgumentException("eventProcessor must not be null");
        log.debug("Tracing object allocations of more than %d bytes.", thresholdSizeBytes);
        this.eventProcessor = eventProcessor;
        this.bigAllocationSizeBytes = thresholdSizeBytes;
    }

    @Override
    public void onEvent(RecordedEvent event) {
        String name = event.getEventType().getName();
        long weight = event.getLong("weight");
        String objectClass = event.getClass("objectClass").getName();
        Instant startTime = event.getStartTime();
        log.trace("%s %s %d", (startTime == null ? "<no-start-time>" : startTime), name, weight);

        reportBigAllocation(event, weight, objectClass, startTime);

        reportTotalAllocations(weight, startTime);
    }

    private void reportTotalAllocations(long weight, Instant startTime) {
        totalAllocationsBytes.addAndGet(weight);

        long now = System.currentTimeMillis();

        if (now - lastAllocationRateReport.get() > reportIntervalMs) {

            lastAllocationRateReport.set(now);

            long totalAllocations = totalAllocationsBytes.getAndSet(0);

            long allocationRate = totalAllocations / (reportIntervalMs / 1000);

            log.debug("Total allocations: %d bytes, allocation rate: %d bytes/s",
                    totalAllocations,
                    allocationRate);

            ProcessedJfrEvent event = ProcessedJfrEvent.of(
                    startTime,
                    "allocation-rate-bytes",
                    "bytes",
                    allocationRate);

            eventProcessor.processEvent(event);
        }
    }

    private void reportBigAllocation(RecordedEvent event, long weight, String objectClass, Instant startTime) {
        if (weight > bigAllocationSizeBytes) {

            if (event.getStackTrace() == null) {
                log.error("No stack trace available for large allocation weight of %d bytes of %s", weight, objectClass);
                return;
            }

            List<String> stackTrace = event.getStackTrace().getFrames().stream()
                    .map(f -> f.getMethod().getType().getName() + "." + f.getMethod().getName() + " (line: " + f.getLineNumber() + ")")
                    .toList();

            String objectClassTranslation = translatePrimitiveClass(objectClass);
            String firstStack = stackTrace.isEmpty() ? "<none>" : stackTrace.get(0);
            log.debug("Found high allocation weight of %d bytes of %s in '%s'", weight, objectClassTranslation, firstStack);

            Map<String, Object> extraFields = Map.of("objectClass", objectClassTranslation, "thread", event.getThread().getJavaName());

            ProcessedJfrEvent processedEvent = new ProcessedJfrEvent(startTime,
                    "big-allocations",
                    "bytes",
                    weight,
                    stackTrace,
                    extraFields);

            eventProcessor.processEvent(processedEvent);
        }
    }

    static String translatePrimitiveClass(String objectClass) {
        if (objectClass.startsWith("[")) {
            int lastIndexOf = objectClass.lastIndexOf('[');
            String remainder = objectClass.substring(lastIndexOf + 1);

            String arrayPrefix = arrayPrefix(lastIndexOf + 1);

            String baseType = switch (remainder.charAt(0)) {
                case 'Z' -> "boolean";
                case 'B' -> "byte";
                case 'C' -> "char";
                case 'D' -> "double";
                case 'F' -> "float";
                case 'I' -> "int";
                case 'J' -> "long";
                case 'S' -> "short";
                case 'L' -> extractObject(remainder);
                default -> throw new JfrExporterException("Unknown array type: " + objectClass);
            };
            return baseType + arrayPrefix;
        }
        else {
            return objectClass;
        }
    }

    private static String arrayPrefix(int count) {
        if (count == 1) return "[]";
        if (count == 2) return "[][]";
        if (count == 3) return "[][][]";
        else return IntStream.range(0, count).mapToObj(i -> "[]").collect(Collectors.joining());
    }

    static String extractObject(String remainder) {
        int end = remainder.indexOf(';');
        return remainder.substring(1, end);
    }

    @Override
    public List<JfrEventSettings> getEventSettings() {
        JfrEventSettings settings = JfrEventSettings.of(JDK_OBJECT_ALLOCATION_SAMPLE, this).withThreshold("throttle", "200/s");
        return List.of(settings);
    }
}
