/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.http.MediaType;

/**
 * Generic API service landing page for Jackson to easily translate to JSON/YAML (also used as a
 * Freemarker template model).
 *
 * @author bradh
 */
public class AbstractLandingPageDocumentNoConformance extends AbstractDocument {

    final String title;
    final String description;

    public AbstractLandingPageDocumentNoConformance(
            String title, String description, String serviceBase) {

        // self and alternate representations of landing page
        addSelfLinks(serviceBase + "/");
        // api
        new LinksBuilder(OpenAPI.class, serviceBase)
                .segment("/openapi")
                .title("API definition for this endpoint as ")
                .rel(Link.REL_SERVICE_DESC)
                .classification("api")
                .updater(
                        (format, link) -> {
                            if (MediaType.TEXT_HTML.equals(format)) {
                                link.setRel(Link.REL_SERVICE_DOC);
                            }
                        })
                .add(this);
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
