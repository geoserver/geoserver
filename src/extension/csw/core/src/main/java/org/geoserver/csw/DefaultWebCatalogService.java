/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import java.io.File;
import java.util.List;
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
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.csw.response.CSWRecordsResult;
import org.geoserver.csw.store.CatalogStore;
import org.geoserver.csw.store.RepositoryItem;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.opengis.feature.type.AttributeDescriptor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * The default CSW implementation
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DefaultWebCatalogService implements WebCatalogService, ApplicationContextAware {

    private GeoServer gs;

    private CatalogStore store;

    protected ApplicationContext context;

    public DefaultWebCatalogService(GeoServer gs) {
        this.gs = gs;
    }

    public CSWInfo getServiceInfo() {
        return gs.getService(CSWInfo.class);
    }

    @Override
    public CapabilitiesType getCapabilities(GetCapabilitiesType request) throws ServiceException {
        checkStore();
        CapabilitiesType caps =
                new GetCapabilities(getServiceInfo(), this.store, context).run(request);

        // check for decorator extensions
        for (CapabilitiesDecorator decorator :
                GeoServerExtensions.extensions(CapabilitiesDecorator.class)) {
            caps = decorator.decorate(caps, this.store);
        }

        return caps;
    }

    @Override
    public AttributeDescriptor[] describeRecord(DescribeRecordType request)
            throws ServiceException {
        checkStore();

        return new DescribeRecord(getServiceInfo(), store).run(request);
    }

    @Override
    public CSWRecordsResult getRecords(GetRecordsType request) throws ServiceException {
        checkStore();
        List<RecordDescriptor> descriptors =
                GeoServerExtensions.extensions(RecordDescriptor.class, context);
        return new GetRecords(getServiceInfo(), store, descriptors).run(request);
    }

    @Override
    public CSWRecordsResult getRecordById(GetRecordByIdType request) throws ServiceException {
        checkStore();
        List<RecordDescriptor> descriptors =
                GeoServerExtensions.extensions(RecordDescriptor.class, context);
        return new GetRecordById(getServiceInfo(), store, descriptors).run(request);
    }

    @Override
    public CloseableIterator<String> getDomain(GetDomainType request) throws ServiceException {
        checkStore();
        return new GetDomain(getServiceInfo(), this.store).run(request);
    }

    @Override
    public RepositoryItem getRepositoryItem(GetRepositoryItemType request) throws ServiceException {
        checkStore();
        return new GetRepositoryItem(getServiceInfo(), this.store).run(request);
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

    @Override
    public List<File> directDownload(DirectDownloadType request) throws ServiceException {
        checkStore();
        return new DirectDownload(getServiceInfo(), this.store).run(request);
    }

    /** Checks we have a store to use */
    private void checkStore() {
        if (store == null) {
            throw new ServiceException(
                    "Catalog service could not find a CatalogStore implementation registered in the Spring application context",
                    ServiceException.NO_APPLICABLE_CODE);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
        // pick the implementation of CatalogStore that has the higher priority
        List<CatalogStore> storeCandidates =
                GeoServerExtensions.extensions(CatalogStore.class, applicationContext);

        if (storeCandidates != null && storeCandidates.size() > 0) {
            String defaultStore = System.getProperty("DefaultCatalogStore");
            if (defaultStore != null) {
                for (CatalogStore store : storeCandidates) {
                    if (store.getClass().getName().equals(defaultStore)) {
                        this.store = store;
                        break;
                    }
                }
            }

            if (store == null) {
                store = storeCandidates.get(0);
            }
        }
    }
}
