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

import io.perfana.jfr.JfrEventProcessor;
import io.perfana.jfr.Logger;
import io.perfana.jfr.ProcessedJfrEvent;

public class InfluxEventProcessor implements JfrEventProcessor, AutoCloseable {

    private static final Logger log = Logger.getLogger(InfluxEventProcessor.class);

    private final InfluxWriter writer;

    public InfluxEventProcessor(InfluxWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException("InfluxWriter must not be null");
        }
        this.writer = writer;
    }

    @Override
    public void processEvent(ProcessedJfrEvent event) {
        log.debug("Process influx event: %s", event.toStringShort());
        writer.writeMetricPoint(event);
    }

    @Override
    public void close() throws Exception {
        writer.close();
    }
}
