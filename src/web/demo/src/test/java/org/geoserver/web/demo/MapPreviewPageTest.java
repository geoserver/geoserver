/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.data.DataView;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class MapPreviewPageTest extends GeoServerWicketTestSupport {
    
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // setup few layers so that we don't have to mess with paging
        for (QName name: CiteTestData.TYPENAMES) {
            if("cite".equals(name.getPrefix())) {
                testData.setUpVectorLayer(name);
            }
        }
    }
    
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
        lg.setWorkspace(cat.getWorkspaceByName("cite"));
        lg.getLayers().add(cat.getLayerByName(getLayerId(MockData.STREAMS)));
        new CatalogBuilder(cat).calculateLayerGroupBounds(lg);

        cat.add(lg);

        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);

        DataView data = 
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        boolean exists = false;
        for (Iterator it = data.iterator(); it.hasNext(); ) {
            MarkupContainer c = (MarkupContainer) it.next();
            Label l = (Label) c.get("itemProperties:1:component");
            String name = l.getDefaultModelObjectAsString();
            System.out.println(name);
            if ("cite:foo".equals(name)) {
                exists = true;
            }
        }

        assertTrue(exists);
    }
    
    @Test
    public void testLayerNamesPrefixed() throws Exception {
        Catalog cat = getCatalog();

        LayerInfo ly = cat.getLayerByName(getLayerId(MockData.STREAMS));
        
        assertNotNull(ly);

        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);
        
        DataView data = 
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        boolean exists = false;
        for (Iterator it = data.iterator(); it.hasNext(); ) {
            MarkupContainer c = (MarkupContainer) it.next();
            Label l = (Label) c.get("itemProperties:1:component");
            String name = l.getDefaultModelObjectAsString();
            if (getLayerId(MockData.STREAMS).equals(name)) {
                exists = true;
            }
        }
        
        assertTrue(exists);
    }
    
    @Test
    public void testGML32Link() throws Exception {
        Catalog cat = getCatalog();

        LayerInfo ly = cat.getLayerByName(getLayerId(MockData.STREAMS));
        
        assertNotNull(ly);

        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);

        // check the GML 3.2 links have been escaped
        tester.assertContains("application%2Fgml%2Bxml%3B\\+version%3D3\\.2");
    }
}
