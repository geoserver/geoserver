/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractCollectionDocument;
import org.geoserver.ogcapi.CollectionExtents;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.LinksBuilder;
import org.geoserver.ogcapi.Queryables;
import org.geoserver.ogcapi.TimeExtentCalculator;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.DateRange;
import org.geotools.util.logging.Logging;
import org.springframework.http.MediaType;

/** Description of a single collection, that will be serialized to JSON/XML/HTML */
@JsonPropertyOrder({"id", "title", "description", "extent", "links"})
@JacksonXmlRootElement(localName = "Collection", namespace = "http://www.opengis.net/wfs/3.0")
public class CollectionDocument extends AbstractCollectionDocument<FeatureTypeInfo> {
    static final Logger LOGGER = Logging.getLogger(CollectionDocument.class);

    FeatureTypeInfo featureType;
    String mapPreviewURL;
    List<String> crs;
    String storageCrs;

    public CollectionDocument(GeoServer geoServer, FeatureTypeInfo featureType, List<String> crs)
            throws IOException {
        super(featureType);
        // basic info
        String collectionId = featureType.prefixedName();
        this.id = collectionId;
        this.title = featureType.getTitle();
        this.description = featureType.getAbstract();
        ReferencedEnvelope bbox = featureType.getLatLonBoundingBox();
        DateRange timeExtent = TimeExtentCalculator.getTimeExtent(featureType);
        setExtent(new CollectionExtents(bbox, timeExtent));
        this.featureType = featureType;
        this.crs = crs;
        this.storageCrs = lookupStorageCrs();

        // links
        Collection<MediaType> formats =
                APIRequestInfo.get().getProducibleMediaTypes(FeaturesResponse.class, true);
        String baseUrl = APIRequestInfo.get().getBaseURL();
        for (MediaType format : formats) {
            String apiUrl =
                    ResponseUtils.buildURL(
                            baseUrl,
                            "ogc/features/v1/collections/" + collectionId + "/items",
                            Collections.singletonMap("f", format.toString()),
                            URLMangler.URLType.SERVICE);
            addLink(
                    new Link(
                            apiUrl,
                            Link.REL_ITEMS,
                            format.toString(),
                            collectionId + " items as " + format.toString(),
                            "items"));
        }
        addSelfLinks("ogc/features/v1/collections/" + id);

        // describedBy as GML schema
        if (isOWSAvailable(geoServer, "WFS")) {
            Map<String, String> kvp =
                    Map.ofEntries(
                            Map.entry("service", "WFS"),
                            Map.entry("version", "2.0"),
                            Map.entry("request", "DescribeFeatureType"),
                            Map.entry("typenames", featureType.prefixedName()));
            String describedByHref =
                    ResponseUtils.buildURL(baseUrl, "wfs", kvp, URLMangler.URLType.SERVICE);
            Link describedBy =
                    new Link(describedByHref, "describedBy", "application/xml", "Schema for " + id);
            addLink(describedBy);
        }

        // queryables
        new LinksBuilder(Queryables.class, "ogc/features/v1/collections")
                .segment(featureType.prefixedName(), true)
                .segment("queryables")
                .title("Queryable attributes as ")
                .rel(Queryables.REL)
                .add(this);

        // map preview
        if (isOWSAvailable(geoServer, "WMS")) {
            Map<String, String> kvp = new HashMap<>();
            kvp.put("LAYERS", featureType.prefixedName());
            kvp.put("FORMAT", "application/openlayers");
            this.mapPreviewURL =
                    ResponseUtils.buildURL(baseUrl, "wms/reflect", kvp, URLMangler.URLType.SERVICE);
        }
    }

    private static boolean isOWSAvailable(GeoServer geoServer, String serviceId) {
        return geoServer.getServices().stream()
                .anyMatch(s -> serviceId.equalsIgnoreCase(s.getName()) && s.isEnabled());
    }

    private String lookupStorageCrs() {
        CoordinateReferenceSystem crs = getSchemaCRS();

        if (crs != null) {
            try {
                return FeatureService.getCRSURI(crs);
            } catch (FactoryException e) {
                LOGGER.log(Level.FINER, "Error looking up epsg code", e);
            }
        }

        return null;
    }

    private CoordinateReferenceSystem getSchemaCRS() {
        FeatureType schema = getSchema();

        if (schema != null) {
            return schema.getCoordinateReferenceSystem();
        }

        return null;
    }

    @JsonIgnore
    public FeatureType getSchema() {
        try {
            return featureType.getFeatureType();
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Failed to compute feature type", e);
            return null;
        }
    }

    @JsonIgnore
    public String getMapPreviewURL() {
        return mapPreviewURL;
    }

    public List<String> getCrs() {
        return crs;
    }

    public String getStorageCrs() {
        return storageCrs;
    }
}
