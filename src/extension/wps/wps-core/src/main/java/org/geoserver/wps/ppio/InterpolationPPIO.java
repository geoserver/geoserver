/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import javax.media.jai.Interpolation;

/**
 * Parses interpolation values
 *
 * @author Andrea Aime - GeoSolutions
 */
public class InterpolationPPIO extends LiteralPPIO {

    protected InterpolationPPIO() {
        super(Interpolation.class);
    }

    @Override
    public Object decode(String value) throws Exception {
        if (value.equalsIgnoreCase("NEAREST")) {
            return Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        } else if (value.equalsIgnoreCase("BILINEAR")) {
            return Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        } else if (value.equalsIgnoreCase("BICUBIC2")) {
            return Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2);
        } else if (value.equalsIgnoreCase("BICUBIC")) {
            return Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
        }

        throw new IllegalArgumentException(
                "Unrecognized interpolation type, valid values are NEAREST, BILINEAR, BICUBIC2, BICUBIC");
    }

    @Override
    public String encode(Object value) throws Exception {
        throw new UnsupportedOperationException("Cannot encode interpolations right now");
    }
}
