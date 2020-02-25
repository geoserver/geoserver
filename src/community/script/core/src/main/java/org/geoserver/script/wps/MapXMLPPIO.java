/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wps;

import java.io.InputStream;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.wps.ppio.XMLPPIO;
import org.springframework.stereotype.Component;
import org.xml.sax.ContentHandler;

/**
 * PPIO that allows scripts to return a Map to be encoded as JSON.
 *
 * @author Justin Deoliveira, OpenGeo
 */
@Component
public class MapXMLPPIO extends XMLPPIO {

    public MapXMLPPIO() {
        super(Map.class, Map.class, "application/xml", new QName("result"));
    }

    @Override
    public void encode(Object object, ContentHandler handler) throws Exception {
        handler.startDocument();
        Map map = (Map) object;
        if (map.size() > 1) {
            handler.startElement(null, "map", "map", null);
            encode((Map) object, handler);
            handler.endElement(null, "map", "map");
        } else {
            encode((Map) object, handler);
        }
        handler.endDocument();
    }

    void encode(Map<?, ?> map, ContentHandler h) throws Exception {
        for (Map.Entry<?, ?> e : map.entrySet()) {
            Object key = e.getKey();
            Object val = e.getValue();
            String name = key != null ? key.toString() : "null";

            // startKey(name, s);
            h.startElement(null, name, name, null);
            if (val != null) {
                if (val instanceof Map) {
                    encode((Map) val, h);
                } else {
                    String str = val.toString();
                    h.characters(str.toCharArray(), 0, str.length());
                }
            } else {
                // nil(s);
            }
            h.endElement(null, name, name);
            // endKey(name, s);
        }
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        throw new UnsupportedOperationException();
    }
}
