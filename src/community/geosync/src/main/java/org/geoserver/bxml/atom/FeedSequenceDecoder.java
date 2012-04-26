package org.geoserver.bxml.atom;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.BxmlElementIterator;
import org.geoserver.bxml.SequenceDecoder;
import org.gvsig.bxml.stream.BxmlStreamReader;

/**
 * The Class FeedSequenceDecoder.
 * 
 * @param <T>
 *            the generic type
 * 
 * @author cfarina
 */
public class FeedSequenceDecoder<T> extends SequenceDecoder<T> {

    /**
     * Instantiates a new feed sequence decoder.
     * 
     * @param minOccurs
     *            the min occurs
     * @param maxOccurs
     *            the max occurs
     */
    public FeedSequenceDecoder(final int minOccurs, final int maxOccurs) {
        super(minOccurs, maxOccurs);
    }

    /**
     * Builds the iterator.
     * 
     * @param r
     *            the r
     * @param sequenceNames
     *            the sequence names
     * @return the bxml element iterator
     */
    @Override
    protected BxmlElementIterator buildIterator(BxmlStreamReader r, Set<QName> sequenceNames) {
        return new FeedElementIterator(r, new HashSet<QName>(sequenceNames));
    }
}
