/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.test.LdapTestUtils;

/**
 * 
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 * 
 */
public class LDAPTestUtils {
    private static final int LDAP_SERVER_PORT = 10389;
    public static final String LDAP_SERVER_URL = "ldap://127.0.0.1:10389";
    public static final String LDAP_BASE_PATH = "dc=example,dc=com";

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
    public static DirContext startApacheDirectoryServer(int port,
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
    
    /**
     * Initializes an in-memory LDAP server to use for testing.
     * 
     * @param allowAnonymous
     *            anonymous access is allowed or not
     * @throws Exception
     */
    public static boolean initLdapServer(boolean allowAnonymous, String ldapServerUrl, String basePath) throws Exception {
        return initLdapServer(allowAnonymous, ldapServerUrl, basePath, "data.ldif");
    }
    
    /**
     * Initializes an in-memory LDAP server to use for testing.
     * 
     * @param allowAnonymous
     *            anonymous access is allowed or not
     * @throws Exception
     */
    public static boolean initLdapServer(boolean allowAnonymous, String ldapServerUrl, String basePath, String ldifPath) throws Exception {
        try {
            if (!portIsBusy("127.0.0.1", LDAP_SERVER_PORT)) {
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
                        new ClassPathResource(ldifPath));
                return true;
            }
            return false;
        } catch (Exception ee) {
            return false;
        }
    }

    /**
     * Checks if a network host / port is already occupied.
     * 
     * @param host
     * @param port
     * @return
     */
    private static boolean portIsBusy(String host, int port) {
        Socket socket = new Socket();
        InetSocketAddress endPoint = new InetSocketAddress(host, port);
    
        if (endPoint.isUnresolved()) {
            return false;
        } else {
            try {
                socket.connect(endPoint, 2000);
                return true;
            } catch (IOException ioe) {
                return false;
            } finally {
                try {
                    socket.close();
                } catch (IOException ioe) {
                }
            }
        }
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

}
