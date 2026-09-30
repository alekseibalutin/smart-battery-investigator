
package com.alexbalutin.smartbattery.api;

final class RequestParameters {

    private RequestParameters() {
    }

    static int validateHours(int hours) {
        if (hours < 1 || hours > 168) {
            throw new IllegalArgumentException("hours must be between 1 and 168");
        }
        return hours;
    }
}
