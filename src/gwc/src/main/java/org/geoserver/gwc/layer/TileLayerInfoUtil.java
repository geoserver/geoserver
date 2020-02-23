/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static org.geoserver.gwc.GWC.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.gwc.config.GWCConfig;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.filter.parameters.FloatParameterFilter;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.filter.parameters.RegexParameterFilter;
import org.geowebcache.filter.parameters.StringParameterFilter;

/** Utility methods for manipulating {@link GeoServerTileLayerInfo}s */
public class TileLayerInfoUtil {

    /**
     * Creates a cached tile layer from the given Layer or Layer Group
     *
     * @param info a Layerinfo or LayerGroupInfo
     * @param defaults default configuration
     */
    public static GeoServerTileLayerInfo loadOrCreate(
            final CatalogInfo info, final GWCConfig defaults) {
        if (info instanceof LayerInfo) {
            return loadOrCreate((LayerInfo) info, defaults);
        }
        if (info instanceof LayerGroupInfo) {
            return loadOrCreate((LayerGroupInfo) info, defaults);
        }
        throw new IllegalArgumentException();
    }

    /**
     * Creates a cached tile layer from the given Layer Group
     *
     * @param groupInfo the layer group to cache
     * @param defaults default configuration
     */
    public static GeoServerTileLayerInfoImpl loadOrCreate(
            final LayerGroupInfo groupInfo, final GWCConfig defaults) {

        GeoServerTileLayerInfoImpl info = LegacyTileLayerInfoLoader.load(groupInfo);
        if (info == null) {
            info = create(defaults);
            checkAutomaticStyles(groupInfo, info);
        }
        info.setName(tileLayerName(groupInfo));
        info.setId(groupInfo.getId());
        return info;
    }

    /**
     * Creates a cached tile layer from the given Layer
     *
     * @param layerInfo the layer to cache
     * @param defaults default configuration
     */
    public static GeoServerTileLayerInfoImpl loadOrCreate(
            final LayerInfo layerInfo, final GWCConfig defaults) {
        GeoServerTileLayerInfoImpl info = LegacyTileLayerInfoLoader.load(layerInfo);
        if (info == null) {
            info = create(defaults);
            final ResourceInfo resource = layerInfo.getResource();
            if (resource instanceof FeatureTypeInfo) {
                info.getMimeFormats().clear();
                info.getMimeFormats().addAll(defaults.getDefaultVectorCacheFormats());
            } else if (resource instanceof CoverageInfo) {
                info.getMimeFormats().clear();
                info.getMimeFormats().addAll(defaults.getDefaultCoverageCacheFormats());
            }

            checkAutomaticStyles(layerInfo, info);
        }
        info.setName(tileLayerName(layerInfo));
        info.setId(layerInfo.getId());
        return info;
    }

    // FIXME I think this only needs to be package visible for the tests to work.
    /**
     * Creates a default tile layer info based on the global defaults, public only for unit testing
     * purposes.
     */
    public static GeoServerTileLayerInfoImpl create(GWCConfig defaults) {

        GeoServerTileLayerInfoImpl info = new GeoServerTileLayerInfoImpl();

        info.setEnabled(defaults.isCacheLayersByDefault());
        info.setAutoCacheStyles(defaults.isCacheNonDefaultStyles());

        for (String gsetId : defaults.getDefaultCachingGridSetIds()) {
            XMLGridSubset subset = new XMLGridSubset();
            subset.setGridSetName(gsetId);
            info.getGridSubsets().add(subset);
        }

        info.getMimeFormats().addAll(defaults.getDefaultOtherCacheFormats());

        info.setGutter(defaults.getGutter());
        info.setMetaTilingX(defaults.getMetaTilingX());
        info.setMetaTilingY(defaults.getMetaTilingY());
        info.setInMemoryCached(true);

        return info;
    }

    public static void addAutoStyleParameterFilter(
            final LayerInfo layer, GeoServerTileLayerInfo layerInfo) {
        StyleParameterFilter filter = new StyleParameterFilter();
        filter.setLayer(layer);
        layerInfo.removeParameterFilter("STYLES");
        layerInfo.getParameterFilters().add(filter);
    }

    /**
     * If the layer is configured for automatic style updates of its Style parameter filter, do so.
     *
     * @param published The GeoServer layer
     * @param layerInfo The GeoWebCache layer
     */
    public static void checkAutomaticStyles(
            final PublishedInfo published, GeoServerTileLayerInfo layerInfo) {
        if (published instanceof LayerInfo) {
            checkAutomaticStyles((LayerInfo) published, layerInfo);
        } else if (published instanceof LayerGroupInfo) {
            checkAutomaticStyles((LayerGroupInfo) published, layerInfo);
        } else {
            throw new IllegalArgumentException(
                    "Do not know how to handle this kind of PublishedInfo: " + published);
        }
    }

    /**
     * If the layer is configured for automatic style updates of its Style parameter filter, do so.
     *
     * @param layer The GeoServer layer
     * @param layerInfo The GeoWebCache layer
     */
    public static void checkAutomaticStyles(
            final LayerInfo layer, GeoServerTileLayerInfo layerInfo) {

        ParameterFilter filter = layerInfo.getParameterFilter("STYLES");

        // Update the filter with the latest available styles if it's a style filter
        if (filter != null && filter instanceof StyleParameterFilter) {
            ((StyleParameterFilter) filter).setLayer(layer);
        }
    }

    /**
     * If the layer is configured for automatic style updates of its Style parameter filter, do so.
     *
     * @param layer The GeoServer layer group
     * @param layerInfo The GeoWebCache layer
     */
    public static void checkAutomaticStyles(
            final LayerGroupInfo layer, GeoServerTileLayerInfo layerInfo) {

        ParameterFilter filter = layerInfo.getParameterFilter("STYLES");

        // Remove the filter as groups shouldn't have auto-updating styles
        if (filter != null && filter instanceof StyleParameterFilter) {
            layerInfo.removeParameterFilter("STYLES");
        }
    }

    /** Set the styles which should be cached on a layer */
    public static void setCachedStyles(
            GeoServerTileLayerInfo info, String defaultStyle, Set<String> cachedStyles) {
        StyleParameterFilter filter = (StyleParameterFilter) info.getParameterFilter("STYLES");
        if (filter == null) filter = new StyleParameterFilter();

        filter.setDefaultValue(defaultStyle);
        filter.setStyles(cachedStyles);
        info.addParameterFilter(filter);
    }

    /**
     * Replace a filter with a new {@link StringParameterFilter}.
     *
     * @param tileLayerInfo layer to update the filter on
     * @param paramKey key for the parameter
     * @param createParam create a new filter if there is none to replace for the specified key
     * @param defaultValue default value
     * @param allowedValues legal values for the parameter
     */
    public static void updateStringParameterFilter(
            final GeoServerTileLayerInfo tileLayerInfo,
            final String paramKey,
            boolean createParam,
            final String defaultValue,
            final String... allowedValues) {

        Set<String> validValues = new HashSet<String>();
        if (allowedValues != null) {
            validValues.addAll(Arrays.asList(allowedValues));
        }
        updateStringParameterFilter(
                tileLayerInfo, paramKey, createParam, defaultValue, validValues);
    }

    /**
     * Add a {@link StringParameterFilter} to the layer, replacing any existing filter for the same
     * parameter.
     *
     * @param tileLayerInfo layer to update the filter on
     * @param paramKey key for the parameter
     * @param createParam create a new filter if there is none to replace for the specified key
     * @param defaultValue default value
     * @param allowedValues legal values for the parameter
     */
    public static void updateStringParameterFilter(
            final GeoServerTileLayerInfo tileLayerInfo,
            final String paramKey,
            boolean createParam,
            final String defaultValue,
            final Set<String> allowedValues) {

        createParam |= tileLayerInfo.removeParameterFilter(paramKey);

        if (createParam && allowedValues != null && allowedValues.size() > 0) {
            // make sure default value is among the list of allowed values
            Set<String> values = new TreeSet<String>(allowedValues);

            StringParameterFilter stringListFilter = new StringParameterFilter();
            stringListFilter.setKey(paramKey);
            stringListFilter.setDefaultValue(defaultValue == null ? "" : defaultValue);
            values.addAll(stringListFilter.getValues());
            stringListFilter.setValues(new ArrayList<String>(values));
            tileLayerInfo.addParameterFilter(stringListFilter);
        }
    }

    /**
     * Add a {@link RegexParameterFilter} set accept anything, replacing any existing filter for the
     * same parameter.
     *
     * @param tileLayerInfo layer to update the filter on
     * @param paramKey key for the parameter
     * @param createParam create a new filter if there is none to replace for the specified key
     */
    public static void updateAcceptAllRegExParameterFilter(
            final GeoServerTileLayerInfo tileLayerInfo,
            final String paramKey,
            boolean createParam) {

        createParam |= tileLayerInfo.removeParameterFilter(paramKey);

        if (createParam) {
            RegexParameterFilter filter = new RegexParameterFilter();
            filter.setKey(paramKey);
            filter.setDefaultValue("");
            filter.setRegex(".*");
            tileLayerInfo.addParameterFilter(filter);
        }
    }

    /**
     * Add a {@link FloatParameterFilter} set accept anything, replacing any existing filter for the
     * same parameter.
     *
     * @param tileLayerInfo layer to update the filter on
     * @param paramKey key for the parameter
     * @param createParam create a new filter if there is none to replace for the specified key
     */
    public static void updateAcceptAllFloatParameterFilter(
            final GeoServerTileLayerInfo tileLayerInfo,
            final String paramKey,
            boolean createParam) {

        createParam |= tileLayerInfo.removeParameterFilter(paramKey);

        if (createParam) {
            FloatParameterFilter filter = new FloatParameterFilter();
            filter.setKey(paramKey);
            filter.setDefaultValue("");
            tileLayerInfo.addParameterFilter(filter);
        }
    }
}
