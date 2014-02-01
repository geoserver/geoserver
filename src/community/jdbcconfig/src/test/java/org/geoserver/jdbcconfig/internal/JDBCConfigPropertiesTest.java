package org.geoserver.jdbcconfig.internal;

import static org.junit.Assert.*;
import static org.geoserver.jdbcconfig.JDBCConfigTestSupport.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.data.DataUtilities;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JDBCConfigPropertiesTest {

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

        //assert files copied over
        assertNotNull(loader.find("jdbcconfig", "jdbcconfig.properties"));
        assertNotNull(loader.find("jdbcconfig", "scripts", "initdb.postgres.sql"));
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

        System.setProperty(
            JDBCConfigPropertiesFactoryBean.CONFIG_SYSPROP, configFile.getAbsolutePath());
        try {
            JDBCConfigPropertiesFactoryBean factory = new JDBCConfigPropertiesFactoryBean(loader);
            JDBCConfigProperties props = (JDBCConfigProperties) factory.createProperties();

            assertEquals("bar", props.getProperty("foo"));
            assertFalse(props.isInitDb());
            assertFalse(props.isImport());
        }
        finally {
            System.clearProperty(JDBCConfigPropertiesFactoryBean.CONFIG_SYSPROP);
        }
    }

    @Test
    public void testLoadFromURL() throws Exception {
        File configFile = createDummyConfigFile();

        System.setProperty( JDBCConfigPropertiesFactoryBean.CONFIG_SYSPROP, 
            DataUtilities.fileToURL(configFile).toString());
        try {
            JDBCConfigPropertiesFactoryBean factory = new JDBCConfigPropertiesFactoryBean(loader);
            JDBCConfigProperties props = (JDBCConfigProperties) factory.createProperties();

            assertEquals("bar", props.getProperty("foo"));
            assertFalse(props.isInitDb());
            assertFalse(props.isImport());
        }
        finally {
            System.clearProperty(JDBCConfigPropertiesFactoryBean.CONFIG_SYSPROP);
        }
    }

    @Test
    public void testLoadFromSysProps() throws Exception {
        System.setProperty( JDBCConfigPropertiesFactoryBean.JDBCURL_SYSPROP, "jdbc:h2:nofile");
        System.setProperty( JDBCConfigPropertiesFactoryBean.INITDB_SYSPROP, "false");
        System.setProperty( JDBCConfigPropertiesFactoryBean.IMPORT_SYSPROP, "false");
        
        try {
            JDBCConfigPropertiesFactoryBean factory = new JDBCConfigPropertiesFactoryBean(loader);
            JDBCConfigProperties props = (JDBCConfigProperties) factory.createProperties();
    
            assertEquals("jdbc:h2:nofile", props.getJdbcUrl().get());
            assertFalse(props.isInitDb());
            assertFalse(props.isImport());
        }
        finally {
            System.clearProperty( JDBCConfigPropertiesFactoryBean.JDBCURL_SYSPROP);
            System.clearProperty( JDBCConfigPropertiesFactoryBean.INITDB_SYSPROP);
            System.clearProperty( JDBCConfigPropertiesFactoryBean.IMPORT_SYSPROP);
        }
    }

    @Test
    public void testDataDirPlaceholder() throws Exception {
        JDBCConfigPropertiesFactoryBean factory = new JDBCConfigPropertiesFactoryBean(loader);
        JDBCConfigProperties props = (JDBCConfigProperties) factory.createProperties();
        props.setJdbcUrl("jdbc:h2:file:${GEOSERVER_DATA_DIR}/jdbcconfig/catalog;AUTO_SERVER=TRUE");
        
        assertThat(props.getJdbcUrl().get(), containsString(loader.getBaseDirectory().getAbsolutePath()));
    }
}
