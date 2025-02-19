/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.web.demo;

import static org.geoserver.mapml.MapMLConstants.MAPML_MULTILAYER_AS_MULTIEXTENT;
import static org.geoserver.mapml.MapMLConstants.MAPML_USE_FEATURES;
import static org.geoserver.mapml.MapMLConstants.MAPML_USE_FEATURES_REP;
import static org.geoserver.mapml.MapMLConstants.MAPML_USE_TILES_REP;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.data.DataView;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.demo.MapPreviewPage;
import org.geoserver.wms.WMSInfo;
import org.junit.After;
import org.junit.Test;

public class MapMLMapPreviewTest extends GeoServerWicketTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    }

    @After
    public void tearDown() throws IOException {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.LINES.getLocalPart());
        li.getResource().getMetadata().put(MAPML_USE_FEATURES, false);
        cat.save(li);

        ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        serviceInfo.getMetadata().put(MAPML_MULTILAYER_AS_MULTIEXTENT, false);
        getGeoServer().save(serviceInfo);
    }

    @Test
    public void testUrlFormatDefault() throws Exception {
        assertLink(
                "http://localhost/context/cgf/wms?service=WMS&amp;version=1.1.0&amp;request=GetMap&amp;layers=cgf%3ALines&amp;"
                        + "bbox=-97.4903565027649%2C-8.117456282619509E-4%2C-97.4871312635105%2C8.117456282619509E-4&amp;"
                        + "width=768&amp;height=384&amp;srs=MapML%3AWGS84&amp;styles=&amp;format=text%2Fhtml%3B%20subtype%3Dmapml&amp;"
                        + "format_options="
                        + MAPML_MULTILAYER_AS_MULTIEXTENT + "%3Afalse%3B" + MAPML_USE_FEATURES_REP + "%3Afalse%3B"
                        + MAPML_USE_TILES_REP + "%3Afalse");
    }

    @Test
    public void testUrlFormatFeature() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.LINES.getLocalPart());
        li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
        cat.save(li);
        assertLink(
                "http://localhost/context/cgf/wms?service=WMS&amp;version=1.1.0&amp;request=GetMap&amp;layers=cgf%3ALines&amp;"
                        + "bbox=-97.4903565027649%2C-8.117456282619509E-4%2C-97.4871312635105%2C8.117456282619509E-4&amp;"
                        + "width=768&amp;height=384&amp;srs=MapML%3AWGS84&amp;styles=&amp;format=text%2Fhtml%3B%20subtype%3Dmapml&amp;"
                        + "format_options="
                        + MAPML_MULTILAYER_AS_MULTIEXTENT + "%3Afalse%3B" + MAPML_USE_FEATURES_REP + "%3Atrue%3B"
                        + MAPML_USE_TILES_REP + "%3Afalse");
    }

    @Test
    public void testURLFormatMulti() throws Exception {
        ServiceInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        serviceInfo.getMetadata().put(MAPML_MULTILAYER_AS_MULTIEXTENT, true);
        getGeoServer().save(serviceInfo);
        assertLink(
                "http://localhost/context/cgf/wms?service=WMS&amp;version=1.1.0&amp;request=GetMap&amp;layers=cgf%3ALines&amp;"
                        + "bbox=-97.4903565027649%2C-8.117456282619509E-4%2C-97.4871312635105%2C8.117456282619509E-4&amp;"
                        + "width=768&amp;height=384&amp;srs=MapML%3AWGS84&amp;styles=&amp;format=text%2Fhtml%3B%20subtype%3Dmapml&amp;"
                        + "format_options="
                        + MAPML_MULTILAYER_AS_MULTIEXTENT + "%3Atrue%3B" + MAPML_USE_FEATURES_REP + "%3Afalse%3B"
                        + MAPML_USE_TILES_REP + "%3Afalse");
    }

    private static void assertLink(String link) {
        tester.startPage(MapPreviewPage.class);
        tester.assertRenderedPage(MapPreviewPage.class);

        @SuppressWarnings("unchecked")
        DataView<Component> data = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        for (Component datum : data) {
            MarkupContainer c = (MarkupContainer) datum;
            Label l = (Label) c.get("itemProperties:2:component");
            String model = l.getDefaultModelObjectAsString();
            if ("cgf:Lines".equals(model)) {
                ExternalLink mapmlLink = (ExternalLink)
                        c.get("itemProperties:3:component:commonFormat:3").getDefaultModelObject();
                assertEquals(link, mapmlLink.getDefaultModelObjectAsString());
            }
        }
    }
}
