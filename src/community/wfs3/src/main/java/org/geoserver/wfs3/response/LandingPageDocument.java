/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import org.geoserver.catalog.Catalog;
import org.geoserver.ows.URLMangler;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs3.BaseRequest;
import org.geoserver.wfs3.DefaultWebFeatureService30;
import org.geoserver.wfs3.LandingPageRequest;

/**
 * A class representing the WFS3 server "contents" in a way that Jackson can easily translate to
 * JSON/YAML (and can be used as a Freemarker template model)
 */
@JacksonXmlRootElement(localName = "LandingPage")
public class LandingPageDocument extends AbstractDocument {

    public LandingPageDocument(LandingPageRequest request, WFSInfo wfs, Catalog catalog) {
        String baseUrl = request.getBaseUrl();

        // self and alternate representations of landing page
        addLinksFor(
                baseUrl,
                "wfs3/",
                LandingPageDocument.class,
                "This document as ",
                "landingPage",
                (format, link) -> {
                    String outputFormat = request.getOutputFormat();
                    if (format.equals(outputFormat)
                            || (outputFormat == null && BaseRequest.JSON_MIME.equals(format))) {
                        link.setRel(Link.REL_SELF);
                        link.setTitle("This document");
                    }
                },
                Link.REL_ALTERNATE);
        // api
        addLinksFor(
                baseUrl,
                "wfs3/api",
                OpenAPI.class,
                "API definition for this endpoint as ",
                "api",
                null,
                Link.REL_SERVICE);
        // conformance
        addLinksFor(
                baseUrl,
                "wfs3/conformance",
                ConformanceDocument.class,
                "Conformance declaration as ",
                "conformance",
                null,
                "conformance");
        // collections
        addLinksFor(
                baseUrl,
                "wfs3/collections",
                CollectionsDocument.class,
                "Collections Metadata as ",
                "collections",
                null,
                "data");
    }

    /** Builds service links for the given response types */
    private void addLinksFor(
            String baseUrl,
            String path,
            Class<?> responseType,
            String titlePrefix,
            String classification,
            BiConsumer<String, Link> linkUpdater,
            String rel) {
        for (String format : DefaultWebFeatureService30.getAvailableFormats(responseType)) {
            Map<String, String> params = Collections.singletonMap("f", format);
            String url = buildURL(baseUrl, path, params, URLMangler.URLType.SERVICE);
            String linkTitle = titlePrefix + format;
            Link link = new Link(url, rel, format, linkTitle);
            link.setClassification(classification);
            if (linkUpdater != null) {
                linkUpdater.accept(format, link);
            }
            addLink(link);
        }
    }
}
