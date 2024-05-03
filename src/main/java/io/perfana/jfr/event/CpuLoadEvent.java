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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CpuLoadEvent implements OnJfrEvent, JfrEventProvider {

    private static final Logger log = Logger.getLogger(CpuLoadEvent.class);

    public static final String JDK_CPULOAD = "jdk.CPULoad";
    public static final String JDK_THREAD_CONTEXT_SWITCH_RATE = "jdk.ThreadContextSwitchRate";

    private final JfrEventProcessor eventProcessor;

    public CpuLoadEvent(JfrEventProcessor eventProcessor) {
        if (eventProcessor == null) throw new IllegalArgumentException("eventProcessor must not be null");
        this.eventProcessor = eventProcessor;
    }

    @Override
    public void onEvent(RecordedEvent event) {

        String name = event.getEventType().getName();
        Instant timestamp = event.getStartTime();

        if (JDK_CPULOAD.equals(name)) {
            String measurementName = "CPU";

            double machineTotal = event.getDouble("machineTotal") * 100.0;
            double jvmUser = event.getDouble("jvmUser") * 100.0;
            double jvmSystem = event.getDouble("jvmSystem") * 100.0;

            Map<String, Object> extraFields = new HashMap<>();
            extraFields.put("jvmUser", jvmUser);
            extraFields.put("jvmSystem", jvmSystem);

            eventProcessor.processEvent(ProcessedJfrEvent.of(timestamp, measurementName, "machineTotal", machineTotal, extraFields));
        }
        else if (JDK_THREAD_CONTEXT_SWITCH_RATE.equals(name)) {
            double switchRateValue = event.getDouble("switchRate");
            ProcessedJfrEvent switchRate = ProcessedJfrEvent.of(timestamp, "thread-context-switch-rate", "switchRate", switchRateValue);
            eventProcessor.processEvent(switchRate);
        }
        else {
            log.debug("Ignoring unknown event: %s", name);
        }
    }

    @Override
    public List<JfrEventSettings> getEventSettings() {

        JfrEventSettings cpuLoadEvent = JfrEventSettings.of(JDK_CPULOAD, this)
                .withPeriod(Duration.ofSeconds(1));
        JfrEventSettings cpuThreadContextSwitchRate = JfrEventSettings.of(JDK_THREAD_CONTEXT_SWITCH_RATE, this)
                .withPeriod(Duration.ofSeconds(10));

        return List.of(cpuLoadEvent, cpuThreadContextSwitchRate);
    }
}
