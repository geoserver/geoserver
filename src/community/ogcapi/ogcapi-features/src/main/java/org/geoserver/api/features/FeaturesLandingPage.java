/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.AbstractLandingPageDocument;
import org.geoserver.catalog.Catalog;
import org.geoserver.wfs.WFSInfo;

/** A Features server landing page */
@JsonPropertyOrder({"title", "description", "links"})
public class FeaturesLandingPage extends AbstractLandingPageDocument {

    public FeaturesLandingPage(WFSInfo wfs, Catalog catalog, String featuresBase) {
        super(
                (wfs.getTitle() == null) ? "Features 1.0 server" : wfs.getTitle(),
                (wfs.getAbstract() == null) ? "" : wfs.getAbstract(),
                "ogc/features");

        final APIRequestInfo requestInfo = APIRequestInfo.get();
        String baseUrl = requestInfo.getBaseURL();
        // collections
        addLinksFor(
                baseUrl,
                featuresBase + "/collections",
                CollectionsDocument.class,
                "Collections Metadata as ",
                "collections",
                null,
                "data");
    }
}
