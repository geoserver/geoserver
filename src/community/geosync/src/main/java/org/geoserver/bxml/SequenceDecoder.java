package org.geoserver.bxml;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.springframework.util.Assert;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;

/**
 * The Class SequenceDecoder.
 * 
 * @param <T>
 *            the generic type
 * 
 * @author groldan
 */
public class SequenceDecoder<T> implements Decoder<Iterator<T>> {

    /** The sequence. */
    private List<Particle<T>> sequence;

    /** The min occurs. */
    private final int minOccurs;

    /** The max occurs. */
    private final int maxOccurs;

    /**
     * Instantiates a new sequence decoder.
     */
    public SequenceDecoder() {
        this(1, 1);
    }

    /**
     * Instantiates a new sequence decoder.
     * 
     * @param minOccurs
     *            the min occurs
     * @param maxOccurs
     *            the max occurs
     */
    public SequenceDecoder(final int minOccurs, final int maxOccurs) {
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        Assert.isTrue(minOccurs >= 0);
        Assert.isTrue(maxOccurs >= 0);
        Assert.isTrue(minOccurs <= maxOccurs);
        this.sequence = new LinkedList<Particle<T>>();
    }

    /**
     * Adds the.
     * 
     * @param particleDecoder
     *            the particle decoder
     * @param minOccurs
     *            the min occurs
     * @param maxOccurs
     *            the max occurs
     */
    public void add(final Decoder<T> particleDecoder, final int minOccurs, final int maxOccurs) {
        Assert.notNull(particleDecoder);
        Assert.isTrue(minOccurs >= 0);
        Assert.isTrue(maxOccurs >= 0);
        Assert.isTrue(minOccurs <= maxOccurs);
        sequence.add(new Particle<T>(particleDecoder, minOccurs, maxOccurs));
    }

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the iterator
     * @throws Exception
     *             the exception
     */
    @Override
    public Iterator<T> decode(final BxmlStreamReader r) throws Exception {
        r.require(EventType.START_ELEMENT, null, null);

        Set<QName> sequenceNames = getTargets();
        BxmlElementIterator xmlIterator = buildIterator(r, sequenceNames);

        Chain<T> chain = new Chain<T>(sequence, minOccurs, maxOccurs);
        Iterator<T> result = Iterators.transform(xmlIterator, chain);
        return result;
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
    protected BxmlElementIterator buildIterator(final BxmlStreamReader r, Set<QName> sequenceNames) {
        return new BxmlElementIterator(r, new HashSet<QName>(sequenceNames));
    }

    /**
     * Gets the targets.
     * 
     * @return the targets
     * @see org.geoserver.bxml.Decoder#getTargets()
     */
    public Set<QName> getTargets() {
        Set<QName> names = new HashSet<QName>();
        for (Particle<T> p : sequence) {
            names.addAll(p.particleDecoder.getTargets());
        }
        return names;
    }

    /**
     * Can handle.
     * 
     * @param name
     *            the name
     * @return true, if successful
     */
    @Override
    public boolean canHandle(final QName name) {
        for (Particle<T> p : sequence) {
            if (p.particleDecoder.canHandle(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The Class Particle.
     * 
     * @param <E>
     *            the element type
     * @author groldan
     */
    private static class Particle<E> {

        /** The particle decoder. */
        private final Decoder<E> particleDecoder;

        /** The min occurs. */
        private final int minOccurs;

        /** The max occurs. */
        private final int maxOccurs;

        /**
         * Instantiates a new particle.
         * 
         * @param particleDecoder
         *            the particle decoder
         * @param minOccurs
         *            the min occurs
         * @param maxOccurs
         *            the max occurs
         */
        public Particle(Decoder<E> particleDecoder, int minOccurs, int maxOccurs) {
            this.particleDecoder = particleDecoder;
            this.minOccurs = minOccurs;
            this.maxOccurs = maxOccurs;
        }

    }

    /**
     * The Class Chain.
     * 
     * @param <E>
     *            the element type
     */
    private static class Chain<E> implements Function<BxmlStreamReader, E> {

        /** The sequence min occurs. */
        private final int sequenceMinOccurs;

        /** The sequence max occurs. */
        private final int sequenceMaxOccurs;

        /** The sequence occurrencies. */
        private int sequenceOccurrencies;

        /** The sequence. */
        private final List<Particle<E>> sequence;

        /** The current particle. */
        private int currentParticle;

        /** The current particle occurrencies. */
        private int currentParticleOccurrencies;

        /**
         * Instantiates a new chain.
         * 
         * @param sequence
         *            the sequence
         * @param minOccurs
         *            the min occurs
         * @param maxOccurs
         *            the max occurs
         */
        public Chain(List<Particle<E>> sequence, int minOccurs, int maxOccurs) {
            this.sequenceMinOccurs = minOccurs;
            this.sequenceMaxOccurs = maxOccurs;
            this.sequenceOccurrencies = 0;
            this.sequence = sequence;
            this.currentParticle = -1;
        }

        /**
         * Apply.
         * 
         * @param positionedReader
         *            the positioned reader
         * @return the e
         */
        @Override
        public E apply(final BxmlStreamReader positionedReader) {
            final QName elementName = positionedReader.getElementName();
            EventType eventType = positionedReader.getEventType();
            final Decoder<E> particleDecoder = findParticleDecoder(elementName);
            E decoded = null;
            try {
                decoded = particleDecoder.decode(positionedReader);
            } catch (Exception e) {
                Throwables.propagate(e);
            }
            return decoded;
        }

        /**
         * Find particle decoder.
         * 
         * @param elementName
         *            the element name
         * @return the decoder
         */
        private Decoder<E> findParticleDecoder(final QName elementName) {
            while (true) {
                if (currentParticle == -1 || currentParticle == sequence.size()) {
                    currentParticle = 0;
                    currentParticleOccurrencies = 0;
                    sequenceOccurrencies++;
                }
                Particle<E> p = sequence.get(currentParticle);
                if (currentParticleOccurrencies == p.maxOccurs) {
                    currentParticle++;
                    currentParticleOccurrencies = 0;
                    continue;
                }

                if (p.particleDecoder.canHandle(elementName)) {
                    currentParticleOccurrencies++;
                    if (currentParticleOccurrencies > p.maxOccurs) {
                        throw new IllegalStateException();
                    }
                    return p.particleDecoder;
                }
                if (currentParticleOccurrencies < p.minOccurs) {
                    throw new IllegalStateException();
                }
            }
        }
    }
}
