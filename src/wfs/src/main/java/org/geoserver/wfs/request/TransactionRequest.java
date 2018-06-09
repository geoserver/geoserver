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
import net.opengis.wfs.AllSomeType;
import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.NativeType;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs20.DeleteType;
import net.opengis.wfs20.InsertType;
import net.opengis.wfs20.ReplaceType;
import net.opengis.wfs20.UpdateType;
import net.opengis.wfs20.Wfs20Factory;
import org.eclipse.emf.ecore.EObject;
import org.geotools.data.Transaction;

/**
 * WFS Transaction request.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class TransactionRequest extends RequestObject {

    public static TransactionRequest adapt(Object request) {
        if (request instanceof TransactionType) {
            return new WFS11((EObject) request);
        } else if (request instanceof net.opengis.wfs20.TransactionType) {
            return new WFS20((EObject) request);
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

    public abstract List<TransactionElement> getElements();

    public abstract TransactionResponse createResponse();

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
        public List<TransactionElement> getElements() {
            List<TransactionElement> list = new ArrayList();
            for (Iterator it = ((TransactionType) adaptee).getGroup().valueListIterator();
                    it.hasNext(); ) {
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

        public static TransactionType unadapt(TransactionRequest request) {
            if (request instanceof WFS11) {
                return (TransactionType) request.getAdaptee();
            }

            WfsFactory factory = (WfsFactory) WfsFactory.eINSTANCE;
            TransactionType tx = factory.createTransactionType();

            tx.setVersion(request.getVersion());
            tx.setHandle(request.getHandle());
            tx.setLockId(request.getLockId());
            tx.setReleaseAction(
                    request.isReleaseActionAll()
                            ? AllSomeType.ALL_LITERAL
                            : AllSomeType.SOME_LITERAL);
            tx.setBaseUrl(request.getBaseUrl());
            tx.setExtendedProperties(request.getExtendedProperties());

            for (TransactionElement te : request.getElements()) {
                if (te instanceof Delete) {
                    tx.getDelete().add(Delete.WFS11.unadapt((Delete) te));
                }
                if (te instanceof Update) {
                    tx.getUpdate().add(Update.WFS11.unadapt((Update) te));
                }
                if (te instanceof Insert) {
                    tx.getInsert().add(Insert.WFS11.unadapt((Insert) te));
                }
                if (te instanceof Native) {
                    tx.getNative().add(Native.WFS11.unadapt((Native) te));
                }
            }

            return tx;
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
            ((net.opengis.wfs20.TransactionType) adaptee)
                    .setReleaseAction(net.opengis.wfs20.AllSomeType.ALL);
        }

        @Override
        public List<TransactionElement> getElements() {
            List<TransactionElement> list = new ArrayList();
            Iterator it =
                    ((net.opengis.wfs20.TransactionType) adaptee)
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
    }
}
