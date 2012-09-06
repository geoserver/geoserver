/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.feature;

/**
 * 
 * @author Juan Marin, OpenGeo
 *
 * @param <T>
 */
public class Attribute {

    private String name;

    private Object value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Attribute(String name, Object value) {
        super();
        this.name = name;
        this.value = value;
    }

}
