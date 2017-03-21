/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.kvp;

import static org.geoserver.opensearch.eo.kvp.SearchRequestKvpReader.COUNT_KEY;
import static org.geoserver.opensearch.eo.kvp.SearchRequestKvpReader.PARENT_ID_KEY;
import static org.geoserver.opensearch.eo.OpenSearchParameters.*;

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OSEOTestSupport;
import org.geoserver.opensearch.eo.OpenSearchParameters;
import org.geoserver.opensearch.eo.SearchRequest;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.OWS20Exception;
import org.geotools.data.Parameter;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

public class SearchRequestKvpReaderTest extends OSEOTestSupport {

    private SearchRequestKvpReader reader;

    private static FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    @Before
    public void getReader() {
        reader = GeoServerExtensions.bean(SearchRequestKvpReader.class);
    }

    private Map<String, String> toMap(String... kvp) {
        Map params = new HashMap<>();
        for (int i = 0; i < kvp.length; i += 2) {
            String name = kvp[i];
            String value = kvp[i + 1];
            params.put(name, value);
        }
        return params;
    }

    private SearchRequest parseSearchRequest(Map<String, String> map) throws Exception {
        return (SearchRequest) reader.read(reader.createRequest(), map, map);
    }
    
    @Test
    public void testGetAll() throws Exception {
        SearchRequest request = parseSearchRequest(Collections.emptyMap());
        assertEquals(null, request.getParentId());
        final Query query = request.getQuery();
        assertNotNull(query);
        assertEquals(Filter.INCLUDE, query.getFilter());
        assertNull(query.getStartIndex());
        assertEquals(OSEOInfo.DEFAULT_RECORDS_PER_PAGE, query.getMaxFeatures());
    }

    @Test
    public void testParseSearchTerms() throws Exception {
        final String searchTermsValue = "a b \"c and d\"";
        Map<String, String> map = toMap(SEARCH_TERMS.key, searchTermsValue);
        SearchRequest request = parseSearchRequest(map);
        assertEquals(null, request.getParentId());
        final Query query = request.getQuery();
        assertNotNull(query);
        final String expectedCql = "htmlDescription ILIKE '%a%' OR htmlDescription ILIKE '%b%' OR htmlDescription ILIKE '%c and d%'";
        assertEquals(expectedCql, ECQL.toCQL(query.getFilter()));
        Map<Parameter, String> searchParameters = request.getSearchParameters();
        assertEquals(1, searchParameters.size());
        assertThat(searchParameters, hasEntry(OpenSearchParameters.SEARCH_TERMS, searchTermsValue));
    }

    /**
     * From spec the parameter keys are case sensitive, and unknown ones should be ignored
     */
    @Test
    public void testParseSearchTermsWrongCase() throws Exception {
        Map<String, String> map = toMap(SEARCH_TERMS.key.toUpperCase(), "a b \"c and d\"");
        SearchRequest request = parseSearchRequest(map);
        assertEquals(null, request.getParentId());
        final Query query = request.getQuery();
        assertEquals(Filter.INCLUDE, query.getFilter());
    }
    
    @Test
    public void testParseGeoUid() throws Exception {
        Map<String, String> map = toMap(GEO_UID.key, "abcd");
        SearchRequest request = parseSearchRequest(map);
        assertEquals(null, request.getParentId());
        final Query query = request.getQuery();
        assertNotNull(query);
        final String expectedCql = "identifier = 'abcd'";
        assertEquals(expectedCql, ECQL.toCQL(query.getFilter()));
        Map<Parameter, String> searchParameters = request.getSearchParameters();
        assertEquals(1, searchParameters.size());
        assertThat(searchParameters, hasEntry(OpenSearchParameters.GEO_UID, "abcd"));
    }
    
    @Test
    public void testParseTimeBox() throws Exception {
        Map<String, String> map = toMap(GEO_BOX.key, "10,20,30,40");
        SearchRequest request = parseSearchRequest(map);
        assertEquals(null, request.getParentId());
        final Query query = request.getQuery();
        assertNotNull(query);
        final String expectedCql = "BBOX(, 10.0,20.0,30.0,40.0)";
        assertEquals(expectedCql, ECQL.toCQL(query.getFilter()));
        Map<Parameter, String> searchParameters = request.getSearchParameters();
        assertEquals(1, searchParameters.size());
        assertThat(searchParameters, hasEntry(OpenSearchParameters.GEO_BOX, "10,20,30,40"));
    }

    @Test
    public void testPaging() throws Exception {
        Map<String, String> map = toMap(START_INDEX.key, "10", COUNT_KEY, "5");
        SearchRequest request = parseSearchRequest(map);
        assertEquals(null, request.getParentId());
        final Query query = request.getQuery();
        assertNotNull(query);
        assertEquals(Filter.INCLUDE, query.getFilter());
        assertEquals(9, (int) query.getStartIndex()); // from 1 based to 0 based
        assertEquals(5, query.getMaxFeatures());
        
        Map<Parameter, String> searchParameters = request.getSearchParameters();
        assertEquals(2, searchParameters.size());
        assertThat(searchParameters, hasEntry(OpenSearchParameters.START_INDEX, "10"));
        // does not work but yet to figure out why
        // assertThat(searchParameters, hasEntry(hasProperty("name", equalTo("count")), "5"));
    }

    @Test
    public void testStartIndexNegative() throws Exception {
        try {
            parseSearchRequest(toMap(START_INDEX.key, "-10"));
            fail("Should have failed");
        } catch (OWS20Exception e) {
            assertEquals("InvalidParameterValue", e.getCode());
        }
    }

    @Test
    public void testStartIndexNotNumber() throws Exception {
        try {
            parseSearchRequest(toMap(START_INDEX.key, "abc"));
            fail("Should have failed");
        } catch (OWS20Exception e) {
            assertEquals("InvalidParameterValue", e.getCode());
        }
    }

    @Test
    public void testStartIndexFloat() throws Exception {
        try {
            parseSearchRequest(toMap(START_INDEX.key, "1.23"));
            fail("Should have failed");
        } catch (OWS20Exception e) {
            assertEquals("InvalidParameterValue", e.getCode());
        }
    }

    @Test
    public void testCountIndexNegative() throws Exception {
        try {
            parseSearchRequest(toMap(COUNT_KEY, "-10"));
            fail("Should have failed");
        } catch (OWS20Exception e) {
            assertEquals("InvalidParameterValue", e.getCode());
        }
    }

    @Test
    public void testCountIndexNotNumber() throws Exception {
        try {
            parseSearchRequest(toMap(COUNT_KEY, "abc"));
            fail("Should have failed");
        } catch (OWS20Exception e) {
            assertEquals("InvalidParameterValue", e.getCode());
        }
    }
    
    @Test
    public void testCountTooBig() throws Exception {
        try {
            parseSearchRequest(toMap(COUNT_KEY, "1000"));
            fail("Should have failed");
        } catch (OWS20Exception e) {
            assertEquals("InvalidParameterValue", e.getCode());
        }
    }

    @Test
    public void testCountIndexFloat() throws Exception {
        try {
            parseSearchRequest(toMap(COUNT_KEY, "1.23"));
            fail("Should have failed");
        } catch (OWS20Exception e) {
            assertEquals("InvalidParameterValue", e.getCode());
        }
    }
    
    @Test
    public void testParentId() throws Exception {
        Map<String, String> map = toMap(PARENT_ID_KEY, "SENTINEL2");
        SearchRequest request = parseSearchRequest(map);
        assertEquals("SENTINEL2", request.getParentId());
        final Query query = request.getQuery();
        assertNotNull(query);
        // no filter here, the parent id needs to be translate to an internal identifier
        assertEquals(Filter.INCLUDE, query.getFilter());
    }
    
    @Test
    public void testDistanceFromPoint() throws Exception {
        Map<String, String> map = toMap(GEO_LON.key, "12", GEO_LAT.key, "45", GEO_RADIUS.key, "20000");
        SearchRequest request = parseSearchRequest(map);
        final Query query = request.getQuery();
        assertNotNull(query);
        final String expectedCql = "DWITHIN(\"\", POINT (12 45), 20000.0, m)";
        assertEquals(expectedCql, ECQL.toCQL(query.getFilter()));
    }
    
    @Test
    public void testNegativeDistanceFromPoint() throws Exception {
        try {
            Map<String, String> map = toMap(GEO_LON.key, "12", GEO_LAT.key, "45", GEO_RADIUS.key, "-10");
            parseSearchRequest(map);
            fail("Should have failed");
        } catch (OWS20Exception e) {
            assertEquals("InvalidParameterValue", e.getCode());
        }
    }
    
    @Test
    public void testTimeRelationInvalid() throws Exception {
        try {
            Map<String, String> map = toMap(TIME_RELATION.key, "abcd");
            parseSearchRequest(map);
            fail("Should have failed");
        } catch (OWS20Exception e) {
            assertEquals("InvalidParameterValue", e.getCode());
            assertEquals("relation", e.getLocator());
        }
    }
    
    @Test
    public void testTimeRelationAlone() throws Exception {
        try {
            Map<String, String> map = toMap(TIME_RELATION.key, "intersects");
            parseSearchRequest(map);
            fail("Should have failed");
        } catch (OWS20Exception e) {
            assertEquals("InvalidParameterValue", e.getCode());
            assertEquals("relation", e.getLocator());
        }
    }
    
    @Test
    public void testTimeStartInvalid() throws Exception {
        try {
            Map<String, String> map = toMap(TIME_START.key, "abcd");
            parseSearchRequest(map);
            fail("Should have failed");
        } catch (OWS20Exception e) {
            assertEquals("InvalidParameterValue", e.getCode());
            assertEquals(TIME_START.key, e.getLocator());
        }
    }
    
    @Test
    public void testTimeEndInvalid() throws Exception {
        try {
            Map<String, String> map = toMap(TIME_END.key, "abcd");
            parseSearchRequest(map);
            fail("Should have failed");
        } catch (OWS20Exception e) {
            assertEquals("InvalidParameterValue", e.getCode());
            assertEquals(TIME_END.key, e.getLocator());
        }
    }
    
    @Test
    public void testTimeFilterStartOnly() throws Exception {
        Map<String, String> map;
        // intersection behavior, the features must overlap the provided range 
        map = toMap(TIME_START.key, "2010-09-01T00:00:00Z");
        assertEquals(ECQL.toFilter("timeEnd >= 2010-09-01T00:00:00Z OR timeEnd IS NULL"), parseAndGetFilter(map));
        // intersection behavior again, explicit
        map = toMap(TIME_START.key, "2010-09-01T00:00:00Z", TIME_RELATION.key, "intersects");
        assertEquals(ECQL.toFilter("timeEnd >= 2010-09-01T00:00:00Z OR timeEnd IS NULL"), parseAndGetFilter(map));
        // contains (the feature must contain the requested interval)
        map = toMap(TIME_START.key, "2010-09-01T00:00:00Z", TIME_RELATION.key, "contains");
        assertEquals(ECQL.toFilter("timeStart <= 2010-09-01T00:00:00Z and timeEnd IS NULL"), parseAndGetFilter(map));
        // during (the features are inside the requested interval)
        map = toMap(TIME_START.key, "2010-09-01T00:00:00Z", TIME_RELATION.key, "during");
        assertEquals(ECQL.toFilter("timeStart >= 2010-09-01T00:00:00Z"), parseAndGetFilter(map));
        // disjoint (the features are outside the requested interval)
        map = toMap(TIME_START.key, "2010-09-01T00:00:00Z", TIME_RELATION.key, "disjoint");
        assertEquals(ECQL.toFilter("timeEnd < 2010-09-01T00:00:00Z"), parseAndGetFilter(map));
        // equal (the features have the same interval of validity
        map = toMap(TIME_START.key, "2010-09-01T00:00:00Z", TIME_RELATION.key, "equals");
        assertEquals(ECQL.toFilter("timeStart = 2010-09-01T00:00:00Z and timeEnd IS NULL"), parseAndGetFilter(map));
    }
    
    @Test
    public void testTimeFilterEndOnly() throws Exception {
        Map<String, String> map;
        // intersection behavior, the features must overlap the provided range 
        map = toMap(TIME_END.key, "2010-09-01T00:00:00Z");
        assertEquals(ECQL.toFilter("timeStart <= 2010-09-01T00:00:00Z OR timeStart IS NULL"), parseAndGetFilter(map));
        // intersection behavior again, explicit
        map = toMap(TIME_END.key, "2010-09-01T00:00:00Z", TIME_RELATION.key, "intersects");
        assertEquals(ECQL.toFilter("timeStart <= 2010-09-01T00:00:00Z OR timeStart IS NULL"), parseAndGetFilter(map));
        // contains (the feature must contain the requested interval)
        map = toMap(TIME_END.key, "2010-09-01T00:00:00Z", TIME_RELATION.key, "contains");
        assertEquals(ECQL.toFilter("timeEnd >= 2010-09-01T00:00:00Z and timeStart IS NULL"), parseAndGetFilter(map));
        // during (the features are inside the requested interval)
        map = toMap(TIME_END.key, "2010-09-01T00:00:00Z", TIME_RELATION.key, "during");
        assertEquals(ECQL.toFilter("timeEnd <= 2010-09-01T00:00:00Z"), parseAndGetFilter(map));
        // disjoint (the features are outside the requested interval)
        map = toMap(TIME_END.key, "2010-09-01T00:00:00Z", TIME_RELATION.key, "disjoint");
        assertEquals(ECQL.toFilter("timeStart > 2010-09-01T00:00:00Z"), parseAndGetFilter(map));
        // equal (the features have the same interval of validity
        map = toMap(TIME_END.key, "2010-09-01T00:00:00Z", TIME_RELATION.key, "equals");
        assertEquals(ECQL.toFilter("timeEnd = 2010-09-01T00:00:00Z and timeStart IS NULL"), parseAndGetFilter(map));
    }
    
    @Test
    public void testTimeFilterStartEndOnly() throws Exception {
        Map<String, String> map;
        // intersection behavior, the features must overlap the provided range 
        map = toMap(TIME_START.key, "2010-08-01T00:00:00Z", TIME_END.key, "2010-09-01T00:00:00Z");
        assertEquals(ECQL.toFilter("(timeStart <= 2010-09-01T00:00:00Z or timeStart IS NULL) AND (timeEnd >= 2010-08-01T00:00:00Z or timeEnd IS NULL)"), parseAndGetFilter(map));
        // intersection behavior again, explicit
        map = toMap(TIME_START.key, "2010-08-01T00:00:00Z", TIME_END.key, "2010-09-01T00:00:00Z", TIME_RELATION.key, "intersects");
        assertEquals(ECQL.toFilter("(timeStart <= 2010-09-01T00:00:00Z or timeStart IS NULL) AND (timeEnd >= 2010-08-01T00:00:00Z or timeEnd IS NULL)"), parseAndGetFilter(map));
        // contains (the feature must contain the requested interval)
        map = toMap(TIME_START.key, "2010-08-01T00:00:00Z", TIME_END.key, "2010-09-01T00:00:00Z", TIME_RELATION.key, "contains");
        assertEquals(ECQL.toFilter("timeStart <= 2010-08-01T00:00:00Z and timeEnd >= 2010-09-01T00:00:00Z"), parseAndGetFilter(map));
        // during (the features are inside the requested interval)
        map = toMap(TIME_START.key, "2010-08-01T00:00:00Z", TIME_END.key, "2010-09-01T00:00:00Z", TIME_RELATION.key, "during");
        assertEquals(ECQL.toFilter("timeStart >= 2010-08-01T00:00:00Z and timeEnd <= 2010-09-01T00:00:00Z"), parseAndGetFilter(map));
        // disjoint (the features are outside the requested interval)
        map = toMap(TIME_START.key, "2010-08-01T00:00:00Z", TIME_END.key, "2010-09-01T00:00:00Z", TIME_RELATION.key, "disjoint");
        assertEquals(ECQL.toFilter("timeStart > 2010-09-01T00:00:00Z or timeEnd < 2010-08-01T00:00:00Z"), parseAndGetFilter(map));
        // equal (the features have the same interval of validity
        map = toMap(TIME_START.key, "2010-08-01T00:00:00Z", TIME_END.key, "2010-09-01T00:00:00Z", TIME_RELATION.key, "equals");
        assertEquals(ECQL.toFilter("timeStart = 2010-08-01T00:00:00Z and timeEnd = 2010-09-01T00:00:00Z"), parseAndGetFilter(map));
    }
    
    @Test
    public void testTimeStartOnlyDate() throws Exception {
        Map<String, String> map;
        // intersection behavior, the features must overlap the provided range 
        map = toMap(TIME_START.key, "2010-09-01");
        assertEquals(ECQL.toFilter("timeEnd >= 2010-09-01T00:00:00Z OR timeEnd IS NULL"), parseAndGetFilter(map));
    }
    
    

    private Filter parseAndGetFilter(Map<String, String> map) throws Exception {
        SearchRequest request = parseSearchRequest(map);
        final Query query = request.getQuery();
        assertNotNull(query);
        Filter filter = query.getFilter();
        return filter;
    }
    
}
