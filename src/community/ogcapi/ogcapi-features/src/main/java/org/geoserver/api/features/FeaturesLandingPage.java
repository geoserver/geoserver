/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.geoserver.api.AbstractLandingPageDocument;
import org.geoserver.api.Link;
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
                featuresBase + "/filter-capabilities",
                FilterCapabilitiesDocument.class,
                "Filter capabilities as ",
                "filter-capabilities",
                null,
                Link.REL_DATA);
    }
}
