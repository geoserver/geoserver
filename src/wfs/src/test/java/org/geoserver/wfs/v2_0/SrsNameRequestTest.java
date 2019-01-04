/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.v2_0;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test that srsName and bbox in a WFS request results in the expected features, srsName, and axis
 * order in the response.
 *
 * @author Ben Caradoc-Davies (CSIRO Earth Science and Resource Engineering)
 */
public class SrsNameRequestTest extends WFS20TestSupport {

    /** WGS 84 as an EPSG code. */
    private static final String EPSG_CODE_SRSNAME = "EPSG:4326";

    /** WGS 84 as an OGC HTTP URL. */
    private static final String HTTP_URL_SRSNAME = "http://www.opengis.net/gml/srs/epsg.xml#4326";

    /** WGS 84 as an OGC URN Experimental. */
    private static final String URN_EXPERIMENTAL_SRSNAME = "urn:x-ogc:def:crs:EPSG::4326";

    /** WGS 84 as an OGC URN. */
    private static final String URN_SRSNAME = "urn:ogc:def:crs:EPSG::4326";

    /** WGS 84 as an OGC HTTP URI. */
    private static final String HTTP_URI_SRSNAME = "http://www.opengis.net/def/crs/EPSG/0/4326";

    /** The name of the feature type under test. */
    private static final String TYPE_NAME = "sf:PrimitiveGeoFeature";

    /** The gml:id of PrimitiveGeoFeature.f015. */
    private static final String GML_ID = "PrimitiveGeoFeature.f015";

    /** The bbox request parameter matching PrimitiveGeoFeature.f015 in longitude/latitude order. */
    private static final String LON_LAT_BBOX = "34.939,-10.521,34.941,-10.519";

    /** The bbox request parameter matching PrimitiveGeoFeature.f015 in latitude/longitude order. */
    private static final String LAT_LON_BBOX = "-10.521,34.939,-10.519,34.941";

    /** The pointProperty data of PrimitiveGeoFeature.f015 in longitude/latitude order. */
    private static final String LON_LAT_DATA = "34.94 -10.52";

    /** The pointProperty data of PrimitiveGeoFeature.f015 in latitude/longitude order. */
    private static final String LAT_LON_DATA = "-10.52 34.94";

    /** The XPath for the numberReturned of the response root element. */
    private static final String NUMBER_RETURNED_XPATH = "//wfs:FeatureCollection/@numberReturned";

    /** The XPath for the gml:id of PrimitiveGeoFeature.f015. */
    private static final String GML_ID_XPATH =
            "//"
                    + TYPE_NAME //
                    + "[@gml:id=\""
                    + GML_ID
                    + "\"]/@gml:id";

    /** The XPath for the pointProperty data of PrimitiveGeoFeature.f015. */
    private static final String DATA_XPATH =
            "//"
                    + TYPE_NAME //
                    + "[@gml:id=\""
                    + GML_ID
                    + "\"]" //
                    + "/sf:pointProperty/gml:Point/gml:pos";

    /** The XPath for the pointProperty srsName of PrimitiveGeoFeature.f015. */
    private static final String SRSNAME_XPATH =
            "//"
                    + TYPE_NAME //
                    + "[@gml:id=\""
                    + GML_ID
                    + "\"]" //
                    + "/sf:pointProperty/gml:Point/@srsName";

    /**
     * Test that, for a given request srsName and bbox, the expected number of features are
     * returned, the response contains the expected feature PrimitiveGeoFeature.f015, and the
     * pointProperty of PrimitiveGeoFeature.f015 in the response has the expected srsName and data
     * axis order (longitude/latitude or latitude/Longitude).
     *
     * @param requestSrsName the srsName request parameter
     * @param requestBbox the bbox request parameter or null if none
     * @param expectedNumberReturned the expected number of features returned in the response
     * @param expectedSrsName the expected pointProperty srsName of PrimitiveGeoFeature.f015 in the
     *     response or null if no features are expected
     * @param expectedData the expected pointProperty data of PrimitiveGeoFeature.f015 in the
     *     response or null if no features are expected
     */
    private void runTest(
            String requestSrsName,
            String requestBbox,
            int expectedNumberReturned,
            String expectedSrsName,
            String expectedData)
            throws Exception {
        StringBuilder requestBuilder = new StringBuilder("wfs");
        requestBuilder.append("?version=2.0.0");
        requestBuilder.append("&request=GetFeature");
        requestBuilder.append("&typenames=");
        requestBuilder.append(TYPE_NAME);
        if (requestSrsName != null) {
            requestBuilder.append("&srsname=");
            requestBuilder.append(encodeSrsName(requestSrsName));
        }
        if (requestBbox != null) {
            requestBuilder.append("&bbox=");
            requestBuilder.append(encodeSrsName(requestBbox));
        }
        Document response = getAsDOM(requestBuilder.toString());
        XMLAssert.assertXpathEvaluatesTo(
                Integer.toString(expectedNumberReturned), NUMBER_RETURNED_XPATH, response);
        if (expectedNumberReturned > 0) {
            XMLAssert.assertXpathEvaluatesTo(GML_ID, GML_ID_XPATH, response);
            XMLAssert.assertXpathEvaluatesTo(expectedSrsName, SRSNAME_XPATH, response);
            XMLAssert.assertXpathEvaluatesTo(expectedData, DATA_XPATH, response);
        }
    }

    /**
     * Encode srsName (or a five-parameter bbox) so that it can be used in a GET request. Safe to
     * use (and has no effect on) a four-parameter bbox.
     *
     * <p>This implementation encodes only "#" so HTTP_URL_SRSNAME works. It could do full URL
     * encoding but that would not be representative of common use.
     *
     * @param srsName unencoded srsName (or a five-parameter bbox)
     * @return srsName encoded srsName (or a five-parameter bbox)
     */
    private String encodeSrsName(String srsName) {
        return srsName.replace("#", "%23");
    }

    /**
     * Build a five parameter bbox from a bbox and srsName
     *
     * @param bbox four-parameter bbox
     * @param srsName srsName for the bbox
     * @return five-parameter bbox with encoded srsName
     */
    private String buildBbox(String bbox, String srsName) {
        StringBuilder bboxBuilder = new StringBuilder(bbox);
        bboxBuilder.append(",");
        bboxBuilder.append(srsName);
        return bboxBuilder.toString();
    }

    /**
     * Test response numberReturned, feature identity, srsName, and axis order for a request with an
     * EPSG code srsName.
     */
    @Test
    public void testEpsgCode() throws Exception {
        runTest(EPSG_CODE_SRSNAME, null, 5, HTTP_URL_SRSNAME, LON_LAT_DATA);
    }

    /**
     * Test response numberReturned, feature identity, srsName, and axis order for a request with an
     * EPSG code srsName and a latitude/longitude (correct axis order) native four-parameter
     * bounding box.
     */
    @Test
    public void testEpsgCodeNativeBbox() throws Exception {
        runTest(EPSG_CODE_SRSNAME, LAT_LON_BBOX, 1, HTTP_URL_SRSNAME, LON_LAT_DATA);
    }

    /**
     * Test that a request with an EPSG code srsName and a longitude/latitude (incorrect axis order)
     * native four-parameter bounding box returns no features.
     */
    @Test
    public void testEpsgCodeNativeBboxWrongAxisOrder() throws Exception {
        runTest(EPSG_CODE_SRSNAME, LON_LAT_BBOX, 0, null, null);
    }

    /**
     * Test response numberReturned, feature identity, srsName, and axis order for a request with an
     * EPSG code srsName and a longitude/latitude (correct axis order) five-parameter bounding box.
     */
    @Test
    public void testEpsgCodeBbox() throws Exception {
        runTest(
                EPSG_CODE_SRSNAME,
                buildBbox(LON_LAT_BBOX, EPSG_CODE_SRSNAME),
                1,
                HTTP_URL_SRSNAME,
                LON_LAT_DATA);
    }

    /**
     * Test that a request with an EPSG code srsName and a latitude/longitude (incorrect axis order)
     * five-parameter bounding box returns no features.
     */
    @Test
    public void testEpsgCodeBboxWrongAxisOrder() throws Exception {
        runTest(EPSG_CODE_SRSNAME, buildBbox(LAT_LON_BBOX, EPSG_CODE_SRSNAME), 0, null, null);
    }

    /**
     * Test response numberReturned, feature identity, srsName, and axis order for a request with an
     * OGC HTTP URL srsName.
     */
    @Test
    public void testHttpUrl() throws Exception {
        runTest(HTTP_URL_SRSNAME, null, 5, HTTP_URL_SRSNAME, LON_LAT_DATA);
    }

    /**
     * Test response numberReturned, feature identity, srsName, and axis order for a request with an
     * OGC HTTP URL srsName and a latitude/longitude (correct axis order) native four-parameter
     * bounding box.
     */
    @Test
    public void testHttpUrlNativeBbox() throws Exception {
        runTest(HTTP_URL_SRSNAME, LAT_LON_BBOX, 1, HTTP_URL_SRSNAME, LON_LAT_DATA);
    }

    /**
     * Test that a request with an OGC HTTP URL srsName and a longitude/latitude (incorrect axis
     * order) native four-parameter bounding box returns no features.
     */
    @Test
    public void testHttpUrlNativeBboxWrongAxisOrder() throws Exception {
        runTest(HTTP_URL_SRSNAME, LON_LAT_BBOX, 0, null, null);
    }

    /**
     * Test response numberReturned, feature identity, srsName, and axis order for a request with an
     * OGC HTTP URL srsName and a longitude/latitude (correct axis order) five-parameter bounding
     * box.
     */
    @Test
    public void testHttpUrlBbox() throws Exception {
        runTest(
                HTTP_URL_SRSNAME,
                buildBbox(LON_LAT_BBOX, HTTP_URL_SRSNAME),
                1,
                HTTP_URL_SRSNAME,
                LON_LAT_DATA);
    }

    /**
     * Test that a request with an OGC HTTP URL srsName and a latitude/longitude (incorrect axis
     * order) five-parameter bounding box returns no features.
     */
    @Test
    public void testHttpUrlBboxWrongAxisOrder() throws Exception {
        runTest(HTTP_URL_SRSNAME, buildBbox(LAT_LON_BBOX, HTTP_URL_SRSNAME), 0, null, null);
    }

    /**
     * Test response numberReturned, feature identity, srsName, and axis order for a request with an
     * OGC URN Experimental srsName.
     */
    @Test
    public void testUrnExperimental() throws Exception {
        runTest(URN_EXPERIMENTAL_SRSNAME, null, 5, URN_SRSNAME, LAT_LON_DATA);
    }

    /**
     * Test response numberReturned, feature identity, srsName, and axis order for a request with an
     * OGC URN Experimental srsName and a latitude/longitude (correct axis order) native
     * four-parameter bounding box.
     */
    @Test
    public void testUrnExperimentalNativeBbox() throws Exception {
        runTest(URN_EXPERIMENTAL_SRSNAME, LAT_LON_BBOX, 1, URN_SRSNAME, LAT_LON_DATA);
    }

    /**
     * Test that a request with an OGC URN Experimental srsName and a longitude/latitude (incorrect
     * axis order) native four-parameter bounding box returns no features.
     */
    @Test
    public void testUrnExperimentalNativeBboxWrongAxisOrder() throws Exception {
        runTest(URN_EXPERIMENTAL_SRSNAME, LON_LAT_BBOX, 0, null, null);
    }

    /**
     * Test response numberReturned, feature identity, srsName, and axis order for a request with an
     * OGC URN Experimental srsName and a latitude/longitude (correct axis order) five-parameter
     * bounding box.
     */
    @Test
    public void testUrnExperimentalBbox() throws Exception {
        runTest(
                URN_EXPERIMENTAL_SRSNAME,
                buildBbox(LAT_LON_BBOX, URN_EXPERIMENTAL_SRSNAME),
                1,
                URN_SRSNAME,
                LAT_LON_DATA);
    }

    /**
     * Test that a request with an OGC URN Experimental srsName and a longitude/latitude (incorrect
     * axis order) five-parameter bounding box returns no features.
     */
    @Test
    public void testUrnExperimentalBboxWrongAxisOrder() throws Exception {
        runTest(
                URN_EXPERIMENTAL_SRSNAME,
                buildBbox(LON_LAT_BBOX, URN_EXPERIMENTAL_SRSNAME),
                0,
                null,
                null);
    }

    /**
     * Test response numberReturned, feature identity, srsName, and axis order for a request with an
     * OGC URN srsName.
     */
    @Test
    public void testUrn() throws Exception {
        runTest(URN_SRSNAME, null, 5, URN_SRSNAME, LAT_LON_DATA);
    }

    /**
     * Test response numberReturned, feature identity, srsName, and axis order for a request with an
     * OGC URN srsName and a latitude/longitude (correct axis order) native four-parameter bounding
     * box.
     */
    @Test
    public void testUrnNativeBbox() throws Exception {
        runTest(URN_SRSNAME, LAT_LON_BBOX, 1, URN_SRSNAME, LAT_LON_DATA);
    }

    /**
     * Test that a request with an OGC URN srsName and a longitude/latitude (incorrect axis order)
     * native four-parameter bounding box returns no features.
     */
    @Test
    public void testUrnNativeBboxWrongAxisOrder() throws Exception {
        runTest(URN_SRSNAME, LON_LAT_BBOX, 0, null, null);
    }

    /**
     * Test response numberReturned, feature identity, srsName, and axis order for a request with an
     * OGC URN srsName and a latitude/longitude (correct axis order) five-parameter bounding box.
     */
    @Test
    public void testUrnBbox() throws Exception {
        runTest(URN_SRSNAME, buildBbox(LAT_LON_BBOX, URN_SRSNAME), 1, URN_SRSNAME, LAT_LON_DATA);
    }

    /**
     * Test that a request with an OGC URN srsName and a longitude/latitude (incorrect axis order)
     * five-parameter bounding box returns no features.
     */
    @Test
    public void testUrnBboxWrongAxisOrder() throws Exception {
        runTest(URN_SRSNAME, buildBbox(LON_LAT_BBOX, URN_SRSNAME), 0, null, null);
    }

    /**
     * Test response numberReturned, feature identity, srsName, and axis order for a request with an
     * OGC HTTP URI srsName.
     */
    @Test
    public void testHttpUri() throws Exception {
        runTest(HTTP_URI_SRSNAME, null, 5, URN_SRSNAME, LAT_LON_DATA);
    }

    /**
     * Test response numberReturned, feature identity, srsName, and axis order for a request with an
     * OGC HTTP URI srsName and a latitude/longitude (correct axis order) native four-parameter
     * bounding box.
     */
    @Test
    public void testHttpUriNativeBbox() throws Exception {
        runTest(HTTP_URI_SRSNAME, LAT_LON_BBOX, 1, URN_SRSNAME, LAT_LON_DATA);
    }

    /**
     * Test that a request with an OGC HTTP URI srsName and a longitude/latitude (incorrect axis
     * order) native four-parameter bounding box returns no features.
     */
    @Test
    public void testHttpUriNativeBboxWrongAxisOrder() throws Exception {
        runTest(HTTP_URI_SRSNAME, LON_LAT_BBOX, 0, null, null);
    }

    /**
     * Test response numberReturned, feature identity, srsName, and axis order for a request with an
     * OGC HTTP URI srsName and a latitude/longitude (correct axis order) five-parameter bounding
     * box.
     */
    @Test
    public void testHttpUriBbox() throws Exception {
        runTest(
                HTTP_URI_SRSNAME,
                buildBbox(LAT_LON_BBOX, HTTP_URI_SRSNAME),
                1,
                URN_SRSNAME,
                LAT_LON_DATA);
    }

    /**
     * Test that a request with an OGC HTTP URI srsName and a longitude/latitude (incorrect axis
     * order) five-parameter bounding box returns no features.
     */
    @Test
    public void testHttpUriBboxWrongAxisOrder() throws Exception {
        runTest(HTTP_URI_SRSNAME, buildBbox(LON_LAT_BBOX, HTTP_URI_SRSNAME), 0, null, null);
    }
}
