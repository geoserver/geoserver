/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.util.converters;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.Converter;
import org.locationtech.jts.geom.Envelope;

public class StringBBoxConverter implements Converter {

    private static String SEPARATOR = ",";

    @SuppressWarnings("unchecked")
    public <T> T convert(Object source, Class<T> target) throws Exception {
        if (source instanceof ReferencedEnvelope) {
            if (String.class.isAssignableFrom(target)) {
                try {
                    ReferencedEnvelope envelope = (ReferencedEnvelope) source;

                    StringBuilder str = new StringBuilder();
                    str.append(envelope.getMinimum(0)).append(SEPARATOR);
                    str.append(envelope.getMaximum(0)).append(SEPARATOR);
                    str.append(envelope.getMinimum(1)).append(SEPARATOR);
                    str.append(envelope.getMaximum(1)).append(SEPARATOR);
                    str.append(CRS.lookupIdentifier(envelope.getCoordinateReferenceSystem(), true));

                    return (T) str.toString();
                } catch (Exception e) {
                    return null;
                }
            }
        } else {
            if (ReferencedEnvelope.class.isAssignableFrom(target)) {
                String text = (String) source;
                String[] parsed = text.split("\\s*" + SEPARATOR + "\\s*");
                try {
                    return (T)
                            (new ReferencedEnvelope(
                                    new Envelope(
                                            Double.valueOf(parsed[0]),
                                            Double.valueOf(parsed[1]),
                                            Double.valueOf(parsed[2]),
                                            Double.valueOf(parsed[3])),
                                    CRS.decode(parsed[4])));
                } catch (Exception e) {
                    return null;
                }
            }
        }

        throw new IllegalArgumentException(
                "String List converter expects to convert ReferencedEnvelope <-> string only. ("
                        + source
                        + "), ("
                        + target
                        + ")");
    }
}
