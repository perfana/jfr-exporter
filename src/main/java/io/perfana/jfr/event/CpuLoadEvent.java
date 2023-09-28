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

public class CpuLoadEvent implements OnJfrEvent, JfrEventProvider {

    private static final Logger log = Logger.getLogger(CpuLoadEvent.class);

    public static final String JDK_CPULOAD = "jdk.CPULoad";
    private final JfrEventProcessor eventProcessor;

    public CpuLoadEvent(JfrEventProcessor eventProcessor) {
        if (eventProcessor == null) throw new IllegalArgumentException("eventProcessor must not be null");
        this.eventProcessor = eventProcessor;
    }

    @Override
    public void onEvent(RecordedEvent event) {
        String measurementName = "CPU";

        MetricCalculation timesHundred = metric -> metric * 100.0;
        eventProcessor.processEvent(ProcessedJfrEvent.of(event, measurementName, "machineTotal", timesHundred));
        eventProcessor.processEvent(ProcessedJfrEvent.of(event, measurementName, "jvmSystem", timesHundred));
        eventProcessor.processEvent(ProcessedJfrEvent.of(event, measurementName, "jvmUser", timesHundred));
    }

    @Override
    public List<JfrEventSettings> getEventSettings() {
        return List.of(JfrEventSettings.of(JDK_CPULOAD, this)
                .withPeriod(Duration.ofSeconds(1)));
    }
}
