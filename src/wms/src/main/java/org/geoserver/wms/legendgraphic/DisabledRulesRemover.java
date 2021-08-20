/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.styling.Rule;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.opengis.filter.Filter;

/**
 * Removes static rules that are statically off, possibly due to an env variable, function call
 * (e.g. language comparison)
 */
class DisabledRulesRemover extends DuplicatingStyleVisitor {

    @Override
    public void visit(Rule rule) {
        Filter filter = rule.getFilter();
        if (filter != null && isStatic(filter)) {
            if (!filter.evaluate(null)) return;
        }
        super.visit(rule);
    }

    private boolean isStatic(Filter filter) {
        FilterAttributeExtractor extractor = new FilterAttributeExtractor();
        filter.accept(extractor, null);
        return extractor.getAttributeNameSet().isEmpty();
    }
}
