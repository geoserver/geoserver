/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import net.razorvine.pickle.Opcodes;
import net.razorvine.pickle.PickleException;
import net.razorvine.pickle.PickleUtils;
import net.razorvine.pickle.Pickler;
import net.razorvine.pickle.Unpickler;

import org.apache.commons.io.IOUtils;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wps.process.RawData;
import org.geoserver.wps.process.ResourceRawData;
import org.geoserver.wps.process.StreamRawData;
import org.geoserver.wps.process.StringRawData;
import org.geoserver.wps.remote.RemoteProcessClient;
import org.geoserver.wps.remote.RemoteProcessFactoryConfigurationWatcher;
import org.geoserver.wps.remote.RemoteProcessFactoryListener;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;

/**
 * XMPP implementation of the {@link RemoteProcessClient}
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public class XMPPClient extends RemoteProcessClient {

    /** The LOGGER */
    public static final Logger LOGGER = Logging.getLogger(XMPPClient.class.getPackage().getName());

    private static final int DEFAULT_PACKET_REPLY_TIMEOUT = 500; // millis

    /** The XMPP Server endpoint */
    private String server;

    /** The XMPP Server port */
    private int port;

    /**
     * XMPP specific parameters and properties
     */
    private XMPPConnection connection;

    private ConnectionConfiguration config;

    private ChatManager chatManager;

    private PacketListener packetListener;

    private ServiceDiscoveryManager discoStu;

    private Map<String, Chat> openChat = Collections.synchronizedMap(new HashMap<String, Chat>());

    private String domain;

    private String bus;

    private String managementChannelUser;

    private String managementChannelPassword;

    protected String managementChannel;

    /**
     * Private structures
     */
    protected List<String> serviceChannels;

    /*
     * protected Map<String, List<String>> occupantsList = Collections .synchronizedMap(new HashMap<String, List<String>>());
     */

    protected List<Name> registeredServices = Collections.synchronizedList(new ArrayList<Name>());

    protected List<MultiUserChat> mucServiceChannels = new ArrayList<MultiUserChat>();

    protected MultiUserChat mucManagementChannel;

    /**
     * Default Constructor
     * 
     * @param remoteProcessFactoryConfigurationWatcher
     * @param enabled
     */
    public XMPPClient(
            RemoteProcessFactoryConfigurationWatcher remoteProcessFactoryConfigurationWatcher,
            boolean enabled, int priority) {
        super(remoteProcessFactoryConfigurationWatcher, enabled, priority);
        this.server = getConfiguration().get("xmpp_server");
        this.port = Integer.parseInt(getConfiguration().get("xmpp_port"));
        this.domain = getConfiguration().get("xmpp_domain");
        this.bus = getConfiguration().get("xmpp_bus");
        this.managementChannelUser = getConfiguration().get("xmpp_management_channel_user");
        this.managementChannelPassword = getConfiguration().get("xmpp_management_channel_pwd");
        this.managementChannel = getConfiguration().get("xmpp_management_channel");

        this.serviceChannels = new ArrayList<String>();

        String[] serviceNamespaces = getConfiguration().get("xmpp_service_channels").split(",");
        for (int sc = 0; sc < serviceNamespaces.length; sc++) {
            this.serviceChannels.add(serviceNamespaces[sc].trim());
        }
    }

    @Override
    public void init(SSLContext customSSLContext) throws Exception {

        // Initializes the XMPP Client and starts the communication. It also register GeoServer as "manager" to the service channels on the MUC (Multi
        // User Channel) Rooms
        LOGGER.info(
                String.format("Initializing connection to server %1$s port %2$d", server, port));

        int packetReplyTimeout = DEFAULT_PACKET_REPLY_TIMEOUT;
        if (getConfiguration().get("xmpp_packet_reply_timeout") != null) {
            packetReplyTimeout = Integer
                    .parseInt(getConfiguration().get("xmpp_packet_reply_timeout"));
        }
        SmackConfiguration.setDefaultPacketReplyTimeout(packetReplyTimeout);

        config = new ConnectionConfiguration(server, port);
        if (customSSLContext != null) {
            // config.setSASLAuthenticationEnabled(false);
            config.setSecurityMode(SecurityMode.enabled);
            config.setCustomSSLContext(customSSLContext);
        } else {
            config.setSecurityMode(SecurityMode.disabled);
        }

        connection = new XMPPTCPConnection(config);
        connection.connect();

        LOGGER.info("Connected: " + connection.isConnected());

        // check if the connection to the XMPP server is successful; the login and registration is not yet performed at this time
        if (connection.isConnected()) {
            chatManager = ChatManager.getInstanceFor(connection);
            discoStu = ServiceDiscoveryManager.getInstanceFor(connection);

            //
            discoProperties();

            //
            performLogin(getConfiguration().get("xmpp_manager_username"),
                    getConfiguration().get("xmpp_manager_password"));

            //
            startPingTask();

            //
            sendInvitations();
        } else {
            setEnabled(false);
        }
    }

    @Override
    public String execute(Name name, Map<String, Object> input, Map<String, Object> metadata,
            ProgressListener monitor) throws Exception {

        // Check for a free machine...
        final String serviceJID = getFlattestMachine(name, (String) metadata.get("serviceJID"));

        LOGGER.info("XMPPClient::execute - trying to send a request message to the service JID ["
                + serviceJID + "]");

        if (metadata != null && serviceJID != null) {
            // Extract the PID
            metadata.put("serviceJID", serviceJID);

            final Object fixedInputs = getFixedInputs(input);

            LOGGER.info("XMPPClient::execute - extracting the PID for the service JID ["
                    + serviceJID + "] with inputs [" + fixedInputs + "]");

            final String pid = md5Java(
                    serviceJID + System.nanoTime() + byteArrayToURLString(pickle(fixedInputs)));

            Request request = Dispatcher.REQUEST.get();
            metadata.put("request", request);
            String baseURL = getGeoServer().getGlobal().getSettings().getProxyBaseUrl();

            try {
                if (baseURL == null) {
                    baseURL = RequestUtils.baseURL(request.getHttpRequest());
                }

                baseURL = ResponseUtils.buildURL(baseURL, "/", null, URLType.SERVICE);
            } catch (Exception e) {
                LOGGER.warning("Could not acquire the GeoServer Base URL!");
            }
            String msg = "topic=request&id=" + pid + "&baseURL=" + baseURL + "&message="
                    + byteArrayToURLString(pickle(fixedInputs));
            sendMessage(serviceJID, msg);

            return pid;
        }

        throw new Exception("Could not send a Request Message to the Remote XMPP Client!");
    }

    /**
     * 
     * @param input
     * @return
     * @throws IOException
     */
    private Object getFixedInputs(Map<String, Object> input) throws IOException {
        Map<String, Object> fixedInputs = new HashMap<String, Object>();

        for (Entry<String, Object> entry : input.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();

            Object fixedValue = value;
            if (value instanceof RawData) {
                fixedValue = IOUtils.toString(((RawData) value).getInputStream(), "UTF-8");
            } else if (value instanceof List) {
                List<Object> values = (List<Object>) value;

                if (values != null && values.size() > 0 && values.get(0) instanceof RawData) {
                    fixedValue = new ArrayList<String>();

                    for (Object o : values) {
                        ((List<String>) fixedValue)
                                .add(IOUtils.toString(((RawData) o).getInputStream(), "UTF-8"));
                    }
                }
            }

            fixedInputs.put(key, fixedValue);
        }

        return fixedInputs;
    }

    /*
     * Add features to our XMPP client We do support Data forms, XHTML-IM, Service Discovery
     */
    private void discoProperties() {
        discoStu.addFeature("http://jabber.org/protocol/xhtml-im");
        discoStu.addFeature("jabber:x:data");
        discoStu.addFeature("http://jabber.org/protocol/disco#info");
        discoStu.addFeature("jabber:iq:privacy");
        discoStu.addFeature("http://jabber.org/protocol/si");
        discoStu.addFeature("http://jabber.org/protocol/bytestreams");
        discoStu.addFeature("http://jabber.org/protocol/ibb");
    }

    /**
     * Logins as manager to the XMPP Server and registers to the service channels management chat rooms
     * 
     * @param username
     * @param password
     * @throws Exception
     */
    public void performLogin(String username, String password) throws Exception {
        if (connection != null && connection.isConnected()) {
            connection.login(username, password, getResource(username));

            // Create a MultiUserChat using a XMPPConnection for a room

            // User joins the new room using a password and specifying
            // the amount of history to receive. In this example we are requesting the last 5 messages.
            DiscussionHistory history = new DiscussionHistory();
            history.setMaxStanzas(5);

            mucManagementChannel = new MultiUserChat(connection,
                    managementChannel + "@" + bus + "." + domain);
            mucManagementChannel.join(getJID(username), managementChannelPassword); /*
                                                                                     * , history, connection.getPacketReplyTimeout());
                                                                                     */

            for (String channel : serviceChannels) {
                MultiUserChat serviceChannel = new MultiUserChat(connection,
                        channel + "@" + bus + "." + domain);
                serviceChannel.join(getJID(username), managementChannelPassword); /*
                                                                                   * , history, connection.getPacketReplyTimeout());
                                                                                   */
                mucServiceChannels.add(serviceChannel);
            }

            //
            setStatus(true, "Orchestrator Active");

            //
            setupListeners();
        }
    }

    /**
     * Generate the XMPP JID
     * 
     * @param username
     * @return
     */
    private String getJID(String username) {
        final String id = md5Java(username + "@" + this.domain + "/" + System.nanoTime());
        return username + "@" + this.domain;
    }

    /**
     * Generate a unique Server JID Resource
     * 
     * @param username
     * @return
     */
    private String getResource(String username) {
        final String id = md5Java(username + "@" + this.domain + "/" + System.nanoTime());
        try {
            return /* this.domain + "/" + */id + "@" + InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return /* this.domain + "/" + */id + "@geoserver";
        }
    }

    /**
     * Declare the status on the XMPP Chat
     * 
     * @param available
     * @param status
     * @throws Exception
     */
    public void setStatus(boolean available, String status) throws Exception {
        Presence.Type type = available ? Type.available : Type.unavailable;
        Presence presence = new Presence(type);

        presence.setStatus(status);
        connection.sendPacket(presence);
    }

    /**
     * Destroy the connection
     */
    public void destroy() throws Exception {
        if (connection != null && connection.isConnected()) {
            stopPingTask();
            connection.disconnect();
        }
    }

    /**
     * 
     * 
     * @param user
     * @param name
     * @throws Exception
     */
    public void createEntry(String user, String name) throws Exception {
        LOGGER.fine(String.format("Creating entry for buddy '%1$s' with name %2$s", user, name));
        Roster roster = connection.getRoster();
        roster.createEntry(user, name, null);
    }

    /**
     * This handles the chat listener. We can't simply listen to chats for some reason, and instead have to grab the chats from the packets. The other
     * listeners work properly in SMACK
     */
    public void setupListeners() {
        /*
         * This is the actual code that handles what happens with XMPP users
         */
        packetListener = new XMPPPacketListener(this);

        // PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
        connection.addPacketListener(packetListener, null);
    }

    /**
     * Conversation setup!
     * 
     * Messages should be moved here once we get this working properly
     */
    public Chat setupChat(final String origin) {
        synchronized (openChat) {
            if (openChat.get(origin) != null) {
                return openChat.get(origin);
            }

            MessageListener listener = new MessageListener() {
                public void processMessage(Chat chat, Message message) {
                    // TODO: Fix this so that this actually does something!
                }
            };

            Chat chat = chatManager.createChat(origin, listener);
            openChat.put(origin, chat);
            return chat;
        }
    }

    /**
     * This is the code that handles HTML messages
     */
    public void sendMessage(String person, String message) {
        synchronized (openChat) {
            Chat chat = openChat.get(person);
            if (chat == null)
                chat = setupChat(person);
            try {
                chat.sendMessage(message);
            } catch (XMPPException e) {
                LOGGER.log(Level.SEVERE, "xmppClient._ReceiveError", e);
            } catch (NotConnectedException e) {
                LOGGER.log(Level.SEVERE, "xmppClient._ReceiveError", e);
            }
        }
    }

    /**
     * Close the XMPP connection
     * 
     * @throws NotConnectedException
     */
    public void disconnect() throws NotConnectedException {
        connection.disconnect();
    }

    /*
     * @param person
     * 
     * @return
     */
    static NameImpl extractServiceName(String person) throws Exception {
        String occupantFlatName = null;
        if (person.lastIndexOf("@") < person.indexOf("/")) {
            occupantFlatName = person.substring(person.indexOf("/") + 1);
        } else {
            occupantFlatName = person.substring(person.indexOf("/") + 1);
            occupantFlatName = occupantFlatName.substring(0, occupantFlatName.indexOf("@"));
        }

        if (occupantFlatName.indexOf(".") > 0) {
            final String serviceName[] = occupantFlatName.split("\\.");
            return new NameImpl(serviceName[0], serviceName[1]);
        } else {
            final String channel = person.substring(0, person.indexOf("@"));
            return new NameImpl(channel, occupantFlatName);
        }
    }

    /*
     * Send an invitation to the new logged in member
     * 
     * @throws Exception
     */
    protected void sendInvitations() throws Exception {
        synchronized (registeredServices) {
            for (MultiUserChat mucServiceChannel : mucServiceChannels) {
                for (String occupant : mucServiceChannel.getOccupants()) {
                    final Name serviceName = extractServiceName(occupant);

                    // send invitation and register source JID
                    String[] serviceJIDParts = occupant.split("/");
                    if (serviceJIDParts.length == 3 && (serviceJIDParts[2].startsWith("master")
                            || serviceJIDParts[2].indexOf("@") < 0)) {
                        sendMessage(occupant, "topic=invite");
                    }
                    // register service on listeners
                    if (!registeredServices.contains(serviceName)) {
                        registeredServices.add(serviceName);
                    }
                }
            }
        }
    }

    /*
     * A new member joined one of the service chat-rooms; send an invitation and see if it is a remote service. If so, register it
     * 
     * @param p
     * 
     * @throws Exception
     */
    protected void handleMemberJoin(Presence p) throws Exception {
        synchronized (registeredServices) {
            LOGGER.finer("Member " + p.getFrom() + " joined the chat.");
            final Name serviceName = extractServiceName(p.getFrom());

            // send invitation and register source JID
            String[] serviceJIDParts = p.getFrom().split("/");

            if (serviceJIDParts.length == 3 && (serviceJIDParts[2].startsWith("master")
                    || serviceJIDParts[2].indexOf("@") < 0)) {
                sendMessage(p.getFrom(), "topic=invite");
            }

            if (!registeredServices.contains(serviceName)) {
                registeredServices.add(serviceName);
            }
        }
    }

    /*
     * A member leaved one of the service chat-rooms; lets remove the service declaration and de-register it
     * 
     * @param p
     * 
     * @throws Exception
     */
    protected void handleMemberLeave(Packet p) throws Exception {
        final Name serviceName = extractServiceName(p.getFrom());

        LOGGER.finer("Member " + p.getFrom() + " leaved the chat.");
        if (registeredServices.contains(serviceName)) {
            registeredServices.remove(serviceName);
        }

        for (RemoteProcessFactoryListener listener : getRemoteFactoryListeners()) {
            listener.deregisterProcess(serviceName);
        }
    }

    /*
     * Find the service by name with the smallest amount of processes running, channel is decoded in service name
     * 
     * e.g. debug.foo@bar/service@localhost
     * 
     * @param service name
     * 
     * @param candidateServiceJID
     * 
     * @return
     */
    private String getFlattestMachine(Name name, String candidateServiceJID) {
        final String serviceName = name.getLocalPart();

        LOGGER.info("XMPPClient::getFlattestMachine - scanning the connected remote services...");

        Map<String, List<String>> availableServices = new HashMap<String, List<String>>();
        Map<String, List<String>> availableServiceJIDs = new HashMap<String, List<String>>();

        for (MultiUserChat muc : this.mucServiceChannels) {

            for (String occupant : muc.getOccupants()) {

                LOGGER.info("XMPPClient::getFlattestMachine - looking for service [" + serviceName
                        + "] @occupant [" + occupant + "]");

                if (occupant.toLowerCase().contains(serviceName.toLowerCase())) {

                    // extracting the machine name
                    String[] serviceJIDParts = occupant.split("/");
                    if (serviceJIDParts.length > 1) {
                        String[] localizedServiceJID = serviceJIDParts[1].split("@");

                        LOGGER.info(
                                "XMPPClient::getFlattestMachine - [localizedServiceJID.length] -> "
                                        + localizedServiceJID.length);
                        LOGGER.info(
                                "XMPPClient::getFlattestMachine - [localizedServiceJID[0].contains(serviceName)] -> "
                                        + localizedServiceJID[0].contains(serviceName));
                        LOGGER.info("XMPPClient::getFlattestMachine - [localizedServiceJID] -> "
                                + localizedServiceJID[0] + " @ " + localizedServiceJID[1]);

                        if (localizedServiceJID.length == 2 && localizedServiceJID[0].toLowerCase()
                                .contains(serviceName.toLowerCase())) {
                            // final String machine = localizedServiceJID[1];
                            final String machine = occupant
                                    .substring(occupant.lastIndexOf("@") + 1);

                            if (availableServices.get(machine) == null) {
                                availableServices.put(machine, new ArrayList<String>());
                            }
                            if (availableServiceJIDs.get(machine) == null) {
                                availableServiceJIDs.put(machine, new ArrayList<String>());
                            }

                            availableServices.get(machine).add(occupant);
                            if (serviceJIDParts.length == 3
                                    && (serviceJIDParts[2].startsWith("master")
                                            || serviceJIDParts[2].indexOf("@") < 0)) {
                                availableServiceJIDs.get(machine).add(occupant);
                            }
                        }
                    }
                }
            }
        }

        if (availableServices == null || availableServices.isEmpty()) {

            LOGGER.info(
                    "XMPPClient::getFlattestMachine - no suitable target JID found, using the default candidate ["
                            + candidateServiceJID + "]");

            return candidateServiceJID;
        }

        String targetMachine = null;
        String targetServiceJID = null;
        int targetMachineCounter = Integer.MAX_VALUE;
        for (String machine : availableServices.keySet()) {
            if (targetMachine == null
                    || targetMachineCounter > availableServices.get(machine).size()) {
                targetMachine = machine;
                targetServiceJID = availableServiceJIDs.get(machine).get(0);
                targetMachineCounter = availableServices.get(machine).size();
            }
        }

        LOGGER.info("XMPPClient::getFlattestMachine - target JID found, using the target ["
                + targetServiceJID + "]");

        return targetServiceJID;
    }

    /**
     * Keep connection alive and check for network changes by sending ping packets
     */

    Thread pingThread;

    private static int ping_task_generation = 1;

    void startPingTask() {
        // Schedule a ping task to run.
        PingTask task = new PingTask();
        pingThread = new Thread(task);
        task.setThread(pingThread);
        pingThread.setDaemon(true);
        pingThread.setName("XmppConnection Pinger " + ping_task_generation);
        ping_task_generation++;
        pingThread.start();
    }

    void stopPingTask() {
        pingThread = null;
    }

    class PingTask implements Runnable {

        private static final long DEFAULT_INITIAL_PING_DELAY = 20000;

        private static final long DEFAULT_PING_INTERVAL = 30000;

        private static final long DEFAULT_PING_TIMEOUT = 10000;

        private long delay;

        private long timeout;

        private long start_delay;

        private Thread thread;

        /**
         * 
         */
        public PingTask() {
            this.delay = DEFAULT_PING_INTERVAL;
            if (getConfiguration().get("xmpp_connection_ping_interval") != null) {
                this.delay = Long
                        .parseLong(getConfiguration().get("xmpp_connection_ping_interval"));
            }

            this.timeout = DEFAULT_PING_TIMEOUT;
            if (getConfiguration().get("xmpp_connection_ping_timeout") != null) {
                this.timeout = Long
                        .parseLong(getConfiguration().get("xmpp_connection_ping_timeout"));
            }

            this.start_delay = DEFAULT_INITIAL_PING_DELAY;
            if (getConfiguration().get("xmpp_connection_ping_initial_delay") != null) {
                this.start_delay = Long
                        .parseLong(getConfiguration().get("xmpp_connection_ping_initial_delay"));
            }
        }

        /**
         * 
         * @param thread
         */
        protected void setThread(Thread thread) {
            this.thread = thread;
        }

        /**
         * 
         * @return
         * @throws NotConnectedException
         */
        private boolean sendPing() throws NotConnectedException {
            IQ req = new IQ() {
                public String getChildElementXML() {
                    return "<ping xmlns='urn:xmpp:ping'/>";
                }
            };
            req.setType(IQ.Type.GET);
            PacketFilter filter = new AndFilter(new PacketIDFilter(req.getPacketID()),
                    new PacketTypeFilter(IQ.class));
            PacketCollector collector = connection.createPacketCollector(filter);
            connection.sendPacket(req);
            IQ result = (IQ) collector.nextResult(timeout);
            if (result == null) {
                LOGGER.warning("ping timeout");
                return false;
            }
            collector.cancel();
            return true;
        }

        /**
         * 
         */
        public void run() {
            try {
                // Sleep before sending first heartbeat. This will give time to
                // properly finish logging in.
                Thread.sleep(start_delay);
            } catch (InterruptedException ie) {
                // Do nothing
            }
            while (connection != null && pingThread == thread) {
                if (connection.isConnected() && connection.isAuthenticated()) {
                    LOGGER.log(Level.FINER, "ping");
                    try {
                        if (!sendPing()) {
                            LOGGER.severe("ping failed - close connection");
                            try {
                                connection.disconnect();
                            } catch (NotConnectedException e) {
                                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                            }
                        }
                    } catch (NotConnectedException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                } else {
                    // Try to reconnect...
                    LOGGER.log(Level.FINER, "Try to reconnect...");
                    try {
                        connection.connect();

                        LOGGER.info("Connected: " + connection.isConnected());

                        // check if the connection to the XMPP server is successful; the login and registration is not yet performed at this time
                        if (connection.isConnected()) {
                            chatManager = ChatManager.getInstanceFor(connection);
                            discoStu = ServiceDiscoveryManager.getInstanceFor(connection);

                            //
                            discoProperties();

                            //
                            performLogin(getConfiguration().get("xmpp_manager_username"),
                                    getConfiguration().get("xmpp_manager_password"));

                            //
                            startPingTask();

                            //
                            sendInvitations();
                        } else {
                            setEnabled(false);
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "XMPP Could not reconnect!", e);
                    }
                }
                try {
                    // Sleep until we should write the next keep-alive.
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    // Do nothing
                }
            }
            LOGGER.log(Level.FINER, "pinger exit");
        }
    }

    /**
     * 
     * @param strdata
     * @return
     * @throws PickleException
     * @throws IOException
     */
    static Object unPickle(String strdata) throws PickleException, IOException {
        return unPickle(PickleUtils.str2bytes(strdata));
    }

    /**
     * 
     * @param data
     * @return
     * @throws PickleException
     * @throws IOException
     */
    static Object unPickle(byte[] data) throws PickleException, IOException {
        Unpickler u = new Unpickler();
        Object o = u.loads(data);
        u.close();
        return o;
    }

    /**
     * 
     * @param s
     * @return
     * @throws IOException
     */
    static byte[] toBytes(String s) throws IOException {
        try {
            byte[] bytes = PickleUtils.str2bytes(s);
            byte[] result = new byte[bytes.length + 3];
            result[0] = (byte) Opcodes.PROTO;
            result[1] = 2;
            result[result.length - 1] = (byte) Opcodes.STOP;
            System.arraycopy(bytes, 0, result, 2, bytes.length);
            return result;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 
     * @param shorts
     * @return
     */
    static byte[] toBytes(short[] shorts) {
        byte[] result = new byte[shorts.length + 3];
        result[0] = (byte) Opcodes.PROTO;
        result[1] = 2;
        result[result.length - 1] = (byte) Opcodes.STOP;
        for (int i = 0; i < shorts.length; ++i) {
            result[i + 2] = (byte) shorts[i];
        }
        return result;
    }

    static byte[] pickle(Object unpickled) throws PickleException, IOException {
        Pickler p = new Pickler();
        return p.dumps(unpickled);
    }

    /**
     * 
     * @param message
     * @return
     */
    public static String md5Java(String message) {
        String digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(message.getBytes("UTF-8"));
            // converting byte array to Hexadecimal String
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            digest = sb.substring(0, 15).toString();
        } catch (UnsupportedEncodingException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return digest;
    }

    /**
     * Convert a byte array to a URL encoded string
     * 
     * @param in byte[]
     * @return String
     */
    public static String byteArrayToURLString(byte in[]) {
        byte ch = 0x00;
        int i = 0;
        if (in == null || in.length <= 0)
            return null;

        String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D",
                "E", "F" };
        StringBuffer out = new StringBuffer(in.length * 2);

        while (i < in.length) {
            // First check to see if we need ASCII or HEX
            if ((in[i] >= '0' && in[i] <= '9') || (in[i] >= 'a' && in[i] <= 'z')
                    || (in[i] >= 'A' && in[i] <= 'Z') || in[i] == '$' || in[i] == '-'
                    || in[i] == '_' || in[i] == '.' || in[i] == '!') {
                out.append((char) in[i]);
                i++;
            } else {
                out.append('%');
                ch = (byte) (in[i] & 0xF0); // Strip off high nibble
                ch = (byte) (ch >>> 4); // shift the bits down
                ch = (byte) (ch & 0x0F); // must do this is high order bit is
                // on!
                out.append(pseudo[(int) ch]); // convert the nibble to a
                // String Character
                ch = (byte) (in[i] & 0x0F); // Strip off low nibble
                out.append(pseudo[(int) ch]); // convert the nibble to a
                // String Character
                i++;
            }
        }

        String rslt = new String(out);

        return rslt;
    }

    /** Primitive type name -> class map. */
    public static final Map<String, Object> PRIMITIVE_NAME_TYPE_MAP = new HashMap<String, Object>();

    /** Setup the primitives map. */
    static enum CType {
        SIMPLE, COMPLEX
    }

    static {
        // ----
        PRIMITIVE_NAME_TYPE_MAP.put("string",
                new Object[] { String.class, CType.SIMPLE, null, "text/plain" });
        PRIMITIVE_NAME_TYPE_MAP.put("url",
                new Object[] { String.class, CType.SIMPLE, null, "text/plain" });
        PRIMITIVE_NAME_TYPE_MAP.put("boolean",
                new Object[] { Boolean.TYPE, CType.SIMPLE, Boolean.TRUE, "" });
        PRIMITIVE_NAME_TYPE_MAP.put("byte", new Object[] { Byte.TYPE, CType.SIMPLE, null, "" });
        PRIMITIVE_NAME_TYPE_MAP.put("char",
                new Object[] { Character.TYPE, CType.SIMPLE, null, "text/plain" });
        PRIMITIVE_NAME_TYPE_MAP.put("short", new Object[] { Short.TYPE, CType.SIMPLE, null, "" });
        PRIMITIVE_NAME_TYPE_MAP.put("int", new Object[] { Integer.TYPE, CType.SIMPLE, null, "" });
        PRIMITIVE_NAME_TYPE_MAP.put("long", new Object[] { Long.TYPE, CType.SIMPLE, null, "" });
        PRIMITIVE_NAME_TYPE_MAP.put("float", new Object[] { Float.TYPE, CType.SIMPLE, null, "" });
        PRIMITIVE_NAME_TYPE_MAP.put("double", new Object[] { Double.TYPE, CType.SIMPLE, null, "" });
        PRIMITIVE_NAME_TYPE_MAP.put("datetime",
                new Object[] { Date.class, CType.SIMPLE, null, "" });

        // Complex and Raw data types
        // ----
        PRIMITIVE_NAME_TYPE_MAP.put("application/xml",
                new Object[] { RawData.class, CType.COMPLEX,
                        new StringRawData("", "application/xml"), "application/xml,text/xml",
                        ".xml" });
        PRIMITIVE_NAME_TYPE_MAP.put("text/xml", new Object[] { RawData.class, CType.COMPLEX,
                new StringRawData("", "text/xml"), "application/xml,text/xml", ".xml" });

        // ----
        PRIMITIVE_NAME_TYPE_MAP.put("text/xml;subtype",
                new Object[] { RawData.class, CType.COMPLEX,
                        new StringRawData("", "application/gml-3.1.1"),
                        "application/xml,application/gml-3.1.1,application/gml-2.1.2,text/xml; subtype=gml/3.1.1,text/xml; subtype=gml/2.1.2",
                        ".xml" });
        PRIMITIVE_NAME_TYPE_MAP.put("text/xml;subtype=gml/3.1.1",
                PRIMITIVE_NAME_TYPE_MAP.get("text/xml;subtype"));
        PRIMITIVE_NAME_TYPE_MAP.put("text/xml;subtype=gml/2.1.2",
                PRIMITIVE_NAME_TYPE_MAP.get("text/xml;subtype"));
        PRIMITIVE_NAME_TYPE_MAP.put("application/gml-3.1.1",
                PRIMITIVE_NAME_TYPE_MAP.get("text/xml;subtype"));
        PRIMITIVE_NAME_TYPE_MAP.put("application/gml-2.1.2",
                PRIMITIVE_NAME_TYPE_MAP.get("text/xml;subtype"));

        // ----
        PRIMITIVE_NAME_TYPE_MAP.put("application/json",
                new Object[] { RawData.class, CType.COMPLEX,
                        new StringRawData("", "application/json"), "application/json,text/plain",
                        ".json" });

        // ----
        PRIMITIVE_NAME_TYPE_MAP.put("application/owc",
                new Object[] { RawData.class, CType.COMPLEX,
                        new StringRawData("", "application/json"), "application/json,text/plain",
                        ".json" });

        // ----
        PRIMITIVE_NAME_TYPE_MAP.put("image/geotiff",
                new Object[] { RawData.class, CType.COMPLEX,
                        new ResourceRawData(null, "image/geotiff", "tif"),
                        "image/geotiff,image/tiff", ".tif" });
        PRIMITIVE_NAME_TYPE_MAP.put("image/geotiff;stream",
                new Object[] { RawData.class, CType.COMPLEX,
                        new StreamRawData("image/geotiff", null, "tif"), "image/geotiff,image/tiff",
                        ".tif" });

        // ----
        PRIMITIVE_NAME_TYPE_MAP.put("application/x-netcdf",
                new Object[] { RawData.class, CType.COMPLEX,
                        new ResourceRawData(null, "application/x-netcdf", "nc"),
                        "application/x-netcdf", ".nc" });
        PRIMITIVE_NAME_TYPE_MAP.put("application/x-netcdf;stream",
                new Object[] { RawData.class, CType.COMPLEX,
                        new StreamRawData("application/x-netcdf", null, "nc"),
                        "application/x-netcdf", ".nc" });

        // ----
        PRIMITIVE_NAME_TYPE_MAP.put("video/mp4", new Object[] { RawData.class, CType.COMPLEX,
                new ResourceRawData(null, "video/mp4", "mp4"), "video/mp4", ".mp4" });
        PRIMITIVE_NAME_TYPE_MAP.put("video/mp4;stream", new Object[] { RawData.class, CType.COMPLEX,
                new StreamRawData("video/mp4", null, "mp4"), "video/mp4", ".mp4" });
    }

    /**
     * Convert a list of Strings from an Interator into an array of Classes (the Strings are taken as classnames).
     * 
     * @param it A java.util.Iterator pointing to a Collection of Strings
     * @param cl The ClassLoader to use
     * 
     * @return Array of Classes
     * 
     * @throws ClassNotFoundException When a class could not be loaded from the specified ClassLoader
     */
    public final static Class<?>[] convertToJavaClasses(Iterator<String> it, ClassLoader cl)
            throws ClassNotFoundException {
        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
        while (it.hasNext()) {
            classes.add(convertToJavaClass(it.next(), cl, null).getClazz());
        }
        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * Convert a given String into the appropriate Class.
     * 
     * @param name Name of class
     * @param cl ClassLoader to use
     * @param object
     * @param sample
     * 
     * @return The class for the given name
     * 
     * @throws ClassNotFoundException When the class could not be found by the specified ClassLoader
     */
    final static ParameterTemplate convertToJavaClass(String name, ClassLoader cl,
            Object defaultValue) throws ClassNotFoundException {
        int arraySize = 0;
        while (name.endsWith("[]")) {
            name = name.substring(0, name.length() - 2);
            arraySize++;
        }

        // Retrieve the Class of the parameter through the mapping
        String mimeTypes = "";
        Class c = null;
        if (name.equalsIgnoreCase("complex") || name.equalsIgnoreCase("complex")) {
            // Is it a complex/raw data type?
            c = RawData.class;
        } else if (PRIMITIVE_NAME_TYPE_MAP.get(name) != null) {
            // Check for a primitive type
            c = (Class) ((Object[]) PRIMITIVE_NAME_TYPE_MAP.get(name))[0];
        }

        if (c == null) {
            // No primitive, try to load it from the given ClassLoader
            try {
                c = cl.loadClass(name);
            } catch (ClassNotFoundException cnfe) {
                throw new ClassNotFoundException("Parameter class not found: " + name);
            }
        }

        // if we have an array get the array class
        if (arraySize > 0) {
            int[] dims = new int[arraySize];
            for (int i = 0; i < arraySize; i++) {
                dims[i] = 1;
            }
            c = Array.newInstance(c, dims).getClass();
        }

        // Set the default value or the sample object
        Object sample;
        if (defaultValue != null
                && CType.SIMPLE.equals(((Object[]) PRIMITIVE_NAME_TYPE_MAP.get(name))[1])) {
            sample = defaultValue;
        } else if (CType.COMPLEX.equals(((Object[]) PRIMITIVE_NAME_TYPE_MAP.get(name))[1])
                && ((Object[]) PRIMITIVE_NAME_TYPE_MAP.get(name)).length > 2) {
            sample = ((Object[]) PRIMITIVE_NAME_TYPE_MAP.get(name))[2];
        } else {
            sample = null;
        }

        if (PRIMITIVE_NAME_TYPE_MAP.get(name) != null)
            mimeTypes = (String) ((Object[]) PRIMITIVE_NAME_TYPE_MAP.get(name))[3];

        return new ParameterTemplate(c, sample, mimeTypes);
    }
}

/**
 * 
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
class XMPPPacketListener implements PacketListener {

    /** The LOGGER */
    public static final Logger LOGGER = Logging
            .getLogger(XMPPPacketListener.class.getPackage().getName());

    private XMPPClient xmppClient;

    public XMPPPacketListener(XMPPClient xmppClient) {
        this.xmppClient = xmppClient;
    }

    @Override
    public void processPacket(Packet packet) {
        if (packet instanceof Presence) {
            Presence p = (Presence) packet;

            try {
                if (p.isAvailable()) {
                    if (p.getFrom().indexOf("@") > 0) {

                        /**
                         * Manage the channel occupants list
                         */
                        final String channel = p.getFrom().substring(0, p.getFrom().indexOf("@"));
                        /*
                         * if (xmppClient.occupantsList.get(channel) == null) { xmppClient.occupantsList.put(channel, new ArrayList<String>()); } if
                         * (xmppClient.occupantsList.get(channel) != null) { if (!xmppClient.occupantsList.get(channel).contains(p.getFrom()))
                         * xmppClient.occupantsList.get(channel).add(p.getFrom()); }
                         */

                        if (xmppClient.serviceChannels.contains(channel))
                            xmppClient.handleMemberJoin(p);
                    }
                } else if (!p.isAvailable()) {
                    if (p.getFrom().indexOf("@") > 0 && p.getFrom().indexOf("/master") > 0) {
                        boolean mustDeregisterService = true;

                        final String channel = p.getFrom().substring(0, p.getFrom().indexOf("@"));
                        final NameImpl serviceName = xmppClient.extractServiceName(p.getFrom());

                        for (MultiUserChat mucServiceChannel : xmppClient.mucServiceChannels) {
                            if (mucServiceChannel.getRoom().startsWith(channel)) {
                                for (String occupant : mucServiceChannel.getOccupants()) {
                                    if (!occupant.equals(p.getFrom())) {
                                        final Name occupantServiceName = xmppClient
                                                .extractServiceName(occupant);

                                        // send invitation and register source JID
                                        String[] serviceJIDParts = occupant.split("/");
                                        if (serviceJIDParts.length == 3
                                                && (serviceJIDParts[2].startsWith("master")
                                                        || serviceJIDParts[2].indexOf("@") < 0)) {
                                            if (serviceName.equals(occupantServiceName)) {
                                                mustDeregisterService = false;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (mustDeregisterService && xmppClient.serviceChannels.contains(channel))
                            xmppClient.handleMemberLeave(p);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        } else if (packet instanceof Message) {
            Message message = (Message) packet;
            String origin = message.getFrom().split("/")[0];
            Chat chat = xmppClient.setupChat(origin);

            if (message.getBody() != null) {
                LOGGER.fine("ReceivedMessage('" + message.getBody() + "','" + origin + "','"
                        + message.getPacketID() + "');");

                Map<String, String> signalArgs = new HashMap<String, String>();
                try {
                    String[] messageParts = message.getBody().split("&");
                    for (String mp : messageParts) {
                        String[] signalArg = mp.split("=");
                        signalArgs.put(signalArg[0], signalArg[1]);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Wrong message! [" + message.getBody() + "]");
                    signalArgs.clear();
                }

                if (!signalArgs.isEmpty() && signalArgs.containsKey("topic")) {

                    for (XMPPMessage xmppMessage : GeoServerExtensions
                            .extensions(XMPPMessage.class)) {
                        if (xmppMessage.canHandle(signalArgs)) {
                            xmppMessage.handleSignal(xmppClient, packet, message, signalArgs);
                        }
                    }

                }
            }
        }
    }
}

/**
 * 
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
class ParameterTemplate {

    private final Class<?> clazz;

    private final Object defaultValue;

    private final Map<String, String> meta = new HashMap<String, String>();

    /**
     * @param clazz
     * @param defaultValue
     */
    public ParameterTemplate(Class<?> clazz, Object defaultValue, String mimeTypes) {
        this.clazz = clazz;
        this.defaultValue = defaultValue;
        this.meta.put("mimeTypes", mimeTypes);
    }

    /**
     * @return the clazz
     */
    public Class<?> getClazz() {
        return clazz;
    }

    /**
     * @return the defaultValue
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    public Map<String, String> getMeta() {
        return meta;
    }

}
