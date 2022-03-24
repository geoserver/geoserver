/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.visitors;

import java.util.List;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.SourceBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicIncludeFlatBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicMergeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.IteratingBuilder;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;

/** Visit a TemplateBuilder tree and make a shallow copy of each TemplateBuilder. */
public class DuplicatingTemplateVisitor implements TemplateVisitor {

    @Override
    public Object visit(RootBuilder rootBuilder, Object extradata) {
        RootBuilder copy = new RootBuilder();
        copy.setVendorOptions(rootBuilder.getVendorOptions());
        copy.setWatchers(rootBuilder.getWatchers());
        addChildren(copy, extradata, rootBuilder.getChildren());
        return copy;
    }

    protected void addChildren(
            TemplateBuilder copy, Object extraData, List<TemplateBuilder> children) {
        for (TemplateBuilder child : children) {
            Object o = child.accept(this, extraData);
            if (o != null) copy.addChild((TemplateBuilder) o);
        }
    }

    @Override
    public Object visit(IteratingBuilder iteratingBuilder, Object extradata) {
        IteratingBuilder copy = iteratingBuilder.copy(false);
        addChildren(copy, extradata, iteratingBuilder.getChildren());
        return copy;
    }

    @Override
    public Object visit(CompositeBuilder compositeBuilder, Object extradata) {
        CompositeBuilder copy = compositeBuilder.copy(false);
        addChildren(copy, extradata, compositeBuilder.getChildren());
        return copy;
    }

    @Override
    public Object visit(DynamicValueBuilder dynamicBuilder, Object extradata) {
        Object result;
        if (dynamicBuilder instanceof DynamicIncludeFlatBuilder)
            result = visit((DynamicIncludeFlatBuilder) dynamicBuilder, extradata);
        else if (dynamicBuilder instanceof DynamicMergeBuilder)
            result = visit((DynamicMergeBuilder) dynamicBuilder, extradata);
        else {
            DynamicValueBuilder copy = dynamicBuilder.copy(false);
            addChildren(copy, extradata, dynamicBuilder.getChildren());
            result = copy;
        }
        return result;
    }

    @Override
    public Object visit(StaticBuilder staticBuilder, Object extradata) {
        StaticBuilder copy = staticBuilder.copy(false);
        addChildren(copy, extradata, staticBuilder.getChildren());
        return copy;
    }

    @Override
    public Object visit(SourceBuilder sourceBuilder, Object extradata) {
        TemplateBuilder copy = sourceBuilder.copy(false);
        addChildren(copy, extradata, sourceBuilder.getChildren());
        return copy;
    }

    @Override
    public Object visit(AbstractTemplateBuilder abstractTemplateBuilder, Object extradata) {
        AbstractTemplateBuilder copy = abstractTemplateBuilder.copy(false);
        addChildren(copy, extradata, abstractTemplateBuilder.getChildren());
        return copy;
    }

    /**
     * Visit a DynamicMergeBuilder.
     *
     * @param merge the DynamicMergeBuilder.
     * @param extradata extradata.
     * @return the shallow copy of the DynamicMergeBuilder.
     */
    public Object visit(DynamicMergeBuilder merge, Object extradata) {
        DynamicMergeBuilder copy = new DynamicMergeBuilder(merge, false);
        addChildren(copy, extradata, merge.getChildren());
        return copy;
    }

    /**
     * Visit a DynamicIncludeFlatBuilder.
     *
     * @param includeFlat the DynamicIncludeFlatBuilder.
     * @param extradata extradata.
     * @return the shallow copy of the DynamicIncludeFlatBuilder.
     */
    public Object visit(DynamicIncludeFlatBuilder includeFlat, Object extradata) {
        DynamicIncludeFlatBuilder copy = new DynamicIncludeFlatBuilder(includeFlat, false);
        addChildren(copy, extradata, includeFlat.getChildren());
        return copy;
    }
}
