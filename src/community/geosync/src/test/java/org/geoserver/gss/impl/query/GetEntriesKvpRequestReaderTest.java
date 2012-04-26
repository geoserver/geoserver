package org.geoserver.gss.impl.query;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.geoserver.gss.service.FeedType;
import org.geoserver.gss.service.GetEntries;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.platform.ServiceException;
import org.geoserver.test.ows.KvpRequestReaderTestSupport;
import org.geotools.feature.type.DateUtil;
import org.geotools.referencing.CRS;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Test suite for {@link GetEntriesKvpRequestReader}.
 * <p>
 * Asserts the correct parsing of the GetEntries operation as per <i>Table 11 – GetEntries KVP
 * request encoding</i>
 * 
 * @author groldan
 * 
 */
@SuppressWarnings("unchecked")
public class GetEntriesKvpRequestReaderTest extends KvpRequestReaderTestSupport {

    private GetEntriesKvpRequestReader reader;

    @SuppressWarnings("rawtypes")
    private Map rawKvp;

    @SuppressWarnings("rawtypes")
    private Map kvp;

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetEntriesKvpRequestReaderTest());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void setUpInternal() throws Exception {
        reader = new GetEntriesKvpRequestReader();
        rawKvp = new HashMap();
        rawKvp.put("service", "GSS");
        rawKvp.put("version", "1.0.0");
        rawKvp.put("request", "GetEntries");
        rawKvp.put("feed", "REPLICATIONFEED");
        rawKvp = new CaseInsensitiveMap(rawKvp);

        kvp = parseKvp(rawKvp);
    }

    public void testMinimal() throws Exception {
        GetEntries request = reader.createRequest();
        GetEntries parsed = reader.read(request, kvp, rawKvp);
        assertNotNull(parsed);

        assertEquals("GSS", parsed.getService());
        assertEquals("1.0.0", parsed.getVersion());
        assertEquals("GetEntries", parsed.getRequest());

        assertEquals(FeedType.REPLICATIONFEED, parsed.getFeed());

        assertNull(parsed.getHandle());
        assertNotNull(parsed.getOutputFormat());
        assertTrue(Filter.INCLUDE.equals(parsed.getFilter()));
        assertEquals(Long.valueOf(25), parsed.getMaxEntries());
        assertNull(parsed.getSearchTerms());
        assertNull(parsed.getStartPosition());
    }

    public void testNoFeedParameter() throws Exception {
        GetEntries request = reader.createRequest();
        kvp.remove("feed");
        rawKvp.remove("feed");
        try {
            reader.read(request, kvp, rawKvp);
            fail("Expected ServiceException with MissingParameterValue code");
        } catch (ServiceException e) {
            assertEquals("MissingParameterValue", e.getCode());
            assertEquals("FEED", e.getLocator());
        }
    }

    public void testInvalidFeedParameter() throws Exception {
        GetEntries request = reader.createRequest();
        kvp.remove("feed");
        rawKvp.put("feed", "NonExistentFeed");
        try {
            reader.read(request, kvp, rawKvp);
            fail("Expected ServiceException with InvalidParameterValue code");
        } catch (ServiceException e) {
            assertEquals("InvalidParameterValue", e.getCode());
            assertEquals("FEED", e.getLocator());
        }
    }

    public void testFullNoFilter() throws Exception {

        rawKvp.put("handle", "test handle");
        rawKvp.put("OuTputFormaT", "text/xml");
        rawKvp.put("MaxEntries", "10");
        rawKvp.put("SEARCHTERMS", "some,comma,separated,seach,terms");
        rawKvp.put("startPosition", "7");

        kvp = parseKvp(rawKvp);

        GetEntries request = reader.createRequest();
        GetEntries parsed = reader.read(request, kvp, rawKvp);

        assertEquals("test handle", parsed.getHandle());
        assertEquals("text/xml", parsed.getOutputFormat());
        assertEquals(Long.valueOf(10), parsed.getMaxEntries());
        List<String> searchTerms = Arrays.asList("some", "comma", "separated", "seach", "terms");
        assertEquals(searchTerms, parsed.getSearchTerms());
        assertEquals(Long.valueOf(7), parsed.getStartPosition());
    }

    /**
     * ENTRYID: Identifier of an entry to retrieve
     */
    public void testIdentityPredicate() throws Exception {
        rawKvp.put("entryId", "fake-entry-id");
        kvp = parseKvp(rawKvp);

        GetEntries request = reader.createRequest();
        GetEntries parsed = reader.read(request, kvp, rawKvp);
        assertTrue(parsed.getFilter() instanceof Id);
    }

    /**
     * This filter has no {@code xmlns:fes="http://www.opengis.net/ogc"} and the parser fails
     * silently, potentially returning all entries instead of filtering them, so lets make sure we
     * catch it up
     * 
     * @throws Exception
     */
    public void testInvalidFitler() throws Exception {
        String filter = "<fes:Filter>" + //
                "  <fes:PropertyIsEqualTo>" + //
                "    <fes:PropertyName>foo</fes:PropertyName>" + //
                "    <fes:Literal>1</fes:Literal>" + //
                "  </fes:PropertyIsEqualTo>" + //
                "</fes:Filter>";

        rawKvp.put("filter", filter);
        kvp = parseKvp(rawKvp);

        GetEntries request = reader.createRequest();
        try {
            reader.read(request, kvp, rawKvp);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals("InvalidParameterValue", e.getCode());
            assertEquals("FILTER", e.getLocator());
        }
    }

    public void testFitler() throws Exception {
        String filter = "<fes:Filter xmlns:fes=\"http://www.opengis.net/ogc\">" + //
                "  <fes:PropertyIsEqualTo>" + //
                "    <fes:PropertyName>foo</fes:PropertyName>" + //
                "    <fes:Literal>1</fes:Literal>" + //
                "  </fes:PropertyIsEqualTo>" + //
                "</fes:Filter>";

        rawKvp.put("filter", filter);
        kvp = parseKvp(rawKvp);

        GetEntries request = reader.createRequest();
        GetEntries parsed = reader.read(request, kvp, rawKvp);
        assertNotNull(parsed.getFilter());
    }

    /**
     * Test case for the BBOX spatial parameter.
     * <p>
     * Spatial parameters could be: BBOX, GEOM, SPATIALOP, CRS
     * </p>
     */
    @SuppressWarnings("deprecation")
    public void testSpatialParameterBBOX_NoCRS() throws Exception {
        rawKvp.put("BBOX", "0,1,2,3");
        kvp = parseKvp(rawKvp);

        GetEntries request = reader.createRequest();
        GetEntries parsed = reader.read(request, kvp, rawKvp);
        Filter filter = parsed.getFilter();
        assertNotNull(filter);
        assertTrue(filter instanceof BBOX);

        BBOX bbox = (BBOX) filter;
        assertEquals("urn:ogc:def:crs:EPSG::4326", bbox.getSRS());
    }

    @SuppressWarnings("deprecation")
    public void testSpatialParameterBBOX_CRS() throws Exception {
        rawKvp.put("BBOX", "0,1,2,3,urn:ogc:def:crs:EPSG::23030");
        kvp = parseKvp(rawKvp);

        GetEntries request = reader.createRequest();
        GetEntries parsed = reader.read(request, kvp, rawKvp);
        Filter filter = parsed.getFilter();
        assertNotNull(filter);
        assertTrue(filter instanceof BBOX);

        BBOX bbox = (BBOX) filter;
        // Note perhaps the filter factory shouldn't change the format of the SRS name?
        assertEquals("EPSG:23030", bbox.getSRS());
    }

    @SuppressWarnings("deprecation")
    public void testSpatialParameterBBOXWithNoCRS_ButWithCRSParam() throws Exception {
        rawKvp.put("BBOX", "0,1,2,3");
        rawKvp.put("CRS", "urn:ogc:def:crs:EPSG::26986");
        kvp = parseKvp(rawKvp);

        GetEntries request = reader.createRequest();
        GetEntries parsed = reader.read(request, kvp, rawKvp);
        Filter filter = parsed.getFilter();
        assertNotNull(filter);
        assertTrue(filter instanceof BBOX);

        BBOX bbox = (BBOX) filter;
        assertEquals("urn:ogc:def:crs:EPSG::26986", bbox.getSRS());
    }

    /**
     * SpatialOP: one of Equals, Disjoint, Touches, Within, Overlaps, Crosses, Intersects , Contains
     * 
     * @throws Exception
     */
    public void testSpatialParameterBBOX_SPATIALOP() throws Exception {
        // Intersects is the only one that should not be translated as that's the default behaviour
        // of a BBOX filter anyways
        assertBBOX_SPATIALOP("Intersects", BBOX.class);

        assertBBOX_SPATIALOP("Equals", Equals.class);
        assertBBOX_SPATIALOP("Disjoint", Disjoint.class);
        assertBBOX_SPATIALOP("Touches", Touches.class);
        assertBBOX_SPATIALOP("Within", Within.class);
        assertBBOX_SPATIALOP("Overlaps", Overlaps.class);
        assertBBOX_SPATIALOP("Crosses", Crosses.class);
        assertBBOX_SPATIALOP("Contains", Contains.class);
    }

    private void assertBBOX_SPATIALOP(final String spatialOp, final Class<? extends Filter> expected)
            throws Exception {
        rawKvp.put("BBOX", "0,1,2,3");
        rawKvp.put("spatialop", spatialOp);
        kvp = parseKvp(rawKvp);

        GetEntries request = reader.createRequest();
        GetEntries parsed = reader.read(request, kvp, rawKvp);
        Filter filter = parsed.getFilter();
        assertNotNull(filter);
        String message = "Expected " + expected.getName() + " but got "
                + filter.getClass().getName();
        assertTrue(message, expected.isAssignableFrom(filter.getClass()));
    }

    public void testSpatialParameterBBOX_And_GEOM_MutuallyExclusive() throws Exception {
        rawKvp.put("BBOX", "0,1,2,3");
        rawKvp.put("GEOM", "POINT(10 10)");
        kvp = parseKvp(rawKvp);

        GetEntries request = reader.createRequest();
        try {
            reader.read(request, kvp, rawKvp);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals("InvalidParameterValue", e.getCode());
            assertEquals("GEOM/BBOX", e.getLocator());
            assertTrue(e.getMessage().contains("mutually exclusive"));
        }
    }

    /**
     * Test case for the GEOM spatial parameter.
     * <p>
     * Spatial parameters could be: BBOX, GEOM, SPATIALOP, CRS
     * </p>
     */
    public void testSpatialParameterGEOM_DefaultCRS() throws Exception {
        rawKvp.put("GEOM", "POINT(10 10)");
        kvp = parseKvp(rawKvp);

        GetEntries parsed = reader.read(reader.createRequest(), kvp, rawKvp);
        Filter filter = parsed.getFilter();
        assertNotNull(filter);
        assertTrue(filter instanceof Intersects);
        Geometry geom = (Geometry) ((Intersects) filter).getExpression2().evaluate(null);
        assertNotNull(geom);
        assertTrue(geom.getUserData() instanceof CoordinateReferenceSystem);
        CoordinateReferenceSystem crs = (CoordinateReferenceSystem) geom.getUserData();
        assertEquals(CRS.decode("urn:ogc:def:crs:EPSG::4326"), crs);
    }

    public void testSpatialParameterGEOM_CRS() throws Exception {
        final String srs = "urn:ogc:def:crs:EPSG::26986";
        rawKvp.put("GEOM", "POINT(10 10)");
        rawKvp.put("CRS", srs);
        kvp = parseKvp(rawKvp);

        GetEntries parsed = reader.read(reader.createRequest(), kvp, rawKvp);
        Filter filter = parsed.getFilter();
        assertNotNull(filter);
        assertTrue(filter instanceof Intersects);
        Geometry geom = (Geometry) ((Intersects) filter).getExpression2().evaluate(null);
        assertNotNull(geom);
        assertTrue(geom.getUserData() instanceof CoordinateReferenceSystem);
        CoordinateReferenceSystem crs = (CoordinateReferenceSystem) geom.getUserData();
        assertEquals(CRS.decode(srs), crs);
    }

    public void testSpatialParameterGEOM_SPATIALOP() throws Exception {
        assertGEOM_SPATIALOP("Intersects", Intersects.class);
        assertGEOM_SPATIALOP("Equals", Equals.class);
        assertGEOM_SPATIALOP("Disjoint", Disjoint.class);
        assertGEOM_SPATIALOP("Touches", Touches.class);
        assertGEOM_SPATIALOP("Within", Within.class);
        assertGEOM_SPATIALOP("Overlaps", Overlaps.class);
        assertGEOM_SPATIALOP("Crosses", Crosses.class);
        assertGEOM_SPATIALOP("Contains", Contains.class);
    }

    public void testSpatialParameterInvalidSpatialOp() throws Exception {
        rawKvp.put("GEOM", "POINT(10 10)");
        rawKvp.put("spatialOp", "TouchesOrCrosesOrDisjoint");
        kvp = parseKvp(rawKvp);

        try {
            reader.read(reader.createRequest(), kvp, rawKvp);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals("InvalidParameterValue", e.getCode());
            assertEquals("SPATIALOP", e.getLocator());
        }
    }

    public void testSpatialParameterInvalidCRS() throws Exception {
        rawKvp.put("GEOM", "POINT(10 10)");
        rawKvp.put("CRS", "NotACRS");
        kvp = parseKvp(rawKvp);

        try {
            reader.read(reader.createRequest(), kvp, rawKvp);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals("InvalidParameterValue", e.getCode());
            assertEquals("CRS", e.getLocator());
        }
    }

    private void assertGEOM_SPATIALOP(final String spatialOp, final Class<? extends Filter> expected)
            throws Exception {
        rawKvp.put("GEOM", "POINT(5 5)");
        rawKvp.put("spatialop", spatialOp);
        kvp = parseKvp(rawKvp);

        GetEntries request = reader.createRequest();
        GetEntries parsed = reader.read(request, kvp, rawKvp);
        Filter filter = parsed.getFilter();
        assertNotNull(filter);
        String message = "Expected " + expected.getName() + " but got "
                + filter.getClass().getName();
        assertTrue(message, expected.isAssignableFrom(filter.getClass()));
    }

    /**
     * Spatial Parameters and Generalized Predicate are mutually exclusive, as of OGC 10-069r2,
     * Table 11, page 63.
     * 
     * @throws Exception
     */
    public void testSpatialParameterAndFilterAreMutuallyExclusive() throws Exception {
        String filter = "<fes:Filter xmlns:fes=\"http://www.opengis.net/ogc\">" + //
                "  <fes:PropertyIsEqualTo>" + //
                "    <fes:PropertyName>foo</fes:PropertyName>" + //
                "    <fes:Literal>1</fes:Literal>" + //
                "  </fes:PropertyIsEqualTo>" + //
                "</fes:Filter>";

        rawKvp.put("filter", filter);
        rawKvp.put("BBOX", "0,1,2,3");
        kvp = parseKvp(rawKvp);

        GetEntries request = reader.createRequest();
        try {
            reader.read(request, kvp, rawKvp);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals("InvalidParameterValue", e.getCode());
            assertEquals("FILTER", e.getLocator());
            assertTrue(e.getMessage().contains("mutually exclusive"));
        }
    }

    public void testTemporalPredicate_TemporalOpAndNoPeriod() throws Exception {
        rawKvp.put("temporalOp", "Begins");
        kvp = parseKvp(rawKvp);

        try {
            reader.read(reader.createRequest(), kvp, rawKvp);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals("MissingParameterValue", e.getCode());
            assertEquals("STARTTIME", e.getLocator());
        }
    }

    public void testTemporalPredicate_OnlyEndTimeSpecified() throws Exception {
        rawKvp.put("temporalOp", "Begins");
        rawKvp.put("endTime", DateUtil.serializeDateTime(1000));
        kvp = parseKvp(rawKvp);

        try {
            reader.read(reader.createRequest(), kvp, rawKvp);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals("MissingParameterValue", e.getCode());
            assertEquals("STARTTIME", e.getLocator());
        }
    }

    /**
     * TEMPORALOP is mandatory if STARTTIME and ENDTIME were provided
     */
    public void testTemporalPredicate_TemporalOpMandatory() throws Exception {
        rawKvp.put("startTime", DateUtil.serializeDateTime(1000));
        rawKvp.put("endTime", DateUtil.serializeDateTime(2000));
        kvp = parseKvp(rawKvp);

        try {
            reader.read(reader.createRequest(), kvp, rawKvp);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals("MissingParameterValue", e.getCode());
            assertEquals("TEMPORALOP", e.getLocator());
        }
    }

    public void testTemporalPredicate_NonPeriodTemporalOpAndBothStartAndEndTimeProvided()
            throws Exception {
        rawKvp.put("startTime", DateUtil.serializeDateTime(1000));
        rawKvp.put("endTime", DateUtil.serializeDateTime(2000));
        rawKvp.put("temporalOp", "After");
        kvp = parseKvp(rawKvp);

        try {
            reader.read(reader.createRequest(), kvp, rawKvp);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals("InvalidParameterValue", e.getCode());
            assertEquals("TEMPORALOP", e.getLocator());
        }
    }

    public void testTemporalPredicate_TemporalOpDefaultsToAfter() throws Exception {
        rawKvp.put("startTime", DateUtil.serializeDateTime(1000));
        kvp = parseKvp(rawKvp);

        GetEntries parsed = reader.read(reader.createRequest(), kvp, rawKvp);
        Filter filter = parsed.getFilter();
        assertNotNull(filter);
        assertTrue(filter instanceof PropertyIsGreaterThan);
        assertEquals("updated",
                ((PropertyName) ((PropertyIsGreaterThan) filter).getExpression1())
                        .getPropertyName());
    }

    public void testTemporalPredicate_SupportedOps() throws Exception {
        assertSupportedTemporalOp(null, "Before", PropertyIsLessThan.class);
        assertSupportedTemporalOp(null, "After", PropertyIsGreaterThan.class);
        assertSupportedTemporalOp(2000L, "During", PropertyIsBetween.class);
        assertSupportedTemporalOp(null, "TEquals", PropertyIsEqualTo.class);
        assertSupportedTemporalOp(null, "Begins", PropertyIsGreaterThanOrEqualTo.class);
        assertSupportedTemporalOp(null, "Ends", PropertyIsLessThanOrEqualTo.class);
        assertSupportedTemporalOp(null, "TEquals", PropertyIsEqualTo.class);
    }

    private void assertSupportedTemporalOp(Long endTime, String temporalOp,
            Class<? extends Filter> expected) throws Exception {
        GetEntries parsed;
        Filter filter;

        rawKvp.put("startTime", DateUtil.serializeDateTime(1000));
        if (null != endTime) {
            rawKvp.put("endTime", DateUtil.serializeDateTime(endTime.longValue()));
        } else {
            rawKvp.remove("endTime");
        }
        rawKvp.put("temporalOp", temporalOp);
        kvp = parseKvp(rawKvp);
        parsed = reader.read(reader.createRequest(), kvp, rawKvp);
        filter = parsed.getFilter();

        String message = "Expected " + expected.getName() + " but got "
                + filter.getClass().getName();
        assertTrue(message, expected.isInstance(filter));
    }

    /**
     * OverlappedBy, BegunBy, EndedBy, Meets, MetBy, TContains, TOverlaps.
     * 
     * @throws Exception
     */
    public void testTemporalPredicate_UnSupportedOps() throws Exception {
        assertUnSupportedTemporalOp(null, "BegunBy");
        assertUnSupportedTemporalOp(null, "EndedBy");
        assertUnSupportedTemporalOp(null, "Meets");
        assertUnSupportedTemporalOp(null, "MetBy");
        assertUnSupportedTemporalOp(2000L, "TContains");
        assertUnSupportedTemporalOp(2000L, "TOverlaps");
        assertUnSupportedTemporalOp(2000L, "OverlappedBy");
    }

    private void assertUnSupportedTemporalOp(Long endTime, String temporalOp) throws Exception {
        rawKvp.put("startTime", DateUtil.serializeDateTime(1000));
        if (null != endTime) {
            rawKvp.put("endTime", DateUtil.serializeDateTime(endTime.longValue()));
        } else {
            rawKvp.remove("endTime");
        }
        rawKvp.put("temporalOp", temporalOp);
        kvp = parseKvp(rawKvp);

        try {
            reader.read(reader.createRequest(), kvp, rawKvp);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertEquals("InvalidParameterValue", e.getCode());
            assertEquals("TEMPORALOP", e.getLocator());
        }
    }

    /**
     * @throws Exception
     */
    public void testEntryId_KVP() throws Exception {
        rawKvp.put("entryId", "test-entry-id");
        kvp = parseKvp(rawKvp);

        GetEntries request = reader.read(reader.createRequest(), kvp, rawKvp);
        assertTrue(request.getFilter() instanceof Id);
        Id idFilter = (Id) request.getFilter();
        assertEquals(1, idFilter.getIdentifiers().size());
        assertEquals("test-entry-id", idFilter.getIdentifiers().iterator().next().getID());
    }
}
