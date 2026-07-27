/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.security.AccessLimits;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.DataAccessLimits;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WMSAccessLimits;
import org.geoserver.security.WMTSAccessLimits;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.parameter.Parameter;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

public class AccessLimitsKeyBuilderTest {

    static final GeometryFactory GF = new GeometryFactory();
    static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    // expected key strings are load-bearing: any change breaks existing tile caches
    static final String TRIANGLE_WKT = "MULTIPOLYGON (((0 0, 0 1, 1 1, 0 0)))";

    // limit chosen so a 500-char field value triggers truncation (JSON overhead ~14 chars -> total 514 > 300)
    static final int TRUNC_LIMIT = 300;

    AccessLimitsKeyBuilder builder;
    AccessLimitsKeyBuilder truncBuilder;
    IgnorableParameterRegistry ignorable;

    @Before
    public void setUp() {
        ignorable = new IgnorableParameterRegistry();
        builder = new AccessLimitsKeyBuilder(List.of(), ignorable);
        truncBuilder = new AccessLimitsKeyBuilder(List.of(), ignorable, TRUNC_LIMIT);
    }

    @After
    public void tearDown() {
        GeoServerExtensionsHelper.clear();
    }

    /** CoverageAccessLimits with a single string parameter: convenient for truncation tests. */
    private static CoverageAccessLimits coverageParam(String paramName, String value) {
        DefaultParameterDescriptor<String> desc = new DefaultParameterDescriptor<>(paramName, String.class, null, null);
        return new CoverageAccessLimits(
                CatalogMode.HIDE, Filter.INCLUDE, null, new GeneralParameterValue[] {new Parameter<>(desc, value)});
    }

    private static String sha256hex(String value) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    static MultiPolygon triangle() {
        Polygon tri = GF.createPolygon(
                new Coordinate[] {new Coordinate(0, 0), new Coordinate(0, 1), new Coordinate(1, 1), new Coordinate(0, 0)
                });
        return GF.createMultiPolygon(new Polygon[] {tri});
    }

    @Test
    public void testNullReturnsNull() {
        assertNull(builder.buildKey(null));
    }

    @Test
    public void testBaseAccessLimitsReturnsNull() {
        assertNull(builder.buildKey(new AccessLimits(CatalogMode.HIDE)));
    }

    @Test
    public void testIncludeFilterReturnsNull() {
        assertNull(builder.buildKey(new DataAccessLimits(CatalogMode.HIDE, Filter.INCLUDE)));
    }

    @Test
    public void testEmptyVectorLimitsReturnsNull() {
        VectorAccessLimits v = new VectorAccessLimits(CatalogMode.HIDE, null, Filter.INCLUDE, null, Filter.INCLUDE);
        assertNull(builder.buildKey(v));
    }

    @Test
    public void testDataFilter() throws Exception {
        DataAccessLimits d = new DataAccessLimits(CatalogMode.HIDE, ECQL.toFilter("population > 1000"));
        assertEquals("{\"readFilter\":\"population > 1000\"}", builder.buildKey(d));
    }

    @Test
    public void testVectorAttributes() {
        List<PropertyName> attrs = List.of(FF.property("name"), FF.property("pop"));
        VectorAccessLimits v = new VectorAccessLimits(CatalogMode.HIDE, attrs, Filter.INCLUDE, null, Filter.INCLUDE);
        assertEquals("{\"readAttributes\":\"name,pop\"}", builder.buildKey(v));
    }

    @Test
    public void testVectorAttributesSorted() {
        List<PropertyName> ab = List.of(FF.property("a"), FF.property("b"));
        List<PropertyName> ba = List.of(FF.property("b"), FF.property("a"));
        VectorAccessLimits va = new VectorAccessLimits(CatalogMode.HIDE, ab, Filter.INCLUDE, null, Filter.INCLUDE);
        VectorAccessLimits vb = new VectorAccessLimits(CatalogMode.HIDE, ba, Filter.INCLUDE, null, Filter.INCLUDE);
        assertEquals(builder.buildKey(va), builder.buildKey(vb));
    }

    @Test
    public void testVectorClip() {
        VectorAccessLimits v =
                new VectorAccessLimits(CatalogMode.HIDE, null, Filter.INCLUDE, null, Filter.INCLUDE, triangle());
        assertEquals("{\"clipVectorFilter\":\"" + TRIANGLE_WKT + "\"}", builder.buildKey(v));
    }

    @Test
    public void testVectorIntersect() {
        VectorAccessLimits v = new VectorAccessLimits(CatalogMode.HIDE, null, Filter.INCLUDE, null, Filter.INCLUDE);
        v.setIntersectVectorFilter(GF.createPoint(new Coordinate(5, 5)));
        assertEquals("{\"intersectVectorFilter\":\"POINT (5 5)\"}", builder.buildKey(v));
    }

    @Test
    public void testCoverageRasterFilter() {
        CoverageAccessLimits c = new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, triangle(), null);
        assertEquals("{\"rasterFilter\":\"" + TRIANGLE_WKT + "\"}", builder.buildKey(c));
    }

    @Test
    public void testWMSRasterFilter() {
        WMSAccessLimits w = new WMSAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, triangle(), true);
        assertEquals("{\"rasterFilter\":\"" + TRIANGLE_WKT + "\"}", builder.buildKey(w));
    }

    @Test
    public void testWMSUnrestrictedReturnsNull() {
        assertNull(builder.buildKey(new WMSAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, null, true)));
    }

    @Test
    public void testWMTSRasterFilter() throws Exception {
        WMTSAccessLimits w = new WMTSAccessLimits(CatalogMode.HIDE, ECQL.toFilter("population > 1000"), triangle());
        assertEquals(
                "{\"readFilter\":\"population > 1000\",\"rasterFilter\":\"" + TRIANGLE_WKT + "\"}",
                builder.buildKey(w));
    }

    @Test
    public void testCoverageIgnorableParam() {
        Parameter<Boolean> mt = new Parameter<>(ImageMosaicFormat.ALLOW_MULTITHREADING, true);
        CoverageAccessLimits c =
                new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, null, new GeneralParameterValue[] {mt});
        assertNull(builder.buildKey(c));
    }

    @Test
    public void testCoverageParam() {
        DefaultParameterDescriptor<String> desc = new DefaultParameterDescriptor<>("BANDS", String.class, null, null);
        Parameter<String> bands = new Parameter<>(desc, "1,2,3");
        CoverageAccessLimits c =
                new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, null, new GeneralParameterValue[] {bands});
        assertEquals("{\"BANDS\":\"1,2,3\"}", builder.buildKey(c));
    }

    @Test
    public void testCoverageUnknownParamThrows() {
        DefaultParameterDescriptor<double[]> desc =
                new DefaultParameterDescriptor<>("MyArray", double[].class, null, null);
        Parameter<double[]> bad = new Parameter<>(desc, new double[] {1.0, 2.0});
        CoverageAccessLimits c =
                new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, null, new GeneralParameterValue[] {bad});
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> builder.buildKey(c));
        assertThat(ex.getMessage(), containsString("MyArray"));
        assertThat(ex.getMessage(), containsString(IgnorableParameterRegistry.SYSTEM_PROPERTY));
    }

    @Test
    public void testLayerGroupAllUnrestricted() throws Exception {
        List<String> names = List.of("ws:a", "ws:b");
        List<AccessLimits> limits = List.of(
                new DataAccessLimits(CatalogMode.HIDE, Filter.INCLUDE),
                new DataAccessLimits(CatalogMode.HIDE, Filter.INCLUDE));
        assertNull(builder.buildLayerGroupKey(names, limits));
    }

    @Test
    public void testLayerGroupPartialRestriction() throws Exception {
        DataAccessLimits restricted = new DataAccessLimits(CatalogMode.HIDE, ECQL.toFilter("population > 0"));
        DataAccessLimits open = new DataAccessLimits(CatalogMode.HIDE, Filter.INCLUDE);
        assertEquals(
                "[{\"layer\":\"ws:a\",\"readFilter\":\"population > 0\"},{\"layer\":\"ws:b\"}]",
                builder.buildLayerGroupKey(List.of("ws:a", "ws:b"), List.of(restricted, open)));
    }

    @Test
    public void testLayerGroupSizeMismatch() {
        assertThrows(IllegalArgumentException.class, () -> builder.buildLayerGroupKey(List.of("a"), List.of()));
    }

    @Test
    public void testCustomSerializerPriority() {
        ParameterValueKeySerializer<String> upper = new ParameterValueKeySerializer<>() {
            @Override
            public Class<String> getValueType() {
                return String.class;
            }

            @Override
            public String toKey(String value) {
                return value.toUpperCase();
            }
        };
        AccessLimitsKeyBuilder b2 = new AccessLimitsKeyBuilder(List.of(upper), ignorable);
        DefaultParameterDescriptor<String> desc = new DefaultParameterDescriptor<>("TAG", String.class, null, null);
        Parameter<String> tag = new Parameter<>(desc, "hello");
        CoverageAccessLimits c =
                new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, null, new GeneralParameterValue[] {tag});
        assertEquals("{\"TAG\":\"HELLO\"}", b2.buildKey(c));
    }

    @Test
    public void testDuplicateSerializerFails() {
        ParameterValueKeySerializer<String> s1 = () -> String.class;
        ParameterValueKeySerializer<String> s2 = () -> String.class;
        assertThrows(IllegalStateException.class, () -> new AccessLimitsKeyBuilder(List.of(s1, s2), ignorable));
    }

    @Test
    public void testFilterNormalization() throws Exception {
        DataAccessLimits a = new DataAccessLimits(CatalogMode.HIDE, ECQL.toFilter("13 = population"));
        DataAccessLimits b = new DataAccessLimits(CatalogMode.HIDE, ECQL.toFilter("population = 13"));
        assertEquals(builder.buildKey(a), builder.buildKey(b));
    }

    @Test
    public void testTimeParam() {
        Date t1 = new Date(1000L);
        Date t2 = new Date(2000L);
        DefaultParameterDescriptor<List> desc = new DefaultParameterDescriptor<>("TIME", List.class, null, null);
        Parameter<List> time = new Parameter<>(desc, List.of(t1, t2));
        CoverageAccessLimits c =
                new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, null, new GeneralParameterValue[] {time});
        assertEquals("{\"TIME\":\"1970-01-01T00:00:01.000Z,1970-01-01T00:00:02.000Z\"}", builder.buildKey(c));
    }

    @Test
    public void testTimeSorted() {
        Date t1 = new Date(1000L);
        Date t2 = new Date(2000L);
        DefaultParameterDescriptor<List> desc = new DefaultParameterDescriptor<>("TIME", List.class, null, null);
        Parameter<List> fwd = new Parameter<>(desc, List.of(t1, t2));
        Parameter<List> rev = new Parameter<>(desc, List.of(t2, t1));
        CoverageAccessLimits ca =
                new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, null, new GeneralParameterValue[] {fwd});
        CoverageAccessLimits cb =
                new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, null, new GeneralParameterValue[] {rev});
        assertEquals(builder.buildKey(ca), builder.buildKey(cb));
    }

    @Test
    public void testDateRange() {
        DateRange range = new DateRange(new Date(1000L), new Date(2000L));
        DefaultParameterDescriptor<List> desc = new DefaultParameterDescriptor<>("TIME", List.class, null, null);
        Parameter<List> time = new Parameter<>(desc, List.of(range));
        CoverageAccessLimits c =
                new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, null, new GeneralParameterValue[] {time});
        assertEquals("{\"TIME\":\"1970-01-01T00:00:01.000Z/1970-01-01T00:00:02.000Z\"}", builder.buildKey(c));
    }

    @Test
    public void testNumberRange() {
        NumberRange<Double> range = new NumberRange<>(Double.class, 100.0, 200.0);
        DefaultParameterDescriptor<List> desc = new DefaultParameterDescriptor<>("ELEVATION", List.class, null, null);
        Parameter<List> elev = new Parameter<>(desc, List.of(range));
        CoverageAccessLimits c =
                new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, null, new GeneralParameterValue[] {elev});
        assertEquals("{\"ELEVATION\":\"100.0/200.0\"}", builder.buildKey(c));
    }

    @Test
    public void testCustomDimension() {
        DefaultParameterDescriptor<List> desc = new DefaultParameterDescriptor<>("WAVELENGTH", List.class, null, null);
        Parameter<List> dim = new Parameter<>(desc, List.of(1, 2, 3));
        CoverageAccessLimits c =
                new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, null, new GeneralParameterValue[] {dim});
        assertEquals("{\"WAVELENGTH\":\"1,2,3\"}", builder.buildKey(c));
    }

    @Test
    public void testNoTruncationWhenUnderLimit() {
        // 100-char value -> JSON ~114 chars, well under TRUNC_LIMIT=300
        String key = truncBuilder.buildKey(coverageParam("PARAM_A", "A".repeat(100)));
        assertThat(key, not(containsString("...too long")));
        assertThat(key.length(), lessThanOrEqualTo(TRUNC_LIMIT));
    }

    @Test
    public void testSingleFieldTruncated() throws Exception {
        // 500-char value > 300 (truncation limit)
        String longValue = "A".repeat(500);
        String key = truncBuilder.buildKey(coverageParam("PARAM_A", longValue));
        assertThat(key, containsString("...too long, sha is "));
        assertThat(key, containsString(sha256hex(longValue)));
        assertThat(key.length(), lessThanOrEqualTo(TRUNC_LIMIT));
    }

    @Test
    public void testTruncationKeepsPrefix() {
        // prefix must be at least MIN_FIELD_PREFIX chars of the original value
        String longValue = "X".repeat(500);
        String key = truncBuilder.buildKey(coverageParam("PARAM_A", longValue));
        // value in JSON is between the opening quote and "...too long"
        int valueStart = key.indexOf('"', key.indexOf(':') + 1) + 1;
        int truncMarker = key.indexOf("...too long");
        String visiblePrefix = key.substring(valueStart, truncMarker);
        assertThat(visiblePrefix.length(), greaterThanOrEqualTo(50));
        assertEquals(
                "visible prefix must match original start",
                longValue.substring(0, visiblePrefix.length()),
                visiblePrefix);
    }

    @Test
    public void testSameValueProducesSameKey() {
        // two users with the same large restriction must share cache -> identical key
        String longValue = "A".repeat(500);
        String k1 = truncBuilder.buildKey(coverageParam("PARAM_A", longValue));
        String k2 = truncBuilder.buildKey(coverageParam("PARAM_A", longValue));
        assertEquals(k1, k2);
    }

    @Test
    public void testDifferentValuesDifferentKeys() throws Exception {
        // different long values must NOT share cache
        String k1 = truncBuilder.buildKey(coverageParam("PARAM_A", "A".repeat(500)));
        String k2 = truncBuilder.buildKey(coverageParam("PARAM_A", "A".repeat(499) + "B"));
        assertNotEquals("different long values must yield different sha -> different keys", k1, k2);
    }

    @Test
    public void testLongestFieldTruncatedFirst() throws Exception {
        // PARAM_A=500 chars (long), PARAM_B=20 chars (short) - only PARAM_A needs truncating
        String longValue = "A".repeat(500);
        String shortValue = "B".repeat(20);
        DefaultParameterDescriptor<String> descA =
                new DefaultParameterDescriptor<>("PARAM_A", String.class, null, null);
        DefaultParameterDescriptor<String> descB =
                new DefaultParameterDescriptor<>("PARAM_B", String.class, null, null);
        CoverageAccessLimits c =
                new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, null, new GeneralParameterValue[] {
                    new Parameter<>(descA, longValue), new Parameter<>(descB, shortValue)
                });
        String key = truncBuilder.buildKey(c);
        // long field must be truncated with sha
        assertThat(key, containsString("...too long, sha is " + sha256hex(longValue)));
        // short field must appear verbatim
        assertThat(key, containsString("\"PARAM_B\":\"" + shortValue + "\""));
        assertThat(key.length(), lessThanOrEqualTo(TRUNC_LIMIT));
    }

    @Test
    public void testMultiFieldsTruncated() throws Exception {
        // PARAM_A=500 chars, PARAM_B=500 chars - truncating only A is not enough
        String valueA = "A".repeat(500);
        String valueB = "B".repeat(500);
        DefaultParameterDescriptor<String> descA =
                new DefaultParameterDescriptor<>("PARAM_A", String.class, null, null);
        DefaultParameterDescriptor<String> descB =
                new DefaultParameterDescriptor<>("PARAM_B", String.class, null, null);
        CoverageAccessLimits c =
                new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, null, new GeneralParameterValue[] {
                    new Parameter<>(descA, valueA), new Parameter<>(descB, valueB)
                });
        String key = truncBuilder.buildKey(c);
        assertThat(key, containsString("...too long, sha is " + sha256hex(valueA)));
        assertThat(key, containsString("...too long, sha is " + sha256hex(valueB)));
        assertThat(key.length(), lessThanOrEqualTo(TRUNC_LIMIT));
    }

    @Test
    public void testLayerGroupFieldTruncated() {
        // layer group with one entry whose rasterFilter WKT exceeds the limit
        // a 30-vertex circle polygon produces ~700 chars of WKT - well over TRUNC_LIMIT=300
        Coordinate[] coords = new Coordinate[31];
        for (int i = 0; i < 30; i++) {
            double a = 2 * Math.PI * i / 30;
            coords[i] = new Coordinate(Math.cos(a) * 100, Math.sin(a) * 100);
        }
        coords[30] = coords[0]; // close the ring
        MultiPolygon bigPoly = GF.createMultiPolygon(new Polygon[] {GF.createPolygon(coords)});
        CoverageAccessLimits restricted = new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, bigPoly, null);
        DataAccessLimits open = new DataAccessLimits(CatalogMode.HIDE, Filter.INCLUDE);
        String key = truncBuilder.buildLayerGroupKey(List.of("ws:a", "ws:b"), List.of(restricted, open));
        assertThat(key, containsString("...too long, sha is "));
        // the second layer entry must still be present and untouched
        assertThat(key, containsString("\"layer\":\"ws:b\""));
        assertThat(key.length(), lessThanOrEqualTo(TRUNC_LIMIT));
    }

    @Test
    public void testShortFieldNotTruncated() {
        // field value <= MIN_FIELD_PREFIX(50) + SUFFIX_FIXED_LENGTH(84) = 134 chars cannot be truncated to shorter form
        // use a tiny limit that can't be achieved - field must appear verbatim anyway
        AccessLimitsKeyBuilder tinyLimitBuilder = new AccessLimitsKeyBuilder(List.of(), ignorable, 10);
        String shortValue = "A".repeat(100); // 100 <= 134, skip truncation
        String key = tinyLimitBuilder.buildKey(coverageParam("PARAM_A", shortValue));
        // field untouched - no truncation marker present
        assertThat(key, not(containsString("...too long")));
        assertThat(key, containsString(shortValue));
    }

    @Test
    public void testCollectsContributedSerializers() {
        GeoServerExtensionsHelper.singleton(
                "customParamSerializer", new CustomParamSerializer(), ParameterValueKeySerializer.class);

        AccessLimitsKeyBuilder b = new AccessLimitsKeyBuilder(ignorable);
        b.afterPropertiesSet();

        CoverageAccessLimits limits =
                new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, null, new GeneralParameterValue[] {
                    new Parameter<>(CustomParamSerializer.DESCRIPTOR, new CustomParam("hello"))
                });
        assertEquals("{\"CUSTOM_PARAM\":\"hello\"}", b.buildKey(limits));
    }

    @Test
    public void testDuplicateContributedFails() {
        GeoServerExtensionsHelper.singleton("ser1", new CustomParamSerializer(), ParameterValueKeySerializer.class);
        GeoServerExtensionsHelper.singleton("ser2", new CustomParamSerializer(), ParameterValueKeySerializer.class);

        AccessLimitsKeyBuilder b = new AccessLimitsKeyBuilder(ignorable);
        assertThrows(IllegalStateException.class, b::afterPropertiesSet);
    }
}
