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

import io.perfana.jfr.event.OnJfrEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JfrConnectorTest {

    @Test
    void connect() {

        AtomicBoolean eventCalled = new AtomicBoolean(false);

        JfrEventHandler eventHandler = new JfrEventHandler();
        OnJfrEvent on = event -> {
            eventCalled.set(true);
            System.out.println("from test event '" + event.getEventType().getName() + "': " + event.getDouble("machineTotal"));
        };
        eventHandler.register(JfrEventSettings.of("jdk.CPULoad", on).withPeriod(Duration.ofMillis(100)));

        JfrConnector jfrConnector = new JfrConnector(eventHandler);

        jfrConnector.connectInternalJVM(Duration.ofMillis(2000));

        assertTrue(eventCalled.get(), "event called check");
    }
}