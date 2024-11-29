/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geotools.util.logging.Logging;
import org.springframework.stereotype.Component;

/**
 * Adds links to the head of the current response. Normally called by LinksBuilder, but can be
 * invoked by others as well
 */
@Component
public class HttpHeaderLinksAppender extends AbstractDispatcherCallback {

    static final Logger LOGGER = Logging.getLogger(HttpHeaderLinksAppender.class);

    static final ThreadLocal<List<Link>> LINKS = ThreadLocal.withInitial(ArrayList::new);
    /* Being cautious here, Jetty allows up to 4KB of response header, then throws an exception */
    static final int MAX_LINKS_HEADER_SIZE = 2048;

    public static void addLink(Link link) {
        LINKS.get().add(link);
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        // is it an OGC API request?
        APIRequestInfo ri = APIRequestInfo.get();
        if (ri == null) return response;

        HttpServletResponse httpResponse = ri.getResponse();
        List<Link> links = LINKS.get();
        if (links.isEmpty()) return response;

        List<String> allFormatted =
                links.stream().map(this::formatLink).collect(Collectors.toList());
        int size = getLinksHeaderSize(allFormatted);
        if (size < MAX_LINKS_HEADER_SIZE) {
            allFormatted.forEach(l -> httpResponse.addHeader("Link", l));
        } else {
            // adding at least the self links, useful to find out the supported formats
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Headers are too large, would have been: " + allFormatted);
                LOGGER.fine("Providing only self/alternate links");
            }
            List<String> selfFormatted =
                    links.stream()
                            .filter(
                                    l -> {
                                        String rel = l.getRel();
                                        return rel.equals(Link.REL_SELF)
                                                || rel.equals(Link.REL_ALTERNATE);
                                    })
                            .map(this::formatLink)
                            .collect(Collectors.toList());
            selfFormatted.forEach(l -> httpResponse.addHeader("Link", l));
        }

        return response;
    }

    private int getLinksHeaderSize(List<String> allFormatted) {
        // size of the links themselves, plus all the "link: " before them
        return allFormatted.stream().mapToInt(l -> l.getBytes().length).sum()
                + allFormatted.size() * 6;
    }

    String formatLink(Link link) {
        StringBuilder sb = new StringBuilder("<").append(link.getHref()).append(">");
        if (link.getRel() != null) {
            sb.append("; rel=\"").append(link.getRel()).append("\"");
        }
        if (link.getType() != null) {
            sb.append("; type=\"").append(link.getType()).append("\"");
        }
        if (link.getTitle() != null) {
            sb.append("; title=\"").append(link.getTitle()).append("\"");
        }
        return sb.toString();
    }

    @Override
    public void finished(Request request) {
        LINKS.remove();
    }
}
