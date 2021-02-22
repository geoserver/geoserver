/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.geoserver.ogcapi.AbstractLandingPageDocumentNoConformance;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.opensearch.eo.OSEOInfo;

/** A Features server landing page */
@JsonPropertyOrder({"title", "description", "links"})
public class STACLandingPage extends AbstractLandingPageDocumentNoConformance {

    public STACLandingPage(OSEOInfo wfs, String basePath) {
        super(
                (wfs.getTitle() == null) ? "Features 1.0 server" : wfs.getTitle(),
                (wfs.getAbstract() == null) ? "" : wfs.getAbstract(),
                basePath);

        // conformance
        addLinksFor(
                basePath + "/conformance",
                ConformanceDocument.class,
                "Conformance declaration as ",
                "conformance",
                null,
                Link.REL_CONFORMANCE);

        // collections
        addLinksFor(
                basePath + "/collections",
                CollectionsDocument.class,
                "Collections Metadata as ",
                "collections",
                null,
                Link.REL_DATA);
    }
}
