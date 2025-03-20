/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.GeometryTransformer;

public class RoundingPrecisionTransformer extends GeometryTransformer {
    private int scale;

    public RoundingPrecisionTransformer(int scale) {
        this.scale = scale;
    }

    @Override
    protected CoordinateSequence transformCoordinates(CoordinateSequence coordinates, Geometry parent) {
        if (coordinates.size() == 0) return null;

        Coordinate[] coordsReduce = roundPointwise(coordinates);
        return factory.getCoordinateSequenceFactory().create(coordsReduce);
    }

    private Coordinate[] roundPointwise(CoordinateSequence coordinates) {
        Coordinate[] coordReduce = new Coordinate[coordinates.size()];
        // copy coordinates and round them
        for (int i = 0; i < coordinates.size(); i++) {
            Coordinate coord = coordinates.getCoordinate(i).copy();
            coord.setX(roundToScale(coord.getX()));
            coord.setY(roundToScale(coord.getY()));
            coordReduce[i] = coord;
        }
        return coordReduce;
    }

    private double roundToScale(double ordinate) {
        BigDecimal bigDecimal = new BigDecimal(Double.toString(ordinate));
        bigDecimal = bigDecimal.setScale(scale, RoundingMode.HALF_UP);
        return bigDecimal.doubleValue();
    }
}
