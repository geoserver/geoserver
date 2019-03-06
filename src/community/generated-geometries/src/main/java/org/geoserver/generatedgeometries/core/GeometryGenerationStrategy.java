/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.core;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import java.io.Serializable;
import java.util.Optional;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.Query;
import org.opengis.feature.Feature;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;

/**
 * Describes particular strategy used by generated-geometries extension.
 *
 * <p>Strategy is responsible for creating 'on-the-fly' definition of geometry attribute for the
 * layer based on its data definition, as well as generating geometry from the data.
 */
public interface GeometryGenerationStrategy<FT extends FeatureType, F extends Feature>
        extends Serializable {

    String STRATEGY_METADATA_KEY = "geometryGenerationStrategy";

    /**
     * Returns strategy name optionally stored in {@link FeatureTypeInfo#getMetadata()} under {@link
     * GeometryGenerationStrategy#STRATEGY_METADATA_KEY} key.
     *
     * @param info feature layer info
     * @return optional containing strategy name or empty otherwise
     */
    static Optional<String> getStrategyName(FeatureTypeInfo info) {
        if (info == null || info.getMetadata() == null) {
            return empty();
        }
        return ofNullable(info.getMetadata().get(STRATEGY_METADATA_KEY, String.class));
    }

    /**
     * The name of the strategy that can be used for identifying it.
     *
     * @return the name
     */
    String getName();

    /**
     * Configures given info object for use by this strategy.
     *
     * @param info feature layer info
     */
    void configure(FeatureTypeInfo info);

    /**
     * Checks whether this strategy can be applied for given feature type.
     *
     * @param info feature layer info
     * @param featureType source feature type
     * @return true if can be applied, false otherwise
     */
    boolean canHandle(FeatureTypeInfo info, FT featureType);

    /**
     * Enhances definition of the feature without geometry with {@link GeometryAttribute} based on
     * particular feature attributes.
     *
     * @param info feature layer info
     * @param simpleFeatureType source feature type without geometry attribute
     * @return simple feature type with geometry attribute
     */
    FT defineGeometryAttributeFor(FeatureTypeInfo info, FT simpleFeatureType);

    /**
     * Generates {@link org.locationtech.jts.geom.Geometry} from simple type's attributes and sets
     * it as default geometry.
     *
     * @param info feature layer info
     * @param schema feature type
     * @param feature source feature
     * @return simple feature with geometry
     */
    F generateGeometry(FeatureTypeInfo info, FT schema, F feature);

    /**
     * Converts given filter operating on source CRS into CRS used by this strategy.
     *
     * <p>Throws a subclass of {@link RuntimeException} in case of failure.
     *
     * @param info feature layer info
     * @param filter source filter
     * @return transformed
     */
    Filter convertFilter(FeatureTypeInfo info, Filter filter);

    /**
     * Converts given query operating on source CRS into CRS used by this strategy.
     *
     * <p>Throws a subclass of {@link RuntimeException} in case of failure.
     *
     * @param info feature layer info
     * @param query query being converted
     * @return converted query
     */
    Query convertQuery(FeatureTypeInfo info, Query query);
}
