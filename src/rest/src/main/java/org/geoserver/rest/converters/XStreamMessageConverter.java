/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.converters;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Logger;
import org.geoserver.rest.RequestInfo;
import org.geotools.util.logging.Logging;
import org.springframework.http.MediaType;

/** Base class for XStream based message converters */
public abstract class XStreamMessageConverter<T> extends BaseMessageConverter<T> {

    static final Logger LOGGER = Logging.getLogger(XStreamMessageConverter.class);

    public XStreamMessageConverter(MediaType... supportedMediaTypes) {
        super(supportedMediaTypes);
    }

    /** Encode the given link */
    public abstract void encodeLink(String link, HierarchicalStreamWriter writer);

    /** Encode the given link */
    public abstract void encodeCollectionLink(String link, HierarchicalStreamWriter writer);

    /** Create the instance of XStream needed to do encoding */
    protected abstract XStream createXStreamInstance();

    protected void encodeAlternateAtomLink(String link, HierarchicalStreamWriter writer) {
        writer.startNode("atom:link");
        writer.addAttribute("xmlns:atom", "http://www.w3.org/2005/Atom");
        writer.addAttribute("rel", "alternate");
        writer.addAttribute("href", href(link));
        writer.addAttribute("type", getMediaType());

        writer.endNode();
    }

    protected String href(String link) {
        final RequestInfo pg = RequestInfo.get();
        String ext = getExtension();

        if (ext != null && ext.length() > 0) link = link + "." + ext;

        // encode as relative or absolute depending on the link type
        if (link.startsWith("/")) {
            // absolute, encode from "root"
            return pg.servletURI(link);
        } else {
            // encode as relative
            return pg.pageURI(link);
        }
    }

    public String encode(String component) {
        try {
            return URLEncoder.encode(component, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.warning("Unable to URL-encode component: " + component);
            return component;
        }
    }

    /** The extension used for resources of the type being encoded */
    public abstract String getExtension();

    /**
     * Get the text representation of the mime type being encoded. Only used in link encoding for
     * xml
     */
    public abstract String getMediaType();
}
