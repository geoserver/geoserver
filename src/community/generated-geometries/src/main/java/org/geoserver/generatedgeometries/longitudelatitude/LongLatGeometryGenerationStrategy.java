/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.longitudelatitude;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.generatedgeometries.GeometryGenerationStrategy;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.geometry.BoundingBox;
import org.vfny.geoserver.global.ConfigurationException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.Double.valueOf;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.logging.Level.WARNING;
import static org.geoserver.generatedgeometries.GeometryGenerationStrategy.getStrategyName;

/** Implementation of geometry generation strategy for long/lat attributes in the layer. */
public class LongLatGeometryGenerationStrategy
        implements GeometryGenerationStrategy<SimpleFeatureType, SimpleFeature> {

    private static Logger LOGGER =
            Logging.getLogger(LongLatGeometryGenerationStrategy.class.getPackage().getName());

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_SRID = 4326;
    static final String DEFAULT_SRS = "EPSG:4326";

    static final String NAME = "longLat";

    static final String LONGITUDE_ATTRIBUTE_NAME = "longitudeAttributeName";
    static final String LATITUDE_ATTRIBUTE_NAME = "latitudeAttributeName";
    static final String GEOMETRY_ATTRIBUTE_NAME = "geometryAttributeName";

    private final transient Map<Name, SimpleFeatureType> cache = new HashMap<>();
    private final GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

    private LongLatGeometryConfigurationPanel ui;
    private Set<String> featureTypeInfos = new HashSet<>();

    static class LongLatConfiguration implements Serializable {
        private static final long serialVersionUID = 1L;

        final String geomAttributeName;
        final String longAttributeName;
        final String latAttributeName;

        LongLatConfiguration(
                String geomAttributeName, String longAttributeName, String latAttributeName) {
            this.geomAttributeName = geomAttributeName;
            this.longAttributeName = longAttributeName;
            this.latAttributeName = latAttributeName;
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
        info.setSRS(DEFAULT_SRS);
        featureTypeInfos.add(info.getId());
    }

    @Override
    public SimpleFeatureType defineGeometryAttributeFor(FeatureTypeInfo info, SimpleFeatureType src)
            throws ConfigurationException {
        if (cache.containsKey(src.getName())) {
            return cache.get(src.getName());
        }

        LongLatConfiguration configuration = getLongLatConfiguration(info);

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.init(src);
        builder.setName(src.getName());
        builder.setCRS(DefaultGeographicCRS.WGS84);
        builder.add(configuration.geomAttributeName, Point.class);
        builder.setDefaultGeometry(configuration.geomAttributeName);

        SimpleFeatureType simpleFeatureType = builder.buildFeatureType();
        cache.put(simpleFeatureType.getName(), simpleFeatureType);
        storeConfiguration(info, configuration);
        return simpleFeatureType;
    }

    private LongLatConfiguration getLongLatConfiguration(FeatureTypeInfo info)
            throws ConfigurationException {
        if (ui != null) {
            return ui.getLongLatConfiguration();
        }
        return getConfigurationFromMetadata(info);
    }

    private LongLatConfiguration getConfigurationFromMetadata(FeatureTypeInfo info) {
        MetadataMap metadata = info.getMetadata();
        if (metadata.containsKey(GEOMETRY_ATTRIBUTE_NAME)) {
            return new LongLatConfiguration(
                    metadata.get(GEOMETRY_ATTRIBUTE_NAME).toString(),
                    metadata.get(LONGITUDE_ATTRIBUTE_NAME).toString(),
                    metadata.get(LATITUDE_ATTRIBUTE_NAME).toString());
        }
        return null;
    }

    private void storeConfiguration(FeatureTypeInfo info, LongLatConfiguration configuration) {
        MetadataMap metadata = info.getMetadata();
        metadata.put(STRATEGY_METADATA_KEY, getName());
        metadata.put(GEOMETRY_ATTRIBUTE_NAME, configuration.geomAttributeName);
        metadata.put(LONGITUDE_ATTRIBUTE_NAME, configuration.longAttributeName);
        metadata.put(LATITUDE_ATTRIBUTE_NAME, configuration.latAttributeName);
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
                point.setSRID(DEFAULT_SRID);
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
    public Filter convertFilter(FeatureTypeInfo info, Filter filter) throws RuntimeException {
        final FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        DuplicatingFilterVisitor dfv = new DuplicatingFilterVisitor();
        Object bb = filter.accept(dfv, ff);
        if (bb instanceof BBOX) {
            BoundingBox bounds = ((BBOX) bb).getBounds();
            LongLatConfiguration configuration = getConfigurationFromMetadata(info);
            PropertyIsBetween longitudeFilter =
                    createBetweenFilter(
                            ff,
                            configuration.longAttributeName,
                            bounds.getMinX(),
                            bounds.getMaxX());
            PropertyIsBetween latitudeFilter =
                    createBetweenFilter(
                            ff, configuration.latAttributeName, bounds.getMinY(), bounds.getMaxY());
            return ff.and(longitudeFilter, latitudeFilter);
        }

        return filter;
    }

    @Override
    public Query convertQuery(FeatureTypeInfo info, Query query) {
        Query q = new Query();
        q.setFilter(convertFilter(info, query.getFilter()));
        LongLatConfiguration configuration = getConfigurationFromMetadata(info);
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

    private PropertyIsBetween createBetweenFilter(
            FilterFactory ff, String name, double minValue, double maxValue) {
        PropertyName propertyName = ff.property(name);
        Literal min = ff.literal(minValue);
        Literal max = ff.literal(maxValue);
        return ff.between(propertyName, min, max);
    }

    @Override
    public Component createUI(String id, IModel model) {
        return ui = new LongLatGeometryConfigurationPanel(id, model);
    }
}
