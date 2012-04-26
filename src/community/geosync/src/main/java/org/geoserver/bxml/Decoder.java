package org.geoserver.bxml;

import java.util.Set;

import javax.xml.namespace.QName;

import org.gvsig.bxml.stream.BxmlStreamReader;

/**
 * This interface has to been implemented by any element decoder.
 * 
 * @param <T>
 *            the generic type
 * 
 * @author groldan
 */
public interface Decoder<T> {

    /**
     * Decode the actual element pointed by r.
     * 
     * @param r
     *            the r
     * @return the t
     * @throws Exception
     *             the exception
     */
    public abstract T decode(BxmlStreamReader r) throws Exception;

    /**
     * Return true if can decode an element with name.
     * 
     * @param name
     *            the name of element
     * @return true, if can decode an element with name, else, return false
     */
    public abstract boolean canHandle(QName name);

    /**
     * Return the sets of names that can be decoded by the decoder.
     * 
     * @return the targets
     */
    public abstract Set<QName> getTargets();

}