/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.maps;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.geoserver.catalog.Catalog;
import org.geoserver.ogcapi.AbstractLandingPageDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.wms.WMSInfo;

/** A Maps server landing page */
@JsonPropertyOrder({"title", "description", "links"})
public class MapsLandingPage extends AbstractLandingPageDocument {

    public MapsLandingPage(WMSInfo wms, Catalog catalog, String base) {
        super(
                (wms.getTitle() == null) ? "Maps 1.0 server" : wms.getTitle(),
                (wms.getAbstract() == null) ? "" : wms.getAbstract(),
                "ogc/maps");

        // collections
        addLinksFor(
                base + "/collections",
                CollectionsDocument.class,
                "Collections Metadata as ",
                "collections",
                null,
                Link.REL_DATA_URI);
    }
}
