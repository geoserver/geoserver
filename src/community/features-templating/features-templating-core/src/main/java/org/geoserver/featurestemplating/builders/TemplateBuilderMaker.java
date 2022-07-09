/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders;

import static org.geoserver.featurestemplating.readers.TemplateReader.FILTERKEY;

import com.fasterxml.jackson.databind.JsonNode;
import org.geoserver.featurestemplating.builders.flat.FlatCompositeBuilder;
import org.geoserver.featurestemplating.builders.flat.FlatDynamicBuilder;
import org.geoserver.featurestemplating.builders.flat.FlatIteratingBuilder;
import org.geoserver.featurestemplating.builders.flat.FlatStaticBuilder;
import org.geoserver.featurestemplating.builders.impl.ArrayIncludeFlatBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicIncludeFlatBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicMergeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.IteratingBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;
import org.geoserver.featurestemplating.readers.TemplateReader;
import org.xml.sax.helpers.NamespaceSupport;

/** A builder of TemplateBuilder. */
public class TemplateBuilderMaker {

    private boolean rootBuilder;

    private String rootCollectionName;

    private JsonNode jsonNode;

    private String textContent;

    private String name;

    private String filter;

    private String source;

    private boolean isCollection;

    private boolean flatOutput;

    private boolean ownOutput = true;

    private boolean topLevelFeature;

    private boolean dynamicIncludeFlatBuilder = false;

    private EncodingHints encondingHints;

    private VendorOptions vendorOptions;

    private NamespaceSupport namespaces;

    private String separator = "_";

    private JsonNode baseNode;

    private JsonNode overlayNode;

    public TemplateBuilderMaker() {
        this.encondingHints = new EncodingHints();
        this.vendorOptions = new VendorOptions();
    }

    public TemplateBuilderMaker(String rootCollectionName) {
        this();
        this.rootCollectionName = rootCollectionName;
    }

    /**
     * Set the textContent for the builder to be created. Only Dynamic and Static builders can have
     * a textContent. In the case of a DynamicBuilder it will be a PropertyName directive
     * ${propertyName} or a cql expression directive $${cql}.
     *
     * @param textContent the textContent
     * @return this TemplateBuilderMaker
     */
    public TemplateBuilderMaker textContent(String textContent) {
        this.textContent = textContent.trim();
        return this;
    }

    /**
     * Set the jsonNode for the builder to be created. Only StaticBuilders can have a jsonNode
     * content.
     *
     * @param jsonNode the textContent
     * @return this TemplateBuilderMaker
     */
    public TemplateBuilderMaker jsonNode(JsonNode jsonNode) {
        this.jsonNode = jsonNode;
        return this;
    }

    /**
     * Set the baseMergeNode for the builder to be created. Only DynamicMergeBuilder can have a
     * baseMergeNode content.
     *
     * @param baseMergeNode the dynamic merge content
     * @return this TemplateBuilderMaker
     */
    public TemplateBuilderMaker baseNode(JsonNode baseMergeNode) {
        this.baseNode = baseMergeNode;
        return this;
    }

    /**
     * Set the overlayMergeNode for the builder to be created. Only DynamicMergeBuilder can have a
     * baseMergeNode content.
     *
     * @param overlayMergeNode the dynamic merge content
     * @return this TemplateBuilderMaker
     */
    public TemplateBuilderMaker overlayNode(JsonNode overlayMergeNode) {
        this.overlayNode = overlayMergeNode;
        return this;
    }

    /**
     * Set the content for the builder to be created can be a String or a JsonNode. Only Static and
     * Dynamic builder can have a value content.
     *
     * @param content the content
     * @return this TemplateBuilderMaker
     */
    public TemplateBuilderMaker content(Object content) {
        if (content instanceof String) textContent(content.toString());
        else if (content instanceof JsonNode) jsonNode((JsonNode) content);
        else
            throw new UnsupportedOperationException(
                    "Unsupported content for builders. Content is of type " + content.getClass());
        return this;
    }

    /**
     * Set the name for the builder to be created. Every builder type can have a name.
     *
     * @param name the name of the builder.
     * @return this TemplateBuilderMaker.
     */
    public TemplateBuilderMaker name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the filter for the builder to be created. Every builder type can have a filter.
     *
     * @param filter the cql filter to be set to the builder.
     * @return this TemplateBuilderMaker.
     */
    public TemplateBuilderMaker filter(String filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Parse a string to extract a filter if present and the builder content. Can be used for Static
     * or Dynamic builders
     *
     * @param contentAndFilter the string holding the builder content and eventually the filter.
     * @return this TemplateBuilderMaker.
     */
    public TemplateBuilderMaker contentAndFilter(String contentAndFilter) {
        String filter = null;
        String content = null;
        if (contentAndFilter.contains(FILTERKEY)) {
            contentAndFilter = contentAndFilter.replace(FILTERKEY + "{", "");
            int sepIndex = contentAndFilter.indexOf('}') + 1;
            String sep = String.valueOf(contentAndFilter.charAt(sepIndex));
            String[] arrData = contentAndFilter.split(sep);
            content = arrData[1];
            filter = arrData[0];
            filter = filter.substring(0, filter.length() - 1);
            filter(filter).textContent(content);
        } else {
            textContent(contentAndFilter);
        }
        return this;
    }

    /**
     * Set an xpath as a source for the builder being created (for Composite or Iterating builder
     * only).
     *
     * @param source the source of the builder being created.
     * @return this TemplateBuilderMaker.
     */
    public TemplateBuilderMaker source(String source) {
        this.source = source;
        return this;
    }

    /**
     * A flag to the TemplateBuilderMaker that the builder being created should be an
     * IteratingBuilder.
     *
     * @param collection true if the builder being created should be an IteratingBuilder.
     * @return this TemplateBuilderMaker.
     */
    public TemplateBuilderMaker collection(boolean collection) {
        isCollection = collection;
        return this;
    }

    /**
     * Set a boolean that tells the TemplateBuilderMaker if the Builder tree needs to be created
     * Flat builder types.
     *
     * @param flatOutput true if flat builders should be created.
     * @return this TemplateBuilderMaker.
     */
    public TemplateBuilderMaker flatOutput(boolean flatOutput) {
        this.flatOutput = flatOutput;
        return this;
    }

    /**
     * Set a boolean to tell the TemplateBuilderMaker if the IteratingBuilder being created should
     * be considered as the first IteratingBuilder of the builder tree.
     *
     * @param hasOwnOutput false if the Builder being created is mapping element that are wrote by
     *     ${@link
     *     org.geoserver.featurestemplating.writers.TemplateOutputWriter#startTemplateOutput(EncodingHints)}
     *     method
     * @return this TemplateBuilderMaker.
     */
    public TemplateBuilderMaker hasOwnOutput(boolean hasOwnOutput) {
        this.ownOutput = hasOwnOutput;
        return this;
    }

    /**
     * Adds an encoding option to the builder being created.
     *
     * @param name the name of the encoding option.
     * @param value the value of the encoding option.
     * @return this TemplateBuilderMaker.
     */
    public TemplateBuilderMaker encodingOption(String name, Object value) {
        this.encondingHints.put(name, value);
        return this;
    }

    /**
     * Set the namespaces to be set to the builders.
     *
     * @param namespaceSupport a NamespacesSupport object having the namespaces.
     * @return this TemplateBuilderMaker.
     */
    public TemplateBuilderMaker namespaces(NamespaceSupport namespaceSupport) {
        this.namespaces = namespaceSupport;
        return this;
    }

    /**
     * The character to be used a separator in case a Flat builder tree is being generated.
     *
     * @param separator the separator that will be used in the attributes name of a flat output.
     * @return this TemplateBuilderMaker.
     */
    public TemplateBuilderMaker separator(String separator) {
        if (separator != null) this.separator = separator;
        return this;
    }

    /**
     * Set a boolean to the TemplateBuilderMaker if the builder being created should be of type
     * RootBuilder.
     *
     * @param rootBuilder true if a Root builder is requested to be created.
     * @return this TemplateBuilderMaker.
     */
    public TemplateBuilderMaker rootBuilder(boolean rootBuilder) {
        this.rootBuilder = rootBuilder;
        return this;
    }

    /**
     * Set to true if the builder is the top level feature builder: a top level builder is a
     * SourceBuilder that is mapping the start of a Feature or of the root Feature in case of
     * complex features
     *
     * @param topLevelFeature
     * @return
     */
    public TemplateBuilderMaker topLevelFeature(boolean topLevelFeature) {
        this.topLevelFeature = topLevelFeature;
        return this;
    }

    /**
     * Set to true if the builder is having a dynamic expression inside includeFlat directive
     *
     * @param dynamicIncludeFlatBuilder
     * @return
     */
    public TemplateBuilderMaker dynamicIncludeFlatBuilder(boolean dynamicIncludeFlatBuilder) {
        this.dynamicIncludeFlatBuilder = dynamicIncludeFlatBuilder;
        return this;
    }

    /** Reset all the attributes of this TemplateBuilderMaker. */
    public void globalReset() {
        localReset();
        this.namespaces = null;
        this.separator = null;
        this.flatOutput = false;
    }

    /** Reset only the attributes set to the builder that should be local to a single builder. */
    public void localReset() {
        this.encondingHints = new EncodingHints();
        this.vendorOptions = new VendorOptions();
        this.filter = null;
        this.isCollection = false;
        this.ownOutput = true;
        this.name = null;
        this.source = null;
        this.textContent = null;
        this.jsonNode = null;
        this.rootBuilder = false;
        this.topLevelFeature = false;
        this.dynamicIncludeFlatBuilder = false;
        this.baseNode = null;
        this.overlayNode = null;
    }

    /**
     * Create a RootBuilder.
     *
     * @return a rootBuilder.
     */
    public RootBuilder buildRootBuilder() {
        RootBuilder rootBuilder = new RootBuilder();
        if (!encondingHints.isEmpty()) rootBuilder.getEncodingHints().putAll(encondingHints);
        if (!vendorOptions.isEmpty()) {
            rootBuilder.addVendorOptions(vendorOptions);
        }
        localReset();
        return rootBuilder;
    }

    private IteratingBuilder buildIteratingBuilder() {
        IteratingBuilder iteratingBuilder;
        if (flatOutput)
            iteratingBuilder =
                    new FlatIteratingBuilder(name, namespaces, separator, topLevelFeature);
        else iteratingBuilder = new IteratingBuilder(name, namespaces, topLevelFeature);
        if (source != null) iteratingBuilder.setSource(source);
        if (filter != null) iteratingBuilder.setFilter(filter);
        if (!encondingHints.isEmpty()) iteratingBuilder.getEncodingHints().putAll(encondingHints);
        if (name != null && rootCollectionName != null && rootCollectionName.equals(name))
            ownOutput = false;
        iteratingBuilder.setOwnOutput(ownOutput);
        iteratingBuilder.setTopLevelFeature(topLevelFeature);
        return iteratingBuilder;
    }

    private CompositeBuilder buildCompositeBuilder() {
        CompositeBuilder compositeBuilder;
        if (flatOutput)
            compositeBuilder =
                    new FlatCompositeBuilder(name, namespaces, separator, topLevelFeature);
        else compositeBuilder = new CompositeBuilder(name, namespaces, topLevelFeature);

        if (source != null) compositeBuilder.setSource(source);
        if (filter != null) compositeBuilder.setFilter(filter);
        if (!encondingHints.isEmpty()) compositeBuilder.getEncodingHints().putAll(encondingHints);
        compositeBuilder.setOwnOutput(ownOutput);
        return compositeBuilder;
    }

    private DynamicValueBuilder buildDynamicBuilder() {
        DynamicValueBuilder dynamicValueBuilder;
        if (flatOutput)
            dynamicValueBuilder = new FlatDynamicBuilder(name, textContent, namespaces, separator);
        else dynamicValueBuilder = new DynamicValueBuilder(name, textContent, namespaces);
        if (filter != null) dynamicValueBuilder.setFilter(filter);
        if (!encondingHints.isEmpty())
            dynamicValueBuilder.getEncodingHints().putAll(encondingHints);
        return dynamicValueBuilder;
    }

    private DynamicMergeBuilder buildDynamicMergeBuilder() {
        boolean overlayExpression = true;
        String expression;
        JsonNode node;
        if (baseNode.isTextual()) {
            expression = baseNode.textValue();
            overlayExpression = false;
            node = overlayNode;
        } else {
            expression = overlayNode.textValue();
            node = baseNode;
        }
        DynamicMergeBuilder dynamicMergeBuilder =
                new DynamicMergeBuilder(name, expression, namespaces, node, overlayExpression);
        if (filter != null) dynamicMergeBuilder.setFilter(filter);
        if (!encondingHints.isEmpty())
            dynamicMergeBuilder.getEncodingHints().putAll(encondingHints);
        return dynamicMergeBuilder;
    }

    private DynamicIncludeFlatBuilder buildDynamicIncludeFlatBuilder() {
        DynamicIncludeFlatBuilder dynamicIncludeFlatBuilder;
        dynamicIncludeFlatBuilder =
                new DynamicIncludeFlatBuilder(textContent, namespaces, baseNode);
        if (filter != null) dynamicIncludeFlatBuilder.setFilter(filter);
        if (!encondingHints.isEmpty())
            dynamicIncludeFlatBuilder.getEncodingHints().putAll(encondingHints);
        return dynamicIncludeFlatBuilder;
    }

    private ArrayIncludeFlatBuilder buildArrayIncludeFlatBuilder() {
        ArrayIncludeFlatBuilder arrayIncludeFlatBuilder =
                new ArrayIncludeFlatBuilder(name, textContent, namespaces, baseNode);
        if (filter != null) arrayIncludeFlatBuilder.setFilter(filter);
        if (!encondingHints.isEmpty())
            arrayIncludeFlatBuilder.getEncodingHints().putAll(encondingHints);
        return arrayIncludeFlatBuilder;
    }

    private StaticBuilder buildStaticBuilder() {
        StaticBuilder staticBuilder;
        boolean hasJsonNode = jsonNode != null;
        boolean hasFilter = filter != null;
        if (flatOutput) {
            if (hasJsonNode && !hasFilter)
                staticBuilder = new FlatStaticBuilder(name, jsonNode, namespaces, separator);
            else staticBuilder = new FlatStaticBuilder(name, textContent, namespaces, separator);
        } else {

            if (hasJsonNode && !hasFilter)
                staticBuilder = new StaticBuilder(name, jsonNode, namespaces);
            else staticBuilder = new StaticBuilder(name, textContent, namespaces);
        }

        if (filter != null) staticBuilder.setFilter(filter);
        if (!encondingHints.isEmpty()) staticBuilder.getEncodingHints().putAll(encondingHints);

        return staticBuilder;
    }

    /**
     * Create a builder according to the attributes that have been set. After having created it does
     * a local reset.
     *
     * @return the templateBuilder.
     */
    public TemplateBuilder build() {
        TemplateBuilder result;
        if (rootBuilder) result = buildRootBuilder();
        else if (baseNode != null && overlayNode != null) {
            result = buildDynamicMergeBuilder();
        } else if (dynamicIncludeFlatBuilder) {
            if (isCollection) result = buildArrayIncludeFlatBuilder();
            else result = buildDynamicIncludeFlatBuilder();
        } else if (textContent == null && jsonNode == null) {
            if (isCollection) result = buildIteratingBuilder();
            else result = buildCompositeBuilder();
        } else {
            if (textContent != null && textContent.contains(TemplateReader.EXPRSTART))
                result = buildDynamicBuilder();
            else result = buildStaticBuilder();
        }
        localReset();
        return result;
    }
}
