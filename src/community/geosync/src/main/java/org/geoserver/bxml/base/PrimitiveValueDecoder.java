package org.geoserver.bxml.base;

import java.util.Arrays;
import java.util.List;

import org.geoserver.bxml.ValueDecoder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.springframework.util.Assert;

/**
 * This class parses a primitive value when an EventType.VALUE_ occurs, and return this value in an
 * instance of <T>.
 * 
 * @param <T>
 *            the generic type
 * 
 * @author cfarina
 */
public class PrimitiveValueDecoder<T> implements ValueDecoder<T> {

    /** The can handle. */
    private List<EventType> canHandle = Arrays.asList(EventType.VALUE_BOOL, EventType.VALUE_DOUBLE,
            EventType.VALUE_FLOAT, EventType.VALUE_BYTE, EventType.VALUE_INT, EventType.VALUE_LONG,
            EventType.VALUE_STRING);

    /** The type. */
    private final Class<T> type;

    /**
     * Instantiates a new primitive value decoder.
     * 
     * @param type
     *            the type
     */
    public PrimitiveValueDecoder(Class<T> type) {
        this.type = type;
    }

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the t
     * @throws Exception
     *             the exception
     */
    @Override
    public T decode(BxmlStreamReader r) throws Exception {
        final EventType event = r.getEventType();
        if (!event.isValue()) {
            throw new IllegalArgumentException("r.getEventType() must to be  value event.");
        }
        Assert.isTrue(canHandle(event));

        T value = null;
        value = new PrimitiveReader<T>().read(r, type, event);
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
        return canHandle.contains(type);
    }

}
