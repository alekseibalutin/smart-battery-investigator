
package com.alexbalutin.smartbattery.telemetry;

public interface TelemetryProvider {

    TelemetrySnapshot collect();

    String mode();
}
