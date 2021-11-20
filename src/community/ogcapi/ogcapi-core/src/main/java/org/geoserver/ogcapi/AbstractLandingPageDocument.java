/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Generic API service landing page with conformance details.
 *
 * <p>Document provided for Jackson translate to JSON/YAML (also used as a Freemarker template
 * model).
 */
@JsonPropertyOrder({"title", "description", "links"})
public class AbstractLandingPageDocument extends AbstractLandingPageDocumentNoConformance {

    public AbstractLandingPageDocument(String title, String description, String serviceBase) {
        super(title, description, serviceBase);
        // conformance
        new LinksBuilder(ConformanceDocument.class, serviceBase)
                .segment("/conformance")
                .title("Conformance declaration as ")
                .rel(Link.REL_CONFORMANCE_URI)
                .add(this);
    }
}
