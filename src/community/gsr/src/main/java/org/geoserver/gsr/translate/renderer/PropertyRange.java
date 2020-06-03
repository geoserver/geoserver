/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.translate.renderer;

import org.geotools.util.NumberRange;

class PropertyRange {

    String propertyName;
    NumberRange range;

    public PropertyRange(String propertyName, NumberRange range) {
        this.propertyName = propertyName;
        this.range = range;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public NumberRange getRange() {
        return range;
    }

    public void setRange(NumberRange range) {
        this.range = range;
    }
}
