/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.gwc.config.GWCConfig;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.filter.parameters.FloatParameterFilter;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.filter.parameters.RegexParameterFilter;
import org.geowebcache.filter.parameters.StringParameterFilter;

/**
 * 
 */
public class TileLayerInfoUtil {

    public static GeoServerTileLayerInfo loadOrCreate(final CatalogInfo info,
            final GWCConfig defaults) {
        if (info instanceof LayerInfo) {
            return loadOrCreate((LayerInfo) info, defaults);
        }
        if (info instanceof LayerGroupInfo) {
            return loadOrCreate((LayerGroupInfo) info, defaults);
        }
        throw new IllegalArgumentException();
    }

    public static GeoServerTileLayerInfoImpl loadOrCreate(final LayerGroupInfo groupInfo,
            final GWCConfig defaults) {

        GeoServerTileLayerInfoImpl info = LegacyTileLayerInfoLoader.load(groupInfo);
        if (info == null) {
            info = create(defaults);
        }
        info.setName(groupInfo.getName());
        info.setId(groupInfo.getId());
        return info;
    }

    public static GeoServerTileLayerInfoImpl loadOrCreate(final LayerInfo layerInfo,
            final GWCConfig defaults) {
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

            checkStyles(layerInfo, info);
        }
        info.setName(layerInfo.getResource().getPrefixedName());
        info.setId(layerInfo.getId());
        return info;
    }

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

        return info;
    }

    private static void checkStyles(final LayerInfo layer, GeoServerTileLayerInfo layerInfo) {
        if (layerInfo.isAutoCacheStyles() && layer.getStyles() != null
                && layer.getStyles().size() > 0) {

            if (null == findParameterFilter("STYLES", layerInfo.getParameterFilters())) {

                String defaultStyle = layer.getDefaultStyle() == null ? null : layer
                        .getDefaultStyle().getName();
                Set<String> cachedStyles = new HashSet<String>();
                for (StyleInfo s : layer.getStyles()) {
                    cachedStyles.add(s.getName());
                }
                setCachedStyles(layerInfo, defaultStyle, cachedStyles);
            }
        }
    }

    public static ParameterFilter findParameterFilter(final String paramName,
            Set<ParameterFilter> parameterFilters) {

        if (parameterFilters == null || parameterFilters.size() == 0) {
            return null;
        }

        for (ParameterFilter pf : parameterFilters) {
            if (paramName.equalsIgnoreCase(pf.getKey())) {
                return pf;
            }
        }
        return null;
    }

    public static void setCachedStyles(GeoServerTileLayerInfo info, String defaultStyle,
            Set<String> cachedStyles) {
        updateStringParameterFilter(info, "STYLES", true, defaultStyle, cachedStyles);
    }

    public static void updateStringParameterFilter(final GeoServerTileLayerInfo tileLayerInfo,
            final String paramKey, boolean createParam, final String defaultValue,
            final String... allowedValues) {

        Set<String> validValues = new HashSet<String>();
        if (allowedValues != null) {
            validValues.addAll(Arrays.asList(allowedValues));
        }
        updateStringParameterFilter(tileLayerInfo, paramKey, createParam, defaultValue, validValues);
    }

    public static void updateStringParameterFilter(final GeoServerTileLayerInfo tileLayerInfo,
            final String paramKey, boolean createParam, final String defaultValue,
            final Set<String> allowedValues) {

        removeParameterFilter(tileLayerInfo, paramKey);

        if (createParam && allowedValues != null && allowedValues.size() > 0) {
            // make sure default value is among the list of allowed values
            Set<String> values = new TreeSet<String>(allowedValues);

            StringParameterFilter stringListFilter = new StringParameterFilter();
            stringListFilter.setKey(paramKey);
            stringListFilter.setDefaultValue(defaultValue == null ? "" : defaultValue);
            stringListFilter.getValues().addAll(values);
            tileLayerInfo.getParameterFilters().add(stringListFilter);
        }
    }

    private static void removeParameterFilter(final GeoServerTileLayerInfo tileLayerInfo,
            final String paramKey) {
        Set<ParameterFilter> parameterFilters = tileLayerInfo.getParameterFilters();
        for (Iterator<? extends ParameterFilter> it = parameterFilters.iterator(); it.hasNext();) {
            if (paramKey.equalsIgnoreCase(it.next().getKey())) {
                it.remove();
                break;
            }
        }
    }

    public static void updateAcceptAllRegExParameterFilter(
            final GeoServerTileLayerInfo tileLayerInfo, final String paramKey, boolean createParam) {

        Set<ParameterFilter> parameterFilters = tileLayerInfo.getParameterFilters();
        for (Iterator<? extends ParameterFilter> it = parameterFilters.iterator(); it.hasNext();) {
            ParameterFilter parameterFilter = it.next();
            String key = parameterFilter.getKey();
            if (paramKey.equalsIgnoreCase(key)) {
                it.remove();
                break;
            }
        }
        if (createParam) {
            RegexParameterFilter filter = new RegexParameterFilter();
            filter.setKey(paramKey);
            filter.setDefaultValue("");
            filter.setRegex(".*");
            tileLayerInfo.getParameterFilters().add(filter);
        }
    }

    public static void updateAcceptAllFloatParameterFilter(final GeoServerTileLayerInfo info,
            final String paramKey, boolean createParam) {

        Set<ParameterFilter> parameterFilters = info.getParameterFilters();
        for (Iterator<? extends ParameterFilter> it = parameterFilters.iterator(); it.hasNext();) {
            ParameterFilter parameterFilter = it.next();
            String key = parameterFilter.getKey();
            if (paramKey.equalsIgnoreCase(key)) {
                it.remove();
                break;
            }
        }
        if (createParam) {
            FloatParameterFilter filter = new FloatParameterFilter();
            filter.setKey(paramKey);
            filter.setDefaultValue("");
            info.getParameterFilters().add(filter);
        }
    }

}
