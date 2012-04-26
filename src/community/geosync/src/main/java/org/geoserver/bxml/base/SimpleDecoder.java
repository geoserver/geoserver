package org.geoserver.bxml.base;

import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.Decoder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;

/**
 * This class is intend to be extended by one simple element decoders.
 * 
 * @param <T>
 *            the generic type
 * 
 * @author cfarina
 */
public abstract class SimpleDecoder<T> implements Decoder<T> {

    /** The elem name. */
    protected final QName elemName;

    /**
     * Instantiates a new simple decoder.
     * 
     * @param elemName
     *            the elem name
     */
    public SimpleDecoder(final QName elemName) {
        this.elemName = elemName;
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
        return elemName.equals(name);
    }

    /**
     * Gets the targets.
     * 
     * @return the targets
     */
    @Override
    public Set<QName> getTargets() {
        return Collections.singleton(elemName);
    }

    /**
     * Checks if is end element.
     * 
     * @param r
     *            the r
     * @return true, if is end element
     */
    protected boolean isEndElement(BxmlStreamReader r) {
        return r.getEventType() == EventType.END_ELEMENT && r.getElementName().equals(elemName);
    }

}
