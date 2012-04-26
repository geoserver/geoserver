package org.geoserver.bxml.filter_1_1.expression;

import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.ChoiceDecoder;
import org.geoserver.bxml.Decoder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.opengis.filter.expression.Expression;
import org.springframework.util.Assert;

/**
 * The Class ExpressionDecoder.
 * 
 * @author cfarina
 */
public class ExpressionDecoder implements Decoder<Expression> {

    /** The chain. */
    private Decoder<Expression> chain;

    /**
     * Instantiates a new expression decoder.
     */
    @SuppressWarnings("unchecked")
    public ExpressionDecoder() {
        this.chain = new ChoiceDecoder<Expression>(new ArithmeticOperatorDecoder(),
                new FunctionExpressionDecoder(), new LiteralExpressionDecoder(),
                new PropertyNameExpressionDecoder());
    }

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the expression
     * @throws Exception
     *             the exception
     */
    @Override
    public Expression decode(final BxmlStreamReader r) throws Exception {
        r.require(EventType.START_ELEMENT, null, null);
        QName name = r.getElementName();
        Assert.isTrue(canHandle(name));

        Expression expression = chain.decode(r);
        r.require(EventType.END_ELEMENT, name.getNamespaceURI(), name.getLocalPart());
        return expression;
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
        return chain.canHandle(name);
    }

    /**
     * Gets the targets.
     * 
     * @return the targets
     */
    @Override
    public Set<QName> getTargets() {
        return chain.getTargets();
    }
}
