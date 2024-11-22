package org.geoserver.ogcapi.v1.features;

import org.geoserver.catalog.MetadataMap;
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
public class FeatureConformance extends ConformanceInfo<WFSInfo> {

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

    /**
     * Configuration for OGCAPI Features.
     *
     * @param serviceInfo
     */
    public FeatureConformance(WFSInfo serviceInfo) {
        super("ogcapi-features",CORE, serviceInfo);
    }

    List<APIConformance> getConformances() {
        List<APIConformance> conformances = new ArrayList<>();
        if (isEnabled()) {
            // built-in rquired conformance classes
            conformances.add(FeatureConformance.CORE);
            conformances.add(FeatureConformance.OAS30);
            conformances.add(FeatureConformance.HTML);
            conformances.add(FeatureConformance.GEOJSON);

            // optional functionality
            if (isCRSByReference()) {
                conformances.add(FeatureConformance.CRS_BY_REFERENCE);
            }
            if (isFeaturesFilter()) {
                conformances.add(FeatureConformance.FEATURES_FILTER);
            }
            if (isFilter()) {
                conformances.add(FeatureConformance.FILTER);
            }

            // optional output formats
            if (isGMLSFO()) {
                conformances.add(FeatureConformance.GMLSF0);
            }
            if (isGMLSF2()) {
                conformances.add(FeatureConformance.GMLSF2);
            }

            // draft functionality
            if (isIDs()) {
                conformances.add(FeatureConformance.IDS);
            }
            if (isSearch()) {
                conformances.add(FeatureConformance.SEARCH);
            }
            if (isSortBy()) {
                conformances.add(FeatureConformance.SORTBY);
            }
        }
        return conformances;
    }

    public boolean isCore() {
        return isEnabled(CORE);
    }
    public void setCore(boolean enabled) {
        setEnabled(CORE, enabled);
    }

    public boolean isGMLSFO() {
        return isEnabled(GMLSF0);
    }
    public void setGMLSF0(boolean enabled) {
        setEnabled(GMLSF0, enabled);
    }

    public boolean isGMLSF2() {
        return isEnabled(GMLSF2);
    }
    public void setGMLSF2(boolean enabled) {
        setEnabled(GMLSF2, enabled);
    }

    public boolean isCRSByReference() {
        return isEnabled(CRS_BY_REFERENCE);
    }
    public void setCRSByReference(boolean enabled) {
        setEnabled(CRS_BY_REFERENCE, enabled);
    }

    public boolean isFeaturesFilter() {
        return isEnabled(FEATURES_FILTER);
    }
    public void setFeaturesFilter(boolean enabled) {
        setEnabled(FEATURES_FILTER, enabled);
    }

    public boolean isFilter() {
        return isEnabled(FILTER);
    }

    public void setFilter(boolean enabled) {
        setEnabled(FILTER, enabled);
    }

    public boolean isSearch() {
        return isEnabled(SEARCH);
    }
    public void setSearch(boolean enabled) {
        setEnabled("search", enabled);
    }

    public boolean isQueryables() {
        return isEnabled(QUERYABLES);
    }
    public void setQueryables(boolean enabled) {
        setEnabled(QUERYABLES, enabled);
    }
    public boolean isIDs() {
        return isEnabled(IDS);
    }
    public void setIDs(boolean enabled) {
        setEnabled("ids", enabled);
    }

    public boolean isSortBy() {
        return isEnabled(SORTBY);
    }
    public void setSortBy(boolean enabled) {
        setEnabled(SORTBY, enabled);
    }
}

