/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.status.monitoring.collector;

import java.io.Serializable;

/**
 * 
 * Stores values and configuration of system information metrics This object is serialized by
 * {@link MonitorRest} to provide XML, JSON and HTML view of data
 * 
 * @author sandr
 *
 */
public class MetricValue implements Serializable {

    private static final long serialVersionUID = 344784541680947799L;

    String value;

    Boolean available;

    String description;

    String name;

    String unit;

    String valueUnit;

    /**
     * Initialize the metric value coping the definition from infomration obejct {@link MetricInfo}
     * 
     * @param info
     *            the data associated with information to retrieve
     */
    public MetricValue(MetricInfo info) {
        this.name = info.name();
        this.description = info.getDescription();
        this.unit = info.getUnit();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * @return value with unit of measurement
     */
    public String getValueUnit() {
        if (available) {
            return this.value + " " + this.unit;
        } else {
            return this.value;
        }
    }

}
