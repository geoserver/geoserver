/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo.Version;
import org.geoserver.wfs.request.RequestObject;

/** WFS application specific exception. */
public class WFSException extends ServiceException {
    public static final String NOT_FOUND = "NotFound";
    public static final String OPERATION_PROCESSING_FAILED = "OperationProcessingFailed";
    public static final String INVALID_VALUE = "InvalidValue";
    public static final String DUPLICATE_STORED_QUERY_ID_VALUE = "DuplicateStoredQueryIdValue";
    public static final String LOCK_HAS_EXPIRED = "LockHasExpired";
    public static final String OPERATION_PARSING_FAILED = "OperationParsingFailed";
    public static final String INVALID_LOCK_ID = "InvalidLockId";
    public static final String CANNOT_LOCK_ALL_FEATURES = "CannotLockAllFeatures";

    public enum Code {
        OperationProcessingFailed
    };

    public WFSException(RequestObject request, String message) {
        this(message);
        init(request);
    }

    public WFSException(RequestObject request, String message, String code) {
        this(message, code);
        init(request);
    }

    public WFSException(RequestObject request, String message, Throwable cause, String code) {
        this(message, cause, code);
        init(request);
    }

    public WFSException(RequestObject request, String message, Throwable cause) {
        this(message, cause);
        init(request);
    }

    public WFSException(RequestObject request, Throwable cause, String code) {
        this(cause, code);
        init(request);
    }

    public WFSException(RequestObject request, Throwable cause) {
        this(cause);
        init(request);
    }

    public WFSException(EObject request, String message) {
        this(message);
        init(request);
    }

    public WFSException(EObject request, String message, String code) {
        this(message, code);
        init(request);
    }

    public WFSException(EObject request, String message, Throwable cause, String code) {
        this(message, cause, code);
        init(request);
    }

    public WFSException(EObject request, String message, Throwable cause) {
        this(message, cause);
        init(request);
    }

    public WFSException(EObject request, Throwable cause, String code) {
        this(cause, code);
        init(request);
    }

    public WFSException(EObject request, Throwable cause) {
        this(cause);
        init(request);
    }

    public WFSException init(Object request) {
        if (request != null) {
            // wfs 2.0 has more requirements for exception codes and handles
            if (OwsUtils.has(request, "version")) {
                Object ver = OwsUtils.get(request, "version");
                Version version = Version.negotiate(ver != null ? ver.toString() : null);
                if (version != null && version.compareTo(Version.V_20) < 0) {
                    return this; // not 2.0
                }
            }
            if (locator == null && OwsUtils.has(request, "handle")) {
                // check the request object
                locator = (String) OwsUtils.get(request, "handle");
            }

            if (locator == null) {
                locator = determineDefaultLocator(request);
            }

            if (code == null) {
                code = Code.OperationProcessingFailed.name();
            }
        }
        return this;
    }

    private String determineDefaultLocator(Object request) {
        // default to name of operation, use the request object class name to determine the
        // operation
        String className = request.getClass().getSimpleName();
        if (request instanceof RequestObject) {
            // request object adapter

            className = request.getClass().getSuperclass().getSimpleName();
            if (className.endsWith("Request")) {
                return className.substring(0, className.length() - "Request".length());
            }
            RequestObject requestObject = (RequestObject) request;
            EObject adaptee = requestObject.getAdaptee();
            if (adaptee.getClass().getName().contains("wfs20")) {
                // locator only for WFS20, implementation package is currently net.opengis.wfs20
                return className;
            }
            return null;
        } else if (className.endsWith("TypeImpl")) {
            // underlying emf request object
            return className.substring(0, className.length() - "TypeImpl".length());
        }
        return null;
    }

    public WFSException(String message) {
        super(message);
    }

    public WFSException(Throwable cause) {
        super(cause);
    }

    public WFSException(String message, String code, String locator) {
        super(message, code, locator);
    }

    public WFSException(String message, String code) {
        super(message, code);
    }

    public WFSException(String message, Throwable cause, String code, String locator) {
        super(message, cause, code, locator);
    }

    public WFSException(String message, Throwable cause, String code) {
        super(message, cause, code);
    }

    public WFSException(String message, Throwable cause) {
        super(message, cause);
    }

    public WFSException(Throwable cause, String code, String locator) {
        super(cause, code, locator);
    }

    public WFSException(Throwable cause, String code) {
        super(cause, code);
    }
}
