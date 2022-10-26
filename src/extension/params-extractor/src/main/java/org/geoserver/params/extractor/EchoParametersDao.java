/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
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
        return getEchoParameters(echoParameters);
    }

    private static GeoServerDataDirectory getDataDirectory() {
        return (GeoServerDataDirectory) GeoServerExtensions.bean("dataDirectory");
    }

    /**
     * Read a list of EchoParameter from a resource. Return an empty list if the resource does not
     * exist. This prevents Resource.in() from creating the file or throwing an exception.
     *
     * @param resource to read.
     * @return a list of EchoParameter or an empty list if the resource does not exist.
     */
    public static List<EchoParameter> getEchoParameters(Resource resource) {
        if (resource.getType() == Resource.Type.RESOURCE) {
            try (InputStream inputStream = resource.in()) {
                if (inputStream.available() == 0) {
                    Utils.debug(LOGGER, "Echo parameters file seems to be empty.");
                } else {
                    EchoParametersList list = (EchoParametersList) xStream.fromXML(inputStream);
                    return list.parameters == null ? new ArrayList<>() : list.parameters;
                }
            } catch (Exception exception) {
                throw Utils.exception(exception, "Error parsing echo parameters files.");
            }
        } else {
            Utils.info(LOGGER, "Echo parameters file does not exist.");
        }
        return new ArrayList<>();
    }

    public static void saveOrUpdateEchoParameter(EchoParameter echoParameter) {
        Resource echoParameters = getDataDirectory().get(getEchoParametersPath());
        Resource tmpEchoParameters = getDataDirectory().get(getTmpEchoParametersPath());
        saveOrUpdateEchoParameter(echoParameter, echoParameters, tmpEchoParameters);
        echoParameters.delete();
        tmpEchoParameters.renameTo(echoParameters);
    }

    public static void saveOrUpdateEchoParameter(
            EchoParameter echoParameter, Resource input, Resource output) {
        List<EchoParameter> echoParameters = getEchoParameters(input);
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

        writeEchoParameters(echoParameters, output);
    }

    public static void deleteEchoParameters(String... echoParametersIds) {
        Resource echoParameters = getDataDirectory().get(getEchoParametersPath());
        Resource tmpEchoParameters = getDataDirectory().get(getTmpEchoParametersPath());
        deleteEchoParameters(echoParameters, tmpEchoParameters, echoParametersIds);
        echoParameters.delete();
        tmpEchoParameters.renameTo(echoParameters);
    }

    public static void deleteEchoParameters(
            Resource inputResource, Resource outputResource, String... forwardParameterIds) {

        List<EchoParameter> collect =
                getEchoParameters(inputResource).stream()
                        .filter(p -> !ArrayUtils.contains(forwardParameterIds, p.getId()))
                        .collect(Collectors.toList());

        writeEchoParameters(collect, outputResource);
    }

    private static void writeEchoParameters(List<EchoParameter> echoParameters, Resource output) {
        try (OutputStream outputStream = output.out()) {
            xStream.toXML(new EchoParametersList(echoParameters), outputStream);
        } catch (Throwable exception) {
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
