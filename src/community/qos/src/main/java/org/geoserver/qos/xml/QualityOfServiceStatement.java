/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;

public class QualityOfServiceStatement implements Serializable {
    private static final long serialVersionUID = 1L;

    private Metric metric;
    private StringValue meassure = new StringValue();

    private ValueType valueType;

    public QualityOfServiceStatement() {}

    public Metric getMetric() {
        return metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    public StringValue getMeassure() {
        return meassure;
    }

    public void setMeassure(StringValue meassure) {
        this.meassure = meassure;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    public static enum ValueType {
        moreThanOrEqual("MoreThanOrEqual"),
        lessThanOrEqual("LessThanOrEqual"),
        value("Value");

        private String type;

        private ValueType(String type) {
            this.type = type;
        }

        public String value() {
            return type;
        }

        @Override
        public String toString() {
            return value();
        }
    }
}
