package org.geotools.process.vector;

import java.util.Collection;
import java.util.logging.Logger;
import org.geotools.api.feature.GeometryAttribute;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.data.graticule.gridsupport.LineFeatureBuilder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.function.StaticGeometry;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

@DescribeProcess(
        title = "Graticule Label Placement",
        description = "Transforms a set of graticule lines into label points")
public class GraticuleLabelPointProcess implements VectorProcess {

    static final Logger log = Logger.getLogger("GraticuleLabelPointProcess");

    public enum PositionEnum {
        TOPLEFT,
        BOTTOMLEFT,
        TOPRIGHT,
        BOTTOMRIGHT,
        BOTH,
        NONE;
    };

    SimpleFeatureType schema;
    SimpleFeatureBuilder builder;

    @DescribeResult(name = "labels", description = "Positions for labels")
    public SimpleFeatureCollection execute(
            @DescribeParameter(name = "grid") SimpleFeatureCollection features,
            @DescribeParameter(name = "boundingBox") ReferencedEnvelope bounds,
            @DescribeParameter(name = "positions", min = 0, max = 1) PositionEnum position)
            throws ProcessException {
        if (position == null) {
            position = PositionEnum.TOPLEFT;
        }
        log.info("Buiilding labels for " + features.size() + " lines, across " + bounds);
        schema = buildNewSchema(features.getSchema());
        builder = new SimpleFeatureBuilder(schema);
        DefaultFeatureCollection results = new DefaultFeatureCollection();

        try (SimpleFeatureIterator itr = features.features()) {
            while (itr.hasNext()) {
                SimpleFeature feature = itr.next();
                SimpleFeature f = setPoint(feature, bounds, position);
                log.finest(f.toString());
                results.add(f);
            }
        }
        log.info("created " + results.size() + " points");
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

        return sftb.buildFeatureType();
    }

    private SimpleFeature setPoint(
            SimpleFeature feature, ReferencedEnvelope bounds, PositionEnum position) {

        LineString line = (LineString) feature.getDefaultGeometry();
        boolean horizontal = (boolean) feature.getAttribute(LineFeatureBuilder.ORIENTATION);
        Point p = null;
        Geometry box = JTS.toGeometry(bounds);
        // find the location of the new point
        if (bounds.contains(line.getEnvelopeInternal())) {
            log.info("bounds contains line - choosing start or end");
            switch (position) {
                case BOTTOMLEFT:
                    p = line.getStartPoint();

                    break;
                case TOPLEFT:
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
                    if (horizontal) {
                        p = line.getEndPoint();
                    } else {
                        p = line.getStartPoint();
                    }
                    break;
            }

        } else {
            log.info("line contained bounds");
            Geometry points = box.intersection(line);
            if (points.getGeometryType() == Geometry.TYPENAME_LINESTRING && !points.isEmpty()) {
                Point[] ps = new Point[2];
                ps[0]=((LineString)points).getStartPoint();
                ps[1]=((LineString)points).getEndPoint();

                // get left most
                log.info("Got a multipoint intersection "+points);
                Point left = null;
                Point right = null;
                Point top = null;
                Point bottom = null;
                for (int i = 0; i < ps.length; i++) {
                    log.info("considering "+ps[i]);
                    Point point = ps[i];
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
                    log.info(
                            "Point: "
                                    + point.toString()
                                    + " left:"
                                    + left
                                    + " right:"
                                    + right
                                    + "\ntop:"
                                    + top
                                    + " bottom:"
                                    + bottom);
                }
                switch (position) {
                    case NONE:
                    default:
                        break;
                    case TOPLEFT:
                        if (horizontal) {
                            p = left;
                        } else {
                            p = top;
                        }
                        break;
                    case TOPRIGHT:
                        if (horizontal) {
                            p = right;
                        } else {
                            p = top;
                        }
                        break;
                    case BOTTOMLEFT:
                        if (horizontal) {
                            p = left;
                        } else {
                            p = bottom;
                        }
                        break;
                    case BOTTOMRIGHT:
                        if (horizontal) {
                            p = right;
                        } else {
                            p = bottom;
                        }
                        break;
                }
                log.info("produced " + p);
            } else {
                // no intersection
            }
        }
        SimpleFeature result = buildFeature(p, feature);
        return result;
    }

    private SimpleFeature buildFeature(Point p, SimpleFeature feature) {
        Collection<Property> atts = feature.getProperties();
        for (Property prop : atts) {
            if (prop instanceof GeometryAttribute) {
                builder.set(prop.getName(), p);
            } else {
                builder.set(prop.getName(), prop.getValue());
            }
        }
        return builder.buildFeature(null);
    }
}
