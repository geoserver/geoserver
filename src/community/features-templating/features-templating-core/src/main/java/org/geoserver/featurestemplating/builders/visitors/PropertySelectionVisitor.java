/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.visitors;

import static org.geoserver.featurestemplating.builders.TemplateBuilderUtils.hasSelectableKey;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.JSONFieldSupport;
import org.geoserver.featurestemplating.builders.SourceBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.ArrayIncludeFlatBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicIncludeFlatBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicJsonBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicMergeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.IteratingBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;
import org.geoserver.featurestemplating.builders.selectionwrappers.CompositePropertySelection;
import org.geoserver.featurestemplating.builders.selectionwrappers.DynamicPropertySelection;
import org.geoserver.featurestemplating.builders.selectionwrappers.IncludeArrayPropertySelection;
import org.geoserver.featurestemplating.builders.selectionwrappers.IncludeFlatPropertySelection;
import org.geoserver.featurestemplating.builders.selectionwrappers.IteratingPropertySelection;
import org.geoserver.featurestemplating.builders.selectionwrappers.MergePropertySelection;
import org.geoserver.featurestemplating.builders.selectionwrappers.PropertySelectionWrapper;
import org.geoserver.featurestemplating.builders.selectionwrappers.StaticPropertySelection;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.feature.type.PropertyType;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/**
 * A DuplicatingVisitor performing a selection of TemplateBuilders based on selected
 * attributes/keys. This visitor is able to select only TemplateBuilders where the full key (the
 * entire path of keys comprising the ones of the parent builders) is static. If the full key is not
 * static a {@link PropertySelectionWrapper} is used and the selection is delegated at template
 * evaluation time.
 */
public class PropertySelectionVisitor extends DuplicatingTemplateVisitor {

    private PropertySelectionHandler selectionHandler;
    private PropertyType propertyType;
    private FilterAttributeExtractor extractor;
    private Set<String> queryProperties;

    private static final Logger LOGGER = Logging.getLogger(PropertySelectionVisitor.class);

    public PropertySelectionVisitor(
            PropertySelectionHandler selectionHandler, PropertyType propertyType) {
        this.selectionHandler = selectionHandler;
        this.propertyType = propertyType;
        this.extractor = new FilterAttributeExtractor();
        this.queryProperties = new HashSet<>();
    }

    @Override
    public Object visit(IteratingBuilder iteratingBuilder, Object extradata) {
        PropertySelectionContext selectionExtradata = createExtradata(iteratingBuilder, extradata);
        IteratingBuilder copy =
                (IteratingBuilder) super.visit(iteratingBuilder, selectionExtradata);
        return selectBuilder(copy, selectionExtradata);
    }

    @Override
    public Object visit(CompositeBuilder compositeBuilder, Object extradata) {
        PropertySelectionContext selectionExtradata = createExtradata(compositeBuilder, extradata);
        CompositeBuilder copy =
                (CompositeBuilder) super.visit(compositeBuilder, selectionExtradata);
        return selectBuilder(copy, selectionExtradata);
    }

    @Override
    public Object visit(DynamicValueBuilder dynamicBuilder, Object extradata) {
        Object result;
        if (dynamicBuilder instanceof DynamicMergeBuilder) {
            result = visit((DynamicMergeBuilder) dynamicBuilder, extradata);
        } else if (dynamicBuilder instanceof DynamicIncludeFlatBuilder) {
            result = visit((DynamicIncludeFlatBuilder) dynamicBuilder, extradata);
        } else {
            PropertySelectionContext selectionExtradata =
                    createExtradata(dynamicBuilder, extradata);
            DynamicValueBuilder copy =
                    (DynamicValueBuilder) super.visit(dynamicBuilder, selectionExtradata);
            AbstractTemplateBuilder builder = selectBuilder(copy, selectionExtradata);
            result = wrapJsonValuesBuilder(builder, selectionExtradata);
        }
        return result;
    }

    @Override
    public Object visit(StaticBuilder staticBuilder, Object extradata) {
        PropertySelectionContext selectionExtradata = createExtradata(staticBuilder, extradata);
        StaticBuilder copy = (StaticBuilder) super.visit(staticBuilder, selectionExtradata);
        AbstractTemplateBuilder builder = selectBuilder(copy, selectionExtradata);
        return wrapJsonValuesBuilder(builder, selectionExtradata);
    }

    @Override
    public Object visit(DynamicMergeBuilder merge, Object extradata) {
        PropertySelectionContext selectionExtradata = createExtradata(merge, extradata);
        DynamicMergeBuilder copy = (DynamicMergeBuilder) super.visit(merge, selectionExtradata);
        AbstractTemplateBuilder builder = selectBuilder(copy, selectionExtradata);
        if (builder != null) {
            builder = new MergePropertySelection((DynamicMergeBuilder) builder, selectionHandler);
        }
        return builder;
    }

    @Override
    public Object visit(DynamicIncludeFlatBuilder includeFlat, Object extradata) {
        PropertySelectionContext selectionExtradata = createExtradata(includeFlat, extradata);
        DynamicIncludeFlatBuilder copy =
                (DynamicIncludeFlatBuilder) super.visit(includeFlat, selectionExtradata);
        return selectBuilder(copy, selectionExtradata);
    }

    public Set<String> getQueryProperties() {
        return queryProperties;
    }

    // check if the builder has a dynamic key
    private boolean hasDynamicKey(AbstractTemplateBuilder builder) {
        boolean result = false;
        Expression expression = builder.getKey();
        if (expression != null) {
            expression.accept(extractor, null);
            result = !extractor.isConstantExpression();
        }
        return result;
    }

    // wrap the builder if the key cannot be computed before template evaluation.
    private PropertySelectionWrapper wrapWhenNonStaticKey(AbstractTemplateBuilder builder) {
        PropertySelectionWrapper result = null;
        if (builder instanceof DynamicIncludeFlatBuilder) {
            result =
                    new IncludeFlatPropertySelection(
                            (DynamicIncludeFlatBuilder) builder, selectionHandler);
        } else if (builder instanceof ArrayIncludeFlatBuilder) {
            result =
                    new IncludeArrayPropertySelection(
                            (ArrayIncludeFlatBuilder) builder, selectionHandler);
        } else {
            boolean wrap = hasDynamicKey(builder);
            if (builder instanceof DynamicValueBuilder && wrap) {
                DynamicValueBuilder dynamic = (DynamicValueBuilder) builder;
                result = new DynamicPropertySelection(dynamic, selectionHandler);
            } else if (builder instanceof StaticBuilder && wrap) {
                StaticBuilder staticBuilder = (StaticBuilder) builder;
                result = new StaticPropertySelection(staticBuilder, selectionHandler);
            } else if (wrap && builder instanceof CompositeBuilder) {
                result =
                        new CompositePropertySelection(
                                (CompositeBuilder) builder, selectionHandler);
            } else if (wrap && builder instanceof IteratingBuilder) {
                result =
                        new IteratingPropertySelection(
                                (IteratingBuilder) builder, selectionHandler);
            }
        }
        return result;
    }

    // wrap a builder if it holds a JsonValue that potentially might be selected
    private AbstractTemplateBuilder wrapJsonValuesBuilder(
            AbstractTemplateBuilder builder, PropertySelectionContext extradata) {
        if (builder instanceof StaticBuilder) {
            builder = wrapStaticBuilder((StaticBuilder) builder, extradata);
        } else if (builder instanceof DynamicValueBuilder) {
            queryProperties.addAll(extractSelectedProperties(builder, extradata));
            builder = wrapDynamicBuilder((DynamicValueBuilder) builder, extradata);
        }
        return builder;
    }

    private AbstractTemplateBuilder wrapStaticBuilder(
            StaticBuilder builder, PropertySelectionContext extradata) {
        AbstractTemplateBuilder result = builder;
        JsonNode jsonValue = builder.getStaticValue();
        if (jsonValue != null
                && !jsonValue.isValueNode()
                && selectionHandler.hasSelectableJsonValue(builder)) {
            PropertySelectionWrapper wrapper =
                    new StaticPropertySelection(builder, selectionHandler);
            if (!extradata.isDynamicKeyParent()) wrapper.setFullKey(extradata.getStaticFullKey());
            result = wrapper;
        }
        return result;
    }

    private AbstractTemplateBuilder wrapDynamicBuilder(
            DynamicValueBuilder builder, PropertySelectionContext extradata) {
        AbstractTemplateBuilder result = builder;
        if (hasJsonField(builder) && selectionHandler.hasSelectableJsonValue(builder)) {
            PropertySelectionWrapper wrapper =
                    new DynamicPropertySelection(builder, selectionHandler);
            if (!extradata.isDynamicKeyParent()) wrapper.setFullKey(extradata.getStaticFullKey());
            result = wrapper;
        }
        return result;
    }

    private Set<PropertyName> extractPropertyNames(DynamicValueBuilder dynamicValueBuilder) {
        Set<PropertyName> props;
        if (dynamicValueBuilder.getXpath() != null) {
            props = new HashSet<>();
            props.add(dynamicValueBuilder.getXpath());
        } else {
            FilterAttributeExtractor extractor = new FilterAttributeExtractor();
            dynamicValueBuilder.getCql().accept(extractor, null);
            props = extractor.getPropertyNameSet();
            extractor.clear();
        }
        return props;
    }

    private boolean hasJsonField(DynamicValueBuilder valueBuilder) {
        Set<PropertyName> propertyNames = extractPropertyNames(valueBuilder);
        boolean result = false;
        for (PropertyName pn : propertyNames) {
            Object evalRes = pn.evaluate(propertyType);
            if (evalRes instanceof PropertyDescriptor) {
                result = JSONFieldSupport.isJSONField((PropertyDescriptor) evalRes);
                break;
            }
        }
        return result;
    }

    private AbstractTemplateBuilder selectBuilder(
            AbstractTemplateBuilder templateBuilder, PropertySelectionContext extradata) {
        PropertySelectionWrapper runtimeSelection = wrapWhenNonStaticKey(templateBuilder);

        if (runtimeSelection != null) {
            extradata.setDynamicKeyCurrent(true);

            if (!extradata.isDynamicKeyParent()) {
                runtimeSelection.setFullKey(extradata.getStaticFullKey());
                if (selectionHandler.isBuilderSelected(runtimeSelection, extradata))
                    templateBuilder = runtimeSelection;
                else templateBuilder = null;
            } else {
                templateBuilder = runtimeSelection;
            }

            if (templateBuilder != null) {
                Set<String> props =
                        extractSelectedProperties(runtimeSelection.getDelegate(), extradata);
                queryProperties.addAll(props);
            }
        } else {
            boolean isSelected = selectionHandler.isBuilderSelected(templateBuilder, extradata);
            if (!isSelected) templateBuilder = null;
            else queryProperties.addAll(extractSelectedProperties(templateBuilder, extradata));
        }

        return templateBuilder;
    }

    private PropertySelectionContext createExtradata(TemplateBuilder current, Object extradata) {
        PropertySelectionContext selectionExtradata;
        if (extradata instanceof PropertySelectionContext)
            selectionExtradata = new PropertySelectionContext((PropertySelectionContext) extradata);
        else selectionExtradata = new PropertySelectionContext();
        if (current instanceof AbstractTemplateBuilder
                && !selectionExtradata.isDynamicKeyParent()) {
            updateFullStaticKey((AbstractTemplateBuilder) current, selectionExtradata);
        }
        return selectionExtradata;
    }

    private void updateFullStaticKey(
            AbstractTemplateBuilder abstractBuilder, PropertySelectionContext selectionExtradata) {
        boolean hasValidKey = hasSelectableKey(abstractBuilder);
        if (hasValidKey) {
            boolean dynamicKey = hasDynamicKey(abstractBuilder);
            if (dynamicKey) {
                selectionExtradata.setDynamicKeyCurrent(true);
            } else {
                String currentFullKey = selectionExtradata.getStaticFullKey();
                String key = abstractBuilder.getKey(null);
                if (currentFullKey != null && key != null)
                    selectionExtradata.setStaticFullKey(currentFullKey.concat(".").concat(key));
                else if (key != null) selectionExtradata.setStaticFullKey(key);
            }
        }
    }

    private Set<String> extractSelectedProperties(
            AbstractTemplateBuilder templateBuilder, PropertySelectionContext context) {
        Set<String> result = Collections.emptySet();
        if (templateBuilder instanceof DynamicJsonBuilder) {
            DynamicJsonBuilder jsonBuilder = (DynamicJsonBuilder) templateBuilder;
            result = getPropertiesFromDynamicJsonBuilder(jsonBuilder, context);
        } else if (templateBuilder instanceof DynamicValueBuilder) {
            result = getPropertiesFromDynamic((DynamicValueBuilder) templateBuilder);
        } else if (templateBuilder instanceof SourceBuilder) {
            SourceBuilder sourceBuilder = (SourceBuilder) templateBuilder;
            result = getPropertiesFromSourceBuilder(sourceBuilder);
        }
        if (hasDynamicKey(templateBuilder)) {
            Set<String> props = getPropertiesFromDynamicKey(templateBuilder);
            if (result.isEmpty()) result = props;
            else result.addAll(props);
        }
        return result;
    }

    private Set<String> getPropertiesFromDynamicJsonBuilder(
            DynamicJsonBuilder dynamicJsonBuilder, PropertySelectionContext context) {
        Set<String> props = new HashSet<>(1);
        if (dynamicJsonBuilder.getXpath() != null) {
            props.add(dynamicJsonBuilder.getXpath().getPropertyName());
        } else if (dynamicJsonBuilder.getCql() != null) {
            dynamicJsonBuilder
                    .getCql()
                    .accept(extractor, createExtradata(dynamicJsonBuilder, context));
        }
        props.addAll(extractor.getAttributeNameSet());
        logQueryAttributes(dynamicJsonBuilder, true, extractor.getAttributeNameSet());
        extractor.clear();
        extractor.getPropertyNameSet().clear();
        if (!context.isDynamicKeyParent()) {
            JsonNode jsonNode = dynamicJsonBuilder.getNode();
            JsonNode node =
                    selectionHandler.pruneJsonAttributes(jsonNode, context.getStaticFullKey());
            TemplateBuilder tb = dynamicJsonBuilder.getNestedTree(node, null);
            tb.accept(this, context);
        }
        return props;
    }

    private void logQueryAttributes(
            AbstractTemplateBuilder templateBuilder, boolean includeKey, Set<String> attributes) {
        if (LOGGER.isLoggable(Level.FINE)) {
            includeKey = includeKey && !(templateBuilder instanceof DynamicIncludeFlatBuilder);
            StringBuilder logMsg = new StringBuilder("Found ");
            boolean hasAttributes = attributes != null && !attributes.isEmpty();
            if (hasAttributes) logMsg.append(" the following ");
            else logMsg.append(" 0 ");

            logMsg.append("query properties for builder with type ")
                    .append(templateBuilder.getClass().getSimpleName());
            if (includeKey) {
                String key = templateBuilder.getKey(null);
                logMsg.append(" and with ");
                if (key == null) logMsg.append("null key");
                else logMsg.append("key ").append(key);
            }
            if (hasAttributes)
                logMsg.append(": ").append(attributes.stream().collect(Collectors.joining(",")));
        }
    }

    private Set<String> getPropertiesFromDynamic(DynamicValueBuilder dynamicValueBuilder) {
        Set<String> props = new HashSet<>(1);
        if (dynamicValueBuilder.getXpath() != null) {
            props.add(dynamicValueBuilder.getXpath().getPropertyName());
        } else if (dynamicValueBuilder.getCql() != null) {
            dynamicValueBuilder.getCql().accept(extractor, null);
        }
        props.addAll(extractor.getAttributeNameSet());
        logQueryAttributes(dynamicValueBuilder, true, extractor.getAttributeNameSet());
        extractor.clear();
        extractor.getPropertyNameSet().clear();
        return props;
    }

    private Set<String> getPropertiesFromSourceBuilder(SourceBuilder sourceBuilder) {
        Set<String> props = new HashSet<>(1);
        if (sourceBuilder.getSource() != null) {
            sourceBuilder.getSource().accept(extractor, null);
            props.addAll(extractor.getAttributeNameSet());
            logQueryAttributes(sourceBuilder, true, extractor.getAttributeNameSet());
            extractor.clear();
            extractor.getPropertyNameSet().clear();
        }
        return props;
    }

    private Set<String> getPropertiesFromDynamicKey(AbstractTemplateBuilder templateBuilder) {
        Set<String> props = new HashSet<>(1);
        Expression exp = templateBuilder.getKey();
        if (exp != null) {
            exp.accept(extractor, null);
        }
        props.addAll(extractor.getAttributeNameSet());
        logQueryAttributes(templateBuilder, false, extractor.getAttributeNameSet());
        return props;
    }
}
