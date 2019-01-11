/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.icons;

import java.util.ArrayList;
import java.util.List;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Graphic;
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
public class MiniRule {
    public final Filter filter;
    public final boolean isElseFilter;
    public final List<Symbolizer> symbolizers;
    private String name;

    public MiniRule(Filter filter, boolean isElseFilter, List<Symbolizer> symbolizers) {
        this.filter = filter;
        this.isElseFilter = isElseFilter;
        this.symbolizers = symbolizers;
    }

    public static List<List<MiniRule>> minify(Style style) {
        return minify(style, false);
    }

    public static List<List<MiniRule>> minify(Style style, boolean includeNonPointGraphics) {
        List<List<MiniRule>> ftStyles = new ArrayList<>();
        for (FeatureTypeStyle ftStyle : style.featureTypeStyles()) {
            List<MiniRule> rules = new ArrayList<>();
            for (Rule rule : ftStyle.rules()) {
                List<Symbolizer> graphicSymbolizers = new ArrayList<>();
                for (Symbolizer symbolizer : rule.symbolizers()) {
                    Graphic graphic =
                            IconPropertyExtractor.getGraphic(symbolizer, includeNonPointGraphics);
                    if (graphic != null) {
                        graphicSymbolizers.add(symbolizer);
                    }
                }
                if (!graphicSymbolizers.isEmpty()) {
                    MiniRule miniRule =
                            new MiniRule(rule.getFilter(), rule.isElseFilter(), graphicSymbolizers);
                    miniRule.setName(rule.getName());
                    rules.add(miniRule);
                }
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

    /** @return the name */
    public String getName() {
        return name;
    }

    /** @param name the name to set */
    public void setName(String name) {
        this.name = name;
    }
}
