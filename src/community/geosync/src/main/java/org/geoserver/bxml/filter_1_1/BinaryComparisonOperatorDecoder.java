package org.geoserver.bxml.filter_1_1;

import static org.geotools.filter.v1_1.OGC.PropertyIsEqualTo;
import static org.geotools.filter.v1_1.OGC.PropertyIsGreaterThan;
import static org.geotools.filter.v1_1.OGC.PropertyIsGreaterThanOrEqualTo;
import static org.geotools.filter.v1_1.OGC.PropertyIsLessThan;
import static org.geotools.filter.v1_1.OGC.PropertyIsLessThanOrEqualTo;
import static org.geotools.filter.v1_1.OGC.PropertyIsNotEqualTo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.Decoder;
import org.geoserver.bxml.SequenceDecoder;
import org.geoserver.bxml.filter_1_1.expression.ExpressionDecoder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.springframework.util.Assert;

import com.google.common.collect.Iterators;

/**
 * The Class BinaryComparisonOperatorDecoder.
 * 
 * @author cfarina
 */
public class BinaryComparisonOperatorDecoder implements Decoder<Filter> {

    /** The ff. */
    protected static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools
            .getDefaultHints());

    /** The Constant names. */
    private static final Set<QName> names;
    static {
        Set<QName> n = new HashSet<QName>();
        n.add(PropertyIsEqualTo);
        n.add(PropertyIsNotEqualTo);
        n.add(PropertyIsLessThan);
        n.add(PropertyIsGreaterThan);
        n.add(PropertyIsLessThanOrEqualTo);
        n.add(PropertyIsGreaterThanOrEqualTo);
        names = Collections.unmodifiableSet(n);
    }

    /** The seq. */
    private SequenceDecoder<Expression> seq;

    /**
     * Instantiates a new binary comparison operator decoder.
     */
    public BinaryComparisonOperatorDecoder() {
        seq = new SequenceDecoder<Expression>(2, 2);
        seq.add(new ExpressionDecoder(), 1, 1);
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
        final QName name = r.getElementName();
        Assert.isTrue(canHandle(name));

        final String matchCaseAtt = r.getAttributeValue(null, "matchCase");
        final boolean matchCase = matchCaseAtt == null ? true : Boolean.valueOf(matchCaseAtt);

        r.nextTag();

        final Iterator<Expression> exprIterator = seq.decode(r);
        final Expression[] expressions = Iterators.toArray(exprIterator, Expression.class);

        Assert.isTrue(expressions.length == 2);

        r.require(EventType.END_ELEMENT, name.getNamespaceURI(), name.getLocalPart());

        if (PropertyIsEqualTo.equals(name)) {
            return ff.equals(expressions[0], expressions[1]);
        }

        if (PropertyIsNotEqualTo.equals(name)) {
            return ff.notEqual(expressions[0], expressions[1]);
        }

        if (PropertyIsLessThan.equals(name)) {
            return ff.less(expressions[0], expressions[1], matchCase);
        }

        if (PropertyIsGreaterThan.equals(name)) {
            return ff.greater(expressions[0], expressions[1], matchCase);
        }

        if (PropertyIsGreaterThanOrEqualTo.equals(name)) {
            return ff.greaterOrEqual(expressions[0], expressions[1], matchCase);
        }

        if (PropertyIsLessThanOrEqualTo.equals(name)) {
            return ff.lessOrEqual(expressions[0], expressions[1], matchCase);
        }

        throw new IllegalArgumentException(this.getClass().getName() + " can not decode " + name);
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
