/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import com.thoughtworks.xstream.XStream;
import java.util.List;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
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

    public Class<OSEOInfo> getServiceClass() {
        return OSEOInfo.class;
    }

    protected OSEOInfo createServiceFromScratch(GeoServer gs) {
        OSEOInfo oseo = new OSEOInfoImpl();
        oseo.setName("OSEO");
        oseo.setAbstract(
                "Provides interoperable access, following ISO/OGC interface guidelines, to Earth Observation metadata.");
        oseo.setTitle("OpenSearch for Earth Observation");
        oseo.setMaximumRecordsPerPage(OSEOInfo.DEFAULT_MAXIMUM_RECORDS);
        oseo.setRecordsPerPage(OSEOInfo.DEFAULT_RECORDS_PER_PAGE);
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
        xs.allowTypeHierarchy(ProductClass.class);
    }

    @Override
    protected OSEOInfo initialize(OSEOInfo service) {
        super.initialize(service);

        if (!service.getVersions().contains(OSEOInfo.VERSION_1_0_0)) {
            service.getVersions().add(OSEOInfo.VERSION_1_0_0);
        }
        return service;
    }
}
