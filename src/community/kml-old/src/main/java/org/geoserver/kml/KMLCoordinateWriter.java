/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.Locale;

import org.geotools.gml.producer.CoordinateWriter;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.vividsolutions.jts.geom.CoordinateSequence;

/**
 * Essentially the same as the GML CoordinateWriter, but avoids adding
 * attributes to coodinates tag and other stuff the KML code does not
 * invoke.
 * 
 * See 
 * 
 * @author Arne Kepp - OpenGeo
 * @deprecated This class is a copy of CoordinateWriter in geotools, we should 
 * reuse that version and create any options we might need to customize.
 */
public class KMLCoordinateWriter extends CoordinateWriter {
    private AttributesImpl atts;

    private static final double DECIMAL_MIN = Math.pow(10, -3);

    private static final double DECIMAL_MAX = Math.pow(10, 7);

    private static final String coordinateDelimiter = ",";

    private static final String tupleDelimiter = " ";

    private final StringBuffer coordBuff = new StringBuffer();

    private char[] buff = new char[200];

    private final double scale;

    private final NumberFormat coordFormatter = NumberFormat
            .getInstance(Locale.US);

    private final FieldPosition zero = new FieldPosition(0);

    /** Dummy Z value (used to override coordinate.Z value) */
    // private final double dummyZ;
    /** Dimension of expected coordinates */
    private final int D;

    public KMLCoordinateWriter(int numDecimals, boolean isDummyZEnabled) {
        super(numDecimals, isDummyZEnabled);
        atts = new org.xml.sax.helpers.AttributesImpl();
        scale = Math.pow(10, numDecimals);
        D = 3;
        // dummyZ = 0;
    }

    public void writeCoordinates(CoordinateSequence c, ContentHandler output)
            throws SAXException {

        output.startElement("", "coordinates", "coordinates", atts);

        final int coordCount = c.size();
        // used to check whether the coordseq handles a third dimension or not
        final int coordSeqDimension = c.getDimension();
        double x, y, z;
        // write down a coordinate at a time
        for (int i = 0, n = coordCount; i < n; i++) {
            x = c.getOrdinate(i, 0);
            y = c.getOrdinate(i, 1);

            // clear the buffer
            coordBuff.setLength(0);

            // format x into buffer and append delimiter
            formatDecimal(x, coordBuff);
            coordBuff.append(coordinateDelimiter);
            // format y into buffer
            formatDecimal(y, coordBuff);

            if (coordSeqDimension > 2 && !Double.isNaN(c.getOrdinate(i, 2))){
                coordBuff.append(coordinateDelimiter);
                formatDecimal(c.getOrdinate(i, 2), coordBuff);
            }

            // if there is another coordinate, tack on a tuple delimiter
            if (i + 1 < coordCount) {
                coordBuff.append(tupleDelimiter);
            }

            // make sure our character buffer is big enough
            if (coordBuff.length() > buff.length) {
                buff = new char[coordBuff.length()];
            }

            // copy the characters
            coordBuff.getChars(0, coordBuff.length(), buff, 0);

            // finally, output
            output.characters(buff, 0, coordBuff.length());
        }
        output.endElement(null, "coordinates", "coordinates");
    }

    private void formatDecimal(double x, StringBuffer sb) {
        if (Math.abs(x) >= DECIMAL_MIN && x < DECIMAL_MAX) {
            x = Math.floor(x * scale + 0.5) / scale;
            long lx = (long) x;
            if (lx == x)
                sb.append(lx);
            else
                sb.append(x);
        } else {
            coordFormatter.format(x, coordBuff, zero);
        }
    }
}
