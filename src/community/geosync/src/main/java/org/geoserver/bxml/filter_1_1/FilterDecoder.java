package org.geoserver.bxml.filter_1_1;

import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.Decoder;
import org.geotools.filter.v1_1.OGC;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.opengis.filter.Filter;

/**
 * The Class FilterDecoder.
 * 
 * @author cfarina
 */
public class FilterDecoder implements Decoder<Filter> {

    /** The any filter. */
    private Decoder<Filter> anyFilter;

    /**
     * Instantiates a new filter decoder.
     */
    public FilterDecoder() {
        this.anyFilter = new AnyFilterDecoder();
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
        r.require(EventType.START_ELEMENT, OGC.Filter.getNamespaceURI(), OGC.Filter.getLocalPart());

        r.nextTag();

        Filter filter = anyFilter.decode(r);

        // When IdDecoder ends iteration, the parser pointer is pointing to Filter tag.
        // It isn't necessary to do r.nextTag
        if (!r.getElementName().equals(OGC.Filter)) {
            r.nextTag();
        }

        r.require(EventType.END_ELEMENT, OGC.Filter.getNamespaceURI(), OGC.Filter.getLocalPart());
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
        return OGC.Filter.equals(name);
    }

    /**
     * Gets the targets.
     * 
     * @return the targets
     */
    @Override
    public Set<QName> getTargets() {
        return Collections.singleton(OGC.Filter);
    }
}
