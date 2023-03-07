/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

/**
 * Configurable links for OGC API services, they can be provided at the {@link
 * org.geoserver.catalog.ResourceInfo} and {@link org.geoserver.config.GeoServerInfo} level. More
 * places to be added in the future (e.g., {@link org.geoserver.catalog.LayerGroupInfo}, {@link
 * org.geoserver.config.ServiceInfo}
 */
import java.io.Serializable;
import org.geoserver.catalog.MetadataMap;

public interface LinkInfo extends Serializable {

    String LINKS_METADATA_KEY = "ogcApiLinks";

    /** Returns the relation type, e.g., "self", "alternate", "service-desc", ... */
    String getRel();

    /**
     * Sets the relation type for this link (mandatory). e.g., "self", "alternate", "service-desc",
     */
    void setRel(String rel);

    /** Returns the MIME type, e.g., "application/json", "text/html", "application/atom+xml", ... */
    String getType();

    /** Sets the MIME type for this link (mandatory) */
    void setType(String type);

    /** Returns the title, if any */
    String getTitle();

    /** Sets the title for this link (optional) */
    void setTitle(String title);

    /** Returns the href for this link */
    String getHref();

    /** Returns the href */
    void setHref(String href);

    /** Returns the service type, if any, e.g.,"Features", "Maps", ... */
    String getService();

    /** Sets the service type for this link (optional) */
    void setService(String service);

    /**
     * Returns the metadata map, which can be used to store additional information about the link
     */
    MetadataMap getMetadata();

    /** Returns a clone of this link info */
    LinkInfo clone();
}
