/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public final class EchoParametersDao {

    private static final Logger LOGGER = Logging.getLogger(EchoParametersDao.class);
    private static SecureXStream xStream;

    static {
        xStream = new SecureXStream();
        xStream.registerConverter(new EchoParameterConverter());
        xStream.alias("EchoParameter", EchoParameter.class);
        xStream.alias("EchoParameters", EchoParametersDao.EchoParametersList.class);
        xStream.addImplicitCollection(EchoParametersDao.EchoParametersList.class, "parameters");
        xStream.allowTypes(
                new Class[] {EchoParameter.class, EchoParametersDao.EchoParametersList.class});
    }

    public static String getEchoParametersPath() {
        return "params-extractor/echo-parameters.xml";
    }

    public static String getTmpEchoParametersPath() {
        return String.format("params-extractor/%s-echo-parameters.xml", UUID.randomUUID());
    }

    public static List<EchoParameter> getEchoParameters() {
        Resource echoParameters = getDataDirectory().get(getEchoParametersPath());
        return getEchoParameters(echoParameters.in());
    }

    private static GeoServerDataDirectory getDataDirectory() {
        return (GeoServerDataDirectory) GeoServerExtensions.bean("dataDirectory");
    }

    public static List<EchoParameter> getEchoParameters(InputStream inputStream) {
        try {
            if (inputStream.available() == 0) {
                Utils.debug(LOGGER, "Echo parameters file seems to be empty.");
                return new ArrayList<>();
            }
            EchoParametersList list = (EchoParametersList) xStream.fromXML(inputStream);
            return list.parameters == null ? new ArrayList<>() : list.parameters;
        } catch (Exception exception) {
            throw Utils.exception(exception, "Error parsing echo parameters files.");
        } finally {
            Utils.closeQuietly(inputStream);
        }
    }

    public static void saveOrUpdateEchoParameter(EchoParameter echoParameter) {
        Resource echoParameters = getDataDirectory().get(getEchoParametersPath());
        Resource tmpEchoParameters = getDataDirectory().get(getTmpEchoParametersPath());
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
        Resource echoParameters = getDataDirectory().get(getEchoParametersPath());
        Resource tmpEchoParameters = getDataDirectory().get(getTmpEchoParametersPath());
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
            xStream.toXML(new EchoParametersList(echoParameters), outputStream);
        } catch (Exception exception) {
            throw Utils.exception(exception, "Something bad happen when writing echo parameters.");
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

    /** Support class for XStream serialization */
    static final class EchoParametersList {
        List<EchoParameter> parameters;

        public EchoParametersList(List<EchoParameter> rules) {
            this.parameters = rules;
        }
    }
}
