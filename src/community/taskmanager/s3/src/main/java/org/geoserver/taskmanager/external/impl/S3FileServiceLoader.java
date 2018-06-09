/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external.impl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import org.geoserver.taskmanager.external.FileService;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class S3FileServiceLoader {

    private static final Logger LOGGER = Logging.getLogger(S3FileServiceLoader.class);

    /** Read the properties of the s3-geotiff module since it will be needed most of the times. */
    public static final String S3_GEOTIFF_CONFIG_PATH = "s3.properties.location";

    @Autowired private LookupFileServiceImpl lookupFileService;

    @PostConstruct
    public void initializeS3GeotiffFileServices() {
        Properties prop = readProperties();
        prop.stringPropertyNames()
                .stream()
                .filter(key -> key.endsWith(".s3.rootfolder"))
                .forEach(
                        key ->
                                Arrays.stream(prop.getProperty(key).split(","))
                                        .forEach(
                                                rootfolder ->
                                                        addS3FileService(
                                                                prop,
                                                                key.replace(".s3.rootfolder", ""),
                                                                rootfolder)));
    }

    private void addS3FileService(Properties properties, String prefix, String rootfolder) {
        ArrayList<FileService> fileServices = new ArrayList<>();
        S3FileServiceImpl fileService =
                new S3FileServiceImpl(
                        properties.getProperty(prefix + ".s3.endpoint"),
                        properties.getProperty(prefix + ".s3.user"),
                        properties.getProperty(prefix + ".s3.password"),
                        prefix,
                        rootfolder);
        fileServices.add(fileService);

        lookupFileService.setFileServices(fileServices);
    }

    private Properties readProperties() {
        Properties prop;
        try {
            prop = new Properties();
            String property = System.getProperty(S3_GEOTIFF_CONFIG_PATH);
            if (property != null) {
                InputStream resourceAsStream = new FileInputStream(property);
                prop.load(resourceAsStream);
            }
        } catch (IOException ex) {
            LOGGER.severe(ex.getMessage());
            throw new IllegalArgumentException("The properties could not be found.", ex);
        }
        return prop;
    }
}
