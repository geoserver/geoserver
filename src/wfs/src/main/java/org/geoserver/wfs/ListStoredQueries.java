/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import javax.xml.namespace.QName;
import net.opengis.wfs20.ListStoredQueriesResponseType;
import net.opengis.wfs20.ListStoredQueriesType;
import net.opengis.wfs20.StoredQueryListItemType;
import net.opengis.wfs20.TitleType;
import net.opengis.wfs20.Wfs20Factory;

/**
 * Web Feature Service ListStoredQueries operation.
 *
 * @author Justin Deoliveira, OpenGeo
 * @version $Id$
 */
public class ListStoredQueries {

    /** service config */
    WFSInfo wfs;

    /** stored query provider */
    StoredQueryProvider storedQueryProvider;

    public ListStoredQueries(WFSInfo wfs, StoredQueryProvider storedQueryProvider) {
        this.wfs = wfs;
        this.storedQueryProvider = storedQueryProvider;
    }

    public ListStoredQueriesResponseType run(ListStoredQueriesType request) throws WFSException {

        Wfs20Factory factory = Wfs20Factory.eINSTANCE;
        ListStoredQueriesResponseType response = factory.createListStoredQueriesResponseType();

        for (StoredQuery sq : storedQueryProvider.listStoredQueries()) {
            StoredQueryListItemType item = factory.createStoredQueryListItemType();
            item.setId(sq.getName());

            TitleType title = factory.createTitleType();
            title.setValue(sq.getTitle());
            item.getTitle().add(title);

            if (!sq.getFeatureTypes().isEmpty()) {
                item.getReturnFeatureType().addAll(sq.getFeatureTypes());
            } else {
                item.getReturnFeatureType().add(new QName(""));
            }

            response.getStoredQuery().add(item);
        }

        return response;
    }
}
