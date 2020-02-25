/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.data.test.MockData.PRIMITIVEGEOFEATURE;

import java.util.HashMap;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

/** @author Alessio Fabiani, GeoSolutions SAS */
public class GetExecutionsTest extends WPSTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addVectorLayer(SystemTestData.PRIMITIVEGEOFEATURE, getCatalog());

        String pgf = PRIMITIVEGEOFEATURE.getLocalPart();
        testData.addVectorLayer(
                new QName("http://foo.org", pgf, "foo"),
                new HashMap<LayerProperty, Object>(),
                pgf + ".properties",
                MockData.class,
                getCatalog());
    }

    @Before
    public void oneTimeSetUp() throws Exception {
        WPSInfo wps = getGeoServer().getService(WPSInfo.class);
        // want at least two asynchronous processes to test concurrency
        wps.setMaxAsynchronousProcesses(Math.max(2, wps.getMaxAsynchronousProcesses()));
        getGeoServer().save(wps);
    }

    @Before
    public void setUpInternal() throws Exception {
        // make extra sure we don't have anything else going
        MonkeyProcess.clearCommands();
    }

    /**
     * Tests a process execution with a BoudingBox as the output and check internal layer request
     * handling as well
     */
    @Test
    public void testGetExecutionsRequest() throws Exception {
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:Bounds</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>features</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\">\n"
                        + "            <wfs:Query typeName=\"cite:Streams\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput>\n"
                        + "      <ows:Identifier>bounds</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        Document dom = postAsDOM(root(), request);
        assertXpathEvaluatesTo("-4.0E-4 -0.0024", "/ows:BoundingBox/ows:LowerCorner", dom);
        assertXpathEvaluatesTo("0.0036 0.0024", "/ows:BoundingBox/ows:UpperCorner", dom);

        // Anonymous users do not have access to the executions list
        dom = getAsDOM(root() + "service=wps&version=1.0.0&request=GetExecutions");
        // print(dom);
        assertXpathEvaluatesTo(
                "No Process Execution available.",
                "/ows:ExceptionReport/ows:Exception/ows:ExceptionText",
                dom);

        // As an Admin I have access to the whole executions list
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        dom = getAsDOM(root() + "service=wps&version=1.0.0&request=GetExecutions");
        // print(dom);
        assertXpathEvaluatesTo("1", "/wps:GetExecutionsResponse/@count", dom);
        assertXpathEvaluatesTo(
                "gs:Bounds",
                "/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Process/ows:Identifier",
                dom);
        assertXpathEvaluatesTo(
                "gs:Bounds",
                "/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Status/wps:Identifier",
                dom);
        assertXpathEvaluatesTo(
                "SUCCEEDED",
                "/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Status/wps:Status",
                dom);
        assertXpathEvaluatesTo(
                "100.0",
                "/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Status/wps:PercentCompleted",
                dom);

        // As a system user I can access only my own processes
        login("afabiani", "geosolutions", "ROLE_AUTHENITCATED");
        dom = postAsDOM(root(), request);
        // print(dom);
        dom = getAsDOM(root() + "service=wps&version=1.0.0&request=GetExecutions");
        // print(dom);
        assertXpathEvaluatesTo("1", "/wps:GetExecutionsResponse/@count", dom);
        assertXpathEvaluatesTo(
                "gs:Bounds",
                "/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Process/ows:Identifier",
                dom);
        assertXpathEvaluatesTo(
                "afabiani",
                "/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Status/wps:Owner",
                dom);

        // Again as an Admin I have access to the whole executions list
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        dom = getAsDOM(root() + "service=wps&version=1.0.0&request=GetExecutions");
        // print(dom);
        assertXpathEvaluatesTo("2", "/wps:GetExecutionsResponse/@count", dom);

        // Unless I filter out only the ones belonging to some user
        dom = getAsDOM(root() + "service=wps&version=1.0.0&request=GetExecutions&owner=afabiani");
        // print(dom);
        assertXpathEvaluatesTo("1", "/wps:GetExecutionsResponse/@count", dom);

        // Let's do some simple pagination tests now...
        for (int i = 0; i < 3; i++) {
            dom = postAsDOM(root(), request);
        }
        dom = getAsDOM(root() + "service=wps&version=1.0.0&request=GetExecutions");
        assertXpathEvaluatesTo("5", "/wps:GetExecutionsResponse/@count", dom);

        dom = getAsDOM(root() + "service=wps&version=1.0.0&request=GetExecutions&maxFeatures=2");
        // print(dom);
        assertXpathEvaluatesTo("5", "/wps:GetExecutionsResponse/@count", dom);
        assertXpathEvaluatesTo("", "/wps:GetExecutionsResponse/@previous", dom);
        assertXpathEvaluatesTo(
                "/ows?service=wps&version=1.0.0&request=GetExecutions&maxFeatures=2&startIndex=2",
                "/wps:GetExecutionsResponse/@next",
                dom);

        dom =
                getAsDOM(
                        root()
                                + "service=wps&version=1.0.0&request=GetExecutions&startIndex=1&maxFeatures=2");
        // print(dom);
        assertXpathEvaluatesTo("5", "/wps:GetExecutionsResponse/@count", dom);
        assertXpathEvaluatesTo(
                "/ows?service=wps&version=1.0.0&request=GetExecutions&maxFeatures=2&startIndex=0",
                "/wps:GetExecutionsResponse/@previous",
                dom);
        assertXpathEvaluatesTo(
                "/ows?service=wps&version=1.0.0&request=GetExecutions&maxFeatures=2&startIndex=3",
                "/wps:GetExecutionsResponse/@next",
                dom);

        dom =
                getAsDOM(
                        root()
                                + "service=wps&version=1.0.0&request=GetExecutions&startIndex=3&maxFeatures=2");
        // print(dom);
        assertXpathEvaluatesTo("5", "/wps:GetExecutionsResponse/@count", dom);
        assertXpathEvaluatesTo(
                "/ows?service=wps&version=1.0.0&request=GetExecutions&maxFeatures=2&startIndex=1",
                "/wps:GetExecutionsResponse/@previous",
                dom);
        assertXpathEvaluatesTo("", "/wps:GetExecutionsResponse/@next", dom);

        /** Tests a process executions representation along with their inputs parameters */
        request =
                "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:RectangularClip</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>features</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\">\n"
                        + "            <wfs:Query typeName=\"cite:Streams\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>clip</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:BoundingBoxData crs=\"EPSG:4326\" dimensions=\"2\">\n"
                        + "          <ows:LowerCorner>-4.0E-4 -0.0024</ows:LowerCorner>\n"
                        + "          <ows:UpperCorner>0.0036 0.0024</ows:UpperCorner>\n"
                        + "        </wps:BoundingBoxData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>preserveZ</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>False</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:ResponseDocument lineage=\"true\" storeExecuteResponse=\"true\" status=\"true\">\n"
                        + "      <wps:Output asReference=\"false\">\n"
                        + "        <ows:Identifier>result</ows:Identifier>\n"
                        + "      </wps:Output>\n"
                        + "    </wps:ResponseDocument>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        dom = postAsDOM(root(), request);
        assertXpathEvaluatesTo(
                "Process accepted.", "/wps:ExecuteResponse/wps:Status/wps:ProcessAccepted", dom);

        // As an Admin I have access to the whole executions list
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        dom = getAsDOM(root() + "service=wps&version=1.0.0&request=GetExecutions");
        // print(dom);
        assertXpathEvaluatesTo("6", "/wps:GetExecutionsResponse/@count", dom);

        dom =
                getAsDOM(
                        root()
                                + "service=wps&version=1.0.0&request=GetExecutions&identifier=gs:RectangularClip");
        // print(dom);
        assertXpathEvaluatesTo("1", "/wps:GetExecutionsResponse/@count", dom);
        assertXpathEvaluatesTo(
                "3",
                "count(/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Status/wps:DataInputs/wps:Input)",
                dom);
        assertXpathEvaluatesTo(
                "1",
                "count(/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Status/wps:DataInputs/wps:Input[ows:Identifier='features'])",
                dom);
        assertXpathEvaluatesTo(
                "1",
                "count(/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Status/wps:DataInputs/wps:Input[ows:Identifier='clip'])",
                dom);
        assertXpathEvaluatesTo(
                "1",
                "count(/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Status/wps:DataInputs/wps:Input[ows:Identifier='preserveZ'])",
                dom);
        assertXpathEvaluatesTo(
                "-4.0E-4 -0.00240.0036 0.0024",
                "/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Status/wps:DataInputs/wps:Input[ows:Identifier='clip']/wps:BoundingBoxData",
                dom);
        assertXpathEvaluatesTo(
                "-4.0E-4 -0.0024",
                "/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Status/wps:DataInputs/wps:Input[ows:Identifier='clip']/wps:BoundingBoxData/ows:LowerCorner",
                dom);
        assertXpathEvaluatesTo(
                "0.0036 0.0024",
                "/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Status/wps:DataInputs/wps:Input[ows:Identifier='clip']/wps:BoundingBoxData/ows:UpperCorner",
                dom);
        assertXpathEvaluatesTo(
                "False",
                "/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Status/wps:DataInputs/wps:Input[ows:Identifier='preserveZ']/wps:Data/wps:LiteralData",
                dom);

        request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:Clip</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>features</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\" xmlns:geonode=\"http://www.geonode.org/\">\n"
                        + "            <wfs:Query typeName=\"geonode:san_andres_y_providencia_administrative\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>clip</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:ComplexData mimeType=\"application/json\"><![CDATA[{\"type\":\"MultiLineString\",\"coordinates\":[[[-81.8254,12.199],[-81.8162,12.1827],[-81.812,12.1653],[-81.8156,12.1465],[-81.8269,12.1321],[-81.8433,12.123],[-81.8614,12.119],[-81.8795,12.1232],[-81.8953,12.1336],[-81.9049,12.1494],[-81.9087,12.1673],[-81.9054,12.1864],[-81.8938,12.2004],[-81.8795,12.2089],[-81.8593,12.2136],[-81.8399,12.2096],[-81.8254,12.199]],[[-81.6565,12.635],[-81.6808,12.6391],[-81.7085,12.6262],[-81.739,12.6046],[-81.7611,12.5775],[-81.775,12.5397],[-81.7708,12.5207],[-81.7667,12.4971],[-81.7701,12.4748],[-81.7646,12.4504],[-81.739,12.4369],[-81.7022,12.4389],[-81.6835,12.4578],[-81.6794,12.4883],[-81.6676,12.5153],[-81.651,12.541],[-81.66,12.5552],[-81.6489,12.5762],[-81.6274,12.5931],[-81.6309,12.6181],[-81.6565,12.635]],[[-81.2954,13.3496],[-81.3004,13.3132],[-81.3143,13.29],[-81.3413,13.2755],[-81.3731,13.2674],[-81.4058,13.2657],[-81.4335,13.2633],[-81.4531,13.2771],[-81.4574,13.3079],[-81.4663,13.3257],[-81.463,13.3476],[-81.447,13.3674],[-81.4228,13.3879],[-81.412,13.4126],[-81.403,13.4375],[-81.391,13.4582],[-81.3674,13.4687],[-81.3503,13.4574],[-81.3205,13.448],[-81.2941,13.4177],[-81.2846,13.3878],[-81.2954,13.3496]],[[-79.9333,14.9856],[-79.9333,15.5028]]]}]]></wps:ComplexData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:ResponseDocument lineage=\"true\" storeExecuteResponse=\"true\" status=\"true\">\n"
                        + "      <wps:Output asReference=\"false\">\n"
                        + "        <ows:Identifier>result</ows:Identifier>\n"
                        + "      </wps:Output>\n"
                        + "    </wps:ResponseDocument>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        dom = postAsDOM(root(), request);
        assertXpathEvaluatesTo(
                "Process accepted.", "/wps:ExecuteResponse/wps:Status/wps:ProcessAccepted", dom);

        // As an Admin I have access to the whole executions list
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        dom = getAsDOM(root() + "service=wps&version=1.0.0&request=GetExecutions");
        // print(dom);
        assertXpathEvaluatesTo("7", "/wps:GetExecutionsResponse/@count", dom);

        dom =
                getAsDOM(
                        root()
                                + "service=wps&version=1.0.0&request=GetExecutions&identifier=gs:Clip");
        // print(dom);
        assertXpathEvaluatesTo("1", "/wps:GetExecutionsResponse/@count", dom);
        assertXpathEvaluatesTo(
                "<![CDATA[{\"type\":\"MultiLineString\",\"coordinates\":[[[-81.8254,12.199],[-81.8162,12.1827],[-81.812,12.1653],[-81.8156,12.1465],[-81.8269,12.1321],[-81.8433,12.123],[-81.8614,12.119],[-81.8795,12.1232],[-81.8953,12.1336],[-81.9049,12.1494],[-81.9087,12.1673],[-81.9054,12.1864],[-81.8938,12.2004],[-81.8795,12.2089],[-81.8593,12.2136],[-81.8399,12.2096],[-81.8254,12.199]],[[-81.6565,12.635],[-81.6808,12.6391],[-81.7085,12.6262],[-81.739,12.6046],[-81.7611,12.5775],[-81.775,12.5397],[-81.7708,12.5207],[-81.7667,12.4971],[-81.7701,12.4748],[-81.7646,12.4504],[-81.739,12.4369],[-81.7022,12.4389],[-81.6835,12.4578],[-81.6794,12.4883],[-81.6676,12.5153],[-81.651,12.541],[-81.66,12.5552],[-81.6489,12.5762],[-81.6274,12.5931],[-81.6309,12.6181],[-81.6565,12.635]],[[-81.2954,13.3496],[-81.3004,13.3132],[-81.3143,13.29],[-81.3413,13.2755],[-81.3731,13.2674],[-81.4058,13.2657],[-81.4335,13.2633],[-81.4531,13.2771],[-81.4574,13.3079],[-81.4663,13.3257],[-81.463,13.3476],[-81.447,13.3674],[-81.4228,13.3879],[-81.412,13.4126],[-81.403,13.4375],[-81.391,13.4582],[-81.3674,13.4687],[-81.3503,13.4574],[-81.3205,13.448],[-81.2941,13.4177],[-81.2846,13.3878],[-81.2954,13.3496]],[[-79.9333,14.9856],[-79.9333,15.5028]]]}]]",
                "/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Status/wps:DataInputs/wps:Input[ows:Identifier='clip']/wps:Data/wps:ComplexData",
                dom);

        request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>JTS:convexHull</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>geom</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"application/wkt\" xlink:href=\"http://www.pippo.it\" method=\"GET\"/>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:ResponseDocument lineage=\"true\" storeExecuteResponse=\"true\" status=\"true\">\n"
                        + "      <wps:Output asReference=\"false\">\n"
                        + "        <ows:Identifier>result</ows:Identifier>\n"
                        + "      </wps:Output>\n"
                        + "    </wps:ResponseDocument>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        dom = postAsDOM(root(), request);
        assertXpathEvaluatesTo(
                "Process accepted.", "/wps:ExecuteResponse/wps:Status/wps:ProcessAccepted", dom);

        // As an Admin I have access to the whole executions list
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        dom = getAsDOM(root() + "service=wps&version=1.0.0&request=GetExecutions");
        // print(dom);
        assertXpathEvaluatesTo("8", "/wps:GetExecutionsResponse/@count", dom);

        dom =
                getAsDOM(
                        root()
                                + "service=wps&version=1.0.0&request=GetExecutions&identifier=JTS:convexHull");
        // print(dom);
        assertXpathEvaluatesTo("1", "/wps:GetExecutionsResponse/@count", dom);
        assertXpathEvaluatesTo(
                "http://www.pippo.it",
                "/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Status/wps:DataInputs/wps:Input[ows:Identifier='geom']/wps:Reference/@xlink:href",
                dom);
    }
}
