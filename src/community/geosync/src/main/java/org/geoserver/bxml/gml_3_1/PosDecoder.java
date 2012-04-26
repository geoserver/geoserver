package org.geoserver.bxml.gml_3_1;

import static org.geotools.gml3.GML.pos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.bxml.Decoder;
import org.geoserver.bxml.SequenceDecoder;
import org.geoserver.bxml.base.PrimitiveListDecoder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;

/**
 * The Class PosDecoder.
 * 
 * @author cfarina
 */
public class PosDecoder implements Decoder<CoordinateSequence> {

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

        r.require(EventType.START_ELEMENT, pos.getNamespaceURI(), pos.getLocalPart());

        final String dimensionAtt = r.getAttributeValue(null, "dimension");

        SequenceDecoder seq = new SequenceDecoder<Double[]>(1, Integer.MAX_VALUE);
        seq.add(new PrimitiveListDecoder<double[]>(pos, double[].class), 1, Integer.MAX_VALUE);

        final Iterator<double[]> iterator = seq.decode(r);
        List<Double> coords = new ArrayList<Double>();
        int dimension = 2;
        while (iterator.hasNext()) {
            double[] coord = iterator.next();
            dimension = coord.length;
            for (int i = 0; i < coord.length; i++) {
                coords.add(coord[i]);

            }
        }

        double[] arrayCoord = new double[coords.size()];
        for (int i = 0; i < arrayCoord.length; i++) {
            arrayCoord[i] = coords.get(i);
        }

        dimension = dimensionAtt == null ? dimension : Integer.parseInt(dimensionAtt);
        return new PackedCoordinateSequence.Double(arrayCoord, dimension);
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
        return pos.equals(name);
    }

    /**
     * Gets the targets.
     * 
     * @return the targets
     */
    @Override
    public Set<QName> getTargets() {
        return Collections.singleton(pos);
    }

}
