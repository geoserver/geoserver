package org.geoserver.bxml.gml_3_1;

import static org.geotools.gml3.GML.posList;

import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.Decoder;
import org.geoserver.bxml.base.PrimitiveListDecoder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;

/**
 * The Class PosListDecoder.
 * 
 * @author cfarina
 */
public class PosListDecoder implements Decoder<CoordinateSequence> {

    /**
     * Decode.
     * 
     * @param r
     *            the r
     * @return the coordinate sequence
     * @throws Exception
     *             the exception
     */
    @Override
    public CoordinateSequence decode(BxmlStreamReader r) throws Exception {
        r.require(EventType.START_ELEMENT, posList.getNamespaceURI(), posList.getLocalPart());

        final String dimensionAtt = r.getAttributeValue(null, "dimension");

        final double[] coords = new PrimitiveListDecoder<double[]>(posList, double[].class)
                .decode(r);

        int dimension = dimensionAtt == null ? 2 : Integer.parseInt(dimensionAtt);
        r.require(EventType.END_ELEMENT, null, null);
        return new PackedCoordinateSequence.Double(coords, dimension);
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
        return posList.equals(name);
    }

    /**
     * Gets the targets.
     * 
     * @return the targets
     */
    @Override
    public Set<QName> getTargets() {
        return Collections.singleton(posList);
    }
}
