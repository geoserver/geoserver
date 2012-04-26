package org.geoserver.bxml.wfs_1_1;

import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.BxmlElementIterator;
import org.geoserver.bxml.SequenceDecoder;
import org.gvsig.bxml.stream.BxmlStreamReader;

/**
 * The Class SimpleFeatureSequenceDecoder extends from SequenceDecoder in order to use an extension
 * of BXMLIterator, which is SimpleFeatureAttributeIterator.
 * 
 * @param <T>
 *            the generic type
 * 
 * @author cfarina
 */
public class SimpleFeatureSequenceDecoder<T> extends SequenceDecoder<T> {

    /**
     * Instantiates a new simple feature sequence decoder.
     * 
     * @param namespace
     *            the namespace
     * @param minOccurs
     *            the min occurs
     * @param maxOccurs
     *            the max occurs
     */
    public SimpleFeatureSequenceDecoder(final int minOccurs, final int maxOccurs) {
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
        return new SimpleFeatureAttributeIterator(r);
    }

}