/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wfs.WFSTestSupport;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class MapMLHTMLGetFeatureTest extends WFSTestSupport {

    @Test
    @SuppressWarnings("PMD.SimplifiableTestAssertion")
    public void testHTML() throws Exception {
        Catalog cat = getCatalog();

        LayerInfo li = cat.getLayerByName(MockData.LAKES.getLocalPart());
        FeatureTypeInfo typeName = getCatalog().getFeatureTypeByName(li.getName());
        String path =
                "wfs?typeName="
                        + typeName.getName()
                        + "&outputFormat="
                        // + "application/json"
                        + MapMLConstants.MAPML_HTML_MIME_TYPE
                        + "&SERVICE=WFS&VERSION=1.1.0"
                        + "&REQUEST=GetFeature"
                        + "&SRSNAME=epsg:3857"
                        + "&CQL_FILTER=NAME='Blue Lake'";

        MockHttpServletRequest request = createRequest(path);

        request.setMethod("GET");
        request.setContent(new byte[] {});
        MockHttpServletResponse response = dispatch(request, "UTF-8");
        String htmlResponse = response.getContentAsString();
        // System.out.println(htmlResponse);
        assertNotNull("Html method must return a document", htmlResponse);
        Document doc = Jsoup.parse(htmlResponse);
        Element webmapimport = doc.head().select("script").first();
        assertTrue(
                "HTML document script must use mapml-viewer.js module",
                webmapimport.attr("src").matches(".*mapml-viewer\\.js"));
        Element map = doc.body().select("mapml-viewer").first();
        assertTrue(
                "viewer must have projection set to \"OSMTILE\"",
                map.attr("projection").equalsIgnoreCase("OSMTILE"));
        Element layer = map.getElementsByTag("layer-").first();
        assertTrue(
                "Layer must have label equal to title or layer name if no title",
                layer.attr("label").equalsIgnoreCase(li.getTitle()));
        assertTrue(
                "HTML title and layer- label attribute should be equal",
                layer.attr("label").equalsIgnoreCase(doc.title()));
        String zoom = doc.select("mapml-viewer").attr("zoom");
        // zoom is calculated based on a display size and the extent of the
        // layer.  In the case of the test layer group "layerGroup", the extent is the
        // maximum extent, so zoom should be 1;
        assertTrue(!"0".equalsIgnoreCase(zoom));
        String layerSrc = layer.attr("src");

        assertThat(layerSrc, containsString("TYPENAME=Lakes"));
        assertThat(layerSrc, containsString("OUTPUTFORMAT=MAPML"));
        assertThat(layerSrc, containsString("CQL_FILTER=NAME%3D%27Blue%20Lake%27"));
        assertThat(layerSrc, containsString("SRSNAME=epsg%3A3857"));
    }

    @Test
    public void testHTMLWorkspaceQualified() throws Exception {
        String path =
                "cite/wfs?typeName=cite:Lakes"
                        + "&OutputFormat="
                        + MapMLConstants.MAPML_HTML_MIME_TYPE
                        + "&SERVICE=WFS&VERSION=1.1.0"
                        + "&REQUEST=GetFeature"
                        + "&SRSNAME=epsg:3857"
                        + "&BBOX=80,-50,100,0";

        Document doc = getAsJSoup(path);
        Element layer = doc.select("mapml-viewer > layer-").first();
        String layerSrc = layer.attr("src");
        assertThat(layerSrc, startsWith("http://localhost:8080/geoserver/cite/wfs?"));
        assertThat(layerSrc, containsString("TYPENAME=cite%3ALakes"));
        assertThat(layerSrc, containsString("SRSNAME=epsg%3A3857"));
        assertThat(layerSrc, containsString("&BBOX=80%2C-50%2C100%2C0"));
    }

    @Test
    public void testInvalidProjectionHTML() throws Exception {
        String path =
                "wfs?typeNames=cite:Lakes"
                        + "&outputFormat="
                        + MapMLConstants.MAPML_HTML_MIME_TYPE
                        + "&SERVICE=WFS&VERSION=1.1.0"
                        + "&REQUEST=GetFeature"
                        + "&SRSNAME=EPSG:32632"
                        + "&BBOX=-13885038,2870337,-7455049,6338174";
        org.w3c.dom.Document dom = getAsDOM(path);
        checkOws10Exception(dom, "InvalidParameterValue", "srsName");
    }
}
