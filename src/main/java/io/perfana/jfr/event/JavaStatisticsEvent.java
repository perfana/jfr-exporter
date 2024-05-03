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

public class JavaStatisticsEvent implements OnJfrEvent, JfrEventProvider {

    private static final Logger log = Logger.getLogger(JavaStatisticsEvent.class);

    public static final String JDK_JAVA_THREAD_STATISTICS = "jdk.JavaThreadStatistics";
    public static final String JDK_JAVA_CLASSLOADING_STATISTICS = "jdk.ClassLoadingStatistics";
    private final JfrEventProcessor eventProcessor;

    public JavaStatisticsEvent(JfrEventProcessor eventProcessor) {
        if (eventProcessor == null) throw new IllegalArgumentException("eventProcessor must not be null");
        this.eventProcessor = eventProcessor;
    }

    @Override
    public void onEvent(RecordedEvent event) {
        String name = event.getEventType().getName();

        if (JDK_JAVA_THREAD_STATISTICS.equals(name)) {
            processJavaThreadStatistics(event);
        } else if (JDK_JAVA_CLASSLOADING_STATISTICS.equals(name)) {
            processJavaClassloadingStatistics(event);
        } else {
            log.debug("Ignoring unknown event: %s", name);
        }
    }

    private void processJavaClassloadingStatistics(RecordedEvent event) {
        long loaded = event.getLong("loadedClassCount");
        long unloaded = event.getLong("unloadedClassCount");

        eventProcessor.processEvent(ProcessedJfrEvent.of(event.getStartTime(), "classes-loaded", "loadedClassCount", loaded - unloaded));
    }

    private void processJavaThreadStatistics(RecordedEvent event) {
        long activeCount = event.getLong("activeCount");
        long daemonCount = event.getLong("daemonCount");
        eventProcessor.processEvent(ProcessedJfrEvent.of(event.getStartTime(), "threads", "activeCount", activeCount, Map.of("daemonCount", daemonCount)));
    }

    @Override
    public List<JfrEventSettings> getEventSettings() {
        Duration period = Duration.ofSeconds(1);
        return List.of(
                    JfrEventSettings.of(JDK_JAVA_THREAD_STATISTICS, this).withPeriod(period),
                    JfrEventSettings.of(JDK_JAVA_CLASSLOADING_STATISTICS, this).withPeriod(period)
                );
    }
}
