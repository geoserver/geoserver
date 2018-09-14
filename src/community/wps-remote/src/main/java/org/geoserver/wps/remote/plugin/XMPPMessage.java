/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin;

import java.io.IOException;
import java.util.Map;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

/**
 * Base interface for XMPP Service Messages sent by remote processes.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public interface XMPPMessage {

    public boolean canHandle(Map<String, String> signalArgs);

    public void handleSignal(
            XMPPClient xmppClient, Packet packet, Message message, Map<String, String> signalArgs)
            throws IOException;
}
