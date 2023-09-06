/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.QueryablesBuilder;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.http.HttpStatus;

/** Simple JSON schema builder based on classpath resources and a feature type */
public class JSONFGSchemaBuilder {

    static final ObjectMapper MAPPER = getObjectMapper();

    private static ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    private final FeatureType featureType;
    private final String schemaId;

    private List<Class<?>> KNOWN_GEOMETRY_TYPES =
            List.of(
                    Point.class,
                    LineString.class,
                    Polygon.class,
                    MultiPoint.class,
                    MultiLineString.class,
                    MultiPolygon.class,
                    GeometryCollection.class);

    public JSONFGSchemaBuilder(FeatureType featureType, String schemaId) {
        this.featureType = featureType;
        this.schemaId = schemaId;
    }

    public String build() throws IOException {
        // sanity check
        String fileName = "schema/" + schemaId + ".json";
        if (getClass().getResource(fileName) == null) {
            throw new APIException(
                    APIException.NOT_FOUND,
                    "No schema found for " + schemaId,
                    HttpStatus.NOT_FOUND);
        }

        // load the schema from the classpath as a string
        String schema;
        try (InputStream is = getClass().getResourceAsStream(fileName)) {
            schema = IOUtils.toString(is, StandardCharsets.UTF_8);
        }

        // only the  feature schema changes based on the feature type, the rest is static
        if (!schemaId.equals("feature")) return schema;

        // replace the feature type name in the schema
        JsonNode root = MAPPER.readTree(schema);

        // TODO: this has been done using a low level JSON manipulation, but it may
        // be better to use the higher level API used by QueryablesBuilder. Not a big
        // amount of code anyways, so not a big deal.

        // care for eventual geometryless objects
        ObjectNode geometry = (ObjectNode) root.get("properties").get("geometry");
        geometry.removeAll();
        ObjectNode place = (ObjectNode) root.get("properties").get("place");
        place.removeAll();
        if (featureType.getGeometryDescriptor() == null) {
            geometry.put("type", "null");
            place.put("type", "null");
        } else {
            Class<?> binding = featureType.getGeometryDescriptor().getType().getBinding();
            Optional<String> type =
                    KNOWN_GEOMETRY_TYPES.stream()
                            .filter(t -> t.isAssignableFrom(binding))
                            .map(t -> t.getSimpleName())
                            .findFirst();
            if (type.isPresent()) {
                addSingleGeometryType(geometry, place, type.get());
            } else {
                throw new IllegalArgumentException("Unsupported geometry type " + binding);
            }
        }

        // handle the other properties
        ObjectNode propertiesObject =
                (ObjectNode) root.get("properties").get("properties").get("oneOf").get(1);
        featureType.getDescriptors().stream()
                // want to skip the default geometry, unclear how to handle other geometry
                // properties
                .filter(d -> !(d instanceof GeometryDescriptor))
                .forEach(
                        d ->
                                propertiesObject.set(
                                        d.getName().getLocalPart(),
                                        MAPPER.valueToTree(
                                                QueryablesBuilder.getAlphanumericSchema(
                                                        d.getType().getBinding()))));

        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private static void addSingleGeometryType(ObjectNode geometry, ObjectNode place, String type) {
        ObjectNode nullType = MAPPER.createObjectNode();
        nullType.put("type", "null");
        ObjectNode refType = MAPPER.createObjectNode();
        refType.put("$ref", "geometry-objects.json#/$defs/" + type);
        ArrayNode values = MAPPER.createArrayNode();
        values.add(nullType);
        values.add(refType);

        // when GeoServer supports solids, "geometry" will need a different type
        // (e.g., a lower dimensional equivalent of the actual geometry)
        geometry.set("oneOf", values);
        place.set("oneOf", values);
    }
}
