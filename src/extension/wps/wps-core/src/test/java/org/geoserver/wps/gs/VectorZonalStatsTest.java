/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.w3c.dom.Document;

public class VectorZonalStatsTest extends WPSTestSupport {

    static final double EPS = 1e-6;

    public static QName BUGSITES = new QName(MockData.SF_URI, "bugsites", MockData.SF_PREFIX);

    public static QName RESTRICTED = new QName(MockData.SF_URI, "restricted", MockData.SF_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Map<LayerProperty, Object> props = new HashMap<SystemTestData.LayerProperty, Object>();
        props.put(
                LayerProperty.ENVELOPE,
                new ReferencedEnvelope(
                        181985.7630,
                        818014.2370,
                        1973809.4640,
                        8894102.4298,
                        CRS.decode("EPSG:26713", true)));

        testData.addVectorLayer(BUGSITES, props, "bugsites.properties", getClass(), getCatalog());
        testData.addVectorLayer(
                RESTRICTED, props, "restricted.properties", getClass(), getCatalog());
    }

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put("feature", "http://cite.opengeospatial.org/gmlsf");
    }

    @Test
    public void testStatistics() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:VectorZonalStatistics</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>data</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                        + "            <wfs:Query typeName=\"sf:bugsites\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>dataAttribute</ows:Identifier>\n"
                        + "      <wps:Data>\n"
                        + "        <wps:LiteralData>cat</wps:LiteralData>\n"
                        + "      </wps:Data>\n"
                        + "    </wps:Input>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>zones</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n"
                        + "            <wfs:Query typeName=\"sf:restricted\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput mimeType=\"text/xml; subtype=wfs-collection/1.0\">\n"
                        + "      <ows:Identifier>statistics</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>\n"
                        + "\n"
                        + "";

        Document dom = postAsDOM(root(), xml);
        print(dom);

        assertXpathEvaluatesTo("", "//feature:restricted[feature:z_cat=2]/feature:count", dom);
        assertXpathEvaluatesTo("23", "//feature:restricted[feature:z_cat=3]/feature:count", dom);
        assertXpathEvaluatesTo("1", "//feature:restricted[feature:z_cat=4]/feature:count", dom);

        assertXpathEvaluatesTo("32.0", "//feature:restricted[feature:z_cat=3]/feature:min", dom);
        assertXpathEvaluatesTo("81.0", "//feature:restricted[feature:z_cat=3]/feature:max", dom);
        assertXpathEvaluatesTo("1331.0", "//feature:restricted[feature:z_cat=3]/feature:sum", dom);
        assertXpathEvaluatesTo(
                "57.869565217391305", "//feature:restricted[feature:z_cat=3]/feature:avg", dom);
        assertXpathEvaluatesTo(
                "15.120686514855372", "//feature:restricted[feature:z_cat=3]/feature:stddev", dom);

        assertXpathEvaluatesTo("84.0", "//feature:restricted[feature:z_cat=4]/feature:min", dom);
        assertXpathEvaluatesTo("84.0", "//feature:restricted[feature:z_cat=4]/feature:max", dom);
        assertXpathEvaluatesTo("84.0", "//feature:restricted[feature:z_cat=4]/feature:sum", dom);
        assertXpathEvaluatesTo("84.0", "//feature:restricted[feature:z_cat=4]/feature:avg", dom);
        assertXpathEvaluatesTo("0.0", "//feature:restricted[feature:z_cat=4]/feature:stddev", dom);
    }
}
