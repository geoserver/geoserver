/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.features;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.geoserver.catalog.Catalog;
import org.geoserver.ogcapi.AbstractLandingPageDocumentNoConformance;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.FunctionsDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.wfs.WFSInfo;

/** A Features server landing page */
@JsonPropertyOrder({"title", "description", "links"})
public class FeaturesLandingPage extends AbstractLandingPageDocumentNoConformance {

    public FeaturesLandingPage(WFSInfo wfs, Catalog catalog, String featuresBase) {
        super(
                (wfs.getTitle() == null) ? "Features 1.0 server" : wfs.getTitle(),
                (wfs.getAbstract() == null) ? "" : wfs.getAbstract(),
                "ogc/features");

        // conformance
        addLinksFor(
                "ogc/features" + "/conformance",
                ConformanceDocument.class,
                "Conformance declaration as ",
                "conformance",
                null,
                Link.REL_CONFORMANCE);

        // collections
        addLinksFor(
                featuresBase + "/collections",
                CollectionsDocument.class,
                "Collections Metadata as ",
                "collections",
                null,
                Link.REL_DATA);

        // filter capabilities
        addLinksFor(
                featuresBase + "/functions",
                FunctionsDocument.class,
                "Filter capabilities as ",
                "filter-capabilities",
                null,
                FunctionsDocument.REL);
    }
}
