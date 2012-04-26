package org.geoserver.bxml.gml_3_1;

import javax.xml.namespace.QName;

import org.geoserver.bxml.Decoder;
import org.gvsig.bxml.stream.BxmlStreamReader;

import com.vividsolutions.jts.geom.Geometry;

/**
 * The Class GeometryMemberDecoder.
 * 
 * @author cfarina
 */
public class GeometryMemberDecoder extends AbstractGeometryDecoder<Geometry> {

    /** The member decoder. */
    private final Decoder<Geometry> memberDecoder;

    /**
     * Instantiates a new geometry member decoder.
     * 
     * @param name
     *            the name
     * @param memberDecoder
     *            the member decoder
     */
    public GeometryMemberDecoder(QName name, Decoder<Geometry> memberDecoder) {
        super(name);
        this.memberDecoder = memberDecoder;
    }

    /**
     * Decode internal.
     * 
     * @param r
     *            the r
     * @param name
     *            the name
     * @return the geometry
     * @throws Exception
     *             the exception
     */
    @Override
    protected Geometry decodeInternal(BxmlStreamReader r, QName name) throws Exception {
        r.nextTag();
        Geometry geometry = memberDecoder.decode(r);
        r.nextTag();
        return geometry;
    }

}
