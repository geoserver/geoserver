/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.URL;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.feature.FeatureCollection;
import org.geotools.wfs.v1_0.WFSConfiguration_1_0;
import org.geotools.xsd.Parser;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class ClipProcessTest extends WPSTestSupport {

    @Override
    protected String getFeatureNamespace() {
        return "http://geoserver.org/iau";
    }

    @Test
    public void testClipRectangle() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>gs:RectangularClip</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>features</ows:Identifier>"
                        + "<wps:Data>"
                        + "<wps:ComplexData>"
                        + readFileIntoString("illinois.xml")
                        + "</wps:ComplexData>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>clip</ows:Identifier>"
                        + "<wps:Data>"
                        + "<ows:BoundingBox>"
                        + "<ows:LowerCorner>-100 40</ows:LowerCorner>"
                        + "<ows:UpperCorner>-90 45</ows:UpperCorner>"
                        + "</ows:BoundingBox>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:RawDataOutput>"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:RawDataOutput>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";

        MockHttpServletResponse response = postAsServletResponse(root(), xml);
        // System.out.println(response.getOutputStreamContent());

        Parser p = new Parser(new WFSConfiguration_1_0());
        FeatureCollectionType fct =
                (FeatureCollectionType)
                        p.parse(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        FeatureCollection fc = (FeatureCollection) fct.getFeature().get(0);

        assertEquals(1, fc.size());
        SimpleFeature sf = (SimpleFeature) fc.features().next();
        Geometry simplified = ((Geometry) sf.getDefaultGeometry());
        // should have been clipped to this specific area
        assertTrue(new Envelope(-100, -90, 40, 45).contains(simplified.getEnvelopeInternal()));
    }

    @Test
    public void testClipIAU() throws Exception {
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1' "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:xlink=\"http://www.w3.org/1999/xlink\">"
                        + "<ows:Identifier>gs:RectangularClip</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>features</ows:Identifier>"
                        + "  <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                        + "    <wps:Body>\n"
                        + "      <wfs:GetFeature service=\"WFS\" version=\"1.0.0\">\n"
                        + "        <wfs:Query typeName=\"iau:MarsPoi\"/>\n"
                        + "      </wfs:GetFeature>\n"
                        + "    </wps:Body>\n"
                        + "  </wps:Reference>\n"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>clip</ows:Identifier>"
                        + "<wps:Data>"
                        + "<ows:BoundingBox crs=\"IAU:49900\">"
                        + "<ows:LowerCorner>-37 -40</ows:LowerCorner>"
                        + "<ows:UpperCorner>-36 -20</ows:UpperCorner>"
                        + "</ows:BoundingBox>"
                        + "</wps:Data>"
                        + "</wps:Input>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:ResponseDocument storeExecuteResponse='false' lineage='true'>"
                        + "<wps:Output>"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:Output>"
                        + "</wps:ResponseDocument>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";

        Document dom = postAsDOM(root(), xml);
        print(dom);

        // check clip
        assertEquals("2", xp.evaluate("count(//feature:MarsPoi)", dom));
        assertEquals("Blunck", xp.evaluate("(//feature:MarsPoi/gml:name)[1]", dom));
        assertEquals("Martynov", xp.evaluate("(//feature:MarsPoi/gml:name)[2]", dom));

        // check crs
        assertEquals(
                "http://www.opengis.net/gml/srs/iau.xml#49900",
                xp.evaluate(
                        "/wps:ExecuteResponse/wps:ProcessOutputs/wps:Output/wps:Data/wps:ComplexData/wfs:FeatureCollection/gml:boundedBy/gml:Box/@srsName",
                        dom));

        // check SRS appears in lineage

    }

    /**
     * Checking WFS 1.1 feature collections with CRS in IAU authority can be parsed correctly
     *
     * @throws Exception
     */
    @Test
    public void testFeatureCollectionIAU() throws Exception {
        // setRemoteInputDisabled(false);
        URL collectionURL = getClass().getResource("../mars-poi-FeatureCollection.xml");
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>gs:RectangularClip</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "<wps:Input>"
                        + "<ows:Identifier>features</ows:Identifier>"
                        + "  <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.1\" "
                        + "xlink:href=\""
                        + collectionURL.toExternalForm() // data in north/east
                        + "\"/>\n"
                        + "</wps:Input>"
                        + "<wps:Input>"
                        + "<ows:Identifier>clip</ows:Identifier>"
                        + "<wps:Data>"
                        + "<ows:BoundingBox crs=\"IAU:49900\">" // this one in east/north
                        + "<ows:LowerCorner>-37 -28</ows:LowerCorner>"
                        + "<ows:UpperCorner>-35 -26</ows:UpperCorner>"
                        + "</ows:BoundingBox>"
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

        // System.out.println(postAsServletResponse("wps", xml).getOutputStreamContent());

        Document d = postAsDOM("wps", xml);
        print(d);
        checkValidationErrors(d);

        assertEquals("wps:ExecuteResponse", d.getDocumentElement().getNodeName());

        // worked without balking at the IAU codes
        assertXpathExists("/wps:ExecuteResponse/wps:Status/wps:ProcessSucceeded", d);
        assertXpathExists(
                "/wps:ExecuteResponse/wps:ProcessOutputs/wps:Output/wps:Data/wps:ComplexData/wfs:FeatureCollection",
                d);

        // only Blunck survived
        assertXpathEvaluatesTo("1", "count(//gml:featureMember)", d);
        assertXpathEvaluatesTo("Blunck", "//gml:featureMember//gml:name", d);
    }

    @Override
    protected String getLogConfiguration() {
        return "DEFAULT_LOGGING";
    }
}
