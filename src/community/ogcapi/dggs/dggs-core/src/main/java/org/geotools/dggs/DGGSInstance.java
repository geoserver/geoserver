/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.dggs;

import com.google.common.collect.Iterators;
import java.util.Iterator;
import java.util.List;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

/**
 * A particular DGGS instance, provides access to zones and conversion operations between zones and
 * classic geometries. A DDGS system can be providing a single instance (e.g., H3) or multiple
 * configurable ones (e.g., rHEALPix)
 */
// TODO: parametrize on T, type of the id, String for rpix but Long for H3. Trying to use
// the string everywhere pretty much failed, leading to bad performance in clickhouse store
// though we might have to do some experiments to check if the numeric id works any better,
// the non hierarchical structure of the H3 ids gets lots of the blame here (
public interface DGGSInstance extends AutoCloseable {

    static final ReferencedEnvelope WORLD =
            new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84);

    public String getIdentifier();

    /** Closes the {@link DGGSInstance} and the resources it is using */
    @Override
    void close();

    /** Returns the list of resolution levels for this DGGS */
    int[] getResolutions();

    /**
     * Allows an instance to describe extra zone properties besides the usual id, geometry, and
     * resolution. Extra property values for a zone can be retrieved using
     * Zone#getExtraPropertyValues()
     *
     * @return
     */
    List<AttributeDescriptor> getExtraProperties();

    /**
     * Returns a {@link Zone} given its id
     *
     * @param id The zone identifier
     */
    Zone getZone(String id);

    /** Returns the zone containing the specified position, at the given resolution */
    Zone getZone(double lat, double lon, int resolution);

    /**
     * Retuns zones covering the desired envelope at the target resolution
     *
     * @param envelope The envelope being queried
     * @param resolution The target resolution
     * @param compact If true, return a lower number of zones needed to cover the area, using parent
     *     zones as needed. Not required to be the minimum set of zones, meant to speed up data
     *     queries only.
     */
    Iterator<Zone> zonesFromEnvelope(Envelope envelope, int resolution, boolean compact);

    /**
     * Returns the count of zones covering the desired envelope, at the target resolution. The
     * default implementation just uses {@link #zonesFromEnvelope(Envelope, int, boolean)},
     * subclasses can provide a better optimized implemnetation
     *
     * @param envelope The area of search
     * @param resolution The target resolution
     * @return A zone count
     */
    default long countZonesFromEnvelope(Envelope envelope, int resolution) {
        return Iterators.size(zonesFromEnvelope(envelope, resolution, false));
    }

    /**
     * Returns the neighbors of a given zone
     *
     * @param id The zone id
     * @param radius The search radius
     * @return
     */
    Iterator<Zone> neighbors(String id, int radius);

    /**
     * Returns the count of neighboring zones. The default implementation just uses {@link
     * #neighbors(String, int)}, subclasses can provide a better optimized implemnetation
     *
     * @param envelope The area of search
     * @param resolution The target resolution
     * @return A zone count
     */
    default long countNeighbors(String id, int resolution) {
        return Iterators.size(neighbors(id, resolution));
    }

    /** Returns the list of children of a zone at the desired resolution level */
    Iterator<Zone> children(String id, int resolution);

    /**
     * Returns the count of children zones. The default implementation just uses {@link
     * #children(String, int)}, subclasses can provide a better optimized implementation
     */
    default long countChildren(String id, int resolution) {
        return Iterators.size(children(id, resolution));
    }

    /**
     * Returns all parents of a given zone, at all resolution levels TODO: normally it's a small
     * number of zones, though it could grow in a system where a cell can have multiple parents
     *
     * @param id
     * @return
     */
    Iterator<Zone> parents(String id);

    /**
     * Counts all the parents of a given zone, at all resolution levels. The default implementation
     * just uses {@link * #parents(String)}, subclasses can provide a better optimized
     * implementation
     */
    default long countParents(String id) {
        long count = 0;
        Iterator<Zone> iterator = parents(id);
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }

    /**
     * Maps a point and a resolution to the corresponding DGGS zone.
     *
     * @param point A point expressed in CRS84
     * @param resolution The target resolution
     */
    Zone point(Point point, int resolution);

    /**
     * Maps a polygon and a resolution to the corresponding list of DGGS zones.
     *
     * @param polygon A polygon expressed in CRS84
     * @param resolution The target resolution
     * @param compact If true, return the minimum number of zones needed to cover the area, using
     *     parent zones as needed.
     */
    Iterator<Zone> polygon(Polygon polygon, int resolution, boolean compact);

    /**
     * Returns the count of zones in the polygon. The default implementation just uses {@link
     * #polygon(POlygon, int, boolean)}, subclasses can provide a better optimized implemnetation
     *
     * @param polygon A polygon expressed in CRS84
     * @param resolution The target resolution
     */
    default long countPolygon(Polygon polygon, int resolution) {
        return Iterators.size(polygon(polygon, resolution, false));
    }

    /**
     * Given a parent id, returns a filter matching all possible children of a given parent.
     *
     * <p>Typically implemented as a range or like search based on the hierarchical structure of the
     * zoneId.
     *
     * <p>Used to optimize filters in database searches, as opposed to enumerating all children one
     * by one.
     *
     * <p>The caller is already adding a "AND resolution <=|== targetResolution" in the query, the
     * resolution and "upTo" values are to be used only to encode zoneId related filters.
     *
     * @param ff The filter factory used to build the response Filter
     * @param zoneId The parent zone id
     * @param upTo If true, return a filter matching all the children, from the direct ones, up to
     *     the given solution. If false, return a filter matching only the children at the target
     *     resolution instead.
     */
    Filter getChildFilter(FilterFactory2 ff, String zoneId, int resolution, boolean upTo);
}
