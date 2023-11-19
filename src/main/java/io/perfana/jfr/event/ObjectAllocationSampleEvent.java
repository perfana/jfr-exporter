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
        // The relative weight of the sample. Aggregating the weights for a large number of samples,
        // for a particular class, thread or stack trace,
        // gives a statistically accurate representation of the allocation pressure
        long weight = event.getLong("weight");
        String objectClass = event.getClass("objectClass").getName();
        Instant startTime = event.getStartTime();
        log.trace("%s %s %d", (startTime == null ? "<no-start-time>" : startTime), name, weight);

        reportLargeAllocationSample(event, weight, objectClass, startTime);

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

    private void reportLargeAllocationSample(RecordedEvent event, long weight, String objectClass, Instant startTime) {
        if (weight > bigAllocationSizeBytes) {

            if (event.getStackTrace() == null) {
                log.error("No stack trace available for large allocation sample weight of %d bytes of objectClass '%s'", weight, objectClass);
                return;
            }

            List<String> stackTrace = JfrUtil.translateStacktrace(event);

            String objectClassTranslation = JfrUtil.translatePrimitiveClass(objectClass);
            String firstStack = stackTrace.isEmpty() ? "<none>" : stackTrace.get(0);
            log.debug("Found high allocation weight of %d bytes of %s in '%s'", weight, objectClassTranslation, firstStack);

            Map<String, Object> extraFields = Map.of("objectClass", objectClassTranslation, "thread", event.getThread().getJavaName());

            ProcessedJfrEvent processedEvent = new ProcessedJfrEvent(startTime,
                    "object-allocation-sample",
                    "bytes",
                    weight,
                    stackTrace,
                    extraFields);

            eventProcessor.processEvent(processedEvent);
        }
    }

    @Override
    public List<JfrEventSettings> getEventSettings() {
        JfrEventSettings settings = JfrEventSettings.of(JDK_OBJECT_ALLOCATION_SAMPLE, this).withThreshold("throttle", "200/s");
        return List.of(settings);
    }
}
