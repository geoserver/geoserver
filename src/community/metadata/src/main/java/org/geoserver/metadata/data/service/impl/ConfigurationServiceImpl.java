/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import org.apache.wicket.Application;
import org.apache.wicket.IApplicationListener;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.metadata.data.dto.AttributeCollection;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.dto.AttributeMappingConfiguration;
import org.geoserver.metadata.data.dto.AttributeTypeConfiguration;
import org.geoserver.metadata.data.dto.AttributeTypeMappingConfiguration;
import org.geoserver.metadata.data.dto.CustomNativeMappingsConfiguration;
import org.geoserver.metadata.data.dto.GeonetworkConfiguration;
import org.geoserver.metadata.data.dto.GeonetworkMappingConfiguration;
import org.geoserver.metadata.data.dto.MetadataConfiguration;
import org.geoserver.metadata.data.dto.NamespaceConfiguration;
import org.geoserver.metadata.data.dto.impl.CustomNativeMappingsConfigurationImpl;
import org.geoserver.metadata.data.dto.impl.GeonetworkMappingConfigurationImpl;
import org.geoserver.metadata.data.dto.impl.MetadataConfigurationImpl;
import org.geoserver.metadata.data.service.ConfigurationService;
import org.geoserver.metadata.web.resource.WicketResourceResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service responsible for interaction with yaml files. It will search for all *.yaml files in a
 * given directory and try to parse the files. Yaml files that cannot do parsed will be ignored.
 *
 * @author Timothy De Bock
 */
@Component
public class ConfigurationServiceImpl implements ConfigurationService {

    private static final java.util.logging.Logger LOGGER =
            Logging.getLogger(ConfigurationServiceImpl.class);

    private static final String SEPARATOR = ";";

    @Autowired private GeoServerDataDirectory dataDirectory;

    @Autowired private Application application;

    private MetadataConfiguration configuration;

    private GeonetworkMappingConfiguration geonetworkMappingConfig;

    private CustomNativeMappingsConfiguration customNativeMappingsConfig;

    private Resource getFolder() {
        return dataDirectory.get(MetadataConstants.DIRECTORY);
    }

    @PostConstruct
    public void init() {
        readCustomTranslations();
        readConfiguration();
        getFolder()
                .addListener(
                        new ResourceListener() {
                            @Override
                            public void changed(ResourceNotification notify) {
                                readConfiguration();
                            }
                        });
    }

    private void readCustomTranslations() {
        application
                .getApplicationListeners()
                .add(
                        new IApplicationListener() {

                            @Override
                            public void onAfterInitialized(Application application) {
                                Resource metadataFolder =
                                        dataDirectory.get(MetadataConstants.DIRECTORY);
                                WicketResourceResourceLoader loader =
                                        new WicketResourceResourceLoader(
                                                metadataFolder, "metadata");
                                loader.setShouldThrowException(false);
                                application
                                        .getResourceSettings()
                                        .getStringResourceLoaders()
                                        .add(0, loader);
                            }

                            @Override
                            public void onBeforeDestroyed(Application application) {}
                        });
    }

    @Override
    public MetadataConfiguration getMetadataConfiguration() {
        return configuration;
    }

    @Override
    public GeonetworkMappingConfiguration getGeonetworkMappingConfiguration() {
        return geonetworkMappingConfig;
    }

    @Override
    public CustomNativeMappingsConfiguration getCustomNativeMappingsConfiguration() {
        return customNativeMappingsConfig;
    }

    private void readConfiguration() {
        Resource folder = getFolder();
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        List<Resource> files = Resources.list(folder, new Resources.ExtensionFilter("YAML"));
        Collections.sort(files, (o1, o2) -> o1.name().compareTo(o2.name()));

        configuration = new MetadataConfigurationImpl();

        geonetworkMappingConfig = new GeonetworkMappingConfigurationImpl();

        customNativeMappingsConfig = new CustomNativeMappingsConfigurationImpl();

        for (Resource file : files) {
            try (InputStream in = file.in()) {
                readConfiguration(in, mapper);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
            try (InputStream in = file.in()) {
                readMapping(in, mapper);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
            try (InputStream in = file.in()) {
                readingCustomNativeMapping(in, mapper);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        // add feature catalog
        try (InputStream in =
                getClass().getResourceAsStream(MetadataConstants.FEATURE_CATALOG_CONFIG_FILE)) {
            readConfiguration(in, mapper);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        // process csv imports
        processCsvImports(configuration);
        for (AttributeTypeConfiguration type : configuration.getTypes()) {
            processCsvImports(type);
        }
    }

    private void readingCustomNativeMapping(InputStream in, ObjectMapper mapper)
            throws IOException {
        CustomNativeMappingsConfiguration config =
                mapper.readValue(in, CustomNativeMappingsConfiguration.class);
        customNativeMappingsConfig
                .getCustomNativeMappings()
                .addAll(config.getCustomNativeMappings());
    }

    private void readConfiguration(InputStream in, ObjectMapper mapper) throws IOException {
        MetadataConfiguration config = mapper.readValue(in, MetadataConfigurationImpl.class);
        // Merge attribute configuration and remove duplicates
        Set<String> attributeKeys = new HashSet<>();
        for (AttributeConfiguration attribute : config.getAttributes()) {
            if (attribute.getKey() == null) {
                throw new IOException(
                        "The key of an attribute may not be null. " + attribute.getLabel());
            }
            if (attribute.getLabel() == null) {
                attribute.setLabel(attribute.getKey());
            }
            if (!attributeKeys.contains(attribute.getKey())) {
                configuration.getAttributes().add(attribute);
                attributeKeys.add(attribute.getKey());
            }
        }

        // Merge geonetwork configuration and remove duplicates
        Set<String> geonetworkKeys = new HashSet<>();
        for (GeonetworkConfiguration geonetwork : config.getGeonetworks()) {
            if (!geonetworkKeys.contains(geonetwork.getName())) {
                configuration.getGeonetworks().add(geonetwork);
                geonetworkKeys.add(geonetwork.getName());
            }
        }
        // Merge Types configuration and remove duplicates
        Set<String> typesKeys = new HashSet<>();
        for (AttributeTypeConfiguration type : config.getTypes()) {
            if (!typesKeys.contains(type.getTypename())) {
                for (AttributeConfiguration attribute : type.getAttributes()) {
                    if (attribute.getLabel() == null) {
                        attribute.setLabel(attribute.getKey());
                    }
                }
                configuration.getTypes().add(type);
                typesKeys.add(type.getTypename());
            }
        }

        // merge csv imports
        for (String csvImport : config.getCsvImports()) {
            configuration.getCsvImports().add(csvImport);
        }
    }

    private void readMapping(InputStream in, ObjectMapper mapper) throws IOException {
        GeonetworkMappingConfiguration config =
                mapper.readValue(in, GeonetworkMappingConfigurationImpl.class);
        for (AttributeMappingConfiguration mapping : config.getGeonetworkmapping()) {
            geonetworkMappingConfig.getGeonetworkmapping().add(mapping);
        }

        Set<String> objectKay = new HashSet<>();
        for (AttributeTypeMappingConfiguration mapping : config.getObjectmapping()) {
            if (!objectKay.contains(mapping.getTypename())) {
                geonetworkMappingConfig.getObjectmapping().add(mapping);
                objectKay.add(mapping.getTypename());
            }
        }

        Set<String> nsPrefixes = new HashSet<>();
        for (NamespaceConfiguration ns : config.getNamespaces()) {
            if (!nsPrefixes.contains(ns.getPrefix())) {
                geonetworkMappingConfig.getNamespaces().add(ns);
                nsPrefixes.add(ns.getPrefix());
            }
        }
    }

    private void processCsvImports(AttributeCollection mapping) {
        for (String csvImport : mapping.getCsvImports()) {
            try (InputStream in = getFolder().get(csvImport).in()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String line = br.readLine();
                if (line == null) {
                    LOGGER.log(Level.WARNING, "No content found for " + csvImport);
                    return;
                }
                String[] splitLine = br == null ? null : line.split(SEPARATOR);
                if (splitLine != null && splitLine.length > 0) {
                    AttributeConfiguration[] configs = new AttributeConfiguration[splitLine.length];
                    for (int i = 0; i < splitLine.length; i++) {
                        configs[i] = mapping.findAttribute(splitLine[i].trim());
                        if (configs[i] != null) {
                            configs[i].getValues().clear();
                        }
                    }

                    while ((line = br.readLine()) != null) {
                        splitLine = line.split(SEPARATOR);
                        for (int i = 0; i < configs.length; i++) {
                            if (configs[i] != null)
                                configs[i]
                                        .getValues()
                                        .add(i < splitLine.length ? splitLine[i].trim() : null);
                        }
                    }
                }

            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }
}
