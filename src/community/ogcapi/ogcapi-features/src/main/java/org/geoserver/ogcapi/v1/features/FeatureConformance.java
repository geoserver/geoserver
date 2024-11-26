package org.geoserver.ogcapi.v1.features;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ogcapi.APIConformance;
import org.geoserver.ogcapi.ConformanceClass;
import org.geoserver.ogcapi.ConformanceInfo;
import org.geoserver.wfs.WFSInfo;
import org.geotools.data.wfs.WFSServiceInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.geoserver.ogcapi.APIConformance.Level.DRAFT_STANDARD;
import static org.geoserver.ogcapi.APIConformance.Level.STANDARD;

/**
 * FeatureService configuration.
 */
public class FeatureConformance extends ConformanceInfo<WFSInfo> implements FeatureConformanceInfo {
    public static String METADATA_KEY = "ogcapiFeatures";

    public static final APIConformance CORE = new APIConformance("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core",STANDARD);

    // required resource formats
    public static final APIConformance HTML = CORE.extend("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/html");
    public static final APIConformance GEOJSON = CORE.extend("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/geojson");
    public static final APIConformance OAS30 = CORE.extend("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/oas30");

    // optional resource formats
    public static final APIConformance GMLSF0 = CORE.extend("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/gmlsf0");
    public static final APIConformance GMLSF2 = CORE.extend("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/gmlsf2");

    // optional
    public static final APIConformance CRS_BY_REFERENCE = CORE.extend("http://www.opengis.net/spec/ogcapi-features-2/1.0/conf/crs");
    public static final APIConformance FEATURES_FILTER = CORE.extend(ConformanceClass.FEATURES_FILTER);
    public static final APIConformance FILTER = CORE.extend(ConformanceClass.FILTER);
    public static final APIConformance QUERYABLES = CORE.extend(ConformanceClass.QUERYABLES);

    // draft
    public static final APIConformance IDS = new APIConformance(ConformanceClass.IDS, DRAFT_STANDARD);
    public static final APIConformance SEARCH = new APIConformance(ConformanceClass.SEARCH, DRAFT_STANDARD);
    public static final APIConformance SORTBY = new APIConformance(ConformanceClass.SORTBY, DRAFT_STANDARD);

    private Boolean core = null;
    private Boolean gmlSF0 = null;
    private Boolean gmlSF2 = null;
    private Boolean featuresFilter = null;
    private Boolean crsByReference = null;
    private Boolean filter = null;
    private Boolean search = null;
    private Boolean queryables = null;
    private Boolean ids = null;
    private Boolean sortBy = null;

    /**
     * Configuration for OGCAPI Features.
     */
    public FeatureConformance() {
        super(METADATA_KEY);
    }

    /**
     * Requires CORE to be enabled.
     * 
     * @param serviceInfo
     * @return requires core to be enabled.
     */
    @Override
    public boolean isEnabled(WFSInfo wfsInfo) {
        return core(wfsInfo);
    }

    /**
     * Obtain FeatureService configuration for WFSInfo.
     *
     * Uses configuration stored in metadata map, or creates default if needed.
     *
     * @param wfsInfo WFSService configuration
     * @return Feature Service configuration
     */
    public static FeatureConformance configuration(WFSInfo wfsInfo) {
        synchronized (wfsInfo) {
            if (wfsInfo.getMetadata().containsKey(METADATA_KEY)) {
                return (FeatureConformance) wfsInfo.getMetadata().get(METADATA_KEY);
            }
            else {
                FeatureConformance conf = new FeatureConformance();
                wfsInfo.getMetadata().put(METADATA_KEY,conf);
                return conf;
            }
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

            // optional output formats
            if (gmlSF0(serviceInfo)) {
                conformance.add(FeatureConformance.GMLSF0);
            }
            if (gmlSF2(serviceInfo)) {
                conformance.add(FeatureConformance.GMLSF2);
            }

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
    @Override
    public Boolean isCore() {
        return core;
    }
    @Override
    public boolean core(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo,core,CORE);
    }
    @Override
    public void setCore(Boolean enabled) {
        core = enabled;
    }
    @Override
    public Boolean isGMLSFO() {
        return gmlSF0;
    }
    @Override
    public boolean gmlSF0(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo,gmlSF0,GMLSF0);
    }

    @Override
    public void setGMLSF0(Boolean enabled) {
        this.gmlSF0 = enabled;
    }

    @Override
    public Boolean isGMLSF2() {
        return gmlSF2;
    }

    @Override
    public boolean gmlSF2(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo,gmlSF2,GMLSF2);
    }

    @Override
    public void setGMLSF2(Boolean enabled) {
        this.gmlSF2 = enabled;
    }

    @Override
    public Boolean isCRSByReference() {
        return crsByReference;
    }
    @Override
    public boolean crsByReference(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo,crsByReference,CRS_BY_REFERENCE);
    }
    @Override
    public void setCRSByReference(Boolean enabled) {
        crsByReference = enabled;
    }

    @Override
    public Boolean isFeaturesFilter() {
        return featuresFilter;
    }
    @Override
    public boolean featuresFilter(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo,featuresFilter,FEATURES_FILTER);
    }
    @Override
    public void setFeaturesFilter(Boolean enabled) {
        featuresFilter = enabled;
    }
    @Override
    public Boolean isFilter() {
        return filter;
    }
    @Override
    public boolean filter(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo,filter,FILTER);
    }
    @Override
    public void setFilter(Boolean enabled) {
        filter = enabled;;
    }
    @Override
    public Boolean isSearch() {
        return search;
    }
    @Override
    public boolean search(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo,search,SEARCH);
    }
    @Override
    public void setSearch(Boolean enabled) {
        search = enabled;
    }

    @Override
    public Boolean isQueryables() {
        return queryables;
    }
    @Override
    public boolean queryables(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo,queryables,QUERYABLES);
    }
    @Override
    public void setQueryables(Boolean enabled) {
        queryables = enabled;
    }
    @Override
    public Boolean isIDs() {
        return ids;
    }
    @Override
    public boolean ids(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo,ids,IDS);
    }
    @Override
    public void setIDs(Boolean enabled) {
        ids = enabled;;
    }
    @Override
    public Boolean isSortBy() {
        return sortBy;
    }
    @Override
    public boolean sortBy(WFSInfo serviceInfo) {
        return isEnabled(serviceInfo,sortBy,SORTBY);
    }
    @Override
    public void setSortBy(Boolean enabled) {
        sortBy = enabled;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder( super.toString());
        sb.append(" { core=").append(core);
        sb.append(", gmlSF0=").append(gmlSF0);
        sb.append(", gmlSF2=").append(gmlSF2);
        sb.append(", featuresFilter=").append(featuresFilter);
        sb.append(", crsByReference=").append(crsByReference);
        sb.append(", filter=").append(filter);
        sb.append(", search=").append(search);
        sb.append(", queryables=").append(queryables);
        sb.append(", ids=").append(ids);
        sb.append(", sortBy=").append(sortBy);
        sb.append('}');
        return sb.toString();
    }
}

