/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.complex;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.apache.commons.lang3.tuple.Pair;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.ows.Request;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.api.feature.type.Name;
import org.geotools.data.complex.feature.type.Types;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Performs the complex to simple feature collection transformation returning the wrapped feature
 * collection.
 */
public class ComplexToSimpleOutputHandler {

    private static final Logger LOGGER = Logging.getLogger(ComplexToSimpleOutputHandler.class);

    private final FeatureCollectionResponse result;
    private final Catalog catalog;

    private final QName layerName;

    public ComplexToSimpleOutputHandler(
            Request request, FeatureCollectionResponse result, Catalog catalog) {
        this.result = requireNonNull(result);
        this.catalog = requireNonNull(catalog);
        this.layerName = requireNonNull(ComplexToSimpleOutputCommons.getLayerName(request));
    }

    /** Performs the transformation and returns the decorated Feature collection . */
    public Object execute() {
        LOGGER.fine(() -> "Executing handler for layer name: " + layerName);
        // get the rules map
        Map<String, String> rulesMap = getRulesMap();
        // get the feature collection
        @SuppressWarnings("unchecked")
        ComplexToSimpleFeatureCollection collection =
                new ComplexToSimpleFeatureCollection(
                        rulesMap, result.getFeature().get(0), buildNamespaceSupport(catalog));
        // return the correct type based on the input collection response type
        if (result instanceof FeatureCollectionResponse.WFS20) {
            return new ComplexToSimpleFeatureCollectionResponse20(result, collection);
        } else if (result instanceof FeatureCollectionResponse.WFS11) {
            return new ComplexToSimpleFeatureCollectionResponse11(result, collection);
        }
        return collection;
    }

    /** Returns the transformation rules map from the GeoServer layer configurations. */
    @SuppressWarnings("unchecked")
    private Map<String, String> getRulesMap() {
        Name name = new NameImpl(layerName);
        LayerInfo layerInfo = catalog.getLayerByName(name);
        MetadataMap metadataMap = layerInfo.getMetadata();
        Map<String, String> map =
                metadataMap.get(ComplexToSimpleOutputCommons.RULES_METADATAMAP_KEY, Map.class);
        if (map == null) {
            LOGGER.fine(() -> "Rules map not found on layer: " + layerInfo);
            return Collections.emptyMap();
        }
        LOGGER.fine(() -> "Rules map found: " + map);
        return map;
    }

    /**
     * Builds the {@link NamespaceSupport} instance from the catalog available namespaces.
     *
     * @param catalog the GeoServer catalog
     * @return the resulting instance
     */
    private NamespaceSupport buildNamespaceSupport(Catalog catalog) {
        List<NamespaceInfo> namespaces = catalog.getNamespaces();
        NamespaceSupport nsSupport = new NamespaceSupport();
        for (NamespaceInfo namespaceInfo : namespaces) {
            nsSupport.declarePrefix(namespaceInfo.getPrefix(), namespaceInfo.getURI());
        }
        // add namespaces support from the layer featureType
        addSupportedNamespaces(nsSupport);
        return nsSupport;
    }

    private void addSupportedNamespaces(NamespaceSupport nsSupport) {
        LayerInfo layerInfo = catalog.getLayerByName(new NameImpl(layerName));
        FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) layerInfo.getResource();
        List<Pair<String, String>> supportedNamespaces = getSupportedNamespaces(featureTypeInfo);
        for (Pair<String, String> pair : supportedNamespaces) {
            nsSupport.declarePrefix(pair.getLeft(), pair.getRight());
        }
    }

    private List<Pair<String, String>> getSupportedNamespaces(FeatureTypeInfo featureTypeInfo) {
        try {
            Object object =
                    featureTypeInfo
                            .getFeatureType()
                            .getUserData()
                            .get(Types.DECLARED_NAMESPACES_MAP);
            if (object instanceof Map) return Collections.emptyList();
            @SuppressWarnings("unchecked")
            Map<String, String> nsMap = (Map<String, String>) object;
            return nsMap.entrySet().stream()
                    .map(es -> Pair.of(es.getKey(), es.getValue()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return Collections.emptyList();
    }
}
