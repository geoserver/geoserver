/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.translate.geometry;

/**
 * Default implementation of {@link AbstractGeometryEncoder}. Encodes geometries as arrays of
 * doubles.
 */
public final class GeometryEncoder extends AbstractGeometryEncoder<Double> {

    @Override
    protected Double[] embeddedCoordinate(org.locationtech.jts.geom.Coordinate coord) {
        return new Double[] {coord.x, coord.y};
    }

    @Override
    protected void startFeature() {}

    @Override
    protected void endFeature() {}
}
