/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.urlchecks;

import static org.geotools.data.ows.URLCheckers.confirm;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.InputStream;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.ows.URLCheckerException;
import org.junit.Test;

public class StyleURLCheckerTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no need for all test data, workspaces are enough
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // pick style with relative icon reference from gs-main tests
        testData.addStyle("burg", "burg.sld", GeoServer.class, getCatalog());
        try (InputStream is = GeoServer.class.getResourceAsStream("burg02.svg")) {
            testData.copyTo(is, "styles/burg02.svg");
        }

        // do the same, but this time inside a workspace
        WorkspaceInfo cite = getCatalog().getWorkspaceByName(SystemTestData.CITE_PREFIX);
        testData.addStyle(cite, "burg", "burg.sld", GeoServer.class, getCatalog());
        getDataDirectory().getStyles(cite).dir().mkdirs();
        try (InputStream is = GeoServer.class.getResourceAsStream("burg02.svg")) {
            testData.copyTo(is, "workspaces/cite/styles/burg02.svg");
        }

        // force creation of the URL checker, beans are lazy loaded
        assertNotNull(applicationContext.getBean(StyleURLChecker.class));
    }

    @Test
    public void testValidStyleIcons() throws Exception {
        File root = getDataDirectory().root();
        confirm(new File(root, "styles/burg02.svg").getPath());
        confirm(new File(root, "workspaces/cite/styles/burg02.svg").getPath());
    }

    @Test
    public void testNonExistingStyleIcons() throws Exception {
        // they are acceptable, even if not existing, the graphic factories will deal with them
        // but we don't want to go out and say they are not allowed, admins can get confused
        File root = getDataDirectory().root();
        confirm(new File(root, "styles/foobar.svg").getPath());
        confirm(new File(root, "workspaces/cite/styles/foobar.svg").getPath());
    }

    @Test
    public void testRelativeOutsideDataDir() throws Exception {
        // not acceptable, not part of the data directory
        File icon = new File("./target/test.png");
        assertURLException(icon);
    }

    @Test
    public void testAbsoluteOutsideDataDir() throws Exception {
        // not acceptable, not part of the data directory
        File icon = new File("./target/test.png").getCanonicalFile();
        assertURLException(icon);
    }

    @Test
    public void testOutsideDataDirectory() throws Exception {
        // not acceptable, trying to escape the data directory
        File root = getDataDirectory().root();
        assertURLException(new File(root, "workspaces/foobar/styles/../../../../test.png"));
        // convoluted but acceptable, refers to icon in the global directory
        confirm(new File(root, "workspaces/foobar/styles/../../../styles/test.png").getPath());
        // not acceptable, within the data dir but sticking nose outside style dirs
        assertURLException(new File(root, "styles/../security/data.properties"));
    }

    @Test
    public void testGlobalStylesReference() throws Exception {
        // convoluted but acceptable, refers to icon in the global directory
        File root = getDataDirectory().root();
        confirm(new File(root, "workspaces/foobar/styles/../../../styles/test.png").getPath());
    }

    @Test
    public void testOtherDirectories() throws Exception {
        // not acceptable, within the data dir but sticking nose outside style dirs
        File root = getDataDirectory().root();
        assertURLException(new File(root, "styles/../security/data.properties"));
    }

    private static void assertURLException(File file) {
        assertThrows(URLCheckerException.class, () -> confirm(file.getPath()));
    }
}
