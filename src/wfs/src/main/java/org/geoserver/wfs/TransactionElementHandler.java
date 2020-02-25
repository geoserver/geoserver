/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.Map;
import javax.xml.namespace.QName;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.wfs.request.TransactionElement;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.request.TransactionResponse;
import org.geotools.data.FeatureStore;

/**
 * Transaction elements are an open ended set, both thanks to the Native element type, and to the
 * XSD sustitution group concept (xsd inheritance). Element handlers know how to process a certain
 * element in a wfs transaction request.
 *
 * @author Andrea Aime - TOPP
 */
public interface TransactionElementHandler {
    /** Returns the element class this handler can proces */
    Class<?> getElementClass();

    /** Returns the qualified names of feature types needed to handle this element */
    QName[] getTypeNames(TransactionRequest request, TransactionElement element)
            throws WFSTransactionException;

    /**
     * Checks the element content is valid, throws an exception otherwise
     *
     * @param element the transaction element we're checking
     * @param featureTypeInfos a map from {@link QName} to {@link FeatureTypeInfo}, where the keys
     *     contain all the feature type names reported by {@link #getTypeNames(EObject)}
     */
    void checkValidity(TransactionElement element, Map<QName, FeatureTypeInfo> featureTypeInfos)
            throws WFSTransactionException;

    /**
     * Executes the element against the provided feature sources
     *
     * @param element the tranaction element to be executed
     * @param request the transaction request
     * @param featureStores map from {@link QName} to {@link FeatureStore}, where the keys do
     *     contain all the feature type names reported by {@link #getTypeNames(EObject)}
     * @param response the transaction response, that the element will update according to the
     *     processing done
     * @param listener a transaction listener that will be called before and after each change
     *     performed against the data stores
     */
    @SuppressWarnings("rawtypes")
    void execute(
            TransactionElement element,
            TransactionRequest request,
            Map<QName, FeatureStore> featureStores,
            TransactionResponse response,
            TransactionListener listener)
            throws WFSTransactionException;
}
