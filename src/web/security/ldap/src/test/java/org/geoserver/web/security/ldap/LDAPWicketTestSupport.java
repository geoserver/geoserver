/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.ldap;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.ldap.LDAPTestUtils;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.junit.AfterClass;
import org.junit.BeforeClass;

@CreateLdapServer(
        transports = {@CreateTransport(protocol = "LDAP", address = "localhost")},
        allowAnonymousAccess = true)
@CreateDS(
        name = "myDS",
        partitions = {@CreatePartition(name = "test", suffix = LDAPTestUtils.LDAP_BASE_PATH)})
public class LDAPWicketTestSupport extends AbstractSecurityWicketTestSupport {

    protected static final String GROUPS_BASE = "ou=Groups";

    protected static final String USERS_BASE = "ou=People";

    protected static final String GROUP_SEARCH_FILTER = "member=cn={0}";

    protected static final String AUTH_USER = "admin";

    protected static final String AUTH_PASSWORD = "secret";

    protected static DirectoryService directoryService;

    protected static LdapServer ldapServer;

    protected static final String ldapServerUrl = LDAPTestUtils.LDAP_SERVER_URL;
    protected static final String basePath = LDAPTestUtils.LDAP_BASE_PATH;

    @BeforeClass
    public static void setupLdapServer() throws Exception {
        // 1. Create and start the directory service
        directoryService = DSAnnotationProcessor.getDirectoryService();

        // Get the schema manager from the service
        SchemaManager schemaManager = directoryService.getSchemaManager();

        // 2. Programmatically apply the LDIF
        try (InputStream is = LDAPAuthProviderPanelTest.class.getResourceAsStream("/data.ldif");
                BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            if (is == null) {
                throw new RuntimeException("Could not find data.ldif in classpath");
            }
            try (LdifReader reader = new LdifReader(schemaManager)) {
                for (LdifEntry ldifEntry : reader.parseLdif(br)) {
                    // Add it via the admin session
                    directoryService.getAdminSession().add(ldifEntry.getEntry());
                }
            }
        }

        // 3. Create and start the LDAP server, using the annotations above
        ldapServer = ServerAnnotationProcessor.createLdapServer(
                LDAPWicketTestSupport.class.getAnnotation(CreateLdapServer.class), directoryService);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // disable url parameter encoding for these tests
        SecurityManagerConfig config = getSecurityManager().getSecurityConfig();
        config.setEncryptingUrlParams(false);
        getSecurityManager().saveSecurityConfig(config);
    }

    protected String getServerURL() {
        return ldapServerUrl + ":" + ldapServer.getPort() + "/" + basePath;
    }

    @AfterClass
    public static void shutdownLdapServer() throws Exception {
        if (ldapServer != null) {
            ldapServer.stop();
        }
        if (directoryService != null) {
            directoryService.shutdown();
        }
    }
}
