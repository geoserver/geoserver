/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import static org.geoserver.ogcapi.MappingJackson2YAMLMessageConverter.APPLICATION_YAML_VALUE;
import static org.geoserver.ogcapi.OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE;

import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import net.opengis.ows11.BoundingBoxType;
import net.opengis.ows11.ExceptionType;
import net.opengis.wps10.ComplexDataType;
import net.opengis.wps10.DataInputsType1;
import net.opengis.wps10.DataType;
import net.opengis.wps10.ExecuteResponseType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.LiteralDataType;
import net.opengis.wps10.OutputDataType;
import net.opengis.wps10.OutputDefinitionType;
import net.opengis.wps10.ResponseFormType;
import net.opengis.wps10.Wps10Factory;
import org.eclipse.emf.common.util.EList;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.ConformanceClass;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.ows.Ows11Util;
import org.geoserver.platform.ServiceException;
import org.geoserver.wps.BinaryEncoderDelegate;
import org.geoserver.wps.CDataEncoderDelegate;
import org.geoserver.wps.DefaultWebProcessingService;
import org.geoserver.wps.RawDataEncoderDelegate;
import org.geoserver.wps.WPSInfo;
import org.geoserver.wps.XMLEncoderDelegate;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.LiteralPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.api.data.Parameter;
import org.geotools.api.feature.type.Name;
import org.geotools.feature.NameImpl;
import org.geotools.process.ProcessFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@APIService(service = "Processes", version = "1.0.0", landingPage = "ogc/processes/v1", serviceClass = WPSInfo.class)
@RequestMapping(path = APIDispatcher.ROOT_PATH + "/processes/v1")
public class ProcessesService {

    public static final String CONF_CLASS_PROCESSES_CORE =
            "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/core";
    public static final String CONF_CLASS_PROCESS_DESCRIPTION =
            "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/ogc-process-description";
    public static final String CONF_CLASS_CALLBACK = "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/callback";
    public static final String CONF_CLASS_JSON = "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/json";
    public static final String CONF_CLASS_HTML = "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/html";
    public static final String CONF_CLASS_OAS3 = "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/oas30";
    public static final String CONF_JOB_LIST = "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/job-list";
    public static final String CONF_CALLBACK = "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/callback";
    public static final String CONF_DISMISS = "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/dismiss";
    // this one comes from the current DRAFT
    // https://docs.ogc.org/DRAFTS/18-062r3.html#_requirements_class_kvp_encoded_execute
    public static final String CONF_KVP_EXECUTE = "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/kvp-execute";
    // The response format parameter for KVP encoded execute
    public static final String RESPONSE_FORMAT_KVP = "response[f]";

    private final GeoServer geoServer;
    private final DefaultWebProcessingService wps;
    private final ApplicationContext context;

    public ProcessesService(GeoServer geoServer, DefaultWebProcessingService wps, ApplicationContext context) {
        this.geoServer = geoServer;
        this.wps = wps;
        this.context = context;
    }

    public WPSInfo getServiceInfo() {
        // required for DisabledServiceCheck class
        return geoServer.getService(WPSInfo.class);
    }

    @GetMapping(name = "getLandingPage")
    @ResponseBody
    @HTMLResponseBody(templateName = "landingPage.ftl", fileName = "landingPage.html")
    public ProcessesLandingPage landingPage() {
        return new ProcessesLandingPage(getServiceInfo(), "ogc/processes/v1");
    }

    @GetMapping(
            path = {"openapi", "openapi.json", "openapi.yaml"},
            name = "getApi",
            produces = {OPEN_API_MEDIA_TYPE_VALUE, APPLICATION_YAML_VALUE, MediaType.TEXT_XML_VALUE})
    @ResponseBody
    @HTMLResponseBody(templateName = "api.ftl", fileName = "api.html")
    public OpenAPI api() throws IOException {
        return new ProcessesAPIBuilder().build(getServiceInfo());
    }

    @GetMapping(path = "conformance", name = "getConformanceDeclaration")
    @ResponseBody
    @HTMLResponseBody(templateName = "conformance.ftl", fileName = "conformance.html")
    public ConformanceDocument conformance() {
        List<String> classes = Arrays.asList(
                ConformanceClass.CORE,
                ConformanceClass.OAS3,
                CONF_CLASS_PROCESSES_CORE,
                CONF_CLASS_PROCESS_DESCRIPTION,
                CONF_CLASS_HTML,
                CONF_CLASS_JSON,
                CONF_KVP_EXECUTE
                // TODO: add job listing, callback, dismiss
                );
        return new ConformanceDocument("OGC API Processes", classes);
    }

    @GetMapping(path = "processes", name = "getProcessList")
    @ResponseBody
    @HTMLResponseBody(templateName = "processes.ftl", fileName = "processes.html")
    public ProcessListDocument processes() {
        return new ProcessListDocument();
    }

    @GetMapping(path = "processes/{processId}", name = "getProcessDescription")
    @ResponseBody
    @HTMLResponseBody(templateName = "process.ftl", fileName = "process.html")
    public ProcessDocument describeProcess(@PathVariable("processId") String processId) {
        Name name = toName(processId);
        ProcessFactory pf = GeoServerProcessors.createProcessFactory(name, true);
        if (pf == null) {
            throw new APIException(
                    ServiceException.INVALID_PARAMETER_VALUE, "Process not found: " + processId, HttpStatus.NOT_FOUND);
        }
        return new ProcessDocument(pf, name, context);
    }

    private static NameImpl toName(String processId) {
        int idx = processId.indexOf(':');
        if (idx < 0) return new NameImpl(processId);
        String namespace = processId.substring(0, idx);
        String localPart = processId.substring(idx + 1);
        return new NameImpl(namespace, localPart);
    }

    /** GeoServer specific extension to support execution of simple processes via the OGC API */
    @GetMapping(path = "processes/{processId}/execution", name = "executeProcessKVP")
    public void executeGet(
            @PathVariable("processId") String processId,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse)
            throws Exception {
        Name name = toName(processId);
        ProcessFactory pf = GeoServerProcessors.createProcessFactory(name, true);
        if (pf == null) {
            throw new APIException(
                    ServiceException.INVALID_PARAMETER_VALUE, "Process not found: " + processId, HttpStatus.NOT_FOUND);
        }

        // bridge to the WPS service
        ExecuteType execute = mapQueryStringToExecute(httpRequest, pf, name);
        ExecuteResponseType response = wps.execute(execute);
        writeRawResponse(httpResponse, response);
    }

    /** Writes the raw response directly to the output */
    private void writeRawResponse(HttpServletResponse httpResponse, ExecuteResponseType response) throws IOException {
        if (response.getStatus().getProcessFailed() != null) {
            StringBuffer errors = new StringBuffer();
            EList exceptions =
                    response.getStatus().getProcessFailed().getExceptionReport().getException();
            for (Object ex : exceptions) {
                if (ex instanceof ExceptionType) {
                    ExceptionType exceptionType = (ExceptionType) ex;
                    throw new APIException(
                            exceptionType.getExceptionCode(),
                            exceptionType.getExceptionText().get(0).toString(),
                            HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }

        OutputDataType result =
                (OutputDataType) response.getProcessOutputs().getOutput().get(0);
        LiteralDataType literal = result.getData().getLiteralData();
        BoundingBoxType bbox = result.getData().getBoundingBoxData();
        if (literal != null) {
            httpResponse.setContentType("text/plain");
            httpResponse.getWriter().write(literal.getValue());
        } else if (bbox != null) {
            // TODO: handle bbox responses
            throw new IllegalArgumentException("Cannot handle bounding box responses yet");
        } else {
            writeComplex(
                    httpResponse, result.getData().getComplexData().getData().get(0));
        }
    }

    @SuppressWarnings("unchecked")
    private ExecuteType mapQueryStringToExecute(HttpServletRequest httpRequest, ProcessFactory pf, Name name) {
        Wps10Factory wpsFactory = Wps10Factory.eINSTANCE;
        ExecuteType execute = wpsFactory.createExecuteType();
        execute.setIdentifier(Ows11Util.code(name));
        DataInputsType1 dataInputsType = wpsFactory.createDataInputsType1();
        execute.setDataInputs(dataInputsType);
        for (Map.Entry<String, Parameter<?>> stringParameterEntry :
                pf.getParameterInfo(name).entrySet()) {
            String key = stringParameterEntry.getKey();
            Parameter<?> parameter = stringParameterEntry.getValue();
            String value = httpRequest.getParameter(key);
            if (value == null) {
                if (parameter.getMinOccurs() > 0)
                    throw new APIException(
                            ServiceException.INVALID_PARAMETER_VALUE,
                            "Missing required parameter: " + key,
                            HttpStatus.BAD_REQUEST);
                // otherwise skip it
                continue;
            }
            String type = httpRequest.getParameter(key + "[type]");

            // map to an input
            InputType inputType = wpsFactory.createInputType();
            inputType.setIdentifier(Ows11Util.code(key));
            DataType dataType = wpsFactory.createDataType();
            inputType.setData(dataType);
            List<ProcessParameterIO> ppios = ProcessParameterIO.findDecoder(parameter, context);
            if (ppios.isEmpty()) {
                throw new IllegalArgumentException(
                        "Could not find process parameter for type " + parameter.key + "," + parameter.type);
            }

            if (type == null) {
                // handle the literal case
                if (ppios.get(0) instanceof LiteralPPIO) {
                    LiteralDataType literal = wpsFactory.createLiteralDataType();
                    literal.setValue(value);
                    dataType.setLiteralData(literal);
                } else {
                    // handle the complex case (only one, default)
                    ComplexPPIO complexPPIO = (ComplexPPIO) ppios.get(0);
                    ComplexDataType complex = getComplexDataType(wpsFactory, complexPPIO, value);
                    dataType.setComplexData(complex);
                }
            } else {
                // look up the type among the PPIOs
                for (ProcessParameterIO ppio : ppios) {
                    if (ppio instanceof ComplexPPIO) {
                        ComplexPPIO complexPPIO = (ComplexPPIO) ppio;
                        if (complexPPIO.getMimeType().equalsIgnoreCase(type)) {
                            ComplexDataType complex = getComplexDataType(wpsFactory, complexPPIO, value);
                            dataType.setComplexData(complex);
                            break;
                        }
                    }
                }
            }

            dataInputsType.getInput().add(inputType);
        }

        // handle the outputs, force a response
        Map<String, Parameter<?>> resultInfo = pf.getResultInfo(name, null);
        if (resultInfo.size() > 1) {
            throw new UnsupportedOperationException("Support for multiple outputs not available in GET request mode");
        }
        OutputDefinitionType outputType = wpsFactory.createOutputDefinitionType();
        outputType.setIdentifier(Ows11Util.code(resultInfo.keySet().iterator().next()));
        String responseFormat = httpRequest.getParameter(RESPONSE_FORMAT_KVP);
        if (responseFormat != null && !responseFormat.isEmpty()) {
            setResponseMediaType(outputType, resultInfo, responseFormat);
        }
        ResponseFormType responseFormType = wpsFactory.createResponseFormType();
        responseFormType.setRawDataOutput(outputType);
        execute.setResponseForm(responseFormType);
        return execute;
    }

    private static ComplexDataType getComplexDataType(Wps10Factory wpsFactory, ComplexPPIO complexPPIO, String value) {
        ComplexDataType complex = wpsFactory.createComplexDataType();
        complex.setMimeType(complexPPIO.getMimeType());
        complex.getData().add(value);
        return complex;
    }

    private void setResponseMediaType(
            OutputDefinitionType outputType, Map<String, Parameter<?>> resultInfo, String responseFormat) {
        String[] formats = responseFormat.split("\\s*,\\s*");
        Set<String> formatsSet = Set.of(formats);
        Parameter<?> result = resultInfo.values().iterator().next();
        List<ProcessParameterIO> encoders = ProcessParameterIO.findEncoder(result, context);
        // look for complex ones
        for (ProcessParameterIO encoder : encoders) {
            if (encoder instanceof ComplexPPIO) {
                ComplexPPIO complexEncoder = (ComplexPPIO) encoder;
                if (formatsSet.contains(complexEncoder.getMimeType())) {
                    outputType.setMimeType(complexEncoder.getMimeType());
                    return;
                }
            }
        }
        // look for literal ones
        for (ProcessParameterIO encoder : encoders) {
            if (encoder instanceof LiteralPPIO) {
                if (formatsSet.contains("text/plain") || formatsSet.contains("application/json")) {
                    // we can do without format
                    return;
                }
            }
        }
        // ouch, we don't know how to handle this
        throw new APIException(
                ServiceException.INVALID_PARAMETER_VALUE,
                "Cannot handle response format: " + responseFormat,
                HttpStatus.BAD_REQUEST);
    }

    private void writeComplex(HttpServletResponse httpResponse, Object rawResult) {
        try {
            if (rawResult instanceof RawDataEncoderDelegate) {
                RawDataEncoderDelegate delegate = (RawDataEncoderDelegate) rawResult;
                delegate.encode(httpResponse.getOutputStream());
                httpResponse.setContentType(delegate.getRawData().getMimeType());
            } else if (rawResult instanceof XMLEncoderDelegate) {
                XMLEncoderDelegate delegate = (XMLEncoderDelegate) rawResult;
                TransformerHandler xmls =
                        ((SAXTransformerFactory) SAXTransformerFactory.newInstance()).newTransformerHandler();
                xmls.setResult(new StreamResult(httpResponse.getOutputStream()));
                delegate.encode(xmls);
                httpResponse.setContentType(delegate.getPPIO().getMimeType());
            } else if (rawResult instanceof CDataEncoderDelegate) {
                CDataEncoderDelegate cdataDelegate = (CDataEncoderDelegate) rawResult;
                cdataDelegate.encode(httpResponse.getOutputStream());
                httpResponse.setContentType(cdataDelegate.getPPIO().getMimeType());
            } else if (rawResult instanceof BinaryEncoderDelegate) {
                BinaryEncoderDelegate binaryDelegate = (BinaryEncoderDelegate) rawResult;
                binaryDelegate.encode(httpResponse.getOutputStream());
                httpResponse.setContentType(binaryDelegate.getPPIO().getMimeType());
            } else {
                throw new IllegalArgumentException(
                        "Cannot encode an object of class " + rawResult.getClass() + " in raw form");
            }
        } catch (Exception e) {
            throw new APIException(
                    "InternalError",
                    "An error occurred while encoding the results of the process",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }
    }
}
