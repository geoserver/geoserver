/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static java.util.Comparator.comparingInt;
import static org.geoserver.catalog.Predicates.and;
import static org.geoserver.catalog.Predicates.equal;
import static org.geoserver.catalog.Predicates.isInstanceOf;
import static org.geoserver.catalog.Predicates.or;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ows.LocalPublished;
import org.geoserver.wms.WMS;
import org.geotools.api.filter.Filter;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;

/**
 * The collections a dataset map is made of: the ones the {@code collections} parameter names, or the contents of the
 * service the request was addressed to when it does not.
 */
class DatasetCollections {

    private static final Logger LOGGER = Logging.getLogger(DatasetCollections.class);

    private DatasetCollections() {}

    /**
     * The collections to render, bottom to top: the {@code collections} values in the order they were given
     * ({@code /req/collections-selection/collections-response} B), or the default contents of the service otherwise.
     * Never empty.
     *
     * @param collections the raw parameter value, null when absent
     * @param max how many collections the default contents hold at most, the parameter itself is not limited
     * @throws APIException with a 400 status if the parameter names an unknown collection
     */
    static List<PublishedInfo> resolve(Catalog catalog, String collections, int max) {
        List<PublishedInfo> result = collections != null ? selected(catalog, collections) : mappable(catalog, max);
        if (result.isEmpty()) {
            throw new APIException(
                    APIException.NOT_FOUND, "There is no geospatial data to map here", HttpStatus.NOT_FOUND);
        }
        return result;
    }

    /** The collections the parameter names, rejecting an unknown one. */
    private static List<PublishedInfo> selected(Catalog catalog, String collections) {
        String[] ids = collections.split(",");
        List<PublishedInfo> result = new ArrayList<>(ids.length);
        for (String id : ids) {
            String collectionId = collectionId(id);
            PublishedInfo published = published(catalog, collectionId);
            if (published == null) {
                throw new APIException(
                        APIException.INVALID_PARAMETER_VALUE,
                        "Unknown collection in the collections parameter: " + collectionId,
                        HttpStatus.BAD_REQUEST);
            }
            if (!isMappable(published)) {
                throw new APIException(
                        APIException.INVALID_PARAMETER_VALUE,
                        "Collection " + collectionId + " cannot be drawn on a map",
                        HttpStatus.BAD_REQUEST);
            }
            result.add(published);
        }
        return result;
    }

    /** The layer or layer group of the given identifier, null when the catalog holds neither. */
    static PublishedInfo published(Catalog catalog, String collectionId) {
        PublishedInfo published = catalog.getLayerByName(collectionId);
        if (published != null) return published;
        if (!collectionId.contains(":")) return catalog.getLayerGroupByName(collectionId);
        String[] split = collectionId.split(":");
        return catalog.getLayerGroupByName(split[0], split[1]);
    }

    /**
     * The identifier of a {@code collections} entry, which is either the identifier itself or the full URL of the
     * collection resource ({@code /req/collections-selection/collections-parameter} C).
     */
    private static String collectionId(String value) {
        String id = value.trim();
        int collectionsPath = id.lastIndexOf("/collections/");
        if (collectionsPath < 0) return id;
        // strip a trailing sub-resource path too, so that both the collection and its map URL are accepted
        id = id.substring(collectionsPath + "/collections/".length());
        int slash = id.indexOf('/');
        return slash < 0 ? id : id.substring(0, slash);
    }

    /**
     * The collections the service draws when the request names none: the layer or layer group of the virtual service
     * the request was addressed to if any, otherwise a selection of the collections of the local workspace, or of the
     * whole catalog at the root. Empty when there is nothing to map, which the landing page reports as an empty extent
     * rather than as a failure.
     *
     * <p>Drawing a whole catalog gives a map that is both slow and unreadable, so at most {@code max} collections are
     * picked ({@code /per/dataset-map/geodata-selection}): the layer groups first, being curated, then the layers by
     * name until the map is full. They are then stacked bottom to top as rasters, polygons, groups, lines and points,
     * so that nothing covers what is thinner than itself.
     */
    static List<PublishedInfo> mappable(Catalog catalog, int max) {
        // a layer group is one collection here, not many: rendering the group itself keeps its own layer order and
        // per-layer styles, which is the closest thing to a natural order the catalog has
        if (LocalPublished.get() != null) return List.of(LocalPublished.get());

        List<PublishedInfo> selection = groups(catalog, max);

        // a layer drawn as a member of one of the groups above is not drawn a second time on its own, whatever the
        // mode of the group: WMS advertises the members of a SINGLE group too, but drawing them twice is only waste
        Set<String> grouped = new HashSet<>();
        for (PublishedInfo group : selection) {
            ((LayerGroupInfo) group).layers().forEach(l -> grouped.add(l.prefixedName()));
        }
        addAll(catalog, LayerInfo.class, max, selection, p -> !grouped.contains(p.prefixedName()));

        // stack the selection, keeping the name order inside each placement
        selection.sort(comparingInt(p -> placement(p).ordinal()));
        return selection;
    }

    /** The layer groups of a default dataset map, by ascending name, at most {@code max} of them. */
    private static List<PublishedInfo> groups(Catalog catalog, int max) {
        // nesting is only known after reading them all, and the cap applies to what is drawn, not to what is read
        List<PublishedInfo> candidates = new ArrayList<>();
        addAll(catalog, LayerGroupInfo.class, Integer.MAX_VALUE, candidates, p -> true);
        Set<String> nested = new HashSet<>();
        candidates.forEach(g -> nestedGroups((LayerGroupInfo) g, nested));
        candidates.removeIf(g -> nested.contains(g.prefixedName()));
        return candidates.size() <= max ? candidates : new ArrayList<>(candidates.subList(0, max));
    }

    /**
     * The first {@code max} collections a map can draw, layer groups first, then the layers, each by ascending name.
     * This is the list a picker offers, so it is not ordered for drawing, and it holds the members of the groups too.
     */
    static List<PublishedInfo> pickable(Catalog catalog, int max) {
        if (LocalPublished.get() != null) return List.of(LocalPublished.get());

        List<PublishedInfo> result = new ArrayList<>();
        addAll(catalog, LayerGroupInfo.class, max, result, p -> true);
        addAll(catalog, LayerInfo.class, max, result, p -> true);
        return result;
    }

    /** Collects the prefixed names of the groups the given one holds, at any depth. */
    private static void nestedGroups(LayerGroupInfo group, Set<String> names) {
        for (PublishedInfo child : group.getLayers()) {
            if (child instanceof LayerGroupInfo childGroup) {
                names.add(childGroup.prefixedName());
                nestedGroups(childGroup, names);
            }
        }
    }

    /**
     * Appends the accepted collections to the accumulator, by ascending name, stopping as soon as it holds {@code max}
     * of them.
     */
    private static void addAll(
            Catalog catalog,
            Class<? extends PublishedInfo> type,
            int max,
            List<PublishedInfo> accumulator,
            Predicate<PublishedInfo> accept) {
        // no workspace filter: the catalog already limits a virtual service to the contents of its workspace or layer
        try (CloseableIterator<? extends PublishedInfo> it =
                catalog.list(type, catalogFilter(type), null, null, Predicates.asc("name"))) {
            while (it.hasNext() && accumulator.size() < max) {
                PublishedInfo published = it.next();
                if (isMappable(published) && accept.test(published)) accumulator.add(published);
            }
        }
    }

    /**
     * The catalog query for a listing of the collections a map can draw, for the type being listed. It carries only the
     * flags a database backed catalog can turn into SQL, so it narrows the query but does not answer it:
     * {@link #isMappable} still has to be applied to everything it returns.
     */
    static Filter catalogFilter(Class<? extends PublishedInfo> type) {
        if (type == LayerInfo.class) return LAYER_FILTER;
        if (type == LayerGroupInfo.class) return GROUP_FILTER;
        return or(LAYER_FILTER, GROUP_FILTER);
    }

    // a layer carries neither flag of its own, both live on the resource, and a layer of a disabled store is disabled
    // too, which the getter chain hides
    private static final Filter LAYER_FILTER = and(List.of(
            isInstanceOf(LayerInfo.class),
            equal("resource.enabled", true),
            equal("resource.store.enabled", true),
            equal("resource.advertised", true)));

    private static final Filter GROUP_FILTER =
            and(List.of(isInstanceOf(LayerGroupInfo.class), equal("enabled", true), equal("advertised", true)));

    /**
     * Whether the maps service offers the collection at all, the one check every listing and every single collection
     * lookup goes through.
     *
     * <p>A disabled or unadvertised collection is no more mappable here than it is in WMS. A geometryless vector layer
     * cannot be drawn, and one of them in a dataset map would fail the whole map, not just its own layer. A
     * {@code CONTAINER} layer group cannot be asked for by name: WMS advertises it without a name, and only as the
     * parent of its members, which are then collections of their own. Layer groups are curated by hand, so their
     * members are taken as drawable.
     */
    static boolean isMappable(PublishedInfo published) {
        if (!published.isEnabled() || !published.isAdvertised()) return false;
        if (published instanceof LayerGroupInfo group) return group.getMode() != LayerGroupInfo.Mode.CONTAINER;
        return WMS.isWmsExposable((LayerInfo) published);
    }

    /** Where a collection belongs in the stack of a default dataset map, bottom to top. */
    private enum Placement {
        RASTER,
        POLYGON,
        GROUP,
        LINE,
        POINT
    }

    /**
     * Where the collection belongs in the stack: areas at the bottom, then the curated layer groups, then the lines and
     * the points, which would otherwise disappear under the areas. A layer whose geometry cannot be told apart, or
     * cannot be read at all, counts as a polygon: covering something is better than being covered.
     */
    private static Placement placement(PublishedInfo published) {
        if (published instanceof LayerGroupInfo) return Placement.GROUP;
        // a layer info that is not a feature type is raster-ish (raster, but also cascaded WMS/WMTS)
        if (!(((LayerInfo) published).getResource() instanceof FeatureTypeInfo featureType)) return Placement.RASTER;
        try {
            Class<?> geometry = featureType
                    .getFeatureType()
                    .getGeometryDescriptor()
                    .getType()
                    .getBinding();
            if (Point.class.isAssignableFrom(geometry) || MultiPoint.class.isAssignableFrom(geometry)) {
                return Placement.POINT;
            }
            if (LineString.class.isAssignableFrom(geometry) || MultiLineString.class.isAssignableFrom(geometry)) {
                return Placement.LINE;
            }
            return Placement.POLYGON;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not read the geometry type of " + published.prefixedName(), e);
            return Placement.POLYGON;
        }
    }
}
