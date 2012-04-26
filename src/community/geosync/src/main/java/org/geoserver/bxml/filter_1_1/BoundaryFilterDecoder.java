package org.geoserver.bxml.filter_1_1;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.Decoder;
import org.geoserver.bxml.filter_1_1.expression.ExpressionDecoder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.filter.v1_1.OGC;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.springframework.util.Assert;

/**
 * The Class BoundaryFilterDecoder.
 * 
 * @author cfarina
 */
public class BoundaryFilterDecoder implements Decoder<Expression> {

    /** The Constant LowerBoundary. */
    public static final QName LowerBoundary = new QName(OGC.NAMESPACE, "LowerBoundary");

    /** The Constant UpperBoundary. */
    public static final QName UpperBoundary = new QName(OGC.NAMESPACE, "UpperBoundary");

    /** The ff. */
    protected static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools
            .getDefaultHints());

    /** The Constant names. */
    private static final Set<QName> names;
    static {
        Set<QName> n = new HashSet<QName>();
        n.add(LowerBoundary);
        n.add(UpperBoundary);
        names = Collections.unmodifiableSet(n);
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
    public Expression decode(BxmlStreamReader r) throws Exception {
        r.require(EventType.START_ELEMENT, null, null);
        final QName name = r.getElementName();
        Assert.isTrue(canHandle(name));

        r.nextTag();
        Expression expression = new ExpressionDecoder().decode(r);
        r.nextTag();

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
