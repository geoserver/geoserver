package org.geoserver.bxml.base;

import java.util.Arrays;
import java.util.List;

import org.geoserver.bxml.ValueDecoder;
import org.geoserver.bxml.base.wrapper.ArrayWrapper;
import org.geoserver.bxml.base.wrapper.ArrayWrapperFactory;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.springframework.util.Assert;

import com.google.common.base.Preconditions;

/**
 * This class parses an array of primitive values when an EventType.VALUE_ occurs, and return this
 * as an array of <T>.
 * 
 * @param <T>
 *            the generic type
 * 
 * @author cfarina
 */
public class PrimitiveListValueDecoder<T> implements ValueDecoder<T> {

    /** The can handle. */
    private List<EventType> canHandle = Arrays.asList(EventType.VALUE_BOOL, EventType.VALUE_DOUBLE,
            EventType.VALUE_FLOAT, EventType.VALUE_BYTE, EventType.VALUE_INT, EventType.VALUE_LONG,
            EventType.VALUE_STRING);

    /** The type. */
    private final Class<T> type;

    /**
     * Instantiates a new primitive list value decoder.
     * 
     * @param type
     *            the type
     */
    public PrimitiveListValueDecoder(Class<T> type) {
        Preconditions.checkArgument(type.getComponentType() != null);
        Preconditions.checkArgument(type.getComponentType().isPrimitive());
        this.type = type;
    }

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the t[]
     * @throws Exception
     *             the exception
     */
    @SuppressWarnings("unchecked")
    @Override
    public T decode(BxmlStreamReader r) throws Exception {
        StringBuilder sb = new StringBuilder();

        EventType eventType = r.getEventType();

        ArrayWrapper arrayWrapper = ArrayWrapperFactory.buildArrayFactory(type);

        if (!eventType.isValue()) {
            throw new IllegalArgumentException("r.getEventType() must to be  value event.");
        }
        Assert.isTrue(canHandle(eventType));

        while (eventType.isValue()) {

            if (EventType.VALUE_STRING.equals(eventType)) {
                sb.append(new StringValueDecoder().decode(r));
            } else {
                if (sb.length() > 0) {
                    arrayWrapper.append(sb);
                    sb.setLength(0);
                }
                arrayWrapper.addNewValues(r);
            }
            if (r.getEventType().isValue()) {
                eventType = r.next();
            } else {
                break;
            }

        }
        if (sb.length() > 0) {
            arrayWrapper.append(sb);
            sb.setLength(0);
        }
        return (T) arrayWrapper.getArray();
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
