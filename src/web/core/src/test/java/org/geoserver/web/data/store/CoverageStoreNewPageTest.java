/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.layer.NewLayerPage;
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

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.setUpDefaultRasterLayers();
    }

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

    @Test
    public void testNewCoverageSave() {
        startPage();
        FormTester ft = tester.newFormTester("rasterStoreForm");
        ft.setValue(
                "parametersPanel:url:fileInput:border:border_body:paramValue",
                "BlueMarble/tazbm.tiff");
        ft.setValue("namePanel:border:border_body:paramValue", "tazbm2");
        ft.submit("save");

        tester.assertNoErrorMessage();
        tester.assertRenderedPage(NewLayerPage.class);
        CoverageStoreInfo store = getCatalog().getCoverageStoreByName("tazbm2");
        assertNotNull(store);
        assertEquals("BlueMarble/tazbm.tiff", store.getURL());
    }

    @Test
    public void testNewCoverageApply() {
        startPage();
        FormTester ft = tester.newFormTester("rasterStoreForm");
        ft.setValue(
                "parametersPanel:url:fileInput:border:border_body:paramValue",
                "BlueMarble/tazbm.tiff");
        ft.setValue("namePanel:border:border_body:paramValue", "tazbm3");
        ft.submit("apply");

        tester.assertNoErrorMessage();
        tester.assertRenderedPage(CoverageStoreEditPage.class);
        CoverageStoreInfo store = getCatalog().getCoverageStoreByName("tazbm3");
        assertNotNull(store);
        assertEquals("BlueMarble/tazbm.tiff", store.getURL());
    }

    @Test
    public void testDisableOnConnFailureCheckbox() {
        startPage();
        FormTester ft = tester.newFormTester("rasterStoreForm");
        ft.setValue(
                "parametersPanel:url:fileInput:border:border_body:paramValue",
                "BlueMarble/tazbm.tiff");
        ft.setValue("namePanel:border:border_body:paramValue", "tazbm99");
        Component component =
                tester.getComponentFromLastRenderedPage(
                        "rasterStoreForm:disableOnConnFailurePanel:paramValue");
        CheckBox checkBox = (CheckBox) component;
        assertFalse(Boolean.valueOf(checkBox.getInput()).booleanValue());

        ft.setValue("disableOnConnFailurePanel:paramValue", true);

        ft.submit("save");

        tester.assertNoErrorMessage();
        CoverageStoreInfo store = getCatalog().getCoverageStoreByName("tazbm99");
        assertNotNull(store);
        assertTrue(store.isDisableOnConnFailure());
    }
}
