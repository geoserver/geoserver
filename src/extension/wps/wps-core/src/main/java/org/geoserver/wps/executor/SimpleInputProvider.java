/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.opengis.wcs11.GetCoverageType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wps10.ComplexDataType;
import net.opengis.wps10.DataType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.HeaderType;
import net.opengis.wps10.InputReferenceType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.LiteralDataType;
import net.opengis.wps10.MethodType;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wcs.WebCoverageService100;
import org.geoserver.wcs.WebCoverageService111;
import org.geoserver.wfs.WebFeatureService;
import org.geoserver.wfs.kvp.GetFeatureKvpRequestReader;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.kvp.ExecuteKvpRequestReader;
import org.geoserver.wps.ppio.BoundingBoxPPIO;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.LiteralPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.ppio.RawDataPPIO;
import org.geoserver.wps.process.StringRawData;
import org.geoserver.wps.resource.GridCoverageResource;
import org.opengis.coverage.grid.GridCoverage;
import org.springframework.context.ApplicationContext;

/**
 * Performs lazy parsing of a specific input
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
class SimpleInputProvider implements InputProvider {
    InputType input;

    ProcessParameterIO ppio;

    Object value;

    ApplicationContext context;

    WPSExecutionManager executor;

    String inputId;

    public SimpleInputProvider(InputType input, ProcessParameterIO ppio, WPSExecutionManager executor,
            ApplicationContext context) {
        this.input = input;
        this.ppio = ppio;
        this.context = context;
        this.executor = executor;
        this.inputId = input.getIdentifier().getValue();
    }
    
    public String getInputId() {
        return inputId;
    }
    
    public boolean longParse() {
        if(input.getReference() == null) {
            return false;
        } else {
            InputReferenceType ref = input.getReference();

            // grab the location and method
            String href = ref.getHref();

            if (href.startsWith("http://geoserver/wfs")) {
                // we get a collection almost instantly
                return false;
            } else if (href.startsWith("http://geoserver/wcs")) {
                // same here, most of the time we get a coverage reference almost instantly
                return false;
            } else {
                return true;
            }
        }
    }

    public Object getValue() throws Exception {
        if (value == null) {
            if (input.getReference() != null) {
                // this is a reference
                InputReferenceType ref = input.getReference();

                // grab the location and method
                String href = ref.getHref();

                if (href.startsWith("http://geoserver/wfs")) {
                    value = handleAsInternalWFS(ppio, ref);
                } else if (href.startsWith("http://geoserver/wcs")) {
                    value = handleAsInternalWCS(ppio, ref);
                } else if (href.startsWith("http://geoserver/wps")) {
                    value = handleAsInternalWPS(ppio, ref);
                } else {
                    value = executeRemoteRequest(ref, (ComplexPPIO) ppio, inputId);
                }

            } else {
                // actual data, figure out which type
                DataType data = input.getData();

                if (data.getLiteralData() != null) {
                    LiteralDataType literal = data.getLiteralData();
                    value = ((LiteralPPIO) ppio).decode(literal.getValue());
                } else if (data.getComplexData() != null) {
                    ComplexDataType complex = data.getComplexData();
                    if (ppio instanceof RawDataPPIO) {
                        String content = complex.getData().get(0).toString();
                        return new StringRawData(content, complex.getMimeType());
                    } else {
                        value = ((ComplexPPIO) ppio).decode(complex.getData().get(0));
                    }
                } else if (data.getBoundingBoxData() != null) {
                    value = ((BoundingBoxPPIO) ppio).decode(data.getBoundingBoxData());
                }

            }
            
            if(value instanceof GridCoverage) {
                executor.getResourceManager().addResource(new GridCoverageResource((GridCoverage) value));
            }
            
            // release the input, it's not needed anymore 
            input = null;
        }

        return value;
    }

    /**
     * Process the request as an internal one, without going through GML encoding/decoding
     * 
     * @param ppio
     * @param ref
     * @param method
     * @return
     * @throws Exception
     */
    Object handleAsInternalWFS(ProcessParameterIO ppio, InputReferenceType ref) throws Exception {
        WebFeatureService wfs = (WebFeatureService) context.getBean("wfsServiceTarget");
        GetFeatureType gft = null;
        if (ref.getMethod() == MethodType.POST_LITERAL) {
            gft = (GetFeatureType) ref.getBody();
        } else {
            GetFeatureKvpRequestReader reader = (GetFeatureKvpRequestReader) context
                    .getBean("getFeatureKvpReader");
            gft = (GetFeatureType) kvpParse(ref.getHref(), reader);
        }

        FeatureCollectionResponse featureCollectionType = wfs.getFeature(gft);
        // this will also deal with axis order issues
        return ((ComplexPPIO) ppio).decode(featureCollectionType.getAdaptee());
    }

    /**
     * Process the request as an internal one, without going through GML encoding/decoding
     * 
     * @param ppio
     * @param ref
     * @param method
     * @return
     * @throws Exception
     */
    Object handleAsInternalWPS(ProcessParameterIO ppio, InputReferenceType ref) throws Exception {
        ExecuteType request = null;
        if (ref.getMethod() == MethodType.POST_LITERAL) {
            request = (ExecuteType) ref.getBody();
        } else {
            ExecuteKvpRequestReader reader = (ExecuteKvpRequestReader) context
                    .getBean("executeKvpRequestReader");
            request = (ExecuteType) kvpParse(ref.getHref(), reader);
        }

        Map<String, Object> results = executor.submitChained(new ExecuteRequest(request));
        Object obj = results.values().iterator().next();
        if (obj != null && !ppio.getType().isInstance(obj)) {
            throw new WPSException(
                    "The process output is incompatible with the input target type, was expecting "
                            + ppio.getType().getName() + " and got " + obj.getClass().getName());
        }
        return obj;
    }

    /**
     * Process the request as an internal one, without going through GML encoding/decoding
     * 
     * @param ppio
     * @param ref
     * @param method
     * @return
     * @throws Exception
     */
    Object handleAsInternalWCS(ProcessParameterIO ppio, InputReferenceType ref) throws Exception {
        // first parse the request, it might be a WCS 1.0 or a WCS 1.1 one
        Object getCoverage = null;
        if (ref.getMethod() == MethodType.POST_LITERAL) {
            getCoverage = ref.getBody();
        } else {
            // what WCS version?
            String version = getVersion(ref.getHref());
            KvpRequestReader reader;
            if (version.equals("1.0.0") || version.equals("1.0")) {
                reader = (KvpRequestReader) context.getBean("wcs100GetCoverageRequestReader");
            } else {
                reader = (KvpRequestReader) context.getBean("wcs111GetCoverageRequestReader");
            }

            getCoverage = kvpParse(ref.getHref(), reader);
        }

        // perform GetCoverage
        if (getCoverage instanceof GetCoverageType) {
            WebCoverageService111 wcs = (WebCoverageService111) context
                    .getBean("wcs111ServiceTarget");
            return wcs.getCoverage((net.opengis.wcs11.GetCoverageType) getCoverage)[0];
        } else if (getCoverage instanceof net.opengis.wcs10.GetCoverageType) {
            WebCoverageService100 wcs = (WebCoverageService100) context
                    .getBean("wcs100ServiceTarget");
            return wcs.getCoverage((net.opengis.wcs10.GetCoverageType) getCoverage)[0];
        } else {
            throw new WPSException("Unrecognized request type " + getCoverage);
        }
    }

    /**
     * Executes
     * 
     * @param ref
     * @return
     */
    Object executeRemoteRequest(InputReferenceType ref, ComplexPPIO ppio, String inputId)
            throws Exception {
        URL destination = new URL(ref.getHref());

        HttpMethod method = null;
        GetMethod refMethod = null;
        InputStream input = null;
        InputStream refInput = null;

        // execute the request
        try {
            if ("http".equalsIgnoreCase(destination.getProtocol())) {
                // setup the client
                HttpClient client = new HttpClient();
                // setting timeouts (30 seconds, TODO: make this configurable)
                HttpConnectionManagerParams params = new HttpConnectionManagerParams();
                params.setSoTimeout(executor.getConnectionTimeout());
                params.setConnectionTimeout(executor.getConnectionTimeout());
                // TODO: make the http client a well behaved http client, no more than x connections
                // per server (x admin configurable maybe), persistent connections and so on
                HttpConnectionManager manager = new SimpleHttpConnectionManager();
                manager.setParams(params);
                client.setHttpConnectionManager(manager);

                // prepare either a GET or a POST request
                if (ref.getMethod() == null || ref.getMethod() == MethodType.GET_LITERAL) {
                    GetMethod get = new GetMethod(ref.getHref());
                    get.setFollowRedirects(true);
                    method = get;
                } else {
                    String encoding = ref.getEncoding();
                    if (encoding == null) {
                        encoding = "UTF-8";
                    }

                    PostMethod post = new PostMethod(ref.getHref());
                    Object body = ref.getBody();
                    if (body == null) {
                        if (ref.getBodyReference() != null) {
                            URL refDestination = new URL(ref.getBodyReference().getHref());
                            if ("http".equalsIgnoreCase(refDestination.getProtocol())) {
                                // open with commons http client
                                refMethod = new GetMethod(ref.getBodyReference().getHref());
                                refMethod.setFollowRedirects(true);
                                client.executeMethod(refMethod);
                                refInput = refMethod.getResponseBodyAsStream();
                            } else {
                                // open with the built-in url management
                                URLConnection conn = refDestination.openConnection();
                                conn.setConnectTimeout(executor.getConnectionTimeout());
                                conn.setReadTimeout(executor.getConnectionTimeout());
                                refInput = conn.getInputStream();
                            }
                            post.setRequestEntity(new InputStreamRequestEntity(refInput, ppio
                                    .getMimeType()));
                        } else {
                            throw new WPSException("A POST request should contain a non empty body");
                        }
                    } else if (body instanceof String) {
                        post.setRequestEntity(new StringRequestEntity((String) body, ppio
                                .getMimeType(), encoding));
                    } else {
                        throw new WPSException(
                                "The request body should be contained in a CDATA section, "
                                        + "otherwise it will get parsed as XML instead of being preserved as is");

                    }
                    method = post;
                }
                // add eventual extra headers
                if (ref.getHeader() != null) {
                    for (Iterator it = ref.getHeader().iterator(); it.hasNext();) {
                        HeaderType header = (HeaderType) it.next();
                        method.setRequestHeader(header.getKey(), header.getValue());
                    }
                }
                int code = client.executeMethod(method);

                if (code == 200) {
                    input = method.getResponseBodyAsStream();
                } else {
                    throw new WPSException("Error getting remote resources from " + ref.getHref()
                            + ", http error " + code + ": " + method.getStatusText());
                }
            } else {
                // use the normal url connection methods then...
                URLConnection conn = destination.openConnection();
                conn.setConnectTimeout(executor.getConnectionTimeout());
                conn.setReadTimeout(executor.getConnectionTimeout());
                input = conn.getInputStream();
            }

            // actually parse teh data
            if (input != null) {
                return ppio.decode(input);
            } else {
                throw new WPSException("Could not find a mean to read input " + inputId);
            }
        } finally {
            // make sure to close the connection and streams no matter what
            if (input != null) {
                input.close();
            }
            if (method != null) {
                method.releaseConnection();
            }
            if (refMethod != null) {
                refMethod.releaseConnection();
            }
        }
    }

    /**
     * Simulates what the Dispatcher is doing when parsing a KVP request
     * 
     * @param href
     * @param reader
     * @return
     */
    Object kvpParse(String href, KvpRequestReader reader) throws Exception {
        Map original = new KvpMap(KvpUtils.parseQueryString(href));
        KvpUtils.normalize(original);
        Map parsed = new KvpMap(original);
        List<Throwable> errors = KvpUtils.parse(parsed);
        if (errors.size() > 0) {
            throw new WPSException("Failed to parse KVP request", errors.get(0));
        }

        // hack to allow wcs filters to work... we should really upgrade the WCS models instead...
        Request r = Dispatcher.REQUEST.get();
        if (r != null) {
            Map kvp = new HashMap(r.getKvp());
            r.setKvp(new CaseInsensitiveMap(parsed));
        }

        return reader.read(reader.createRequest(), parsed, original);
    }

    /**
     * Returns the version from the kvp request
     * 
     * @param href
     * @return
     */
    String getVersion(String href) {
        return (String) new KvpMap(KvpUtils.parseQueryString(href)).get("VERSION");
    }

    @Override
    public boolean resolved() {
        return value != null;
    }
}
