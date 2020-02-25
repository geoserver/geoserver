/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.ArrayList;
import java.util.List;
import net.opengis.fes20.AbstractQueryExpressionType;
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
import net.opengis.wfs20.QueryType;
import net.opengis.wfs20.TransactionResponseType;
import net.opengis.wfs20.TransactionType;
import net.opengis.wfs20.ValueCollectionType;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.request.DescribeFeatureTypeRequest;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetCapabilitiesRequest;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.LockFeatureRequest;
import org.geoserver.wfs.request.LockFeatureResponse;
import org.geoserver.wfs.request.Query;
import org.geoserver.wfs.request.TransactionRequest;
import org.geotools.xml.transform.TransformerBase;
import org.opengis.filter.FilterFactory2;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class DefaultWebFeatureService20 implements WebFeatureService20, ApplicationContextAware {

    /** GeoServer configuration */
    protected GeoServer geoServer;

    /** filter factory */
    protected FilterFactory2 filterFactory;

    /**
     * The spring application context, used to look up transaction listeners, plugins and element
     * handlers
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
        return new StoredQueryProvider(
                getCatalog(),
                getServiceInfo(),
                geoServer.getGlobal().isAllowStoredQueriesPerWorkspace());
    }

    public TransformerBase getCapabilities(GetCapabilitiesType request) throws WFSException {
        return new GetCapabilities(
                        getServiceInfo(),
                        getCatalog(),
                        WFSExtensions.findExtendedCapabilitiesProviders(context))
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
        LockFeatureRequest.WFS20 requestWrapper = new LockFeatureRequest.WFS20(request);
        if (request.getLockId() != null) {
            lockFeature.refresh(request.getLockId(), true);
            LockFeatureResponse response = requestWrapper.createResponse();
            response.setLockId(request.getLockId());

            return (LockFeatureResponseType) response.getAdaptee();
        } else {
            // Need to perform some of the same Stored Query handling as GetFeature
            // ... expand eventual stored queries
            boolean getFeatureById =
                    GetFeature.expandStoredQueries(
                            requestWrapper,
                            request.getAbstractQueryExpression(),
                            getStoredQueryProvider());
            // ... expand the typenames from feature id filters (the wrappers will modify the
            // underlying object
            List<Query> queries =
                    GetFeatureRequest.WFS20.getQueries(request.getAbstractQueryExpression());
            GetFeature.expandTypeNames(requestWrapper, queries, getFeatureById, getCatalog());
            // ... lock cannot handle queries with multiple target typenames, need to expand them
            // into separate queries
            fixQueriesForLock(request.getAbstractQueryExpression());

            // run the lock
            return (LockFeatureResponseType) lockFeature.lockFeature(requestWrapper).getAdaptee();
        }
    }

    private void fixQueriesForLock(EList<AbstractQueryExpressionType> queries) {
        for (int i = 0; i < queries.size(); i++) {
            Object obj = queries.get(i);
            if (obj instanceof QueryType) {
                QueryType query = (QueryType) queries.get(0);

                if (query.getTypeNames().size() > 1) {
                    List<QueryType> expanded = new ArrayList<>();
                    for (Object typeName : query.getTypeNames()) {
                        QueryType copy = EcoreUtil.copy(query);
                        copy.getTypeNames().clear();
                        copy.getTypeNames().add(typeName);
                        expanded.add(copy);
                    }
                    queries.remove(i);
                    queries.addAll(i, expanded);
                    i += expanded.size();
                }
            }
        }
    }

    public TransactionResponseType transaction(TransactionType request) throws WFSException {
        Transaction tx = new Transaction(getServiceInfo(), getCatalog(), context);
        tx.setFilterFactory(filterFactory);

        return (TransactionResponseType)
                tx.transaction(new TransactionRequest.WFS20(request)).getAdaptee();
    }

    public ListStoredQueriesResponseType listStoredQueries(ListStoredQueriesType request)
            throws WFSException {
        return new ListStoredQueries(getCatalog(), getStoredQueryProvider()).run(request);
    }

    public DescribeStoredQueriesResponseType describeStoredQueries(
            DescribeStoredQueriesType request) throws WFSException {
        return new DescribeStoredQueries(getServiceInfo(), getStoredQueryProvider()).run(request);
    }

    public CreateStoredQueryResponseType createStoredQuery(CreateStoredQueryType request)
            throws WFSException {
        return new CreateStoredQuery(getServiceInfo(), getStoredQueryProvider()).run(request);
    }

    public ExecutionStatusType dropStoredQuery(DropStoredQueryType request) throws WFSException {
        return new DropStoredQuery(getServiceInfo(), getStoredQueryProvider()).run(request);
    }

    // the following operations are not part of the spec
    public void releaseLock(String lockId) throws WFSException {
        new LockFeature(getServiceInfo(), getCatalog()).release(lockId);
    }
}
