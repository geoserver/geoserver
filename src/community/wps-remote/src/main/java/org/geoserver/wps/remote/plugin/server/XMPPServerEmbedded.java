/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.vysper.stanzasession.StanzaSessionFactory;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.authorization.Anonymous;
import org.apache.vysper.xmpp.authorization.External;
import org.apache.vysper.xmpp.authorization.Plain;
import org.apache.vysper.xmpp.authorization.SASLMechanism;
import org.apache.vysper.xmpp.cryptography.BogusTrustManagerFactory;
import org.apache.vysper.xmpp.cryptography.FileBasedTLSContextFactory;
import org.apache.vysper.xmpp.delivery.OfflineStanzaReceiver;
import org.apache.vysper.xmpp.delivery.StanzaRelayBroker;
import org.apache.vysper.xmpp.delivery.inbound.DeliveringExternalInboundStanzaRelay;
import org.apache.vysper.xmpp.delivery.inbound.DeliveringInternalInboundStanzaRelay;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCModule;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.modules.extension.xep0133_service_administration.ServiceAdministrationModule;
import org.apache.vysper.xmpp.modules.extension.xep0160_offline_storage.OfflineStorageProvider;
import org.apache.vysper.xmpp.modules.roster.RosterModule;
import org.apache.vysper.xmpp.modules.servicediscovery.ServiceDiscoveryModule;
import org.apache.vysper.xmpp.protocol.HandlerDictionary;
import org.apache.vysper.xmpp.server.DefaultServerRuntimeContext;
import org.apache.vysper.xmpp.server.Endpoint;
import org.apache.vysper.xmpp.server.ServerFeatures;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.state.resourcebinding.ResourceRegistry;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resources;
import org.geoserver.wps.remote.RemoteProcessFactoryConfiguration;
import org.geoserver.wps.remote.RemoteProcessFactoryConfigurationWatcher;
import org.springframework.core.io.Resource;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class XMPPServerEmbedded {

    /** Common logger for test cases */
    static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(XMPPServerEmbedded.class);

    private final List<SASLMechanism> saslMechanisms = new ArrayList<SASLMechanism>();

    private String serverDomain;

    private DefaultServerRuntimeContext serverRuntimeContext;

    private StorageProviderRegistry storageProviderRegistry;

    private RemoteProcessFactoryConfigurationWatcher remoteProcessFactoryConfigurationWatcher;

    private RemoteProcessFactoryConfiguration configuration;

    private final List<Module> listOfModules = new ArrayList<Module>();

    private File certificateFile = null;

    private String certificatePassword = null;

    private Collection<Endpoint> endpoints = new ArrayList<Endpoint>();

    public XMPPServerEmbedded(
            RemoteProcessFactoryConfigurationWatcher remoteProcessFactoryConfigurationWatcher) {
        this.serverDomain =
                remoteProcessFactoryConfigurationWatcher.getConfiguration().get("xmpp_domain");
        this.remoteProcessFactoryConfigurationWatcher = remoteProcessFactoryConfigurationWatcher;
        this.configuration = this.remoteProcessFactoryConfigurationWatcher.getConfiguration();

        // default list of SASL mechanisms
        saslMechanisms.add(new Plain());
        saslMechanisms.add(new External());
        saslMechanisms.add(new Anonymous());
    }

    public void setCertificateFile(Resource certificateFile) throws IOException {
        this.certificateFile = certificateFile.getFile();
    }

    public void setCertificatePassword(String certificatePassword) {
        this.certificatePassword = certificatePassword;
    }

    public void setEndpoints(Collection<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public void setModules(Collection<Module> modules) {
        listOfModules.addAll(modules);
    }

    public void setStorageProviderRegistry(StorageProviderRegistry storageProviderRegistry) {
        this.storageProviderRegistry = storageProviderRegistry;
    }

    public ServerRuntimeContext getServerRuntimeContext() {
        return serverRuntimeContext;
    }

    public void addEndpoint(Endpoint endpoint) {
        endpoints.add(endpoint);
    }

    public void init() throws Exception {
        final String xmppDomain = configuration.get("xmpp_domain");
        final String xmppUserName = configuration.get("xmpp_manager_username");
        final String xmppUserPassword = configuration.get("xmpp_manager_password");

        final String xmppServerEmbedded = configuration.get("xmpp_server_embedded");

        checkSecured(configuration);

        if (xmppServerEmbedded != null && Boolean.valueOf(xmppServerEmbedded.trim())) {
            // /
            LOGGER.info("initializing embedded vysper server...");

            // Admin User
            final Entity adminJID = EntityImpl.parseUnchecked(xmppUserName + "@" + xmppDomain);
            final AccountManagement accountManagement =
                    (AccountManagement) storageProviderRegistry.retrieve(AccountManagement.class);

            if (!accountManagement.verifyAccountExists(adminJID)) {
                accountManagement.addUser(adminJID, xmppUserPassword);
            }

            // Add Endpoints
            if (endpoints == null || endpoints.size() == 0) {
                XMPPBoshEndpoint boshEndpoint = new XMPPBoshEndpoint();
                boshEndpoint.setPort(Integer.parseInt(configuration.get("xmpp_port")));
                if (certificateFile != null && certificatePassword != null) {
                    boshEndpoint.setSSLCertificateKeystore(certificateFile.getAbsolutePath());
                    boshEndpoint.setSSLCertificateKeystorePassword(certificatePassword);
                    boshEndpoint.setSSLEnabled(true);
                } else {
                    boshEndpoint.setSSLEnabled(false);
                }

                addEndpoint(boshEndpoint);
            }

            addEndpoint(new StanzaSessionFactory());

            // Start Server
            List<HandlerDictionary> dictionaries = new ArrayList<HandlerDictionary>();
            addCoreDictionaries(dictionaries);

            ResourceRegistry resourceRegistry = new ResourceRegistry();

            EntityImpl serverEntity = new EntityImpl(null, serverDomain, null);

            OfflineStanzaReceiver offlineReceiver =
                    (OfflineStanzaReceiver)
                            storageProviderRegistry.retrieve(OfflineStorageProvider.class);
            DeliveringInternalInboundStanzaRelay internalStanzaRelay =
                    new DeliveringInternalInboundStanzaRelay(
                            serverEntity, resourceRegistry, accountManagement, offlineReceiver);
            DeliveringExternalInboundStanzaRelay externalStanzaRelay =
                    new DeliveringExternalInboundStanzaRelay();

            StanzaRelayBroker stanzaRelayBroker = new StanzaRelayBroker();
            stanzaRelayBroker.setInternalRelay(internalStanzaRelay);
            stanzaRelayBroker.setExternalRelay(externalStanzaRelay);

            ServerFeatures serverFeatures = new ServerFeatures();
            serverFeatures.setAuthenticationMethods(saslMechanisms);

            serverRuntimeContext =
                    new DefaultServerRuntimeContext(
                            serverEntity,
                            stanzaRelayBroker,
                            serverFeatures,
                            dictionaries,
                            resourceRegistry);
            serverRuntimeContext.setStorageProviderRegistry(storageProviderRegistry);

            serverRuntimeContext.addModule(new ServiceDiscoveryModule());
            serverRuntimeContext.addModule(new RosterModule());

            stanzaRelayBroker.setServerRuntimeContext(serverRuntimeContext);
            internalStanzaRelay.setServerRuntimeContext(serverRuntimeContext);
            externalStanzaRelay.setServerRuntimeContext(serverRuntimeContext);

            // Add SSL Certificates
            if (certificateFile != null && certificatePassword != null) {
                FileBasedTLSContextFactory tlsContextFactory =
                        new FileBasedTLSContextFactory(certificateFile);
                tlsContextFactory.setPassword(certificatePassword);
                tlsContextFactory.setTrustManagerFactory(new BogusTrustManagerFactory());

                serverRuntimeContext.setTlsContextFactory(tlsContextFactory);
            }

            // /
            start();

            // Add Modules
            if (listOfModules != null) {
                for (Module module : listOfModules) {
                    addModule(module);
                }
            }

            final ServiceAdministrationModule serviceAdministrationModule =
                    new ServiceAdministrationModule();
            // unless admin user account with a secure password is added, this will be not become
            // effective
            serviceAdministrationModule.setAddAdminJIDs(Arrays.asList(adminJID));

            addModule(serviceAdministrationModule);

            // add management channels and rooms
            Conference conference = new Conference(configuration.get("xmpp_bus"));

            Entity managementRoomJID =
                    EntityImpl.parseUnchecked(
                            configuration.get("xmpp_management_channel")
                                    + "@"
                                    + configuration.get("xmpp_bus")
                                    + "."
                                    + xmppDomain);
            Room management =
                    conference.createRoom(
                            managementRoomJID,
                            configuration.get("xmpp_management_channel"),
                            RoomType.PasswordProtected);
            management.setPassword(configuration.get("xmpp_management_channel_pwd"));

            final String[] serviceChannels = configuration.get("xmpp_service_channels").split(",");
            if (serviceChannels != null) {
                for (String channel : serviceChannels) {
                    Entity serviceRoomJID =
                            EntityImpl.parseUnchecked(
                                    channel
                                            + "@"
                                            + configuration.get("xmpp_bus")
                                            + "."
                                            + xmppDomain);
                    conference.createRoom(serviceRoomJID, channel, RoomType.Public);
                }
            }

            addModule(new MUCModule(configuration.get("xmpp_bus"), conference));

            // /
            Thread.sleep(3000);
            LOGGER.info("embedded vysper server is running...");
        }
    }

    public void start() throws Exception {
        if (endpoints.size() == 0)
            throw new IllegalStateException("server must have at least one endpoint");
        for (Endpoint endpoint : endpoints) {
            endpoint.setServerRuntimeContext(serverRuntimeContext);
            endpoint.start();
        }
    }

    public void stop() {
        for (Endpoint endpoint : endpoints) {
            endpoint.stop();
        }

        serverRuntimeContext.getServerConnectorRegistry().close();
    }

    public void addModule(Module module) {
        serverRuntimeContext.addModule(module);
    }

    private void addCoreDictionaries(List<HandlerDictionary> dictionaries) {
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.base.BaseStreamStanzaDictionary());
        dictionaries.add(
                new org.apache.vysper.xmpp.modules.core.starttls.StartTLSStanzaDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.sasl.SASLStanzaDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.bind.BindResourceDictionary());
        dictionaries.add(new org.apache.vysper.xmpp.modules.core.session.SessionStanzaDictionary());
        dictionaries.add(
                new org.apache.vysper.xmpp.modules.core.compatibility.jabber_iq_auth
                        .JabberIQAuthDictionary());
    }

    private void checkSecured(RemoteProcessFactoryConfiguration configuration) {
        final String xmppServerEmbeddedSecure = configuration.get("xmpp_server_embedded_secure");
        final String xmppServerEmbeddedCertFile =
                configuration.get("xmpp_server_embedded_certificate_file");
        final String xmppServerEmbeddedCertPwd =
                configuration.get("xmpp_server_embedded_certificate_password");

        if (xmppServerEmbeddedSecure != null && Boolean.valueOf(xmppServerEmbeddedSecure.trim())) {
            // Override XML properties
            if (xmppServerEmbeddedCertFile != null && xmppServerEmbeddedCertPwd != null) {
                final org.geoserver.platform.resource.Resource certFileResource =
                        Resources.fromURL(xmppServerEmbeddedCertFile.trim());
                if (certFileResource != null) {
                    this.certificateFile = certFileResource.file();
                    this.certificatePassword = xmppServerEmbeddedCertPwd.trim();
                } else {
                    // Get the Resource loader
                    GeoServerResourceLoader loader =
                            GeoServerExtensions.bean(GeoServerResourceLoader.class);
                    try {
                        // Copy the default property file into the data directory
                        // URL url =
                        // RemoteProcessFactoryConfigurationWatcher.class.getResource(xmppServerEmbeddedCertFile.trim());
                        // if (url != null) {
                        this.certificateFile = loader.createFile(xmppServerEmbeddedCertFile.trim());
                        loader.copyFromClassPath(
                                xmppServerEmbeddedCertFile.trim(), this.certificateFile /*,
                                    RemoteProcessFactoryConfigurationWatcher.class*/);
                        // }
                        this.certificateFile = loader.find(xmppServerEmbeddedCertFile.trim());
                        this.certificatePassword = xmppServerEmbeddedCertPwd.trim();
                    } catch (IOException e) {
                        if (LOGGER.isLoggable(Level.WARNING)) {
                            LOGGER.log(Level.WARNING, e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }
}
