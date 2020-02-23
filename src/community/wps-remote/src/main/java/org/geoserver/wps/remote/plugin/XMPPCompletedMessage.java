/* (c) 2014 - 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import net.razorvine.pickle.PickleException;
import org.geoserver.wps.remote.RemoteProcessClientListener;
import org.geoserver.wps.remote.plugin.output.XMPPOutputDefaultProducer;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

/**
 * Listens for "COMPLETE" messages from XMPP service channels and takes action accordingly.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class XMPPCompletedMessage extends XMPPOutputMessage {

    public XMPPCompletedMessage() {
        super("completed");
    }

    @Override
    public void handleSignal(
            XMPPClient xmppClient, Packet packet, Message message, Map<String, String> signalArgs)
            throws IOException {

        final String pID = signalArgs.get("id");
        final String msg = signalArgs.get("message");
        final String baseURL = signalArgs.get("baseURL");
        final XMPPOutputDefaultProducer outputProducer = new XMPPOutputDefaultProducer();

        // NOTIFY THE LISTENERS
        if (msg != null && msg.equals(this.topic)) {
            final Map<String, Object> outputs = new HashMap<String, Object>();
            try {
                for (Entry<String, String> result :
                        new TreeMap<String, String>(signalArgs).entrySet()) {
                    if (result.getKey().startsWith("result")) {
                        final String key = result.getKey();
                        final Object output = getOutPuts(xmppClient, result);
                        // XMPP Output Visitor
                        if (output instanceof Map) {
                            final Map<String, Object> resultParams = (Map<String, Object>) output;
                            // transform the textual value into a real WPS
                            // output
                            try {
                                Object wpsOutputValue =
                                        transformOutputs(
                                                xmppClient,
                                                pID,
                                                baseURL,
                                                outputProducer,
                                                key,
                                                resultParams);

                                // add the transformed result to the process
                                // outputs
                                if (wpsOutputValue != null) {
                                    outputs.put(key, wpsOutputValue);
                                    continue;
                                } else {
                                    // throw new Exception("All the Output
                                    // Producers failed transforming the WPS
                                    // Output!");
                                    LOGGER.warning(
                                            "At least one of the Oputput Producres failed transforming the WPS Output!");
                                }
                            } catch (Exception e) {
                                LOGGER.log(
                                        Level.SEVERE,
                                        "Exception occurred while trying to produce the result:",
                                        e);
                                throw new IOException(
                                        "Exception occurred while trying to produce the result:",
                                        e);
                            }
                        }
                    }
                }

                for (RemoteProcessClientListener listener : xmppClient.getRemoteClientListeners()) {
                    listener.complete(pID, outputs);
                }
            } catch (PickleException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
                throw new IOException(e.getMessage(), e);

            } catch (IOException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
                throw new IOException(e.getMessage(), e);
            } finally {
                // Cleanup the executing requests
                if (xmppClient.getExecutingRequests().containsKey(pID)) {
                    xmppClient.getExecutingRequests().remove(pID);
                }
            }
        }
        // In any case stop the process by notifying the listeners ...
        else {
            for (RemoteProcessClientListener listener : xmppClient.getRemoteClientListeners()) {
                listener.complete(pID, null);
            }
        }

        // NOTIFY THE SERVICE
        final String serviceJID = message.getFrom();
        xmppClient.sendMessage(serviceJID, "topic=finish");
    }

    /** */
    protected Object transformOutputs(
            XMPPClient xmppClient,
            final String pID,
            final String baseURL,
            final XMPPOutputDefaultProducer outputProducer,
            final String key,
            final Map<String, Object> resultParams)
            throws Exception {
        final Object value =
                (resultParams.get(key + "_value") != null
                        ? resultParams.get(key + "_value")
                        : null);
        final String type =
                (String)
                        (resultParams.get(key + "_type") != null
                                ? resultParams.get(key + "_type")
                                : null);
        final String description =
                (resultParams.get(key + "_description") != null
                                && resultParams.get(key + "_description") instanceof String
                        ? (String) resultParams.get(key + "_description")
                        : null);
        final String title =
                (resultParams.get(key + "_title") != null
                                && resultParams.get(key + "_title") instanceof String
                        ? (String) resultParams.get(key + "_title")
                        : null);
        final String layerName =
                (resultParams.get(key + "_layer_name") != null
                                && resultParams.get(key + "_layer_name") instanceof String
                        ? (String) resultParams.get(key + "_layer_name")
                        : null);
        final String defaultStyle =
                (resultParams.get(key + "_style") != null
                                && resultParams.get(key + "_style") instanceof String
                        ? (String) resultParams.get(key + "_style")
                        : null);
        final String targetWorkspace =
                (resultParams.get(key + "_workspace") != null
                                && resultParams.get(key + "_workspace") instanceof String
                        ? (String) resultParams.get(key + "_workspace")
                        : null);
        final String metadata =
                (resultParams.get(key + "_metadata") != null
                                && resultParams.get(key + "_metadata") instanceof String
                        ? (String) resultParams.get(key + "_metadata")
                        : null);

        Boolean publish = true;

        if (resultParams.get(key + "_pub") != null) {
            if (resultParams.get(key + "_pub") instanceof String)
                publish = Boolean.valueOf((String) resultParams.get(key + "_pub"));
            else if (resultParams.get(key + "_pub") instanceof Boolean)
                publish = (Boolean) resultParams.get(key + "_pub");
        }

        Object wpsOutputValue =
                outputProducer.produceOutput(
                        value,
                        type,
                        pID,
                        baseURL,
                        xmppClient,
                        publish,
                        layerName,
                        title,
                        description,
                        defaultStyle,
                        targetWorkspace,
                        metadata);

        LOGGER.finest("[XMPPCompletedMessage] wpsOutputValue:" + wpsOutputValue);
        return wpsOutputValue;
    }
}
