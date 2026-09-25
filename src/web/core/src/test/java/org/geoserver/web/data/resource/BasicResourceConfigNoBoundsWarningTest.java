/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import javax.xml.namespace.QName;
import org.apache.wicket.Component;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.referencing.CRS;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Tests for the no-bounds CRS warning in {@link BasicResourceConfig}.
 *
 * <p>Uses a simple vector layer fixture without any Docker/Testcontainers dependency.
 */
public class BasicResourceConfigNoBoundsWarningTest extends GeoServerWicketTestSupport {

    private static final QName LINES = new QName(MockData.SF_URI, "null_srid_line", MockData.SF_PREFIX);

    private static final String WARNING_PATH =
            "publishedinfo:tabs:panel:theList:0:content:referencingForm:noBoundsWarning";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addVectorLayer(
                LINES,
                Collections.emptyMap(),
                "null_srid_line.properties",
                BasicResourceConfigNoBoundsWarningTest.class,
                getCatalog());
    }

    @Test
    public void testWarningVisibleForEngineeringCRS() throws Exception {
        Catalog catalog = getGeoServerApplication().getCatalog();
        LayerInfo layer = catalog.getLayerByName(getLayerId(LINES));
        assertNotNull(layer);
        ResourceInfo resource = layer.getResource();
        resource.setSRS("EPSG:404000");
        resource.setNativeCRS(CRS.decode("EPSG:404000"));
        catalog.save(resource);

        login();
        tester.startPage(new ResourceConfigurationPage(layer, false));
        tester.assertNoErrorMessage();

        tester.assertVisible(WARNING_PATH);
        Component warning = tester.getComponentFromLastRenderedPage(WARNING_PATH);
        assertThat(warning.getDefaultModelObjectAsString(), Matchers.containsString("EPSG:404000"));
    }

    @Test
    public void testWarningHiddenForNormalCRS() throws Exception {
        Catalog catalog = getGeoServerApplication().getCatalog();
        LayerInfo layer = catalog.getLayerByName(getLayerId(LINES));
        assertNotNull(layer);
        ResourceInfo resource = layer.getResource();
        resource.setSRS("EPSG:4326");
        resource.setNativeCRS(CRS.decode("EPSG:4326"));
        catalog.save(resource);

        login();
        tester.startPage(new ResourceConfigurationPage(layer, false));
        tester.assertNoErrorMessage();

        tester.assertInvisible(WARNING_PATH);
    }

    @Test
    public void testComputeFromSRSShowsWarningWhenNoBounds() throws Exception {
        Catalog catalog = getGeoServerApplication().getCatalog();
        LayerInfo layer = catalog.getLayerByName(getLayerId(LINES));
        assertNotNull(layer);
        ResourceInfo resource = layer.getResource();
        resource.setSRS("EPSG:404000");
        resource.setNativeCRS(CRS.decode("EPSG:404000"));
        catalog.save(resource);

        login();
        tester.startPage(new ResourceConfigurationPage(layer, false));

        // Click "Compute from SRS bounds"
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:theList:0:content:referencingForm:computeLatLonFromNativeSRS", "click");

        tester.assertVisible(WARNING_PATH);
    }

    @Test
    public void testComputeFromSRSHidesWarningWhenBoundsExist() throws Exception {
        Catalog catalog = getGeoServerApplication().getCatalog();
        LayerInfo layer = catalog.getLayerByName(getLayerId(LINES));
        assertNotNull(layer);
        ResourceInfo resource = layer.getResource();
        resource.setSRS("EPSG:4326");
        resource.setNativeCRS(CRS.decode("EPSG:4326"));
        catalog.save(resource);

        login();
        tester.startPage(new ResourceConfigurationPage(layer, false));

        // Click "Compute from SRS bounds"
        tester.executeAjaxEvent(
                "publishedinfo:tabs:panel:theList:0:content:referencingForm:computeLatLonFromNativeSRS", "click");

        tester.assertInvisible(WARNING_PATH);
    }
}
