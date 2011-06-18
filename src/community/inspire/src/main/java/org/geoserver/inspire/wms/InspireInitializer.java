package org.geoserver.inspire.wms;

import java.io.File;

import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.util.Assert;

public class InspireInitializer implements GeoServerInitializer {

    public void initialize(GeoServer geoServer) throws Exception {
        // copy over the schema
        GeoServerResourceLoader l = geoServer.getCatalog().getResourceLoader();
        File target = l.createFile("www", "inspire", "inspire_vs.xsd");
        l.copyFromClassPath("inspire_vs.xsd", target, getClass());
        Assert.isTrue(target.exists());
    }

}
