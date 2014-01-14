/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.csw;

import java.util.ArrayList;
import java.util.HashMap;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.Version;

import com.thoughtworks.xstream.XStream;

/**
 * Service loader for the Catalog Services for the Web
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CSWXStreamLoader extends XStreamServiceLoader<CSWInfo> {
    public CSWXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "csw");
    }

    public String getServiceId() {
        return "csw";
    }

    public Class<CSWInfo> getServiceClass() {
        return CSWInfo.class;
    }

    protected CSWInfo createServiceFromScratch(GeoServer gs) {
        CSWInfoImpl csw = new CSWInfoImpl();
        csw.setName("CSW");
        csw.setId(getServiceId());
        csw.setGeoServer(gs);
        return csw;
    }

    @Override
    protected void initXStreamPersister(XStreamPersister xp, GeoServer gs) {
        XStream xs = xp.getXStream();
        xs.alias("csw", CSWInfo.class, CSWInfoImpl.class);
    }

    @Override
    protected CSWInfo initialize(CSWInfo service) {
        // TODO: move this code block to the parent class
        if (service.getKeywords() == null) {
            ((CSWInfoImpl) service).setKeywords(new ArrayList());
        }
        if (service.getExceptionFormats() == null) {
            ((CSWInfoImpl) service).setExceptionFormats(new ArrayList());
        }
        if (service.getMetadata() == null) {
            ((CSWInfoImpl) service).setMetadata(new MetadataMap());
        }
        if (service.getClientProperties() == null) {
            ((CSWInfoImpl) service).setClientProperties(new HashMap());
        }
        if (service.getVersions() == null) {
            ((CSWInfoImpl) service).setVersions(new ArrayList());
        }
        if (service.getVersions().isEmpty()) {
            service.getVersions().add(new Version("2.0.2"));
        }

        return service;
    }

}
