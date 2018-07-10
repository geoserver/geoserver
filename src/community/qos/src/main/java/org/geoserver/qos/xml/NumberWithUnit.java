/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;

public abstract class NumberWithUnit<T extends Serializable> implements Serializable {
    private static final long serialVersionUID = 1L;

    private T value;
    private String uom;

    public NumberWithUnit() {}

    public NumberWithUnit(T value, String uom) {
        this.value = value;
        this.uom = uom;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public abstract static class DoubleValue extends NumberWithUnit<Double> {

        public DoubleValue() {
            super();
        }

        public DoubleValue(Double value, String uom) {
            super(value, uom);
        }
    }

    public static class LessThanOrEqual extends DoubleValue {
        private static final long serialVersionUID = 1L;

        public LessThanOrEqual() {
            super();
        }

        public LessThanOrEqual(Double value, String uom) {
            super(value, uom);
        }
    }

    public static class MoreThanOrEqual extends DoubleValue {
        private static final long serialVersionUID = 1L;

        public MoreThanOrEqual() {
            super();
        }

        public MoreThanOrEqual(Double value, String uom) {
            super(value, uom);
        }
    }
}
