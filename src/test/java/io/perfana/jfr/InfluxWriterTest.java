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

import io.perfana.jfr.influx.InfluxWriter;
import io.perfana.jfr.influx.InfluxWriterClient;
import io.perfana.jfr.influx.InfluxWriterConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InfluxWriterTest {

    @Test
    @Disabled("Run with running influxdb on localhost:8086")
    void writeMetricPoint() throws Exception {
        try (InfluxWriter influxWriter = createInfluxWriterClient()) {
            assertTrue(influxWriter.isHealthy());
        }
    }

    @NotNull
    private static InfluxWriterClient createInfluxWriterClient() {
        return new InfluxWriterClient(
                new InfluxWriterConfig(
                        "http://localhost:8086",
                        "jfr",
                        "username",
                        "password",
                        "autogen",
                        Map.of("service", "afterburner"),
                        true));
    }

    @Test
    @Disabled("Run with running influxdb on localhost:8086")
    void writeMetricPoints() throws Exception {
        try (InfluxWriter influxWriter = createInfluxWriterClient()) {
            ProcessedJfrEvent event1 = new ProcessedJfrEvent(
                    Instant.now(),
                    "measurement-1",
                    Collections.emptyMap(),
                    "number",
                    1.0,
                    Collections.emptyMap(),
                    Collections.emptyList());

            ProcessedJfrEvent event2 = new ProcessedJfrEvent(
                    Instant.now(),
                    "measurement-2",
                    Collections.emptyMap(),
                    "temperature",
                    2.0,
                    Collections.emptyMap(),
                    Collections.emptyList());

            influxWriter.writeMetricPoint(event1);
            influxWriter.writeMetricPoint(event2);

            assertTrue(influxWriter.isHealthy());
        }
    }
}