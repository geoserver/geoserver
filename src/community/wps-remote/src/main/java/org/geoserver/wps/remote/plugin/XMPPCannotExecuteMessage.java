/* (c) 2014 - 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin;

import java.util.Map;
import java.util.logging.Level;
import org.geoserver.wps.remote.RemoteRequestDescriptor;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

/**
 * Listens for "CANTEXEC" messages from XMPP service channels and takes action accordingly.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class XMPPCannotExecuteMessage extends XMPPOutputMessage {

    public XMPPCannotExecuteMessage() {
        super("cantexec");
    }

    @Override
    public void handleSignal(
            XMPPClient xmppClient, Packet packet, Message message, Map<String, String> signalArgs) {

        final String serviceJID = (message != null ? message.getFrom() : packet.getFrom());
        final String pID = signalArgs.get("id");
        final String msg = signalArgs.get("message");
        final String execId = signalArgs.get("exec_id");

        if (pID != null
                && pID.equalsIgnoreCase("master")
                && msg != null
                && msg.equals(this.topic)) {
            if (xmppClient.getExecutingRequests().containsKey(execId)) {
                RemoteRequestDescriptor request = xmppClient.getExecutingRequests().get(execId);
                if (request != null && request.getMetadata().get("serviceJID").equals(serviceJID)) {
                    // Cleanup the executing request
                    xmppClient.getExecutingRequests().remove(execId);
                    // Enqueuing the executing request
                    xmppClient.getPendingRequests().add(request);
                    LOGGER.log(Level.FINE, "Enqueuing Request [" + execId + "]...");
                }
            }
        }
    }
}
