/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.function.BiConsumer;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicIncludeFlatBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;
import org.geotools.api.feature.Feature;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;

/** Class visiting a feature template and performing some action on the properties mapped via dynamic builders */
class TemplatePropertyVisitor {

    public static final String JSON_PROPERTY_TYPE = "jsonPropertyType";

    private final TemplateBuilder templateBuilder;
    private final BiConsumer<String, DynamicValueBuilder> propertyConsumer;
    private final Feature sampleFeature;

    TemplatePropertyVisitor(
            TemplateBuilder templateBuilder,
            Feature sampleFeature,
            BiConsumer<String, DynamicValueBuilder> propertyConsumer) {
        this.templateBuilder = templateBuilder;
        this.propertyConsumer = propertyConsumer;
        this.sampleFeature = sampleFeature;
    }

    public void visit() {
        this.visitTemplateBuilder(null, templateBuilder, true);
    }

    private void visitTemplateBuilder(String parentPath, TemplateBuilder atb, boolean skipPath) {
        // no queryables out of static builders for the moment, we might want
        // to revisit once we consider eventual filters
        if (atb instanceof StaticBuilder || !(atb instanceof AbstractTemplateBuilder)) return;

        // dynamic include flat builders eat their parent node to perform dynamic merge,
        // get it out and visit its children directly, without further checks (the returned builder
        // is a key-less composite)
        if (atb instanceof DynamicIncludeFlatBuilder builder) {
            visitDynamicIncludeFlatBuilder(parentPath, builder);
            return;
        }

        // check the key, if we get a null it means the key is dynamic and it's not possible
        // to do anything with this JSON sub-tree
        String key = ((AbstractTemplateBuilder) atb).getKey(null);
        if (key == null && ((AbstractTemplateBuilder) atb).getKey() != null) return;

        String path = getPath(parentPath, key, skipPath);
        if (atb instanceof DynamicValueBuilder db) {
            propertyConsumer.accept(path, db);
        } else {
            for (TemplateBuilder child : atb.getChildren()) {
                visitTemplateBuilder(path, child, false);
            }
        }
    }

    private void visitDynamicIncludeFlatBuilder(String parentPath, DynamicIncludeFlatBuilder atb) {
        DynamicIncludeFlatBuilder dyn = atb;

        // handle the parent node, encapsulated to handle overrides
        TemplateBuilder parentBuilder = dyn.getIncludingNodeBuilder(parentPath);
        visitChildren(parentPath, parentBuilder);

        // visit the overlay based on the sample feature
        TemplateBuilder dataBuilder = dyn.getIncludeFlatBuilder(parentPath, sampleFeature);
        if (dataBuilder == null) return;
        visitChildren(parentPath, dataBuilder);

        // however in this case we want to extract queryables also for the static builders
        // found, as they are really coming from the database, so in fact, dynamic
        if (dataBuilder instanceof CompositeBuilder && dyn.getXpath() != null) {
            for (TemplateBuilder child : dataBuilder.getChildren()) {
                if (child instanceof StaticBuilder sb) {
                    Expression keyex = sb.getKey();
                    if (!(keyex instanceof Literal)) continue;
                    String key = keyex.evaluate(null, String.class);

                    // for now, just consider direct properties
                    JsonNode value = sb.getStaticValue();
                    JsonNodeType nodeType = value.getNodeType();
                    if (nodeType == JsonNodeType.NUMBER
                            || nodeType == JsonNodeType.STRING
                            || nodeType == JsonNodeType.BOOLEAN) {
                        String path = parentPath != null ? parentPath + "." + key : key;
                        String cql = "$${jsonPointer(" + dyn.getXpath().getPropertyName() + ", '" + key + "')}";
                        DynamicValueBuilder fakeBuilder =
                                new DynamicValueBuilder(key, cql, ((CompositeBuilder) parentBuilder).getNamespaces());
                        fakeBuilder.addEncodingHint(JSON_PROPERTY_TYPE, getClass(value));
                        propertyConsumer.accept(path, fakeBuilder);
                    }
                }
            }
        }
    }

    private Class getClass(JsonNode value) {
        JsonNodeType nodeType = value.getNodeType();
        switch (nodeType) {
            case NUMBER:
                return Double.class;
            case STRING:
                if (isDate(value.textValue())) return Date.class;
                return String.class;
            case BOOLEAN:
                return Boolean.class;
            default:
                throw new IllegalArgumentException("Cannot handle this node type: " + nodeType);
        }
    }

    private boolean isDate(String txt) {
        try {
            DateTimeFormatter.ISO_INSTANT.parse(txt);
        } catch (DateTimeParseException e) {
            return false;
        }
        return true;
    }

    private void visitChildren(String parentPath, TemplateBuilder parentBuilder) {
        if (parentBuilder instanceof CompositeBuilder) {
            for (TemplateBuilder child : parentBuilder.getChildren()) {
                visitTemplateBuilder(parentPath, child, false);
            }
        }
    }

    private String getPath(String parentPath, String key, boolean skipPath) {
        if (skipPath) return null;
        return parentPath == null ? key : parentPath + "." + key;
    }
}
