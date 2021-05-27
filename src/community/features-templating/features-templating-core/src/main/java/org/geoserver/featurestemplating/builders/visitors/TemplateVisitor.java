package org.geoserver.featurestemplating.builders.visitors;

import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.SourceBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.IteratingBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;

/**
 * Visitor with {@code visit} methods to be called by {@link
 * org.geoserver.featurestemplating.builders.TemplateBuilder#accept}
 */
public interface TemplateVisitor {

    /**
     * Used to visit a {@link RootBuilder}.
     *
     * @param rootBuilder the root builder.
     * @param extradata
     * @return the eventual result of the visiting process.
     */
    Object visit(RootBuilder rootBuilder, Object extradata);

    /**
     * Used to visit a {@link IteratingBuilder}.
     *
     * @param iteratingBuilder the iterating builder to be visited.
     * @param extradata
     * @return the eventual result of the visiting process.
     */
    Object visit(IteratingBuilder iteratingBuilder, Object extradata);

    /**
     * Used to visit a {@link CompositeBuilder}.
     *
     * @param compositeBuilder the composite builder to be visited.
     * @param extradata
     * @return the eventual result of the visiting process.
     */
    Object visit(CompositeBuilder compositeBuilder, Object extradata);

    /**
     * Used to visit a {@link DynamicValueBuilder}.
     *
     * @param dynamicBuilder the dynamic builder to be visited.
     * @param extradata
     * @return the eventual result of the visiting process.
     */
    Object visit(DynamicValueBuilder dynamicBuilder, Object extradata);

    /**
     * Used to visit a {@link StaticBuilder}.
     *
     * @param staticBuilder the static builder to be visited.
     * @param extradata
     * @return the eventual result of the visiting process.
     */
    Object visit(StaticBuilder staticBuilder, Object extradata);

    /**
     * Used to visit a {@link SourceBuilder}.
     *
     * @param sourceBuilder the source builder to be visited.
     * @param extradata
     * @return the eventual result of the visiting process.
     */
    Object visit(SourceBuilder sourceBuilder, Object extradata);

    /**
     * Used to visit a {@link AbstractTemplateBuilder}
     *
     * @param abstractTemplateBuilder the abstract builder to be visited.
     * @param extradata
     * @return the eventual result of the visiting process.
     */
    Object visit(AbstractTemplateBuilder abstractTemplateBuilder, Object extradata);
}
