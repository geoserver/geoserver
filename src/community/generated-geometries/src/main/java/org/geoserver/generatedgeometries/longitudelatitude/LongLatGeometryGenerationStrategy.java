/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
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
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.geoserver.generatedgeometries.GeometryGenerationStrategy.getStrategyName;

/**
 * Implementation of geometry generation strategy for long/lat attributes in the layer.
 */
public class LongLatGeometryGenerationStrategy implements GeometryGenerationStrategy<SimpleFeatureType, SimpleFeature> {

    private static final long serialVersionUID = 1L;

    private static final String NAME = "longLat";

    static final String LONGITUDE_ATTRIBUTE_NAME = "longitudeAttributeName";
    static final String LATITUDE_ATTRIBUTE_NAME = "latitudeAttributeName";
    static final String GEOMETRY_ATTRIBUTE_NAME = "geometryAttributeName";
    static final int SRID = 4326;

    private final Map<Name, SimpleFeatureType> cache = new HashMap<>();
    private final GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

    private LongLatGeometryConfigurationPanel ui;

    static class LongLatConfiguration implements Serializable {
        private static final long serialVersionUID = 1L;

        final String geomAttributeName;
        final String longAttributeName;
        final String latAttributeName;

        LongLatConfiguration(
                String geomAttributeName,
                String longAttributeName,
                String latAttributeName) {
            this.geomAttributeName = geomAttributeName;
            this.longAttributeName = longAttributeName;
            this.latAttributeName = latAttributeName;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public SimpleFeatureType defineGeometryAttributeFor(FeatureTypeInfo info, SimpleFeatureType src) throws ConfigurationException {
        if (cache.containsKey(src.getName())) {
            return cache.get(src.getName());
        }

        LongLatConfiguration configuration = getLongLatConfiguration(info);
        storeConfiguration(info, configuration);

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(src.getName());
        builder.setCRS(DefaultGeographicCRS.WGS84);
        builder.add(configuration.geomAttributeName, Point.class);
        builder.add(configuration.longAttributeName, Double.class);
        builder.add(configuration.latAttributeName, Double.class);
        SimpleFeatureType simpleFeatureType = builder.buildFeatureType();
        cache.put(simpleFeatureType.getName(), simpleFeatureType);
        return simpleFeatureType;
    }

    private LongLatConfiguration getLongLatConfiguration(FeatureTypeInfo info) throws ConfigurationException {
        LongLatConfiguration configuration = getConfigurationFromMetadata(info);
        if (configuration == null) {
            checkNotNull(ui, "configuration cannot be null; createUI() method has not been called");
            configuration = ui.getLongLatConfiguration();
        }
        return configuration;
    }

    private LongLatConfiguration getConfigurationFromMetadata(FeatureTypeInfo info) {
        MetadataMap metadata = info.getMetadata();
        if (metadata.containsKey(GEOMETRY_ATTRIBUTE_NAME)) {
            return new LongLatConfiguration(
                    metadata.get(GEOMETRY_ATTRIBUTE_NAME).toString(),
                    metadata.get(LONGITUDE_ATTRIBUTE_NAME).toString(),
                    metadata.get(LATITUDE_ATTRIBUTE_NAME).toString()
            );
        }
        return null;
    }

    private void storeConfiguration(FeatureTypeInfo info, LongLatConfiguration configuration) {
        String strategyName = getStrategyName(info);
        if (isEmpty(strategyName)) {
            MetadataMap metadata = info.getMetadata();
            metadata.put(STRATEGY_METADATA_KEY, getName());
            metadata.put(GEOMETRY_ATTRIBUTE_NAME, configuration.geomAttributeName);
            metadata.put(LONGITUDE_ATTRIBUTE_NAME, configuration.longAttributeName);
            metadata.put(LATITUDE_ATTRIBUTE_NAME, configuration.latAttributeName);
        }
    }

    @Override
    public SimpleFeature generateGeometry(FeatureTypeInfo info, SimpleFeatureType schema, SimpleFeature simpleFeature) {
        if (simpleFeature != null) {
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(schema);
            LongLatConfiguration configuration = getConfigurationFromMetadata(info);
            Double x = (Double) simpleFeature.getProperty(configuration.longAttributeName).getValue();
            Double y = (Double) simpleFeature.getProperty(configuration.latAttributeName).getValue();

            Point point = geometryFactory.createPoint(new Coordinate(x, y));
            point.setSRID(SRID);
            featureBuilder.add(point);
            simpleFeature = featureBuilder.buildFeature(simpleFeature.getID());
        }
        return simpleFeature;
    }

    @Override
    public Filter convertFilter(FeatureTypeInfo info, Filter filter) throws RuntimeException {
        final FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        DuplicatingFilterVisitor dfv = new DuplicatingFilterVisitor();
        Object bb = filter.accept(dfv, ff);
        if (bb instanceof BBOX) {
            BoundingBox bounds = ((BBOX) bb).getBounds();
            LongLatConfiguration configuration = getConfigurationFromMetadata(info);
            PropertyIsBetween longitudeFilter = createBetweenFilter(ff, configuration.longAttributeName, bounds.getMinX(), bounds.getMaxX());
            PropertyIsBetween latitudeFilter = createBetweenFilter(ff, configuration.latAttributeName, bounds.getMinY(), bounds.getMaxY());
            return ff.and(longitudeFilter, latitudeFilter);
        }

        return filter;
    }

    @Override
    public Query convertQuery(FeatureTypeInfo info, Query query) {
        Query q = new Query();
        q.setFilter(convertFilter(info, query.getFilter()));
        LongLatConfiguration configuration = getConfigurationFromMetadata(info);
        q.setPropertyNames(asList(configuration.longAttributeName, configuration.latAttributeName));
        return q;
    }

    private PropertyIsBetween createBetweenFilter(FilterFactory ff, String name, double minValue, double maxValue) {
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
