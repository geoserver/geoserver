/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import javax.media.jai.Interpolation;
import net.opengis.wcs20.InterpolationMethodType;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.exception.WCS20Exception.WCS20ExceptionCode;
import org.geotools.util.Utilities;

/** Enum that represents the possible interpolation values. */
enum InterpolationPolicy {
    linear("http://www.opengis.net/def/interpolation/OGC/1/linear") {
        @Override
        public Interpolation getInterpolation() {
            return Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
        }
    },
    nearestneighbor("http://www.opengis.net/def/interpolation/OGC/1/nearest-neighbor") {
        @Override
        public Interpolation getInterpolation() {
            return Interpolation.getInstance(Interpolation.INTERP_NEAREST);
        }
    },
    quadratic("http://www.opengis.net/def/interpolation/OGC/1/quadratic") {
        @Override
        public Interpolation getInterpolation() {
            throw new WCS20Exception(
                    "Interpolation not supported",
                    WCS20Exception.WCS20ExceptionCode.InterpolationMethodNotSupported,
                    quadratic.toString());
        }
    },
    cubic("http://www.opengis.net/def/interpolation/OGC/1/cubic") {
        @Override
        public Interpolation getInterpolation() {
            return Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2);
        }
    },
    lostarea("http://www.opengis.net/def/interpolation/OGC/1/lost-area") {
        @Override
        public Interpolation getInterpolation() {
            throw new WCS20Exception(
                    "Interpolation not supported",
                    WCS20Exception.WCS20ExceptionCode.InterpolationMethodNotSupported,
                    lostarea.toString());
        }
    },
    barycentric("http://www.opengis.net/def/interpolation/OGC/1/barycentric") {
        @Override
        public Interpolation getInterpolation() {
            throw new WCS20Exception(
                    "Interpolation not supported",
                    WCS20Exception.WCS20ExceptionCode.InterpolationMethodNotSupported,
                    barycentric.toString());
        }
    };

    private InterpolationPolicy(String representation) {
        this.strVal = representation;
    }

    private final String strVal;

    public abstract Interpolation getInterpolation();

    static InterpolationPolicy getPolicy(InterpolationMethodType interpolationMethodType) {
        Utilities.ensureNonNull("interpolationMethodType", interpolationMethodType);
        final String interpolationMethod = interpolationMethodType.getInterpolationMethod();
        return getPolicy(interpolationMethod);
    }

    static InterpolationPolicy getPolicy(String interpolationMethod) {
        Utilities.ensureNonNull("interpolationMethod", interpolationMethod);
        final InterpolationPolicy[] values = InterpolationPolicy.values();
        for (InterpolationPolicy policy : values) {
            if (policy.strVal.equals(interpolationMethod)) {
                return policy;
            }
        }

        // method not found
        throw new WCS20Exception(
                "Interpolation method not supported",
                WCS20ExceptionCode.InterpolationMethodNotSupported,
                interpolationMethod);
    }
    /**
     * Default interpolation policy for this implementation.
     *
     * @return an instance of {@link InterpolationPolicy} which is actually the default one.
     */
    static InterpolationPolicy getDefaultPolicy() {
        return nearestneighbor;
    }
}
