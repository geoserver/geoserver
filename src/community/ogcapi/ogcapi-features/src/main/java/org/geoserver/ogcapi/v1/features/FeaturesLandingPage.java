/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.geoserver.catalog.Catalog;
import org.geoserver.ogcapi.AbstractLandingPageDocumentNoConformance;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.FunctionsDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.LinksBuilder;
import org.geoserver.wfs.WFSInfo;

/** A Features server landing page */
@JsonPropertyOrder({"title", "description", "links"})
public class FeaturesLandingPage extends AbstractLandingPageDocumentNoConformance {

    public FeaturesLandingPage(WFSInfo wfs, Catalog catalog, String featuresBase) {
        super(
                (wfs.getTitle() == null) ? "Features 1.0 server" : wfs.getTitle(),
                (wfs.getAbstract() == null) ? "" : wfs.getAbstract(),
                "ogc/features/v1");

        // conformance
        new LinksBuilder(ConformanceDocument.class, featuresBase)
                .segment("conformance")
                .title("Conformance declaration as ")
                .rel(Link.REL_CONFORMANCE)
                .add(this);

        // collections
        new LinksBuilder(CollectionsDocument.class, featuresBase)
                .segment("collections")
                .title("Collections Metadata as ")
                .rel(Link.REL_DATA)
                .add(this);

        // filter capabilities
        new LinksBuilder(FunctionsDocument.class, featuresBase)
                .segment("/functions")
                .title("Filter capabilities as ")
                .rel(FunctionsDocument.REL)
                .add(this);
    }
}
