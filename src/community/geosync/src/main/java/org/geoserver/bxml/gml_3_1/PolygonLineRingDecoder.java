package org.geoserver.bxml.gml_3_1;

import javax.xml.namespace.QName;

import org.gvsig.bxml.stream.BxmlStreamReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * The Class PolygonLineRingDecoder.
 * 
 * @author cfarina
 */
public class PolygonLineRingDecoder extends AbstractGeometryDecoder<Geometry> {

    /**
     * Instantiates a new polygon line ring decoder.
     * 
     * @param coordinateReferenceSystem
     *            the coordinate reference system
     * @param dimension
     *            the dimension
     * @param elemName
     *            the elem name
     */
    public PolygonLineRingDecoder(CoordinateReferenceSystem coordinateReferenceSystem,
            int dimension, QName elemName) {
        super(coordinateReferenceSystem, dimension, elemName);
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
    public Geometry decodeInternal(BxmlStreamReader r, QName name) throws Exception {
        r.nextTag();
        return new LinearRingDecoder().decode(r);
    }

}
