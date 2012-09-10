/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import net.opengis.cat.csw20.CapabilitiesType;
import net.opengis.cat.csw20.DescribeRecordType;
import net.opengis.cat.csw20.GetCapabilitiesType;
import net.opengis.cat.csw20.GetDomainType;
import net.opengis.cat.csw20.GetRecordByIdType;
import net.opengis.cat.csw20.GetRecordsType;
import net.opengis.cat.csw20.HarvestResponseType;
import net.opengis.cat.csw20.HarvestType;
import net.opengis.cat.csw20.TransactionType;

import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.csw.store.CatalogStore;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.type.FeatureType;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * The default CSW implementation
 * @author Andrea Aime - GeoSolutions
 *
 */
public class DefaultCatalogService implements CatalogService, ApplicationContextAware {
    
    private GeoServer gs;
    private CatalogStore store;

    public DefaultCatalogService(GeoServer gs) {
        this.gs = gs;
    }

    @Override
    public CapabilitiesType getCapabilities(GetCapabilitiesType request) throws ServiceException {
        checkStore();
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FeatureType[] describeRecord(DescribeRecordType request) throws ServiceException {
        checkStore();

        try {
            if (request.getTypeName() == null || request.getTypeName().isEmpty()) {
                // return all the ones we have
                return store.getRecordSchemas();
            } else {
                List<FeatureType> result = new ArrayList<FeatureType>();
                Set<QName> requested = new HashSet(request.getTypeName());
                FeatureType[] schemas = store.getRecordSchemas();
                for (FeatureType featureType : schemas) {
                    // convert the feature type name to a QName and check if it was requested
                    QName typeName = new QName(featureType.getName().getNamespaceURI(), featureType
                            .getName().getLocalPart());
                    if (requested.remove(typeName)) {
                        result.add(featureType);
                    }
                }

                // if we still have some elements it means we don't have some of the requested
                // schemas
                if (requested.size() != 0) {
                    throw new ServiceException("Failed to find feature types: " + requested,
                            ServiceException.INVALID_PARAMETER_VALUE, "typeName");
                }

                return (FeatureType[]) result.toArray(new FeatureType[result.size()]);
            }
        } catch (IOException e) {
            throw new ServiceException("Failed to retrieve the feature type schemas",
                    ServiceException.NO_APPLICABLE_CODE);
        }
    }

    @Override
    public FeatureCollection getRecords(GetRecordsType request) throws ServiceException {
        checkStore();
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FeatureCollection getRecordById(GetRecordByIdType request) throws ServiceException {
        checkStore();
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CloseableIterator<String> getDomain(GetDomainType request) throws ServiceException {
        checkStore();
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RepositoryItem getRepositoryItem(GetRepositoryItem request) throws ServiceException {
        checkStore();
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HarvestResponseType harvest(HarvestType request) throws ServiceException {
        checkStore();
        throw new ServiceException("Harvest operation is not supported by this CSW service");
    }

    @Override
    public HarvestResponseType transaction(TransactionType request) throws ServiceException {
        checkStore();
        throw new ServiceException("Transactions are not supported by this CSW service");
    }
    
    /**
     * Checks we have a store to use
     */
    private void checkStore() {
        if(store == null) {
            throw new ServiceException("Catalog service could not find a CatalogStore implementation registered in the Spring application context", 
                    ServiceException.NO_APPLICABLE_CODE);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // pick the implementation of CatalogStore that has the higher
        store = GeoServerExtensions.bean(CatalogStore.class, applicationContext);
    }

}
