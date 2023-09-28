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

import javax.management.*;
import java.lang.management.ManagementFactory;

public class JfrUtil {

    private JfrUtil() {
    }

    void startFlightRecorderViaMBean() {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        String diagnosticCommandBeanName = "com.sun.management:type=DiagnosticCommand";
        ObjectName objectName;
        try {
            objectName = new ObjectName(diagnosticCommandBeanName);
        } catch (MalformedObjectNameException e) {
            throw new JfrExporterException("Cannot find MBean: " + diagnosticCommandBeanName, e);
        }
        Object[] args = new Object[] {
                new String[] {
                        "dumponexit=true",
                        "filename=/recordings/rec.jfr",
                        "duration=600s"
                }
        };
        String[] sig = new String[] {"[Ljava.lang.String;"};
        try {
            mBeanServer.invoke(objectName, "jfrStart", args, sig);
        } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
            throw new JfrExporterException("Cannot call jfrStart.", e);
        }
    }
}
