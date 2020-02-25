/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.opengis.cat.csw20.GetDomainType;
import net.opengis.ows10.DomainType;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.csw.store.CatalogStore;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Runs the GetDomain request
 *
 * @author Alessio Fabiani - GeoSolutions
 */
public class GetDomain {

    CSWInfo csw;

    CatalogStore store;

    Map<Name, Name> attributeTypeMap = new HashMap<Name, Name>();

    NamespaceSupport ns = new NamespaceSupport();

    public GetDomain(CSWInfo csw, CatalogStore store) {
        this.csw = csw;
        this.store = store;

        try {
            for (RecordDescriptor rd : store.getRecordDescriptors()) {
                for (Name prop :
                        store.getCapabilities()
                                .getDomainQueriables(rd.getFeatureDescriptor().getName())) {
                    attributeTypeMap.put(prop, rd.getFeatureDescriptor().getName());
                    Enumeration declaredPrefixes = rd.getNamespaceSupport().getDeclaredPrefixes();
                    while (declaredPrefixes.hasMoreElements()) {
                        String prefix = (String) declaredPrefixes.nextElement();
                        String uri = rd.getNamespaceSupport().getURI(prefix);
                        ns.declarePrefix(prefix, uri);
                    }
                }
            }
        } catch (IOException e) {
            throw new ServiceException(
                    e, "Failed to retrieve the domain values", ServiceException.NO_APPLICABLE_CODE);
        }
    }

    /** Returns the requested feature types */
    public CloseableIterator<String> run(GetDomainType request) {
        try {
            List<String> result = new ArrayList<String>();
            if (request.getParameterName() != null && !request.getParameterName().isEmpty()) {
                String parameterName = request.getParameterName();
                if (parameterName.indexOf(".") > 0) {
                    final String operation = parameterName.split("\\.")[0];
                    final String parameter = parameterName.split("\\.")[1];

                    if (store.getCapabilities().getOperationParameters().get(operation) != null) {
                        for (DomainType param :
                                store.getCapabilities().getOperationParameters().get(operation)) {
                            if (param.getName().equalsIgnoreCase(parameter)) {
                                for (Object value : param.getValue()) {
                                    result.add((String) value);
                                }
                            }
                        }
                    }
                }
            }

            if (request.getPropertyName() != null && !request.getPropertyName().isEmpty()) {
                final String propertyName = request.getPropertyName();
                String nameSpace = "";
                String localPart = null;
                if (propertyName.indexOf(":") > 0) {
                    nameSpace = propertyName.split(":")[0];
                    localPart = propertyName.split(":")[1];
                } else {
                    if (propertyName.equalsIgnoreCase("anyText")) {
                        nameSpace = ns.getURI("csw");
                    }
                    localPart = propertyName;
                }

                Name attName = new NameImpl(ns.getURI(nameSpace), localPart);

                Name typeName = attributeTypeMap.get(attName);
                if (typeName != null) {
                    return this.store.getDomain(typeName, attName);
                }
            }

            return new CloseableIteratorAdapter<String>(result.iterator());
        } catch (Exception e) {
            throw new ServiceException(
                    e, "Failed to retrieve the domain values", ServiceException.NO_APPLICABLE_CODE);
        }
    }
}
