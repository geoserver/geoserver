package org.geoserver.gwc.layer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.gwc.config.GWCConfig;
import org.springframework.util.Assert;

public class GeoServerTileLayerInfo {
    private static final String CONFIG_KEY_ENABLED = "GWC.enabled";

    private static final String CONFIG_KEY_GUTTER = "GWC.gutter";

    private static final String CONFIG_KEY_GRIDSETS = "GWC.gridSets";

    private static final String CONFIG_KEY_METATILING_X = "GWC.metaTilingX";

    private static final String CONFIG_KEY_METATILING_Y = "GWC.metaTilingY";

    private static final String CONFIG_KEY_FORMATS = "GWC.cacheFormats";

    private static final String CONFIG_KEY_AUTO_CACHE_STYLES = "GWC.autoCacheStyles";

    private static final String CONFIG_KEY_CACHED_STYLES = "GWC.cachedNonDefaultStyles";

    private int gutter;

    private boolean enabled;

    private Set<String> cachedGridSetIds;

    private int metaTilingX;

    private int metaTilingY;

    private Set<String> mimeFormats;

    private boolean autoCacheStyles;

    private Set<String> cachedStyles;

    private boolean dirty;

    private static Set<String> unmarshalSet(final String listStr) {
        Set<String> unmarshalled = new HashSet<String>(Arrays.asList(listStr.split(",")));
        return unmarshalled;
    }

    private static String marshalList(final Collection<String> list) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<String> i = list.iterator(); i.hasNext();) {
            sb.append(i.next());
            if (i.hasNext()) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static GeoServerTileLayerInfo create(final LayerInfo layerInfo, final GWCConfig defaults) {
        ResourceInfo resourceInfo = layerInfo.getResource();

        MetadataMap metadataMap = layerInfo.getMetadata();
        GeoServerTileLayerInfo info = create(metadataMap, defaults);

        if (metadataMap.containsKey(CONFIG_KEY_FORMATS)) {
            String mimeFormatsStr = metadataMap.get(CONFIG_KEY_FORMATS, String.class);
            Set<String> mimeFormats = unmarshalSet(mimeFormatsStr);
            info.setMimeFormats(mimeFormats);
        } else if (resourceInfo instanceof FeatureTypeInfo) {
            info.setMimeFormats(defaults.getDefaultVectorCacheFormats());
            info.setDirty(true);
        } else if (resourceInfo instanceof CoverageInfo) {
            info.setMimeFormats(defaults.getDefaultCoverageCacheFormats());
            info.setDirty(true);
        } else {
            info.setMimeFormats(defaults.getDefaultOtherCacheFormats());
            info.setDirty(true);
        }

        boolean autoCacheStyles = defaults.isCacheNonDefaultStyles();
        if (metadataMap.containsKey(CONFIG_KEY_AUTO_CACHE_STYLES)) {
            autoCacheStyles = metadataMap.get(CONFIG_KEY_AUTO_CACHE_STYLES, Boolean.class)
                    .booleanValue();
        } else {
            info.setDirty(true);
        }
        info.setAutoCacheStyles(autoCacheStyles);

        info.setCachedStyles(Collections.EMPTY_SET);
        if (metadataMap.containsKey(CONFIG_KEY_CACHED_STYLES)) {
            String cachedStylesStr = metadataMap.get(CONFIG_KEY_CACHED_STYLES, String.class);
            Set<String> cachedStyles = unmarshalSet(cachedStylesStr);
            info.setCachedStyles(cachedStyles);
        } else {
            if (autoCacheStyles) {
                Set<StyleInfo> alternateStyles = layerInfo.getStyles();
                if (alternateStyles != null && alternateStyles.size() > 0) {
                    Set<String> cachedStyles = new HashSet<String>();
                    for (StyleInfo si : alternateStyles) {
                        if (si != null)
                            cachedStyles.add(si.getName());
                    }
                    info.setCachedStyles(cachedStyles);
                    info.setDirty(true);
                }
            }
        }

        return info;
    }

    @SuppressWarnings("unchecked")
    public static GeoServerTileLayerInfo create(final LayerGroupInfo layerInfo,
            final GWCConfig defaults) {

        MetadataMap metadataMap = layerInfo.getMetadata();
        GeoServerTileLayerInfo info = create(metadataMap, defaults);

        if (metadataMap.containsKey(CONFIG_KEY_FORMATS)) {
            String mimeFormatsStr = metadataMap.get(CONFIG_KEY_FORMATS, String.class);
            Set<String> mimeFormats = unmarshalSet(mimeFormatsStr);
            info.setMimeFormats(mimeFormats);
        } else {
            info.setMimeFormats(defaults.getDefaultOtherCacheFormats());
            info.setDirty(true);
        }

        info.setCachedStyles(Collections.EMPTY_SET);

        return info;
    }

    /**
     * Factory method based on a {@link LayerInfo}'s or {@link LayerGroupInfo}'s metadata map
     * 
     * @param resourceInfo
     * 
     * @param metadataMap
     * @return
     */
    private static GeoServerTileLayerInfo create(final MetadataMap metadataMap,
            final GWCConfig defaults) {

        GeoServerTileLayerInfo info = new GeoServerTileLayerInfo();
        // whether the config needs to be saved
        boolean dirty = false;
        boolean enabled = defaults.isCacheLayersByDefault();
        if (metadataMap.containsKey(CONFIG_KEY_ENABLED)) {
            enabled = metadataMap.get(CONFIG_KEY_ENABLED, Boolean.class).booleanValue();
        } else {
            dirty = true;
        }
        info.setEnabled(enabled);

        info.setGutter(defaults.getGutter());
        if (metadataMap.containsKey(CONFIG_KEY_GUTTER)) {
            int gutter = metadataMap.get(CONFIG_KEY_GUTTER, Integer.class).intValue();
            info.setGutter(gutter);
        } else {
            dirty = true;
        }

        info.setCachedGridSetIds(defaults.getDefaultCachingGridSetIds());
        if (metadataMap.containsKey(CONFIG_KEY_GRIDSETS)) {
            String gridsets = metadataMap.get(CONFIG_KEY_GRIDSETS, String.class);
            Set<String> gridSetIds = unmarshalSet(gridsets);
            info.setCachedGridSetIds(gridSetIds);
        } else {
            dirty = true;
        }

        info.setMetaTilingX(defaults.getMetaTilingX());
        info.setMetaTilingY(defaults.getMetaTilingY());
        if (metadataMap.containsKey(CONFIG_KEY_METATILING_X)) {
            info.setMetaTilingX(metadataMap.get(CONFIG_KEY_METATILING_X, Integer.class).intValue());
        } else {
            dirty = true;
        }
        if (metadataMap.containsKey(CONFIG_KEY_METATILING_Y)) {
            info.setMetaTilingY(metadataMap.get(CONFIG_KEY_METATILING_Y, Integer.class).intValue());
        } else {
            dirty = true;
        }

        info.setDirty(dirty);
        return info;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * @return whether the config needs to be saved because it's out of sync with the
     *         layer(group)info metadatamap
     */
    public boolean isDirty() {
        return dirty;
    }

    public void saveTo(MetadataMap metadata) {

        final boolean enabled = this.isEnabled();
        final int gutter = this.getGutter();
        final Set<String> cachedGridSetIds = this.getCachedGridSetIds();
        final int metaTilingX = this.getMetaTilingX();
        final int metaTilingY = this.getMetaTilingY();
        final Set<String> mimeFormats = this.getMimeFormats();
        final Set<String> cachedStyles = this.getCachedStyles();

        metadata.put(CONFIG_KEY_ENABLED, Boolean.valueOf(enabled));
        metadata.put(CONFIG_KEY_GUTTER, Integer.valueOf(gutter));
        metadata.put(CONFIG_KEY_GRIDSETS, marshalList(cachedGridSetIds));
        metadata.put(CONFIG_KEY_METATILING_X, Integer.valueOf(metaTilingX));
        metadata.put(CONFIG_KEY_METATILING_Y, Integer.valueOf(metaTilingY));
        metadata.put(CONFIG_KEY_FORMATS, marshalList(mimeFormats));
        metadata.put(CONFIG_KEY_AUTO_CACHE_STYLES, autoCacheStyles);

        if (cachedStyles.size() > 0) {
            metadata.put(CONFIG_KEY_CACHED_STYLES, marshalList(cachedStyles));
        }
    }

    public Set<String> getCachedStyles() {
        return cachedStyles;
    }

    public void setCachedStyles(Set<String> cachedStyles) {
        this.cachedStyles = new HashSet<String>(cachedStyles);
    }

    public Set<String> getMimeFormats() {
        return mimeFormats;
    }

    public void setMimeFormats(Set<String> mimeFormats) {
        this.mimeFormats = new HashSet<String>(mimeFormats);
    }

    public int getMetaTilingX() {
        return metaTilingX;
    }

    public int getMetaTilingY() {
        return metaTilingY;
    }

    public void setMetaTilingY(int metaTilingY) {
        Assert.isTrue(metaTilingY > 0);
        this.metaTilingY = metaTilingY;
    }

    public void setMetaTilingX(int metaTilingX) {
        Assert.isTrue(metaTilingX > 0);
        this.metaTilingX = metaTilingX;
    }

    public void setCachedGridSetIds(Set<String> cachedGridSetIds) {
        this.cachedGridSetIds = cachedGridSetIds;
    }

    @SuppressWarnings("unchecked")
    public Set<String> getCachedGridSetIds() {
        return cachedGridSetIds == null ? Collections.EMPTY_SET : cachedGridSetIds;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setGutter(int gutter) {
        this.gutter = gutter;
    }

    public int getGutter() {
        return gutter;
    }

    @Override
    public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    public boolean isAutoCacheStyles() {
        return autoCacheStyles;
    }

    public void setAutoCacheStyles(boolean autoCacheStyles) {
        this.autoCacheStyles = autoCacheStyles;
    }

}
