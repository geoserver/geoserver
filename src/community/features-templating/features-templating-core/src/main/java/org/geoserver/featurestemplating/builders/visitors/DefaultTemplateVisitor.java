package org.geoserver.featurestemplating.builders.visitors;

import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.SourceBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.IteratingBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;

/**
 * A default implementation of a {@link TemplateVisitor}. It simply traverse a TemplateBuilder tree.
 */
public class DefaultTemplateVisitor implements TemplateVisitor {

    @Override
    public Object visit(RootBuilder rootBuilder, Object extradata) {
        rootBuilder.getChildren().forEach(b -> b.accept(this, extradata));
        return extradata;
    }

    @Override
    public Object visit(IteratingBuilder iteratingBuilder, Object extradata) {
        iteratingBuilder.getChildren().forEach(b -> b.accept(this, extradata));
        return extradata;
    }

    @Override
    public Object visit(CompositeBuilder compositeBuilder, Object extradata) {
        compositeBuilder.getChildren().forEach(b -> b.accept(this, extradata));
        return extradata;
    }

    @Override
    public Object visit(DynamicValueBuilder dynamicBuilder, Object extradata) {
        dynamicBuilder.getChildren().forEach(b -> b.accept(this, extradata));
        return extradata;
    }

    @Override
    public Object visit(StaticBuilder staticBuilder, Object extradata) {
        staticBuilder.getChildren().forEach(b -> b.accept(this, extradata));
        return extradata;
    }

    @Override
    public Object visit(SourceBuilder sourceBuilder, Object extradata) {
        sourceBuilder.getChildren().forEach(b -> b.accept(this, extradata));
        return extradata;
    }

    @Override
    public Object visit(AbstractTemplateBuilder abstractTemplateBuilder, Object extradata) {
        abstractTemplateBuilder.getChildren().forEach(b -> b.accept(this, extradata));
        return extradata;
    }
}
