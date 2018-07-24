package com.boundlessgeo.gsr.core.geometry;

import afu.org.checkerframework.checker.oigj.qual.O;
import com.boundlessgeo.gsr.Utils;
import com.vividsolutions.jts.geom.Coordinate;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import com.vividsolutions.jts.geom.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuantizedGeometryEncoder extends AbstractGeometryEncoder<Long> {

    public enum Mode {
        view
    }

    public enum OriginPosition {
        upperLeft,
        bottomRight
    }

    public QuantizedGeometryEncoder(Mode mode, OriginPosition originPosition, double tolerance, Envelope envelope, CoordinateReferenceSystem requestCRS) {
        super();
        this.mode = mode;
        this.originPosition = originPosition;
        this.tolerance = tolerance;
        this.envelope = envelope;
        this.envelopeCrs = requestCRS;
    }

    //set by constructor
    Mode mode;
    OriginPosition originPosition;
    double tolerance;
    Envelope envelope;
    CoordinateReferenceSystem envelopeCrs;

    //set when calling toRepresentation
    double[] originCoords;
    Long[] startingCoords = null;
    CoordinateReferenceSystem outCrs;


    @Override
    public com.boundlessgeo.gsr.core.geometry.Geometry toRepresentation(
            com.vividsolutions.jts.geom.Geometry geom, SpatialReference spatialReference) {
        //TODO: Init SR, starting coord, etc
        outCrs = Utils.parseSpatialReference(String.valueOf(geom.getSRID()));

        MathTransform mathTx;
        try {
            mathTx = CRS.findMathTransform(envelopeCrs, outCrs, true);
        } catch (FactoryException e) {
            throw new IllegalArgumentException(
                    "Unable to transform between input and native coordinate reference systems", e);
        }
        Envelope transformedEnvelope;
        try {
            transformedEnvelope = JTS.transform(envelope, mathTx);
        } catch (TransformException e) {
            throw new IllegalArgumentException(
                    "Error while converting envelope from input to native coordinate system", e);
        }

        switch (originPosition) {
            case upperLeft:
                originCoords = new double[]{transformedEnvelope.getMinX(), transformedEnvelope.getMaxY()};
                break;
            case bottomRight:
                originCoords = new double[]{transformedEnvelope.getMaxX(), transformedEnvelope.getMinY()};
                break;
        }

        return super.toRepresentation(geom, spatialReference);
    }

    @Override
    protected Long[] embeddedCoordinate(Coordinate coord) {
        //delta from origin
        double[] transformedCoords = new double[]{coord.x-originCoords[0], originCoords[1]-coord.y};

        //divide by tolerance and round to nearest whole number
        Long[] longCoords = new Long[]{
                Math.round(transformedCoords[0] / tolerance),
                Math.round(transformedCoords[1] / tolerance)};
        if (startingCoords != null) {
            //return the delta from last point in the feature
            Long[] deltaCoords = new Long[]{longCoords[0]-startingCoords[0],longCoords[1]-startingCoords[1]};
            startingCoords = longCoords;
            return deltaCoords;
        }
        //first point in the feature, just save and return it
        startingCoords = longCoords;
        return longCoords;
    }

    @Override
    protected Long[][] embeddedLineString(com.vividsolutions.jts.geom.LineString line) {
        List<Long[]> points = new ArrayList<>();
        startFeature();
        for (com.vividsolutions.jts.geom.Coordinate c : line.getCoordinates()) {
            Long[] point = embeddedCoordinate(c);
            //eliminate duplicated points
            if (points.size() == 0 || !Arrays.equals(new Long[]{0L,0L}, point)) {
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
}
