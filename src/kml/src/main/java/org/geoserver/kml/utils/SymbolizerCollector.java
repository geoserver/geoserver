/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.utils;

import java.util.ArrayList;
import java.util.List;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.Rule;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Collects the symbolizers active on the specified simple feature
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SymbolizerCollector extends AbstractStyleVisitor {

    SimpleFeature sf;

    List<Symbolizer> symbolizers = new ArrayList<Symbolizer>();
    List<Symbolizer> elseSymbolizers = new ArrayList<Symbolizer>();

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
        if (symbolizers.size() == 0) {
            return elseSymbolizers;
        } else {
            return symbolizers;
        }
    }
}
