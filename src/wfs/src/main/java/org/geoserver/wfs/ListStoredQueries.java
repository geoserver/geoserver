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
import org.geoserver.catalog.Catalog;

/**
 * Web Feature Service ListStoredQueries operation.
 *
 * @author Justin Deoliveira, OpenGeo
 * @version $Id$
 */
public class ListStoredQueries {

    private Catalog catalog;

    /** stored query provider */
    StoredQueryProvider storedQueryProvider;

    public ListStoredQueries(Catalog catalog, StoredQueryProvider storedQueryProvider) {
        this.storedQueryProvider = storedQueryProvider;
        this.catalog = catalog;
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
                // WFS 2.0 mandates the element, but the empty string is not a valid QName
                // WFS 2.0.2 allows the element to be omitted, but the empty string is still schema
                // invalid
                // in order to support both versions for the time being we'll have to list them
                // all...
                catalog.getFeatureTypes()
                        .stream()
                        .map(
                                ft ->
                                        new QName(
                                                ft.getNamespace().getURI(),
                                                ft.getName(),
                                                ft.getNamespace().getPrefix()))
                        .forEach(qn -> item.getReturnFeatureType().add(qn));
            }

            response.getStoredQuery().add(item);
        }

        return response;
    }
}
