/*
 *  Copyright (C) 2007 - 2014 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * 
 *  GPLv3 + Classpath exception
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
