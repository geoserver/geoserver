/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import static org.geoserver.ows.util.ResponseUtils.baseURL;
import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.logging.Level;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Request;
import org.geoserver.ows.ServiceExceptionHandler;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.ows.xml.v1_0.OWS;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

/**
 * An implementation of {@link ServiceExceptionHandler} which outputs as service exception in a
 * <code>ServiceExceptionReport</code> document.
 *
 * <p>This handler is referred to as "legacy" as newer services move to the ows style exception
 * report. See {@link org.geoserver.ows.OWS10ServiceExceptionHandler}.
 *
 * <p>
 *
 * <h3>Version</h3>
 *
 * By default this exception handler will output a <code>ServiceExceptionReport</code> which is of
 * version <code>1.2.0</code>. This may be overriden with {@link #setVersion(String)}.
 *
 * <p>
 *
 * <h3>DTD and Schema</h3>
 *
 * By default, no DTD or XML Schema reference will be included in the document. The methods {@link
 * #setDTDLocation(String)} and {@link #setSchemaLocation(String)} can be used to override this
 * behaviour. Only one of these methods should be set per instance of this class.
 *
 * <p>The supplied value should be relative, and will be appended to the result of {@link
 * OWS#getSchemaBaseURL()}.
 *
 * <p>
 *
 * <h3>Content Type</h3>
 *
 * The default content type for the created document is <code>text/xml</code>, this can be
 * overridden with {@link #setContentType(String)}.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class LegacyServiceExceptionHandler extends ServiceExceptionHandler {
    /** the version of the service exceptoin report. */
    protected String version = "1.2.0";

    /** Location of document type defintion for document */
    protected String dtdLocation = null;

    /** Location of schema for document. */
    protected String schemaLocation = null;

    /** The content type of the produced document */
    protected String contentType = "text/xml";

    /** The central configuration, used to decide whether to dump a verbose stack trace, or not */
    protected GeoServer geoServer;

    public LegacyServiceExceptionHandler(List services, GeoServer geoServer) {
        super(services);
        this.geoServer = geoServer;
    }

    public LegacyServiceExceptionHandler(Service service, GeoServer geoServer) {
        super(service);
        this.geoServer = geoServer;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setDTDLocation(String dtd) {
        this.dtdLocation = dtd;
    }

    public void setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void handleServiceException(ServiceException exception, Request request) {
        String tab = "   ";
        StringBuffer sb = new StringBuffer();

        // xml header TODO: should the encoding the server default?
        sb.append("<?xml version=\"1.0\"");
        sb.append(" encoding=\"UTF-8\"");

        if (dtdLocation != null) {
            sb.append(" standalone=\"no\"");
        }

        sb.append("?>");

        // dtd location
        if (dtdLocation != null) {
            String fullDtdLocation = buildSchemaURL(baseURL(request.getHttpRequest()), dtdLocation);
            sb.append("<!DOCTYPE ServiceExceptionReport SYSTEM \"" + fullDtdLocation + "\"> ");
        }

        // root element
        sb.append("<ServiceExceptionReport version=\"" + version + "\" ");

        // xml schema location
        if ((schemaLocation != null) && (dtdLocation == null)) {
            String fullSchemaLocation =
                    buildSchemaURL(baseURL(request.getHttpRequest()), schemaLocation);

            sb.append("xmlns=\"http://www.opengis.net/ogc\" ");
            sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
            sb.append(
                    "xsi:schemaLocation=\"http://www.opengis.net/ogc " + fullSchemaLocation + "\"");
        }

        sb.append(">");

        // write out the service exception
        sb.append(tab + "<ServiceException");

        // exception code
        if ((exception.getCode() != null) && !exception.getCode().equals("")) {
            sb.append(" code=\"" + ResponseUtils.encodeXML(exception.getCode()) + "\"");
        }

        // exception locator
        if ((exception.getLocator() != null) && !exception.getLocator().equals("")) {
            sb.append(" locator=\"" + ResponseUtils.encodeXML(exception.getLocator()) + "\"");
        }

        sb.append(">");

        // message
        if ((exception.getMessage() != null)) {
            sb.append("\n" + tab + tab);
            OwsUtils.dumpExceptionMessages(exception, sb, true);

            if (geoServer.getSettings().isVerboseExceptions()) {
                ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
                exception.printStackTrace(new PrintStream(stackTrace));

                sb.append("\nDetails:\n");
                sb.append(ResponseUtils.encodeXML(new String(stackTrace.toByteArray())));
            }
        }

        sb.append("\n</ServiceException>");
        sb.append("</ServiceExceptionReport>");

        HttpServletResponse response = request.getHttpResponse();
        response.setContentType(contentType);

        // TODO: server encoding?
        response.setCharacterEncoding("UTF-8");

        try {
            response.getOutputStream().write(sb.toString().getBytes());
            response.getOutputStream().flush();
        } catch (IOException e) {
            // throw new RuntimeException(e);
            // Hmm, not much we can do here.  I guess log the fact that we couldn't write out the
            // exception and be done with it...
            LOGGER.log(
                    Level.INFO, "Problem writing exception information back to calling client:", e);
        }
    }
}
