/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wps;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONWriter;
import org.geoserver.wps.ppio.CDataPPIO;
import org.springframework.stereotype.Component;

/**
 * PPIO that allows scripts to return a Map to be encoded as JSON.
 *
 * @author Justin Deoliveira, OpenGeo
 */
@Component
public class MapJSONPPIO extends CDataPPIO {

    protected MapJSONPPIO() {
        super(Map.class, Map.class, "application/json");
    }

    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        OutputStreamWriter writer = new OutputStreamWriter(os);
        JSONWriter w = new JSONWriter(writer);

        encode((Map) value, w);
        writer.flush();
    }

    void encode(Map<?, ?> map, JSONWriter w) throws JSONException {
        w.object();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            Object key = e.getKey();
            Object val = e.getValue();

            String name = key != null ? key.toString() : null;
            w.key(name);

            if (val instanceof Map) {
                encode((Map) val, w);
            } else {
                w.value(val);
            }
        }
        w.endObject();
    }

    @Override
    public String getFileExtension() {
        return "json";
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object decode(String input) throws Exception {
        throw new UnsupportedOperationException();
    }
}
