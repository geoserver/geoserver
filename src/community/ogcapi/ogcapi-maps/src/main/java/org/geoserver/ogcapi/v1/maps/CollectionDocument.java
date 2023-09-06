/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.IOException;
import java.util.logging.Logger;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.AbstractCollectionDocument;
import org.geoserver.ogcapi.CollectionExtents;
import org.geoserver.ogcapi.LinkInfoConverter;
import org.geoserver.ogcapi.LinksBuilder;
import org.geoserver.ogcapi.TimeExtentCalculator;
import org.geoserver.platform.ServiceException;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.DateRange;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpStatus;

/** Description of a single collection, that will be serialized to JSON/XML/HTML */
@JsonPropertyOrder({"id", "title", "description", "extent", "links"})
public class CollectionDocument extends AbstractCollectionDocument<PublishedInfo> {
    static final Logger LOGGER = Logging.getLogger(CollectionDocument.class);

    PublishedInfo published;

    public CollectionDocument(GeoServer geoServer, PublishedInfo published) throws IOException {
        super(published);
        LinkInfoConverter.addLinksToDocument(this, published, MapsService.class);
        // basic info
        String collectionId = published.prefixedName();
        this.id = collectionId;
        this.title = published.getTitle();
        this.description = published.getAbstract();
        ReferencedEnvelope bbox = getSpatialExtents(published);
        DateRange timeExtent = getTimeExtent(published);
        setExtent(new CollectionExtents(bbox, timeExtent));
        this.published = published;

        addSelfLinks("ogc/maps/v1/collections/" + id);

        // queryables
        new LinksBuilder(StylesDocument.class, "ogc/maps/v1/collections/")
                .segment(published.prefixedName(), true)
                .segment("styles")
                .title("Styles as ")
                .rel("styles")
                .add(this);
    }

    private ReferencedEnvelope getSpatialExtents(PublishedInfo published) {
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

    private DateRange getTimeExtent(PublishedInfo published) throws IOException {
        if (published instanceof LayerInfo) {
            return TimeExtentCalculator.getTimeExtent(((LayerInfo) published).getResource());
        } else if (published instanceof LayerGroupInfo) {
            LOGGER.fine("Time extent not supported for Layer Groups");
        } else {
            throw new RuntimeException("Unexpected, don't know how to handle: " + published);
        }
        return null;
    }
}
