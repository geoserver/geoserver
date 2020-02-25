/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.wps.remote.RemoteProcessClientListener;
import org.geotools.util.logging.Logging;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

/**
 * Listens for "LOG" messages from XMPP service channels and takes action accordingly.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class XMPPLogMessage implements XMPPMessage {

    /** The LOGGER */
    public static final Logger LOGGER = Logging.getLogger(XMPPMessage.class.getPackage().getName());

    @Override
    public boolean canHandle(Map<String, String> signalArgs) {
        if (signalArgs != null && signalArgs.get("topic") != null)
            return signalArgs.get("topic").equals("log");
        return false;
    }

    @Override
    public void handleSignal(
            XMPPClient xmppClient, Packet packet, Message message, Map<String, String> signalArgs) {

        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("serviceJID", packet.getFrom());

        try {
            final String pID = (signalArgs != null ? signalArgs.get("id") : null);

            Level logLevel = Level.INFO;
            try {
                logLevel = Level.parse(signalArgs.get("level"));
            } catch (Exception e) {
                LOGGER.fine(
                        "Could not correctly parse the Log level; using the default one 'INFO'.");
            }
            String logMessage =
                    "[" + pID + "]" + URLDecoder.decode(signalArgs.get("message"), "UTF-8");
            LOGGER.log(logLevel, logMessage);

            // NOTIFY LISTENERS
            for (RemoteProcessClientListener listener : xmppClient.getRemoteClientListeners()) {
                listener.setTask(pID, logMessage);
                listener.progress(pID, listener.getProgress(pID));
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Error while trying to decode Log message: " + message.getBody(),
                    e);
        }
    }
}
