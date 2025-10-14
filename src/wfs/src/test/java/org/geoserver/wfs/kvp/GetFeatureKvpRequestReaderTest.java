/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.QueryType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.ServiceException;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wfs.WFSException;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Test;

public class GetFeatureKvpRequestReaderTest extends GeoServerSystemTestSupport {

    private static GetFeatureKvpRequestReader reader;

    @Override
    protected void onSetUp(SystemTestData data) throws Exception {
        reader = new GetFeatureKvpRequestReader(
                GetFeatureType.class, getGeoServer(), CommonFactoryFinder.getFilterFactory(null));
    }

    /** https://osgeo-org.atlassian.net/browse/GEOS-1875 */
    @Test
    @SuppressWarnings("unchecked")
    public void testInvalidTypeNameBbox() throws Exception {
        Map raw = new HashMap<>();
        raw.put("service", "WFS");
        raw.put("version", "1.1.0");
        raw.put("request", "GetFeature");
        raw.put("bbox", "-80.4864795578115,25.6176257083275,-80.3401307394915,25.7002737069969");
        raw.put("typeName", "cite:InvalidTypeName");

        Map parsed = parseKvp(raw);

        try {
            // before fix for GEOS-1875 this would bomb out with an NPE instead of the proper
            // exception
            reader.read(WfsFactory.eINSTANCE.createGetFeatureType(), parsed, raw);
        } catch (WFSException e) {
            assertEquals("InvalidParameterValue", e.getCode());
            assertEquals("typeName", e.getLocator());
            // System.out.println(e.getMessage());
            assertTrue(e.getMessage().contains("cite:InvalidTypeName"));
        }
    }

    /** Same as GEOS-1875, but let's check without bbox and without name prefix */
    @SuppressWarnings("unchecked")
    @Test
    public void testInvalidTypeName() throws Exception {
        Map raw = new HashMap<>();
        raw.put("service", "WFS");
        raw.put("version", "1.1.0");
        raw.put("request", "GetFeature");
        raw.put("typeName", "InvalidTypeName");

        try {
            Map parsed = parseKvp(raw);
            reader.read(WfsFactory.eINSTANCE.createGetFeatureType(), parsed, raw);
        } catch (WFSException e) {
            assertEquals("InvalidParameterValue", e.getCode());
            assertEquals("typeName", e.getLocator());
            // System.out.println(e.getMessage());
            assertTrue(e.getMessage().contains("InvalidTypeName"));
        }
    }

    /** See https://osgeo-org.atlassian.net/browse/GEOS-1875 */
    @SuppressWarnings("unchecked")
    @Test
    public void testUserProvidedNamespace() throws Exception {
        final String localPart = SystemTestData.MLINES.getLocalPart();
        final String namespace = SystemTestData.MLINES.getNamespaceURI();
        final String alternamePrefix = "ex";
        final String alternameTypeName = alternamePrefix + ":" + localPart;

        Map<String, Object> raw = new HashMap<>();
        raw.put("service", "WFS");
        raw.put("version", "1.1.0");
        raw.put("request", "GetFeature");
        raw.put("typeName", alternameTypeName);
        raw.put("namespace", "xmlns(" + alternamePrefix + "=" + namespace + ")");

        Map<String, Object> parsed = parseKvp(raw);

        GetFeatureType req = WfsFactory.eINSTANCE.createGetFeatureType();
        Object read = reader.read(req, parsed, raw);
        GetFeatureType parsedReq = (GetFeatureType) read;
        QueryType query = (QueryType) parsedReq.getQuery().get(0);
        List<QName> typeNames = query.getTypeName();
        assertEquals(1, typeNames.size());
        assertEquals(SystemTestData.MLINES, typeNames.get(0));
    }

    /** See https://osgeo-org.atlassian.net/browse/GEOS-1875 */
    @SuppressWarnings("unchecked")
    @Test
    public void testUserProvidedDefaultNamespace() throws Exception {
        final QName qName = SystemTestData.STREAMS;
        final String typeName = qName.getLocalPart();
        final String defaultNamespace = qName.getNamespaceURI();

        Map<String, Object> raw = new HashMap<>();
        raw.put("service", "WFS");
        raw.put("version", "1.1.0");
        raw.put("request", "GetFeature");
        raw.put("typeName", typeName);
        raw.put("namespace", "xmlns(" + defaultNamespace + ")");

        Map<String, Object> parsed = parseKvp(raw);

        GetFeatureType req = WfsFactory.eINSTANCE.createGetFeatureType();
        Object read = reader.read(req, parsed, raw);
        GetFeatureType parsedReq = (GetFeatureType) read;
        QueryType query = (QueryType) parsedReq.getQuery().get(0);
        List<QName> typeNames = query.getTypeName();
        assertEquals(1, typeNames.size());
        assertEquals(qName, typeNames.get(0));
    }

    @Test
    public void testViewParams() throws Exception {
        Map<String, Object> raw = new HashMap<>();
        raw.put("service", "WFS");
        raw.put("version", "1.1.0");
        raw.put("request", "GetFeature");
        raw.put("typeName", getLayerId(SystemTestData.STREAMS));
        raw.put("viewParams", "where:WHERE PERSONS > 1000000;str:ABCD");

        Map<String, Object> parsed = parseKvp(raw);

        GetFeatureType req = WfsFactory.eINSTANCE.createGetFeatureType();
        Object read = reader.read(req, parsed, raw);
        GetFeatureType parsedReq = (GetFeatureType) read;
        assertEquals(1, parsedReq.getViewParams().size());
        List viewParams = parsedReq.getViewParams();
        assertEquals(1, viewParams.size());
        @SuppressWarnings("unchecked")
        Map<String, String> vp1 = (Map) viewParams.get(0);
        assertEquals("WHERE PERSONS > 1000000", vp1.get("where"));
        assertEquals("ABCD", vp1.get("str"));
    }

    @Test
    public void testViewParamsMulti() throws Exception {
        Map<String, Object> raw = new HashMap<>();
        raw.put("service", "WFS");
        raw.put("version", "1.1.0");
        raw.put("request", "GetFeature");
        raw.put("typeName", getLayerId(SystemTestData.STREAMS) + "," + getLayerId(SystemTestData.BASIC_POLYGONS));
        raw.put("viewParams", "where:WHERE PERSONS > 1000000;str:ABCD,where:WHERE PERSONS > 10;str:FOO");

        Map<String, Object> parsed = parseKvp(raw);

        GetFeatureType req = WfsFactory.eINSTANCE.createGetFeatureType();
        Object read = reader.read(req, parsed, raw);
        GetFeatureType parsedReq = (GetFeatureType) read;
        List viewParams = parsedReq.getViewParams();
        assertEquals(2, viewParams.size());
        @SuppressWarnings("unchecked")
        Map<String, String> vp1 = (Map) viewParams.get(0);
        assertEquals("WHERE PERSONS > 1000000", vp1.get("where"));
        assertEquals("ABCD", vp1.get("str"));
        @SuppressWarnings("unchecked")
        Map<String, String> vp2 = (Map) viewParams.get(1);
        assertEquals("WHERE PERSONS > 10", vp2.get("where"));
        assertEquals("FOO", vp2.get("str"));
    }

    @Test
    public void testViewParamsFanOut() throws Exception {
        Map<String, Object> raw = new HashMap<>();
        raw.put("service", "WFS");
        raw.put("version", "1.1.0");
        raw.put("request", "GetFeature");
        raw.put("typeName", getLayerId(SystemTestData.STREAMS) + "," + getLayerId(SystemTestData.BASIC_POLYGONS));
        raw.put("viewParams", "where:WHERE PERSONS > 1000000;str:ABCD");

        Map<String, Object> parsed = parseKvp(raw);

        GetFeatureType req = WfsFactory.eINSTANCE.createGetFeatureType();
        Object read = reader.read(req, parsed, raw);
        GetFeatureType parsedReq = (GetFeatureType) read;
        List viewParams = parsedReq.getViewParams();
        assertEquals(2, viewParams.size());
        @SuppressWarnings("unchecked")
        Map<String, String> vp1 = (Map) viewParams.get(0);
        assertEquals("WHERE PERSONS > 1000000", vp1.get("where"));
        assertEquals("ABCD", vp1.get("str"));
        @SuppressWarnings("unchecked")
        Map<String, String> vp2 = (Map) viewParams.get(1);
        assertEquals("WHERE PERSONS > 1000000", vp2.get("where"));
        assertEquals("ABCD", vp2.get("str"));
    }

    @Test
    public void testXMLViewParamsMulti() throws Exception {
        try {
            Request request = new Request();
            request.setRawKvp(new HashMap<>());
            request.getRawKvp().put("viewParamsFormat", "XML");
            Dispatcher.REQUEST.set(request);
            Map<String, Object> raw = prepareRawKVPMap();

            Map<String, Object> parsed = parseKvp(raw);

            GetFeatureType req = WfsFactory.eINSTANCE.createGetFeatureType();
            Object read = reader.read(req, parsed, raw);
            GetFeatureType parsedReq = (GetFeatureType) read;
            checkXMLResults(parsedReq);
        } finally {
            Dispatcher.REQUEST.set(null);
        }
    }

    private Map<String, Object> prepareRawKVPMap() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("service", "WFS");
        raw.put("version", "1.1.0");
        raw.put("request", "GetFeature");
        raw.put("typeName", getLayerId(SystemTestData.STREAMS) + "," + getLayerId(SystemTestData.BASIC_POLYGONS));
        raw.put(
                "viewParams",
                "<VP><PS><P n=\"where\">WHERE PERSONS &gt; 1000000</P><P n=\"str\">ABCD</P></PS>"
                        + "<PS><P n=\"where\">WHERE PERSONS &gt; 10</P><P n=\"str\">FOO</P></PS></VP>");
        return raw;
    }

    @Test
    public void testXMLViewParamsMultiCaseInsensitive() throws Exception {
        try {
            Request request = new Request();
            request.setRawKvp(new HashMap<>());
            request.getRawKvp().put("VIEWPARAMSFORMAT", "XML");
            Dispatcher.REQUEST.set(request);
            Map<String, Object> raw = prepareRawKVPMap();

            Map<String, Object> parsed = parseKvp(raw);

            GetFeatureType req = WfsFactory.eINSTANCE.createGetFeatureType();
            Object read = reader.read(req, parsed, raw);
            GetFeatureType parsedReq = (GetFeatureType) read;
            checkXMLResults(parsedReq);
        } finally {
            Dispatcher.REQUEST.set(null);
        }
    }

    private void checkXMLResults(GetFeatureType parsedReq) {
        List viewParams = parsedReq.getViewParams();
        assertEquals(2, viewParams.size());
        @SuppressWarnings("unchecked")
        Map<String, String> vp1 = (Map) viewParams.get(0);
        assertEquals("WHERE PERSONS > 1000000", vp1.get("where"));
        assertEquals("ABCD", vp1.get("str"));
        @SuppressWarnings("unchecked")
        Map<String, String> vp2 = (Map) viewParams.get(1);
        assertEquals("WHERE PERSONS > 10", vp2.get("where"));
        assertEquals("FOO", vp2.get("str"));
    }

    @Test
    public void testXMLViewParamsMultiServiceException() throws Exception {
        ServiceException serviceException = null;
        try {
            Request request = new Request();
            request.setRawKvp(new HashMap<>());
            request.getRawKvp().put("viewParamsFormat", "unknown-format");
            Dispatcher.REQUEST.set(request);
            Map<String, Object> raw = new HashMap<>();
            raw.put("service", "WFS");
            raw.put("version", "1.1.0");
            raw.put("request", "GetFeature");
            raw.put("typeName", getLayerId(SystemTestData.STREAMS) + "," + getLayerId(SystemTestData.BASIC_POLYGONS));
            raw.put(
                    "viewParams",
                    "<VP><PS><P n=\"where\">WHERE PERSONS &gt; 1000000</P><P n=\"str\">ABCD</P></PS>"
                            + "<PS><P n=\"where\">WHERE PERSONS &gt; 10</P><P n=\"str\">FOO</P></PS></VP>");

            parseKvp(raw);
        } catch (ServiceException ex) {
            serviceException = ex;
        } finally {
            Dispatcher.REQUEST.set(null);
        }
        assertNotNull("ServiceException not catched", serviceException);
        assertEquals("viewParamsFormat", serviceException.getLocator());
        assertEquals(ServiceException.INVALID_PARAMETER_VALUE, serviceException.getCode());
    }
}
