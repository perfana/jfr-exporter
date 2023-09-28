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

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordingStream;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

public class JfrConnector {

    private static final Logger log = Logger.getLogger(JfrConnector.class);

    private final JfrEventHandler eventHandler;

    JfrConnector(JfrEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    public void connectInternalJVM() {
        connectInternalJVM(null);
    }

    public void connectInternalJVM(Duration duration) {
        log.info("Connect to internal JVM for %s seconds", duration == null ? "unlimited" : duration.getSeconds());
        try (RecordingStream stream = new RecordingStream()) {
            eventHandler.enableEvents(stream);
            processEventStream(stream, duration);
        }
    }

    public void connectRemoteJvm(int processId) {
        connectRemoteJvm(processId, null);
    }

    public void connectRemoteJvm(int processId, Duration duration) {
        log.info("Connect to remote JVM with process id %d for %s seconds", processId, duration == null ? "unlimited" : duration.getSeconds());

        Path jfrRepositoryDir = findJfrRepository(processId);

        try (EventStream stream = EventStream.openRepository(jfrRepositoryDir)) {
            processEventStream(stream, duration);
        } catch (IOException e) {
            throw new JfrExporterException("Error while opening jfr repository: " + jfrRepositoryDir, e);
        }

    }

    private void processEventStream(EventStream stream, Duration duration) {

        long endTimestamp = duration == null
                ? Long.MAX_VALUE
                : System.currentTimeMillis() + duration.toMillis();

        eventHandler.subscribe(stream);

        stream.startAsync();
        while (System.currentTimeMillis() < endTimestamp) {
            sleep(500);
        }
    }

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Path findJfrRepository(int processId) {
        checkVirtualMachineAttach();

        // see: https://openjdk.org/jeps/349
        try {
            VirtualMachine vm = VirtualMachine.attach(String.valueOf(processId));

            Properties systemProperties = vm.getSystemProperties();
            String jfrRepositoryDir = (String) systemProperties.get("jdk.jfr.repository");

            if (jfrRepositoryDir == null) {
                throw new JfrExporterException("Cannot find 'jdk.jfr.repository' property in remote jvm using processId: " + processId +
                        ". Please make sure you are using a JDK with JEP 349 (JDK 14+) or later and use '-XX:StartFlightRecording'.");
            }
            return Path.of(jfrRepositoryDir);

        } catch (AttachNotSupportedException e) {
            throw new JfrExporterException("Cannot attach to remote jvm using processId: " + processId, e);
        } catch (IOException e) {
            throw new JfrExporterException("Issues connecting to remote jvm using processId: " + processId, e);
        }
    }

    private static void checkVirtualMachineAttach() {
        try {
            Class.forName("com.sun.tools.attach.VirtualMachine");
        } catch (ClassNotFoundException e) {
            throw new JfrExporterException("Connecting to processId only works on certain JVMs. Sorry, not this one... ", e);
        }
    }
}
