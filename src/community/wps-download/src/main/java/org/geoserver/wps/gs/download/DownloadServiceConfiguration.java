/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

/**
 * Bean that includes the configurations parameters for the download service
 *
 * @author Simone Giannecchini, GeoSolutions
 */
public class DownloadServiceConfiguration {

    /** Value used to indicate no limits */
    public static final int NO_LIMIT = 0;

    public static final String COMPRESSION_LEVEL_NAME = "compressionLevel";

    public static final String HARD_OUTPUT_LIMITS_NAME = "hardOutputLimit";

    public static final String RASTER_SIZE_LIMITS_NAME = "rasterSizeLimits";

    public static final String WRITE_LIMITS_NAME = "writeLimits";

    public static final String MAX_FEATURES_NAME = "maxFeatures";

    public static final String MAX_ANIMATION_FRAMES_NAME = "maxAnimationFrames";

    public static final int DEFAULT_COMPRESSION_LEVEL = 4;

    public static final long DEFAULT_HARD_OUTPUT_LIMITS = NO_LIMIT;

    public static final long DEFAULT_RASTER_SIZE_LIMITS = NO_LIMIT;

    public static final long DEFAULT_WRITE_LIMITS = NO_LIMIT;

    public static final long DEFAULT_MAX_FEATURES = NO_LIMIT;

    public static final int DEFAULT_MAX_ANIMATION_FRAMES = NO_LIMIT;

    /** Max #of features */
    private long maxFeatures = DEFAULT_MAX_FEATURES;

    /** 8000 px X 8000 px */
    private long rasterSizeLimits = DEFAULT_RASTER_SIZE_LIMITS;

    /** Max size in bytes of raw raster output */
    private long writeLimits = DEFAULT_WRITE_LIMITS;

    /** 50 MB */
    private long hardOutputLimit = DEFAULT_HARD_OUTPUT_LIMITS;

    /** STORE =0, BEST =8 */
    private int compressionLevel = DEFAULT_COMPRESSION_LEVEL;

    private int maxAnimationFrames = DEFAULT_MAX_ANIMATION_FRAMES;

    /** Constructor: */
    public DownloadServiceConfiguration(
            long maxFeatures,
            long rasterSizeLimits,
            long writeLimits,
            long hardOutputLimit,
            int compressionLevel,
            int maxAnimationFrames) {
        this.maxFeatures = maxFeatures;
        this.rasterSizeLimits = rasterSizeLimits;
        this.writeLimits = writeLimits;
        this.hardOutputLimit = hardOutputLimit;
        this.compressionLevel = compressionLevel;
        this.maxAnimationFrames = maxAnimationFrames;
    }

    /** Default constructor */
    public DownloadServiceConfiguration() {
        this(
                DEFAULT_MAX_FEATURES,
                DEFAULT_RASTER_SIZE_LIMITS,
                DEFAULT_WRITE_LIMITS,
                DEFAULT_HARD_OUTPUT_LIMITS,
                DEFAULT_COMPRESSION_LEVEL,
                DEFAULT_MAX_ANIMATION_FRAMES);
    }

    public long getMaxFeatures() {
        return maxFeatures;
    }

    public long getRasterSizeLimits() {
        return rasterSizeLimits;
    }

    public long getWriteLimits() {
        return writeLimits;
    }

    public long getHardOutputLimit() {
        return hardOutputLimit;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public int getMaxAnimationFrames() {
        return maxAnimationFrames;
    }

    @Override
    public String toString() {
        return "DownloadServiceConfiguration [maxFeatures="
                + maxFeatures
                + ", rasterSizeLimits="
                + rasterSizeLimits
                + ", writeLimits="
                + writeLimits
                + ", hardOutputLimit="
                + hardOutputLimit
                + ", compressionLevel="
                + compressionLevel
                + "]";
    }
}
