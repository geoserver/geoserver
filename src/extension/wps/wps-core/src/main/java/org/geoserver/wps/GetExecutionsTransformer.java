/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.datatype.XMLGregorianCalendar;
import net.opengis.ows11.ExceptionType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.QueryType;
import net.opengis.wps10.DataType;
import net.opengis.wps10.DocumentOutputDefinitionType;
import net.opengis.wps10.ExecuteResponseType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.InputReferenceType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.OutputDefinitionType;
import net.opengis.wps10.ResponseDocumentType;
import org.eclipse.emf.common.util.EList;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.wps.executor.ExecuteResponseBuilder;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessState;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.FilterPPIO;
import org.geoserver.wps.ppio.LiteralPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.data.Parameter;
import org.geotools.process.ProcessFactory;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.springframework.context.ApplicationContext;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Internal Base {@link TransformerBase} for GetExecutions
 *
 * @author Alessio Fabiani, GeoSolutions SAS
 */
public class GetExecutionsTransformer extends TransformerBase {

    static final Logger LOGGER = Logging.getLogger(GetExecutionsTransformer.class);

    private List<ExecutionStatus> executions = new ArrayList<ExecutionStatus>();

    WPSInfo wps;

    /** The resource tracker, we use it to build the responses */
    private WPSResourceManager resources;

    /** Used by the response builder */
    private ApplicationContext ctx;

    /** The original request POJO storing the query KVPs */
    private GetExecutionsType request;

    /** Pagination variables */
    private Integer total;

    private Integer startIndex;
    private Integer maxFeatures;

    public GetExecutionsTransformer(
            WPSInfo wps,
            WPSResourceManager resources,
            ApplicationContext ctx,
            GetExecutionsType request,
            Integer total,
            Integer startIndex,
            Integer maxFeatures) {
        this.wps = wps;
        this.resources = resources;
        this.ctx = ctx;
        this.request = request;
        this.total = total;
        this.startIndex = startIndex;
        this.maxFeatures = maxFeatures;
    }

    public void append(ExecutionStatus status) {
        executions.add(status);
    }

    class GMLTranslator extends TranslatorSupport {

        public GMLTranslator(ContentHandler contentHandler) {
            super(contentHandler, null, null);
        }

        @Override
        public void encode(Object o) throws IllegalArgumentException {
            // register namespaces provided by extended capabilities
            NamespaceSupport namespaces = getNamespaceSupport();
            namespaces.declarePrefix("wps", "http://www.opengis.net/wps/1.0.0");
            namespaces.declarePrefix("wfs", "http://www.opengis.net/wfs");
            namespaces.declarePrefix("wcs", "http://www.opengis.net/wcs/1.1.1");
            namespaces.declarePrefix("ogc", "http://www.opengis.net/ogc");
            namespaces.declarePrefix("ows", "http://www.opengis.net/ows/1.1");
            namespaces.declarePrefix("gml", "http://www.opengis.net/gml/3.2");
            namespaces.declarePrefix("xs", "http://www.w3.org/2001/XMLSchema");
            namespaces.declarePrefix("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            namespaces.declarePrefix("xlink", "http://www.w3.org/1999/xlink");

            final AttributesImpl attributes = new AttributesImpl();
            registerNamespaces(getNamespaceSupport(), attributes);

            final String proxyBaseUrl =
                    wps.getGeoServer().getGlobal().getSettings().getProxyBaseUrl();
            final String baseUrl = proxyBaseUrl != null ? proxyBaseUrl : "/";
            String serviceInstance =
                    ResponseUtils.appendQueryString(
                            ResponseUtils.buildURL(baseUrl, "ows", null, URLType.SERVICE), "");

            attributes.addAttribute("", "xml:lang", "xml:lang", "", "en");
            attributes.addAttribute(
                    "",
                    "xsi:schemaLocation",
                    "xsi:schemaLocation",
                    "",
                    "http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd");
            attributes.addAttribute("", "service", "service", "", wps.getName());
            attributes.addAttribute("", "version", "version", "", "1.0.0");
            attributes.addAttribute("", "serviceInstance", "serviceInstance", "", serviceInstance);

            final AttributesImpl getExecutionsAttributes = new AttributesImpl(attributes);
            getExecutionsAttributes.addAttribute("", "count", "count", "", String.valueOf(total));
            getPaginationAttributes(serviceInstance, getExecutionsAttributes);
            start("wps:GetExecutionsResponse", getExecutionsAttributes);
            for (ExecutionStatus status : executions) {
                ExecuteType execute = status.getRequest();
                try {
                    if (execute == null) {
                        execute = resources.getStoredRequestObject(status.getExecutionId());
                    }
                    if (execute == null) {
                        throw new WPSException(
                                "Could not locate the original request for execution id: "
                                        + status.getExecutionId());
                    } else {
                        ExecuteResponseBuilder builder =
                                new ExecuteResponseBuilder(execute, ctx, status);
                        ExecuteResponseType responseType = builder.build();

                        start("wps:ExecuteResponse");
                        final AttributesImpl processAttributes = new AttributesImpl();
                        processAttributes.addAttribute(
                                "",
                                "wps:processVersion",
                                "wps:processVersion",
                                "",
                                responseType.getProcess().getProcessVersion());
                        start("wps:Process", processAttributes);
                        element(
                                "ows:Identifier",
                                responseType.getProcess().getIdentifier().getValue());
                        element("ows:Title", responseType.getProcess().getTitle().getValue());
                        element("ows:Abstract", responseType.getProcess().getAbstract().getValue());
                        end("wps:Process");

                        final AttributesImpl statusAttributes = new AttributesImpl();
                        if (status.getCreationTime() != null) {
                            statusAttributes.addAttribute(
                                    "",
                                    "creationTime",
                                    "creationTime",
                                    "",
                                    responseType.getStatus().getCreationTime().toString());
                        }
                        if (status.getCompletionTime() != null) {
                            statusAttributes.addAttribute(
                                    "",
                                    "completionTime",
                                    "completionTime",
                                    "",
                                    Converters.convert(
                                                    status.getCompletionTime(),
                                                    XMLGregorianCalendar.class)
                                            .toString());
                        }
                        if (status.getLastUpdated() != null) {
                            statusAttributes.addAttribute(
                                    "",
                                    "lastUpdated",
                                    "lastUpdated",
                                    "",
                                    Converters.convert(
                                                    status.getLastUpdated(),
                                                    XMLGregorianCalendar.class)
                                            .toString());
                        }
                        start("wps:Status", statusAttributes);
                        element("wps:JobID", status.getExecutionId());
                        element(
                                "wps:Identifier",
                                responseType.getProcess().getIdentifier().getValue());
                        element("wps:Owner", status.getUserName());
                        element("wps:Status", status.getPhase().name());
                        if (status.getEstimatedCompletion() != null) {
                            element(
                                    "wps:EstimatedCompletion",
                                    Converters.convert(
                                                    status.getEstimatedCompletion(),
                                                    XMLGregorianCalendar.class)
                                            .toString());
                        }
                        element(
                                "wps:ExpirationDate",
                                Converters.convert(
                                                status.getExpirationDate(),
                                                XMLGregorianCalendar.class)
                                        .toString());
                        element(
                                "wps:NextPoll",
                                Converters.convert(status.getNextPoll(), XMLGregorianCalendar.class)
                                        .toString());
                        element("wps:PercentCompleted", String.valueOf(status.getProgress()));
                        if (status.getException() != null) {
                            StringBuffer stackTrace = new StringBuffer();
                            EList exceptions =
                                    responseType
                                            .getStatus()
                                            .getProcessFailed()
                                            .getExceptionReport()
                                            .getException();
                            for (Object ex : exceptions) {
                                if (ex instanceof ExceptionType) {
                                    stackTrace.append(((ExceptionType) ex).getExceptionCode());
                                    stackTrace.append(": ");
                                    stackTrace.append(((ExceptionType) ex).getExceptionText());
                                    stackTrace.append("\n");
                                }
                            }
                            element("wps:ProcessFailed", stackTrace.toString());
                        } else if (status.getPhase() == ProcessState.QUEUED) {
                            element(
                                    "wps:ProcessAccepted",
                                    responseType.getStatus().getProcessAccepted());
                        } else if (status.getPhase() == ProcessState.RUNNING) {
                            element(
                                    "wps:ProcessStarted",
                                    responseType.getStatus().getProcessStarted().getValue());
                        } else {
                            element(
                                    "wps:ProcessSucceeded",
                                    responseType.getStatus().getProcessSucceeded());
                        }

                        // status location, if asynch
                        if (responseType.getStatusLocation() != null) {
                            element("wps:StatusLocation", responseType.getStatusLocation());
                        }

                        // lineage, should be included only if requested, the response should
                        // contain it even if the process is not done computing. From the spec:
                        // * If lineage is "true" the server shall include in the execute response a
                        // complete copy of the DataInputs and OutputDefinition elements _as
                        // received in the execute request_.
                        // *If lineage is "false" then/ these elements shall be omitted from the
                        // response
                        if (responseType.getDataInputs() != null
                                && responseType.getDataInputs().getInput().size() > 0) {
                            EList inputs = responseType.getDataInputs().getInput();
                            encodeDataInputs(status, inputs, attributes);
                        }

                        if (responseType.getOutputDefinitions() != null
                                && responseType.getOutputDefinitions().getOutput().size() > 0) {
                            EList outputs = responseType.getOutputDefinitions().getOutput();
                            encodeDataOutputs(status, outputs, attributes);
                        }
                        end("wps:Status");
                        end("wps:ExecuteResponse");
                    }
                } catch (IOException e) {
                    throw new WPSException(Executions.INTERNAL_SERVER_ERROR_CODE, e);
                }
            }
            end("wps:GetExecutionsResponse");
        }

        /** Encodes {@link ExecuteType} to XML */
        protected void encodeExecuteRequest(
                ExecutionStatus status, ExecuteType exec, AttributesImpl attributes) {
            final AttributesImpl executeTypeAttributes = new AttributesImpl(attributes);
            start("wps:Execute", executeTypeAttributes);
            element("ows:Identifier", exec.getIdentifier().getValue());
            encodeDataInputs(status, exec.getDataInputs().getInput(), executeTypeAttributes);
            start("wps:ResponseForm");
            if (exec.getResponseForm() != null) {
                if (exec.getResponseForm().getRawDataOutput() != null) {
                    OutputDefinitionType rawDataOutput = exec.getResponseForm().getRawDataOutput();
                    final AttributesImpl rawDataOutputAttributes = new AttributesImpl();
                    if (rawDataOutput.getMimeType() != null) {
                        rawDataOutputAttributes.addAttribute(
                                "", "mimeType", "mimeType", "", rawDataOutput.getMimeType());
                    }
                    if (rawDataOutput.getEncoding() != null) {
                        rawDataOutputAttributes.addAttribute(
                                "", "encoding", "encoding", "", rawDataOutput.getEncoding());
                    }
                    if (rawDataOutput.getSchema() != null) {
                        rawDataOutputAttributes.addAttribute(
                                "", "schema", "schema", "", rawDataOutput.getSchema());
                    }
                    if (rawDataOutput.getUom() != null) {
                        rawDataOutputAttributes.addAttribute(
                                "", "uom", "uom", "", rawDataOutput.getUom());
                    }
                    start("wps:RawDataOutput", rawDataOutputAttributes);
                    element("ows:Identifier", rawDataOutput.getIdentifier().getValue());
                    end("wps:RawDataOutput");
                }

                if (exec.getResponseForm().getResponseDocument() != null) {
                    ResponseDocumentType responseDocument =
                            exec.getResponseForm().getResponseDocument();
                    final AttributesImpl responseDocumentAttributes = new AttributesImpl();
                    responseDocumentAttributes.addAttribute(
                            "",
                            "status",
                            "status",
                            "",
                            String.valueOf(responseDocument.isSetStatus()));
                    responseDocumentAttributes.addAttribute(
                            "",
                            "storeExecuteResponse",
                            "storeExecuteResponse",
                            "",
                            String.valueOf(responseDocument.isSetStoreExecuteResponse()));
                    responseDocumentAttributes.addAttribute(
                            "",
                            "lineage",
                            "lineage",
                            "",
                            String.valueOf(responseDocument.isSetLineage()));
                    start("wps:ResponseDocument", responseDocumentAttributes);
                    end("wps:ResponseDocument");
                }
            }
            end("wps:ResponseForm");
            end("wps:Execute");
        }

        /** Encode Data Inputs from "lineage" as XML */
        protected void encodeDataInputs(
                ExecutionStatus status, EList inputs, AttributesImpl attributes) {
            start("wps:DataInputs");
            for (Object input : inputs) {
                // Encode Inputs on the Status Response
                if (input instanceof InputType) {
                    InputType ii = (InputType) input;
                    start("wps:Input");
                    if (ii.getIdentifier() != null) {
                        element("ows:Identifier", ii.getIdentifier().getValue());
                    }
                    if (ii.getTitle() != null) {
                        element("ows:Title", ii.getTitle().getValue());
                    }
                    if (ii.getAbstract() != null) {
                        element("ows:Abstract", ii.getAbstract().getValue());
                    }

                    InputReferenceType reference = ii.getReference();
                    if (reference != null) {
                        final AttributesImpl inputAttributes = new AttributesImpl();
                        if (reference.getHref() != null) {
                            inputAttributes.addAttribute(
                                    "", "xlink:href", "xlink:href", "", reference.getHref());
                        }
                        if (reference.getMimeType() != null) {
                            inputAttributes.addAttribute(
                                    "", "mimeType", "mimeType", "", reference.getMimeType());
                        }
                        if (reference.getSchema() != null) {
                            inputAttributes.addAttribute(
                                    "", "schema", "schema", "", reference.getSchema());
                        }
                        if (reference.getEncoding() != null) {
                            inputAttributes.addAttribute(
                                    "", "encoding", "encoding", "", reference.getSchema());
                        }
                        if (reference.getMethod() != null) {
                            inputAttributes.addAttribute(
                                    "", "method", "method", "", reference.getMethod().getName());
                        }
                        start("wps:Reference", inputAttributes);
                        if (reference.getBody() != null) {
                            start("wps:Body");
                            // get the input descriptors
                            Name processName = status.getProcessName();
                            ProcessFactory pf =
                                    GeoServerProcessors.createProcessFactory(processName, true);
                            final Map<String, Parameter<?>> parameters =
                                    pf.getParameterInfo(processName);
                            String inputId = ii.getIdentifier().getValue();
                            List<ProcessParameterIO> ppios =
                                    ProcessParameterIO.findDecoder(parameters.get(inputId), ctx);
                            if (ppios != null && !ppios.isEmpty()) {
                                for (ProcessParameterIO ppio : ppios) {
                                    if (ppio.isComplex(parameters.get(inputId), ctx)) {
                                        try {
                                            Object out =
                                                    ((ComplexPPIO) ppio)
                                                            .decode(reference.getBody());
                                            if (out instanceof ExecuteType) {
                                                ExecuteType exec = (ExecuteType) out;
                                                start("wps:ComplexData", inputAttributes);
                                                encodeExecuteRequest(status, exec, attributes);
                                                end("wps:ComplexData");
                                            } else if (out instanceof GetFeatureType) {
                                                GetFeatureType features = (GetFeatureType) out;
                                                final AttributesImpl getFeatureAttributes =
                                                        new AttributesImpl(inputAttributes);
                                                if (features.getService() != null) {
                                                    getFeatureAttributes.addAttribute(
                                                            "",
                                                            "service",
                                                            "service",
                                                            "",
                                                            features.getService());
                                                }
                                                if (features.getVersion() != null) {
                                                    getFeatureAttributes.addAttribute(
                                                            "",
                                                            "version",
                                                            "version",
                                                            "",
                                                            features.getVersion());
                                                }
                                                if (features.getBaseUrl() != null) {
                                                    getFeatureAttributes.addAttribute(
                                                            "",
                                                            "baseUrl",
                                                            "baseUrl",
                                                            "",
                                                            features.getBaseUrl());
                                                }
                                                if (features.getOutputFormat() != null) {
                                                    getFeatureAttributes.addAttribute(
                                                            "",
                                                            "outputFormat",
                                                            "outputFormat",
                                                            "",
                                                            features.getOutputFormat());
                                                }
                                                if (features.getMaxFeatures() != null) {
                                                    getFeatureAttributes.addAttribute(
                                                            "",
                                                            "maxFeatures",
                                                            "maxFeatures",
                                                            "",
                                                            String.valueOf(
                                                                    features.getMaxFeatures()));
                                                }
                                                start("wfs:GetFeature", getFeatureAttributes);
                                                EList queries = features.getQuery();
                                                if (queries != null && !queries.isEmpty()) {
                                                    for (Object query : queries) {
                                                        QueryType qt = (QueryType) query;
                                                        final AttributesImpl queryTypeAttributes =
                                                                new AttributesImpl(inputAttributes);
                                                        if (qt.getTypeName() != null
                                                                && !qt.getTypeName().isEmpty()) {
                                                            queryTypeAttributes.addAttribute(
                                                                    "",
                                                                    "typeName",
                                                                    "typeName",
                                                                    "",
                                                                    qt.getTypeName()
                                                                            .get(0)
                                                                            .toString());
                                                        }
                                                        start("wfs:Query", queryTypeAttributes);
                                                        Filter filter = qt.getFilter();
                                                        if (filter != null) {
                                                            FilterPPIO fppio =
                                                                    new FilterPPIO.Filter11();
                                                            fppio.encode(filter, contentHandler);
                                                        }
                                                        end("wfs:Query");
                                                    }
                                                }
                                                end("wfs:GetFeature");
                                            } else {
                                                element(
                                                        "wps:ComplexData",
                                                        "<![CDATA[" + out.toString() + "]]",
                                                        inputAttributes);
                                            }
                                        } catch (Exception e) {
                                            LOGGER.log(Level.WARNING, "", e);
                                        }
                                    } else {
                                        if (ppio instanceof LiteralPPIO) {
                                            try {
                                                element(
                                                        "wps:LiteralData",
                                                        ((LiteralPPIO) ppio)
                                                                .encode(reference.getBody()));
                                            } catch (Exception e) {
                                                LOGGER.log(Level.WARNING, "", e);
                                            }
                                        }
                                    }
                                }
                            }
                            end("wps:Body");
                        }
                        if (reference.getBodyReference() != null) {
                            element(
                                    "wps:BodyReference",
                                    ii.getReference().getBodyReference().getHref());
                        }
                        end("wps:Reference");
                    }

                    DataType data = ii.getData();
                    if (data != null) {
                        AttributesImpl inputAttributes = new AttributesImpl();
                        if (data.getComplexData() != null) {
                            start("wps:Data");
                            for (Object complexData : data.getComplexData().getData()) {
                                if (data.getComplexData().getMimeType() != null) {
                                    inputAttributes.addAttribute(
                                            "",
                                            "mimeType",
                                            "mimeType",
                                            "",
                                            data.getComplexData().getMimeType());
                                }
                                // get the input descriptors
                                Name processName = status.getProcessName();
                                ProcessFactory pf =
                                        GeoServerProcessors.createProcessFactory(processName, true);
                                final Map<String, Parameter<?>> parameters =
                                        pf.getParameterInfo(processName);
                                String inputId = ii.getIdentifier().getValue();
                                List<ProcessParameterIO> ppio =
                                        ProcessParameterIO.findEncoder(
                                                parameters.get(inputId), ctx);
                                final ProcessParameterIO processParameterIO = ppio.get(0);
                                if (ppio != null && !ppio.isEmpty()) {
                                    if (processParameterIO.isComplex(
                                            parameters.get(inputId), ctx)) {
                                        element(
                                                "wps:ComplexData",
                                                "<![CDATA[" + complexData.toString() + "]]",
                                                inputAttributes);
                                    }
                                } else {
                                    if (processParameterIO instanceof LiteralPPIO) {
                                        try {
                                            element(
                                                    "wps:LiteralData",
                                                    ((LiteralPPIO) processParameterIO)
                                                            .encode(complexData),
                                                    inputAttributes);
                                        } catch (Exception e) {
                                            LOGGER.log(Level.WARNING, "", e);
                                        }
                                    }
                                }
                            }
                            end("wps:Data");
                        }

                        if (data.getLiteralData() != null) {
                            if (data.getLiteralData().getDataType() != null) {
                                inputAttributes.addAttribute(
                                        "",
                                        "dataType",
                                        "dataType",
                                        "",
                                        data.getLiteralData().getDataType());
                            }
                            if (data.getLiteralData().getUom() != null) {
                                inputAttributes.addAttribute(
                                        "", "uom", "uom", "", data.getLiteralData().getUom());
                            }
                            start("wps:Data", inputAttributes);
                            element("wps:LiteralData", data.getLiteralData().getValue());
                            end("wps:Data");
                        }

                        if (data.getBoundingBoxData() != null) {
                            if (data.getBoundingBoxData().getDimensions() != null) {
                                inputAttributes.addAttribute(
                                        "",
                                        "dimensions",
                                        "dimensions",
                                        "",
                                        data.getBoundingBoxData().getDimensions().toString());
                            }
                            if (data.getBoundingBoxData().getCrs() != null) {
                                inputAttributes.addAttribute(
                                        "", "crs", "crs", "", data.getBoundingBoxData().getCrs());
                            }
                            start("wps:BoundingBoxData", inputAttributes);
                            if (data.getBoundingBoxData().getLowerCorner() != null) {
                                StringBuffer lowerCorner = new StringBuffer();
                                for (Object coord : data.getBoundingBoxData().getLowerCorner()) {
                                    lowerCorner.append(coord).append(" ");
                                }
                                element("ows:LowerCorner", lowerCorner.toString().trim());
                            }
                            if (data.getBoundingBoxData().getUpperCorner() != null) {
                                StringBuffer upperCorner = new StringBuffer();
                                for (Object coord : data.getBoundingBoxData().getUpperCorner()) {
                                    upperCorner.append(coord).append(" ");
                                }
                                element("ows:UpperCorner", upperCorner.toString().trim());
                            }
                            end("wps:BoundingBoxData");
                        }
                    }
                    end("wps:Input");
                }
            }
            end("wps:DataInputs");
        }

        /** Encode Data Outputs from "lineage" as XML */
        protected void encodeDataOutputs(
                ExecutionStatus status, EList outputs, AttributesImpl attributes) {
            start("wps:DataOutputs");
            for (Object output : outputs) {
                // Encode Outputs on the Status Response
                if (output instanceof DocumentOutputDefinitionType) {
                    DocumentOutputDefinitionType oo = (DocumentOutputDefinitionType) output;
                    final AttributesImpl outputDefinitionTypeAttributes = new AttributesImpl();
                    outputDefinitionTypeAttributes.addAttribute(
                            "",
                            "asReference",
                            "asReference",
                            "",
                            String.valueOf(oo.isSetAsReference()));
                    if (oo.getMimeType() != null) {
                        outputDefinitionTypeAttributes.addAttribute(
                                "", "mimeType", "mimeType", "", oo.getMimeType());
                    }
                    if (oo.getEncoding() != null) {
                        outputDefinitionTypeAttributes.addAttribute(
                                "", "encoding", "encoding", "", oo.getEncoding());
                    }
                    if (oo.getSchema() != null) {
                        outputDefinitionTypeAttributes.addAttribute(
                                "", "schema", "schema", "", oo.getSchema());
                    }
                    if (oo.getUom() != null) {
                        outputDefinitionTypeAttributes.addAttribute(
                                "", "uom", "uom", "", oo.getUom());
                    }
                    start("wps:Output", outputDefinitionTypeAttributes);
                    if (oo.getIdentifier() != null) {
                        element("ows:Identifier", oo.getIdentifier().getValue());
                    }
                    if (oo.getTitle() != null) {
                        element("ows:Title", oo.getTitle().getValue());
                    }
                    if (oo.getAbstract() != null) {
                        element("ows:Abstract", oo.getAbstract().getValue());
                    }

                    end("wps:Output");
                }
            }
            end("wps:DataOutputs");
        }

        /**
         * Set Pagination accordingly to the GSIP-169: - if number less or equal than
         * MAX_FEATURES_PER_PAGE, then go ahead - if number greater than MAX_FEATURES_PER_PAGE --
         * add "count" attribute to the GetExecutionsResponse, representing the total number of
         * elements -- add "next" attribute to the GetExecutionsResponse, representing the URL of
         * the next page; it this is not present then there are no more pages available -- add
         * "previous" attribute to the GetExecutionsResponse, representing the URL of the previous
         * page; it this is not present then we are at the first page
         */
        protected void getPaginationAttributes(String serviceInstance, AttributesImpl attributes) {
            String baseRequestUrl =
                    serviceInstance
                            + "service="
                            + request.service
                            + "&version="
                            + request.version
                            + "&request=GetExecutions&";
            if (request.identifier != null) {
                baseRequestUrl += "identifier=" + request.identifier;
            }
            if (request.owner != null) {
                baseRequestUrl += "owner=" + request.owner;
            }
            if (request.status != null) {
                baseRequestUrl += "status=" + request.status;
            }
            if (request.orderBy != null) {
                baseRequestUrl += "orderBy=" + request.orderBy;
            }
            if (request.maxFeatures != null) {
                baseRequestUrl += "maxFeatures=" + request.maxFeatures;
            }

            if (maxFeatures != null && maxFeatures > 0) {
                int index = startIndex != null ? startIndex : 0;
                if (index > 0) {
                    attributes.addAttribute(
                            "",
                            "previous",
                            "previous",
                            "",
                            baseRequestUrl
                                    + "&startIndex="
                                    + (index - Math.min(maxFeatures, index)));
                }

                if ((total - maxFeatures) > index) {
                    attributes.addAttribute(
                            "",
                            "next",
                            "next",
                            "",
                            baseRequestUrl + "&startIndex=" + (index + maxFeatures));
                }
            }
        }

        /**
         * Register all namespaces as xmlns:xxx attributes for the top level element of a xml
         * document
         */
        protected void registerNamespaces(NamespaceSupport ns, AttributesImpl attributes) {
            Enumeration declaredPrefixes = ns.getDeclaredPrefixes();
            while (declaredPrefixes.hasMoreElements()) {
                String prefix = (String) declaredPrefixes.nextElement();
                String uri = ns.getURI(prefix);

                // ignore xml prefix
                if ("xml".equals(prefix)) {
                    continue;
                }

                String prefixDef = "xmlns:" + prefix;

                attributes.addAttribute("", prefixDef, prefixDef, "", uri);
            }
        }
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new GMLTranslator(handler);
    }

    /** @return the executions */
    public List<ExecutionStatus> getExecutions() {
        return executions;
    }
}
