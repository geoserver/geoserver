/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import net.opengis.wfs.DescribeFeatureTypeType;
import net.opengis.wfs.GetCapabilitiesType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.GetFeatureWithLockType;
import net.opengis.wfs.GetGmlObjectType;
import net.opengis.wfs.LockFeatureResponseType;
import net.opengis.wfs.LockFeatureType;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;
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

/**
 * Web Feature Service implementation.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class DefaultWebFeatureService implements WebFeatureService, ApplicationContextAware {
    /** GeoServer configuration */
    protected GeoServer geoServer;
    /** The catalog */
    protected Catalog catalog;

    /** Filter factory */
    protected FilterFactory2 filterFactory;

    /**
     * The spring application context, used to look up transaction listeners, plugins and element
     * handlers
     */
    protected ApplicationContext context;

    public DefaultWebFeatureService(GeoServer gs) {
        this.geoServer = gs;
        this.catalog = gs.getCatalog();
    }

    /** Sets the fitler factory. */
    public void setFilterFactory(FilterFactory2 filterFactory) {
        this.filterFactory = filterFactory;
    }

    public WFSInfo getServiceInfo() {
        return geoServer.getService(WFSInfo.class);
    }

    /**
     * WFS GetCapabilities operation.
     *
     * @param request The get capabilities request.
     * @return A transformer instance capable of serializing a wfs capabilities document.
     * @throws WFSException Any service exceptions.
     */
    public TransformerBase getCapabilities(GetCapabilitiesType request) throws WFSException {
        return new GetCapabilities(
                        getServiceInfo(),
                        catalog,
                        WFSExtensions.findExtendedCapabilitiesProviders(context))
                .run(new GetCapabilitiesRequest.WFS11(request));
    }

    /**
     * WFS DescribeFeatureType operation.
     *
     * @param request The describe feature type request.
     * @return A set of feature type metadata objects.
     * @throws WFSException Any service exceptions.
     */
    public FeatureTypeInfo[] describeFeatureType(DescribeFeatureTypeType request)
            throws WFSException {
        return new DescribeFeatureType(getServiceInfo(), catalog)
                .run(new DescribeFeatureTypeRequest.WFS11(request));
    }

    /**
     * WFS GetFeature operation.
     *
     * @param request The get feature request.
     * @return A feature collection type instance.
     * @throws WFSException Any service exceptions.
     */
    public FeatureCollectionResponse getFeature(GetFeatureType request) throws WFSException {
        GetFeature getFeature = new GetFeature(getServiceInfo(), catalog);
        getFeature.setFilterFactory(filterFactory);

        return getFeature.run(new GetFeatureRequest.WFS11(request));
    }

    /**
     * WFS GetFeatureWithLock operation.
     *
     * @param request The get feature with lock request.
     * @return A feature collection type instance.
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
     * @return A lock feature response type.
     * @throws WFSException An service exceptions.
     */
    public LockFeatureResponseType lockFeature(LockFeatureType request) throws WFSException {
        LockFeature lockFeature = new LockFeature(getServiceInfo(), catalog);
        lockFeature.setFilterFactory(filterFactory);

        return (LockFeatureResponseType)
                lockFeature.lockFeature(new LockFeatureRequest.WFS11(request)).getAdaptee();
    }

    /**
     * WFS transaction operation.
     *
     * @param request The transaction request.
     * @return A transaction response instance.
     * @throws WFSException Any service exceptions.
     */
    public TransactionResponseType transaction(TransactionType request) throws WFSException {
        Transaction transaction = new Transaction(getServiceInfo(), catalog, context);
        transaction.setFilterFactory(filterFactory);

        return (TransactionResponseType)
                transaction.transaction(new TransactionRequest.WFS11(request)).getAdaptee();
    }

    /**
     * WFS GetGmlObject operation.
     *
     * @param request The GetGmlObject request.
     * @return The gml object request.
     * @throws WFSException Any service exceptions.
     */
    public Object getGmlObject(GetGmlObjectType request) throws WFSException {

        GetGmlObject getGmlObject = new GetGmlObject(getServiceInfo(), catalog);
        getGmlObject.setFilterFactory(filterFactory);

        return getGmlObject.run(request);
    }

    // the following operations are not part of the spec
    public void releaseLock(String lockId) throws WFSException {
        new LockFeature(getServiceInfo(), catalog).release(lockId);
    }

    public void releaseAllLocks() throws WFSException {
        new LockFeature(getServiceInfo(), catalog).releaseAll();
    }

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }
}
