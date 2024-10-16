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
package io.perfana.jfr;

import jdk.jfr.EventSettings;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingStream;

import java.util.concurrent.ConcurrentHashMap;

public class JfrEventHandler {

    private static final Logger log = Logger.getLogger(JfrEventHandler.class);

    private final ConcurrentHashMap<String, JfrEventSettings> events = new ConcurrentHashMap<>();

    public void register(JfrEventSettings jfrEventSettings) {
        events.put(jfrEventSettings.getName(), jfrEventSettings);
    }

    void handle(String eventName, RecordedEvent event) {
        JfrEventSettings jfrEventSettings = events.get(eventName);
        if (jfrEventSettings != null) {
            try {
                jfrEventSettings.getOnJfrEvent().onEvent(event);
            } catch (Throwable e) {
                log.error("Error handling event %s: %s", eventName, e.getMessage());
            }
        }
        else {
            log.trace("Skip handle event: no event settings for event %s", eventName);
        }
    }

    public void subscribe(EventStream stream) {
        for (JfrEventSettings jfrEventSettings : events.values()) {
            String name = jfrEventSettings.getName();
            log.debug("Subscribe to event %s", name);
            stream.onEvent(name, event -> handle(name, event)) ;
        }
    }

    public void enableEvents(RecordingStream stream) {
        for (JfrEventSettings jfrEventSettings : events.values()) {

            EventSettings settings = stream.enable(jfrEventSettings.getName());

            StringBuilder debugInfo = new StringBuilder(512);
            if (jfrEventSettings.hasPeriod()) {
                settings.withPeriod(jfrEventSettings.getPeriod());
                debugInfo.append(" with period ").append(jfrEventSettings.getPeriod());
            }
            if (jfrEventSettings.hasThreshold()) {
                settings.withThreshold(jfrEventSettings.getThreshold());
                debugInfo.append(" with threshold ").append(jfrEventSettings.getThreshold());
            }
            if (jfrEventSettings.hasOtherThreshold()) {
                ThresholdPair otherThreshold = jfrEventSettings.getOtherThreshold();
                settings.with(otherThreshold.name(), otherThreshold.value());
                debugInfo.append(" with ").append(otherThreshold.name()).append(" ").append(otherThreshold.value());
            }
            log.debug("Enable event %s%s", jfrEventSettings.getName(), debugInfo.toString());
        }
    }
}
