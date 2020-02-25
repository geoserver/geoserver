/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote.plugin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import net.razorvine.pickle.PickleException;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geotools.util.logging.Logging;

/**
 * Listens for Ouput messages from XMPP service channels and takes action accordingly.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public abstract class XMPPOutputMessage implements XMPPMessage {

    /** The LOGGER */
    public static final Logger LOGGER = Logging.getLogger(XMPPMessage.class.getPackage().getName());

    String topic;

    public XMPPOutputMessage(String topic) {
        this.topic = topic;
    }

    @Override
    public boolean canHandle(Map<String, String> signalArgs) {
        if (signalArgs != null && signalArgs.get("topic") != null)
            return signalArgs.get("topic").equals(this.topic);
        return false;
    }

    /** */
    protected Object getOutPuts(XMPPClient xmppClient, Entry<String, String> result)
            throws UnsupportedEncodingException, PickleException, IOException {
        final String serviceResultString = URLDecoder.decode(result.getValue(), "UTF-8");
        final JSONObject serviceResultJSON =
                (JSONObject) JSONSerializer.toJSON(serviceResultString);

        // Cleaning up from JSONNull values
        final LinkedList nullValues = new LinkedList<>();
        for (Object jsonKey : serviceResultJSON.keySet()) {
            Object value = serviceResultJSON.get(jsonKey);
            if (value == null || value instanceof JSONNull) {
                nullValues.add(jsonKey);
            }
        }
        for (Object keyToDiscard : nullValues) {
            serviceResultJSON.discard((String) keyToDiscard);
        }

        // Transforming the JSON Object into a HashMap
        final Object output = xmppClient.unPickle(xmppClient.pickle(serviceResultJSON));
        return output;
    }
}
