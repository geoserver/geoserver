/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.data.DataView;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.wfs.WFSInfo;
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

        // move to next page
        GeoServerTablePanel table =
                (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        System.out.println(table.getDataProvider().size());
        tester.clickLink("table:navigatorBottom:navigator:next", true);

        DataView data =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        boolean exists = false;
        for (Iterator it = data.iterator(); it.hasNext(); ) {
            MarkupContainer c = (MarkupContainer) it.next();
            Label l = (Label) c.get("itemProperties:2:component");
            String model = l.getDefaultModelObjectAsString();
            if ("sf:foo".equals(model)) {
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

        // move to next page
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

    @Test
    public void testMaxNumberOfFeaturesForPreview() throws Exception {

        GeoServer geoserver = getGeoServer();
        WFSInfo wfsInfo = geoserver.getService(WFSInfo.class);

        int maxFeatures = 100;
        wfsInfo.setMaxNumberOfFeaturesForPreview(maxFeatures);
        geoserver.save(wfsInfo);

        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);

        assertMaxFeaturesInData(
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items"),
                maxFeatures);

        maxFeatures = 0;
        wfsInfo.setMaxNumberOfFeaturesForPreview(maxFeatures);
        geoserver.save(wfsInfo);

        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);

        assertMaxFeaturesInData(
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items"),
                maxFeatures);
    }

    @Test
    public void testWfsOutputFormatValueUrlEncoding() {
        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);

        Label optionLabel =
                (Label)
                        tester.getComponentFromLastRenderedPage(
                                "table:listContainer:items:4:itemProperties:4:component:menu:wfs:wfsFormats:3");
        assertTrue(optionLabel.getDefaultModelObjectAsString().equals("GML3.2"));
        for (Iterator<? extends Behavior> itBehaviors = optionLabel.getBehaviors().iterator();
                itBehaviors.hasNext(); ) {
            Behavior b = itBehaviors.next();
            if (b instanceof AttributeModifier) {
                AttributeModifier am = (AttributeModifier) b;
                String url = am.toString();
                assertTrue(!url.contains("gml+xml"));
                assertTrue(url.contains("gml%2Bxml"));
                break;
            }
        }
    }

    private void assertMaxFeaturesInData(DataView data, int maxFeatures) {
        for (Iterator it = data.iterator(); it.hasNext(); ) {
            MarkupContainer c = (MarkupContainer) it.next();
            MarkupContainer list = (MarkupContainer) c.get("itemProperties:4:component:menu");
            for (Iterator<? extends Behavior> itBehaviors = list.getBehaviors().iterator();
                    itBehaviors.hasNext(); ) {
                Behavior b = itBehaviors.next();
                if (b instanceof AttributeModifier) {
                    AttributeModifier am = (AttributeModifier) b;
                    String url = am.toString();
                    if (maxFeatures > 0) {
                        assertTrue(url.contains("&maxFeatures=" + maxFeatures));
                    } else {
                        assertTrue(!url.contains("&maxFeatures="));
                    }
                }
            }
        }
    }
}
