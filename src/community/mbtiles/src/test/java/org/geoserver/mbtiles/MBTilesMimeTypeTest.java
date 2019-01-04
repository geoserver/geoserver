/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mbtiles;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.demo.MapPreviewPage;
import org.geoserver.web.wicket.ParamResourceModel;
import org.junit.Test;

public class MBTilesMimeTypeTest extends GeoServerWicketTestSupport {

    @Test
    public void testMBTilesFormat() {
        // Opening the selected page
        tester.startPage(new MapPreviewPage());
        tester.assertRenderedPage(MapPreviewPage.class);
        tester.assertNoErrorMessage();

        // Getting the wms outputformats available
        Component component =
                tester.getComponentFromLastRenderedPage(
                        "table:listContainer:items:1:itemProperties:4:component:menu:wmsFormats");
        assertNotNull(component);
        assertTrue(component instanceof RepeatingView);
        // Get the list of all the format
        RepeatingView view = (RepeatingView) component;
        Iterator<? extends Component> iterator = view.iterator();
        // Check that MBTiles has been found
        boolean mbtilesFound = false;
        // Get the string for the application/x-sqlite3 mimetype
        ParamResourceModel rm =
                new ParamResourceModel("format.wms.application/x-sqlite3", null, "");
        String mbtiles = rm.getString();
        while (iterator.hasNext()) {
            Component comp = iterator.next();
            assertTrue(comp instanceof Label);
            Label lb = (Label) comp;
            String test = lb.getDefaultModelObjectAsString();
            if (test.contains(mbtiles)) {
                mbtilesFound = true;
            }
        }
        // Ensure the MBTiles string has been found
        assertTrue(mbtilesFound);
    }
}
