/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.impl;

import java.util.Objects;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.ogcapi.LinkInfo;

/** Default implementation of {@link LinkInfo} */
public class LinkInfoImpl implements LinkInfo, Cloneable {

    protected String rel;
    protected String type;
    protected String title;
    protected String href;
    protected String service;

    protected MetadataMap metadata = new MetadataMap();

    public LinkInfoImpl() {}

    public LinkInfoImpl(String rel, String type, String href) {
        this.rel = rel;
        this.type = type;
        this.href = href;
    }

    @Override
    public String getRel() {
        return rel;
    }

    @Override
    public void setRel(String rel) {
        this.rel = rel;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getHref() {
        return href;
    }

    @Override
    public void setHref(String href) {
        this.href = href;
    }

    @Override
    public String getService() {
        return service;
    }

    @Override
    public void setService(String service) {
        this.service = service;
    }

    @Override
    public MetadataMap getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "LinkInfoImpl{"
                + "rel='"
                + rel
                + '\''
                + ", type='"
                + type
                + '\''
                + ", title='"
                + title
                + '\''
                + ", href='"
                + href
                + '\''
                + ", service='"
                + service
                + "'}'";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinkInfoImpl linkInfo = (LinkInfoImpl) o;
        return Objects.equals(rel, linkInfo.rel)
                && Objects.equals(type, linkInfo.type)
                && Objects.equals(title, linkInfo.title)
                && Objects.equals(href, linkInfo.href)
                && Objects.equals(service, linkInfo.service)
                && Objects.equals(metadata, linkInfo.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rel, type, title, href, service, metadata);
    }

    @Override
    public LinkInfo clone() {
        try {
            return (LinkInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
