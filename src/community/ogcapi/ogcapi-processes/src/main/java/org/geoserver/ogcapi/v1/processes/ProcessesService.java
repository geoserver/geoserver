/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.geoserver.ogcapi.MappingJackson2YAMLMessageConverter.APPLICATION_YAML_VALUE;
import static org.geoserver.ogcapi.OpenAPIMessageConverter.OPEN_API_MEDIA_TYPE_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import net.opengis.ows11.BoundingBoxType;
import net.opengis.ows11.ExceptionType;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wps10.ComplexDataType;
import net.opengis.wps10.DataType;
import net.opengis.wps10.ExecuteResponseType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.LiteralDataType;
import net.opengis.wps10.OutputDataType;
import net.opengis.wps10.OutputReferenceType;
import net.opengis.wps10.ResponseFormType;
import org.apache.commons.io.output.WriterOutputStream;
import org.eclipse.emf.common.util.EList;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ogcapi.ConformanceClass;
import org.geoserver.ogcapi.ConformanceDocument;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.v1.processes.JobStatus.StatusCode;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wps.BinaryEncoderDelegate;
import org.geoserver.wps.CDataEncoderDelegate;
import org.geoserver.wps.DefaultWebProcessingService;
import org.geoserver.wps.RawDataEncoderDelegate;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.WPSInfo;
import org.geoserver.wps.XMLEncoderDelegate;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessState;
import org.geoserver.wps.executor.ProcessStatusTracker;
import org.geoserver.wps.executor.WPSExecutionManager;
import org.geoserver.wps.ppio.WFSPPIO;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.api.data.Parameter;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.Converters;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    // purposedly not implemented, we cannot list all jobs and then allow their dismissal to anyone
    public static final String CONF_JOB_LIST = "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/job-list";
    public static final String CONF_CALLBACK = "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/callback";
    public static final String CONF_DISMISS = "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/dismiss";
    // this one comes from the current DRAFT
    // https://docs.ogc.org/DRAFTS/18-062r3.html#_requirements_class_kvp_encoded_execute
    public static final String CONF_KVP_EXECUTE = "http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/kvp-execute";

    private final GeoServer geoServer;
    private final DefaultWebProcessingService wps;
    private final ApplicationContext context;
    private final ProcessStatusTracker statusTracker;
    private final WPSResourceManager resourceManager;
    private final WPSExecutionManager executionManager;

    public ProcessesService(
            GeoServer geoServer,
            DefaultWebProcessingService wps,
            ProcessStatusTracker statusTracker,
            WPSExecutionManager executionManager,
            WPSResourceManager resourceManager,
            ApplicationContext context) {
        this.geoServer = geoServer;
        this.wps = wps;
        this.context = context;
        this.executionManager = executionManager;
        this.statusTracker = statusTracker;
        this.resourceManager = resourceManager;
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
                CONF_KVP_EXECUTE,
                CONF_DISMISS);
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
        Process process = new Process(processId);
        return new ProcessDocument(process, context);
    }

    /** Base execute conformance class */
    @PostMapping(path = "processes/{processId}/execution", name = "executeProcessPOST")
    public void executePost(
            @PathVariable("processId") String processId,
            @RequestBody ExecuteRequest apiRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse)
            throws Exception {
        Process process = new Process(processId);
        // bridge to the WPS service
        ExecuteType request = new ExecuteMapper(process, context).mapToExecute(httpRequest, apiRequest);
        ExecuteResponseType response = wps.execute(request);
        writeResponse(process, httpResponse, request, response, false);
    }

    /** KVP execute conformance class */
    @GetMapping(path = "processes/{processId}/execution", name = "executeProcessKVP")
    public void executeGet(
            @PathVariable("processId") String processId,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse)
            throws Exception {
        Process process = new Process(processId);

        // bridge to the WPS service
        ExecuteType request = new ExecuteMapper(process, context).mapQueryStringToExecute(httpRequest);
        ExecuteResponseType response = wps.execute(request);
        // TODO: handle the response case for no output at all
        writeResponse(process, httpResponse, request, response, false);
    }

    /** Writes the raw response directly to the output */
    private void writeResponse(
            Process process,
            HttpServletResponse httpResponse,
            ExecuteType request,
            ExecuteResponseType response,
            boolean dumpFinalResponse)
            throws Exception {
        // handle exceptions
        boolean failed = Optional.ofNullable(response.getStatus())
                .map(s -> s.getProcessFailed() != null)
                .orElse(false);
        if (failed) {
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

        // follow up with the required response type
        ResponseFormType responseForm = request.getResponseForm();
        if (!dumpFinalResponse
                && responseForm.getResponseDocument() != null
                && responseForm.getResponseDocument().isStatus()) {
            writeStatusResponse(httpResponse, request, response);
        } else if (responseForm.getRawDataOutput() != null
                || responseForm.getResponseDocument().isLineage()) {
            writeRawResponse(httpResponse, response);
        } else {
            writeDocumentResponse(process, httpResponse, response);
        }
    }

    private static void writeStatusResponse(
            HttpServletResponse httpResponse, ExecuteType request, ExecuteResponseType response) {
        // have to extract the executionId from the status location, the WPS EMF Module does not have a place for the id
        // alone
        Map<String, Object> params = KvpUtils.parseQueryString(response.getStatusLocation());
        String executionId = (String) params.get("executionId");
        httpResponse.setStatus(HttpStatus.CREATED.value());
        String jobStatus = ResponseUtils.buildURL(
                request.getBaseUrl(), "ogc/processes/v1/jobs/" + executionId, null, URLMangler.URLType.SERVICE);
        httpResponse.setHeader("Location", jobStatus);
    }

    private void writeRawResponse(HttpServletResponse httpResponse, ExecuteResponseType response) throws IOException {
        OutputDataType result =
                (OutputDataType) response.getProcessOutputs().getOutput().get(0);
        LiteralDataType literal = result.getData().getLiteralData();
        BoundingBoxType bbox = result.getData().getBoundingBoxData();
        if (literal != null) {
            httpResponse.setContentType(TEXT_PLAIN_VALUE);
            httpResponse.getWriter().write(literal.getValue());
        } else if (bbox != null) {
            httpResponse.setContentType(APPLICATION_JSON_VALUE);
            try (JsonGenerator generator = new JsonFactory().createGenerator(httpResponse.getOutputStream())) {
                writeBoundingBox(generator, bbox);
            }
        } else {
            writeComplex(httpResponse, result.getData().getComplexData());
        }
    }

    /**
     * Writes the document response to the output stream, direclty, streaming the JSON and leveraging the PPIOs to avoid
     * materializing everything into memory
     */
    private void writeDocumentResponse(Process process, HttpServletResponse httpResponse, ExecuteResponseType response)
            throws Exception {
        httpResponse.setContentType("application/json");
        try (JsonGenerator generator = new JsonFactory().createGenerator(httpResponse.getOutputStream())) {
            generator.writeStartObject();

            @SuppressWarnings("unchecked")
            List<OutputDataType> outputs = response.getProcessOutputs().getOutput();

            for (OutputDataType output : outputs) {
                String outputId = output.getIdentifier().getValue();
                generator.writeFieldName(outputId);
                Parameter<?> parameter = process.getResultInfo().get(outputId);
                DataType data = output.getData();
                if (data != null) {
                    if (data.getLiteralData() != null) {
                        Object converted =
                                Converters.convert(data.getLiteralData().getValue(), parameter.getType());
                        generator.writeObject(converted);
                    } else if (data.getBoundingBoxData() != null) {
                        BoundingBoxType bbox = data.getBoundingBoxData();
                        writeBoundingBox(generator, bbox);
                    } else if (data.getComplexData() != null) {
                        writeComplex(generator, data.getComplexData());
                    } else {
                        throw new UnsupportedEncodingException("Unsupported output type: " + data);
                    }
                } else if (output.getReference() != null) {
                    OutputReferenceType reference = output.getReference();
                    generator.writeStartObject();
                    if (reference.getMimeType() != null)
                        generator.writeStringField("mediaType", reference.getMimeType());
                    generator.writeStringField("href", reference.getHref());

                } else {
                    throw new IllegalStateException("Cannot handle this output yet: " + outputId);
                }
            }

            generator.writeEndObject();
        }
    }

    private void writeComplex(JsonGenerator generator, ComplexDataType complexData) throws Exception {
        Object result = complexData.getData().get(0);
        if (result == null) {
            generator.writeNull();
            return;
        }

        if (result instanceof RawDataEncoderDelegate) {
            RawDataEncoderDelegate encoder = (RawDataEncoderDelegate) result;
            generator.writeStartObject();
            String mimeType = encoder.getRawData().getMimeType();
            generator.writeStringField("mediaType", mimeType);
            generator.writeFieldName("value");
            generator.writeRaw(": ");
            encoder.encode(generator);
            generator.writeEndObject();
        } else if (result instanceof XMLEncoderDelegate) {
            writeXMLOutput(generator, (XMLEncoderDelegate) result);
        } else if (result instanceof CDataEncoderDelegate) {
            CDataEncoderDelegate cdata = (CDataEncoderDelegate) result;
            String mimeType = cdata.getPPIO().getMimeType();

            if (mimeType != null && APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(mimeType))) {
                generator.writeRaw(": ");
                streamToGenerator(generator, false, cdata);
            } else {
                generator.writeStartObject();
                generator.writeStringField("mediaType", mimeType);
                generator.writeFieldName("value");
                generator.writeRaw(": \"");
                streamToGenerator(generator, true, cdata);
                generator.writeRaw("\"");
                generator.writeEndObject();
            }
        } else if (result instanceof BinaryEncoderDelegate) {
            BinaryEncoderDelegate binary = (BinaryEncoderDelegate) result;
            generator.writeStartObject();
            generator.writeStringField("mediaType", binary.getPPIO().getMimeType());
            generator.writeFieldName("value");
            generator.writeRaw(": ");
            binary.encode(generator);
            generator.writeEndObject();
        } else if (result instanceof String) {
            generator.writeString((String) result);
        } else if (result instanceof Number) {
            generator.writeNumber(((Number) result).doubleValue());
        } else if (result instanceof Boolean) {
            generator.writeBoolean((Boolean) result);
        } else if (result
                instanceof FeatureCollectionType) { // these input happen while deserializing an async response
            FeatureCollectionType fct = (FeatureCollectionType) result;
            FeatureCollection<?, ?> fc =
                    (FeatureCollection<?, ?>) fct.getFeature().get(0);
            writeXMLOutput(generator, new XMLEncoderDelegate(new WFSPPIO.WFS10(), fc));
        } else if (result instanceof net.opengis.wfs20.FeatureCollectionType) {
            net.opengis.wfs20.FeatureCollectionType fct = (net.opengis.wfs20.FeatureCollectionType) result;
            FeatureCollection<?, ?> fc =
                    (FeatureCollection<?, ?>) fct.getMember().get(0);
            writeXMLOutput(generator, new XMLEncoderDelegate(new WFSPPIO.WFS20(), fc));
        } else {
            throw new WPSException("Don't know how to encode " + result);
        }
    }

    private static void writeXMLOutput(JsonGenerator generator, XMLEncoderDelegate xml) throws Exception {
        generator.writeStartObject();
        generator.writeStringField("mediaType", xml.getPPIO().getMimeType());
        generator.writeFieldName("value");
        generator.writeRaw(": \"");
        streamToGenerator(generator, xml);
        generator.writeRaw("\"");
        generator.writeEndObject();
    }

    private static void streamToGenerator(JsonGenerator generator, boolean escape, CDataEncoderDelegate cdata)
            throws Exception {
        try (OutputStream os = WriterOutputStream.builder()
                .setWriter(new GeneratorWriter(generator, escape))
                .setCharset(UTF_8)
                .get()) {
            cdata.encode(os);
        }
    }

    private static void streamToGenerator(JsonGenerator generator, XMLEncoderDelegate encoder) throws Exception {
        try (OutputStream os = WriterOutputStream.builder()
                .setWriter(new GeneratorWriter(generator, true))
                .setCharset(UTF_8)
                .get()) {
            encoder.getPPIO().encode(encoder.getObject(), os);
        }
    }

    private static void writeBoundingBox(JsonGenerator generator, BoundingBoxType bbox) throws IOException {
        generator.writeStartObject();
        generator.writeFieldName("bbox");
        generator.writeStartArray();
        for (int i = 0; i < bbox.getLowerCorner().size(); i++) {
            generator.writeObject(bbox.getLowerCorner().get(i));
        }
        for (int i = 0; i < bbox.getUpperCorner().size(); i++) {
            generator.writeObject(bbox.getUpperCorner().get(i));
        }
        generator.writeEndArray();
        if (bbox.getCrs() != null) {
            generator.writeStringField("crs", bbox.getCrs());
        }
        generator.writeEndObject();
    }

    private void writeComplex(HttpServletResponse httpResponse, ComplexDataType complexData) {
        Object rawResult = complexData.getData().get(0);
        try {
            if (rawResult instanceof RawDataEncoderDelegate) {
                RawDataEncoderDelegate delegate = (RawDataEncoderDelegate) rawResult;
                httpResponse.setContentType(delegate.getRawData().getMimeType());
                delegate.encode(httpResponse.getOutputStream());
            } else if (rawResult instanceof XMLEncoderDelegate) {
                XMLEncoderDelegate delegate = (XMLEncoderDelegate) rawResult;
                TransformerHandler xmls =
                        ((SAXTransformerFactory) SAXTransformerFactory.newInstance()).newTransformerHandler();
                xmls.setResult(new StreamResult(httpResponse.getOutputStream()));
                httpResponse.setContentType(delegate.getPPIO().getMimeType());
                delegate.encode(xmls);
            } else if (rawResult instanceof CDataEncoderDelegate) {
                CDataEncoderDelegate cdataDelegate = (CDataEncoderDelegate) rawResult;
                httpResponse.setContentType(cdataDelegate.getPPIO().getMimeType());
                cdataDelegate.encode(httpResponse.getOutputStream());
            } else if (rawResult instanceof BinaryEncoderDelegate) {
                BinaryEncoderDelegate binaryDelegate = (BinaryEncoderDelegate) rawResult;
                httpResponse.setContentType(binaryDelegate.getPPIO().getMimeType());
                binaryDelegate.encode(httpResponse.getOutputStream());
            } else if (rawResult instanceof String) {
                // this is an already encoded string (async execution stored), just write it out
                String result = (String) rawResult;
                httpResponse.setContentType(complexData.getMimeType());
                httpResponse.getWriter().print(result);
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

    /** KVP execute conformance class */
    @GetMapping(path = "jobs/{jobId}", name = "getStatus")
    @ResponseBody
    public JobStatus getStatus(@PathVariable("jobId") String jobId) {
        // TODO: add a HTML representation once we provide also a way to start a process via HTML and retrieve there
        // its job location (since we're not listing everyone's jobs on /jobs)

        ExecutionStatus status = statusTracker.getStatus(jobId);
        if (status == null) {
            throw new APIException(
                    "NotFound", "The job with id " + jobId + " does not exist, has expired", HttpStatus.NOT_FOUND);
        }

        JobStatus response = mapExecutionStatus(status, mapProcessPhaseToStatus(status.getPhase()));

        if (status.getPhase() == ProcessState.SUCCEEDED) {
            Link results = new Link();
            results.setRel(JobStatus.RESULTS_REL);
            results.setType(APPLICATION_JSON_VALUE);
            String href = ResponseUtils.buildURL(
                    APIRequestInfo.get().getBaseURL(),
                    "ogc/processes/v1/jobs/" + jobId + "/results",
                    null,
                    URLMangler.URLType.SERVICE);
            results.setHref(href);
            response.addLink(results);
        }

        return response;
    }

    /** KVP execute conformance class */
    @DeleteMapping(path = "jobs/{jobId}", name = "dismiss")
    @ResponseBody
    public JobStatus dismiss(@PathVariable("jobId") String jobId) {
        ExecutionStatus status = statusTracker.getStatus(jobId);
        if (status == null || status.getPhase() == ProcessState.DISMISSING) {
            throw new APIException(
                    "NotFound",
                    "The job with id " + jobId + " does not exist, has expired, or is being dismissed",
                    HttpStatus.NOT_FOUND);
        }

        // actually cancel the execution
        executionManager.cancel(jobId);

        return mapExecutionStatus(status, StatusCode.DISMISSED);
    }

    private static JobStatus mapExecutionStatus(ExecutionStatus status, StatusCode dismissed) {
        JobStatus response = new JobStatus();
        response.setProcessID(status.getSimpleProcessName());
        response.setCreated(status.getCreationTime());
        response.setFinished(status.getCompletionTime());
        response.setStatus(dismissed);
        response.setProgress(Math.round(status.getProgress()));
        return response;
    }

    private StatusCode mapProcessPhaseToStatus(ProcessState phase) {
        switch (phase) {
            case QUEUED:
                return StatusCode.ACCEPTED;
            case RUNNING:
                return StatusCode.RUNNING;
            case SUCCEEDED:
                return StatusCode.SUCCESSFUL;
            case DISMISSING:
                return StatusCode.DISMISSED;
            case FAILED:
                return StatusCode.FAILED;
            default:
                throw new IllegalStateException("Unexpected value: " + phase);
        }
    }

    @GetMapping(path = "jobs/{jobId}/results", name = "getResults")
    @ResponseBody
    public void getResults(@PathVariable("jobId") String jobId, HttpServletResponse httpResponse) throws Exception {
        ExecutionStatus status = statusTracker.getStatus(jobId);
        if (status == null) {
            throw new APIException(
                    "NotFound",
                    "The job with id " + jobId + " does not exist, has expired or is being dismissed",
                    HttpStatus.NOT_FOUND);
        }

        ExecuteType executeRequest = resourceManager.getStoredRequestObject(status.getExecutionId());
        ExecuteResponseType executeResponse = resourceManager.getStoredResponseObject(status.getExecutionId());
        Process process = new Process(executeRequest.getIdentifier().getValue());
        writeResponse(process, httpResponse, executeRequest, executeResponse, true);
    }
}
