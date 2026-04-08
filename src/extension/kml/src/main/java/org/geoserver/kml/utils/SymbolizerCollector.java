/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.utils;

import java.util.ArrayList;
import java.util.List;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Symbolizer;
import org.geotools.styling.AbstractStyleVisitor;

/**
 * Collects the symbolizers active on the specified simple feature
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SymbolizerCollector extends AbstractStyleVisitor {

    SimpleFeature sf;

    List<Symbolizer> symbolizers = new ArrayList<>();
    List<Symbolizer> elseSymbolizers = new ArrayList<>();

    public SymbolizerCollector(SimpleFeature sf) {
        this.sf = sf;
    }

    @Override
    public void visit(Rule rule) {
        if (rule.isElseFilter()) {
            elseSymbolizers.addAll(rule.symbolizers());
        } else if (rule.getFilter() == null || rule.getFilter().evaluate(sf)) {
            symbolizers.addAll(rule.symbolizers());
        }
    }

    public List<Symbolizer> getSymbolizers() {
        // the else filters are activated only if the regular rules are not catching the style
        if (symbolizers.isEmpty()) {
            return elseSymbolizers;
        } else {
            return symbolizers;
        }
    }
}
