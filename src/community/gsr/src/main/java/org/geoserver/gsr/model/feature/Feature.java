/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.feature;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import org.geoserver.gsr.model.geometry.Geometry;

/**
 * Feature model object
 *
 * <p>See https://developers.arcgis.com/documentation/common-data-types/feature-object.htm
 *
 * @author Juan Marin, OpenGeo
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Feature {

    private Geometry geometry;

    private Map<String, Object> attributes;

    private Object id;

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public Feature(Geometry geometry, Map<String, Object> attributes, Object id) {

        super();
        this.id = id;
        this.geometry = geometry;
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }
}
