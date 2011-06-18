/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.response;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import net.opengis.ows11.BoundingBoxType;
import net.opengis.wps10.ExecuteResponseType;
import net.opengis.wps10.ExecuteType;
import net.opengis.wps10.LiteralDataType;
import net.opengis.wps10.OutputDataType;
import net.opengis.wps10.OutputDefinitionType;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.geoserver.ows.Response;
import org.geoserver.ows.XmlObjectEncodingResponse;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wps.BinaryEncoderDelegate;
import org.geoserver.wps.CDataEncoderDelegate;
import org.geoserver.wps.Execute;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.XMLEncoderDelegate;
import org.geotools.ows.v1_1.OWS;
import org.geotools.ows.v1_1.OWSConfiguration;
import org.geotools.xml.Encoder;

/**
 * Encodes the Execute response either in the normal XML format or in the raw format
 * @author Andrea Aime
 *
 */
public class ExecuteProcessResponse extends Response {

    XmlObjectEncodingResponse standardResponse;

    public ExecuteProcessResponse(Class binding, String elementName, Class xmlConfiguration) {
        super(ExecuteResponseType.class);
        this.standardResponse = new XmlObjectEncodingResponse(binding, elementName, xmlConfiguration);
    }
    
    @Override
    public String getMimeType(Object value, Operation operation)
            throws ServiceException {
        ExecuteType execute = (ExecuteType) operation.getParameters()[0];
        if (execute.getResponseForm().getRawDataOutput() == null) {
            // normal execute response encoding
            return standardResponse.getMimeType(value, operation);
        } else {
            // raw response, let's see what the output is
            ExecuteResponseType response = (ExecuteResponseType) value;
            OutputDataType result = (OutputDataType) response
                    .getProcessOutputs().getOutput().get(0);
            LiteralDataType literal = result.getData().getLiteralData();
            if(literal != null) {
                // literals are encoded as plain strings
                return "text/plain";
            } else {
                // Execute should have properly setup the mime type
                OutputDefinitionType definition = (OutputDefinitionType) response
                    .getOutputDefinitions().getOutput().get(0);
                return definition.getMimeType();
            }
        }

    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        ExecuteType execute = (ExecuteType) operation.getParameters()[0];
        if (execute.getResponseForm().getRawDataOutput() == null) {
            // normal execute response encoding
            standardResponse.write(value, output, operation);
        } else {
            // raw response, let's see what the output is
            ExecuteResponseType response = (ExecuteResponseType) value;
            OutputDataType result = (OutputDataType) response
                    .getProcessOutputs().getOutput().get(0);
            LiteralDataType literal = result.getData().getLiteralData();
            BoundingBoxType bbox = result.getData().getBoundingBoxData();
            if (literal != null) {
                writeLiteral(output, literal);
            } else if(bbox != null) {
                writeBBox(output, bbox);
            } else {
                writeComplex(output, result);
            }
        }
    }

    private void writeBBox(OutputStream os, BoundingBoxType bbox) throws IOException {
        Encoder encoder = new Encoder(new OWSConfiguration());
        encoder.encode(bbox, OWS.BoundingBox, os);
    }

    /**
     * Write out complex data assuming {@link Execute} has set up the proper
     * encoder as the output
     * 
     * @param output
     * @param result
     * @throws IOException
     */
    void writeComplex(OutputStream output, OutputDataType result)
            throws IOException {
        Object rawResult = result.getData().getComplexData().getData().get(0);
        if (rawResult instanceof XMLEncoderDelegate) {
            XMLEncoderDelegate delegate = (XMLEncoderDelegate) rawResult;
            XMLSerializer xmls = new XMLSerializer(output, new OutputFormat());
            xmls.setNamespaces(true);

            try {
                delegate.encode(xmls);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new WPSException("An error occurred while encoding "
                        + "the results of the process", e);
            }
        } else if(rawResult instanceof CDataEncoderDelegate) {
        	try {
                ((CDataEncoderDelegate) rawResult).encode(output);
        	} catch(Exception e) {
        		throw new WPSException("An error occurred while encoding "
                        + "the results of the process", e);
        	}
        } else if(rawResult instanceof BinaryEncoderDelegate) {
        	try {
        		((BinaryEncoderDelegate) rawResult).encode(output);
        	} catch(Exception e) {
        		throw new WPSException("An error occurred while encoding "
                        + "the results of the process", e);
        	}
        } else {
            throw new WPSException("Cannot encode an object of class "
                    + rawResult.getClass() + " in raw form");
        }
    }

    /**
     * Write out literal results by converting them to strings
     * @param output
     * @param literal
     */
    void writeLiteral(OutputStream output, LiteralDataType literal) {
        PrintWriter writer = new PrintWriter(output);
        writer.write(literal.getValue());
        writer.flush();
    }

    

}
