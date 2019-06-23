/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;
import net.opengis.wfs.QueryType;
import net.opengis.wfs.XlinkPropertyNameType;
import org.eclipse.emf.ecore.EObject;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * Query of a GetFeature/LockFeature request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class Query extends RequestObject {

    public static Query adapt(Object query) {
        if (query instanceof QueryType) {
            return new WFS11((EObject) query);
        } else if (query instanceof net.opengis.wfs20.QueryType) {
            return new WFS20((EObject) query);
        }
        return null;
    }

    protected Query(EObject adaptee) {
        super(adaptee);
    }

    public URI getSrsName() {
        return eGet(adaptee, "srsName", URI.class);
    }

    public String getFeatureVersion() {
        return eGet(adaptee, "featureVersion", String.class);
    }

    // public abstract boolean isTypeNamesUnset(List queries);

    public abstract List<QName> getTypeNames();

    public abstract List<String> getAliases();

    public abstract List<String> getPropertyNames();

    public abstract Filter getFilter();

    public abstract List<SortBy> getSortBy();

    public abstract List<XlinkPropertyNameType> getXlinkPropertyNames();

    public static class WFS11 extends Query {

        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public List<QName> getTypeNames() {
            return eGet(adaptee, "typeName", List.class);
        }

        @Override
        public List<String> getAliases() {
            return new ArrayList();
        }

        @Override
        public List<String> getPropertyNames() {
            return eGet(adaptee, "propertyName", List.class);
        }

        @Override
        public Filter getFilter() {
            return eGet(adaptee, "filter", Filter.class);
        }

        @Override
        public List<SortBy> getSortBy() {
            return eGet(adaptee, "sortBy", List.class);
        }

        @Override
        public List<XlinkPropertyNameType> getXlinkPropertyNames() {
            return eGet(adaptee, "xlinkPropertyName", List.class);
        }
    }

    public static class WFS20 extends Query {

        public WFS20(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public List<QName> getTypeNames() {
            return eGet(adaptee, "typeNames", List.class);
        }

        public void setTypeNames(List<QName> typeNames) {
            List l = eGet(adaptee, "typeNames", List.class);
            l.clear();
            l.addAll(typeNames);
        }

        @Override
        public List<String> getAliases() {
            return eGet(adaptee, "aliases", List.class);
        }

        @Override
        public List<String> getPropertyNames() {
            // WFS 2.0 has this as a list of QNAme, drop the qualified part
            List<QName> propertyNames = eGet(adaptee, "abstractProjectionClause", List.class);
            List<String> l = new ArrayList();
            for (QName name : propertyNames) {
                l.add(name.getLocalPart());
            }
            return l;
        }

        @Override
        public Filter getFilter() {
            return eGet(adaptee, "abstractSelectionClause", Filter.class);
        }

        @Override
        public List<SortBy> getSortBy() {
            return eGet(adaptee, "abstractSortingClause", List.class);
        }

        @Override
        public List<XlinkPropertyNameType> getXlinkPropertyNames() {
            // no equivalent in wfs 2.0
            return Collections.EMPTY_LIST;
        }
    }
}
