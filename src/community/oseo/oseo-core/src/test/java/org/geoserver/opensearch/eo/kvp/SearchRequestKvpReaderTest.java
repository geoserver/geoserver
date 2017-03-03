/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.kvp;

import static org.geoserver.opensearch.eo.kvp.SearchRequestKvpReader.COUNT;
import static org.geoserver.opensearch.eo.kvp.SearchRequestKvpReader.SEARCH_TERMS;
import static org.geoserver.opensearch.eo.kvp.SearchRequestKvpReader.START_INDEX;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.opensearch.eo.OSEOTestSupport;
import org.geoserver.opensearch.eo.SearchRequest;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.OWS20Exception;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.ecql.ECQL;
import org.hamcrest.collection.IsMapContaining;
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
    public void testParseSearchTerms() throws Exception {
        final String searchTermsValue = "a b \"c and d\"";
        Map<String, String> map = toMap(SEARCH_TERMS, searchTermsValue);
        SearchRequest request = parseSearchRequest(map);
        assertEquals(null, request.getParentId());
        final Query query = request.getQuery();
        assertNotNull(query);
        final String expectedCql = "htmlDescription ILIKE '%a%' OR htmlDescription ILIKE '%b%' OR htmlDescription ILIKE '%c and d%'";
        assertEquals(expectedCql, ECQL.toCQL(query.getFilter()));
        Map<String, String> searchParameters = request.getSearchParameters();
        assertEquals(1, searchParameters.size());
        assertThat(searchParameters, hasEntry("os:searchTerms", searchTermsValue));
    }

    /**
     * From spec the parameter keys are case sensitive, and unknown ones should be ignored
     */
    @Test
    public void testParseSearchTermsWrongCase() throws Exception {
        Map<String, String> map = toMap(SEARCH_TERMS.toUpperCase(), "a b \"c and d\"");
        SearchRequest request = parseSearchRequest(map);
        assertEquals(null, request.getParentId());
        final Query query = request.getQuery();
        assertEquals(Filter.INCLUDE, query.getFilter());
    }

    @Test
    public void testPaging() throws Exception {
        Map<String, String> map = toMap(START_INDEX, "10", COUNT, "5");
        SearchRequest request = parseSearchRequest(map);
        assertEquals(null, request.getParentId());
        final Query query = request.getQuery();
        assertNotNull(query);
        assertEquals(Filter.INCLUDE, query.getFilter());
        assertEquals(10, (int) query.getStartIndex());
        assertEquals(5, query.getMaxFeatures());
        
        Map<String, String> searchParameters = request.getSearchParameters();
        assertEquals(2, searchParameters.size());
        assertThat(searchParameters, hasEntry("os:startIndex", "10"));
        assertThat(searchParameters, hasEntry("os:count", "5"));
    }

    @Test
    public void testStartIndexNegative() throws Exception {
        try {
            parseSearchRequest(toMap(START_INDEX, "-10"));
        } catch (OWS20Exception e) {
            assertEquals("InvalidParameterValue", e.getCode());
        }
    }

    @Test
    public void testStartIndexNotNumber() throws Exception {
        try {
            parseSearchRequest(toMap(START_INDEX, "abc"));
        } catch (OWS20Exception e) {
            assertEquals("InvalidParameterValue", e.getCode());
        }
    }

    @Test
    public void testStartIndexFloat() throws Exception {
        try {
            parseSearchRequest(toMap(START_INDEX, "1.23"));
        } catch (OWS20Exception e) {
            assertEquals("InvalidParameterValue", e.getCode());
        }
    }

    @Test
    public void testCountIndexNegative() throws Exception {
        try {
            parseSearchRequest(toMap(COUNT, "-10"));
        } catch (OWS20Exception e) {
            assertEquals("InvalidParameterValue", e.getCode());
        }
    }

    @Test
    public void testCountIndexNotNumber() throws Exception {
        try {
            parseSearchRequest(toMap(COUNT, "abc"));
        } catch (OWS20Exception e) {
            assertEquals("InvalidParameterValue", e.getCode());
        }
    }

    @Test
    public void testcountIndexFloat() throws Exception {
        try {
            parseSearchRequest(toMap(COUNT, "1.23"));
        } catch (OWS20Exception e) {
            assertEquals("InvalidParameterValue", e.getCode());
        }
    }

}
