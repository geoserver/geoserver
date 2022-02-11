/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2021, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.ogcapi.stac;

import java.util.function.BiConsumer;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicIncludeFlatBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;

/**
 * Class visiting a feature template and performing some action on the properties mapped via dynamic
 * builders
 */
class TemplatePropertyVisitor {

    private final TemplateBuilder templateBuilder;
    private final BiConsumer<String, DynamicValueBuilder> propertyConsumer;

    TemplatePropertyVisitor(
            TemplateBuilder templateBuilder,
            BiConsumer<String, DynamicValueBuilder> propertyConsumer) {
        this.templateBuilder = templateBuilder;
        this.propertyConsumer = propertyConsumer;
    }

    public void visit() {
        this.visitTemplateBuilder(null, templateBuilder, true);
    }

    private void visitTemplateBuilder(String parentPath, TemplateBuilder atb, boolean skipPath) {
        // no queryables out of static builders for the moment, we migth want
        // to revisit once we consider eventual filters
        if (atb instanceof StaticBuilder || !(atb instanceof AbstractTemplateBuilder)) return;

        // dynamic include flat builders eat their parent node to perform dynamic merge,
        // get it out and visit its children directly, without further checks (the returned builder
        // is a key-less composite)
        if (atb instanceof DynamicIncludeFlatBuilder) {
            DynamicIncludeFlatBuilder dyn = (DynamicIncludeFlatBuilder) atb;
            TemplateBuilder builder = dyn.getIncludingNodeBuilder(parentPath);
            if (builder instanceof CompositeBuilder) {
                for (TemplateBuilder child : builder.getChildren()) {
                    visitTemplateBuilder(parentPath, child, false);
                }
            }
            return;
        }

        // check the key, if we get a null it means the key is dynamic and it's not possible
        // to do anything with this JSON sub-tree
        String key = ((AbstractTemplateBuilder) atb).getKey(null);
        if (key == null) return;

        String path = getPath(parentPath, key, skipPath);
        if (atb instanceof DynamicValueBuilder) {
            DynamicValueBuilder db = (DynamicValueBuilder) atb;
            propertyConsumer.accept(path, db);
        } else {
            for (TemplateBuilder child : atb.getChildren()) {
                visitTemplateBuilder(path, child, false);
            }
        }
    }

    private String getPath(String parentPath, String key, boolean skipPath) {
        if (skipPath) return null;
        return parentPath == null ? key : parentPath + "." + key;
    }
}
