/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.feature;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class Field {

    private String name;

    private FieldTypeEnum type;

    private String alias;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FieldTypeEnum getType() {
        return type;
    }

    public void setType(FieldTypeEnum type) {
        this.type = type;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Field(String name, FieldTypeEnum type, String alias) {
        super();
        this.name = name;
        this.type = type;
        this.alias = alias;
    }

}
