/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test.web;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.data.DataView;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.demo.MapPreviewPage;

public class AbstractMapPreviewPageTest extends GeoServerWicketTestSupport {

    protected List<String> EXPECTED_GML_LINKS = new ArrayList<String>();

    protected AbstractMapPreviewPageTest(List<String> expectedGmlLinks) {
        this.EXPECTED_GML_LINKS = expectedGmlLinks;
    }

    public void testAppSchemaGmlLinks() {
        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);

        DataView items =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertNotNull(items);
        assertEquals(EXPECTED_GML_LINKS.size(), items.size());

        // collect GML links model objects
        List<String> gmlLinks = new ArrayList<String>();
        for (int i = 1; i <= EXPECTED_GML_LINKS.size(); i++) {
            ExternalLink gmlLink =
                    (ExternalLink)
                            items.get(i + ":itemProperties:3:component:commonFormat:1")
                                    .getDefaultModelObject();
            assertNotNull(gmlLink);
            gmlLinks.add(gmlLink.getDefaultModelObjectAsString());
        }

        Collections.sort(EXPECTED_GML_LINKS);
        Collections.sort(gmlLinks);
        // check the two lists match
        assertArrayEquals(
                EXPECTED_GML_LINKS.toArray(new String[] {}), gmlLinks.toArray(new String[] {}));
    }
}
