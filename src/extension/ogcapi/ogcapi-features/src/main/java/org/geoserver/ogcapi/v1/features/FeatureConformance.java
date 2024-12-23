/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static org.geoserver.ogcapi.APIConformance.Level.DRAFT_STANDARD;
import static org.geoserver.ogcapi.APIConformance.Level.STANDARD;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.ogcapi.APIConformance;
import org.geoserver.ogcapi.ConformanceClass;
import org.geoserver.ogcapi.ConformanceInfo;
import org.geoserver.wfs.WFSInfo;

/** FeatureService configuration. */
@SuppressWarnings("serial")
public class FeatureConformance extends ConformanceInfo<WFSInfo> {
    public static String METADATA_KEY = "ogcapiFeatures";

    public static final APIConformance CORE =
            new APIConformance("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core", STANDARD);

    // required resource formats
    public static final APIConformance HTML =
            CORE.extend("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/html");
    public static final APIConformance GEOJSON =
            CORE.extend("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/geojson");
    public static final APIConformance OAS30 =
            CORE.extend("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/oas30");

    // optional output format from WFS
    public static final APIConformance GML321 =
            new APIConformance("http://schemas.opengis.net/gml/3.2.1/gml.xsd", STANDARD);

    // not-implemented resource formats
    public static final APIConformance GMLSF0 =
            CORE.extend("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/gmlsf0");
    public static final APIConformance GMLSF2 =
            CORE.extend("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/gmlsf2");

    // optional
    public static final APIConformance CRS_BY_REFERENCE =
            CORE.extend("http://www.opengis.net/spec/ogcapi-features-2/1.0/conf/crs");
    public static final APIConformance FEATURES_FILTER = CORE.extend(ConformanceClass.FEATURES_FILTER);
    public static final APIConformance FILTER = CORE.extend(ConformanceClass.FILTER);
    public static final APIConformance QUERYABLES = CORE.extend(ConformanceClass.QUERYABLES);

    // draft
    public static final APIConformance IDS = new APIConformance(ConformanceClass.IDS, DRAFT_STANDARD);
    public static final APIConformance SEARCH = new APIConformance(ConformanceClass.SEARCH, DRAFT_STANDARD);
    public static final APIConformance SORTBY = new APIConformance(ConformanceClass.SORTBY, DRAFT_STANDARD);

    private Boolean core = null;
    private Boolean gml321 = null;
    private Boolean gmlSF0 = null;
    private Boolean gmlSF2 = null;
    private Boolean featuresFilter = null;
    private Boolean crsByReference = null;
    private Boolean filter = null;
    private Boolean search = null;
    private Boolean queryables = null;
    private Boolean ids = null;
    private Boolean sortBy = null;

    /** Configuration for OGCAPI Features. */
    public FeatureConformance() {}

    /**
     * Requires CORE to be enabled.
     *
     * @param wfsInfo
     * @return requires core to be enabled.
     */
    @Override
    public boolean isEnabled(WFSInfo wfsInfo) {
        return core(wfsInfo);
    }

    /**
     * Obtain FeatureService configuration for WFSInfo.
     *
     * <p>Uses configuration stored in metadata map, or creates default if needed.
     *
     * @param wfsInfo WFSService configuration
     * @return Feature Service configuration
     */
    public static FeatureConformance configuration(WFSInfo wfsInfo) {
        if (wfsInfo.getMetadata().containsKey(METADATA_KEY)) {
            return (FeatureConformance) wfsInfo.getMetadata().get(METADATA_KEY);
        } else {
            FeatureConformance conf = new FeatureConformance();
            wfsInfo.getMetadata().put(METADATA_KEY, conf);
            return conf;
        }
    }

    /**
     * Configuration for FeatureService CORE functionality and extensions.
     *
     * @param serviceInfo WFSService configuration
     * @return List of enabled conformance
     */
    @Override
    public List<APIConformance> conformances(WFSInfo serviceInfo) {
        List<APIConformance> conformance = new ArrayList<>();
        if (isEnabled(serviceInfo)) {
            // built-in rquired conformance classes
            conformance.add(FeatureConformance.CORE);
            conformance.add(FeatureConformance.OAS30);
            conformance.add(FeatureConformance.HTML);
            conformance.add(FeatureConformance.GEOJSON);

            // optional functionality
            if (crsByReference(serviceInfo)) {
                conformance.add(FeatureConformance.CRS_BY_REFERENCE);
            }
            if (featuresFilter(serviceInfo)) {
                conformance.add(FeatureConformance.FEATURES_FILTER);
            }
            if (filter(serviceInfo)) {
                conformance.add(FeatureConformance.FILTER);
            }
            if (queryables(serviceInfo)) {
                conformance.add(FeatureConformance.QUERYABLES);
            }

            // output formats
            if (gml321(serviceInfo)) {
                conformance.add(FeatureConformance.GML321);
            }
            // if (gmlSF0(serviceInfo)) {
            // not implemented
            // conformance.add(FeatureConformance.GMLSF0);
            // }
            // if (gmlSF2(serviceInfo)) {
            // not implemented
            // conformance.add(FeatureConformance.GMLSF2);
            // }

            // draft functionality
            if (ids(serviceInfo)) {
                conformance.add(FeatureConformance.IDS);
            }
            if (search(serviceInfo)) {
                conformance.add(FeatureConformance.SEARCH);
            }
            if (sortBy(serviceInfo)) {
                conformance.add(FeatureConformance.SORTBY);
            }
        }
        return conformance;
    }

    /**
     * Configuration for FeatureService CORE functionality and extensions.
     *
     * @return CORE conformance enabled, or @{code null} for default.
     */
    public Boolean isCore() {
        return core;
    }

    /**
     * Configuration for FeatureService CORE functionality and extensions.
     *
     * @param enabled Enable CORE conformance, or @{code null} for default.
     */
    public void setCore(Boolean enabled) {
        core = enabled;
    }
    /**
     * Configuration for FeatureService CORE functionality and extensions.
     *
     * @param serviceInfo WFSService configuration
     * @return {@true} if CORE conformance enabled
     */
    public boolean core(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo, core, CORE);
    }

    /**
     * GML321 conformance enabled by configuration.
     *
     * @return GML321 conformance enabled, or @{code null} for default.
     */
    public Boolean isGML321() {
        return gml321;
    }

    /**
     * GML321 conformance enablement.
     *
     * @param enabled GML321 conformance enabled, or @{code null} for default.
     */
    public void setGML321(Boolean enabled) {
        this.gml321 = enabled;
    }

    /**
     * GML321 conformance enabled by configuration or default.
     *
     * @param serviceInfo WFSService configuration used to determine default
     * @return {@true} if GML321 conformance enabled
     */
    public boolean gml321(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo, gml321, GML321);
    }

    /**
     * GMlSF0 conformance enabled by configuration.
     *
     * @return GMlSF0 conformance enabled, or @{code null} for default.
     */
    public Boolean isGMLSFO() {
        return gmlSF0;
    }

    /**
     * GMlSF0 conformance enablement.
     *
     * @param enabled GMlSF0 conformance enabled, or @{code null} for default.
     */
    public void setGMLSF0(Boolean enabled) {
        this.gmlSF0 = enabled;
    }

    /**
     * GMlSF0 conformance enabled by configuration or default.
     *
     * @param serviceInfo WFSService configuration used to determine default
     * @return {@true} if GMlSF0 conformance enabled
     */
    public boolean gmlSF0(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo, gmlSF0, GMLSF0);
    }

    /**
     * GMlSF2 conformance enabled by configuration.
     *
     * @return GMlSF2 conformance enabled, or @{code null} for default.
     */
    public Boolean isGMLSF2() {
        return gmlSF2;
    }

    /**
     * GMlSF2 conformance enabled by configuration or default.
     *
     * @param serviceInfo WFSService configuration used to determine default
     * @return {@true} if GMlSF2 conformance enabled
     */
    public boolean gmlSF2(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo, gmlSF2, GMLSF2);
    }

    /**
     * GMlSF2 conformance enablement.
     *
     * @param enabled GMlSF2 conformance enabled, or @{code null} for default.
     */
    public void setGMLSF2(Boolean enabled) {
        this.gmlSF2 = enabled;
    }

    /**
     * CRS_BY_REFERENCE conformance enabled by configuration.
     *
     * @return CRSByReference conformance enabled, or @{code null} for default.
     */
    public Boolean isCRSByReference() {
        return crsByReference;
    }

    /**
     * CRS_BY_REFERENCE conformance enabled by configuration or default.
     *
     * @param serviceInfo WFSService configuration used to determine default
     * @return {@true} if CRS_BY_REFERENCE conformance enabled
     */
    public boolean crsByReference(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo, crsByReference, CRS_BY_REFERENCE);
    }

    /**
     * CRS_BY_REFERENCE conformance enablement.
     *
     * @param enabled CRS_BY_REFERENCE conformance enabled, or @{code null} for default.
     */
    public void setCRSByReference(Boolean enabled) {
        crsByReference = enabled;
    }

    /**
     * FEATURES_FILTER conformance enabled by configuration.
     *
     * @return FeaturesFilter conformance enabled, or @{code null} for default.
     */
    public Boolean isFeaturesFilter() {
        return featuresFilter;
    }

    /**
     * FEATURES_FILTER conformance enabled by configuration or default.
     *
     * <p>This requires FILTER to be enabled, providing:
     *
     * <p>Queryables resource exists for every collection
     *
     * <p>Supports filters on the Features resource.
     *
     * <p>Supports filter and a bbox parameter
     *
     * @param serviceInfo WFSService configuration used to determine default
     * @return {@true} if FEATURES_FILTER conformance enabled
     */
    public boolean featuresFilter(WFSInfo serviceInfo) {
        return filter(serviceInfo) && isEnabled(serviceInfo, featuresFilter, FEATURES_FILTER);
    }

    /**
     * FeaturesFilter conformance enablement.
     *
     * @param enabled FeaturesFilter conformance enabled, or @{code null} for default.
     */
    public void setFeaturesFilter(Boolean enabled) {
        featuresFilter = enabled;
    }

    /**
     * FILTER conformance enabled by configuration.
     *
     * @return FILTER conformance enabled, or @{code null} for default.
     */
    public Boolean isFilter() {
        return filter;
    }

    /**
     * FILTER conformance enabled by configuration or default.
     *
     * @param serviceInfo WFSService configuration used to determine default
     * @return {@true} if Filter conformance enabled
     */
    public boolean filter(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo, filter, FILTER);
    }

    /**
     * FILTER conformance enablement.
     *
     * @param enabled Filter conformance enabled, or @{code null} for default.
     */
    public void setFilter(Boolean enabled) {
        filter = enabled;
    }

    /**
     * SEARCH conformance enabled by configuration.
     *
     * @return Search conformance enabled, or @{code null} for default.
     */
    public Boolean isSearch() {
        return search;
    }

    /**
     * SEARCH conformance enabled by configuration or default.
     *
     * @param serviceInfo WFSService configuration used to determine default
     * @return {@true} if Search conformance enabled
     */
    public boolean search(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo, search, SEARCH);
    }

    /**
     * SEARCH conformance enablement.
     *
     * @param enabled Search conformance enabled, or @{code null} for default.
     */
    public void setSearch(Boolean enabled) {
        search = enabled;
    }

    /**
     * QUERYABLES conformance enabled by configuration.
     *
     * @return Queryables conformance enabled, or @{code null} for default.
     */
    public Boolean isQueryables() {
        return queryables;
    }

    /**
     * QUERYABLES conformance enabled by configuration or default.
     *
     * @param serviceInfo WFSService configuration used to determine default
     * @return {@true} if Queryables conformance enabled
     */
    public boolean queryables(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo, queryables, QUERYABLES);
    }

    /**
     * QUERYABLES conformance enablement.
     *
     * @param enabled Queryables conformance enabled, or @{code null} for default.
     */
    public void setQueryables(Boolean enabled) {
        queryables = enabled;
    }

    /**
     * IDS conformance enabled by configuration.
     *
     * @return IDS conformance enabled, or @{code null} for default.
     */
    public Boolean isIDs() {
        return ids;
    }

    /**
     * IDS conformance enabled by configuration or default.
     *
     * @param serviceInfo WFSService configuration used to determine default
     * @return {@true} if IDS conformance enabled
     */
    public boolean ids(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo, ids, IDS);
    }

    /**
     * IDS conformance enablement.
     *
     * @param enabled IDS conformance enabled, or @{code null} for default.
     */
    public void setIDs(Boolean enabled) {
        ids = enabled;
    }

    /**
     * SORTBY conformance enabled by configuration.
     *
     * @return SORTBY conformance enabled, or @{code null} for default.
     */
    public Boolean isSortBy() {
        return sortBy;
    }

    /**
     * SORTBY conformance enabled by configuration or default.
     *
     * @param serviceInfo WFSService configuration used to determine default
     * @return {@true} if SORTBY conformance enabled
     */
    public boolean sortBy(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo, sortBy, SORTBY);
    }

    /**
     * SORTBY conformance enablement.
     *
     * @param enabled SORTBY conformance enabled, or @{code null} for default.
     */
    public void setSortBy(Boolean enabled) {
        sortBy = enabled;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FeatureConformance");
        sb.append(" ").append(METADATA_KEY);
        sb.append(" { core=").append(core);
        sb.append(", crsByReference=").append(crsByReference);
        sb.append(", featuresFilter=").append(featuresFilter);
        sb.append(", filter=").append(filter);
        sb.append(", gmlSF0=").append(gmlSF0);
        sb.append(", gmlSF2=").append(gmlSF2);
        sb.append(", ids=").append(ids);
        sb.append(", queryables=").append(queryables);
        sb.append(", search=").append(search);
        sb.append(", sortBy=").append(sortBy);
        sb.append('}');
        return sb.toString();
    }
}
