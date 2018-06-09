/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.utils;

import java.util.Collections;
import java.util.Map;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpMap;

/** Options used for computing geometry centroids by {@link KmlCentroidBuilder}. */
public class KmlCentroidOptions {

    public static final String PREFIX = "kmcentroid";
    public static final String CONTAIN = PREFIX + "_contain";
    public static final String SAMPLE = PREFIX + "_sample";
    public static final String CLIP = PREFIX + "_clip";

    public static final KmlCentroidOptions DEFAULT = new KmlCentroidOptions(new KvpMap());

    static final int DEFAULT_SAMPLES = 5;

    /** Creates centroid options from the specified encoding context. */
    public static KmlCentroidOptions create(KmlEncodingContext context) {
        return create(
                context != null && context.getRequest() != null
                        ? context.getRequest().getFormatOptions()
                        : Collections.EMPTY_MAP);
    }

    /** Creates centroid options from the specified format options. */
    public static KmlCentroidOptions create(Map formatOptions) {
        if (formatOptions != null) {
            for (Object key : formatOptions.keySet()) {
                if (key.toString().toLowerCase().startsWith(PREFIX)) {
                    return new KmlCentroidOptions(CaseInsensitiveMap.wrap(formatOptions));
                }
            }
        }
        return KmlCentroidOptions.DEFAULT;
    }

    Map raw;

    public KmlCentroidOptions(Map raw) {
        this.raw = raw;
    }

    /**
     * Determines if the "contain" option is set.
     *
     * <p>This option causes the centroid builder to find a point (via sampling if necessary) that
     * is contained within a polygon geometry.
     *
     * @see #getSamples()
     */
    public boolean isContain() {
        return Boolean.valueOf(raw.getOrDefault(CONTAIN, "false").toString());
    }

    /**
     * Determines if the "clip" option is set.
     *
     * <p>This option causes the centroid builder to clip geometries by the request bounding box
     * before computing the centroid.
     */
    public boolean isClip() {
        return Boolean.valueOf(raw.getOrDefault(CLIP, "false").toString());
    }

    /**
     * The number of samples to try when computing a centroid when {@link #isContain()} is set.
     *
     * <p>When unset this falls back to
     */
    public int getSamples() {
        try {
            return Integer.parseInt(
                    raw.getOrDefault(SAMPLE, String.valueOf(DEFAULT_SAMPLES)).toString());
        } catch (NumberFormatException e) {
            return DEFAULT_SAMPLES;
        }
    }
}
