/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.writers;

import static org.geoserver.featurestemplating.builders.EncodingHints.CONTEXT;
import static org.geoserver.featurestemplating.builders.VendorOptions.COLLECTION_NAME;
import static org.geoserver.featurestemplating.builders.VendorOptions.JSONLD_TYPE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.featurestemplating.builders.EncodingHints;
import org.geoserver.featurestemplating.builders.VendorOptions;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.configuration.TemplateLoader;
import org.geoserver.featurestemplating.utils.FeatureTypeInfoUtils;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;

/** This class provides methods to handle a JSON-LD output writing outside of WFS Response class. */
public class JSONLDOutputHelper {

    private Catalog catalog;
    protected TemplateLoader loader;

    private static final Logger LOGGER = Logging.getLogger(JSONLDOutputHelper.class);

    public JSONLDOutputHelper() {
        this.catalog = (Catalog) GeoServerExtensions.bean("catalog");
        this.loader = GeoServerExtensions.bean(TemplateLoader.class);
    }

    /**
     * Retrieve all the vendor options from all the involved templates and populate and
     * EncodingHints map suitable to be consumed by the TemplateWriter.
     *
     * @param collectionList the list of FeatureCollection being encoded.
     * @return the EncodingHints map.
     * @throws ExecutionException
     */
    public EncodingHints optionsToEncodingHints(List<FeatureCollection> collectionList)
            throws ExecutionException {
        EncodingHints encodingHints = new EncodingHints();
        List<JsonNode> allContexts = new ArrayList<>();
        for (FeatureCollection collection : collectionList) {
            FeatureTypeInfo fti = FeatureTypeInfoUtils.getFeatureTypeInfo(catalog, collection);
            Request request = new Request(Dispatcher.REQUEST.get());
            request.setOutputFormat(TemplateIdentifier.JSONLD.getOutputFormat());
            RootBuilder rootBuilder = loader.getTemplate(fti, request.getOutputFormat(), request);
            if (rootBuilder != null) {
                VendorOptions options = rootBuilder.getVendorOptions();
                JsonNode node = options.get(CONTEXT, JsonNode.class);
                if (node == null)
                    node = rootBuilder.getEncodingHints().get(CONTEXT, JsonNode.class);
                if (node != null) allContexts.add(node);
                putEncodingHintIfAbsent(encodingHints, options, JSONLD_TYPE, String.class);
                putEncodingHintIfAbsent(encodingHints, options, COLLECTION_NAME, String.class);
            }
        }
        // merge contexts in case we have more then one
        JsonNode finalContext = nodesUnion(allContexts);
        encodingHints.put(CONTEXT, finalContext);
        return encodingHints;
    }

    /**
     * Retrieve a template tree.
     *
     * @param fti the FeatureTypeInfo for which retrieve the template.
     * @return the template tree as a RootBuilder.
     * @throws ExecutionException
     */
    protected RootBuilder getTemplate(FeatureTypeInfo fti) throws ExecutionException {
        return loader.getTemplate(fti, TemplateIdentifier.JSONLD.getOutputFormat());
    }

    private void putEncodingHintIfAbsent(
            EncodingHints encodingHints, VendorOptions options, String name, Class<?> targetType) {
        if (!encodingHints.hasHint(name)) {
            Object value = options.get(name, targetType);
            if (value != null) encodingHints.put(name, value);
        }
    }

    private JsonNode nodesUnion(List<JsonNode> contexts) {
        JsonNode base = null;
        for (JsonNode node : contexts) {
            if (base == null) base = node;
            else base = nodesUnion(base, node);
        }
        return base;
    }

    // union of two JsonNode. By union it is meant that a single @context is created from
    // two contexts.
    private JsonNode nodesUnion(JsonNode node1, JsonNode node2) {
        JsonNode result = node1;
        if (!node1.equals(node2)) {
            if (node1.isObject() && node2.isObject()) {
                ObjectNode copy = node1.deepCopy();
                copy.setAll((ObjectNode) node2);
                result = copy;
            } else if (node1.isArray() && node2.isArray()) {
                ArrayNode copy = node1.deepCopy();
                copy.addAll((ArrayNode) node2);
                result = copy;
            } else if (node2.isArray()) {
                ArrayNode copy = node2.deepCopy();
                copy.add(node1);
                result = copy;
            } else if (node1.isArray()) {
                ArrayNode copy = node1.deepCopy();
                copy.add(node2);
                result = copy;
            } else {
                // two text fields or one field and one object.
                ObjectMapper mapper = new ObjectMapper();
                ArrayNode arrayNode = mapper.createArrayNode();
                arrayNode.add(node1);
                arrayNode.add(node2);
                result = arrayNode;
            }
        }
        return result;
    }

    /**
     * Write the list of FeatureCollection as a JSONLD.
     *
     * @param collections the list of FeatureCollection to be encoded.
     * @param hints EncodingHints to be used by the writer.
     * @param writer the JSONLDWriter.
     * @throws IOException
     * @throws ExecutionException
     */
    void write(List<FeatureCollection> collections, EncodingHints hints, JSONLDWriter writer)
            throws IOException, ExecutionException {
        writer.startTemplateOutput(hints);
        for (FeatureCollection coll : collections) {
            FeatureTypeInfo fti = FeatureTypeInfoUtils.getFeatureTypeInfo(catalog, coll);
            Request request = new Request(Dispatcher.REQUEST.get());
            request.setOutputFormat(TemplateIdentifier.JSONLD.getOutputFormat());
            RootBuilder root = loader.getTemplate(fti, request.getOutputFormat(), request);
            if (root == null) {
                String message =
                        "Unable to find a JSON-LD template for type "
                                + fti.prefixedName()
                                + " throwing exception.";
                LOGGER.info(message);
                throw new RuntimeException(message);
            }
            FeatureIterator iterator = coll.features();
            try {
                while (iterator.hasNext()) {
                    Feature feature = iterator.next();
                    TemplateBuilderContext context = new TemplateBuilderContext(feature);
                    root.evaluate(writer, context);
                }
            } finally {
                iterator.close();
            }
        }
        writer.endTemplateOutput(hints);
    }
}
