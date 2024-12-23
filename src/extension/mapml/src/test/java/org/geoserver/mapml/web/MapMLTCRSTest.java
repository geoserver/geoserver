/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.mapml.tcrs.CustomTiledCRSTest;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Test for {@link MapMLAdminPanel}. */
public class MapMLTCRSTest extends GeoServerWicketTestSupport {

    @Before
    public void before() {
        login();
    }

    @After
    public void after() {
        logout();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();
        Catalog catalog = gs.getCatalog();
        CustomTiledCRSTest.addGridSets(gs, catalog, global);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAddSupportedTCRSFromGridsetsSelector() throws FactoryException {
        MapMLTCRSSettingsPage settingsPage = new MapMLTCRSSettingsPage();
        tester.startPage(settingsPage);
        tester.assertRenderedPage(MapMLTCRSSettingsPage.class);
        FormTester ft = tester.newFormTester("form");
        Palette<?> palette = (Palette<?>) tester.getComponentFromLastRenderedPage("form:mapMLTCRS:tcrspalette");
        List<String> selectedItems = (List<String>) new ArrayList<>(palette.getModelObject());
        selectedItems.add("UTM31WGS84Quad"); // Add your choice to the selected items
        palette.setDefaultModelObject(selectedItems);
        ft.submit();
        tester.assertModelValue("form:mapMLTCRS:tcrspalette", selectedItems);
        CoordinateReferenceSystem crs = CRS.decode("MapML:UTM31WGS84Quad");
        assertNotNull(crs);
        Set<String> codes = CRS.getSupportedCodes("MapML");
        assertTrue(codes.contains("UTM31WGS84Quad"));
    }
}
