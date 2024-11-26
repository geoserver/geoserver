package org.geoserver.ogcapi.v1.features;

import java.io.Serializable;
import org.geoserver.wfs.WFSInfo;

public interface FeatureConformanceInfo extends Serializable {
    /**
     * Configuration for FeatureService CORE functionality and extensions.
     *
     * @return CORE conformance enabled, or @{code null} for default.
     */
    Boolean isCore();

    /**
     * Configuration for FeatureService CORE functionality and extensions.
     *
     * @param serviceInfo WFSService configuration
     * @return {@true} if CORE conformance enabled
     */
    boolean core(WFSInfo serviceInfo);

    /**
     * Configuration for FeatureService CORE functionality and extensions.
     *
     * @param enabled Enable CORE conformance, or @{code null} for default.
     */
    void setCore(Boolean enabled);

    /**
     * GMlSF0 conformance enabled by configuration.
     *
     * @return GMlSF0 conformance enabled, or @{code null} for default.
     */
    Boolean isGMLSFO();

    /**
     * GMlSF0 conformance enabled by configuration or default.
     *
     * @param serviceInfo WFSService configuration used to determine default
     * @return {@true} if GMlSF0 conformance enabled
     */
    boolean gmlSF0(WFSInfo serviceInfo);

    /**
     * GMlSF0 conformance enablement.
     *
     * @param enabled GMlSF0 conformance enabled, or @{code null} for default.
     */
    void setGMLSF0(Boolean enabled);

    /**
     * GMlSF2 conformance enabled by configuration.
     *
     * @return GMlSF2 conformance enabled, or @{code null} for default.
     */
    Boolean isGMLSF2();

    /**
     * GMlSF2 conformance enabled by configuration or default.
     *
     * @param serviceInfo WFSService configuration used to determine default
     * @return {@true} if GMlSF2 conformance enabled
     */
    boolean gmlSF2(WFSInfo serviceInfo);

    /**
     * GMlSF2 conformance enablement.
     *
     * @param enabled GMlSF2 conformance enabled, or @{code null} for default.
     */
    void setGMLSF2(Boolean enabled);

    /**
     * CRS_BY_REFERENCE conformance enabled by configuration.
     *
     * @return CRSByReference conformance enabled, or @{code null} for default.
     */
    Boolean isCRSByReference();

    boolean crsByReference(WFSInfo serviceInfo);

    /**
     * CRS_BY_REFERENCE conformance enablement.
     *
     * @param enabled CRS_BY_REFERENCE conformance enabled, or @{code null} for default.
     */
    void setCRSByReference(Boolean enabled);

    /**
     * FeaturesFilter conformance enabled by configuration.
     *
     * @return FeaturesFilter conformance enabled, or @{code null} for default.
     */
    Boolean isFeaturesFilter();

    /**
     * FEATURES_FILTER conformance enabled by configuration or default.
     *
     * @param serviceInfo WFSService configuration used to determine default
     * @return {@true} if FEATURES_FILTER conformance enabled
     */
    boolean featuresFilter(WFSInfo serviceInfo);

    /**
     * FeaturesFilter conformance enablement.
     *
     * @param enabled FeaturesFilter conformance enabled, or @{code null} for default.
     */
    void setFeaturesFilter(Boolean enabled);

    /**
     * FILTER conformance enabled by configuration.
     *
     * @return FILTER conformance enabled, or @{code null} for default.
     */
    Boolean isFilter();

    /**
     * FILTER conformance enabled by configuration or default.
     *
     * @param serviceInfo WFSService configuration used to determine default
     * @return {@true} if Filter conformance enabled
     */
    boolean filter(WFSInfo serviceInfo);

    /**
     * FILTER conformance enablement.
     *
     * @param enabled Filter conformance enabled, or @{code null} for default.
     */
    void setFilter(Boolean enabled);

    /**
     * SEARCH conformance enabled by configuration.
     *
     * @return Search conformance enabled, or @{code null} for default.
     */
    Boolean isSearch();

    /**
     * SEARCH conformance enabled by configuration or default.
     *
     * @param serviceInfo WFSService configuration used to determine default
     * @return {@true} if Search conformance enabled
     */
    boolean search(WFSInfo serviceInfo);

    /**
     * SEARCH conformance enablement.
     *
     * @param enabled Search conformance enabled, or @{code null} for default.
     */
    void setSearch(Boolean enabled);

    /**
     * QUERYABLES conformance enabled by configuration.
     *
     * @return QUERYABLES conformance enabled, or @{code null} for default.
     */
    Boolean isQueryables();

    /**
     * QUERYABLES conformance enabled by configuration or default.
     *
     * @param serviceInfo WFSService configuration used to determine default
     * @return {@true} if Queryables conformance enabled
     */
    boolean queryables(WFSInfo serviceInfo);

    /**
     * QUERYABLES conformance enablement.
     *
     * @param enabled Queryables conformance enabled, or @{code null} for default.
     */
    void setQueryables(Boolean enabled);

    /**
     * IDS conformance enabled by configuration.
     *
     * @return IDS conformance enabled, or @{code null} for default.
     */
    Boolean isIDs();

    /**
     * IDS conformance enabled by configuration or default.
     *
     * @param serviceInfo WFSService configuration used to determine default
     * @return {@true} if IDS conformance enabled
     */
    boolean ids(WFSInfo serviceInfo);

    /**
     * IDS conformance enablement.
     *
     * @param enabled IDS conformance enabled, or @{code null} for default.
     */
    void setIDs(Boolean enabled);

    /**
     * SORTBY conformance enabled by configuration.
     *
     * @return SORTBY conformance enabled, or @{code null} for default.
     */
    Boolean isSortBy();

    /**
     * SORTBY conformance enabled by configuration or default.
     *
     * @param serviceInfo WFSService configuration used to determine default
     * @return {@true} if SORTBY conformance enabled
     */
    boolean sortBy(WFSInfo serviceInfo);

    /**
     * SORTBY conformance enablement.
     *
     * @param enabled SORTBY conformance enabled, or @{code null} for default.
     */
    void setSortBy(Boolean enabled);
}
