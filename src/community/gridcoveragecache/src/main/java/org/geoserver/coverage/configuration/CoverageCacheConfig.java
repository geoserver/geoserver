/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage.configuration;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.geoserver.coverage.layer.CoverageTileLayerInfo.InterpolationType;
import org.geoserver.coverage.layer.CoverageTileLayerInfo.SeedingPolicy;
import org.geoserver.coverage.layer.CoverageTileLayerInfo.TiffCompression;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geowebcache.locks.LockProvider;

public class CoverageCacheConfig implements Cloneable, Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * Default meta-tiling factor for the X axis
     */
    private int metaTilingX;

    /**
     * Default meta-tiling factor for the Y axis
     */
    private int metaTilingY;

    /**
     * Default gutter size in pixels
     */
    private int gutter;

    /**
     * Which SRS's to cache by default when adding a new Layer. Defaults to
     * {@code [EPSG:4326, EPSG:900913]}
     */
    private HashSet<String> defaultCachingGridSetIds;
    
    private String lockProviderName;

    /**
     * Creates a new CoverageCacheConfig with default values
     */
    public CoverageCacheConfig() {
        setOldDefaults();
        readResolve();
    }

    private CoverageCacheConfig readResolve() {

        if (defaultCachingGridSetIds == null) {
            defaultCachingGridSetIds = new HashSet<String>();
        }

        return this;
    }

    public Set<String> getDefaultCachingGridSetIds() {
        return defaultCachingGridSetIds;
    }

    public void setDefaultCachingGridSetIds(Set<String> defaultCachingGridSetIds) {
        this.defaultCachingGridSetIds = new HashSet<String>(defaultCachingGridSetIds);
    }

//    /**
//     * @return an instance of GWCConfig (possibly {@code this}) that is sane, in case the configured
//     *         defaults are not (like in missing some config option). This is just a safety measure
//     *         to ensure a mis configured gwc-gs.xml does not prevent the creation of tile layers
//     *         (ej, automatic creation of new tile layers may be disabled in gwc-gs.xml and its
//     *         contents may not lead to a sane state to be used as default settings).
//     */
//    public CoverageCacheConfig saneConfig() {
//        if (isSane()) {
//            return this;
//        }
//        CoverageCacheConfig sane = CoverageCacheConfig.getOldDefaults();
//
//        // sane.setCacheLayersByDefault(cacheLayersByDefault);
//        if (metaTilingX > 0) {
//            sane.setMetaTilingX(metaTilingX);
//        }
//        if (metaTilingY > 0) {
//            sane.setMetaTilingY(metaTilingY);
//        }
//        if (gutter >= 0) {
//            sane.setGutter(gutter);
//        }
//        if (!defaultCachingGridSetIds.isEmpty()) {
//            sane.setDefaultCachingGridSetIds(defaultCachingGridSetIds);
//        }
//        return sane;
//    }

    public boolean isSane() {
        return metaTilingX > 0 && metaTilingY > 0 && gutter >= 0
                && !defaultCachingGridSetIds.isEmpty();

    }

    /**
     * Returns a config suitable to match the old defaults when the integrated GWC behaivour was not
     * configurable.
     */
    public static CoverageCacheConfig getOldDefaults() {
        CoverageCacheConfig config = new CoverageCacheConfig();
        config.setOldDefaults();
        return config;
    }

    private void setOldDefaults() {
        setMetaTilingX(2);
        setMetaTilingY(2);
        setGutter(0);
        
        setSeedingPolicy(SeedingPolicy.DIRECT);
        setOverviewPolicy(OverviewPolicy.IGNORE);
        setInterpolationType(InterpolationType.NEAREST);
        setTiffCompression(TiffCompression.NONE);
        // this is not an old default, but a new feature so we enabled it anyway
        setDefaultCachingGridSetIds(new HashSet<String>(Arrays.asList("EPSG:4326")));
    }

    public int getMetaTilingX() {
        return metaTilingX;
    }

    public void setMetaTilingX(int metaFactorX) {
        this.metaTilingX = metaFactorX;
    }

    public int getMetaTilingY() {
        return metaTilingY;
    }

    public void setMetaTilingY(int metaFactorY) {
        this.metaTilingY = metaFactorY;
    }

    public int getGutter() {
        return gutter;
    }

    public void setGutter(int gutter) {
        this.gutter = gutter;
    }
    
    private SeedingPolicy seedingPolicy = SeedingPolicy.DIRECT;
    
    private InterpolationType interpolationType = InterpolationType.NEAREST;
    
    private TiffCompression tiffCompression = TiffCompression.NONE;

    private OverviewPolicy overviewPolicy = OverviewPolicy.IGNORE;

    public void setInterpolationType(InterpolationType resamplingAlgorithm) {
        this.interpolationType = resamplingAlgorithm;
    }

    public InterpolationType getInterpolationType() {
        return interpolationType;
    }

    public void setSeedingPolicy(SeedingPolicy seedingPolicy) {
        this.seedingPolicy = seedingPolicy;
    }

    public SeedingPolicy getSeedingPolicy() {
        return seedingPolicy;
    }

    public void setTiffCompression(TiffCompression tiffCompression) {
        this.tiffCompression = tiffCompression;
    }

    public TiffCompression getTiffCompression() {
        return tiffCompression;
    }

    public void setOverviewPolicy(OverviewPolicy overviewPolicy) {
        this.overviewPolicy  = overviewPolicy;
    }

    public OverviewPolicy getOverviewPolicy() {
        return overviewPolicy;
    }

    @Override
    public CoverageCacheConfig clone() {
        CoverageCacheConfig clone;
        try {
            clone = (CoverageCacheConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        clone.setDefaultCachingGridSetIds(getDefaultCachingGridSetIds());

        return clone;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }
    
    public String getLockProviderName() {
        return lockProviderName;
    }
    
    /**
     * Sets the name of the {@link LockProvider} Spring bean to be used as the lock provider
     * for this GWC instance
     * 
     * @param lockProviderName
     */
    public void setLockProviderName(String lockProviderName) {
        this.lockProviderName = lockProviderName;
    }
}
