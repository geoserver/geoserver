/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.Version;
import org.springframework.stereotype.Component;

/** Loads and persist the {@link DGGSInfo} object to and from xstream persistence. */
@Component
public class DGGSXStreamLoader extends XStreamServiceLoader<DGGSInfo> {

    public DGGSXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "dggs");
    }

    @Override
    public void initXStreamPersister(XStreamPersister xp, GeoServer gs) {
        super.initXStreamPersister(xp, gs);
        initXStreamPersister(xp);
    }

    /** Sets up aliases and allowed types for the xstream persister */
    public static void initXStreamPersister(XStreamPersister xp) {
        XStream xs = xp.getXStream();
        xs.alias("dggs", DGGSInfo.class, DGGSInfoImpl.class);
        xs.allowTypes(new Class[] {DGGSInfo.class});
    }

    @Override
    protected DGGSInfo createServiceFromScratch(GeoServer gs) {
        DGGSInfoImpl dggs = new DGGSInfoImpl();
        dggs.setName("DGGS");
        return dggs;
    }

    @Override
    public Class<DGGSInfo> getServiceClass() {
        return DGGSInfo.class;
    }

    @Override
    protected DGGSInfo initialize(DGGSInfo service) {
        super.initialize(service);
        if (service.getVersions().isEmpty()) {
            service.getVersions().add(new Version("1.0.0"));
        }
        return service;
    }
}
