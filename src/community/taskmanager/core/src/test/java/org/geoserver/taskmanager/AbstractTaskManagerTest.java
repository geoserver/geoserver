/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Abstract test class.
 *
 * @author Niels Charlier
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({
    "classpath*:/applicationContext.xml",
    "classpath*:/applicationSecurityContext.xml"
})
@WebAppConfiguration // we need web app context to have data directory set.
public abstract class AbstractTaskManagerTest {

    protected static MockData DATA_DIRECTORY;

    @Autowired protected GeoServer geoServer;

    @BeforeClass
    public static void init() throws Exception {
        // Disable CSRF protection for tests, since the test framework doesn't set the Referer
        // System.setProperty(GEOSERVER_CSRF_DISABLED, "true");

        URL s3propertiesFile = AbstractTaskManagerTest.class.getResource("s3.properties");
        if (s3propertiesFile != null) {
            System.setProperty("s3.properties.location", s3propertiesFile.getFile());
        }

        if (DATA_DIRECTORY == null) {
            // set data directory
            DATA_DIRECTORY = new MockData();
            System.setProperty(
                    "GEOSERVER_DATA_DIR", DATA_DIRECTORY.getDataDirectoryRoot().toString());
        }
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

    @After
    public void cleanDataDirectory() throws Exception {
        for (File file :
                DATA_DIRECTORY
                        .getDataDirectoryRoot()
                        .listFiles(
                                new FilenameFilter() {
                                    @Override
                                    public boolean accept(java.io.File dir, String name) {
                                        return !name.equals("taskmanager");
                                    }
                                })) {
            if (file.isDirectory()) {
                FileUtils.cleanDirectory(file);
            } else {
                file.delete();
            }
        }
    }

    /**
     * Sets up the authentication context for the test.
     *
     * <p>This context lasts only for a single test case, it is cleared after every test has
     * completed.
     *
     * @param username The username.
     * @param password The password.
     * @param roles Roles to assign.
     */
    protected void login(String username, String password, String... roles) {
        SecurityContextHolder.setContext(new SecurityContextImpl());
        List<GrantedAuthority> l = new ArrayList<GrantedAuthority>();
        for (String role : roles) {
            l.add(new SimpleGrantedAuthority(role));
        }

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(username, password, l));
    }

    protected void logout() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }
}
