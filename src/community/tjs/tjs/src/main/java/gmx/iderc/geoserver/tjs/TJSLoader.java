package gmx.iderc.geoserver.tjs;

import org.geoserver.config.GeoServer;
import org.geoserver.config.util.LegacyServiceLoader;
import org.geoserver.config.util.LegacyServicesReader;
import org.geotools.util.Version;

public class TJSLoader extends LegacyServiceLoader<TJSInfo> {

    public Class<TJSInfo> getServiceClass() {
        return TJSInfo.class;
    }

    public TJSInfo load(LegacyServicesReader reader, GeoServer geoServer)
            throws Exception {
        TJSInfoImpl tjs = new TJSInfoImpl();
        tjs.setId("tjs");
        tjs.setGeoServer(geoServer);
        tjs.getVersions().add(new Version("1.0.0"));
        return tjs;
    }

}
