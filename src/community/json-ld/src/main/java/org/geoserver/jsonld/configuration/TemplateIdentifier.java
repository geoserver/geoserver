/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jsonld.configuration;

/** This enum provides constants which match output formats with template names */
public enum TemplateIdentifier {
    GEOJSON("application/json", "geojson-template.json"),
    JSONLD("application/ld+json", "json-ld-template.json");

    private String outputFormat;
    private String filename;

    TemplateIdentifier(String outputFormat, String filename) {
        this.outputFormat = outputFormat;
        this.filename = filename;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public String getFilename() {
        return filename;
    }
}
