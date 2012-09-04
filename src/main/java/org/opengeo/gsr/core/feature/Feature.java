/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.feature;

import java.util.Map;

import org.opengeo.gsr.core.geometry.Geometry;

import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class Feature {

    private Geometry geometry;

    @XStreamImplicit
    private Map<String, Object> attributes;

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Feature(Geometry geometry, Map<String, Object> attributes) {
        super();
        this.geometry = geometry;
        this.attributes = attributes;
    }
}
