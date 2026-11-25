package com.homeenergymonitor.meter;

/**
 * Hardware-independent boundary for a smart electricity meter.
 *
 * <p>Version 1 ships only a simulated implementation. Real adapters (Modbus, MQTT,
 * vendor gateways, ...) can be added later without changing telemetry, persistence
 * or the REST/SSE layers.</p>
 */
public interface MeterAdapter {

    String meterSerial();

    /** Start or connect to the meter source. */
    void start();

    /** Stop or disconnect from the meter source. */
    void stop();

    boolean isConnected();

    /** Obtain the current measurement. Implementations own any internal state. */
    MeterSample read();

    /** Obtain the current device/connection status. */
    MeterStatus status();
}
