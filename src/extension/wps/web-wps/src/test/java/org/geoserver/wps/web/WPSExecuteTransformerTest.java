/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.transform.dom.DOMSource;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.easymock.EasyMock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wps.web.InputParameterValues.ParameterType;
import org.geoserver.wps.web.InputParameterValues.ParameterValue;
import org.geoserver.wps.xml.WPSConfiguration;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.xsd.Parser;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

public class WPSExecuteTransformerTest extends GeoServerWicketTestSupport {

    @Before
    public void setUpInternal() throws Exception {
        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("wps", "http://www.opengis.net/wps/1.0.0");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("feature", "http://geoserver.sf.net");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

        XMLUnit.setIgnoreWhitespace(true);
    }

    @Test
    public void testSingleProcess() throws Exception {
        ExecuteRequest executeBuffer = getExecuteBuffer();

        WPSExecuteTransformer tx = new WPSExecuteTransformer();
        tx.setIndentation(2);
        String xml = tx.transform(executeBuffer);
        // System.out.println(xml);

        String expected =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" "
                        + "xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                        + "xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>JTS:buffer</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>geom</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:ComplexData mimeType=\"application/wkt\"><![CDATA[POINT(0 0)]]></wps:ComplexData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>distance</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>10</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"text/xml; subtype=gml/3.1.1\">\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        Document test = XMLUnit.buildTestDocument(xml);
        checkValidationErrors(test);
        Document control = XMLUnit.buildControlDocument(expected);

        assertXMLEqual(control, test);
    }

    @Test
    public void testSubprocess() throws Exception {
        Name areaName = new NameImpl("JTS", "area");

        InputParameterValues areaGeomValues = new InputParameterValues(areaName, "geom");
        ParameterValue geom = areaGeomValues.values.get(0);
        geom.setType(ParameterType.SUBPROCESS);
        geom.setValue(getExecuteBuffer());

        OutputParameter bufferOutput = new OutputParameter(areaName, "result");

        ExecuteRequest executeArea =
                new ExecuteRequest(
                        areaName.getURI(),
                        Arrays.asList(areaGeomValues),
                        Arrays.asList(bufferOutput));

        WPSExecuteTransformer tx = new WPSExecuteTransformer();
        tx.setIndentation(2);
        String xml = tx.transform(executeArea);
        // System.out.println(xml);

        String expected =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>JTS:area</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>geom</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=gml/3.1.1\" xlink:href=\"http://geoserver/wps\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wps:Execute version=\"1.0.0\" service=\"WPS\">\n"
                        + "            <ows:Identifier>JTS:buffer</ows:Identifier>\n"
                        + "            <wps:DataInputs>\n"
                        + "              <wps:Input>\n"
                        + "                <ows:Identifier>geom</ows:Identifier>\n"
                        + "                <wps:Data>\n"
                        + "                  <wps:ComplexData mimeType=\"application/wkt\"><![CDATA[POINT(0 0)]]></wps:ComplexData>\n"
                        + "                </wps:Data>\n"
                        + "              </wps:Input>\n"
                        + "              <wps:Input>\n"
                        + "                <ows:Identifier>distance</ows:Identifier>\n"
                        + "                <wps:Data>\n"
                        + "                  <wps:LiteralData>10</wps:LiteralData>\n"
                        + "                </wps:Data>\n"
                        + "              </wps:Input>\n"
                        + "            </wps:DataInputs>\n"
                        + "            <wps:ResponseForm>\n"
                        + "              <wps:RawDataOutput mimeType=\"text/xml; subtype=gml/3.1.1\">\n"
                        + "                <ows:Identifier>result</ows:Identifier>\n"
                        + "              </wps:RawDataOutput>\n"
                        + "            </wps:ResponseForm>\n"
                        + "          </wps:Execute>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput>\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        Document test = XMLUnit.buildTestDocument(xml);
        checkValidationErrors(test);
        Document control = XMLUnit.buildControlDocument(expected);

        assertXMLEqual(control, test);
    }

    @Test
    public void testBoundingBoxEncoding() throws Exception {
        ExecuteRequest executeClipAndShip = getExecuteClipAndShip();
        WPSExecuteTransformer tx = new WPSExecuteTransformer();
        tx.setIndentation(2);
        String xml = tx.transform(executeClipAndShip);
        // System.out.println(xml);
        String expected =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:CropCoverage</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>coverage</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"image/tiff\" xlink:href=\"http://geoserver/wcs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wcs:GetCoverage service=\"WCS\" version=\"1.1.1\">\n"
                        + "            <ows:Identifier>geosolutions:usa</ows:Identifier>\n"
                        + "            <wcs:DomainSubset>\n"
                        + "              <ows:BoundingBox crs=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">\n"
                        + "                <ows:LowerCorner>-180.0 -90.000000000036</ows:LowerCorner>\n"
                        + "                <ows:UpperCorner>180.0 90.0</ows:UpperCorner>\n"
                        + "              </ows:BoundingBox>\n"
                        + "            </wcs:DomainSubset>\n"
                        + "            <wcs:Output format=\"image/tiff\"/>\n"
                        + "          </wcs:GetCoverage>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>cropShape</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=gml/3.1.1\" xlink:href=\"http://geoserver/wps\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wps:Execute version=\"1.0.0\" service=\"WPS\">\n"
                        + "            <ows:Identifier>gs:CollectGeometries</ows:Identifier>\n"
                        + "            <wps:DataInputs>\n"
                        + "              <wps:Input>\n"
                        + "                <ows:Identifier>features</ows:Identifier>\n"
                        + "                <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                        + "                  <wps:Body>\n"
                        + "                    <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                        + "                      <wfs:Query typeName=\"geosolutions:states\"/>\n"
                        + "                    </wfs:GetFeature>\n"
                        + "                  </wps:Body>\n"
                        + "                </wps:Reference>\n"
                        + "              </wps:Input>\n"
                        + "            </wps:DataInputs>\n"
                        + "            <wps:ResponseForm>\n"
                        + "              <wps:RawDataOutput mimeType=\"text/xml; subtype=gml/3.1.1\">\n"
                        + "                <ows:Identifier>result</ows:Identifier>\n"
                        + "              </wps:RawDataOutput>\n"
                        + "            </wps:ResponseForm>\n"
                        + "          </wps:Execute>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"image/tiff\">\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";
        Document test = XMLUnit.buildTestDocument(xml);
        checkValidationErrors(test);
        Document control = XMLUnit.buildControlDocument(expected);
        assertXMLEqual(control, test);
    }

    /*
     * Emulate clip-and-ship example from http://geoserver.geo-solutions.it/edu/en/wps/chaining_processes.html
     */
    private ExecuteRequest getExecuteClipAndShip() throws Exception {
        CoordinateReferenceSystem epsg4326 = CRS.decode("EPSG:4326");

        Name collectGeometriesName = new NameImpl("gs", "CollectGeometries");
        InputParameterValues collectGeometriesFeaturesValues =
                new InputParameterValues(collectGeometriesName, "features");
        VectorLayerConfiguration geosolutionsStates = new VectorLayerConfiguration();
        geosolutionsStates.setLayerName("geosolutions:states");
        ParameterValue cgFeatures = collectGeometriesFeaturesValues.values.get(0);
        cgFeatures.setType(ParameterType.VECTOR_LAYER);
        cgFeatures.setValue(geosolutionsStates);
        OutputParameter collectGeometriesOutput =
                new OutputParameter(collectGeometriesName, "result");
        ExecuteRequest collectGeometriesRequest =
                new ExecuteRequest(
                        collectGeometriesName.getURI(),
                        Arrays.asList(collectGeometriesFeaturesValues),
                        Arrays.asList(collectGeometriesOutput));

        Name clipName = new NameImpl("gs", "CropCoverage");
        InputParameterValues clipFeaturesValues = new InputParameterValues(clipName, "coverage");
        ParameterValue features = clipFeaturesValues.values.get(0);
        features.setType(ParameterType.RASTER_LAYER);
        RasterLayerConfiguration geosolutionsUsa = new RasterLayerConfiguration();
        geosolutionsUsa.setLayerName("geosolutions:usa");
        geosolutionsUsa.setSpatialDomain(
                new ReferencedEnvelope(-180.0, 180, -90.000000000036, 90, epsg4326));
        features.setValue(geosolutionsUsa);

        InputParameterValues clipClipValues = new InputParameterValues(clipName, "cropShape");
        ParameterValue clip = clipClipValues.values.get(0);
        clip.setType(ParameterType.SUBPROCESS);
        clip.setValue(collectGeometriesRequest);

        OutputParameter clipOutput = new OutputParameter(clipName, "result");

        ExecuteRequest executeBuffer =
                new ExecuteRequest(
                        clipName.getURI(),
                        Arrays.asList(clipFeaturesValues, clipClipValues),
                        Arrays.asList(clipOutput));
        return executeBuffer;
    }

    private ExecuteRequest getExecuteBuffer() {
        Name bufferName = new NameImpl("JTS", "buffer");
        InputParameterValues bufferGeomValues = new InputParameterValues(bufferName, "geom");
        ParameterValue geom = bufferGeomValues.values.get(0);
        geom.setMime("application/wkt");
        geom.setType(ParameterType.TEXT);
        geom.setValue("POINT(0 0)");

        InputParameterValues bufferDistanceValues =
                new InputParameterValues(bufferName, "distance");
        ParameterValue distance = bufferDistanceValues.values.get(0);
        distance.setType(ParameterType.LITERAL);
        distance.setValue("10");

        OutputParameter bufferOutput = new OutputParameter(bufferName, "result");

        ExecuteRequest executeBuffer =
                new ExecuteRequest(
                        bufferName.getURI(),
                        Arrays.asList(bufferGeomValues, bufferDistanceValues),
                        Arrays.asList(bufferOutput));
        return executeBuffer;
    }

    /** Validates a document against the */
    protected void checkValidationErrors(Document dom) throws Exception {
        Parser p = new Parser(new WPSConfiguration());
        p.setValidating(true);
        p.parse(new DOMSource(dom));

        if (!p.getValidationErrors().isEmpty()) {
            for (Iterator e = p.getValidationErrors().iterator(); e.hasNext(); ) {
                SAXParseException ex = (SAXParseException) e.next();
                // System.out.println(
                //       ex.getLineNumber() + "," + ex.getColumnNumber() + " -" + ex.toString());
            }
            fail("Document did not validate.");
        }
    }

    @Test
    public void testIncludeNamespaceMapping() throws Exception {
        Name centroidName = new NameImpl("gs", "Centroid");
        InputParameterValues inputValues = new InputParameterValues(centroidName, "features");

        VectorLayerConfiguration layer = new VectorLayerConfiguration();
        layer.setLayerName("foo:myLayer");

        ParameterValue features = inputValues.values.get(0);
        features.setType(ParameterType.VECTOR_LAYER);
        features.setValue(layer);

        OutputParameter output = new OutputParameter(centroidName, "result");

        ExecuteRequest execute =
                new ExecuteRequest(
                        centroidName.getURI(), Arrays.asList(inputValues), Arrays.asList(output));

        NamespaceInfo fooNs = EasyMock.createNiceMock(NamespaceInfo.class);
        expect(fooNs.getURI()).andReturn("http://foo.org");
        replay(fooNs);

        Catalog cat = createNiceMock(Catalog.class);
        expect(cat.getNamespaceByPrefix("foo")).andReturn(fooNs);
        replay(cat);

        WPSExecuteTransformer tx = new WPSExecuteTransformer(cat);
        tx.setIndentation(2);
        String xml = tx.transform(execute);
        assertTrue(xml.contains("xmlns:foo=\"http://foo.org\""));
    }
}
