/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.data.DataView;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Ignore;
import org.junit.Test;

public class MapPreviewPageTest extends GeoServerWicketTestSupport {
    @Test
    public void testValues() throws Exception {
        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);
    }

    @Test
    public void testLayerGroupNamesPrefixed() throws Exception {
        Catalog cat = getCatalog();

        LayerGroupInfo lg = cat.getFactory().createLayerGroup();
        lg.setName("foo");
        lg.setWorkspace(cat.getWorkspaceByName("sf"));
        lg.getLayers().add(cat.getLayerByName(getLayerId(MockData.PRIMITIVEGEOFEATURE)));
        new CatalogBuilder(cat).calculateLayerGroupBounds(lg);

        cat.add(lg);

        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);

        //move to next page
        tester.clickLink("table:navigatorBottom:navigator:next", true);

        DataView data = 
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        boolean exists = false;
        for (Iterator it = data.iterator(); it.hasNext(); ) {
            MarkupContainer c = (MarkupContainer) it.next();
            Label l = (Label) c.get("itemProperties:1:component");
            if ("sf:foo".equals(l.getDefaultModelObjectAsString())) {
                exists = true;
            }
        }

        assertTrue(exists);
    }
    
    @Test
    @Ignore
    public void testLayerNamesPrefixed() throws Exception {
        Catalog cat = getCatalog();

        LayerInfo ly = cat.getLayerByName(getLayerId(MockData.STREAMS));
        
        assertNotNull(ly);

        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);

        //move to next page
        tester.clickLink("table:navigatorBottom:navigator:next", true);

        DataView data = 
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        boolean exists = false;
        for (Iterator it = data.iterator(); it.hasNext(); ) {
            MarkupContainer c = (MarkupContainer) it.next();
            Label l = (Label) c.get("itemProperties:1:component");
            if (getLayerId(MockData.STREAMS).equals(l.getDefaultModelObjectAsString())) {
                exists = true;
            }
        }

        assertTrue(exists);
    }
}
