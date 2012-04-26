package org.geoserver.bxml.filter_1_1;

import static org.geotools.filter.v1_1.OGC.PropertyIsNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.Decoder;
import org.geoserver.bxml.SequenceDecoder;
import org.geoserver.bxml.filter_1_1.expression.PropertyNameExpressionDecoder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

/**
 * The Class PropertyIsNullFilterDecoder.
 * 
 * @author cfarina
 */
public class PropertyIsNullFilterDecoder implements Decoder<Filter> {

    /** The seq. */
    private SequenceDecoder<Expression> seq;

    /** The ff. */
    protected static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools
            .getDefaultHints());

    /**
     * Instantiates a new property is null filter decoder.
     */
    public PropertyIsNullFilterDecoder() {
        seq = new SequenceDecoder<Expression>(1, 1);
        seq.add(new PropertyNameExpressionDecoder(), 1, 1);
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
        r.require(EventType.START_ELEMENT, PropertyIsNull.getNamespaceURI(),
                PropertyIsNull.getLocalPart());
        r.nextTag();
        Iterator<Expression> expressions = seq.decode(r);
        Expression e1 = expressions.next();

        r.nextTag();
        r.require(EventType.END_ELEMENT, PropertyIsNull.getNamespaceURI(),
                PropertyIsNull.getLocalPart());
        return ff.isNull(e1);
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
        return PropertyIsNull.equals(name);
    }

    /**
     * Gets the targets.
     * 
     * @return the targets
     */
    @Override
    public Set<QName> getTargets() {
        return Collections.singleton(PropertyIsNull);
    }

}
