package org.geoserver.bxml.gml_3_1;

import static org.geotools.gml3.GML.Envelope;

import java.util.Iterator;

import javax.xml.namespace.QName;

import org.geoserver.bxml.ChoiceDecoder;
import org.geoserver.bxml.SequenceDecoder;
import org.geoserver.bxml.SetterDecoder;
import org.geoserver.bxml.base.PrimitiveListDecoder;
import org.geoserver.bxml.filter_1_1.AbstractTypeDecoder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml3.GML;
import org.gvsig.bxml.stream.BxmlStreamReader;

import com.google.common.collect.Iterators;
import com.vividsolutions.jts.geom.Envelope;

/**
 * The Class EnvelopeDecoder.
 * 
 * @author cfarina
 */
public class EnvelopeDecoder extends AbstractTypeDecoder<ReferencedEnvelope> {

    /** The Constant lowerCorner. */
    private static final QName lowerCorner = new QName(GML.NAMESPACE, "lowerCorner");

    /** The Constant upperCorner. */
    private static final QName upperCorner = new QName(GML.NAMESPACE, "upperCorner");

    /**
     * Instantiates a new envelope decoder.
     */
    public EnvelopeDecoder() {
        super(Envelope);
    }

    /**
     * Decode internal.
     * 
     * @param r
     *            the r
     * @param name
     *            the name
     * @return the referenced envelope
     * @throws Exception
     *             the exception
     */
    @Override
    protected ReferencedEnvelope decodeInternal(BxmlStreamReader r, QName name) throws Exception {
        ChoiceDecoder<Object> choice = new ChoiceDecoder<Object>();

        EnvelopeParams params = new EnvelopeParams();
        choice.addOption(new SetterDecoder<Object>(new PrimitiveListDecoder<double[]>(lowerCorner,
                double[].class), params, "lowerCornerValues"));
        choice.addOption(new SetterDecoder<Object>(new PrimitiveListDecoder<double[]>(upperCorner,
                double[].class), params, "uperCornerValues"));

        SequenceDecoder<Object> seq = new SequenceDecoder<Object>(1, 1);
        seq.add(choice, 0, Integer.MAX_VALUE);

        r.nextTag();
        final Iterator<Object> iterator = seq.decode(r);
        Iterators.toArray(iterator, Object.class);

        Envelope envelope = new Envelope(params.getLowerCornerValues()[0],
                params.getUperCornerValues()[0], params.getLowerCornerValues()[1],
                params.getUperCornerValues()[1]);
        // TODO: set crs
        ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(envelope, null);

        return referencedEnvelope;
    }

    /**
     * The Class EnvelopeParams.
     */
    public class EnvelopeParams {

        /** The lower corner values. */
        private double[] lowerCornerValues;

        /** The uper corner values. */
        private double[] uperCornerValues;

        /**
         * Gets the uper corner values.
         * 
         * @return the uper corner values
         */
        public double[] getUperCornerValues() {
            return uperCornerValues;
        }

        /**
         * Sets the uper corner values.
         * 
         * @param uperCornerValues
         *            the new uper corner values
         */
        public void setUperCornerValues(double[] uperCornerValues) {
            this.uperCornerValues = uperCornerValues;
        }

        /**
         * Gets the lower corner values.
         * 
         * @return the lower corner values
         */
        public double[] getLowerCornerValues() {
            return lowerCornerValues;
        }

        /**
         * Sets the lower corner values.
         * 
         * @param lowerCornerValues
         *            the new lower corner values
         */
        public void setLowerCornerValues(double[] lowerCornerValues) {
            this.lowerCornerValues = lowerCornerValues;
        }
    }

}
