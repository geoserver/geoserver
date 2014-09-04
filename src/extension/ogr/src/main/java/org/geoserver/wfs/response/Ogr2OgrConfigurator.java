/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.File;
import java.io.FileInputStream;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.vfny.geoserver.global.GeoserverDataDirectory;

import com.thoughtworks.xstream.XStream;

/**
 * Loads the ogr2ogr.xml configuration file and configures the output format accordingly.
 * <p>Also keeps tabs on the configuration file, reloading the file as needed. 
 * @author Administrator
 *
 */
public class Ogr2OgrConfigurator implements ApplicationListener {
    private static final Logger LOGGER = Logging.getLogger(Ogr2OgrConfigurator.class);

    Ogr2OgrOutputFormat of;

    OGRWrapper wrapper;

    File configFile;
    
    Timer timer;

    public Ogr2OgrConfigurator(Ogr2OgrOutputFormat format, long pollInterval) {
        this.of = format;
        configFile = new File(GeoserverDataDirectory.getGeoserverDataDirectory(), "ogr2ogr.xml");
        timer = new Timer(true);
        timer.schedule(new ConfigurationPoller(), pollInterval);
    }
    
    public Ogr2OgrConfigurator(Ogr2OgrOutputFormat format) {
        this(format, 1000);
    }

    protected void loadConfiguration() {
        // start with the default configuration, override if we can load the file
        OgrConfiguration configuration = OgrConfiguration.DEFAULT;
        try {
            if (configFile.exists()) {
                XStream xstream = buildXStream();
                configuration = (OgrConfiguration) xstream.fromXML(new FileInputStream(configFile));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reading the ogr2ogr.xml configuration file", e);
        }

        if (configuration == null) {
            LOGGER
                    .log(Level.INFO,
                            "Could not find/load the ogr2ogr.xml configuration file, using internal defaults");
        }

        // let's load the configuration
        OGRWrapper wrapper = new OGRWrapper(configuration.ogr2ogrLocation, configuration.gdalData);
        Set<String> supported = wrapper.getSupportedFormats();
        of.setOgrExecutable(configuration.ogr2ogrLocation);
        of.setGdalData(configuration.gdalData);
        of.clearFormats();
        for (OgrFormat format : configuration.formats) {
            if (supported.contains(format.ogrFormat)) {
                of.addFormat(format);
            } else {
                LOGGER.severe("Skipping '" + format.formatName + "' as its OGR format '"
                        + format.ogrFormat + "' is not among the ones supported by "
                        + configuration.ogr2ogrLocation);
            }
        }
    }

    /**
     * Builds and configures the XStream used for de-serializing the configuration
     * @return
     */
    static XStream buildXStream() {
        XStream xstream = new XStream();
        xstream.alias("OgrConfiguration", OgrConfiguration.class);
        xstream.alias("Format", OgrFormat.class);
        xstream.addImplicitCollection(OgrFormat.class, "options", "option", String.class);
        return xstream;
    }
    
    private class ConfigurationPoller extends TimerTask {
        Long lastModified = null;
        
        public ConfigurationPoller() {
            run();
        }
        
        public void run() {
            long newLastModified = configFile.exists() ? configFile.lastModified() : -1;
            if(lastModified == null || newLastModified != lastModified) {
                lastModified = newLastModified;
                loadConfiguration();
            }
        }
    }

    /**
     * Kill all threads on web app context shutdown to avoid permgen leaks
     */
    public void onApplicationEvent(ApplicationEvent event) {
        if(event instanceof ContextClosedEvent) {
            timer.cancel();
        }
    }

}
