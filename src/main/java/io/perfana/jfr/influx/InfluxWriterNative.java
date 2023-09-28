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

import io.perfana.jfr.Logger;
import io.perfana.jfr.ProcessedJfrEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InfluxWriterNative implements InfluxWriter {

    private static final Logger log = Logger.getLogger(InfluxWriterNative.class);


    private final HttpClient httpClient;
    private final InfluxWriterConfig config;

    private final List<String> metricsBuffer = new ArrayList<>();

    private final AtomicLong nextFlush = new AtomicLong(System.currentTimeMillis());

    private static final int maxBatchSize = 100;
    private static final int maxBatchAgeMs = 5_000;

    private final Object lock = new Object();

    public InfluxWriterNative(InfluxWriterConfig config) {

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        this.config = config;

    }
    @Override
    public boolean isHealthy() {
        return false;
    }

    @Override
    public void writeMetricPoint(ProcessedJfrEvent event) {

        // Line protocol: https://github.com/influxdata/influxdb/blob/master/tsdb/README.md
        // jdk.SafepointEnd,application=afterburner duration=0.172 1691147875098417583

        Instant timestamp = event.timestamp() == null ? Instant.now() : event.timestamp();
        long timestampEpochNano = InfluxWriter.toEpochNs(timestamp);

        Map<String, String> tags = new HashMap<>();
        tags.put("application", config.application());

        String generatedTags = tags.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));

        String key = event.measurementName() + "," + generatedTags;

        // note: field String values must be quoted, unlike tags
        Map<String, String> fields = new HashMap<>();
        fields.put(event.field(), String.valueOf(event.value()));

        if (!event.stacktrace().isEmpty()) {
            String stacktrace = String.join("\n", event.stacktrace());
            fields.put("stacktrace", escapeFieldForInflux(stacktrace));
        }

        if (!event.extraFields().isEmpty()) {
            for (Map.Entry<String, Object> entry : event.extraFields().entrySet()) {
                Object value = entry.getValue();
                String escapedValue = escapeFieldForInflux(value);
                fields.put(entry.getKey(), escapedValue);
            }
        }

        String generatedFields = fields.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));

        StringBuilder data = new StringBuilder();
        data.append(key).append(" ").append(generatedFields).append(" ").append(timestampEpochNano);

        log.debug("Writing event to InfluxDB: %s", event.toStringShort());

        boolean useBuffer = false;

        if (useBuffer) {
            bufferAndSendToInflux(data);
        } else {
            sendInfluxData(data.toString());
        }
    }

    private void bufferAndSendToInflux(StringBuilder data) {
        synchronized (lock) {
            metricsBuffer.add(data.toString());
            if ((metricsBuffer.size() > maxBatchSize) || (nextFlush.get() < System.currentTimeMillis())) {
                try {
                    log.debug("Flushing %d metrics to InfluxDB", metricsBuffer.size());
                    String allData = String.join("\n", metricsBuffer);
                    log.trace("Writing data to InfluxDB: %s", allData);
                    sendInfluxData(allData);
                    metricsBuffer.clear();
                } finally {
                    nextFlush.set(System.currentTimeMillis() + maxBatchAgeMs);
                    metricsBuffer.clear();
                }
            }
        }
    }

    private void sendInfluxData(String data) {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("db", config.database());
        requestParams.put("u", config.username());
        requestParams.put("p", config.password());
        requestParams.put("precision", "n");

        String requestParamsString = requestParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        URI uri = URI.create(config.url() + "/write?" + requestParamsString);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMinutes(1))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-agent", "jfr-exporter/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(data))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.trace("InfluxDB response: %d %s", response.statusCode(), response.body());
            if (response.statusCode() != 204) {
                log.error("Failed to send request to InfluxDB: %s", response.body());
            }
        } catch (IOException e) {
            log.error("Failed to send request to InfluxDB: %s", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to send request to InfluxDB: %s", e.getMessage());
        }

    }

    @NotNull
    private static String escapeFieldForInflux(Object value) {
        if (value == null) {
            return "\"\"";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        String stringValue = value.toString();
        if (stringValue.contains("\"") || stringValue.contains("\\")) {
            return "\"" + escapeSlashesAndDoubleQuotes(value.toString()) + "\"";
        }
        return "\"" + value + "\"";
    }

    private static String escapeSlashesAndDoubleQuotes(String text) {
        return text.replace("\"", "\\\"").replace("\\", "\\\\");
    }

    @Override
    public void close() throws Exception {
    }
}
