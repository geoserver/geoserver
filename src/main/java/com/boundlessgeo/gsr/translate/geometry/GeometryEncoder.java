/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.translate.geometry;

/**
 * Default implementation of {@link AbstractGeometryEncoder}. Encodes geometries as arrays of doubles.
 */
public final class GeometryEncoder extends AbstractGeometryEncoder<Double> {

    protected Double[] embeddedCoordinate(com.vividsolutions.jts.geom.Coordinate coord) {
        return new Double[] {coord.x, coord.y};
    }

    @Override
    protected void startFeature() { }

    @Override
    protected void endFeature() { }
}
