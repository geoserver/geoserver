/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import it.geosolutions.jaiext.range.Range;
import org.geotools.util.Converter;

/**
 * Parses a JAIExt range from a string defining it
 *
 * @author Andrea Aime - GeoSolutions
 * @author Emanuele Tajarol - GeoSolutions
 */
public class JAIExtRangePPIO extends LiteralPPIO {

    static Converter CONVERTER = new JAIExtRangeConverterFactory().createConverter(String.class, Range.class, null);

    /** Parses a single range from a string */
    public static Range parseRange(String sRange) {
        try {
            Range result = CONVERTER.convert(sRange, Range.class);
            if (result == null) {
                throw new IllegalArgumentException("Bad range definition '" + sRange + "'");
            }

            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Bad range definition '" + sRange + "'", e);
        }
    }

    public JAIExtRangePPIO() {
        super(Range.class);
    }

    @Override
    public PPIODirection getDirection() {
        return PPIODirection.DECODING;
    }

    /** Decodes the parameter (as a string) to its internal object implementation. */
    @Override
    public Object decode(String value) throws Exception {
        return parseRange(value);
    }

    /** Encodes the internal object representation of a parameter as a string. */
    @Override
    public String encode(Object value) throws Exception {
        throw new UnsupportedOperationException("JAIExt range encoding not supported out of the box");
    }
}
