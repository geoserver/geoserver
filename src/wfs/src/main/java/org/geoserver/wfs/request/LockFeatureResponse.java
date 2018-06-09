/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import java.util.List;
import net.opengis.wfs.LockFeatureResponseType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs20.Wfs20Factory;
import org.eclipse.emf.ecore.EObject;
import org.opengis.filter.identity.FeatureId;

/**
 * WFS LockFeature response.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class LockFeatureResponse extends RequestObject {

    protected LockFeatureResponse(EObject adaptee) {
        super(adaptee);
    }

    public String getLockId() {
        return eGet(adaptee, "lockId", String.class);
    }

    public void setLockId(String lockId) {
        eSet(adaptee, "lockId", lockId);
    }

    public abstract void addLockedFeature(FeatureId fid);

    public abstract void addNotLockedFeature(FeatureId fid);

    public abstract List<FeatureId> getNotLockedFeatures();

    public abstract List<FeatureId> getLockedFeatures();

    public static class WFS11 extends LockFeatureResponse {
        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public void addLockedFeature(FeatureId fid) {
            LockFeatureResponseType lfr = (LockFeatureResponseType) adaptee;
            if (lfr.getFeaturesLocked() == null) {
                lfr.setFeaturesLocked(((WfsFactory) getFactory()).createFeaturesLockedType());
            }
            lfr.getFeaturesLocked().getFeatureId().add(fid);
        }

        @Override
        public void addNotLockedFeature(FeatureId fid) {
            LockFeatureResponseType lfr = (LockFeatureResponseType) adaptee;
            if (lfr.getFeaturesNotLocked() == null) {
                lfr.setFeaturesNotLocked(((WfsFactory) getFactory()).createFeaturesNotLockedType());
            }
            lfr.getFeaturesNotLocked().getFeatureId().add(fid);
        }

        @Override
        public List<FeatureId> getNotLockedFeatures() {
            return eGet(adaptee, "featuresNotLocked.featureId", List.class);
        }

        @Override
        public List<FeatureId> getLockedFeatures() {
            return eGet(adaptee, "featuresLocked.featureId", List.class);
        }
    }

    public static class WFS20 extends LockFeatureResponse {
        public WFS20(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public void addLockedFeature(FeatureId fid) {
            net.opengis.wfs20.LockFeatureResponseType lfr =
                    (net.opengis.wfs20.LockFeatureResponseType) adaptee;
            if (lfr.getFeaturesLocked() == null) {
                lfr.setFeaturesLocked(((Wfs20Factory) getFactory()).createFeaturesLockedType());
            }
            lfr.getFeaturesLocked().getResourceId().add(fid);
        }

        @Override
        public void addNotLockedFeature(FeatureId fid) {
            net.opengis.wfs20.LockFeatureResponseType lfr =
                    (net.opengis.wfs20.LockFeatureResponseType) adaptee;
            if (lfr.getFeaturesNotLocked() == null) {
                lfr.setFeaturesNotLocked(
                        ((Wfs20Factory) getFactory()).createFeaturesNotLockedType());
            }
            lfr.getFeaturesNotLocked().getResourceId().add(fid);
        }

        @Override
        public List<FeatureId> getNotLockedFeatures() {
            return eGet(adaptee, "featuresNotLocked.resourceId", List.class);
        }

        @Override
        public List<FeatureId> getLockedFeatures() {
            return eGet(adaptee, "featuresLocked.resourceId", List.class);
        }
    }
}
