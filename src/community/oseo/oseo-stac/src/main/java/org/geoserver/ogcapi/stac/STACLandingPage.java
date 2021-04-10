/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import static org.geoserver.ogcapi.JSONSchemaMessageConverter.SCHEMA_TYPE_VALUE;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractLandingPageDocumentNoConformance;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.Queryables;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.ows.util.ResponseUtils;
import org.springframework.http.HttpMethod;

/** A STAC server landing page */
@JsonPropertyOrder({"title", "description", "links"})
public class STACLandingPage extends AbstractLandingPageDocumentNoConformance {

    /** <code>rel</code> for the search resource */
    public static String REL_SEARCH = "search";

    public static String LANDING_PAGE_ID = "GeoserverSTACLandingPage";

    private String stacVersion = STACService.STAC_VERSION;
    private final List<String> conformsTo;
    private final String type = STACService.TYPE_CATALOG;
    private final String id = LANDING_PAGE_ID;

    public STACLandingPage(
            OSEOInfo service, String basePath, List<String> conformsTo, Set<String> collectionIds) {
        super(
                (service.getTitle() == null) ? "STAC server" : service.getTitle(),
                (service.getAbstract() == null)
                        ? "STAC server implementation"
                        : service.getAbstract(),
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

        // link to each collection as a child
        for (String collectionId : collectionIds) {
            String href = basePath + "/collections/" + ResponseUtils.urlEncode(collectionId);
            Link link = new Link(href, "child", "application/json", null);
            getLinks().add(link);
        }

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
                                "searchPost",
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

    public String getType() {
        return type;
    }

    @Override
    public String getId() {
        return id;
    }
}
