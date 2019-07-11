/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.api;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.geoserver.ows.URLMangler;
import org.springframework.http.MediaType;

/** Base OGC API document class with shared link generation facilities */
public class AbstractDocument {
    protected final List<Link> links = new ArrayList<>();

    /**
     * Adds a link to the document
     *
     * @param link
     */
    public void addLink(Link link) {
        links.add(link);
    }

    @JacksonXmlProperty(namespace = Link.ATOM_NS, localName = "link")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<Link> getLinks() {
        return links;
    }

    /** Returns a link URL with the given classification and type, or null if not found */
    public String getLinkUrl(String classification, String type) {
        return links.stream()
                .filter(l -> Objects.equals(classification, l.getClassification()))
                .filter(l -> type.equals(l.getType()))
                .map(l -> l.getHref())
                .findFirst()
                .orElse(null);
    }

    /** Returns all links except the ones matching both classification and type provided */
    public List<Link> getLinksExcept(String classification, String excludedType) {
        return links.stream()
                .filter(
                        l ->
                                classification == null
                                        || Objects.equals(classification, l.getClassification()))
                .filter(l -> excludedType == null || !excludedType.equals(l.getType()))
                .collect(Collectors.toList());
    }

    /** Builds service links for the given response types */
    protected void addLinksFor(
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
}
