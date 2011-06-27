/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import net.opengis.wfs.NativeType;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.FeatureStore;


/**
 * Processes native elements as unrecognized ones, and checks wheter they can be
 * safely ignored on not.
 *
 * @author Andrea Aime - TOPP
 */
public class NativeElementHandler implements TransactionElementHandler {
    /**
     * logger
     */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.wfs");

    /**
     * Empty array of QNames
     */
    protected static final QName[] EMPTY_QNAMES = new QName[0];

    public void checkValidity(EObject element, Map<QName, FeatureTypeInfo> featureTypeInfos)
        throws WFSTransactionException {
        NativeType nativ = (NativeType) element;

        if (!nativ.isSafeToIgnore()) {
            throw new WFSTransactionException("Native element:" + nativ.getVendorId()
                + " unsupported but marked as" + " unsafe to ignore", "InvalidParameterValue");
        }
    }

    public void execute(EObject element, TransactionType request,
            @SuppressWarnings("rawtypes") Map<QName, FeatureStore> featureSources,
            TransactionResponseType response, TransactionListener listener)
            throws WFSTransactionException {
        // nothing to do, we just ignore if possible
    }

    /**
     * @see org.geoserver.wfs.TransactionElementHandler#getElementClass()
     */
    public Class<NativeType> getElementClass() {
        return NativeType.class;
    }

    /**
     * @return an empty array.
     * @see org.geoserver.wfs.TransactionElementHandler#getTypeNames(org.eclipse.emf.ecore.EObject)
     */
    public QName[] getTypeNames(EObject element) throws WFSTransactionException {
        // we don't handle this
        return EMPTY_QNAMES;
    }
}
