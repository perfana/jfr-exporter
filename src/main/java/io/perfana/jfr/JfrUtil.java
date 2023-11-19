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

import jdk.jfr.consumer.RecordedEvent;
import org.jetbrains.annotations.NotNull;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JfrUtil {

    private JfrUtil() {
    }

    public static String translatePrimitiveClass(String objectClass) {
        if (objectClass.startsWith("[")) {
            int lastIndexOf = objectClass.lastIndexOf('[');
            String remainder = objectClass.substring(lastIndexOf + 1);

            String arrayPrefix = arrayPrefix(lastIndexOf + 1);

            String baseType = switch (remainder.charAt(0)) {
                case 'Z' -> "boolean";
                case 'B' -> "byte";
                case 'C' -> "char";
                case 'D' -> "double";
                case 'F' -> "float";
                case 'I' -> "int";
                case 'J' -> "long";
                case 'S' -> "short";
                case 'L' -> extractObject(remainder);
                default -> throw new JfrExporterException("Unknown array type: " + objectClass);
            };
            return baseType + arrayPrefix;
        }
        else {
            return objectClass;
        }
    }

    private static String arrayPrefix(int count) {
        if (count == 1) return "[]";
        if (count == 2) return "[][]";
        if (count == 3) return "[][][]";
        else return IntStream.range(0, count).mapToObj(i -> "[]").collect(Collectors.joining());
    }

    static String extractObject(String remainder) {
        int end = remainder.indexOf(';');
        return remainder.substring(1, end);
    }

    @NotNull
    public static List<String> translateStacktrace(RecordedEvent event) {
        return  event.getStackTrace().getFrames().stream()
                .map(f -> f.getMethod().getType().getName() + "." + f.getMethod().getName() + " (line: " + f.getLineNumber() + ")")
                .toList();
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
