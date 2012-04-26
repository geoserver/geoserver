package org.geoserver.bxml.base;

import org.geoserver.bxml.ValueDecoder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;

/**
 * The Class StringValueDecoder.
 * 
 * @author cfarina
 */
public class StringValueDecoder implements ValueDecoder<String> {

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the string
     * @throws Exception
     *             the exception
     */
    @Override
    public String decode(BxmlStreamReader r) throws Exception {
        EventType type;
        if (!r.getEventType().isValue()) {
            throw new IllegalArgumentException("r.getEventType() must to be  value event.");
        }
        StringBuilder sb = null;
        while ((type = r.getEventType()).isValue()) {
            if (sb == null) {
                sb = new StringBuilder();
            }
            sb.append(r.getStringValue());
            r.next();
        }
        return sb == null ? null : sb.toString();
    }

    /**
     * Can handle.
     * 
     * @param type
     *            the type
     * @return true, if successful
     */
    @Override
    public boolean canHandle(EventType type) {
        return EventType.VALUE_STRING.equals(type);
    }

}
