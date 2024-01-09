/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.process.vector;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.api.feature.GeometryAttribute;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.data.graticule.gridsupport.LineFeatureBuilder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

@DescribeProcess(
        title = "Graticule Label Placement",
        description = "Transforms a set of graticule lines into label points")
public class GraticuleLabelPointProcess implements VectorProcess {

    static final Logger log = Logger.getLogger("GraticuleLabelPointProcess");
    public static final double DELTA = 0.0;

    public enum PositionEnum {
        TOPLEFT("topleft"),
        BOTTOMLEFT("bottomleft"),
        TOPRIGHT("topright"),
        BOTTOMRIGHT("bottomright"),
        BOTH("both"),
        TOP("top"),
        BOTTOM("bottom"),
        LEFT("left"),
        RIGHT("right"),
        NONE("none");

        private final String value;

        PositionEnum(String position) {
            value = position;
        }

        static Optional<PositionEnum> byName(String givenName) {
            return Arrays.stream(values())
                    .filter(it -> it.name().equalsIgnoreCase(givenName))
                    .findAny();
        }
    }

    SimpleFeatureType schema;
    SimpleFeatureBuilder builder;

    @DescribeResult(name = "labels", description = "Positions for labels")
    public SimpleFeatureCollection execute(
            @DescribeParameter(name = "grid") SimpleFeatureCollection features,
            @DescribeParameter(name = "boundingBox", min = 0) ReferencedEnvelope bounds,
            @DescribeParameter(name = "offset", min = 0, max = 1, defaultValue = "0") Double offset,
            @DescribeParameter(name = "positions", min = 0, max = 1) String placement)
            throws ProcessException, FactoryException, TransformException {
        PositionEnum position;
        if (placement == null) {
            position = PositionEnum.BOTH;
        } else {
            Optional<PositionEnum> opt = PositionEnum.byName(placement);
            if (opt.isPresent()) {
                position = opt.get();
            } else {
                position = PositionEnum.BOTH;
            }
        }
        if (offset == null) offset = 0d;
        log.fine("Buiilding labels for " + features.size() + " lines, across " + bounds);
        schema = buildNewSchema(features.getSchema());
        builder = new SimpleFeatureBuilder(schema);
        DefaultFeatureCollection results = new DefaultFeatureCollection();

        // in case of reprojection, transform the bounds to the features CRS
        PreparedGeometry clipper = null;
        if (bounds != null) {
            if (CRS.isTransformationRequired(
                    bounds.getCoordinateReferenceSystem(),
                    features.getSchema().getCoordinateReferenceSystem())) {
                bounds =
                        bounds.transform(features.getSchema().getCoordinateReferenceSystem(), true);
            }
            clipper = PreparedGeometryFactory.prepare(JTS.toGeometry(bounds));
        }

        try (SimpleFeatureIterator itr = features.features()) {
            while (itr.hasNext()) {
                SimpleFeature feature = itr.next();

                if (position.equals(PositionEnum.BOTH)) {
                    log.finest("Doing both ");
                    SimpleFeature f = setPoint(feature, clipper, PositionEnum.TOPLEFT, offset);
                    if (f != null) results.add(f);
                    f = setPoint(feature, clipper, PositionEnum.BOTTOMRIGHT, offset);
                    if (f != null) results.add(f);
                } else {
                    SimpleFeature f = setPoint(feature, clipper, position, offset);

                    if (f != null) results.add(f);
                }
            }
        }
        log.finest("created " + results.size() + " points");
        return results;
    }

    private SimpleFeatureType buildNewSchema(SimpleFeatureType schema) {
        SimpleFeatureTypeBuilder sftb = new SimpleFeatureTypeBuilder();
        sftb.setName(schema.getName().toString() + "-label");
        for (AttributeDescriptor descriptor : schema.getAttributeDescriptors()) {
            if (descriptor instanceof GeometryDescriptor) {
                sftb.add(
                        descriptor.getLocalName(),
                        Point.class,
                        ((GeometryDescriptor) descriptor).getCoordinateReferenceSystem());
            } else {
                sftb.add(descriptor);
            }
        }
        sftb.add(LineFeatureBuilder.TOP, Boolean.class);
        sftb.add(LineFeatureBuilder.LEFT, Boolean.class);
        sftb.add(LineFeatureBuilder.ANCHOR_X, Double.class);
        sftb.add(LineFeatureBuilder.ANCHOR_Y, Double.class);
        sftb.add(LineFeatureBuilder.OFFSET_X, Double.class);
        sftb.add(LineFeatureBuilder.OFFSET_Y, Double.class);
        return sftb.buildFeatureType();
    }

    private SimpleFeature setPoint(
            SimpleFeature feature, PreparedGeometry bounds, PositionEnum position, double offset) {

        LineString line = (LineString) feature.getDefaultGeometry();
        boolean horizontal = (boolean) feature.getAttribute(LineFeatureBuilder.HORIZONTAL);
        Point p = null;
        // find the location of the new point
        if (bounds == null || bounds.contains(line)) {
            log.finest("bounds contains line - choosing start or end");
            switch (position) {
                case BOTTOMLEFT:
                    p = line.getStartPoint();
                    break;
                case TOPLEFT:
                case LEFT:
                case TOP:
                    if (horizontal) {
                        p = line.getStartPoint();
                    } else {
                        p = line.getEndPoint();
                    }
                    break;
                case TOPRIGHT:
                    p = line.getEndPoint();
                    break;
                case BOTTOMRIGHT:
                case RIGHT:
                case BOTTOM:
                    if (horizontal) {
                        p = line.getEndPoint();
                    } else {
                        p = line.getStartPoint();
                    }
                    break;
            }

        } else {
            Geometry points = line.intersection(bounds.getGeometry());
            if (log.isLoggable(Level.FINE)) {
                log.finest("line contained bounds " + points + " " + feature.getAttribute("label"));
                log.finest("bounds:" + bounds);
            }
            if (points.getGeometryType() == Geometry.TYPENAME_LINESTRING && !points.isEmpty()) {
                Point[] ps = new Point[2];
                ps[0] = ((LineString) points).getStartPoint();
                ps[1] = ((LineString) points).getEndPoint();

                // get left most
                if (log.isLoggable(Level.FINEST))
                    log.finest("Got a multipoint intersection " + ps[0] + " " + ps[1]);
                Point left = null;
                Point right = null;
                Point top = null;
                Point bottom = null;
                for (Point point : ps) {
                    if (left == null) {
                        left = point;
                    } else if (point.getX() < left.getX()) {
                        left = point;
                    }
                    if (right == null) {
                        right = point;
                    } else if (point.getX() > right.getX()) {
                        right = point;
                    }
                    if (bottom == null) {
                        bottom = point;
                    } else if (point.getY() < bottom.getY()) {
                        bottom = point;
                    }
                    if (top == null) {
                        top = point;
                    } else if (point.getY() > top.getY()) {
                        top = point;
                    }
                }
                switch (position) {
                    case NONE:
                    default:
                        break;
                    case TOPLEFT:
                    case TOP:
                    case LEFT:
                        if (horizontal) {
                            p = left;
                        } else {
                            p = top;
                        }
                        break;
                    case TOPRIGHT:
                        if (horizontal) {
                            p = right;
                            p.getCoordinate().setX(p.getX() - DELTA);
                        } else {
                            p = top;
                        }
                        break;
                    case BOTTOMLEFT:
                        if (horizontal) {
                            p = left;
                        } else {
                            p = bottom;
                            p.getCoordinate().setY(p.getY() - DELTA);
                        }
                        break;
                    case BOTTOMRIGHT:
                    case BOTTOM:
                    case RIGHT:
                        if (horizontal) {
                            p = right;
                            p.getCoordinate().setX(p.getX() - 0.1);
                        } else {
                            p = bottom;
                            p.getCoordinate().setY(p.getY() - 0.1);
                        }
                        break;
                }
                if (log.isLoggable(Level.FINEST))
                    log.finest("produced " + p + " " + feature.getAttribute("label"));
            } else {
                // no intersection
                log.finest("No intersection");
            }
        }
        if (p != null) {
            if (log.isLoggable(Level.FINEST))
                log.finest("buiding point at " + p + " " + feature.getAttribute("label"));
            SimpleFeature result = buildFeature(p, feature, position, offset);
            return result;
        }
        return null;
    }

    private SimpleFeature buildFeature(
            Point p, SimpleFeature feature, PositionEnum position, double offset) {
        log.finest("building Feature at " + p + " pos:" + position);
        Collection<Property> atts = feature.getProperties();
        for (Property prop : atts) {
            if (prop instanceof GeometryAttribute) {
                builder.set(prop.getName(), p);
            } else {
                builder.set(prop.getName(), prop.getValue());
            }
        }
        boolean top = false;
        boolean left = false;
        switch (position) {
            case TOPLEFT:
            case TOPRIGHT:
            case TOP:
                top = true;
        }
        switch (position) {
            case TOPLEFT:
            case BOTTOMLEFT:
            case LEFT:
                left = true;
        }
        builder.set(LineFeatureBuilder.TOP, top);
        builder.set(LineFeatureBuilder.LEFT, left);
        double anchorX = 0.5;
        double anchorY = 0.5;
        double offsetX = 0;
        double offsetY = 0;
        if (Boolean.TRUE.equals(feature.getAttribute(LineFeatureBuilder.HORIZONTAL))) {
            anchorX = left ? 0 : 1;
            offsetX = offset * (left ? 1 : -1);
        } else {
            anchorY = top ? 1 : 0;
            offsetY = offset * (top ? -1 : 1);
        }
        builder.set(LineFeatureBuilder.ANCHOR_X, anchorX);
        builder.set(LineFeatureBuilder.ANCHOR_Y, anchorY);
        builder.set(LineFeatureBuilder.OFFSET_X, offsetX);
        builder.set(LineFeatureBuilder.OFFSET_Y, offsetY);

        SimpleFeature output = builder.buildFeature(null);
        log.finest(
                "Feature builder - left:"
                        + output.getAttribute(LineFeatureBuilder.LEFT)
                        + " top:"
                        + output.getAttribute(LineFeatureBuilder.TOP));
        return output;
    }
}
