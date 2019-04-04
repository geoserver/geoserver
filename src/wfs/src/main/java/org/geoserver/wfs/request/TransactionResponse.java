/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import net.opengis.wfs.ActionType;
import net.opengis.wfs.InsertedFeatureType;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs20.CreatedOrModifiedFeatureType;
import net.opengis.wfs20.Wfs20Factory;
import org.eclipse.emf.ecore.EObject;
import org.opengis.filter.identity.FeatureId;

public abstract class TransactionResponse extends RequestObject {

    public static TransactionResponse adapt(Object request) {
        if (request instanceof TransactionResponseType) {
            return new WFS11((EObject) request);
        } else if (request instanceof net.opengis.wfs20.TransactionResponseType) {
            return new WFS20((EObject) request);
        }
        return null;
    }

    protected TransactionResponse(EObject adaptee) {
        super(adaptee);
    }

    public BigInteger getTotalInserted() {
        return eGet(adaptee, "transactionSummary.totalInserted", BigInteger.class);
    }

    public void setTotalInserted(BigInteger inserted) {
        eSet(adaptee, "transactionSummary.totalInserted", inserted);
    }

    public BigInteger getTotalUpdated() {
        return eGet(adaptee, "transactionSummary.totalUpdated", BigInteger.class);
    }

    public void setTotalUpdated(BigInteger updated) {
        eSet(adaptee, "transactionSummary.totalUpdated", updated);
    }

    public BigInteger getTotalDeleted() {
        return eGet(adaptee, "transactionSummary.totalDeleted", BigInteger.class);
    }

    public void setTotalDeleted(BigInteger deleted) {
        eSet(adaptee, "transactionSummary.totalDeleted", deleted);
    }

    public BigInteger getTotalReplaced() {
        return eGet(adaptee, "transactionSummary.totalReplaced", BigInteger.class);
    }

    public void setTotalReplaced(BigInteger replaced) {
        eSet(adaptee, "transactionSummary.totalReplaced", replaced);
    }

    public List getInsertedFeatures() {
        return eGet(adaptee, "insertResults.feature", List.class);
    }

    public abstract void setHandle(String handle);

    public abstract void addInsertedFeature(String handle, FeatureId id);

    public abstract void addUpdatedFeatures(String handle, Collection<FeatureId> ids);

    public abstract void addReplacedFeatures(String handle, Collection<FeatureId> ids);

    public abstract void addAction(String code, String locator, String message);

    public static class WFS11 extends TransactionResponse {

        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public void setHandle(String handle) {
            eSet(adaptee, "transactionResults.handle", handle);
        }

        public void addInsertedFeature(String handle, FeatureId featureId) {
            InsertedFeatureType insertedFeature =
                    ((WfsFactory) getFactory()).createInsertedFeatureType();
            insertedFeature.setHandle(handle);
            insertedFeature.getFeatureId().add(featureId);

            ((TransactionResponseType) adaptee)
                    .getInsertResults()
                    .getFeature()
                    .add(insertedFeature);
        }

        @Override
        public void addUpdatedFeatures(String handle, Collection<FeatureId> id) {
            // no-op
        }

        @Override
        public void addReplacedFeatures(String handle, Collection<FeatureId> ids) {
            // no-op
        }

        @Override
        public void addAction(String code, String locator, String message) {
            // transaction failed, rollback
            ActionType action = ((WfsFactory) getFactory()).createActionType();
            action.setCode(code);
            action.setLocator(locator);
            action.setMessage(message);

            ((TransactionResponseType) adaptee).getTransactionResults().getAction().add(action);
        }

        public static TransactionResponseType unadapt(TransactionResponse response) {
            if (response instanceof WFS11) {
                return (TransactionResponseType) response.getAdaptee();
            }
            return null;
        }
    }

    public static class WFS20 extends TransactionResponse {

        public WFS20(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public void setHandle(String handle) {
            // no-op
        }

        @Override
        public void addInsertedFeature(String handle, FeatureId featureId) {
            CreatedOrModifiedFeatureType inserted =
                    ((Wfs20Factory) getFactory()).createCreatedOrModifiedFeatureType();
            inserted.setHandle(handle);
            inserted.getResourceId().add(featureId);

            net.opengis.wfs20.TransactionResponseType tr =
                    (net.opengis.wfs20.TransactionResponseType) adaptee;
            if (tr.getInsertResults() == null) {
                tr.setInsertResults(((Wfs20Factory) getFactory()).createActionResultsType());
            }

            tr.getInsertResults().getFeature().add(inserted);
        }

        @Override
        public void addUpdatedFeatures(String handle, Collection<FeatureId> ids) {
            CreatedOrModifiedFeatureType updated =
                    ((Wfs20Factory) getFactory()).createCreatedOrModifiedFeatureType();
            updated.setHandle(handle);
            updated.getResourceId().addAll(ids);

            net.opengis.wfs20.TransactionResponseType tr =
                    (net.opengis.wfs20.TransactionResponseType) adaptee;
            if (tr.getUpdateResults() == null) {
                tr.setUpdateResults(((Wfs20Factory) getFactory()).createActionResultsType());
            }

            tr.getUpdateResults().getFeature().add(updated);
        }

        @Override
        public void addReplacedFeatures(String handle, Collection<FeatureId> ids) {
            CreatedOrModifiedFeatureType updated =
                    ((Wfs20Factory) getFactory()).createCreatedOrModifiedFeatureType();
            updated.setHandle(handle);
            updated.getResourceId().addAll(ids);

            net.opengis.wfs20.TransactionResponseType tr =
                    (net.opengis.wfs20.TransactionResponseType) adaptee;
            if (tr.getReplaceResults() == null) {
                tr.setReplaceResults(((Wfs20Factory) getFactory()).createActionResultsType());
            }

            tr.getReplaceResults().getFeature().add(updated);
        }

        @Override
        public void addAction(String code, String locator, String message) {
            // no-op
        }
    }
}
