/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.translate.renderer;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.style.StyleFactory;
import org.geotools.api.style.Symbolizer;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.function.CategorizeFunction;
import org.geotools.filter.function.RecodeFunction;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;

class ClassificationFunctionEraser extends DuplicatingFilterVisitor {

    public static final StyleFactory STYLE_FACTORY = CommonFactoryFinder.getStyleFactory();
    public static final FilterFactory FILTER_FACTORY_2 = CommonFactoryFinder.getFilterFactory();
    private final SimpleFeature sampleFeature;

    public static Symbolizer erase(Symbolizer source, String propertyName, Object value) {
        SimpleFeature sample = buildSampleFeature(propertyName, value);
        DuplicatingStyleVisitor eraser =
                new DuplicatingStyleVisitor(
                        STYLE_FACTORY, FILTER_FACTORY_2, new ClassificationFunctionEraser(sample));
        source.accept(eraser);
        return (Symbolizer) eraser.getCopy();
    }

    public static SimpleFeature buildSampleFeature(String propertyName, Object value) {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.add(propertyName, value != null ? value.getClass() : Object.class);
        tb.setName("sampleType");
        SimpleFeatureType type = tb.buildFeatureType();

        return SimpleFeatureBuilder.build(type, new Object[] {value}, null);
    }

    public ClassificationFunctionEraser(SimpleFeature sampleFeature) {
        super(FILTER_FACTORY_2);
        this.sampleFeature = sampleFeature;
    }

    @Override
    public Object visit(Function expression, Object extraData) {
        if (expression instanceof RecodeFunction || expression instanceof CategorizeFunction) {
            return ff.literal(expression.evaluate(sampleFeature));
        }
        return super.visit(expression, extraData);
    }
}
