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

public class GCHeapEvent implements OnJfrEvent, JfrEventProvider {

    private static final Logger log = Logger.getLogger(GCHeapEvent.class);

    public static final String JDK_GC_HEAP_CONFIGURATION = "jdk.GCHeapConfiguration";
    public static final String JDK_GC_HEAP_SUMMARY = "jdk.GCHeapSummary";
    public static final String JDK_YOUNG_GC = "jdk.YoungGarbageCollection";
    public static final String JDK_OLD_GC = "jdk.OldGarbageCollection";

    private final JfrEventProcessor eventProcessor;

    public GCHeapEvent(JfrEventProcessor eventProcessor) {
        if (eventProcessor == null) throw new IllegalArgumentException("eventProcessor must not be null");
        this.eventProcessor = eventProcessor;
    }

    @Override
    public void onEvent(RecordedEvent event) {

        String name = event.getEventType().getName();
        log.debug(event.getStartTime() + " " + name);

        ProcessedJfrEvent processedEvent;
        if (JDK_GC_HEAP_SUMMARY.equals(name)) {
            processedEvent = processGcHeapSummary(event);
        } else if (JDK_GC_HEAP_CONFIGURATION.equals(name)) {
            log.debug("Not implemented event: %s", name);
            return;
        } else if (JDK_YOUNG_GC.equals(name)) {
            processedEvent = processYoungGc(event);
        } else if (JDK_OLD_GC.equals(name)) {
            processedEvent = processOldGc(event);
        } else {
            log.debug("Ignoring unknown event: %s", name);
            return;
        }
        eventProcessor.processEvent(processedEvent);
    }

    private ProcessedJfrEvent processOldGc(RecordedEvent event) {
        double durationMs = event.getDuration().toMillis();

        return ProcessedJfrEvent.of(
                event.getStartTime(),
                "oldGc",
                "duration-ms",
                durationMs);
    }

    private ProcessedJfrEvent processYoungGc(RecordedEvent event) {
        double durationMs = event.getDuration().toMillis();

        return ProcessedJfrEvent.of(
                event.getStartTime(),
                "youngGc",
                "duration-ms",
                durationMs);
    }

    private ProcessedJfrEvent processGcHeapSummary(RecordedEvent event) {
        long heapUsed = event.getLong("heapUsed");
        long heapCommitted = event.getLong("heapSpace.committedSize");

        return ProcessedJfrEvent.of(
                event.getStartTime(),
                "heap",
                "heapUsed",
                heapUsed,
                Map.of("heapCommitted", heapCommitted));
    }

    @Override
    public List<JfrEventSettings> getEventSettings() {
        Duration zeroMs = Duration.ofMillis(0);
        return List.of(
                JfrEventSettings.of(JDK_GC_HEAP_SUMMARY, this).withPeriod(Duration.ofSeconds(1)),
                JfrEventSettings.of(JDK_OLD_GC, this).withThreshold(zeroMs),
                JfrEventSettings.of(JDK_YOUNG_GC, this).withThreshold(zeroMs)
        );
    }
}
