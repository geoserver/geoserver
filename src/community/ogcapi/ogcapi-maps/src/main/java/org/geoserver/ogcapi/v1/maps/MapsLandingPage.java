/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractLandingPageDocument;
import org.geoserver.ogcapi.CollectionExtents;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.LinksBuilder;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wms.WMSInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;

/** A Maps server landing page */
@JsonPropertyOrder({"title", "description", "extent", "crs", "links"})
public class MapsLandingPage extends AbstractLandingPageDocument {

    private static final Logger LOGGER = Logging.getLogger(MapsLandingPage.class);

    private final CollectionExtents extent;
    private final List<String> crs;

    public MapsLandingPage(WMSInfo wms, Catalog catalog, String base, List<String> crs) {
        super(
                (wms.getTitle() == null) ? "Maps 1.0 server" : wms.getTitle(),
                (wms.getAbstract() == null) ? "" : wms.getAbstract(),
                "ogc/maps/v1");
        this.crs = crs;

        // collections
        new LinksBuilder(CollectionsDocument.class, base)
                .segment("/collections")
                .title("Collections Metadata as ")
                .rel(Link.REL_DATA_URI)
                .add(this);

        // the dataset map, one link per encoding it can be delivered in, plus the extent of what it covers
        MapsConformance conf = MapsConformance.configuration(wms);
        if (conf.datasetMap(wms)) {
            List<PublishedInfo> collections =
                    DatasetCollections.mappable(catalog, MapsSettings.defaultCollections(wms));
            this.extent = new CollectionExtents(datasetBounds(collections), null);
            String href = ResponseUtils.buildURL(
                    APIRequestInfo.get().getBaseURL(), "ogc/maps/v1/map", null, URLMangler.URLType.SERVICE);
            addLink(new Link(href, CollectionDocument.REL_MAP, null, "Dataset map", "map"));
        } else {
            this.extent = null;
        }
    }

    /** The CRS84 bounds of the dataset map contents (null tolerant, but this should not really happen) */
    private static ReferencedEnvelope datasetBounds(List<PublishedInfo> collections) {
        ReferencedEnvelope result = null;
        for (PublishedInfo published : collections) {
            ReferencedEnvelope bounds = latLonBounds(published);
            if (bounds == null) continue;
            if (result == null) result = new ReferencedEnvelope(bounds);
            else result.expandToInclude(bounds);
        }
        return result;
    }

    /** The CRS84 bounds of one collection (from metadata, should always be populated) */
    private static ReferencedEnvelope latLonBounds(PublishedInfo published) {
        if (published instanceof LayerInfo layer) return layer.getResource().getLatLonBoundingBox();
        try {
            ReferencedEnvelope bounds = ((LayerGroupInfo) published).getBounds();
            return bounds == null ? null : bounds.transform(DefaultGeographicCRS.WGS84, true);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not compute the CRS84 bounds of " + published.prefixedName(), e);
            return null;
        }
    }

    /** The extent of the dataset map contents, absent when dataset maps are disabled. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public CollectionExtents getExtent() {
        return extent;
    }

    /** The CRSs the dataset map can be delivered in, CRS84 first ({@code /req/dataset-map/desc-crs}). */
    public List<String> getCrs() {
        return crs;
    }
}
