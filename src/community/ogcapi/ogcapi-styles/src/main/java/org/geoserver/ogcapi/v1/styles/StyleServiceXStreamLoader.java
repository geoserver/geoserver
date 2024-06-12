/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.styles;

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
        if (info.getTitle() == null) {
            info.setTitle("Styles Service");
            info.setAbstract(
                    "OGCAPI-Styles is a Web API that enables map servers, clients as well as visual style editors, to manage and fetch styles that consist of symbolizing instructions that can be applied by a rendering engine on features and/or coverages.");
        }
        return info;
    }

    @Override
    public Class<StylesServiceInfo> getServiceClass() {
        return StylesServiceInfo.class;
    }
}
