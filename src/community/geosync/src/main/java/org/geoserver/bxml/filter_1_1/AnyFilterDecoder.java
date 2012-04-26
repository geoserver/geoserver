package org.geoserver.bxml.filter_1_1;

import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.ChoiceDecoder;
import org.geoserver.bxml.Decoder;
import org.geoserver.bxml.filter_1_1.spatial.BBOXFilterDecoder;
import org.geoserver.bxml.filter_1_1.spatial.BinarySpatialOperationDecoder;
import org.geoserver.bxml.filter_1_1.spatial.DistanceBufferFilterDecoder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.opengis.filter.Filter;

/**
 * The Class AnyFilterDecoder.
 * 
 * @author cfarina
 */
public class AnyFilterDecoder implements Decoder<Filter> {

    /** The choice. */
    private Decoder<Filter> choice;

    /**
     * Instantiates a new any filter decoder.
     */
    @SuppressWarnings("unchecked")
    public AnyFilterDecoder() {

        ChoiceDecoder<Filter> predicates = new ChoiceDecoder<Filter>(
                new BinaryComparisonOperatorDecoder(), //
                new PropertyIsBetweenFilterDecoder(), new PropertyIsLikeFilterDecoder(),
                new PropertyIsNullFilterDecoder(), new BinaryLogicOperatorDecoder(),
                new NotDecoder(), new DistanceBufferFilterDecoder(),
                new BinarySpatialOperationDecoder(), new BBOXFilterDecoder());

        this.choice = new ChoiceDecoder<Filter>(new IdDecoder(), predicates);
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

        Filter filter = choice.decode(r);

        r.require(EventType.END_ELEMENT, null, null);
        return filter;
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
        return choice.canHandle(name);
    }

    /**
     * Gets the targets.
     * 
     * @return the targets
     */
    @Override
    public Set<QName> getTargets() {
        return choice.getTargets();
    }
}
