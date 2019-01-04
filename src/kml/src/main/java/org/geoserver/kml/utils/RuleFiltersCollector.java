/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.utils;

import java.util.ArrayList;
import java.util.List;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

/**
 * Collects all the filters in the style and builds a summary filter that matches every feature
 * matched by at least one rule in the style
 *
 * @author Andrea Aime - GeoSolutions
 */
public class RuleFiltersCollector extends AbstractStyleVisitor {

    List<Filter> filters = new ArrayList<Filter>();

    @Override
    public void visit(FeatureTypeStyle fts) {
        for (Rule rule : fts.rules()) {
            if (rule.isElseFilter()) {
                filters.add(Filter.INCLUDE);
            } else {
                Filter filter = rule.getFilter();
                if (filter == null) {
                    filters.add(Filter.INCLUDE);
                } else {
                    filters.add(filter);
                }
            }
        }
    }

    /** Returns a filter that includes all the visited rules */
    Filter getSummaryFilter() {
        if (filters.size() == 0) {
            return Filter.INCLUDE;
        } else if (filters.size() == 1) {
            return filters.get(0);
        } else {
            FilterFactory ff = CommonFactoryFinder.getFilterFactory();
            Filter or = ff.or(filters);
            SimplifyingFilterVisitor simplifier = new SimplifyingFilterVisitor();
            return (Filter) or.accept(simplifier, null);
        }
    }
}
