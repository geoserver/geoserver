/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.Map;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.geoserver.wfs.request.Native;
import org.geoserver.wfs.request.TransactionElement;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.request.TransactionResponse;

/**
 * Processes native elements as unrecognized ones, and checks wheter they can be safely ignored on
 * not.
 *
 * @author Andrea Aime - TOPP
 */
public class NativeElementHandler implements TransactionElementHandler {
    /** logger */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.wfs");

    /** Empty array of QNames */
    protected static final QName[] EMPTY_QNAMES = new QName[0];

    public NativeElementHandler() {}

    public void checkValidity(TransactionElement element, Map featureTypeInfos)
            throws WFSTransactionException {

        Native nativ = (Native) element;
        if (!nativ.isSafeToIgnore()) {
            throw new WFSTransactionException(
                    "Native element:"
                            + nativ.getVendorId()
                            + " unsupported but marked as"
                            + " unsafe to ignore",
                    "InvalidParameterValue");
        }
    }

    public void execute(
            TransactionElement element,
            TransactionRequest request,
            Map featureSources,
            TransactionResponse response,
            TransactionListener listener)
            throws WFSTransactionException {
        // nothing to do, we just ignore if possible
    }

    public Class getElementClass() {
        return Native.class;
    }

    /**
     * @return an empty array.
     * @see TransactionElementHandler#getTypeNames(TransactionRequest, TransactionElement)
     */
    public QName[] getTypeNames(TransactionRequest request, TransactionElement element)
            throws WFSTransactionException {
        // we don't handle this
        return EMPTY_QNAMES;
    }
}
