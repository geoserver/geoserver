/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Teams with {@link SuffixAwareHandlerMapping} and {@link SuffixStripFilter} to allow matching path extensions to
 * formats. In particular, this negotiation strategy picks the extension detected by the filter and maps it to a media
 * type, allowing other strategies to kick in if no extension is found.
 */
public class SuffixContentNegotiationStrategy implements ContentNegotiationStrategy {

    private final Map<String, MediaType> mediaTypes;

    // Name of the attribute your GeoServerFilter sets
    public static final String FORMAT_ATTRIBUTE = "geoserver.formatExtension";

    public SuffixContentNegotiationStrategy(Map<String, MediaType> mediaTypes) {
        // lower-case normalization
        this.mediaTypes = new LinkedHashMap<>();
        mediaTypes.forEach((ext, mt) -> this.mediaTypes.put(ext.toLowerCase(Locale.ROOT), mt));
    }

    @Override
    public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest) throws HttpMediaTypeNotAcceptableException {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            return MEDIA_TYPE_ALL_LIST;
        }

        // ---------------------------------------------------------
        // 1. First try to read the extension detected by the filter
        // ---------------------------------------------------------
        String ext = (String) request.getAttribute(FORMAT_ATTRIBUTE);

        // ---------------------------------------------------------
        // 2. Fallback: parse the *original* URI if forwarded
        // ---------------------------------------------------------
        if (ext == null) {
            String origUri = (String) request.getAttribute("jakarta.servlet.forward.request_uri");

            String uri = (origUri != null ? origUri : request.getRequestURI());

            int lastSlash = uri.lastIndexOf('/');
            int dot = uri.lastIndexOf('.');
            if (dot > lastSlash) {
                ext = uri.substring(dot + 1);
            }
        }

        // ---------------------------------------------------------
        // 3. Check mapped media type
        // ---------------------------------------------------------
        if (ext != null) {
            MediaType mt = mediaTypes.get(ext.toLowerCase(Locale.ROOT));
            if (mt != null) {
                return Collections.singletonList(mt);
            }
        }

        // ---------------------------------------------------------
        // 4. No extension → let other strategies decide
        // ---------------------------------------------------------
        return MEDIA_TYPE_ALL_LIST;
    }
}
