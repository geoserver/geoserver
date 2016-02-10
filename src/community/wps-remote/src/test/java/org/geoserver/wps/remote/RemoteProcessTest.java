/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCModule;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0049_privatedata.PrivateDataModule;
import org.apache.vysper.xmpp.modules.extension.xep0054_vcardtemp.VcardTempModule;
import org.apache.vysper.xmpp.modules.extension.xep0092_software_version.SoftwareVersionModule;
import org.apache.vysper.xmpp.modules.extension.xep0119_xmppping.XmppPingModule;
import org.apache.vysper.xmpp.modules.extension.xep0202_entity_time.EntityTimeModule;
import org.apache.vysper.xmpp.server.XMPPServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.remote.plugin.MockRemoteClient;
import org.geoserver.wps.remote.plugin.XMPPClient;
import org.geoserver.wps.remote.plugin.XMPPMessage;
import org.geoserver.wps.remote.plugin.XMPPRegisterMessage;
import org.geotools.factory.FactoryIteratorProvider;
import org.geotools.factory.GeoTools;
import org.geotools.feature.NameImpl;
import org.geotools.process.ProcessFactory;
import org.jivesoftware.smack.packet.Packet;
import org.junit.Test;
import org.opengis.feature.type.Name;

/**
 * This class tests checks if the RemoteProcess class behaves correctly.
 * 
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 */
public class RemoteProcessTest extends WPSTestSupport {

    private static final boolean DISABLE = "true"
            .equalsIgnoreCase(System.getProperty("disableTest", "true"));

    private RemoteProcessFactory factory;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("testRemoteAppContext.xml");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        // add limits properties file
        testData.copyTo(RemoteProcessTest.class.getClassLoader().getResourceAsStream(
                "remote-process/remoteProcess.properties"), "remoteProcess.properties");

        testData.copyTo(RemoteProcessTest.class.getClassLoader()
                .getResourceAsStream("remote-process/bogus_mina_tls.cert"), "bogus_mina_tls.cert");
    }

    @Test
    public void testNames() {
        setupFactory();

        assertNotNull(factory);
        Set<Name> names = factory.getNames();
        assertNotNull(names);
        assertTrue(names.size() == 0);

        final NameImpl name = new NameImpl("default", "Service");
        factory.registerProcess(
                new RemoteServiceDescriptor(name, "Service", "A test service", null, null, null));
        assertTrue(names.size() == 1);
        assertTrue(names.contains(name));

        factory.deregisterProcess(name);
        assertTrue(names.size() == 0);
    }

    @Test
    public void testListeners() {
        setupFactory();

        assertNotNull(factory);
        RemoteProcessClient remoteClient = factory.getRemoteClient();
        assertNotNull(remoteClient);
        assertTrue(remoteClient instanceof MockRemoteClient);

        Set<Name> names = factory.getNames();
        assertNotNull(names);
        assertTrue(names.size() == 0);

        final NameImpl name = new NameImpl("default", "Service");
        try {
            remoteClient.execute(name, null, null, null);
            assertTrue(names.size() == 1);
            assertTrue(names.contains(name));

            factory.deregisterProcess(name);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getLocalizedMessage());
        } finally {
            assertTrue(names.size() == 0);
        }
    }

    @Test
    public void testXMPPClient() {

        if (DISABLE) {
            return;
        }

        setupFactory();

        try {
            // Start Server
            StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();

            final AccountManagement accountManagement = (AccountManagement) providerRegistry
                    .retrieve(AccountManagement.class);

            final RemoteProcessFactoryConfiguration configuration = factory.getRemoteClient()
                    .getConfiguration();
            final String xmppDomain = configuration.get("xmpp_domain");
            final String xmppUserName = configuration.get("xmpp_manager_username");
            final String xmppUserPassword = configuration.get("xmpp_manager_password");

            if (!accountManagement
                    .verifyAccountExists(EntityImpl.parse(xmppUserName + "@" + xmppDomain))) {
                accountManagement.addUser(EntityImpl.parse(xmppUserName + "@" + xmppDomain),
                        xmppUserPassword);
            }

            XMPPServer server = new XMPPServer(xmppDomain);
            TCPEndpoint tcpEndpoint = new TCPEndpoint();
            tcpEndpoint.setPort(Integer.parseInt(configuration.get("xmpp_port")));
            server.addEndpoint(tcpEndpoint);
            server.setStorageProviderRegistry(providerRegistry);

            // setup CA
            final File certfile = new File(testData.getDataDirectoryRoot(), "bogus_mina_tls.cert");
            char[] password = "boguspw".toCharArray();

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream instream = new FileInputStream(certfile);
            try {
                trustStore.load(instream, password);
            } finally {
                instream.close();
            }

            // Trust own CA and all self-signed certs
            SSLContext sslcontext = SSLContexts.custom()
                    .loadTrustMaterial(trustStore, new TrustSelfSignedStrategy()).build();

            server.setTLSCertificateInfo(certfile, "boguspw");

            server.start();

            // other initialization
            server.addModule(new SoftwareVersionModule());
            server.addModule(new EntityTimeModule());
            server.addModule(new VcardTempModule());
            server.addModule(new XmppPingModule());
            server.addModule(new PrivateDataModule());

            Conference conference = new Conference(configuration.get("xmpp_bus"));
            server.addModule(new MUCModule(configuration.get("xmpp_bus"), conference));

            /**
             * Entity managementRoomJID = EntityImpl.parseUnchecked(configuration.get("xmpp_management_channel") + "@" + xmppDomain);
             * 
             * Room management = conference.findOrCreateRoom(managementRoomJID, configuration.get("xmpp_management_channel"));
             * management.setPassword(configuration.get("xmpp_management_channel_pwd"));
             * 
             * String[] serviceChannels = configuration.get("xmpp_service_channels").split(","); if (serviceChannels != null) { for (String channel :
             * serviceChannels) { Entity serviceRoomJID = EntityImpl.parseUnchecked(channel + "@" + xmppDomain);
             * conference.findOrCreateRoom(serviceRoomJID, channel); } }
             **/

            // /
            XMPPClient xmppRemoteClient = (XMPPClient) applicationContext
                    .getBean("xmppRemoteProcessClient");
            assertNotNull(xmppRemoteClient);

            xmppRemoteClient.init(sslcontext);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testRegisterMessage() {
        // /
        XMPPClient xmppRemoteClient = (XMPPClient) applicationContext
                .getBean("xmppRemoteProcessClient");
        assertNotNull(xmppRemoteClient);

        XMPPMessage msg = new XMPPRegisterMessage();

        // build register body
        Map<String, String> signalArgs = new HashMap<String, String>();
        signalArgs.put("topic", "register");
        signalArgs.put("service", "test.Service");
        /**
         * JSON URL Encoded Body
         * 
         * { "title": "test.Service", "description": "This is a test Service!", "input": [ ["simpleType",
         * "{\"type\": \"string\", \"description\": \"A simple string parameter\", \"max\": 1}"], ["complexType",
         * "{\"type\": \"complex\", \"description\": \"A complex parameter\", \"min\": 1, \"max\": 10}"] ] }
         */
        signalArgs.put("message",
                "%7B%0A%20%20%22title%22%3A%20%22test.Service%22%2C%0A%20%20%22description%22%3A%20%22This%20is%20a%20test%20Service!%22%2C%0A%20%20%22input%22%3A%20%5B%0A%20%20%20%20%5B%22simpleType%22%2C%20%22%7B%5C%22type%5C%22%3A%20%5C%22string%5C%22%2C%20%5C%22description%5C%22%3A%20%5C%22A%20simple%20string%20parameter%5C%22%2C%20%5C%22max%5C%22%3A%201%7D%22%5D%2C%0A%20%20%20%20%5B%22complexType%22%2C%20%22%7B%5C%22type%5C%22%3A%20%5C%22complex%5C%22%2C%20%5C%22description%5C%22%3A%20%5C%22A%20complex%20parameter%5C%22%2C%20%5C%22min%5C%22%3A%201%2C%20%5C%22max%5C%22%3A%2010%7D%22%5D%0A%20%20%5D%0A%7D");

        // handle signal
        Packet packet = new Packet() {

            @Override
            public String getFrom() {
                return "test@geoserver.org";
            }

            @Override
            public CharSequence toXML() {
                return null;
            }
        };
        msg.handleSignal(xmppRemoteClient, packet, null, signalArgs);
    }

    /**
     * 
     */
    protected void setupFactory() {
        if (factory == null) {
            factory = new RemoteProcessFactory();

            // check SPI will see the factory if we register it using an iterator
            // provider
            GeoTools.addFactoryIteratorProvider(new FactoryIteratorProvider() {

                public <T> Iterator<T> iterator(Class<T> category) {
                    if (ProcessFactory.class.isAssignableFrom(category)) {
                        return (Iterator<T>) Collections.singletonList(factory).iterator();
                    } else {
                        return null;
                    }
                }
            });
        }
    }

    /**
     * 
     * @param fname
     * @return
     * @throws IOException
     */
    private static InputStream fullStream(File fname) throws IOException {
        FileInputStream fis = new FileInputStream(fname);
        DataInputStream dis = new DataInputStream(fis);
        byte[] bytes = new byte[dis.available()];
        dis.readFully(bytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return bais;
    }

}
