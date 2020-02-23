/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import static com.google.common.base.Preconditions.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.geowebcache.locks.LockProvider;
import org.geowebcache.storage.blobstore.memory.CacheConfiguration;
import org.geowebcache.storage.blobstore.memory.CacheProvider;
import org.geowebcache.storage.blobstore.memory.guava.GuavaCacheProvider;

public class GWCConfig implements Cloneable, Serializable {

    private static final long serialVersionUID = 3287178222706781438L;

    private String version;

    private boolean directWMSIntegrationEnabled;

    private boolean WMSCEnabled;

    private boolean TMSEnabled;

    private Boolean WMTSEnabled;

    private boolean securityEnabled;

    /** Boolean indicating if InnerCaching should be used instead of default FileSystem caching */
    private boolean innerCachingEnabled;

    /**
     * Boolean indicating if the Tiles stored in memory should be also stored in the FileSystem as
     * backup
     */
    private boolean persistenceEnabled;

    /**
     * String indicating the class of the {@link CacheProvider} instance used for caching GWC Tiles
     */
    private String cacheProviderClass;

    /**
     * {@link Map} containing all the {@link CacheConfiguration} object stored for each {@link
     * CacheProvider} instance.
     */
    private Map<String, CacheConfiguration> cacheConfigurations;

    /** Whether to automatically cache GeoServer layers or they should be enabled explicitly */
    private boolean cacheLayersByDefault = true;

    /** Whether to cache any non default Style associated to the layer */
    private boolean cacheNonDefaultStyles;

    /** Default meta-tiling factor for the X axis */
    private int metaTilingX;

    /** Default meta-tiling factor for the Y axis */
    private int metaTilingY;

    /** Default gutter size in pixels */
    private int gutter;

    /**
     * Which SRS's to cache by default when adding a new Layer. Defaults to {@code [EPSG:4326,
     * EPSG:900913]}
     */
    private HashSet<String> defaultCachingGridSetIds;

    private HashSet<String> defaultCoverageCacheFormats;

    private HashSet<String> defaultVectorCacheFormats;

    /** Default cache formats for non coverage/vector layers (LayerGroups and WMS layers) */
    private HashSet<String> defaultOtherCacheFormats;

    private String lockProviderName;

    /** Creates a new GWC config with default values */
    public GWCConfig() {
        setOldDefaults();

        String png = "image/png";
        String jpeg = "image/jpeg";
        setDefaultCoverageCacheFormats(Collections.singleton(jpeg));
        setDefaultOtherCacheFormats(new HashSet<String>(Arrays.asList(png, jpeg)));
        setDefaultVectorCacheFormats(Collections.singleton(png));
        Map<String, CacheConfiguration> map = new HashMap<String, CacheConfiguration>();
        map.put(GuavaCacheProvider.class.toString(), new CacheConfiguration());
        setCacheConfigurations(map);

        readResolve();
    }

    protected Object readResolve() {
        if (null == version) {
            version = "0.0.1";
        }

        if (defaultCachingGridSetIds == null) {
            defaultCachingGridSetIds = new HashSet<String>();
        }
        if (defaultCoverageCacheFormats == null) {
            defaultCoverageCacheFormats = new HashSet<String>();
        }
        if (defaultOtherCacheFormats == null) {
            defaultOtherCacheFormats = new HashSet<String>();
        }
        if (defaultVectorCacheFormats == null) {
            defaultVectorCacheFormats = new HashSet<String>();
        }
        if (cacheConfigurations == null) {
            cacheConfigurations = new HashMap<String, CacheConfiguration>();
            cacheConfigurations.put(GuavaCacheProvider.class.toString(), new CacheConfiguration());
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
        this.defaultCachingGridSetIds = new HashSet<String>(defaultCachingGridSetIds);
    }

    public Set<String> getDefaultCoverageCacheFormats() {
        return defaultCoverageCacheFormats;
    }

    public void setDefaultCoverageCacheFormats(Set<String> defaultCoverageCacheFormats) {
        this.defaultCoverageCacheFormats = new HashSet<String>(defaultCoverageCacheFormats);
    }

    public Set<String> getDefaultVectorCacheFormats() {
        return defaultVectorCacheFormats;
    }

    public void setDefaultVectorCacheFormats(Set<String> defaultVectorCacheFormats) {
        this.defaultVectorCacheFormats = new HashSet<String>(defaultVectorCacheFormats);
    }

    public Set<String> getDefaultOtherCacheFormats() {
        return defaultOtherCacheFormats;
    }

    public void setDefaultOtherCacheFormats(Set<String> defaultOtherCacheFormats) {
        this.defaultOtherCacheFormats = new HashSet<String>(defaultOtherCacheFormats);
    }

    /**
     * @return an instance of GWCConfig (possibly {@code this}) that is sane, in case the configured
     *     defaults are not (like in missing some config option). This is just a safety measure to
     *     ensure a mis configured gwc-gs.xml does not prevent the creation of tile layers (ej,
     *     automatic creation of new tile layers may be disabled in gwc-gs.xml and its contents may
     *     not lead to a sane state to be used as default settings).
     */
    public GWCConfig saneConfig() {
        if (isSane()) {
            return this;
        }
        GWCConfig sane = GWCConfig.getOldDefaults();

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
        return sane;
    }

    public boolean isSane() {
        return metaTilingX > 0
                && metaTilingY > 0
                && gutter >= 0
                && !defaultCachingGridSetIds.isEmpty()
                && !defaultCoverageCacheFormats.isEmpty()
                && !defaultOtherCacheFormats.isEmpty()
                && !defaultVectorCacheFormats.isEmpty();
    }

    /**
     * Returns a config suitable to match the old defaults when the integrated GWC behaivour was not
     * configurable.
     */
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
        setDefaultCachingGridSetIds(new HashSet<String>(Arrays.asList("EPSG:4326", "EPSG:900913")));
        Set<String> oldDefaultFormats =
                new HashSet<String>(Arrays.asList("image/png", "image/jpeg"));
        setDefaultCoverageCacheFormats(oldDefaultFormats);
        setDefaultOtherCacheFormats(oldDefaultFormats);
        setDefaultVectorCacheFormats(oldDefaultFormats);
        setDirectWMSIntegrationEnabled(false);
        setWMSCEnabled(true);
        setTMSEnabled(true);
        setEnabledPersistence(true);
        setInnerCachingEnabled(false);
        HashMap<String, CacheConfiguration> map = new HashMap<String, CacheConfiguration>();
        map.put(GuavaCacheProvider.class.toString(), new CacheConfiguration());
        setCacheConfigurations(map);
        setCacheProviderClass(GuavaCacheProvider.class.toString());
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
        clone.setCacheConfigurations(getCacheConfigurations());

        return clone;
    }

    public boolean isEnabled(final String serviceId) {
        checkNotNull(serviceId);
        if ("wms".equalsIgnoreCase(serviceId)) {
            return isWMSCEnabled();
        }
        if ("wmts".equalsIgnoreCase(serviceId)) {
            throw new RuntimeException(
                    "To check if WMTS service is enable or disable use service info.");
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
                && WMSCEnabled == gwcConfig.WMSCEnabled
                && TMSEnabled == gwcConfig.TMSEnabled
                && securityEnabled == gwcConfig.securityEnabled
                && innerCachingEnabled == gwcConfig.innerCachingEnabled
                && persistenceEnabled == gwcConfig.persistenceEnabled
                && cacheLayersByDefault == gwcConfig.cacheLayersByDefault
                && cacheNonDefaultStyles == gwcConfig.cacheNonDefaultStyles
                && metaTilingX == gwcConfig.metaTilingX
                && metaTilingY == gwcConfig.metaTilingY
                && gutter == gwcConfig.gutter
                && Objects.equals(version, gwcConfig.version)
                && Objects.equals(WMTSEnabled, gwcConfig.WMTSEnabled)
                && Objects.equals(cacheProviderClass, gwcConfig.cacheProviderClass)
                && Objects.equals(cacheConfigurations, gwcConfig.cacheConfigurations)
                && Objects.equals(defaultCachingGridSetIds, gwcConfig.defaultCachingGridSetIds)
                && Objects.equals(
                        defaultCoverageCacheFormats, gwcConfig.defaultCoverageCacheFormats)
                && Objects.equals(defaultVectorCacheFormats, gwcConfig.defaultVectorCacheFormats)
                && Objects.equals(defaultOtherCacheFormats, gwcConfig.defaultOtherCacheFormats)
                && Objects.equals(lockProviderName, gwcConfig.lockProviderName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                version,
                directWMSIntegrationEnabled,
                WMSCEnabled,
                TMSEnabled,
                WMTSEnabled,
                securityEnabled,
                innerCachingEnabled,
                persistenceEnabled,
                cacheProviderClass,
                cacheConfigurations,
                cacheLayersByDefault,
                cacheNonDefaultStyles,
                metaTilingX,
                metaTilingY,
                gutter,
                defaultCachingGridSetIds,
                defaultCoverageCacheFormats,
                defaultVectorCacheFormats,
                defaultOtherCacheFormats,
                lockProviderName);
    }

    public String getLockProviderName() {
        return lockProviderName;
    }

    /**
     * Sets the name of the {@link LockProvider} Spring bean to be used as the lock provider for
     * this GWC instance
     */
    public void setLockProviderName(String lockProviderName) {
        this.lockProviderName = lockProviderName;
    }

    /**
     * Checks whether GWC Tiles should be cached in memory instead of caching them in the File
     * System
     *
     * @return a boolean indicating if tiles are cached in memory
     */
    public boolean isInnerCachingEnabled() {
        return innerCachingEnabled;
    }

    /**
     * This method sets a flag indicating if GWC tiles must be cached in memory
     *
     * @param innerCachingEnabled If this flag is set to true, GWC tiles will be cached in memory
     *     instead of being cached on the disk.
     */
    public void setInnerCachingEnabled(boolean innerCachingEnabled) {
        this.innerCachingEnabled = innerCachingEnabled;
    }

    /**
     * Checks whether GWC Tiles are stored in the File System also if they are already stored in
     * memory
     *
     * @return a boolean indicating if GWC tiles are also cached in FileSystem
     */
    public boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }

    /**
     * This method sets a flag indicating if GWC tiles must be stored in File System even if they
     * are also cached in memory
     *
     * @param persistenceEnabled If this flag is set to true, GWC tiles are stored in the File
     *     System as backup.
     */
    public void setEnabledPersistence(boolean persistenceEnabled) {
        this.persistenceEnabled = persistenceEnabled;
    }

    /**
     * Method returning the current {@link CacheProvider} class name
     *
     * @return the currently used {@link CacheProvider} name
     */
    public String getCacheProviderClass() {
        return cacheProviderClass;
    }

    /**
     * This method allows to set a new {@link CacheProvider} instance by defining its name.
     *
     * @param cacheProviderClass The name of the new {@link CacheProvider} instance to set.
     */
    public void setCacheProviderClass(String cacheProviderClass) {
        this.cacheProviderClass = cacheProviderClass;
    }

    /**
     * This method returns a {@link Map} containing the {@link CacheConfiguration} instances related
     * to each {@link CacheProvider}.
     *
     * @return A {@link Map} which maps {@link CacheConfiguration}s to {@link CacheProvider}s
     */
    public Map<String, CacheConfiguration> getCacheConfigurations() {
        return cacheConfigurations;
    }

    /**
     * This method sets a new {@link Map} which associates to each {@link CacheProvider}, the
     * related {@link CacheConfiguration}.
     *
     * @param cacheConfigurations A {@link Map} containing {@link CacheConfiguration}s associated to
     *     the {@link CacheProvider} keys.
     */
    public void setCacheConfigurations(Map<String, CacheConfiguration> cacheConfigurations) {
        this.cacheConfigurations = new HashMap<String, CacheConfiguration>(cacheConfigurations);
    }

    public Boolean isWMTSEnabled() {
        return WMTSEnabled;
    }

    public void setWMTSEnabled(Boolean WMTSEnabled) {
        this.WMTSEnabled = WMTSEnabled;
    }
}
