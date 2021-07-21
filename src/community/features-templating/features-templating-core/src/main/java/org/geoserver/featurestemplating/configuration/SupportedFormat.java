/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import java.util.Arrays;
import java.util.List;

public enum SupportedFormat {
    JSONLD("JSON-LD"),
    GML("GML"),
    GEOJSON("GeoJSON"),
    HTML("HTML");

    private String format;

    SupportedFormat(String format) {
        this.format = format;
    }

    public String getFormat() {
        return this.format;
    }

    public static List<SupportedFormat> getByExtension(String extensions) {
        if (extensions == null) return Arrays.asList(SupportedFormat.values());
        else if (extensions.equals("xml")) return Arrays.asList(GML);
        else if (extensions.equals("xhtml")) return Arrays.asList(HTML);
        else return Arrays.asList(JSONLD, GEOJSON);
    }

    public static String toExtension(SupportedFormat format) {
        if (format.equals(SupportedFormat.GML)) return "xml";
        else return "json";
    }
}
