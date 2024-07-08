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

import io.perfana.jfr.JfrEventProcessor;
import io.perfana.jfr.JfrEventSettings;
import io.perfana.jfr.Logger;
import io.perfana.jfr.ProcessedJfrEvent;
import jdk.jfr.consumer.RecordedEvent;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class NativeMemoryEvent implements OnJfrEvent, JfrEventProvider {

    private static final Logger log = Logger.getLogger(NativeMemoryEvent.class);

    public static final String JDK_NATIVE_MEMORY_USAGE = "jdk.NativeMemoryUsage";
    public static final String JDK_NATIVE_MEMORY_USAGE_TOTAL = "jdk.NativeMemoryUsageTotal";
    public static final String JDK_RESIDENT_SET_SIZE = "jdk.ResidentSetSize";
    public static final String FIELD_COMMITTED = "committed";
    public static final String FIELD_RESERVED = "reserved";

    private final JfrEventProcessor eventProcessor;

    public NativeMemoryEvent(JfrEventProcessor eventProcessor) {
        if (eventProcessor == null) throw new IllegalArgumentException("eventProcessor must not be null");
        this.eventProcessor = eventProcessor;
    }

    @Override
    public void onEvent(RecordedEvent event) {

        String name = event.getEventType().getName();
        log.debug(event.getStartTime() + " " + name);

        ProcessedJfrEvent processedEvent;
        if (JDK_NATIVE_MEMORY_USAGE_TOTAL.equals(name)) {
            processedEvent = processNativeMemoryUsageTotal(event);
        } else if (JDK_NATIVE_MEMORY_USAGE.equals(name)) {
            processedEvent = processNativeMemoryUsage(event);
        } else if (JDK_RESIDENT_SET_SIZE.equals(name)) {
            processedEvent = processResidentSetSize(event);
        } else {
            log.debug("Ignoring unknown event: %s", name);
            return;
        }
        eventProcessor.processEvent(processedEvent);
    }

    private ProcessedJfrEvent processNativeMemoryUsageTotal(RecordedEvent event) {
        long reservedMemory = event.getLong(FIELD_RESERVED);
        long committedMemory = event.getLong(FIELD_COMMITTED);

        return ProcessedJfrEvent.of(
                event.getStartTime(),
                "memory-native-total",
                FIELD_RESERVED,
                reservedMemory,
                Map.of(FIELD_COMMITTED, committedMemory));
    }

    private ProcessedJfrEvent processNativeMemoryUsage(RecordedEvent event) {
        long reservedMemory = event.getLong(FIELD_RESERVED);
        long committedMemory = event.getLong(FIELD_COMMITTED);
        String nmtType = event.getString("type");

        return ProcessedJfrEvent.of(
                event.getStartTime(),
                "memory-native",
                Map.of("type", nmtType),
                FIELD_RESERVED,
                reservedMemory,
                Map.of(FIELD_COMMITTED, committedMemory));
    }

    private ProcessedJfrEvent processResidentSetSize(RecordedEvent event) {
        long rssSize = event.getLong("size");
        long rssPeak = event.getLong("peak");

        return ProcessedJfrEvent.of(
                event.getStartTime(),
                "memory-resident-set-size",
                "size",
                rssSize,
                Map.of("peak", rssPeak));
    }

    @Override
    public List<JfrEventSettings> getEventSettings() {
        Duration oneSecond = Duration.ofSeconds(1);
        return List.of(
                JfrEventSettings.of(JDK_NATIVE_MEMORY_USAGE_TOTAL, this).withPeriod(oneSecond),
                JfrEventSettings.of(JDK_NATIVE_MEMORY_USAGE, this).withPeriod(oneSecond),
                JfrEventSettings.of(JDK_RESIDENT_SET_SIZE, this).withPeriod(oneSecond)
        );
    }
}
