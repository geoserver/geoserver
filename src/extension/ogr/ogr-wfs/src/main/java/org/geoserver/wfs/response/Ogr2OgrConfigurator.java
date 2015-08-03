/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.InputStream;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.config.util.SecureXStream;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotification;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

import com.thoughtworks.xstream.XStream;

/**
 * Loads the ogr2ogr.xml configuration file and configures the output format accordingly.
 *
 * <p>Also keeps tabs on the configuration file, reloading the file as needed.
 * @author Administrator
 *
 */
public class Ogr2OgrConfigurator implements ApplicationListener<ContextClosedEvent> {
    private static final Logger LOGGER = Logging.getLogger(Ogr2OgrConfigurator.class);

    public Ogr2OgrOutputFormat of;

    OGRWrapper wrapper;

    Resource configFile;

    // ConfigurationPoller
    private ResourceListener listener = new ResourceListener() {
        public void changed(ResourceNotification notify) {
            loadConfiguration();
        }
    };

    public Ogr2OgrConfigurator(Ogr2OgrOutputFormat format) {
        this.of = format;

        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        configFile = loader.get("ogr2ogr.xml");
        loadConfiguration();
        configFile.addListener( listener );
    }

    public void loadConfiguration() {
        // start with the default configuration, override if we can load the file
        OgrConfiguration configuration = OgrConfiguration.DEFAULT;
        try {
            if (configFile.getType() == Type.RESOURCE) {
                InputStream in = configFile.in();
                try {
                    XStream xstream = buildXStream();
                    configuration = (OgrConfiguration) xstream.fromXML( in);
                }
                finally {
                    in.close();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reading the ogr2ogr.xml configuration file", e);
        }

        if (configuration == null) {
            LOGGER.log(Level.INFO,
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
        XStream xstream = new SecureXStream();
        xstream.alias("OgrConfiguration", OgrConfiguration.class);
        xstream.alias("Format", OgrFormat.class);
        xstream.allowTypes(new Class[] { OgrConfiguration.class, OgrFormat.class });
        xstream.addImplicitCollection(OgrFormat.class, "options", "option", String.class);
        return xstream;
    }

    /**
     * Kill all threads on web app context shutdown to avoid permgen leaks
     */
    public void onApplicationEvent(ContextClosedEvent event) {
        if( configFile != null ){
            configFile.removeListener(listener);
        }
    }

}
