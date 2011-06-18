/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.geoserver.wps.WPSException;
import org.geoserver.wps.jts.DescribeParameter;
import org.geoserver.wps.jts.DescribeProcess;
import org.geoserver.wps.jts.DescribeResult;
import org.geotools.data.Parameter;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.measure.Measure;
import org.geotools.process.ProcessException;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.text.Text;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.distance.DistanceOp;

@DescribeProcess(title = "Snap", description = "Snap to the nearest feature")
public class SnapProcess implements GeoServerProcess {
    private static final Logger LOGGER = Logging.getLogger(SnapProcess.class);

    private GeometryFactory geometryFactory = new GeometryFactory();

    @SuppressWarnings("unchecked")
    public Map<String, Parameter<?>> getResultInfo(Map<String, Object> inputs) {
        Map<String, Parameter<?>> outputInfo = new HashMap<String, Parameter<?>>();
        outputInfo.put("result", new Parameter("result", FeatureCollection.class, Text
                .text("Result"), Text.text("The nearest feature")));
        return outputInfo;
    }

    /**
     * Process the input data set.
     * 
     * @param featureCollection
     *            the data set
     * @param crs
     *            the CRS
     * @param point
     *            the given point
     * @return the snapped to feature
     * @throws ProcessException
     *             error
     */
    @DescribeResult(name = "result", description = "The nearest feature")
    public FeatureCollection execute(
            @DescribeParameter(name = "features", description = "The feature collection to search") FeatureCollection featureCollection,
            @DescribeParameter(name = "point", description = "The point to search from") Point point,
            @DescribeParameter(name = "crs", min = 0, description = "The coordinate reference system of the collection and point (will use the collection one if not provided") CoordinateReferenceSystem crs)
            throws ProcessException {
        try {
            if (crs == null) {
                GeometryDescriptor gd = featureCollection.getSchema().getGeometryDescriptor();
                if (gd != null) {
                    crs = gd.getCoordinateReferenceSystem();
                }
            }
            if (crs == null) {
                throw new ProcessException(
                        "The CRS parameter was not provided and the feature collection does not have a default one either");
            }

            CoordinateReferenceSystem epsg4326;
            try {
                epsg4326 = CRS.decode("EPSG:4326");
            } catch (Exception e) {
                throw new WPSException("Unknown CRS code: EPSG:4326", e);
            }
            MathTransform crsTransform = CRS.findMathTransform(crs, epsg4326);

            FeatureCollection results = FeatureCollections.newCollection();
            FeatureType targetFeatureType = createTargetFeatureType(featureCollection.getSchema());
            Unit fromUnit = SI.METER;
            Unit toUnit = Unit.valueOf("mi");
            UnitConverter unitConvert = fromUnit.getConverterTo(toUnit);
            Feature nearestFeature = null;
            double nearestDistance = 9e9;
            double nearestBearing = 0;
            Iterator featureIterator = null;
            double[] nearestPoint = new double[2];
            try {
                featureIterator = featureCollection.iterator();
                while (featureIterator.hasNext()) {
                    SimpleFeature f = (SimpleFeature) featureIterator.next();
                    if (f.getDefaultGeometryProperty().getValue() == null)
                        continue;
                    DistanceOp op = new DistanceOp(point, (Geometry) f.getDefaultGeometryProperty()
                            .getValue());
                    Coordinate[] co = op.closestPoints();
                    double[] co0 = new double[] { co[0].x, co[0].y, };
                    double[] co1 = new double[] { co[1].x, co[1].y, };
                    double[] geo0 = new double[2];
                    double[] geo1 = new double[2];
                    crsTransform.transform(co0, 0, geo0, 0, 1);
                    crsTransform.transform(co1, 0, geo1, 0, 1);

                    // get distance
                    Measure m = DefaultGeographicCRS.WGS84.distance(geo0, geo1);
                    if (m.doubleValue() > nearestDistance)
                        continue;
                    nearestFeature = f;
                    nearestDistance = m.doubleValue();
                    nearestBearing = calcBearing(co);
                    nearestPoint[0] = geo1[0];
                    nearestPoint[1] = geo1[1];
                }
            } finally {
                if (featureIterator != null)
                    featureCollection.close(featureIterator);
            }
            if (nearestFeature != null) {
                nearestDistance = unitConvert.convert(nearestDistance);
                results.add(createTargetFeature(nearestFeature,
                        (SimpleFeatureType) targetFeatureType, nearestPoint, nearestDistance,
                        nearestBearing));
            }
            return results;
        } catch (WPSException e) {
            throw e;
        } catch (Throwable e) {
            LOGGER.warning("Error executing method: " + e);
            throw new WPSException("Error executing method: " + e, e);
        }
    }

    /**
     * Create the modified feature type.
     * 
     * @param sourceFeatureType
     *            the source feature type
     * @return the modified feature type
     * @throws ProcessException
     *             errror
     */
    private SimpleFeatureType createTargetFeatureType(FeatureType sourceFeatureType)
            throws ProcessException {
        try {
            SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
            typeBuilder.setName(sourceFeatureType.getName().getLocalPart());
            typeBuilder.setNamespaceURI(sourceFeatureType.getName().getNamespaceURI());
            AttributeDescriptor geomAttbType = sourceFeatureType.getGeometryDescriptor();
            for (PropertyDescriptor attbType : sourceFeatureType.getDescriptors()) {
                if (attbType.equals(geomAttbType)) {
                    typeBuilder.add(geomAttbType.getLocalName(), Point.class);
                } else {
                    typeBuilder.add((AttributeDescriptor) attbType);
                }
            }
            typeBuilder.minOccurs(1).maxOccurs(1).nillable(false).add("nearest_distance",
                    Double.class);
            typeBuilder.minOccurs(1).maxOccurs(1).nillable(false).add("nearest_bearing",
                    Double.class);
            typeBuilder
                    .setDefaultGeometry(sourceFeatureType.getGeometryDescriptor().getLocalName());
            return typeBuilder.buildFeatureType();
        } catch (Exception e) {
            LOGGER.warning("Error creating type: " + e);
            throw new WPSException("Error creating type: " + e, e);
        }
    }

    /**
     * Create the modified feature.
     * 
     * @param feature
     *            the source feature
     * @param targetFeatureType
     *            the modified feature type
     * @param nearestDistance
     *            the snap distance
     * @param nearestBearing
     *            the snap bearing
     * @return the modified feature
     * @throws ProcessException
     *             error
     */
    private SimpleFeature createTargetFeature(Feature feature, SimpleFeatureType targetFeatureType,
            double[] nearestPoint, Double nearestDistance, Double nearestBearing)
            throws ProcessException {
        try {
            AttributeDescriptor geomAttbType = targetFeatureType.getGeometryDescriptor();
            AttributeDescriptor distanceAttbType = targetFeatureType
                    .getDescriptor("nearest_distance");
            AttributeDescriptor bearingAttbType = targetFeatureType
                    .getDescriptor("nearest_bearing");
            Object[] attributes = new Object[targetFeatureType.getAttributeCount()];
            for (int i = 0; i < attributes.length; i++) {
                AttributeDescriptor attbType = targetFeatureType.getAttributeDescriptors().get(i);
                if (attbType.equals(geomAttbType)) {
                    attributes[i] = geometryFactory.createPoint(new Coordinate(nearestPoint[0],
                            nearestPoint[1]));
                } else if (attbType.equals(distanceAttbType)) {
                    attributes[i] = nearestDistance;
                } else if (attbType.equals(bearingAttbType)) {
                    attributes[i] = nearestBearing;
                } else {
                    attributes[i] = feature.getProperty(attbType.getName()).getValue();
                }
            }
            return SimpleFeatureBuilder.build(targetFeatureType, attributes, feature
                    .getIdentifier().getID());
        } catch (Exception e) {
            LOGGER.warning("Error creating feature: " + e);
            throw new WPSException("Error creating feature: " + e, e);
        }
    }

    /**
     * Calculate the bearing between two points.
     * 
     * @param coords
     *            the points
     * @return the bearing
     */
    private double calcBearing(Coordinate[] coords) {
        double y = Math.sin(coords[1].x - coords[0].x) * Math.cos(coords[1].y);
        double x = Math.cos(coords[0].y) * Math.sin(coords[1].y) - Math.sin(coords[0].y)
                * Math.cos(coords[1].y) * Math.cos(coords[1].x - coords[0].x);
        double brng = ((Math.atan2(y, x) * 180.0 / Math.PI) + 360) % 360;
        return brng;
    }

}
