/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractCollectionDocument;
import org.geoserver.ogcapi.CollectionExtents;
import org.geoserver.ogcapi.DimensionExtent;
import org.geoserver.ogcapi.DimensionExtent.Grid;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.LinkInfoConverter;
import org.geoserver.ogcapi.LinksBuilder;
import org.geoserver.ogcapi.TimeExtentCalculator;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.capabilities.DimensionHelper;
import org.geoserver.wms.capabilities.DimensionHelper.ElevationDimensionRasterHelper;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.geotools.util.DateRange;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/** Description of a single collection, that will be serialized to JSON/XML/HTML */
@JsonPropertyOrder({"id", "title", "description", "extent", "links"})
public class CollectionDocument extends AbstractCollectionDocument<PublishedInfo> {
    static final Logger LOGGER = Logging.getLogger(CollectionDocument.class);
    static final String REL_MAP = "https://www.opengis.net/def/rel/ogc/1.0/map";

    /** Gregorian temporal reference system on the OGC definitions server, used as the {@code trs} of a time axis. */
    static final String GREGORIAN_TRS = "http://www.opengis.net/def/uom/ISO-8601/0/Gregorian";

    /** OGC nil-reason "unknown", used as the {@code definition} when no semantic URI is known for a dimension. */
    static final String UNKNOWN_DEFINITION = "http://www.opengis.net/def/nil/OGC/0/unknown";

    PublishedInfo published;
    List<String> crs;
    String storageCrs;

    public CollectionDocument(GeoServer geoServer, PublishedInfo published, List<String> crs) throws IOException {
        super(published);
        LinkInfoConverter.addLinksToDocument(this, published, MapsService.class);
        // basic info
        String collectionId = published.prefixedName();
        this.id = collectionId;
        this.title = published.getTitle();
        this.description = published.getAbstract();
        ReferencedEnvelope bbox = getSpatialExtents(published);
        DateRange timeExtent = getTimeExtent(published);
        CollectionExtents extents = new CollectionExtents(bbox, timeExtent);
        addAdditionalDimensions(extents, geoServer, published);
        this.published = published;

        // the CRSs the map can be delivered in, with the storage one, which the map renders without reprojecting
        // (/req/collection-map/desc-crs). A CRS84 storage CRS is left out, the requirement asks for the others only
        this.storageCrs = crsUri(storageCrs(published));
        this.crs = crs;
        if (storageCrs != null) {
            if (!crs.contains(storageCrs)) {
                this.crs = new ArrayList<>(crs);
                this.crs.add(1, storageCrs);
            }
            extents.setStorageCrsBbox(storageBounds(published));
        }
        setExtent(extents);

        addSelfLinks("ogc/maps/v1/collections/" + id);

        // maps in default style
        Collection<MediaType> formats = APIRequestInfo.get().getProducibleMediaTypes(WebMap.class, true);
        String baseUrl = APIRequestInfo.get().getBaseURL();
        for (MediaType format : formats) {
            String mapHref = ResponseUtils.buildURL(
                    baseUrl,
                    "ogc/maps/v1/collections/" + collectionId + "/map",
                    Collections.singletonMap("f", format.toString()),
                    URLMangler.URLType.SERVICE);
            addLink(new Link(mapHref, REL_MAP, format.toString(), collectionId + " map as " + format, "defaultMap"));
        }

        // styles
        new LinksBuilder(StylesDocument.class, "ogc/maps/v1/collections/")
                .segment(published.prefixedName(), true)
                .segment("styles")
                .title("Styles as ")
                .rel("styles")
                .add(this);
    }

    public List<String> getCrs() {
        return crs;
    }

    public String getStorageCrs() {
        return storageCrs;
    }

    /** The CRS the collection is delivered in without reprojecting, null when it is not known. */
    private static CoordinateReferenceSystem storageCrs(PublishedInfo published) {
        if (published instanceof LayerInfo layer) return layer.getResource().getCRS();
        ReferencedEnvelope bounds = ((LayerGroupInfo) published).getBounds();
        return bounds != null ? bounds.getCoordinateReferenceSystem() : null;
    }

    /**
     * The URI of a storage CRS, null when it is CRS84, which the requirement excludes, and when it carries no authority
     * code, a custom projection for example.
     */
    private static String crsUri(CoordinateReferenceSystem crs) {
        if (crs == null || CRS.equalsIgnoreMetadata(crs, DefaultGeographicCRS.WGS84)) return null;
        try {
            String identifier = ResourcePool.lookupIdentifier(crs, false);
            return identifier == null ? null : MapsService.crsUri(identifier);
        } catch (FactoryException e) {
            LOGGER.log(Level.FINER, "Could not look up the authority code of the storage CRS", e);
            return null;
        }
    }

    /**
     * The collection bounds in its storage CRS, for the {@code storageCrsBbox}, null when they cannot be worked out, in
     * which case the extent reports the CRS84 box only.
     */
    private static ReferencedEnvelope storageBounds(PublishedInfo published) {
        if (!(published instanceof LayerInfo layer)) return ((LayerGroupInfo) published).getBounds();
        try {
            return layer.getResource().boundingBox();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not compute the collection bounds in its storage CRS", e);
            return null;
        }
    }

    private ReferencedEnvelope getSpatialExtents(PublishedInfo published) {
        try {
            if (published instanceof LayerInfo info1) {
                if (info1.getResource().getLatLonBoundingBox() == null) {
                    throw new RuntimeException("Layer has no bounding box: " + published);
                }
                return info1.getResource().getLatLonBoundingBox();
            } else if (published instanceof LayerGroupInfo info) {
                ReferencedEnvelope bounds = info.getBounds();
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
        if (published instanceof LayerInfo info) {
            return TimeExtentCalculator.getTimeExtent(info.getResource());
        } else if (published instanceof LayerGroupInfo) {
            LOGGER.fine("Time extent not supported for Layer Groups");
        } else {
            throw new RuntimeException("Unexpected, don't know how to handle: " + published);
        }
        return null;
    }

    /**
     * Advertises the extent of every enabled dimension beyond space and time (elevation and custom dimensions) as an
     * additional dimension, keyed by the dimension name. Layer groups carry no dimensions.
     */
    private void addAdditionalDimensions(CollectionExtents extents, GeoServer geoServer, PublishedInfo published) {
        if (!(published instanceof LayerInfo layer)) return;
        ResourceInfo resource = layer.getResource();
        DimensionInfo elevation = resource.getMetadata().get(ResourceInfo.ELEVATION, DimensionInfo.class);
        if (elevation != null && elevation.isEnabled()) {
            addDimensionExtent(extents, geoServer, resource, "elevation", elevation);
        }
        DimensionHelper.getCustomDimensions(resource)
                .forEach((name, dim) -> addDimensionExtent(extents, geoServer, resource, name, dim));
    }

    /**
     * Advertises one additional dimension under its name, with its {@code interval}, schema reference, and
     * {@code grid}.
     */
    private void addDimensionExtent(
            CollectionExtents extents,
            GeoServer geoServer,
            ResourceInfo resource,
            String name,
            DimensionInfo dimension) {
        try {
            TreeSet<Object> values = dimensionValues(geoServer, resource, name, dimension);
            if (values == null || values.isEmpty()) return;
            DimensionExtent extent = new DimensionExtent();

            // overall interval from the domain min and max
            extent.setInterval(List.of(Arrays.asList(values.first(), values.last())));

            // the schema requires trs for a time axis, the vertical CRS as vrs for elevation, a URI-valued unit as
            // definition, otherwise the nil "unknown" definition plus the plain unit
            Optional<Class<?>> type = DimensionHelper.getDataType(values);
            boolean temporal = type.isPresent() && Date.class.isAssignableFrom(type.get());
            String units = dimension.getUnits();
            boolean unitIsUri = isUri(units);
            String verticalCrs = ResourceInfo.ELEVATION.equals(name) ? verticalCrsUri(units, unitIsUri) : null;
            if (temporal) {
                extent.setTrs(GREGORIAN_TRS);
            } else if (verticalCrs != null) {
                extent.setVrs(verticalCrs);
            } else if (unitIsUri) {
                extent.setDefinition(units);
            } else {
                extent.setDefinition(UNKNOWN_DEFINITION);
                extent.setUnit(units);
            }

            // grid: enumerated coordinates, a resolution for a regular interval, or nothing for a continuous one
            DimensionPresentation presentation = dimension.getPresentation();
            if (presentation == DimensionPresentation.LIST) {
                extent.setGrid(Grid.enumerated(new ArrayList<>(values)));
            } else if (presentation == DimensionPresentation.DISCRETE_INTERVAL && dimension.getResolution() != null) {
                // a time resolution is configured in milliseconds, reported as an ISO 8601 duration
                Object resolution = temporal
                        ? new DefaultPeriodDuration(dimension.getResolution().longValue()).toString()
                        : dimension.getResolution();
                extent.setGrid(Grid.regular(resolution));
            }

            extents.addDimension(name, extent);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to compute the extent of dimension " + name, e);
        }
    }

    /**
     * The URI of the vertical CRS an elevation is measured in, null when the units name no CRS and are a plain unit of
     * measure instead. An authority code is expanded to its URI, so that the {@link DimensionInfo#ELEVATION_UNITS} form
     * WMS capabilities advertise can be configured once and serve both protocols.
     */
    private static String verticalCrsUri(String units, boolean unitIsUri) {
        if (units == null) return null;
        if (unitIsUri) return units;
        // only EPSG codes can be expanded reliably, the OGC "CRS:88" family has no matching URI in the same form
        return units.matches("(?i)EPSG:\\d+") ? MapsService.crsUri(units) : null;
    }

    /** Whether a configured unit is itself a URI, rather than a unit of measure name. */
    private static boolean isUri(String units) {
        return units != null && (units.startsWith("http://") || units.startsWith("https://"));
    }

    /** Domain values of a dimension, sorted. */
    private TreeSet<Object> dimensionValues(
            GeoServer geoServer, ResourceInfo resource, String name, DimensionInfo dimension) throws IOException {
        if (resource instanceof FeatureTypeInfo featureType) {
            return new WMS(geoServer).getDimensionValues(featureType, dimension);
        }
        if (resource instanceof CoverageInfo coverage) {
            // the reader is catalog-managed and shared, so it must not be disposed here.
            GridCoverage2DReader reader = (GridCoverage2DReader) coverage.getGridCoverageReader(null, null);
            ReaderDimensionsAccessor accessor = new ReaderDimensionsAccessor(reader);
            // reuse the capabilities elevation domain reader, which honors the presentation and configured start/end
            if (ResourceInfo.ELEVATION.equals(name)) {
                return new ElevationDimensionRasterHelper(dimension, accessor).getDomain();
            }
            List<String> domain = accessor.getDomain(name);
            return domain == null ? null : new TreeSet<>(domain);
        }
        return null;
    }
}
