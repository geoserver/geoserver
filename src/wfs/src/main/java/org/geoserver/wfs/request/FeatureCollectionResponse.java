/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.List;
import java.util.function.Supplier;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs20.Wfs20Factory;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.wfs.WFSException;
import org.geotools.feature.FeatureCollection;

/**
 * Response object for a feature collection, most notably from a GetFeature request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class FeatureCollectionResponse extends RequestObject {

    protected boolean getFeatureById = false;

    /**
     * It can be expensive to determine the total number of features up front, by using a supplier we can defer
     * calculation until the end of the request (when the value may already have been established).
     */
    protected Supplier<BigInteger> lazyTotalNumberOfFeatures = null;

    public static FeatureCollectionResponse adapt(Object adaptee) {
        if (adaptee instanceof FeatureCollectionType type) {
            return new WFS11(type);
        } else if (adaptee instanceof net.opengis.wfs20.FeatureCollectionType type) {
            return new WFS20(type);
        }
        return null;
    }

    protected FeatureCollectionResponse(EObject adaptee) {
        super(adaptee);
    }

    public String getLockId() {
        return eGet(adaptee, "lockId", String.class);
    }

    public void setLockId(String lockId) {
        eSet(adaptee, "lockId", lockId);
    }

    public Calendar getTimeStamp() {
        return eGet(adaptee, "timeStamp", Calendar.class);
    }

    public void setTimeStamp(Calendar timeStamp) {
        eSet(adaptee, "timeStamp", timeStamp);
    }

    /**
     * Factory method creating a new FeatureCollectionResponse.
     *
     * @return feature collection response
     */
    public abstract FeatureCollectionResponse create();

    /**
     * Number of features included in this response. Number reflect the number of features included, which may be less
     * than {@link #getTotalNumberOfFeatures()} when paging through more content than can be obtained in a single
     * request.
     *
     * @return number of features included in this response.
     */
    public abstract BigInteger getNumberOfFeatures();

    /**
     * Number of features included in response.
     *
     * @param n Number of features included in response
     */
    public abstract void setNumberOfFeatures(BigInteger n);

    /**
     * Used to calculate total number of features on demand (only if needed). This allows formats that do not need the
     * total to avoid calculating this expensive result, it also may be that some data stores can better estimate this
     * total is obtained after traversing results.
     *
     * @param totalNumberOfFeatures Delayed calculation of total number of featuers.
     */
    public void setLazyTotalNumberOfFeatures(Supplier<BigInteger> totalNumberOfFeatures) {
        this.lazyTotalNumberOfFeatures = totalNumberOfFeatures;
    }

    /**
     * Total number of features hits matched, or {@code null} for "unknown". Total is used as a guide when paging
     * through more content than can be obtained in a single request.
     *
     * <p>This value is set by calling {@link #setLazyTotalNumberOfFeatures(Supplier)} (deferred value), or
     * {@link #setTotalNumberOfFeatures(BigInteger)}.
     *
     * @return total number of features available, or null for "unknown".
     */
    public BigInteger getTotalNumberOfFeatures() {
        if (lazyTotalNumberOfFeatures != null) {
            return lazyTotalNumberOfFeatures.get();
        } else {
            return null;
        }
    }

    /**
     * Total number of Features hits matched, which may be greater than the number included in an individual result.
     *
     * @param totalHits total number of feature hits matched, or {@code null} for "unknown".
     */
    public void setTotalNumberOfFeatures(final BigInteger totalHits) {
        lazyTotalNumberOfFeatures = () -> totalHits;
    }

    public abstract void setPrevious(String previous);

    public abstract String getPrevious();

    public abstract void setNext(String next);

    public abstract String getNext();

    public abstract List<FeatureCollection> getFeatures();

    public abstract void setFeatures(List<FeatureCollection> features);

    public abstract Object unadapt(Class<?> target);

    public List<FeatureCollection> getFeature() {
        // alias
        return getFeatures();
    }

    public void setGetFeatureById(boolean getFeatureById) {
        this.getFeatureById = getFeatureById;
    }

    public boolean isGetFeatureById() {
        return getFeatureById;
    }

    /** FeatureCollection response adapted from {@link net.opengis.wfs20.FeatureCollectionType}. */
    public static class WFS11 extends FeatureCollectionResponse {

        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public FeatureCollectionResponse create() {
            return FeatureCollectionResponse.adapt(((WfsFactory) getFactory()).createFeatureCollectionType());
        }

        @Override
        public BigInteger getNumberOfFeatures() {
            return eGet(adaptee, "numberOfFeatures", BigInteger.class);
        }

        @Override
        public void setNumberOfFeatures(BigInteger n) {
            eSet(adaptee, "numberOfFeatures", n);
        }

        @Override
        public String getPrevious() {
            // noop
            return null;
        }

        @Override
        public void setPrevious(String previous) {
            // noop
        }

        @Override
        public String getNext() {
            // noop
            return null;
        }

        @Override
        public void setNext(String next) {
            // noop
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<FeatureCollection> getFeatures() {
            return eGet(adaptee, "feature", List.class);
        }

        @Override
        public void setFeatures(List<FeatureCollection> features) {
            eSet(adaptee, "feature", features);
        }

        @Override
        @SuppressWarnings("unchecked") // EMF model without generics
        public Object unadapt(Class<?> target) {
            if (target.equals(FeatureCollectionType.class)) {
                return adaptee;
            } else if (target.equals(net.opengis.wfs20.FeatureCollectionType.class)) {
                FeatureCollectionType source = (FeatureCollectionType) adaptee;
                net.opengis.wfs20.FeatureCollectionType result = Wfs20Factory.eINSTANCE.createFeatureCollectionType();
                result.getMember().addAll(source.getFeature());
                result.setNumberReturned(source.getNumberOfFeatures());
                result.setLockId(source.getLockId());
                result.setTimeStamp(source.getTimeStamp());
                return result;
            } else {
                throw new WFSException("Cannot transform " + adaptee + " to the specified target class " + target);
            }
        }
    }

    /** FeatureCollectionResponse from {@link net.opengis.wfs20.FeatureCollectionType}. */
    public static class WFS20 extends FeatureCollectionResponse {
        public WFS20(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public FeatureCollectionResponse create() {
            return FeatureCollectionResponse.adapt(((Wfs20Factory) getFactory()).createFeatureCollectionType());
        }

        @Override
        public BigInteger getNumberOfFeatures() {
            return eGet(adaptee, "numberReturned", BigInteger.class);
        }

        @Override
        public void setNumberOfFeatures(BigInteger n) {
            eSet(adaptee, "numberReturned", n);
        }

        @Override
        public BigInteger getTotalNumberOfFeatures() {
            if (lazyTotalNumberOfFeatures != null) {
                return lazyTotalNumberOfFeatures.get();
            } else {
                BigInteger result = eGet(adaptee, "numberMatched", BigInteger.class);
                if (result != null && result.signum() < 0) {
                    // indicates "unknown"
                    return null;
                }
                return result;
            }
        }

        @Override
        public void setTotalNumberOfFeatures(BigInteger n) {
            eSet(adaptee, "numberMatched", n);
            this.lazyTotalNumberOfFeatures = () -> {
                BigInteger result = eGet(adaptee, "numberMatched", BigInteger.class);
                if (result != null && result.signum() < 0) {
                    return null; // indicates "unknown"
                }
                return result;
            };
        }

        @Override
        public String getPrevious() {
            return eGet(adaptee, "previous", String.class);
        }

        @Override
        public void setPrevious(String previous) {
            eSet(adaptee, "previous", previous);
        }

        @Override
        public String getNext() {
            return eGet(adaptee, "next", String.class);
        }

        @Override
        public void setNext(String next) {
            eSet(adaptee, "next", next);
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<FeatureCollection> getFeatures() {
            return eGet(adaptee, "member", List.class);
        }

        @Override
        public void setFeatures(List<FeatureCollection> features) {
            eSet(adaptee, "member", features);
        }

        @Override
        @SuppressWarnings("unchecked") // EMF model without generics
        public Object unadapt(Class<?> target) {
            if (target.equals(net.opengis.wfs20.FeatureCollectionType.class)) {
                eSet(adaptee, "numberMatched", getTotalNumberOfFeatures());
                return adaptee;
            } else if (target.equals(FeatureCollectionType.class)) {
                net.opengis.wfs20.FeatureCollectionType source = (net.opengis.wfs20.FeatureCollectionType) adaptee;
                FeatureCollectionType result = WfsFactory.eINSTANCE.createFeatureCollectionType();
                result.getFeature().addAll(source.getMember());
                result.setNumberOfFeatures(source.getNumberReturned());
                result.setLockId(source.getLockId());
                result.setTimeStamp(source.getTimeStamp());
                return result;
            } else {
                throw new WFSException("Cannot transform " + adaptee + " to the specified target class " + target);
            }
        }
    }
}
