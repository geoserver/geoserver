/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.featurestemplating.configuration;

/** This enum provides constants which match output formats with template names */
public enum TemplateIdentifier {
    JSON("application/json", "geojson-template.json"),
    GEOJSON("application/geo+json", "geojson-template.json"),
    JSONLD("application/ld+json", "json-ld-template.json"),
    GML32("application/gml+xml;version=3.2", "gml32-template.xml"),
    GML31("gml3", "gml31-template.xml"),
    GML2("GML2text/xml;subtype=gml/2.1.2", "gml2-template.xml");

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

    /**
     * Find the templateIdentifier that correspond to the outputFormat.
     *
     * @param outputFormat the outputFormat for which to find a TemplateIdentifier.
     * @return the TemplateIdentifier matching the outputFormat.
     */
    public static TemplateIdentifier getTemplateIdentifierFromOutputFormat(String outputFormat) {
        TemplateIdentifier identifier = null;
        String trimOutputFormat = outputFormat.trim().replaceAll(" ", "");
        if (trimOutputFormat.equalsIgnoreCase(TemplateIdentifier.JSON.getOutputFormat()))
            identifier = TemplateIdentifier.JSON;
        else if (trimOutputFormat.equalsIgnoreCase(TemplateIdentifier.JSONLD.getOutputFormat()))
            identifier = TemplateIdentifier.JSONLD;
        else if (trimOutputFormat.equalsIgnoreCase(TemplateIdentifier.GEOJSON.getOutputFormat()))
            identifier = TemplateIdentifier.GEOJSON;
        else if (trimOutputFormat.equalsIgnoreCase(TemplateIdentifier.GML32.getOutputFormat()))
            identifier = TemplateIdentifier.GML32;
        else if (trimOutputFormat.equalsIgnoreCase(TemplateIdentifier.GML31.getOutputFormat()))
            identifier = TemplateIdentifier.GML31;
        else if (TemplateIdentifier.GML2.getOutputFormat().contains(trimOutputFormat))
            identifier = TemplateIdentifier.GML2;
        return identifier;
    }
}
