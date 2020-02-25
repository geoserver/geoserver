/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.io.Serializable;

/**
 * Contents of the css page data attribute table
 *
 * @author Andrea Aime - GeoSolutions
 */
class DataAttribute implements Serializable {

    private static final long serialVersionUID = -6470442390382618241L;

    String name;

    String type;

    String sample;

    String min;

    String max;

    public DataAttribute(String name, String type, String sample) {
        super();
        this.name = name;
        this.type = type;
        this.sample = sample;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSample() {
        return sample;
    }

    public void setSample(String sample) {
        this.sample = sample;
    }

    public String getMin() {
        return min;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }
}
