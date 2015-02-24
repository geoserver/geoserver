/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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
import org.vfny.geoserver.global.GeoserverDataDirectory;
import org.geoserver.data.util.IOUtils;

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
        // find the config parameter and update it so it points to
        // $GEOSERVER_DATA_DIR/printing/$CONFIG 
        String configProp = initParameters.getProperty("config");		

        try {
            File dir = GeoserverDataDirectory.findCreateConfigDir("printing");
            File qualifiedConfig = new File(dir, configProp);
            if (!qualifiedConfig.exists()) {
                InputStream conf = getClass().getResourceAsStream("default-config.yaml");
                IOUtils.copy(conf, qualifiedConfig);
            }
            if (!qualifiedConfig.canRead()) {
                LOG.warning("Printing module missing its configuration.  Any actions it takes will fail.");
                return;
            }
            initParameters.setProperty("config", qualifiedConfig.getCanonicalPath());			
        } catch(org.vfny.geoserver.global.ConfigurationException e){
            LOG.warning("Explosion while attempting to access/create config directory for MapFish " +
                    "printing module.  Module will fail when run. Config exception is: " + e);
        } catch(java.io.IOException e){
            LOG.warning("Explosion while calculating canonical path for MapFish printing servlet. " +
                    "Module will fail when run.  IO Exception is: " + e);
        }

        super.setInitParameters(initParameters);
    }
}
