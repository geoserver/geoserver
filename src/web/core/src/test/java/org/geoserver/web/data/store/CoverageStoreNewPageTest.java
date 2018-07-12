/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.wicket.Component;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.panel.FileParamPanel;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.geotools.gce.geotiff.GeoTiffFormatFactorySpi;
import org.geotools.geopkg.mosaic.GeoPackageFormat;
import org.junit.Before;
import org.junit.Test;
import org.opengis.coverage.grid.Format;

public class CoverageStoreNewPageTest extends GeoServerWicketTestSupport {

    /** print page structure? */
    private static final boolean debugMode = false;

    String formatType;

    String formatDescription;

    @Before
    public void init() {
        Format format = new GeoTiffFormatFactorySpi().createFormat();
        formatType = format.getName();
        formatDescription = format.getDescription();
    }

    private CoverageStoreNewPage startPage() {

        login();
        final CoverageStoreNewPage page = new CoverageStoreNewPage(formatType);
        tester.startPage(page);

        if (debugMode) {
            print(page, true, true);
        }

        return page;
    }

    @Test
    public void testInitCreateNewCoverageStoreInvalidDataStoreFactoryName() {

        final String formatName = "_invalid_";
        try {
            login();
            new CoverageStoreNewPage(formatName);
            fail("Expected IAE on invalid format name");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("Can't obtain the factory"));
        }
    }

    /** A kind of smoke test that only asserts the page is rendered when first loaded */
    @Test
    public void testPageRendersOnLoad() {

        startPage();

        tester.assertLabel("rasterStoreForm:storeType", formatType);
        tester.assertLabel("rasterStoreForm:storeTypeDescription", formatDescription);

        tester.assertComponent("rasterStoreForm:workspacePanel", WorkspacePanel.class);
    }

    @Test
    public void testInitialModelState() {

        CoverageStoreNewPage page = startPage();
        // print(page, true, true);

        assertNull(page.getDefaultModelObject());

        tester.assertModelValue("rasterStoreForm:enabledPanel:paramValue", Boolean.TRUE);
        tester.assertModelValue(
                "rasterStoreForm:workspacePanel:border:border_body:paramValue",
                getCatalog().getDefaultWorkspace());
        tester.assertModelValue(
                "rasterStoreForm:parametersPanel:url", "file:data/example.extension");
    }

    @Test
    public void testMultipleResources() {

        CoverageStoreNewPage page = startPage();

        assertNull(page.getDefaultModelObject());

        tester.assertModelValue("rasterStoreForm:enabledPanel:paramValue", Boolean.TRUE);
        tester.assertModelValue(
                "rasterStoreForm:workspacePanel:border:border_body:paramValue",
                getCatalog().getDefaultWorkspace());
        tester.assertModelValue(
                "rasterStoreForm:parametersPanel:url", "file:data/example.extension");
    }

    @Test
    public void testGeoPackageRaster() {
        login();
        formatType = new GeoPackageFormat().getName();
        final CoverageStoreNewPage page = new CoverageStoreNewPage(formatType);
        tester.startPage(page);

        tester.debugComponentTrees();
        Component urlComponent =
                tester.getComponentFromLastRenderedPage("rasterStoreForm:parametersPanel:url");
        assertThat(urlComponent, instanceOf(FileParamPanel.class));
    }
}
