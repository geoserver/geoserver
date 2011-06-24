/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.util.Assert;

public class GWCConfig implements Cloneable, Serializable {

    private static final long serialVersionUID = 3287178222706781438L;

    private boolean directWMSIntegrationEnabled;

    private boolean WMSCEnabled;

    private boolean WMTSEnabled;

    private boolean TMSEnabled;

    /**
     * Whether to automatically cache GeoServer layers or they should be enabled explicitly
     */
    private boolean cacheLayersByDefault = true;

    /**
     * Whether to cache the layer's declared CRS by default
     * <p>
     * TODO:this one is problematic until we have a steady way of defining new gridsets based on a
     * CRS
     * </p>
     */
    private transient boolean cacheDeclaredCRS;

    /**
     * Whether to cache any non default Style associated to the layer
     */
    private boolean cacheNonDefaultStyles;

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

    private HashSet<String> defaultCoverageCacheFormats;

    private HashSet<String> defaultVectorCacheFormats;

    /**
     * Default cache formats for non coverage/vector layers (LayerGroups and WMS layers)
     */
    private HashSet<String> defaultOtherCacheFormats;

    /**
     * Creates a new GWC config with default values
     */
    public GWCConfig() {
        setOldDefaults();
        String png = "image/png";
        String jpeg = "image/jpeg";

        setDefaultCoverageCacheFormats(Collections.singleton(jpeg));
        setDefaultOtherCacheFormats(new HashSet<String>(Arrays.asList(png, jpeg)));
        setDefaultVectorCacheFormats(Collections.singleton(png));
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

    public boolean isWMTSEnabled() {
        return WMTSEnabled;
    }

    public void setWMTSEnabled(boolean wMTSEnabled) {
        WMTSEnabled = wMTSEnabled;
    }

    public boolean isTMSEnabled() {
        return TMSEnabled;
    }

    public void setTMSEnabled(boolean tMSEnabled) {
        TMSEnabled = tMSEnabled;
    }

    /**
     * see reason of getting rid of this property at the fields comment
     */
    private boolean isCacheDeclaredCRS() {
        return cacheDeclaredCRS;
    }

    private void setCacheDeclaredCRS(boolean cacheDeclaredCRS) {
        this.cacheDeclaredCRS = cacheDeclaredCRS;
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
     * Returns a config suitable to match the old defaults when the integrated GWC behaivour was not
     * configurable.
     */
    public static GWCConfig getOldDefaults() {
        GWCConfig config = new GWCConfig();
        config.setOldDefaults();
        return config;
    }

    private void setOldDefaults() {

        setCacheDeclaredCRS(false);
        setCacheLayersByDefault(true);
        setMetaTilingX(4);
        setMetaTilingY(4);
        setGutter(0);
        // this is not an old default, but a new feature so we enabled it anyway
        setCacheNonDefaultStyles(true);
        setDefaultCachingGridSetIds(new HashSet<String>(Arrays.asList("EPSG:4326", "EPSG:900913")));
        Set<String> oldDefaultFormats = new HashSet<String>(
                Arrays.asList("image/png", "image/jpeg"));
        setDefaultCoverageCacheFormats(oldDefaultFormats);
        setDefaultOtherCacheFormats(oldDefaultFormats);
        setDefaultVectorCacheFormats(oldDefaultFormats);
        setDirectWMSIntegrationEnabled(false);
        setWMSCEnabled(true);
        setWMTSEnabled(true);
        setTMSEnabled(true);
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

        return clone;
    }

    public boolean isEnabled(final String serviceId) {
        Assert.notNull(serviceId);
        if ("wms".equals(serviceId)) {
            return isWMSCEnabled();
        }
        if ("wmts".equals(serviceId)) {
            return isWMTSEnabled();
        }
        if ("tms".equals(serviceId)) {
            return isTMSEnabled();
        }
        return true;
    }
}
