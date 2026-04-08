/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import com.thoughtworks.xstream.XStream;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.opensearch.eo.security.EOAccessLimitInfo;
import org.geoserver.opensearch.eo.security.EOCollectionAccessLimitInfo;
import org.geoserver.opensearch.eo.security.EOCollectionAccessLimitInfoImpl;
import org.geoserver.opensearch.eo.security.EOProductAccessLimitInfo;
import org.geoserver.opensearch.eo.security.EOProductAccessLimitInfoImpl;
import org.geoserver.platform.GeoServerResourceLoader;

/**
 * Loads the OpenSearch EO configuration from XML
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OSEOXStreamLoader extends XStreamServiceLoader<OSEOInfo> {

    public OSEOXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "oseo");
    }

    @Override
    public Class<OSEOInfo> getServiceClass() {
        return OSEOInfo.class;
    }

    @Override
    protected OSEOInfo createServiceFromScratch(GeoServer gs) {
        OSEOInfo oseo = new OSEOInfoImpl();
        oseo.setName("OSEO");
        oseo.setAbstract(
                "Provides interoperable access, following ISO/OGC interface guidelines, to Earth Observation metadata.");
        oseo.setTitle("OpenSearch for Earth Observation");
        oseo.setMaximumRecordsPerPage(OSEOInfo.DEFAULT_MAXIMUM_RECORDS);
        oseo.setRecordsPerPage(OSEOInfo.DEFAULT_RECORDS_PER_PAGE);
        oseo.setAggregatesCacheTTL(OSEOInfo.DEFAULT_AGGR_CACHE_TTL);
        oseo.setAggregatesCacheTTLUnit(OSEOInfo.DEFAULT_AGGR_CACHE_TTL_UNIT);
        final List<KeywordInfo> keywords = oseo.getKeywords();
        keywords.add(new Keyword("EarthObservation"));
        keywords.add(new Keyword("OGC"));
        return oseo;
    }

    @Override
    protected void initXStreamPersister(XStreamPersister xp, GeoServer gs) {
        super.initXStreamPersister(xp, gs);
        initXStreamPersister(xp);
    }

    /** Sets up aliases and allowed types for the xstream persister */
    public static void initXStreamPersister(XStreamPersister xp) {
        XStream xs = xp.getXStream();
        xs.alias("oseo", OSEOInfo.class, OSEOInfoImpl.class);
        xs.alias("productClass", ProductClass.class, ProductClass.class);
        xs.alias("eoCollectionLimit", EOCollectionAccessLimitInfo.class, EOCollectionAccessLimitInfoImpl.class);
        xs.alias("eoProductLimit", EOProductAccessLimitInfo.class, EOProductAccessLimitInfoImpl.class);
        xs.allowTypeHierarchy(ProductClass.class);
        xs.allowTypeHierarchy(EOAccessLimitInfo.class);
    }

    @Override
    protected OSEOInfo initialize(OSEOInfo service) {
        super.initialize(service);

        if (!service.getVersions().contains(OSEOInfo.VERSION_1_0_0)) {
            service.getVersions().add(OSEOInfo.VERSION_1_0_0);
        }
        if (service.getGlobalQueryables() == null) {
            ((OSEOInfoImpl) service).setGlobalQueryables(new ArrayList<>());
        }
        if (service.getAggregatesCacheTTLUnit() == null) {
            service.setAggregatesCacheTTLUnit(OSEOInfo.DEFAULT_AGGR_CACHE_TTL_UNIT);
        }
        if (service.getAggregatesCacheTTL() == null) {
            service.setAggregatesCacheTTL(OSEOInfo.DEFAULT_AGGR_CACHE_TTL);
        }
        if (service.getCollectionLimits() == null) {
            ((OSEOInfoImpl) service).setCollectionLimits(new ArrayList<>());
        }
        if (service.getProductLimits() == null) {
            ((OSEOInfoImpl) service).setProductLimits(new ArrayList<>());
        }

        return service;
    }
}
