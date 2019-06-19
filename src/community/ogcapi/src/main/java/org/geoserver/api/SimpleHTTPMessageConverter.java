/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 *
 */

package org.geoserver.api;

import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;

public class SimpleHTTPMessageConverter extends AbstractHTMLMessageConverter {

    private final String templateName;

    public SimpleHTTPMessageConverter(
            Class binding,
            Class serviceClass,
            GeoServerResourceLoader loader,
            GeoServer geoServer,
            String templateName) {
        super(binding, serviceClass, loader, geoServer);
        this.templateName = templateName;
    }

    @Override
    protected String getTemplateName(Object value) {
        return templateName;
    }

    @Override
    protected ResourceInfo getResource(Object value) {
        return null;
    }
}
