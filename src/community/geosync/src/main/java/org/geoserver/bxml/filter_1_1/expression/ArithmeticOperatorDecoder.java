package org.geoserver.bxml.filter_1_1.expression;

import static org.geotools.filter.v1_1.OGC.Add;
import static org.geotools.filter.v1_1.OGC.Div;
import static org.geotools.filter.v1_1.OGC.Mul;
import static org.geotools.filter.v1_1.OGC.Sub;

import javax.xml.namespace.QName;

import org.geoserver.bxml.SequenceDecoder;
import org.geoserver.bxml.filter_1_1.AbstractTypeDecoder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

import com.google.common.collect.Iterators;

/**
 * The Class ArithmeticOperatorDecoder.
 * 
 * @author cfarina
 */
public class ArithmeticOperatorDecoder extends AbstractTypeDecoder<Expression> {

    /** The ff. */
    protected static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools
            .getDefaultHints());

    /**
     * Instantiates a new arithmetic operator decoder.
     */
    public ArithmeticOperatorDecoder() {
        super(Add, Sub, Mul, Div);
    }

    /**
     * Decode internal.
     * 
     * @param r
     *            the r
     * @param name
     *            the name
     * @return the expression
     * @throws Exception
     *             the exception
     */
    @Override
    protected Expression decodeInternal(BxmlStreamReader r, QName name) throws Exception {

        r.nextTag();

        final SequenceDecoder<Expression> seq;
        seq = new SequenceDecoder<Expression>();
        seq.add(new ExpressionDecoder(), 2, 2);

        Expression[] expressions = Iterators.toArray(seq.decode(r), Expression.class);

        if (Add.equals(name)) {
            return ff.add(expressions[0], expressions[1]);
        }

        if (Sub.equals(name)) {
            return ff.subtract(expressions[0], expressions[1]);
        }

        if (Mul.equals(name)) {
            return ff.multiply(expressions[0], expressions[1]);
        }

        if (Div.equals(name)) {
            return ff.divide(expressions[0], expressions[1]);
        }

        throw new IllegalArgumentException(this.getClass().getName() + " can not decode " + name);
    }
}
