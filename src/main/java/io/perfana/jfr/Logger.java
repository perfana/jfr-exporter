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

import java.util.concurrent.atomic.AtomicBoolean;

public class Logger {

    private static final AtomicBoolean isDebugEnabled =
        "true".equalsIgnoreCase(System.getProperty("io.perfana.jfr.log.debug")) ?
                new AtomicBoolean(true) : new AtomicBoolean(false);

    private static final AtomicBoolean isTraceEnabled =
        "true".equalsIgnoreCase(System.getProperty("io.perfana.jfr.log.trace")) ?
                new AtomicBoolean(true) : new AtomicBoolean(false);

    public static final Object[] NO_ARGS = {};
    private final String className;

    private Logger(Class<?> clazz) {
        this.className = clazz.getSimpleName();
    }

    public static void enabledDebug() {
        isDebugEnabled.set(true);
    }

    public void debug(String message) {
        debug(message, NO_ARGS);
    }

    public void debug(String message, Object... args) {
        if (isDebugEnabled.get()) {
            println("[DEBUG]", message, args);
        }
    }

    public void info(String message, Object... args) {
        println("[INFO]", message, args);
    }

    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz);
    }

    private void println(String prefix, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        System.out.println(prefix + " " + className + ": " + message);
    }

    public void error(String message, Object... args) {
        println("[ERROR]", message, args);
    }
    public void trace(String message, Object... args) {
        if (isTraceEnabled.get()) {
            println("[TRACE]", message, args);
        }
    }

}
