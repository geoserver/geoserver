/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import org.geoserver.security.decorators.DecoratingSimpleFeatureSource;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * This is a mock implementation
 *
 * @author ImranR
 */
public class MockRetypeFeatureTypeCallback implements RetypeFeatureTypeCallback {

    public static final String RETYPED = "RETYPED";
    public static final String RETYPED_GEOM_COLUMN = "GENERATED_POINT";

    @Override
    public FeatureType retypeFeatureType(FeatureTypeInfo featureTypeInfo, FeatureType src) {
        // only work for the test file : test/resources/org/geoserver/catalog/longlat.properties
        if (!RetypeFeatureTypeCallbackTest.LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER.equalsIgnoreCase(
                featureTypeInfo.getName())) return src;
        try {
            CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();

            builder.setName(src.getName());
            builder.setCRS(crs);

            builder.add(RETYPED_GEOM_COLUMN, Point.class);
            for (PropertyDescriptor ad : src.getDescriptors()) {
                builder.add((AttributeDescriptor) ad);
            }
            FeatureType newType = builder.buildFeatureType();
            newType.getUserData().put(RETYPED, true);
            return newType;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return src;
    }

    @Override
    public FeatureSource wrapFeatureSource(
            FeatureTypeInfo featureTypeInfo, FeatureSource featureSource) {
        // only work for the test file : test/resources/org/geoserver/catalog/longlat.properties
        if (!RetypeFeatureTypeCallbackTest.LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER.equalsIgnoreCase(
                featureTypeInfo.getName())) return featureSource;

        MockRetypeFeatureTypedFeatureSource wrapped =
                new MockRetypeFeatureTypedFeatureSource((SimpleFeatureSource) featureSource);

        return wrapped;
    }

    class MockRetypeFeatureTypedFeatureSource extends DecoratingSimpleFeatureSource {

        public MockRetypeFeatureTypedFeatureSource(SimpleFeatureSource delegate) {
            super(delegate);
        }
    }
}
