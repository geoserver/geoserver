package org.geoserver.bxml.base;

import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.BXMLDecoderUtil;
import org.geoserver.bxml.Decoder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;

import com.google.common.base.Preconditions;

/**
 * This class parses an element with an array of primitive value and return this value as an in an
 * array of <T>.
 * 
 * @param <T>
 *            the generic type
 * 
 * @author cfarina
 */
public class PrimitiveListDecoder<T> implements Decoder<T> {

    /** The name. */
    private final QName name;

    /** The type. */
    private final Class<T> type;

    /**
     * Instantiates a new primitive list decoder.
     * 
     * @param name
     *            the name
     * @param type
     *            the type
     */
    public PrimitiveListDecoder(final QName name, Class<T> type) {
        Preconditions.checkArgument(type.getComponentType() != null);
        Preconditions.checkArgument(type.getComponentType().isPrimitive());
        this.name = name;
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
    @Override
    public T decode(BxmlStreamReader r) throws Exception {
        r.require(EventType.START_ELEMENT, null, name.getLocalPart());
        EventType eventType = r.next();

        T values = null;
        if (eventType.isValue()) {
            values = new PrimitiveListValueDecoder<T>(type).decode(r);
        }

        BXMLDecoderUtil.goToEnd(r, name);
        r.require(EventType.END_ELEMENT, null, name.getLocalPart());
        return values;
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
        return this.name.equals(name);
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
