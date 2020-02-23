/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.kvp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.opengis.ows11.BoundingBoxType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.wps10.ComplexDataType;
import net.opengis.wps10.DataInputsType1;
import net.opengis.wps10.DocumentOutputDefinitionType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.InputReferenceType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.LiteralDataType;
import net.opengis.wps10.OutputDefinitionType;
import net.opengis.wps10.ResponseDocumentType;
import net.opengis.wps10.ResponseFormType;
import net.opengis.wps10.Wps10Factory;
import org.geoserver.ows.Ows11Util;
import org.geoserver.ows.kvp.EMFKvpRequestReader;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.ppio.BoundingBoxPPIO;
import org.geoserver.wps.ppio.LiteralPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.data.Parameter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.geotools.process.ProcessFactory;
import org.opengis.feature.type.Name;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ExecuteKvpRequestReader extends EMFKvpRequestReader
        implements ApplicationContextAware {

    ApplicationContext applicationContext;

    public ExecuteKvpRequestReader() {
        super(ExecuteType.class, Wps10Factory.eINSTANCE);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        ExecuteType execute = (ExecuteType) super.read(request, kvp, rawKvp);
        Wps10Factory factory = Wps10Factory.eINSTANCE;

        // grab the process, we need it to parse the data inputs
        Name processName = Ows11Util.name(execute.getIdentifier());
        ProcessFactory pf = GeoServerProcessors.createProcessFactory(processName, false);
        if (pf == null) {
            throw new WPSException("No such process: " + processName);
        }

        // parse inputs
        List<InputType> inputs =
                parseDataInputs(
                        pf.getParameterInfo(processName), (String) rawKvp.get("DataInputs"));
        DataInputsType1 input1 = factory.createDataInputsType1();
        input1.getInput().addAll(inputs);
        execute.setDataInputs(input1);

        if (rawKvp.containsKey("responseDocument")) {
            execute.setResponseForm(
                    parseResponseDocument(
                            pf.getResultInfo(processName, null),
                            (String) rawKvp.get("responseDocument")));
        } else if (rawKvp.containsKey("rawDataOutput")) {
            execute.setResponseForm(
                    parseRawDataOutput(
                            pf.getResultInfo(processName, null),
                            (String) rawKvp.get("rawDataOutput")));
        } else {
            ResponseFormType responseForm = factory.createResponseFormType();
            responseForm.setResponseDocument(factory.createResponseDocumentType());
            execute.setResponseForm(responseForm);
        }

        if ("true".equals(kvp.get("storeExecuteResponse"))) {
            if (execute.getResponseForm().getResponseDocument() == null) {
                throw new WPSException(
                        "InvalidParameterValue",
                        "Cannot store the response for raw data outputs, "
                                + "please use response document instead");
            }
            execute.getResponseForm().getResponseDocument().setStoreExecuteResponse(true);
        }

        if ("true".equals(kvp.get("lineage"))) {
            if (execute.getResponseForm().getResponseDocument() == null) {
                throw new WPSException(
                        "InvalidParameterValue",
                        "Cannot provide lineage in the response for raw data outputs, "
                                + "please use response document instead");
            }
            execute.getResponseForm().getResponseDocument().setLineage(true);
        }

        if ("true".equals(kvp.get("status"))) {
            if (execute.getResponseForm().getResponseDocument() == null) {
                throw new WPSException(
                        "InvalidParameterValue",
                        "Cannot add status with raw data outputs, "
                                + "please use response document with store option instead");
            }
            if (!execute.getResponseForm().getResponseDocument().isStoreExecuteResponse()) {
                throw new WPSException(
                        "InvalidParameterValue",
                        "Cannot add status if the response "
                                + "is not stored, please add storeExecuteResponse=true your request");
            }
            execute.getResponseForm().getResponseDocument().setStatus(true);
        }

        return execute;
    }

    protected boolean filter(String kvp) {
        return "DataInputs".equalsIgnoreCase(kvp)
                || "responseDocument".equalsIgnoreCase(kvp)
                || "rawDataOutput".equalsIgnoreCase(kvp);
    };

    List<InputType> parseDataInputs(Map<String, Parameter<?>> inputParams, String inputString) {
        List<IOParam> params = parseIOParameters(inputString);

        List<InputType> result = new ArrayList<InputType>();
        for (IOParam ioParam : params) {
            // common
            Wps10Factory factory = Wps10Factory.eINSTANCE;
            InputType it = factory.createInputType();
            it.setIdentifier(Ows11Util.code(ioParam.id));
            it.setData(factory.createDataType());

            Parameter<?> gtParam = inputParams.get(ioParam.id);
            if (gtParam == null) {
                throw new WPSException("Unknown data input named '" + ioParam.id + "'");
            }
            ProcessParameterIO ppio =
                    ProcessParameterIO.findAll(gtParam, applicationContext).get(0);

            if (ppio instanceof LiteralPPIO) {
                it.getData().setLiteralData(parseLiteral(it, factory, ioParam));
            } else if (ppio instanceof BoundingBoxPPIO) {
                it.getData().setBoundingBoxData(parseBoundingBox(it, factory, ioParam));
            } else if (ioParam.isReference()) {
                it.setReference(parseReferenceType(it, factory, ioParam));
            } else {
                it.getData().setComplexData(parseComplex(it, factory, ioParam));
            }

            result.add(it);
        }

        return result;
    }

    /** Parses a list of a I/O parameters */
    List<IOParam> parseIOParameters(String inputString) {
        List<IOParam> result = new ArrayList<IOParam>();

        if (inputString == null || "".equals(inputString.trim())) {
            return Collections.emptyList();
        }

        // inputs are separated by ;
        String[] inputs = inputString.split(";");

        for (String input : inputs) {
            // separate the id form the value/attribute
            int idx = input.indexOf("=");
            if (idx == -1) {
                result.add(new IOParam(input, null, Collections.EMPTY_MAP));
            } else {
                String inputId = input.substring(0, idx);
                String[] valueAttributes = input.substring(idx + 1, input.length()).split("@");

                String value = valueAttributes[0];
                Map<String, String> attributes = parseAttributes(valueAttributes);

                result.add(new IOParam(inputId, value, attributes));
            }
        }

        return result;
    }

    Map<String, String> parseAttributes(String[] attributes) {
        Map<String, String> result = new HashMap<String, String>();

        // start from 1, 0 is the value
        for (int i = 1; i < attributes.length; i++) {
            final String att = attributes[i];
            int idx = att.indexOf("=");
            if (idx == -1) {
                throw new WPSException("Invalid syntax for data input attribute: @" + att);
            }
            if (idx == att.length() - 1) {
                result.put(att.substring(0, idx), null);
            } else {
                result.put(att.substring(0, idx), att.substring(idx + 1, att.length()));
            }
        }

        return result;
    }

    private InputReferenceType parseReferenceType(
            InputType it, Wps10Factory factory, IOParam param) {
        InputReferenceType ref = factory.createInputReferenceType();
        if (param.attributes.containsKey("href")) {
            ref.setHref(param.attributes.get("href"));
        } else {
            ref.setHref(param.attributes.get("xlink:href"));
        }
        ref.setEncoding(param.attributes.get("encoding"));
        ref.setMimeType(param.attributes.get("mimetype"));
        ref.setSchema(param.attributes.get("schema"));

        return ref;
    }

    private ComplexDataType parseComplex(InputType it, Wps10Factory factory, IOParam param) {
        ComplexDataType complex = factory.createComplexDataType();
        complex.getData().add(param.value);
        complex.setEncoding(param.attributes.get("encoding"));
        complex.setMimeType(param.attributes.get("mimetype"));
        complex.setSchema(param.attributes.get("schema"));

        return complex;
    }

    private BoundingBoxType parseBoundingBox(InputType it, Wps10Factory factory, IOParam param) {
        try {
            ReferencedEnvelope envelope =
                    (ReferencedEnvelope)
                            new org.geoserver.wfs.kvp.BBoxKvpParser().parse(param.value);
            if (envelope != null) {
                BoundingBoxType bbox = Ows11Factory.eINSTANCE.createBoundingBoxType();
                if (envelope.getCoordinateReferenceSystem() != null) {
                    bbox.setCrs(
                            GML2EncodingUtils.epsgCode(envelope.getCoordinateReferenceSystem()));
                }
                List<Double> min = new ArrayList<Double>(envelope.getDimension());
                List<Double> max = new ArrayList<Double>(envelope.getDimension());
                for (int i = 0; i < envelope.getDimension(); i++) {
                    min.set(i, envelope.getMinimum(i));
                    max.set(i, envelope.getMaximum(i));
                }

                return bbox;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new WPSException("Failed to parse the bounding box", e);
        }
    }

    private LiteralDataType parseLiteral(InputType it, Wps10Factory factory, IOParam param) {
        LiteralDataType literal = factory.createLiteralDataType();
        literal.setValue(param.value);
        literal.setDataType(param.attributes.get("datatype"));
        literal.setUom(param.attributes.get("uom"));

        return literal;
    }

    ResponseFormType parseResponseDocument(
            Map<String, Parameter<?>> outputs, String responseDefinition) {
        Wps10Factory factory = Wps10Factory.eINSTANCE;
        List<IOParam> ioParams = parseIOParameters(responseDefinition);

        ResponseFormType response = factory.createResponseFormType();
        ResponseDocumentType doc = factory.createResponseDocumentType();
        response.setResponseDocument(doc);

        for (IOParam ioParam : ioParams) {
            doc.getOutput().add(parseOutputDefinitionType(outputs, factory, ioParam, true));
        }

        return response;
    }

    OutputDefinitionType parseOutputDefinitionType(
            Map<String, Parameter<?>> outputs,
            Wps10Factory factory,
            IOParam ioParam,
            boolean inDocument) {
        if (!outputs.containsKey(ioParam.id)) {
            throw new WPSException("Unknown output " + ioParam.id);
        }

        OutputDefinitionType odt;
        if (inDocument) {
            DocumentOutputDefinitionType dout = factory.createDocumentOutputDefinitionType();
            dout.setAsReference(Boolean.parseBoolean(ioParam.attributes.get("asReference")));
            odt = dout;
        } else {
            odt = factory.createOutputDefinitionType();
        }
        odt.setIdentifier(Ows11Util.code(ioParam.id));
        odt.setEncoding(ioParam.attributes.get("encoding"));
        odt.setMimeType(ioParam.attributes.get("mimetype"));
        odt.setSchema(ioParam.attributes.get("schema"));
        odt.setUom(ioParam.attributes.get("uom"));

        return odt;
    }

    ResponseFormType parseRawDataOutput(Map<String, Parameter<?>> resultInfo, String rawOutputs) {
        Wps10Factory factory = Wps10Factory.eINSTANCE;
        ResponseFormType response = factory.createResponseFormType();

        List<IOParam> ioParams = parseIOParameters(rawOutputs);

        if (ioParams.size() == 0) {
            return response;
        }
        if (ioParams.size() > 1) {
            throw new WPSException("There can be only one RawDataOutput");
        }

        response.setRawDataOutput(
                parseOutputDefinitionType(resultInfo, factory, ioParams.get(0), false));

        return response;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    class IOParam {
        String id;
        String value;
        Map<String, String> attributes;

        public IOParam(String id, String value, Map<String, String> attributes) {
            this.id = id;
            this.value = value;
            this.attributes = attributes;
        }

        public boolean isReference() {
            return attributes.keySet().contains("href")
                    || attributes.keySet().contains("xlink:href");
        }
    }
}
