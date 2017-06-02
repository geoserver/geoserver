package com.boundlessgeo.gsr.api.service.map;

import com.boundlessgeo.gsr.api.AbstractGSRController;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(path = "/gsr/services/{workspace}/MapService", produces = MediaType.APPLICATION_JSON_VALUE)
public class AbstractMapServiceController extends AbstractGSRController {

    public AbstractMapServiceController(GeoServer geoServer) {
        super(geoServer);
    }
}
