/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.inspire;

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
        l.copyFromClassPath("inspire_vs.xsd", target, InspireInitializer.class);
        Assert.isTrue(target.exists());
    }

}
