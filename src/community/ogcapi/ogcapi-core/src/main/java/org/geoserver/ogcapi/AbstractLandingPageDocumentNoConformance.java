/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import io.swagger.v3.oas.models.OpenAPI;
import java.util.function.BiConsumer;
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
        final APIRequestInfo requestInfo = APIRequestInfo.get();

        // self and alternate representations of landing page
        addLinksFor(
                serviceBase + "/",
                AbstractLandingPageDocument.class,
                "This document as ",
                "landingPage",
                new BiConsumer<MediaType, Link>() {
                    boolean first = true;

                    @Override
                    public void accept(MediaType mediaType, Link link) {
                        if ((first
                                && requestInfo.isFormatRequested(
                                        mediaType, MediaType.APPLICATION_JSON))) {
                            link.setRel(Link.REL_SELF);
                            link.setTitle("This document");
                            first = false;
                        }
                    }
                },
                Link.REL_ALTERNATE);
        // api
        addLinksFor(
                serviceBase + "/api",
                OpenAPI.class,
                "API definition for this endpoint as ",
                "api",
                (format, link) -> {
                    if (MediaType.TEXT_HTML.equals(format)) {
                        link.setRel(Link.REL_SERVICE_DOC);
                    }
                },
                Link.REL_SERVICE_DESC);
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
