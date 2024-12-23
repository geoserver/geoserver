/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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

    public CollectionDocument(GeoServer geoServer, FeatureTypeInfo featureType, List<String> crs) throws IOException {
        this(geoServer, featureType, crs, null);
    }

    public CollectionDocument(
            GeoServer geoServer, FeatureTypeInfo featureType, List<String> crs, List<String> serviceCRS)
            throws IOException {
        super(featureType);
        // basic info
        String collectionId = featureType.prefixedName();
        this.id = collectionId;
        this.title = featureType.getTitle();
        this.description = featureType.getAbstract();
        DateRange timeExtent = TimeExtentCalculator.getTimeExtent(featureType);

        // Prepare a lat/lon bounding box adhering to the GeoServer configured number of decimal
        // places for text output
        ReferencedEnvelope bbox = featureType.getLatLonBoundingBox();
        int numDecimals = geoServer.getSettings().getNumDecimals();
        ReferencedEnvelope roundedOutBounds = roundLonLatBbox(bbox, numDecimals);

        setExtent(new CollectionExtents(roundedOutBounds, timeExtent));

        this.featureType = featureType;
        this.storageCrs = lookupStorageCrs();

        // the crs list must contain the storage crs, make sure it is there
        if (crs == null) {
            this.crs = List.of(storageCrs);
        } else if (!crsListContains(storageCrs, crs, serviceCRS)) {
            // provided list may be immutable
            this.crs = new ArrayList<>(crs);
            this.crs.add(0, storageCrs);
        } else {
            this.crs = crs;
        }

        // links
        Collection<MediaType> formats = APIRequestInfo.get().getProducibleMediaTypes(FeaturesResponse.class, true);
        String baseUrl = APIRequestInfo.get().getBaseURL();
        for (MediaType format : formats) {
            String apiUrl = ResponseUtils.buildURL(
                    baseUrl,
                    "ogc/features/v1/collections/" + collectionId + "/items",
                    Collections.singletonMap("f", format.toString()),
                    URLMangler.URLType.SERVICE);
            addLink(new Link(
                    apiUrl,
                    Link.REL_ITEMS,
                    format.toString(),
                    collectionId + " items as " + format.toString(),
                    "items"));
        }
        addSelfLinks("ogc/features/v1/collections/" + id);

        // describedBy as GML schema
        if (isOWSAvailable(geoServer, "WFS")) {
            Map<String, String> kvp = Map.ofEntries(
                    Map.entry("service", "WFS"),
                    Map.entry("version", "2.0"),
                    Map.entry("request", "DescribeFeatureType"),
                    Map.entry("typenames", featureType.prefixedName()));
            String describedByHref = ResponseUtils.buildURL(baseUrl, "wfs", kvp, URLMangler.URLType.SERVICE);
            Link describedBy = new Link(describedByHref, "describedBy", "application/xml", "Schema for " + id);
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
            this.mapPreviewURL = ResponseUtils.buildURL(baseUrl, "wms/reflect", kvp, URLMangler.URLType.SERVICE);
        }
    }

    /**
     * @param bbox a FeatureType's WGS84 bounds with east-north axis order
     * @param numDecimals precision to round coordinates to
     * @return an envelope rounded to the specified number of decimaps and expanded as necessary to the west-east and
     *     south-north directions. Zero width and height are preserved.
     */
    private ReferencedEnvelope roundLonLatBbox(ReferencedEnvelope bbox, int numDecimals) {
        // if(true)return bbox;
        double minX = Math.max(-180d, round(bbox.getMinX(), numDecimals, RoundingMode.FLOOR));
        double minY = Math.max(-90d, round(bbox.getMinY(), numDecimals, RoundingMode.FLOOR));
        double maxX = Math.min(180d, round(bbox.getMaxX(), numDecimals, RoundingMode.CEILING));
        double maxY = Math.min(90d, round(bbox.getMaxY(), numDecimals, RoundingMode.CEILING));

        if (bbox.getWidth() == 0d) {
            maxX = minX;
        }
        if (bbox.getHeight() == 0d) {
            maxY = minY;
        }

        return new ReferencedEnvelope(minX, maxX, minY, maxY, bbox.getCoordinateReferenceSystem());
    }

    /** Round a value to the specified number of decimal places with a provided rounding strategy */
    double round(double value, int places, RoundingMode mode) {
        if (places < 0) throw new IllegalArgumentException("Decimal places must be non-negative");
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, mode);
        return bd.doubleValue();
    }

    private boolean crsListContains(String storageCrs, List<String> collectionCRSs, List<String> serviceCRSs) {
        // is it referring to the whole server CRS list?
        if (collectionCRSs.contains("#/crs")) {
            if (serviceCRSs != null && serviceCRSs.contains(storageCrs)) return true;
        }
        return collectionCRSs.contains(storageCrs);
    }

    private static boolean isOWSAvailable(GeoServer geoServer, String serviceId) {
        return geoServer.getServices().stream().anyMatch(s -> serviceId.equalsIgnoreCase(s.getName()) && s.isEnabled());
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
