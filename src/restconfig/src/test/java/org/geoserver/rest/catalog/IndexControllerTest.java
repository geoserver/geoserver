/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geoserver.rest.RestBaseController;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Integration test for {@link org.geoserver.rest.IndexController}, verifying that all restconfig
 * endpoints show up in the index
 */
public class IndexControllerTest extends CatalogRESTTestSupport {
    @Test
    public void testGetAsHTML() throws Exception {
        // TODO: Add more endpoints as they are added;
        List<String> linksToFind =
                new ArrayList<>(Arrays.asList("layers", "layergroups", "styles", "workspaces"));

        List<String> invalidLinks = Arrays.asList("reset", "reload");

        Document dom = getAsDOM(RestBaseController.ROOT_PATH);
        print(dom);

        NodeList links = xp.getMatchingNodes("//li/a", dom);

        for (int i = 0; i < links.getLength(); i++) {
            Element link = (Element) links.item(i);
            String linkText = link.getTextContent();
            linksToFind.remove(linkText);
            assertFalse(
                    "Index should only contain GET endpoints. Found: " + linkText,
                    invalidLinks.contains(linkText));
        }
        assertTrue(
                "Could not find the following links in index: " + linksToFind.toString(),
                linksToFind.size() == 0);
    }
}
