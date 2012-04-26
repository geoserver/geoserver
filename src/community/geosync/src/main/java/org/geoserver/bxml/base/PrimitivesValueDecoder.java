package org.geoserver.bxml.base;

import org.geoserver.bxml.ValueDecoder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;

/**
 * The Class PrimitivesValueDecoder.
 * 
 * @author cfarina
 */
public class PrimitivesValueDecoder implements ValueDecoder<Object> {

    /** The string decoder. */
    final private StringValueDecoder stringDecoder = new StringValueDecoder();

    /** The primitives value decoder. */
    final private PrimitiveValueDecoder<Object> primitivesValueDecoder = new PrimitiveValueDecoder<Object>(
            Object.class);

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the object
     * @throws Exception
     *             the exception
     */
    @Override
    public Object decode(BxmlStreamReader r) throws Exception {
        EventType type = r.getEventType();

        if (!type.isValue()) {
            throw new IllegalArgumentException("r.getEventType() must be a value event: " + type);
        }

        Object value = null;

        if (EventType.VALUE_STRING == type) {
            value = stringDecoder.decode(r);
        } else if (r.getValueCount() > 1) {
            PrimitiveListValueDecoder decoder = PrimitiveListValueDecoderFactory.build(type);
            value = decoder.decode(r);
        } else {
            value = primitivesValueDecoder.decode(r);
        }
        return value;
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
        return true;
    }

}
