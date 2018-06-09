/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import static org.geoserver.jdbcconfig.JDBCConfigTestSupport.createTempDir;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.geoserver.jdbcloader.JDBCLoaderPropertiesFactoryBean;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.URLs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JDBCConfigPropertiesTest {

    protected static final String CONFIG_FILE = "jdbcconfig.properties";

    protected static final String CONFIG_SYSPROP = "jdbcconfig.properties";

    protected static final String JDBCURL_SYSPROP = "jdbcconfig.jdbcurl";

    protected static final String INITDB_SYSPROP = "jdbcconfig.initdb";

    protected static final String IMPORT_SYSPROP = "jdbcconfig.import";

    GeoServerResourceLoader loader;

    @Before
    public void setUp() throws IOException {
        loader = new GeoServerResourceLoader(createTempDir());
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(loader.getBaseDirectory());
    }

    @Test
    public void testLoadDefaults() throws IOException {
        JDBCConfigPropertiesFactoryBean factory = new JDBCConfigPropertiesFactoryBean(loader);
        JDBCConfigProperties props = (JDBCConfigProperties) factory.createProperties();

        assertFalse(props.isEnabled());
        assertTrue(props.isInitDb());
        assertTrue(props.isImport());

        // assert files copied over
        assertNotNull(loader.find("jdbcconfig", "jdbcconfig.properties"));
        assertNotNull(loader.find("jdbcconfig", "scripts", "initdb.postgres.sql"));

        // assert file location are accessible
        assertNotNull(factory.getFileLocations());

        // assert configuration can be stored successfully on another resource loader
        File tmpDir = org.geoserver.jdbcconfig.JDBCConfigTestSupport.createTempDir();
        Resources.directory(Files.asResource(tmpDir).get("jdbcconfig"), true);

        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(tmpDir);
        factory.saveConfiguration(resourceLoader);

        assertEquals(
                factory.getFileLocations().size(),
                (resourceLoader.find("jdbcconfig").list().length - 1)
                        + (resourceLoader.find("jdbcconfig/scripts").list().length));
    }

    private File createDummyConfigFile() throws IOException {
        Properties p = new Properties();
        p.put("foo", "bar");
        p.put("initdb", "false");
        p.put("import", "false");

        File configFile = new File(loader.getBaseDirectory(), "foo.properties");
        FileOutputStream fout = new FileOutputStream(configFile);
        p.store(fout, "");
        fout.flush();
        fout.close();

        return configFile;
    }

    @Test
    public void testLoadFromFile() throws Exception {
        File configFile = createDummyConfigFile();

        System.setProperty(CONFIG_SYSPROP, configFile.getAbsolutePath());
        try {
            JDBCLoaderPropertiesFactoryBean factory = new JDBCConfigPropertiesFactoryBean(loader);
            JDBCConfigProperties props = (JDBCConfigProperties) factory.createProperties();

            assertEquals("bar", props.getProperty("foo"));
            assertFalse(props.isInitDb());
            assertFalse(props.isImport());
        } finally {
            System.clearProperty(CONFIG_SYSPROP);
        }
    }

    @Test
    public void testLoadFromURL() throws Exception {
        File configFile = createDummyConfigFile();

        System.setProperty(CONFIG_SYSPROP, URLs.fileToUrl(configFile).toString());
        try {
            JDBCLoaderPropertiesFactoryBean factory = new JDBCConfigPropertiesFactoryBean(loader);
            JDBCConfigProperties props = (JDBCConfigProperties) factory.createProperties();

            assertEquals("bar", props.getProperty("foo"));
            assertFalse(props.isInitDb());
            assertFalse(props.isImport());
        } finally {
            System.clearProperty(CONFIG_SYSPROP);
        }
    }

    @Test
    public void testLoadFromSysProps() throws Exception {
        System.setProperty(JDBCURL_SYSPROP, "jdbc:h2:nofile");
        System.setProperty(INITDB_SYSPROP, "false");
        System.setProperty(IMPORT_SYSPROP, "false");

        try {
            JDBCLoaderPropertiesFactoryBean factory = new JDBCConfigPropertiesFactoryBean(loader);
            JDBCConfigProperties props = (JDBCConfigProperties) factory.createProperties();

            assertEquals("jdbc:h2:nofile", props.getJdbcUrl().get());
            assertFalse(props.isInitDb());
            assertFalse(props.isImport());
        } finally {
            System.clearProperty(JDBCURL_SYSPROP);
            System.clearProperty(INITDB_SYSPROP);
            System.clearProperty(IMPORT_SYSPROP);
        }
    }

    @Test
    public void testDataDirPlaceholder() throws Exception {
        JDBCConfigPropertiesFactoryBean factory = new JDBCConfigPropertiesFactoryBean(loader);
        JDBCConfigProperties props = (JDBCConfigProperties) factory.createProperties();
        props.setJdbcUrl("jdbc:h2:file:${GEOSERVER_DATA_DIR}");

        assertThat(
                props.getJdbcUrl().get(),
                containsString(loader.getBaseDirectory().getAbsolutePath()));
    }
}
