package org.geoserver.rest.catalog;

import org.geoserver.rest.RestBaseController;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 * Integration test for {@link org.geoserver.rest.IndexController}, verifying that all restconfig endpoints show up in
 * the index
 */
public class IndexControllerTest extends CatalogRESTTestSupport {
    @Test
    public void testGetAsHTML() throws Exception {
        //TODO: Add more endpoints as they are added;
        ArrayList<String> linksToFind = new ArrayList<>(Arrays.asList("styles"));

        Document dom = getAsDOM(RestBaseController.ROOT_PATH);
        print(dom);

        NodeList links = xp.getMatchingNodes("//li/a", dom);

        for ( int i = 0; i < links.getLength(); i++ ) {
            Element link = (Element) links.item( i );
            linksToFind.remove(link.getTextContent());
        }
        assertTrue("Could not find the following links in index: "+linksToFind.toString(), linksToFind.size() == 0);
    }
}
