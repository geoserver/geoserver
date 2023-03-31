/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.geoserver.catalog.Catalog;
import org.geoserver.ogcapi.AbstractLandingPageDocumentNoConformance;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.LinksBuilder;
import org.geoserver.wcs.WCSInfo;

/** A Coverage server landing page */
@JsonPropertyOrder({"title", "description", "links"})
public class CoveragesLandingPage extends AbstractLandingPageDocumentNoConformance {

    public CoveragesLandingPage(WCSInfo wcs, Catalog catalog, String urlBase) {
        super(
                (wcs.getTitle() == null) ? "Coverages 1.0 server" : wcs.getTitle(),
                (wcs.getAbstract() == null) ? "" : wcs.getAbstract(),
                urlBase);

        // conformance
        new LinksBuilder(ConformanceDocument.class, urlBase)
                .segment("/conformance")
                .title("Conformance declaration as ")
                .rel(Link.REL_CONFORMANCE)
                .add(this);

        // collections
        new LinksBuilder(CollectionsDocument.class, urlBase)
                .segment("/collections")
                .title("Collections Metadata as ")
                .rel(Link.REL_DATA)
                .add(this);
    }
}
