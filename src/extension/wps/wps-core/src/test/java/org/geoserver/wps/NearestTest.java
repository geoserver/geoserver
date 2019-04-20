/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.wfs.WFSInfo;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class NearestTest extends WPSTestSupport {

    public static QName STREAMS = new QName(MockData.CITE_URI, "Streams", MockData.CITE_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addVectorLayer(
                STREAMS,
                new HashMap<LayerProperty, Object>(),
                "Streams.properties",
                MockData.class,
                getCatalog());
    }

    @Before
    public void oneTimeSetUp() throws Exception {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        wfs.setFeatureBounding(true);
        getGeoServer().save(wfs);
    }

    @Test
    public void testFeatureCollectionInline4326Raw() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>gs:Nearest</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>features</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData>"
                        + readFileIntoString("nearest-FeatureCollection-4326.xml")
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>point</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData mimeType=\"application/wkt\">"
                        + "<![CDATA[POINT(-76.248 36.777)]]>"
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:RawDataOutput mimeType=\"text/XML\" schema=\"http://schemas.opengis.net/gml/2.1.2/feature.xsd\">"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:RawDataOutput>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";

        Document d = postAsDOM("wps", xml);
        // print(d);
        // checkValidationErrors(d);

        assertEquals("wfs:FeatureCollection", d.getDocumentElement().getNodeName());
        assertXpathExists("/wfs:FeatureCollection/gml:featureMember", d);
    }

    @Test
    public void testFeatureCollectionInline4326Doc() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>gs:Nearest</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>features</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData>"
                        + readFileIntoString("nearest-FeatureCollection-4326.xml")
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>point</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData mimeType=\"application/wkt\">"
                        + "<![CDATA[POINT(-76.248 36.777)]]>"
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:ResponseDocument storeExecuteResponse='false'>"
                        + "<wps:Output>"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:Output>"
                        + "</wps:ResponseDocument>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";

        Document d = postAsDOM("wps", xml);
        // print(d);
        // checkValidationErrors(d);

        assertEquals("wps:ExecuteResponse", d.getDocumentElement().getNodeName());
        assertXpathExists(
                "/wps:ExecuteResponse/wps:ProcessOutputs/wps:Output/wps:Data/wps:ComplexData/wfs:FeatureCollection/gml:featureMember",
                d);
    }

    @Test
    public void testFeatureCollectionInline3338Raw() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>gs:Nearest</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>features</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData>"
                        + readFileIntoString("nearest-FeatureCollection-3338.xml")
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>crs</ows:Identifier>"
                        + "<wps:Data><wps:LiteralData>EPSG:3338</wps:LiteralData></wps:Data>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>point</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData mimeType=\"application/wkt\">"
                        + "<![CDATA[POINT(445492.82 1369133.56)]]>"
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:RawDataOutput mimeType=\"text/XML\" schema=\"http://schemas.opengis.net/gml/2.1.2/feature.xsd\">"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:RawDataOutput>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";

        Document d = postAsDOM("wps", xml);
        //        print(d);
        // checkValidationErrors(d);

        assertEquals("wfs:FeatureCollection", d.getDocumentElement().getNodeName());
        assertXpathExists("/wfs:FeatureCollection/gml:featureMember", d);
    }

    @Test
    public void testFeatureCollectionInline3338Doc() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>gs:Nearest</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>features</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData>"
                        + readFileIntoString("nearest-FeatureCollection-3338.xml")
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>crs</ows:Identifier>"
                        + "<wps:Data><wps:LiteralData>EPSG:3338</wps:LiteralData></wps:Data>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>point</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData mimeType=\"application/wkt\">"
                        + "<![CDATA[POINT(445492.82 1369133.56)]]>"
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:ResponseDocument storeExecuteResponse='false'>"
                        + "<wps:Output>"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:Output>"
                        + "</wps:ResponseDocument>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";

        Document d = postAsDOM("wps", xml);
        //        print(d);
        // checkValidationErrors(d);

        assertEquals("wps:ExecuteResponse", d.getDocumentElement().getNodeName());
        assertXpathExists(
                "/wps:ExecuteResponse/wps:ProcessOutputs/wps:Output/wps:Data/wps:ComplexData/wfs:FeatureCollection/gml:featureMember",
                d);
    }

    @Test
    public void testFeatureCollectionInternalWFSRaw() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1' xmlns:xlink=\"http://www.w3.org/1999/xlink\" >"
                        + "<ows:Identifier>gs:Nearest</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>features</ows:Identifier>"
                        + "<wps:Reference schema=\"http://schemas.opengis.net/gml/2.1.2/feature.xsd\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">"
                        + "<wps:Body>"
                        + "<wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\" "
                        + "xmlns:cite=\"http://www.opengis.net/cite\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" >"
                        + "<wfs:Query typeName=\"cite:Streams\" />"
                        + "</wfs:GetFeature>"
                        + "</wps:Body>"
                        + "</wps:Reference>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>point</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData mimeType=\"application/wkt\">"
                        + "<![CDATA[POINT(-4.2E-4 0.003)]]>"
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:RawDataOutput mimeType=\"text/XML\" schema=\"http://schemas.opengis.net/gml/2.1.2/feature.xsd\">"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:RawDataOutput>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";

        Document d = postAsDOM("wps", xml);
        //        print(d);
        // checkValidationErrors(d);

        assertEquals("wfs:FeatureCollection", d.getDocumentElement().getNodeName());
        assertXpathExists("/wfs:FeatureCollection/gml:featureMember", d);
    }

    @Test
    public void testFeatureCollectionWFSFilter1Raw() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1' xmlns:xlink=\"http://www.w3.org/1999/xlink\" >"
                        + "<ows:Identifier>gs:Nearest</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>features</ows:Identifier>"
                        + "<wps:Reference schema=\"http://schemas.opengis.net/gml/2.1.2/feature.xsd\" xlink:href=\"http://geoserver/wfs\"  method=\"POST\">"
                        + "<wps:Body>"
                        + "<wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\" "
                        + "xmlns:cite=\"http://www.opengis.net/cite\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\"> "
                        + "<wfs:Query typeName=\"cite:Streams\">"
                        + "<wfs:PropertyName>cite:the_geom</wfs:PropertyName>"
                        + "<wfs:PropertyName>cite:NAME</wfs:PropertyName>"
                        + "<ogc:Filter>"
                        + "<ogc:PropertyIsEqualTo>"
                        + "<ogc:PropertyName>feature:NAME</ogc:PropertyName>"
                        + "<ogc:Literal>Cam Stream</ogc:Literal>"
                        + "</ogc:PropertyIsEqualTo>"
                        + "</ogc:Filter>"
                        + "</wfs:Query>"
                        + "</wfs:GetFeature>"
                        + "</wps:Body>"
                        + "</wps:Reference>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>point</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData mimeType=\"application/wkt\">"
                        + "<![CDATA[POINT(-4.2E-4 0.003)]]>"
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:RawDataOutput mimeType=\"text/XML\" schema=\"http://schemas.opengis.net/gml/2.1.2/feature.xsd\">"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:RawDataOutput>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";

        Document d = postAsDOM("wps", xml);
        //        print(d);
        // checkValidationErrors(d);

        assertEquals("wfs:FeatureCollection", d.getDocumentElement().getNodeName());
        assertXpathExists("/wfs:FeatureCollection/gml:featureMember", d);
    }

    @Test
    public void testFeatureCollectionWFSFilter2Raw() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1' xmlns:xlink=\"http://www.w3.org/1999/xlink\" >"
                        + "<ows:Identifier>gs:Nearest</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>features</ows:Identifier>"
                        + "<wps:Reference schema=\"http://schemas.opengis.net/gml/2.1.2/feature.xsd\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">"
                        + "<wps:Body>"
                        + "<wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\" "
                        + "xmlns:cite=\"http://www.opengis.net/cite\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\">"
                        + "<wfs:Query typeName=\"cite:Streams\">"
                        + "<wfs:PropertyName>cite:the_geom</wfs:PropertyName>"
                        + "<wfs:PropertyName>cite:NAME</wfs:PropertyName>"
                        + "<ogc:Filter>"
                        + "<ogc:DWithin>"
                        + "<ogc:PropertyName>cite:the_geom</ogc:PropertyName>"
                        + "<gml:Point><gml:coordinates>-4.2E-4,0.003</gml:coordinates></gml:Point>"
                        + "<ogc:Distance units=\"degrees\">1.0</ogc:Distance>"
                        + "</ogc:DWithin>"
                        + "</ogc:Filter>"
                        + "</wfs:Query>"
                        + "</wfs:GetFeature>"
                        + "</wps:Body>"
                        + "</wps:Reference>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>point</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData mimeType=\"application/wkt\">"
                        + "<![CDATA[POINT(-4.2E-4 0.003)]]>"
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:RawDataOutput mimeType=\"text/XML\" schema=\"http://schemas.opengis.net/gml/2.1.2/feature.xsd\">"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:RawDataOutput>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";

        Document d = postAsDOM("wps", xml);
        //        print(d);
        // checkValidationErrors(d);

        assertEquals("wfs:FeatureCollection", d.getDocumentElement().getNodeName());
        assertXpathExists("/wfs:FeatureCollection/gml:featureMember", d);
    }

    @Test
    public void testMissingFeatures() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>gs:Nearest</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>crs</ows:Identifier>"
                        + "<wps:Data><wps:LiteralData>EPSG:3338</wps:LiteralData></wps:Data>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>point</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData mimeType=\"application/wkt\">"
                        + "<![CDATA[POINT(445492.82,1369133.56)]]>"
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:RawDataOutput mimeType=\"text/XML\" schema=\"http://schemas.opengis.net/gml/2.1.2/feature.xsd\">"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:RawDataOutput>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";

        Document d = postAsDOM("wps", xml);
        //        print(d);

        assertXpathExists("//wps:Status/wps:ProcessFailed", d);
    }

    @Test
    public void testMissingPoint() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>gs:Nearest</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>features</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData>"
                        + readFileIntoString("nearest-FeatureCollection-4326.xml")
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:RawDataOutput mimeType=\"text/XML\" schema=\"http://schemas.opengis.net/gml/2.1.2/feature.xsd\">"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:RawDataOutput>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";

        Document d = postAsDOM("wps", xml);
        //        print(d);

        assertXpathExists("//wps:Status/wps:ProcessFailed", d);
    }

    @Test
    public void testWrongCRS() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>gs:Nearest</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>features</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData>"
                        + readFileIntoString("nearest-FeatureCollection-4326.xml")
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>crs</ows:Identifier>"
                        + "<wps:Data><wps:LiteralData>EPSG:EPSG</wps:LiteralData></wps:Data>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>point</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData mimeType=\"application/wkt\">"
                        + "<![CDATA[POINT(-76.248 36.777)]]>"
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:RawDataOutput mimeType=\"text/XML\" schema=\"http://schemas.opengis.net/gml/2.1.2/feature.xsd\">"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:RawDataOutput>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";

        Document d = postAsDOM("wps", xml);
        //        print(d);

        assertXpathExists("//wps:Status/wps:ProcessFailed", d);
    }
}
