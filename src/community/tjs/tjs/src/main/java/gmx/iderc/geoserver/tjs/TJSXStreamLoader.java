package gmx.iderc.geoserver.tjs;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.Version;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Loads and persist the {@link TJSInfo} object to and from xstream
 * persistence.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class TJSXStreamLoader extends XStreamServiceLoader<TJSInfo> {

    public TJSXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "tjs");
    }

    @Override
    protected void initXStreamPersister(XStreamPersister xp, GeoServer gs) {
        xp.getXStream().alias("tjs", TJSInfo.class, TJSInfoImpl.class);
    }

    protected TJSInfo createServiceFromScratch(GeoServer gs) {
        TJSInfoImpl tjs = new TJSInfoImpl();
        tjs.setId("tjs");
        tjs.setName("tjs");
        tjs.setGeoServer(gs);
        return tjs;
    }

    public Class<TJSInfo> getServiceClass() {
        return TJSInfo.class;
    }

    @Override
    protected TJSInfo initialize(TJSInfo service) {
        if (service.getKeywords() == null) {
            ((TJSInfoImpl) service).setKeywords(new ArrayList());
        }
        if (service.getExceptionFormats() == null) {
            ((TJSInfoImpl) service).setExceptionFormats(new ArrayList());
        }
        if (service.getMetadata() == null) {
            ((TJSInfoImpl) service).setMetadata(new MetadataMap());
        }
        if (service.getClientProperties() == null) {
            ((TJSInfoImpl) service).setClientProperties(new HashMap());
        }
        if (service.getVersions() == null) {
            ((TJSInfoImpl) service).setVersions(new ArrayList());
        }
        if (service.getVersions().isEmpty()) {
            service.getVersions().add(new Version("1.0.0"));
        }
        return service;
    }

}
