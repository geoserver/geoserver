/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcstore;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.geoserver.platform.resource.ResourceMatchers.directory;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

import java.io.OutputStream;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.geoserver.jdbcstore.internal.JDBCResourceStoreProperties;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceTheoryTest;
import org.junit.After;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theory;

/**
 * @author Kevin Smith, Boundless
 * @author Niels Charlier
 */
public abstract class AbstractJDBCResourceTheoryTest extends ResourceTheoryTest {

    DatabaseTestSupport support;

    @DataPoints
    public static String[] testPaths() {
        return new String[] {
            "FileA",
            "FileB",
            "DirC",
            "DirC/FileD",
            "DirE",
            "UndefF",
            "DirC/UndefF",
            "DirE/UndefF" /*, "DirE/UndefG/UndefH/UndefI"*/
        };
    }

    protected JDBCResourceStoreProperties mockConfig(boolean enabled, boolean init) {
        JDBCResourceStoreProperties config = createMock(JDBCResourceStoreProperties.class);

        expect(config.isInitDb()).andStubReturn(init);
        expect(config.isEnabled()).andStubReturn(enabled);
        expect(config.isImport()).andStubReturn(init);

        support.stubConfig(config);

        return config;
    }

    protected DataSource testDataSource() throws Exception {
        return support.getDataSource();
    }

    public AbstractJDBCResourceTheoryTest() {
        super();
    }

    protected void standardData() throws Exception {
        support.initialize();

        support.addFile("FileA", 0, "FileA Contents".getBytes());
        support.addFile("FileB", 0, "FileB Contents".getBytes());
        int c = support.addDir("DirC", 0);
        support.addFile("FileD", c, "FileD Contents".getBytes());
        support.addDir("DirE", 0);
    }

    Integer getInt(ResultSet rs, String column) throws Exception {
        int i = rs.getInt(column);
        if (rs.wasNull()) return null;
        return i;
    }

    @After
    public void cleanUp() throws Exception {
        support.close();
    }

    @Override
    protected Resource getDirectory() {
        return getStore().get("/DirE");
    }

    @Override
    protected Resource getResource() {
        return getStore().get("/DirC/FileD");
    }

    @Override
    protected Resource getUndefined() {
        return getStore().get("/un/de/fined");
    }

    @Override
    protected Resource getResource(String path) throws Exception {
        return getStore().get(path);
    }

    protected abstract JDBCResourceStore getStore();

    @Override
    public void theoryAlteringFileAltersResource(String path) throws Exception {
        // disabled
    }

    @Override
    public void theoryAddingFileToDirectoryAddsResource(String path) throws Exception {
        // disabled
    }

    @Theory
    public void theoryModifiedUpdated(String path) throws Exception {
        Resource res = getResource(path);

        assumeThat(res, not(directory()));

        long lastmod = res.lastmodified();

        OutputStream out = res.out();
        out.write(1234);
        out.close();

        assertTrue(res.lastmodified() > lastmod);
    }
}
