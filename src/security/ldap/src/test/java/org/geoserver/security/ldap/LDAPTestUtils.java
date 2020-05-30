/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import javax.naming.Binding;
import javax.naming.ContextNotEmptyException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import org.apache.commons.io.IOUtils;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.protocol.shared.store.LdifFileLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapAttributes;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.DefaultDirObjectFactory;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.ldif.parser.LdifParser;
import org.springframework.ldap.support.LdapUtils;

/**
 * copied and modified from org.springframework.ldap.test.LdapTestUtils to allow anonymous access
 * (there was no alternative way)
 *
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 * @author Mattias Hellborg Arthursson
 * @author Niels Charlier
 */
public class LDAPTestUtils {
    public static final int LDAP_SERVER_PORT = 10391;
    public static final String LDAP_SERVER_URL = "ldap://127.0.0.1:" + LDAP_SERVER_PORT;
    public static final String LDAP_BASE_PATH = "dc=example,dc=com";
    public static final String DEFAULT_PRINCIPAL = "uid=admin,ou=system";
    public static final String DEFAULT_PASSWORD = "secret";

    private static EmbeddedLdapServer embeddedServer;

    /**
     * Start an embedded Apache Directory Server. Only one embedded server will be permitted in the
     * same JVM.
     *
     * @param port the port on which the server will be listening.
     * @param defaultPartitionSuffix The default base suffix that will be used for the LDAP server.
     * @param defaultPartitionName The name to use in the directory server configuration for the
     *     default base suffix.
     * @throws IllegalStateException if an embedded server is already started.
     * @since 1.3.2
     */
    public static void startEmbeddedServer(
            int port,
            String defaultPartitionSuffix,
            String defaultPartitionName,
            boolean allowAnonymousAccess) {
        if (embeddedServer != null) {
            throw new IllegalStateException("An embedded server is already started");
        }

        try {
            embeddedServer =
                    EmbeddedLdapServer.newEmbeddedServer(
                            defaultPartitionName,
                            defaultPartitionSuffix,
                            port,
                            allowAnonymousAccess);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start embedded server", e);
        }
    }

    /**
     * Shuts down the embedded server, if there is one. If no server was previously started in this
     * JVM this is silently ignored.
     *
     * @since 1.3.2
     */
    public static void shutdownEmbeddedServer() throws Exception {
        if (embeddedServer != null) {
            embeddedServer.shutdown();
            embeddedServer = null;
        }
    }

    /**
     * Initializes an in-memory LDAP server to use for testing.
     *
     * @param allowAnonymous anonymous access is allowed or not
     */
    public static boolean initLdapServer(
            boolean allowAnonymous, String ldapServerUrl, String basePath) throws Exception {
        return initLdapServer(allowAnonymous, ldapServerUrl, basePath, "data.ldif");
    }

    /**
     * Initializes an in-memory LDAP server to use for testing.
     *
     * @param allowAnonymous anonymous access is allowed or not
     */
    public static boolean initLdapServer(
            boolean allowAnonymous, String ldapServerUrl, String basePath, String ldifPath)
            throws Exception {
        try {
            if (!portIsBusy("127.0.0.1", LDAP_SERVER_PORT)) {
                startEmbeddedServer(LDAP_SERVER_PORT, basePath, "test", allowAnonymous);

                // Bind to the directory
                LdapContextSource contextSource = new LdapContextSource();
                contextSource.setUrl(ldapServerUrl);
                contextSource.setUserDn(DEFAULT_PRINCIPAL);
                contextSource.setPassword(DEFAULT_PASSWORD);
                contextSource.setPooled(false);
                contextSource.afterPropertiesSet();

                // Create the Sprint LDAP template
                LdapTemplate template = new LdapTemplate(contextSource);

                // Clear out any old data - and load the test data
                cleanAndSetup(
                        template.getContextSource(),
                        new LdapName("dc=example,dc=com"),
                        new ClassPathResource(ldifPath));
                return true;
            }
            return false;
        } catch (Exception ee) {
            ee.printStackTrace();
            return false;
        }
    }

    /** Checks if a network host / port is already occupied. */
    private static boolean portIsBusy(String host, int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return false;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }
        return true;
    }

    /**
     * Clear the directory sub-tree starting with the node represented by the supplied distinguished
     * name.
     *
     * @param contextSource the ContextSource to use for getting a DirContext.
     * @param name the distinguished name of the root node.
     * @throws NamingException if anything goes wrong removing the sub-tree.
     */
    public static void clearSubContexts(ContextSource contextSource, Name name)
            throws NamingException {
        DirContext ctx = null;
        try {
            ctx = contextSource.getReadWriteContext();
            clearSubContexts(ctx, name);
        } finally {
            try {
                ctx.close();
            } catch (Exception e) {
                // Never mind this
            }
        }
    }

    /**
     * Clear the directory sub-tree starting with the node represented by the supplied distinguished
     * name.
     *
     * @param ctx The DirContext to use for cleaning the tree.
     * @param name the distinguished name of the root node.
     * @throws NamingException if anything goes wrong removing the sub-tree.
     */
    public static void clearSubContexts(DirContext ctx, Name name) throws NamingException {

        NamingEnumeration enumeration = null;
        try {
            enumeration = ctx.listBindings(name);
            while (enumeration.hasMore()) {
                Binding element = (Binding) enumeration.next();
                ArrayList<Rdn> list = new ArrayList<>(((LdapName) name).getRdns());
                list.addAll(LdapUtils.newLdapName(element.getName()).getRdns());
                LdapName childName = new LdapName(list);

                try {
                    ctx.destroySubcontext(childName);
                } catch (ContextNotEmptyException e) {
                    clearSubContexts(ctx, childName);
                    ctx.destroySubcontext(childName);
                }
            }
        } catch (NamingException e) {
            e.printStackTrace();
        } finally {
            try {
                enumeration.close();
            } catch (Exception e) {
                // Never mind this
            }
        }
    }

    /**
     * Load an Ldif file into an LDAP server.
     *
     * @param contextSource ContextSource to use for getting a DirContext to interact with the LDAP
     *     server.
     * @param ldifFile a Resource representing a valid LDIF file.
     * @throws IOException if the Resource cannot be read.
     */
    public static void loadLdif(ContextSource contextSource, Resource ldifFile) throws IOException {
        DirContext context = contextSource.getReadWriteContext();
        try {
            loadLdif(context, ldifFile);
        } finally {
            try {
                context.close();
            } catch (Exception e) {
                // This is not the exception we are interested in.
            }
        }
    }

    public static void cleanAndSetup(
            ContextSource contextSource, LdapName rootNode, Resource ldifFile)
            throws NamingException, IOException {

        clearSubContexts(contextSource, rootNode);
        loadLdif(contextSource, ldifFile);
    }

    @SuppressWarnings("deprecation")
    private static void loadLdif(DirContext context, Resource ldifFile) throws IOException {
        try {
            LdapName baseDn =
                    (LdapName)
                            context.getEnvironment()
                                    .get(DefaultDirObjectFactory.JNDI_ENV_BASE_PATH_KEY);

            LdifParser parser = new LdifParser(ldifFile);
            parser.open();
            while (parser.hasMoreRecords()) {
                LdapAttributes record = parser.getRecord();

                org.springframework.ldap.core.DistinguishedName dn = record.getDN();
                if (baseDn != null) {
                    dn.removeFirst(baseDn);
                }
                context.bind(dn, null, record);
            }
        } catch (NamingException e) {
            throw new RuntimeException("Failed to populate LDIF", e);
        }
    }

    public static void loadLdif(DefaultDirectoryService directoryService, Resource ldifFile)
            throws IOException {
        File tempFile = File.createTempFile("spring_ldap_test", ".ldif");
        try {
            InputStream inputStream = ldifFile.getInputStream();
            IOUtils.copy(inputStream, new FileOutputStream(tempFile));
            LdifFileLoader fileLoader =
                    new LdifFileLoader(directoryService.getSession(), tempFile.getAbsolutePath());
            fileLoader.execute();
        } finally {
            try {
                tempFile.delete();
            } catch (Exception e) {
                // Ignore this
            }
        }
    }
}
