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

import io.perfana.jfr.event.MetricCalculation;
import jdk.jfr.consumer.RecordedEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A processed JFR event.
 * The timestamp can be null if not present.
 */
public record ProcessedJfrEvent(@Nullable Instant timestamp,
                                @Nonnull String measurementName,
                                @Nonnull String field,
                                @Nonnull Number value,
                                @Nonnull List<String> stacktrace,
                                @Nonnull Map<String, Object> extraFields) {
    public ProcessedJfrEvent {
        if (measurementName.isBlank()) {
            throw new IllegalArgumentException("measurementName cannot be blank.");
        }
        if (field.isBlank()) {
            throw new IllegalArgumentException("field cannot be blank.");
        }
    }

    public static ProcessedJfrEvent of(@Nullable Instant timestamp,
                                       @Nonnull String measurementName,
                                       @Nonnull String field,
                                       @Nonnull Number value) {
        return new ProcessedJfrEvent(timestamp, measurementName, field, value, List.of(), Map.of());
    }

    public static ProcessedJfrEvent of(RecordedEvent event, String measurementName, String metric, MetricCalculation calculation) {
       double metricValue = event.getDouble(metric);
       double metricValueCalculated = calculation.calculate(metricValue);
       return of(
               event.getStartTime(),
               measurementName,
               metric,
               metricValueCalculated
       );

   }

    public String toStringShort() {
        return "ProcessedJfrEvent{" +
                "timestamp=" + timestamp +
                ", measurementName='" + measurementName + '\'' +
                ", field='" + field + '\'' +
                ", value=" + value +
                ", stacktrace=" + (stacktrace.isEmpty() ? "[]" : stacktrace.get(0) + "...") +
                ", extraFields=" + extraFields +
                '}';
    }
}