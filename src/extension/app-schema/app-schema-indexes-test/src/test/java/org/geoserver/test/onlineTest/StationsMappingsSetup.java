/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class StationsMappingsSetup {

    public static final String MAPPING_FILE_NAME = "mappings.xml";
    public static final String INCLUDED_FILE_NAME = "includedTypes.xml";

    public void setupMapping(
            String solrUrl, String solrCoreName, PostgresqlProperties pgProps, File testDir) {
        // replace mappings.xml placeholders
        String mappingsContent = loadFileAsString("test-data/" + MAPPING_FILE_NAME);
        mappingsContent = StringUtils.replace(mappingsContent, "${solr_url}", solrUrl);
        mappingsContent = StringUtils.replace(mappingsContent, "${solr_core}", solrCoreName);
        mappingsContent = replacePgPlaceholders(mappingsContent, pgProps);
        // replace includedTypes.xml placeholders
        String includedContent = loadFileAsString("test-data/" + INCLUDED_FILE_NAME);
        includedContent = replacePgPlaceholders(includedContent, pgProps);
        try {
            // save as mappings.xml final file
            Path path = Paths.get(testDir.getAbsolutePath(), MAPPING_FILE_NAME);
            Files.write(path, mappingsContent.getBytes());
            // save as includedTypes.xml final file
            Path itpath = Paths.get(testDir.getAbsolutePath(), INCLUDED_FILE_NAME);
            Files.write(itpath, includedContent.getBytes());
            // create app-schema-cache directory
            Path dirpath = Paths.get(testDir.getAbsolutePath(), "app-schema-cache");
            Files.createDirectories(dirpath);
            copyRelatedFiles(testDir.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String replacePgPlaceholders(String mappingsContent, PostgresqlProperties pgProps) {
        mappingsContent = StringUtils.replace(mappingsContent, "${pg_host}", pgProps.getHost());
        mappingsContent = StringUtils.replace(mappingsContent, "${pg_port}", pgProps.getPort());
        mappingsContent =
                StringUtils.replace(mappingsContent, "${pg_database}", pgProps.getDatabase());
        mappingsContent = StringUtils.replace(mappingsContent, "${pg_schema}", pgProps.getSchema());
        mappingsContent = StringUtils.replace(mappingsContent, "${pg_user}", pgProps.getUser());
        mappingsContent =
                StringUtils.replace(mappingsContent, "${pg_password}", pgProps.getPassword());
        return mappingsContent;
    }

    /**
     * Copy remaining related files from test-data to temp test directory
     *
     */
    private void copyRelatedFiles(String testDirPath) throws IOException {
        // meteo.xsd
        copyFile("meteo.xsd", testDirPath);
    }

    private void copyFile(String fileName, String testDirPath) throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("test-data/" + fileName);
        Path target = Paths.get(testDirPath, fileName);
        Files.copy(in, target);
    }

    public String loadFileAsString(String uri) {
        String content = null;
        try {
            content =
                    IOUtils.toString(
                            getClass().getClassLoader().getResourceAsStream(uri),
                            StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error loading mapping file.", e);
        }
        return content;
    }
}
