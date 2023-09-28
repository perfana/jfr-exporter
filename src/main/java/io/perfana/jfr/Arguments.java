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

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

public class Arguments {
    private Integer processId = null;
    private String application = "jfr-exporter";
    private String influxUrl = null;
    private String influxDatabase = "jfr";
    private String influxUser = "";
    private String influxPassword = "";
    private boolean debug = false;
    private String perfanaUrl = null;
    private String perfanaApiKey = null;
    private Duration duration = null;
    private String influxRetentionPolicy = "autogen";
    private long bigObjectThresholdBytes = 1_000_000L;

    public static String usage() {
        return "Usage: java JfrExporter " +
                "--debug,-d --processId,-p <processId> " +
                " --duration <ISO-duration> --application,-a <application>" +
                " --bigObjectThreshold <bytes>" +
                " --influxUrl <influxUrl> --influxDatabase <influxDatabase>" +
                " --influxUser <influxUser> --influxPassword <influxPassword>" +
                " --perfanaUrl <perfanaUrl> --perfanaApiKey <perfanaApiKey>";
    }

    public static void print(String message) {
        System.out.println(message);
    }

    public static Arguments parseArgs(String[] args) {

        Queue<String> options = new ArrayDeque<>(Arrays.asList(args));

        Arguments arguments = new Arguments();

        while (!options.isEmpty()) {
            String arg = options.remove();

            if (matches(arg, "-h", "--help", "help")) {
                print(Arguments.usage());
                System.exit(1);
            }

            if (matches(arg, "-d", "--debug", "debug")) {
                arguments.debug = true;
                continue;
            }

            if (matches(arg, "-p", "--processId", "processId")) {
                String processId = options.remove();
                arguments.processId = Integer.parseInt(processId);
                continue;
            }

            if (matches(arg, "-a", "--application", "application")) {
                arguments.application = options.remove();
                continue;
            }

            if (matches(arg,  "--influxUrl", "influxUrl")) {
                arguments.influxUrl = options.remove();
                continue;
            }

            if (matches(arg, "", "--influxDatabase", "influxDatabase")) {
                arguments.influxDatabase = options.remove();
                continue;
            }

            if (matches(arg, "", "--influxUser", "influxUser")) {
                arguments.influxUser = options.remove();
                continue;
            }

            if (matches(arg, "", "--influxPassword", "influxPassword")) {
                arguments.influxPassword = options.remove();
                continue;
            }

            if (matches(arg, "", "--perfanaUrl", "perfanaUrl")) {
                arguments.perfanaUrl = options.remove();
                continue;
            }

            if (matches(arg, "", "--perfanaApiKey", "perfanaApiKey")) {
                arguments.perfanaApiKey = options.remove();
                continue;
            }

            if (matches(arg, "", "--duration", "duration")) {
                arguments.duration = Duration.parse(options.remove());
                continue;
            }

            if (matches(arg, "", "--bigObjectThreshold", "bigObjectThreshold")) {
                arguments.bigObjectThresholdBytes = Long.parseLong(options.remove());
                continue;
            }

            print("WARN: unknown option: " + arg);

        }

        return arguments;
    }

    private static boolean matches(String arg, String... matchers) {
        return Arrays.asList(matchers).contains(arg);
    }

    public Integer getProcessId() {
        return processId;
    }

    public String getApplication() {
        return application;
    }

    public long getBigObjectThresholdBytes() {
        return bigObjectThresholdBytes;
    }

    public String getInfluxUrl() {
        return influxUrl;
    }

    public String getInfluxDatabase() {
        return influxDatabase;
    }

    public String getInfluxUser() {
        return influxUser;
    }

    public String getInfluxPassword() {
        return influxPassword;
    }

    public boolean isDebug() {
        return debug;
    }

    public String getPerfanaUrl() {
        return perfanaUrl;
    }

    public String getPerfanaApiKey() {
        return perfanaApiKey;
    }

    public Duration getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return "Arguments{" +
                "processId=" + processId +
                ", application='" + application + '\'' +
                ", bigObjectThreshold='" + bigObjectThresholdBytes + '\'' +
                ", influxUrl='" + influxUrl + '\'' +
                ", influxDatabase='" + influxDatabase + '\'' +
                ", influxUser='" + influxUser + '\'' +
                ", influxPassword='" + influxPassword + '\'' +
                ", debug=" + debug +
                ", perfanaUrl='" + perfanaUrl + '\'' +
                ", perfanaApiKey='" + perfanaApiKey + '\'' +
                ", duration=" + duration +
                '}';
    }

    public String getInfluxRetentionPolicy() {
        return influxRetentionPolicy;
    }

}

