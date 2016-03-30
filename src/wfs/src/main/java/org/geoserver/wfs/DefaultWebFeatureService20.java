/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import net.opengis.wfs20.CreateStoredQueryResponseType;
import net.opengis.wfs20.CreateStoredQueryType;
import net.opengis.wfs20.DescribeFeatureTypeType;
import net.opengis.wfs20.DescribeStoredQueriesResponseType;
import net.opengis.wfs20.DescribeStoredQueriesType;
import net.opengis.wfs20.DropStoredQueryType;
import net.opengis.wfs20.ExecutionStatusType;
import net.opengis.wfs20.GetCapabilitiesType;
import net.opengis.wfs20.GetFeatureType;
import net.opengis.wfs20.GetFeatureWithLockType;
import net.opengis.wfs20.GetPropertyValueType;
import net.opengis.wfs20.ListStoredQueriesResponseType;
import net.opengis.wfs20.ListStoredQueriesType;
import net.opengis.wfs20.LockFeatureResponseType;
import net.opengis.wfs20.LockFeatureType;
import net.opengis.wfs20.TransactionResponseType;
import net.opengis.wfs20.TransactionType;
import net.opengis.wfs20.ValueCollectionType;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.request.DescribeFeatureTypeRequest;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetCapabilitiesRequest;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.LockFeatureRequest;
import org.geoserver.wfs.request.TransactionRequest;
import org.geotools.xml.transform.TransformerBase;
import org.opengis.filter.FilterFactory2;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class DefaultWebFeatureService20 implements WebFeatureService20, ApplicationContextAware {

    /**
     * GeoServer configuration
     */
    protected GeoServer geoServer;

    /** filter factory */
    protected FilterFactory2 filterFactory;

    /**
     * The spring application context, used to look up transaction listeners, plugins and
     * element handlers
     */
    protected ApplicationContext context;

    public DefaultWebFeatureService20(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    public void setFilterFactory(FilterFactory2 filterFactory) {
        this.filterFactory = filterFactory;
    }
    
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
    
    public WFSInfo getServiceInfo() {
        return geoServer.getService(WFSInfo.class);
    }
    
    public Catalog getCatalog() {
        return geoServer.getCatalog();
    }
    
    public StoredQueryProvider getStoredQueryProvider() {
        return new StoredQueryProvider(getCatalog());
    }

    public TransformerBase getCapabilities(GetCapabilitiesType request) throws WFSException {
        return new GetCapabilities(getServiceInfo(), getCatalog(), WFSExtensions.findExtendedCapabilitiesProviders(context))
        .run(new GetCapabilitiesRequest.WFS20(request));
    }
    
    public FeatureTypeInfo[] describeFeatureType(DescribeFeatureTypeType request)
            throws WFSException {
        return new DescribeFeatureType(getServiceInfo(), getCatalog())
            .run(new DescribeFeatureTypeRequest.WFS20(request));
    }

    public FeatureCollectionResponse getFeature(GetFeatureType request) throws WFSException {
        GetFeature gf = new GetFeature(getServiceInfo(), getCatalog());
        gf.setFilterFactory(filterFactory);
        gf.setStoredQueryProvider(getStoredQueryProvider());
        
        return gf.run(new GetFeatureRequest.WFS20(request));
    }
    
    public FeatureCollectionResponse getFeatureWithLock(GetFeatureWithLockType request)
            throws WFSException {
        return getFeature(request);
    }

    @Override
    public ValueCollectionType getPropertyValue(GetPropertyValueType request) throws WFSException {
    	return new GetPropertyValue(getServiceInfo(), getCatalog(), filterFactory).run(request);
    }

    public LockFeatureResponseType lockFeature(LockFeatureType request) throws WFSException {
        LockFeature lockFeature = new LockFeature(getServiceInfo(), getCatalog(), filterFactory);
        return (LockFeatureResponseType) 
            lockFeature.lockFeature(new LockFeatureRequest.WFS20(request)).getAdaptee();
    }
    
    public TransactionResponseType transaction(TransactionType request) throws WFSException {
        Transaction tx = new Transaction(getServiceInfo(), getCatalog(), context);
        tx.setFilterFactory(filterFactory);
        
        return (TransactionResponseType) 
            tx.transaction(new TransactionRequest.WFS20(request)).getAdaptee();
    }
    
    public ListStoredQueriesResponseType listStoredQueries(ListStoredQueriesType request) 
        throws WFSException {
        return new ListStoredQueries(getServiceInfo(), getStoredQueryProvider()).run(request);
    }
    
    public DescribeStoredQueriesResponseType describeStoredQueries(DescribeStoredQueriesType request)
            throws WFSException {
        return new DescribeStoredQueries(getServiceInfo(), getStoredQueryProvider()).run(request);
    }
    
    //the following operations are not part of the spec
    public void releaseLock(String lockId) throws WFSException {
        new LockFeature(getServiceInfo(), getCatalog()).release(lockId);
    }
}
