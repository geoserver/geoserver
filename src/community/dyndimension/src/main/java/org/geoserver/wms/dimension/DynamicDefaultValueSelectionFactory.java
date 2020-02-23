/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.kvp.ElevationKvpParser;
import org.geoserver.ows.kvp.TimeKvpParser;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.security.decorators.DecoratingFeatureTypeInfo;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.dimension.DefaultValueConfiguration.DefaultValuePolicy;
import org.geoserver.wms.dimension.impl.CoverageMaximumValueSelectionStrategyImpl;
import org.geoserver.wms.dimension.impl.CoverageMinimumValueSelectionStrategyImpl;
import org.geoserver.wms.dimension.impl.CoverageNearestValueSelectionStrategyImpl;
import org.geoserver.wms.dimension.impl.DimensionDefaultValueSelectionStrategyFactoryImpl;
import org.geoserver.wms.dimension.impl.FixedValueStrategyImpl;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.view.DefaultView;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.visitor.FeatureCalc;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.feature.visitor.NearestVisitor;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * A {@link DimensionDefaultValueSelectionStrategyFactory} implementation using the dynamic default
 * values configurations, is present
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DynamicDefaultValueSelectionFactory extends AbstractDispatcherCallback
        implements DimensionDefaultValueSelectionStrategyFactory,
                ExtensionPriority,
                ApplicationContextAware {
    static final Logger LOGGER = Logging.getLogger(DynamicDefaultValueSelectionFactory.class);

    static final ThreadLocal<Map<String, Map<String, Object>>> DYNAMIC_DEFAULTS =
            new ThreadLocal<Map<String, Map<String, Object>>>() {
                protected java.util.Map<String, java.util.Map<String, Object>> initialValue() {
                    return new HashMap<String, Map<String, Object>>();
                };
            };

    FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    DimensionDefaultValueSelectionStrategyFactory delegate;

    WMS wms;

    @Override
    public DimensionDefaultValueSelectionStrategy getStrategy(
            ResourceInfo resource, String dimensionName, DimensionInfo dimension) {
        Request request = Dispatcher.REQUEST.get();
        DefaultValueConfigurations config = getConfigurations(resource);
        if (config != null
                && request != null
                && ("GetMap".equalsIgnoreCase(request.getRequest())
                        || "GetFeatureInfo".equalsIgnoreCase(request.getRequest()))) {
            GetMapRequest getMap = getGetMap(request);
            try {
                Map<String, Object> defaults = getDefaultValues(resource, getMap, config);

                String key = dimensionName;
                if (dimensionName.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)) {
                    key = dimensionName.substring(ResourceInfo.CUSTOM_DIMENSION_PREFIX.length());
                }
                if (defaults.containsKey(key)) {
                    Object defaultValue = defaults.get(key);
                    Object simple = getSimpleValue(defaultValue);
                    if (simple != null) {
                        return new DefaultFixedValueStrategyFactory()
                                .createFixedValueStrategy(simple);
                    }
                }
            } catch (IOException e) {
                throw new ServiceException("Failed to setup dynamic dimension default values", e);
            }
        }

        // was not a GetMap/GetFeatureInfo, or we did not have a dynamic default for this dimension
        return delegate.getStrategy(resource, dimensionName, dimension);
    }

    /**
     * Returns the default values configuration if present, not empty, and has at least a dynamic
     * dimension to compute
     */
    private DefaultValueConfigurations getConfigurations(ResourceInfo resource) {
        DefaultValueConfigurations configurations =
                resource.getMetadata()
                        .get(DefaultValueConfigurations.KEY, DefaultValueConfigurations.class);
        if (configurations == null) {
            return null;
        }
        List<DefaultValueConfiguration> list = configurations.getConfigurations();
        if (list == null || list.isEmpty()) {
            LOGGER.fine("Skipping dynamic dimension configuration as it is empty");
            return null;
        }
        for (DefaultValueConfiguration config : list) {
            if (config.getPolicy() != null && config.getPolicy() != DefaultValuePolicy.STANDARD) {
                return configurations;
            }
        }

        LOGGER.fine(
                "Skipping dynamic dimension cconfiguration as all dimensions are "
                        + "using the standard policy (e.g. there is nothing dynamic there");
        return null;
    }

    private GetMapRequest getGetMap(Request request) {
        Operation op = request.getOperation();
        Object parsedRequest = op.getParameters()[0];
        GetMapRequest getMap;

        if (parsedRequest instanceof GetMapRequest) {
            getMap = (GetMapRequest) parsedRequest;
        } else if (parsedRequest instanceof GetFeatureInfoRequest) {
            getMap = ((GetFeatureInfoRequest) parsedRequest).getGetMapRequest();
        } else {
            throw new IllegalArgumentException(
                    "Could not get a GetMapRequest out of the parsed request, the parsed request object was:_"
                            + parsedRequest);
        }
        return getMap;
    }

    private Map<String, Object> getDefaultValues(
            ResourceInfo resource, GetMapRequest getMap, DefaultValueConfigurations configurations)
            throws IOException {

        Map<String, Map<String, Object>> defaults = DYNAMIC_DEFAULTS.get();
        String resourceKey = resource.getId();
        Map<String, Object> result = defaults.get(resourceKey);
        if (result == null) {
            result = buildDynamicDefaults(resource, getMap, configurations);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        "Computed the following dynamic dimension default values for layer "
                                + resource.prefixedName()
                                + ":\n"
                                + result
                                + "\nbased on configuration:\n"
                                + configurations);
            }
            defaults.put(resourceKey, result);
        }
        return result;
    }

    @Override
    public void finished(Request request) {
        DYNAMIC_DEFAULTS.remove();
    }

    private Map<String, Object> buildDynamicDefaults(
            ResourceInfo resource, GetMapRequest getMap, DefaultValueConfigurations configsBean)
            throws IOException {

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Building dynamic defaults for resource " + resource.prefixedName());
        }

        List<DefaultValueConfiguration> configurations =
                filterConfigurations(configsBean, resource);

        Map<String, Object> result;
        result = new HashMap<String, Object>();
        Map<String, List<? extends Object>> incompleteSpecs =
                new HashMap<String, List<? extends Object>>();

        // lookup for user specified restrictions and dimensions that have a static default
        for (Map.Entry<String, Serializable> entry : resource.getMetadata().entrySet()) {
            String key = entry.getKey();
            Serializable metadata = entry.getValue();
            if (metadata instanceof DimensionInfo) {
                // skip disabled dimensions
                DimensionInfo di = (DimensionInfo) metadata;
                if (!di.isEnabled()) {
                    continue;
                }

                // get the dimension name
                String dimensionName;
                if (key.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)) {
                    dimensionName = key.substring(ResourceInfo.CUSTOM_DIMENSION_PREFIX.length());
                } else {
                    dimensionName = key;
                }

                // check if the values were provided already in the request, we are
                // going to use them for domain restriction
                boolean foundExplicitDefault = false;
                if (ResourceInfo.TIME.equals(dimensionName)
                        && getMap.getRawKvp().get("time") != null) {
                    List<Object> times = getMap.getTime();
                    addExplicitValues(dimensionName, times, result, incompleteSpecs);
                    foundExplicitDefault = true;
                } else if (ResourceInfo.ELEVATION.equals(dimensionName)
                        && getMap.getRawKvp().get("elevation") != null) {
                    List<Object> elevations = getMap.getElevation();
                    addExplicitValues(dimensionName, elevations, result, incompleteSpecs);
                    foundExplicitDefault = true;
                } else if (getMap.getRawKvp().get("dim_" + dimensionName) != null) {
                    List<String> customDimensions = getMap.getCustomDimension(dimensionName);
                    addExplicitValues(dimensionName, customDimensions, result, incompleteSpecs);
                    foundExplicitDefault = true;
                }

                if (!foundExplicitDefault) {
                    if (!hasDynamicConfiguration(configurations, dimensionName)) {
                        DimensionDefaultValueSelectionStrategy strategy =
                                delegate.getStrategy(resource, key, di);
                        if (strategy == null) {
                            LOGGER.warning(
                                    "Skipping static setting of default dimension value for dimension "
                                            + dimensionName
                                            + " in layer "
                                            + resource.prefixedName()
                                            + " as the default value strategy could not be found");
                        } else {
                            Object staticDefault =
                                    strategy.getDefaultValue(
                                            resource,
                                            dimensionName,
                                            di,
                                            getDimensionClass(dimensionName));
                            if (staticDefault != null) {
                                result.put(dimensionName, wrapIntoList(staticDefault));
                            } else {
                                LOGGER.warning(
                                        "Skipping static setting of default dimension value for dimension "
                                                + dimensionName
                                                + " in layer "
                                                + resource.prefixedName()
                                                + " as the default value strategy "
                                                + strategy
                                                + "returned a null value");
                            }
                        }
                    }
                }
            }
        }

        // if there are incomplete user value specifications, resolve them in the same order as the
        // dynamic defaults
        if (!incompleteSpecs.isEmpty()) {
            for (DefaultValueConfiguration config : configurations) {
                String dimensionName = config.getDimension();
                if (!incompleteSpecs.containsKey(dimensionName)) {
                    continue;
                }

                List<Object> values = (List<Object>) incompleteSpecs.remove(dimensionName);
                List<Object> defaulted = new ArrayList<Object>(values.size());
                for (int i = 0; i < values.size(); i++) {
                    Object v = values.get(i);
                    if (v != null) {
                        defaulted.add(v);
                    } else {
                        Object dynamicValue =
                                getDynamicDefault(resource, result, config, dimensionName);
                        if (dynamicValue != null) {
                            defaulted.add(dynamicValue);
                        }
                    }

                    result.put(dimensionName, defaulted);
                }
            }

            // the ones that have no dynamc behavior get copied straight
            result.putAll(incompleteSpecs);
        }

        // now go for the ones having dynamic defaults
        for (DefaultValueConfiguration config : configurations) {
            String dimensionName = config.getDimension();

            // do we have explicit defaults already?
            if (result.containsKey(dimensionName)) {
                continue;
            } else {
                // compute the dynamic default
                Object value = getDynamicDefault(resource, result, config, dimensionName);
                if (value != null) {
                    result.put(dimensionName, value);
                }
            }
        }
        return result;
    }

    private boolean hasDynamicConfiguration(
            List<DefaultValueConfiguration> configurations, String dimensionName) {
        for (DefaultValueConfiguration config : configurations) {
            if (dimensionName.equals(config.getDimension())) {
                return true;
            }
        }

        return false;
    }

    private List<DefaultValueConfiguration> filterConfigurations(
            DefaultValueConfigurations configsBean, ResourceInfo resource) {
        List<DefaultValueConfiguration> configs = configsBean.getConfigurations();
        List<DefaultValueConfiguration> result =
                new ArrayList<DefaultValueConfiguration>(configs.size());
        MetadataMap metadata = resource.getMetadata();
        for (DefaultValueConfiguration config : configs) {
            String key = getDimensionMetadataKey(config.getDimension());
            DimensionInfo di = metadata.get(key, DimensionInfo.class);
            if (di == null) {
                LOGGER.warning(
                        "Skipping dynamic default configuration for dimension "
                                + config.getDimension()
                                + " as the base dimension configuration is missing");
            } else if (!di.isEnabled()) {
                LOGGER.warning(
                        "Skipping dynamic default configuration for dimension "
                                + config.getDimension()
                                + " as the dimension is not enabled");
            } else {
                result.add(config);
            }
        }

        return result;
    }

    /**
     * Separates values provided by the user that do not need defaulting, from those where the user
     * asked explicitly for a default value
     */
    private void addExplicitValues(
            String dimensionName,
            List<? extends Object> values,
            Map<String, Object> completeSpecs,
            Map<String, List<? extends Object>> incompleteSpecs) {
        if (values.contains(null)) {
            incompleteSpecs.put(dimensionName, values);
        } else {
            completeSpecs.put(dimensionName, values);
        }
    }

    private Object getDynamicDefault(
            ResourceInfo resource,
            Map<String, Object> result,
            DefaultValueConfiguration config,
            String dimensionName)
            throws IOException {
        // get the dimension info
        DimensionInfo di =
                resource.getMetadata()
                        .get(getDimensionMetadataKey(config.getDimension()), DimensionInfo.class);

        // ok, let's see what policy we have here
        Class<?> dimensionClass = getDimensionClass(config.getDimension());
        if (config.policy == DefaultValuePolicy.STANDARD) {
            if (di == null) {
                return null;
            } else {
                // we fetch the value anyways so that we can run domain restrictions later
                DimensionDefaultValueSelectionStrategy strategy =
                        delegate.getStrategy(resource, config.dimension, di);
                Object value =
                        strategy.getDefaultValue(resource, dimensionName, di, dimensionClass);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            "Computed default value for dimension "
                                    + dimensionName
                                    + " using standard policy "
                                    + strategy
                                    + " which resulted in: "
                                    + value);
                }

                return value;
            }
        } else if (config.policy == DefaultValuePolicy.EXPRESSION) {
            return applyExpression(result, config, dimensionName, dimensionClass);
        } else if (config.policy == DefaultValuePolicy.LIMIT_DOMAIN) {
            if (resource instanceof FeatureTypeInfo) {
                Object value = getVectorDefaultValue((FeatureTypeInfo) resource, config, result);
                return value;
            } else if (resource instanceof CoverageInfo) {
                DimensionDefaultValueSelectionStrategy delegateStrategy =
                        delegate.getStrategy(
                                resource, getDimensionMetadataKey(config.getDimension()), di);
                Object value =
                        getRasterDefaultValue(
                                (CoverageInfo) resource, config, result, delegateStrategy);
                return value;
            } else {
                throw new IllegalArgumentException(
                        "Don't know how to handle domain restriction for layers of type "
                                + resource.getClass());
            }
        }

        return null;
    }

    private Object applyExpression(
            Map<String, Object> result,
            DefaultValueConfiguration config,
            String dimensionName,
            Class<?> dimensionClass) {
        Expression expression;
        String expressionStr = config.getDefaultValueExpression();
        try {
            expression = ECQL.toExpression(expressionStr);
        } catch (CQLException e) {
            throw new ServiceException(
                    "Failed to parse default value expression " + expressionStr, e);
        }
        SimpleFeature values = buildSampleFeature(result);
        String strValue = expression.evaluate(values, String.class);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "Computed default value for dimension "
                            + dimensionName
                            + " using expression "
                            + expression
                            + " returned value "
                            + strValue);
        }
        try {
            Object value;
            if (strValue == null) {
                value = null;
            } else if (Date.class.isAssignableFrom(dimensionClass)) {
                value = new TimeKvpParser("whatever").parse(strValue);
            } else if (Double.class.isAssignableFrom(dimensionClass)) {
                value = new ElevationKvpParser("whatever").parse(strValue);
            } else {
                value = strValue;
            }
            value = getSimpleValue(value);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Computed value " + strValue + " got parsed to " + value);
            }

            return value;
        } catch (ParseException e) {
            throw new ServiceException(
                    "Failed to parse value returned from dynamic default value expression: "
                            + strValue,
                    e);
        }
    }

    /**
     * Dimension values are often wrapped in list and/or using ranges, unwrap them to get a single
     * value
     */
    private Object getSimpleValue(Object value) {
        if (value instanceof List) {
            List list = (List) value;
            if (list.isEmpty()) {
                value = null;
            } else {
                value = list.get(0);
            }
        }
        if (value instanceof DateRange) {
            value = ((DateRange) value).getMinValue();
        } else if (value instanceof NumberRange) {
            value = ((NumberRange) value).getMinimum();
        }
        return value;
    }

    private Object getRasterDefaultValue(
            CoverageInfo resource,
            DefaultValueConfiguration config,
            Map<String, Object> restrictions,
            DimensionDefaultValueSelectionStrategy delegateStrategy)
            throws IOException {
        String dimensionName = config.getDimension();
        DimensionInfo di =
                resource.getMetadata()
                        .get(getDimensionMetadataKey(config.getDimension()), DimensionInfo.class);

        // check if it's a fixed strategy, simple case, no domain restriction needed
        if (delegateStrategy instanceof FixedValueStrategyImpl) {
            Class<?> dimensionClass = getDimensionClass(config.getDimension());
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        "Dynamic domain restriction on dimension "
                                + dimensionName
                                + " in layer "
                                + resource.prefixedName()
                                + " not possible, the default value strategy is a fixed value one");
            }
            return delegateStrategy.getDefaultValue(resource, dimensionName, di, dimensionClass);
        }

        // ok, grab the reader, and check it's a structured one
        GridCoverageReader genericReader = resource.getGridCoverageReader(null, null);
        if (!(genericReader instanceof StructuredGridCoverage2DReader)) {
            throw new IllegalStateException(
                    "Cannot perform dynaminc domain restriction unless the reader is a structured one");
        }

        // we have a descriptor, now we need to find the association between the exposed
        // dimension names and the granule source attributes
        StructuredGridCoverage2DReader reader = (StructuredGridCoverage2DReader) genericReader;

        String coverageName = resource.getNativeCoverageName();
        if (coverageName == null) {
            coverageName = reader.getGridCoverageNames()[0];
        }
        Map<String, DimensionDescriptor> descriptors =
                getDimensionDescriptors(reader, coverageName);
        DimensionFilterBuilder builder = new DimensionFilterBuilder(ff);
        for (Map.Entry<String, Object> entry : restrictions.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            List<Object> values = wrapIntoList(value);

            DimensionDescriptor descriptor = descriptors.get(name);
            if (descriptor == null) {
                throw new ServiceException(
                        "Could not find dimension "
                                + name
                                + " in coverage reader backing "
                                + resource.prefixedName());
            }

            builder.appendFilters(
                    descriptor.getStartAttribute(), descriptor.getEndAttribute(), values);
        }
        Filter domainRestriction = builder.getFilter();

        // get the restricted domain
        GranuleSource granules = reader.getGranules(coverageName, true);
        Query q = new Query(granules.getSchema().getTypeName(), domainRestriction);
        SimpleFeatureCollection fc = granules.getGranules(q);

        DimensionDescriptor dd = descriptors.get(dimensionName);
        if (dd == null) {
            throw new ServiceException(
                    "Could not find dimension "
                            + dimensionName
                            + " in coverage reader backing "
                            + resource.prefixedName());
        }
        FeatureCalc calc = getFeatureCalcForStrategy(delegateStrategy, dd);
        fc.accepts(calc, null);
        Object result = calc.getResult().getValue();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "Computed default value for "
                            + dimensionName
                            + " in layer "
                            + resource.prefixedName()
                            + " using "
                            + domainRestriction
                            + " to limit the domain resulted in value: "
                            + result);
        }
        return result;
    }

    private FeatureCalc getFeatureCalcForStrategy(
            DimensionDefaultValueSelectionStrategy delegateStrategy, DimensionDescriptor dd) {
        String sa = dd.getStartAttribute();
        String ea = dd.getEndAttribute();
        if (delegateStrategy instanceof CoverageMaximumValueSelectionStrategyImpl) {
            if (ea != null) {
                return new MaxVisitor(ea);
            } else {
                return new MaxVisitor(sa);
            }
        } else if (delegateStrategy instanceof CoverageMinimumValueSelectionStrategyImpl) {
            return new MinVisitor(sa);
        } else if (delegateStrategy instanceof CoverageNearestValueSelectionStrategyImpl) {
            CoverageNearestValueSelectionStrategyImpl impl =
                    (CoverageNearestValueSelectionStrategyImpl) delegateStrategy;
            Object targetValue = impl.getTargetValue();
            return new NearestVisitor(ff.property(sa), targetValue);
        } else {
            throw new ServiceException(
                    "Don't konw how to restrict the domain for strategy " + delegateStrategy);
        }
    }

    private Map<String, DimensionDescriptor> getDimensionDescriptors(
            StructuredGridCoverage2DReader reader, String coverageName) throws IOException {
        Map<String, DimensionDescriptor> result = new HashMap<String, DimensionDescriptor>();
        List<DimensionDescriptor> dimensionDescriptors =
                reader.getDimensionDescriptors(coverageName);
        for (DimensionDescriptor dd : dimensionDescriptors) {
            result.put(dd.getName(), dd);
        }

        return new CaseInsensitiveMap(result);
    }

    /** Applies the normal policy, but restricted to the */
    private Object getVectorDefaultValue(
            FeatureTypeInfo resource,
            DefaultValueConfiguration config,
            Map<String, Object> restrictions)
            throws IOException {
        DimensionInfo di =
                resource.getMetadata()
                        .get(getDimensionMetadataKey(config.getDimension()), DimensionInfo.class);
        DimensionDefaultValueSelectionStrategy strategy =
                delegate.getStrategy(resource, config.getDimension(), di);

        // vector data only support time and elevation now
        Object time = restrictions.get(ResourceInfo.TIME);
        List<Object> times = wrapIntoList(time);

        Object elevation = restrictions.get(ResourceInfo.ELEVATION);
        List<Object> elevations = wrapIntoList(elevation);

        // reuse the same logic as dimension selection to restrict the domain of possible values
        final Filter filter = wms.getTimeElevationToFilter(times, elevations, resource);

        ResourceInfo restrictedResource;
        if (filter != null && !Filter.INCLUDE.equals(filter)) {
            restrictedResource =
                    new DecoratingFeatureTypeInfo(resource) {

                        @Override
                        public FeatureSource getFeatureSource(
                                ProgressListener listener, Hints hints) throws IOException {
                            FeatureSource fs = super.getFeatureSource(listener, hints);
                            if (!(fs instanceof SimpleFeatureSource)) {
                                throw new IllegalStateException(
                                        "Cannot apply dynamic dimension restrictions to complex features");
                            }
                            SimpleFeatureSource simpleSource = (SimpleFeatureSource) fs;
                            try {
                                return new DefaultView(
                                        simpleSource,
                                        new Query(simpleSource.getSchema().getTypeName(), filter));
                            } catch (SchemaException e) {
                                throw new IOException("Failed to restrict the domain");
                            }
                        }
                    };

        } else {
            restrictedResource = resource;
        }

        Class<?> dimensionClass = getDimensionClass(config.getDimension());
        Object result =
                strategy.getDefaultValue(
                        restrictedResource, config.getDimension(), di, dimensionClass);

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "Computing default value for"
                            + config.getDimension()
                            + " in layer "
                            + resource.prefixedName()
                            + " using the following filter to restrict the domain: "
                            + filter);
        }
        return result;
    }

    /** If the object is not a List, it wraps it into one */
    private List<Object> wrapIntoList(Object value) {
        List<Object> values;
        if (value == null) {
            return null;
        } else if (value instanceof List) {
            values = (List<Object>) value;
        } else {
            values = new ArrayList<Object>();
            values.add(value);
        }
        return values;
    }

    private SimpleFeature buildSampleFeature(Map<String, Object> values) {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        for (String dimension : values.keySet()) {
            tb.add(dimension, Object.class);
        }
        tb.setName("DimensionValues");
        SimpleFeatureType schema = tb.buildFeatureType();

        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object value = entry.getValue();
            value = getSimpleValue(value);
            fb.set(entry.getKey(), value);
        }

        return fb.buildFeature(null);
    }

    public String getDimensionMetadataKey(String dimension) {
        if (ResourceInfo.TIME.equals(dimension) || ResourceInfo.ELEVATION.equals(dimension)) {
            return dimension;
        } else {
            return ResourceInfo.CUSTOM_DIMENSION_PREFIX + dimension;
        }
    }

    public Class<?> getDimensionClass(String dimension) {
        if (ResourceInfo.TIME.equals(dimension)) {
            return Date.class;
        } else if (ResourceInfo.ELEVATION.equals(dimension)) {
            return Double.class;
        } else {
            return String.class;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // get the normal dimension evaluator
        delegate =
                applicationContext.getBean(DimensionDefaultValueSelectionStrategyFactoryImpl.class);
        // avoid dependency loop
        wms = applicationContext.getBean(WMS.class);
    }

    @Override
    public int getPriority() {
        // pick an average value, we allow for further overrides if needs be
        return (ExtensionPriority.HIGHEST + ExtensionPriority.LOWEST) / 2;
    }
}
