/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum OperationalStatus {
    OPERATIONAL("Operational"),
    PREOPERATIONAL("PreOperational"),
    NONOPERATIONAL("NonOperational");

    public static final String URL =
            "http://def.opengeospatial.org/codelist/qos/status/1.0/operationalStatus.rdf#";

    private String code;

    private OperationalStatus(String code) {
        this.code = code;
    }

    public String value() {
        return code;
    }

    @Override
    public String toString() {
        return value();
    }

    public static OperationalStatus fromValue(String value) {
        for (OperationalStatus os : OperationalStatus.values()) {
            if (os.value().equals(value)) return os;
        }
        return null;
    }

    public static List<String> valuesStringList() {
        return Arrays.asList(OperationalStatus.values())
                .stream()
                .map(x -> x.value())
                .collect(Collectors.toList());
    }
}
