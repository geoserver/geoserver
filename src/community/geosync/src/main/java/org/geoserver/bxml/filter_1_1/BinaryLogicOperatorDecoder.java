package org.geoserver.bxml.filter_1_1;

import static org.geotools.filter.v1_1.OGC.And;
import static org.geotools.filter.v1_1.OGC.Or;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.Decoder;
import org.geoserver.bxml.SequenceDecoder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.springframework.util.Assert;

import com.google.common.collect.Iterators;

/**
 * The Class BinaryLogicOperatorDecoder.
 * 
 * @author cfarina
 */
public class BinaryLogicOperatorDecoder implements Decoder<Filter> {

    /** The ff. */
    protected static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools
            .getDefaultHints());

    /** The Constant names. */
    private static final Set<QName> names;
    static {
        Set<QName> n = new HashSet<QName>();
        n.add(And);
        n.add(Or);
        names = Collections.unmodifiableSet(n);
    }

    /**
     * Instantiates a new binary logic operator decoder.
     */
    public BinaryLogicOperatorDecoder() {
    }

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the filter
     * @throws Exception
     *             the exception
     */
    @Override
    public Filter decode(final BxmlStreamReader r) throws Exception {
        r.require(EventType.START_ELEMENT, null, null);
        final QName elementName = r.getElementName();
        Assert.isTrue(canHandle(elementName));
        r.nextTag();

        SequenceDecoder<Filter> seq = new SequenceDecoder<Filter>(2, Integer.MAX_VALUE);
        seq.add(new AnyFilterDecoder(), 1, 1);

        Filter[] filters = Iterators.toArray(seq.decode(r), Filter.class);
        // r.nextTag();
        r.require(EventType.END_ELEMENT, elementName.getNamespaceURI(), elementName.getLocalPart());

        Filter f;
        if (And.equals(elementName)) {
            f = ff.and(Arrays.asList(filters));
        } else {
            f = ff.or(Arrays.asList(filters));
        }
        return f;
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
        return names.contains(name);
    }

    /**
     * Gets the targets.
     * 
     * @return the targets
     */
    @Override
    public Set<QName> getTargets() {
        return names;
    }

}
