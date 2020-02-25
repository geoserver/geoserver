/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import org.geotools.util.Converter;
import org.jaitools.JAITools;
import org.jaitools.numeric.Range;

/**
 * Parses a {@link JAITools} range from a string defining it
 *
 * @author Andrea Aime - GeoSolutions
 * @author Emanuele Tajarol - GeoSolutions
 */
public class JAIToolsRangePPIO extends LiteralPPIO {

    static Converter CONVERTER =
            new JAIToolsRangeConverterFactory().createConverter(String.class, Range.class, null);

    /** Parses a single range from a string */
    public static Range<Double> parseRange(String sRange) {
        try {
            Range<Double> result = CONVERTER.convert(sRange, Range.class);
            if (result == null) {
                throw new IllegalArgumentException("Bad range definition '" + sRange + "'");
            }

            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Bad range definition '" + sRange + "'", e);
        }
    }

    public JAIToolsRangePPIO() {
        super(Range.class);
    }

    /** Decodes the parameter (as a string) to its internal object implementation. */
    public Object decode(String value) throws Exception {
        return parseRange(value);
    }

    /** Encodes the internal object representation of a parameter as a string. */
    public String encode(Object value) throws Exception {
        throw new UnsupportedOperationException("JaiTools range not supported out of the box");
    }
}
