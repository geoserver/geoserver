/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.geoserver.ogcapi.APIConformance.Level.COMMUNITY_STANDARD;
import static org.geoserver.ogcapi.APIConformance.Level.STANDARD;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.ogcapi.APIConformance;
import org.geoserver.ogcapi.APIFilterParser;
import org.geoserver.ogcapi.CQL2Conformance;
import org.geoserver.ogcapi.ConformanceClass;
import org.geoserver.ogcapi.ConformanceInfo;
import org.geoserver.ogcapi.ECQLConformance;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;

/**
 * OGC API - Maps 1.0.0 conformance configuration, persisted per server in {@link WMSInfo} metadata; the GetFeatureInfo
 * class is a GeoServer extension outside the standard.
 */
@SuppressWarnings("serial")
public class MapsConformance extends ConformanceInfo<WMSInfo> {
    public static String METADATA_KEY = "ogcapiMaps";

    private static final String BASE = "https://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/";

    /** The {@link WMSInfo} metadata key holding the SVG renderer choice, see {@code WMS#getSvgRenderer()}. */
    private static final String SVG_RENDERER_KEY = "svgRenderer";

    public static final APIConformance CORE = new APIConformance(BASE + "core", STANDARD);
    public static final APIConformance COLLECTION_MAP = CORE.extend(BASE + "collection-map");
    public static final APIConformance STYLED_MAP = CORE.extend(BASE + "styled-map");
    public static final APIConformance HTML = CORE.extend(BASE + "html");
    public static final APIConformance API_OPERATIONS = CORE.extend(BASE + "api-operations");
    public static final APIConformance PNG = CORE.extend(BASE + "png");
    public static final APIConformance JPEG = CORE.extend(BASE + "jpeg");

    // configurable parameter and format classes; hyphenated ids need an explicit property matching the Java field,
    // since the default property (last id segment) would not resolve by reflection in ConformanceInfo
    public static final APIConformance SPATIAL_SUBSETTING = new APIConformance(
            BASE + "spatial-subsetting", STANDARD, APIConformance.Type.EXTENSION, CORE, "spatialSubsetting");
    public static final APIConformance SCALING = CORE.extend(BASE + "scaling");
    public static final APIConformance DISPLAY_RESOLUTION = new APIConformance(
            BASE + "display-resolution", STANDARD, APIConformance.Type.EXTENSION, CORE, "displayResolution");
    public static final APIConformance DATETIME = CORE.extend(BASE + "datetime");
    public static final APIConformance CRS = CORE.extend(BASE + "crs");
    public static final APIConformance BACKGROUND = CORE.extend(BASE + "background");
    public static final APIConformance ORIENTATION = CORE.extend(BASE + "orientation");
    public static final APIConformance TIFF = CORE.extend(BASE + "tiff");
    public static final APIConformance SVG = CORE.extend(BASE + "svg");
    public static final APIConformance GENERAL_SUBSETTING = new APIConformance(
            BASE + "general-subsetting", STANDARD, APIConformance.Type.EXTENSION, CORE, "generalSubsetting");

    /**
     * Attribute filtering is not part of OGC API - Maps 1.0.0; the Features Part 3 classes are reused because their
     * requirements are not bound to the features resource, and the Part 3 roadmap expects them to move to OGC API -
     * Common. The {@code features-filter} class is deliberately not declared, it is bound to
     * {@code /collections/{collectionId}/items}.
     */
    public static final APIConformance FILTER = CORE.extend(ConformanceClass.FILTER);

    /** Required by {@link #FILTER}, which has a hard dependency on the queryables class. */
    public static final APIConformance QUERYABLES = CORE.extend(ConformanceClass.QUERYABLES);

    /** GeoServer extension: binds the filter parameters to the map resources. */
    public static final APIConformance MAP_FILTER = new APIConformance(
            "http://geoserver.org/spec/ogcapi-maps/1.0/conf/map-filter",
            COMMUNITY_STANDARD,
            APIConformance.Type.EXTENSION,
            CORE,
            "mapFilter");

    /** GeoServer extension: WMS-style GetFeatureInfo on a map, not part of the OGC API - Maps standard. */
    public static final APIConformance FEATURE_INFO = new APIConformance(
            "http://geoserver.org/spec/ogcapi-maps/1.0/conf/featureinfo",
            COMMUNITY_STANDARD,
            APIConformance.Type.EXTENSION,
            CORE,
            "featureInfo");

    /** GeoServer extension: WMS-style GetLegendGraphic for a style, not part of the OGC API - Maps standard. */
    public static final APIConformance LEGEND = new APIConformance(
            "http://geoserver.org/spec/ogcapi-maps/1.0/conf/legend",
            COMMUNITY_STANDARD,
            APIConformance.Type.EXTENSION,
            CORE,
            "legend");

    private Boolean core = null;
    private Boolean spatialSubsetting = null;
    private Boolean scaling = null;
    private Boolean displayResolution = null;
    private Boolean datetime = null;
    private Boolean crs = null;
    private Boolean background = null;
    private Boolean orientation = null;
    private Boolean tiff = null;
    private Boolean svg = null;
    private Boolean generalSubsetting = null;
    private Boolean filter = null;
    private Boolean queryables = null;
    private Boolean mapFilter = null;
    private Boolean featureInfo = null;
    private Boolean legend = null;

    public MapsConformance() {}

    @Override
    public String getId() {
        return "maps";
    }

    @Override
    public boolean isEnabled(WMSInfo wmsInfo) {
        return core(wmsInfo);
    }

    /** Returns the maps configuration stored in the {@link WMSInfo} metadata, creating a default if absent. */
    public static MapsConformance configuration(WMSInfo wmsInfo) {
        if (wmsInfo.getMetadata().containsKey(METADATA_KEY)) {
            return (MapsConformance) wmsInfo.getMetadata().get(METADATA_KEY);
        }
        MapsConformance conf = new MapsConformance();
        wmsInfo.getMetadata().put(METADATA_KEY, conf);
        return conf;
    }

    @Override
    public List<APIConformance> configurableConformances() {
        return new ArrayList<>(List.of(
                SPATIAL_SUBSETTING,
                SCALING,
                DISPLAY_RESOLUTION,
                DATETIME,
                CRS,
                BACKGROUND,
                ORIENTATION,
                TIFF,
                SVG,
                GENERAL_SUBSETTING,
                FILTER,
                QUERYABLES,
                MAP_FILTER,
                FEATURE_INFO,
                LEGEND));
    }

    @Override
    public List<APIConformance> conformances(WMSInfo wmsInfo) {
        List<APIConformance> conformance = new ArrayList<>();
        if (!isEnabled(wmsInfo)) {
            return conformance;
        }
        // OGC API Common shared classes
        conformance.add(new APIConformance(ConformanceClass.CORE, STANDARD));
        conformance.add(new APIConformance(ConformanceClass.COLLECTIONS, STANDARD));
        // maps core baseline
        conformance.add(CORE);
        conformance.add(COLLECTION_MAP);
        conformance.add(STYLED_MAP);
        conformance.add(HTML);
        conformance.add(API_OPERATIONS);
        conformance.add(PNG);
        conformance.add(JPEG);
        // configurable classes
        if (spatialSubsetting(wmsInfo)) conformance.add(SPATIAL_SUBSETTING);
        if (scaling(wmsInfo)) conformance.add(SCALING);
        if (displayResolution(wmsInfo)) conformance.add(DISPLAY_RESOLUTION);
        if (datetime(wmsInfo)) conformance.add(DATETIME);
        if (crs(wmsInfo)) conformance.add(CRS);
        if (background(wmsInfo)) conformance.add(BACKGROUND);
        if (orientation(wmsInfo)) conformance.add(ORIENTATION);
        if (tiff(wmsInfo)) conformance.add(TIFF);
        if (svg(wmsInfo)) conformance.add(SVG);
        if (generalSubsetting(wmsInfo)) conformance.add(GENERAL_SUBSETTING);
        if (filtering(wmsInfo)) {
            conformance.add(FILTER);
            conformance.add(MAP_FILTER);
            if (queryables(wmsInfo)) conformance.add(QUERYABLES);
            conformance.addAll(ECQLConformance.configuration(wmsInfo).conformances(wmsInfo));
            conformance.addAll(CQL2Conformance.configuration(wmsInfo).conformances(wmsInfo));
        }
        if (featureInfo(wmsInfo)) conformance.add(FEATURE_INFO);
        if (legend(wmsInfo)) conformance.add(LEGEND);
        return conformance;
    }

    /** @return the core conformance flag, or {@code null} for the class default. */
    public Boolean isCore() {
        return core;
    }

    /**
     * Sets the core conformance flag; {@code null} restores the class default.
     *
     * @param enabled the flag value, or {@code null} for the default
     */
    public void setCore(Boolean enabled) {
        this.core = enabled;
    }

    /** @return {@code true} if core conformance is enabled, resolving {@code null} to the class default */
    public boolean core(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, core, CORE);
    }

    /** @return the spatial subsetting conformance flag, or {@code null} for the class default. */
    public Boolean isSpatialSubsetting() {
        return spatialSubsetting;
    }

    /**
     * Sets the spatial subsetting conformance flag; {@code null} restores the class default.
     *
     * @param enabled the flag value, or {@code null} for the default
     */
    public void setSpatialSubsetting(Boolean enabled) {
        this.spatialSubsetting = enabled;
    }

    /**
     * @return {@code true} if spatial subsetting conformance is enabled, resolving {@code null} to the class default
     */
    public boolean spatialSubsetting(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, spatialSubsetting, SPATIAL_SUBSETTING);
    }

    /** @return the scaling conformance flag, or {@code null} for the class default. */
    public Boolean isScaling() {
        return scaling;
    }

    /**
     * Sets the scaling conformance flag; {@code null} restores the class default.
     *
     * @param enabled the flag value, or {@code null} for the default
     */
    public void setScaling(Boolean enabled) {
        this.scaling = enabled;
    }

    /** @return {@code true} if scaling conformance is enabled, resolving {@code null} to the class default */
    public boolean scaling(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, scaling, SCALING);
    }

    /** @return the display resolution conformance flag, or {@code null} for the class default. */
    public Boolean isDisplayResolution() {
        return displayResolution;
    }

    /**
     * Sets the display resolution conformance flag; {@code null} restores the class default.
     *
     * @param enabled the flag value, or {@code null} for the default
     */
    public void setDisplayResolution(Boolean enabled) {
        this.displayResolution = enabled;
    }

    /**
     * @return {@code true} if display resolution conformance is enabled, resolving {@code null} to the class default
     */
    public boolean displayResolution(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, displayResolution, DISPLAY_RESOLUTION);
    }

    /** @return the date and time conformance flag, or {@code null} for the class default. */
    public Boolean isDatetime() {
        return datetime;
    }

    /**
     * Sets the date and time conformance flag; {@code null} restores the class default.
     *
     * @param enabled the flag value, or {@code null} for the default
     */
    public void setDatetime(Boolean enabled) {
        this.datetime = enabled;
    }

    /** @return {@code true} if date and time conformance is enabled, resolving {@code null} to the class default */
    public boolean datetime(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, datetime, DATETIME);
    }

    /** @return the CRS conformance flag, or {@code null} for the class default. */
    public Boolean isCrs() {
        return crs;
    }

    /**
     * Sets the CRS conformance flag; {@code null} restores the class default.
     *
     * @param enabled the flag value, or {@code null} for the default
     */
    public void setCrs(Boolean enabled) {
        this.crs = enabled;
    }

    /** @return {@code true} if CRS conformance is enabled, resolving {@code null} to the class default */
    public boolean crs(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, crs, CRS);
    }

    /** @return the background conformance flag, or {@code null} for the class default. */
    public Boolean isBackground() {
        return background;
    }

    /**
     * Sets the background conformance flag; {@code null} restores the class default.
     *
     * @param enabled the flag value, or {@code null} for the default
     */
    public void setBackground(Boolean enabled) {
        this.background = enabled;
    }

    /** @return {@code true} if background conformance is enabled, resolving {@code null} to the class default */
    public boolean background(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, background, BACKGROUND);
    }

    /** @return the orientation conformance flag, or {@code null} for the class default. */
    public Boolean isOrientation() {
        return orientation;
    }

    /**
     * Sets the orientation conformance flag; {@code null} restores the class default.
     *
     * @param enabled the flag value, or {@code null} for the default
     */
    public void setOrientation(Boolean enabled) {
        this.orientation = enabled;
    }

    /** @return {@code true} if orientation conformance is enabled, resolving {@code null} to the class default */
    public boolean orientation(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, orientation, ORIENTATION);
    }

    /** @return the TIFF conformance flag, or {@code null} for the class default. */
    public Boolean isTiff() {
        return tiff;
    }

    /**
     * Sets the TIFF conformance flag; {@code null} restores the class default.
     *
     * @param enabled the flag value, or {@code null} for the default
     */
    public void setTiff(Boolean enabled) {
        this.tiff = enabled;
    }

    /** @return {@code true} if TIFF conformance is enabled, resolving {@code null} to the class default */
    public boolean tiff(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, tiff, TIFF);
    }

    /** @return the SVG conformance flag, or {@code null} for the class default. */
    public Boolean isSvg() {
        return svg;
    }

    /**
     * Sets the SVG conformance flag; {@code null} restores the class default.
     *
     * @param enabled the flag value, or {@code null} for the default
     */
    public void setSvg(Boolean enabled) {
        this.svg = enabled;
    }

    /**
     * @return {@code true} if SVG conformance is enabled. With no explicit setting the class follows the WMS SVG
     *     renderer of this service: only the Batik one draws in a coordinate system running from 0,0 to the requested
     *     width and height, as {@code /req/svg/content} demands, while the default streaming renderer writes the world
     *     coordinates instead. Setting the flag turns the class on regardless, deviation included.
     */
    public boolean svg(WMSInfo wmsInfo) {
        if (svg == null) return WMS.SVG_BATIK.equals(wmsInfo.getMetadata().get(SVG_RENDERER_KEY));
        return isEnabled(wmsInfo, svg, SVG);
    }

    /** @return the general subsetting conformance flag, or {@code null} for the class default. */
    public Boolean isGeneralSubsetting() {
        return generalSubsetting;
    }

    /**
     * Sets the general subsetting conformance flag; {@code null} restores the class default.
     *
     * @param enabled the flag value, or {@code null} for the default
     */
    public void setGeneralSubsetting(Boolean enabled) {
        this.generalSubsetting = enabled;
    }

    /**
     * @return {@code true} if general subsetting conformance is enabled, resolving {@code null} to the class default
     */
    public boolean generalSubsetting(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, generalSubsetting, GENERAL_SUBSETTING);
    }

    /** @return the filter conformance flag, or {@code null} for the class default. */
    public Boolean isFilter() {
        return filter;
    }

    /**
     * Sets the filter conformance flag; {@code null} restores the class default.
     *
     * @param enabled the flag value, or {@code null} for the default
     */
    public void setFilter(Boolean enabled) {
        this.filter = enabled;
    }

    /** @return {@code true} if filter conformance is enabled, resolving {@code null} to the class default */
    public boolean filter(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, filter, FILTER);
    }

    /** @return the queryables conformance flag, or {@code null} for the class default. */
    public Boolean isQueryables() {
        return queryables;
    }

    /**
     * Sets the queryables conformance flag; {@code null} restores the class default.
     *
     * @param enabled the flag value, or {@code null} for the default
     */
    public void setQueryables(Boolean enabled) {
        this.queryables = enabled;
    }

    /** @return {@code true} if queryables conformance is enabled, resolving {@code null} to the class default */
    public boolean queryables(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, queryables, QUERYABLES);
    }

    /**
     * @return {@code true} if the filter parameters are available, which takes the standard {@link #FILTER} class, the
     *     {@link #MAP_FILTER} one binding it to the map resources, and at least one filter language, since OGC API -
     *     Features - Part 3 {@code /req/filter/filter-lang-param} makes the language classes part of the filter class
     */
    public boolean filtering(WMSInfo wmsInfo) {
        return filter(wmsInfo)
                && mapFilter(wmsInfo)
                && !APIFilterParser.enabledLanguages(wmsInfo).isEmpty();
    }

    /**
     * @return {@code true} if the queryables resource is available. On a map the queryables only describe what the
     *     filter parameters accept, so they follow {@link #filtering(WMSInfo)}
     */
    public boolean queryablesAvailable(WMSInfo wmsInfo) {
        return filtering(wmsInfo) && queryables(wmsInfo);
    }

    /** @return the map filter conformance flag, or {@code null} for the class default. */
    public Boolean isMapFilter() {
        return mapFilter;
    }

    /**
     * Sets the map filter conformance flag; {@code null} restores the class default.
     *
     * @param enabled the flag value, or {@code null} for the default
     */
    public void setMapFilter(Boolean enabled) {
        this.mapFilter = enabled;
    }

    /** @return {@code true} if map filter conformance is enabled, resolving {@code null} to the class default */
    public boolean mapFilter(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, mapFilter, MAP_FILTER);
    }

    /** @return the GetFeatureInfo conformance flag, or {@code null} for the class default. */
    public Boolean isFeatureInfo() {
        return featureInfo;
    }

    /**
     * Sets the GetFeatureInfo conformance flag; {@code null} restores the class default.
     *
     * @param enabled the flag value, or {@code null} for the default
     */
    public void setFeatureInfo(Boolean enabled) {
        this.featureInfo = enabled;
    }

    /** @return {@code true} if GetFeatureInfo conformance is enabled, resolving {@code null} to the class default */
    public boolean featureInfo(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, featureInfo, FEATURE_INFO);
    }

    public Boolean isLegend() {
        return legend;
    }

    public void setLegend(Boolean enabled) {
        this.legend = enabled;
    }

    public boolean legend(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, legend, LEGEND);
    }
}
