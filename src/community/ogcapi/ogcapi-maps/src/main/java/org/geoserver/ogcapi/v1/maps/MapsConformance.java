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
 *
 * <p>Every class has three accessors: {@code isXyz()} and {@code setXyz(Boolean)} carry the configured flag, where
 * {@code null} means the class default, while {@code xyz(WMSInfo)} resolves the flag against that default. Only the
 * ones deciding more than that carry Javadoc of their own.
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
    public static final APIConformance DATASET_MAP =
            new APIConformance(BASE + "dataset-map", STANDARD, APIConformance.Type.EXTENSION, CORE, "datasetMap");
    public static final APIConformance COLLECTIONS_SELECTION = new APIConformance(
            BASE + "collections-selection", STANDARD, APIConformance.Type.EXTENSION, CORE, "collectionsSelection");
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
    private Boolean datasetMap = null;
    private Boolean collectionsSelection = null;
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
                DATASET_MAP,
                COLLECTIONS_SELECTION,
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
        if (datasetMap(wmsInfo)) {
            conformance.add(DATASET_MAP);
            if (collectionsSelection(wmsInfo)) conformance.add(COLLECTIONS_SELECTION);
        }
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

    public Boolean isCore() {
        return core;
    }

    public void setCore(Boolean enabled) {
        this.core = enabled;
    }

    public boolean core(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, core, CORE);
    }

    public Boolean isDatasetMap() {
        return datasetMap;
    }

    public void setDatasetMap(Boolean enabled) {
        this.datasetMap = enabled;
    }

    public boolean datasetMap(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, datasetMap, DATASET_MAP);
    }

    public Boolean isCollectionsSelection() {
        return collectionsSelection;
    }

    public void setCollectionsSelection(Boolean enabled) {
        this.collectionsSelection = enabled;
    }

    /** Selecting the collections of a map means nothing without the dataset map class it selects the contents of. */
    public boolean collectionsSelection(WMSInfo wmsInfo) {
        return datasetMap(wmsInfo) && isEnabled(wmsInfo, collectionsSelection, COLLECTIONS_SELECTION);
    }

    public Boolean isSpatialSubsetting() {
        return spatialSubsetting;
    }

    public void setSpatialSubsetting(Boolean enabled) {
        this.spatialSubsetting = enabled;
    }

    public boolean spatialSubsetting(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, spatialSubsetting, SPATIAL_SUBSETTING);
    }

    public Boolean isScaling() {
        return scaling;
    }

    public void setScaling(Boolean enabled) {
        this.scaling = enabled;
    }

    public boolean scaling(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, scaling, SCALING);
    }

    public Boolean isDisplayResolution() {
        return displayResolution;
    }

    public void setDisplayResolution(Boolean enabled) {
        this.displayResolution = enabled;
    }

    public boolean displayResolution(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, displayResolution, DISPLAY_RESOLUTION);
    }

    public Boolean isDatetime() {
        return datetime;
    }

    public void setDatetime(Boolean enabled) {
        this.datetime = enabled;
    }

    public boolean datetime(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, datetime, DATETIME);
    }

    public Boolean isCrs() {
        return crs;
    }

    public void setCrs(Boolean enabled) {
        this.crs = enabled;
    }

    public boolean crs(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, crs, CRS);
    }

    public Boolean isBackground() {
        return background;
    }

    public void setBackground(Boolean enabled) {
        this.background = enabled;
    }

    public boolean background(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, background, BACKGROUND);
    }

    public Boolean isOrientation() {
        return orientation;
    }

    public void setOrientation(Boolean enabled) {
        this.orientation = enabled;
    }

    public boolean orientation(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, orientation, ORIENTATION);
    }

    public Boolean isTiff() {
        return tiff;
    }

    public void setTiff(Boolean enabled) {
        this.tiff = enabled;
    }

    public boolean tiff(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, tiff, TIFF);
    }

    public Boolean isSvg() {
        return svg;
    }

    public void setSvg(Boolean enabled) {
        this.svg = enabled;
    }

    /**
     * With no explicit setting the class follows the WMS SVG renderer of this service: only the Batik one draws in a
     * coordinate system running from 0,0 to the requested width and height, as {@code /req/svg/content} demands, while
     * the default streaming renderer writes the world coordinates instead. Setting the flag turns the class on
     * regardless, deviation included.
     */
    public boolean svg(WMSInfo wmsInfo) {
        if (svg == null) return WMS.SVG_BATIK.equals(wmsInfo.getMetadata().get(SVG_RENDERER_KEY));
        return isEnabled(wmsInfo, svg, SVG);
    }

    public Boolean isGeneralSubsetting() {
        return generalSubsetting;
    }

    public void setGeneralSubsetting(Boolean enabled) {
        this.generalSubsetting = enabled;
    }

    public boolean generalSubsetting(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, generalSubsetting, GENERAL_SUBSETTING);
    }

    public Boolean isFilter() {
        return filter;
    }

    public void setFilter(Boolean enabled) {
        this.filter = enabled;
    }

    public boolean filter(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, filter, FILTER);
    }

    public Boolean isQueryables() {
        return queryables;
    }

    public void setQueryables(Boolean enabled) {
        this.queryables = enabled;
    }

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

    public Boolean isMapFilter() {
        return mapFilter;
    }

    public void setMapFilter(Boolean enabled) {
        this.mapFilter = enabled;
    }

    public boolean mapFilter(WMSInfo wmsInfo) {
        return isEnabled(wmsInfo, mapFilter, MAP_FILTER);
    }

    public Boolean isFeatureInfo() {
        return featureInfo;
    }

    public void setFeatureInfo(Boolean enabled) {
        this.featureInfo = enabled;
    }

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
