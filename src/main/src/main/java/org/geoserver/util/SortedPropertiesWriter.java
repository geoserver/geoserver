/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeSet;

public class SortedPropertiesWriter {

    /**
     * Stores the given properties to the output stream in sorted key order, with a properties-style timestamp header
     * and Unicode escaping, using UTF-8 encoding. The output will be in the format: #Mon Aug 26 10:16:17 SAST 2024
     *
     * @param props the Properties to write
     * @param out the OutputStream to write to (not closed)
     * @param comment an optional single-line comment to add at the top (without #)
     * @throws IOException if writing fails
     */
    public static void storeSorted(Properties props, OutputStream out, String comment) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        // Optional comment line
        if (comment != null && !comment.isEmpty()) {
            writer.write("# " + comment);
            writer.newLine();
        }
        // Timestamp line in Java properties format
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
        // Use local timezone, or set as needed. Example: Africa/Johannesburg for SAST.
        sdf.setTimeZone(TimeZone.getDefault());
        writer.write("#" + sdf.format(new Date()));
        writer.newLine();

        // Sort keys
        TreeSet<String> keys = new TreeSet<>(props.stringPropertyNames());
        for (String key : keys) {
            String value = props.getProperty(key);
            writer.write(escapeProperty(key) + "=" + escapeProperty(value));
            writer.newLine();
        }
        writer.flush();
    }

    /** Escapes a string for properties file output (handles Unicode, special chars, etc). */
    private static String escapeProperty(String str) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '=':
                    sb.append("\\=");
                    break;
                case ':':
                    sb.append("\\:");
                    break;
                case '#':
                    sb.append("\\#");
                    break;
                case '!':
                    sb.append("\\!");
                    break;
                case ' ':
                    sb.append("\\ ");
                    break;
                default:
                    if (c < 0x20 || c > 0x7e) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private SortedPropertiesWriter() {
        // Utility class, do not instantiate
    }
}
