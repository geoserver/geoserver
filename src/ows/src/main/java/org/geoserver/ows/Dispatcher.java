/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xsd.EMFUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

/**
 * Dispatches an http request to an open web service (OWS).
 *
 * <p>An OWS request contains three bits of information:
 *
 * <ol>
 *   <li>The service being called
 *   <li>The operation of the service to execute
 *   <li>The version of the service ( optional )
 * </ol>
 *
 * <p>Additional, an OWS request can contain an arbitray number of additional parameters.
 *
 * <p>An OWS request can be specified in two forms. The first form is known as "KVP" in which all
 * the parameters come in the form of a set of key-value pairs. Commonly this type of request is
 * made in an http "GET" request, the parameters being specified in the query string:
 *
 * <pre>
 * <code>http://www.xyz.com/geoserver?service=someService&amp;request=someRequest&amp;version=X.Y.Z&amp;param1=...&amp;param2=...</code>
 * </pre>
 *
 * <p>This type of request can also be made in a "POST" request in with a mime-type of
 * "application/x-www-form-urlencoded".
 *
 * <p>The second form is known as "XML" in which all the parameters come in the form of an xml
 * document. This type of request is made in an http "POST" request.
 *
 * <pre><code>
 *  &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 *  &lt;SomeRequest service="someService" version="X.Y.Z"&gt;
 *    &lt;Param1&gt;...&lt;/Param1&gt;
 *    &lt;Param2&gt;...&lt;/Param2&gt;
 *    ...
 *  &lt;/SomeRequest&gt;
 * </code></pre>
 *
 * <p>When a request is received, the <b>service</b> the <b>version</b> parameters are used to
 * locate a service desciptor, an instance of {@link Service} . With the service descriptor, the
 * <b>request</b> parameter is used to locate the operation of the service to call.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class Dispatcher extends AbstractController {
    /** Logging instance */
    static Logger logger = Logging.getLogger("org.geoserver.ows");

    /** flag to control wether the dispatcher is cite compliant */
    boolean citeCompliant = false;

    /** thread local variable for the request */
    public static final ThreadLocal<Request> REQUEST = new InheritableThreadLocal<>();

    static final Charset UTF8 = StandardCharsets.UTF_8;

    /** The amount of bytes to be read to determine the proper xml reader in POST request */
    static int XML_LOOKAHEAD = 8192;

    /** list of callbacks */
    List<DispatcherCallback> callbacks = Collections.emptyList();

    /** SOAP namespaces */
    public static final String SOAP_12_NS = "http://www.w3.org/2003/05/soap-envelope";

    public static final String SOAP_11_NS = "http://schemas.xmlsoap.org/soap/envelope/";

    /** SOAP mime type */
    static final String SOAP_MIME = "application/soap+xml";

    private Method getEntityResolver = null;

    {
        try {
            // Use reflection to access class/method in the gs-main module.
            Class<?> clazz = Class.forName("org.geoserver.util.EntityResolverProvider");
            getEntityResolver = clazz.getMethod("getEntityResolver");
        } catch (Exception e) {
            // This should only happen when running the gs-ows unit tests.
            logger.log(
                    Level.WARNING,
                    "Unable to load EntityResolverProvider. Entity resolution will be enabled: "
                            + e.getClass().getName()
                            + ": "
                            + e.getMessage());
        }
    }

    /**
     * Sets the flag to control wether the dispatcher is cite compliante.
     *
     * <p>If set to <code>true</code>, the dispatcher with throw exceptions when it encounters
     * something that is not 100% compliant with CITE standards. An example would be a request which
     * specifies the servce in the context path: '.../geoserver/wfs?request=...' and not with the
     * kvp '&amp;service=wfs'.
     *
     * @param citeCompliant <code>true</code> to set compliance, <code>false</code> to unset it.
     */
    public void setCiteCompliant(boolean citeCompliant) {
        this.citeCompliant = citeCompliant;
    }

    public boolean isCiteCompliant() {
        return citeCompliant;
    }

    @Override
    protected void initApplicationContext(ApplicationContext context) {
        // load life cycle callbacks
        callbacks = GeoServerExtensions.extensions(DispatcherCallback.class, context);

        // setup the xml lookahead value
        String lookahead = GeoServerExtensions.getProperty("XML_LOOKAHEAD", context);
        if (lookahead != null) {
            try {
                int lookaheadValue = Integer.valueOf(lookahead);
                if (lookaheadValue <= 0)
                    logger.log(
                            Level.SEVERE,
                            "Invalid XML_LOOKAHEAD value, "
                                    + "will use "
                                    + XML_LOOKAHEAD
                                    + " instead");
                XML_LOOKAHEAD = lookaheadValue;
            } catch (Exception e) {
                logger.log(
                        Level.SEVERE,
                        "Invalid XML_LOOKAHEAD value, " + "will use " + XML_LOOKAHEAD + " instead");
            }
        }
    }

    protected void preprocessRequest(HttpServletRequest request) throws Exception {
        // set the charset

        // TODO: make this server settable
        Charset charSet = UTF8;
        if (request.getCharacterEncoding() != null)
            try {
                charSet = Charset.forName(request.getCharacterEncoding());
            } catch (Exception e) {
                // ok, we tried...
            }

        request.setCharacterEncoding(charSet.name());
    }

    @Override
    protected ModelAndView handleRequestInternal(
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
        preprocessRequest(httpRequest);

        // create a new request instance
        Request request = new Request();

        // set request / response
        request.setHttpRequest(httpRequest);
        request.setHttpResponse(httpResponse);

        Service service = null;

        try {
            // initialize the request and allow callbacks to override it
            request = init(request);

            // store it in the thread local
            REQUEST.set(request);

            // find the service
            try {
                service = service(request);
            } catch (Throwable t) {
                exception(t, null, request);

                return null;
            }

            // throw any outstanding errors
            if (request.getError() != null) {
                throw request.getError();
            }

            // dispatch the operation
            Operation operation = dispatch(request, service);
            request.setOperation(operation);

            if (request.isSOAP()) {
                // let the request object know that this is a SOAP request, since it effects
                // often how the request will be encoded
                flagAsSOAP(operation);
            }

            // execute it
            Object result = execute(request, operation);

            // write the response
            if (result != null) {
                response(result, request, operation);
            }
        } catch (Throwable t) {
            // make Spring security exceptions flow so that exception transformer filter can handle
            // them
            if (isSecurityException(t)) throw (Exception) t;
            exception(t, service, request);
        } finally {
            fireFinishedCallback(request);
            REQUEST.remove();
        }

        return null;
    }

    void flagAsSOAP(Operation op) {
        for (Object reqObj : op.getParameters()) {
            if (OwsUtils.has(reqObj, "formatOptions")) {
                OwsUtils.put(reqObj, "formatOptions", "SOAP", true);
            }
            if (OwsUtils.has(reqObj, "extendedProperties")) {
                OwsUtils.put(reqObj, "extendedProperties", "SOAP", true);
            }
            if (OwsUtils.has(reqObj, "metadata")) {
                OwsUtils.put(reqObj, "metadata", "SOAP", true);
            }
        }
    }

    void fireFinishedCallback(Request req) {
        for (DispatcherCallback cb : callbacks) {
            try {
                cb.finished(req);
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Error firing finished callback for " + cb.getClass(), t);
            }
        }
    }

    Request init(Request request) throws ServiceException, IOException {
        HttpServletRequest httpRequest = request.getHttpRequest();

        String reqContentType = httpRequest.getContentType();
        // figure out method
        request.setGet("GET".equalsIgnoreCase(httpRequest.getMethod()) || isForm(reqContentType));

        // create the kvp map
        parseKVP(request);

        if (!request.isGet()) { // && httpRequest.getInputStream().available() > 0) {
            // check for a SOAP request, if so we need to unwrap the SOAP stuff
            if (httpRequest.getContentType() != null
                    && httpRequest.getContentType().startsWith(SOAP_MIME)) {
                request.setSOAP(true);
                request.setInput(soapReader(httpRequest, request));
            } else if (reqContentType != null
                    && ServletFileUpload.isMultipartContent(httpRequest)) {
                // multipart form upload
                ServletFileUpload up = new ServletFileUpload(new DiskFileItemFactory());

                // treat regular form fields as additional kvp parameters
                Map<String, FileItem> kvpFileItems =
                        new CaseInsensitiveMap<>(new LinkedHashMap<>());
                try {
                    List<FileItem> items = up.parseRequest(httpRequest);
                    FileItemCleanupCallback.setFileItems(items);
                    FileItem body = null;
                    for (FileItem item : items) {
                        if (item.isFormField()) {
                            FileItem old = kvpFileItems.put(item.getFieldName(), item);
                            if (old != null) {
                                old.delete(); // the temp file can be deleted at this point
                            }
                        } else {
                            if (body != null) {
                                // GeoServer only reads the last uploaded file per request so any
                                // other file uploads can be deleted immediately
                                body.delete();
                            }
                            body = item;
                        }
                    }
                    // if no file fields were found, look for one named "body"
                    if (body == null) {
                        body = kvpFileItems.remove("body");
                        if (body == null) {
                            throw new IllegalArgumentException(
                                    "Unable to find input from multipart/form-data content");
                        }
                    }
                    request.setInput(fileItemReader(body));
                } catch (Exception e) {
                    throw new ServiceException("Error handling multipart/form-data content", e);
                }

                Map<String, Object> kvpItems = new LinkedHashMap<>();
                kvpFileItems.forEach(
                        (key, value) -> {
                            kvpItems.put(key, value.getString());
                            value.delete(); // the temp file can be deleted at this point
                        });

                request.setOrAppendKvp(parseKVP(request, kvpItems));
            } else {
                // regular XML POST
                // wrap the input stream in a buffered input stream
                request.setInput(reader(httpRequest));
            }

            if (-1 == request.getInput().read()) {
                request.setInput(null);
            } else {
                request.getInput().reset();
            }
        }
        initRequestContext(request);

        return fireInitCallback(request);
    }

    /**
     * Initializes the request context by parsing the request path into two components: the
     * 'context' and the 'path'. The 'context' is the part of the URI before the last '/' and the
     * 'path' is the part after the last '/'.
     *
     * <p>This method processes the given {@link Request} object to extract and set the context and
     * path based on the HTTP request URI. Leading and trailing slashes are stripped from the
     * request path before the context and path are determined.
     *
     * @param request the {@link Request} object whose context and path need to be initialized.
     * @throws NullPointerException if the {@link Request} or its HTTP request is null.
     *     <pre>
     * Example:
     * If the request URI is "/app/resource/item", then:
     * - context: "app/resource"
     * - path: "item"
     * </pre>
     */
    public static void initRequestContext(Request request) {
        // parse the request path into two components. (1) the 'path' which
        // is the string after the last '/', and the 'context' which is the
        // string before the last '/'
        String ctxPath = request.httpRequest.getContextPath();
        String reqPath = request.httpRequest.getRequestURI();
        reqPath = reqPath.substring(ctxPath.length());

        // strip off leading and trailing slashes
        if (reqPath.startsWith("/")) {
            reqPath = reqPath.substring(1, reqPath.length());
        }

        if (reqPath.endsWith("/")) {
            reqPath = reqPath.substring(0, reqPath.length() - 1);
        }

        String context = reqPath;
        String path = null;
        int index = context.lastIndexOf('/');
        if (index != -1) {
            path = context.substring(index + 1);
            context = context.substring(0, index);
        } else {
            path = reqPath;
            context = null;
        }

        request.setContext(context);
        request.setPath(path);
    }

    private boolean isForm(String contentType) {
        if (contentType == null) {
            return false;
        } else {
            return contentType.startsWith("application/x-www-form-urlencoded");
        }
    }

    Request fireInitCallback(Request req) {
        for (DispatcherCallback cb : callbacks) {
            Request r = cb.init(req);
            req = r != null ? r : req;
        }
        return req;
    }

    BufferedReader soapReader(HttpServletRequest httpRequest, Request request) throws IOException {
        // in order to pull out the payload we have to parse the entire request and then reencode it
        // not nice... but then again neither is using SOAP
        Document dom = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Object provider = GeoServerExtensions.bean("entityResolverProvider");
            if (provider != null && getEntityResolver != null) {
                db.setEntityResolver((EntityResolver) getEntityResolver.invoke(provider));
            }
            dom = db.parse(httpRequest.getInputStream());
        } catch (Exception e) {
            throw new IOException("Error parsing SOAP request", e);
        }

        // find the soap:Body element
        NodeList list = dom.getElementsByTagNameNS(SOAP_12_NS, "Body");
        if (list.getLength() != 1) {
            list = dom.getElementsByTagNameNS(SOAP_11_NS, "Body");
            if (list.getLength() != 1) {
                throw new IOException("SOAP requests should specify a single Body element");
            } else {
                request.setSOAPNamespace(SOAP_11_NS);
            }
        } else {
            request.setSOAPNamespace(SOAP_12_NS);
        }

        Element body = (Element) list.item(0);

        // pull out the first element child
        Element payload = null;
        for (int i = 0; payload == null && i < body.getChildNodes().getLength(); i++) {
            Node n = body.getChildNodes().item(i);
            if (n instanceof Element) {
                payload = (Element) n;
            }
        }

        if (payload == null) {
            throw new IOException("Could not find payload in SOAP request");
        }

        // transform the payload back into an input stream so we can parse it as usual
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            TransformerFactory.newInstance()
                    .newTransformer()
                    .transform(new DOMSource(payload), new StreamResult(bout));
        } catch (Exception e) {
            throw new IOException("Error encoding payload of SOAP request", e);
        }

        return RequestUtils.getBufferedXMLReader(
                new ByteArrayInputStream(bout.toByteArray()), XML_LOOKAHEAD);
    }

    BufferedReader reader(HttpServletRequest httpRequest) throws IOException {
        return RequestUtils.getBufferedXMLReader(httpRequest.getInputStream(), XML_LOOKAHEAD);
    }

    BufferedReader fileItemReader(FileItem fileItem) throws IOException {
        return RequestUtils.getBufferedXMLReader(fileItem.getInputStream(), XML_LOOKAHEAD);
    }

    Service service(Request req) throws Exception {
        String service = getServiceFromRequest(req);

        // load from teh context
        Service serviceDescriptor = findService(service, req.getVersion(), req.getNamespace());
        if (serviceDescriptor == null) {
            // hack for backwards compatability, try finding the service with the context instead
            // of the service
            if (req.getContext() != null) {
                serviceDescriptor =
                        findService(req.getContext(), req.getVersion(), req.getNamespace());
                if (serviceDescriptor != null) {
                    // found, assume that the client is using <service>/<request>
                    if (req.getRequest() == null) {
                        req.setRequest(req.getService());
                    }
                    req.setService(req.getContext());
                    req.setContext(null);
                }
            }
            if (serviceDescriptor == null) {
                String msg = "No service: ( " + service + " )";
                throw new ServiceException(msg, "InvalidParameterValue", "service");
            }
        }
        req.setServiceDescriptor(serviceDescriptor);
        return fireServiceDispatchedCallback(req, serviceDescriptor);
    }

    /**
     * Retrieves the service name from the given request object, which may contain key-value pairs
     * (KVP) or a request body. If the service name is not explicitly provided in the request,
     * attempts to infer it from the request context.
     *
     * @param req The request object containing information about the service and request.
     * @return The name of the service extracted from the request.
     * @throws ServiceException If the service name cannot be determined from the request.
     * @throws Exception If an unexpected error occurs during the extraction process.
     */
    public String getServiceFromRequest(Request req) throws Exception {
        // check kvp
        if (req.getKvp() != null) {

            req.setService(normalize(KvpUtils.getSingleValue(req.getKvp(), "service")));
            req.setVersion(
                    normalizeVersion(normalize(KvpUtils.getSingleValue(req.getKvp(), "version"))));
            req.setRequest(normalize(KvpUtils.getSingleValue(req.getKvp(), "request")));
            req.setOutputFormat(normalize(KvpUtils.getSingleValue(req.getKvp(), "outputFormat")));
        }
        // check the body
        if (req.getInput() != null && "POST".equalsIgnoreCase(req.getHttpRequest().getMethod())) {
            readOpPost(req);
        }

        // try to infer from context
        // JD: for cite compliance, a service *must* be specified explicitly by
        // either a kvp, or a xml attribute, however in reality the context
        // is often a good way to infer the service or request
        String service = req.getService();

        if ((service == null) || (req.getRequest() == null)) {
            Map map = readOpContext(req);

            if (service == null) {
                service = normalize((String) map.get("service"));

                if ((service != null) && !citeCompliant) {
                    req.setService(service);
                }
            }

            if (req.getRequest() == null) {
                req.setRequest(normalize((String) map.get("request")));
            }
        }

        if (service == null) {
            // give up
            throw new ServiceException(
                    "Could not determine service", "MissingParameterValue", "service");
        }
        return service;
    }

    Service fireServiceDispatchedCallback(Request req, Service service) {
        for (DispatcherCallback cb : callbacks) {
            Service s = cb.serviceDispatched(req, service);
            service = s != null ? s : service;
        }
        return service;
    }

    /**
     * Normalize a parameter, trimming whitespace
     *
     * @return The value with whitespace trimmed, or null if this would result in an empty string.
     */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }

        if ("".equals(value.trim())) {
            return null;
        }

        return value.trim();
    }

    /**
     * Normalize the version, handling cases like forcing "x.y" to "x.y.z".
     *
     * @return normalized version
     */
    public static String normalizeVersion(String version) {
        if (version == null) {
            return null;
        }

        Version v = new Version(version);
        if (v.getMajor() == null) {
            return null;
        }

        if (v.getMinor() == null) {
            return String.format("%d.0.0", ((Number) v.getMajor()).intValue());
        }

        if (v.getRevision() == null) {
            return String.format(
                    "%d.%d.0",
                    ((Number) v.getMajor()).intValue(), ((Number) v.getMinor()).intValue());
        }

        // version ok
        return version;
    }

    Operation dispatch(Request req, Service serviceDescriptor) throws Throwable {
        if (req.getRequest() == null) {
            String msg =
                    "Could not determine geoserver request from http request "
                            + req.getHttpRequest();
            throw new ServiceException(msg, "MissingParameterValue", "request");
        }

        // ensure the requested operation exists
        boolean exists = operationExists(req, serviceDescriptor);
        // did we have a mixed kvp + post request and trusted the body for the request?
        if (!exists && req.getKvp().get("request") != null) {
            req.setRequest(normalize(KvpUtils.getSingleValue(req.getKvp(), "request")));
            exists = operationExists(req, serviceDescriptor);
        }

        // lookup the operation, initial lookup based on (service,request)
        Object serviceBean = serviceDescriptor.getService();
        Method operation = OwsUtils.method(serviceBean.getClass(), req.getRequest());

        if (operation == null || !exists) {
            String msg = "No such operation " + req;
            throw new ServiceException(msg, "OperationNotSupported", req.getRequest());
        }

        // step 4: setup the paramters
        Object[] parameters = new Object[operation.getParameterTypes().length];

        for (int i = 0; i < parameters.length; i++) {
            Class<?> parameterType = operation.getParameterTypes()[i];

            // first check for servlet request and response
            if (parameterType.isAssignableFrom(HttpServletRequest.class)) {
                parameters[i] = req.getHttpRequest();
            } else if (parameterType.isAssignableFrom(HttpServletResponse.class)) {
                parameters[i] = req.getHttpResponse();
            }
            // next check for input and output
            else if (parameterType.isAssignableFrom(InputStream.class)) {
                parameters[i] = req.getHttpRequest().getInputStream();
            } else if (parameterType.isAssignableFrom(OutputStream.class)) {
                parameters[i] = req.getHttpResponse().getOutputStream();
            } else {
                // check for a request object
                Object requestBean = null;

                // track an exception
                Throwable t = null;

                // Boolean used for evaluating if the request bean has been parsed in KVP or in XML
                boolean kvpParsed = false;
                boolean xmlParsed = false;

                if (req.getKvp() != null
                        && req.getKvp().size() > 0
                        && (!(req.getKvp().size() == 1 && req.getKvp().containsKey("service")))) {
                    // use the kvp reader mechanism, but not if the only kvp is the service
                    try {
                        requestBean = parseRequestKVP(parameterType, req);
                        kvpParsed = true;
                    } catch (Exception e) {
                        // don't die now, there might be a body to parse
                        t = e;
                    }
                }
                if (req.getInput() != null) {
                    // use the xml reader mechanism
                    requestBean = parseRequestXML(requestBean, req.getInput(), req);
                    xmlParsed = true;
                }

                // if no reader found for the request, throw exception
                // TODO: we may wish to make this configurable, as perhaps there
                // might be cases when the service prefers that null be passed in?
                if (requestBean == null) {
                    // unable to parse request object, throw exception if we
                    // caught one
                    if (t != null) {
                        throw t;
                    }
                    if (kvpParsed && xmlParsed || (!kvpParsed && !xmlParsed)) {
                        throw new ServiceException(
                                "Could not find request reader (either kvp or xml) for: "
                                        + parameterType.getName()
                                        + ", it might be that some request parameters are missing, "
                                        + "please check the documentation");
                    } else if (kvpParsed) {
                        throw new ServiceException(
                                "Could not parse the KVP for: " + parameterType.getName());
                    } else {
                        throw new ServiceException(
                                "Could not parse the XML for: " + parameterType.getName());
                    }
                }

                // GEOS-934  and GEOS-1288
                Method setBaseUrl =
                        OwsUtils.setter(requestBean.getClass(), "baseUrl", String.class);
                if (setBaseUrl != null) {
                    setBaseUrl.invoke(
                            requestBean,
                            new String[] {ResponseUtils.baseURL(req.getHttpRequest())});
                }

                // another couple of thos of those lovley cite things, version+service has to
                // specified for
                // non capabilities request, so if we dont have either thus far, check the request
                // objects to try and find one
                // TODO: should make this configurable
                if (requestBean != null) {
                    // if we dont have a version thus far, check the request object
                    if (req.getService() == null) {
                        req.setService(lookupRequestBeanProperty(requestBean, "service", false));
                    }

                    if (req.getVersion() == null) {
                        req.setVersion(
                                normalizeVersion(
                                        lookupRequestBeanProperty(requestBean, "version", false)));
                    }

                    if (req.getOutputFormat() == null) {
                        req.setOutputFormat(
                                lookupRequestBeanProperty(requestBean, "outputFormat", true));
                    }

                    parameters[i] = requestBean;
                }
            }
        }

        // if we are in cite compliant mode, do some additional checks to make
        // sure the "mandatory" parameters are specified, even though we
        // succesfully dispatched the request.
        if (citeCompliant) {
            // the version is mandatory for all requests but GetCapabilities
            if (!"GetCapabilities".equalsIgnoreCase(req.getRequest())) {
                if (req.getVersion() == null) {
                    // must be a version on non-capabilities requests
                    throw new ServiceException(
                            "Could not determine version", "MissingParameterValue", "version");
                } else {
                    // version must be valid
                    if (!req.getVersion().matches("[0-99].[0-99].[0-99]")) {
                        throw new ServiceException(
                                "Invalid version: " + req.getVersion(),
                                "InvalidParameterValue",
                                "version");
                    }

                    // make sure the versoin actually exists
                    boolean found = false;
                    Version version = new Version(req.getVersion());

                    for (Service service : loadServices()) {
                        if (version.equals(service.getVersion())) {
                            found = true;

                            break;
                        }
                    }

                    if (!found) {
                        throw new ServiceException(
                                "Invalid version: " + req.getVersion(),
                                "InvalidParameterValue",
                                "version");
                    }
                }
            }

            // the service is mandatory for all requests instead
            if (req.getService() == null) {
                // give up
                throw new ServiceException(
                        "Could not determine service", "MissingParameterValue", "service");
            }
        }

        Operation op = new Operation(req.getRequest(), serviceDescriptor, operation, parameters);
        return fireOperationDispatchedCallback(req, op);
    }

    private boolean operationExists(Request req, Service serviceDescriptor) {
        boolean exists = false;
        for (String op : serviceDescriptor.getOperations()) {
            if (op.equalsIgnoreCase(req.getRequest())) {
                exists = true;
                break;
            }
        }
        return exists;
    }

    Operation fireOperationDispatchedCallback(Request req, Operation op) {
        for (DispatcherCallback cb : callbacks) {
            Operation o = cb.operationDispatched(req, op);
            op = o != null ? o : op;
        }
        return op;
    }

    String lookupRequestBeanProperty(
            Object requestBean, String property, boolean allowDefaultValues) {
        if (requestBean instanceof EObject && EMFUtils.has((EObject) requestBean, property)) {
            // special case hack for eObject, we should move
            // this out into an extension ppint
            EObject eObject = (EObject) requestBean;

            if (allowDefaultValues || EMFUtils.isSet(eObject, property)) {
                return normalize((String) EMFUtils.get(eObject, property));
            }
        } else {
            // straight reflection
            String version = OwsUtils.property(requestBean, property, String.class);

            if (version != null) {
                return normalize(version);
            }
        }

        return null;
    }

    Object execute(Request req, Operation opDescriptor) throws Throwable {
        Service serviceDescriptor = opDescriptor.getService();
        Object serviceBean = serviceDescriptor.getService();
        Object[] parameters = opDescriptor.getParameters();

        // step 5: execute
        Object result = null;

        try {
            if (serviceBean instanceof DirectInvocationService) {
                // invokeDirect expects the operation to be called as declared in the operation
                // descriptor, although it used to match a method name, lets use the declared
                // operation name for contract compliance.
                String operationName = opDescriptor.getId();
                result =
                        ((DirectInvocationService) serviceBean)
                                .invokeDirect(operationName, parameters);
            } else {
                Method operation = opDescriptor.getMethod();
                result = operation.invoke(serviceBean, parameters);
            }
        } catch (Exception e) {
            if (e.getCause() != null) {
                throw e.getCause();
            }
            throw e;
        }

        return fireOperationExecutedCallback(req, opDescriptor, result);
    }

    Object fireOperationExecutedCallback(Request req, Operation op, Object result) {
        for (DispatcherCallback cb : callbacks) {
            Object r = cb.operationExecuted(req, op, result);
            result = r != null ? r : result;
        }
        return result;
    }

    void response(Object result, Request req, Operation opDescriptor) throws Throwable {
        // step 6: write response
        if (result != null) {
            // look up respones
            List<Response> responses = GeoServerExtensions.extensions(Response.class);

            // first filter by binding, and canHandle
            O:
            for (Iterator itr = responses.iterator(); itr.hasNext(); ) {
                Response response = (Response) itr.next();

                Class<?> binding = response.getBinding();

                if (!binding.isAssignableFrom(result.getClass())
                        || !response.canHandle(opDescriptor)) {
                    itr.remove();

                    continue;
                }

                // filter by output format
                Set outputFormats = response.getOutputFormats();

                if ((req.getOutputFormat() != null)
                        && (!outputFormats.isEmpty())
                        && !outputFormats.contains(req.getOutputFormat())) {

                    // must do a case insensitive check
                    for (Object format : outputFormats) {
                        String outputFormat = (String) format;
                        if (req.getOutputFormat().equalsIgnoreCase(outputFormat)) {
                            continue O;
                        }
                    }

                    itr.remove();
                }
            }

            if (responses.isEmpty()) {
                if (req.getOutputFormat() != null) {
                    throw new ServiceException(
                            "Failed to find response for output format " + req.getOutputFormat(),
                            ServiceException.INVALID_PARAMETER_VALUE,
                            "outputFormat");
                } else {
                    String msg = "No response: ( object = " + result.getClass();

                    if (req.getOutputFormat() != null) {
                        msg += (", outputFormat = " + req.getOutputFormat());
                    }

                    msg += " )";

                    throw new RuntimeException(msg);
                }
            }

            if (responses.size() > 1) {
                // sort by class hierarchy
                Collections.sort(
                        responses,
                        (o1, o2) -> {
                            Class<?> c1 = o1.getBinding();
                            Class<?> c2 = o2.getBinding();

                            if (c1.equals(c2)) {
                                return 0;
                            }

                            if (c1.isAssignableFrom(c2)) {
                                return 1;
                            }

                            return -1;
                        });

                // check first two and make sure bindings are not equal
                Response r1 = responses.get(0);
                Response r2 = responses.get(1);

                if (r1.getBinding().equals(r2.getBinding())) {
                    String msg =
                            "Multiple responses: (" + result.getClass() + "): " + r1 + ", " + r2;
                    throw new RuntimeException(msg);
                }
            }

            Response response = responses.get(0);
            response = fireResponseDispatchedCallback(req, opDescriptor, result, response);

            // load the output strategy to be used
            ServiceStrategy outputStrategy = findOutputStrategy(req.getHttpResponse());

            if (outputStrategy == null) {
                outputStrategy = new DefaultOutputStrategy();
            }

            // set the mime type
            String mimeType = response.getMimeType(result, opDescriptor);

            // check for SOAP request
            if (req.isSOAP()) {
                req.getHttpResponse().setContentType(SOAP_MIME);
            } else {
                req.getHttpResponse().setContentType(mimeType);
            }

            // set the charset
            String charset = response.getCharset(opDescriptor);
            if (charset != null) {
                req.getHttpResponse().setCharacterEncoding(charset);
            }

            setHeaders(req, opDescriptor, result, response);

            @SuppressWarnings("PMD.CloseResource") // managed by the output strategy
            OutputStream output = outputStrategy.getDestination(req.getHttpResponse());
            boolean abortResponse = true;
            try {
                if (req.isSOAP()) {
                    // SOAP request, start the SOAP wrapper
                    startSOAPEnvelope(output, req, response);
                }

                // special check for transformer
                if (req.isSOAP() && result instanceof TransformerBase) {
                    ((TransformerBase) result).setOmitXMLDeclaration(true);
                }

                // actually write out the response
                response.write(result, output, opDescriptor);

                if (req.isSOAP()) {
                    // SOAP request, start the SOAP wrapper
                    endSOAPEnvelope(output);
                }

                // flush the output with detection of client shutting the door in our face
                try {
                    outputStrategy.flush(req.getHttpResponse());
                } catch (IOException e) {
                    throw new ClientStreamAbortedException(e);
                }
                abortResponse = true;
            } finally {
                if (abortResponse) {
                    outputStrategy.abort();
                }
            }

            // flush the underlying out stream for good measure
            req.getHttpResponse().getOutputStream().flush();
        }
    }

    void setHeaders(Request req, Operation opDescriptor, Object result, Response response) {
        // get the basics using the new api
        Map rawKvp = req.getRawKvp();
        String disposition = response.getPreferredDisposition(result, opDescriptor);
        String filename = response.getAttachmentFileName(result, opDescriptor);

        // get user overrides, if any
        if (rawKvp != null) {
            // check if the filename and content disposition were provided
            if (rawKvp.get("FILENAME") != null) {
                filename = (String) rawKvp.get("FILENAME");
            }
            if (rawKvp.get("CONTENT-DISPOSITION") != null) {
                disposition = (String) rawKvp.get("CONTENT-DISPOSITION");
            }
        }

        // make sure the disposition obtained so far is valid
        // check and prevent invalid header injection
        if (disposition != null
                && !Response.DISPOSITION_ATTACH.equals(disposition)
                && !Response.DISPOSITION_INLINE.equals(disposition)) {
            disposition = null;
        }

        // set any extra headers, other than the mime-type
        String[][] headers = response.getHeaders(result, opDescriptor);
        boolean contentDispositionProvided = false;
        if (headers != null) {
            for (String[] header : headers) {
                if (header[0].equalsIgnoreCase("Content-Disposition")) {
                    contentDispositionProvided = true;
                    if (disposition == null) {
                        req.getHttpResponse().addHeader(header[0], header[1]);
                    }
                } else {
                    req.getHttpResponse().addHeader(header[0], header[1]);
                }
            }
        }

        // default disposition value and set if not forced by the user and not set
        // directly by the response
        if (!contentDispositionProvided) {
            if (disposition == null) {
                disposition = Response.DISPOSITION_INLINE;
            }

            // override any existing header
            String disp = disposition + "; filename=" + filename;
            req.getHttpResponse().setHeader("Content-Disposition", disp);
        }
    }

    void startSOAPEnvelope(OutputStream output, Request request, Response response)
            throws IOException {
        output.write(
                ("<soap:Envelope xmlns:soap='" + request.getSOAPNamespace() + "'><soap:Header/>")
                        .getBytes());
        output.write("<soap:Body".getBytes());
        if (response != null && response instanceof SOAPAwareResponse) {
            String type = ((SOAPAwareResponse) response).getBodyType();
            if (type != null) {
                output.write((" type='" + type + "'").getBytes());
            }
        }
        output.write(">".getBytes());
    }

    void endSOAPEnvelope(OutputStream output) throws IOException {
        output.write(("</soap:Body></soap:Envelope>").getBytes());
    }

    Response fireResponseDispatchedCallback(
            Request req, Operation op, Object result, Response response) {
        for (DispatcherCallback cb : callbacks) {
            Response r = cb.responseDispatched(req, op, result, response);
            response = r != null ? r : response;
        }
        return response;
    }

    Collection<Service> loadServices() {
        Collection<Service> services = GeoServerExtensions.extensions(Service.class);

        if (!(new HashSet<>(services).size() == services.size())) {
            String msg = "Two identical service descriptors found";
            throw new IllegalStateException(msg);
        }

        return services;
    }

    Service findService(String id, String ver, String namespace) throws ServiceException {
        Version version = (ver != null) ? new Version(ver) : null;
        Collection<Service> services = loadServices();

        // the id is actually the pathinfo, in case workspace specific services
        // are active we want to skip the workspace part in the path and go directly to the
        // servlet, which normally, if we ended up here, is a reflector (wms/kml)
        if (id.contains("/")) {
            id = id.substring(id.indexOf("/") + 1);
        }

        // first just match on service,request
        List<Service> matches = new ArrayList<>();

        for (Service sBean : services) {
            if (sBean.getId().equalsIgnoreCase(id)) {
                matches.add(sBean);
            }
        }

        if (matches.isEmpty()) {
            return null;
        }

        Service sBean = null;

        // if multiple, use version to filter match
        if (matches.size() > 1) {
            List<Service> vmatches = new ArrayList<>(matches);

            // match up the version
            if (version != null) {
                // version specified, look for a match
                for (Iterator<Service> itr = vmatches.iterator(); itr.hasNext(); ) {
                    Service s = itr.next();

                    if (version.equals(s.getVersion())) {
                        continue;
                    }

                    itr.remove();
                }

                if (vmatches.isEmpty()) {
                    // no matching version found, drop out and next step
                    // will sort to return highest version
                    vmatches = new ArrayList<>(matches);
                }
            }

            // if still multiple matches use namespace, if available, to filter
            if (namespace != null && vmatches.size() > 1) {
                List<Service> nmatches = new ArrayList<>(vmatches);
                for (Iterator<Service> itr = nmatches.iterator(); itr.hasNext(); ) {
                    Service s = itr.next();
                    if (s.getNamespace() != null && !s.getNamespace().equals(namespace)) {
                        // service declares namespace, kick it out if there is no match, otherwise
                        // leave it along
                        itr.remove();
                    }
                }

                if (!nmatches.isEmpty()) {
                    vmatches = nmatches;
                }
            }

            // multiple services found, sort by version
            if (vmatches.size() > 1) {
                // use highest version
                Comparator<Service> comparator =
                        (s1, s2) -> s1.getVersion().compareTo(s2.getVersion());

                Collections.sort(vmatches, comparator);
            }

            sBean = vmatches.get(vmatches.size() - 1);
        } else {
            // only a single match, that was easy
            sBean = matches.get(0);
        }

        return sBean;
    }

    public static Collection<KvpRequestReader> loadKvpRequestReaders() {
        Collection<KvpRequestReader> kvpReaders =
                GeoServerExtensions.extensions(KvpRequestReader.class);

        if (!(new HashSet<>(kvpReaders).size() == kvpReaders.size())) {
            String msg = "Two identical kvp readers found";
            throw new IllegalStateException(msg);
        }

        return kvpReaders;
    }

    public static KvpRequestReader findKvpRequestReader(Class<?> type) {
        Collection<KvpRequestReader> kvpReaders = loadKvpRequestReaders();

        List<KvpRequestReader> matches = new ArrayList<>();

        for (KvpRequestReader kvpReader : kvpReaders) {
            if (kvpReader.getRequestBean().isAssignableFrom(type)) {
                matches.add(kvpReader);
            }
        }

        if (matches.isEmpty()) {
            return null;
        }

        if (matches.size() > 1) {
            // sort by class hierarchy
            Comparator<KvpRequestReader> comparator =
                    (kvp1, kvp2) -> {
                        if (kvp2.getRequestBean().isAssignableFrom(kvp1.getRequestBean())) {
                            return -1;
                        }

                        return 1;
                    };

            Collections.sort(matches, comparator);
        }

        return matches.get(0);
    }

    static Collection<XmlRequestReader> loadXmlReaders() {
        List<XmlRequestReader> xmlReaders = GeoServerExtensions.extensions(XmlRequestReader.class);

        if (!(new HashSet<>(xmlReaders).size() == xmlReaders.size())) {

            String msg = "Two identical xml readers found";
            for (int i = 0; i < xmlReaders.size(); i++) {
                XmlRequestReader r1 = xmlReaders.get(i);
                for (int j = i + 1; j < xmlReaders.size(); j++) {
                    XmlRequestReader r2 = xmlReaders.get(j);
                    if (r1.equals(r2)) {
                        msg += ": " + r1 + " and " + r2;
                        break;
                    }
                }
            }

            throw new IllegalStateException(msg);
        }

        return xmlReaders;
    }

    /**
     * Finds a registered {@link XmlRequestReader} bean able to read a request, given the request
     * details
     *
     * @param namespace The XML namespace of the request body
     * @param element The OWS request, e.g. "GetMap"
     * @param serviceId The OWS service, e.g. "WMS"
     * @param ver The OWS service version, e.g "1.1.1"
     * @return An {@link XmlRequestReader} capable of reading the request body
     */
    public static XmlRequestReader findXmlReader(
            String namespace, String element, String serviceId, String ver) {
        Collection<XmlRequestReader> xmlReaders = loadXmlReaders();

        // first just match on namespace, element
        List<XmlRequestReader> matches = new ArrayList<>();

        for (XmlRequestReader xmlReader : xmlReaders) {
            QName xmlElement = xmlReader.getElement();

            if (xmlElement.getLocalPart().equalsIgnoreCase(element)) {
                if (xmlElement.getNamespaceURI().equalsIgnoreCase(namespace)) {
                    matches.add(xmlReader);
                }
            }
        }

        if (matches.isEmpty()) {
            // do a more lax serach, search only on the element name if the
            // namespace was unspecified
            if (namespace == null || namespace.equals("")) {
                String msg =
                        "No namespace specified in request, searching for "
                                + " xml reader by element name only";
                logger.info(msg);

                for (XmlRequestReader xmlReader : xmlReaders) {
                    if (xmlReader.getElement().getLocalPart().equals(element)) {
                        matches.add(xmlReader);
                    }
                }

                if (!matches.isEmpty()) {
                    // we found some matches, make sure they are all in the
                    // same service
                    Iterator itr = matches.iterator();
                    XmlRequestReader first = (XmlRequestReader) itr.next();
                    while (itr.hasNext()) {
                        XmlRequestReader xmlReader = (XmlRequestReader) itr.next();
                        if (!first.getServiceId().equals(xmlReader.getServiceId())) {
                            // abort
                            matches.clear();
                            break;
                        }
                    }
                }
            }
        }

        if (matches.isEmpty()) {
            String msg = "No xml reader: (" + namespace + "," + element + ")";
            logger.info(msg);
            return null;
        }

        XmlRequestReader xmlReader = null;

        // if multiple, use version to filter match
        if (matches.size() > 1) {
            List<XmlRequestReader> vmatches = new ArrayList<>(matches);

            // match up the service
            if (serviceId != null) {
                for (Iterator<XmlRequestReader> itr = vmatches.iterator(); itr.hasNext(); ) {
                    XmlRequestReader r = itr.next();

                    if (r.getServiceId() == null || serviceId.equalsIgnoreCase(r.getServiceId())) {
                        continue;
                    }

                    itr.remove();
                }

                // if no reader matching the service is found, we should
                // not return a reader, as service is key to identify the reader
                // we cannot just assume a meaningful default
            }

            // match up the version
            if (ver != null) {
                Version version = new Version(ver);

                // version specified, look for a match (and allow version
                // generic ones to live by)
                for (Iterator itr = vmatches.iterator(); itr.hasNext(); ) {
                    XmlRequestReader r = (XmlRequestReader) itr.next();

                    if (r.getVersion() == null || version.equals(r.getVersion())) {
                        continue;
                    }

                    itr.remove();
                }

                if (vmatches.isEmpty()) {
                    // no matching version found, drop out and next step
                    // will sort to return highest version
                    vmatches = new ArrayList<>(matches);
                }
            }

            // multiple readers found, sort by version and by service match
            if (vmatches.size() > 1) {
                // use highest version
                Comparator<XmlRequestReader> comparator =
                        (r1, r2) -> {
                            Version v1 = r1.getVersion();
                            Version v2 = r2.getVersion();

                            if ((v1 == null) && (v2 == null)) {
                                return 0;
                            }

                            if ((v1 != null) && (v2 == null)) {
                                return 1;
                            }

                            if ((v1 == null) && (v2 != null)) {
                                return -1;
                            }

                            int versionCompare = v1.compareTo(v2);

                            if (versionCompare != 0) {
                                return versionCompare;
                            }

                            String sid1 = r1.getServiceId();
                            String sid2 = r2.getServiceId();

                            if ((sid1 == null) && (sid2 == null)) {
                                return 0;
                            }

                            if ((sid1 != null) && (sid2 == null)) {
                                return 1;
                            }

                            if ((sid1 == null) && (sid2 != null)) {
                                return -1;
                            }

                            return sid1.compareTo(sid2);
                        };

                Collections.sort(vmatches, comparator);
            }

            if (!vmatches.isEmpty()) xmlReader = vmatches.get(vmatches.size() - 1);
        } else {
            // only a single match, that was easy
            xmlReader = matches.get(0);
        }

        return xmlReader;
    }

    ServiceStrategy findOutputStrategy(HttpServletResponse response) {
        OutputStrategyFactory factory = null;
        try {
            factory = (OutputStrategyFactory) GeoServerExtensions.bean("serviceStrategyFactory");
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
        return factory.createOutputStrategy(response);
    }

    BufferedInputStream input(File cache) throws IOException {
        return (cache == null) ? null : new BufferedInputStream(new FileInputStream(cache));
    }

    void preParseKVP(Request req) throws ServiceException {
        HttpServletRequest request = req.getHttpRequest();

        // unparsed kvp set
        Map<String, String[]> kvp = request.getParameterMap();

        if (kvp == null || kvp.isEmpty()) {
            req.setKvp(new HashMap<>());
            // req.kvp = null;
            return;
        }

        // track parsed kvp and unparsd
        Map<String, Object> parsedKvp = KvpUtils.normalize(kvp);
        Map<String, Object> rawKvp = new KvpMap<>(parsedKvp);

        req.setKvp(parsedKvp);
        req.setRawKvp(rawKvp);
    }

    void parseKVP(Request req) throws ServiceException {
        preParseKVP(req);
        parseKVP(req, req.getKvp());
    }

    Map<String, Object> parseKVP(Request req, Map<String, Object> kvp) {
        // if the service is not explicitly set, some parsers will not be found
        if (!kvp.containsKey("service") && !kvp.containsKey("SERVICE") && !citeCompliant) {
            String service = null;
            try {
                service = getServiceFromRequest(req);
                if (!"OWS".equalsIgnoreCase(service)) { // OWS is too generic for the parser lookup
                    kvp.put("service", service);
                }
                logger.log(Level.FINER, "service kvp parameter not found, setting to " + service);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to determine service from kvp or context");
            }
        }
        List<Throwable> errors = KvpUtils.parse(kvp);
        if (!errors.isEmpty()) {
            req.setError(errors.get(0));
        }
        return kvp;
    }

    Object parseRequestKVP(Class<?> type, Request request) throws Exception {
        KvpRequestReader kvpReader = findKvpRequestReader(type);

        if (kvpReader != null) {
            Object requestBean = kvpReader.createRequest();

            if (requestBean != null && request.getKvp() != null && request.getRawKvp() != null) {
                requestBean = kvpReader.read(requestBean, request.getKvp(), request.getRawKvp());
            }

            return requestBean;
        }

        return null;
    }

    /**
     * To be called once it was determined the request is a POST request with an XML request body;
     * uses {@link XmlRequestReader} to parse the {@link Request#getInput() request input}.
     *
     * <p>This method expects {@code request.} {@link Request#getNamespace() namespace}, {@link
     * Request#getPostRequestElementName() postRequestElementName}, {@link Request#getVersion()
     * version}, and {@link Request#getService() service} to be set already.
     */
    Object parseRequestXML(Object requestBean, BufferedReader input, Request request)
            throws Exception {
        // check for an empty input stream
        if (!input.ready()) {
            return null;
        }

        String namespace = request.getNamespace();
        // resolve reader based on root XML element name, may differ from request name (e.g.
        // request=GetMap, root element=StyledLayerDescriptor)
        String element = request.getPostRequestElementName();
        String version = request.getVersion();
        String service = request.getService();

        XmlRequestReader xmlReader = findXmlReader(namespace, element, service, version);
        if (xmlReader == null) {
            // no xml reader, just return object passed in
            return requestBean;
        }

        return xmlReader.read(requestBean, input, request.getKvp());
    }

    /**
     * Reads the following parameters from an OWS XML request body: * service
     *
     * @param request {@link Request} object
     * @return a {@link Map} containing the parsed parameters.
     */
    public static Map<String, String> readOpContext(Request request) {

        Map<String, String> map = new HashMap<>();
        if (request.getPath() != null) {
            map.put("service", request.getPath());
        } else if (request.getHttpRequest() != null
                && request.getHttpRequest().getServletPath() != null) {
            // Path not found so try to fall back on HTTPRequest Servlet Path
            String service =
                    extractServiceFromHttpRequest(request.getHttpRequest().getRequestURI());
            if (!service.trim().isEmpty()) {
                map.put("service", service.trim().toUpperCase());
            }
        }

        return map;
    }

    private static String extractServiceFromHttpRequest(String servletPath) {
        int startIndex = servletPath.lastIndexOf("/") + 1;
        int endIndex = servletPath.indexOf("?");

        if (startIndex == 0) {
            return ""; // No forward slash found
        }

        if (endIndex == -1) {
            return servletPath.substring(startIndex); // No question mark found
        }

        return servletPath.substring(startIndex, endIndex);
    }
    /**
     * To be called once determined the incoming request is an HTTP POST request
     * with a request body, and the request's {@link Request#getInput() input
     * reader} has been set; in order to pre-parse the XML request body root element
     * and establish the following request properties:
     * <p>
     * <ul>
     * <li>{@link Request#setNamespace namespace}: The xml root element's namespace,
     * or {@code ""} (empty string) if {@code null}
     * <li>{@link Request#setPostRequestElementName PostRequestElementName}: The xml
     * root element name (e.g. {@code GetMap}, {@code GetFeature},
     * {@code StyledLayerDescriptor}, etc.)
     * <li>{@link Request#setRequest: The xml root element name, assuming it matches
     * the request name, might be overriten later while parting the request's query
     * string key-value pairs
     * <li>{@link Request#setService service}: matching the xml root element's
     * {@code service} attribute, or {@code null}
     * <li>{@link Request#setVersion version}: matching the xml root element's
     * {@code version} attribute, or {@code null}
     * <li>{@link Request#setOutputFormat outputFormat}: matching the xml root
     * element's {@code outputFormat} attribute, or {@code null}
     * </ul>
     *
     * @param req The request to set properties to based on the xml request body's
     *            root element
     * @return a {@link Map} containing the parsed parameters.
     * @throws Exception if there was an error reading the input.
     */
    public static Request readOpPost(Request req) throws Exception {
        String namespace;
        String elementName;
        String request;
        String service;
        String version;
        String outputFormat;

        XMLStreamReader parser = createParserForRootElement(req);
        try {
            // position at root element
            while (parser.hasNext()) {
                if (START_ELEMENT == parser.next()) {
                    break;
                }
            }
            namespace = parser.getNamespaceURI() == null ? "" : parser.getNamespaceURI();
            elementName = parser.getLocalName();
            request = elementName;
            service = parser.getAttributeValue(null, "service");
            version = parser.getAttributeValue(null, "version");
            outputFormat = parser.getAttributeValue(null, "outputFormat");
        } finally {
            parser.close();
        }

        req.setNamespace(normalize(namespace));
        req.setPostRequestElementName(normalize(elementName));
        // These may already be given by the request query string KVP's, override only if non-null
        if (request != null) {
            req.setRequest(normalize(request));
        }
        if (service != null) {
            req.setService(normalize(service));
        }
        if (version != null) {
            req.setVersion(normalizeVersion(normalize(version)));
        }
        if (outputFormat != null) {
            req.setOutputFormat(normalize(outputFormat));
        }

        return req;
    }

    private static XMLStreamReader createParserForRootElement(Request req)
            throws IOException, FactoryConfigurationError, XMLStreamException {
        char[] buff = new char[XML_LOOKAHEAD];
        {
            // Read into buff and use that for the XML stream reader, using req.getInput() directly
            // can mess up the BufferedReader's state depending on the implementation
            @SuppressWarnings("PMD.CloseResource")
            BufferedReader input = req.getInput();
            input.mark(XML_LOOKAHEAD);
            input.read(buff);
            input.reset();
        }
        // create stream parser
        XMLInputFactory factory = XMLInputFactory.newFactory();
        // disable DTDs
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        // disable external entities
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        XMLStreamReader parser = factory.createXMLStreamReader(new CharArrayReader(buff));
        return parser;
    }

    void exception(Throwable t, Service service, Request request) {
        Throwable current = t;
        while (current != null
                && !(current instanceof ClientStreamAbortedException)
                && !isSecurityException(current)
                && !(current instanceof HttpErrorCodeException)) {
            if (current instanceof SAXException) current = ((SAXException) current).getException();
            else current = current.getCause();
        }
        if (current instanceof ClientStreamAbortedException) {
            logger.log(Level.FINER, "Client has closed stream", t);
            return;
        }
        if (isSecurityException(current)) {
            throw (RuntimeException) current;
        }

        if (current instanceof HttpErrorCodeException) {
            HttpErrorCodeException ece = (HttpErrorCodeException) current;
            int errorCode = ece.getErrorCode();
            if (errorCode < 199 || errorCode > 299) {
                logger.log(Level.FINE, "", t);
            } else {
                logger.log(Level.FINER, "", t);
            }

            boolean isError = ece.getErrorCode() >= 400;
            HttpServletResponse rsp = request.getHttpResponse();

            if (ece.getContentType() != null) {
                rsp.setContentType(ece.getContentType());
            }
            try {
                if (isError) {
                    if (ece.getMessage() != null) {
                        rsp.sendError(ece.getErrorCode(), ece.getMessage());
                    } else {
                        rsp.sendError(ece.getErrorCode());
                    }
                } else {
                    rsp.setStatus(ece.getErrorCode());
                    if (ece.getMessage() != null) {
                        rsp.getOutputStream().print(ece.getMessage());
                    }
                }
                if (!isError) {
                    // gwc returns an HttpErrorCodeException for 304s
                    // we don't want to flag these as errors for upstream filters, ie the monitoring
                    // extension
                    t = null;
                }
            } catch (IOException e) {
                // means the resposne was already commited something
                logger.log(Level.FINER, "", t);
            }
        } else {
            String errorCode = null;
            String message = t.getMessage();
            Level level = Level.SEVERE;

            // unwind the exception stack until we find one we know about
            Throwable cause = t;
            while (cause != null) {
                if (cause instanceof ServiceException) {
                    errorCode = ((ServiceException) cause).getCode();
                    break;
                }
                cause = cause.getCause();
            }
            // Some pieces of code catch the exception and add it to the error list
            // (e.g. KvpUtils during parsing) without rethrowing it or wrapping it
            // since the service might not be known in advance. Therefore, there
            // might be the case that it isn't a ServiceException and therefore no
            // errorCode available. Let's use the error message in that case.
            if ((errorCode != null && errorCode.toUpperCase().startsWith("INVALID"))
                    || (errorCode == null
                            && message != null
                            && message.toUpperCase().startsWith("INVALID"))) {
                // Log Exceptions dealing with "Invalid" to INFO
                level = Level.INFO;
            }

            logger.log(level, "", t);

            if (cause == null) {
                // did not fine a "special" exception, create a service exception by default
                cause = new ServiceException(t);
            }

            // at this point we're sure it'a service exception
            ServiceException se = (ServiceException) cause;
            if (cause != t) {
                // copy the message, code + locator, but set cause equal to root
                se = new ServiceException(se.getMessage(), t, se.getCode(), se.getLocator());
            }

            handleServiceException(se, service, request);
        }

        request.error = t;
    }

    void handleServiceException(ServiceException se, Service service, Request request) {
        // find an exception handler
        ServiceExceptionHandler handler = null;

        if (service != null) {
            // look up the service exception handler
            Collection handlers = GeoServerExtensions.extensions(ServiceExceptionHandler.class);
            for (Object o : handlers) {
                ServiceExceptionHandler seh = (ServiceExceptionHandler) o;

                if (seh.getServices().contains(service)) {
                    // found one,
                    handler = seh;

                    break;
                }
            }
        }

        if (handler == null) {
            // none found, fall back on default
            handler = new OWS10ServiceExceptionHandler();
        }

        // if SOAP request use special SOAP exception handler, but only for OWS requests because
        // there could be other service exception handlers (like WMS for instance) that do not
        // output XML
        if (request.isSOAP()
                && (handler instanceof OWS10ServiceExceptionHandler
                        || handler instanceof OWS11ServiceExceptionHandler)) {
            handler = new SOAPServiceExceptionHandler(handler);
        }

        // remove content disposition if set (to allow browsers inline display of exception, and
        // avoid
        // proposing to save a file with the wrong extension that some OSs won't know how to open)
        HttpServletResponse httpResponse = request.getHttpResponse();
        if (httpResponse.containsHeader(HttpHeaders.CONTENT_DISPOSITION)) {
            try {
                httpResponse.setHeader(HttpHeaders.CONTENT_DISPOSITION, null);
            } catch (Exception e) {
                // the spec is not clear about setting null, but reportedly works
                // in Jetty, Tomcat, Glassfish... Not in your test harness though
                logger.log(Level.FINE, "Failed to reset content disposition header", e);
            }
        }

        handler.handleServiceException(se, request);
    }

    /**
     * Examines a {@link Throwable} object and returns true if it represents a security exception.
     *
     * @param t Throwable
     * @return true if t is a security exception
     */
    public static boolean isSecurityException(Throwable t) {
        return t != null
                && t.getClass().getPackage().getName().startsWith("org.springframework.security");
    }
}
