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

import static io.perfana.jfr.JfrUtil.*;

public class ObjectAllocationEvent implements OnJfrEvent, JfrEventProvider {

    private static final Logger log = Logger.getLogger(ObjectAllocationEvent.class);

    public static final String JDK_OBJECT_ALLOCATION_OUTSIDE_TLAB = "jdk.ObjectAllocationOutsideTLAB";
    private final JfrEventProcessor eventProcessor;
    private final long bigAllocationSizeBytes;

    public ObjectAllocationEvent(JfrEventProcessor eventProcessor, long thresholdSizeBytes) {
        if (eventProcessor == null) throw new IllegalArgumentException("eventProcessor must not be null");
        log.debug("Tracing object allocations of more than %d bytes.", thresholdSizeBytes);
        this.eventProcessor = eventProcessor;
        this.bigAllocationSizeBytes = thresholdSizeBytes;
    }

    @Override
    public void onEvent(RecordedEvent event) {
        String name = event.getEventType().getName();
        long allocationSize = event.getLong("allocationSize");
        String objectClass = event.getClass("objectClass").getName();
        Instant startTime = event.getStartTime();
        log.trace("%s %s %d", (startTime == null ? "<no-start-time>" : startTime), name, allocationSize);

        reportBigAllocation(event, allocationSize, objectClass, startTime);

    }

    private void reportBigAllocation(RecordedEvent event, long allocationSize, String objectClass, Instant startTime) {
        if (allocationSize > bigAllocationSizeBytes) {

            if (event.getStackTrace() == null) {
                log.error("No stack trace available for big allocation of %d bytes of objectClass '%s'", allocationSize, objectClass);
                return;
            }

            List<String> stackTrace = translateStacktrace(event);

            String objectClassTranslation = translatePrimitiveClass(objectClass);
            String firstStack = stackTrace.isEmpty() ? "<none>" : stackTrace.get(0);
            log.debug("Found big object allocation of %d bytes of %s in '%s'", allocationSize, objectClassTranslation, firstStack);

            Map<String, Object> extraFields = Map.of(
                    "objectClass", objectClassTranslation,
                    "thread", JfrUtil.nullSafeGetThreadJavaName(event)
            );

            ProcessedJfrEvent processedEvent = ProcessedJfrEvent.of(
                    startTime,
                    "big-allocations",
                    "bytes",
                    allocationSize,
                    extraFields,
                    stackTrace);

            eventProcessor.processEvent(processedEvent);
        }
    }

    @Override
    public List<JfrEventSettings> getEventSettings() {
        JfrEventSettings settings = JfrEventSettings.of(JDK_OBJECT_ALLOCATION_OUTSIDE_TLAB, this);
        return List.of(settings);
    }
}
