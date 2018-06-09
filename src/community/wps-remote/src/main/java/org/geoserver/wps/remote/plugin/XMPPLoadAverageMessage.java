/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.razorvine.pickle.PickleException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geoserver.wps.remote.RemoteMachineDescriptor;
import org.geotools.util.logging.Logging;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

/**
 * Listens for "LOADAVG" messages from XMPP service channels and takes action accordingly.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class XMPPLoadAverageMessage implements XMPPMessage {

    /** The LOGGER */
    public static final Logger LOGGER = Logging.getLogger(XMPPMessage.class.getPackage().getName());

    @Override
    public boolean canHandle(Map<String, String> signalArgs) {
        if (signalArgs != null && signalArgs.get("topic") != null)
            return signalArgs.get("topic").equals("loadavg");
        return false;
    }

    @Override
    public void handleSignal(
            XMPPClient xmppClient, Packet packet, Message message, Map<String, String> signalArgs) {

        final String serviceJID = (message != null ? message.getFrom() : packet.getFrom());

        final String pID = signalArgs.get("id");
        final String msg = signalArgs.get("message");

        if (pID != null && pID.equalsIgnoreCase("master") && msg != null && msg.equals("loadavg")) {
            Map<String, Object> outputs = new HashMap<String, Object>();
            try {
                final List<RemoteMachineDescriptor> registeredProcessingMachines =
                        xmppClient.getRegisteredProcessingMachines();

                synchronized (registeredProcessingMachines) {
                    RemoteMachineDescriptor registeredProcessingMachine = null;
                    for (RemoteMachineDescriptor node : registeredProcessingMachines) {
                        if (serviceJID.equals(node.getNodeJID())) {
                            registeredProcessingMachine = node;
                            registeredProcessingMachine.setAvailable(true);
                            break;
                        }
                    }

                    if (registeredProcessingMachine != null) {
                        for (Entry<String, String> result : signalArgs.entrySet()) {
                            if (result.getKey().startsWith("result_")) {
                                final String key = result.getKey().substring("result_".length());
                                final String serviceResultString =
                                        URLDecoder.decode(result.getValue(), "UTF-8");
                                final JSONObject serviceResultJSON =
                                        (JSONObject) JSONSerializer.toJSON(serviceResultString);
                                final Object output =
                                        xmppClient.unPickle(xmppClient.pickle(serviceResultJSON));

                                // XMPP Output Visitor
                                if (output instanceof Map) {
                                    Map<String, Object> resultParams = (Map<String, Object>) output;
                                    // transform the textual value into a real WPS
                                    // output
                                    try {
                                        final Object value =
                                                (resultParams.get(key + "_value") != null
                                                        ? resultParams.get(key + "_value")
                                                        : null);
                                        final String description =
                                                (resultParams.get(key + "_description") != null
                                                                && resultParams.get(
                                                                                result.getKey()
                                                                                        + "_description")
                                                                        instanceof String
                                                        ? (String)
                                                                resultParams.get(
                                                                        key + "_description")
                                                        : null);

                                        if ("vmem".equalsIgnoreCase(key)) {
                                            registeredProcessingMachine.setMemPercUsed(
                                                    (Double) value);
                                        }

                                        if ("loadavg".equalsIgnoreCase(key)) {
                                            registeredProcessingMachine.setLoadAverage(
                                                    (Double) value);
                                        }

                                    } catch (Exception e) {
                                        LOGGER.log(
                                                Level.SEVERE,
                                                "Exception occurred while trying to produce the result:",
                                                e);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (PickleException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            } catch (IOException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
        }
    }
}
