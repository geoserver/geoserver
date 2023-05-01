/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static java.util.Optional.ofNullable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.security.decorators.DecoratingSimpleFeatureSource;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.vfny.geoserver.global.GeoServerFeatureSource;

/**
 * This test asserts the an implementation of RetypeFeatureTypeCallback is integrated properly with
 * GeoServerFeatureSource wrapper. The test asserts that when a feature type is retyped through
 * RetypeFeatureTypeCallback implementation, it does not break or override the functionality
 * provided by GeoServerFeatureSource wrapper.
 */
public class RetypeFeatureTypeCallbackTest extends GeoServerSystemTestSupport {

    public static final String LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER_FILE = "longlat.properties";
    public static final QName LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME =
            new QName(MockData.DEFAULT_PREFIX, "longlat", MockData.DEFAULT_PREFIX);

    public static final QName LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME_REPROJECTED =
            new QName(MockData.DEFAULT_PREFIX, "longlat_reprojected", MockData.DEFAULT_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // registering singleton for this test only
        TestRetypeFeatureTypeCallback o2 = new TestRetypeFeatureTypeCallback();
        GeoServerExtensionsHelper.singleton(
                "retypeFeatureTypeCallbackTest", o2, RetypeFeatureTypeCallback.class);

        super.onSetUp(testData);
        setUpNonGeometryLayer(testData);
        setReprojectedUpNonGeometryLayer(testData);
    }

    private void setUpNonGeometryLayer(SystemTestData testData) throws IOException {
        // Loading a vector layer with location given as latitude and longitude and no geometry
        Map<LayerProperty, Object> props = new HashMap<>();
        testData.addVectorLayer(
                LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME,
                props,
                LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER_FILE,
                getClass(),
                getCatalog());
    }

    private void setReprojectedUpNonGeometryLayer(SystemTestData testData) throws IOException {
        // Loading a vector layer with location given as latitude and longitude and no geometry
        Map<LayerProperty, Object> props = new HashMap<>();
        // declaring a different CRS to test re-projection
        props.put(LayerProperty.PROJECTION_POLICY, ProjectionPolicy.REPROJECT_TO_DECLARED);
        props.put(LayerProperty.SRS, 900913);
        testData.addVectorLayer(
                LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME_REPROJECTED,
                props,
                LONG_LAT_NO_GEOM_ON_THE_FLY_LAYER_FILE,
                getClass(),
                getCatalog());
    }

    @Test
    public void testGeometryCreation() throws Exception {
        ResourcePool pool = ResourcePool.create(getCatalog());
        FeatureTypeInfo info =
                getCatalog()
                        .getFeatureTypeByName(
                                LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME.getNamespaceURI(),
                                LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME.getLocalPart());
        FeatureType ft1 = pool.getFeatureType(info);

        // assert that feature type is returned with a point geometry
        assertTrue(ft1.getUserData().containsKey(TestRetypeFeatureTypeCallback.RETYPED));
        assertEquals(ft1.getGeometryDescriptor().getType().getBinding(), Point.class);

        FeatureSource retyped = pool.getFeatureSource(info, null);
        // assert FeatureSource is nicely wrapped inside Geoserver wrapper
        assertTrue(retyped instanceof GeoServerFeatureSource);
        // assert FeatureSource has Geometry type set to Point
        assertEquals(
                retyped.getSchema().getGeometryDescriptor().getType().getBinding(), Point.class);
        // Finally assert that each features has a valid geometry
        try (FeatureIterator iterator = retyped.getFeatures().features()) {
            while (iterator.hasNext()) {
                Feature feature = iterator.next();
                assertTrue(feature.getDefaultGeometryProperty().getValue() instanceof Point);
            }
        }
    }

    @Test
    public void testReProjection() throws Exception {
        ResourcePool pool = ResourcePool.create(getCatalog());
        FeatureTypeInfo info =
                getCatalog()
                        .getFeatureTypeByName(
                                LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME_REPROJECTED.getNamespaceURI(),
                                LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME_REPROJECTED.getLocalPart());

        FeatureSource retyped = pool.getFeatureSource(info, null);
        CoordinateReferenceSystem reprojected = CRS.decode("EPSG:" + 900913);

        // asserting re-projection occurred to the declared CRS
        try (FeatureIterator iterator = retyped.getFeatures().features()) {
            while (iterator.hasNext()) {
                Feature feature = iterator.next();
                // check if resulting feature are in correct CRS
                assertFalse(
                        CRS.isTransformationRequired(
                                reprojected, feature.getType().getCoordinateReferenceSystem()));
            }
        }
    }

    @Test
    public void testCQLFilter() throws Exception {
        ResourcePool pool = ResourcePool.create(getCatalog());
        FeatureTypeInfo info =
                getCatalog()
                        .getFeatureTypeByName(
                                LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME.getNamespaceURI(),
                                LONG_LAT_NO_GEOM_ON_THE_FLY_QNAME.getLocalPart());
        pool.getFeatureType(info);
        // setting a feature type level CQL to allow only ONE feature
        info.setCqlFilter("data = 'd1'");
        getCatalog().save(info);

        FeatureSource retyped = pool.getFeatureSource(info, null);

        try {
            int count = retyped.getFeatures().size();
            assertEquals(1, count);
        } finally {
            // reset
            info.setCqlFilter(null);
            getCatalog().save(info);
        }
    }

    // sample classes providing required implementations in context of the unit test

    /* The main implementation for extension point  */
    public static class TestRetypeFeatureTypeCallback implements RetypeFeatureTypeCallback {

        public static Logger LOGGER =
                Logger.getLogger(TestRetypeFeatureTypeCallback.class.getCanonicalName());

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
                LOGGER.log(
                        Level.SEVERE,
                        "Error in TestRetypeFeatureTypeCallback:retypeFeatureType",
                        e);
            }
            return src;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends FeatureType, U extends Feature> FeatureSource<T, U> wrapFeatureSource(
                FeatureTypeInfo featureTypeInfo, FeatureSource<T, U> featureSource) {
            TestRetypedSource wrapped =
                    new TestRetypedSource(featureTypeInfo, (SimpleFeatureSource) featureSource);

            return (FeatureSource<T, U>) wrapped;
        }
    }

    public static class TestRetypedSource extends DecoratingSimpleFeatureSource {

        private final FeatureTypeInfo featureTypeInfo;
        SimpleFeatureSource delegate;

        RetypeHelper converter = new RetypeHelper();

        public TestRetypedSource(FeatureTypeInfo featureTypeInfo, SimpleFeatureSource delegate) {
            super(delegate);
            this.featureTypeInfo = featureTypeInfo;

            this.delegate = delegate;
        }

        @Override
        public SimpleFeatureType getSchema() {

            SimpleFeatureType src = super.getSchema();
            try {
                return converter.defineGeometryAttributeFor(featureTypeInfo, src);
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Error in TestRetypedSource.getSchema while adding Geometry attribute to  schema:"
                                + src.getName(),
                        e);
            }

            return src;
        }

        @Override
        public SimpleFeatureCollection getFeatures() throws IOException {

            SimpleFeatureCollection features = getFeatures(Query.ALL);
            return new TestRetypedFeatureCollection(features, featureTypeInfo, getSchema());
        }

        @Override
        public SimpleFeatureCollection getFeatures(Filter srcFilter) throws IOException {
            Query query = new Query(Query.ALL);
            query.setFilter(srcFilter);
            Query newQuery = converter.convertQuery(featureTypeInfo, query);
            SimpleFeatureCollection features = super.getFeatures(newQuery);
            return new TestRetypedFeatureCollection(features, featureTypeInfo, getSchema());
        }

        @Override
        public SimpleFeatureCollection getFeatures(Query srcQuery) throws IOException {
            Query newQuery = converter.convertQuery(featureTypeInfo, srcQuery);
            SimpleFeatureCollection features = super.getFeatures(newQuery);
            return new TestRetypedFeatureCollection(features, featureTypeInfo, getSchema());
        }

        @Override
        public int getCount(Query srcQuery) throws IOException {
            Query newQuery = converter.convertQuery(featureTypeInfo, srcQuery);
            return super.getCount(newQuery);
        }
    }

    public static class TestRetypedFeatureCollection extends DecoratingSimpleFeatureCollection {

        private final FeatureTypeInfo featureTypeInfo;
        private final SimpleFeatureType schema;

        TestRetypedFeatureCollection(
                SimpleFeatureCollection delegate,
                FeatureTypeInfo featureTypeInfo,
                SimpleFeatureType schema) {
            super(delegate);

            this.featureTypeInfo = featureTypeInfo;
            this.schema = schema;
        }

        @Override
        public SimpleFeatureType getSchema() {
            return this.schema;
        }

        @Override
        public SimpleFeatureIterator features() {
            return new GeometryGenerationCollectionIterator(super.features());
        }

        private class GeometryGenerationCollectionIterator implements SimpleFeatureIterator {

            private final SimpleFeatureIterator delegate;

            RetypeHelper converter = new RetypeHelper();

            private GeometryGenerationCollectionIterator(SimpleFeatureIterator delegate) {
                this.delegate = delegate;
            }

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public SimpleFeature next() throws NoSuchElementException {
                SimpleFeature feature = delegate.next();
                return converter.generateGeometry(featureTypeInfo, schema, feature);
            }

            @Override
            public void close() {
                delegate.close();
            }
        }
    }

    public static class RetypeHelper {
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
                                            simpleFeature,
                                            TestRetypeFeatureTypeCallback.LONG_FIELD));
                    Double y =
                            Double.valueOf(
                                    getAsString(
                                            simpleFeature,
                                            TestRetypeFeatureTypeCallback.LAT_FIELD));

                    Point point = geometryFactory.createPoint(new Coordinate(x, y));
                    point.setSRID(TestRetypeFeatureTypeCallback.EPSG_CODE);

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
                                            String.format(
                                                    "cannot get value of property [%s]", name)));
        }

        public SimpleFeatureType defineGeometryAttributeFor(
                FeatureTypeInfo info, SimpleFeatureType src) throws Exception {

            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();

            builder.setName(src.getName());
            builder.setCRS(CRS.decode("EPSG:" + TestRetypeFeatureTypeCallback.EPSG_CODE));

            builder.add(TestRetypeFeatureTypeCallback.RETYPED_GEOM_COLUMN, Point.class);
            for (AttributeDescriptor ad : src.getAttributeDescriptors()) {
                if (!ad.getLocalName()
                        .equalsIgnoreCase(TestRetypeFeatureTypeCallback.RETYPED_GEOM_COLUMN)) {
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
                            info.getFeatureType().getDescriptors().stream()
                                    .filter(
                                            propertyDescriptor ->
                                                    !propertyDescriptor
                                                            .getName()
                                                            .toString()
                                                            .equals(
                                                                    TestRetypeFeatureTypeCallback
                                                                            .RETYPED_GEOM_COLUMN))
                                    .map(
                                            propertyDescriptor ->
                                                    propertyDescriptor.getName().toString())
                                    .collect(Collectors.toList());
                } else {
                    // else use the passed fields of this query
                    // but make sure geom field is replaced with Long and Lat fields
                    List<String> existingProperties =
                            new LinkedList<>(Arrays.asList(query.getPropertyNames()));
                    // remove geom column
                    existingProperties.remove(TestRetypeFeatureTypeCallback.RETYPED_GEOM_COLUMN);
                    // make sure longitude field is present
                    if (!existingProperties.contains(TestRetypeFeatureTypeCallback.LONG_FIELD))
                        existingProperties.add(TestRetypeFeatureTypeCallback.LONG_FIELD);
                    // make sure latitude field is present
                    if (!existingProperties.contains(TestRetypeFeatureTypeCallback.LAT_FIELD))
                        existingProperties.add(TestRetypeFeatureTypeCallback.LAT_FIELD);

                    properties = new ArrayList<>(existingProperties);
                }

            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Error occurred when converting the query for re-typed tyoe "
                                + info.getName(),
                        e);
            }
            q.setPropertyNames(properties);
            return q;
        }
    }
}
