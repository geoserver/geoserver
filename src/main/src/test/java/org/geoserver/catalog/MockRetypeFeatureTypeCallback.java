/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.retype.MockRetypedSource;
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
 * This is a mock implementation used for unit testing and demonstration on how to use this
 * interface
 */
public class MockRetypeFeatureTypeCallback implements RetypeFeatureTypeCallback {

    public static Logger LOGGER =
            Logger.getLogger(MockRetypeFeatureTypeCallback.class.getCanonicalName());

    public static final String RETYPED = "RETYPED";
    public static final String RETYPED_GEOM_COLUMN = "GENERATED_POINT";

    public static final String LONG_FIELD = "lon";
    public static final String LAT_FIELD = "lat";
    public static final int EPSG_CODE = 4326;

    @Override
    public FeatureType retypeFeatureType(FeatureTypeInfo featureTypeInfo, FeatureType src) {
        try {
            CoordinateReferenceSystem crs = CRS.decode("EPSG:" + EPSG_CODE);
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
            LOGGER.log(Level.SEVERE, "Error in MockRetypeFeatureTypeCallback:retypeFeatureType", e);
        }
        return src;
    }

    @Override
    public FeatureSource wrapFeatureSource(
            FeatureTypeInfo featureTypeInfo, FeatureSource featureSource) {
        MockRetypedSource wrapped =
                new MockRetypedSource(featureTypeInfo, (SimpleFeatureSource) featureSource);

        return wrapped;
    }
}
