/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.springframework.http.MediaType;

/** Helper class to build links to all representations of a given resource */
public class LinksBuilder {

    private final Class<?> responseType;
    private List<String> segments = new ArrayList<>();
    private String title = "Resource as ";
    private String rel;
    private boolean includeHTML = true;
    private String classification;
    private BiConsumer<MediaType, Link> updater;
    private Function<List<MediaType>, List<MediaType>> mediaTypeCustomizer;
    private boolean appendToHead = true;

    public LinksBuilder(Class<?> responseType) {
        this.responseType = responseType;
    }

    public LinksBuilder(Class<?> responseType, String path) {
        this.responseType = responseType;
        this.segments.add(path);
    }

    /**
     * Adds a path segment for the resource. Can be called multiple times, the segments will be
     * chained.
     */
    public LinksBuilder segment(String segment) {
        this.segments.add(segment);
        return this;
    }

    /**
     * Allows customization of media type list, e.g., removing entries or moving their position
     * around
     *
     * @param mediaTypeCustomizer
     * @return
     */
    public LinksBuilder mediaTypeCustomizer(
            Function<List<MediaType>, List<MediaType>> mediaTypeCustomizer) {
        this.mediaTypeCustomizer = mediaTypeCustomizer;
        return this;
    }

    /**
     * Adds a path segment for the resource. Can be called multiple times, the segments will be
     * chained.
     */
    public LinksBuilder segment(String segment, boolean urlEncode) {
        if (!urlEncode) return this.segment(segment);
        this.segments.add(ResponseUtils.urlEncode(segment, ':'));
        return this;
    }

    /**
     * Adds a title prefix. The code will add the format name to this prefix and make it the link
     * title
     */
    public LinksBuilder title(String title) {
        this.title = title;
        return this;
    }

    /** Specifies the rel of the link */
    public LinksBuilder rel(String rel) {
        this.rel = rel;
        return this;
    }

    public LinksBuilder html(boolean includeHTML) {
        this.includeHTML = includeHTML;
        return this;
    }

    /**
     * Specifies the link classification. Not required, normally used by the HTML template to find
     * links (as the rel URIs are sometimes long). If missing, the last segment of the rel URI is
     * going to be used
     */
    public LinksBuilder classification(String classification) {
        this.classification = classification;
        return this;
    }

    /** An optional callback to update the link object */
    public LinksBuilder updater(BiConsumer<MediaType, Link> updater) {
        this.updater = updater;
        return this;
    }

    /**
     * Sets whether the links should be automatically appended to the current HTTP response header.
     * Defaults to true.
     */
    public LinksBuilder appendToHead(boolean append) {
        this.appendToHead = append;
        return this;
    }

    /** Builds the list of links for all formats, and returns it */
    public List<Link> build() {
        List<Link> result = new ArrayList<>();
        List<MediaType> mediaTypes =
                new ArrayList<>(
                        APIRequestInfo.get().getProducibleMediaTypes(responseType, includeHTML));
        if (mediaTypeCustomizer != null) {
            mediaTypes = mediaTypeCustomizer.apply(mediaTypes);
        }
        for (MediaType mediaType : mediaTypes) {
            String format = mediaType.toString();
            Map<String, String> params = Collections.singletonMap("f", format);
            String url =
                    buildURL(
                            APIRequestInfo.get().getBaseURL(),
                            getPath(),
                            params,
                            URLMangler.URLType.SERVICE);
            String linkTitle = title + format;
            Link link = new Link(url, rel, format, linkTitle);
            link.setClassification(getClassification());
            if (updater != null) {
                updater.accept(mediaType, link);
            }
            result.add(link);
        }
        if (appendToHead) {
            result.forEach(l -> HttpHeaderLinksAppender.addLink(l));
        }
        return result;
    }

    /** Buidls the list of links for all formats, and adds it to the document links */
    public void add(AbstractDocument document) {
        document.getLinks().addAll(build());
    }

    private String getClassification() {
        if (classification != null) return classification;
        int idx = rel.lastIndexOf('/');
        if (idx == -1) return rel;
        return rel.substring(idx + 1);
    }

    private String getPath() {
        return ResponseUtils.appendPath(segments.toArray(new String[segments.size()]));
    }
}
