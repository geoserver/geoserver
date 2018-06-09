/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.kvp;

import java.util.List;
import org.geoserver.ows.FlatKvpParser;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

/**
 * Parses kvp of the form 'sortBy=Field1:{A|D},Field2:{A|D}...' into a list of {@link
 * org.opengis.filter.sort.SortBy}.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class SortByKvpParser extends FlatKvpParser {
    FilterFactory filterFactory;

    public SortByKvpParser(FilterFactory filterFactory) {
        super("sortBy", SortBy.class);
        setService("csw");
        this.filterFactory = filterFactory;
    }

    /** Parses a token of the form 'Field1 {A|D}' into an instnace of {@link SortBy}. */
    protected Object parseToken(String token) throws Exception {
        SortOrder order = SortOrder.ASCENDING;
        int idx = token.lastIndexOf(":");
        if (idx > 0 && idx == token.length() - 2) {
            String ad = token.substring(idx + 1);
            if ("A".equals(ad)) {
                order = SortOrder.ASCENDING;
                token = token.substring(0, idx);
            } else if ("D".equals(ad)) {
                order = SortOrder.DESCENDING;
                token = token.substring(0, idx);
            }
        }

        return filterFactory.sort(token, order);
    }

    @Override
    protected Object parse(List values) throws Exception {
        return (SortBy[]) values.toArray(new SortBy[values.size()]);
    }
}
