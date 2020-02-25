/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.retype;

import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MockRetypeFeatureTypeCallback;
import org.geotools.data.Query;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

public class RetypeHelper {
    public static Logger LOGGER = Logger.getLogger(RetypeHelper.class.getCanonicalName());

    private final GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

    public SimpleFeature generateGeometry(
            FeatureTypeInfo info, SimpleFeatureType schema, SimpleFeature simpleFeature) {
        if (simpleFeature != null) {
            try {
                SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(schema);
                Double x =
                        Double.valueOf(
                                getAsString(
                                        simpleFeature, MockRetypeFeatureTypeCallback.LONG_FIELD));
                Double y =
                        Double.valueOf(
                                getAsString(
                                        simpleFeature, MockRetypeFeatureTypeCallback.LAT_FIELD));

                Point point = geometryFactory.createPoint(new Coordinate(x, y));
                point.setSRID(MockRetypeFeatureTypeCallback.EPSG_CODE);

                featureBuilder.add(point);
                for (Property prop : simpleFeature.getProperties()) {
                    featureBuilder.set(prop.getName(), prop.getValue());
                }
                simpleFeature = featureBuilder.buildFeature(simpleFeature.getID());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return simpleFeature;
    }

    private String getAsString(SimpleFeature simpleFeature, String name) {
        return ofNullable(simpleFeature.getProperty(name))
                .flatMap(property -> ofNullable(property.getValue()))
                .map(Object::toString)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        String.format("cannot get value of property [%s]", name)));
    }

    public SimpleFeatureType defineGeometryAttributeFor(FeatureTypeInfo info, SimpleFeatureType src)
            throws Exception {

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();

        builder.setName(src.getName());
        builder.setCRS(CRS.decode("EPSG:" + MockRetypeFeatureTypeCallback.EPSG_CODE));

        builder.add(MockRetypeFeatureTypeCallback.RETYPED_GEOM_COLUMN, Point.class);
        for (AttributeDescriptor ad : src.getAttributeDescriptors()) {
            if (!ad.getLocalName()
                    .equalsIgnoreCase(MockRetypeFeatureTypeCallback.RETYPED_GEOM_COLUMN)) {
                builder.add(ad);
            }
        }
        SimpleFeatureType simpleFeatureType = builder.buildFeatureType();

        return simpleFeatureType;
    }

    public Query convertQuery(FeatureTypeInfo info, Query query) {

        Query q = new Query(query);
        List<String> properties = new ArrayList<>();
        try {
            // no fields were sent, use all fields excluding geom field
            if (query.getPropertyNames() == null) {
                properties =
                        info.getFeatureType()
                                .getDescriptors()
                                .stream()
                                .filter(
                                        propertyDescriptor ->
                                                !propertyDescriptor
                                                        .getName()
                                                        .toString()
                                                        .equals(
                                                                MockRetypeFeatureTypeCallback
                                                                        .RETYPED_GEOM_COLUMN))
                                .map(propertyDescriptor -> propertyDescriptor.getName().toString())
                                .collect(Collectors.toList());
            } else {
                // else use the passed fields of this query
                // but make sure geom field is replaced with Long and Lat fields
                List<String> existingProperties =
                        new LinkedList<String>(Arrays.asList(query.getPropertyNames()));
                // remove geom column
                existingProperties.remove(MockRetypeFeatureTypeCallback.RETYPED_GEOM_COLUMN);
                // make sure longitude field is present
                if (!existingProperties.contains(MockRetypeFeatureTypeCallback.LONG_FIELD))
                    existingProperties.add(MockRetypeFeatureTypeCallback.LONG_FIELD);
                // make sure latitude field is present
                if (!existingProperties.contains(MockRetypeFeatureTypeCallback.LAT_FIELD))
                    existingProperties.add(MockRetypeFeatureTypeCallback.LAT_FIELD);

                properties = new ArrayList<>(existingProperties);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        q.setPropertyNames(properties);
        return q;
    }
}
