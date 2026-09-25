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
        if (wfs.isDisableStoredQueriesManagement()) {
            throw new WFSException("Stored queries management is disabled");
        }
        if (request.getId() == null) {
            throw new WFSException(request, "No stored query id specified");
        }

        if (!dropStoredQuery(request.getId())) {
            WFSException exception = new WFSException(
                    request,
                    "Stored query %s does not exist.".formatted(request.getId()),
                    ServiceException.INVALID_PARAMETER_VALUE);
            // CITE tests vagary, the XML uses "id" and KVP uses "STOREDQUERY_ID", the CITE tests
            // mandate "id" in all bindings
            exception.setLocator("id");
            throw exception;
        }

        Wfs20Factory factory = Wfs20Factory.eINSTANCE;
        ExecutionStatusType response = factory.createExecutionStatusType();
        response.setStatus("OK");
        return response;
    }

    /**
     * Removes the stored query identified by {@code name}, falling back to removing the definition by name if it can no
     * longer be parsed.
     *
     * @return {@code true} if a stored query existed and was removed, {@code false} if none existed with that name.
     */
    private boolean dropStoredQuery(String name) {
        try {
            StoredQuery query = storedQueryProvider.getStoredQuery(name);
            if (query == null) {
                return false;
            }
            storedQueryProvider.removeStoredQuery(query);
            return true;
        } catch (RuntimeException unparseable) {
            return storedQueryProvider.removeStoredQueryByName(name);
        }
    }
}
