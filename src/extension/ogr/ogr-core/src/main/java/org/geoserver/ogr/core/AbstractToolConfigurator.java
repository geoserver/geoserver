/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogr.core;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.extended.NamedMapConverter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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

/**
 * Loads the tool configuration file and configures the output formats accordingly.
 *
 * <p>Also keeps tabs on the configuration file, reloading it as needed.
 *
 * @author Andrea Aime, GeoSolutions
 * @author Stefano Costa, GeoSolutions
 */
public abstract class AbstractToolConfigurator implements ApplicationListener<ContextClosedEvent> {
    private static final Logger LOGGER = Logging.getLogger(AbstractToolConfigurator.class);

    public FormatConverter of;

    protected ToolWrapperFactory wrapperFactory;

    protected Resource configFile;

    // ConfigurationPoller
    protected ResourceListener listener =
            new ResourceListener() {
                public void changed(ResourceNotification notify) {
                    loadConfiguration();
                }
            };

    /** @param formatConverter the format converter tool */
    public AbstractToolConfigurator(
            FormatConverter formatConverter, ToolWrapperFactory wrapperFactory) {
        this.of = formatConverter;
        this.wrapperFactory = wrapperFactory;

        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        configFile = loader.get(getConfigurationFile());
        loadConfiguration();
        configFile.addListener(listener);
    }

    /** @return the name of the tool configuration file, relative to GeoServer's data directory. */
    protected abstract String getConfigurationFile();

    /** @return the tool's default configuration */
    protected abstract ToolConfiguration getDefaultConfiguration();

    /** Loads configuration from file, if any; otherwise, loads internal defaults. */
    public void loadConfiguration() {
        // start with the default configuration, override if we can load the file
        ToolConfiguration configuration = getDefaultConfiguration();
        try {
            if (configFile.getType() == Type.RESOURCE) {
                InputStream in = configFile.in();
                try {
                    XStream xstream = buildXStream();
                    configuration = (ToolConfiguration) xstream.fromXML(in);
                } finally {
                    in.close();
                }
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Error reading the " + getConfigurationFile() + " configuration file",
                    e);
        }

        if (configuration == null) {
            LOGGER.log(
                    Level.INFO,
                    "Could not find/load the "
                            + getConfigurationFile()
                            + " configuration file, using internal defaults");
            configuration = getDefaultConfiguration();
        }

        // should never happen, but just in case...
        if (configuration == null) {
            throw new IllegalStateException("No default configuration available, giving up");
        }

        // let's load the configuration
        ToolWrapper wrapper =
                wrapperFactory.createWrapper(
                        configuration.getExecutable(), configuration.getEnvironment());
        Set<String> supported = wrapper.getSupportedFormats();
        of.setExecutable(configuration.getExecutable());
        of.setEnvironment(configuration.getEnvironment());
        List<Format> toBeAdded = new ArrayList<Format>();
        for (Format format : configuration.getFormats()) {
            if (supported.contains(format.getToolFormat())) {
                toBeAdded.add(format);
            } else {
                LOGGER.severe(
                        "Skipping '"
                                + format.getGeoserverFormat()
                                + "' as its tool format '"
                                + format.getToolFormat()
                                + "' is not among the ones supported by "
                                + configuration.getExecutable());
            }
        }
        // update configured formats at once, potentially alleviating locking overhead
        of.replaceFormats(toBeAdded);
    }

    /** Builds and configures the XStream used for de-serializing the configuration */
    protected XStream buildXStream() {
        XStream xstream = new SecureXStream();
        xstream.alias("ToolConfiguration", ToolConfiguration.class);
        xstream.alias("Format", Format.class);
        xstream.allowTypes(new Class[] {ToolConfiguration.class, Format.class});
        xstream.allowTypeHierarchy(FormatAdapter.class);
        xstream.addImplicitCollection(Format.class, "options", "option", String.class);
        NamedMapConverter environmentConverter =
                new NamedMapConverter(
                        xstream.getMapper(),
                        "variable",
                        "name",
                        String.class,
                        "value",
                        String.class,
                        true,
                        true,
                        xstream.getConverterLookup());
        xstream.registerConverter(environmentConverter);

        return xstream;
    }

    /** Kill all threads on web app context shutdown to avoid permgen leaks */
    public void onApplicationEvent(ContextClosedEvent event) {
        if (configFile != null) {
            configFile.removeListener(listener);
        }
    }
}
