/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.featurestemplating.configuration;

import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;

/** This enum provides constants which match output formats with template names */
public enum TemplateIdentifier {
    JSON("application/json", "geojson-template.json"),
    GEOJSON("application/geo+json", "geojson-template.json"),
    JSONLD("application/ld+json", "json-ld-template.json"),
    GML32("application/gml+xml;version=3.2", "gml32-template.xml"),
    GML31("gml3", "gml31-template.xml"),
    GML2("GML2text/xml;subtype=gml/2.1.2", "gml2-template.xml"),
    HTML("text/html", "html-template.xhtml");

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
    public static TemplateIdentifier fromOutputFormat(String outputFormat) {
        if (outputFormat == null) {
            return null;
        }
        TemplateIdentifier identifier = null;
        String trimOutputFormat = outputFormat.trim().replaceAll(" ", "");
        if (trimOutputFormat.equalsIgnoreCase(TemplateIdentifier.JSON.getOutputFormat()))
            identifier = TemplateIdentifier.JSON;
        else if (trimOutputFormat.equalsIgnoreCase(TemplateIdentifier.JSONLD.getOutputFormat()))
            identifier = TemplateIdentifier.JSONLD;
        else if (trimOutputFormat.equalsIgnoreCase(TemplateIdentifier.GEOJSON.getOutputFormat()))
            identifier = TemplateIdentifier.GEOJSON;
        else if (isGML32(trimOutputFormat)) identifier = TemplateIdentifier.GML32;
        else if (isGML31(trimOutputFormat)) identifier = TemplateIdentifier.GML31;
        else if (isGML2(trimOutputFormat)) identifier = TemplateIdentifier.GML2;
        else if (TemplateIdentifier.HTML.getOutputFormat().equals(trimOutputFormat))
            identifier = TemplateIdentifier.HTML;
        return identifier;
    }

    private static boolean isGML2(String outputFormat) {
        Request request = Dispatcher.REQUEST.get();
        boolean isFeatureInfo = request != null && "GetFeatureInfo".equalsIgnoreCase(request.getRequest());
        boolean result = false;
        if (TemplateIdentifier.GML2.getOutputFormat().contains(outputFormat)) result = true;
        else if (isFeatureInfo && "text/xml".equals(outputFormat)) result = true;
        return result;
    }

    private static boolean isGML32(String outputFormat) {
        return TemplateIdentifier.GML32.getOutputFormat().contains(outputFormat);
    }

    private static boolean isGML31(String outputFormat) {
        return outputFormat.equalsIgnoreCase(TemplateIdentifier.GML31.getOutputFormat())
                || outputFormat.equalsIgnoreCase("text/xml;subtype=gml/3.1.1")
                || outputFormat.equalsIgnoreCase("application/vnd.ogc.gml/3.1.1");
    }
}
