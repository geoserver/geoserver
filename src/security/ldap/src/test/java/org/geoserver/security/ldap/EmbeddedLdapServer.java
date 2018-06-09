/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import java.io.File;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.shared.ldap.name.LdapDN;

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

        DefaultDirectoryService directoryService = new DefaultDirectoryService();
        directoryService.setShutdownHookEnabled(true);
        directoryService.setAllowAnonymousAccess(allowAnonymousAccess);
        directoryService.setWorkingDirectory(
                new File(System.getProperty("java.io.tmpdir") + "/apacheds-test"));
        directoryService.getChangeLog().setEnabled(false);

        JdbmPartition partition = new JdbmPartition();
        partition.setId(defaultPartitionName);
        partition.setSuffix(defaultPartitionSuffix);
        directoryService.addPartition(partition);

        directoryService.startup();

        // Inject the apache root entry if it does not already exist
        if (!directoryService.getAdminSession().exists(partition.getSuffixDn())) {
            ServerEntry entry = directoryService.newEntry(new LdapDN(defaultPartitionSuffix));
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

    public void shutdown() throws Exception {
        ldapServer.stop();
        directoryService.shutdown();
    }
}
