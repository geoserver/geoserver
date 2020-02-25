/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.GetFeatureWithLockType;
import net.opengis.wfs.ResultTypeType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs20.ResolveValueType;
import net.opengis.wfs20.Wfs20Factory;
import org.eclipse.emf.ecore.EObject;
import org.geotools.xsd.EMFUtils;

/**
 * WFS GetFeature request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class GetFeatureRequest extends RequestObject {

    public static GetFeatureRequest adapt(Object request) {
        if (request instanceof GetFeatureType) {
            return new WFS11((EObject) request);
        } else if (request instanceof net.opengis.wfs20.GetFeatureType) {
            return new WFS20((EObject) request);
        }
        return null;
    }

    protected GetFeatureRequest(EObject adaptee) {
        super(adaptee);
    }

    public BigInteger getStartIndex() {
        return eGet(adaptee, "startIndex", BigInteger.class);
    }

    public void setStartIndex(BigInteger startIndex) {
        eSet(adaptee, "startIndex", startIndex);
    }

    public List<Map<String, String>> getViewParams() {
        return eGet(adaptee, "viewParams", List.class);
    }

    public void setViewParams(List<Map<String, String>> viewParams) {
        List l = eGet(adaptee, "viewParams", List.class);
        l.clear();
        l.addAll(viewParams);
    }

    public abstract List<Query> getQueries();

    public abstract List<Object> getAdaptedQueries();

    public abstract boolean isQueryTypeNamesUnset();

    public abstract BigInteger getMaxFeatures();

    public abstract void setMaxFeatures(BigInteger maxFeatures);

    public abstract String getTraverseXlinkDepth();

    public abstract boolean isResultTypeResults();

    public abstract boolean isResultTypeHits();

    public abstract boolean isLockRequest();

    public abstract boolean isLockActionSome();

    public abstract Query createQuery();

    public abstract LockFeatureRequest createLockRequest();

    public abstract FeatureCollectionResponse createResponse();

    public abstract ResolveValueType getResolve();

    public abstract BigInteger getResolveTimeOut();

    //
    // GetFeatureWithLock
    //
    public BigInteger getExpiry() {
        return eGet(adaptee, "expiry", BigInteger.class);
    }

    public void setExpiry(BigInteger expiry) {
        eSet(adaptee, "expiry", expiry);
    }

    public static class WFS11 extends GetFeatureRequest {
        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public List<Query> getQueries() {
            // TODO: instead of creating a new list we should wrap the existing on in case the
            // client
            // code needs to modify
            List<Query> list = new ArrayList<Query>();
            for (Object o : getAdaptedQueries()) {
                list.add(new Query.WFS11((EObject) o));
            }

            return list;
        }

        @Override
        public List<Object> getAdaptedQueries() {
            return eGet(adaptee, "query", List.class);
        }

        @Override
        public boolean isQueryTypeNamesUnset() {
            return EMFUtils.isUnset(eGet(adaptee, "query", List.class), "typeName");
        }

        @Override
        public BigInteger getMaxFeatures() {
            return eGet(adaptee, "maxFeatures", BigInteger.class);
        }

        @Override
        public void setMaxFeatures(BigInteger maxFeatures) {
            eSet(adaptee, "maxFeatures", maxFeatures);
        }

        @Override
        public String getTraverseXlinkDepth() {
            return eGet(adaptee, "traverseXlinkDepth", String.class);
        }

        @Override
        public boolean isResultTypeResults() {
            return ((GetFeatureType) adaptee).getResultType() == ResultTypeType.RESULTS_LITERAL;
        }

        @Override
        public boolean isResultTypeHits() {
            return ((GetFeatureType) adaptee).getResultType() == ResultTypeType.HITS_LITERAL;
        }

        @Override
        public boolean isLockRequest() {
            return adaptee instanceof GetFeatureWithLockType;
        }

        @Override
        public boolean isLockActionSome() {
            // no concept of "some" in WFS 1.1 GetFeatureWithLock
            return false;
        }

        @Override
        public Query createQuery() {
            return new Query.WFS11(((WfsFactory) getFactory()).createQueryType());
        }

        @Override
        public LockFeatureRequest createLockRequest() {
            return new LockFeatureRequest.WFS11(
                    ((WfsFactory) getFactory()).createLockFeatureType());
        }

        @Override
        public FeatureCollectionResponse createResponse() {
            return new FeatureCollectionResponse.WFS11(
                    ((WfsFactory) getFactory()).createFeatureCollectionType());
        }

        @Override
        public ResolveValueType getResolve() {
            return ResolveValueType.ALL;
        }

        @Override
        public BigInteger getResolveTimeOut() {
            BigInteger seconds = eGet(adaptee, "traverseXlinkExpiry", BigInteger.class);
            return seconds == null ? null : BigInteger.valueOf(60).multiply(seconds);
        }
    }

    public static class WFS20 extends GetFeatureRequest {
        public WFS20(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public List<Query> getQueries() {
            List<Object> adaptedQueries = getAdaptedQueries();

            return getQueries(adaptedQueries);
        }

        public static List<Query> getQueries(List<?> adaptedQueries) {
            List<Query> list = new ArrayList<Query>();
            for (Object o : adaptedQueries) {
                list.add(new Query.WFS20((EObject) o));
            }

            return list;
        }

        @Override
        public List<Object> getAdaptedQueries() {
            return eGet(adaptee, "abstractQueryExpression", List.class);
        }

        @Override
        public boolean isQueryTypeNamesUnset() {
            return EMFUtils.isUnset(
                    eGet(adaptee, "abstractQueryExpression", List.class), "typeNames");
        }

        @Override
        public BigInteger getMaxFeatures() {
            return eGet(adaptee, "count", BigInteger.class);
        }

        @Override
        public void setMaxFeatures(BigInteger maxFeatures) {
            eSet(adaptee, "count", maxFeatures);
        }

        @Override
        public String getTraverseXlinkDepth() {
            Object obj = eGet(adaptee, "resolveDepth", Object.class);
            return obj != null ? obj.toString() : null;
        }

        @Override
        public boolean isResultTypeResults() {
            return ((net.opengis.wfs20.GetFeatureType) adaptee).getResultType()
                    == net.opengis.wfs20.ResultTypeType.RESULTS;
        }

        @Override
        public boolean isResultTypeHits() {
            return ((net.opengis.wfs20.GetFeatureType) adaptee).getResultType()
                    == net.opengis.wfs20.ResultTypeType.HITS;
        }

        @Override
        public boolean isLockRequest() {
            return adaptee instanceof net.opengis.wfs20.GetFeatureWithLockType;
        }

        @Override
        public boolean isLockActionSome() {
            return ((net.opengis.wfs20.GetFeatureWithLockType) adaptee).getLockAction()
                    == net.opengis.wfs20.AllSomeType.SOME;
        }

        @Override
        public Query createQuery() {
            return new Query.WFS20(((Wfs20Factory) getFactory()).createQueryType());
        }

        @Override
        public LockFeatureRequest createLockRequest() {
            return new LockFeatureRequest.WFS20(
                    ((Wfs20Factory) getFactory()).createLockFeatureType());
        }

        @Override
        public FeatureCollectionResponse createResponse() {
            return new FeatureCollectionResponse.WFS20(
                    ((Wfs20Factory) getFactory()).createFeatureCollectionType());
        }

        @Override
        public ResolveValueType getResolve() {
            return eGet(adaptee, "resolve", ResolveValueType.class);
        }

        @Override
        public BigInteger getResolveTimeOut() {
            return eGet(adaptee, "resolveTimeOut", BigInteger.class);
        }
    }
}
