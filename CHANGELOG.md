# Change log jfr-exporter

### v0.4.1: May 2024
* socket events: lowered dynamic ports to 16_000 

### v0.4.0: May 2024
* Added new events:
    * Native Memory
    * Container CPU and memory
    * Thread context switch rate
* Updated dashboard with new events
* Improved dashboard, application selection is working for all panels

### v0.3.0: January 2024
* Added new events: Java Monitor waits and enters, Network read/write
* New dashboard with new events

### v0.3.1: April 2024
* Make use of buffer to send events in batches
* Make event handling more robust by catching exceptions
* Added disableStackTraces option to limit the amount of stacktrace data