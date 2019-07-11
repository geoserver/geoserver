package org.geoserver.api.styles;

import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class StyleServiceXStreamLoader extends XStreamServiceLoader<StylesServiceInfo> {

    public StyleServiceXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "styles");
    }

    @Override
    protected StylesServiceInfo createServiceFromScratch(GeoServer gs) {
        StylesServiceInfoImpl info = new StylesServiceInfoImpl();
        info.setName("styles");
        info.setTitle("Styles server");
        return info;
    }

    @Override
    public Class<StylesServiceInfo> getServiceClass() {
        return StylesServiceInfo.class;
    }
}
