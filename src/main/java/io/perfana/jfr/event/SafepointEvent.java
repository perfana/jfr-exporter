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
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SafepointEvent implements OnJfrEvent, JfrEventProvider {

    private static final Logger log = Logger.getLogger(SafepointEvent.class);

    private final ConcurrentHashMap<Long, Instant> safepoints = new ConcurrentHashMap<>();

    public static final String JDK_SAFEPOINT_BEGIN = "jdk.SafepointBegin";
    public static final String JDK_SAFEPOINT_END = "jdk.SafepointEnd";
    private final JfrEventProcessor eventProcessor;

    public SafepointEvent(JfrEventProcessor eventProcessor) {
        if (eventProcessor == null) throw new IllegalArgumentException("eventProcessor must not be null");
        this.eventProcessor = eventProcessor;
    }

    @Override
    public void onEvent(RecordedEvent event) {
        String name = event.getEventType().getName();
        Long safepointId = event.getLong("safepointId");
        log.debug(event.getStartTime() + " " + name + ": " + safepointId);

        if (name.equals(JDK_SAFEPOINT_BEGIN)) {
            safepoints.put(safepointId, event.getStartTime());
        } else if (name.equals(JDK_SAFEPOINT_END)) {
            Instant startTime = safepoints.remove(safepointId);
            if (startTime != null) {
                Duration duration = Duration.between(startTime, event.getEndTime());
                log.debug("Safepoint duration: %s", duration);

                ProcessedJfrEvent processedEvent = ProcessedJfrEvent.of(
                        event.getStartTime(),
                        "safepoint",
                        "duration",
                        (double) duration.toMillis());

                eventProcessor.processEvent(processedEvent);
            } else {
                log.debug("Safepoint begin with id %d not found", safepointId);
            }
            reportSafepointsInProgress();
        } else {
            log.debug("Unknown safepoint event '%s'", name);
        }
    }

    private void reportSafepointsInProgress() {
        if (!safepoints.isEmpty()) {
            log.info("Safepoints in progress: %d", safepoints.size());
        }
    }

    @Override
    public List<JfrEventSettings> getEventSettings() {
        return List.of(
                JfrEventSettings.of(JDK_SAFEPOINT_BEGIN, this),
                JfrEventSettings.of(JDK_SAFEPOINT_END, this)
        );
    }
}
