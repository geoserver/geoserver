/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.system.status;

import java.io.Serializable;
import java.time.LocalTime;
import org.geotools.util.Converters;

/**
 * Stores values and configuration of system information metrics This object is serialized by
 * MonitorRest to provide XML, JSON and HTML view of data
 *
 * @author sandr
 */
public class MetricValue implements Serializable {

    private static final long serialVersionUID = 344784541680947799L;

    Object value;

    Boolean available;

    String description;

    String name;

    String unit;

    String category;

    String identifier;

    int priority;

    ValueHolder holder;

    /**
     * Initialize the metric value coping the definition from infomration obejct {@link MetricInfo}
     *
     * @param info the data associated with information to retrieve
     */
    public MetricValue(MetricInfo info) {
        this.priority = info.getPriority();
        this.name = info.name();
        this.description = info.getDescription();
        this.unit = info.getUnit();
        this.category = info.getCategory();
        this.identifier = info.name();
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
        this.holder = new ValueHolder(value);
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

    public String getCategory() {
        return category;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getValueUnit() {
        if (!available || value == null) {
            return BaseSystemInfoCollector.DEFAULT_VALUE;
        }
        if (value instanceof Double || value instanceof Float) {
            final Number numberValue = (Number) value;
            return String.format(
                    "%.2f %s",
                    value instanceof Double ? numberValue.doubleValue() : numberValue.floatValue(),
                    unit);
        }
        if (unit != null && unit.equalsIgnoreCase("bytes")) {
            long bytes = Converters.convert(value, Long.class);
            return humanReadableByteCount(bytes);
        } else if (unit != null && unit.equalsIgnoreCase("sec")) {
            long seconds = Converters.convert(value, Long.class);
            return LocalTime.MIN.plusSeconds(seconds).toString();
        }
        return String.format("%s %s", value, unit);
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Based on this article:
     * http://programming.guide/java/formatting-byte-size-to-human-readable-format.html
     */
    private static String humanReadableByteCount(long bytes) {
        // df -h and du -h use 1024 by default, system monitoring use MB
        int unit = 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %siB", bytes / Math.pow(unit, exp), pre);
    }

    /** Value holder used for XML and JSOn encoding. */
    public static class ValueHolder implements Serializable {

        private final Object valueOlder;

        public ValueHolder(Object valueOlder) {
            this.valueOlder = valueOlder;
        }

        public Object getValue() {
            return valueOlder;
        }
    }
}
