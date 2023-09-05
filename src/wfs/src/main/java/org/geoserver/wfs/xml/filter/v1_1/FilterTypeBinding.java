/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.filter.v1_1;

import java.util.HashSet;
import java.util.Set;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.identity.Identifier;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

public class FilterTypeBinding extends org.geotools.filter.v1_1.FilterTypeBinding {
    public FilterTypeBinding(FilterFactory filterFactory) {
        super(filterFactory);
    }

    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        Filter filter = (Filter) super.parse(instance, node, value);

        // some checks, these should perhaps be made part of the Filter binding
        if (filter instanceof Id) {
            Id idFilter = (Id) filter;

            if (idFilter.getIdentifiers().size() > 1) {
                // there should only be one type of id specified
                Set<Class<?>> types = new HashSet<>();

                for (Identifier id : idFilter.getIdentifiers()) {
                    types.add(id.getClass());
                }

                if (types.size() != 1) {
                    throw new Exception("Only one type of Id can be supplied in a single filter");
                }
            }
        }

        return filter;
    }
}
