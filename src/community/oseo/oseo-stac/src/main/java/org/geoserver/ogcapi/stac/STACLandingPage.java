/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import static org.geoserver.ogcapi.JSONSchemaMessageConverter.SCHEMA_TYPE_VALUE;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractLandingPageDocumentNoConformance;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.Queryables;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.springframework.http.HttpMethod;

/** A STAC server landing page */
@JsonPropertyOrder({"title", "description", "links"})
public class STACLandingPage extends AbstractLandingPageDocumentNoConformance {

    /** <code>rel</code> for the search resource */
    public static String REL_SEARCH = "search";

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
                CollectionsResponse.class,
                "Collections Metadata as ",
                "collections",
                null,
                Link.REL_DATA);

        // search, GET
        links.addAll(
                APIRequestInfo.get()
                        .getLinksFor(
                                basePath + "/search",
                                SearchResponse.class,
                                "Items as ",
                                "searchGet",
                                (t, l) -> l.setMethod(HttpMethod.GET),
                                REL_SEARCH,
                                true));

        // search, POST
        links.addAll(
                APIRequestInfo.get()
                        .getLinksFor(
                                basePath + "/search",
                                SearchResponse.class,
                                "Items as ",
                                "searchGet",
                                (t, l) -> l.setMethod(HttpMethod.POST),
                                REL_SEARCH,
                                true));

        // queryables
        links.addAll(
                APIRequestInfo.get()
                        .getLinksFor(
                                basePath + "/queryables",
                                Queryables.class,
                                "Queryables as ",
                                "queryables",
                                null,
                                Queryables.REL,
                                true)
                        .stream()
                        .filter(
                                l ->
                                        "text/html".equals(l.getType())
                                                || SCHEMA_TYPE_VALUE.equals(l.getType()))
                        .collect(Collectors.toList()));
    }

    @JsonProperty("stac_version")
    public String getStacVersion() {
        return stacVersion;
    }

    public List<String> getConformsTo() {
        return conformsTo;
    }
}
