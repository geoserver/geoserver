/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.request;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.opengis.wfs.AllSomeType;
import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.NativeType;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs20.AbstractTransactionActionType;
import net.opengis.wfs20.DeleteType;
import net.opengis.wfs20.InsertType;
import net.opengis.wfs20.ReplaceType;
import net.opengis.wfs20.UpdateType;
import net.opengis.wfs20.Wfs20Factory;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.FeatureMap.ValueListIterator;
import org.geotools.api.data.Transaction;

/**
 * WFS Transaction request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class TransactionRequest extends RequestObject {

    public static TransactionRequest adapt(Object request) {
        if (request instanceof TransactionType type) {
            return new WFS11(type);
        } else if (request instanceof net.opengis.wfs20.TransactionType type) {
            return new WFS20(type);
        }
        return null;
    }

    private Transaction transaction;

    protected TransactionRequest(EObject adaptee) {
        super(adaptee);
    }

    public Object getReleaseAction() {
        return eGet(adaptee, "releaseAction", Object.class);
    }

    public String getLockId() {
        return eGet(adaptee, "lockId", String.class);
    }

    public void setLockId(String lockId) {
        eSet(adaptee, "lockId", lockId);
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public abstract boolean isReleaseActionAll();

    public abstract boolean isReleaseActionSome();

    public abstract void setReleaseActionAll();

    public abstract void setReleaseActionSome();

    public abstract List<TransactionElement> getElements();

    public abstract void setElements(List<TransactionElement> elements);

    public abstract TransactionResponse createResponse();

    public abstract Insert createInsert();

    public abstract Update createUpdate();

    public abstract Delete createDelete();

    public abstract Replace createReplace();

    public abstract boolean remove(TransactionElement pElement);

    public static class WFS11 extends TransactionRequest {
        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public boolean isReleaseActionAll() {
            return ((TransactionType) adaptee).getReleaseAction() == AllSomeType.ALL_LITERAL;
        }

        @Override
        public boolean isReleaseActionSome() {
            return ((TransactionType) adaptee).getReleaseAction() == AllSomeType.SOME_LITERAL;
        }

        @Override
        public void setReleaseActionAll() {
            ((TransactionType) adaptee).setReleaseAction(AllSomeType.ALL_LITERAL);
        }

        @Override
        public void setReleaseActionSome() {
            ((TransactionType) adaptee).setReleaseAction(AllSomeType.SOME_LITERAL);
        }

        @Override
        public List<TransactionElement> getElements() {
            List<TransactionElement> list = new ArrayList<>();
            for (Iterator it = ((TransactionType) adaptee).getGroup().valueListIterator(); it.hasNext(); ) {
                EObject el = (EObject) it.next();
                if (el instanceof DeleteElementType) {
                    list.add(new Delete.WFS11(el));
                } else if (el instanceof InsertElementType) {
                    list.add(new Insert.WFS11(el));
                } else if (el instanceof UpdateElementType) {
                    list.add(new Update.WFS11(el));
                } else if (el instanceof NativeType) {
                    list.add(new Native.WFS11(el));
                } else {
                    throw new IllegalArgumentException("Unrecognized transaction element: " + el);
                }
            }

            return list;
        }

        @Override
        @SuppressWarnings("unchecked") // EMF model without generics
        public void setElements(List<TransactionElement> elements) {
            TransactionType tx = (TransactionType) adaptee;
            tx.getInsert().clear();
            tx.getDelete().clear();
            tx.getUpdate().clear();

            for (TransactionElement element : elements) {
                if (element instanceof Insert) {
                    tx.getInsert().add(element.getAdaptee());
                } else if (element instanceof Update) {
                    tx.getUpdate().add(element.getAdaptee());
                } else if (element instanceof Delete) {
                    tx.getDelete().add(element.getAdaptee());
                }
                // no replace in wfs 1.1, cannot be there
            }
        }

        @Override
        public TransactionResponse createResponse() {
            WfsFactory factory = (WfsFactory) getFactory();
            TransactionResponseType tr = factory.createTransactionResponseType();
            tr.setTransactionSummary(factory.createTransactionSummaryType());
            tr.getTransactionSummary().setTotalInserted(BigInteger.valueOf(0));
            tr.getTransactionSummary().setTotalUpdated(BigInteger.valueOf(0));
            tr.getTransactionSummary().setTotalDeleted(BigInteger.valueOf(0));
            tr.setTransactionResults(factory.createTransactionResultsType());
            tr.setInsertResults(factory.createInsertResultsType());

            return new TransactionResponse.WFS11(tr);
        }

        @Override
        public Insert createInsert() {
            WfsFactory factory = (WfsFactory) getFactory();
            return new Insert.WFS11(factory.createInsertElementType());
        }

        @Override
        public Update createUpdate() {
            WfsFactory factory = (WfsFactory) getFactory();
            return new Update.WFS11(factory.createUpdateElementType());
        }

        @Override
        public Delete createDelete() {
            WfsFactory factory = (WfsFactory) getFactory();
            return new Delete.WFS11(factory.createDeleteElementType());
        }

        @Override
        public Replace createReplace() {
            throw new UnsupportedOperationException("Replace not supported in WFS 1.1 transactions");
        }

        @SuppressWarnings("unchecked") // EMF model without generics
        public static TransactionType unadapt(TransactionRequest request) {
            if (request instanceof WFS11) {
                return (TransactionType) request.getAdaptee();
            }

            WfsFactory factory = WfsFactory.eINSTANCE;
            TransactionType tx = factory.createTransactionType();

            tx.setVersion(request.getVersion());
            tx.setHandle(request.getHandle());
            tx.setLockId(request.getLockId());
            tx.setReleaseAction(request.isReleaseActionAll() ? AllSomeType.ALL_LITERAL : AllSomeType.SOME_LITERAL);
            tx.setBaseUrl(request.getBaseUrl());
            tx.setExtendedProperties(request.getExtendedProperties());

            for (TransactionElement te : request.getElements()) {
                if (te instanceof Delete delete) {
                    tx.getDelete().add(Delete.WFS11.unadapt(delete));
                }
                if (te instanceof Update update) {
                    tx.getUpdate().add(Update.WFS11.unadapt(update));
                }
                if (te instanceof Insert insert) {
                    tx.getInsert().add(Insert.WFS11.unadapt(insert));
                }
                if (te instanceof Native native1) {
                    tx.getNative().add(Native.WFS11.unadapt(native1));
                }
            }

            return tx;
        }

        @Override
        public Map getExtendedProperties() {
            return ((TransactionType) adaptee).getExtendedProperties();
        }

        @Override
        public boolean remove(TransactionElement pElement) {
            if (pElement == null) {
                return false;
            }
            for (ValueListIterator<Object> it =
                            ((TransactionType) adaptee).getGroup().valueListIterator();
                    it.hasNext(); ) {
                EObject el = (EObject) it.next();
                if (pElement.getAdaptee() == el) {
                    it.remove();
                    return true;
                }
            }
            return false;
        }
    }

    public static class WFS20 extends TransactionRequest {
        public WFS20(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public boolean isReleaseActionAll() {
            return ((net.opengis.wfs20.TransactionType) adaptee).getReleaseAction()
                    == net.opengis.wfs20.AllSomeType.ALL;
        }

        @Override
        public boolean isReleaseActionSome() {
            return ((net.opengis.wfs20.TransactionType) adaptee).getReleaseAction()
                    == net.opengis.wfs20.AllSomeType.SOME;
        }

        @Override
        public void setReleaseActionAll() {
            ((net.opengis.wfs20.TransactionType) adaptee).setReleaseAction(net.opengis.wfs20.AllSomeType.ALL);
        }

        @Override
        public void setReleaseActionSome() {
            ((net.opengis.wfs20.TransactionType) adaptee).setReleaseAction(net.opengis.wfs20.AllSomeType.SOME);
        }

        @Override
        public List<TransactionElement> getElements() {
            List<TransactionElement> list = new ArrayList<>();
            Iterator it = ((net.opengis.wfs20.TransactionType) adaptee)
                    .getAbstractTransactionAction()
                    .iterator();
            while (it.hasNext()) {
                EObject el = (EObject) it.next();
                if (el instanceof DeleteType) {
                    list.add(new Delete.WFS20(el));
                } else if (el instanceof InsertType) {
                    list.add(new Insert.WFS20(el));
                } else if (el instanceof UpdateType) {
                    list.add(new Update.WFS20(el));
                } else if (el instanceof ReplaceType) {
                    list.add(new Replace.WFS20(el));
                } else if (el instanceof net.opengis.wfs20.NativeType) {
                    list.add(new Native.WFS20(el));
                } else {
                    throw new IllegalArgumentException("Unrecognized transaction element: " + el);
                }
            }
            return list;
        }

        @Override
        public void setElements(List<TransactionElement> elements) {
            net.opengis.wfs20.TransactionType tx = (net.opengis.wfs20.TransactionType) adaptee;
            EList<AbstractTransactionActionType> transactionElements = tx.getAbstractTransactionAction();
            transactionElements.clear();
            elements.stream()
                    .map(e -> (AbstractTransactionActionType) e.getAdaptee())
                    .forEach(e -> transactionElements.add(e));
        }

        @Override
        public TransactionResponse createResponse() {
            Wfs20Factory factory = (Wfs20Factory) getFactory();
            net.opengis.wfs20.TransactionResponseType tr = factory.createTransactionResponseType();
            tr.setTransactionSummary(factory.createTransactionSummaryType());
            tr.getTransactionSummary().setTotalDeleted(BigInteger.valueOf(0));
            tr.getTransactionSummary().setTotalInserted(BigInteger.valueOf(0));
            tr.getTransactionSummary().setTotalUpdated(BigInteger.valueOf(0));
            tr.getTransactionSummary().setTotalReplaced(BigInteger.valueOf(0));

            return new TransactionResponse.WFS20(tr);
        }

        @Override
        public Insert createInsert() {
            Wfs20Factory factory = (Wfs20Factory) getFactory();
            return new Insert.WFS20(factory.createInsertType());
        }

        @Override
        public Update createUpdate() {
            Wfs20Factory factory = (Wfs20Factory) getFactory();
            return new Update.WFS20(factory.createUpdateType());
        }

        @Override
        public Delete createDelete() {
            Wfs20Factory factory = (Wfs20Factory) getFactory();
            return new Delete.WFS20(factory.createDeleteType());
        }

        @Override
        public Replace createReplace() {
            Wfs20Factory factory = (Wfs20Factory) getFactory();
            return new Replace.WFS20(factory.createReplaceType());
        }

        @Override
        public Map getExtendedProperties() {
            return ((net.opengis.wfs20.TransactionType) adaptee).getExtendedProperties();
        }

        @Override
        public boolean remove(TransactionElement pElement) {
            if (pElement == null) {
                return false;
            }
            Iterator<AbstractTransactionActionType> it = ((net.opengis.wfs20.TransactionType) adaptee)
                    .getAbstractTransactionAction()
                    .iterator();
            while (it.hasNext()) {
                EObject el = it.next();
                if (pElement.getAdaptee() == el) {
                    it.remove();
                    return true;
                }
            }
            return false;
        }
    }
}
