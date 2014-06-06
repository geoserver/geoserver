/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.feature;

import java.util.List;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class AttributeList {

    private List<Attribute> attributes;

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public AttributeList(List<Attribute> attributes) {
        super();
        this.attributes = attributes;
    }

    public void add(Attribute attribute) {
        attributes.add(attribute);
    }

}
