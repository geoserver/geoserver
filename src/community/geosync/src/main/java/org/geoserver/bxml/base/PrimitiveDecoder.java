package org.geoserver.bxml.base;

import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.BXMLDecoderUtil;
import org.geoserver.bxml.Decoder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;

/**
 * This class parses an element with a primitive value and return this value as an in an instance of
 * <T>.
 * 
 * @param <T>
 *            the generic type
 * 
 * @author cfarina
 */
public class PrimitiveDecoder<T> implements Decoder<T> {

    /** The name. */
    private final QName name;

    /** The type. */
    private final Class<T> type;

    /**
     * Instantiates a new primitive decoder.
     * 
     * @param name
     *            the name
     * @param type
     *            the type
     */
    public PrimitiveDecoder(final QName name, Class<T> type) {
        this.name = name;
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
        r.require(EventType.START_ELEMENT, null, name.getLocalPart());
        final EventType event = r.next();
        final PrimitiveValueDecoder<T> valueDecoder = new PrimitiveValueDecoder<T>(type);

        T value = null;
        if (event.isValue() && valueDecoder.canHandle(event)) {
            value = (T) valueDecoder.decode(r);
        }

        BXMLDecoderUtil.goToEnd(r, name);
        r.require(EventType.END_ELEMENT, null, name.getLocalPart());

        return value;
    }

    /**
     * Can handle.
     * 
     * @param name
     *            the name
     * @return true, if successful
     */
    @Override
    public boolean canHandle(QName name) {
        return name.equals(name);
    }

    /**
     * Gets the targets.
     * 
     * @return the targets
     */
    @Override
    public Set<QName> getTargets() {
        return Collections.singleton(name);
    }

}
