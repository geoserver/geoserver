/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Ordering;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.namespace.QName;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.visitor.UniqueVisitor;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opengis.feature.simple.SimpleFeatureType;

public class PagedUniqueProcessTest extends WPSTestSupport {

    private static final String FIELD_NAME = "state_name";

    private static final int TOTAL_DISTINCT = 4;

    public PagedUniqueProcessTest() {
        super();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // do the default by calling super
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addVectorLayer(
                new QName(MockData.SF_URI, "states", MockData.SF_PREFIX),
                Collections.EMPTY_MAP,
                "states.properties",
                PagedUniqueProcessTest.class,
                catalog);
    }

    @Test
    public void testAll() throws Exception {
        String xml = buildInputXml(FIELD_NAME, null, null, null, null);
        String jsonString = string(post(root(), xml));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray values = json.getJSONArray("values");
        int size = json.getInt("size");
        assertEquals(TOTAL_DISTINCT, size);
        assertEquals(size, values.size());
    }

    @Test
    public void testASCPagination1() throws Exception {
        String xml = buildInputXml(FIELD_NAME, null, 0, 1, "ASC");
        String jsonString = string(post(root(), xml));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray values = json.getJSONArray("values");
        int size = json.getInt("size");
        assertEquals(TOTAL_DISTINCT, size);
        assertEquals(1, values.size());
        assertEquals("Delaware", values.get(0));
    }

    @Test
    public void testASCPagination2() throws Exception {
        String xml = buildInputXml(FIELD_NAME, null, 1, 1, "ASC");
        String jsonString = string(post(root(), xml));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray values = json.getJSONArray("values");
        int size = json.getInt("size");
        assertEquals(TOTAL_DISTINCT, size);
        assertEquals("District of Columbia", values.get(0));
    }

    @Test
    public void testASCPagination3() throws Exception {
        String xml = buildInputXml(FIELD_NAME, null, 2, 1, "ASC");
        String jsonString = string(post(root(), xml));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray values = json.getJSONArray("values");
        int size = json.getInt("size");
        assertEquals(TOTAL_DISTINCT, size);
        assertEquals("Illinois", values.get(0));
    }

    @Test
    public void testDESCPagination1() throws Exception {
        String xml = buildInputXml(FIELD_NAME, null, 0, 1, "DESC");
        String jsonString = string(post(root(), xml));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray values = json.getJSONArray("values");
        int size = json.getInt("size");
        assertEquals(TOTAL_DISTINCT, size);
        assertEquals("West Virginia", values.get(0));
    }

    @Test
    public void testDESCPagination2() throws Exception {
        String xml = buildInputXml(FIELD_NAME, null, 1, 1, "DESC");
        String jsonString = string(post(root(), xml));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray values = json.getJSONArray("values");
        int size = json.getInt("size");
        assertEquals(TOTAL_DISTINCT, size);
        assertEquals("Illinois", values.get(0));
    }

    @Test
    public void testDESCPagination3() throws Exception {
        String xml = buildInputXml(FIELD_NAME, null, 2, 1, "DESC");
        String jsonString = string(post(root(), xml));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray values = json.getJSONArray("values");
        int size = json.getInt("size");
        assertEquals(TOTAL_DISTINCT, size);
        assertEquals("District of Columbia", values.get(0));
    }

    @Test
    public void testLimits() throws Exception {
        String xml = buildInputXml(FIELD_NAME, null, 2, 2, null);
        String jsonString = string(post(root(), xml));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray values = json.getJSONArray("values");
        int size = json.getInt("size");
        assertEquals(TOTAL_DISTINCT, size);
        assertEquals(2, values.size());
    }

    @Test
    public void testUniqueVisitorAlwaysDeclaresLimits() throws Exception {
        PagedUniqueProcess process = new PagedUniqueProcess();
        SimpleFeatureCollection features = Mockito.mock(SimpleFeatureCollection.class);
        SimpleFeatureType featureType =
                (SimpleFeatureType) catalog.getFeatureTypeByName("states").getFeatureType();
        Mockito.when(features.getSchema()).thenReturn(featureType);
        final AtomicInteger counter = new AtomicInteger();
        // mock optimized store behaviour to always
        // use hasLimits
        Mockito.doAnswer(
                        new Answer() {
                            @Override
                            public Object answer(InvocationOnMock invocation) throws Throwable {
                                UniqueVisitor visitor =
                                        (UniqueVisitor) invocation.getArguments()[0];
                                if (visitor.hasLimits()) {
                                    counter.incrementAndGet();
                                }
                                visitor.setValue(Arrays.asList("a", "b", "c", "d"));
                                return null;
                            }
                        })
                .when(features)
                .accepts(Mockito.any(UniqueVisitor.class), Mockito.any());
        process.execute(features, FIELD_NAME, 0, 2);
        // checks that hasLimits is always true
        // both for size calculation query and for page extraction query
        assertEquals(2, counter.intValue());
    }

    /*
     * MaxFeature overflow is not an error: return all result from startIndex to end
     */
    @Test
    public void testMaxFeaturesOverflow() throws Exception {
        String xml = buildInputXml(FIELD_NAME, null, 2, 20, null);
        String jsonString = string(post(root(), xml));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray values = json.getJSONArray("values");
        int size = json.getInt("size");
        assertEquals(TOTAL_DISTINCT, size);
        assertEquals(TOTAL_DISTINCT - 2, values.size());
    }

    @Test
    public void testAllParameters() throws Exception {
        String xml = buildInputXml(FIELD_NAME, "*a*", 1, 2, "DESC");
        String jsonString = string(post(root(), xml));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray values = json.getJSONArray("values");
        int size = json.getInt("size");
        assertEquals(3, size);
        assertEquals(2, values.size());
        assertEquals(true, Ordering.natural().reverse().isOrdered(values));
        for (int count = 0; count < values.size(); count++) {
            assertEquals(true, ((String) values.get(count)).matches(".*(?i:a)?.*"));
        }
    }

    @Test
    public void testFilteredStarts() throws Exception {
        String xml = buildInputXml(FIELD_NAME, "d*", null, null, null);
        String jsonString = string(post(root(), xml));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray values = json.getJSONArray("values");
        int size = json.getInt("size");
        assertEquals(size, values.size());
        for (int count = 0; count < values.size(); count++) {
            assertEquals(true, ((String) values.get(count)).matches("^(?i:d).*"));
        }
    }

    @Test
    public void testFilteredContains() throws Exception {
        String xml = buildInputXml(FIELD_NAME, "*A*", null, null, null);
        String jsonString = string(post(root(), xml));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray values = json.getJSONArray("values");
        int size = json.getInt("size");
        assertEquals(size, values.size());
        for (int count = 0; count < values.size(); count++) {
            assertEquals(true, ((String) values.get(count)).matches(".*(?i:a)?.*"));
        }
    }

    @Test
    public void testFilteredEnds() throws Exception {
        String xml = buildInputXml(FIELD_NAME, "*A", null, null, null);
        String jsonString = string(post(root(), xml));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray values = json.getJSONArray("values");
        int size = json.getInt("size");
        assertEquals(size, values.size());
        for (int count = 0; count < values.size(); count++) {
            assertEquals(true, ((String) values.get(count)).matches(".*(?i:a)$"));
        }
    }

    @Test
    /*
     * StartIndex overflow is an error: return no result
     */
    public void testStartIndexOverflow() throws Exception {
        String xml = buildInputXml(FIELD_NAME, null, 6, 4, null);
        String jsonString = string(post(root(), xml));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray values = json.getJSONArray("values");
        int size = json.getInt("size");
        assertEquals(TOTAL_DISTINCT, size);
        assertEquals(0, values.size());
    }

    @Test
    public void testAscOrder() throws Exception {
        String xml = buildInputXml(FIELD_NAME, null, null, null, "ASC");
        String jsonString = string(post(root(), xml));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray values = json.getJSONArray("values");
        int size = json.getInt("size");
        assertEquals(TOTAL_DISTINCT, size);
        assertEquals(true, Ordering.natural().isOrdered(values));
    }

    @Test
    public void testDescOrder() throws Exception {
        String xml = buildInputXml(FIELD_NAME, null, null, null, "DESC");
        String jsonString = string(post(root(), xml));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray values = json.getJSONArray("values");
        int size = json.getInt("size");
        assertEquals(TOTAL_DISTINCT, size);
        assertEquals(true, Ordering.natural().reverse().isOrdered(values));
    }

    private String buildInputXml(
            String fieldName,
            String fieldFilter,
            Integer startIndex,
            Integer maxFeatures,
            String sort) {

        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:PagedUnique</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "     <ows:Identifier>features</ows:Identifier>\n"
                        + "     <wps:Reference mimeType=\"text/xml\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                        + "      <wps:Body>\n"
                        + "       <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\" xmlns:"
                        + MockData.SF_PREFIX
                        + "=\""
                        + MockData.SF_URI
                        + "\">\n"
                        + "          <wfs:Query typeName=\"sf:states\">\n";
        if (fieldFilter != null) {
            xml =
                    xml
                            + "     <ogc:Filter>\n"
                            + "                  <ogc:PropertyIsLike wildCard=\"*\" singleChar=\"?\" escape=\"\\\\\" matchCase=\"false\">\n"
                            + "                    <ogc:PropertyName>"
                            + fieldName
                            + "</ogc:PropertyName>\n"
                            + "                    <ogc:Literal>"
                            + fieldFilter
                            + "</ogc:Literal>\n"
                            + "                  </ogc:PropertyIsLike>\n"
                            + "               </ogc:Filter>\n";
        }
        if (sort != null) {
            xml =
                    xml
                            + "     <ogc:SortBy>\n"
                            + "                 <ogc:SortProperty>\n"
                            + "                  <ogc:PropertyName>"
                            + fieldName
                            + "</ogc:PropertyName>\n"
                            + "                  <ogc:SortOrder>"
                            + sort
                            + "</ogc:SortOrder>\n"
                            + "                 </ogc:SortProperty>\n"
                            + "                </ogc:SortBy>\n";
        }
        xml =
                xml
                        + "        </wfs:Query>\n"
                        + "         </wfs:GetFeature>\n"
                        + "         </wps:Body>\n"
                        + "       </wps:Reference>\n"
                        + "     </wps:Input>\n";

        xml =
                xml
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>fieldName</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>"
                        + fieldName
                        + "</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n";

        if (startIndex != null) {
            xml =
                    xml
                            + "    <wps:Input>\n"
                            + "      <ows:Identifier>startIndex</ows:Identifier>\n"
                            + "      <wps:Data>\n"
                            + "        <wps:LiteralData>"
                            + startIndex
                            + "</wps:LiteralData>\n"
                            + "      </wps:Data>\n"
                            + "    </wps:Input>\n";
        }
        if (maxFeatures != null) {
            xml =
                    xml
                            + "    <wps:Input>\n"
                            + "      <ows:Identifier>maxFeatures</ows:Identifier>\n"
                            + "      <wps:Data>\n"
                            + "        <wps:LiteralData>"
                            + maxFeatures
                            + "</wps:LiteralData>\n"
                            + "      </wps:Data>\n"
                            + "    </wps:Input>\n";
        }

        xml =
                xml
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"application/json\">\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";
        return xml;
    }
}
