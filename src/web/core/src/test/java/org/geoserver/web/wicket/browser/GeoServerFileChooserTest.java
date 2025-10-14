/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket.browser;

import static org.geoserver.security.impl.DefaultFileAccessManager.GEOSERVER_DATA_SANDBOX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestHandler;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.Model;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.impl.DefaultFileAccessManager;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerDialog.DialogDelegate;
import org.junit.Before;
import org.junit.Test;

public class GeoServerFileChooserTest extends GeoServerWicketTestSupport {

    private static final String ROLE_CITE = "ROLE_CITE";
    private static final String ROLE_SF = "ROLE_SF";
    private File root;
    private File one;
    private File two;
    private File child;
    private DefaultFileAccessManager fam;

    @Before
    public void init() throws IOException {

        root = new File("target/test-filechooser");
        if (root.exists()) FileUtils.deleteDirectory(root);
        child = new File(root, "child");
        child.mkdirs();
        one = new File(child, "one.txt");
        one.createNewFile();
        two = new File(child, "two.sld");
        two.createNewFile();
    }

    @Before
    public void cleanupRestrictions() throws Exception {
        // clean up the security restrictions
        GeoServerDataDirectory dd = getDataDirectory();
        Resource layerSecurity = dd.get("security/layers.properties");
        Properties properties = new Properties();
        properties.put("*.*.r", "*");
        properties.put("*.*.w", "*");
        try (OutputStream os = layerSecurity.out()) {
            properties.store(os, "everyone can read and write");
        }

        // clear the system sandbox
        System.clearProperty(GEOSERVER_DATA_SANDBOX);

        // grab the file access manager and force reloading definitions
        fam = GeoServerExtensions.bean(DefaultFileAccessManager.class, applicationContext);
        fam.reload();
    }

    public void setupChooser(final File file) {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new GeoServerFileChooser(id, new Model<>(file))));

        // WicketHierarchyPrinter.print(tester.getLastRenderedPage(), true, true);
    }

    @Test
    public void testLoad() {
        setupChooser(root);

        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();

        tester.assertLabel("form:panel:fileTable:fileTable:fileContent:files:1:nameLink:name", "child/");
        assertEquals(
                1,
                ((DataView) tester.getComponentFromLastRenderedPage("form:panel:fileTable:fileTable:fileContent:files"))
                        .size());
    }

    @Test
    public void testNullRoot() {
        setupChooser(null);

        // make sure it does not now blow out because of the null
        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();

        tester.assertLabel(
                "form:panel:breadcrumbs:path:0:pathItemLink:pathItem",
                getTestData().getDataDirectoryRoot().getName() + "/");
    }

    @Test
    public void testInDialog() throws Exception {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new GeoServerDialog(id)));

        tester.assertRenderedPage(FormTestPage.class);

        tester.debugComponentTrees();

        GeoServerDialog dialog = (GeoServerDialog) tester.getComponentFromLastRenderedPage("form:panel");
        assertNotNull(dialog);

        dialog.showOkCancel(new AjaxRequestHandler(tester.getLastRenderedPage()), new DialogDelegate() {
            @Override
            protected Component getContents(String id) {
                return new GeoServerFileChooser(id, new Model<>(root));
            }

            @Override
            protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                assertNotNull(contents);
                assertTrue(contents instanceof GeoServerFileChooser);
                return true;
            }
        });

        dialog.submit(new AjaxRequestHandler(tester.getLastRenderedPage()));
    }

    @Test
    public void testHideFileSystem() throws Exception {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new GeoServerFileChooser(id, new Model<>(), true)));

        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();

        @SuppressWarnings("unchecked")
        DropDownChoice<File> rootChoice =
                (DropDownChoice<File>) tester.getComponentFromLastRenderedPage("form:panel:roots");
        assertEquals(1, rootChoice.getChoices().size());
        assertEquals(getDataDirectory().root(), rootChoice.getChoices().get(0));
    }

    @Test
    public void testAutocompleteDataDirectory() throws Exception {
        FileRootsFinder rootsFinder = new FileRootsFinder(true);
        getDataDirectory().get("/").dir();

        // looking for a match on basic polygons
        List<String> values =
                rootsFinder.getMatches(MockData.CITE_PREFIX + "/poly", null).collect(Collectors.toList());
        assertEquals(1, values.size());
        assertEquals("file:cite/BasicPolygons.properties", values.get(0));

        // for the sake of checking, find a specific style with a file extension filter (the dd
        // contains both
        // raster.sld and raster.xml
        values = rootsFinder
                .getMatches("/styles/raster", new ExtensionFileFilter(".sld"))
                .collect(Collectors.toList());
        assertEquals(1, values.size());
        assertEquals("file:styles/raster.sld", values.get(0));
    }

    @Test
    public void testAutocompleteDirectories() throws Exception {
        FileRootsFinder rootsFinder = new FileRootsFinder(true);
        File dir = getDataDirectory().get("/").dir();

        // look for a property file in the data dir full path (so, not a relative path match).
        // we should still get directories
        String rootPath = dir.getCanonicalFile().getAbsolutePath() + File.separator;
        ExtensionFileFilter fileFilter = new ExtensionFileFilter(".properties");
        List<String> values = rootsFinder.getMatches(rootPath, fileFilter).collect(Collectors.toList());
        assertThat(values.size(), greaterThan(0));
        assertEquals(new HashSet<>(values).size(), values.size());
        assertThat(values, hasItem("file://" + new File(rootPath, MockData.CITE_PREFIX).getAbsolutePath()));
        assertThat(values, hasItem("file://" + new File(rootPath, MockData.SF_PREFIX).getAbsolutePath()));
    }

    @Test
    public void testAdminSandbox() throws Exception {
        // setup sandbox on file system
        File systemSandbox = new File("./target/fc-systemSandbox").getCanonicalFile();
        File citeFolder = new File(systemSandbox, MockData.CITE_PREFIX);
        File sfFolder = new File(systemSandbox, MockData.SF_PREFIX);
        citeFolder.mkdirs();
        sfFolder.mkdirs();

        // configure security, make sure the file access manager pays attention
        System.setProperty(DefaultFileAccessManager.GEOSERVER_DATA_SANDBOX, systemSandbox.getAbsolutePath());
        fam.reload();

        // roots finder limits to that directory
        login("admin", "geoserver", GeoServerRole.ADMIN_ROLE.getAuthority());
        FileRootsFinder rootsFinder = new FileRootsFinder(true);
        ArrayList<File> files = rootsFinder.getRoots();
        assertEquals(List.of(systemSandbox), files);

        // check autocomplete
        List<String> values = rootsFinder
                .getMatches(systemSandbox.getAbsolutePath() + File.separator, null)
                .collect(Collectors.toList());
        assertEquals(List.of("file://" + citeFolder.getAbsoluteFile(), "file://" + sfFolder.getAbsoluteFile()), values);
    }

    @Test
    public void testWorkspaceSandbox() throws Exception {
        // setup sandbox on file system
        File sandbox = new File("./target/sandbox").getCanonicalFile();
        File citeFolder = new File(sandbox, MockData.CITE_PREFIX);
        File sfFolder = new File(sandbox, MockData.SF_PREFIX);
        File toppFolder = new File(sandbox, "topp"); // this won't be allowed
        citeFolder.mkdirs();
        sfFolder.mkdirs();
        toppFolder.mkdirs();

        // setup a sandbox by security config
        Resource layerSecurity = getDataDirectory().get("security/layers.properties");
        Properties properties = new Properties();
        properties.put("filesystemSandbox", sandbox.getAbsolutePath());
        properties.put("cite.*.a", ROLE_CITE);
        properties.put("sf.*.a", ROLE_SF);
        try (OutputStream os = layerSecurity.out()) {
            properties.store(os, "sandbox");
        }

        // force reloading definitions
        fam.reload();

        // roots finder limits two out of three of the available directories
        login("multi", "multi", ROLE_CITE, ROLE_SF);
        FileRootsFinder rootsFinder = new FileRootsFinder(true);
        ArrayList<File> files = rootsFinder.getRoots();
        assertEquals(List.of(citeFolder, sfFolder), files);

        // check autocomplete
        List<String> values = rootsFinder
                .getMatches(sandbox.getAbsolutePath() + File.separator, null)
                .collect(Collectors.toList());
        assertEquals(List.of("file://" + citeFolder.getAbsoluteFile(), "file://" + sfFolder.getAbsoluteFile()), values);
    }
}
