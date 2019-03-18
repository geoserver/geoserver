/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.exception;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import javax.servlet.http.HttpServletResponse;
import net.opengis.ows20.ExceptionReportType;
import net.opengis.ows20.ExceptionType;
import net.opengis.ows20.Ows20Factory;
import org.eclipse.xsd.XSDSchema;
import org.geoserver.ows.Request;
import org.geoserver.ows.ServiceExceptionHandler;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.platform.OWS20Exception.OWSExceptionCode;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.ows.v2_0.OWS;
import org.geotools.ows.v2_0.OWSConfiguration;
import org.geotools.xsd.Encoder;

/**
 * A default implementation of {@link ServiceExceptionHandler} which outputs as service exception in
 * a <code>ows:ExceptionReport</code> document.
 *
 * <p>This service exception handler will generate an OWS exception report, see {@linkplain
 * "http://schemas.opengis.net/ows/1.1.0/owsExceptionReport.xsd"}.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class OWS20ServiceExceptionHandler extends ServiceExceptionHandler {
    /**
     * A flag controlling whether the http return code should always be 200. It is required when
     * running CITE tests for WCS2.0, but breaks OWS2 and WCS2 standards.
     */
    public static boolean force200httpcode = Boolean.getBoolean("force200");

    /**
     * verbose exception flag controlling whether the exception stack trace will be included in the
     * encoded ows exception report
     */
    protected boolean verboseExceptions = false;

    /** flag that controls what version to use in the ows exception report. */
    protected boolean useServiceVersion = false;

    /** Constructor to be called if the exception is not for a particular service. */
    public OWS20ServiceExceptionHandler() {
        super(Collections.EMPTY_LIST);
    }

    /**
     * Constructor to be called if the exception is for a particular service.
     *
     * @param services List of services this handler handles exceptions for.
     */
    public OWS20ServiceExceptionHandler(List services) {
        super(services);
    }

    /**
     * Constructor to be called if the exception is for a particular service.
     *
     * @param service The service this handler handles exceptions for.
     */
    public OWS20ServiceExceptionHandler(Service service) {
        super(Arrays.asList(service));
    }

    /** Writes out an OWS ExceptionReport document. */
    public void handleServiceException(ServiceException exception, Request request) {
        LOGGER.warning("OWS20SEH: handling " + exception);

        String version = null;
        if (useServiceVersion && request.getServiceDescriptor() != null) {
            version = request.getServiceDescriptor().getVersion().toString();
        }

        ExceptionReportType report = exceptionReport(exception, verboseExceptions, version);

        HttpServletResponse response = request.getHttpResponse();
        if (!request.isSOAP()) {
            // there will already be a SOAP mime type
            response.setContentType("application/xml");
        }

        OWS20Exception ows2ex;
        if (exception instanceof OWS20Exception) {
            ows2ex = (OWS20Exception) exception;
        } else if (exception.getCause() != null && exception.getCause() instanceof OWS20Exception) {
            ows2ex = (OWS20Exception) exception.getCause();
        } else {
            // try to infer if it's a standard exception
            String code = exception.getCode();
            OWSExceptionCode exCode = OWS20Exception.OWSExceptionCode.getByCode(code);
            if (exCode != null) {
                ows2ex =
                        new OWS20Exception(
                                exception.getMessage(), exception, exCode, exception.getLocator());
            } else {
                ows2ex =
                        new OWS20Exception(
                                exception.getMessage(),
                                exception,
                                OWSExceptionCode.NoApplicableCode,
                                exception.getLocator());
            }
        }

        // response.setCharacterEncoding( "UTF-8" );
        OWSConfiguration configuration = new OWSConfiguration();

        XSDSchema result;
        try {
            result = configuration.getXSD().getSchema();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Encoder encoder = new Encoder(configuration, result);
        encoder.setIndenting(true);
        encoder.setIndentSize(2);
        encoder.setLineWidth(60);
        encoder.setOmitXMLDeclaration(request.isSOAP());

        // String schemaLocation = buildSchemaURL(baseURL(request.getHttpRequest()),
        // "ows/2.0/owsAll.xsd");
        String schemaLocation = "http://schemas.opengis.net/ows/2.0/owsExceptionReport.xsd";
        encoder.setSchemaLocation(OWS.NAMESPACE, schemaLocation);

        try {

            //            if(ows2ex != null) {
            if (ows2ex.getHttpCode() != null) {
                response.setStatus(ows2ex.getHttpCode());
            }

            if (force200httpcode) {
                response.setStatus(200);
            }

            encoder.encode(report, OWS.ExceptionReport, response.getOutputStream());
            //            }

        } catch (Exception ex) {
            // throw new RuntimeException(ex);
            // Hmm, not much we can do here.  I guess log the fact that we couldn't write out the
            // exception and be done with it...
            LOGGER.log(
                    Level.INFO,
                    "Problem writing exception information back to calling client:",
                    ex);
        } finally {
            try {
                response.getOutputStream().flush();
            } catch (IOException ioe) {
            }
        }
    }

    /**
     * Flag that controls what version to use in the ows exception report.
     *
     * <p>Setting to true will cause the service version to be used rather than the ows spec
     * version.
     */
    public void setUseServiceVersion(boolean useServiceVersion) {
        this.useServiceVersion = useServiceVersion;
    }

    public static ExceptionReportType exceptionReport(
            ServiceException exception, boolean verboseExceptions, String version) {

        ExceptionType e = Ows20Factory.eINSTANCE.createExceptionType();

        if (exception.getCode() != null) {
            e.setExceptionCode(exception.getCode());
        } else {
            // set a default
            e.setExceptionCode("NoApplicableCode");
        }

        e.setLocator(exception.getLocator());

        // add the message
        StringBuffer sb = new StringBuffer();
        OwsUtils.dumpExceptionMessages(exception, sb, true);
        if (exception.getExceptionText() != null && !exception.getExceptionText().isEmpty()) {
            sb.append("\n");
            sb.append(exception.getExceptionText()); // check this
        }
        //        e.getExceptionText().add(sb.toString());
        //        e.getExceptionText().addAll(exception.getExceptionText());

        if (verboseExceptions) {
            // add the entire stack trace
            // exception.
            sb.append("\nDetails:\n");
            ByteArrayOutputStream trace = new ByteArrayOutputStream();
            exception.printStackTrace(new PrintStream(trace));
            sb.append(new String(trace.toByteArray()));
        }

        e.getExceptionText().add(sb.toString());

        ExceptionReportType report = Ows20Factory.eINSTANCE.createExceptionReportType();

        version = version != null ? version : "2.0.0";
        report.setVersion(version);
        report.getException().add(e);

        return report;
    }
}
