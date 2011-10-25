/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv;

import java.util.ArrayList;
import java.util.List;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetCapabilitiesType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.GetFeatureWithLockType;
import net.opengis.wfs.GetGmlObjectType;
import net.opengis.wfs.LockFeatureResponseType;
import net.opengis.wfs.LockFeatureType;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfsv.DescribeVersionedFeatureTypeType;
import net.opengis.wfsv.GetDiffType;
import net.opengis.wfsv.GetLogType;
import net.opengis.wfsv.GetVersionedFeatureType;
import net.opengis.wfsv.VersionedFeatureCollectionType;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.DescribeFeatureType;
import org.geoserver.wfs.GetCapabilities;
import org.geoserver.wfs.GetFeature;
import org.geoserver.wfs.LockFeature;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.DescribeFeatureTypeRequest;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetCapabilitiesRequest;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.LockFeatureRequest;
import org.geoserver.wfs.request.TransactionRequest;
import org.geotools.data.FeatureDiffReader;
import org.geotools.util.Version;
import org.geotools.xml.transform.TransformerBase;
import org.opengis.filter.FilterFactory2;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;



/**
 * Default implementation of the versioned feature service
 *
 * @author aaime
 */
public class DefaultVersioningWebFeatureService 
    implements VersionedWebFeatureService {
    
    /**
     * WFS service configuration.
     */
    protected WFSInfo wfs;

    /**
     * The catalog
     */
    protected Catalog catalog;

    /**
     * Filter factory
     */
    protected FilterFactory2 filterFactory;

    /**
     * The spring application context, used to look up transaction listeners, plugins and
     * element handlers
     */
    protected ApplicationContext context;
    
    /**
     * list of available versions
     */
    protected List<Version> versions; 

    public DefaultVersioningWebFeatureService(GeoServer gs) {
        this.wfs = gs.getService( WFSInfo.class );
        this.catalog = gs.getCatalog();
        
        versions = new ArrayList();
        versions.add( new Version("1.0.0" ) );
        versions.add( new Version("1.1.0" ) );
    }
    
    public WFSInfo getServiceInfo() {
        return wfs;
    }

    public List<Version> getVersions() {
        return versions;
    }
    
    /**
     * Sets the fitler factory.
     */
    public void setFilterFactory(FilterFactory2 filterFactory) {
        this.filterFactory = filterFactory;
    }

    /**
     * WFS GetCapabilities operation.
     *
     * @param request The get capabilities request.
     *
     * @return A transformer instance capable of serializing a wfs capabilities
     * document.
     *
     * @throws WFSException Any service exceptions.
     */
    public TransformerBase getCapabilities(GetCapabilitiesType request)
        throws WFSException {
        return new GetCapabilities(wfs, catalog).run(GetCapabilitiesRequest.adapt(request));
    }

    /**
     * WFS GetFeature operation.
     *
     * @param request The get feature request.
     *
     * @return A feature collection type instance.
     *
     * @throws WFSException Any service exceptions.
     */
    public FeatureCollectionResponse getFeature(GetFeatureType request)
        throws WFSException {
        GetFeature getFeature = new GetFeature(wfs, catalog);
        getFeature.setFilterFactory(filterFactory);

        return getFeature.run(GetFeatureRequest.adapt(request));
    }

    /**
     * WFS GetFeatureWithLock operation.
     *
     * @param request The get feature with lock request.
     *
      * @return A feature collection type instance.
     *
     * @throws WFSException Any service exceptions.
     */
    public FeatureCollectionResponse getFeatureWithLock(GetFeatureWithLockType request)
        throws WFSException {
        return getFeature(request);
    }

    /**
     * WFS LockFeatureType operation.
     *
     * @param request The lock feature request.
     *
     * @return A lock feture response type.
     *
     * @throws WFSException An service exceptions.
     */
    public LockFeatureResponseType lockFeature(LockFeatureType request)
        throws WFSException {
        LockFeature lockFeature = new LockFeature(wfs, catalog);
        lockFeature.setFilterFactory(filterFactory);

        return (LockFeatureResponseType) 
            lockFeature.lockFeature(LockFeatureRequest.adapt(request)).getAdaptee();
    }

    public Object getGmlObject(GetGmlObjectType request) throws WFSException {
        throw new UnsupportedOperationException();
    }
    
    //the following operations are not part of the spec
    public void releaseLock(String lockId) throws WFSException {
        new LockFeature(wfs, catalog).release(lockId);
    }

    public void releaseAllLocks() throws WFSException {
        new LockFeature(wfs, catalog).releaseAll();
    }

    public void setApplicationContext(ApplicationContext context)
        throws BeansException {
        this.context = context;
    }

    public TransactionResponseType transaction(TransactionType request)
        throws WFSException {
        VersioningTransaction transaction = new VersioningTransaction(wfs, catalog, context);
        transaction.setFilterFactory(filterFactory);

        return (TransactionResponseType) 
            transaction.transaction(TransactionRequest.adapt(request)).getAdaptee();
    }

    public FeatureCollectionType getLog(GetLogType request) {
        GetLog log = new GetLog(wfs, catalog);

        return log.run(request);
    }

    public FeatureDiffReader[] getDiff(GetDiffType request) {
        GetDiff diff = new GetDiff(wfs, catalog);

        return diff.run(request);
    }

    public VersionedFeatureCollectionType getVersionedFeature(
            GetVersionedFeatureType request) {
        VersionedGetFeature getFeature = new VersionedGetFeature(wfs, catalog);
        getFeature.setFilterFactory(filterFactory);

        return (VersionedFeatureCollectionType) 
            getFeature.run(GetFeatureRequest.adapt(request)).getAdaptee();
    }
    
    public FeatureTypeInfo[] describeFeatureType(net.opengis.wfs.DescribeFeatureTypeType request) {
        return new DescribeFeatureType(wfs, catalog).run(DescribeFeatureTypeRequest.adapt(request));
    }
    
    public VersionedDescribeResults describeVersionedFeatureType(DescribeVersionedFeatureTypeType request) {
        FeatureTypeInfo[] infos = 
            new DescribeFeatureType(wfs, catalog).run(DescribeFeatureTypeRequest.adapt(request));
        return new VersionedDescribeResults(infos, request.isVersioned());
    }
}
