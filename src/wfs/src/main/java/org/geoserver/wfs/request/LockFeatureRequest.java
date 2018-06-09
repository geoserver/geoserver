/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import net.opengis.wfs.AllSomeType;
import net.opengis.wfs.LockFeatureType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs20.Wfs20Factory;
import net.opengis.wfs20.Wfs20Package;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.FeatureMap;

/**
 * WFS LockFeature request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class LockFeatureRequest extends RequestObject {

    public static LockFeatureRequest adapt(Object request) {
        if (request instanceof LockFeatureType) {
            return new WFS11((EObject) request);
        } else if (request instanceof net.opengis.wfs20.LockFeatureType) {
            return new WFS20((EObject) request);
        }
        return null;
    }

    protected LockFeatureRequest(EObject adaptee) {
        super(adaptee);
    }

    public BigInteger getExpiry() {
        return eGet(adaptee, "expiry", BigInteger.class);
    }

    public void setExpiry(BigInteger expiry) {
        eSet(adaptee, "expiry", expiry);
    }

    public abstract List<Lock> getLocks();

    public abstract void addLock(Lock lock);

    public abstract boolean isLockActionSome();

    public abstract void setLockActionSome();

    public abstract boolean isLockActionAll();

    public abstract void setLockActionAll();

    public abstract Lock createLock();

    public abstract LockFeatureResponse createResponse();

    public abstract List getAdaptedQueries();

    public abstract RequestObject createQuery();

    public abstract List<Query> getQueries();

    public static class WFS11 extends LockFeatureRequest {

        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public List<Lock> getLocks() {
            List<Lock> locks = new ArrayList();
            for (Object lock : eGet(adaptee, "lock", List.class)) {
                locks.add(new Lock.WFS11((EObject) lock));
            }
            return locks;
        }

        @Override
        public void addLock(Lock lock) {
            eGet(adaptee, "lock", List.class).add(lock.getAdaptee());
        }

        @Override
        public boolean isLockActionAll() {
            return ((LockFeatureType) adaptee).getLockAction() == AllSomeType.ALL_LITERAL;
        }

        @Override
        public void setLockActionAll() {
            ((LockFeatureType) adaptee).setLockAction(AllSomeType.ALL_LITERAL);
        }

        @Override
        public boolean isLockActionSome() {
            return ((LockFeatureType) adaptee).getLockAction() == AllSomeType.SOME_LITERAL;
        }

        @Override
        public void setLockActionSome() {
            ((LockFeatureType) adaptee).setLockAction(AllSomeType.SOME_LITERAL);
        }

        @Override
        public Lock createLock() {
            return new Lock.WFS11(((WfsFactory) getFactory()).createLockType());
        }

        @Override
        public LockFeatureResponse createResponse() {
            return new LockFeatureResponse.WFS11(
                    ((WfsFactory) getFactory()).createLockFeatureResponseType());
        }

        @Override
        public List getAdaptedQueries() {
            throw new UnsupportedOperationException();
        }

        @Override
        public RequestObject createQuery() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Query> getQueries() {
            throw new UnsupportedOperationException();
        }
    }

    public static class WFS20 extends LockFeatureRequest {

        public WFS20(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public List<Lock> getLocks() {
            List<Lock> locks = new ArrayList();
            for (Object lock : eGet(adaptee, "abstractQueryExpression", List.class)) {
                locks.add(new Lock.WFS20((EObject) lock));
            }
            return locks;
        }

        @Override
        public void addLock(Lock lock) {
            ((FeatureMap) eGet(adaptee, "abstractQueryExpressionGroup", List.class))
                    .add(Wfs20Package.Literals.DOCUMENT_ROOT__QUERY, lock.getAdaptee());
        }

        @Override
        public boolean isLockActionAll() {
            return ((net.opengis.wfs20.LockFeatureType) adaptee).getLockAction()
                    == net.opengis.wfs20.AllSomeType.ALL;
        }

        @Override
        public void setLockActionAll() {
            ((net.opengis.wfs20.LockFeatureType) adaptee)
                    .setLockAction(net.opengis.wfs20.AllSomeType.ALL);
        }

        @Override
        public boolean isLockActionSome() {
            return ((net.opengis.wfs20.LockFeatureType) adaptee).getLockAction()
                    == net.opengis.wfs20.AllSomeType.SOME;
        }

        @Override
        public void setLockActionSome() {
            ((net.opengis.wfs20.LockFeatureType) adaptee)
                    .setLockAction(net.opengis.wfs20.AllSomeType.SOME);
        }

        @Override
        public Lock createLock() {
            return new Lock.WFS20(((Wfs20Factory) getFactory()).createQueryType());
        }

        @Override
        public LockFeatureResponse createResponse() {
            return new LockFeatureResponse.WFS20(
                    ((Wfs20Factory) getFactory()).createLockFeatureResponseType());
        }

        @Override
        public List<Object> getAdaptedQueries() {
            return eGet(adaptee, "abstractQueryExpression", List.class);
        }

        @Override
        public Query createQuery() {
            return new Query.WFS20(((Wfs20Factory) getFactory()).createQueryType());
        }

        @Override
        public List<Query> getQueries() {
            List<Object> adaptedQueries = getAdaptedQueries();
            return GetFeatureRequest.WFS20.getQueries(adaptedQueries);
        }
    }
}
