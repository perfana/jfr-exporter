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

public class ContainerEvent implements OnJfrEvent, JfrEventProvider {

    private static final Logger log = Logger.getLogger(ContainerEvent.class);

    public static final String JDK_CONTAINER_CPU_THROTTLING = "jdk.ContainerCPUThrottling";
    public static final String JDK_CONTAINER_MEMORY_USAGE = "jdk.ContainerMemoryUsage";

    private final JfrEventProcessor eventProcessor;

    public ContainerEvent(JfrEventProcessor eventProcessor) {
        if (eventProcessor == null) throw new IllegalArgumentException("eventProcessor must not be null");
        this.eventProcessor = eventProcessor;
    }

    @Override
    public void onEvent(RecordedEvent event) {

        String name = event.getEventType().getName();
        Instant timestamp = event.getStartTime();

        if (JDK_CONTAINER_CPU_THROTTLING.equals(name)) {
            String measurementNameSlices = "container-cpu-throttling-slices";
            String measurementNameTime = "container-cpu-throttling-time";

            long cpuElapsedSlices = event.getLong("cpuElapsedSlices");
            long cpuThrottledSlices = event.getLong("cpuThrottledSlices");

            Map<String, Object> extraFields = Map.of("cpuThrottledSlices", cpuThrottledSlices);
            eventProcessor.processEvent(ProcessedJfrEvent.of(timestamp, measurementNameSlices, "cpuElapsedSlices", cpuElapsedSlices, extraFields));

            long cpuThrottledTime = event.getLong("cpuThrottledTime");
            eventProcessor.processEvent(ProcessedJfrEvent.of(timestamp, measurementNameTime, "cpuThrottledTime", cpuThrottledTime));
        }
        else if (JDK_CONTAINER_MEMORY_USAGE.equals(name)) {
            String measurementName = "container-memory-usage";
            long memoryUsage = event.getLong("memoryUsage");
            long swapMemoryUsage = event.getLong("swapMemoryUsage");
            Map<String, Object> extraFields = Map.of("swapMemoryUsage", swapMemoryUsage - memoryUsage);
            eventProcessor.processEvent(ProcessedJfrEvent.of(timestamp, measurementName, "memoryUsage", memoryUsage, extraFields));

            String measurementNameFailures = "container-memory-usage-failures";
            long memoryFailCount = event.getLong("memoryFailCount");
            eventProcessor.processEvent(ProcessedJfrEvent.of(timestamp, measurementNameFailures, "memoryFailCount", memoryFailCount));
        }
        else {
            log.debug("Ignoring unknown event: %s", name);
        }
    }

    @Override
    public List<JfrEventSettings> getEventSettings() {

        JfrEventSettings containerCpuThrottling = JfrEventSettings.of(JDK_CONTAINER_CPU_THROTTLING, this)
                .withPeriod(Duration.ofSeconds(1));
        JfrEventSettings containerMemoryUsage = JfrEventSettings.of(JDK_CONTAINER_MEMORY_USAGE, this)
                .withPeriod(Duration.ofSeconds(1));

        return List.of(containerMemoryUsage, containerCpuThrottling);
    }
}
