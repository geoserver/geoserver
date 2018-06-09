/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import static org.junit.Assert.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Test;

public class ResourceConfigurationPageTest extends GeoServerWicketTestSupport {

    @Test
    public void testBasic() {
        LayerInfo layer =
                getGeoServerApplication()
                        .getCatalog()
                        .getLayerByName(getLayerId(MockData.BASIC_POLYGONS));

        login();
        tester.startPage(new ResourceConfigurationPage(layer, false));
        tester.assertLabel("publishedinfoname", layer.getResource().getPrefixedName());
        tester.assertComponent(
                "publishedinfo:tabs:panel:theList:0:content", BasicResourceConfig.class);
    }

    @Test
    public void testUpdateResource() {
        LayerInfo layer =
                getGeoServerApplication()
                        .getCatalog()
                        .getLayerByName(getLayerId(MockData.GEOMETRYLESS));

        login();
        ResourceConfigurationPage page = new ResourceConfigurationPage(layer, false);

        tester.startPage(page);
        tester.assertContainsNot("the_geom");

        FeatureTypeInfo info =
                getCatalog()
                        .getResourceByName(MockData.BRIDGES.getLocalPart(), FeatureTypeInfo.class);

        // Apply the new feature to the page
        page.add(
                new AjaxEventBehavior("ondblclick") {
                    public void onEvent(AjaxRequestTarget target) {
                        page.updateResource(info, target);
                    }
                });
        tester.executeAjaxEvent(page, "ondblclick");
        print(tester.getLastRenderedPage(), true, true);

        // verify contents were updated
        tester.assertContains("the_geom");
    }

    @Test
    public void testComputeLatLon() throws Exception {
        final Catalog catalog = getCatalog();

        final CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setStore(catalog.getStoreByName(MockData.POLYGONS.getPrefix(), DataStoreInfo.class));
        FeatureTypeInfo ft = cb.buildFeatureType(new NameImpl(MockData.POLYGONS));
        LayerInfo layer = cb.buildLayer(ft);

        login();
        ResourceConfigurationPage page = new ResourceConfigurationPage(layer, true);
        tester.startPage(page);
        // print(tester.getLastRenderedPage(), true, true, true);
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:theList:0:content:referencingForm:computeLatLon",
                "onclick");
        // print(tester.getLastRenderedPage(), true, true, true);
        // we used to have error messages
        tester.assertNoErrorMessage();
        Component llbox =
                tester.getComponentFromLastRenderedPage(
                        "publishedinfo:tabs:panel:theList:0:content:referencingForm:latLonBoundingBox");
        ReferencedEnvelope re = (ReferencedEnvelope) llbox.getDefaultModelObject();
        assertEquals(-93, re.getMinX(), 0.1);
        assertEquals(4.5, re.getMinY(), 0.1);
        assertEquals(-93, re.getMaxX(), 0.1);
        assertEquals(4.5, re.getMaxY(), 0.1);
    }
}
