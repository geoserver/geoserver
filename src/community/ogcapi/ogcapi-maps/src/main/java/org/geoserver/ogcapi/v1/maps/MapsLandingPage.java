/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.geoserver.catalog.Catalog;
import org.geoserver.ogcapi.AbstractLandingPageDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.LinksBuilder;
import org.geoserver.wms.WMSInfo;

/** A Maps server landing page */
@JsonPropertyOrder({"title", "description", "links"})
public class MapsLandingPage extends AbstractLandingPageDocument {

    public MapsLandingPage(WMSInfo wms, Catalog catalog, String base) {
        super(
                (wms.getTitle() == null) ? "Maps 1.0 server" : wms.getTitle(),
                (wms.getAbstract() == null) ? "" : wms.getAbstract(),
                "ogc/maps/v1");

        // collections
        new LinksBuilder(CollectionsDocument.class, base)
                .segment("/collections")
                .title("Collections Metadata as ")
                .rel(Link.REL_DATA_URI)
                .add(this);
    }
}
