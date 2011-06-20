/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import net.opengis.ows11.BoundingBoxType;
import net.opengis.wcs11.GetCoverageType;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wps10.ComplexDataType;
import net.opengis.wps10.DataType;
import net.opengis.wps10.DocumentOutputDefinitionType;
import net.opengis.wps10.ExecuteResponseType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.HeaderType;
import net.opengis.wps10.InputReferenceType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.LiteralDataType;
import net.opengis.wps10.MethodType;
import net.opengis.wps10.OutputDataType;
import net.opengis.wps10.OutputDefinitionType;
import net.opengis.wps10.OutputDefinitionsType;
import net.opengis.wps10.OutputReferenceType;
import net.opengis.wps10.ProcessBriefType;
import net.opengis.wps10.ProcessOutputsType1;
import net.opengis.wps10.Wps10Factory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.ows.Ows11Util;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wcs.WebCoverageService100;
import org.geoserver.wcs.WebCoverageService111;
import org.geoserver.wfs.WebFeatureService;
import org.geoserver.wfs.kvp.GetFeatureKvpRequestReader;
import org.geoserver.wps.kvp.ExecuteKvpRequestReader;
import org.geoserver.wps.ppio.BinaryPPIO;
import org.geoserver.wps.ppio.BoundingBoxPPIO;
import org.geoserver.wps.ppio.CDataPPIO;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.LiteralPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.ppio.ReferencePPIO;
import org.geoserver.wps.ppio.XMLPPIO;
import org.geotools.data.Parameter;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.geotools.util.Converters;
import org.geotools.xml.EMFUtils;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;
import org.opengis.util.ProgressListener;
import org.springframework.context.ApplicationContext;

/**
 * Main class used to handle Execute requests
 * 
 * @author Lucas Reed, Refractions Research Inc
 * @author Andrea Aime, OpenGeo
 */
public class Execute {
    
    int connectionTimeout;
    
    WPSInfo wps;

    GeoServerInfo gs;

    ApplicationContext context;

    public Execute(WPSInfo wps, GeoServerInfo gs, ApplicationContext context) {
        this.wps = wps;
        this.gs = gs;
        this.context = context;
        double timeout = wps.getConnectionTimeout();
        
        // The specified timeout is in seconds. Convert it to milliseconds  
        if (timeout >= 0) {
            this.connectionTimeout = (int) (timeout * 1000);
        } else {
            // specified timeout == -1 represents infinite timeout.
            // by convention, for infinite URLConnection timeouts, we need to use zero. 
            this.connectionTimeout = 0;
        }
    }

    /**
     * Main method for performing decoding, execution, and response
     * 
     * @param object
     * @param output
     * @throws IllegalArgumentException
     */
    public ExecuteResponseType run(ExecuteType request) {
        // note the current time
        Date started = Calendar.getInstance().getTime();

        // perform the execution and grab the results
        Map<String, ProcessOutput> outputMap = executeInternal(request);
        
        // build the response
        Wps10Factory f = Wps10Factory.eINSTANCE;
        ExecuteResponseType response = f.createExecuteResponseType();
        response.setLang("en");
        if(request.getBaseUrl() != null) {
        	response.setServiceInstance(ResponseUtils.appendQueryString(ResponseUtils.buildURL(request
                .getBaseUrl(), "ows", null, URLType.SERVICE), ""));
        }

        // process
        Name processName = Ows11Util.name(request.getIdentifier());
        ProcessFactory pf = Processors.createProcessFactory(processName);
        final ProcessBriefType process = f.createProcessBriefType();
        response.setProcess(process);
        process.setIdentifier(request.getIdentifier());
        process.setProcessVersion(pf.getVersion(processName));
        process.setTitle(Ows11Util.languageString(pf.getTitle(processName)));
        process.setAbstract(Ows11Util.languageString(pf.getDescription(processName)));

        // status
        response.setStatus(f.createStatusType());
        response.getStatus().setCreationTime(
                Converters.convert(started, XMLGregorianCalendar.class));
        response.getStatus().setProcessSucceeded("Process succeeded.");

        // inputs
        response.setDataInputs(f.createDataInputsType1());
        for (Iterator i = request.getDataInputs().getInput().iterator(); i.hasNext();) {
            InputType input = (InputType) i.next();
            response.getDataInputs().getInput().add(EMFUtils.clone(input, f, true));
        }

        // output definitions
        OutputDefinitionsType outputs = f.createOutputDefinitionsType();
        response.setOutputDefinitions(outputs);

        Map<String, Parameter<?>> outs = pf.getResultInfo(processName, null);
        Map<String, ProcessParameterIO> ppios = new HashMap();

        for (String key : outputMap.keySet()) {
            Parameter p = pf.getResultInfo(processName, null).get(key);
            if (p == null) {
                throw new WPSException("No such output: " + key);
            }

            // find the ppio
            String mime = outputMap.get(key).definition.getMimeType();
            ProcessParameterIO ppio = ProcessParameterIO.find(p, context, mime);
            if (ppio == null) {
                throw new WPSException("Unable to encode output: " + p.key);
            }
            ppios.put(p.key, ppio);

            DocumentOutputDefinitionType output = f.createDocumentOutputDefinitionType();
            outputs.getOutput().add(output);

            output.setIdentifier(Ows11Util.code(p.key));
            if (ppio instanceof ComplexPPIO) {
                output.setMimeType(((ComplexPPIO) ppio).getMimeType());
                if (ppio instanceof BinaryPPIO) {
                    output.setEncoding("base64");
                } else if (ppio instanceof XMLPPIO) {
                    output.setEncoding("utf-8");
                }
            }

            // TODO: better encoding handling + schema
        }

        // process outputs
        ProcessOutputsType1 processOutputs = f.createProcessOutputsType1();
        response.setProcessOutputs(processOutputs);

        for (String key : outputMap.keySet()) {
            OutputDataType output = f.createOutputDataType();
            output.setIdentifier(Ows11Util.code(key));
            output.setTitle(Ows11Util
                    .languageString(pf.getResultInfo(processName, null).get(key).description));
            processOutputs.getOutput().add(output);

            final Object o = outputMap.get(key).object;
            ProcessParameterIO ppio = ppios.get(key);

            if (ppio instanceof ReferencePPIO) {
                // encode as a reference
                OutputReferenceType ref = f.createOutputReferenceType();
                output.setReference(ref);

                ref.setMimeType(outputMap.get(key).definition.getMimeType());
                ref.setHref(((ReferencePPIO) ppio).encode(o).toString());
            } else {
                // encode as data
                DataType data = f.createDataType();
                output.setData(data);

                try {
	                if (ppio instanceof LiteralPPIO) {
	                    LiteralDataType literal = f.createLiteralDataType();
	                    data.setLiteralData(literal);
	
	                    literal.setValue(((LiteralPPIO) ppio).encode(o));
	                } else if (ppio instanceof BoundingBoxPPIO) {
	                    BoundingBoxType bbox = ((BoundingBoxPPIO) ppio).encode(o);
	                    data.setBoundingBoxData(bbox);
	                } else if (ppio instanceof ComplexPPIO) {
	                    ComplexDataType complex = f.createComplexDataType();
	                    data.setComplexData(complex);
	
	                    ComplexPPIO cppio = (ComplexPPIO) ppio;
	                    complex.setMimeType(cppio.getMimeType());
	
	                    if (cppio instanceof XMLPPIO) {
	                        // encode directly
	                        complex.getData().add(new XMLEncoderDelegate((XMLPPIO) cppio, o));
	                    } else if (cppio instanceof CDataPPIO) {
	                        complex.getData().add(new CDataEncoderDelegate((CDataPPIO) cppio, o));
	                    } else if (cppio instanceof BinaryPPIO) {
	                        complex.getData().add(new BinaryEncoderDelegate((BinaryPPIO) cppio, o));
	                    } else {
	                        throw new WPSException("Don't know how to encode an output whose PPIO is "
	                                + cppio);
	                    }
	                }
                } catch(Exception e) {
                	throw new WPSException("Failed to encode the " + key + " output", e);
                }
            }
        }

        return response;
    }
    
    Map<String, ProcessOutput> executeInternal(ExecuteType request) {
    	// load the process factory
        Name processName = Ows11Util.name(request.getIdentifier());
        ProcessFactory pf = Processors.createProcessFactory(processName);
        if (pf == null) {
            throw new WPSException("No such process: " + processName);
        }

        // parse the inputs into in memory representations the process can handle
        Map<String, Object> inputs = parseProcessInputs(request, processName, pf);

        // execute the process
        Map<String, Object> result = null;
        ProcessListener listener = new ProcessListener();
        Throwable exception = null;
        try {
            Process p = pf.create(processName);
            result = p.execute(inputs, listener);
        } catch (Exception e) {
            exception = e;
        }
        
        // if no direct exception, check if failure occurred from the listener
        if(exception == null) {
        	exception = listener.exception;
        }
        // if we got any exception report back with a service exception
        if(exception != null) {
        	if(exception instanceof WPSException) {
        		throw (WPSException) exception;
        	} else if(exception instanceof ProcessException) {
            	throw new WPSException("Process returned with an exception", exception);
            } else {
            	throw new WPSException("InternalError: " + exception.getMessage(), exception);
            }
        }

        // filter out the results we have not been asked about
        // and create a direct map between required outputs and
        // the gt process outputs
        Map<String, ProcessOutput> outputMap = new HashMap<String, ProcessOutput>();
        if (request.getResponseForm().getRawDataOutput() != null) {
            // only one output in raw form
            OutputDefinitionType od = request.getResponseForm().getRawDataOutput();
            String outputName = od.getIdentifier().getValue();
            outputMap.put(outputName, new ProcessOutput(od, result.get(outputName)));
        } else {
            for (Iterator it = request.getResponseForm().getResponseDocument().getOutput()
                    .iterator(); it.hasNext();) {
                OutputDefinitionType od = (OutputDefinitionType) it.next();
                String outputName = od.getIdentifier().getValue();
                outputMap.put(outputName, new ProcessOutput(od, result.get(outputName)));
            }
        }

        return outputMap;
    }

    /**
     * Parses the process inputs into a {@link Map} using the various {@link ProcessParameterIO}
     * implementations
     * 
     * @param request
     * @param processName
     * @param pf
     */
    Map<String, Object> parseProcessInputs(ExecuteType request, Name processName, ProcessFactory pf) {
        Map<String, Object> inputs = new HashMap<String, Object>();
        final Map<String, Parameter<?>> parameters = pf.getParameterInfo(processName);
        for (Iterator i = request.getDataInputs().getInput().iterator(); i.hasNext();) {
            InputType input = (InputType) i.next();
            String inputId = input.getIdentifier().getValue();

            // locate the parameter for this request
            Parameter p = parameters.get(inputId);
            if (p == null) {
                throw new WPSException("No such parameter: " + inputId);
            }

            // find the ppio
            String mime = null;
            if (input.getData() != null && input.getData().getComplexData() != null) {
                mime = input.getData().getComplexData().getMimeType();
            } else if (input.getReference() != null) {
                mime = input.getReference().getMimeType();
            }
            ProcessParameterIO ppio = ProcessParameterIO.find(p, context, mime);
            if (ppio == null) {
                throw new WPSException("Unable to decode input: " + inputId);
            }

            // read the data
            Object decoded = null;
            try {
                if (input.getReference() != null) {
                    // this is a reference
                    InputReferenceType ref = input.getReference();

                    // grab the location and method
                    String href = ref.getHref();

                    if (href.startsWith("http://geoserver/wfs")) {
                        decoded = handleAsInternalWFS(ppio, ref);
                    } else if (href.startsWith("http://geoserver/wcs")) {
                        decoded = handleAsInternalWCS(ppio, ref);
                    } else if (href.startsWith("http://geoserver/wps")) {
                        decoded = handleAsInternalWPS(ppio, ref);
                    } else {
                        decoded = executeRemoteRequest(ref, (ComplexPPIO) ppio, inputId);
                    }

                } else {
                    // actual data, figure out which type
                    DataType data = input.getData();

                    if (data.getLiteralData() != null) {
                        LiteralDataType literal = data.getLiteralData();
                        decoded = ((LiteralPPIO) ppio).decode(literal.getValue());
                    } else if (data.getComplexData() != null) {
                        ComplexDataType complex = data.getComplexData();
                        decoded = ((ComplexPPIO) ppio).decode(complex.getData().get(0));
                    } else if (data.getBoundingBoxData() != null) {
                        decoded = ((BoundingBoxPPIO) ppio).decode(data.getBoundingBoxData());
                    }

                }
            } catch (Exception e) {
                throw new WPSException("Unable to decode input: " + inputId, e);
            }
            
            // store the input
            if(p.maxOccurs > 1) {
                Collection values = (Collection) inputs.get(p.key);
                if(values == null) {
                    values = new ArrayList();
                } 
                values.add(decoded);
                inputs.put(p.key, values);
            } else {
                inputs.put(p.key, decoded);
            }
            
        }
        
        return inputs;
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

        FeatureCollectionType featureCollectionType = wfs.getFeature(gft);
        // this will also deal with axis order issues
        return ((ComplexPPIO) ppio).decode(featureCollectionType);
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
            ExecuteKvpRequestReader reader = (ExecuteKvpRequestReader) context.getBean("executeKvpRequestReader");
            request = (ExecuteType) kvpParse(ref.getHref(), reader);
        }
        
        Map<String, ProcessOutput> results = executeInternal(request);
        Object obj = results.values().iterator().next().object;
        if(obj != null && !ppio.getType().isInstance(obj)) {
			throw new WPSException(
					"The process output is incompatible with the input target type, was expecting "
							+ ppio.getType().getName() + " and got "
							+ obj.getClass().getName());
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
            if(version.equals("1.0.0") || version.equals("1.0")) {
                reader = (KvpRequestReader) context.getBean("wcs100GetCoverageRequestReader");
            } else {
                reader = (KvpRequestReader) context.getBean("wcs111GetCoverageRequestReader");
            }
            
            
            getCoverage =  kvpParse(ref.getHref(), reader);
        }
        
        // perform GetCoverage
        if(getCoverage instanceof GetCoverageType) {
            WebCoverageService111 wcs = (WebCoverageService111) context.getBean("wcs111ServiceTarget");
            return wcs.getCoverage((net.opengis.wcs11.GetCoverageType) getCoverage)[0];
        } else if(getCoverage instanceof net.opengis.wcs10.GetCoverageType) {
            WebCoverageService100 wcs = (WebCoverageService100) context.getBean("wcs100ServiceTarget");
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
            if("http".equalsIgnoreCase(destination.getProtocol())) {
                // setup the client
                HttpClient client = new HttpClient();
                // setting timeouts (30 seconds, TODO: make this configurable)
                HttpConnectionManagerParams params = new HttpConnectionManagerParams();
                params.setSoTimeout(connectionTimeout);
                params.setConnectionTimeout(connectionTimeout);
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
                        if(ref.getBodyReference() != null) {
                            URL refDestination = new URL(ref.getBodyReference().getHref());
                            if("http".equalsIgnoreCase(refDestination.getProtocol())) {
                                // open with commons http client
                                refMethod = new GetMethod(ref.getBodyReference().getHref());
                                refMethod.setFollowRedirects(true);
                                client.executeMethod(refMethod);
                                refInput = refMethod.getResponseBodyAsStream();
                            } else {
                                // open with the built-in url management
                                URLConnection conn = refDestination.openConnection();
                                conn.setConnectTimeout(connectionTimeout);
                                conn.setReadTimeout(connectionTimeout);
                                refInput = conn.getInputStream();
                            }
                            post.setRequestEntity(new InputStreamRequestEntity(refInput, ppio.getMimeType()));
                        } else {
                            throw new WPSException("A POST request should contain a non empty body");
                        }
                    } else if (body instanceof String) {
                        post.setRequestEntity(new StringRequestEntity((String) body,
                                ppio.getMimeType(), encoding));
                    } else {
                        throw new WPSException(
                                "The request body should be contained in a CDATA section, "
                                        + "otherwise it will get parsed as XML instead of being preserved as is");
    
                    }
                    method = post;
                }
                // add eventual extra headers
                if(ref.getHeader() != null) {
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
                conn.setConnectTimeout(connectionTimeout);
                conn.setReadTimeout(connectionTimeout);
                input = conn.getInputStream();
            }
            
            // actually parse teh data
            if(input != null) {
                return ppio.decode(input);
            } else {
                throw new WPSException("Could not find a mean to read input " + inputId);
            }
        } finally {
            // make sure to close the connection and streams no matter what
            if(input != null) {
                input.close();
            }
            if (method != null) {
                method.releaseConnection();
            }
            if(refMethod != null) {
                refMethod.releaseConnection();
            }
        }
    }
    
    /**
     * Simulates what the Dispatcher is doing when parsing a KVP request
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
        if(r != null) {
            Map kvp = new HashMap(r.getKvp());
            r.setKvp(new CaseInsensitiveMap(parsed));
        }
        
        return reader.read(reader.createRequest(), parsed, original);
    }
    
    /**
     * Returns the version from the kvp request
     * @param href
     * @return
     */
    String getVersion(String href) {
        return (String) new KvpMap(KvpUtils.parseQueryString(href)).get("VERSION");
    }

    
    static class ProcessOutput {
    	OutputDefinitionType definition;
    	Object object;
		
    	public ProcessOutput(OutputDefinitionType definition, Object object) {
			super();
			this.definition = definition;
			this.object = object;
		}
    }
    
    /**
     * A process listener. For the moment just used to check if the process execution failed
     * @author Andrea Aime - OpenGeo
     *
     */
    static class ProcessListener implements ProgressListener {
        Throwable exception; 

        public void complete() {
            // TODO Auto-generated method stub
            
        }

        public void dispose() {
            // TODO Auto-generated method stub
            
        }

        public void exceptionOccurred(Throwable exception) {
            this.exception = exception;
            
        }

        public String getDescription() {
            // TODO Auto-generated method stub
            return null;
        }

        public float getProgress() {
            // TODO Auto-generated method stub
            return 0;
        }

        public InternationalString getTask() {
            // TODO Auto-generated method stub
            return null;
        }

        public boolean isCanceled() {
            // TODO Auto-generated method stub
            return false;
        }

        public void progress(float percent) {
            // TODO Auto-generated method stub
            
        }

        public void setCanceled(boolean cancel) {
            // TODO Auto-generated method stub
            
        }

        public void setDescription(String description) {
            // TODO Auto-generated method stub
            
        }

        public void setTask(InternationalString task) {
            // TODO Auto-generated method stub
            
        }

        public void started() {
            // TODO Auto-generated method stub
            
        }

        public void warningOccurred(String source, String location, String warning) {
            // TODO Auto-generated method stub
            
        }
        
    }
    
}