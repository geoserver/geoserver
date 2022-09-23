/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.Version;

public class WMTSXStreamLoader extends XStreamServiceLoader<WMTSInfo> {

    public WMTSXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "wmts");
    }

    @Override
    public Class<WMTSInfo> getServiceClass() {
        return WMTSInfo.class;
    }

    @Override
    protected WMTSInfo createServiceFromScratch(GeoServer gs) {
        WMTSInfoImpl wmts = new WMTSInfoImpl();
        wmts.setName("WMTS");
        return wmts;
    }

    @Override
    public void initXStreamPersister(XStreamPersister xp, GeoServer gs) {
        super.initXStreamPersister(xp, gs);
        xp.getXStream().alias("wmts", WMTSInfo.class, WMTSInfoImpl.class);
    }

    @Override
    protected WMTSInfo initialize(WMTSInfo service) {
        service = super.initialize(service);
        if (service.getMaintainer() == null) {
            service.setMaintainer("GeoServer");
        }
        if (service.getOnlineResource() == null) {
            service.setOnlineResource("https://geoserver.org");
        }
        if (service.getTitle() == null) {
            service.setTitle("GeoServer Web Map Tile Service");
        }
        if (service.getAbstract() == null) {
            service.setAbstract(
                    "Predefined map tiles generated from vector data, raster data, and imagery.");
        }
        if (service.getFees() == null) {
            service.setFees("NONE");
        }
        if (service.getAccessConstraints() == null) {
            service.setAccessConstraints("NONE");
        }
        if (service.getVersions() == null || service.getVersions().isEmpty()) {
            service.getVersions().add(new Version("1.0.0"));
        }
        return service;
    }
}
