/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;

public class GeofenceTestUtils {

    public static String readConfig(String fileName)
            throws URISyntaxException, FileNotFoundException, IOException {
        File configFile = new File(GeofenceTestUtils.class.getResource("/" + fileName).toURI());

        BufferedReader reader = null;
        StringBuilder content = new StringBuilder();
        try {
            reader = new BufferedReader(new FileReader(configFile));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line.trim());
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return content.toString();
    }

    public static String readConfig(File configFile)
            throws URISyntaxException, FileNotFoundException, IOException {

        BufferedReader reader = null;
        StringBuilder content = new StringBuilder();
        try {
            reader = new BufferedReader(new FileReader(configFile));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line.trim());
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return content.toString();
    }

    public static File emptyFile(String fileName) throws URISyntaxException, IOException {
        if (GeofenceTestUtils.class.getResource("/" + fileName) != null) {
            File file = new File(GeofenceTestUtils.class.getResource("/" + fileName).toURI());
            FileWriter writer = null;
            try {
                writer = new FileWriter(file);
                writer.write("");
                return file;
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }
        return null;
    }
}
