/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.directory.server.configuration.MutableServerStartupConfiguration;
import org.apache.directory.server.core.partition.impl.btree.MutableBTreePartitionConfiguration;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerSecurityManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.test.LdapTestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * 
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 * 
 */
public class LDAPAuthenticationProviderTest {
    private LDAPSecurityProvider securityProvider;
    private GeoServerSecurityManager securityManager;
    private LDAPSecurityServiceConfig config;
    private LDAPAuthenticationProvider authProvider;
    private Authentication authentication;
    private Authentication authenticationOther;
    private File tempFolder;

    private static final String ldapServerUrl = "ldap://127.0.0.1:10389";
    private static final String basePath = "dc=example,dc=com";

    /**
     * Initializes an in-memory LDAP server to use for testing.
     * 
     * @param allowAnonymous
     *            anonymous access is allowed or not
     * @throws Exception
     */
    private boolean initLdapServer(boolean allowAnonymous) throws Exception {
        try {
            // Start an LDAP server and import test data
            startApacheDirectoryServer(10389, basePath, "test",
                    LdapTestUtils.DEFAULT_PRINCIPAL,
                    LdapTestUtils.DEFAULT_PASSWORD, allowAnonymous);
    
            // Bind to the directory
            LdapContextSource contextSource = new LdapContextSource();
            contextSource.setUrl(ldapServerUrl);
            contextSource.setUserDn(LdapTestUtils.DEFAULT_PRINCIPAL);
            contextSource.setPassword(LdapTestUtils.DEFAULT_PASSWORD);
            contextSource.setPooled(false);
            contextSource.afterPropertiesSet();
    
            // Create the Sprint LDAP template
            LdapTemplate template = new LdapTemplate(contextSource);
    
            // Clear out any old data - and load the test data
            LdapTestUtils.cleanAndSetup(template.getContextSource(),
                    new DistinguishedName("dc=example,dc=com"),
                    new ClassPathResource("data.ldif"));
            return true;
        } catch(Exception ee) {
            return false;
        }
    }

    /**
     * Starts the in-memory LDAP server
     * 
     * @param port
     *            listening port
     * @param defaultPartitionSuffix
     * @param defaultPartitionName
     * @param principal
     * @param credentials
     * @param anonymousEnabled
     * @return
     * @throws NamingException
     */
    private static DirContext startApacheDirectoryServer(int port,
            String defaultPartitionSuffix, String defaultPartitionName,
            String principal, String credentials, boolean anonymousEnabled)
            throws NamingException {

        MutableServerStartupConfiguration cfg = new MutableServerStartupConfiguration();
        cfg.setAllowAnonymousAccess(anonymousEnabled);
        // Determine an appropriate working directory
        String tempDir = System.getProperty("java.io.tmpdir");
        cfg.setWorkingDirectory(new File(tempDir));

        cfg.setLdapPort(port);

        MutableBTreePartitionConfiguration partitionConfiguration = new MutableBTreePartitionConfiguration();
        partitionConfiguration.setSuffix(defaultPartitionSuffix);
        partitionConfiguration
                .setContextEntry(getRootPartitionAttributes(defaultPartitionName));
        partitionConfiguration.setName(defaultPartitionName);

        cfg.setContextPartitionConfigurations(Collections
                .singleton(partitionConfiguration));
        // Start the Server

        Hashtable<String, String> env = createEnv(principal, credentials);
        env.putAll(cfg.toJndiEnvironment());
        return new InitialDirContext(env);
    }

    private static Attributes getRootPartitionAttributes(
            String defaultPartitionName) {
        BasicAttributes attributes = new BasicAttributes();
        BasicAttribute objectClassAttribute = new BasicAttribute("objectClass");
        objectClassAttribute.add("top");
        objectClassAttribute.add("domain");
        objectClassAttribute.add("extensibleObject");
        attributes.put(objectClassAttribute);
        attributes.put("dc", defaultPartitionName);

        return attributes;
    }

    private static Hashtable<String, String> createEnv(String principal,
            String credentials) {
        Hashtable<String, String> env = new Hashtable<String, String>();

        env.put(Context.PROVIDER_URL, "");
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "org.apache.directory.server.jndi.ServerContextFactory");

        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, credentials);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");

        return env;
    }

    @Before
    public void setUp() throws Exception {

        tempFolder = File.createTempFile("ldap", "test");
        tempFolder.delete();
        tempFolder.mkdirs();
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(
                tempFolder);
        securityManager = new GeoServerSecurityManager(
                new GeoServerDataDirectory(resourceLoader));
        securityProvider = new LDAPSecurityProvider(securityManager);
        config = new LDAPSecurityServiceConfig();
        config.setServerURL(ldapServerUrl + "/" + basePath);
        config.setGroupSearchBase("ou=Groups");
        config.setGroupSearchFilter("member=cn={1}");
        config.setUseTLS(false);

        authentication = new UsernamePasswordAuthenticationToken("admin",
                "admin");
        authenticationOther = new UsernamePasswordAuthenticationToken("other",
                "other");
    }

    @After
    public void tearDown() throws Exception {
        tempFolder.delete();

        LdapTestUtils
                .destroyApacheDirectoryServer(LdapTestUtils.DEFAULT_PRINCIPAL,
                        LdapTestUtils.DEFAULT_PASSWORD);
    }

    /**
     * Test that bindBeforeGroupSearch correctly enables roles fetching on a
     * server without anonymous access enabled.
     * 
     * @throws Exception
     */
    @Test
    public void testBindBeforeGroupSearch() throws Exception {
        // no anonymous access
        if(initLdapServer(false)) {
            config.setUserDnPattern("uid={0},ou=People");
            config.setBindBeforeGroupSearch(true);
            createAuthenticationProvider();
    
            Authentication result = authProvider.authenticate(authentication);
            assertNotNull(result);
            assertEquals("admin", result.getName());
            assertEquals(2, result.getAuthorities().size());
        }

    }

    /**
     * Test that without bindBeforeGroupSearch we get an exception during roles
     * fetching on a server without anonymous access enabled.
     * 
     * @throws Exception
     */
    @Test
    public void testBindBeforeGroupSearchRequiredIfAnonymousDisabled()
            throws Exception {
        // no anonymous access
        if(initLdapServer(false)) {
            config.setUserDnPattern("uid={0},ou=People");
            // we don't bind
            config.setBindBeforeGroupSearch(false);
            createAuthenticationProvider();
            boolean error = false;
            try {
                authProvider.authenticate(authentication);
            } catch (Exception e) {
                error = true;
            }
            assertTrue(error);
        }

    }

    /**
     * Test that authentication can be done using the couple userFilter and
     * userFormat instead of userDnPattern.
     * 
     * @throws Exception
     */
    @Test
    public void testUserFilterAndFormat() throws Exception {
        if(initLdapServer(true)) {    
            // filter to extract user data
            config.setUserFilter("(telephonenumber=1)");
            // username to bind to
            config.setUserFormat("uid={0},ou=People,dc=example,dc=com");
    
            createAuthenticationProvider();
    
            Authentication result = authProvider.authenticate(authentication);
            assertEquals(2, result.getAuthorities().size());
        }
    }

    /**
     * Test that if and adminGroup is defined, the roles contain
     * ROLE_ADMINISTRATOR
     * 
     * @throws Exception
     */
    @Test
    public void testAdminGroup() throws Exception {
        if(initLdapServer(true)) {
            config.setUserDnPattern("uid={0},ou=People");
            config.setAdminGroup("other");
    
            createAuthenticationProvider();
    
            Authentication result = authProvider.authenticate(authenticationOther);
            boolean foundAdmin = false;
            for (GrantedAuthority authority : result.getAuthorities()) {
                if (authority.getAuthority().equalsIgnoreCase("ROLE_ADMINISTRATOR")) {
                    foundAdmin = true;
                }
            }
            assertTrue(foundAdmin);
        }
    }

    /**
     * Test that if and groupAdminGroup is defined, the roles contain
     * ROLE_GROUP_ADMIN
     * 
     * @throws Exception
     */
    @Test
    public void testGroupAdminGroup() throws Exception {
        if(initLdapServer(true)) {
            config.setUserDnPattern("uid={0},ou=People");
            config.setGroupAdminGroup("other");
    
            createAuthenticationProvider();
    
            Authentication result = authProvider.authenticate(authenticationOther);
            boolean foundAdmin = false;
            for (GrantedAuthority authority : result.getAuthorities()) {
                if (authority.getAuthority().equalsIgnoreCase("ROLE_GROUP_ADMIN")) {
                    foundAdmin = true;
                }
            }
            assertTrue(foundAdmin);
        }
    }
    

    private void createAuthenticationProvider() {
        authProvider = (LDAPAuthenticationProvider) securityProvider
                .createAuthenticationProvider(config);

    }
}
