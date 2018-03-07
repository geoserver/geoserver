/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Representation of the API as a set of beans, for Jackson to translate down in JSON and YAML.
 * The class contains a makeshift object model for Swagger, based on observation, not actual spec reading and probably
 * not formally correct, but good enough for the moment (looks like Java libs are more concentrated on code generation,
 * but somewhere in there, if it does not have too many dependencies, there is probably a proper object model for this).
 * <p>
 * Actually, found this after implementing the class, https://github
 * .com/swagger-api/swagger-core/tree/master/modules/swagger-models.
 * It might  be used for re-implementing this class in a cleaner, less error prone and more general way (although,
 * think about
 * streaming support for large API documents too)
 */
@JsonPropertyOrder({"openapi", "info", "paths", "parameters"})
public class APIDocument {

    private static final String TYPE_STRING = "string";
    private static final String TYPE_NUMBER = "number";
    private static final String TYPE_INTEGER = "integer";
    private static final String TYPE_BOOLEAN = "boolean";
    private static final String TYPE_ARRAY = "array";
    private static final String TYPE_OBJECT = "object";
    public static final String IN_QUERY = "query";
    public static final String IN_PATH = "path";
    private static final Reference REF_FORMAT = new Reference("#/components/parameters/f");
    private static final Reference REF_START_INDEX = new Reference("#/components/parameters/startIndex");
    private static final Reference REF_COUNT = new Reference("#/components/parameters/count");
    private static final Reference REF_BBOX = new Reference("#/components/parameters/bbox");
    private static final Reference REF_RESULT_TYPE = new Reference("#/components/parameters/resultType");
    private static final Reference REF_ID = new Reference("#/components/parameters/id");
    private static final Reference REF_EXCEPTION = new Reference("#/components/schemas/exception");
    private static final String TAG_CAPABILITIES = "Capabilities";
    private static final Response ERROR_RESPONSE;

    static {
        ERROR_RESPONSE = new Response("An error occurred");
        ERROR_RESPONSE.addFormat(BaseRequest.JSON_MIME, new FormatDescription().withReference(REF_EXCEPTION));
        // uncomment when HTML format is supported
        // ERROR_RESPONSE.addFormat(BaseRequest.HTML_MIME, new FormatDescription().withType(TYPE_STRING));
    }


    public class Info {
        public String getTitle() {
            return wfs.getTitle() == null ? "WFS 3.0 server" : wfs.getTitle();
        }

        public String getDescription() {
            return wfs.getAbstract();
        }

        public Contact getContact() {
            return new Contact(wfs.getGeoServer().getGlobal().getSettings().getContact());
        }

        public String getVersion() {
            return "0.0.1";
        }

    }

    public class Contact {
        ContactInfo contact;

        public Contact(ContactInfo contact) {
            this.contact = contact;
        }

        public String getName() {
            return Stream.of(contact.getContactPerson(), contact.getContactOrganization())
                    .filter(s -> s != null)
                    .collect(Collectors.joining(" - "));
        }

        public String getEmail() {
            return contact.getContactEmail();
        }

        public String getUrl() {
            return contact.getOnlineResource();
        }
    }

    public static class Reference {

        String reference;

        public Reference(String reference) {
            this.reference = reference;
        }

        @JsonProperty("$ref")
        public String getReference() {
            return reference;
        }
    }

    public static class Type {

        String type;

        public Type(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public class Method {
        String summary;
        String operationId;
        List<String> tags;
        List<Object> parameters;
        Map<String, Response> responses;

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getOperationId() {
            return operationId;
        }

        public void setOperationId(String operationId) {
            this.operationId = operationId;
        }

        public List<String> getTags() {
            return tags;
        }

        public void addTag(String tag) {
            if (tags == null) {
                tags = new ArrayList<>();
            }
            tags.add(tag);
        }

        public List<Object> getParameters() {
            return parameters;
        }

        public void addParameter(Object parameter) {
            if (parameters == null) {
                parameters = new ArrayList<>();
            }
            parameters.add(parameter);
        }

        public Map<String, Response> getResponses() {
            return responses;
        }

        public void addResponse(String status, Response response) {
            if (responses == null) {
                responses = new LinkedHashMap<>();
            }
            responses.put(status, response);
        }

    }

    public static class Response {
        String description;
        Map<String, FormatDescription> content;


        public Response(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Map<String, FormatDescription> getContent() {
            return content;
        }

        public void setContent(Map<String, FormatDescription> content) {
            this.content = content;
        }

        public void addFormat(String name, FormatDescription format) {
            if (content == null) {
                content = new HashMap<>();
            }
            content.put(name, format);
        }
    }

    public static class FormatDescription {
        Object schema;

        public FormatDescription withReference(String reference) {
            this.schema = new Reference(reference);
            return this;
        }

        public FormatDescription withReference(Reference reference) {
            this.schema = reference;
            return this;
        }

        public FormatDescription withType(String type) {
            this.schema = new Type(type);
            return this;
        }

        public Object getSchema() {
            return schema;
        }

        public void setSchema(Object schema) {
            this.schema = schema;
        }

    }

    public static class Items {
        String type;
        Object minimum;
        Object maximum;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Object getMinimum() {
            return minimum;
        }

        public void setMinimum(Object minimum) {
            this.minimum = minimum;
        }

        public Object getMaximum() {
            return maximum;
        }

        public void setMaximum(Object maximum) {
            this.maximum = maximum;
        }
    }

    public static class Parameter {
        String name;
        String in;
        Boolean required;
        String description;
        Schema schema;
        String style;
        String type;
        Items items;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIn() {
            return in;
        }

        public void setIn(String in) {
            this.in = in;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Boolean getRequired() {
            return required;
        }

        public void setRequired(Boolean required) {
            this.required = required;
        }

        public Schema getSchema() {
            return schema;
        }

        public void setSchema(Schema schema) {
            this.schema = schema;
        }

        public String getStyle() {
            return style;
        }

        public void setStyle(String style) {
            this.style = style;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Parameter withType(String type) {
            this.type = type;
            return this;
        }

        public Items getItems() {
            return items;
        }

        public void setItems(Items items) {
            this.items = items;
        }
    }

    public static class Schema {
        String type;
        String name;
        String in;
        List<String> required;
        Map<String, Parameter> properties;
        Items items;
        List<String> example;
        Object minimum;
        Object maximum;
        Object def;
        Integer minItems;
        Integer maxItems;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIn() {
            return in;
        }

        public void setIn(String in) {
            this.in = in;
        }

        public List<String> getRequired() {
            return required;
        }

        public void setRequired(List<String> required) {
            this.required = required;
        }

        public void addRequired(String name) {
            if (required == null) {
                required = new ArrayList<>();
            }
            required.add(name);
        }

        public Map<String, Parameter> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Parameter> properties) {
            this.properties = properties;
        }

        public void addProperty(String name, Parameter property) {
            if (properties == null) {
                properties = new LinkedHashMap<>();
            }
            properties.put(name, property);
        }

        public Items getItems() {
            return items;
        }

        public void setItems(Items items) {
            this.items = items;
        }

        public List<String> getExample() {
            return example;
        }

        public void setExample(List<String> example) {
            this.example = example;
        }

        public void addExample(String name) {
            if (example == null) {
                example = new ArrayList<>();
            }
            example.add(name);
        }

        public Object getMinimum() {
            return minimum;
        }

        public void setMinimum(Object minimum) {
            this.minimum = minimum;
        }

        public Object getMaximum() {
            return maximum;
        }

        public void setMaximum(Object maximum) {
            this.maximum = maximum;
        }

        public Object getDefault() {
            return def;
        }

        public void setDefault(Object def) {
            this.def = def;
        }

        public Integer getMinItems() {
            return minItems;
        }

        public void setMinItems(Integer minItems) {
            this.minItems = minItems;
        }

        public Integer getMaxItems() {
            return maxItems;
        }

        public void setMaxItems(Integer maxItems) {
            this.maxItems = maxItems;
        }

    }

    private final WFSInfo wfs;
    private final Catalog catalog;
    private String openapi = "3.0.0";


    public APIDocument(WFSInfo wfs, Catalog catalog) {
        this.wfs = wfs;
        this.catalog = catalog;
    }

    public String getOpenapi() {
        return openapi;
    }

    public Info getInfo() {
        return new Info();
    }

    public Map<String, Object> getPaths() {
        // TODO: make this streaming somehow
        // TODO: make all output content reflect from available responses

        Map<String, Object> result = new LinkedHashMap<>();
        // 
        Method caps = new Method();
        caps.addTag(TAG_CAPABILITIES);
        caps.setSummary("describe the feature collections in the dataset");
        caps.setOperationId("describeCollections");
        caps.addParameter(REF_FORMAT);
        Response contents = new Response("The feature collections shared by this API");
        contents.addFormat(BaseRequest.JSON_MIME, new FormatDescription().withReference
                ("'#/components/schemas/content"));
        // uncomment when HTML format is supported
        // contents.addFormat(BaseRequest.HTML_MIME, new FormatDescription().withType(TYPE_STRING));
        caps.addResponse("200", contents);
        caps.addResponse("default", ERROR_RESPONSE);
        result.put("/", Collections.singletonMap("get", caps));

        Method api = new Method();
        api.setOperationId("getApiDescription");
        api.setSummary("this API");
        api.addTag(TAG_CAPABILITIES);
        api.addParameter(REF_FORMAT);
        Response apiResponse = new Response("This API");
        apiResponse.addFormat(BaseRequest.JSON_MIME, new FormatDescription().withType(TYPE_OBJECT));
        // uncomment when HTML format is supported
        // apiResponse.addFormat(BaseRequest.HTML_MIME, new FormatDescription().withType(TYPE_STRING));
        apiResponse.addFormat(BaseRequest.YAML_MIME, new FormatDescription().withType(TYPE_OBJECT));
        api.addResponse("200", apiResponse);
        api.addResponse("default", ERROR_RESPONSE);
        result.put("/api", Collections.singletonMap("get", api));

        Method conformance = new Method();
        conformance.setOperationId("getRequirementsClasses");
        conformance.setSummary("list all requirements classes specified in a standard (e.g., WFS 3.0) that the server" +
                " conforms to");
        conformance.addTag(TAG_CAPABILITIES);
        Response conformanceResponse = new Response("the URIs of all requirements classes supported by the server");
        conformanceResponse.addFormat(BaseRequest.JSON_MIME, new FormatDescription().withReference
                ("#/components/schemas/req-classes"));
        conformance.addResponse("200", conformanceResponse);
        conformance.addResponse("default", ERROR_RESPONSE);
        result.put("/api/conformance", Collections.singletonMap("get", conformance));

        Map<String, FormatDescription> outputFormats = getAvailableFormats();
        for (FeatureTypeInfo ftInfo : catalog.getFeatureTypes()) {
            Method layer = new Method();
            String layerName = NCNameResourceCodec.encode(ftInfo);
            layer.setSummary("Retrieve features from " + ftInfo.prefixedName());
            layer.setOperationId("Get" + layerName);
            layer.addTag("Features");
            layer.addParameter(REF_FORMAT);
            layer.addParameter(REF_START_INDEX);
            layer.addParameter(REF_COUNT);
            layer.addParameter(REF_RESULT_TYPE);
            layer.addParameter(REF_BBOX);
            Response layerResponse = new Response("Information about the feature collection plus the first features " +
                    "matching the selection parameters.");
            layerResponse.setContent(outputFormats);
            layer.addResponse("200", layerResponse);
            layer.addResponse("default", ERROR_RESPONSE);
            result.put("/" + layerName, Collections.singletonMap("get", layer));

            Method layerId = new Method();
            layerId.setSummary("Retrieve one feature from " + ftInfo.prefixedName());
            layerId.setOperationId("GetSingle" + layerName);
            layerId.addTag("Feature");
            layerId.addParameter(REF_ID);
            layerId.addResponse("200", layerResponse);
            layerId.addResponse("default", ERROR_RESPONSE);
            result.put("/" + layerName + "/{id}", Collections.singletonMap("get", layerId));
        }

        return result;
    }

    public Map<String, Object> getComponents() {
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("parameters", getParameters());
        components.put("schemas", getSchemas());
        return components;
    }

    protected Map<String, Parameter> getParameters() {
        Map<String, Parameter> parameters = new LinkedHashMap<>();

        Parameter count = new Parameter();
        count.setName("count");
        count.setIn(IN_QUERY);
        Schema countSchema = new Schema();
        countSchema.setType(TYPE_INTEGER);
        countSchema.setMinimum(1);
        int maxFeatures = wfs.getMaxFeatures();
        if (maxFeatures > 0) {
            countSchema.setMaximum(maxFeatures);
            countSchema.setDefault(maxFeatures);
        }
        count.setSchema(countSchema);
        count.setDescription("The optional count parameter limits the number of items that are presented " +
                "in the response document.\n\n" +
                "Only items are counted that are on the first level of the collection in the response document.  " +
                "Nested objects contained within the explicitly requested items shall not be counted.");
        count.setStyle("form");
        parameters.put("count", count);

        Parameter startIndex = new Parameter();
        startIndex.setName("startIndex");
        startIndex.setIn(IN_QUERY);
        Schema indexSchema = new Schema();
        indexSchema.setType(TYPE_INTEGER);
        startIndex.setSchema(indexSchema);
        startIndex.setDescription("The optional offset of the first item returned, can be used for random paging.");
        startIndex.setStyle("form");
        parameters.put("startIndex", startIndex);

        Parameter bbox = new Parameter();
        bbox.setName("bbox");
        bbox.setIn(IN_QUERY);
        bbox.setDescription("Only features that have a geometry that intersects the bounding box are\n" +
                "selected. The bounding box is provided as four numbers:\n" +
                "* Lower left corner, coordinate axis 1\n" +
                "* Lower left corner, coordinate axis 2\n" +
                "* Upper right corner, coordinate axis 1\n" +
                "* Upper right corner, coordinate axis 2\n" +
                "For WGS84 longitude/latitude this is in most cases the sequence of\n" +
                "minimum longitude, minimum latitude, maximum longitude and maximum latitude.\n" +
                "However, in cases where the box spans the antimeridian the first value\n" +
                "(west-most box edge) is larger than the third value (east-most box edge).");
        Schema bboxSchema = new Schema();
        bboxSchema.setType(TYPE_ARRAY);
        bboxSchema.setMinItems(4);
        bboxSchema.setMaxItems(4);
        bbox.setSchema(bboxSchema);
        Items items = new Items();
        items.setType(TYPE_NUMBER);
        bboxSchema.setItems(items);
        bbox.setStyle("form");
        parameters.put("bbox", bbox);

        Parameter id = new Parameter();
        id.setName("id");
        id.setIn(IN_PATH);
        id.setDescription("The id of a feature");
        id.setRequired(true);
        Schema idSchema = new Schema();
        idSchema.setType(TYPE_STRING);
        id.setSchema(idSchema);
        parameters.put("id", id);

        return parameters;
    }

    protected Map<String, Schema> getSchemas() {
        Map<String, Schema> schemas = new LinkedHashMap<>();

        Schema exception = new Schema();
        exception.setType(TYPE_OBJECT);
        exception.addRequired("code");
        exception.addProperty("code", new Parameter().withType(TYPE_STRING));
        exception.addProperty("description", new Parameter().withType(TYPE_STRING));
        schemas.put("exception", exception);

        Schema reqClasses = new Schema();
        reqClasses.setType(TYPE_OBJECT);
        reqClasses.addRequired("conformsTo");
        Parameter param = new Parameter();
        param.setType(TYPE_ARRAY);
        Items items = new Items();
        items.setType(TYPE_STRING);
        param.setItems(items);
        reqClasses.addProperty("conformsTo", param);
        reqClasses.addExample("http://www.opengis.net/spec/wfs-1/3.0/req/core");
        reqClasses.addExample("http://www.opengis.net/spec/wfs-1/3.0/req/oas30");
        reqClasses.addExample("http://www.opengis.net/spec/wfs-1/3.0/req/html");
        reqClasses.addExample("http://www.opengis.net/spec/wfs-1/3.0/req/geojson");
        schemas.put("exception", exception);

        return schemas;
    }

    protected Map<String, FormatDescription> getAvailableFormats() {
        Map<String, FormatDescription> descriptions = new LinkedHashMap<>();
        List<String> formatNames = DefaultWebFeatureService30.getAvailableFormats();
        for (String formatName: formatNames) {
            if ((formatName.contains("text") && !formatName.contains("xml") && !formatName.contains("gml")) ||
                    formatName.contains("csv")) {
                descriptions.put(formatName, new FormatDescription().withType(TYPE_STRING));
            } else {
                descriptions.put(formatName, new FormatDescription().withType(TYPE_OBJECT));
            }

        }
        return descriptions;
    }
}
