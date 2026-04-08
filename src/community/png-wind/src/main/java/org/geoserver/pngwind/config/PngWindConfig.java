/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pngwind.config;

/**
 * Configuration for PNG Wind format, including band matching rules and defaults for speed quantization and direction
 * interpretation.
 *
 * <p>See @link BandMatchingConfig
 */
public final class PngWindConfig {

    public enum DirectionConvention {
        FROM,
        TO
    }

    public enum DirectionUnit {
        DEG,
        RAD
    }

    private final BandMatchingConfig bandMatching;
    private final double defaultMin;
    private final double defaultMax;
    private final DirectionConvention directionConvention;
    private final DirectionUnit directionUnit;

    public PngWindConfig(
            BandMatchingConfig bandMatching,
            double defaultMin,
            double defaultMax,
            DirectionConvention directionConvention,
            DirectionUnit directionUnit) {
        this.bandMatching = bandMatching;
        this.defaultMin = defaultMin;
        this.defaultMax = defaultMax;
        this.directionConvention = directionConvention;
        this.directionUnit = directionUnit;
    }

    public BandMatchingConfig getBandMatching() {
        return bandMatching;
    }

    /** @return the default minimum value to use for speed quantization when not provided. */
    public double getDefaultMin() {
        return defaultMin;
    }

    /** @return the default maximum value to use for speed quantization when not provided. */
    public double getDefaultMax() {
        return defaultMax;
    }

    /**
     * @return the convention used to interpret direction values, either as the direction the wind is coming from (FROM)
     *     or going to (TO)
     */
    public DirectionConvention getDirectionConvention() {
        return directionConvention;
    }

    /** @return the unit used for direction values, either degrees (DEG) or radians (RAD) */
    public DirectionUnit getDirectionUnit() {
        return directionUnit;
    }
}
