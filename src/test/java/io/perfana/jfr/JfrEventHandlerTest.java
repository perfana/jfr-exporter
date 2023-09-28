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
import jdk.jfr.consumer.RecordedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JfrEventHandlerTest {

    @Test
    void handle() {

        RecordedEvent eventMock = Mockito.mock(RecordedEvent.class);
        Mockito.when(eventMock.getDuration()).thenReturn(Duration.ofMillis(1234));

        JfrEventHandler jfrEventHandler = new JfrEventHandler();
        OnJfrEvent onJfrEvent = event -> assertEquals(Duration.ofMillis(1234), event.getDuration());
        jfrEventHandler.register(JfrEventSettings.of("test", onJfrEvent).withPeriod(Duration.ofMillis(100)));
        jfrEventHandler.handle("test", eventMock);
    }

}