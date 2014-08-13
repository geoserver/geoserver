/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.printing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Logger;

import org.springframework.web.servlet.mvc.ServletWrappingController;
import org.geoserver.data.util.IOUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;

/**
 * Wrapper for Spring's ServletWrappingController to allow use of GeoServer's config dir.
 * 
 * @author Alan Gerber, The Open Planning Project
 *
 */
public class PrintingServletWrappingController extends
ServletWrappingController {

    private Logger LOG = org.geotools.util.logging.Logging.getLogger("org.geoserver.printing");

    public void setInitParameters(Properties initParameters) {
        // find the config parameter and update it so it points to $GEOSERVER_DATA_DIR/printing/$CONFIG
        String configProp = initParameters.getProperty("config");

        try {
            GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
            String configPath = Paths.path("printing", Paths.convert(configProp));
            Resource config = loader.get(configPath);

            if (config.getType() == Type.UNDEFINED) {
                InputStream conf = getClass().getResourceAsStream("default-config.yaml");
                IOUtils.copy(conf, config.file());
            }
            File qualifiedConfig = config.file();
            if (!qualifiedConfig.canRead()) {
                LOG.warning("Printing module missing its configuration.  Any actions it takes will fail.");
                return;
            }
            initParameters.setProperty("config", qualifiedConfig.getCanonicalPath());
        } catch (java.io.IOException e) {
            LOG.warning("Unable to calcule canonical path for MapFish printing servlet. "
                    + "Module will fail when run.  IO Exception is: " + e);
        } catch (Exception e) {
            LOG.warning("Unable to access/create config directory for MapFish printing module."
                    + "Module will fail when run. Config exception is: " + e);
        }
        super.setInitParameters(initParameters);
    }
}
