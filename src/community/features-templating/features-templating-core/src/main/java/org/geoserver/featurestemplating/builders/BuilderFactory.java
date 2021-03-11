/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.featurestemplating.builders;

import com.fasterxml.jackson.databind.JsonNode;
import org.geoserver.featurestemplating.builders.flat.FlatCompositeBuilder;
import org.geoserver.featurestemplating.builders.flat.FlatDynamicBuilder;
import org.geoserver.featurestemplating.builders.flat.FlatIteratingBuilder;
import org.geoserver.featurestemplating.builders.flat.FlatStaticBuilder;
import org.geoserver.featurestemplating.builders.geojson.GeoJSONRootBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.IteratingBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;
import org.geoserver.featurestemplating.builders.jsonld.JSONLDRootBuilder;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * A factory to produce builders according to the output format and the value of the vendor option
 * flat_output.
 */
public class BuilderFactory {

    /**
     * Name of the collection at the root of the document. This one receives special treatment as
     * it's the one that the FeatureCollection to be encoded is iterated on.
     */
    private static final String GEOJSON_ROOT_COLLECTION_NAME = "features";

    private final String rootCollectionName;

    private boolean isJsonLd;
    private boolean flatOutput;
    private String separator = "_";

    public BuilderFactory(boolean isJsonLd) {
        this(isJsonLd, GEOJSON_ROOT_COLLECTION_NAME);
    }

    public BuilderFactory(boolean isJsonLd, String rootCollectionName) {
        this.isJsonLd = isJsonLd;
        this.rootCollectionName = rootCollectionName;
    }

    /**
     * Produce an IteratingBuilder
     *
     * @param key the attribute key
     * @param namespaces the namespaces
     * @return an IteratingBuilder
     */
    public TemplateBuilder getIteratingBuilder(String key, NamespaceSupport namespaces) {
        IteratingBuilder iterating;
        if (flatOutput) iterating = new FlatIteratingBuilder(key, namespaces, separator);
        else iterating = new IteratingBuilder(key, namespaces);
        iterating.setRootCollection(key != null && key.equalsIgnoreCase(rootCollectionName));
        return iterating;
    }

    /**
     * Produce a CompositeBuilder
     *
     * @param key the attribute key
     * @param namespaces the namespaces
     * @return a CompositeBuilder
     */
    public TemplateBuilder getCompositeBuilder(String key, NamespaceSupport namespaces) {
        if (isJsonLd) new CompositeBuilder(key, namespaces);
        if (flatOutput) return new FlatCompositeBuilder(key, namespaces, separator);
        return new CompositeBuilder(key, namespaces);
    }

    /**
     * Produce a DynamicValueBuilder
     *
     * @param key the attribute key
     * @param expression the expression or property name as a string
     * @param namespaces the namespaces
     * @return a DynamicValueBuilder
     */
    public TemplateBuilder getDynamicBuilder(
            String key, String expression, NamespaceSupport namespaces) {
        TemplateBuilder dynamic;
        //        if (isJsonLd) {
        //            dynamic = new JSONLDDynamicBuilder(key, expression, namespaces);
        //        } else {
        if (flatOutput && !isJsonLd)
            dynamic = new FlatDynamicBuilder(key, expression, namespaces, separator);
        else dynamic = new DynamicValueBuilder(key, expression, namespaces);
        //        }
        return dynamic;
    }

    /**
     * Produce a DynamicValueBuilder
     *
     * @param key the attribute key
     * @param strValue the static value as a string
     * @param namespaces the namespaces
     * @return a StaticValueBuilder
     */
    public TemplateBuilder getStaticBuilder(
            String key, String strValue, NamespaceSupport namespaces) {
        TemplateBuilder staticBuilder;
        if (isJsonLd) {
            staticBuilder = new StaticBuilder(key, strValue, namespaces);
        } else {
            if (flatOutput)
                staticBuilder = new FlatStaticBuilder(key, strValue, namespaces, separator);
            else staticBuilder = new StaticBuilder(key, strValue, namespaces);
        }
        return staticBuilder;
    }

    /**
     * Produce a DynamicValueBuilder
     *
     * @param key the attribute key
     * @param value the static value as a JsonNode
     * @param namespaces the namespaces
     * @return a StaticBuilder
     */
    public TemplateBuilder getStaticBuilder(
            String key, JsonNode value, NamespaceSupport namespaces) {
        TemplateBuilder staticBuilder;
        if (isJsonLd) {
            staticBuilder = new StaticBuilder(key, value, namespaces);
        } else {
            if (flatOutput)
                staticBuilder = new FlatStaticBuilder(key, value, namespaces, separator);
            else staticBuilder = new StaticBuilder(key, value, namespaces);
        }
        return staticBuilder;
    }

    /**
     * Produce a RootBuilder
     *
     * @return a RootBuilder
     */
    public RootBuilder getRootBuilder() {
        if (isJsonLd) return new JSONLDRootBuilder();
        else return new GeoJSONRootBuilder();
    }

    /**
     * Set flat_output vendor option value
     *
     * @param flatOutput flat_output vendor option value
     */
    public void setFlatOutput(boolean flatOutput) {
        this.flatOutput = flatOutput;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }
}
