/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public final class EchoParametersDao {

    private static final Logger LOGGER = Logging.getLogger(EchoParametersDao.class);
    private static final String NEW_LINE = System.getProperty("line.separator");

    private static final GeoServerDataDirectory DATA_DIRECTORY =
            (GeoServerDataDirectory) GeoServerExtensions.bean("dataDirectory");

    public static String getEchoParametersPath() {
        return "params-extractor/echo-parameters.xml";
    }

    public static String getTmpEchoParametersPath() {
        return String.format("params-extractor/%s-echo-parameters.xml", UUID.randomUUID());
    }

    public static List<EchoParameter> getEchoParameters() {
        Resource echoParameters = DATA_DIRECTORY.get(getEchoParametersPath());
        return getEchoParameters(echoParameters.in());
    }

    public static List<EchoParameter> getEchoParameters(InputStream inputStream) {
        try {
            if (inputStream.available() == 0) {
                Utils.debug(LOGGER, "Echo parameters file seems to be empty.");
                return new ArrayList<>();
            }
            EchoParameterHandler handler = new EchoParameterHandler();
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            saxParser.parse(inputStream, handler);
            return handler.echoParameters;
        } catch (Exception exception) {
            throw Utils.exception(exception, "Error parsing echo parameters files.");
        } finally {
            Utils.closeQuietly(inputStream);
        }
    }

    public static void saveOrUpdateEchoParameter(EchoParameter echoParameter) {
        Resource echoParameters = DATA_DIRECTORY.get(getEchoParametersPath());
        Resource tmpEchoParameters = DATA_DIRECTORY.get(getTmpEchoParametersPath());
        saveOrUpdateEchoParameter(echoParameter, echoParameters.in(), tmpEchoParameters.out());
        echoParameters.delete();
        tmpEchoParameters.renameTo(echoParameters);
    }

    public static void saveOrUpdateEchoParameter(
            EchoParameter echoParameter, InputStream inputStream, OutputStream outputStream) {
        try {
            List<EchoParameter> echoParameters = getEchoParameters(inputStream);
            boolean exists = false;
            for (int i = 0; i < echoParameters.size() && !exists; i++) {
                if (echoParameters.get(i).getId().equals(echoParameter.getId())) {
                    echoParameters.set(i, echoParameter);
                    exists = true;
                }
            }
            if (!exists) {
                echoParameters.add(echoParameter);
            }
            writeEchoParameters(echoParameters, outputStream);
        } finally {
            Utils.closeQuietly(inputStream);
            Utils.closeQuietly(outputStream);
        }
    }

    public static void deleteEchoParameters(String... echoParametersIds) {
        Resource echoParameters = DATA_DIRECTORY.get(getEchoParametersPath());
        Resource tmpEchoParameters = DATA_DIRECTORY.get(getTmpEchoParametersPath());
        deleteEchoParameters(echoParameters.in(), tmpEchoParameters.out(), echoParametersIds);
        echoParameters.delete();
        tmpEchoParameters.renameTo(echoParameters);
    }

    public static void deleteEchoParameters(
            InputStream inputStream, OutputStream outputStream, String... forwardParameterIds) {
        try {
            writeEchoParameters(
                    getEchoParameters(inputStream)
                            .stream()
                            .filter(
                                    forwardParameter ->
                                            !Arrays.stream(forwardParameterIds)
                                                    .anyMatch(
                                                            forwardParameterId ->
                                                                    forwardParameterId.equals(
                                                                            forwardParameter
                                                                                    .getId())))
                            .collect(Collectors.toList()),
                    outputStream);
        } finally {
            Utils.closeQuietly(inputStream);
            Utils.closeQuietly(outputStream);
        }
    }

    private static void writeEchoParameters(
            List<EchoParameter> echoParameters, OutputStream outputStream) {
        try {
            XMLStreamWriter output =
                    XMLOutputFactory.newInstance()
                            .createXMLStreamWriter(new OutputStreamWriter(outputStream, "utf-8"));
            output.writeStartDocument();
            output.writeCharacters(NEW_LINE);
            output.writeStartElement("EchoParameters");
            output.writeCharacters(NEW_LINE);
            echoParameters.forEach(echoParameter -> writeEchoParameter(echoParameter, output));
            output.writeEndElement();
            output.writeCharacters(NEW_LINE);
            output.writeEndDocument();
            output.close();
        } catch (Exception exception) {
            throw Utils.exception(exception, "Something bad happen when writing echo parameters.");
        }
    }

    private static void writeEchoParameter(EchoParameter echoParameter, XMLStreamWriter output) {
        try {
            output.writeCharacters("  ");
            output.writeStartElement("EchoParameter");
            writeAttribute("id", echoParameter.getId(), output);
            writeAttribute("parameter", echoParameter.getParameter(), output);
            writeAttribute("activated", echoParameter.getActivated(), output);
            output.writeEndElement();
            output.writeCharacters(NEW_LINE);
        } catch (Exception exception) {
            throw Utils.exception(
                    exception, "Error writing echo parameter %s.", echoParameter.getId());
        }
    }

    private static <T> void writeAttribute(String name, T value, XMLStreamWriter output)
            throws Exception {
        if (value != null) {
            output.writeAttribute(name, value.toString());
        }
    }

    private static final class EchoParameterHandler extends DefaultHandler {

        final List<EchoParameter> echoParameters = new ArrayList<>();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            if (!qName.equalsIgnoreCase("EchoParameter")) {
                return;
            }
            Utils.debug(LOGGER, "Start parsing echo parameter.");
            EchoParameterBuilder echoParameterBuilder = new EchoParameterBuilder();
            getAttribute("id", attributes, echoParameterBuilder::withId);
            getAttribute("parameter", attributes, echoParameterBuilder::withParameter);
            getAttribute(
                    "activated",
                    attributes,
                    compose(Boolean::valueOf, echoParameterBuilder::withActivated));
            echoParameters.add(echoParameterBuilder.build());
            Utils.debug(LOGGER, "End parsing echo parameter.");
        }

        private static <T> Consumer<String> compose(
                Function<String, T> convert, Consumer<T> setter) {
            return (value) -> setter.accept(convert.apply(value));
        }

        private void getAttribute(
                String attributeName, Attributes attributes, Consumer<String> setter) {
            String attributeValue = attributes.getValue(attributeName);
            if (attributeValue == null) {
                Utils.debug(LOGGER, "Echo parameter attribute %s is NULL.", attributeName);
                return;
            }
            Utils.debug(
                    LOGGER, "Echo parameter attribute %s is %s.", attributeName, attributeValue);
            try {
                setter.accept(attributeValue);
            } catch (Exception exception) {
                throw Utils.exception(
                        exception,
                        "Error setting attribute '%s' with value '%s'.",
                        attributeName,
                        attributeValue);
            }
        }
    }
}
