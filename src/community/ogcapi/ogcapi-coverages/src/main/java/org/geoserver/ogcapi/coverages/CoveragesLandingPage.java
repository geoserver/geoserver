/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.coverages;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.geoserver.catalog.Catalog;
import org.geoserver.ogcapi.AbstractLandingPageDocumentNoConformance;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.Link;
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
        addLinksFor(
                urlBase + "/conformance",
                ConformanceDocument.class,
                "Conformance declaration as ",
                "conformance",
                null,
                Link.REL_CONFORMANCE);

        // collections
        addLinksFor(
                urlBase + "/collections",
                CollectionsDocument.class,
                "Collections Metadata as ",
                "collections",
                null,
                Link.REL_DATA);
    }
}
