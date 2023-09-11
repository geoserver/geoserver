/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.geotools.data.complex.util.ComplexFeatureConstants.FEATURE_CHAINING_LINK_NAME;

import io.swagger.v3.oas.models.media.Schema;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.PropertyType;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

public class QueryablesBuilder {

    public static final String POINT_SCHEMA_REF = "https://geojson.org/schema/Point.json";
    public static final String MULTIPOINT_SCHEMA_REF = "https://geojson.org/schema/MultiPoint.json";
    public static final String LINESTRING_SCHEMA_REF = "https://geojson.org/schema/LineString.json";
    public static final String MULTILINESTRING_SCHEMA_REF =
            "https://geojson.org/schema/MultiLineString.json";
    public static final String POLYGON_SCHEMA_REF = "https://geojson.org/schema/Polygon.json";
    public static final String MULTIPOLYGON_SCHEMA_REF =
            "https://geojson.org/schema/MultiPolygon.json";
    public static final String GEOMETRY_SCHEMA_REF = "https://geojson.org/schema/Geometry.json";

    public static final String POINT = "Point";
    public static final String MULTIPOINT = "MultiPoint";
    public static final String LINESTRING = "LineString";
    public static final String MULTILINESTRING = "MultiLineString";
    public static final String POLYGON = "Polygon";
    public static final String MULTIPOLYGON = "MultiPolygon";
    public static final String GENERIC_GEOMETRY = "Generic geometry";
    public static final String DATE = "Date";
    public static final String DATE_TIME = "DateTime";
    public static final String TIME = "Time";

    Queryables queryables;

    public QueryablesBuilder(String id) {
        this.queryables = new Queryables(id);
        this.queryables.setType("object");
    }

    public QueryablesBuilder forType(FeatureTypeInfo ft) throws IOException {
        this.queryables.setCollectionId(ft.prefixedName());
        this.queryables.setTitle(Optional.ofNullable(ft.getTitle()).orElse(ft.prefixedName()));
        this.queryables.setDescription(ft.getDescription());
        return forType(ft.getFeatureType());
    }

    public QueryablesBuilder forType(FeatureType ft) {
        Map<String, Schema> properties =
                ft.getDescriptors().stream()
                        .filter( // ignore feature chaining links, they might be duplicated
                                ad -> !ad.getName().equals(FEATURE_CHAINING_LINK_NAME))
                        .collect(
                                Collectors.toMap(
                                        ad -> ad.getName().getLocalPart(),
                                        ad -> getSchema(ad.getType()),
                                        (u, v) -> {
                                            throw new IllegalStateException(
                                                    String.format("Duplicate key %s", u));
                                        },
                                        () -> new LinkedHashMap<>()));
        this.queryables.setProperties(properties);
        return this;
    }

    private Schema<?> getSchema(PropertyType type) {
        Class<?> binding = type.getBinding();
        return getSchema(binding);
    }

    /** Returns the schema for a given data type */
    public static Schema<?> getSchema(Class<?> binding) {
        if (Geometry.class.isAssignableFrom(binding)) return getGeometrySchema(binding);
        else return getAlphanumericSchema(binding);
    }

    private static Schema<?> getGeometrySchema(Class<?> binding) {
        Schema schema = new Schema();
        String ref;
        String description;
        if (Point.class.isAssignableFrom(binding)) {
            ref = POINT_SCHEMA_REF;
            description = POINT;
        } else if (MultiPoint.class.isAssignableFrom(binding)) {
            ref = MULTIPOINT_SCHEMA_REF;
            description = MULTIPOINT;
        } else if (LineString.class.isAssignableFrom(binding)) {
            ref = LINESTRING_SCHEMA_REF;
            description = LINESTRING;
        } else if (MultiLineString.class.isAssignableFrom(binding)) {
            ref = MULTILINESTRING_SCHEMA_REF;
            description = MULTILINESTRING;
        } else if (Polygon.class.isAssignableFrom(binding)) {
            ref = POLYGON_SCHEMA_REF;
            description = POLYGON;
        } else if (MultiPolygon.class.isAssignableFrom(binding)) {
            ref = MULTIPOLYGON_SCHEMA_REF;
            description = MULTIPOLYGON;
        } else {
            ref = GEOMETRY_SCHEMA_REF;
            description = GENERIC_GEOMETRY;
        }

        schema.set$ref(ref);
        schema.setDescription(description);
        return schema;
    }

    /**
     * Returns a schema for a given data type, assuming it is alphanumeric
     *
     * @param binding the data type
     * @return the schema
     */
    public static Schema<?> getAlphanumericSchema(Class<?> binding) {
        Schema<?> schema = new Schema<>();

        schema.setType(org.geoserver.ogcapi.AttributeType.fromClass(binding).getType());
        schema.setDescription(schema.getType());
        if (java.sql.Date.class.isAssignableFrom(binding)) {
            schema.setFormat("date");
            schema.setDescription(DATE);
        } else if (java.sql.Time.class.isAssignableFrom(binding)) {
            schema.setFormat("time");
            schema.setDescription(TIME);
        } else if (java.util.Date.class.isAssignableFrom(binding)) {
            schema.setFormat("date-time");
            schema.setDescription(DATE_TIME);
        }
        return schema;
    }

    public Queryables build() {
        return queryables;
    }
}
