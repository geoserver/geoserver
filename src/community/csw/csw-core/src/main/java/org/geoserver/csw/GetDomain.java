/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.opengis.cat.csw20.GetDomainType;
import net.opengis.ows10.DomainType;

import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.csw.store.CatalogStore;
import org.geoserver.platform.ServiceException;

/**
 * Runs the GetDomain request
 * 
 * @author Alessio Fabiani - GeoSolutions
 */
public class GetDomain {

    CSWInfo csw;

    CatalogStore store;

    public GetDomain(CSWInfo csw, CatalogStore store) {
        this.csw = csw;
        this.store = store;
    }

    /**
     * Returns the requested feature types
     * 
     * @param request
     * @return
     */
    public CloseableIterator<String> run(GetDomainType request) {
        try {
            List<String> result = new ArrayList<String>();
            if (request.getParameterName() != null && !request.getParameterName().isEmpty()) {
                String parameterName = request.getParameterName();
                if (parameterName.indexOf(".") > 0)
                {
                    final String operation = parameterName.split("\\.")[0];
                    final String parameter = parameterName.split("\\.")[1];
                    
                    if (store.getCapabilities().operationParameters.get(operation) != null)
                    {
                        for (DomainType param : store.getCapabilities().operationParameters.get(operation))
                        {
                            if (param.getName().equalsIgnoreCase(parameter))
                            {
                                for (Object value : param.getValue())
                                {
                                    result.add((String) value);
                                }
                            }
                        }
                    }
                }
            }

            if (request.getPropertyName() != null && !request.getPropertyName().isEmpty()) {
                
            }

            return new CloseableIteratorAdapter<String>(result.iterator());
        } catch (Exception e) {
            throw new ServiceException(e, "Failed to retrieve the domain values",
                    ServiceException.NO_APPLICABLE_CODE);
        }
    }
}
