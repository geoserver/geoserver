/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.util.Arrays;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Manages OGC API sort parameter. Current definition is a comma separate list, with + or - in front
 * of the attribute name to specify the direction. In case of no direction specifier, + is the
 * default.
 */
@Component
public class SortByConverter implements Converter<String, SortBy[]> {

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    @Override
    public SortBy[] convert(String spec) {
        return Arrays.stream(spec.split("\\s*,\\s*"))
                .map(s -> sortBy(s))
                .toArray(n -> new SortBy[n]);
    }

    private SortBy sortBy(String spec) {
        SortOrder order = SortOrder.ASCENDING;
        if (spec.startsWith("+")) {
            spec = spec.substring(1);
        } else if (spec.startsWith("-")) {
            spec = spec.substring(1);
            order = SortOrder.DESCENDING;
        }
        return FF.sort(spec, order);
    }
}
