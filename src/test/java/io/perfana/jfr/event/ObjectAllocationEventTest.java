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
import io.perfana.jfr.NoopEventProcessor;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedStackTrace;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class ObjectAllocationEventTest {

    @Test
    void onEvent() {
        JfrEventProcessor eventProcessor = new NoopEventProcessor();
        ObjectAllocationEvent objectAllocationEvent = new ObjectAllocationEvent(eventProcessor, 1_000_000);

        RecordedEvent eventMock = Mockito.mock(RecordedEvent.class);

        EventType eventTypeMock = Mockito.mock(EventType.class);
        Mockito.when(eventTypeMock.getName()).thenReturn("jdk.ObjectAllocationSample");
        Mockito.when(eventMock.getEventType()).thenReturn(eventTypeMock);

        Mockito.when(eventMock.getDuration()).thenReturn(Duration.ofMillis(1234));

        RecordedClass recordedClassMock = Mockito.mock(RecordedClass.class);
        Mockito.when(recordedClassMock.getName()).thenReturn("java.lang.String");
        Mockito.when(eventMock.getClass("objectClass")).thenReturn(recordedClassMock);

        // no allocation trace created when smaller than threshold
        Mockito.when(eventMock.getLong("weight")).thenReturn(900_000L);

        assertDoesNotThrow(() -> objectAllocationEvent.onEvent(eventMock));
    }

    @Test
    void testTranslate() {
        assertEquals(byte.class.getCanonicalName(), ObjectAllocationEvent.translatePrimitiveClass("byte"));
        assertEquals("byte[]", ObjectAllocationEvent.translatePrimitiveClass("[B"));
        assertEquals("java.lang.Byte[]", ObjectAllocationEvent.translatePrimitiveClass("[Ljava.lang.Byte;"));
        assertEquals(byte[][][][][].class.getCanonicalName(), ObjectAllocationEvent.translatePrimitiveClass("[[[[[B"));
    }
}