package org.geoserver.bxml.filter_1_1;

import static org.geotools.filter.v1_1.OGC.PropertyIsLike;

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.geoserver.bxml.SequenceDecoder;
import org.geoserver.bxml.base.SimpleDecoder;
import org.geoserver.bxml.filter_1_1.expression.ExpressionDecoder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.filter.LiteralExpressionImpl;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.springframework.util.Assert;

import com.google.common.collect.Iterators;

/**
 * The Class PropertyIsLikeFilterDecoder.
 * 
 * @author cfarina
 */
public class PropertyIsLikeFilterDecoder extends SimpleDecoder<Filter> {

    /** The seq. */
    private SequenceDecoder<Expression> seq;

    /** The ff. */
    protected static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools
            .getDefaultHints());

    /**
     * Instantiates a new property is like filter decoder.
     */
    public PropertyIsLikeFilterDecoder() {
        super(PropertyIsLike);
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
    public Filter decode(BxmlStreamReader r) throws Exception {
        r.require(EventType.START_ELEMENT, null, null);
        final QName name = r.getElementName();
        Assert.isTrue(canHandle(name));

        r.nextTag();

        final Iterator<Expression> exprIterator = seq.decode(r);
        final Expression[] expressions = Iterators.toArray(exprIterator, Expression.class);

        Assert.isTrue(expressions.length == 2);

        r.require(EventType.END_ELEMENT, name.getNamespaceURI(), name.getLocalPart());

        Object value = ((LiteralExpressionImpl) expressions[1]).getValue();
        Assert.notNull(value, "Literal in IsLike filte can't be null");
        return ff.like(expressions[0], value.toString());
    }

}
