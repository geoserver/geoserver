/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.config.GeoServer;
import org.vfny.geoserver.util.Requests;
import org.vfny.geoserver.util.ResponseUtils;

/**
 * Represents a standard OGC service exception. Able to turn itself into the proper xml response.
 *
 * <p>JG - here is my guess on what the parameters do:
 *
 * <pre><code>
 * [?xml version="1.0" ?
 * [ServiceExceptionReport
 *    version="1.2.0"
 *    xmlns="http://www.opengis.net/ogc"
 *    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *    xsi:schemaLocation="http://www.opengis.net/ogc <i>SchemaBaseUrl</i> wfs/1.0.0/OGC-exception.xsd"]
 *   [ServiceException code="<i>code</i>"
 *                     locator="<i>locator</i>"]
 *     </i>preMessage<i>:<i>getMessage()</i>
 *     <i>stack trace</i>
 *   [/ServiceException]
 * [/ServiceExceptionReport]
 * </code></pre>
 *
 * <p>Where:
 *
 * <ul>
 *   <li>code: is a diagnostic code
 *   <li>locator: is the java class that caused the problem
 *   <li>preMessage: is your chance to place things in user terms
 *   <li>message: is the exception message
 *   <li>stack trace: is the exception strack trace
 * </ul>
 *
 * <p>Java Exception have recently developed the ability to contain other exceptions. By calling
 * initCause on your ServiceConfig Exception you can get the real exception included in the
 * stacktrace above.
 *
 * @author Gabriel Rold?n
 * @author Chris Holmes
 * @task REVISIT: Take a request in the constructor? This would make it so we do not have to rely on
 *     schemas.opengis.net being available, as it will just reference the geoserver instance that
 *     created it. But to do this we need the request, as that's how we figure out the baseUrl.
 *     Would probably not be that hard to get the request included, and would lead to better error
 *     reporting...
 * @deprecated use {@link org.geoserver.platform.ServiceException}
 */
public class ServiceException extends org.geoserver.platform.ServiceException {
    /** Class logger */
    private static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.responses");

    /** message inserted by GeoServer as to what it thinks happened */
    protected String preMessage = new String();

    /** full classpath of originating GeoServer class */
    protected String locator = new String();

    /** Empty constructor. */
    public ServiceException() {
        super((String) null);
    }

    /**
     * Empty constructor.
     *
     * @param message The message for the .
     */
    public ServiceException(String message) {
        super(message, (String) null);
    }

    /**
     * This should be the most used entry point.
     *
     * @param message User message
     * @param cause The origional exception that caused failure
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause, null);
    }

    /**
     * Empty constructor.
     *
     * @param e The message for the .
     */
    public ServiceException(Throwable e) {
        super(e, null);
    }

    /**
     * Empty constructor.
     *
     * @param message The message for the .
     * @param locator The message for the .
     */
    public ServiceException(String message, String locator) {
        super(message);

        this.locator = locator;
    }

    public ServiceException(String message, String code, String locator) {
        super(message, code, locator);
        this.locator = locator;
    }

    /**
     * DOCUMENT ME!
     *
     * @param e The message for the .
     * @param preMessage The message to tack on the front.
     * @param locator The message for the .
     */
    public ServiceException(Throwable e, String preMessage, String locator) {
        this(e);

        this.preMessage = preMessage;

        this.locator = locator;
    }

    public ServiceException(ServiceException e) {
        super(e.getMessage(), e.getCause(), e.getLocator());
        this.preMessage = e.preMessage;
    }

    /**
     * DOCUMENT ME!
     *
     * @param testString DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    protected boolean isEmpty(String testString) {
        return (testString == null) || testString.equals("");
    }

    public String getLocator() {
        return locator;
    }

    /**
     * DOCUMENT ME!
     *
     * @param printStackTrace DOCUMENT ME!
     * @return DOCUMENT ME!
     */

    // public String getXmlResponse() {
    //	return getXmlResponse(true);
    // }

    /**
     * gets the message, encoding it with the proper escaped xml characters. If requested it prints
     * the whole stack trace in the response.
     *
     * @param printStackTrace set to <tt>true</tt> if the full stack trace should be returned to
     *     client apps.
     * @return The message of this error, with xml escapes.
     * @task REVISIT: The stack trace printing is not that efficient, but it should be relatively
     *     small. Once we convert errors to print directly to the servlet output stream we can make
     *     it faster.
     */
    public String getXmlMessage(boolean printStackTrace) {
        String indent = "   ";
        StringBuffer mesg = new StringBuffer();

        // this distinction no longer so much applies, as we don't always
        // throw Service exceptions for all expected exceptions.
        // if (!isEmpty(this.preMessage)) {
        //    mesg.append(this.preMessage + ": ");
        // }
        // mesg.append(ResponseUtils.encodeXML(this.getMessage()) + "\n");
        //        if (printStackTrace) {
        if (printStackTrace) {
            mesg.append(createStackTrace());
        } else {
            mesg.append(this.getMessage());
        }

        return ResponseUtils.encodeXML(mesg.toString());
    }

    private String createStackTrace() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);
        Throwable cause = getCause();

        if (cause == null) {
            this.printStackTrace(writer);
        } else {
            cause.printStackTrace(writer);
        }

        writer.flush();

        return baos.toString();
    }

    /**
     * Return request type.
     *
     * @param printStackTrace whether the stack trace should be included.
     * @param request DOCUMENT ME!
     * @return The ServiceExceptionReport of this error.
     * @task REVISIT: Our error handling should actually have knowledge of the app configuration, so
     *     that we can set the ogc error report to validate right (reference our own schema), and to
     *     put the correct mime type here.
     */
    public String getXmlResponse(
            boolean printStackTrace, HttpServletRequest request, GeoServer geoserver) {
        // Perhaps not the best place to do this, but it's by far the best place to ensure
        // that all logged errors get recorded in the same way, as there all must return
        // xml responses.
        LOGGER.warning(
                "encountered error: " + getMessage() + "\nStackTrace: " + createStackTrace());

        String indent = "   ";

        StringBuffer returnXml = new StringBuffer("<?xml version=\"1.0\" ?>\n");

        returnXml.append("<ServiceExceptionReport\n");

        returnXml.append(indent + "version=\"1.2.0\"\n");

        returnXml.append(indent + "xmlns=\"http://www.opengis.net/ogc\"\n");

        returnXml.append(indent + "xmlns:xsi=\"http://www.w3.org/2001/" + "XMLSchema-instance\"\n");

        returnXml.append(indent);

        returnXml.append("xsi:schemaLocation=\"http://www.opengis.net/ogc ");

        returnXml.append(
                Requests.getSchemaBaseUrl(request, geoserver)
                        + "/wfs/1.0.0/OGC-exception.xsd\">\n");

        // REVISIT: handle multiple service exceptions?  must refactor class.
        returnXml.append(indent + "<ServiceException");

        if (!isEmpty(getCode())) {
            returnXml.append(" code=\"" + getCode() + "\"");
        }

        if (!isEmpty(this.locator)) {
            returnXml.append(" locator=\"" + this.locator + "\"");
        }

        returnXml.append(">\n" + indent + indent);
        returnXml.append(getXmlMessage(printStackTrace));

        returnXml.append(indent + "</ServiceException>\n");

        returnXml.append("</ServiceExceptionReport>");

        LOGGER.fine("return wfs exception is " + returnXml);

        return returnXml.toString();
    }

    /**
     * Returns the mime type that should be exposed to the client when sending the exception
     * message.
     *
     * <p>Defaults to <code>geoserver.getMimeType()</code>
     */
    public String getMimeType(GeoServer geoserver) {
        return "text/xml; charset=" + geoserver.getSettings().getCharset();
    }
}
