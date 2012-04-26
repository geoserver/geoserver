package org.geoserver.bxml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.springframework.util.Assert;

/**
 * Choice the appropriate decoder from options for a given element to decode.
 * 
 * @param <T>
 *            the generic type
 * 
 * @author cfarina
 */
public class ChoiceDecoder<T> implements Decoder<T> {

    /** The options. */
    private final List<Decoder<T>> options;

    /**
     * Instantiates a new choice decoder.
     */
    public ChoiceDecoder() {
        this.options = new ArrayList<Decoder<T>>();
    }

    /**
     * Instantiates a new choice decoder.
     * 
     * @param options
     *            the options
     */
    public ChoiceDecoder(Decoder<T>... options) {
        this.options = new ArrayList<Decoder<T>>();
        if (options != null && options.length > 0) {
            Assert.noNullElements(options);
            this.options.addAll(Arrays.asList(options));
        }
    }

    /**
     * Adds the option.
     * 
     * @param option
     *            the option
     */
    public void addOption(Decoder<T> option) {
        this.options.add(option);
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
    public T decode(final BxmlStreamReader r) throws Exception {
        r.require(EventType.START_ELEMENT, null, null);
        final QName name = r.getElementName();
        for (Decoder<T> decoder : options) {
            if (decoder.canHandle(name)) {
                return decoder.decode(r);
            }
        }
        throw new IllegalArgumentException("No decoder found for " + name);
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
        for (Decoder<T> decoder : options) {
            if (decoder.canHandle(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the targets.
     * 
     * @return the targets
     */
    @Override
    public Set<QName> getTargets() {
        Set<QName> targets = new HashSet<QName>();
        for (Decoder<T> decoder : options) {
            targets.addAll(decoder.getTargets());
        }
        return targets;
    }
}
