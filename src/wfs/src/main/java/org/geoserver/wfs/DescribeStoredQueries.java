/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.net.URI;
import net.opengis.wfs20.DescribeStoredQueriesResponseType;
import net.opengis.wfs20.DescribeStoredQueriesType;
import net.opengis.wfs20.Wfs20Factory;
import org.geoserver.platform.ServiceException;

/**
 * Web Feature Service DescribeStoredQueries operation.
 *
 * @author Justin Deoliveira, OpenGeo
 * @version $Id$
 */
public class DescribeStoredQueries {

    /** service config */
    WFSInfo wfs;

    /** stored query provider */
    StoredQueryProvider storedQueryProvider;

    public DescribeStoredQueries(WFSInfo wfs, StoredQueryProvider storedQueryProvider) {
        this.wfs = wfs;
        this.storedQueryProvider = storedQueryProvider;
    }

    public DescribeStoredQueriesResponseType run(DescribeStoredQueriesType request)
            throws WFSException {

        Wfs20Factory factory = Wfs20Factory.eINSTANCE;
        DescribeStoredQueriesResponseType response =
                factory.createDescribeStoredQueriesResponseType();

        if (request.getStoredQueryId().isEmpty()) {
            for (StoredQuery query : storedQueryProvider.listStoredQueries()) {
                describeStoredQuery(query, response);
            }
        } else {
            for (URI id : request.getStoredQueryId()) {
                StoredQuery query = storedQueryProvider.getStoredQuery(id.toString());
                if (query == null) {
                    WFSException exception =
                            new WFSException(
                                    request,
                                    "No such stored query: " + id,
                                    ServiceException.INVALID_PARAMETER_VALUE);
                    exception.setLocator("STOREDQUERY_ID");
                    throw exception;
                }

                describeStoredQuery(query, response);
            }
        }

        return response;
    }

    void describeStoredQuery(StoredQuery query, DescribeStoredQueriesResponseType response) {
        response.getStoredQueryDescription().add(query.getQuery());
    }
}
