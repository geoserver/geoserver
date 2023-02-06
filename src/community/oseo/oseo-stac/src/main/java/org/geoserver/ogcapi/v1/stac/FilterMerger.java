/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import java.util.ArrayList;
import java.util.List;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

/** Simple support class that can accumulate a list of filters and then merge them in and or or */
class FilterMerger {

    public static FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    List<Filter> filters = new ArrayList<>();

    public void add(Filter filter) {
        filters.add(filter);
    }

    public Filter or() {
        Filter result;
        if (filters.isEmpty()) {
            result = Filter.EXCLUDE;
        } else if (filters.size() == 1) {
            result = filters.get(0);
        } else {
            result = FF.or(new ArrayList<>(filters));
        }
        filters.clear();
        return result;
    }

    public Filter and() {
        Filter result;
        if (filters.isEmpty()) {
            result = Filter.INCLUDE;
        } else if (filters.size() == 1) {
            result = filters.get(0);
        } else {
            result = FF.and(new ArrayList<>(filters));
        }
        filters.clear();
        return result;
    }
}
