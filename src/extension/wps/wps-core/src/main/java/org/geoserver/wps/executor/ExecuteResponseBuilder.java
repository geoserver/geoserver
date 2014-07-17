/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.datatype.XMLGregorianCalendar;

import net.opengis.ows11.BoundingBoxType;
import net.opengis.ows11.CodeType;
import net.opengis.ows11.ExceptionReportType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.wps10.ComplexDataType;
import net.opengis.wps10.DataType;
import net.opengis.wps10.DocumentOutputDefinitionType;
import net.opengis.wps10.ExecuteResponseType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.InputType;
import net.opengis.wps10.LiteralDataType;
import net.opengis.wps10.OutputDataType;
import net.opengis.wps10.OutputDefinitionType;
import net.opengis.wps10.OutputDefinitionsType;
import net.opengis.wps10.OutputReferenceType;
import net.opengis.wps10.ProcessBriefType;
import net.opengis.wps10.ProcessFailedType;
import net.opengis.wps10.ProcessOutputsType1;
import net.opengis.wps10.ProcessStartedType;
import net.opengis.wps10.ResponseDocumentType;
import net.opengis.wps10.Wps10Factory;

import org.apache.commons.io.IOUtils;
import org.eclipse.emf.common.util.EList;
import org.geoserver.ows.Ows11Util;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wps.BinaryEncoderDelegate;
import org.geoserver.wps.CDataEncoderDelegate;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.XMLEncoderDelegate;
import org.geoserver.wps.executor.ExecutionStatus.ProcessState;
import org.geoserver.wps.ppio.BinaryPPIO;
import org.geoserver.wps.ppio.BoundingBoxPPIO;
import org.geoserver.wps.ppio.CDataPPIO;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.LiteralPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.ppio.XMLPPIO;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geoserver.wps.resource.GridCoverageResource;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.data.Parameter;
import org.geotools.process.ProcessFactory;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.geotools.xml.EMFUtils;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.feature.type.Name;
import org.springframework.context.ApplicationContext;

public class ExecuteResponseBuilder {
    
    static final Logger LOGGER = Logging.getLogger(ExecuteResponseBuilder.class);

    ExecuteType request;

    ExecutionStatus status;

    Map<String, Object> outputs;

    Date created;

    boolean verboseExceptions;

    Throwable exception;

    ApplicationContext context;

    String executionId;

    WPSResourceManager resourceManager;

    public ExecuteResponseBuilder(ExecuteType request, ApplicationContext context, Date created) {
        this.request = request;
        this.created = created;
        this.context = context;
        this.resourceManager = context.getBean(WPSResourceManager.class);
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public void setOutputs(Map<String, Object> outputs) {
        this.outputs = outputs;
        // mark the output coverages as resources to be cleaned after
        // ... hmmm.. wondering if we could make this more pluggable...
        for (Object result : outputs.values()) {
            if(result instanceof GridCoverage) {
                resourceManager.addResource(new GridCoverageResource(((GridCoverage) result)));
            }
        }
    }

    public ExecuteResponseType build() {
        ExecuteRequest helper = new ExecuteRequest(request);

        // build the response
        Wps10Factory f = Wps10Factory.eINSTANCE;
        ExecuteResponseType response = f.createExecuteResponseType();
        response.setLang("en");
        if (request.getBaseUrl() != null) {
            response.setServiceInstance(ResponseUtils.appendQueryString(
                    ResponseUtils.buildURL(request.getBaseUrl(), "ows", null, URLType.SERVICE), ""));
        }

        // process
        Name processName = helper.getProcessName();
        ProcessFactory pf = GeoServerProcessors.createProcessFactory(processName);
        final ProcessBriefType process = f.createProcessBriefType();
        response.setProcess(process);
        // damn blasted EMF changes the state of request if we set its identifier on
        // another object! (I guess, following some strict ownership rule...)
        process.setIdentifier((CodeType) EMFUtils.clone(request.getIdentifier(),
                Ows11Factory.eINSTANCE, true));
        process.setProcessVersion(pf.getVersion(processName));
        process.setTitle(Ows11Util.languageString(pf.getTitle(processName)));
        process.setAbstract(Ows11Util.languageString(pf.getDescription(processName)));

        // status
        response.setStatus(f.createStatusType());
        XMLGregorianCalendar gc = Converters.convert(created, XMLGregorianCalendar.class);
        response.getStatus().setCreationTime(gc);
        if (status == null) {
            if (exception != null) {
                setResponseFailed(response, getException(ProcessState.COMPLETED));
            } else if (outputs == null) {
                response.getStatus().setProcessAccepted("Process accepted.");
            } else {
                response.getStatus().setProcessSucceeded("Process succeeded.");
            }
        } else {
            if (status.getPhase() == ProcessState.QUEUED) {
                response.getStatus().setProcessAccepted("Process accepted.");
            } else if (status.getPhase() == ProcessState.RUNNING) {
                ProcessStartedType startedType = f.createProcessStartedType();
                int progressPercent = Math.round(status.getProgress());
                if(progressPercent < 0) {
                    LOGGER.warning("Progress reported is below zero, fixing it to 0: " + progressPercent);
                    progressPercent = 0;
                } else if(progressPercent > 100) {
                    LOGGER.warning("Progress reported is above 100, fixing it to 100: " + progressPercent);
                    progressPercent = 100;
                }
                startedType.setPercentCompleted(new BigInteger(String.valueOf(progressPercent)));
                startedType.setValue(status.getTask());
                response.getStatus().setProcessStarted(startedType);
            } else if (status.getPhase() == ProcessState.COMPLETED) {
                response.getStatus().setProcessSucceeded("Process succeeded.");
            } else {
                ServiceException reportException = getException(status.getPhase());
                setResponseFailed(response, reportException);
            }
        }

        // status location, if asynch
        if (helper.isAsynchronous() && request.getBaseUrl() != null && executionId != null) {
            Map<String, String> kvp = new LinkedHashMap<String, String>();
            kvp.put("service", "WPS");
            kvp.put("version", "1.0.0");
            kvp.put("request", "GetExecutionStatus");
            kvp.put("executionId", executionId);
            response.setStatusLocation(ResponseUtils.buildURL(request.getBaseUrl(), "ows", kvp, URLType.SERVICE));

        }

        // lineage, should be included only if requested, the response should contain it
        // even if the process is not done computing. From the spec:
        // * If lineage is "true" the server shall include in the execute response a complete copy
        // of
        // the DataInputs and OutputDefinition elements _as received in the execute request_.
        // *If lineage is "false" then/ these elements shall be omitted from the response
        if (helper.isLineageRequested()) {
            // inputs
            if (request.getDataInputs() != null && request.getDataInputs().getInput().size() > 0) {
                response.setDataInputs(f.createDataInputsType1());
                for (Iterator i = request.getDataInputs().getInput().iterator(); i.hasNext();) {
                    InputType input = (InputType) i.next();
                    response.getDataInputs().getInput().add(EMFUtils.clone(input, f, true));
                }
            }

            // output definitions, if any was requested explicitly
            List<DocumentOutputDefinitionType> outputList = helper.getRequestedOutputs();
            if (outputList != null) {
                OutputDefinitionsType outputs = f.createOutputDefinitionsType();
                response.setOutputDefinitions(outputs);
                for (DocumentOutputDefinitionType output : outputList) {
                    outputs.getOutput().add(EMFUtils.clone(output, f, true));
                }
            }
        }

        // process outputs
        if (exception == null && outputs != null) {
            ProcessOutputsType1 processOutputs = f.createProcessOutputsType1();
            response.setProcessOutputs(processOutputs);

            Map<String, Parameter<?>> resultInfo = pf.getResultInfo(processName, null);

            if (request.getResponseForm() != null
                    && request.getResponseForm().getResponseDocument() != null
                    && request.getResponseForm().getResponseDocument().getOutput() != null
                    && request.getResponseForm().getResponseDocument().getOutput().size() > 0) {
                // we have a selection of outputs, possibly with indication of mime type
                // and reference encoding
                EList outputs = request.getResponseForm().getResponseDocument().getOutput();
                for (Object object : outputs) {
                    DocumentOutputDefinitionType odt = (DocumentOutputDefinitionType) object;
                    String key = odt.getIdentifier().getValue();
                    Parameter<?> outputParam = resultInfo.get(key);
                    if (outputParam == null) {
                        throw new WPSException("Unknown output " + key + " possible values are: "
                                + resultInfo.keySet());
                    }

                    String mimeType = odt.getMimeType();
                    OutputDataType output = encodeOutput(key, outputParam, mimeType, odt.isAsReference());
                    processOutputs.getOutput().add(output);
                }
            } else {
                // encode all as inline for the moment
                for (String key : outputs.keySet()) {
                    Parameter<?> outputParam = resultInfo.get(key);
                    OutputDataType output = encodeOutput(key, outputParam, null, false);
                    processOutputs.getOutput().add(output);
                }
            }
        }

        return response;
    }

    OutputDataType encodeOutput(String key, Parameter<?> outputParam, String mimeType,
            boolean reference) {
        Wps10Factory f = Wps10Factory.eINSTANCE;
        OutputDataType output = f.createOutputDataType();
        output.setIdentifier(Ows11Util.code(key));
        output.setTitle(Ows11Util.languageString(outputParam.description));

        final Object o = outputs.get(key);
        if (mimeType == null) {
            mimeType = getOutputMimeType(key);
        }
        ProcessParameterIO ppio = ProcessParameterIO.find(outputParam, context, mimeType);

        if (ppio == null) {
            throw new WPSException("Don't know how to encode output " + key + " in mime type "
                    + mimeType);
        }

        try {
            if (reference && ppio instanceof ComplexPPIO) {
                // encode as reference
                OutputReferenceType outputReference = f.createOutputReferenceType();
                output.setReference(outputReference);
                
                ComplexPPIO cppio = (ComplexPPIO) ppio;
                File file = resourceManager.getOutputFile(executionId, key + "." + cppio.getFileExtension());
                
                // write out the file
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(file);
                    cppio.encode(o, fos);
                } finally {
                    IOUtils.closeQuietly(fos);
                }
                
                // create the link
                Map<String, String> kvp = new LinkedHashMap<String, String>();
                kvp.put("service", "WPS");
                kvp.put("version", "1.0.0");
                kvp.put("request", "GetExecutionResult");
                kvp.put("executionId", executionId);
                kvp.put("outputId", file.getName());
                kvp.put("mimetype", cppio.getMimeType());
                outputReference.setHref(ResponseUtils.buildURL(request.getBaseUrl(), "ows", kvp, URLType.SERVICE));
                outputReference.setMimeType(cppio.getMimeType());
            } else {
                // encode as data
                DataType data = f.createDataType();
                output.setData(data);

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
            }
        } catch (Exception e) {
            throw new WPSException("Failed to encode the " + key + " output", e);
        }
        return output;
    }

    void setResponseFailed(ExecuteResponseType response, ServiceException reportException) {
        Wps10Factory f = Wps10Factory.eINSTANCE;
        ProcessFailedType failedType = f.createProcessFailedType();
        ExceptionReportType report = Ows11Util.exceptionReport(reportException, verboseExceptions,
                "1.1.0");
        failedType.setExceptionReport(report);
        response.getStatus().setProcessFailed(failedType);
    }

    /**
     * Gets the mime type for the specified output
     * 
     * @param key
     * @return
     */
    private String getOutputMimeType(String key) {
        // lookup for the OutputDefinitionType
        OutputDefinitionType odt = request.getResponseForm().getRawDataOutput();
        ResponseDocumentType responseDocument = request.getResponseForm().getResponseDocument();
        if (responseDocument != null && odt == null) {
            Iterator it = responseDocument.getOutput().iterator();
            while (it.hasNext()) {
                OutputDefinitionType curr = (OutputDefinitionType) it.next();
                if (curr.getIdentifier().getValue().equals(key)) {
                    odt = curr;
                    break;
                }
            }
        }

        // have we got anything?
        if (odt != null) {
            return odt.getMimeType();
        } else {
            return null;
        }
    }

    private ServiceException getException(ProcessState phase) {
        if (phase == ProcessState.CANCELLED) {
            return new WPSException("Process was cancelled by the administrator");
        } else {
            return new WPSException("Process failed during execution", exception);
        }
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
}
