/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import net.opengis.wfs20.DropStoredQueryType;
import net.opengis.wfs20.ExecutionStatusType;
import net.opengis.wfs20.Wfs20Factory;
import org.geoserver.platform.ServiceException;

/**
 * Web Feature Service DropStoredQuery operation.
 *
 * @author Justin Deoliveira, OpenGeo
 * @version $Id$
 */
public class DropStoredQuery {

    /** service config */
    WFSInfo wfs;

    /** stored query provider */
    StoredQueryProvider storedQueryProvider;

    public DropStoredQuery(WFSInfo wfs, StoredQueryProvider storedQueryProvider) {
        this.wfs = wfs;
        this.storedQueryProvider = storedQueryProvider;
    }

    public ExecutionStatusType run(DropStoredQueryType request) throws WFSException {

        if (request.getId() == null) {
            throw new WFSException(request, "No stored query id specified");
        }

        StoredQuery query = storedQueryProvider.getStoredQuery(request.getId());
        if (query != null) {
            storedQueryProvider.removeStoredQuery(query);
        } else {
            WFSException exception =
                    new WFSException(
                            request,
                            String.format("Stored query %s does not exist.", request.getId()),
                            ServiceException.INVALID_PARAMETER_VALUE);
            // CITE tests vagary, the XML uses "id" and KVP uses "STOREDQUERY_ID", the CITE tests
            // mandate "id"
            // in all bindings
            exception.setLocator("id");
            throw exception;
        }

        Wfs20Factory factory = Wfs20Factory.eINSTANCE;
        ExecutionStatusType response = factory.createExecutionStatusType();
        response.setStatus("OK");
        return response;
    }
}
