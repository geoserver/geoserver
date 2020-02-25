/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.config.util.LegacyServicesReader;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;

/**
 * Imports logging configuration from the legacy services.xml file.
 *
 * @author Justin Deoliveira, OpenGEO
 */
public class LegacyLoggingImporter {

    /** logger */
    static Logger LOGGER = Logging.getLogger("org.geoserver.confg");

    private String configFileName;

    private String logFile;

    private Boolean suppressStdOutLogging;

    private LegacyServicesReader reader(File dir) throws Exception {
        // services.xml
        File servicesFile = new File(dir, "services.xml");
        if (!servicesFile.exists()) {
            throw new FileNotFoundException(
                    "Could not find services.xml under:" + dir.getAbsolutePath());
        }

        // create a services.xml reader
        LegacyServicesReader reader = new LegacyServicesReader();
        reader.read(servicesFile);

        return reader;
    }

    public void imprt(File dir) throws Exception {
        LegacyServicesReader reader = reader(dir);
        Map<String, Object> global = reader.global();

        configFileName = (String) global.get("log4jConfigFile");
        logFile = (String) global.get("logLocation");

        suppressStdOutLogging =
                Converters.convert(global.get("suppressStdOutLogging"), Boolean.class);
    }

    public String getConfigFileName() {
        return configFileName;
    }

    public String getLogFile() {
        return logFile;
    }

    public Boolean getSuppressStdOutLogging() {
        return suppressStdOutLogging;
    }
}
