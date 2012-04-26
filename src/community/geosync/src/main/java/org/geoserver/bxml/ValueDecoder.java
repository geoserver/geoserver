package org.geoserver.bxml;

import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;

/**
 * The Interface ValueDecoder must to be implemented by class that parses a given primitive value.
 * 
 * @param <T>
 *            the generic type
 * 
 * @author cfarina
 */
public interface ValueDecoder<T> {

    /**
     * Decode a primitive value.
     * 
     * @param r
     *            the r
     * @return the t
     * @throws Exception
     *             the exception
     */
    public abstract T decode(BxmlStreamReader r) throws Exception;

    /**
     * Retun true if this decoder can manage an event of type EventType.Value_.
     * 
     * @param type
     *            the type
     * @return true, if successful
     */
    public abstract boolean canHandle(EventType type);
}
