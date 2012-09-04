/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.feature;

/**
 * 
 * @author Juan Marin, OpenGeo
 *
 */
public class StringField extends Field {

    private int length;

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public StringField(String name, FieldTypeEnum type, String alias, int length) {
        super(name, type, alias);
        this.length = length;

    }

}
