/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

public enum DayOfWeek {
    MONDAY("Monday"),
    TUESDAY("Tuesday"),
    WEDNESDAY("Wednesday"),
    THURSDAY("Thursday"),
    FRIDAY("Friday"),
    SATURDAY("Saturday"),
    SUNDAY("Sunday"),
    EVERYDAY("EveryDay");

    private final String name;

    DayOfWeek(String name) {
        this.name = name;
    }

    public String value() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static DayOfWeek fromValue(String value) {
        for (DayOfWeek d : DayOfWeek.values()) {
            if (d.value().equals(value)) return d;
        }
        throw new IllegalArgumentException(value);
    }
}
