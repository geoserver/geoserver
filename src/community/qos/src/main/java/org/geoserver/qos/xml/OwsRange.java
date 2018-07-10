/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;

public class OwsRange implements Serializable {

    private String minimunValue;
    private String maximunValue;
    private String spacing;

    public OwsRange() {}

    public String getMinimunValue() {
        return minimunValue;
    }

    public void setMinimunValue(String minimunValue) {
        this.minimunValue = minimunValue;
    }

    public String getMaximunValue() {
        return maximunValue;
    }

    public void setMaximunValue(String maximunValue) {
        this.maximunValue = maximunValue;
    }

    public String getSpacing() {
        return spacing;
    }

    public void setSpacing(String spacing) {
        this.spacing = spacing;
    }
}
