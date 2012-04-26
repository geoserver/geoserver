package org.geoserver.bxml.gml_3_1;

import static org.geotools.gml3.GML.coord;

import javax.xml.namespace.QName;

import org.geoserver.bxml.SequenceDecoder;
import org.geoserver.bxml.base.PrimitiveDecoder;
import org.geotools.gml3.GML;
import org.gvsig.bxml.stream.BxmlStreamReader;

import com.google.common.collect.Iterators;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;

/**
 * The Class CoordDecoder.
 * 
 * @author cfarina
 */
public class CoordDecoder extends AbstractGeometryDecoder<CoordinateSequence> {

    /** The Constant X. */
    private static final QName X = new QName(GML.NAMESPACE, "X");

    /** The Constant Y. */
    private static final QName Y = new QName(GML.NAMESPACE, "Y");

    /** The Constant Z. */
    private static final QName Z = new QName(GML.NAMESPACE, "Z");

    /** The seq. */
    private SequenceDecoder<Double> seq;

    /**
     * Instantiates a new coord decoder.
     */
    public CoordDecoder() {
        super(coord);
        seq = new SequenceDecoder<Double>();
        seq.add(new PrimitiveDecoder<Double>(X, Double.class), 1, 1);
        seq.add(new PrimitiveDecoder<Double>(Y, Double.class), 1, 1);
        seq.add(new PrimitiveDecoder<Double>(Z, Double.class), 1, 1);
    }

    /**
     * Decode internal.
     * 
     * @param r
     *            the r
     * @param name
     *            the name
     * @return the coordinate sequence
     * @throws Exception
     *             the exception
     */
    @Override
    public CoordinateSequence decodeInternal(final BxmlStreamReader r, QName name) throws Exception {
        r.nextTag();
        Double[] doubles = Iterators.toArray(seq.decode(r), Double.class);

        final int dimension = doubles.length;
        double[] values = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            values[i] = doubles[i].doubleValue();
        }
        return new PackedCoordinateSequence.Double(values, dimension);
    }

}
