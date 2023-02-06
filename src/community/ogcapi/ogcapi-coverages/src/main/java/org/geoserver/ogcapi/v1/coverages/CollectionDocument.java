/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractCollectionDocument;
import org.geoserver.ogcapi.CollectionExtents;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.TimeExtentCalculator;
import org.geoserver.ogcapi.v1.coverages.cis.DomainSet;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.DateRange;
import org.geotools.util.logging.Logging;
import org.springframework.http.MediaType;

/** Description of a single collection, that will be serialized to JSON/XML/HTML */
@JsonPropertyOrder({"id", "title", "description", "extent", "links"})
public class CollectionDocument extends AbstractCollectionDocument<CoverageInfo> {
    static final Logger LOGGER = Logging.getLogger(CollectionDocument.class);

    public static final String REL_COVERAGE = "http://www.opengis.net/def/rel/ogc/1.0/coverage";
    public static final String REL_DOMAINSET =
            "http://www.opengis.net/def/rel/ogc/1.0/coverage-domainset";

    CoverageInfo coverage;
    String mapPreviewURL;
    List<String> crs;

    public CollectionDocument(GeoServer geoServer, CoverageInfo coverage, List<String> crs)
            throws IOException {
        super(coverage);
        // basic info
        String collectionId = coverage.prefixedName();
        this.id = collectionId;
        this.title = coverage.getTitle();
        this.description = coverage.getAbstract();
        ReferencedEnvelope bbox = coverage.getLatLonBoundingBox();
        DateRange timeExtent = TimeExtentCalculator.getTimeExtent(coverage);
        setExtent(new CollectionExtents(bbox, timeExtent));
        this.coverage = coverage;
        this.crs = crs;

        // self link
        addSelfLinks("ogc/coverages/v1/collections/" + id);

        // links for coverage extraction
        String baseUrl = APIRequestInfo.get().getBaseURL();
        for (MediaType format :
                APIRequestInfo.get().getProducibleMediaTypes(CoveragesResponse.class, false)) {
            String apiUrl =
                    ResponseUtils.buildURL(
                            baseUrl,
                            "ogc/coverages/v1/collections/" + collectionId + "/coverage",
                            Collections.singletonMap("f", format.toString()),
                            URLMangler.URLType.SERVICE);
            addLink(
                    new Link(
                            apiUrl,
                            REL_COVERAGE,
                            format.toString(),
                            collectionId + " coverage as " + format,
                            "coverage"));
        }

        // domainSet
        for (MediaType format :
                APIRequestInfo.get().getProducibleMediaTypes(DomainSet.class, false)) {
            String apiUrl =
                    ResponseUtils.buildURL(
                            baseUrl,
                            "ogc/coverages/v1/collections/" + collectionId + "/coverage/domainset",
                            Collections.singletonMap("f", format.toString()),
                            URLMangler.URLType.SERVICE);
            addLink(
                    new Link(
                            apiUrl,
                            REL_DOMAINSET,
                            format.toString(),
                            collectionId + " coverage domainset as " + format,
                            "domainset"));
        }

        // map preview
        if (isWMSAvailable(geoServer)) {
            Map<String, String> kvp = new HashMap<>();
            kvp.put("LAYERS", coverage.prefixedName());
            kvp.put("FORMAT", "application/openlayers");
            this.mapPreviewURL =
                    ResponseUtils.buildURL(baseUrl, "wms/reflect", kvp, URLMangler.URLType.SERVICE);
        }
    }

    private boolean isWMSAvailable(GeoServer geoServer) {
        ServiceInfo si =
                geoServer.getServices().stream()
                        .filter(s -> "WMS".equals(s.getId()))
                        .findFirst()
                        .orElse(null);
        return si != null;
    }

    @JsonIgnore
    public String getMapPreviewURL() {
        return mapPreviewURL;
    }

    public List<String> getCrs() {
        return crs;
    }
}
