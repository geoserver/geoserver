/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.feature.FeatureCollection;
import org.junit.Test;
import org.w3c.dom.Document;

public class QueryProcessTest extends WPSTestSupport {

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put("feature", SystemTestData.BUILDINGS.getNamespaceURI());
    }

    @Test
    public void testNoOp() throws Exception {
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:Query</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>features</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" "
                        + "        xlink:href=\"http://geoserver/wfs?request=GetFeatures&amp;service=WFS&amp;version=1.0.0&amp;typeName="
                        + getLayerId(MockData.BUILDINGS)
                        + "\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                        + "            <wfs:Query typeName=\"topp:states\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"text/xml; subtype=wfs-collection/1.0\">\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        Document d = postAsDOM(root(), request);
        // print(d);
        FeatureTypeInfo fti = getCatalog().getFeatureTypeByName(getLayerId(MockData.BUILDINGS));
        FeatureCollection fc = fti.getFeatureSource(null, null).getFeatures();

        XpathEngine xpath = XMLUnit.newXpathEngine();
        // the expected number of features
        assertEquals(
                xpath.getMatchingNodes(
                                "/wfs:FeatureCollection/gml:featureMember/feature:Buildings", d)
                        .getLength(),
                fc.size());
        // the expected number of attributes (+1 for the feature bounds)
        final String base =
                "/wfs:FeatureCollection/gml:featureMember/feature:Buildings[@fid=\"Buildings.1107531701010\"]";
        assertXpathEvaluatesTo("4", "count(" + base + "/*)", d);
        assertXpathEvaluatesTo("1", "count(" + base + "/feature:the_geom)", d);
        assertXpathEvaluatesTo("1", "count(" + base + "/feature:FID)", d);
        assertXpathEvaluatesTo("1", "count(" + base + "/feature:ADDRESS)", d);
    }

    @Test
    public void testFilter() throws Exception {
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:Query</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>features</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" "
                        + "        xlink:href=\"http://geoserver/wfs?request=GetFeatures&amp;service=WFS&amp;version=1.0.0&amp;typeName="
                        + getLayerId(MockData.BUILDINGS)
                        + "\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                        + "            <wfs:Query typeName=\"topp:states\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n" //
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>filter</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:ComplexData mimeType=\"text/plain; subtype=cql\"><![CDATA[FID=113]]></wps:ComplexData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"text/xml; subtype=wfs-collection/1.0\">\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        Document d = postAsDOM(root(), request);
        // print(d);

        // the expected number of features
        assertXpathEvaluatesTo(
                "1", "count(/wfs:FeatureCollection/gml:featureMember/feature:Buildings)", d);
        // the expected number of attributes (+1 for the feature bounds)
        final String base =
                "/wfs:FeatureCollection/gml:featureMember/feature:Buildings[@fid=\"Buildings.1107531701010\"]";
        assertXpathEvaluatesTo("4", "count(" + base + "/*)", d);
        assertXpathEvaluatesTo("1", "count(" + base + "/feature:the_geom)", d);
        assertXpathEvaluatesTo("1", "count(" + base + "/feature:FID)", d);
        assertXpathEvaluatesTo("1", "count(" + base + "/feature:ADDRESS)", d);
    }

    @Test
    public void testFilterOGC() throws Exception {
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:Query</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>features</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" "
                        + "        xlink:href=\"http://geoserver/wfs?request=GetFeatures&amp;service=WFS&amp;version=1.0.0&amp;typeName="
                        + getLayerId(MockData.BUILDINGS)
                        + "\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                        + "            <wfs:Query typeName=\"topp:states\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n" //
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>filter</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:ComplexData mimeType=\"text/xml; subtype=filter/1.0\">\n"
                        + "          <ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
                        + "              <ogc:PropertyIsEqualTo>\n"
                        + "                <ogc:PropertyName>FID</ogc:PropertyName>\n"
                        + "                <ogc:Literal>113</ogc:Literal>\n"
                        + "              </ogc:PropertyIsEqualTo>\n"
                        + "          </ogc:Filter>\n"
                        + "        </wps:ComplexData>\n"
                        + "      </wps:Data>"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"text/xml; subtype=wfs-collection/1.0\">\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        Document d = postAsDOM(root(), request);
        // print(d);

        // the expected number of features
        assertXpathEvaluatesTo(
                "1", "count(/wfs:FeatureCollection/gml:featureMember/feature:Buildings)", d);
        // the expected number of attributes (+1 for the feature bounds)
        final String base =
                "/wfs:FeatureCollection/gml:featureMember/feature:Buildings[@fid=\"Buildings.1107531701010\"]";
        assertXpathEvaluatesTo("4", "count(" + base + "/*)", d);
        assertXpathEvaluatesTo("1", "count(" + base + "/feature:the_geom)", d);
        assertXpathEvaluatesTo("1", "count(" + base + "/feature:FID)", d);
        assertXpathEvaluatesTo("1", "count(" + base + "/feature:ADDRESS)", d);
    }

    @Test
    public void testFilterOGCCData() throws Exception {
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:Query</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>features</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" "
                        + "        xlink:href=\"http://geoserver/wfs?request=GetFeatures&amp;service=WFS&amp;version=1.0.0&amp;typeName="
                        + getLayerId(MockData.BUILDINGS)
                        + "\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                        + "            <wfs:Query typeName=\"topp:states\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n" //
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>filter</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:ComplexData mimeType=\"text/xml; subtype=filter/1.0\">\n"
                        + "          <![CDATA[\n"
                        + "          <ogc:Filter xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
                        + "              <ogc:PropertyIsEqualTo>\n"
                        + "                <ogc:PropertyName>FID</ogc:PropertyName>\n"
                        + "                <ogc:Literal>113</ogc:Literal>\n"
                        + "              </ogc:PropertyIsEqualTo>\n"
                        + "          </ogc:Filter>\n"
                        + "]]>\n"
                        + "        </wps:ComplexData>\n"
                        + "      </wps:Data>"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"text/xml; subtype=wfs-collection/1.0\">\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        Document d = postAsDOM(root(), request);
        // print(d);

        // the expected number of features
        assertXpathEvaluatesTo(
                "1", "count(/wfs:FeatureCollection/gml:featureMember/feature:Buildings)", d);
        // the expected number of attributes (+1 for the feature bounds)
        final String base =
                "/wfs:FeatureCollection/gml:featureMember/feature:Buildings[@fid=\"Buildings.1107531701010\"]";
        assertXpathEvaluatesTo("4", "count(" + base + "/*)", d);
        assertXpathEvaluatesTo("1", "count(" + base + "/feature:the_geom)", d);
        assertXpathEvaluatesTo("1", "count(" + base + "/feature:FID)", d);
        assertXpathEvaluatesTo("1", "count(" + base + "/feature:ADDRESS)", d);
    }

    @Test
    public void testAttribute() throws Exception {
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:Query</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>features</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" "
                        + "        xlink:href=\"http://geoserver/wfs?request=GetFeatures&amp;service=WFS&amp;version=1.0.0&amp;typeName="
                        + getLayerId(MockData.BUILDINGS)
                        + "\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                        + "            <wfs:Query typeName=\"topp:states\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n" //
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>attribute</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>FID</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>attribute</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>ADDRESS</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"text/xml; subtype=wfs-collection/1.0\">\n"
                        + "      <ows:Identifier>result</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        Document d = postAsDOM(root(), request);
        // print(d);

        // the expected number of features
        assertXpathEvaluatesTo(
                "2", "count(/wfs:FeatureCollection/gml:featureMember/feature:Buildings)", d);
        // the expected number of attributes (just two, no feature bounds without geometry)
        final String base =
                "/wfs:FeatureCollection/gml:featureMember/feature:Buildings[@fid=\"Buildings.1107531701010\"]";
        assertXpathEvaluatesTo("2", "count(" + base + "/*)", d);
        assertXpathEvaluatesTo("1", "count(" + base + "/feature:FID)", d);
        assertXpathEvaluatesTo("1", "count(" + base + "/feature:ADDRESS)", d);
    }
}
