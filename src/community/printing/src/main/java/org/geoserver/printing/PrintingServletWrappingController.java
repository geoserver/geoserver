/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.printing;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.data.util.IOUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ServletWrappingController;
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 * Wrapper for Spring's ServletWrappingController to allow use of GeoServer's config dir.
 *
 * @author Alan Gerber, The Open Planning Project
 *
 */
public class PrintingServletWrappingController extends
ServletWrappingController {

    private static final String DEFAULT_CONFIG_FILENAME = "default-config.yaml";

    private static final String CREATE_URL = "/create.json";
    private static final String INFO_URL = "/info.json";

    private Logger LOG = org.geotools.util.logging.Logging.getLogger("org.geoserver.printing");

    private String configPropOrig;
    private Properties initParametersOrig;

    @Override
    public void setInitParameters(Properties initParameters) {
        // find the config parameter and update it so it points to
        // $GEOSERVER_DATA_DIR/printing/$CONFIG
        this.configPropOrig = initParameters.getProperty("config");

        initParameters = setConfig(initParameters, this.configPropOrig);
        this.initParametersOrig = initParameters;

        super.setInitParameters(initParameters);
    }

    private Properties setConfig(Properties initParameters, String configProp) {
      try {
          File dir = GeoserverDataDirectory.findCreateConfigDir("printing");
          String qualifiedConfigFilename = new File(configProp).getName();
          File qualifiedConfig = new File(dir, qualifiedConfigFilename);
          if (!qualifiedConfig.exists() && DEFAULT_CONFIG_FILENAME.equals(configProp)) {
              // just copy the default config, if it does not exist
              InputStream conf = getClass().getResourceAsStream(DEFAULT_CONFIG_FILENAME);
              IOUtils.copy(conf, qualifiedConfig);
          }
          if (!qualifiedConfig.canRead()) {
              LOG.warning("Printing module missing its configuration '"
                  + qualifiedConfig.getCanonicalPath()
                  + "'.  Any actions it takes will fail.");
              return initParameters;
          }
          initParameters.setProperty("config", qualifiedConfig.getCanonicalPath());
      } catch(org.vfny.geoserver.global.ConfigurationException e){
          LOG.warning("Explosion while attempting to access/create config directory for MapFish " +
                  "printing module.  Module will fail when run. Config exception is: " + e);
      } catch(java.io.IOException e){
          LOG.warning("Explosion while calculating canonical path for MapFish printing servlet. " +
                  "Module will fail when run.  IO Exception is: " + e);
      }
      return initParameters;
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final String additionalPath = request.getPathInfo();
        synchronized (additionalPath) {
            if (additionalPath.equals(CREATE_URL) || additionalPath.equals(INFO_URL)) {
                // just react at creation and info
                String configProp = request.getParameter("config");
                Properties initParameters = this.initParametersOrig;
                if (configProp != null) {
                    LOG.info("Set config to '" + configProp + "'");
                } else {
                    LOG.info("Use original config '" + this.configPropOrig + "'");
                    // use original configuration
                    configProp = this.configPropOrig;
                }
                // set initParameters
                initParameters = setConfig(initParameters, configProp);
                super.setInitParameters(initParameters);
                super.afterPropertiesSet();
            }
            return super.handleRequestInternal(request, response);
        }
    }
}
