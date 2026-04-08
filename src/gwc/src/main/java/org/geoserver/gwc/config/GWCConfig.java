/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.geoserver.util.DimensionWarning;
import org.geowebcache.locks.LockProvider;

public class GWCConfig implements Cloneable, Serializable {

    @Serial
    private static final long serialVersionUID = 3287178222706781438L;

    private String version;

    private boolean directWMSIntegrationEnabled;

    private Boolean requireTiledParameter = true;

    private boolean WMSCEnabled;

    private boolean TMSEnabled;

    private Boolean WMTSEnabled;

    private boolean securityEnabled;

    /** Whether to automatically cache GeoServer layers or they should be enabled explicitly */
    private boolean cacheLayersByDefault = true;

    /** Whether to cache any non default Style associated to the layer */
    private boolean cacheNonDefaultStyles;

    /** Default meta-tiling factor for the X axis */
    private int metaTilingX;

    /** Default meta-tiling factor for the Y axis */
    private int metaTilingY;

    /** Number of threads available for concurrent encoding/saving of tiles within a meta-tile */
    private Integer metaTilingThreads;

    /** Default gutter size in pixels */
    private int gutter;

    /** Which SRS's to cache by default when adding a new Layer. Defaults to {@code [EPSG:4326, EPSG:900913]} */
    private HashSet<String> defaultCachingGridSetIds;

    private HashSet<String> defaultCoverageCacheFormats;

    private HashSet<String> defaultVectorCacheFormats;

    /** Default cache formats for non coverage/vector layers (LayerGroups and WMS layers) */
    private HashSet<String> defaultOtherCacheFormats;

    private String lockProviderName;

    // Set of cache warnings that would cause caching being skipped
    Set<DimensionWarning.WarningType> cacheWarningSkips;

    /** Creates a new GWC config with default values */
    public GWCConfig() {
        setOldDefaults();

        String png = "image/png";
        String jpeg = "image/jpeg";
        setDefaultCoverageCacheFormats(Collections.singleton(jpeg));
        setDefaultOtherCacheFormats(new HashSet<>(Arrays.asList(png, jpeg)));
        setDefaultVectorCacheFormats(Collections.singleton(png));
        setCacheWarningSkips(Collections.emptySet());
        setRequireTiledParameter(true);
        readResolve();
    }

    protected Object readResolve() {
        if (null == version) {
            version = "0.0.1";
        }

        if (defaultCachingGridSetIds == null) {
            defaultCachingGridSetIds = new HashSet<>();
        }
        if (defaultCoverageCacheFormats == null) {
            defaultCoverageCacheFormats = new HashSet<>();
        }
        if (defaultOtherCacheFormats == null) {
            defaultOtherCacheFormats = new HashSet<>();
        }
        if (defaultVectorCacheFormats == null) {
            defaultVectorCacheFormats = new HashSet<>();
        }
        if (cacheWarningSkips == null) {
            cacheWarningSkips = new LinkedHashSet<>();
        }

        return this;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isCacheLayersByDefault() {
        return cacheLayersByDefault;
    }

    public void setCacheLayersByDefault(boolean cacheLayersByDefault) {
        this.cacheLayersByDefault = cacheLayersByDefault;
    }

    public boolean isDirectWMSIntegrationEnabled() {
        return directWMSIntegrationEnabled;
    }

    public void setDirectWMSIntegrationEnabled(boolean directWMSIntegrationEnabled) {
        this.directWMSIntegrationEnabled = directWMSIntegrationEnabled;
    }

    public boolean isRequireTiledParameter() {
        if (requireTiledParameter == null) {
            return true;
        }
        return requireTiledParameter;
    }

    public void setRequireTiledParameter(boolean requireTiledParameter) {
        this.requireTiledParameter = requireTiledParameter;
    }

    public boolean isWMSCEnabled() {
        return WMSCEnabled;
    }

    public void setWMSCEnabled(boolean wMSCEnabled) {
        WMSCEnabled = wMSCEnabled;
    }

    public boolean isTMSEnabled() {
        return TMSEnabled;
    }

    public void setTMSEnabled(boolean tMSEnabled) {
        TMSEnabled = tMSEnabled;
    }

    public void setSecurityEnabled(boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }

    public boolean isSecurityEnabled() {
        return securityEnabled;
    }

    public boolean isCacheNonDefaultStyles() {
        return cacheNonDefaultStyles;
    }

    public void setCacheNonDefaultStyles(boolean cacheNonDefaultStyles) {
        this.cacheNonDefaultStyles = cacheNonDefaultStyles;
    }

    public Set<String> getDefaultCachingGridSetIds() {
        return defaultCachingGridSetIds;
    }

    public void setDefaultCachingGridSetIds(Set<String> defaultCachingGridSetIds) {
        this.defaultCachingGridSetIds = new HashSet<>(defaultCachingGridSetIds);
    }

    public Set<String> getDefaultCoverageCacheFormats() {
        return defaultCoverageCacheFormats;
    }

    public void setDefaultCoverageCacheFormats(Set<String> defaultCoverageCacheFormats) {
        this.defaultCoverageCacheFormats = new HashSet<>(defaultCoverageCacheFormats);
    }

    public Set<String> getDefaultVectorCacheFormats() {
        return defaultVectorCacheFormats;
    }

    public void setDefaultVectorCacheFormats(Set<String> defaultVectorCacheFormats) {
        this.defaultVectorCacheFormats = new HashSet<>(defaultVectorCacheFormats);
    }

    public Set<String> getDefaultOtherCacheFormats() {
        return defaultOtherCacheFormats;
    }

    public void setDefaultOtherCacheFormats(Set<String> defaultOtherCacheFormats) {
        this.defaultOtherCacheFormats = new HashSet<>(defaultOtherCacheFormats);
    }

    /**
     * @return an instance of GWCConfig (possibly {@code this}) that is sane, in case the configured defaults are not
     *     (like in missing some config option). This is just a safety measure to ensure a mis configured gwc-gs.xml
     *     does not prevent the creation of tile layers (ej, automatic creation of new tile layers may be disabled in
     *     gwc-gs.xml and its contents may not lead to a sane state to be used as default settings).
     */
    public GWCConfig saneConfig() {
        if (isSane()) {
            return this;
        }
        GWCConfig sane = getOldDefaults();
        sane.setRequireTiledParameter(true);
        // sane.setCacheLayersByDefault(cacheLayersByDefault);
        if (metaTilingX > 0) {
            sane.setMetaTilingX(metaTilingX);
        }
        if (metaTilingY > 0) {
            sane.setMetaTilingY(metaTilingY);
        }
        if (gutter >= 0) {
            sane.setGutter(gutter);
        }
        if (!defaultCachingGridSetIds.isEmpty()) {
            sane.setDefaultCachingGridSetIds(defaultCachingGridSetIds);
        }
        if (!defaultCoverageCacheFormats.isEmpty()) {
            sane.setDefaultCoverageCacheFormats(defaultCoverageCacheFormats);
        }
        if (!defaultOtherCacheFormats.isEmpty()) {
            sane.setDefaultOtherCacheFormats(defaultOtherCacheFormats);
        }
        if (!defaultVectorCacheFormats.isEmpty()) {
            sane.setDefaultVectorCacheFormats(defaultVectorCacheFormats);
        }
        if (metaTilingThreads != null && metaTilingThreads >= 0) {
            sane.setMetaTilingThreads(metaTilingThreads);
        }
        return sane;
    }

    public boolean isSane() {
        return metaTilingX > 0
                && metaTilingY > 0
                && (metaTilingThreads == null || metaTilingThreads >= 0)
                && gutter >= 0
                && !defaultCachingGridSetIds.isEmpty()
                && !defaultCoverageCacheFormats.isEmpty()
                && !defaultOtherCacheFormats.isEmpty()
                && !defaultVectorCacheFormats.isEmpty();
    }

    /** Returns a config suitable to match the old defaults when the integrated GWC behaivour was not configurable. */
    public static GWCConfig getOldDefaults() {
        GWCConfig config = new GWCConfig();
        config.setOldDefaults();
        return config;
    }

    private void setOldDefaults() {
        setCacheLayersByDefault(true);
        setMetaTilingX(4);
        setMetaTilingY(4);
        setGutter(0);
        // this is not an old default, but a new feature so we enabled it anyway
        setCacheNonDefaultStyles(true);
        setRequireTiledParameter(true);
        setDefaultCachingGridSetIds(new HashSet<>(Arrays.asList("EPSG:4326", "EPSG:900913")));
        Set<String> oldDefaultFormats = new HashSet<>(Arrays.asList("image/png", "image/jpeg"));
        setDefaultCoverageCacheFormats(oldDefaultFormats);
        setDefaultOtherCacheFormats(oldDefaultFormats);
        setDefaultVectorCacheFormats(oldDefaultFormats);
        setDirectWMSIntegrationEnabled(false);
        setWMSCEnabled(true);
        setTMSEnabled(true);
        setCacheWarningSkips(new LinkedHashSet<>());
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

    public Integer getMetaTilingThreads() {
        return metaTilingThreads;
    }

    public void setMetaTilingThreads(Integer metaTilingThreads) {
        this.metaTilingThreads = metaTilingThreads;
    }

    public int getGutter() {
        return gutter;
    }

    public void setGutter(int gutter) {
        this.gutter = gutter;
    }

    @Override
    public GWCConfig clone() {
        GWCConfig clone;
        try {
            clone = (GWCConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        clone.setDefaultCachingGridSetIds(getDefaultCachingGridSetIds());
        clone.setDefaultCoverageCacheFormats(getDefaultCoverageCacheFormats());
        clone.setDefaultVectorCacheFormats(getDefaultVectorCacheFormats());
        clone.setDefaultOtherCacheFormats(getDefaultOtherCacheFormats());
        clone.setRequireTiledParameter(this.isRequireTiledParameter());
        clone.setCacheWarningSkips(getCacheWarningSkips());

        return clone;
    }

    public boolean isEnabled(final String serviceId) {
        checkNotNull(serviceId);
        if ("wms".equalsIgnoreCase(serviceId)) {
            return isWMSCEnabled();
        }
        if ("wmts".equalsIgnoreCase(serviceId)) {
            throw new RuntimeException("To check if WMTS service is enable or disable use service info.");
        }
        if ("tms".equalsIgnoreCase(serviceId)) {
            return isTMSEnabled();
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GWCConfig gwcConfig = (GWCConfig) o;
        return directWMSIntegrationEnabled == gwcConfig.directWMSIntegrationEnabled
                && Objects.equals(requireTiledParameter, gwcConfig.requireTiledParameter)
                && WMSCEnabled == gwcConfig.WMSCEnabled
                && TMSEnabled == gwcConfig.TMSEnabled
                && securityEnabled == gwcConfig.securityEnabled
                && cacheLayersByDefault == gwcConfig.cacheLayersByDefault
                && cacheNonDefaultStyles == gwcConfig.cacheNonDefaultStyles
                && metaTilingX == gwcConfig.metaTilingX
                && metaTilingY == gwcConfig.metaTilingY
                && Objects.equals(metaTilingThreads, gwcConfig.metaTilingThreads)
                && gutter == gwcConfig.gutter
                && Objects.equals(version, gwcConfig.version)
                && Objects.equals(WMTSEnabled, gwcConfig.WMTSEnabled)
                && Objects.equals(defaultCachingGridSetIds, gwcConfig.defaultCachingGridSetIds)
                && Objects.equals(defaultCoverageCacheFormats, gwcConfig.defaultCoverageCacheFormats)
                && Objects.equals(defaultVectorCacheFormats, gwcConfig.defaultVectorCacheFormats)
                && Objects.equals(defaultOtherCacheFormats, gwcConfig.defaultOtherCacheFormats)
                && Objects.equals(lockProviderName, gwcConfig.lockProviderName)
                && Objects.equals(cacheWarningSkips, gwcConfig.cacheWarningSkips);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                version,
                directWMSIntegrationEnabled,
                requireTiledParameter,
                WMSCEnabled,
                TMSEnabled,
                WMTSEnabled,
                securityEnabled,
                cacheLayersByDefault,
                cacheNonDefaultStyles,
                metaTilingX,
                metaTilingY,
                metaTilingThreads,
                gutter,
                defaultCachingGridSetIds,
                defaultCoverageCacheFormats,
                defaultVectorCacheFormats,
                defaultOtherCacheFormats,
                lockProviderName,
                cacheWarningSkips);
    }

    public String getLockProviderName() {
        return lockProviderName;
    }

    /** Sets the name of the {@link LockProvider} Spring bean to be used as the lock provider for this GWC instance */
    public void setLockProviderName(String lockProviderName) {
        this.lockProviderName = lockProviderName;
    }

    public Boolean isWMTSEnabled() {
        return WMTSEnabled;
    }

    public void setWMTSEnabled(Boolean WMTSEnabled) {
        this.WMTSEnabled = WMTSEnabled;
    }

    public Set<DimensionWarning.WarningType> getCacheWarningSkips() {
        return cacheWarningSkips;
    }

    public void setCacheWarningSkips(Set<DimensionWarning.WarningType> cacheWarningSkips) {
        this.cacheWarningSkips = new LinkedHashSet<>(cacheWarningSkips);
    }
}
