/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.OseoEvent;
import org.geoserver.opensearch.eo.OseoEventListener;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.filter.function.JsonPointerFunction;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.NilExpression;
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
                handleCollectionsInsertEvent(event.getCollectionName());
                break;
            case "PreInsert":
                handleCollectionsInsertEvent(event.getCollectionName());
                break;
            case "PreUpdate":
                handleCollectionsDeleteEvent(event.getCollectionName());
                break;
            case "PostUpdate":
                handleCollectionsInsertEvent(event.getCollectionName());
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
            return accessProvider.getOpenSearchAccess().getIndexNamesByLayer(tableName);
        } catch (IOException e) {
            LOGGER.warning(
                    "Error while getting index list for " + tableName + " " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private void handleCollectionsDeleteEvent(String collectionName) {
        Map<String, Expression> expressionMap = null;
        try {
            expressionMap = getQueryablesMap(collectionName);

            for (Map.Entry<String, Expression> entry : expressionMap.entrySet()) {
                LOGGER.log(Level.INFO, entry.getKey() + ":" + entry.getValue().toString());
                Feature sampleFeature = sampleFeatures.getSample(collectionName);
                OpenSearchAccess.FieldType type = getFieldType(sampleFeature, entry.getValue());
                accessProvider
                        .getOpenSearchAccess()
                        .dropIndex(collectionName, entry.getKey(), entry.getValue(), type);
            }
        } catch (IOException e) {
            LOGGER.warning(
                    "Error while getting expression map for "
                            + collectionName
                            + " "
                            + e.getMessage());
        }
    }

    private void handleCollectionsInsertEvent(String collectionName) {
        Map<String, Expression> expressionMap = null;
        try {
            expressionMap = getQueryablesMap(collectionName);
        } catch (IOException e) {
            LOGGER.warning(
                    "Error while getting expression map for "
                            + collectionName
                            + " "
                            + e.getMessage());
        }
        for (Map.Entry<String, Expression> entry : expressionMap.entrySet()) {
            LOGGER.log(Level.INFO, entry.getKey() + ":" + entry.getValue().toString());
            try {
                Feature sampleFeature = sampleFeatures.getSample(collectionName);
                OpenSearchAccess.FieldType type = getFieldType(sampleFeature, entry.getValue());
                accessProvider
                        .getOpenSearchAccess()
                        .createIndex(entry.getKey(), collectionName, entry.getValue(), type);
            } catch (IOException e) {
                LOGGER.warning(
                        "Error while getting creating index for "
                                + collectionName
                                + " "
                                + e.getMessage());
            }
        }
    }

    private OpenSearchAccess.FieldType getFieldType(Feature sampleFeature, Expression expression) {
        if (!(expression instanceof JsonPointerFunction)) {
            return OpenSearchAccess.FieldType.NOT_JSON;
        }
        JsonPointerFunction jsonPointerFunction = (JsonPointerFunction) expression;
        checkAndFixJsonPointerWithoutLeadingSlash(jsonPointerFunction);
        Object evaluated = expression.evaluate(sampleFeature);
        if (evaluated instanceof NilExpression
                || evaluated == null
                || (evaluated instanceof Literal && ((Literal) evaluated).getValue() == null)) {
            // if sample returns null try your best with text
            return OpenSearchAccess.FieldType.String;
        }
        String out = expression.evaluate(sampleFeature).getClass().getSimpleName();
        if (isNumeric(out)) { // Convert all numeric types to double due to uncertainty of json
            // conversions from sample
            out = Double.class.getSimpleName();
        }
        return OpenSearchAccess.FieldType.valueOf(out);
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
