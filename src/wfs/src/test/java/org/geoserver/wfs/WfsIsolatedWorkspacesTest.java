/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.IsolatedWorkspacesTest;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Contains tests related to isolated workspaces, this tests exercise WFS operations. An workspace
 * in GeoServer is composed of the workspace information and a namespace which has a special
 * relevance in WFS.
 */
public final class WfsIsolatedWorkspacesTest extends IsolatedWorkspacesTest {

    // WFS 1.1.0 namespaces
    private static final Map<String, String> NAMESPACES_WFS11 = new HashMap<>();

    // init WFS 1.1.0 namespaces
    static {
        NAMESPACES_WFS11.put("wfs", "http://www.opengis.net/wfs");
        NAMESPACES_WFS11.put("ows", "http://www.opengis.net/ows");
        NAMESPACES_WFS11.put("ogc", "http://www.opengis.net/ogc");
        NAMESPACES_WFS11.put("xs", "http://www.w3.org/2001/XMLSchema");
        NAMESPACES_WFS11.put("xsd", "http://www.w3.org/2001/XMLSchema");
        NAMESPACES_WFS11.put("gml", "http://www.opengis.net/gml");
        NAMESPACES_WFS11.put("xlink", "http://www.w3.org/1999/xlink");
        NAMESPACES_WFS11.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        NAMESPACES_WFS11.put("gs", "http://geoserver.org");
    }

    // WFS 2.0 namespaces
    private static final Map<String, String> NAMESPACES_WFS20 = new HashMap<>();

    // init WFS 2.0 namespaces
    static {
        NAMESPACES_WFS20.put("wfs", "http://www.opengis.net/wfs/2.0");
        NAMESPACES_WFS20.put("ows", "http://www.opengis.net/ows/1.1");
        NAMESPACES_WFS20.put("fes", "http://www.opengis.net/fes/2.0");
        NAMESPACES_WFS20.put("gml", "http://www.opengis.net/gml/3.2");
        NAMESPACES_WFS20.put("ogc", "http://www.opengis.net/ogc");
        NAMESPACES_WFS20.put("xs", "http://www.w3.org/2001/XMLSchema");
        NAMESPACES_WFS20.put("xsd", "http://www.w3.org/2001/XMLSchema");
        NAMESPACES_WFS20.put("xlink", "http://www.w3.org/1999/xlink");
        NAMESPACES_WFS20.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        NAMESPACES_WFS20.put("gs", "http://geoserver.org");
    }

    @Test
    public void getFeatureInfoOnLayerFromIsolatedWorkspaces() throws Exception {
        Catalog catalog = getCatalog();
        // adding two workspaces with the same URI, one of them is isolated
        createWorkspace("test_a1", "https://www.test_a.com", false);
        createWorkspace("test_a2", "https://www.test_a.com", true);
        // get created workspaces and associated namespaces
        WorkspaceInfo workspace1 = catalog.getWorkspaceByName("test_a1");
        NamespaceInfo namespace1 = catalog.getNamespaceByPrefix("test_a1");
        WorkspaceInfo workspace2 = catalog.getWorkspaceByName("test_a2");
        NamespaceInfo namespace2 = catalog.getNamespaceByPrefix("test_a2");
        // add a layer with the same name to both workspaces, layers have different content
        LayerInfo clonedLayer1 =
                cloneVectorLayerIntoWorkspace(workspace1, namespace1, "Lines", "layer_e");
        LayerInfo clonedLayer2 =
                cloneVectorLayerIntoWorkspace(workspace2, namespace2, "Points", "layer_e");
        assertThat(clonedLayer1.getId(), not(clonedLayer2.getId()));
        // test get feature requests for WFS 1.1.0
        MockHttpServletResponse response =
                getAsServletResponse(
                        "test_a1/wfs?SERVICE=wfs&VERSION=1.1.0&REQUEST=getFeature&typeName=layer_e&maxFeatures=1");
        evaluateAndCheckXpath(
                mergeNamespaces(NAMESPACES_WFS11, "test_a1", "https://www.test_a.com"),
                response,
                "count(//wfs:FeatureCollection/gml:featureMembers/test_a1:layer_e/test_a1:lineStringProperty)",
                "1");
        response =
                getAsServletResponse(
                        "test_a2/wfs?SERVICE=wfs&VERSION=1.1.0&REQUEST=getFeature&typeName=layer_e&maxFeatures=1");
        evaluateAndCheckXpath(
                mergeNamespaces(NAMESPACES_WFS11, "test_a2", "https://www.test_a.com"),
                response,
                "count(//wfs:FeatureCollection/gml:featureMembers/test_a2:layer_e/test_a2:pointProperty)",
                "1");
    }

    private Map<String, String> mergeNamespaces(
            Map<String, String> namespaces, String... extraNamespaces) {
        Map<String, String> finalNamespaces = new HashMap<>();
        finalNamespaces.putAll(namespaces);
        for (int i = 0; i < extraNamespaces.length; i += 2) {
            finalNamespaces.put(extraNamespaces[i], extraNamespaces[i + 1]);
        }
        return finalNamespaces;
    }

    private void evaluateAndCheckXpath(
            Map<String, String> namespaces,
            MockHttpServletResponse response,
            String xpath,
            String expectResult)
            throws Exception {
        // convert response to document
        Document document = null;
        try (InputStream input =
                new ByteArrayInputStream(response.getContentAsString().getBytes())) {
            // create the DOM document
            document = dom(input, true);
        }
        // configure the correct WFS namespaces
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        XpathEngine xpathEngine = XMLUnit.newXpathEngine();
        // validate the provided XPATH
        String result = xpathEngine.evaluate(xpath, document);
        assertThat(result, is(expectResult));
    }
}
