/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlAnnotationIntrospector;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.response.WFSResponse;
import org.geoserver.wfs3.BaseRequest;

/** Response encoding outputs in JSON/YAML using Jackson */
public abstract class JacksonResponse extends WFSResponse {

    public JacksonResponse(GeoServer gs, Class targetClass) {
        this(
                gs,
                targetClass,
                new LinkedHashSet<>(
                        Arrays.asList(
                                BaseRequest.JSON_MIME,
                                BaseRequest.YAML_MIME,
                                BaseRequest.XML_MIME)));
    }

    protected JacksonResponse(GeoServer gs, Class targetClass, Set<String> formats) {
        super(gs, targetClass, formats);
    }

    @Override
    public boolean canHandle(Operation operation) {
        return isJsonFormat(operation) || isYamlFormat(operation) || isXMLFormat(operation);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        if (isJsonFormat(operation)) {
            return BaseRequest.JSON_MIME;
        } else if (isYamlFormat(operation)) {
            return BaseRequest.YAML_MIME;
        } else if (isXMLFormat(operation)) {
            return BaseRequest.XML_MIME;
        } else {
            throw new ServiceException("Unknown format requested " + getFormat(operation));
        }
    }

    /**
     * Returns the requested format from the operation, falls back to {@link BaseRequest#JSON_MIME}
     * if none was requested
     */
    protected String getFormat(Operation operation) {
        BaseRequest request = (BaseRequest) operation.getParameters()[0];
        Optional<String> format = Optional.ofNullable(request.getOutputFormat());
        return format.orElse(BaseRequest.JSON_MIME);
    }

    /** Checks if the operation requested the JSON format */
    protected boolean isJsonFormat(Operation operation) {
        return BaseRequest.JSON_MIME.equalsIgnoreCase(getFormat(operation));
    }

    /** Checks if the operation requested the YAML format */
    protected boolean isYamlFormat(Operation operation) {
        return BaseRequest.YAML_MIME.equalsIgnoreCase(getFormat(operation));
    }

    /** Checks if the operation requested the XML format */
    protected boolean isXMLFormat(Operation operation) {
        return BaseRequest.XML_MIME.equalsIgnoreCase(getFormat(operation));
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        ObjectMapper mapper;
        if (isYamlFormat(operation)) {
            mapper = Yaml.mapper();
        } else if (isXMLFormat(operation)) {
            mapper = new XmlMapper();
            // using a custom annotation introspector to set the desired namespace
            mapper.setAnnotationIntrospector(
                    new JacksonXmlAnnotationIntrospector() {
                        @Override
                        public String findNamespace(Annotated ann) {
                            String ns = super.findNamespace(ann);
                            if (ns == null || ns.isEmpty()) {
                                return "http://www.opengis.net/wfs/3.0";
                            } else {
                                return ns;
                            }
                        }
                    });

        } else {
            mapper = Json.mapper();
            mapper.writer(new DefaultPrettyPrinter());
        }

        mapper.writeValue(output, value);
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        return getFileName(value, operation) + (isJsonFormat(operation) ? ".json" : ".yaml");
    }

    /** Just the name of the file to be returned (no extension) */
    protected abstract String getFileName(Object value, Operation operation);
}
