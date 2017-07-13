/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.core.feature;

import java.util.Map;

import com.boundlessgeo.gsr.core.geometry.Geometry;

/**
 *
 * @author Juan Marin, OpenGeo
 *
 */
public class Feature {

    private Geometry geometry;

    private Map<String, Object> attributes;

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public Feature(Geometry geometry, Map<String, Object> attributes) {

        super();
        this.geometry = geometry;
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
