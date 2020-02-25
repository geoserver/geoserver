/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import java.io.File;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerSecurityManager;
import org.junit.After;
import org.junit.Before;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Basic class for LDAP related tests.
 *
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 */
public abstract class LDAPBaseTest extends AbstractLdapTestUnit {
    protected GeoServerSecurityManager securityManager;
    protected LDAPSecurityProvider securityProvider;

    protected Authentication authentication;
    protected Authentication authenticationOther;
    protected Authentication authenticationNested;
    protected LDAPBaseSecurityServiceConfig config;
    private File tempFolder;

    public static final String ldapServerUrl = LDAPTestUtils.LDAP_SERVER_URL;
    public static final String basePath = LDAPTestUtils.LDAP_BASE_PATH;

    @Before
    public void setUp() throws Exception {

        tempFolder = File.createTempFile("ldap", "test");
        tempFolder.delete();
        tempFolder.mkdirs();
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(tempFolder);
        securityManager = new GeoServerSecurityManager(new GeoServerDataDirectory(resourceLoader));
        securityProvider = new LDAPSecurityProvider(securityManager);

        createConfig();
        config.setServerURL(ldapServerUrl + "/" + basePath);
        config.setGroupSearchBase("ou=Groups");
        config.setGroupSearchFilter("member=cn={1}");
        config.setUseTLS(false);

        authentication = new UsernamePasswordAuthenticationToken("admin", "admin");
        authenticationOther = new UsernamePasswordAuthenticationToken("other", "other");
        authenticationNested = new UsernamePasswordAuthenticationToken("nestedUser", "other");
    }

    protected abstract void createConfig();

    @After
    public void tearDown() throws Exception {
        tempFolder.delete();

        if (SecurityContextHolder.getContext() != null) {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }
}
