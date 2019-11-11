/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.wps.remote.RemoteProcessClientListener;
import org.geotools.util.logging.Logging;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

/**
 * Listens for "ERROR" messages from XMPP service channels and takes action accordingly.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class XMPPErrorMessage implements XMPPMessage {

    /** The LOGGER */
    public static final Logger LOGGER = Logging.getLogger(XMPPMessage.class.getPackage().getName());

    @Override
    public boolean canHandle(Map<String, String> signalArgs) {
        if (signalArgs != null && signalArgs.get("topic") != null)
            return signalArgs.get("topic").equals("error");
        return false;
    }

    @Override
    public void handleSignal(
            XMPPClient xmppClient, Packet packet, Message message, Map<String, String> signalArgs) {

        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("serviceJID", packet.getFrom());

        Exception cause = null;
        try {
            cause = new Exception(URLDecoder.decode(signalArgs.get("message"), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            cause = e;
        }
        final String pID = (signalArgs != null ? signalArgs.get("id") : null);

        // NOTIFY LISTENERS
        for (RemoteProcessClientListener listener : xmppClient.getRemoteClientListeners()) {
            listener.setTask(pID, cause.getLocalizedMessage());
            listener.exceptionOccurred(pID, cause, metadata);
            listener.progress(pID, listener.getProgress(pID));
        }

        // Cleanup the executing requests
        if (xmppClient.getExecutingRequests().containsKey(pID)) {
            xmppClient.getExecutingRequests().remove(pID);
        }
    }
}
