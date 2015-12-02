package org.geoserver.gwc.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geoserver.data.util.IOUtils;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.Filter;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.config.ConfigurationResourceProvider;

public class GeoserverXMLResourceProvider implements ConfigurationResourceProvider {

    private static Log log = LogFactory.getLog(org.geowebcache.config.XMLFileResourceProvider.class);
    
    public static final String DEFAULT_CONFIGURATION_DIR_NAME = "gwc";
    
    /**
     * Location of the configuration file
     */
    private final Resource configDirectory;

    /**
     * Name of the configuration file
     */
    private final String configFileName;
    
    private String templateLocation;
    
    public GeoserverXMLResourceProvider(final String configFileDirectory,
            final String configFileName,
            final ResourceStore resourceStore) throws ConfigurationException {
        
        this.configFileName = configFileName;
        
        if(configFileDirectory != null) {
            // Use the given path
            if ((new File(configFileDirectory)).isAbsolute()) {
                
                log.info("Provided configuration directory as absolute path '" + configFileDirectory + "'");
                this.configDirectory = Files.asResource(new File(configFileDirectory));
            } else {
                log.info("Provided configuration directory in the resource store. ");
                this.configDirectory = resourceStore.get(configFileDirectory);
            }
        } else {
            this.configDirectory = resourceStore.get(DEFAULT_CONFIGURATION_DIR_NAME);
        }
        log.info("Will look for geowebcache.xml in '" + configDirectory + "'");
    }
    
    public GeoserverXMLResourceProvider(final String configFileName,
            final ResourceStore resourceStore) throws ConfigurationException {
        this(null, configFileName, resourceStore);        
    }
   

    @Override
    public InputStream in() throws IOException {
        return findOrCreateConfFile().in();
    }

    @Override
    public OutputStream out() throws IOException {
        return findOrCreateConfFile().out();
    }

    @Override
    public void backup() throws IOException {
        backUpConfig(findOrCreateConfFile());
    }

    @Override
    public String getId() {
        return configDirectory.path();
    }

    @Override
    public void setTemplate(final String templateLocation) {
        this.templateLocation = templateLocation;
    }

    private Resource findConfigFile() throws IOException {
        return configDirectory.get(configFileName);
    }
    
    public String getLocation() throws IOException {
        return findConfigFile().path();
    }

    private Resource findOrCreateConfFile() throws IOException {
        Resource xmlFile = findConfigFile();

        if (Resources.exists(xmlFile)) {
            log.info("Found configuration file in " + configDirectory.path());
        } else if (templateLocation != null) {
            log.warn("Found no configuration file in config directory, will create one at '"
                    + xmlFile.path() + "' from template "
                    + getClass().getResource(templateLocation).toExternalForm());
            // grab template from classpath
            try {
                 IOUtils.copy(getClass().getResourceAsStream(templateLocation), 
                        xmlFile.out());
            } catch (IOException e) {
                throw new IOException("Error copying template config to "
                        + xmlFile.path(), e);
            }
        } 

        return xmlFile;
    }
    

    private void backUpConfig(final Resource xmlFile) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss").format(new Date());
        String backUpFileName = "geowebcache_" + timeStamp + ".bak";
        Resource parentFile = xmlFile.parent();

        log.debug("Backing up config file " + xmlFile.name() + " to " + backUpFileName);

        List<Resource> previousBackUps = Resources.list(parentFile, new Filter<Resource>() {
            public boolean accept(Resource res) {
                if (configFileName.equals(res.name())) {
                    return false;
                }
                if (res.name().startsWith(configFileName) && res.name().endsWith(".bak")) {
                    return true;
                }
                return false;
            }
        });

        final int maxBackups = 10;
        if (previousBackUps.size() > maxBackups) {
            Collections.sort(previousBackUps, new Comparator<Resource>() {

                @Override
                public int compare(Resource o1, Resource o2) {
                    return (int) (o1.lastmodified() - o2.lastmodified());
                }
                
            });
            Resource oldest = previousBackUps.get(0);
            log.debug("Deleting oldest config backup " + oldest + " to keep a maximum of "
                    + maxBackups + " backups.");
            oldest.delete();
        }

        Resource backUpFile = parentFile.get(backUpFileName);
        IOUtils.copy(xmlFile.in(), backUpFile.out());
        log.debug("Config backup done");
    }

    @Override
    public boolean hasInput() {
        return true;
    }

    @Override
    public boolean hasOutput() {
        return true;
    }


}
