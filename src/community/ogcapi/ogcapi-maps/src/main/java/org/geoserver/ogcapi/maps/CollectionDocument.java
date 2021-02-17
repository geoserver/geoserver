/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.maps;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.AbstractCollectionDocument;
import org.geoserver.ogcapi.CollectionExtents;
import org.geoserver.platform.ServiceException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.http.HttpStatus;

import java.util.logging.Logger;

/** Description of a single collection, that will be serialized to JSON/XML/HTML */
@JsonPropertyOrder({"id", "title", "description", "extent", "links"})
public class CollectionDocument extends AbstractCollectionDocument<PublishedInfo> {
    static final Logger LOGGER = Logging.getLogger(CollectionDocument.class);

    PublishedInfo published;

    public CollectionDocument(GeoServer geoServer, PublishedInfo published) {
        super(published);
        // basic info
        String collectionId = published.prefixedName();
        this.id = collectionId;
        this.title = published.getTitle();
        this.description = published.getAbstract();
        ReferencedEnvelope bbox = getExtents(published);
        setExtent(new CollectionExtents(bbox));
        this.published = published;

        //        // links
        //        Collection<MediaType> formats =
        //                APIRequestInfo.get().getProducibleMediaTypes(FeaturesResponse.class,
        // true);
        //        String baseUrl = APIRequestInfo.get().getBaseURL();
        //        for (MediaType format : formats) {
        //            String apiUrl =
        //                    ResponseUtils.buildURL(
        //                            baseUrl,
        //                            "ogc/features/collections/" + collectionId + "/items",
        //                            Collections.singletonMap("f", format.toString()),
        //                            URLMangler.URLType.SERVICE);
        //            addLink(
        //                    new Link(
        //                            apiUrl,
        //                            Link.REL_ITEMS,
        //                            format.toString(),
        //                            collectionId + " items as " + format.toString(),
        //                            "items"));
        //        }
        addSelfLinks("ogc/features/collections/" + id);

        //        // queryables
        //        addLinksFor(
        //                "ogc/features/collections/"
        //                        + ResponseUtils.urlEncode(featureType.prefixedName())
        //                        + "/queryables",
        //                QueryablesDocument.class,
        //                "Queryable attributes as ",
        //                "queryables",
        //                null,
        //                "queryables");
        //
        //        // map preview
        //        if (isWMSAvailable(geoServer)) {
        //            Map<String, String> kvp = new HashMap<>();
        //            kvp.put("LAYERS", featureType.prefixedName());
        //            kvp.put("FORMAT", "application/openlayers");
        //            this.mapPreviewURL =
        //                    ResponseUtils.buildURL(baseUrl, "wms/reflect", kvp,
        // URLMangler.URLType.SERVICE);
        //        }
    }

    private ReferencedEnvelope getExtents(PublishedInfo published) {
        try {
            if (published instanceof LayerInfo) {
                return ((LayerInfo) published).getResource().getLatLonBoundingBox();
            } else if (published instanceof LayerGroupInfo) {
                ReferencedEnvelope bounds = ((LayerGroupInfo) published).getBounds();
                return bounds.transform(DefaultGeographicCRS.WGS84, true);
            } else {
                throw new RuntimeException("Unexpected, don't know how to handle: " + published);
            }
        } catch (TransformException | FactoryException e) {
            throw new APIException(
                    ServiceException.NO_APPLICABLE_CODE,
                    "Failed to transform bounding box to WGS84",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }
    }
}
