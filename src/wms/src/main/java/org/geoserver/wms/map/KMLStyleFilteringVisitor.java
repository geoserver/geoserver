/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.util.ArrayList;
import java.util.List;

import org.geotools.styling.Graphic;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.StyleFactoryImpl;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.opengis.filter.Filter;
import org.opengis.style.Description;

/**
 * A style visitor that copies styles but removes all point and text symbolizers
 * 
 * @author Andrea Aime - OpenGeo
 */
public class KMLStyleFilteringVisitor extends DuplicatingStyleVisitor {

    public void visit(PointSymbolizer ps) {
        pages.push(null);
    }

    public void visit(org.geotools.styling.TextSymbolizer ts) {
        pages.push(null);
    }

    public void visit(Rule rule) {
        Rule copy = null;

        Filter filterCopy = null;

        if (rule.getFilter() != null) {
            Filter filter = rule.getFilter();
            filterCopy = copy(filter);
        }

        // modified to deal with null copies that should be skipped
        List<Symbolizer> symArray = new ArrayList<Symbolizer>();
        for (Symbolizer sym : rule.symbolizers()) {
            Symbolizer symcopy = copy(sym);
            if (symcopy != null)
                symArray.add(symcopy);
        }
        Symbolizer[] symsCopy = (Symbolizer[]) symArray.toArray(new Symbolizer[symArray.size()]);

        Graphic[] legendCopy = rule.getLegendGraphic();
        for (int i = 0; i < legendCopy.length; i++) {
            legendCopy[i] = copy(legendCopy[i]);
        }

        Description descCopy = rule.getDescription();
        descCopy = copy(descCopy);

        copy = new StyleFactoryImpl().createRule(symsCopy, descCopy, legendCopy, rule.getName(),
                filterCopy, rule.isElseFilter(), rule.getMaxScaleDenominator(), rule
                        .getMinScaleDenominator());

        if (STRICT && !copy.equals(rule)) {
            throw new IllegalStateException("Was unable to duplicate provided Rule:" + rule);
        }
        pages.push(copy);
    }

}
