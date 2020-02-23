/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static org.geoserver.gwc.GWC.tileLayerName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geotools.util.logging.Logging;
import org.geowebcache.config.XMLGridSubset;

/**
 * Backwards (<= 2.1.3) compatible {@link GeoServerTileLayerInfoImpl} loader.
 *
 * @author groldan
 */
public class LegacyTileLayerInfoLoader {

    private static final Logger LOGGER = Logging.getLogger(LegacyTileLayerInfoLoader.class);

    public static final String CONFIG_KEY_ENABLED = "GWC.enabled";

    public static final String CONFIG_KEY_GUTTER = "GWC.gutter";

    public static final String CONFIG_KEY_GRIDSETS = "GWC.gridSets";

    public static final String CONFIG_KEY_METATILING_X = "GWC.metaTilingX";

    public static final String CONFIG_KEY_METATILING_Y = "GWC.metaTilingY";

    public static final String CONFIG_KEY_FORMATS = "GWC.cacheFormats";

    public static final String CONFIG_KEY_AUTO_CACHE_STYLES = "GWC.autoCacheStyles";

    public static final String CONFIG_KEY_CACHED_STYLES = "GWC.cachedNonDefaultStyles";

    public static final String CONFIG_KEY_IN_MEMORY_CACHED = "GWC.inMemoryUncached";

    public static final String[] _ALL_KEYS = {
        CONFIG_KEY_ENABLED,
        CONFIG_KEY_GUTTER,
        CONFIG_KEY_GRIDSETS,
        CONFIG_KEY_METATILING_X,
        CONFIG_KEY_METATILING_Y,
        CONFIG_KEY_FORMATS,
        CONFIG_KEY_AUTO_CACHE_STYLES,
        CONFIG_KEY_CACHED_STYLES,
        CONFIG_KEY_IN_MEMORY_CACHED
    };

    public static GeoServerTileLayerInfoImpl load(final LayerInfo layer) {
        MetadataMap metadataMap = layer.getMetadata();

        if (!hasTileLayerDef(metadataMap)) {
            return null;
        }
        GeoServerTileLayerInfoImpl tileLayerInfo = load(metadataMap);

        if (metadataMap.containsKey(CONFIG_KEY_CACHED_STYLES)) {
            final String defaultStyle =
                    layer.getDefaultStyle() == null ? "" : layer.getDefaultStyle().prefixedName();
            String cachedStylesStr = metadataMap.get(CONFIG_KEY_CACHED_STYLES, String.class);
            Set<String> cachedStyles = unmarshalSet(cachedStylesStr);
            TileLayerInfoUtil.setCachedStyles(tileLayerInfo, defaultStyle, cachedStyles);
        }

        TileLayerInfoUtil.checkAutomaticStyles(layer, tileLayerInfo);
        tileLayerInfo.setName(tileLayerName(layer));
        tileLayerInfo.setId(layer.getId());
        return tileLayerInfo;
    }

    public static boolean hasTileLayerDef(MetadataMap metadataMap) {
        return metadataMap.containsKey(CONFIG_KEY_ENABLED);
    }

    public static GeoServerTileLayerInfoImpl load(final LayerGroupInfo layerGroup) {
        MetadataMap metadataMap = layerGroup.getMetadata();
        if (!hasTileLayerDef(metadataMap)) {
            return null;
        }
        GeoServerTileLayerInfoImpl tileLayerInfo = load(metadataMap);
        if (tileLayerInfo != null) {
            tileLayerInfo.setName(tileLayerName(layerGroup));
            tileLayerInfo.setId(layerGroup.getId());
        }
        TileLayerInfoUtil.checkAutomaticStyles(layerGroup, tileLayerInfo);
        return tileLayerInfo;
    }

    private static GeoServerTileLayerInfoImpl load(final MetadataMap metadataMap) {

        GeoServerTileLayerInfoImpl info = new GeoServerTileLayerInfoImpl();
        // whether the config needs to be saved

        final boolean enabled = metadataMap.get(CONFIG_KEY_ENABLED, Boolean.class).booleanValue();
        info.setEnabled(enabled);

        int gutter = metadataMap.get(CONFIG_KEY_GUTTER, Integer.class).intValue();
        info.setGutter(gutter);

        String gridsets = metadataMap.get(CONFIG_KEY_GRIDSETS, String.class);
        Set<XMLGridSubset> gridSetIds = unmarshalGridSubsets(gridsets);
        info.getGridSubsets().addAll(gridSetIds);

        int metaTilingX = metadataMap.get(CONFIG_KEY_METATILING_X, Integer.class).intValue();
        info.setMetaTilingX(metaTilingX);

        int metaTilingY = metadataMap.get(CONFIG_KEY_METATILING_Y, Integer.class).intValue();
        info.setMetaTilingY(metaTilingY);

        if (metadataMap.containsKey(CONFIG_KEY_FORMATS)) {
            String mimeFormatsStr = metadataMap.get(CONFIG_KEY_FORMATS, String.class);
            Set<String> mimeFormats = unmarshalSet(mimeFormatsStr);
            info.getMimeFormats().addAll(mimeFormats);
        }

        if (metadataMap.containsKey(CONFIG_KEY_AUTO_CACHE_STYLES)) {
            boolean autoCacheStyles =
                    metadataMap.get(CONFIG_KEY_AUTO_CACHE_STYLES, Boolean.class).booleanValue();
            info.setAutoCacheStyles(autoCacheStyles);
        }

        if (metadataMap.containsKey(CONFIG_KEY_IN_MEMORY_CACHED)) {
            boolean inMemoryCached = metadataMap.get(CONFIG_KEY_IN_MEMORY_CACHED, Boolean.class);
            info.setInMemoryCached(inMemoryCached);
        }

        return info;
    }

    private static Set<String> unmarshalSet(final String listStr) {
        Set<String> unmarshalled = new HashSet<String>(Arrays.asList(listStr.split(",")));
        return unmarshalled;
    }

    private static String marshalList(final Collection<String> list) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<String> i = list.iterator(); i.hasNext(); ) {
            sb.append(i.next());
            if (i.hasNext()) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    /**
     * @param gridSubsetsStr comma separated list of epsg codes (usually just {@code
     *     EPSG:900913,EPSG:4326}
     * @return the list of parsed grid subsets from the argument JSON array
     * @throws IllegalArgumentException if {@code str} can't be parsed to a JSONArray
     */
    private static Set<XMLGridSubset> unmarshalGridSubsets(String gridSubsetsStr)
            throws IllegalArgumentException {

        Set<XMLGridSubset> gridSubsets = new HashSet<XMLGridSubset>();
        // backwards compatibility check for when str comes in as "EPSG:XXX,EPSG:YYY"
        String[] epsgCodes = gridSubsetsStr.split(",");
        for (String code : epsgCodes) {
            if (code.trim().length() == 0) {
                continue;
            }
            try {
                XMLGridSubset xmlGridSubset = new XMLGridSubset();
                xmlGridSubset.setGridSetName(code);
                gridSubsets.add(xmlGridSubset);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Invalid GridSubset list: " + gridSubsetsStr);
            }
        }

        gridSubsets.remove(null);
        return gridSubsets;
    }

    public static void clear(MetadataMap metadata) {
        if (metadata != null) {
            for (String key : LegacyTileLayerInfoLoader._ALL_KEYS) {
                metadata.remove(key);
            }
        }
    }

    /**
     * Saves a tile layer info into the given metadata map using the old legacy metadata elements.
     * For unit testing only.
     */
    public static void save(GeoServerTileLayerInfo source, MetadataMap metadata) {
        final boolean enabled = source.isEnabled();
        final int gutter = source.getGutter();
        final Set<XMLGridSubset> cachedGridSubsets = source.getGridSubsets();
        final int metaTilingX = source.getMetaTilingX();
        final int metaTilingY = source.getMetaTilingY();
        final Set<String> mimeFormats = source.getMimeFormats();
        final Boolean autoCacheStyles = source.isAutoCacheStyles();
        final Set<String> cachedStyles = source.cachedStyles();
        final boolean inMemoryCached = source.isInMemoryCached();

        metadata.put(CONFIG_KEY_ENABLED, Boolean.valueOf(enabled));
        metadata.put(CONFIG_KEY_GUTTER, Integer.valueOf(gutter));
        Collection<String> subsetNames = new ArrayList<String>();
        for (XMLGridSubset s : cachedGridSubsets) {
            subsetNames.add(s.getGridSetName());
        }
        metadata.put(CONFIG_KEY_GRIDSETS, marshalList(subsetNames));
        metadata.put(CONFIG_KEY_METATILING_X, Integer.valueOf(metaTilingX));
        metadata.put(CONFIG_KEY_METATILING_Y, Integer.valueOf(metaTilingY));
        metadata.put(CONFIG_KEY_FORMATS, marshalList(mimeFormats));
        metadata.put(CONFIG_KEY_AUTO_CACHE_STYLES, autoCacheStyles);
        metadata.put(CONFIG_KEY_IN_MEMORY_CACHED, inMemoryCached);

        if (cachedStyles.size() > 0) {
            metadata.put(CONFIG_KEY_CACHED_STYLES, marshalList(cachedStyles));
        } else {
            metadata.remove(CONFIG_KEY_CACHED_STYLES);
        }
    }
}
