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

import java.time.Duration;

public class JfrEventSettings {

    private final String name;

    private final OnJfrEvent onJfrEvent;

    private Duration period;

    private Duration threshold;

    private ThresholdPair otherThreshold;

    private JfrEventSettings(String name, OnJfrEvent onJfrEvent) {
        this.name = name;
        this.onJfrEvent = onJfrEvent;
    }

    public static JfrEventSettings of(String name, OnJfrEvent onJfrEvent) {
        return new JfrEventSettings(name, onJfrEvent);
    }

    public JfrEventSettings withPeriod(Duration period) {
        this.period = period;
        return this;
    }

    public JfrEventSettings withThreshold(Duration threshold) {
        this.threshold = threshold;
        return this;
    }

    public JfrEventSettings withThreshold(String name, String value) {
        this.otherThreshold = new ThresholdPair(name, value);
        return this;
    }

    public String getName() {
        return name;
    }

    public OnJfrEvent getOnJfrEvent() {
        return onJfrEvent;
    }

    public Duration getPeriod() {
        return period;
    }

    public boolean hasPeriod() {
        return period != null;
    }

    public boolean hasOtherThreshold() {
        return otherThreshold != null;
    }
    public boolean hasThreshold() {
        return threshold != null;
    }

    public ThresholdPair getOtherThreshold() {
        return otherThreshold;
    }

    public Duration getThreshold() {
        return threshold;
    }

}
