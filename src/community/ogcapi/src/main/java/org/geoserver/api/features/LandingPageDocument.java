/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.ows.URLMangler;
import org.geoserver.wfs.WFSInfo;
import org.springframework.http.MediaType;

/**
 * A class representing the WFS3 server "contents" in a way that Jackson can easily translate to
 * JSON/YAML (and can be used as a Freemarker template model)
 */
@JsonPropertyOrder({"title", "description", "links"})
@JacksonXmlRootElement(localName = "LandingPage", namespace = "http://www.opengis.net/wfs/3.0")
public class LandingPageDocument extends AbstractDocument {

    final String title;
    final String description;

    public LandingPageDocument(WFSInfo wfs, Catalog catalog, String featuresBase) {
        final APIRequestInfo requestInfo = APIRequestInfo.get();
        String baseUrl = requestInfo.getBaseURL();

        // self and alternate representations of landing page
        addLinksFor(
                baseUrl,
                featuresBase + "/",
                LandingPageDocument.class,
                "This document as ",
                "landingPage",
                new BiConsumer<MediaType, Link>() {
                    boolean first = true;

                    @Override
                    public void accept(MediaType mediaType, Link link) {
                        if ((requestInfo.isAnyMediaTypeAccepted()
                                        && MediaType.APPLICATION_JSON.equals(mediaType))
                                || (first && requestInfo.isFormatRequested(mediaType))) {
                            link.setRel(Link.REL_SELF);
                            link.setTitle("This document");
                            first = false;
                        }
                    }
                },
                Link.REL_ALTERNATE);
        // api
        addLinksFor(
                baseUrl,
                featuresBase + "/api",
                OpenAPI.class,
                "API definition for this endpoint as ",
                "api",
                null,
                Link.REL_SERVICE);
        // conformance
        addLinksFor(
                baseUrl,
                featuresBase + "/conformance",
                ConformanceDocument.class,
                "Conformance declaration as ",
                "conformance",
                null,
                "conformance");
        // collections
        addLinksFor(
                baseUrl,
                featuresBase + "/collections",
                CollectionsDocument.class,
                "Collections Metadata as ",
                "collections",
                null,
                "data");
        title = (wfs.getTitle() == null) ? "WFS 3.0 server" : wfs.getTitle();
        description = (wfs.getAbstract() == null) ? "" : wfs.getAbstract();
    }

    /** Builds service links for the given response types */
    private void addLinksFor(
            String baseUrl,
            String path,
            Class<?> responseType,
            String titlePrefix,
            String classification,
            BiConsumer<MediaType, Link> linkUpdater,
            String rel) {
        for (MediaType mediaType :
                APIRequestInfo.get().getProducibleMediaTypes(responseType, true)) {
            String format = mediaType.toString();
            Map<String, String> params = Collections.singletonMap("f", format);
            String url = buildURL(baseUrl, path, params, URLMangler.URLType.SERVICE);
            String linkTitle = titlePrefix + format;
            Link link = new Link(url, rel, format, linkTitle);
            link.setClassification(classification);
            if (linkUpdater != null) {
                linkUpdater.accept(mediaType, link);
            }
            addLink(link);
        }
    }

    @JacksonXmlProperty(localName = "Title")
    public String getTitle() {
        return title;
    }

    @JacksonXmlProperty(localName = "Description")
    public String getDescription() {
        return description;
    }
}
