/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.servlets;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.ServiceStrategy;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.vfny.geoserver.Response;
import org.vfny.geoserver.util.requests.readers.KvpRequestReader;
import org.vfny.geoserver.util.requests.readers.XmlRequestReader;


/**
 * Represents a service that all others extend from.  Subclasses should provide
 * response and exception handlers as appropriate.
 *
 * <p>
 * It is <b>really</b> important to adhere to the following workflow:
 *
 * <ol>
 * <li>
 * get a Request reader
 * </li>
 * <li>
 * ask the Request Reader for the Request object
 * </li>
 * <li>
 * Provide the resulting Request with the ServletRequest that generated it
 * </li>
 * <li>
 * get the appropiate ResponseHandler
 * </li>
 * <li>
 * ask it to execute the Request
 * </li>
 * <li>
 * set the response content type
 * </li>
 * <li>
 * write to the http response's output stream
 * </li>
 * <li>
 * pending - call Response cleanup
 * </li>
 * </ol>
 * </p>
 *
 * <p>
 * If anything goes wrong a ServiceException can be thrown and will be written
 * to the output stream instead.
 * </p>
 *
 * <p>
 * This is because we have to be sure that no exception have been produced
 * before setting the response's content type, so we can set the exception
 * specific content type; and that Response.getContentType is called AFTER
 * Response.execute, since the MIME type can depend on any request parameter
 * or another kind of desission making during the execute process. (i.e.
 * FORMAT in WMS GetMap)
 * </p>
 *
 * <p>
 * TODO: We need to call Response.abort() if anything goes wrong to allow the
 * Response a chance to cleanup after itself.
 * </p>
 *
 * @author Gabriel Rold?n
 * @author Chris Holmes
 * @author Jody Garnett, Refractions Research
 * @version $Id$
 * @deprecated
 */
public abstract class AbstractService extends HttpServlet implements ApplicationContextAware {
    /** Class logger */
    protected static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.servlets");

    /**
     * Servivce group (maps to 'SERVICE' parameter in OGC service urls)
     */
    String service;

    /**
     * Request type (maps to 'REQUEST' parameter in OGC service urls)
     */
    String request;

    /**
     * Application context used to look up "Services"
     */
    WebApplicationContext context;

    /**
     * Reference to the global geoserver instnace.
     */
    GeoServer geoServer;

    /**
     * Reference to the catalog.
     */
    Catalog catalog;

    /**
     * Id of the service strategy to use.
     */
    String serviceStrategy;

    /**
     * buffer size to use when PARTIAL-BUFFER is being used
     */
    int partialBufferSize;

    /**
     * Cached service strategy object
     */

    //    ServiceStrategy strategy;

    /**
     * Reference to the service
     */
    ServiceInfo serviceRef;
    private String kvpString;

    //    /** DOCUMENT ME!  */
    //    protected HttpServletRequest curRequest;

    /**
     * Constructor for abstract service.
     *
     * @param service The service group the service falls into (WFS,WMS,...)
     * @param request The service being requested (GetCapabilities, GetMap, ...)
     * @param serviceRef The global service this "servlet" falls into
     */
    public AbstractService(String service, String request, ServiceInfo serviceRef) {
        this.service = service;
        this.request = request;
        this.serviceRef = serviceRef;
    }

    /**
     * @return Returns the "service group" that this service falls into.
     */
    public String getService() {
        return service;
    }

    /**
     * @return Returns the "request" this service maps to.
     */
    public String getRequest() {
        return request;
    }

    /**
     * Sets a refeference to the global service instance.
     */
    public void setServiceRef(ServiceInfo serviceRef) {
        this.serviceRef = serviceRef;
    }

    /**
     * @return The reference to the global service instance.
     */
    public ServiceInfo getServiceRef() {
        return serviceRef;
    }

    /**
     * Sets the application context.
     * <p>
     * Used to process the {@link Service} extension point.
     * </p>
     */
    public void setApplicationContext(ApplicationContext context)
        throws BeansException {
        this.context = (WebApplicationContext) context;
    }

    /**
     * @return The application context.
     */
    public WebApplicationContext getApplicationContext() {
        return context;
    }

    /**
     * Sets the reference to the global geoserver instance.
     */
    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    /**
     * @return the reference to the global geoserver instance.
     */
    public GeoServer getGeoServer() {
        return geoServer;
    }

    /**
     * @return The reference to the global catalog instance.
     */
    public Catalog getCatalog() {
        return catalog;
    }

    /**
     * Sets the reference to the global catalog instance.
     *
     */
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    /**
     * @return The id used to identify the service strategy to be used.
     * @see ServiceStrategy#getId()
     */
    public String getServiceStrategy() {
        return serviceStrategy;
    }

    /**
     * Sets the id used to identify the service strategy to be used.
     */
    public void setServiceStrategy(String serviceStrategy) {
        this.serviceStrategy = serviceStrategy;
    }

    /**
     * Determines if the service is enabled.
     * <p>
     * Subclass should override this method if the service can be turned on/off.
     * This implementation returns <code>true</code>
     * </p>
     */
    protected boolean isServiceEnabled(HttpServletRequest req) {
        return true;
    }

    /**
     * Override and use spring set servlet context.
     */
    public ServletContext getServletContext() {
        //override and use spring 
        return ((WebApplicationContext) context).getServletContext();
    }

//    /**
//     * DOCUMENT ME!
//     *
//     * @param request DOCUMENT ME!
//     * @param response DOCUMENT ME!
//     *
//     * @throws ServletException DOCUMENT ME!
//     * @throws IOException DOCUMENT ME!
//     */
//    public void doGet(HttpServletRequest request, HttpServletResponse response)
//        throws ServletException, IOException {
//        // implements the main request/response logic
//        // this.curRequest = request;
//        Request serviceRequest = null;
//
//        if (!isServiceEnabled(request)) {
//            sendDisabledServiceError(response);
//
//            return;
//        }
//
//        try {
//            Map requestParams = new HashMap();
//            String qString = ((this.kvpString != null) ? this.kvpString : request.getQueryString());
//            LOGGER.fine("reading request: " + qString);
//
//            if (this.kvpString != null) {
//                requestParams = KvpRequestReader.parseKvpSet(qString);
//            } else {
//                String paramName;
//                String paramValue;
//
//                for (Enumeration pnames = request.getParameterNames(); pnames.hasMoreElements();) {
//                    paramName = (String) pnames.nextElement();
//                    paramValue = request.getParameter(paramName);
//                    requestParams.put(paramName.toUpperCase(), paramValue);
//                }
//            }
//
//            KvpRequestReader requestReader = getKvpReader(requestParams);
//
//            serviceRequest = requestReader.getRequest(request);
//            LOGGER.finer("serviceRequest provided with HttpServletRequest: " + request);
//
//            //serviceRequest.setHttpServletRequest(request);
//        } catch (ServiceException se) {
//            sendError(request, response, se);
//
//            return;
//        } catch (Throwable e) {
//            sendError(request, response, e);
//
//            return;
//        } finally {
//            this.kvpString = null;
//        }
//
//        doService(request, response, serviceRequest);
//    }

    /**
     * Sends the standard disabled service error message (a 503 error followed by an english description).
     * @param response
     * @throws IOException
     */
    protected void sendDisabledServiceError(HttpServletResponse response)
        throws IOException {
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
            getService() + " service is not enabled. " + "You can enable it in the web admin tool.");
    }

//    /**
//     * Performs the post method.  Simply passes itself on to the three argument
//     * doPost method, with null for the reader, because the
//     * request.getReader() will not have been used if this servlet is called
//     * directly.
//     *
//     * @param request DOCUMENT ME!
//     * @param response DOCUMENT ME!
//     *
//     * @throws ServletException DOCUMENT ME!
//     * @throws IOException DOCUMENT ME!
//     */
//    public void doPost(HttpServletRequest request, HttpServletResponse response)
//        throws ServletException, IOException {
//        doPost(request, response, null);
//    }

//    /**
//     * Performs the post method.  Gets the appropriate xml reader and
//     * determines the request from that, and then passes the request on to
//     * doService.
//     *
//     * @param request The request made.
//     * @param response The response to be returned.
//     * @param requestXml A reader of the xml to be read.  This is only used by
//     *        the dispatcher, everyone else should just pass in null.  This is
//     *        needed because afaik HttpServletRequest.getReader() can not be
//     *        used twice.  So in a dispatched case we write it to a temp file,
//     *        which we can then read in twice.
//     *
//     * @throws ServletException DOCUMENT ME!
//     * @throws IOException DOCUMENT ME!
//     */
//    public void doPost(HttpServletRequest request, HttpServletResponse response, Reader requestXml)
//        throws ServletException, IOException {
//        //        this.curRequest = request;
//        Request serviceRequest = null;
//
//        //TODO: This isn't a proper ogc service response.
//        if (!isServiceEnabled(request)) {
//            sendDisabledServiceError(response);
//
//            return;
//        }
//
//        // implements the main request/response logic
//        try {
//            XmlRequestReader requestReader = getXmlRequestReader();
//
//            //JD: GEOS-323, adding support for character encoding detection
//            // Reader xml = (requestXml != null) ? requestXml : request.getReader();
//            Reader xml;
//
//            if (null != requestXml) {
//                xml = requestXml;
//            } else {
//                /*
//                 * `getCharsetAwareReader` returns a reader which not support
//                 * mark/reset. So it is a good idea to wrap it into BufferedReader.
//                 * In this case the below debug output will work.
//                 */
//                xml = new BufferedReader(XmlCharsetDetector.getCharsetAwareReader(
//                            request.getInputStream()));
//            }
//
//            //JD: GEOS-323
//
//            //DJB: add support for POST loggin
//            if (LOGGER.isLoggable(Level.FINE)) {
//                if (xml.markSupported()) {
//                    // a little protection for large POSTs (ie. updates)
//                    // for FINE, I assume people just want to see the "normal" ones - not the big ones
//                    // for FINER, I assume they would want to see a bit more
//                    // for FINEST, I assume they would want to see even more
//                    int maxChars = 16000;
//
//                    if (LOGGER.isLoggable(Level.FINER)) {
//                        maxChars = 64000;
//                    }
//
//                    if (LOGGER.isLoggable(Level.FINEST)) {
//                        maxChars = 640000; // Bill gates says 640k is good enough for anyone
//                    }
//
//                    xml.mark(maxChars + 1); // +1 so if you read the whole thing you can still reset()
//
//                    char[] buffer = new char[maxChars];
//                    int actualRead = xml.read(buffer);
//                    xml.reset();
//                    LOGGER.fine("------------XML POST START-----------\n"
//                        + new String(buffer, 0, actualRead)
//                        + "\n------------XML POST END-----------");
//
//                    if (actualRead == maxChars) {
//                        LOGGER.fine("------------XML POST REPORT WAS TRUNCATED AT " + maxChars
//                            + " CHARACTERS.  RUN WITH HIGHER LOGGING LEVEL TO SEE MORE");
//                    }
//                } else {
//                    LOGGER.fine(
//                        "ATTEMPTED TO LOG POST XML, BUT WAS PREVENTED BECAUSE markSupported() IS FALSE");
//                }
//            }
//
//            serviceRequest = requestReader.read(xml, request);
//            serviceRequest.setHttpServletRequest(request);
//        } catch (ServiceException se) {
//            sendError(request, response, se);
//
//            return;
//        } catch (Throwable e) {
//            sendError(request, response, e);
//
//            return;
//        }
//
//        doService(request, response, serviceRequest);
//    }

//    /**
//     * Peforms service according to ServiceStrategy.
//     *
//     * <p>
//     * This method has very strict requirements, please see the class
//     * description for the specifics.
//     * </p>
//     *
//     * <p>
//     * It has a lot of try/catch blocks, but they are fairly necessary to
//     * handle things correctly and to avoid as many ugly servlet responses, so
//     * that everything is wrapped correctly.
//     * </p>
//     *
//     * @param request The httpServlet of the request.
//     * @param response The response to be returned.
//     * @param serviceRequest The OGC request to service.
//     *
//     * @throws ServletException if the strategy can't be instantiated
//     */
//    protected void doService(HttpServletRequest request, HttpServletResponse response,
//        Request serviceRequest) throws ServletException {
//        LOGGER.info("handling request: " + serviceRequest);
//
//        if (!isServiceEnabled(request)) {
//            try {
//                sendDisabledServiceError(response);
//            } catch (IOException e) {
//                LOGGER.log(Level.WARNING, "Error writing service unavailable response", e);
//            }
//
//            return;
//        }
//
//        ServiceStrategy strategy = null;
//        Response serviceResponse = null;
//
//        try {
//            strategy = createServiceStrategy();
//            LOGGER.fine("strategy is: " + strategy.getId());
//            serviceResponse = getResponseHandler();
//        } catch (Throwable t) {
//            sendError(request, response, t);
//
//            return;
//        }
//
//        Map services = context.getBeansOfType(Service.class);
//        Service s = null;
//
//        for (Iterator itr = services.entrySet().iterator(); itr.hasNext();) {
//            Map.Entry entry = (Map.Entry) itr.next();
//            String id = (String) entry.getKey();
//            Service service = (Service) entry.getValue();
//
//            if (id.toLowerCase().startsWith(serviceRequest.getService().toLowerCase().trim())) {
//                s = service;
//
//                break;
//            }
//        }
//
//        if (s == null) {
//            String msg = "No service found matching: " + serviceRequest.getService();
//            sendError(request, response, new ServiceException(msg));
//
//            return;
//        }
//
//        try {
//            // execute request
//            LOGGER.finer("executing request");
//            serviceResponse.execute(serviceRequest);
//            LOGGER.finer("execution succeed");
//        } catch (ServiceException serviceException) {
//            LOGGER.warning("service exception while executing request: " + serviceRequest
//                + "\ncause: " + serviceException.getMessage());
//            serviceResponse.abort(s);
//            sendError(request, response, serviceException);
//
//            return;
//        } catch (Throwable t) {
//            //we can safelly send errors here, since we have not touched response yet
//            serviceResponse.abort(s);
//            sendError(request, response, t);
//
//            return;
//        }
//
//        OutputStream strategyOuput = null;
//
//        //obtain the strategy output stream
//        try {
//            LOGGER.finest("getting strategy output");
//            strategyOuput = strategy.getDestination(response);
//            LOGGER.finer("strategy output is: " + strategyOuput.getClass().getName());
//
//            String mimeType = serviceResponse.getContentType(s.getGeoServer());
//            LOGGER.fine("mime type is: " + mimeType);
//            response.setContentType(mimeType);
//
//            String encoding = serviceResponse.getContentEncoding();
//
//            if (encoding != null) {
//                LOGGER.fine("content encoding is: " + encoding);
//                response.setHeader("Content-Encoding", encoding);
//            }
//
//            String disposition = serviceResponse.getContentDisposition();
//
//            if (disposition != null) {
//                LOGGER.fine("content encoding is: " + encoding);
//                response.setHeader("Content-Disposition", disposition);
//            }
//        } catch (SocketException socketException) {
//            LOGGER.fine("it seems that the user has closed the request stream: "
//                + socketException.getMessage());
//
//            // It seems the user has closed the request stream
//            // Apparently this is a "cancel" and will quietly go away
//            //
//            // I will still give strategy and serviceResponse
//            // a chance to clean up
//            //
//            serviceResponse.abort(s);
//            strategy.abort();
//
//            return;
//        } catch (IOException ex) {
//            serviceResponse.abort(s);
//            strategy.abort();
//            sendError(request, response, ex);
//
//            return;
//        }
//
//        try {
//            // gather response
//            serviceResponse.writeTo(strategyOuput);
//            strategyOuput.flush();
//            strategy.flush(response);
//        } catch (java.net.SocketException sockEx) { // user cancel
//            LOGGER.info("Stream abruptly closed by client, response aborted");
//            serviceResponse.abort(s);
//            strategy.abort();
//
//            return;
//        } catch (IOException ioException) { // strategyOutput error
//            response.setHeader("Content-Disposition", ""); // reset it so we get a proper XML error returned
//            LOGGER.info("Stream abruptly closed by client, response aborted");
//            LOGGER.log(Level.FINE, "Error writing out " + ioException.getMessage(), ioException);
//            serviceResponse.abort(s);
//            strategy.abort();
//
//            return;
//        } catch (ServiceException writeToFailure) { // writeTo Failure
//            response.setHeader("Content-Disposition", ""); // reset it so we get a proper XML error returned
//            serviceResponse.abort(s);
//            strategy.abort();
//            sendError(request, response, writeToFailure);
//
//            return;
//        } catch (Throwable help) { // This is an unexpected error(!)
//            response.setHeader("Content-Disposition", ""); // reset it so we get a proper XML error returned
//            help.printStackTrace();
//            serviceResponse.abort(s);
//            strategy.abort();
//            sendError(request, response, help);
//
//            return;
//        }
//
//        // Finish Response
//        // I have moved closing the output stream out here, it was being
//        // done by a few of the ServiceStrategy
//        //
//        // By this time serviceResponse has finished successfully
//        // and strategy is also finished
//        //
//        try {
//            response.getOutputStream().flush();
//            response.getOutputStream().close();
//        } catch (SocketException sockEx) { // user cancel
//            LOGGER.warning("Could not send completed response to user:" + sockEx);
//
//            return;
//        } catch (IOException ioException) {
//            // This is bad, the user did not get the completed response
//            LOGGER.warning("Could not send completed response to user:" + ioException);
//
//            return;
//        }
//
//        LOGGER.info("Service handled");
//    }

    /**
     * Gets the response class that should handle the request of this service.
     * All subclasses must implement.
     * <p>
     * This method is not abstract to support subclasses that use the
     * request-response mechanism.
     * </p>
     *
     * @return The response that the request read by this servlet should be
     *         passed to.
     */
    protected Response getResponseHandler() {
        return null;
    }

    /**
     * This method was added in order to adapt the old style servlet services
     * to the new ows dispatching interface, without having to modify the
     * services themselves.
     *
     * @return A call to {@link #getResponseHandler()}.
     */
    public final Response getResponse() {
        return getResponseHandler();
    }

    /**
     * Gets a reader that will figure out the correct Key Vaule Pairs for this
     * service.
     * <p>
     * Subclasses should override to supply a specific kvp reader. Default
     * implementation returns <code>null</code>
     * </p>
     * @param params A map of the kvp pairs.
     *
     * @return An initialized KVP reader to decode the request.
     */
    protected KvpRequestReader getKvpReader(Map params) {
        return null;
    }

    /**
     * Gets a reader that will handle a posted xml request for this servlet.
     * <p>
     * Subclasses should override to supply a specific xml reader. Default
     * implementation returns <code>null</code>
     * </p>
     * @return An XmlRequestReader appropriate to this service.
     */
    protected XmlRequestReader getXmlRequestReader() {
        return null;
    }

    /**
     * Gets the exception handler for this service.
     *
     * @return The correct ExceptionHandler
     */
    //protected abstract ExceptionHandler getExceptionHandler();

//    /**
//     * Gets the strategy for outputting the response.  This method gets the
//     * strategy from the serviceStrategy param in the web.xml file.  This is
//     * sort of odd behavior, as all other such parameters are set in the
//     * services and catalog xml files, and this param may move there.  But as
//     * it is much  more of a programmer configuration than a user
//     * configuration there is  no rush to move it.
//     *
//     * <p>
//     * Subclasses may choose to override this method in order to get a strategy
//     * more suited to their response.  Currently only Transaction will do
//     * this, since the commit is only called after writeTo, and it often
//     * messes up, so we want to be able to see the error message (SPEED writes
//     * the output directly, so errors in writeTo do not show up.)
//     * </p>
//     *
//     * <p>
//     * Most subclasses should not override, this method will most always return
//     * the SPEED  strategy, since it is the fastest response and should work
//     * fine if everything is well tested.  FILE and BUFFER should be used when
//     * there  are errors in writeTo methods of child classes, set by the
//     * programmer in the web.xml file.
//     * </p>
//     *
//     * @return The service strategy found in the web.xml serviceStrategy
//     *         parameter.   The code that finds this is in the init method
//     *
//     * @throws ServiceException If the service strategy set in #init() is not
//     *         valid.
//     *
//     * @see #init() for the code that sets the serviceStrategy.
//     */
//    protected ServiceStrategy createServiceStrategy() throws ServiceException {
//        // If verbose exceptions is on then lets make sure they actually get the
//        // exception by using the file strategy.
//        ServiceStrategy theStrategy = null;
//
//        if (geoServer.isVerboseExceptions()) {
//            theStrategy = (ServiceStrategy) context.getBean("fileServiceStrategy");
//        } else {
//            if (serviceStrategy == null) {
//                // none set, look up in web applicatino context
//                serviceStrategy = getServletContext().getInitParameter("serviceStrategy");
//            }
//
//            // do a lookup
//            if (serviceStrategy != null) {
//                Map strategies = context.getBeansOfType(ServiceStrategy.class);
//
//                for (Iterator itr = strategies.values().iterator(); itr.hasNext();) {
//                    ServiceStrategy bean = (ServiceStrategy) itr.next();
//
//                    if (bean.getId().equals(serviceStrategy)) {
//                        theStrategy = bean;
//
//                        break;
//                    }
//                }
//            }
//        }
//
//        if (theStrategy == null) {
//            // default to buffer
//            theStrategy = (ServiceStrategy) context.getBean("bufferServiceStrategy");
//        }
//
//        // clone the strategy since at the moment the strategies are marked as singletons
//        // in the web.xml file.
//        try {
//            theStrategy = (ServiceStrategy) theStrategy.clone();
//        } catch (CloneNotSupportedException e) {
//            LOGGER.log(Level.SEVERE,
//                "Programming error found, service strategies should be cloneable, " + e, e);
//            throw new RuntimeException("Found a strategy that does not support cloning...", e);
//        }
//
//        // TODO: this hack should be removed once modules have their own config
//        if (theStrategy instanceof PartialBufferStrategy2) {
//            if (partialBufferSize == 0) {
//                String size = getServletContext().getInitParameter("PARTIAL_BUFFER_STRATEGY_SIZE");
//
//                if (size != null) {
//                    try {
//                        partialBufferSize = Integer.valueOf(size).intValue();
//
//                        if (partialBufferSize <= 0) {
//                            LOGGER.warning("Invalid partial buffer size, defaulting to "
//                                + PartialBufferedOutputStream2.DEFAULT_BUFFER_SIZE + " (was "
//                                + partialBufferSize + ")");
//                            partialBufferSize = 0;
//                        }
//                    } catch (NumberFormatException nfe) {
//                        LOGGER.warning("Invalid partial buffer size, defaulting to "
//                            + PartialBufferedOutputStream2.DEFAULT_BUFFER_SIZE + " (was "
//                            + partialBufferSize + ")");
//                        partialBufferSize = 0;
//                    }
//                }
//            }
//
//            ((PartialBufferStrategy2) theStrategy).setBufferSize(partialBufferSize);
//        }
//
//        return theStrategy;
//    }

//    /**
//     * DOCUMENT ME!
//     *
//     * @return DOCUMENT ME!
//     */
//    protected String getMimeType() {
//        ServletContext servContext = getServletContext();
//
//        try {
//            return ((GeoServer) servContext.getAttribute("GeoServer")).getMimeType();
//        } catch (NullPointerException e) {
//            return "text/xml; charset=" + Charset.forName("UTF-8").name();
//        }
//    }

//    /**
//     * DOCUMENT ME!
//     *
//     * @param response DOCUMENT ME!
//     * @param content DOCUMENT ME!
//     */
//    protected void send(HttpServletResponse response, CharSequence content) {
//        send(response, content, getMimeType());
//    }

    /**
     * DOCUMENT ME!
     *
     * @param response DOCUMENT ME!
     * @param content DOCUMENT ME!
     * @param mimeType DOCUMENT ME!
     */
    protected void send(HttpServletResponse response, CharSequence content, String mimeType) {
        try {
            response.setContentType(mimeType);
            response.getWriter().write(content.toString());
        } catch (IOException ex) { //stream closed by client, do nothing
            LOGGER.info("Stream abruptly closed by client, response aborted");
            LOGGER.fine(ex.getMessage());
        } catch (IllegalStateException ex) { //stream closed by client, do nothing
            LOGGER.info("Stream abruptly closed by client, response aborted");
            LOGGER.fine(ex.getMessage());
        }
    }

//    /**
//     * Send error produced during getService opperation.
//     *
//     * <p>
//     * Some errors know how to write themselves out WfsTransactionException for
//     * instance. It looks like this might be is handled by
//     * getExceptionHandler().newServiceException( t, pre, null ). I still
//     * would not mind seeing a check for ServiceConfig Exception here.
//     * </p>
//     *
//     * <p>
//     * This code says that it deals with UNCAUGHT EXCEPTIONS, so I think it
//     * would be wise to explicitly catch ServiceExceptions.
//     * </p>
//     *
//     * @param response DOCUMENT ME!
//     * @param t DOCUMENT ME!
//     */
//    protected void sendError(HttpServletRequest request, HttpServletResponse response, Throwable t) {
//        if (t instanceof ServiceException) {
//            sendError(request, response, (ServiceException) t);
//
//            return;
//        }
//
//        LOGGER.info("Had an undefined error: " + t.getMessage());
//
//        //TODO: put the stack trace in the logger.
//        //t.printStackTrace();
//        //String pre = "UNCAUGHT EXCEPTION";
//        ExceptionHandler exHandler = getExceptionHandler();
//        ServiceException se = exHandler.newServiceException(t);
//
//        sendError(request, response, se);
//
//        //GeoServer geoServer = (GeoServer) this.getServletConfig()
//        //                                      .getServletContext().getAttribute(GeoServer.WEB_CONTAINER_KEY);
//        //send(response, se.getXmlResponse(geoServer.isVerboseExceptions()));
//    }

//    /**
//     * Send a serviceException produced during getService opperation.
//     *
//     * @param response DOCUMENT ME!
//     * @param se DOCUMENT ME!
//     */
//    protected void sendError(HttpServletRequest request, HttpServletResponse response,
//        ServiceException se) {
//        // first log the exception
//        LOGGER.log(Level.SEVERE, "Service exception occurred", se);
//
//        String mimeType = se.getMimeType(geoServer);
//
//        send(response, se.getXmlResponse(geoServer.isVerboseExceptions(), request, geoServer),
//            mimeType);
//    }

//    /**
//     * DOCUMENT ME!
//     *
//     * @param response DOCUMENT ME!
//     * @param result DOCUMENT ME!
//     */
//    protected void send(HttpServletRequest httpRequest, HttpServletResponse response,
//        Response result) {
//        OutputStream responseOut = null;
//
//        try {
//            responseOut = response.getOutputStream();
//        } catch (IOException ex) { //stream closed, do nothing.
//            LOGGER.info("apparently client has closed stream: " + ex.getMessage());
//        }
//
//        OutputStream out = new BufferedOutputStream(responseOut);
//        ServletContext servContext = getServletContext();
//        response.setContentType(result.getContentType(
//                (GeoServer) servContext.getAttribute("GeoServer")));
//
//        try {
//            result.writeTo(out);
//            out.flush();
//            responseOut.flush();
//        } catch (IOException ioe) {
//            //user just closed the socket stream, do nothing
//            LOGGER.fine("connection closed by user: " + ioe.getMessage());
//        } catch (ServiceException ex) {
//            sendError(httpRequest, response, ex);
//        }
//    }

    /**
     * Checks if the client requests supports gzipped responses by quering it's
     * 'accept-encoding' header.
     *
     * @param request the request to query the HTTP header from
     *
     * @return true if 'gzip' if one of the supported content encodings of
     *         <code>request</code>, false otherwise.
     */
    protected boolean requestSupportsGzip(HttpServletRequest request) {
        boolean supportsGzip = false;
        String header = request.getHeader("accept-encoding");

        if ((header != null) && (header.indexOf("gzip") > -1)) {
            supportsGzip = true;
        }

        if (LOGGER.isLoggable(Level.CONFIG)) {
            LOGGER.config("user-agent=" + request.getHeader("user-agent"));
            LOGGER.config("accept=" + request.getHeader("accept"));
            LOGGER.config("accept-encoding=" + request.getHeader("accept-encoding"));
        }

        return supportsGzip;
    }

    public String getKvpString() {
        return kvpString;
    }

    public void setKvpString(String kvpString) {
        this.kvpString = kvpString;
    }
}
