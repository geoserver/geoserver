package org.geoserver.restng.converters;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.context.ApplicationContext;
import org.springframework.http.converter.HttpMessageConverter;

/**
 * Base message converter behavior
 */
public abstract class BaseMessageConverter implements HttpMessageConverter {

    protected final Catalog catalog;

    protected final XStreamPersisterFactory xpf;

    protected  final GeoServer geoServer;

    protected final ApplicationContext applicationContext;

    public BaseMessageConverter(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.catalog = (Catalog) GeoServerExtensions.bean("catalog");
        this.xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        this.geoServer = GeoServerExtensions.bean(GeoServer.class);
    }
}
