/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import org.geoserver.ows.NestedKvpParser;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

/**
 * Parses kvp of the form 'sortBy=Field1 {A|D},Field2 {A|D}...' into a list of {@link
 * org.opengis.filter.sort.SortBy} (WFS style syntax, as opposed to the CSW one, which is slightly
 * different)
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class SortByKvpParser extends NestedKvpParser {
    FilterFactory filterFactory;

    public SortByKvpParser(FilterFactory filterFactory) {
        super("sortBy", SortBy.class);
        this.filterFactory = filterFactory;
    }

    /** Parses a token of the form 'Field1 {A|D}' into an instnace of {@link SortBy}. */
    protected Object parseToken(String token) throws Exception {
        String[] nameOrder = token.trim().split(" ");
        String propertyName = nameOrder[0];

        SortOrder order = SortOrder.ASCENDING;

        if (nameOrder.length > 1) {
            if ("D".equalsIgnoreCase(nameOrder[1]) || "DESC".equalsIgnoreCase(nameOrder[1])) {
                order = SortOrder.DESCENDING;
            }
        }

        return filterFactory.sort(propertyName, order);
    }
}
