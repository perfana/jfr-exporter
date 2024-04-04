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
import io.perfana.jfr.influx.InfluxWriterConfig;
import io.perfana.jfr.influx.InfluxWriterNative;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InfluxWriterNativeTest {

    @Test
    @Disabled("Run with running influxdb on localhost:8086")
    void writeMetricPoint() throws Exception {
        try (InfluxWriter influxWriter = createInfluxWriterNative()) {
            assertTrue(influxWriter.isHealthy());
        }
    }

    @NotNull
    private static InfluxWriterNative createInfluxWriterNative() {
        InfluxWriterConfig config = new InfluxWriterConfig(
                "http://localhost:8086",
                "jfr",
                "username",
                "password",
                "autogen",
                "afterburner",
                true);
        return new InfluxWriterNative(config);
    }

    /**
     * This test requires a running influxdb on localhost:8086
     * <p>
     * To verify the data:
     * > docker exec -it jfr-influx influx
     * > use jfr
     * > show measurements
     * > select * from "measurement-1"
     * > select * from "measurement-2"
     */
    @Test
    @Disabled("Run with running influxdb on localhost:8086")
    void writeMetricPoints() throws Exception {
        try (InfluxWriter influxWriter = createInfluxWriterNative()) {
            ProcessedJfrEvent event1 = new ProcessedJfrEvent(
                    Instant.now(),
                    "measurement-1",
                    Map.of("tag1", "value1", "tag2", "value2"),
                    "height",
                    1.0,
                    Collections.emptyMap(),
                    Collections.emptyList());

            ProcessedJfrEvent event2 = new ProcessedJfrEvent(
                    Instant.now(),
                    "measurement-2",
                    Collections.emptyMap(),
                    "temperature",
                    new BigDecimal("2.0"),
                    Collections.emptyMap(),
                    List.of("stacktrace-line-1", "stacktrace-line-2"));

            influxWriter.writeMetricPoint(event1);
            influxWriter.writeMetricPoint(event2);

            // to flush the buffer
            influxWriter.close();

            assertTrue(influxWriter.isHealthy());
        }
    }
}