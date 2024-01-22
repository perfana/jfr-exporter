# JfrExporter

Send JFR events to time series databases.

Now in "beta", feedback welcome!

See tutorial in this [blog post](https://perfana.io/continuous-deep-dive-with-jfr-events/).

[![JfrExporter tutarial video](https://img.youtube.com/vi/rAq2Xo-VoVc/0.jpg)](https://www.youtube.com/watch?v=rAq2Xo-VoVc)

Makes use of [JFR Event Streaming](https://openjdk.org/jeps/349) as found in hotspot based JDK 14+.

The InfluxDB time series database is used as storage for the metrics.

The metrics are displayed in a Grafana dashboard, as shown in the following screenshots.

CPU, Heap, Threads and Memory Allocation Rate:
![dashboard overview 1](images/dashboard-6.jpg)

Garbage Collection events:
![dashboard overview 2](images/dashboard-8.jpg)

Large allocation samples and Big Allocations:
![dashboard overview 3](images/dashboard-7.jpg)

Java Monitor waits and enters:
![dashboard overview 4](images/dashboard-4.jpg)

Network reads and writes:
![dashboard overview 5](images/dashboard-5.jpg)

For some events stacktraces are present, such as where in the code a big memory allocation took place.
(see screenshot below)

## Steps

To use JfrExporter:

1. create a database in InfluxDB (or use an existing one)
2. download and put the agent jar on the classpath, point it to InfluxDB
3. create a datasource in Grafana for the InfluxDB database
4. import the Grafana dashboard from the `dashboards` directory, choose InfluxDB datasource
5. start the application
6. optional: start load test

## Download

Direct [download version 0.3.0](https://github.com/perfana/jfr-exporter/releases/download/0.3.0/jfr-exporter-0.3.0.jar)

Download the latest release from the [releases page](https://github.com/perfana/jfr-exporter/releases).

## Agent

To use JfrExporter simply add `-javaagent:/path/to/jfr-exporter.jar` to your JVM arguments.

There is no need to enable JFR in the JVM arguments (`-XX:StartFlightRecording`).

Options can be sent as `-javaagent:/path/to/jfr-exporter.jar=option1=value1,option2=value2`.

Example: `-javaagent:/path/to/jfr-exporter.jar=debug,application=afterburner`, which will enable
debug logging and set the application name to "afterburner".

When used as agent, there is no need to add JFR activation to the JVM arguments.

Be aware that some shells require a backslash before the equal signs.

## Standalone

JfrExporter can also be used as a standalone application, for example to monitor a running process.

The monitored process needs to be started with JFR enabled: `-XX:StartFlightRecording`

```bash
Usage: java JfrExporter 
 --debug,-d 
 --processId,-p <processId> 
 --duration <ISO-duration> 
 --application,-a <application>
 --bigObjectThreshold <bytes>
 --bigObjectSampleWeightThreshold <bytes>
 --influxUrl <influxUrl> 
 --influxDatabase <influxDatabase>
 --influxUser <influxUser> 
 --influxPassword <influxPassword>
```

The default InfluxDB database name is `jfr`.

Example to connect to process with id 1234 and send events with application name afterburner:
```bash
java -jar jfr-exporter.jar --processId 1234 --application afterburner \
  --duration PT30s --influxUrl http://localhost:8086
```

To enable extra monitoring, such as safe points, or different thresholds, 
use a saved JFR profile in the JDK used, for example saved as `mySettings`: `-XX:StartFlightRecording:settings=mySettings`

## Events

These JFR events are processed: 
* CPU load
* Thread count
* Classes loaded
* Garbage Collection (GC) events
* Safepoints
* Memory (heap usage, large allocations)
* Network read/write
* Java Monitor waits and enters

For reference: [list of JFR events](https://bestsolution-at.github.io/jfr-doc/index.html).

## Stacktraces

Stack traces for several events are sent to InfuxDB.
Via the dashboard you can see the details by clicking in the stacktrace columns.

Example of a big memory allocation stacktrace:
![stacktrace example 1](images/stacktrace-2.jpg)

## Dashboard

A Grafana dashboard can be imported to view the JFR metrics.

Import the dashboard in the `dashboards` directory into Grafana and
connect to an InfluxDB datasource that points to the `jfr` database.

For version 0.3.0 and above use dashboard `jfr-dashboard-export-share-0.3.json`.

## Troubleshoot

Use `-Dio.perfana.jfr.debug=true` to enable debug logging or `--debug` as argument.

For tracing (more debug logging) use: `-Dio.perfana.jfr.trace=true`

Debug and tracing will output a lot of data, so only use for troubleshooting.

# Releases

### v0.3.0: January 2024
* Added new events: Java Monitor waits and enters, Network read/write
* New dashboard with new events
