/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import org.geoserver.importer.ImportTask;
import org.geotools.data.DataStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

public class AttributesToPointGeometryTransform extends AbstractTransform
        implements InlineVectorTransform {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    private static final String POINT_NAME = "location";

    private final String latField;

    private final String lngField;

    private final String pointFieldName;

    private static GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    public AttributesToPointGeometryTransform(String latField, String lngField) {
        this(latField, lngField, AttributesToPointGeometryTransform.POINT_NAME);
    }

    public AttributesToPointGeometryTransform(
            String latField, String lngField, String pointFieldName) {
        this.latField = latField;
        this.lngField = lngField;
        this.pointFieldName = pointFieldName;
    }

    @Override
    public SimpleFeatureType apply(
            ImportTask task, DataStore dataStore, SimpleFeatureType featureType) throws Exception {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.init(featureType);

        int latIndex = featureType.indexOf(latField);
        int lngIndex = featureType.indexOf(lngField);
        if (latIndex < 0 || lngIndex < 0) {
            throw new Exception(
                    "FeatureType "
                            + featureType.getName()
                            + " does not have lat lng fields named '"
                            + latField
                            + "'"
                            + " and "
                            + "'"
                            + lngField
                            + "'");
        }

        GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        if (geometryDescriptor != null) {
            builder.remove(geometryDescriptor.getLocalName());
        }
        builder.remove(latField);
        builder.remove(lngField);
        builder.add(pointFieldName, Point.class);

        return builder.buildFeatureType();
    }

    @Override
    public SimpleFeature apply(
            ImportTask task, DataStore dataStore, SimpleFeature oldFeature, SimpleFeature feature)
            throws Exception {
        Object latObject = oldFeature.getAttribute(latField);
        Object lngObject = oldFeature.getAttribute(lngField);
        Double lat = asDouble(latObject);
        Double lng = asDouble(lngObject);
        if (lat == null || lng == null) {
            feature.setDefaultGeometry(null);
        } else {
            Coordinate coordinate = new Coordinate(lng, lat);
            Point point = GEOMETRY_FACTORY.createPoint(coordinate);
            feature.setAttribute(pointFieldName, point);
        }
        return feature;
    }

    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getLatField() {
        return latField;
    }

    public String getLngField() {
        return lngField;
    }
}
