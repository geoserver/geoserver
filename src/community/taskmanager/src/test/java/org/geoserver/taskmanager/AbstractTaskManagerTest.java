package org.geoserver.taskmanager;

import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Abstract test class.
 * 
 * @author Niels Charlier
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath*:/applicationContext.xml", "classpath*:/applicationSecurityContext.xml"})
@WebAppConfiguration //we need web app context to have data directory set.
public abstract class AbstractTaskManagerTest {    
    
    protected static MockData DATA_DIRECTORY;

    @Autowired
    protected GeoServer geoServer;

    @BeforeClass
    public static void init() throws Exception {
        //set data directory
        DATA_DIRECTORY = new MockData();
        System.setProperty("GEOSERVER_DATA_DIR", 
                DATA_DIRECTORY.getDataDirectoryRoot().toString());

    }
    
    protected boolean setupDataDirectory() throws Exception {
        return false;
    }
    
    @Before
    public final void setupAndLoadDataDirectory() throws Exception {        
        if (setupDataDirectory()) {
            DATA_DIRECTORY.setUp();
            geoServer.reload();
        }
    }
    
}
