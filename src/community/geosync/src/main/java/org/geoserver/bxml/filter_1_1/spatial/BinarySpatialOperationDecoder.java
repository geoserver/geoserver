package org.geoserver.bxml.filter_1_1.spatial;

import static org.geotools.filter.v1_1.OGC.Contains;
import static org.geotools.filter.v1_1.OGC.Crosses;
import static org.geotools.filter.v1_1.OGC.Disjoint;
import static org.geotools.filter.v1_1.OGC.Equals;
import static org.geotools.filter.v1_1.OGC.Intersects;
import static org.geotools.filter.v1_1.OGC.Overlaps;
import static org.geotools.filter.v1_1.OGC.Touches;
import static org.geotools.filter.v1_1.OGC.Within;

import javax.xml.namespace.QName;

import org.geoserver.bxml.ChoiceDecoder;
import org.geoserver.bxml.filter_1_1.AbstractTypeDecoder;
import org.geoserver.bxml.filter_1_1.expression.ExpressionDecoder;
import org.geoserver.bxml.gml_3_1.EnvelopeDecoder;
import org.geoserver.bxml.gml_3_1.GeometryDecoder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

/**
 * The Class BinarySpatialOperationDecoder.
 * 
 * @author cfarina
 */
public class BinarySpatialOperationDecoder extends AbstractTypeDecoder<Filter> {

    /** The ff. */
    protected static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools
            .getDefaultHints());

    /** The choice. */
    @SuppressWarnings("rawtypes")
    private ChoiceDecoder choice;

    /**
     * Instantiates a new binary spatial operation decoder.
     */
    @SuppressWarnings("unchecked")
    public BinarySpatialOperationDecoder() {
        super(Equals, Disjoint, Touches, Within, Overlaps, Intersects, Crosses, Contains);
        choice = new ChoiceDecoder<Object>();
        choice.addOption(new GeometryDecoder());
        choice.addOption(new EnvelopeDecoder());
    }

    /**
     * Decode internal.
     * 
     * @param r
     *            the r
     * @param name
     *            the name
     * @return the filter
     * @throws Exception
     *             the exception
     */
    @Override
    protected Filter decodeInternal(final BxmlStreamReader r, final QName name) throws Exception {

        r.nextTag();
        Expression expression = new ExpressionDecoder().decode(r);
        r.nextTag();
        Object geometry = choice.decode(r);
        r.nextTag();

        Filter f;
        if (Equals.equals(name)) {
            f = ff.equal(expression, ff.literal(geometry));
        } else if (Disjoint.equals(name)) {
            f = ff.disjoint(expression, ff.literal(geometry));
        } else if (Touches.equals(name)) {
            f = ff.touches(expression, ff.literal(geometry));
        } else if (Within.equals(name)) {
            f = ff.within(expression, ff.literal(geometry));
        } else if (Overlaps.equals(name)) {
            f = ff.overlaps(expression, ff.literal(geometry));
        } else if (Crosses.equals(name)) {
            f = ff.crosses(expression, ff.literal(geometry));
        } else if (Intersects.equals(name)) {
            f = ff.intersects(expression, ff.literal(geometry));
        } else if (Contains.equals(name)) {
            f = ff.contains(expression, ff.literal(geometry));
        } else {
            throw new IllegalArgumentException(this.getClass().getName() + " can not decode "
                    + name);
        }
        return f;
    }

}
