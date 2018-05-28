/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.geoserver.catalog.Catalog;
import org.geoserver.ows.URLMangler;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs3.BaseRequest;
import org.geoserver.wfs3.DefaultWebFeatureService30;
import org.geoserver.wfs3.LandingPageRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

/**
 * A class representing the WFS3 server "contents" in a way that Jackson can easily translate to
 * JSON/YAML (and can be used as a Freemarker template model)
 */
@JacksonXmlRootElement(localName = "LandingPage")
public class LandingPageDocument {

    private final Catalog catalog;
    private final WFSInfo wfs;
    private final List<Link> links = new ArrayList<>();
    private final LandingPageRequest request;

    public LandingPageDocument(LandingPageRequest request, WFSInfo wfs, Catalog catalog) {
        this.wfs = wfs;
        this.catalog = catalog;
        this.request = request;
        String baseUrl = request.getBaseUrl();

        // self and alternate representations of landing page
        addLinksFor(
                baseUrl,
                "wfs3/",
                LandingPageDocument.class,
                "This document as ",
                (format, link) -> {
                    String outputFormat = request.getOutputFormat();
                    if (format.equals(outputFormat) 
                            || (outputFormat == null && BaseRequest.JSON_MIME.equals(format))) {
                        link.setRel(Link.REL_SELF);
                        link.setTitle("This document");
                    }
                });
        // api
        addLinksFor(
                baseUrl,
                "wfs3/api",
                APIDocument.class,
                "API definition for this endpoint as ",
                null);
        // conformance
        addLinksFor(
                baseUrl,
                "wfs3/conformance",
                ConformanceDocument.class,
                "Conformance declaration as ",
                null);
        // collections
        addLinksFor(
                baseUrl,
                "wfs3/collections",
                CollectionsDocument.class,
                "Collections Metadata as ",
                null);
    }

    /** Builds service links for the given response types */
    private void addLinksFor(
            String baseUrl,
            String path,
            Class<?> responseType,
            String titlePrefix,
            BiConsumer<String, Link> linkUpdater) {
        for (String format : DefaultWebFeatureService30.getAvailableFormats(responseType)) {
            Map<String, String> params = Collections.singletonMap("f", format);
            String url = buildURL(baseUrl, path, params, URLMangler.URLType.SERVICE);
            String linkType = Link.REL_SERVICE;
            String linkTitle = titlePrefix + format;
            Link link = new Link(url, linkType, format, linkTitle);
            if (linkUpdater != null) {
                linkUpdater.accept(format, link);
            }
            links.add(link);
        }
    }

    public void addLink(Link link) {
        links.add(link);
    }

    @JacksonXmlProperty(namespace = Link.ATOM_NS, localName = "link")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<Link> getLinks() {
        return links;
    }
}
