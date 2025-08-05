/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.ogcapi;

/** Simple POJO to hold the output format and the schema content for a specific schema override */
public class OGCSchemaOverride {

    private final String outputFormat;
    private final String content;

    public OGCSchemaOverride(String outputFormat, String content) {
        this.outputFormat = outputFormat;
        this.content = content;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public String getContent() {
        return content;
    }
}
