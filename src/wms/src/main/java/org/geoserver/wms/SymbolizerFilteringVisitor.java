/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.ArrayList;
import java.util.List;
import org.geotools.styling.Graphic;
import org.geotools.styling.Rule;
import org.geotools.styling.StyleFactoryImpl;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.opengis.filter.Filter;
import org.opengis.style.Description;

/**
 * Base class for style visitors that copies styles but removes certain symbolizers
 *
 * @author Andrea Aime - OpenGeo
 */
public abstract class SymbolizerFilteringVisitor extends DuplicatingStyleVisitor {

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
            if (symcopy != null) symArray.add(symcopy);
        }
        Symbolizer[] symsCopy = (Symbolizer[]) symArray.toArray(new Symbolizer[symArray.size()]);

        Graphic legendCopy = copy((Graphic) rule.getLegend());
        Description descCopy = rule.getDescription();
        descCopy = copy(descCopy);

        copy =
                new StyleFactoryImpl()
                        .createRule(
                                symsCopy,
                                descCopy,
                                legendCopy,
                                rule.getName(),
                                filterCopy,
                                rule.isElseFilter(),
                                rule.getMaxScaleDenominator(),
                                rule.getMinScaleDenominator());

        if (STRICT && !copy.equals(rule)) {
            throw new IllegalStateException("Was unable to duplicate provided Rule:" + rule);
        }
        pages.push(copy);
    }
}
