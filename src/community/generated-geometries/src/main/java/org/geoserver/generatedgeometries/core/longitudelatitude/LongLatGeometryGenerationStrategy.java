/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.core.longitudelatitude;

import java.io.Serializable;
import static java.lang.Double.valueOf;
import static java.lang.String.format;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import static java.util.Optional.ofNullable;
import java.util.Set;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.generatedgeometries.core.GeneratedGeometryConfigurationException;
import org.geoserver.generatedgeometries.core.GeometryGenerationStrategy;
import static org.geoserver.generatedgeometries.core.GeometryGenerationStrategy.getStrategyName;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/** Implementation of geometry generation strategy for long/lat attributes in the layer. */
public class LongLatGeometryGenerationStrategy
        implements GeometryGenerationStrategy<SimpleFeatureType, SimpleFeature> {

    private static Logger LOGGER =
            Logging.getLogger(LongLatGeometryGenerationStrategy.class.getPackage().getName());

    private static final long serialVersionUID = 1L;

    static final String NAME = "longLat";
    static final String LONGITUDE_ATTRIBUTE_NAME = "longitudeAttributeName";
    static final String LATITUDE_ATTRIBUTE_NAME = "latitudeAttributeName";
    static final String GEOMETRY_ATTRIBUTE_NAME = "geometryAttributeName";
    static final String GEOMETRY_CRS = "geometryCRS";

    private final transient Map<Name, SimpleFeatureType> cache = new HashMap<>();
    private final GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
    private Set<String> featureTypeInfos = new HashSet<>();
    private Map<String, LongLatConfiguration> configurations = new HashMap<>();

    public static class LongLatConfiguration implements Serializable {
        private static final long serialVersionUID = 1L;

        public final String geomAttributeName;
        public final String longAttributeName;
        public final String latAttributeName;
        public final CoordinateReferenceSystem crs;
        public final int srid;

        public LongLatConfiguration(
                String geomAttributeName,
                String longAttributeName,
                String latAttributeName,
                CoordinateReferenceSystem crs) {
            this.geomAttributeName = geomAttributeName;
            this.longAttributeName = longAttributeName;
            this.latAttributeName = latAttributeName;
            this.crs = crs;
            try {
                this.srid = CRS.lookupEpsgCode(crs, true);
            } catch (FactoryException e) {
                throw new GeneratedGeometryConfigurationException(e);
            }
        }
    }

    Logger logger() {
        return LOGGER;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean canHandle(FeatureTypeInfo info, SimpleFeatureType unused) {
        return info != null
                && (featureTypeInfos.contains(info.getId())
                        || getStrategyName(info).map(NAME::equals).orElse(false));
    }

    @Override
    public void configure(FeatureTypeInfo info) {
        info.setSRS(CRS.toSRS(getLongLatConfiguration(info).crs));
        featureTypeInfos.add(info.getId());
    }

    @Override
    public SimpleFeatureType defineGeometryAttributeFor(
            FeatureTypeInfo info, SimpleFeatureType src) {
        if (cache.containsKey(src.getName())) {
            return cache.get(src.getName());
        }

        LongLatConfiguration configuration = getLongLatConfiguration(info);
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();

        builder.setName(src.getName());
        builder.setCRS(configuration.crs);

        builder.add(configuration.geomAttributeName, Point.class);
        for (AttributeDescriptor ad : src.getAttributeDescriptors()) {
            if (!ad.getLocalName().equalsIgnoreCase(configuration.geomAttributeName)) {
                builder.add(ad);
            }
        }
        SimpleFeatureType simpleFeatureType = builder.buildFeatureType();
        cache.put(simpleFeatureType.getName(), simpleFeatureType);
        storeConfiguration(info, configuration);
        return simpleFeatureType;
    }

    public void setConfigurationForLayer(String layerId, LongLatConfiguration configuration) {
        configurations.put(layerId, configuration);
        cache.clear();
    }

    private LongLatConfiguration getLongLatConfiguration(FeatureTypeInfo info) {
        String layerId = info.getId();
        LongLatConfiguration configuration = configurations.get(layerId);
        if (configuration == null) {
            configuration = getConfigurationFromMetadata(info);
            configurations.put(layerId, configuration);
        }
        return configuration;
    }

    private LongLatConfiguration getConfigurationFromMetadata(FeatureTypeInfo info) {
        MetadataMap metadata = info.getMetadata();
        if (metadata.containsKey(GEOMETRY_ATTRIBUTE_NAME)) {
            try {
                return new LongLatConfiguration(
                        metadata.get(GEOMETRY_ATTRIBUTE_NAME).toString(),
                        metadata.get(LONGITUDE_ATTRIBUTE_NAME).toString(),
                        metadata.get(LATITUDE_ATTRIBUTE_NAME).toString(),
                        CRS.decode(metadata.get(GEOMETRY_CRS).toString()));
            } catch (FactoryException e) {
                throw new GeneratedGeometryConfigurationException(e);
            }
        }
        throw new GeneratedGeometryConfigurationException(
                "configuration does not contain geometry attribute");
    }

    private void storeConfiguration(FeatureTypeInfo info, LongLatConfiguration configuration) {
        MetadataMap metadata = info.getMetadata();
        metadata.put(STRATEGY_METADATA_KEY, getName());
        metadata.put(GEOMETRY_ATTRIBUTE_NAME, configuration.geomAttributeName);
        metadata.put(LONGITUDE_ATTRIBUTE_NAME, configuration.longAttributeName);
        metadata.put(LATITUDE_ATTRIBUTE_NAME, configuration.latAttributeName);
        metadata.put(GEOMETRY_CRS, CRS.toSRS(configuration.crs));
    }

    @Override
    public SimpleFeature generateGeometry(
            FeatureTypeInfo info, SimpleFeatureType schema, SimpleFeature simpleFeature) {
        if (simpleFeature != null) {
            try {
                SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(schema);
                LongLatConfiguration configuration = getConfigurationFromMetadata(info);
                Double x = valueOf(getAsString(simpleFeature, configuration.longAttributeName));
                Double y = valueOf(getAsString(simpleFeature, configuration.latAttributeName));

                Point point = geometryFactory.createPoint(new Coordinate(x, y));
                point.setSRID(getLongLatConfiguration(info).srid);

                featureBuilder.add(point);
                for (Property prop : simpleFeature.getProperties()) {
                    featureBuilder.set(prop.getName(), prop.getValue());
                }
                simpleFeature = featureBuilder.buildFeature(simpleFeature.getID());
            } catch (Exception e) {
                String message =
                        format(
                                "could not generate geometry for feature [%s] of type: %s",
                                simpleFeature.getID(), schema.getName());
                logger().log(WARNING, message, e);
            }
        }
        return simpleFeature;
    }

    private String getAsString(SimpleFeature simpleFeature, String name) {
        return ofNullable(simpleFeature.getProperty(name))
                .flatMap(property -> ofNullable(property.getValue()))
                .map(Object::toString)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        format("cannot get value of property [%s]", name)));
    }

    @Override
    public Filter convertFilter(FeatureTypeInfo info, Filter filter) {
        final FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        LongLatConfiguration configuration = getLongLatConfiguration(info);
        BBOXToXYFilterVisitor dfv = new BBOXToXYFilterVisitor(ff, configuration);
        return (Filter) filter.accept(dfv, ff);
    }

    @Override
    public Query convertQuery(FeatureTypeInfo info, Query query) {
        Query q = new Query();
        q.setFilter(convertFilter(info, query.getFilter()));
        LongLatConfiguration configuration = getLongLatConfiguration(info);
        List<String> properties = new ArrayList<>();
        try {
            properties =
                    info.getFeatureType()
                            .getDescriptors()
                            .stream()
                            .filter(
                                    propertyDescriptor ->
                                            !propertyDescriptor
                                                    .getName()
                                                    .toString()
                                                    .equals(configuration.geomAttributeName))
                            .map(propertyDescriptor -> propertyDescriptor.getName().toString())
                            .collect(Collectors.toList());
        } catch (Exception e) {
            String message = format("could not convert query [%s]", query);
            logger().log(WARNING, message, e);
        }
        q.setPropertyNames(properties);
        return q;
    }
}
