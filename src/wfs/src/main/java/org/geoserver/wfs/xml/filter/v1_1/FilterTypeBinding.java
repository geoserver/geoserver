/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.filter.v1_1;

import java.util.HashSet;
import java.util.Iterator;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Id;
import org.opengis.filter.identity.Identifier;

public class FilterTypeBinding extends org.geotools.filter.v1_1.FilterTypeBinding {
    public FilterTypeBinding(FilterFactory filterFactory) {
        super(filterFactory);
    }

    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        Filter filter = (Filter) super.parse(instance, node, value);

        // some checks, these should perhaps be made part of the Filter binding
        if (filter instanceof Id) {
            Id idFilter = (Id) filter;

            if (idFilter.getIdentifiers().size() > 1) {
                // there should only be one type of id specified
                HashSet types = new HashSet();

                for (Iterator i = idFilter.getIdentifiers().iterator(); i.hasNext(); ) {
                    Identifier id = (Identifier) i.next();
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
