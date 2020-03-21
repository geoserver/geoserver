/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.feature;

import com.boundlessgeo.gsr.model.GSRModel;

/**
 *
 * @author Juan Marin, OpenGeo
 *
 */
public class Field implements GSRModel {

    private String name;

    private String type;

    private String alias;
    private Integer length;
    private Boolean editable;
    private Boolean nullable;
    private String domain = null;

    private String defaultValue = null;


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

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Integer getLength() {
        return length;
    }

    public Boolean getEditable() {
        return editable;
    }

    public Boolean getNullable() {
        return nullable;
    }

    public String getDomain() {
        return domain;
    }

    public Field(String name, FieldTypeEnum type, String alias) {
        this(name, type, alias, null);
    }

    public Field(String name, FieldTypeEnum type, String alias, Integer length) {
        this(name, type, alias, length, null, null);
    }

    public Field(String name, FieldTypeEnum type, String alias, Integer length, Boolean editable, Boolean nullable) {
        this.name = name;
        this.type = type.getFieldType();
        this.alias = alias;
        this.length = length;
        this.editable = editable;
        this.nullable = nullable;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
