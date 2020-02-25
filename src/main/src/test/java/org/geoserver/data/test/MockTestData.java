/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.test;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.test.GeoServerMockTestSupport;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.util.IOUtils;

/**
 * Test setup uses for GeoServer mock tests.
 *
 * <p>This is the default test setup used by {@link GeoServerMockTestSupport}. During setup this
 * class creates a catalog whose contents contain all the layers defined by {@link CiteTestData}
 *
 * <p>Customizing the setup, adding layers, etc... is done from {@link
 * GeoServerSystemTestSupport#setUpTestData}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class MockTestData extends CiteTestData {

    File data;
    Catalog catalog;
    GeoServerSecurityManager secMgr;
    MockCreator mockCreator;
    boolean includeRaster;

    public MockTestData() throws IOException {
        // setup the root
        data = IOUtils.createRandomDirectory("./target", "mock", "data");
        data.delete();
        data.mkdir();

        mockCreator = new MockCreator();
    }

    public void setMockCreator(MockCreator mockCreator) {
        this.mockCreator = mockCreator;
    }

    public boolean isInludeRaster() {
        return includeRaster;
    }

    public void setIncludeRaster(boolean includeRaster) {
        this.includeRaster = includeRaster;
    }

    public Catalog getCatalog() {
        if (catalog == null) {
            try {
                catalog = mockCreator.createCatalog(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return catalog;
    }

    public GeoServerSecurityManager getSecurityManager() {
        if (secMgr == null) {
            try {
                secMgr = mockCreator.createSecurityManager(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return secMgr;
    }

    @Override
    public void setUp() throws Exception {}

    @Override
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(data);
    }

    @Override
    public File getDataDirectoryRoot() {
        return data;
    }

    @Override
    public boolean isTestDataAvailable() {
        return true;
    }
}
