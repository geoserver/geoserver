/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.geometry;

import java.io.Serializable;

/**
 * 
 * @author Juan Marin - OpenGeo
 *
 */
public abstract class Geometry implements Serializable {

    protected GeometryType geometryType;

    public GeometryType getGeometryType() {
        return geometryType;
    }

    public void setGeometryType(GeometryType geometryType) {
        this.geometryType = geometryType;
    }
}
