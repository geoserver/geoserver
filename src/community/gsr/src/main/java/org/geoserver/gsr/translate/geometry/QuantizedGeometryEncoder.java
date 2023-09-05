/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.translate.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geoserver.gsr.model.geometry.SpatialReference;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

/**
 * Geometry encoder used to quantize geometries.
 *
 * <p>Encodes geometries as {@link Long} arrays representing pixel coordinates on a screen, and
 * performs simplification based on the resolution.
 */
public class QuantizedGeometryEncoder extends AbstractGeometryEncoder<Long> {

    public enum Mode {
        view
    }

    public enum OriginPosition {
        upperLeft,
        bottomRight
    }

    /**
     * Initializes the geometry encoder.
     *
     * @param mode {@link Mode#view}
     * @param originPosition Integer coordinates will be returned relative to the origin position
     *     defined by this property value. Defaults to {@link OriginPosition#upperLeft}
     * @param tolerance The tolerance is the size of one pixel in the units of the output geometry,
     *     as described in the spatialReference parameter of {@link
     *     #toRepresentation(org.locationtech.jts.geom.Geometry, SpatialReference)}. This number is
     *     used to convert the coordinates to integers by building a grid with resolution matching
     *     the tolerance. Each coordinate is then snapped to one pixel on the grid. Consecutive
     *     coordinates snapped to the same pixel are removed to reduce the overall response size.
     * @param envelope An extent defining the quantization grid bounds. Its SpatialReference matches
     *     the output geometry spatial reference, as supplied to {@link
     *     #toRepresentation(org.locationtech.jts.geom.Geometry, SpatialReference)}.
     */
    public QuantizedGeometryEncoder(
            Mode mode, OriginPosition originPosition, double tolerance, Envelope envelope) {
        super();
        this.mode = mode == null ? Mode.view : mode;
        this.originPosition = originPosition == null ? OriginPosition.upperLeft : originPosition;
        this.tolerance = tolerance;
        this.envelope = envelope;
    }

    // set by constructor
    private Mode mode;
    private OriginPosition originPosition;
    private double tolerance;
    private Envelope envelope;

    // set when calling toRepresentation
    private double[] originCoords;
    private Long[] startingCoords = null;
    private CoordinateReferenceSystem outCrs;

    @Override
    public org.geoserver.gsr.model.geometry.Geometry toRepresentation(
            org.locationtech.jts.geom.Geometry geom, SpatialReference spatialReference) {
        try {
            outCrs = SpatialReferences.fromSpatialReference(spatialReference);
        } catch (FactoryException e) {
            throw new IllegalArgumentException("Unable to parse spatial reference", e);
        }
        switch (originPosition) {
            case upperLeft:
                originCoords = new double[] {envelope.getMinX(), envelope.getMaxY()};
                break;
            case bottomRight:
                originCoords = new double[] {envelope.getMaxX(), envelope.getMinY()};
                break;
        }

        return super.toRepresentation(geom, spatialReference);
    }

    @Override
    protected Long[] embeddedCoordinate(Coordinate coord) {
        // delta from origin
        double[] transformedCoords =
                new double[] {coord.x - originCoords[0], originCoords[1] - coord.y};

        // divide by tolerance and round to nearest whole number
        Long[] longCoords =
                new Long[] {
                    Math.round(transformedCoords[0] / tolerance),
                    Math.round(transformedCoords[1] / tolerance)
                };
        if (startingCoords != null) {
            // return the delta from last point in the feature
            Long[] deltaCoords =
                    new Long[] {
                        longCoords[0] - startingCoords[0], longCoords[1] - startingCoords[1]
                    };
            startingCoords = longCoords;
            return deltaCoords;
        }
        // first point in the feature, just save and return it
        startingCoords = longCoords;
        return longCoords;
    }

    @Override
    protected Long[][] embeddedLineString(org.locationtech.jts.geom.LineString line) {
        List<Long[]> points = new ArrayList<>();
        startFeature();
        for (org.locationtech.jts.geom.Coordinate c : line.getCoordinates()) {
            Long[] point = embeddedCoordinate(c);
            // eliminate duplicated points
            if (points.size() == 0 || !Arrays.equals(new Long[] {0L, 0L}, point)) {
                points.add(point);
            }
        }
        endFeature();
        return points.toArray(new Long[points.size()][]);
    }

    @Override
    protected void startFeature() {
        startingCoords = null;
    }

    @Override
    protected void endFeature() {
        startingCoords = null;
    }

    public Mode getMode() {
        return mode;
    }

    public CoordinateReferenceSystem getOutCrs() {
        return outCrs;
    }
}
