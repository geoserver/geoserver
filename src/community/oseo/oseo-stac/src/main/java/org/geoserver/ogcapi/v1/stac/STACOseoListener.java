/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.OseoEvent;
import org.geoserver.opensearch.eo.OseoEventListener;
import org.geoserver.opensearch.eo.store.Indexable;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.NilExpression;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.filter.function.JsonPointerFunction;
import org.geotools.util.logging.Logging;
import org.springframework.stereotype.Component;

/**
 * Handles OSEO data store events by creating or deleting queryable indices
 *
 * @author Joseph Miller
 */
@Component
public class STACOseoListener implements OseoEventListener {
    static final Logger LOGGER = Logging.getLogger(STACOseoListener.class);
    private final GeoServer geoServer;
    private final STACTemplates templates;
    private final SampleFeatures sampleFeatures;
    private final CollectionsCache collectionsCache;
    private final OpenSearchAccessProvider accessProvider;
    private static final String NOT_JSON = "NOT_JSON";

    public STACOseoListener(
            GeoServer geoServer,
            STACTemplates templates,
            SampleFeatures sampleFeatures,
            CollectionsCache collectionsCache,
            OpenSearchAccessProvider openSearchAccessProvider) {
        this.geoServer = geoServer;
        this.templates = templates;
        this.sampleFeatures = sampleFeatures;
        this.collectionsCache = collectionsCache;
        this.accessProvider = openSearchAccessProvider;
    }

    /**
     * Handles OSEO data store events by creating or deleting queryable indices
     *
     * @param event OSEO Event
     */
    @Override
    public void dataStoreChange(OseoEvent event) {
        switch (event.getType().name()) {
            case "PostInsert":
                handleCollectionsUpdateEvent(event.getCollectionName());
                break;
            case "PostUpdate":
                handleCollectionsUpdateEvent(event.getCollectionName());
                break;
            case "PreDelete":
                handleCollectionsDeleteEvent(event.getCollectionName());
                break;
        }
    }

    /**
     * Get index names via OpenSearchAccess (primarily for testing)
     *
     * @param tableName table with indices
     * @return List of index names
     */
    public List<String> getIndexListByTable(String tableName) {
        try {
            return accessProvider.getOpenSearchAccess().getIndexNames(tableName);
        } catch (IOException e) {
            LOGGER.warning(
                    "Error while getting index list for " + tableName + " " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private void handleCollectionsDeleteEvent(String collectionName) {
        try {
            accessProvider
                    .getOpenSearchAccess()
                    .updateIndexes(collectionName, Collections.emptyList());
        } catch (IOException e) {
            LOGGER.warning(
                    "Error while updating indexes for " + collectionName + " " + e.getMessage());
        }
    }

    private void handleCollectionsUpdateEvent(String collectionName) {
        try {
            // handle the empty collection case
            Feature sampleFeature = sampleFeatures.getSample(collectionName);
            if (sampleFeature == null) return;

            Map<String, Expression> expressionMap = getQueryablesMap(collectionName);
            List<Indexable> indexables =
                    expressionMap.entrySet().stream()
                            .map(
                                    entry ->
                                            new Indexable(
                                                    entry.getKey(),
                                                    entry.getValue(),
                                                    getFieldType(sampleFeature, entry.getValue())))
                            .collect(Collectors.toList());
            accessProvider.getOpenSearchAccess().updateIndexes(collectionName, indexables);
        } catch (IOException e) {
            LOGGER.warning(
                    "Error while getting creating index for "
                            + collectionName
                            + " "
                            + e.getMessage());
        }
    }

    private Indexable.FieldType getFieldType(Feature sampleFeature, Expression expression) {
        if (!(expression instanceof JsonPointerFunction)) {
            if (isGeometry(expression, sampleFeature.getType()))
                return Indexable.FieldType.Geometry;
            else if (isArray(expression, sampleFeature.getType())) return Indexable.FieldType.Array;
            return Indexable.FieldType.Other;
        }
        JsonPointerFunction jsonPointerFunction = (JsonPointerFunction) expression;
        checkAndFixJsonPointerWithoutLeadingSlash(jsonPointerFunction);
        Object evaluated = expression.evaluate(sampleFeature);
        if (evaluated instanceof NilExpression
                || evaluated == null
                || (evaluated instanceof Literal && ((Literal) evaluated).getValue() == null)) {
            // if sample returns null try your best with text
            return Indexable.FieldType.JsonString;
        }
        String out = expression.evaluate(sampleFeature).getClass().getSimpleName();
        if (isNumeric(out)) { // Convert all numeric types to double due to uncertainty of json
            // conversions from sample
            out = Double.class.getSimpleName();
        }
        return Indexable.FieldType.valueOf("Json" + out);
    }

    private boolean isGeometry(Expression expression, FeatureType type) {
        if (!(expression instanceof PropertyName)) return false;
        PropertyName pn = (PropertyName) expression;
        return type.getDescriptor(pn.getPropertyName()) instanceof GeometryDescriptor;
    }

    private boolean isArray(Expression expression, FeatureType type) {
        if (!(expression instanceof PropertyName)) return false;
        PropertyName pn = (PropertyName) expression;
        PropertyDescriptor pd = type.getDescriptor(pn.getPropertyName());
        return pd != null && pd.getType().getBinding().isArray();
    }

    private boolean isNumeric(String test) {
        boolean out = false;
        List<String> list =
                Arrays.asList(
                        new String[] {
                            Short.class.getSimpleName(),
                            Integer.class.getSimpleName(),
                            Long.class.getSimpleName(),
                            Float.class.getSimpleName(),
                            Double.class.getSimpleName(),
                            BigInteger.class.getSimpleName(),
                            BigDecimal.class.getSimpleName(),
                            Double.class.getSimpleName()
                        });
        if (list.contains(test)) {
            return true;
        }
        return out;
    }

    private void checkAndFixJsonPointerWithoutLeadingSlash(
            JsonPointerFunction jsonPointerFunction) {
        Object secondArg = jsonPointerFunction.getParameters().get(1);
        if (secondArg instanceof LiteralExpressionImpl) {
            LiteralExpressionImpl literalExpression = (LiteralExpressionImpl) secondArg;
            String path = (String) literalExpression.getValue();
            if (!path.startsWith("/")) {
                literalExpression.setValue("/" + path);
            }
        }
    }

    private Map<String, Expression> getQueryablesMap(String layerName) throws IOException {
        STACQueryablesBuilder stacQueryablesBuilder =
                new STACQueryablesBuilder(
                        null,
                        templates.getItemTemplate(layerName),
                        sampleFeatures.getSchema(),
                        sampleFeatures.getSample(layerName),
                        collectionsCache.getCollection(layerName),
                        geoServer.getService(OSEOInfo.class));
        return stacQueryablesBuilder.getExpressionMap();
    }
}
