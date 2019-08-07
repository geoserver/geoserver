/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.catalog;

import static org.geoserver.config.FileExistsMatcher.fileExists;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.config.GeoServerPersistersTest;
import org.geoserver.config.GeoServerResourcePersister;
import org.geoserver.jdbcconfig.JDBCConfigTestSupport;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JDBGeoserverResourcePersisterTest {

    private JDBCCatalogFacade facade;

    private JDBCConfigTestSupport testSupport;

    private Catalog catalog;

    public JDBGeoserverResourcePersisterTest(JDBCConfigTestSupport.DBConfig dbConfig) {
        testSupport = new JDBCConfigTestSupport(dbConfig);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return JDBCConfigTestSupport.parameterizedDBConfigs();
    }

    @Before
    public void initCatalog() throws Exception {
        testSupport.setUp();

        ConfigDatabase configDb = testSupport.getDatabase();
        facade = new JDBCCatalogFacade(configDb);
        CatalogImpl catalogImpl = new CatalogImpl();
        catalogImpl.setFacade(facade);
        catalogImpl.setResourceLoader(testSupport.getResourceLoader());
        catalogImpl.addListener(new GeoServerResourcePersister(catalogImpl));

        WorkspaceInfo gs = new WorkspaceInfoImpl();
        gs.setName("gs");
        catalogImpl.add(gs);

        this.catalog = catalogImpl;

        GeoServerExtensionsHelper.singleton(
                "sldHandler", new org.geoserver.catalog.SLDHandler(), StyleHandler.class);
        new File(testSupport.getResourceLoader().getBaseDirectory(), "styles").mkdir();
    }

    @After
    public void tearDown() throws Exception {
        facade.dispose();
        testSupport.tearDown();
    }

    public void addStyle() throws Exception {
        StyleInfo s = catalog.getFactory().createStyle();
        s.setName("foostyle");
        s.setFilename("foostyle.sld");
        catalog.add(s);
    }

    public void addStyleWithWorkspace() throws Exception {
        StyleInfo s = catalog.getFactory().createStyle();
        s.setName("foostyle");
        s.setFilename("foostyle.sld");
        s.setWorkspace(catalog.getDefaultWorkspace());
        catalog.add(s);
    }

    @Test
    public void testRemoveStyle() throws Exception {
        addStyle();

        File sf =
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld");
        sf.createNewFile();
        assertTrue(sf.exists());

        StyleInfo s = catalog.getStyleByName("foostyle");
        catalog.remove(s);

        assertThat(sf, not(fileExists()));

        File sfb =
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "styles/foostyle.sld.bak");
        assertThat(sfb, fileExists());

        // do it a second time

        addStyle();
        sf = new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld");
        sf.createNewFile();
        assertTrue(sf.exists());

        s = catalog.getStyleByName("foostyle");
        catalog.remove(s);

        assertThat(sf, not(fileExists()));

        sfb =
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "styles/foostyle.sld.bak.1");
        assertThat(sfb, fileExists());
    }

    @Test
    public void testRenameStyle() throws Exception {
        addStyle();
        File sldFile =
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld");
        sldFile.createNewFile();

        StyleInfo s = catalog.getStyleByName("foostyle");
        s.setName("boostyle");
        catalog.save(s);

        File renamedSldFile =
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/boostyle.sld");
        assertThat(sldFile, not(fileExists()));
        assertThat(renamedSldFile, fileExists());
    }

    @Test
    public void testRenameStyleConflict() throws Exception {
        addStyle();
        File sldFile =
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld");
        sldFile.createNewFile();
        File conflictingFile =
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/boostyle.sld");
        conflictingFile.createNewFile();

        StyleInfo s = catalog.getStyleByName("foostyle");
        s.setName("boostyle");
        catalog.save(s);

        File renamedSldFile =
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(), "styles/boostyle1.sld");
        assertThat(sldFile, not(fileExists()));
        assertThat(renamedSldFile, fileExists());
    }

    @Test
    public void testRenameStyleWithExistingIncrementedVersion() throws Exception {
        addStyle();

        File sldFile =
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld");
        sldFile.createNewFile();

        File sldFile0 =
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/boostyle.sld");
        sldFile0.createNewFile();

        File sldFile1 =
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(), "styles/boostyle1.sld");
        sldFile1.createNewFile();

        File sldFile2 =
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(), "styles/boostyle2.sld");

        StyleInfo s = catalog.getStyleByName("foostyle");
        s.setName("boostyle");
        catalog.save(s);

        assertThat(sldFile, not(fileExists()));
        assertThat(sldFile0, fileExists());
        assertThat(sldFile1, fileExists());
        assertThat(sldFile2, fileExists());

        sldFile1.delete();
    }

    @Test
    public void testModifyStyleChangeWorkspace() throws Exception {
        addStyle();

        // copy an sld into place
        FileUtils.copyURLToFile(
                GeoServerPersistersTest.class.getResource("default_line.sld"),
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld"));

        assertTrue(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld")
                        .exists());

        StyleInfo s = catalog.getStyleByName("foostyle");
        s.setWorkspace(catalog.getDefaultWorkspace());
        catalog.save(s);

        assertFalse(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld")
                        .exists());
        assertTrue(
                new File(
                                testSupport.getResourceLoader().getBaseDirectory(),
                                "workspaces/gs/styles/foostyle.sld")
                        .exists());
    }

    @Test
    public void testModifyStyleChangeWorkspaceToGlobal() throws Exception {
        addStyleWithWorkspace();

        // copy an sld into place
        FileUtils.copyURLToFile(
                GeoServerPersistersTest.class.getResource("default_line.sld"),
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "workspaces/gs/styles/foostyle.sld"));

        assertTrue(
                new File(
                                testSupport.getResourceLoader().getBaseDirectory(),
                                "workspaces/gs/styles/foostyle.sld")
                        .exists());

        StyleInfo s = catalog.getStyleByName("foostyle");
        s.setWorkspace(null);
        catalog.save(s);

        assertTrue(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld")
                        .exists());
        assertFalse(
                new File(
                                testSupport.getResourceLoader().getBaseDirectory(),
                                "workspaces/gs/styles/foostyle.sld")
                        .exists());
    }

    @Test
    public void testModifyStyleWithResourceChangeWorkspace() throws Exception {
        addStyle();

        // copy an sld with its resource into place
        FileUtils.copyURLToFile(
                GeoServerPersistersTest.class.getResource("burg.sld"),
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld"));
        FileUtils.copyURLToFile(
                GeoServerPersistersTest.class.getResource("burg02.svg"),
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/burg02.svg"));

        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld"),
                fileExists());
        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/burg02.svg"),
                fileExists());

        StyleInfo s = catalog.getStyleByName("foostyle");
        s.setWorkspace(catalog.getDefaultWorkspace());
        catalog.save(s);

        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld"),
                not(fileExists()));
        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/burg02.svg"),
                fileExists());

        assertThat(
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "workspaces/gs/styles/foostyle.sld"),
                fileExists());
        assertThat(
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "workspaces/gs/styles/burg02.svg"),
                fileExists());
    }

    @Test
    public void testModifyStyleWithResourcesInParentDirChangeWorkspace() throws Exception {
        addStyle();

        // If a relative URI with parent references is used, give up on trying to copy the resource.
        // The style will break but copying arbitrary files from parent directories around is a bad
        // idea. Handle the rest normally. KS

        FileUtils.copyURLToFile(
                GeoServerPersistersTest.class.getResource("burgParentReference.sld"),
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld"));
        FileUtils.copyURLToFile(
                GeoServerPersistersTest.class.getResource("burg02.svg"),
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/burg02.svg"));
        FileUtils.copyURLToFile(
                GeoServerPersistersTest.class.getResource("burg02.svg"),
                new File(testSupport.getResourceLoader().getBaseDirectory(), "burg03.svg"));

        new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/burg03.svg").delete();

        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld"),
                fileExists());
        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/burg02.svg"),
                fileExists());
        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "burg03.svg"),
                fileExists());
        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/burg03.svg"),
                not(fileExists()));

        StyleInfo s = catalog.getStyleByName("foostyle");

        s.setWorkspace(catalog.getDefaultWorkspace());
        catalog.save(s);

        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld"),
                not(fileExists()));
        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "burg03.svg"),
                fileExists());
        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/burg02.svg"),
                fileExists());

        assertThat(
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "workspaces/gs/styles/foostyle.sld"),
                fileExists());
        assertThat(
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "workspaces/gs/styles/burg03.svg"),
                not(fileExists()));
        assertThat(
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "workspaces/gs/burg03.svg"),
                not(fileExists()));
        assertThat(
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "workspaces/gs/styles/burg02.svg"),
                fileExists());
    }

    @Test
    public void testModifyStyleWithResourcesAbsoluteChangeWorkspace() throws Exception {
        addStyle();

        // If an absolute uri is used, don't copy it anywhere. The reference is absolute
        // so it will still work.

        File styleFile =
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld");
        FileUtils.copyURLToFile(
                GeoServerPersistersTest.class.getResource("burgParentReference.sld"), styleFile);
        FileUtils.copyURLToFile(
                GeoServerPersistersTest.class.getResource("burg02.svg"),
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/burg02.svg"));
        File target = new File(testSupport.getResourceLoader().getBaseDirectory(), "burg03.svg");
        FileUtils.copyURLToFile(GeoServerPersistersTest.class.getResource("burg02.svg"), target);

        // Insert an absolute path to test
        String content = new String(Files.readAllBytes(styleFile.toPath()), StandardCharsets.UTF_8);
        content = content.replaceAll("./burg03.svg", "http://doesnotexist.example.org/burg03.svg");
        Files.write(styleFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
        new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/burg03.svg").delete();

        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld"),
                fileExists());
        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/burg02.svg"),
                fileExists());
        assertThat(target, fileExists());
        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/burg03.svg"),
                not(fileExists()));

        StyleInfo s = catalog.getStyleByName("foostyle");

        s.setWorkspace(catalog.getDefaultWorkspace());
        catalog.save(s);

        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld"),
                not(fileExists()));
        assertThat(target, fileExists());
        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/burg02.svg"),
                fileExists());

        assertThat(
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "workspaces/gs/styles/foostyle.sld"),
                fileExists());
        assertThat(
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "workspaces/gs/styles/burg03.svg"),
                not(fileExists()));
        assertThat(
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "workspaces/gs/burg03.svg"),
                not(fileExists()));
        assertThat(
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "workspaces/gs" + target.getPath()),
                not(fileExists()));
        assertThat(
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "workspaces/gs/styles" + target.getPath()),
                not(fileExists()));
        assertThat(
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "workspaces/gs/styles/burg02.svg"),
                fileExists());
    }

    @Test
    public void testModifyStyleWithResourcesRemoteChangeWorkspace() throws Exception {
        addStyle();

        // If an absolute uri is used, don't copy it anywhere. The reference is absolute
        // so it will still work.

        File styleFile =
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld");
        FileUtils.copyURLToFile(
                GeoServerPersistersTest.class.getResource("burgRemoteReference.sld"), styleFile);
        FileUtils.copyURLToFile(
                GeoServerPersistersTest.class.getResource("burg02.svg"),
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/burg02.svg"));

        new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/burg03.svg").delete();
        new File(testSupport.getResourceLoader().getBaseDirectory(), "burg03.svg").delete();

        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld"),
                fileExists());
        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/burg02.svg"),
                fileExists());
        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/burg03.svg"),
                not(fileExists()));
        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "burg03.svg"),
                not(fileExists()));

        StyleInfo s = catalog.getStyleByName("foostyle");

        s.setWorkspace(catalog.getDefaultWorkspace());
        catalog.save(s);

        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/foostyle.sld"),
                not(fileExists()));
        assertThat(
                new File(testSupport.getResourceLoader().getBaseDirectory(), "styles/burg02.svg"),
                fileExists());

        assertThat(
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "workspaces/gs/styles/foostyle.sld"),
                fileExists());
        assertThat(
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "workspaces/gs/styles/burg03.svg"),
                not(fileExists()));
        assertThat(
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "workspaces/gs/burg03.svg"),
                not(fileExists()));
        assertThat(
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "workspaces/gs/example.com/burg03.svg"),
                not(fileExists()));
        assertThat(
                new File(
                        testSupport.getResourceLoader().getBaseDirectory(),
                        "workspaces/gs/styles/burg02.svg"),
                fileExists());
    }
}
