/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import static org.geoserver.ogcapi.v1.processes.ExecuteOutput.TransmissionMode.REFERENCE;
import static org.geoserver.ogcapi.v1.processes.ExecuteRequest.ResponseMode.DOCUMENT;
import static org.geoserver.ogcapi.v1.processes.InputValue.ComplexJSONInputValue;
import static org.geoserver.ogcapi.v1.processes.InputValueDeserializer.CRS84;
import static org.geoserver.ogcapi.v1.processes.InputValueDeserializer.CRS84H;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.opengis.ows11.BoundingBoxType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.wps10.ComplexDataType;
import net.opengis.wps10.DataInputsType1;
import net.opengis.wps10.DataType;
import net.opengis.wps10.DocumentOutputDefinitionType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.InputReferenceType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.LiteralDataType;
import net.opengis.wps10.OutputDefinitionType;
import net.opengis.wps10.ResponseDocumentType;
import net.opengis.wps10.ResponseFormType;
import net.opengis.wps10.Wps10Factory;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.v1.processes.ExecuteOutput.ExecuteOutputFormat;
import org.geoserver.ogcapi.v1.processes.InputValue.ArrayInputValue;
import org.geoserver.ogcapi.v1.processes.InputValue.BoundingBoxInputValue;
import org.geoserver.ogcapi.v1.processes.InputValue.InlineFileInputValue;
import org.geoserver.ogcapi.v1.processes.InputValue.LiteralInputValue;
import org.geoserver.ogcapi.v1.processes.InputValue.ReferenceInputValue;
import org.geoserver.ows.Ows11Util;
import org.geoserver.platform.ServiceException;
import org.geoserver.wps.ppio.BoundingBoxPPIO;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.LiteralPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geotools.api.data.Parameter;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

/** Maps an OGC API Processes Execute request to a GeoServer WPS {@link ExecuteType} */
// TODO: handle content negotiation from the HTTP header too
public class ExecuteMapper {

    // The response format parameter for KVP encoded execute
    public static final String RESPONSE_FORMAT_KVP = "response[f]";
    public static final String LINK_HREF = "[href]";
    public static final String LINK_TYPE = "[type]";
    public static final String INLINE_TYPE = "[mediaType]";
    public static final String BBOX_CRS = "[crs]";
    public static final String INCLUDE = "[include]";
    private static final Wps10Factory WPS_FACTORY = Wps10Factory.eINSTANCE;

    // used to parse JSON strings in KVP requests (used for array/complex inputs)
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final String HTTP_HEADER_PREFER = "Prefer";

    private final Process process;
    private final ApplicationContext context;

    ExecuteMapper(Process process, ApplicationContext context) {
        this.process = process;
        this.context = context;
    }

    /** Maps a {@link ExecuteRequest} object to a WPS {@link ExecuteType} object. */
    @SuppressWarnings("unchecked")
    ExecuteType mapToExecute(HttpServletRequest httpRequest, ExecuteRequest executeRequest) {
        ExecuteType execute = WPS_FACTORY.createExecuteType();
        execute.setIdentifier(Ows11Util.code(process.getName()));
        DataInputsType1 dataInputsType = WPS_FACTORY.createDataInputsType1();
        execute.setDataInputs(dataInputsType);
        for (Map.Entry<String, Parameter<?>> entry : process.getInputMap().entrySet()) {
            String inputName = entry.getKey();
            Parameter<?> parameter = entry.getValue();

            InputValue inputValue = executeRequest.getInputs().get(inputName);
            if (inputValue == null) {
                if (parameter.getMinOccurs() > 0)
                    throw new APIException(
                            ServiceException.INVALID_PARAMETER_VALUE,
                            "Missing required parameter: " + inputName,
                            HttpStatus.BAD_REQUEST);
                // otherwise skip it
                continue;
            }

            if (inputValue instanceof ArrayInputValue array) {
                for (InputValue arrayEntry : array.getValues()) {
                    InputType inputType = getInputType(inputName, parameter, arrayEntry);
                    dataInputsType.getInput().add(inputType);
                }
            } else {
                InputType inputType = getInputType(inputName, parameter, inputValue);
                dataInputsType.getInput().add(inputType);
            }
        }

        // handle the outputs
        Map<String, Parameter<?>> resultInfo = process.getResultInfo();

        // filter based on output selection, while validating the chosen output names
        if (executeRequest.getOutputs() != null && !executeRequest.getOutputs().isEmpty()) {
            Set<String> selectedOutputs = executeRequest.getOutputs().keySet();
            if (!resultInfo.keySet().containsAll(selectedOutputs)) {
                Set<String> extraOutputs = selectedOutputs.stream()
                        .filter(outputName -> !resultInfo.containsKey(outputName))
                        .collect(Collectors.toSet());
                throw new APIException(
                        ServiceException.INVALID_PARAMETER_VALUE,
                        "Invalid output name(s): " + extraOutputs,
                        HttpStatus.BAD_REQUEST);
            }
            // remove the selected outputs from the resultInfo
            resultInfo.keySet().removeIf(k -> !selectedOutputs.contains(k));
        }

        // check request headers and set up the async execution
        String prefer = httpRequest.getHeader(HTTP_HEADER_PREFER);
        boolean async = prefer != null && prefer.contains("respond-async");

        // build WPS response form
        ResponseFormType responseForm = WPS_FACTORY.createResponseFormType();
        execute.setResponseForm(responseForm);
        if (resultInfo.size() != 1 || async) {
            ResponseDocumentType responseDocument = WPS_FACTORY.createResponseDocumentType();
            responseForm.setResponseDocument(responseDocument);
            responseDocument.setStatus(async);
            responseDocument.setStoreExecuteResponse(async);

            for (Map.Entry<String, Parameter<?>> entry : resultInfo.entrySet()) {
                String outputName = entry.getKey();
                Parameter<?> result = entry.getValue();
                List<ProcessParameterIO> encoders = ProcessParameterIO.findEncoder(result, context);
                if (encoders == null || encoders.isEmpty())
                    throw new APIException(
                            ServiceException.NO_APPLICABLE_CODE,
                            "Cannot handle response format for " + outputName,
                            HttpStatus.INTERNAL_SERVER_ERROR);

                // build the output
                DocumentOutputDefinitionType output = WPS_FACTORY.createDocumentOutputDefinitionType();
                output.setIdentifier(Ows11Util.code(outputName));
                String mediaType = Optional.ofNullable(executeRequest.getOutputs())
                        .map(outputs -> outputs.get(outputName))
                        .map(ExecuteOutput::getFormat)
                        .map(ExecuteOutputFormat::getMediaType)
                        .orElse(null);

                if (mediaType != null) {
                    setResponseMediaType(output, resultInfo, mediaType);
                } else {
                    ProcessParameterIO encoder = encoders.get(0);
                    if (encoder instanceof ComplexPPIO iO) {
                        output.setMimeType(iO.getMimeType());
                    }
                }

                // Using the lineage flag as a marker to indicate that the response should be raw
                // as WPS has limitations: in raw mode can only do one output, in async mode can only do document
                // This must be removed once we have a process engine that can use beans instead
                // of EMF models and has a custom serialization support for them
                if (resultInfo.size() == 1 || (executeRequest.getResponse() != DOCUMENT)) {
                    responseDocument.setLineage(true);
                }

                setupReference(executeRequest, output, outputName);
                responseDocument.getOutput().add(output);
            }
        } else {
            String resultIdentifier = resultInfo.keySet().iterator().next();

            OutputDefinitionType output;
            if (executeRequest.getResponse() == DOCUMENT) {
                DocumentOutputDefinitionType docOutput = WPS_FACTORY.createDocumentOutputDefinitionType();
                setupReference(executeRequest, docOutput, resultIdentifier);
                output = docOutput;
            } else {
                output = WPS_FACTORY.createOutputDefinitionType();
            }

            output.setIdentifier(Ows11Util.code(resultIdentifier));
            Optional.ofNullable(executeRequest.getOutputs())
                    .map(outputs -> outputs.get(resultIdentifier))
                    .map(ExecuteOutput::getFormat)
                    .map(ExecuteOutputFormat::getMediaType)
                    .ifPresent(mt -> setResponseMediaType(output, resultInfo, mt));

            if (executeRequest.getResponse() == DOCUMENT) {
                ResponseDocumentType responseDocument = WPS_FACTORY.createResponseDocumentType();
                responseForm.setResponseDocument(responseDocument);
                responseDocument.getOutput().add(output);
            } else {
                responseForm.setRawDataOutput(output);
            }
        }
        // setup the base URL for the reference outputs
        execute.setBaseUrl(APIRequestInfo.get().getBaseURL());
        return execute;

        // TODO: validate input and output provided make sense, both here and in the KVP style
    }

    private static void setupReference(
            ExecuteRequest request, DocumentOutputDefinitionType docOutput, String resultIdentifier) {
        docOutput.setAsReference(Optional.ofNullable(request.getOutputs())
                .map(outputs -> outputs.get(resultIdentifier))
                .map(o -> REFERENCE.equals(o.getTransmissionMode()))
                .orElse(false));
    }

    private InputType getInputType(String inputName, Parameter<?> parameter, InputValue inputValue) {
        // map to an input
        InputType inputType = WPS_FACTORY.createInputType();
        inputType.setIdentifier(Ows11Util.code(inputName));
        List<ProcessParameterIO> ppios = ProcessParameterIO.findDecoder(parameter, context);
        if (ppios.isEmpty()) {
            throw new IllegalArgumentException(
                    "Could not find process parameter for type " + parameter.key + "," + parameter.type);
        }

        if (inputValue instanceof LiteralInputValue literalValue) {
            // handle the literal case
            if (ppios.get(0) instanceof LiteralPPIO) {
                LiteralDataType literal = WPS_FACTORY.createLiteralDataType();
                literal.setValue(literalValue.getString());
                DataType dataType = WPS_FACTORY.createDataType();
                inputType.setData(dataType);
                dataType.setLiteralData(literal);
            } else {
                throw new RuntimeException(
                        "Unexpected situation, a literal should not map to a complex PPIO:" + literalValue);
            }
        } else if (inputValue instanceof InlineFileInputValue fileValue) {
            ComplexPPIO ppio = lookupPPIO(inputName, ppios, fileValue.getMediaType());
            ComplexDataType complex = getComplexInlineDataType(WPS_FACTORY, ppio, fileValue.getValue());
            DataType dataType = WPS_FACTORY.createDataType();
            inputType.setData(dataType);
            dataType.setComplexData(complex);
        } else if (inputValue instanceof ReferenceInputValue referenceValue) {
            // look up the type among the PPIOs
            ComplexPPIO ppio = lookupPPIO(inputName, ppios, referenceValue.getType());
            InputReferenceType reference = getRefenceType(WPS_FACTORY, ppio, referenceValue.getHref());
            inputType.setReference(reference);
        } else if (inputValue instanceof ComplexJSONInputValue complexValue) {
            ComplexPPIO ppio =
                    lookupPPIO(inputName, ppios, mime -> mime.startsWith("application/") && mime.contains("json"));

            // back to JSON so that PPIOs can parse it
            String json = complexValue.getValue().toString();
            ComplexDataType complex = getComplexInlineDataType(WPS_FACTORY, ppio, json);
            DataType dataType = WPS_FACTORY.createDataType();
            inputType.setData(dataType);
            dataType.setComplexData(complex);
        } else if (inputValue instanceof BoundingBoxInputValue bboxValue) {
            BoundingBoxType bboxType = Ows11Factory.eINSTANCE.createBoundingBoxType();
            bboxType.setLowerCorner(bboxValue.getLowerCorner());
            bboxType.setUpperCorner(bboxValue.getUpperCorner());
            bboxType.setCrs(bboxValue.getCrs());
            DataType dataType = WPS_FACTORY.createDataType();
            dataType.setBoundingBoxData(bboxType);
            inputType.setData(dataType);
        } else {
            throw new IllegalArgumentException("Cannot handle input value of type " + inputValue.getClass());
        }
        return inputType;
    }

    private ComplexPPIO lookupPPIO(String inputName, List<ProcessParameterIO> ppios, String targetType) {
        return lookupPPIO(inputName, ppios, m -> m.equalsIgnoreCase(targetType) || targetType == null);
    }

    private ComplexPPIO lookupPPIO(String inputName, List<ProcessParameterIO> ppios, Predicate<String> matcher) {
        List<ComplexPPIO> complexPPIOS = ppios.stream()
                .filter(p -> p instanceof ComplexPPIO)
                .map(p -> (ComplexPPIO) p)
                .collect(Collectors.toList());
        if (complexPPIOS.isEmpty())
            throw new APIException(
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "Cannot find a reader for " + inputName + " no supported reader found in any format",
                    HttpStatus.BAD_REQUEST);

        for (ComplexPPIO complexPPIO : complexPPIOS) {
            if (matcher.test(complexPPIO.getMimeType())) {
                return complexPPIO;
            }
        }

        throw new APIException(
                ServiceException.INVALID_PARAMETER_VALUE,
                "Cannot find a reader for " + inputName + ", supported formats are: "
                        + complexPPIOS.stream()
                                .map(ComplexPPIO::getMimeType)
                                .filter(m -> m != null)
                                .distinct()
                                .collect(Collectors.joining(", ")),
                HttpStatus.BAD_REQUEST);
    }

    /** Maps a the KVP parameters on a HTTP GET request to a WPS {@link ExecuteType} object. */
    @SuppressWarnings("unchecked")
    ExecuteType mapQueryStringToExecute(HttpServletRequest httpRequest) throws IOException {
        ExecuteType execute = WPS_FACTORY.createExecuteType();
        execute.setIdentifier(Ows11Util.code(process.getName()));
        DataInputsType1 dataInputsType = WPS_FACTORY.createDataInputsType1();
        execute.setDataInputs(dataInputsType);
        for (Map.Entry<String, Parameter<?>> entry : process.getInputMap().entrySet()) {
            String inputName = entry.getKey();
            Parameter<?> parameter = entry.getValue();

            // map to an input
            InputType inputType = WPS_FACTORY.createInputType();
            inputType.setIdentifier(Ows11Util.code(inputName));
            DataType dataType = WPS_FACTORY.createDataType();
            inputType.setData(dataType);
            List<ProcessParameterIO> ppios = ProcessParameterIO.findDecoder(parameter, context);
            if (ppios.isEmpty()) {
                throw new IllegalArgumentException(
                        "Could not find process parameter for type " + parameter.key + "," + parameter.type);
            }

            // check first for a reference
            String href = httpRequest.getParameter(inputName + LINK_HREF);
            if (href != null) {
                String type = httpRequest.getParameter(inputName + LINK_TYPE);
                ComplexPPIO ppio = lookupPPIO(inputName, ppios, type);
                InputReferenceType reference = getRefenceType(WPS_FACTORY, ppio, href);
                inputType.setReference(reference);
                inputType.setData(null);
                dataInputsType.getInput().add(inputType);
                continue;
            }

            String value = httpRequest.getParameter(inputName);
            if (value == null) {
                if (parameter.getMinOccurs() > 0)
                    throw new APIException(
                            ServiceException.INVALID_PARAMETER_VALUE,
                            "Missing required parameter: " + inputName,
                            HttpStatus.BAD_REQUEST);
                // otherwise skip it
                continue;
            }
            // get the eventual input mime type
            String type = httpRequest.getParameter(inputName + INLINE_TYPE);
            if (type == null) {
                // is it a JSON array and do we have a multi-valued parameter?
                if (parameter.getMaxOccurs() > 1) {
                    JsonNode arrayNode = parseIfJsonArray(value);
                    if (arrayNode != null) {
                        ArrayInputValue arrayInput =
                                (ArrayInputValue) new InputValueDeserializer().getInputValue(arrayNode);
                        for (InputValue arrayEntry : arrayInput.getValues()) {
                            InputType localInputType = getInputType(inputName, parameter, arrayEntry);
                            dataInputsType.getInput().add(localInputType);
                        }
                        continue;
                    }
                }

                // not recognized as an array, handle the literal case
                if (ppios.get(0) instanceof LiteralPPIO) {
                    LiteralDataType literal = WPS_FACTORY.createLiteralDataType();
                    literal.setValue(value);
                    dataType.setLiteralData(literal);
                } else if (ppios.get(0) instanceof BoundingBoxPPIO) {
                    BoundingBoxType bbox = Ows11Factory.eINSTANCE.createBoundingBoxType();
                    double[] coords = Arrays.stream(value.split("\\s*,\\s*"))
                            .mapToDouble(d -> Double.parseDouble(d))
                            .toArray();
                    String crs = httpRequest.getParameter(inputName + BBOX_CRS);
                    bbox.setCrs(crs);
                    if (coords.length == 4) {
                        bbox.setLowerCorner(Arrays.asList(coords[0], coords[1]));
                        bbox.setUpperCorner(Arrays.asList(coords[2], coords[3]));
                        if (crs == null) bbox.setCrs(CRS84);
                    } else if (coords.length == 6) {
                        bbox.setLowerCorner(Arrays.asList(coords[0], coords[1], coords[2]));
                        bbox.setUpperCorner(Arrays.asList(coords[3], coords[4], coords[5]));
                        if (crs == null) bbox.setCrs(CRS84H);
                    } else {
                        throw new APIException(
                                ServiceException.INVALID_PARAMETER_VALUE,
                                "Invalid bounding box coordinates: " + value,
                                HttpStatus.BAD_REQUEST);
                    }
                    dataType.setBoundingBoxData(bbox);
                } else {
                    // handle the complex case (only one, default)
                    ComplexPPIO complexPPIO = (ComplexPPIO) ppios.get(0);
                    ComplexDataType complex = getComplexInlineDataType(WPS_FACTORY, complexPPIO, value);
                    dataType.setComplexData(complex);
                }
            } else {
                // look up the type among the PPIOs
                ComplexPPIO complexPPIO = lookupPPIO(inputName, ppios, type);
                ComplexDataType complex = getComplexInlineDataType(WPS_FACTORY, complexPPIO, value);
                dataType.setComplexData(complex);
            }

            dataInputsType.getInput().add(inputType);
        }

        // handle the outputs, force a response
        Map<String, Parameter<?>> resultInfo = process.getResultInfo();
        ResponseFormType responseFormType = WPS_FACTORY.createResponseFormType();
        execute.setResponseForm(responseFormType);
        String responseFormat = httpRequest.getParameter(RESPONSE_FORMAT_KVP);

        // filter based on output selection (an output must be explicitly removed using output[include]=false)
        resultInfo.keySet().removeIf(k -> shouldBeRemoved(httpRequest, k));

        // check request headers and set up the async execution
        String prefer = httpRequest.getHeader(HTTP_HEADER_PREFER);
        boolean async = prefer != null && prefer.contains("respond-async");

        // The specification in OGC API processes is at the time of writing, significantly different from 1.0, and
        // still has not stabilizted. For the time being, we are implementing a hybrid approach, where:
        // - if multiple outputs are requested, we always go document mode
        // - if a single output is requested, we go raw mode
        if (resultInfo.size() > 1 || async) {
            ResponseDocumentType responseDocument = WPS_FACTORY.createResponseDocumentType();
            responseDocument.setStatus(async);
            responseDocument.setStoreExecuteResponse(async);
            responseFormType.setResponseDocument(responseDocument);
            for (Map.Entry<String, Parameter<?>> entry : resultInfo.entrySet()) {
                String outputName = entry.getKey();
                Parameter<?> result = entry.getValue();
                List<ProcessParameterIO> encoders = ProcessParameterIO.findEncoder(result, context);
                if (encoders == null || encoders.isEmpty())
                    throw new APIException(
                            ServiceException.NO_APPLICABLE_CODE,
                            "Cannot handle response format for " + outputName,
                            HttpStatus.INTERNAL_SERVER_ERROR);

                DocumentOutputDefinitionType output = WPS_FACTORY.createDocumentOutputDefinitionType();
                output.setIdentifier(Ows11Util.code(outputName));
                if (async && resultInfo.size() == 1 && responseFormat != null) {
                    setResponseMediaType(output, resultInfo, responseFormat);
                } else {
                    ProcessParameterIO encoder = encoders.get(0);
                    if (encoder instanceof ComplexPPIO iO) {
                        output.setMimeType(iO.getMimeType());
                    }
                }

                // WPS can execute async only in document response mode, but populate the
                // raw one too for the response to be written correctly (as raw)... using the
                // lineage flag as a marker to indicate that the response is async but should be raw
                if (resultInfo.size() == 1 && async) {
                    responseDocument.setLineage(true);
                }

                responseDocument.getOutput().add(output);
            }
        } else {
            OutputDefinitionType outputType = WPS_FACTORY.createOutputDefinitionType();
            outputType.setIdentifier(
                    Ows11Util.code(resultInfo.keySet().iterator().next()));
            if (responseFormat != null && !responseFormat.isEmpty()) {
                setResponseMediaType(outputType, resultInfo, responseFormat);
            }

            responseFormType.setRawDataOutput(outputType);
        }
        // setup the base URL for the reference outputs
        execute.setBaseUrl(APIRequestInfo.get().getBaseURL());

        return execute;
    }

    private static Boolean shouldBeRemoved(HttpServletRequest httpRequest, String k) {
        return Optional.ofNullable(httpRequest.getParameter(k + INCLUDE))
                .map("false"::equalsIgnoreCase)
                .orElse(false);
    }

    /** Cehcks if the input is a JSON array, in that case, parses and returns it. If not, null is returned instead. */
    private static JsonNode parseIfJsonArray(String input) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(input);
            if (node.isArray()) {
                return node;
            }
        } catch (JsonProcessingException e) {
            // Not a valid JSON, fine, the purpose is to check if it is a JSON array
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static ComplexDataType getComplexInlineDataType(
            Wps10Factory wpsFactory, ComplexPPIO complexPPIO, String base64Value) {
        ComplexDataType complex = wpsFactory.createComplexDataType();
        complex.setMimeType(complexPPIO.getMimeType());
        complex.getData().add(base64Value);
        complex.setEncoding("base64");
        return complex;
    }

    private static InputReferenceType getRefenceType(Wps10Factory wpsFactory, ComplexPPIO complexPPIO, String href) {
        InputReferenceType reference = wpsFactory.createInputReferenceType();
        reference.setMimeType(complexPPIO.getMimeType());
        reference.setHref(href);
        return reference;
    }

    private void setResponseMediaType(
            OutputDefinitionType outputType, Map<String, Parameter<?>> resultInfo, String responseFormat) {
        String[] formats = responseFormat.split("\\s*,\\s*");
        Set<String> formatsSet = Set.of(formats);
        Parameter<?> result = resultInfo.get(outputType.getIdentifier().getValue());
        List<ProcessParameterIO> encoders = ProcessParameterIO.findEncoder(result, context);
        // look for complex ones
        for (ProcessParameterIO encoder : encoders) {
            if (encoder instanceof ComplexPPIO complexEncoder) {
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
}
