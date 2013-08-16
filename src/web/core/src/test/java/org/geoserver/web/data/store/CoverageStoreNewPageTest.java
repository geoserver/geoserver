/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static org.junit.Assert.*;

import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.geotools.gce.arcgrid.ArcGridFormatFactory;
import org.geotools.gce.gtopo30.GTopo30FormatFactory;
import org.junit.Before;
import org.junit.Test;
import org.opengis.coverage.grid.Format;

public class CoverageStoreNewPageTest extends GeoServerWicketTestSupport {

    /**
     * print page structure?
     */
    private static final boolean debugMode = false;

    String formatType;

    String formatDescription;

    @SuppressWarnings("deprecation")
    @Before
    public void init() {
        Format format = new GTopo30FormatFactory().createFormat();
        formatType = format.getName();
        formatDescription = format.getDescription();
    }

    private CoverageStoreNewPage startPage() {

        final CoverageStoreNewPage page = new CoverageStoreNewPage(formatType);
        login();
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
            new CoverageStoreNewPage(formatName);
            fail("Expected IAE on invalid format name");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("Can't obtain the factory"));
        }
    }

    /**
     * A kind of smoke test that only asserts the page is rendered when first loaded
     */
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
        tester.assertModelValue("rasterStoreForm:workspacePanel:border:paramValue", getCatalog()
                .getDefaultWorkspace());
        tester.assertModelValue("rasterStoreForm:parametersPanel:url",
                "file:data/example.extension");
    }

    @Test
    public void testMultipleResources() {

        CoverageStoreNewPage page = startPage();

        assertNull(page.getDefaultModelObject());

        tester.assertModelValue("rasterStoreForm:enabledPanel:paramValue", Boolean.TRUE);
        tester.assertModelValue("rasterStoreForm:workspacePanel:border:paramValue", getCatalog()
                .getDefaultWorkspace());
        tester.assertModelValue("rasterStoreForm:parametersPanel:url",
                "file:data/example.extension");

    }
}
