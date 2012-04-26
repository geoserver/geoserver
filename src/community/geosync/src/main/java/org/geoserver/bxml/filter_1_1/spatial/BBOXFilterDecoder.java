package org.geoserver.bxml.filter_1_1.spatial;

import static org.geotools.filter.v1_1.OGC.BBOX;

import java.util.logging.Level;

import javax.xml.namespace.QName;

import org.geoserver.bxml.filter_1_1.AbstractTypeDecoder;
import org.geoserver.bxml.filter_1_1.expression.ExpressionDecoder;
import org.geoserver.bxml.gml_3_1.EnvelopeDecoder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;

/**
 * The Class BBOXFilterDecoder.
 * 
 * @author cfarina
 */
public class BBOXFilterDecoder extends AbstractTypeDecoder<Filter> {

    /** The ff. */
    protected static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools
            .getDefaultHints());

    /**
     * Instantiates a new bBOX filter decoder.
     */
    public BBOXFilterDecoder() {
        super(BBOX);
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
    protected Filter decodeInternal(BxmlStreamReader r, QName name) throws Exception {
        r.nextTag();
        AttributeExpressionImpl expression = (AttributeExpressionImpl) new ExpressionDecoder()
                .decode(r);
        String propertyName = expression.getPropertyName();
        r.nextTag();
        ReferencedEnvelope envelope = (ReferencedEnvelope) new EnvelopeDecoder().decode(r);
        r.nextTag();

        String epsCode = null;
        if (envelope.crs() != null) {
            try {
                epsCode = CRS.lookupEpsgCode(envelope.crs(), true).toString();
            } catch (FactoryException e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
        }
        return ff.bbox(propertyName, envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(),
                envelope.getMaxY(), epsCode);
    }

}
