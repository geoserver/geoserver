/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import org.geoserver.catalog.MetadataLinkInfo;

public class MetadataLinkInfoImpl implements MetadataLinkInfo {

    String id;

    String type;

    String about;

    String metadataType;

    String content;

    public MetadataLinkInfoImpl() {
        // nothing to do
    }

    public MetadataLinkInfoImpl(MetadataLinkInfoImpl other) {
        this.id = other.id;
        this.type = other.type;
        this.about = other.about;
        this.metadataType = other.metadataType;
        this.content = other.content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getMetadataType() {
        return metadataType;
    }

    public void setMetadataType(String metadataType) {
        this.metadataType = metadataType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    static final List<String> protocols = Arrays.asList("http", "https", "ftp");
    /** @throws IllegalArgumentException if the url is invalid for use as a Metadata Link */
    public static void validate(String url) {
        if (url == null) return;
        URL dummy;
        try {
            dummy = new URL("http://dummy/");
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Could not parse dummy context URL", ex);
        }
        try {
            // Doing this with exceptions isn't ideal but it works, and we're throwing an
            // exception anyway

            // The dummy context will allow it to parse relative URLs, which should be allowed.
            URL parsed = new URL(dummy, url);
            String protocol = parsed.getProtocol();

            // Converting to URI forces validation
            parsed.toURI();

            if (!protocols.contains(protocol)) {
                throw new IllegalArgumentException(
                        "Protocol " + protocol + " is not supported in url " + url);
            }
        } catch (MalformedURLException | URISyntaxException ex) {
            throw new IllegalArgumentException("Not a valid URL: " + url, ex);
        }
    }

    public void setContent(String content) {
        validate(content);
        this.content = content;
    }

    private Object readResolve() {
        validate(content);
        return this;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((about == null) ? 0 : about.hashCode());
        result = PRIME * result + ((content == null) ? 0 : content.hashCode());
        result = PRIME * result + ((metadataType == null) ? 0 : metadataType.hashCode());
        result = PRIME * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof MetadataLinkInfo)) {
            return false;
        }

        final MetadataLinkInfo other = (MetadataLinkInfo) obj;
        if (about == null) {
            if (other.getAbout() != null) return false;
        } else if (!about.equals(other.getAbout())) return false;
        if (content == null) {
            if (other.getContent() != null) return false;
        } else if (!content.equals(other.getContent())) return false;
        if (metadataType == null) {
            if (other.getMetadataType() != null) return false;
        } else if (!metadataType.equals(other.getMetadataType())) return false;
        if (type == null) {
            if (other.getType() != null) return false;
        } else if (!type.equals(other.getType())) return false;
        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append("[type:")
                .append(type)
                .append(", metadataType:")
                .append(metadataType)
                .append(", content:")
                .append(content)
                .append(']')
                .toString();
    }
}
