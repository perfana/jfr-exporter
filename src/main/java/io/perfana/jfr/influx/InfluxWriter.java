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
package io.perfana.jfr.influx;

import io.perfana.jfr.ProcessedJfrEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;

public interface InfluxWriter extends AutoCloseable {

    String STACKTRACE_DELIMITER = " --- ";

    boolean isHealthy();

    void writeMetricPoint(ProcessedJfrEvent event);

    static long toEpochNs(Instant timestamp) {
        return (timestamp.toEpochMilli() * 1_000_000) + timestamp.getNano();
    }

    @NotNull
    static String formatStacktrace(List<String> stacktrace1, boolean enableStacktraces) {
        return enableStacktraces
                ? String.join(STACKTRACE_DELIMITER, stacktrace1)
                : stacktrace1.get(0)
                    + STACKTRACE_DELIMITER + stacktrace1.get(1)
                    + STACKTRACE_DELIMITER + stacktrace1.get(2);
    }

    @Override
    void close() throws Exception;
}
