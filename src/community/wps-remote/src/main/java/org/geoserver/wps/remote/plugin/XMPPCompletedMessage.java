/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.razorvine.pickle.PickleException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geoserver.wps.remote.RemoteProcessClientListener;
import org.geoserver.wps.remote.plugin.output.XMPPOutputDefaultProducer;
import org.geotools.util.logging.Logging;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

/**
 * Listens for "COMPLETE" messages from XMPP service channels and takes action accordingly.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class XMPPCompletedMessage implements XMPPMessage {

    /** The LOGGER */
    public static final Logger LOGGER = Logging.getLogger(XMPPMessage.class.getPackage().getName());

    @Override
    public boolean canHandle(Map<String, String> signalArgs) {
        if (signalArgs != null && signalArgs.get("topic") != null)
            return signalArgs.get("topic").equals("completed");
        return false;
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
        if (msg != null && msg.equals("completed")) {
            final Map<String, Object> outputs = new HashMap<String, Object>();
            try {
                for (Entry<String, String> result :
                        new TreeMap<String, String>(signalArgs).entrySet()) {
                    if (result.getKey().startsWith("result")) {
                        final String serviceResultString =
                                URLDecoder.decode(result.getValue(), "UTF-8");
                        final JSONObject serviceResultJSON =
                                (JSONObject) JSONSerializer.toJSON(serviceResultString);
                        final Object output =
                                xmppClient.unPickle(xmppClient.pickle(serviceResultJSON));

                        // XMPP Output Visitor
                        if (output instanceof Map) {
                            final Map<String, Object> resultParams = (Map<String, Object>) output;
                            // transform the textual value into a real WPS
                            // output
                            try {
                                final Object value =
                                        (resultParams.get(result.getKey() + "_value") != null
                                                ? resultParams.get(result.getKey() + "_value")
                                                : null);
                                final String type =
                                        (String)
                                                (resultParams.get(result.getKey() + "_type") != null
                                                        ? resultParams.get(
                                                                result.getKey() + "_type")
                                                        : null);
                                final String description =
                                        (resultParams.get(result.getKey() + "_description") != null
                                                        && resultParams.get(
                                                                        result.getKey()
                                                                                + "_description")
                                                                instanceof String
                                                ? (String)
                                                        resultParams.get(
                                                                result.getKey() + "_description")
                                                : null);
                                final String title =
                                        (resultParams.get(result.getKey() + "_title") != null
                                                        && resultParams.get(
                                                                        result.getKey() + "_title")
                                                                instanceof String
                                                ? (String)
                                                        resultParams.get(result.getKey() + "_title")
                                                : null);
                                final String layerName =
                                        (resultParams.get(result.getKey() + "_layer_name") != null
                                                        && resultParams.get(
                                                                        result.getKey()
                                                                                + "_layer_name")
                                                                instanceof String
                                                ? (String)
                                                        resultParams.get(
                                                                result.getKey() + "_layer_name")
                                                : null);
                                final String defaultStyle =
                                        (resultParams.get(result.getKey() + "_style") != null
                                                        && resultParams.get(
                                                                        result.getKey() + "_style")
                                                                instanceof String
                                                ? (String)
                                                        resultParams.get(result.getKey() + "_style")
                                                : null);
                                final String targetWorkspace =
                                        (resultParams.get(result.getKey() + "_workspace") != null
                                                        && resultParams.get(
                                                                        result.getKey()
                                                                                + "_workspace")
                                                                instanceof String
                                                ? (String)
                                                        resultParams.get(
                                                                result.getKey() + "_workspace")
                                                : null);
                                final String metadata =
                                        (resultParams.get(result.getKey() + "_metadata") != null
                                                        && resultParams.get(
                                                                        result.getKey()
                                                                                + "_metadata")
                                                                instanceof String
                                                ? (String)
                                                        resultParams.get(
                                                                result.getKey() + "_metadata")
                                                : null);

                                Boolean publish = true;

                                if (resultParams.get(result.getKey() + "_pub") != null) {
                                    if (resultParams.get(result.getKey() + "_pub")
                                            instanceof String)
                                        publish =
                                                Boolean.valueOf(
                                                        (String)
                                                                resultParams.get(
                                                                        result.getKey() + "_pub"));
                                    else if (resultParams.get(result.getKey() + "_pub")
                                            instanceof Boolean)
                                        publish =
                                                (Boolean)
                                                        resultParams.get(result.getKey() + "_pub");
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

                                LOGGER.finest(
                                        "[XMPPCompletedMessage] wpsOutputValue:" + wpsOutputValue);

                                // add the transformed result to the process
                                // outputs
                                if (wpsOutputValue != null) {
                                    outputs.put(result.getKey(), wpsOutputValue);
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
}
