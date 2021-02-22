/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import org.geoserver.ogcapi.AbstractLandingPageDocumentNoConformance;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.opensearch.eo.OSEOInfo;

/** A STAC server landing page */
@JsonPropertyOrder({"title", "description", "links"})
public class STACLandingPage extends AbstractLandingPageDocumentNoConformance {

    private String stacVersion = STACService.STAC_VERSION;
    private final List<String> conformsTo;

    public STACLandingPage(OSEOInfo service, String basePath, List<String> conformsTo) {
        super(
                (service.getTitle() == null) ? "STAC server" : service.getTitle(),
                (service.getAbstract() == null) ? "" : service.getAbstract(),
                basePath);
        this.conformsTo = conformsTo;

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

    @JsonProperty("stac_version")
    public String getStacVersion() {
        return stacVersion;
    }

    public List<String> getConformsTo() {
        return conformsTo;
    }
}
