/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs2_0.exception;

import org.geoserver.platform.OWS20Exception;

/**
 * This defines an exception that can be turned into a valid xml service exception that wcs clients
 * will expect. All errors should be wrapped in this before returning to clients.
 * 
 * @author Alessio Fabiani (alessio.fabiani@gmail.com)
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Emanuele Tajariol, GeoSolutions SAS
 */
public class WCS20Exception extends OWS20Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = -6110652531274829497L;

	public enum WCSExceptionCode {
//        MissingParameterValue,
//        InvalidParameterValue,
//        NoApplicableCode,
//        UnsupportedCombination,
//        NotEnoughStorage,
//        InvalidUpdateSequence,
//        CurrentUpdateSequence,
        
        NoSuchCoverage(404), 
        emptyCoverageIdList(404);

        private final Integer httpCode;

        private WCSExceptionCode() {
            this.httpCode = null;
        }

        private WCSExceptionCode(Integer httpCode) {
            this.httpCode = httpCode;
        }

        public Integer getHttpCode() {
            return httpCode;
        }
        
    }

    /**
     * The fixed MIME type of a WCS exception.
     */
    private static final String SE_XML = "application/vnd.ogc.se_xml";

    /**
     * Message constructor.
     * 
     * @param message
     *            The message for the .
     */
    public WCS20Exception(String message) {
        super(message);
    }

    /**
     * Throwable constructor.
     * 
     * @param e
     *            The message for the .
     */
    public WCS20Exception(Throwable e) {
        super(e);
    }

    /**
     * Message Locator constructor.
     * 
     * @param message
     *            The message for the .
     * @param locator
     *            The java class that caused the problem
     */
    public WCS20Exception(String message, String locator) {
        super(message, locator);
    }

    public WCS20Exception(String message, WCSExceptionCode code, String locator) {
        super(message, code.name(), locator);
        setHttpCode(code.getHttpCode());
    }

    public WCS20Exception(String message, OWS20Exception.OWSExceptionCode code, String locator) {
        super(message, code.name(), locator);
        setHttpCode(code.getHttpCode());
    }

    public WCS20Exception(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * DOCUMENT ME!
     * 
     * @param e
     *            The cause of failure
     * @param preMessage
     *            The message to tack on the front.
     * @param locator
     *            The java class that caused the problem
     */
    public WCS20Exception(Throwable e, String preMessage, String locator) {
        super(e, preMessage, locator);
    }

}
