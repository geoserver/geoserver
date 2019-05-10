/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import java.io.File;
import java.util.UUID;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;

/**
 * Helper class for embedded Apache Directory Server.
 *
 * <p>copied and modified from org.springframework.ldap.test.EmbeddedLdapServer to allow anonymous
 * access (there was no alternative way)
 *
 * @author Mattias Hellborg Arthursson
 * @author Niels Charlier
 */
public class EmbeddedLdapServer {

    private final DirectoryService directoryService;
    private final LdapServer ldapServer;
    private static File workingDirectory;

    private EmbeddedLdapServer(DirectoryService directoryService, LdapServer ldapServer) {
        this.directoryService = directoryService;
        this.ldapServer = ldapServer;
    }

    public static EmbeddedLdapServer newEmbeddedServer(
            String defaultPartitionName,
            String defaultPartitionSuffix,
            int port,
            boolean allowAnonymousAccess)
            throws Exception {
        DirectoryServiceFactory directoryServiceFactory = new DefaultDirectoryServiceFactory();
        directoryServiceFactory.init("geoserver-ldap" + UUID.randomUUID().toString());
        DirectoryService directoryService = directoryServiceFactory.getDirectoryService();
        workingDirectory =
                new File(
                        System.getProperty("java.io.tmpdir")
                                + "/apacheds-test"
                                + UUID.randomUUID().toString());
        directoryService.setShutdownHookEnabled(true);
        directoryService.setAllowAnonymousAccess(allowAnonymousAccess);
        directoryService.getChangeLog().setEnabled(false);

        JdbmPartition partition =
                new JdbmPartition(
                        directoryService.getSchemaManager(), directoryService.getDnFactory());
        partition.setId(defaultPartitionName);
        partition.setSuffixDn(new Dn(defaultPartitionSuffix));
        partition.setPartitionPath(workingDirectory.toURI());
        directoryService.addPartition(partition);

        directoryService.startup();

        // Inject the apache root entry if it does not already exist
        if (!directoryService.getAdminSession().exists(partition.getSuffixDn())) {
            Entry entry = directoryService.newEntry(new Dn(defaultPartitionSuffix));
            entry.add("objectClass", "top", "domain", "extensibleObject");
            entry.add("dc", defaultPartitionName);
            directoryService.getAdminSession().add(entry);
        }

        LdapServer ldapServer = new LdapServer();
        ldapServer.setDirectoryService(directoryService);

        TcpTransport ldapTransport = new TcpTransport(port);
        ldapServer.setTransports(ldapTransport);
        ldapServer.start();

        return new EmbeddedLdapServer(directoryService, ldapServer);
    }

    public void setAllowAnonymousAccess(boolean allowAnonymousAccess) {
        directoryService.setAllowAnonymousAccess(allowAnonymousAccess);
    }

    public void shutdown() throws Exception {
        ldapServer.stop();
        directoryService.shutdown();
    }
}
