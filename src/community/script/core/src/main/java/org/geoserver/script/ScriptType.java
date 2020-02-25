/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

/**
 * An enumeration representing the different scripting extension points.
 *
 * @author Jared Erickson
 */
public enum ScriptType {
    APP("App"),
    FUNCTION("Function"),
    WPS("WPS"),
    WFSTX("WFS/TX");

    private final String label;

    ScriptType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ScriptType getByLabel(String label) {
        for (ScriptType type : ScriptType.values()) {
            if (type.getLabel().equalsIgnoreCase(label) || type.name().equalsIgnoreCase(label)) {
                return type;
            }
        }
        return null;
    }
}
