/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import net.opengis.cat.csw20.CapabilitiesType;
import net.opengis.cat.csw20.DescribeRecordType;
import net.opengis.cat.csw20.GetCapabilitiesType;
import net.opengis.cat.csw20.GetDomainType;
import net.opengis.cat.csw20.GetRecordByIdType;
import net.opengis.cat.csw20.GetRecordsType;
import net.opengis.cat.csw20.HarvestResponseType;
import net.opengis.cat.csw20.HarvestType;
import net.opengis.cat.csw20.TransactionType;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.type.FeatureType;

/**
 * The default CSW implementation
 * @author Andrea Aime - GeoSolutions
 *
 */
public class DefaultCatalogService implements CatalogService {
    
    private GeoServer gs;

    public DefaultCatalogService(GeoServer gs) {
        this.gs = gs;
    }

    @Override
    public CapabilitiesType getCapabilities(GetCapabilitiesType request) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FeatureType[] describeRecord(DescribeRecordType request) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FeatureCollection getRecords(GetRecordsType request) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FeatureCollection getRecordById(GetRecordByIdType request) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<String> getDomain(GetDomainType request) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RepositoryItem getRepositoryItem(GetRepositoryItem request) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HarvestResponseType harvest(HarvestType request) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HarvestResponseType transaction(TransactionType request) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

}
