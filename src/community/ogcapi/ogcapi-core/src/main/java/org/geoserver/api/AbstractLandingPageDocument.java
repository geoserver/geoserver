/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.function.BiConsumer;
import org.springframework.http.MediaType;

/**
 * A class representing a generic API service landing page in a way that Jackson can easily
 * translate to JSON/YAML (and can be used as a Freemarker template model)
 */
@JsonPropertyOrder({"title", "description", "links"})
public class AbstractLandingPageDocument extends AbstractDocument {

    final String title;
    final String description;

    public AbstractLandingPageDocument(String title, String description, String serviceBase) {
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
        // conformance
        addLinksFor(
                serviceBase + "/conformance",
                ConformanceDocument.class,
                "Conformance declaration as ",
                "conformance",
                null,
                Link.REL_CONFORMANCE);
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
