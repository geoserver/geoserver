/* Copyright (c) 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs2_0.exception;

import org.geoserver.platform.OWS20Exception;

/**
 * This defines an exception that can be turned into a valid xml service exception that wcs clients
 * will expect.
 *
 * All errors should be wrapped in this before returning to clients.
 * 
 * @author Emanuele Tajariol, GeoSolutions SAS
 * @author Simone Giannecchini, GeoSolutionS SAS
 */
public class WCS20Exception extends OWS20Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = -6110652531274829497L;

	public static class WCSExceptionCode extends OWS20Exception.OWSExceptionCode {

        public final static OWSExceptionCode NoSuchCoverage = new WCSExceptionCode("NoSuchCoverage", 404);
        public final static OWSExceptionCode EmptyCoverageIdList = new WCSExceptionCode("emptyCoverageIdList", 404);
        public final static OWSExceptionCode InvalidEncodingSyntax = new WCSExceptionCode("InvalidEncodingSyntax", 400);
        public final static OWSExceptionCode InvalidScaleFactor = new WCSExceptionCode("InvalidScaleFactor", 404);
        public final static OWSExceptionCode InvalidExtent = new WCSExceptionCode("InvalidExtent", 404);
        public final static OWSExceptionCode ScalingAxisUndefined = new WCSExceptionCode("ScalingAxisUndefined", 404);

        protected WCSExceptionCode(String exceptionCode, Integer httpCode) {
            super(exceptionCode, httpCode);
        }        
    }

    public WCS20Exception(String message) {
        super(message);
    }

    public WCS20Exception(Throwable e) {
        super(e);
    }

    public WCS20Exception(String message, String locator) {
        super(message, locator);
    }

    public WCS20Exception(String message, OWS20Exception.OWSExceptionCode code, String locator) {
        super(message, code, locator);
    }

    public WCS20Exception(String message, Throwable cause) {
        super(message, cause);
    }


}
