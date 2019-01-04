/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.geoserver.wps.remote.RemoteProcessClientListener;
import org.geotools.util.logging.Logging;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

/**
 * Listens for "UNREGISTER" messages from XMPP service channels and takes action accordingly.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class XMPPUnRegisterMessage implements XMPPMessage {

    /** The LOGGER */
    public static final Logger LOGGER = Logging.getLogger(XMPPMessage.class.getPackage().getName());

    @Override
    public boolean canHandle(Map<String, String> signalArgs) {
        if (signalArgs != null && signalArgs.get("topic") != null)
            return signalArgs.get("topic").equals("unregister");
        return false;
    }

    @Override
    public void handleSignal(
            XMPPClient xmppClient, Packet packet, Message message, Map<String, String> signalArgs) {

        try {
            xmppClient.handleMemberLeave(packet);
        } catch (Exception e) {
            // NOTIFY LISTENERS
            final Set<RemoteProcessClientListener> remoteClientListeners =
                    xmppClient.getRemoteClientListeners();
            synchronized (remoteClientListeners) {
                for (RemoteProcessClientListener listener : remoteClientListeners) {

                    Map<String, Object> metadata = new HashMap<String, Object>();
                    metadata.put("serviceJID", packet.getFrom());

                    final String pID = (signalArgs != null ? signalArgs.get("id") : null);

                    listener.exceptionOccurred(pID, e, metadata);
                }
            }
        }
    }
}
