/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import static org.geoserver.security.impl.DefaultFileAccessManager.GEOSERVER_DATA_SANDBOX;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.impl.DefaultFileAccessManager;
import org.geoserver.security.impl.FileSandboxEnforcer;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.layer.NewLayerPage;
import org.geoserver.web.data.store.panel.FileParamPanel;
import org.geoserver.web.data.store.panel.WorkspacePanel;
import org.geotools.api.coverage.grid.Format;
import org.geotools.gce.geotiff.GeoTiffFormatFactorySpi;
import org.geotools.geopkg.mosaic.GeoPackageFormat;
import org.junit.Before;
import org.junit.Test;

public class CoverageStoreNewPageTest extends GeoServerWicketTestSupport {

    /** print page structure? */
    private static final boolean debugMode = false;

    String formatType;

    String formatDescription;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.setUpDefaultRasterLayers();

        // force creation of the FileSanboxEnforcer (beans are lazy loaded in tests, and this
        // one registers itself on the catalog on creation)
        GeoServerExtensions.bean(FileSandboxEnforcer.class, applicationContext);
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
        tester.assertModelValue("rasterStoreForm:parametersPanel:url", "file:data/example.extension");
    }

    @Test
    public void testMultipleResources() {

        CoverageStoreNewPage page = startPage();

        assertNull(page.getDefaultModelObject());

        tester.assertModelValue("rasterStoreForm:enabledPanel:paramValue", Boolean.TRUE);
        tester.assertModelValue(
                "rasterStoreForm:workspacePanel:border:border_body:paramValue",
                getCatalog().getDefaultWorkspace());
        tester.assertModelValue("rasterStoreForm:parametersPanel:url", "file:data/example.extension");
    }

    @Test
    public void testGeoPackageRaster() {
        login();
        formatType = new GeoPackageFormat().getName();
        final CoverageStoreNewPage page = new CoverageStoreNewPage(formatType);
        tester.startPage(page);

        tester.debugComponentTrees();
        Component urlComponent = tester.getComponentFromLastRenderedPage("rasterStoreForm:parametersPanel:url");
        assertThat(urlComponent, instanceOf(FileParamPanel.class));
    }

    @Test
    public void testNewCoverageSave() {
        startPage();
        FormTester ft = tester.newFormTester("rasterStoreForm");
        ft.setValue("parametersPanel:url:fileInput:border:border_body:paramValue", "BlueMarble/tazbm.tiff");
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
        ft.setValue("parametersPanel:url:fileInput:border:border_body:paramValue", "BlueMarble/tazbm.tiff");
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
        ft.setValue("parametersPanel:url:fileInput:border:border_body:paramValue", "BlueMarble/tazbm.tiff");
        ft.setValue("namePanel:border:border_body:paramValue", "tazbm99");
        Component component =
                tester.getComponentFromLastRenderedPage("rasterStoreForm:disableOnConnFailurePanel:paramValue");
        CheckBox checkBox = (CheckBox) component;
        assertFalse(Boolean.valueOf(checkBox.getInput()).booleanValue());

        ft.setValue("disableOnConnFailurePanel:paramValue", true);

        ft.submit("save");

        tester.assertNoErrorMessage();
        CoverageStoreInfo store = getCatalog().getCoverageStoreByName("tazbm99");
        assertNotNull(store);
        assertTrue(store.isDisableOnConnFailure());
    }

    @Test
    public void testNewCoverageSandbox() throws Exception {
        // setup sandbox on file system and a random directory
        File systemSandbox = new File("./target/systemSandbox").getCanonicalFile();
        System.setProperty(GEOSERVER_DATA_SANDBOX, systemSandbox.getAbsolutePath());
        File testDir = new File("./target/test").getCanonicalFile();
        testDir.mkdirs();
        File bmFolder = new File(systemSandbox, "bm");
        bmFolder.mkdirs();
        GeoServerExtensions.bean(DefaultFileAccessManager.class).reload();

        // copy over tazbmInside in the two directories
        String fileName = "tazbm.tiff";
        File tazbmInside = new File(bmFolder, fileName);
        File tazbmOutside = new File(testDir, fileName);
        try (InputStream is = MockData.class.getResourceAsStream(fileName)) {
            FileUtils.copyToFile(is, tazbmInside);
        }
        try (InputStream is = MockData.class.getResourceAsStream(fileName)) {
            FileUtils.copyToFile(is, tazbmOutside);
        }

        try {
            // try to create a new coverage store outside of the sandbox
            startPage();
            FormTester ft = tester.newFormTester("rasterStoreForm");
            ft.setValue("parametersPanel:url:fileInput:border:border_body:paramValue", tazbmOutside.getAbsolutePath());
            ft.setValue("namePanel:border:border_body:paramValue", "tazbm4");
            ft.submit("apply");

            List<Serializable> messages = tester.getMessages(FeedbackMessage.ERROR);
            assertEquals(1, messages.size());
            checkSandboxDeniedMessage(messages.get(0).toString(), tazbmOutside);

            // the error got actually rendered
            checkSandboxDeniedMessage(tester.getLastResponseAsString(), tazbmOutside);

            // now save within the sandbox instead
            tester.clearFeedbackMessages();
            ft.setValue("parametersPanel:url:fileInput:border:border_body:paramValue", tazbmInside.getAbsolutePath());
            ft.submit("apply");

            tester.assertNoErrorMessage();

            tester.assertRenderedPage(CoverageStoreEditPage.class);
            CoverageStoreInfo store = getCatalog().getCoverageStoreByName("tazbm4");
            assertNotNull(store);
            // replace is for Windows
            assertEquals("file://" + tazbmInside.getAbsolutePath().replace("\\", "/"), store.getURL());
        } finally {
            System.clearProperty(GEOSERVER_DATA_SANDBOX);
            GeoServerExtensions.bean(DefaultFileAccessManager.class).reload();
        }
    }

    private static void checkSandboxDeniedMessage(String message, File tazbmOutside) {
        assertThat(
                message,
                allOf(
                        containsString("Access to "),
                        containsString(tazbmOutside.getAbsolutePath()),
                        containsString(" denied by file sandboxing")));
    }
}
