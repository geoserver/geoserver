package org.geoserver.bxml.gml_3_1;

import static org.geotools.gml3.GML.LinearRing;

import javax.xml.namespace.QName;

import org.geoserver.bxml.ChoiceDecoder;
import org.gvsig.bxml.stream.BxmlStreamReader;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;

/**
 * The Class LinearRingDecoder.
 * 
 * @author cfarina
 */
public class LinearRingDecoder extends AbstractGeometryDecoder<Geometry> {

    /** The choice. */
    private ChoiceDecoder<CoordinateSequence> choice;

    /**
     * Instantiates a new linear ring decoder.
     */
    public LinearRingDecoder() {
        super(LinearRing);
        choice = new ChoiceDecoder<CoordinateSequence>();
        choice.addOption(new PosDecoder());
        choice.addOption(new PosListDecoder());
        choice.addOption(new CoordinatesDecoder());
        choice.addOption(new CoordDecoder());
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
    public Geometry decodeInternal(final BxmlStreamReader r, final QName name) throws Exception {
        r.nextTag();

        CoordinateSequence coordinates = choice.decode(r);

        LinearRing linearRing = new GeometryFactory().createLinearRing(coordinates);
        linearRing.setUserData(getCrs());
        return linearRing;
    }

}
