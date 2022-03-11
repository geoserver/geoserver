/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.visitors;

import static org.geoserver.featurestemplating.builders.TemplateBuildersUtils.hasSelectableKey;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Set;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.JSONFieldSupport;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicIncludeFlatBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicMergeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.IteratingBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;
import org.geoserver.featurestemplating.builders.selectionwrappers.DynamicPropertySelection;
import org.geoserver.featurestemplating.builders.selectionwrappers.IncludeFlatPropertySelection;
import org.geoserver.featurestemplating.builders.selectionwrappers.MergePropertySelection;
import org.geoserver.featurestemplating.builders.selectionwrappers.PropertySelectionWrapper;
import org.geoserver.featurestemplating.builders.selectionwrappers.StaticPropertySelection;
import org.geotools.filter.FilterAttributeExtractor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
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
    private FeatureType featureType;

    public PropertySelectionVisitor(
            PropertySelectionHandler selectionHandler, FeatureType featureType) {
        this.selectionHandler = selectionHandler;
        this.featureType = featureType;
    }

    @Override
    public Object visit(IteratingBuilder iteratingBuilder, Object extradata) {
        PropertySelectionExtradata selectionExtradata =
                createExtradata(iteratingBuilder, extradata);
        IteratingBuilder copy =
                (IteratingBuilder) super.visit(iteratingBuilder, selectionExtradata);
        return selectBuilder(copy, selectionExtradata);
    }

    @Override
    public Object visit(CompositeBuilder compositeBuilder, Object extradata) {
        PropertySelectionExtradata selectionExtradata =
                createExtradata(compositeBuilder, extradata);
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
            PropertySelectionExtradata selectionExtradata =
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
        PropertySelectionExtradata selectionExtradata = createExtradata(staticBuilder, extradata);
        StaticBuilder copy = (StaticBuilder) super.visit(staticBuilder, selectionExtradata);
        AbstractTemplateBuilder builder = selectBuilder(copy, selectionExtradata);
        return wrapJsonValuesBuilder(builder, selectionExtradata);
    }

    @Override
    public Object visit(DynamicMergeBuilder merge, Object extradata) {
        PropertySelectionExtradata selectionExtradata = createExtradata(merge, extradata);
        DynamicMergeBuilder copy = (DynamicMergeBuilder) super.visit(merge, selectionExtradata);
        return selectBuilder(copy, selectionExtradata);
    }

    @Override
    public Object visit(DynamicIncludeFlatBuilder includeFlat, Object extradata) {
        PropertySelectionExtradata selectionExtradata = createExtradata(includeFlat, extradata);
        DynamicIncludeFlatBuilder copy =
                (DynamicIncludeFlatBuilder) super.visit(includeFlat, selectionExtradata);
        return selectBuilder(copy, selectionExtradata);
    }

    // check if the builder has a dynamic key
    private boolean hasDynamicKey(AbstractTemplateBuilder builder) {
        boolean result = false;
        Expression expression = builder.getKey();
        if (expression != null) {
            FilterAttributeExtractor extractor = new FilterAttributeExtractor();
            expression.accept(extractor, null);
            result = !extractor.isConstantExpression();
        }
        return result;
    }

    // wrap the builder if the key cannot be computed before template evaluation.
    private PropertySelectionWrapper wrapWhenNonStaticKey(AbstractTemplateBuilder builder) {
        PropertySelectionWrapper result = null;
        if (builder instanceof DynamicMergeBuilder)
            result = new MergePropertySelection((DynamicMergeBuilder) builder, selectionHandler);
        else if (builder instanceof DynamicIncludeFlatBuilder)
            result =
                    new IncludeFlatPropertySelection(
                            (DynamicIncludeFlatBuilder) builder, selectionHandler);
        else {
            boolean wrap = hasDynamicKey(builder);
            if (builder instanceof DynamicValueBuilder && wrap) {
                DynamicValueBuilder dynamic = (DynamicValueBuilder) builder;
                result = new DynamicPropertySelection(dynamic, selectionHandler);
            } else if (builder instanceof StaticBuilder && wrap) {
                StaticBuilder staticBuilder = (StaticBuilder) builder;
                result = new StaticPropertySelection(staticBuilder, selectionHandler);
            } else if (wrap) {
                result = new PropertySelectionWrapper(builder, selectionHandler);
            }
        }
        return result;
    }

    // wrap a builder if it holds a JsonValue that potentially might be selected
    private AbstractTemplateBuilder wrapJsonValuesBuilder(
            AbstractTemplateBuilder builder, PropertySelectionExtradata extradata) {
        if (builder instanceof StaticBuilder) {
            builder = wrapStaticBuilder((StaticBuilder) builder, extradata);
        } else if (builder instanceof DynamicValueBuilder) {
            builder = wrapDynamicBuilder((DynamicValueBuilder) builder, extradata);
        }
        return builder;
    }

    private AbstractTemplateBuilder wrapStaticBuilder(
            StaticBuilder builder, PropertySelectionExtradata extradata) {
        AbstractTemplateBuilder result = builder;
        JsonNode jsonValue = builder.getStaticValue();
        if (jsonValue != null
                && !jsonValue.isValueNode()
                && selectionHandler.mustWrapJsonValueBuilder(builder)) {
            PropertySelectionWrapper wrapper =
                    new StaticPropertySelection(builder, selectionHandler);
            if (!extradata.isDynamicKeyParent()) wrapper.setFullKey(extradata.getStaticFullKey());
            result = wrapper;
        }
        return result;
    }

    private AbstractTemplateBuilder wrapDynamicBuilder(
            DynamicValueBuilder builder, PropertySelectionExtradata extradata) {
        AbstractTemplateBuilder result = builder;
        if (hasJsonField(builder) && selectionHandler.mustWrapJsonValueBuilder(builder)) {
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
        }
        return props;
    }

    private boolean hasJsonField(DynamicValueBuilder valueBuilder) {
        Set<PropertyName> propertyNames = extractPropertyNames(valueBuilder);
        boolean result = false;
        for (PropertyName pn : propertyNames) {
            Object evalRes = pn.evaluate(featureType);
            if (evalRes instanceof PropertyDescriptor) {
                result = JSONFieldSupport.isJSONField((PropertyDescriptor) evalRes);
                break;
            }
        }
        return result;
    }

    private AbstractTemplateBuilder selectBuilder(
            AbstractTemplateBuilder templateBuilder, PropertySelectionExtradata extradata) {
        PropertySelectionWrapper runtimeSelection = wrapWhenNonStaticKey(templateBuilder);

        if (runtimeSelection != null) {
            if (!extradata.isDynamicKeyParent())
                runtimeSelection.setFullKey(extradata.getStaticFullKey());
            templateBuilder = runtimeSelection;
        } else if (!selectionHandler.isBuilderSelected(templateBuilder, extradata))
            templateBuilder = null;
        return templateBuilder;
    }

    private PropertySelectionExtradata createExtradata(TemplateBuilder current, Object extradata) {
        PropertySelectionExtradata selectionExtradata;
        if (extradata instanceof PropertySelectionExtradata)
            selectionExtradata =
                    new PropertySelectionExtradata((PropertySelectionExtradata) extradata);
        else selectionExtradata = new PropertySelectionExtradata();
        if (current instanceof AbstractTemplateBuilder
                && !selectionExtradata.isDynamicKeyParent()) {
            updateFullStaticKey((AbstractTemplateBuilder) current, selectionExtradata);
        }
        return selectionExtradata;
    }

    private void updateFullStaticKey(
            AbstractTemplateBuilder abstractBuilder,
            PropertySelectionExtradata selectionExtradata) {
        boolean hasValidKey = hasSelectableKey(abstractBuilder);
        if (hasValidKey) {
            boolean dynamicKey = hasDynamicKey(abstractBuilder);
            if (dynamicKey) {
                selectionExtradata.setDynamicKeyParent(true);
            } else {
                String currentFullKey = selectionExtradata.getStaticFullKey();
                String key = abstractBuilder.getKey(null);
                if (currentFullKey != null && key != null)
                    selectionExtradata.setStaticFullKey(currentFullKey.concat(".").concat(key));
                else if (key != null) selectionExtradata.setStaticFullKey(key);
            }
        }
    }
}
