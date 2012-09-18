/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.util.ArrayList;
import java.util.List;

import net.opengis.cat.csw20.GetDomainType;
import net.opengis.ows10.DomainType;

import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.store.CatalogStore;
import org.geoserver.platform.ServiceException;
import org.geotools.csw.CSW;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;

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
                    
                    if (GetCapabilities.operationParameters.get(operation) != null)
                    {
                        for (DomainType param : GetCapabilities.operationParameters.get(operation))
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
                final String propertyName = request.getPropertyName();
                String nameSpace = null;
                String localPart = null;
                if (propertyName.indexOf(":") > 0)
                {
                    nameSpace = propertyName.split(":")[0];
                    localPart = propertyName.split(":")[1];
                } 
                else 
                {
                    if (propertyName.equalsIgnoreCase("anyText"))
                    {
                        nameSpace = CSWRecordDescriptor.NAMESPACES.getURI("csw");
                    }
                    localPart = propertyName;
                }
                Name typeName = (nameSpace != null ? new NameImpl(CSWRecordDescriptor.NAMESPACES.getURI(nameSpace), localPart) : new NameImpl(localPart) );

                List<Name> domainQueriables = store.getCapabilities().getDomainQueriables(typeName);
                if (domainQueriables != null && domainQueriables.size() > 0)
                {
                    return this.store.getDomain(new NameImpl(CSW.NAMESPACE, "Record"), typeName);
                }
            }

            return new CloseableIteratorAdapter<String>(result.iterator());
        } catch (Exception e) {
            throw new ServiceException(e, "Failed to retrieve the domain values",
                    ServiceException.NO_APPLICABLE_CODE);
        }
    }
}
