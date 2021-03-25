/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.maps;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.logging.Logger;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.AbstractCollectionDocument;
import org.geoserver.ogcapi.CollectionExtents;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.http.HttpStatus;

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

        addSelfLinks("ogc/maps/collections/" + id);

        // queryables
        addLinksFor(
                "ogc/maps/collections/"
                        + ResponseUtils.urlEncode(published.prefixedName())
                        + "/styles",
                StylesDocument.class,
                "Styles as ",
                "styles",
                null,
                "styles");
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
