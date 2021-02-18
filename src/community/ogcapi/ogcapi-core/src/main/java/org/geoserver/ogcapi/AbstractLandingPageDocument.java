/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A class representing a generic API service landing page in a way that Jackson can easily
 * translate to JSON/YAML (and can be used as a Freemarker template model)
 */
@JsonPropertyOrder({"title", "description", "links"})
public class AbstractLandingPageDocument extends AbstractLandingPageDocumentNoConformance {

    public AbstractLandingPageDocument(String title, String description, String serviceBase) {
        super(title, description, serviceBase);
        // conformance
        addLinksFor(
                serviceBase + "/conformance",
                ConformanceDocument.class,
                "Conformance declaration as ",
                "conformance",
                null,
                Link.REL_CONFORMANCE_URI);
    }
}
