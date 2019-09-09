/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.dto.impl;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.dto.FieldTypeEnum;
import org.geoserver.metadata.data.dto.OccurrenceEnum;

/**
 * Object that matches yaml structure.
 *
 * <p>The configuration descibes one field for the gui.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class AttributeConfigurationImpl implements AttributeConfiguration {

    private static final long serialVersionUID = 3130368513874060531L;

    String key;

    String label;

    FieldTypeEnum fieldType;

    OccurrenceEnum occurrence = OccurrenceEnum.SINGLE;

    List<String> values = new ArrayList<>();

    String typename;

    String derivedFrom;

    String condition;

    public AttributeConfigurationImpl() {}

    public AttributeConfigurationImpl(String key, FieldTypeEnum fieldType) {
        this.key = key;
        this.label = key;
        this.fieldType = fieldType;
    }

    public AttributeConfigurationImpl(AttributeConfigurationImpl other) {
        if (other != null) {
            key = other.getKey();
            label = other.getLabel();
            fieldType = other.getFieldType();
            occurrence = other.getOccurrence();
            typename = other.getTypename();
            for (String values : other.getValues()) {
                this.values.add(values);
            }
        }
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public FieldTypeEnum getFieldType() {
        return fieldType;
    }

    @Override
    public List<String> getValues() {
        return values;
    }

    @Override
    public String getTypename() {
        return typename;
    }

    @Override
    public OccurrenceEnum getOccurrence() {
        return occurrence;
    }

    @Override
    public String getDerivedFrom() {
        return derivedFrom;
    }

    @Override
    public String getCondition() {
        return condition;
    }
}
