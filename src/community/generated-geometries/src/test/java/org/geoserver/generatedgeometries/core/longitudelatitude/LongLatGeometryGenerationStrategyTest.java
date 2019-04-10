/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.generatedgeometries.core.longitudelatitude;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatGeometryGenerationStrategy.GEOMETRY_ATTRIBUTE_NAME;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatGeometryGenerationStrategy.GEOMETRY_CRS;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatGeometryGenerationStrategy.LATITUDE_ATTRIBUTE_NAME;
import static org.geoserver.generatedgeometries.core.longitudelatitude.LongLatGeometryGenerationStrategy.LONGITUDE_ATTRIBUTE_NAME;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.Capabilities;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import static org.mockito.AdditionalMatchers.and;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentCaptor.forClass;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.And;
import org.opengis.filter.BinaryLogicOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;

public class LongLatGeometryGenerationStrategyTest {

    private static final String TEST_TYPE_NAME = "testTypeName";
    private static final String LATITUDE_PROPERTY_NAME = "LATITUDE_PROPERTY_NAME";
    private static final String LONGITUDE_PROPERTY_NAME = "LONGITUDE_PROPERTY_NAME";
    private static final String STRING_1_PROPERTY_NAME = "STRING_1_PROPERTY_NAME";
    private static final String STRING_2_PROPERTY_NAME = "STRING_2_PROPERTY_NAME";
    private static final String STRING_3_PROPERTY_NAME = "STRING_3_PROPERTY_NAME";
    private static final String STRING_4_PROPERTY_NAME = "STRING_4_PROPERTY_NAME";
    private static final String GEOMETRY_PROPERTY_NAME = "GEOMETRY_PROPERTY_NAME";
    private static final String TEST_FEATURE_ID = "testFeature";
    private static final String EPSG_4326 = "EPSG:4326";

    private final Logger logger = mock(Logger.class);

    private final LongLatGeometryGenerationStrategy strategy =
            new LongLatGeometryGenerationStrategy() {
                @Override
                Logger logger() {
                    return logger;
                }
            };

    private SimpleFeatureType getNonGeoType() {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(TEST_TYPE_NAME);
        builder.add(LATITUDE_PROPERTY_NAME, Double.class);
        builder.add(LONGITUDE_PROPERTY_NAME, Double.class);
        builder.add(STRING_1_PROPERTY_NAME, String.class);
        builder.add(STRING_2_PROPERTY_NAME, String.class);
        builder.add(STRING_3_PROPERTY_NAME, String.class);
        builder.add(STRING_4_PROPERTY_NAME, String.class);

        return builder.buildFeatureType();
    }

    private SimpleFeatureType getGeoType() {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(TEST_TYPE_NAME);
        builder.setCRS(DefaultGeographicCRS.WGS84);
        builder.add(GEOMETRY_PROPERTY_NAME, Point.class);
        builder.add(LATITUDE_PROPERTY_NAME, Double.class);
        builder.add(LONGITUDE_PROPERTY_NAME, Double.class);
        builder.add(STRING_1_PROPERTY_NAME, String.class);
        builder.add(STRING_2_PROPERTY_NAME, String.class);
        builder.add(STRING_3_PROPERTY_NAME, String.class);
        builder.add(STRING_4_PROPERTY_NAME, String.class);

        return builder.buildFeatureType();
    }

    private MetadataMap getMetadata() {
        MetadataMap metadata = new MetadataMap();
        metadata.put(GEOMETRY_ATTRIBUTE_NAME, GEOMETRY_PROPERTY_NAME);
        metadata.put(LATITUDE_ATTRIBUTE_NAME, LONGITUDE_PROPERTY_NAME);
        metadata.put(LONGITUDE_ATTRIBUTE_NAME, LATITUDE_PROPERTY_NAME);
        metadata.put(GEOMETRY_CRS, EPSG_4326);
        return metadata;
    }

    private SimpleFeature getNonGeoFeature(Object longitude, Object latitude) {
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(getNonGeoType());
        featureBuilder.add(longitude);
        featureBuilder.add(latitude);
        featureBuilder.add("test1");
        featureBuilder.add("test2");
        featureBuilder.add("test3");
        featureBuilder.add("test4");
        return featureBuilder.buildFeature(TEST_FEATURE_ID);
    }

    @Test
    public void testThatAfterCreateGeoTypeFromValidConfigurationHaveGeoAndAllAttributesExist() {
        // given
        FeatureTypeInfo info = mock(FeatureTypeInfo.class);
        MetadataMap metadata = getMetadata();
        when(info.getMetadata()).thenReturn(metadata);

        // when
        SimpleFeatureType geotype = strategy.defineGeometryAttributeFor(info, getNonGeoType());

        // then
        assertNotNull(geotype);
        assertNotNull(geotype.getGeometryDescriptor());
        assertNotNull(geotype.getDescriptor(LATITUDE_PROPERTY_NAME));
        assertNotNull(geotype.getDescriptor(LONGITUDE_PROPERTY_NAME));
        assertNotNull(geotype.getDescriptor(STRING_1_PROPERTY_NAME));
        assertNotNull(geotype.getDescriptor(STRING_2_PROPERTY_NAME));
        assertNotNull(geotype.getDescriptor(STRING_3_PROPERTY_NAME));
        assertNotNull(geotype.getDescriptor(STRING_4_PROPERTY_NAME));
    }

    @Test
    public void testThatAfterCreateFeatureFromValidConfigurationHaveGeoAndValidLonLat() {
        // given
        FeatureTypeInfo info = mock(FeatureTypeInfo.class);
        MetadataMap metadata = getMetadata();
        when(info.getMetadata()).thenReturn(metadata);

        SimpleFeatureType sft = getGeoType();
        int x = 1;
        int y = 2;
        SimpleFeature ft = getNonGeoFeature(x, y);

        // when
        SimpleFeature sfWithGeom = strategy.generateGeometry(info, sft, ft);

        // then
        assertNotNull(sfWithGeom);
        assertSame(sfWithGeom.getFeatureType(), sft);
        assertNotNull(sfWithGeom.getDefaultGeometry());
        assertTrue(sfWithGeom.getDefaultGeometry() instanceof Point);
        assertEquals(((Point) sfWithGeom.getDefaultGeometry()).getX(), x, 0.0);
        assertEquals(((Point) sfWithGeom.getDefaultGeometry()).getY(), y, 0.0);
    }

    @Test
    public void testThatAfterCreateFeatureFromNumericValueResultHaveGeoAndValidLonLat() {
        // given
        FeatureTypeInfo info = mock(FeatureTypeInfo.class);
        MetadataMap metadata = getMetadata();
        when(info.getMetadata()).thenReturn(metadata);

        SimpleFeatureType sft = getGeoType();
        String x = "1.4";
        double y = 2.1;
        SimpleFeature ft = getNonGeoFeature(x, y);

        // when
        SimpleFeature sfWithGeom = strategy.generateGeometry(info, sft, ft);

        // then
        assertNotNull(sfWithGeom);
        assertSame(sfWithGeom.getFeatureType(), sft);
        assertNotNull(sfWithGeom.getDefaultGeometry());
        assertTrue(sfWithGeom.getDefaultGeometry() instanceof Point);
        assertEquals(((Point) sfWithGeom.getDefaultGeometry()).getX(), 1.4, 0.0);
        assertEquals(((Point) sfWithGeom.getDefaultGeometry()).getY(), y, 0.0);
    }

    @Test
    public void testThatNotGeneratePointIfLonOrLatIsEmpty() {
        // given
        FeatureTypeInfo info = mock(FeatureTypeInfo.class);
        MetadataMap metadata = getMetadata();
        when(info.getMetadata()).thenReturn(metadata);

        SimpleFeatureType sft = getGeoType();
        int x = 1;
        SimpleFeature ft = getNonGeoFeature(x, null);

        // when
        SimpleFeature sfWithGeom = strategy.generateGeometry(info, sft, ft);

        // then
        assertNotNull(sfWithGeom);
        assertSame(ft, sfWithGeom);
        assertNull(sfWithGeom.getDefaultGeometry());

        ArgumentCaptor<IllegalArgumentException> exceptionCaptor =
                forClass(IllegalArgumentException.class);
        verify(logger)
                .log(
                        eq(Level.WARNING),
                        and(
                                ArgumentMatchers.contains(
                                        "could not generate geometry for feature [testFeature]"),
                                ArgumentMatchers.contains(sft.getName().toString())),
                        exceptionCaptor.capture());
        IllegalArgumentException exception = exceptionCaptor.getValue();
        assertThat(
                exception.getMessage(),
                allOf(
                        containsString("cannot get value of property"),
                        containsString(LONGITUDE_PROPERTY_NAME)));
    }

    @Test
    public void testThatOtherAttributesAreCopiedWhenGeoIsCreated() {
        // given
        FeatureTypeInfo info = mock(FeatureTypeInfo.class);
        MetadataMap metadata = getMetadata();
        when(info.getMetadata()).thenReturn(metadata);

        SimpleFeatureType sft = getGeoType();
        int x = 1;
        int y = 3;
        SimpleFeature ft = getNonGeoFeature(x, y);

        // when
        // exception?
        SimpleFeature sfWithGeom = strategy.generateGeometry(info, sft, ft);

        // then
        assertNotNull(sfWithGeom);
        assertSame(sfWithGeom.getFeatureType(), sft);
        assertNotNull(sfWithGeom.getDefaultGeometry());
        assertTrue(sfWithGeom.getDefaultGeometry() instanceof Point);
        assertTrue(!sfWithGeom.getProperties().isEmpty());
        assertTrue(sfWithGeom.getProperties().size() > ft.getProperties().size());
        for (Property srcProp : ft.getProperties()) {
            Property targetProp = sfWithGeom.getProperty(srcProp.getName());
            assertNotNull(targetProp);
            assertEquals(srcProp.getValue(), targetProp.getValue());
        }
    }

    @Test
    public void testThatBBoxFilterIsConvertedToBetween() {
        // given
        final FilterFactory ff = CommonFactoryFinder.getFilterFactory();

        FeatureTypeInfo info = mock(FeatureTypeInfo.class);
        MetadataMap metadata = getMetadata();
        when(info.getMetadata()).thenReturn(metadata);
        Filter filter = ff.bbox("point", -2, -2, 2, 2, "WGS84");

        // when
        Filter convFilter = strategy.convertFilter(info, filter);

        // then
        assertNotNull(convFilter);
        assertTrue(convFilter instanceof And);
    }

    @Test
    public void testThatNonBBoxFilterIsNotChanged() {
        // given
        final FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        PropertyName propertyName = ff.property("someProperty");
        Literal min = ff.literal(1);
        Literal max = ff.literal(2);

        FeatureTypeInfo info = mock(FeatureTypeInfo.class);
        MetadataMap metadata = getMetadata();
        when(info.getMetadata()).thenReturn(metadata);
        Filter srcFilter = ff.between(propertyName, min, max);
        // when

        Filter convFilter = strategy.convertFilter(info, ff.between(propertyName, min, max));

        // then
        assertEquals(convFilter, srcFilter);
    }

    @Test
    public void testThatBBoxQueryIsConvertedToBetween() throws IOException {
        // given
        final FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        Filter filter = ff.bbox("point", -2, -2, 2, 2, "WGS84");
        Query q = new Query();
        q.setFilter(filter);
        FeatureTypeInfo info = mock(FeatureTypeInfo.class);
        MetadataMap metadata = getMetadata();
        when(info.getMetadata()).thenReturn(metadata);
        when(info.getMetadata()).thenReturn(metadata);
        when(info.getFeatureType()).thenReturn(getGeoType());

        // when
        Query convertedQuery = strategy.convertQuery(info, q);

        // then
        String[] propertyNames = convertedQuery.getPropertyNames();
        assertTrue(propertyNames.length > 0);
        assertSame(propertyNames[0], LATITUDE_PROPERTY_NAME);
        assertSame(propertyNames[1], LONGITUDE_PROPERTY_NAME);
        assertTrue(convertedQuery.getFilter() instanceof And);
    }

    @Test
    public void testThatNonBBoxQueryIsIgnored() throws IOException {
        // given
        final FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        Filter filter = ff.begunBy(ff.literal(1), ff.literal(2));
        Query q = new Query();
        q.setFilter(filter);
        FeatureTypeInfo info = mock(FeatureTypeInfo.class);
        MetadataMap metadata = getMetadata();
        when(info.getMetadata()).thenReturn(metadata);
        when(info.getFeatureType()).thenReturn(getGeoType());

        // when
        Query convertedQuery = strategy.convertQuery(info, q);

        // then
        assertEquals(q.getFilter(), convertedQuery.getFilter());
    }

    @Test
    public void testBBoxFilterWithOthersConvertedToBetween() {
        // given
        final FilterFactory ff = CommonFactoryFinder.getFilterFactory();

        // to evaluate a filter for BBOX capabilities after conversion
        Capabilities bboxCapabilities = new Capabilities();
        bboxCapabilities.addType(BBOX.class);

        FeatureTypeInfo info = mock(FeatureTypeInfo.class);
        MetadataMap metadata = getMetadata();
        when(info.getMetadata()).thenReturn(metadata);
        Filter bbox1 = ff.bbox("point", -2, -2, 2, 2, "WGS84");
        Filter bbox2 = ff.bbox("point", -1, -1, 1, 1, "WGS84");
        Filter nonSpatial = ff.equals(ff.literal("123"), ff.literal(123));

        Filter filterOr = ff.or(Arrays.asList(bbox1, bbox2, nonSpatial));

        // when
        Filter convFilter = strategy.convertFilter(info, filterOr);

        // then
        assertNotNull(convFilter);
        // converted filer should not have any BBOX capabilities
        assertEquals(bboxCapabilities.supports(convFilter), false);
        Filter expected =
                ff.or(
                        Arrays.asList(
                                getTargetFilter(-2d, -2d, 2d, 2d, ff),
                                getTargetFilter(-1d, -1d, 1d, 1d, ff),
                                nonSpatial));
        assertThat(equal(convFilter, expected), is(true));

        // Now check with And

        Filter filterAnd = ff.and(Arrays.asList(bbox1, bbox2, nonSpatial));

        // when
        convFilter = strategy.convertFilter(info, filterAnd);

        // then
        assertNotNull(convFilter);
        // converted filer should not have any BBOX capabilities
        assertEquals(bboxCapabilities.supports(convFilter), false);
        expected =
                ff.and(
                        Arrays.asList(
                                getTargetFilter(-2d, -2d, 2d, 2d, ff),
                                getTargetFilter(-1d, -1d, 1d, 1d, ff),
                                nonSpatial));
        assertThat(equal(convFilter, expected), is(true));

        // check with Not
        Filter filterNot = ff.not(bbox1);

        // when
        convFilter = strategy.convertFilter(info, filterNot);

        // then
        assertNotNull(convFilter);
        // converted filer should not have any BBOX capabilities
        assertEquals(bboxCapabilities.supports(convFilter), false);

        expected = ff.not(getTargetFilter(-2d, -2d, 2d, 2d, ff));
        assertThat(equal(convFilter, expected), is(true));
    }

    @Test
    public void testNestedBBoxFilterWithOthersConvertedToBetween() {

        // given
        final FilterFactory ff = CommonFactoryFinder.getFilterFactory();

        // to evaluate a filter for BBOX capabilities after conversion
        Capabilities bboxCapabilities = new Capabilities();
        bboxCapabilities.addType(BBOX.class);

        FeatureTypeInfo info = mock(FeatureTypeInfo.class);
        MetadataMap metadata = getMetadata();
        when(info.getMetadata()).thenReturn(metadata);
        Filter bbox1 = ff.bbox("point", -2, -2, 2, 2, "WGS84");

        Filter nonSpatial = ff.equals(ff.literal("123"), ff.literal(123));
        Filter nonSpatial2 = ff.equals(ff.literal("321"), ff.literal(321));
        Filter nonSpatial3 = ff.equals(ff.literal("456"), ff.literal(456));

        Filter filterLevel0 = ff.and(bbox1, nonSpatial);
        Filter filterLevel1 = ff.and(nonSpatial2, filterLevel0);
        Filter filterLevel2 = ff.and(nonSpatial3, filterLevel1);

        // when
        Filter convFilter = strategy.convertFilter(info, filterLevel2);

        // then
        assertNotNull(convFilter);
        // converted filer should not have any BBOX capabilities
        assertEquals(bboxCapabilities.supports(convFilter), false);

        Filter expetectedfilterLevel0 = ff.and(getTargetFilter(-2d, -2d, 2d, 2d, ff), nonSpatial);
        Filter expectedLevel1 = ff.and(nonSpatial2, expetectedfilterLevel0);
        Filter expectedLevel2 = ff.and(nonSpatial3, expectedLevel1);

        assertThat(equal(convFilter, expectedLevel2), is(true));
    }

    private static Filter getTargetFilter(
            double minx, double miny, double maxx, double maxy, final FilterFactory ff) {

        return ff.and(
                ff.between(ff.property(LATITUDE_PROPERTY_NAME), ff.literal(miny), ff.literal(maxy)),
                ff.between(
                        ff.property(LONGITUDE_PROPERTY_NAME), ff.literal(minx), ff.literal(maxx)));
    }

    /**
     * Compares if two filters are equal, as a special handling for binary operators who can have a
     * list of children that will be checked for equality recursively.
     */
    private boolean equal(Filter filterA, Filter filterB) {
        if (filterA instanceof BinaryLogicOperator) {
            if (filterB instanceof BinaryLogicOperator) {
                // check that the operators are equivalent
                if (!filterA.getClass().isAssignableFrom(filterB.getClass())) {
                    // filters types are not equivalent
                    return false;
                }
                // we have to check that both have the same children
                List<Filter> filtersA = ((BinaryLogicOperator) filterA).getChildren();
                List<Filter> filtersB = ((BinaryLogicOperator) filterB).getChildren();
                return equal(filtersA, filtersB);
            }
            // different type of operators
            return false;
        }
        // normal case, let the equals method do its job
        return filterA.equals(filterB);
    }

    /**
     * Checks that the two list contain the same filters, or equivalent. This method may invoke
     * itself recursively in certain scenarios.
     */
    private boolean equal(List<Filter> filtersA, List<Filter> filtersB) {
        if (filtersA.size() != filtersB.size()) {
            // different number of filters, so for sure not the same
            return false;
        }
        // let's check that both lists have the same filters
        for (Filter filter : filtersA) {
            if (!contains(filtersB, filter)) {
                // we found a difference, their are not the same
                return false;
            }
        }
        // both list contain the same filters
        return true;
    }

    /** Checks that provided list of filters contains the provided filter. */
    private boolean contains(List<Filter> filters, Filter filter) {
        for (Filter candidate : filters) {
            if (equal(filter, candidate)) {
                // we found our filter, we are good
                return true;
            }
        }
        // the filters doesn't contains the filter
        return false;
    }
}
