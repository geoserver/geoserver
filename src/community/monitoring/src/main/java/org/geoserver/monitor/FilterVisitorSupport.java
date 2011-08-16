/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor;


/**
 * Base class for filter visitors.
 * <p>
 * Subclasses should override the {@link #handleFilter(Filter)} and 
 * {@link #handleComposite(CompositeFilter, String)} methods.
 * </p>
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class FilterVisitorSupport implements FilterVisitor {

    public void visit(Filter f) {
        if (f instanceof And) {
            handleComposite((And)f, "AND");
        }
        else if (f instanceof Or) {
            handleComposite((Or)f, "OR");
        }
        else {
            handleFilter(f);
        }
    }
    
    protected void handleComposite(CompositeFilter f, String type) {
        for (Filter fil : f.getFilters()) {
            fil.accept(this);
        }
    }
    
    protected void handleFilter(Filter f) {
    }
}
