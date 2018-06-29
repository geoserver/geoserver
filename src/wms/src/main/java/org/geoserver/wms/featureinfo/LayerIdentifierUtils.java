/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wms.featureinfo;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geotools.data.crs.ReprojectFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/** Contain helpers methods needed by layers identifiers. */
public final class LayerIdentifierUtils {

    private static final Logger LOGGER = Logging.getLogger(LayerIdentifierUtils.class);

    private LayerIdentifierUtils() {}

    /**
     * Helper method that tries to reproject each feature collection to the target CRS. Complex
     * features collections will not be reprojected.
     *
     * @param featureCollections feature collections to reprojected, should NOT be NULL
     * @param targetCrs reprojection target CRS, can be NULL
     * @return feature collections, some may not have been reprojected
     */
    @SuppressWarnings("unchecked")
    public static List<FeatureCollection> reproject(
            List<FeatureCollection> featureCollections, CoordinateReferenceSystem targetCrs) {
        if (targetCrs == null) {
            // nothing to do
            return featureCollections;
        }
        // try to reproject features collections to the target CRS
        return featureCollections
                .stream()
                .map(featureCollection -> reproject(featureCollection, targetCrs))
                .collect(Collectors.toList());
    }

    /**
     * Helper method that reprojects a feature collection to the target CRS. If the provided feature
     * collection doesn't contain simple features or if the source CRS is equal to the target CRS
     * nothing will be done.
     *
     * @param featureCollection feature collection to be reprojected, should NOT be NULL
     * @param targetCrs reprojection target CRS, can be NULL
     * @return feature collection, it may be reprojected or not
     */
    @SuppressWarnings("unchecked")
    public static FeatureCollection reproject(
            FeatureCollection featureCollection, CoordinateReferenceSystem targetCrs) {
        if (targetCrs == null) {
            // nothing to do
            return featureCollection;
        }
        if (!(featureCollection instanceof SimpleFeatureCollection)) {
            // not able to reproject complex features collection
            LOGGER.warning("Complex feature collection will not be reprojected.");
            return featureCollection;
        }
        // get feature collection CRS
        CoordinateReferenceSystem sourceCrs =
                featureCollection.getSchema().getCoordinateReferenceSystem();
        if (sourceCrs == null) {
            // reprojector requires the source CRS to be defined
            return featureCollection;
        }
        if (!CRS.equalsIgnoreMetadata(sourceCrs, targetCrs)) {
            try {
                // reproject to to the target CRS
                return new ReprojectFeatureResults(featureCollection, targetCrs);
            } catch (Exception exception) {
                throw new RuntimeException(
                        String.format(
                                "Error reproject feature collection from SRS '%s' to SRS '%s'.",
                                CRS.toSRS(sourceCrs), CRS.toSRS(targetCrs)),
                        exception);
            }
        }
        // the target CRS and the source CRS are the same, so nothing to do
        return featureCollection;
    }

    /**
     * Helper method that tries to find feature collection CRS. First we try to use schema defined
     * CRS then we try to find a common CRS among simple features default geometries. If this is not
     * a simple feature collection or if no common CRS can be found (i.e. we have geometries with
     * different CRS) NULL will be returned.
     *
     * @param featureCollection feature collection, should NOT be NULL
     * @return the found CRS, may be NULL
     */
    public static CoordinateReferenceSystem getCrs(FeatureCollection featureCollection) {
        CoordinateReferenceSystem crs =
                featureCollection.getSchema().getCoordinateReferenceSystem();
        if (crs != null || featureCollection.isEmpty()) {
            // the feature collection has a defined CRS or the feature collection is empty
            return crs;
        }
        // try to extract the CRS from the geometry descriptor (normally it should be NULL too)
        GeometryDescriptor geometryDescriptor =
                featureCollection.getSchema().getGeometryDescriptor();
        crs = geometryDescriptor == null ? null : geometryDescriptor.getCoordinateReferenceSystem();
        if (crs != null) {
            // the geometry descriptor has a defined CRS
            return crs;
        }
        // iterate over features and find the common CRS
        try (FeatureIterator iterator = featureCollection.features()) {
            while (iterator.hasNext()) {
                Feature feature = iterator.next();
                if (!(feature instanceof SimpleFeature)) {
                    // not a simple feature, we are done
                    return null;
                }
                SimpleFeature simpleFeature = (SimpleFeature) feature;
                Object object = simpleFeature.getDefaultGeometry();
                if (!(object instanceof Geometry)) {
                    // current feature doesn't have a geometry, move to the next one
                    continue;
                }
                // the user data may contain the coordinate reference system
                Geometry geometry = (Geometry) object;
                Object userData = geometry.getUserData();
                if (!(userData instanceof CoordinateReferenceSystem)) {
                    // no user data available or doesn't contain a coordinate reference system
                    return null;
                }
                CoordinateReferenceSystem geometryCrs = (CoordinateReferenceSystem) userData;
                if (crs != null && !CRS.equalsIgnoreMetadata(crs, geometryCrs)) {
                    // this geometry CRS is different from the other ones, we are done
                    return null;
                }
                // store the found CRS
                crs = geometryCrs;
            }
        }
        // the found common CRS among geometries, or NULL if no geometries are defined
        return crs;
    }
}
