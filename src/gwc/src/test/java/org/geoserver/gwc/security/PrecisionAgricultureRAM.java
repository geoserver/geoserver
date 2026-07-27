/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.security;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.security.AbstractResourceAccessManager;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.DataAccessLimits;
import org.geoserver.security.VectorAccessLimits;
import org.geotools.api.filter.Filter;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.security.core.Authentication;

/**
 * Test RAM modeling precision agriculture access control: each user owns a set of "fields" identified by integer IDs.
 * Users can only read records/raster areas belonging to their fields. A manager user can own multiple fields.
 *
 * <p>For raster layers, access limits are a {@link CoverageAccessLimits} with the union of the user's field geometries
 * as raster clip. For vector layers, access limits are a {@link VectorAccessLimits} with a {@code FIELD_ID IN (...)}
 * filter. Security tags are set to {@code "field:N"} for each owned field N - allowing targeted cache invalidation when
 * a specific field's ownership changes.
 *
 * <p>Also implements {@link SecurityCacheInvalidationSource}: calling {@link #addField} or {@link #removeField}
 * notifies all registered listeners with a {@link SecurityConfigurationChangeEvent} carrying the changed field tag.
 */
public class PrecisionAgricultureRAM extends AbstractResourceAccessManager implements SecurityCacheInvalidationSource {

    private static final GeometryFactory GF = new GeometryFactory();

    private final Map<String, Set<Integer>> userFields = new ConcurrentHashMap<>();
    private final List<SecurityCacheInvalidationListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void register(SecurityCacheInvalidationListener listener) {
        listeners.add(listener);
    }

    /** Sets the initial field assignment without firing an invalidation event. For test setup only. */
    public void setInitialFields(String username, int... fieldIds) {
        Set<Integer> fields = ConcurrentHashMap.newKeySet();
        for (int id : fieldIds) fields.add(id);
        userFields.put(username, fields);
    }

    public void addField(String username, int fieldId) {
        userFields.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(fieldId);
        fireEvent(fieldId);
    }

    public void removeField(String username, int fieldId) {
        Set<Integer> fields = userFields.get(username);
        if (fields != null) fields.remove(fieldId);
        fireEvent(fieldId);
    }

    /** Revokes several fields at once, firing a single event carrying all the affected tags. */
    public void removeFields(String username, int... fieldIds) {
        Set<Integer> fields = userFields.get(username);
        for (int id : fieldIds) {
            if (fields != null) fields.remove(id);
        }
        fireEvent(fieldIds);
    }

    private void fireEvent(int... fieldIds) {
        Set<String> tags = new HashSet<>();
        for (int id : fieldIds) tags.add("field:" + id);
        SecurityConfigurationChangeEvent event = new SecurityConfigurationChangeEvent(null, tags);
        for (SecurityCacheInvalidationListener listener : listeners) {
            listener.onSecurityConfigChange(event);
        }
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer) {
        return getAccessLimits(user, layer.getResource());
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        String username = user != null ? user.getName() : null;
        Set<Integer> fields = userFields.get(username);
        if (fields == null || fields.isEmpty()) return null;

        Set<String> tags = fields.stream().map(f -> "field:" + f).collect(Collectors.toSet());

        if (resource instanceof CoverageInfo) {
            Polygon[] polygons = fields.stream()
                    .sorted()
                    .map(PrecisionAgricultureRAM::fieldPolygon)
                    .toArray(Polygon[]::new);
            MultiPolygon clip = GF.createMultiPolygon(polygons);
            CoverageAccessLimits limits = new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, clip, null);
            limits.setSecurityTags(tags);
            return limits;
        }

        String inList = fields.stream().sorted().map(Object::toString).collect(Collectors.joining(", "));
        Filter filter;
        try {
            filter = ECQL.toFilter("FIELD_ID IN (" + inList + ")");
        } catch (CQLException e) {
            throw new IllegalStateException("Failed to build field filter: " + inList, e);
        }
        VectorAccessLimits limits = new VectorAccessLimits(CatalogMode.HIDE, null, filter, null, Filter.INCLUDE);
        limits.setSecurityTags(tags);
        return limits;
    }

    /**
     * Returns a 1x1 unit square for the given field ID. Fields are laid out in a row: field 1 = (0,0)-(1,1), field 2 =
     * (2,0)-(3,1), field 3 = (4,0)-(5,1), etc.
     */
    static Polygon fieldPolygon(int fieldId) {
        double x = (fieldId - 1) * 2.0;
        Coordinate[] ring = {
            new Coordinate(x, 0), new Coordinate(x, 1),
            new Coordinate(x + 1, 1), new Coordinate(x + 1, 0),
            new Coordinate(x, 0)
        };
        return GF.createPolygon(ring);
    }
}
