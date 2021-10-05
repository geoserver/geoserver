/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs.impl.DeleteElementTypeImpl;
import net.opengis.wfs20.impl.DeleteTypeImpl;
import org.eclipse.emf.ecore.EObject;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.xsd.EMFUtils;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

/**
 * Delete element in a Transaction request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class Delete extends TransactionElement {

    protected Delete(EObject adaptee) {
        super(adaptee);
    }

    public abstract List<Filter> getFilters();

    public abstract void deleteFilter();

    public abstract void addFilter(List<Filter> features);

    public static class WFS11 extends Delete {
        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public List<Filter> getFilters() {
            Filter filter = eGet(adaptee, "filter", Filter.class);
            return Arrays.asList(filter);
        }

        @Override
        public void deleteFilter() {
            Filter filter = (Filter) EMFUtils.get(adaptee, "filter");
            eSet(adaptee, "filter", filter);
        }

        @Override
        public void addFilter(List<Filter> features) {
            eAddForDelete(adaptee, "filter", features);
        }

        public static DeleteElementType unadapt(Delete delete) {
            DeleteElementType de = WfsFactory.eINSTANCE.createDeleteElementType();
            de.setHandle(delete.getHandle());
            de.setTypeName(delete.getTypeName());
            de.setFilter(delete.getFilter());
            return de;
        }
    }

    public static class WFS20 extends Delete {
        public WFS20(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public List<Filter> getFilters() {
            Filter filter = eGet(adaptee, "filter", Filter.class);
            return Arrays.asList(filter);
        }

        @Override
        public void deleteFilter() {
            Filter filter = (Filter) EMFUtils.get(adaptee, "filter");
            eSet(adaptee, "filter", filter);
        }

        @Override
        public void addFilter(List<Filter> features) {
            eAddForDelete(adaptee, "filter", features);
        }
    }

    protected void eAddForDelete(EObject obj, String property, List<Filter> features) {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory2();
        Filter filter = (Filter) EMFUtils.get(obj, property);

        List<Filter> collection = new ArrayList<>(Collections.singletonList(filter));
        for (Filter element : features) {
            collection.add(element);
        }

        if (obj instanceof DeleteElementTypeImpl) {
            ((DeleteElementTypeImpl) obj).setFilter(ff.or(collection));
        } else if (obj instanceof DeleteTypeImpl) {
            ((DeleteTypeImpl) obj).setFilter(ff.or(collection));
        }
    }
}
