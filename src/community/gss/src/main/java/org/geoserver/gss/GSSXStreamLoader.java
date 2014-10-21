/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import java.util.ArrayList;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.Version;

import com.thoughtworks.xstream.XStream;

/**
 * Loads GSS configuration from the disk
 * 
 * @author aaime
 */
public class GSSXStreamLoader extends XStreamServiceLoader<GSSInfo> {

    public GSSXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "gss");
    }

    @Override
    protected GSSInfo createServiceFromScratch(GeoServer gs) {
        return new GSSInfoImpl();
    }

    public Class<GSSInfo> getServiceClass() {
        return GSSInfo.class;
    }

    @Override
    protected void initXStreamPersister(XStreamPersister xp, GeoServer gs) {
        super.initXStreamPersister(xp, gs);
        XStream xs = xp.getXStream();
        xs.alias("gss", GSSInfo.class, GSSInfoImpl.class);
        xs.registerLocalConverter(GSSInfoImpl.class, "versioningDataStore", xp.buildReferenceConverter(DataStoreInfo.class));
    }

    @Override
    protected GSSInfo initialize(GSSInfo service) {
        if (service.getVersions() == null) {
            ((GSSInfoImpl) service).setVersions(new ArrayList());
        }
        if (service.getVersions().isEmpty()) {
            service.getVersions().add(new Version("1.0.0"));
        }
        return service;
    }
}
