/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.File;
import java.net.URI;
import java.net.URL;

/**
 * Represents a vector granule resource that can be expressed as a {@link File}, {@link URI}, or {@link URL}, regardless
 * of the input type used to construct it.
 *
 * <p>The constructor accepts a variety of resource representations—including {@code File}, {@code URL}, {@code URI},
 * and {@code String}—and normalizes them into consistent {@link File}, {@link URI}, and {@link URL} forms when
 * possible. Strings are first interpreted as URIs, falling back to filesystem paths if URI parsing fails.
 *
 * <p>If the underlying representation cannot be converted to one of the normalized forms (e.g., an invalid URL or
 * non-file URI when creating a {@code File}), the corresponding accessor may return {@code null}.
 */
public class VectorGranuleReference {

    private final File file;
    private final URI uri;
    private final URL url;

    public VectorGranuleReference(Object resource) {
        if (resource instanceof File) {
            this.file = (File) resource;
            this.uri = file.toURI();
            this.url = uriToURL(uri);
        } else if (resource instanceof URL) {
            this.url = (URL) resource;
            this.uri = urlToURI(url);
            this.file = uri != null && "file".equalsIgnoreCase(uri.getScheme()) ? new File(uri) : null;
        } else if (resource instanceof URI) {
            this.uri = (URI) resource;
            this.url = uriToURL(uri);
            this.file = "file".equalsIgnoreCase(uri.getScheme()) ? new File(uri) : null;
        } else if (resource instanceof String) {
            // First try URI to URL, then fallback to File path
            URI tmpUri = tryCreateURI((String) resource);
            if (tmpUri != null) {
                this.uri = tmpUri;
                this.url = uriToURL(tmpUri);
                this.file = "file".equalsIgnoreCase(tmpUri.getScheme()) ? new File(tmpUri) : null;
            } else {
                this.file = new File((String) resource);
                this.uri = file.toURI();
                this.url = uriToURL(uri);
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported type: " + resource.getClass().getName());
        }
    }

    private static URI tryCreateURI(String s) {
        try {
            return new URI(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static URL uriToURL(URI uri) {
        try {
            return uri.toURL();
        } catch (Exception e) {
            return null;
        }
    }

    private static URI urlToURI(URL url) {
        try {
            return url.toURI();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the resource as a {@link File}, or {@code null} if the original input does not represent a file-based
     * resource.
     */
    public File getFile() {
        return file;
    }

    /** Returns the resource as a {@link URI}, or {@code null} if the URI could not be created. */
    public URI getUri() {
        return uri;
    }

    /** Returns the resource as a {@link URL}, or {@code null} if the URL could not be created. */
    public URL getUrl() {
        return url;
    }
}
