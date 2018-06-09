/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.icons;

import java.util.ArrayList;
import java.util.List;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.opengis.filter.Filter;

/**
 * A simplified model of an SLD rule used for internal manipulation.
 *
 * @author David Winslow, OpenGeo
 */
class MiniRule {
    public final Filter filter;
    public final boolean isElseFilter;
    public final List<PointSymbolizer> symbolizers;

    public MiniRule(Filter filter, boolean isElseFilter, List<PointSymbolizer> symbolizers) {
        this.filter = filter;
        this.isElseFilter = isElseFilter;
        this.symbolizers = symbolizers;
    }

    static List<List<MiniRule>> minify(Style style) {
        List<List<MiniRule>> ftStyles = new ArrayList<List<MiniRule>>();
        for (FeatureTypeStyle ftStyle : style.featureTypeStyles()) {
            List<MiniRule> rules = new ArrayList<MiniRule>();
            for (Rule rule : ftStyle.rules()) {
                List<PointSymbolizer> pointSymbolizers = new ArrayList<PointSymbolizer>();
                for (Symbolizer symbolizer : rule.symbolizers()) {
                    if (symbolizer instanceof PointSymbolizer) {
                        pointSymbolizers.add((PointSymbolizer) symbolizer);
                    }
                }
                if (!pointSymbolizers.isEmpty())
                    rules.add(
                            new MiniRule(rule.getFilter(), rule.isElseFilter(), pointSymbolizers));
            }
            if (!rules.isEmpty()) {
                ftStyles.add(rules);
            }
        }
        return ftStyles;
    }

    static Style makeStyle(StyleFactory factory, List<List<MiniRule>> ftStyles) {
        Style style = factory.createStyle();
        for (List<MiniRule> rules : ftStyles) {
            FeatureTypeStyle ftStyle = factory.createFeatureTypeStyle();
            for (MiniRule miniRule : rules) {
                if (!miniRule.symbolizers.isEmpty()) {
                    Rule realRule = factory.createRule();
                    for (Symbolizer sym : miniRule.symbolizers) {
                        realRule.symbolizers().add(sym);
                    }
                    ftStyle.rules().add(realRule);
                }
            }
            style.featureTypeStyles().add(ftStyle);
        }
        return style;
    }
}
